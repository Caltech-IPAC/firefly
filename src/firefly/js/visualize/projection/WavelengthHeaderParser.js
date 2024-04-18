/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {makeDoubleHeaderParse} from '../FitsHeaderUtil.js';
import {convertToArraySize} from '../../tables/TableUtil.js';
import {algorithmTypes, spectralCoordTypes, LINEAR, TAB, WAVE, VRAD} from './Wavelength.js';  // eslint-disable-line no-unused-vars
import {isDefined} from '../../util/WebUtil.js';
import {Logger} from '../../util/Logger.js';


/**
 * @typedef WavelengthTabRelatedData
 *
 * @prop {String} dataType - for wavelength, the dataType should always be RDConst.WAVELENGTH_TABLE_RESOLVED
 * @prop {String} hduName - the name of the table hdu
 * @prop {int} hduVersion - table version number
 * @prop {int} hduLevel - table level number
 * @prop {String} hduIdx - the index of the table hdu
 * @prop {TableModel} table - the table with wavelength algorythm
 */

const logger = Logger('SpectralCoord');

/**
 *
 * @param {FitsHeader} header
 * @param {String} altWcs
 * @param {FitsHeader} zeroHeader
 * @param {Array.<WavelengthTabRelatedData>} wlTableRelatedAry
 * @return {SpectralWCSData|undefined}
 */
export function parseWavelengthHeaderInfo(header, altWcs = '', zeroHeader, wlTableRelatedAry) {
    try {
        return doParse(header, altWcs, zeroHeader, wlTableRelatedAry);
    } catch (e) {
        logger.error(`Exception parsing wavelength header: ${e?.message}`);
        logger.error(e.stack);
    }
}

/**
 *
 * @param {FitsHeader} header
 * @param {String} altWcs
 * @param {FitsHeader} zeroHeader
 * @param {Array.<WavelengthTabRelatedData>} wlTableRelatedAry
 * @return {SpectralWCSData|undefined}
 */
function doParse(header, altWcs = '', zeroHeader, wlTableRelatedAry) {
    const parse = makeDoubleHeaderParse(header, zeroHeader, altWcs);

    // identify spectral coordinates and the subset that requires table lookup (-TAB) algorithm
    const {iCtypeSupported, iCtypeTab} = findSupportedCTYPEs(parse, altWcs, [WAVE, VRAD]); // array of i, for which CTYPEi=XXXX-TAB

    if (iCtypeSupported.length === 0) return;

    const spectralCoords = [];
    const failWarnings = [];

    // fields that belong to WCS, not to a specific spectral coordinate
    const {nAxis, N, r_j, restWAV} = calculateSharedParams(parse, altWcs);
    if (N < 0) return;  // Dimension value is not available, must be NAXIS or WCSAXES

    // loop through spectral coordinates, assemble per-coordinate parameters
    iCtypeSupported.forEach((which) => {
        const {ctype, cname, units, crpix, crval, cdelt} = getCoreSpectralHeaders(parse, altWcs, which);

        const mijMatrixKeyRoot = getPC_ijKey(parse, which);
        if (!mijMatrixKeyRoot) {
            failWarnings.push(`${ctype}: both PC i_j and CD i_j are present`);
            return;
        }

        let pc_3j = parse.getDoubleAry(`${mijMatrixKeyRoot}${which}_`, altWcs, 1, N, undefined);
        if (!pc_3j && mijMatrixKeyRoot === 'CD') {
            // CD matrix values default to 0.0 - the spectral coordinate value is constant
            failWarnings.push(`CD3_i${altWcs} is not defined: ${ctype} value is constant`);
        }

        //check if any value is not defined, use default
        pc_3j = applyDefaultValues(pc_3j, mijMatrixKeyRoot, which, N);

        const {algorithm, coordType} = getAlgorithmAndType(ctype);

        // per-plane readout is possible only for the coordinates that do not depend on any axis but the 3rd (index 2)
        const canDoPlaneCalc = nAxis === 3 && pc_3j?.every((v, i) => i === 2 ? v !== 0 : v === 0) &&
            allGoodValues(crpix, crval, cdelt && nAxis === 3);

        const spectralCoord = {
            planeOnly: canDoPlaneCalc,
            name: cname.trim() || spectralCoordTypes[coordType].type,
            symbol: spectralCoordTypes[coordType].symbol,
            units: units.trim() || spectralCoordTypes[coordType].defaultUnits,
            coordType, algorithm,
            ctype, N, r_j, pc_3j,
            crval, cdelt,
            restWAV};

        if (iCtypeTab.includes(which)) {
            // get lookup table data
            const tab = calculateTabParameters(parse, altWcs, which, wlTableRelatedAry, ctype, failWarnings);
            if (!tab) return;  // parsing error

            Object.assign(spectralCoord, {tab});
        }
        spectralCoords.push(spectralCoord);
    });

    // In theory, WCS could contain multiple spectral coordinates of different kind.
    // In practice, there will be one spectral coordinate in a WCS.
    // If there are more than two, they are likely related to each other.

    const spectralWCSData = {
        headerType: 'wavelength',
        spectralCoords,
        hasPlainOnlyCoordInfo: spectralCoords.some((c) => c.planeOnly),
        hasPixelLevelCoordInfo: spectralCoords.some((c) => !c.planeOnly),
        isNonSeparableTABGroup: isNonSeparableTABGroup(spectralCoords)  // always pixel level, must be processed together
    };

    if (failWarnings.length === 0) {
        spectralWCSData.failReason = failWarnings.join(', ');
    }

    return spectralWCSData;
}

const MAX_CTYPES = 4;

/**
 * Get indices of CTYPEs that we need to preserve: all and those requiring table lookup algorithm.
 * @param parse
 * @param {String} altWcs alternate WCS character
 * @param supportedCoordTypes list of the supported coordinate type codes
 * @returns {{iCtypeSupported: Array.<int>, iCtypeTab: Array.<int>}} indices of the supported coordinates and those requiring TAB algorithm
 */
function findSupportedCTYPEs(parse, altWcs = '', supportedCoordTypes=Object.keys(spectralCoordTypes)) {
    const iCtypeSupported = [];  // supported ctype indexes
    const iCtypeTab = [];  // ctype indexes requiring TAB algorithm

    for (let i = 1; i <= MAX_CTYPES; i++) {
        const ctype = parse.getValue(`CTYPE${i}${altWcs}`, ' ');
        const {algorithm, coordType} = getAlgorithmAndType(ctype);

        if (algorithmTypes[algorithm]?.implemented && supportedCoordTypes.includes(coordType)) {
            iCtypeSupported.push(i);
        }
        if (algorithm === TAB) {
            iCtypeTab.push(i);
        }
    }
    return {iCtypeSupported, iCtypeTab};
}

/**
 * Check if spectral coordinates form a non-separable group.
 * @param {Array.<SpectralCoord>} spectralCoords
 * @returns {boolean} True if the coordinates form non-separable TAB group
 */
function isNonSeparableTABGroup(spectralCoords) {
    // check if the spectral coordinates form a non-separable table lookup group

    // there must be more than one coordinate in a group
    if (spectralCoords.length < 2) return false;

    // all of them must be table lookup coordinates
    if (spectralCoords.some((c) => c.algorithm !== TAB)) return false;

    // all coordinates refer to the same table and output column
    const table = spectralCoords[0].tab.table;
    const ps3_1 = spectralCoords[0].tab.ps3_1.toUpperCase();
    return spectralCoords.slice(1).every((c) => c.tab.table === table || c.tab.ps3_1.toUpperCase() === ps3_1);
}


/**
 * Get parameters that can be relevant to multiple spectral coordinates in a WCS.
 * @param parse
 * @param altWcs
 * @returns {{nAxis: int, restWAV: number, r_j: Array.<int>, N: int}}
 */
function calculateSharedParams(parse, altWcs) {

    // todo should N be max(WCSAXESa, NAXIS)?
    // WCSAXES – [integer; default: NAXIS, or larger of WCS indices i
    // or j]. Number of axes in the WCS description. This keyword,
    // if present, must precede all WCS keywords except NAXIS in
    // the HDU. The value of WCSAXES may exceed the number of
    // pixel axes for the HDU.
    //
    // It is not recommended for WCSAXES to be smaller than NAXIS,
    // as this would imply that some of the data dimensions are not included in the WCS.
    const N = parse.getIntOneOfValue(['WCSAXES', 'WCSAXIS', 'NAXIS'], -1);

    const nAxis = parse.getIntValue('NAXIS');

    // reference pixel, CRPIXj default is 0.0
    let r_j = parse.getDoubleAry('CRPIX', altWcs, 1, N, undefined);
    r_j = applyDefaultValues(r_j, 'CRPIX', 1, N);  // for CRPIX, "which" does not matter

    // rest wavelength
    const restWAV = parse.getDoubleValue('RESTWAV' + altWcs, 0);

    return {nAxis, N, r_j, restWAV};
}


/**
 * Get table lookup parameters
 * @param parse
 * @param altWcs
 * @param which
 * @param wlTableRelatedAry
 * @param ctype
 * @param failWarnings
 * @returns {LookupTableData|undefined}
 */
function calculateTabParameters(parse, altWcs, which, wlTableRelatedAry, ctype, failWarnings) {

    // matching lookup table must be present
    const table = findWLTableToMatch(parse, altWcs, which, wlTableRelatedAry);
    if (!table) {
        failWarnings.push(`${ctype}: lookup table is not found`);
        return;
    }

    // column name for the coordinate array (TTYPEn1) - PSi_1a must be present
    const ps3_1 = parse.getValue(`PS${which}_1${altWcs}`);
    if (!ps3_1) {
        failWarnings.push(`${ctype}: column name for the coordinate array is not found`);
        return;
    }

    // column name for the indexing vector (TTYPEn2) - no index if PSi_2a is not present
    const ps3_2 = parse.getValue(`PS${which}_2${altWcs}`);

    //coordinate array column must be defined
    const coordData = getArrayDataFromTable(table, ps3_1);
    if (!coordData) {
        failWarnings.push(`${ctype}: column for the coordinate array is not found in lookup table`);
        return;
    }
    if (coordData.length === 0) {
        failWarnings.push(`${ctype}: coordinate array should not be empty`);
        return;
    }

    // indexing vector column can be undefined in no index case
    const indexData = getArrayDataFromTable(table, ps3_2);

    // PVi_3a values are needed to handle more than one TAB axes
    // axis number (m) in the coordinate array
    const m = parse.getIntValue(`PV${which}_3${altWcs}`, 1);

    // confirm that innermost coordinate array dimension is big enough
    const arraySize = getArraySize(coordData);
    if (arraySize[0] < m) {
        failWarnings.push(`${ctype}: coordinate array innermost dimension ${arraySize[0]} has less than ${m} elements`);
        return;
    }

    return {coordData, indexData, m, table, ps3_1};
}


/**
 * According to the reference papers above, the coordinate has M+1 dimension where the M means the
 *  dependence on the M axis, Coords[M][K1][K2[]...[Km].  In our case, the M=1, the coordinate is the
 *  two-dimensional array, and the first dimension is M=1 and the second dimension is K1, which is the sampling
 *  number the data has been sampled. Thus, for each image point, the corresponding index or coordinate array value
 *  is coords[1][K1].  For example, if the sampling count is K1=100, each cell in the coordinate table, contains 100
 *  data values. The coordinate column can convert to 100 rows of the single value table.
 *
 *
 * @param table
 * @param columnName
 * @returns {*}
 */
const getArrayDataFromTable = (table, columnName) => {
    if (!columnName) return undefined;
    const tableData = table.tableData;
    const arrayData = tableData.data;
    const columns = tableData.columns;
    for (let i = 0; i < columns.length; i++) {
        if (columns[i].name.toUpperCase() === columnName.toUpperCase()) {
            // array data are flat array that need to be folded
            // according to array size, which is a string where
            // dimensions are separated by 'x'
            return convertToArraySize(columns[i], arrayData[0][i]);
        }
    }
    return undefined;
};

/**
 * Get lengths for all dimensions of the array, innermost dimension is the first
 * @param array
 * @returns {number[]}
 */
function getArraySize(array) {
    if (Array.isArray(array)) {
        return getArraySize(array[0]).concat([array.length]);
    } else {
        return [];
    }
}


function findWLTableToMatch(parse, altWcs, which, wlTableRelatedAry) {
    if (!wlTableRelatedAry?.length) return;
    const tName = parse.getValue(`PS${which}_0${altWcs}`);  // table name
    const tVersion = parse.getIntValue(`PV${which}_1${altWcs}`, 1);  // table version
    const tLevel = parse.getIntValue(`PV${which}_2${altWcs}`, 1);  // table level
    return wlTableRelatedAry.find((entry) =>
        entry.hduName.toUpperCase() === tName.toUpperCase() &&
        entry.hduVersion === tVersion &&
        entry.hduLevel === tLevel)?.table;
}


const allGoodValues = (...numbers) => numbers.every((n) => !isNaN(n));


/**
 * According to A&A 395, 1061-1075 (2002) DOI: 10.1051/0004-6361:20021326
 * Representations of world coordinates in FITS E. W. Greisen1 - M. R. Calabretta2
 * https://www.aanda.org/articles/aa/full/2002/45/aah3859/aah3859.html
 * the CD_ij is for the older FITS header. The newer FITS header has PC only.
 * 1. If both PC and CD exist, it is wrong. Instead of throwing exception, no wavelength is displayed.
 * 2. If PC does not exist, one or more CD is exist, use CD_ij (j=1..N); if any of CD_ij
 *   is not defined, the default is 0.0;
 * 3. If any PC_ij is defined or neither PC nor CD is defined, we use PC, the default
 *    is 0 for j!=i and 1 if j==i
 *
 * @param parser
 * @param which
 * @returns {*}
 */
function getPC_ijKey(parser, which) {

    const hasPC = parser.hasKeyStartWith(`PC${which}_`);
    const hasCD = parser.hasKeyStartWith(`CD${which}_`);

    if (hasPC && hasCD) {
        return undefined;
    }
    if (!hasPC && hasCD) {
        return 'CD';
    }
    return 'PC';
}


/**
 *
 * Reference:  A&A 395, 1061-1075 (2002) DOI: 10.1051/0004-6361:20021326
 * Representations of world coordinates in FITS E. W. Greisen1 - M. R. Calabretta2
 * paper,  the CD_ij is for the older FITS header. The newer FITS header has PC only.
 *
 * This method is checking the pc_ij and r_j array.  If any of the values are undefined,
 * the default are assigned.  The default values are defined as following based on the reference
 * paper above:
 *
 * PC_ij: an array defined in the FITS header. Fhe size of the dimension is N.  N is defined by
 *  WCSAXES or naxis
 *   i: the wcsaxes's value, if the wcsaxes = 2, then i=2
 *   j: 0, ...N
 *   If any of the PC_ij is not defined in the header, the following default is assigned:
 *   1.  PC i_j =    1.0 when i = j
 *   2.  PC i_j = 0.0 when i!=j
 *
 *  If instead of using PC, CD is used, the following default is assigned:
 *  CD i_j    0.0  NOTE: CD i_j's default is different from PC i_j
 *
 *
 *  r_j: An array of the CRPIX values, the size of the dimension is N.  N is defined by
 *  WCSAXES or naxis
 *  j: 0,...N
 *
 *  Default values:
 *   if r_j  = value of CRPIXj, if it is not defined, the default value is 0.0
 *
 * @param inArr
 * @param keyRoot
 * @param which
 * @param N
 * @returns {Array}
 */
function applyDefaultValues(inArr, keyRoot, which, N) {
    const retAry = [];
    for (let i = 0; i < N; i++) {
        if (isDefined(inArr?.[i]) && !isNaN(inArr[i])) {
            //if inArr is defined and it has a valid value
            retAry[i] = inArr[i];
            continue;
        }
        switch (keyRoot) { //either inArr is undefined, or inArr[i] is undefined
            case 'PC':
                // PCi_j – [floating point; defaults: 1.0 when i = j, 0.0 otherwise].
                // Linear transformation matrix between Pixel Axes j
                // and Intermediate-coordinate Axes i.
                retAry[i] = (i + 1) === parseInt(which) ? 1 : 0;
                break;
            case 'CRPIX':
            case 'CD':
                // CRPIXj – [floating point; default: 0.0]
                // CDi_j – [floating point; defaults: 0.0]
                retAry[i] = 0.0;
                break;
        }
    }
    return retAry;
}


/**
 * Get the key headers for a coordinate.
 * @param parse
 * @param {string} altWcs one character alternative WCS version, '' for primary
 * @param {number} which index of the world coordinate
 * @param {string} defaultUnits default units
 * @returns {*}
 */
function getCoreSpectralHeaders(parse, altWcs, which, defaultUnits = ' ') {

    // Based on https://fits.gsfc.nasa.gov/standard40/fits_standard40aa-le.pdf
    // The default values defined below:
    // CTYPEi	' ' (i.e. a linear undefined axis)
    // CUNITi	' ' (i.e. undefined)
    // CRPIXj   0.0
    // CRVALi   0.0
    // CDELTi	1.0

    const ctype = parse.getValue(`CTYPE${which}${altWcs}`, ' ');
    const cname = parse.getValue(`CNAME${which}${altWcs}`, ' '); // spectral coordinate description
    const units = parse.getValue(`CUNIT${which}${altWcs}`, defaultUnits);
    const crpix = parse.getDoubleValue(`CRPIX${which}${altWcs}`, 0.0);
    const crval = parse.getDoubleValue(`CRVAL${which}${altWcs}`, 0.0);
    const cdelt = parse.getDoubleValue(`CDELT${which}${altWcs}`, 1.0);
    return {ctype, cname, units, crpix, crval, cdelt};
}


/**
 * Get algorithm and type of the spectral coordinate from the CTYPEka value.
 *
 * CTYPEka has the form XXXX-AAA, where the first four characters specify the coordinate type,
 * the fifth character is ’-’ and the next three characters specify a predefined algorithm
 * for computing the world coordinates from intermediate physical coordinates.
 * (If only the type is present, the algorithm is assumed to be linear.)
 *
 * When k is the spectral axis, the first four characters shall be one of:
 * FREQ, ENER, WAVN, VRAD, WAVE, VOPT, ZOPT, AWAV, VELO, BETA
 * per Table 1 of E. W. Greisen et al.: Representations of spectral coordinates in FITS
 * @param {String} ctype
 * @return {{algorithm:string,coordType:string}}
 */
function getAlgorithmAndType(ctype) {
    let algorithm;
    if (!ctype.trim()) return {algorithm: undefined, coordType: undefined};
    ctype = ctype.toUpperCase();

    const sArray = ctype.split('-');

    const coordType = sArray[0];

    if (sArray.length === 1 ) {
        algorithm = LINEAR;
    } else {
        algorithm = sArray[1].trim();
    }

    // verify that coordinate type is valid
    const validCoordTypes = Object.keys(spectralCoordTypes);
    if (!validCoordTypes.includes(coordType)) {
        // handle non-conforming coordinates: check if coordinate type matches one of the aliases
        const matchingType = validCoordTypes.find((c) => spectralCoordTypes[c].aliases?.includes(coordType));
        return {algorithm, coordType: matchingType};
    }
    // the standard does not limit the algorithm to the known algorithms
    return {algorithm, coordType};
}

