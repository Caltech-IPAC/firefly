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
export const PLANE = 'PLANE';
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
 * @prop {number} restWAV
 * @prop {number} crpix
 * @prop {number} cdelt
 * @prop {number} crval
 */



const wlTypes = {
    [PLANE]: {
        getWaveLength: getWaveLengthPlane,
        implemented: true,
    },
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
        getWaveLength: getWaveLengthTable,
        implemented: true,
    },
};


export function getWavelength(pt, cubeIdx, wlData) {
    const {algorithm, wlType} = wlData;
    if (wlTypes[algorithm] && wlTypes[algorithm].implemented && wlTypes[algorithm].getWaveLength) {
        const wl = wlTypes[algorithm].getWaveLength(pt, cubeIdx, wlData);
        if (wlType === WAVE || wlType === VRAD) return wl;
        if (wlType === AWAV) return convertAirToVacuum(wl);
        return wl;
    }
}

//TODO check here to see the cubeIdx??
function getWaveLengthPlane(ipt, cubeIdx, wlData) {
    const {crpix, crval, cdelt} = wlData;
    // pixel count starts from 1 to naxisn
    const wl = crval + (cubeIdx + 1 - Math.round(crpix)) * cdelt;
    return wl;
}

export function isWLAlgorithmImplemented(wlData) {
    return wlTypes[wlData?.algorithm]?.implemented ?? false;
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
 * @param {WaveLengthData} wlData
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
 * Other non-linear algorithms may be supported from Table 5 can be added in future.
 *
 * @param {ImagePt} ipt
 * @param {number} cubeIdx
 * @param {WaveLengthData} wlData
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
    const tableData = table.tableData;
    const arrayData = tableData.data;
    const columns = tableData.columns;
    for (let i = 0; i < columns.length; i++) {
        if (columns[i].name.toUpperCase() === columnName.toUpperCase()) {
            return arrayData[0][i];
        }
    }
    return undefined;
};

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
 * @param {WaveLengthData} wlData
 * @param {Number} wlData.N number of world coordinate axes
 * @param {Array.<number>} wlData.r_j
 * @param {Array.<number>} wlData.pc_3j
 * @param {Number} wlData.cdelt scaling factor
 * @param {Number} wlData.crval value at the reference point
 * @param wlData.wlTable lookup table
 * @param wlData.ps3_1 column name for the coordinate array
 * @param wlData.ps3_2 column name for the indexing vector
 * @return {number} spectral coordinate value
 */
function getWaveLengthTable(ipt, cubeIdx, wlData) {

    const {N, r_j, cdelt, crval, pc_3j, wlTable, ps3_1, ps3_2} = wlData;

    const omega = getOmega(getPixelCoords(ipt, cubeIdx), N, r_j, pc_3j, cdelt);
    const psi_m = crval + omega;
    if (!wlTable) return NaN;

    //read the cell coordinate from the FITS table and save to two one dimensional arrays
    const coordData = getArrayDataFromTable(wlTable, ps3_1 || 'COORDS');
    if (!coordData) return NaN;

    const indexData = getArrayDataFromTable(wlTable, ps3_2 || 'INDEX');

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
            return calculateC_m(Y_m, coordData, indexData.length);
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
 * @param kMax index data length (should match coordData.length, at least in 1d case)
 * @returns {*}
 */
export function calculateC_m(Y_m, coordData, kMax) {

    if (kMax !== coordData.length) {
        // this could happen in 2d case - not yet implemented
        // the index data length is not matching coordinate data length
        // the function will produce wrong results,
        // better not to pretend we know the result
        return NaN;
    }

    // The value of Y_m derived from ψm must lie in the range 0.5 ≤ Y_m ≤ K + 0.5;
    // K > 1: degenerate axes are forbidden
    if (kMax === 1) {
        return NaN;
    }

    let C_m = undefined;

    // linear interpolation: find the value at index i if [v1, v2] have indexes [0, 1]
    const lerp = (v1, v2, i) => v1 + i * (v2 - v1);

    // function to get C_m; the range of i is from -0.5 to 1.5
    const findC_m = (i1, i2, i) => lerp(coordData[i1], coordData[i2], i);

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

    // Indexing vector should be monotonically increasing or decreasing.
    const sort = isSorted(indexVec); // 1: ascending, -1: descending, 0: not sorted
    if (sort === 0) {
        return undefined;
    }

    // get zero-based index k of index vector that Ψk ≤ ψm ≤ Ψk+1 or Ψk ≥ ψm ≥ Ψk+1
    // k is -1 if psi_m is outside the index array
    const psiIndex = searchIndex(indexVec, psi_m, sort);

    if (psiIndex !== -1) {
        // calculate Y_m using interpolation

        if (indexVec[psiIndex] !== indexVec[psiIndex + 1]) {
            // Υm = k + (ψm − Ψk) / (Ψk+1− Ψk) where k = 1,... Kmax,
            // psiIndex here is from 0, so the k = psiIndex+1.  For example when psiIndex=0, it means k=1, Ψ1=ψm
            Y_m = findY(psiIndex, psiIndex + 1, psi_m);
        }

    } else {
        // calculate Y_m using extrapolation: psi_m is out of the indexVector's range

        if (indexVec.length === 1) {
            // When the index vector's length  = 1 with ψm in the range Ψ1 − 0.5 ≤ ψm ≤ Ψ1 + 0.5
            // (noting that Ψ1 should be equal to 1 in this case) hence Υm = ψm
            if (indexVec[0] - 0.5 <= psi_m && psi_m <= indexVec[0] + 0.5) {
                Y_m = psi_m;
            }
        } else { //do extrapolation
            const kMax = indexVec.length;
            const left = indexVec[0] - (indexVec[1] - indexVec[0]) / 2;
            const right = indexVec[kMax - 1] + (indexVec[kMax - 1] - indexVec[kMax - 2]) / 2;

            switch (sort) {
                case 1:
                    if (left <= psi_m && psi_m < indexVec[0]) {
                        Y_m = findY(0, 1, psi_m);
                    } else if (indexVec[kMax - 1] < psi_m && psi_m <= right) {
                        Y_m = findY(kMax - 2, kMax - 1, psi_m);
                    }
                    break;
                case -1:
                    if (left >= psi_m && psi_m > indexVec[0]) {
                        Y_m = findY(0, 1, psi_m);
                    } else if (indexVec[kMax - 1] > psi_m && psi_m >= right) {
                        Y_m = findY(kMax - 2, kMax - 1, psi_m);
                    }
                    break;
            }
        }
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
 * Get lower array index k for linear interpolation in ****-TAB algorithm. See eq. (88) in Paper2.
 *
 * Scan the indexing vector, (Ψ1, Ψ2,...), sequentially starting from the first element, Ψ1,
 * until a successive pair of index values is found that encompass ψm (i.e. such that Ψk ≤ ψm ≤ Ψk+1
 * for monotonically increasing index values or Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
 * for some k). Return the value of k.
 *
 * The data in the indexing vectors must be monotonically increasing or decreasing, although two adjacent
 * index values in the vector may have the same value.
 * However, it is not valid for an index value to appear more than twice in an index vector,
 * nor for an index value to be repeated at the start or at the end of an index vector.
 *
 * NOTE: the index in the algorthm is starting with 1, in Javascript, the index is starting from 0.
 *
 * @param arrayData
 * @param psi_m
 * @param sort 1 if index values are monotonically increasing, -1 o
 * @returns {number} zero-based k such that Ψk ≤ ψm ≤ Ψk+1 or Ψk ≥ ψm ≥ Ψk+1; -1 if such index does not exist
 */
export function searchIndex(arrayData, psi_m, sort) {

    for (let i = 0; i < arrayData.length - 1; i++) {
        // monotonically increasing array data
        if (sort === 1 && arrayData[i] <= psi_m && psi_m <= arrayData[i + 1]) {
            return i;
        }
        // monotonically decreasing array data
        if (sort === -1 && arrayData[i] >= psi_m && psi_m >= arrayData[i + 1]) {
            return i;

        }
    }

    // no range found, the psi_m is outside the indexVector's range
    return -1;
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

