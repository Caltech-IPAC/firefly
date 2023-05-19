import assert from 'assert';
import {parseWavelengthHeaderInfo} from '../WavelengthHeaderParser.js';
import {getJsonFiles} from './Projection-test.js';
import {getWavelength} from '../Wavelength.js';
import {makeWorldPt} from '../../Point.js';
import {getHeader} from '../../FitsHeaderUtil.js';
import {RDConst} from 'firefly/visualize/WebPlot.js';

const fs = require('fs');
const precision = 10;
/**
 * This is the unit test suits for Wavelength.  For each algorithm, the expected value is calculated independently based
 * on the algorithm in the reference paper and the known header data values.
 *
 * The testing data is stored as a json format. FitsHeaderToJson.java utility class can be used to convert FITS header
 * to JSON.
 *
 * For Linear, Log, F2W, V2W the four known world points (pointArray) and test files are used.
 * The TAB algorithm is tested with the header and lookup table in a JSON string (jsonTAB).
 *
 * The expected value is calculated according to the algorithm in the reference paper.
 * The actual values are calculated using Wavelength.js.
 *
 * The unit tests verify the calculated values against the expected values.
 *
 */
const pointArray = [
    {ra: 66.5375697, dec: 66.1379036},
    {ra: 153.0763342, dec: -30.4940466},
    {ra: 233.5892165, dec: -49.1636287},
    {ra: 63.6285129, dec: -3.5576294}
];

// data for TAB algorithm test
// 5x5 image with wavelength changing in y direction (along NAXIS2)
const jsonTAB = {
    'wlTable': {
        'tableData': {
            'data': {'0': [[0,2,4], [10,18,20]]},
            'columns': [
                {'name':'INDEX','arraySize':'3','type':'float'},
                {'name':'COORDS','arraySize':'3','type':'float'}
            ]
        }},
    'header':{
        // only NAXIS mattter in this section
        'NAXIS':{'comment':'number of array dimensions','value':'2'},
        'NAXIS1':{'value':5},
        'NAXIS2':{'value':5},

        'WCSAXESw':{'value':'1'},
        'WCSNAMEw':{'value':'SPECTRAL'},
        'CTYPE1w':{'value':'WAVE-TAB'},
        'CUNIT1w':{'value':'m'},
        'PS1_0w':{'comment':'table extension name','value':'WCS-table'},
        'PS1_1w':{'comment':'table version number','value':'COORDS'},
        'PS1_2w':{'comment':'table level number','value':'INDEX'},
        'PV1_1w':{'comment':'column for coordinate array','value':'1'},
        'PV1_2w':{'comment':'column for indexing vector','value':'1'},
        'PV1_3w':{'comment':'axis number','value':'1'},

        // CRPIXj is 0 by default
        // we only care about second pixel coordinate
        'CRPIX2w':{'comment':'Pixel coordinate of reference point','value':'1'},
        // in our case, CRVAL1w should match the first index in the index array
        'CRVAL1w':{'comment':'Coordinate value at reference point','value':'0'},
        'CDELT1w':{'comment':'Coordinate increment at reference point','value':'1'},

        // PCi_j: linear transformation matrix between Pixel Axes j
        //  and Intermediate-coordinate Axes i
        //  default values of PCi_j is 1 where i=j and 0 where i!=j
        'PC1_1w':{'value':'0'},
        'PC1_2w':{'value':'1'},
    }
};

// image points for TAB algorithm test
// the wavelength is changing in y direction, x does not matter
const pointArrayTAB=[
    {x: 0, y: 0},
    {x: 1, y: 1},
    {x: 2, y: 2},
    {x: 3, y: 3},
    {x: 4, y: 4}
];

// expected wavelength for tab algorithm test
const expectedWlTAB = [10, 14, 18, 19, 20];

/**
 * This is the testing data location.  The testing data is generated using java FitsHeaderToJson class.
 * The main function in ProjectionTest.java created the json files for Wavelength unit test
 * @type {string}
 */
const JAVA_TEST_DATA_PATH = 'firefly_test_data/edu/caltech/ipac/visualize/plot/projection/';

/**
 * This method gets the testing json file the corresponding algorithm
 * @param allFiles
 * @param fileName
 * @returns {*}
 */
function getTestJsonFile(allFiles, fileName) {
    for (let i = 0; i < allFiles.length; i++) {
        if (allFiles[i].toLowerCase().includes(fileName.toLowerCase())) {
            return allFiles[i];
        }
    }
}

function getPixelCoordinates(pt, header) {
    const px = Math.round(pt.x - 0.5) + 1; //x
    const py = Math.round(pt.y - 0.5) + 1; //y

    const pz = getHeader(header, 'SPOT_PL', '0') + 1; //header.get('SPOT_PL', 0) + 1 ;

    return [px, py, pz];
}

function getOmega(pixCoords, N, r_j, pc_3j, s_3) {
    let omega = 0;
    for (let i = 0; i < N; i++) {
        omega += s_3 * pc_3j[i] * (pixCoords[i] - r_j[i]);
    }
    return omega;
}

function getParamValues(header) {

    let N = parseInt(getHeader(header, 'WCSAXES', '-1'));
    if (N === -1) {
        N = parseInt(getHeader(header, 'NAXIS', '-1'));
    }
    if (N === -1) return undefined;


    const r_j = [];
    const pc_3j = [];
    //The pi_j can be either CDi_j or PCi_j, so we have to test both
    for (let i = 0; i < N; i++) {
        r_j[i] = parseFloat(getHeader(header, 'CRPIX' + (i + 1), undefined));

        //matrix mij can be either PCij or CDij
        pc_3j[i] = parseFloat(getHeader(header, 'PC3' + '_' + (i + 1), undefined));
        if (pc_3j[i] === undefined) {
            pc_3j[i] = parseFloat(getHeader(header, 'CD3' + '_' + (i + 1), undefined));
        }

        if (r_j[i] === undefined) {
            throw Error('CRPIXka  is not defined');
        }
        if (pc_3j[i] === undefined) {
            throw Error('Either PC3_i or CD3_i has to be defined');
        }
    }

    const cdelt = parseFloat(getHeader(header, 'CDELT3', '1.0'));
    const crval = parseFloat(getHeader(header, 'CRVAL3', '0.0'));
    return {N, r_j, pc_3j, cdelt, crval};
}

//this is to implement the wavelength independently to verify the wavelength implementation
function calculatedExpectedValue(pt, algorithm, header) {

    const pixCoords = getPixelCoordinates(pt, header);
    const {N, r_j, pc_3j, cdelt, crval} = getParamValues(header);

    const omega = getOmega(pixCoords, N, r_j, pc_3j, cdelt);
    let wl;
    switch (algorithm.toUpperCase()) {
        case 'LINEAR':
            wl = crval + omega;
            break;
        case 'LOG':
            wl = crval * Math.exp(omega / crval);
            break;
        case 'F2W':
            wl = crval * crval / (crval - omega);
            break;
        case 'V2W':
            const lambda_0 = parseInt(getHeader(header, 'RESTWAV', '0'));
            const b_lambda = (Math.pow(crval, 4) - Math.pow(lambda_0, 4) + 4 * Math.pow(lambda_0, 2) * crval * omega) /
                Math.pow((Math.pow(lambda_0, 2) + Math.pow(crval, 2)), 2);
            wl = lambda_0 - Math.sqrt((1 + b_lambda) / (1 - b_lambda));
            break;

    }

    return wl;
}


function doTest(jsonStr, algorithm) {
    const header = jsonStr.header;
    const wlData = parseWavelengthHeaderInfo(header, '', undefined, undefined);

    for (let i = 0; i < pointArray.length; i++) {
        const pt = makeWorldPt(pointArray[i].ra, pointArray[i].dec);
        const calculatedWL = getWavelength(pt, 0, wlData)[0];
        const expectedValue = calculatedExpectedValue(pt, algorithm, header);
        assert.equal(calculatedWL.toFixed(precision), expectedValue.toFixed(precision), 'The current calculated wavelength value in' +
            '  is not the same as previously stored value');
    }
}

describe('A test suite for Wavelength.js', function () {


    const path = require('path');
    //__filename returns absolute path to file where it is placed
    const scriptDirString = path.dirname(fs.realpathSync(__filename));
    const rootPath = scriptDirString.split('firefly')[0];
    const dataPath = rootPath + JAVA_TEST_DATA_PATH;
    //read out all test files stored in json format
    const jsonFiles = getJsonFiles(dataPath, true);


    test('Test Linear Algorithm', () => {
        const linearFile = getTestJsonFile(jsonFiles, 'linear');
        const jsonStr = require(linearFile);
        doTest(jsonStr, 'linear');

    });

    test('Test Log Algorithm', () => {
        const linearFile = getTestJsonFile(jsonFiles, 'log');
        const jsonStr = require(linearFile);
        doTest(jsonStr, 'log');

    });

    test('Test F2W Algorithm', () => {
        const linearFile = getTestJsonFile(jsonFiles, 'F2W');
        const jsonStr = require(linearFile);
        doTest(jsonStr, 'F2W');

    });

    test('Test V2W Algorithm', () => {
        const linearFile = getTestJsonFile(jsonFiles, 'V2W');
        const jsonStr = require(linearFile);
        doTest(jsonStr, 'V2W');

    });
    /**
     * Use the known psi_m and corresponding index to test the calculated value against the expected values.
     */
    test('Test WAVE-TAB Algorithm', () => {

        const header = jsonTAB.header;
        const wlTable = jsonTAB.wlTable;
        const wlData= parseWavelengthHeaderInfo(header, 'w', undefined,
            [ {dataType:RDConst.WAVELENGTH_TABLE_RESOLVED, table:wlTable, hduName:'WCS-table', hduVersion: 1, hduLevel: 1} ]);

        const calculatedWl = [];
        for (let i = 0; i < pointArrayTAB.length; i++) {
            calculatedWl[i] = getWavelength(pointArrayTAB[i], 0, wlData)[0];
        }
        for (let i = 0; i < pointArrayTAB.length; i++) {
            assert.equal(calculatedWl[i].toFixed(precision), expectedWlTAB[i].toFixed(precision),
                'The current calculated wavelength value ' + i +
                ' is not the same as previously stored value');
        }

    });
});
