/**
 *  References:
 *  1. "Representations of spectral coordinates in FITS", by E. W. Greisen1,M.R.Calabretta2, F.G.Valdes3,
 *     and S. L. Allen. A&A Volume 446, Number 2, February I 2006
 *  2. "Representations of world coordinates in FITS" A&A 395, 1061-1075 (2002) DOI: 10.1051/0004-6361:20021326
 *     E. W. Greisen1 - M. R. Calabretta2
 *  3. https://www.aanda.org/articles/aa/full/2002/45/aah3859/aah3859.html,
 *
 */
import {LinearInterpolator} from '../../util/Interp/LinearInterpolator.js';
import {isDefined} from '../../util/WebUtil.js';
export const PLANE  = 'PLANE';
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
    },
};

/*const vradTypes= {
    [PLANE] : {
        getVrad : getVradPlane,
        implemented : true,
    },
};*/


export function getWavelength(pt, cubeIdx, wlData) {
    const {algorithm, wlType}= wlData;
    if (wlTypes[algorithm] && wlTypes[algorithm].implemented && wlTypes[algorithm].getWaveLength) {
        const wl=  wlTypes[algorithm].getWaveLength(pt,cubeIdx,wlData);
        //const vrad= wlTypes[algorithm].getVrad(pt,cubeIdx,wlData);
        if (wlType===WAVE || wlType===VRAD) return wl;
        if (wlType===AWAV) return convertAirToVacuum(wl);
        //if (wlType===VRAD) return getWavelengthPlane(wl);
        return wl;
    }
}
//TODO check here to see the cubeIdx??
function getWaveLengthPlane(ipt, cubeIdx, wlData) {
    const {crpix, crval, cdelt} = wlData;
    //pixel count starts from 1 to naxisn
    const wl = crval + ( cubeIdx + 1  - Math.round(crpix) ) * cdelt;
    return wl;
}

export function isWLAlgorithmImplemented(wlData) {
    const {algorithm}= wlData;
    return wlTypes[algorithm].implemented;
}


/*export function getVrad(ipt, cubeIdx, wl) {
    const {algorithm, wlType}= vradData;
    if (wlTypes[algorithm] && wlTypes[algorithm].implemented && wlTypes[algorithm].getVrad) {
        const vrad=  wlTypes[algorithm].getVrad(ipt,cubeIdx,vradData);
        if (wlType===VRAD) return vrad;
        return vrad;
    }
}*/

/*function getVradPlane(ipt, cubeIdx, vradData) {
    const {crpix, crval, cdelt} = vradData;
    //pixel count starts from 1 to naxisn
    const vrad = crval + ( cubeIdx + 1  - Math.round(crpix) ) * cdelt;
    return vrad;
}

export function isVRADAlgorithmImplemented(vradData) {
    const {algorithm}= vradData;
    return vradTypes[algorithm].implemented;
}*/

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

// Add other VRAD conversion and algorithem here

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

    const {N,  r_j, pc_3j, s_3, lambda_r, algorithm= 'F2W', restWAV=0}= wlData;
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

/**
 * According to the reference papers above, the coordinate has M+1 dimension where the M means the
 *  dependence on the M axis, Coords[M][K1][K2[]...[Km].  In our case, the M=1, the coordinate is the
 *  two dimensional array, and the first dimension is M=1  and the second dimension is K1 which is the sampling
 *  number the data has been sampled. Thus, each image point, it corresponding coordinate is
 *  coords[1][K1].  For example, if the sampling count is K1=100, each cell in the coordinate table, contains 100
 *  data values. The coordinate column can convert to 100 rows of the single value table. 
 *
 *
 * @param table
 * @param columnName
 * @returns {*}
 */
const getArrayDataFromTable= (table, columnName) => {
    let tableData = table.tableData;
    let arrayData = tableData.data;
    let columns = tableData.columns;
    for (let i=0; i<columns.length; i++){
        if (columns[i].name.toUpperCase() === columnName.toUpperCase()){
            return arrayData[0][i];

        }
    }
    return undefined;
};

/**
 *
 * The pre-requirement is that the FITS has three axies, ra (1), dec(2) and wavelength (3).
 * Therefore the keyword for i referenced in the paper is 3
 * ψm = xi + CRVALia: xi = omega, CRVALia = CRIVAL3=lambda_r
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {WaveLengthData} wlData
 * @param {Number} wlData.N
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.s_3, s_3 is the value of CDELT3
 * @param {Number} wlData.lambda_r
 * @return {number}
 */
function getWaveLengthTable(ipt, cubeIdx, wlData) {


    const {N, r_j, pc_3j, s_3, lambda_r, wlTable, ps3_1, ps3_2}= wlData;

    const omega= getOmega(getPixelCoords(ipt,cubeIdx),  N, r_j, pc_3j, s_3);
    const psi_m = lambda_r + omega;
    if (!wlTable) return NaN;


    //read the cell coordinate from the FITS table and save to two one dimensional arrays
    const coordData = getArrayDataFromTable(wlTable, ps3_1 || 'COORDS');
    if (!coordData) return NaN;

    const indexData =  getArrayDataFromTable(wlTable, ps3_2 || 'INDEX');

    if (!indexData) {
        //There is no Index table, the C_m is the direct search in the coordinate table
        //In this case the condition 1<=k <=psi_m<k+1<=kMax does not apply in sofia case
        return calculateC_m( psi_m,  coordData, coordData.length, false) ;
    }


    const sort = isSorted(indexData); //1:ascending, -1: descending, 0: not sorted
    if (sort===0){
        return NaN; //The vector index array has to be either ascending or descending
    }
    const gamma_m = calculateGamma_m(indexData, psi_m, sort);

    if (!isDefined(gamma_m)) return NaN;

    return calculateC_m( gamma_m,  coordData, indexData.length) ;

}

/**
 * In the case of a single separable coordinate with 1 ≤ k ≤ Υm < k+1 ≤K,
 * the coordinate value is given by
 *     Cm = Ck + (Υm − k) (Ck+1 − Ck)
 * For Υm such that 0.5 ≤ Υm < 1 or K < Υm ≤ K + 0.5 linear
 * extrapolation is permitted, with Cm = C1 when K = 1.
 *
 * @param gamma_m
 * @param coordData
 * @param kMax
 * @param hasIndexTable
 * @returns {*}
 */
function calculateC_m(gamma_m,  coordData, kMax, hasIndexTable=true){

    let C_m = undefined;

    /*The value of gamma_m derived from ψm must lie in the range 0.5 ≤ gamma_m ≤ K + 0.5.
    However, if the index array is not given, this seems does not apply.  In sofia data, it does not apply.
    Thus, we can not use kIndex = Math.trunc(gamma_m).
    * */

    if (kMax===1) {
        const k =  Math.trunc(gamma_m);//integer part of the gamma_m
        if (k===0) return coordData[0];
    }

    /*In the case of a single separable coordinate with 1 ≤ k ≤ gamma_m < k+1 ≤ kMax, the coordinate value is given by
     Cm = Ck + (gamma_m − k) (Ck+1 − Ck)
     Note: we use the index starting from 0, so we have to kIndex>=0.
    */
    let kIndex;
    if (hasIndexTable){
        /*1 ≤ k ≤ gamma_m < k+1 ≤ Kmax
        gamma_m = k + (ψm − Ψk(/ (Ψk+1 − Ψk); The value of gamma_m derived from ψm must lie in the
        range 0.5 ≤Υm ≤ K + 0.5.
         */
        kIndex = Math.trunc(gamma_m); //thus gamma_m>kIndex and gamma_m<kIndex+1
    }
    else {
        const sort = isSorted(coordData); //1:ascending, -1: descending, 0: not sorted
        if (sort === 0) {
            coordData = coordData.sort((a, b) => { return a - b; }); //sort the data
        }
        kIndex = searchIndex(coordData, gamma_m, sort);
    }
    if (  (hasIndexTable  && kIndex>=0 && kIndex <= gamma_m && gamma_m < kIndex+1 && kIndex+1 <=kMax-1 ) ||
          (!hasIndexTable && kIndex >= 0 && coordData[kIndex] <= gamma_m && gamma_m < coordData[kIndex + 1] && kIndex + 1 <= kMax - 1) ) {
           C_m = coordData[kIndex] + (gamma_m - kIndex - 1) * (coordData[kIndex + 1] - coordData[kIndex]);
    }
    else {
        const inCoords = Array.from(new Array(coordData.length), (x,i) => i);
        const linearInterpolator = LinearInterpolator(inCoords, coordData, true);
        if (!hasIndexTable && gamma_m<coordData[0] || hasIndexTable && gamma_m>=0.5 && gamma_m<1) {
            //linear extrapolation CoorData next to the first coordinate point
            const coordData_far_left = linearInterpolator(0.5);
            return coordData_far_left + gamma_m * (coordData[0]-coordData_far_left);
        }
        else  if( ( !hasIndexTable && gamma_m>coordData[kMax-1]) || (hasIndexTable && kMax <gamma_m && gamma_m <= kMax+ 0.5) ) {
            //linear extrapolation CoorData
            const coordData_far_right = linearInterpolator(kMax + 0.5);
            return coordData[kMax-1] + (gamma_m-kMax) * (coordData_far_right - coordData[kMax-1]);

        }
    }
   return C_m;
}

function getGammaByInterpolation(psiIndex, psi_m, indexVec, sort){

    let gamma_m=undefined;
    /*--------------------------------------------------------------------------------
        * psiIndex is found, however the following four cases, gamma_m is undefined and so is C_m
        */
    //case 1: Ψk+1=Ψk=Ψm, return undefined
    if (indexVec[psiIndex]===indexVec[psiIndex+1] && indexVec[psiIndex]===psi_m && psiIndex<indexVec.length-1 ||
      psi_m===indexVec[0] && indexVec[0]===indexVec[1]) {
        return gamma_m;
    }

    /* case 2: Ψk < ψm = Ψk+1 for monotonically increasing index values,except when ψm = Ψ1,
    * If Ψk+2 = Ψk+1 = ψm (or Ψ2 = Ψ1 = ψm), then Υm and Cm are undefined.
    */
    if (sort===1 && psiIndex!==0 && indexVec[psiIndex] < psi_m && psi_m===indexVec[psiIndex+1] ||

     /* case 2: Ψk > ψm = Ψk+1 for monotonically decreasing index values,except when ψm = Ψ1
      * Ψk > ψm = Ψk+1 for monotonically decreasing index values,except when ψm = Ψ1
      */
     sort===-1 && psiIndex!==0 && indexVec[psiIndex] >psi_m && psi_m===indexVec[psiIndex+1] ){

        /* Since two consecutive index values may be equal, the index following
         * the matched index must be examined. If Ψk+2 = Ψk+1 = ψm
         * (or Ψ2 = Ψ1 = ψm), then Υm and Cm are undefined
         */
        if (indexVec[psiIndex+2]===psi_m){
            return gamma_m;
        }
    }
    //case 4: ψm = Ψ1
    if (indexVec[0]===psi_m && indexVec[1]===psi_m ){
        return gamma_m;
    }

    /*--------------------------------------------------------------------------------
    * psiIndex is found, the gamma_m can be interpolated
    */
    if (indexVec[psiIndex]!==indexVec[psiIndex + 1] ) {
        // Υm = k + (ψm − Ψk) / (Ψk+1− Ψk) where k = 1,... Kmax,
        // array index here is from 0, so the k = psiIndex +1.  For example is psiIndex =0, it means k=1, Ψ1=ψm
        gamma_m =psiIndex+1  + (psi_m - indexVec[psiIndex]) / (indexVec[psiIndex+1] - indexVec[psiIndex]);
        return gamma_m;
    }
}
function getGammaByExtrarpolation(psi_m, indexVec, sort){
    let gamma_m=undefined;
    const kMax = indexVec.length;
    const inCoords = Array.from(new Array(indexVec.length), (x,i) => i);
    const linearInterpolator = LinearInterpolator(inCoords, indexVec, true);
    const left = indexVec[0] - (indexVec[1] - indexVec[0]) / 2;
    const right = indexVec[kMax - 1] + (indexVec[kMax - 1] - indexVec[kMax - 2]) / 2;

    // When no psi_m is found in the vector psiIndex=-1),  we need to check if the gamma_m can be extrapolated.
    if (indexVec.length===1){
        /* When the indexVector's length  = 1 with ψm in the range Ψ1 − 0.5 ≤ ψm ≤ Ψ1 + 0.5 (noting that Ψ1
        * should be equal to 1 in this case) whence Υm = ψm
        */
        if (indexVec[0] - 0.5 <=psi_m && psi_m <=indexVec[0] + 0.5){
            gamma_m= psi_m;
        }
    }
    else { //do extrapolation
        switch (sort) {
            case 1:

                if (left <= psi_m && psi_m < indexVec[0]) {
                    const psi_left = linearInterpolator( left);
                    gamma_m = (psi_m - psi_left) /(indexVec[0] -psi_left);

                }
                else if (indexVec[kMax - 1] < psi_m && psi_m <= right) {
                    const psi_right =linearInterpolator( right );
                    gamma_m = indexVec.length + 1 + (psi_m - indexVec[kMax - 1]) /
                        (psi_right - indexVec[kMax - 1]);
                }

                break;
            case -1:
                if (left >= psi_m && psi_m > indexVec[0]) {
                    const psi_left = linearInterpolator( left);
                    gamma_m = (psi_m - psi_left) /
                        (indexVec[0] - psi_left);

                }
                else if (indexVec[kMax - 1] > psi_m && psi_m >= right)  {
                    const psi_right = linearInterpolator( right);
                    gamma_m = indexVec.length + 1 + (psi_m - indexVec[kMax - 1])/(psi_right - indexVec[kMax - 1]);
                }

                break;
        }
    }

    return gamma_m;

}
/**
 * The algorithm for computing Υm, and thence Cm,is as follows:
 * Scan the indexing vector, (Ψ1,Ψ2, . . .), sequentially
 * starting from the first element, Ψ1, until a successive
 * pair of index values is found that encompass ψm (i.e. such that
 * Ψk ≤ ψm ≤ Ψk+1 for monotonically increasing index values or
 * Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
 * for some k). Then, when Ψk  Ψk+1, interpolate linearly on the
 * indices  Υm = k +  (ψm − Ψk)/ (Ψk+1 − Ψk).  The Ym (gamma_m) is the index in
 * the coordinate array.
 *
 * However if Ψk+1 = Ψk, the Υm is undefined.
 *
 *  NOTE, the array index starting from 1, not 0 as in javascript
 *
 *  Υm = (k + (psi_m-psi_k)/psi_k+1 - psi_k) )
 *  Υm = k +  ( (ψm − Ψk ) /( Ψk+1 − Ψk) )
 *  How to calculate gamma_m (Υm):
 *  case 1:
 *  where psi is the index vector.  In our case, it is indexVec.
 *  If (psi_k <= psi_m <= psi_k+1) for for monotonically increasing index values
 *  or
 *  If (psi_k >= psi_m >= psi_k+1) for for monotonically decreasing index values
 *  Then the gamma_m = (k + (psi_m-psi_k)/psi_k+1 - psi_k) )
 *
 *  case 2:
 *    if psi_k = psi_k+1 = psi_m, based on the formula above, the value is undefined. In this case,
 *    we need to find a psi_k such that psi_k < psi_m = psi_k+1 for monotonically increasing index values
 *    or psi_k > psi_m = psi_k+1 for monotonically decreasing index values. Since two consecutive index
 *    values may be equal, the index following the matched index must be examined. If Ψk+2 = Ψk+1 = ψm
 *    (or Ψ2 = Ψ1 = ψm), then Υm and Cm are undefined.
 *
 * case 3: linear interpolation
 *  If psi_m in the range psi_1 to psi_k inclusive, interpolation is needed.
 *
 * case 4:
 *     if Ψk+1 = Ψk, the Υm is undefined.
 *
 * case 5: extrapolation
 *    psi_m outside range psi_1 - psi_k, and k>1,
 *    if (psi_1 - (psi_2-psi_1)/2 ) < = psi_m <psi_1 or
 *    psi_k <psi_m <= (psi_k + (psi_k - psi_k-1) /2)
 *    monotonically increasing index values
 *
 *    (psi_1 - (psi_2-psi_1)/2 ) > = psi_m > psi_1 or
 *    psi_k > psi_m >= (psi_k + (psi_k - psi_k-1) /2)
 *    for monotonically decreasing index values
 *
 *
 * @param {Array.<number>} indexVec
 * @param {number} psi_m
 * @param sort
 * @returns {*}
 */
function calculateGamma_m(indexVec, psi_m,sort) {

    const psiIndex = searchIndex(indexVec, psi_m,sort);



    if (isNaN(psiIndex)) {//The index vector is not sorted, return undefined
        return undefined;
    }


    if (psiIndex!==-1){
        /*
          Ψk ≤ ψm ≤ Ψk+1 for monotonically increasing index values or
          Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values for some k (here, we use psiIndex)
       */
        return getGammaByInterpolation(psiIndex,psi_m, indexVec, sort);
    }
    else {
        // psiIndex=-1: no range found, the psi_m is outside of the indexVector's range
       return getGammaByExtrarpolation(psi_m, indexVec, sort);
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


 /**
 /* Scan the indexing vector, (Ψ1, Ψ2,...), sequentially starting from the ﬁrst element, Ψ1,
  * until a successive pair of index values is found that encompass ψm (i.e. such that Ψk ≤ ψm ≤ Ψk+1
  * for monotonically increasing index values or Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
  * for some k). Then, when Ψk  Ψk+1, interpolate linearly on the indices
  *
  * NOTE: the index in the algorthim is starting with 1, in Javascript, the index is starting from 0.
  *
  * @param arrayData
  * @param psi_m
  * @param sort
  * @returns {number}
  */
 function searchIndex(arrayData, psi_m, sort) {


    for (let i=0; i<arrayData.length-1; i++){
        if (sort===1 && arrayData[i]<=psi_m  && psi_m<=arrayData[i+1]){
            return i;
        }
        if (sort===-1 && arrayData[i]>=psi_m && psi_m>=arrayData[i+1]){
            return i;

        }
    }

    //no range found, the psi_m is outside of the indexVector's range
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
 * @return {Array.<number>}
 */
function getPixelCoords(ipt, cubeIdx) {

    //As noted above, the pixel is counting from 1 to naxis j where (j=1, 2,... naxis).  Since the p = Math.round(ipt.x - 0.5)
    //starts from 0.  Thus, 1 is added here.
    //pixel numbers refer to the center of the pixel, so we subtract 0.5, see notes above
    const p0 = Math.round(ipt.x - 0.5) + 1 ;
    const p1 = Math.round(ipt.y - 0.5) + 1 ;
    return [p0, p1, cubeIdx+1]; //since cubeIdx starts from 0
}

/**
 *
 * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
 * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
 * Thus, the k value referenced in the paper is 3 here
 *                      N
 *  omega  = x_3 = s_3* ∑ [ PC3_j * (p_j - r_j) ]
 *                      j=1
 *
 *  N: is the dimensionality of the WCS representation given by NAXIS or WCSAXES
 *  p_j: is the pixel coordinates
 *  r_j: is the pixel coordinate of the reference point given by CRPIXJ where j=1... N, where N=3 in our case
 *  s_3: is a scaling factor given by CDELT3
 *  m3_j: is a linear transformation matrix given either by PC3_j or CD3_j
 *
 *
 * @param pixCoords: pixel coordinates
 * @param {number} N
 * @param {Array.<number>} r_j: r j are pixel coordinates of the reference point given by CRPIX j
 * @param {Array.<number>} pc_3j: PC_3j matrix
 * @param {Number} s_3: CDETl3 value
 * @return number
 */

function getOmega(pixCoords, N,  r_j, pc_3j, s_3){
    //if (!pc_3j || !r_j ) return s_3*(pixCoords[0]+pixCoords[1]);

    let omega =0.0;
    for (let i=0; i<N; i++){
        omega += s_3 * pc_3j[i] * (pixCoords[i]-r_j[i]);
    }
    return omega;
}

