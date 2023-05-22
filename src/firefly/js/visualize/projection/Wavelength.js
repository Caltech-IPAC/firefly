/**
 *  References:
 *  1. "Representations of spectral coordinates in FITS", by E. W. Greisen1,M.R.Calabretta2, F.G.Valdes3,
 *     and S. L. Allen. A&A Volume 446, Number 2, February I 2006
 *  2. "Representations of world coordinates in FITS" A&A 395, 1061-1075 (2002) DOI: 10.1051/0004-6361:20021326
 *     E. W. Greisen1 - M. R. Calabretta2
 *  3. https://www.aanda.org/articles/aa/full/2002/45/aah3859/aah3859.html,
 *
 */
import {isDefined} from '../../util/WebUtil.js';

export const LINEAR = 'LINEAR';
export const LOG = 'LOG';
export const F2W = 'F2W';
export const V2W = 'V2W';
export const TAB = 'TAB';


export const WAVE = 'WAVE';
export const AWAV = 'AWAV';
export const VRAD = 'VRAD';


/**
 * @global
 * @public
 * @typedef {Object} LookupTableData
 * @summary The information needed to calculate spectral coordinate using lookup table
 *

 * @prop {Array} coordData    coordinate data
 * @prop {Array} indexData    indexData
 * @prop {number} m           axis number (m) in array PSi_1a
 * @prop {number} [ps3_1]     column name for the coordinate array (PSi_1a)
 * @prop {Table} [table]      lookup table
 */

/**
 * @global
 * @public
 * @typedef {Object} SpectralCoord
 * @summary The information needed to calculate and present (public interface) spectral coordinate value
 *
 * @prop {boolean} planeOnly     coordinate can be calculated per plane (public interface)
 * @prop {string} name           spectral coordinate name (public interface)
 * @prop {string} symbol         spectral coordinate symbol (public interface)
 * @prop {string} units          spectral coordinate units (public interface)
 * @prop {string} coordType      spectral coordinate type
 * @prop {string} algorithm      the algorithm used to calculate spectral coordinate value
 * @prop {string} ctype          spectral coordinate type in the form XXXX-YYY, where XXXX is coordinate type, YYY is algorithm
 * @prop {Number} N              (WCS level) the dimensionality of WCS representation given either by NAXIS or WCSAXES
 * @prop {Array.<number>} r_j    (WCS Level) pixel coordinates of the reference point given by CRPIXj
 * @prop {Array.<number>} pc_3j  the linear transformation matrix row corresponding to the spectral coordinate i (given either by PCi_j or CDi_j)
 * @prop {number} crval          spectral coordinate value at reference point
 * @prop {number} cdelt          scaling factor CDELTi or 1.0
 * @prop {number} [restWAV]      (WCS Level) rest wavelength (used for V2W algorithm)
 * @prop {LookupTableData} [tab] lookup table data for TAB algorithm
 */

/**
 * @global
 * @public
 * @typedef {Object} SpectralWCSData
 *
 * @prop {Array.<SpectralCoord>} spectralCoords  parameters specific to each spectral coordinate
 * @prop {string} failReason
 * @prop {boolean} hasPlainOnlyCoordInfo  WCS includes plane level spectral coordinate
 * @prop {boolean} hasPixelLevelCoordInfo WCS includes pixel level spectral coordinate
 * @prop {boolean} isNonSeparableTABGroup all spectral coordinates are TAB that should be processed together
 */


/**
 * Spectral-coordinate type codes.
 */
export const spectralCoordTypes = {
    ['FREQ']: {
        type: 'Frequency',
        symbol: String.fromCharCode(0x03BD), // ν (nu)
        defaultUnits: 'Hz',
    },
    ['ENER']: {
        type: 'Energy',
        symbol: 'E',
        defaultUnits: 'J',
    },
    ['WAVN']: {
        type: 'Wavenumber',
        symbol: 'k',
        defaultUnits: '1/m',
    },
    [VRAD]: {
        type: 'Radio velocity',
        symbol: 'V',
        defaultUnits: 'm/s',
    },
    [WAVE]: {
        aliases: ['WAVELENGTH', 'LAMBDA'],
        type: 'Vacuum wavelength',
        symbol: String.fromCharCode(0x03BB), // λ
        defaultUnits: 'm',
    },
    ['VOPT']: {
        type: 'Optical velocity',
        symbol: 'Z',
        defaultUnits: 'm/s',
    },
    ['ZOPT']: {
        type: 'Redshift',
        symbol: 'z',
        defaultUnits: ' ',
    },
    [AWAV]: {
        type: 'Air wavelength',
        symbol: String.fromCharCode(0x03BB, 0x2090), // λₐ
        defaultUnits: 'm',
    },
    ['VELO']: {
        type: 'Apparent radial velocity',
        symbol: 'v',
        defaultUnits: 'm/s',
    },
    ['BETA']: {
        type: 'Beta factor',
        symbol: '0x03B2', // β
        defaultUnits: ' ',
    }
};


export const algorithmTypes = {
    [LINEAR]: {
        getWaveLength: getWaveLengthLinear,
        implemented: true,
    },
    [LOG]: {
        getWaveLength: getWaveLengthLog,
        implemented: true,
    },
    [F2W]: {
        getWaveLength: getWaveLengthNonLinear,
        implemented: true,
    },
    [V2W]: {
        getWaveLength: getWaveLengthNonLinear,
        implemented: true,
    },
    [TAB]: {
        getWaveLength: getWaveLengthTab,
        implemented: true,
    },
};

/**
 *
 * @param pt
 * @param cubeIdx
 * @param {SpectralWCSData} spectralWCSData
 * @param {Array<SpectralCoord>} spectralWCSData.spectralCoords
 * @returns {*}
 */
export function getWavelength(pt, cubeIdx, spectralWCSData) {

    let coordValArray;
    if (spectralWCSData.isNonSeparableTABGroup) {
        coordValArray = getWaveLengthTabMultiD(pt, cubeIdx, spectralWCSData);
    } else {
        coordValArray = spectralWCSData.spectralCoords.map((c) => {
            const {algorithm, coordType} = c;
            if (algorithmTypes[algorithm]?.implemented && algorithmTypes[algorithm].getWaveLength) {

                let wl = algorithmTypes[algorithm].getWaveLength(pt, cubeIdx, c);
                if (coordType === AWAV) wl = convertAirToVacuum(wl);
                return wl;
            }
        });
    }
    return coordValArray;
}

/**
 *  Function for air to vacuum wavelength conversion (Eq. 66 in paper2.)
 *  @param {number} wl - air wavelength
 *  @return {number}
 */
const convertAirToVacuum = (wl) => 1 + 10 ** -6 * (287.6155 + 1.62887 / wl ** 2 + 0.01360 / wl ** 4);


/**
 * Get linear spectral coordinate value.
 *
 * Assuming spectral coordinate is the third (ex. CTYPE3 = WAVE)
 *
 * spectral_coord_val = crval_3 + omega
 *
 * where  omega  = x_3 = cdelt_3 * Sum [ m3_j * (p_j - r_j) ]
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {Object} wlData
 * @param {Number} wlData.N
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.cdelt
 * @param {Number} wlData.crval
 * @return {number}
 */
function getWaveLengthLinear(ipt, cubeIdx, wlData) {
    const {N, r_j, pc_3j, cdelt, crval} = wlData;

    return crval + getOmega(getPixelCoords(ipt, cubeIdx), N, r_j, pc_3j, cdelt);
}

/**
 * Get logarithmic spectral coordinate value.
 *
 * Assuming spectral coordinate is the third (ex. CTYPE3 = WAVE-LOG)
 *
 * spectral_coord_val = crval_3 * exp (omega/crval_3),  eq. (5)
 *
 * where  omega  = x_3 = cdelt_3 * Sum [ m3_j * (p_j - r_j) ]
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {SpectralCoord} wlData
 * @param {Number} wlData.N
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.cdelt
 * @param {Number} wlData.crval
 * @return {number}
 */
function getWaveLengthLog(ipt, cubeIdx, wlData) {
    const {N, r_j, pc_3j, cdelt, crval} = wlData;
    const omega = getOmega(getPixelCoords(ipt, cubeIdx), N, r_j, pc_3j, cdelt);
    return crval * Math.exp(omega / crval);
}

/**
 * Get non-linear spectral coordinate value.
 *
 * Assuming spectral coordinate is the third,
 *
 * for ex. CTYPE3 = WAVE-F2W:
 *
 * spectral_coord_val = crval_3 * crval_3 / (crval3 - omega),  eq. (52)
 *
 * where   omega = x_3 = cdelt_3 * Sum [ m3_j * (p_j - r_j) ]
 *
 * for ex. CTYPE3 = WAVE-V2W: see eq. (57) and (58)
 *
 * Other non-linear algorithms may be supported from Table 5 can be added in the future.
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {SpectralCoord} wlData
 * @param {Number} wlData.N
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.cdelt
 * @param {Number} wlData.crval
 * @param {string} wlData.algorithm
 * @param {number} wlData.restWAV
 * @return {number}
 */
function getWaveLengthNonLinear(ipt, cubeIdx, wlData) {

    const {N, r_j, pc_3j, cdelt, crval, algorithm='F2W', restWAV=0} = wlData;
    let lambda = NaN;
    const omega = getOmega(getPixelCoords(ipt, cubeIdx), N, r_j, pc_3j, cdelt);

    switch (algorithm) {
        case 'F2W':
            lambda = crval * crval / (crval - omega);
            break;
        case 'V2W':
            const lambda_0 = restWAV;
            const b_lambda = (Math.pow(crval, 4) - Math.pow(lambda_0, 4) + 4 * Math.pow(lambda_0, 2) * crval * omega) /
                Math.pow((Math.pow(lambda_0, 2) + Math.pow(crval, 2)), 2);
            lambda = lambda_0 - Math.sqrt((1 + b_lambda) / (1 - b_lambda));
            break;
    }
    return lambda;
}


/**
 * Get non-linear spectral coordinate using table lookup, see Section 6 of Paper2.
 *
 * The intermediate world coordinate for axis i is calculated as
 *
 * psi_m = x_i + CRVALia, eq. (87)
 *
 * where   x_i = omega = cdelt_i * Sum [ mi_j * (p_j - r_j) ]
 *
 * Using linear interpolation, if necessary, in the indexing vector
 * for intermediate world coordinate axis i, one determines the
 * location, Ym, corresponding to psi_m. Then the coordinate value,
 * Cm, of type specified by the first four characters of CTYPEia,
 * is that at location (m,Y1,Y2,...Y_M) in the coordinate array,
 * again using linear interpolation as needed.
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {SpectralCoord} wlData
 * @param {Number} wlData.N number of world coordinate axes
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.cdelt scaling factor
 * @param {Number} wlData.crval value at the reference point
 * @param {LookupTableData} wlData.tab lookup table data
 * @return {number} spectral coordinate value
 */
function getWaveLengthTab(ipt, cubeIdx, wlData) {

    const {N, r_j, cdelt, crval, pc_3j, tab} = wlData;

    //coordinate array column must be defined
    const coordData = tab?.coordData;
    if (!coordData) return NaN;

    // indexing vector column can be undefined in no index case
    const indexData = tab.indexData;

    const pixCoord = getPixelCoords(ipt, cubeIdx);

    // intermediate pixel coordinates (array of psi_m)
    const psi_ms = crval + getOmega(pixCoord, N, r_j, pc_3j, cdelt);

    const val = tabInterpolate1D(indexData, coordData, psi_ms);

    return Array.isArray(val) ? val[0] : val;
}

/**
 * Get non-separable spectral coordinate values using multi-d table lookup, see Section 6 of Paper2.
 *
 * The intermediate world coordinate for axis i is calculated as
 *
 * psi_m = x_i + CRVALia, eq. (87)
 *
 * where   x_i = omega = cdelt_i * Sum [ mi_j * (p_j - r_j) ]
 *
 * Using linear interpolation, if necessary, in the indexing vector
 * for intermediate world coordinate axis i, one determines the
 * location, Ym, corresponding to psi_m. Then the coordinate value,
 * Cm, of type specified by the first four characters of CTYPEia,
 * is that at location (m,Y1,Y2,...Y_M) in the coordinate array,
 * again using linear interpolation.
 *
 * The concept described above is extended to M non-separable axes
 * so long as the indexing vectors for each of the M axes are separable.
 * M coordinate values are required for each of the possible index positions.
 * Therefore, the coordinates will be in a single (1 + M)-dimensional array.
 * This coordinate array will have dimensions (M, K1, K2,... KM), where Km is the
 * maximum value of the index on axis m + 1 of the coordinate array.
 * For simplicity, degenerate axes are forbidden; therefore, Km > 1.
 * The indexing vectors for each of the M axes each contain a one-dimensional
 * array of length Km.
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {SpectralWCSData} wcsData
 * @param {Array.<SpectralCoord>} wcsData.spectralCoords
 * @param {Number} wcsData.spectralCoords.N number of world coordinate axes, shared for all spectral coords in WCS
 * @param {Array.<number>} wcsData.spectralCoord.r_j array of reference pixels, shared for all spectral coords in WCS
 * @param {Array.<number>} wcsData.spectralCoord.pc_3j linear transformation matrix row
 * @param {Number} wcsData.spectralCoord.cdelt scaling factor
 * @param {Number} wcsData.spectralCoord.crval value at the reference point
 * @param {LookupTableData} wcsData.spectralCoord.tab lookup table data
 * @return {Array<number>|number} spectral coordinate value
 */
function getWaveLengthTabMultiD(ipt, cubeIdx, wcsData) {

    const {spectralCoords} = wcsData;

    const c1 = spectralCoords[0];
    const {N, r_j, tab} = c1;

    //coordinate array column must be defined
    const coordData = tab?.coordData;
    if (!coordData) return spectralCoords.map(() => NaN);

    const pixCoord = getPixelCoords(ipt, cubeIdx);

    // spectral coordinates must be sorted by axis number m (pv3_3 field)
    const sortedSpectralCoords = spectralCoords.sort((a, b) => a.tab.m - b.tab.m);
    const sortingOrder = spectralCoords.map((c) => sortedSpectralCoords.indexOf(c));

    // indexing vector column can be undefined in no index case
    const indexDataArr = sortedSpectralCoords.map((c) => c.tab.indexData);

    // intermediate pixel coordinates (array of psi_m)
    const psi_ms = sortedSpectralCoords.map((c) => c.crval + getOmega(pixCoord, N, r_j, c.pc_3j, c.cdelt));

    // array of coordinate values sorted by axis index m
    const valsSortedByM = tabInterpolateMultiD(indexDataArr, coordData, psi_ms);

    // put values in the order corresponding to the order of the spectral coordinates
    return sortingOrder.map((idx) => valsSortedByM?.[idx] ?? NaN);
}

/**
 * Recursive linear interpolation:
 * start from the last coordinate, replace index array with the two that are necessary to find Y.
 * @param indexDataArr indexing vectors
 * @param coordData multi-d coordinate data array
 * @param psi_ms intermediate pixel coordinates
 * @param nprocessed the number of processed coordinates
 * @returns {number|*}
 */
export function tabInterpolateMultiD(indexDataArr, coordData, psi_ms, nprocessed = 0) {
    // the number of spectral coordinates should match the dimension of indexDataArr or psi_ms

    // index of currently processing coord, corresponds to coordinate axis numbers m
    const m = psi_ms.length - 1 - nprocessed;

    if (nprocessed < psi_ms.length) {
        // m > 0
        const psiIndexRange = searchIndexRange(indexDataArr[m], psi_ms[m]); // bounding indexes
        if (!psiIndexRange) return NaN;

        const [psiIndex1, psiIndex2] = psiIndexRange;
        // spectral coordinate values for bounding points
        const coordData1 = [psiIndex1, psiIndex2].map(
            (idx) => tabInterpolateMultiD(indexDataArr.slice(0,-1), coordData[idx], psi_ms.slice(0,-1), nprocessed+1)
        );
        const indexData = psiIndexRange.map((idx) => indexDataArr[m][idx]);
        return tabInterpolate1D(indexData, coordData1, psi_ms[m]);
    } else {
        // m === 0
        return tabInterpolate1D(indexDataArr[0], coordData, psi_ms[0]);
    }
}


/**
 * Get spectral coordinate value using linear interpolation in one dimension case.
 * @param indexData
 * @param coordData
 * @param psi_m  intermediate pixel coordinate along axis m
 * @returns {number|*}
 */
function tabInterpolate1D(indexData, coordData, psi_m) {
    if (!indexData) {
        // No indexing vector, psi_m is a direct index into the coordinate array.
        // psi_m should be an integer between 1 and coordData.length
        if (Math.round(psi_m) === psi_m && psi_m >= 1 && psi_m <= coordData.length) {
            return coordData[psi_m - 1];
        }
    } else {
        // Indexing vector is present.
        const Y_m = calculateY_m(indexData, psi_m);

        if (isDefined(Y_m)) {
            return calculateC_m(Y_m, coordData) ?? NaN;
        }
    }
    return NaN;
}

/**
 * Get coordinate value, when indexing vector is present.
 * In the case of a single separable coordinate with 1 ≤ k ≤ Υm < k+1 ≤K,
 * the coordinate value is given by
 *     Cm = Ck + (Υm − k) (Ck+1 − Ck)
 * For Υm such that 0.5 ≤ Υm < 1 or K < Υm ≤ K + 0.5 linear
 * extrapolation is permitted, with Cm = C1 when K = 1.
 *
 * @param Y_m index in coordinate data array with range from 0.5 to kMax+0.5
 * @param coordData coordinate data array
 * @returns {*}
 */
export function calculateC_m(Y_m, coordData) {

    const kMax = coordData.length;

    // The value of Y_m derived from ψm must lie in the range 0.5 ≤ Y_m ≤ K + 0.5;
    // K > 1: degenerate axes are forbidden
    if (kMax === 1) {
        return NaN;
    }

    let C_m = undefined;

    // linear interpolation: find the value at index i if [v1, v2] have indexes [0, 1]
    const lerp = (v1, v2, i) => v1 + i * (v2 - v1);

    // function to get C_m; the range of i is from -0.5 to 1.5
    const findC_m = (i1, i2, i) => {
        if (Array.isArray(coordData[i1])) {
            // If there are multiple non-separable coordinates, the coordinate data
            // must be represented as a multi-dimensional array.
            // The first dimension of the array represents the number of non-separable
            // coordinates, while the remaining dimensions represent the coordinate values
            // along each dimension.
            return coordData[i1].map((_,ic) => lerp(coordData[i1][ic], coordData[i2][ic], i));
        } else {
            // If there is only one non-separable coordinate, the coordinate data
            // can be represented as a one-dimensional array.
            // In this case, each element of the array would correspond to a single
            // coordinate value along the non-separable dimension.
            return lerp(coordData[i1], coordData[i2], i);
        }
    };

    if (Y_m >= 1 && Y_m < kMax) {
        // In the case of a single separable coordinate with 1 ≤ k ≤ Y_m < k+1 ≤ kMax, the coordinate value is given by
        // Cm = Ck + (Y_m − k) * (Ck+1 − Ck)  eq. (89) of Paper 2
        const kIndex = Math.trunc(Y_m);
        C_m = findC_m(kIndex - 1, kIndex, Y_m - kIndex);
    } else if (Y_m === kMax) {
        C_m = coordData[kMax - 1];
    } else {
        if (Y_m >= 0.5 && Y_m < 1) {
            //linear extrapolation coordData next to the first coordinate point
            C_m = findC_m(0, 1, Y_m - 1);
        } else if (kMax < Y_m && Y_m <= kMax + 0.5) {
            //linear extrapolation coordData
            C_m = findC_m(kMax-2, kMax-1, Y_m - (kMax-1));
        }
    }
    return C_m;
}

/**
 * Get Y_m – the index of ψm in the indexing vector.
 *
 * The algorithm for computing Υm, and thence Cm, is as follows:
 * Scan the indexing vector, (Ψ1,Ψ2, . . .), sequentially
 * starting from the first element, Ψ1, until a successive
 * pair of index values is found that encompass ψm (i.e. such that
 * Ψk ≤ ψm ≤ Ψk+1 for monotonically increasing index values or
 * Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
 * for some k). Then, when Ψk != Ψk+1, interpolate linearly on the
 * indices  Υm = k +  (ψm − Ψk)/ (Ψk+1 − Ψk).  The Ym is the index in
 * the coordinate array. If Ψk+1 == Ψk, the Υm is undefined.
 *
 *  NOTE, the array index starting from 1, not 0 as in javascript
 *
 *  Υm = k + (psi_m - psi_k) / (psi_k+1 - psi_k)   eq. (88) of Paper2
 *
 *  If Ψk+1 == Ψk, the Υm is undefined.
 *
 *  Extrapolation can be used
 *    if psi_m outside range (psi_1, psi_k) and k>1 and
 *
 *    (psi_1 - (psi_2-psi_1)/2 ) < = psi_m < psi_1 or
 *    psi_k < psi_m <= (psi_k + (psi_k - psi_k-1) / 2)
 *    monotonically increasing index values
 *
 *    (psi_1 - (psi_2-psi_1)/2 ) > = psi_m > psi_1 or
 *    psi_k > psi_m >= (psi_k + (psi_k - psi_k-1) / 2)
 *    for monotonically decreasing index values
 *
 * @param {Array.<number>} indexVec
 * @param {number} psi_m  ψm in eq (87)
 * @returns {*} the index of psi_m in the indexing vector in range [0.5, indexVec.length]
 */
export function calculateY_m(indexVec, psi_m) {

    let Y_m = undefined;

    // inverse linear interpolation: find the index of v if [v1, v2] have indexes [0, 1]
    const invlerp = (v1, v2, v) => (v - v1) / (v2 - v1);

    // function to get Y: 1-based index in indexVec; Y range is from 0.5 to indexVec.length+0.5
    const findY = (i1, i2, v) => i2 + invlerp(indexVec[i1], indexVec[i2], v);

    // get zero-based indexes k, k+1 for linear interpolation or extrapolation
    // for interpolation, k and k+1 are such that Ψk ≤ ψm ≤ Ψk+1 or Ψk ≥ ψm ≥ Ψk+1
    // for extrapolation they are the first or the last indexes of the array
    // if extrapolation is not allowed or index vector is not sorted, undefined is returned
    const psiIndexRange = searchIndexRange(indexVec, psi_m);
    if (!psiIndexRange) return undefined;

    const [psiIndex1, psiIndex2] = psiIndexRange;

    if (psiIndex1 === psiIndex2) {
        Y_m = psiIndex1;
    } else {
        Y_m = findY(psiIndex1, psiIndex2, psi_m);
    }
    return Y_m;
}

/**
 * Check if array is monotonically increasing or decreasing.
 * @param intArray
 * @returns {number}  1 for increasing, -1 for decreasing, 0 for non-monotonic
 */
function isSorted(intArray) {

    const end = intArray.length - 1;
    let counterAsc = 0;
    let counterDesc = 0;

    for (let i = 0; i < end; i++) {
        if (intArray[i] < intArray[i + 1]) counterAsc++;
        else if (intArray[i] > intArray[i + 1]) counterDesc++;
    }
    if (counterDesc === 0) return 1;
    else if (counterAsc === 0) return -1;
    else return 0;
}

/**
 * Get lower and upper array indexes k and k+1 for linear interpolation in ****-TAB algorithm.
 * See eq. (88) in Paper2 and the text for extrapolation cases.
 *
 * Scan the indexing vector, (Ψ1, Ψ2,...), sequentially starting from the first element, Ψ1,
 * until a successive pair of index values is found that encompass ψm (i.e. such that Ψk ≤ ψm ≤ Ψk+1
 * for monotonically increasing index values or Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
 * for some k). Return the value of [k, k+1].
 *
 * If ψm is outside indexing vector, check if extrapolation is allowed; if so, return the indices [k, k+1]
 * to be used for extrapolation.
 *
 * Linear extrapolation is allowed for values of ψm such that
 * Ψ1 − (Ψ2 − Ψ1)/2 ≤ ψm < Ψ1 or Ψk < ψm ≤ Ψk + (Ψk − Ψk-1)/2
 *     for monotonic increasing index values,
 * and for Ψ1 + (Ψ1 − Ψ2)/2 ≥ ψm > Ψ1 or Ψk > ψm ≥ Ψk − (Ψk − Ψk-1)/2
 *     for monotonic decreasing index values.
 * Extrapolation is also allowed for k = 1 with ψm in the range Ψ1 − 0.5 ≤ ψm ≤ Ψ1 + 0.5
 *
 * The data in the indexing vectors must be monotonically increasing or decreasing, although two adjacent
 * index values in the vector may have the same value.
 *
 * However, it is not valid for an index value to appear more than twice in an index vector,
 * nor for an index value to be repeated at the start or at the end of an index vector.
 *
 * NOTE: the index in the algorthm is starting with 1, in Javascript, the index is starting from 0.
 *
 * @param indexVec
 * @param psi_m
 * @returns {Array.<number>} zero-based k and k+1 such that Ψk ≤ ψm ≤ Ψk+1 or Ψk ≥ ψm ≥ Ψk+1; [0, 1] or [k_max-1, k_max] if such index does not exist.
 */
export function searchIndexRange(indexVec, psi_m) {

    // determine the sort order of the indexing vector
    const sort = isSorted(indexVec); // 1: ascending, -1: descending, 0: not sorted
    // indexing vector should be monotonically increasing or decreasing
    if (sort === 0) return undefined;

    // get zero-based index k of index vector that Ψk ≤ ψm ≤ Ψk+1 or Ψk ≥ ψm ≥ Ψk+1
    for (let i = 0; i < indexVec.length - 1; i++) {
        // monotonically increasing array data
        if (sort === 1 && indexVec[i] <= psi_m && psi_m <= indexVec[i + 1]) {
            return [i, i+1];
        }
        // monotonically decreasing array data
        if (sort === -1 && indexVec[i] >= psi_m && psi_m >= indexVec[i + 1]) {
            return [i, i+1];
        }
    }

    // extrapolation cases

    // Linear extrapolation is allowed for values of ψm such that
    // Ψ0 − (Ψ1 − Ψ0)/2 ≤ ψm < Ψ0 or Ψk-1 < ψm ≤ Ψk-1 + (Ψk-1 − Ψk-2)/2 for monotonic increasing index values,
    // and for Ψ0 + (Ψ0 − Ψ1)/2 ≥ ψm > Ψ0 or Ψk-1 > ψm ≥ Ψk-1 − (Ψk-1 − Ψk-2)/2 for monotonic decreasing index values.

    if (indexVec.length === 1) {
        // extrapolation is allowed with ψm in the range Ψ0 − 0.5 ≤ ψm ≤ Ψ0 + 0.5
        if (indexVec[0] - 0.5 <= psi_m && psi_m <= indexVec[0] + 0.5) {
            return [0,0];
        }
    } else {
        const kMax = indexVec.length;
        const left = indexVec[0] - (indexVec[1] - indexVec[0]) / 2;
        const right = indexVec[kMax - 1] + (indexVec[kMax - 1] - indexVec[kMax - 2]) / 2;

        switch (sort) {
            case 1:
                if (left <= psi_m && psi_m < indexVec[0]) {
                    return [0, 1];
                } else if (indexVec[kMax - 1] < psi_m && psi_m <= right) {
                    return [kMax - 2, kMax - 1];
                }
                break;
            case -1:
                if (left >= psi_m && psi_m > indexVec[0]) {
                    return [0, 1];
                } else if (indexVec[kMax - 1] > psi_m && psi_m >= right) {
                    return[kMax - 2, kMax - 1];
                }
                break;
        }
    }

    // no valid range found and extrapolation is not allowed
    return undefined;
}


/**
 *
 * Get pixel at given "ImagePt" coordinates
 *   "ImagePt" coordinates have 0,0 lower left corner of lower left pixel
 *   (from the reference paper above) Note that integer pixel numbers refer to the center of the pixel in each axis,
 *   so that, for example, the first pixel runs from pixel number 0.5 to pixel number 1.5 on every axis.
 *   Note also that the reference point location need not be integer nor need it even occur within the image.
 *   The original FITS paper (Wells et al. 1981) defined the pixel numbers to be counted from 1 to NAXIS j ($ \geq $1)
 *   on each axis in a Fortran-like order as presented in the FITS image[*].
 *
 *   This method get the coordinates for given image point (p1, p2, p3) where imagePt=(p1, p2)
 *   If it is only one plan, the p3=1 since the axis is count staring from 1.
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @return {Array.<number>}  center of pixel coordinates
 */
function getPixelCoords(ipt, cubeIdx) {

    //As noted above, the pixel is counting from 1 to naxis j where (j=1, 2,... naxis).  Since the p = Math.round(ipt.x - 0.5)
    //starts from 0.  Thus, 1 is added here.
    //pixel numbers refer to the center of the pixel, so we subtract 0.5, see notes above
    const p0 = ipt ? Math.round(ipt.x - 0.5) + 1 : 0;
    const p1 = ipt ? Math.round(ipt.y - 0.5) + 1 : 0;
    return [p0, p1, cubeIdx + 1]; //since cubeIdx starts from 0
}

/**
 * Get intermediate world coordinate. (See Fig.1 in WCS Paper 1.)
 * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
 * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
 * Thus, the k value referenced in the paper is 3 here
 *                       N
 *  omega  = x_3 = s_3 * ∑ [ PC3_j * (p_j - r_j) ]
 *                      j=1
 *
 *  N: is the dimensionality of the WCS representation given by NAXIS or WCSAXES
 *  p_j: is the pixel coordinates
 *  r_j: is the pixel coordinate of the reference point given by CRPIXJ where j=1... N, where N=3 in our case
 *  s_3: is a scaling factor given by CDELT3
 *  m3_j: is a linear transformation matrix given either by PC3_j or CD3_j
 *
 * @param pixCoords: pixel coordinates, normally this will be an array [imagePt.x,imagePt.y,plane]
 * @param {number} N
 * @param {Array.<number>} r_j: r j are pixel coordinates of the reference point given by CRPIX j
 * @param {Array.<number>} pc_3j: PC_3j matrix, if length=3 array and first two entries are 0,
 * then probably all pixels on the plane have the same value
 * @param {Number} s_3: CDELT3 value
 * @return number
 */

function getOmega(pixCoords, N, r_j, pc_3j, s_3) {
    let omega = 0.0;
    for (let i = 0; i < N; i++) {
        omega += s_3 * pc_3j[i] * (pixCoords[i] - r_j[i]);
    }
    return omega;
}

