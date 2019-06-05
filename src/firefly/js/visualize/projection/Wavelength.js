




export const PLANE  = 'PLANE';
export const LINEAR = 'LINEAR';
export const LOG = 'LOG';
export const F2W = 'F2W';
export const V2W = 'V2W';
export const TAB = 'TAB';


export const WAVE= 'WAVE';
export const AWAV= 'AWAV';


/**
 * @global
 * @public
 * @typedef {Object} WaveLengthData
 *
 * @prop {string} algorithm
 * @prop {string} wlType
 * @prop {Number} N
 * @prop {Array.<number>} r_j
 * @prop {Array.<number>} pc_3j
 * @prop {Number} s_3
 * @prop {Number} lambda_r
 * @prop {number} restWAV
 * @prop {number} crpix
 * @prop {number} cdelt
 * @prop {number} crval
 */



const wlTypes= {
    [PLANE] : {
        getWaveLength : getWaveLengthPlane,
        implemented : true,
    },
    [LINEAR] : {
        getWaveLength : getWaveLengthLinear,
        implemented : true,
    },
    [LOG] : {
        getWaveLength : getWaveLengthLog,
        implemented : true,
    },
    [F2W] : {
        getWaveLength : getWaveLengthNonLinear,
        implemented : true,
    },
    [V2W] : {
        getWaveLength : getWaveLengthNonLinear,
        implemented : true,
    },
    [TAB] : {
        getWaveLength : getWaveLengthTable,
        implemented : true,
    }
};


export function getWavelength(pt, cubeIdx, wlData) {
    const {algorithm, wlType}= wlData;
    if (wlTypes[algorithm] && wlTypes[algorithm].implemented && wlTypes[algorithm].getWaveLength) {
        const wl=  wlTypes[algorithm].getWaveLength(pt,cubeIdx,wlData);
        if (wlType===WAVE) return wl;
        if (wlType===AWAV) return convertAirToVacuum(wl);
        return wl;
    }
}

function getWaveLengthPlane(ipt, cubeIdx, wlData) {
    const {crpix, crval, cdelt} = wlData;
    const wl = crval + ( cubeIdx - crpix ) * cdelt;
    return wl;
}

export function isWLAlgorithmImplemented(wlData) {
    const {algorithm}= wlData;
    return wlTypes[algorithm].implemented;
}

/**
 *  calculate the air wavelength and then convert to vacuum wavelength
 *  @param {number} wl
 *  @return {number}
 */
const convertAirToVacuum= (wl) => 1 + 10**-6 * ( 287.6155 + 1.62887/wl**2 + 0.01360/wl**4);


/**
 *
 * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
 * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
 * Thus, the k value referenced in the paper is 3 here
 *
 *  omega  = x_3 = s_3* Sum [ m3_j * (p_j - r_j) ]
 *
 *  lamda = lambda_r + omega
 *
 *  lambda_r : is the reference value,  given by CRVAL3
 *
 *
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {Object} wlData
 * @param {Number} wlData.N
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.s_3
 * @param wlData.lambda_r
 * @return {number}
 */
function getWaveLengthLinear(ipt, cubeIdx, wlData) {
    const {N,  r_j, pc_3j, s_3,  lambda_r}= wlData;
    return lambda_r + getOmega(getPixelCoords(ipt,cubeIdx),  N, r_j, pc_3j, s_3);
}

/**
 *
 * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
 * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
 * Thus, the k value referenced in the paper is 3 here
 *
 *  omega  = x_3 = s_3* Sum [ m3_j * (p_j - r_j) ]
 *
 *  lamda = lambda_r * exp (omega/s_3)
 *
 *  lambda_r : is the reference value,  given by CRVAL3
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {WaveLengthData} wlData
 * @param {Number} wlData.N
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.s_3
 * @param {Number} wlData.lambda_r
 * @return {number}
 */
function getWaveLengthLog(ipt, cubeIdx, wlData) {
    const {N,  r_j, pc_3j, s_3,  lambda_r}= wlData;
    const omega= getOmega(getPixelCoords(ipt,cubeIdx),  N, r_j, pc_3j, s_3);
    return lambda_r* Math.exp(omega/lambda_r);
}

/**
 *
 * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
 * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
 * Thus, the k value referenced in the paper is 3 here
 *
 *  omega  = x_3 = s_3* Sum [ m3_j * (p_j - r_j) ]
 *
 *  lamda = lambda_r * exp (omega/s_3)
 *
 *  lambda_r : is the reference value,  given by CRVAL3
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {WaveLengthData} wlData
 * @param {Number} wlData.N
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.s_3
 * @param {Number} wlData.lambda_r
 * @param {string} wlData.algorithm
 * @param {number} wlData.restWAV
 * @return {number}
 */
function getWaveLengthNonLinear(ipt, cubeIdx, wlData) {

    const {N,  r_j, pc_3j, s_3,  lambda_r, algorithm= 'F2W', restWAV=0}= wlData;
    let lamda= NaN;
    const omega= getOmega(getPixelCoords(ipt,cubeIdx),  N, r_j, pc_3j, s_3);


    switch (algorithm){
        case 'F2W':
            lamda =lambda_r *  lambda_r/(lambda_r - omega);
            break;
        case 'V2W':
            const lamda_0 = restWAV;
            const b_lamda = ( Math.pow(lambda_r, 4) - Math.pow(lamda_0, 4) + 4 *Math.pow(lamda_0, 2)*lambda_r*omega )/
            Math.pow((Math.pow(lamda_0, 2) + Math.pow(lambda_r, 2) ), 2);
            lamda = lamda_0 - Math.sqrt( (1 + b_lamda)/(1-b_lamda) );
            break;
    }
    return lamda;
}

const getArrayDataFromTable= (table, rowIdx, columnName) => undefined;  // todo implement


/**
 *
 * The pre-requirement is that the FITS has three axies, ra (1), dec(2) and wavelength (3).
 * Therefore the keyword for i referenced in the paper is 3
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {WaveLengthData} wlData
 * @param {Number} wlData.N
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.s_3
 * @param {Number} wlData.lambda_r
 * @return {number}
 */
function getWaveLengthTable(ipt, cubeIdx, wlData) {


    const {N, r_j, pc_3j, s_3, cdelt, lambda_r, wlTable, ps3_1, ps3_2}= wlData;

    const omega= getOmega(getPixelCoords(ipt,cubeIdx),  N, r_j, pc_3j, s_3);
    const psi_m = !isNaN(cdelt)? lambda_r + cdelt* omega : lambda_r + omega;
    if (!wlTable) return NaN;


    //read the cell coordinate from the FITS table and save to two one dimensional arrays
    const coordData = getArrayDataFromTable(wlTable,0, ps3_1 || 'COORDS');
    if (!coordData) return NaN;

    const indexData =  getArrayDataFromTable(wlTable,0, ps3_2 || 'INDEX');
    if (!indexData) return coordData[psi_m];

    const psiIndex = searchIndex(indexData, psi_m);
    if (isNaN(psiIndex) || psiIndex===-1) return NaN; // No index found in the index array, gamma is undefined, so is coordinate

    const gamma_m = calculateGamma_m(indexData, psi_m, psiIndex);
    if (isNaN(gamma_m)) return NaN;

    return  coordData[psiIndex] + (gamma_m - psiIndex) * (coordData[psiIndex+1] - coordData[psiIndex]);
}


/**
 *
 * @param {Array.<number>} indexVec
 * @param {number} psi
 * @param {number} idx
 * @return {*}
 */
function calculateGamma_m(indexVec, psi, idx) {
    /* Scan the indexing vector, (Ψ1, Ψ2,...), sequen-tially starting from the ﬁrst element, Ψ1,
     * until a successive pair of index values is found that encompass ψm (i.e. such that Ψk ≤ ψm ≤ Ψk+1
     * for monotonically increasing index values or Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
     * for some k). Then, when Ψk  Ψk+1, interpolate linearly on the indices
     */


    if (idx!==-1 && indexVec[idx-1]!==indexVec[idx]){
        // Υm = k + (ψm − Ψk) / (Ψk+1− Ψk)
        return  idx + (psi-indexVec[idx-1])/(indexVec[idx]-indexVec[idx-1]);
    }
    else {
        return NaN; // No index found in the index array, gamma is undefined, so is coordinate
    }

}





function isSorted(intArray) {

    const end = intArray.length-1;
    let counterAsc = 0;
    let counterDesc = 0;

    for (let i = 0; i < end; i++) {
        if (intArray[i] < intArray[i+1]) counterAsc++;
        else if(intArray[i] > intArray[i+1]) counterDesc++;
    }
    if(counterDesc===0) return 1;
    else if (counterAsc===0) return -1;
    else return 0;
}




function searchIndex(indexVec, psi) {
    /*Scan the indexing vector, (Ψ1, Ψ2,...), sequen-tially starting from the ﬁrst element, Ψ1,
      until a successive pair of index values is found that encompass ψm (i.e. such that Ψk ≤ ψm ≤ Ψk+1
      for monotonically increasing index values or Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
      for some k). Then, when Ψk  Ψk+1, interpolate linearly on the indices
     */

    const sort = isSorted(indexVec); //1:ascending, -1: descending, 0: not sorted
    if (sort===0){
        return NaN; //The vector index array has to be either ascending or descending
    }


    for (let i=1; i<indexVec.length; i++){
        if (sort===1 && indexVec[i-1]<=psi  && psi<=indexVec[i]){
            return i;
        }
        if (sort===-1 && indexVec[i-1]>=psi && psi>=indexVec[i]){
            return i;

        }
    }
    return -1;
}





/**
 *
 * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
 * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
 * Thus, the k value referenced in the paper is 3 here
 *
 *                      N
 *  omega  = x_3 = s_3* ∑ [ m3_j * (p_j - r_j) ]
 *                      j=1
 *
 *  lamda = lambda_r + omega
 *
 *  lambda_r : is the reference value,  given by CRVAL3
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @return {Array.<number>}
 */
function getPixelCoords(ipt, cubeIdx) {
    const p0 = Math.round(ipt.x - 0.5); //x
    const p1 = Math.round(ipt.y - 0.5); //y
    return [p0, p1, cubeIdx];
}

/**
 *
 * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
 * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
 * Thus, the k value referenced in the paper is 3 here
 *                      N
 *  omega  = x_3 = s_3* ∑ [ m3_j * (p_j - r_j) ]
 *                      j=1
 *
 *  N: is the dimensionality of the WCS representation given by NAXIS or WCSAXES
 *  p_j: is the pixel coordinates
 *  r_j: is the pixel coordinate of the reference point given by CRPIXJ where j=1... N, where N=3 in our case
 *  s_3: is a scaling factor given by CDELT3
 *  m3_j: is a linear transformation matrix given either by PC3_j or CD3_j
 *
 *
 * @param pixCoords
 * @param {number} N
 * @param {Array.<number>} r_j
 * @param {Array.<number>} pc_3j
 * @param {Number} s_3
 * @return number
 */
function getOmega(pixCoords, N,  r_j, pc_3j, s_3){
    if (!pc_3j || !r_j ) return s_3*(pixCoords[0]+pixCoords[1]);

    let omega =0.0;
    for (let i=0; i<N; i++){
        omega += s_3 * pc_3j[i] * (pixCoords[i]-r_j[i]);
    }
    return omega;
}

