/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {makeDoubleHeaderParse} from '../FitsHeaderUtil.js';
import {AWAV, F2W, LINEAR, LOG, PLANE, TAB, V2W, WAVE} from './Wavelength.js';

export function parseWavelengthHeaderInfo(header, altWcs='', zeroHeader, wlTable) {
    const parse= makeDoubleHeaderParse(header, zeroHeader, altWcs);
    return calculateWavelengthParams(parse,altWcs, wlTable);
}


const isWaveParseDependent = (algorithm) => algorithm===LINEAR || algorithm===LOG  || algorithm===F2W  ||
                                            algorithm===V2W  || algorithm===TAB;

const allGoodValues= (...numbers)  => numbers.every((n) => !isNaN(n));


const L_10= Math.log(10);

function calculateWavelengthParams(parse, altWcs, wlTable) {

    const which= altWcs?'1':'3';

    const ctype= parse.getValue(`CTYPE${which}${altWcs}`);
    const crpix= parse.getDoubleValue(`CRPIX${which}${altWcs}`, NaN);
    const crval= parse.getDoubleValue(`CRVAL${which}${altWcs}`, NaN);
    const cdelt= parse.getDoubleValue(`CDELT${which}${altWcs}`, NaN);
    const units= parse.getValue(`CUNIT${which}${altWcs}`, '');
    const restWAV= parse.getDoubleValue('RESTWAV'+altWcs, 0);
    const nAxis= parse.getIntValue('NAXIS'+altWcs);
    // const N= parse.getIntOneOfValue(['WCSAXES'+altWcs, 'WCSAXIS'+altWcs, 'NAXIS'+altWcs], -1);
    const N= parse.getIntOneOfValue(['WCSAXES', 'WCSAXIS', 'NAXIS'], -1);
    let pc_3j = parse.getDoubleAry('PC3_',altWcs,1,N) || parse.getDoubleAry('CD3_',altWcs,1,N);
    let r_j = parse.getDoubleAry('CRPIX',altWcs,1,N);
    const ps3_0= parse.getValue(`PS${which}_0${altWcs}`, '');
    const ps3_1= parse.getValue(`PS${which}_1${altWcs}`, '');
    const ps3_2= parse.getValue(`PS${which}_2${altWcs}`, '');
    const pv3_0= parse.getValue(`PV${which}_0${altWcs}`, '');
    const pv3_1= parse.getValue(`PV${which}_1${altWcs}`, '');
    const pv3_2= parse.getValue(`PV${which}_2${altWcs}`, '');

    const {algorithm, wlType} = getAlgorithmAndType(ctype,N);

    const canDoPlaneCalc= allGoodValues(crpix,crval,cdelt) && nAxis===3;
    if (canDoPlaneCalc && (!algorithm || !pc_3j || !r_j)) {
        return makeSimplePlaneBased(crpix, crval, cdelt, nAxis, wlType, units, 'use PLANE since is cube and parameters missing');
    }

    if (!algorithm || !wlType) return;


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

