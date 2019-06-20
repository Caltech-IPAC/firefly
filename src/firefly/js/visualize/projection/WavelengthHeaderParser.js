/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {makeDoubleHeaderParse} from '../FitsHeaderUtil.js';
import {AWAV, F2W, LINEAR, LOG, PLANE, TAB, V2W, WAVE} from './Wavelength.js';

export function parseWavelengthHeaderInfo(header, altWcs='', zeroHeader, wlTable) {
    const parse= makeDoubleHeaderParse(header, zeroHeader, altWcs);
    const which= altWcs?'1':getWCSAXES(parse);
    const mijMatrixKeyRoot = getPC_ijKey(parse,which);
    if (!mijMatrixKeyRoot) return; //When both PC i_j and CD i_j are present, we don't show the wavelength
    return calculateWavelengthParams(parse,altWcs,which,mijMatrixKeyRoot, wlTable);
}


const isWaveParseDependent = (algorithm) => algorithm===LINEAR || algorithm===LOG  || algorithm===F2W  ||
                                            algorithm===V2W  || algorithm===TAB;

const allGoodValues= (...numbers)  => numbers.every((n) => !isNaN(n));


const L_10= Math.log(10);

/**
 * According to A&A 395, 1061-1075 (2002) DOI: 10.1051/0004-6361:20021326
 * Representations of world coordinates in FITS E. W. Greisen1 - M. R. Calabretta2
 * https://www.aanda.org/articles/aa/full/2002/45/aah3859/aah3859.html
 * the CD_ij is for the older FITs header. The newer FITs header has PC only.
 * 1. If both PC and CD exist, it is wrong. Instead of throwing exception, no wavelength is displayed.
 * 2. If PC is not exist, one or more CD is exist, use CD_ij (j=1..N); if any of CD_ij
 *   is not defined, the default is 0.0;
 * 3. If any PC_ij is defined or neither PC nor CD is defined, we use PC, the default
 *    is 0 for j!=i and 1 if j==i
 *
 *
 * @param parser
 * @param which
 * @returns {*}
 */
function getPC_ijKey(parser,which){

    let hasPC = parser.hasKeyStartWith(`PC${which}_`);
    let hasCD = parser.hasKeyStartWith(`CD${which}_`);

    if (hasPC && hasCD){
        return undefined;
    }
    if (!hasPC && hasCD){
        return 'CD';
    }
    return 'PC';
}


/**
 * This method will return the value of the WCSAXES.  NOTE: the WCSAXES can be any naxis, it does
 * not have to be 3.  In general, if the wavelength is depending on two dimensional images, it most likely
 * is 3.  But the axis 3 can also be other quantity such as Frequency etc.
 *
 * If the FITs header has 'WCSAXES', this will be the wavelength axis.
 * If 'WCSAXES' is not defined, the default will be the larger of naxis and the j where j=1, 2, 3, ..)
 * @param parse
 * @returns {*}
 */
function getWCSAXES(parse){
    const  wcsAxis = parse.getValue('WCSAXES');

    if (wcsAxis) return wcsAxis;

    const nAxis= parse.getIntValue('NAXIS');

    const ctype= parse.getValue(`CTYPE${nAxis+1}`, undefined);

    if(ctype){
        return (nAxis+1).toString();
    }
    else{
        return nAxis.toString();
    }

}

/**
 *
 * Reference:  A&A 395, 1061-1075 (2002) DOI: 10.1051/0004-6361:20021326
 * Representations of world coordinates in FITS E. W. Greisen1 - M. R. Calabretta2
 * paper,  the CD_ij is for the older FITs header. The newer FITs header has PC only.
 *
 * This method is checking the pc_ij and r_j array.  If any of the values are undefined,
 * the default are assigned.  The default values are defined as following based on the reference
 * paper above:
 *
 * PC_ij: an array defined in the FITs header. Fhe size of the dimension is N.  N is defined by
 *  WCSAXES or naxis
 *   i: the wcsaxes's value, if the wcsaxes = 2, then i=2
 *   j: 0, ...N
 *   If any of the PC_ij is not defined in the header, the following default is assigned:
 *   1.  PC i_j = 	1.0 when i = j
 *   2.  PC i_j = 0.0 when i!=j
 *
 *  If instead of using PC, CD is used, the following default is assigned:
 *  CD i_j	0.0  NOTE: CD i_j's default is different from PC i_j
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
function applyDefaultValues(inArr, keyRoot, which, N){
    let retAry=[];
    for (let i=0; i<N; i++) {
        if (inArr && inArr[i] && !isNaN(inArr[i])){
            //if inArr is defined and it has a valid value
            retAry[i]=inArr[i];
            continue;
        }
        switch (keyRoot){ //either inArr is undefined, or inArr[i] is undefined
            case 'PC':
                retAry[i] = (i+1) === parseInt(which)? 1:0;
                break;
            case 'CRPIX' || 'CD':
                retAry[i] = 0.0;
                break;
        };
    }
    return retAry;
}
function isWaveLength(ctype, pc_3j){

    if (ctype.trim()==='') return false;

    const sArray= ctype.split('-');

    //The header has the axis dependency information, ie. pc_31 (naxis1) or pc_32 (naxis2) are defined.
    //If no such information, there is no dependency.  The wavelength will not be displayed in the mouse readout.
    if (sArray[0]==='WAVE'  && ( pc_3j[0]!==0 || pc_3j[1]!==0) ){
        return true;
    }
    return false;

}
/**
 * NOTE:
 *   pc_3j, means the the wavelength axis is 3.  In fact, the wavelength can be in any axis.
 *   Which means which axis has the wavelength
 * @param parse
 * @param altWcs
 * @param which
 * @param pc_3j_key
 * @param wlTable
 * @returns {*}
 */
function calculateWavelengthParams(parse, altWcs, which, pc_3j_key,wlTable) {

    /*
    * Base on the reference: A&A 395, 1061-1075 (2002) DOI: 10.1051/0004-6361:20021326
    * Representations of world coordinates in FITS E. W. Greisen.  The default values
    * defined below:
    * CDELT i	1.0
    * CTYPE i	' ' (i.e. a linear undefined axis)
    * CUNIT i	' ' (i.e. undefined)
    * NOTE: i is the which variable here.
    */
    const ctype= parse.getValue(`CTYPE${which}${altWcs}`, ' ');
    const crpix= parse.getDoubleValue(`CRPIX${which}${altWcs}`, 0.0);
    const crval= parse.getDoubleValue(`CRVAL${which}${altWcs}`, 0.0);
    const cdelt= parse.getDoubleValue(`CDELT${which}${altWcs}`, 1.0);
    const units= parse.getValue(`CUNIT${which}${altWcs}`, '');
    const restWAV= parse.getDoubleValue('RESTWAV'+altWcs, 0);
    const nAxis= parse.getIntValue('NAXIS'+altWcs);
    const N= parse.getIntOneOfValue(['WCSAXES', 'WCSAXIS', 'NAXIS'], -1);
    let pc_3j = parse.getDoubleAry(`${pc_3j_key}${which}_`,altWcs,1,N, undefined);
    let r_j = parse.getDoubleAry('CRPIX',altWcs,1,N, undefined);

    //check if any value is not defined, use default
    pc_3j = applyDefaultValues(pc_3j,pc_3j_key, which, N);
    r_j   = applyDefaultValues(r_j, 'CRPIX', which, N);

    const ps3_0= parse.getValue(`PS${which}_0${altWcs}`, '');
    const ps3_1= parse.getValue(`PS${which}_1${altWcs}`, '');
    const ps3_2= parse.getValue(`PS${which}_2${altWcs}`, '');
    const pv3_0= parse.getValue(`PV${which}_0${altWcs}`, '');
    const pv3_1= parse.getValue(`PV${which}_1${altWcs}`, '');
    const pv3_2= parse.getValue(`PV${which}_2${altWcs}`, '');

    const {algorithm, wlType} = getAlgorithmAndType(ctype,N);


    const canDoPlaneCalc= allGoodValues(crpix,crval,cdelt && nAxis===3 ) ;

    //We only support the standard format in the FITs header.  The standard means the CTYPEka='WAVE-ccc" where
    //ccc can be 'F2W', 'V2W', 'LOG', 'TAB' or empty that means linear. If the header does not have this kind
    // CTYPEka defined, we check if it is a spectra cube.  If it is a spectra cube, we display the wavelength
    // at each plane.
    const isWL = isWaveLength(ctype, pc_3j);

    //If it is a cube plane FITS, plot and display the wavelength at each plane
    if (canDoPlaneCalc && !isWL  && nAxis===3 ) {
        //The FITs has wavelength planes, and each plane has the same wavelength, ie. the third axis
        //is wavelength
        return makeSimplePlaneBased(crpix, crval, cdelt,nAxis, wlType, units, 'use PLANE since is cube and parameters missing');
    }

    //Plot and display the wavelength as one of the mouse readout only if the FITs header
    //contains the required parameters and the wavelength is depending on the image axes.
    /* We don't show the wavelength in the mouse readout in following three situations:
     *  1. Algorithm is not defined
     *  2. wlType is not defined or the type is not supported
     *  3. The FITs file is not wavelength type (may be plane)
     *
     */
    if (!algorithm || !wlType  || !isWL) return;


    if (algorithm===LOG){  //the values in CRPIXk and CDi_j (PC_i_j) are log based on 10 rather than natural log, so a factor is needed.
        r_j && (r_j= r_j.map( (v) => v*L_10));
        pc_3j && (pc_3j= pc_3j.map( (v) => v*L_10));
    }

    let failReason= '';
    let failWarning= '';
    if (N<0) {
        failReason+= 'Dimension value is not available, must be NAXIS or WCSAXES';
    }
    else {
        // if (!pc_3j) failReason+= `, PC3_i${altWcs} or CD3_i${altWcs} is not defined`;
        // if (!r_j) failReason+= `, CRPIXi${altWcs} is not defined`;
        if (!pc_3j) failWarning+= `, PC3_i${altWcs} or CD3_i${altWcs} is not defined`;
        if (!r_j) failWarning+= `, CRPIXi${altWcs} is not defined`;
    }

    if (algorithm===TAB) failWarning+= ', Table array types not implemented';



    return {
        N, algorithm, ctype, restWAV, wlType, crpix, crval, cdelt, failReason, failWarning, units, pc_3j, r_j,
        ps3_0, ps3_1, ps3_2, pv3_0, pv3_1, pv3_2,
        s_3: isNaN(cdelt) ? 0 : cdelt,
        lambda_r: isNaN(crval) ? 0 : crval,
        wlTable
    };
}

function makeSimplePlaneBased(crpix,crval, cdelt, nAxis, wlType, units, reason) {
    if (allGoodValues(crpix,crval,cdelt) && nAxis===3 ) {
        return {algorithm: PLANE, wlType: wlType || WAVE, crpix, crval, cdelt, units, reason};
    }
    else {
        return {algorithm: undefined, wlType, failReason: 'CTYPE3, CRVAL3, CDELT3 are required for simple and NAXIS===3', failReason2: reason};
    }
}

/**
 * This method will return the algorithm specified in the FITS header.
 * If the algorithm is TAB, the header has to contain the keyword "EXTNAME".
 * The fitsType = header.getStringValue("CTYPE3"), will tell what algorithm it is.
 * The value of "CTYPE3" is WAVE-AAA, the AAA means algorithm.  If the fitsType only has
 * "WAVE', it is linear.
 * @param {String} ctype3
 * @return {{algorithm:string,wlType:string}}
 */
function getAlgorithmAndType(ctype3){
    let wlType, algorithm;
    if (!ctype3) return {algorithm:undefined,wlType:undefined};
    ctype3= ctype3.toUpperCase();
    const sArray = ctype3.split('-');

    if (ctype3.startsWith(WAVE) || ctype3.startsWith(AWAV)) {
        if (ctype3.startsWith(WAVE)) wlType= WAVE;
        else if (ctype3.startsWith(AWAV)) wlType= AWAV;

        if (sArray.length===1) algorithm= LINEAR;
        else if (sArray[1].trim()===LOG) algorithm=LOG;
        else algorithm=sArray[1];
    }
    else if (ctype3.startsWith('LAMBDA')) {
        wlType= WAVE;
        algorithm=LINEAR;
    }
    return {algorithm,wlType};
}

