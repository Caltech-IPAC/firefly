import assert from 'assert';
import {parseWavelengthHeaderInfo} from '../WavelengthHeaderParser.js';
import { getJsonFiles} from './Projection-test.js';
import {getWavelength, LOG} from '../Wavelength.js';
import {makeWorldPt} from '../../Point.js';
import {getHeader} from '../../FitsHeaderUtil.js';

const fs = require('fs');
const precision=10;
/**
 * This is the unit test suits for Wavelength.  For each algorithm, the expected value is calculated independently based
 * on the algorithm in the reference paper and the known header data values.
 *
 * For Linear, Log, F2W, V2W, the four known points (pointArray) are used.
 * For TAB, the known simulated testing data "wavelengthTable.fits" stored in firefly_test_data under the same path as
 * projection is used.  The coordinates and index arrays are known and so are the psi_m and the pixIndex array.
 * The expected value is calculated according to the algorithm in the reference paper.
 *
 * For all the algorithm, the calculated values are the values that calculated using the Wavelength.js.
 *
 * The unit tests verify the the calculated values against the expected values.
 *
 * The testing data is stored as a json format.  The testing data wavelengthTableHeader.json was created using
 * the utility class FitsHeaderToJson.java.
 *
 */
const pointArray=[
   {ra: 66.5375697, dec: 66.1379036},
   {ra:153.0763342, dec:-30.4940466},
   {ra:233.5892165, dec:-49.1636287},
   {ra: 63.6285129, dec: -3.5576294}
];
/**
 * This is the two arrays in the wlTable that used for unit testing
 */
const coords=[
0.93842,
0.80334,
0.48544,
0.24423,
0.8719,
0.33002,
0.10626,
0.5729,
0.2104,
0.0198,
300.84592,
300.63605,
300.18134,
300.51,
300.27945,
300.03137,
300.06183,
300.9286,
300.3224,
300.4762,
300.0438,
300.43887,
300.36633,
300.35422,
300.5422,
300.49628,
300.9784,
300.25714,
300.23654,
300.6987,
300.54196,
300.17014,
300.40082,
300.76392,
300.41455,
300.33228,
300.58975,
300.54297,
300.80682,
300.59067,
300.04755,
300.2681,
300.82596,
300.91806,
300.54764,
300.3925,
300.10175,
300.4943,
300.89313,
300.25214,
300.287,
300.27628,
300.13312,
300.4609,
300.75735,
300.50327,
300.16064,
300.00058,
300.59317,
300.30023,
300.08948,
300.549,
300.00647,
300.07312,
300.76205,
300.74362,
300.2954,
300.79175,
300.20938,
300.28116,
300.90735,
300.9193,
300.1779,
300.8492,
300.97272,
300.44876,
300.75287,
300.89493,
300.49915,
300.96185,
300.1626,
300.65762,
300.64664,
300.99368,
300.12363,
300.39447,
300.1654,
300.2791,
300.2836,
300.33493,
300.9119,
300.8512,
300.10233,
300.95352,
300.64737,
300.83862,
300.65298,
300.698,
300.2936,
300.11954];

const indexVec=[
0.0,
1.0,
2.0,
3.0,
4.0,
5.0,
6.0,
7.0,
8.0,
9.0,
 10.0,
 11.0,
 12.0,
 13.0,
 14.0,
 15.0,
 16.0,
 17.0,
 18.0,
 19.0,
 20.0,
 21.0,
 22.0,
 23.0,
 24.0,
 25.0,
 26.0,
 27.0,
 28.0,
 29.0,
 30.0,
 31.0,
 32.0,
 33.0,
 34.0,
 35.0,
 36.0,
 37.0,
 38.0,
 39.0,
 40.0,
 41.0,
 42.0,
 43.0,
 44.0,
 45.0,
 46.0,
 47.0,
 48.0,
 49.0,
 50.0,
 51.0,
 52.0,
 53.0,
 54.0,
 55.0,
 56.0,
 57.0,
 58.0,
 59.0,
 60.0,
 61.0,
 62.0,
 63.0,
 64.0,
 65.0,
 66.0,
 67.0,
 68.0,
 69.0,
 70.0,
 71.0,
 72.0,
 73.0,
 74.0,
 75.0,
 76.0,
 77.0,
 78.0,
 79.0,
 80.0,
 81.0,
 82.0,
 83.0,
 84.0,
 85.0,
 86.0,
 87.0,
 88.0,
 89.0];

//This is the points to test WAVE_TAB
const tabPointArray=[
    {x: 1744, y: 1069},
    {x:1371, y:1032},
    {x:1040, y:1067}
];
//psi_m = lambda_r + cdelt* omega;
const psi_mAry=[72.500000961253,17.000000851964,69.500000754981 ];
const pixIdxAry=[73, 18, 70];

/**
 * This is the testing data location.  The testing data is generated using java FitsHeaderToJson class.
 * The main function in ProjectionTest.java created the json files for Wavelength unit test
 * @type {string}
 */
const JAVA_TEST_DATA_PATH='firefly_test_data/edu/caltech/ipac/visualize/plot/projection/';

/**
 * This method gets the testing json file the corresponding algorithm
 * @param allFiles
 * @param fileName
 * @returns {*}
 */
function getTestJsonFile(allFiles, fileName){
    for (let i=0; i<allFiles.length; i++){
        if (allFiles[i].toLowerCase().includes(fileName.toLowerCase())){
            return allFiles[i];
        }
    }

}

function getPixelCoordinates(pt, header){
    let px = Math.round(pt.x - 0.5) +1 ;//x
    let py = Math.round(pt.y - 0.5) +1; //y

    let pz = getHeader(header,'SPOT_PL',0 ) + 1 ;//header.get('SPOT_PL', 0) + 1 ;

    return [px, py, pz];
}
function  getOmega(pixCoords, N, r_j, pc_3j, s_3){
    let omega=0;
    for (let i=0; i<N; i++){
        omega += s_3 * pc_3j[i] * (pixCoords[i]-r_j[i]);
    }
    return omega;
}

function getParamValues(header,algorithm){

    let N = parseInt(getHeader(header, 'WCSAXES', -1));
    if (N==-1){
        N=parseInt(getHeader(header, 'NAXIS', -1));
    }
    if (N==-1) return undefined;


    let r_j = [];
    let  pc_3j = [];
    //The pi_j can be either CDi_j or PCi_j, so we have to test both
    for (let i = 0; i < N; i++) {
        r_j[i] = parseFloat(getHeader(header, 'CRPIX' + (i+1), undefined));

        //matrix mij can be either PCij or CDij
        pc_3j[i]=parseFloat(getHeader(header, 'PC3' +'_'+ (i+1), undefined));
        if (pc_3j[i]===undefined) {
            pc_3j[i]=parseFloat(getHeader(header, 'CD3' +'_'+ (i+1), undefined));
        }

        if (r_j[i]===undefined){
            throw Error('CRPIXka  is not defined');
        }
        if ( pc_3j[i]===undefined ){
            throw Error('Either PC3_i or CD3_i has to be defined');
        }

        if (algorithm==='LOG'){
            //the values in CRPIXk and CDi_j (PC_i_j) are log based on 10 rather than natural log, so a factor is needed.
            r_j[i] *=Math.log(10);
            pc_3j[i] *=Math.log(10);
        }
    }

    let s_3 = parseFloat(getHeader(header, 'CDELT3', 0));
    let lambda_r =parseFloat(getHeader( header, 'CRVAL3', 0.0));
    return {N, r_j, pc_3j, s_3,  lambda_r};
}
//this is to implement the wavelength independently to verify the wavelength implementation
function calculatedExpectedValue(pt, algorithm, header){

     let pixCoords = getPixelCoordinates(pt, header);
     const {N, r_j, pc_3j, s_3,  lambda_r} = getParamValues(header,algorithm);

    let omega = getOmega(pixCoords,  N, r_j, pc_3j, s_3);
    let wl;
    switch( algorithm.toUpperCase()){
        case 'LINEAR':
            wl= lambda_r + omega;
            break;
        case 'LOG':
            wl=lambda_r* Math.exp(omega/lambda_r);
            break;
        case 'F2W':
            wl=lambda_r *  lambda_r/(lambda_r - omega);
            break;
        case 'V2W':
            const lamda_0 = getHeader(header, 'RESTWAV', 0);
            const b_lamda = ( Math.pow(lambda_r, 4) - Math.pow(lamda_0, 4) + 4 *Math.pow(lamda_0, 2)*lambda_r*omega )/
            Math.pow((Math.pow(lamda_0, 2) + Math.pow(lambda_r, 2) ), 2);
            wl = lamda_0 - Math.sqrt( (1 + b_lamda)/(1-b_lamda) );
            break;

    }

    return wl;
}

/**
 * The index array is indexAry=[0, 1, 2, 3, ...89]
 *
 * Use a few known values as psi_m to do the test
 * The psi_m array is calculated by hand using the data in the header and stored as a known data
 * pixIdxAry is also a known array because using the psi_m array, the pixIndxAry can be determined using the
 * index array data.
 *
 * @param header
 * @param wlTable
 * @returns {number}
 */
function calculateExpectedTabWavelength( header, wlTable){

    //lambda_r = crval3=7.5E-07
    //PC_3_1=2.93E-10
    //PC3_2 = 1.5
    //PC3_3=3.5
    //CDELT3=1.0=S3
    //let pixCoords = getPixelCoordinates(pt, header);
    //CRPIX1 = 1024.0, CRPIX2=1024, CRPIX3=0.0
    // lambda_r = crval3=7.5E-07
    //psi_m = lambda_r + cdelt* omega = 10.5


    let pixCoords, ipt, gamma_m;

    let wl=[];
    let psiIdx, kIndex;
    for (let i=0; i<tabPointArray.length; i++){
        ipt = tabPointArray[i];
        pixCoords = getPixelCoordinates(ipt, header);

        //const omega= parseFloat(getOmega(pixCoords,  N, r_j, pc_3j, s_3));
        //console.log('omega='+omega);
        //psi_m = lambda_r + cdelt* omega;
        //console.log('psi_m='+psi_m);
        psiIdx = pixIdxAry[i];
        gamma_m = psiIdx + 1  + (psi_mAry[i] - indexVec[psiIdx]) / (indexVec[psiIdx+1] - indexVec[psiIdx]);
        kIndex = Math.floor(gamma_m);
        wl[i] =coords[kIndex] + (gamma_m - (kIndex+1) )* (coords[kIndex+1]-coords[kIndex]);
    }

    return wl;


}
function doTest(jsonStr, algorithm){
    const header = jsonStr.header;
    const wlData= parseWavelengthHeaderInfo(header, '', undefined, undefined);

    for (let i=0; i<pointArray.length; i++){
        const pt = makeWorldPt(pointArray[i].ra, pointArray[i].dec);
        const calculatedWL = getWavelength(pt, 0, wlData);
        const expectedValue = calculatedExpectedValue(pt, algorithm , header );
        assert.equal( calculatedWL.toFixed(precision), expectedValue.toFixed(precision), 'The current calculated wavelength value in' +
            '  is not the same as previously stored value');
    }
}

describe('A test suite for Wavelength.js', function () {


    const path = require('path');
    //__filename returns absolute path to file where it is placed
    const scriptDirString = path.dirname(fs.realpathSync(__filename));
    const rootPath = scriptDirString.split('firefly')[0];
    const dataPath =rootPath+JAVA_TEST_DATA_PATH;
    //read out all test files stored in json format
    const jsonFiles = getJsonFiles(dataPath, true);


    test('Test Linear Algorithm',   ()=> {
        const linearFile = getTestJsonFile(jsonFiles, 'linear');
        const jsonStr =require(linearFile);
        doTest(jsonStr, 'linear');

    });

    test('Test Log Algorithm',   ()=> {
        const linearFile = getTestJsonFile(jsonFiles, 'log');
        const jsonStr =require(linearFile);
        doTest(jsonStr, 'log');

    });

    test('Test F2W Algorithm',   ()=> {
        const linearFile = getTestJsonFile(jsonFiles, 'F2W');
        const jsonStr =require(linearFile);
        doTest(jsonStr, 'F2W');

    });

    test('Test V2W Algorithm',   ()=> {
        const linearFile = getTestJsonFile(jsonFiles, 'V2W');
        const jsonStr =require(linearFile);
        doTest(jsonStr, 'V2W');

    });
    /**
     * Use the known psi_m and corresponding index to test the calculated value against the expected values.
     */
    test('Test WAVE-TAB Algorithm',   ()=> {

        const tableFile = getTestJsonFile(jsonFiles, 'Table');
        const jsonStr =require(tableFile);
        const header = jsonStr.header;
        const wlTable = jsonStr.wlTable;
        const wlData= parseWavelengthHeaderInfo(header, '', undefined, wlTable);


        const expectedTabWl = calculateExpectedTabWavelength(header, wlTable);
        let calculatedTabWl=[];
        for (let i=0; i<tabPointArray.length; i++){
            calculatedTabWl[i] = getWavelength(tabPointArray[i], 0, wlData);
        }
        for (let i=0; i<tabPointArray.length; i++){
            assert.equal( calculatedTabWl[i].toFixed(precision), expectedTabWl[i].toFixed(precision), 'The current calculated wavelength value in' +
                '  is not the same as previously stored value');
        }

    });


});



