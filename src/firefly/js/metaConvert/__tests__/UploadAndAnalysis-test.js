import {getAppOptions} from '../../core/AppDataCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../../core/MasterSaga.js';
import {FileAnalysisType, Format} from '../../data/FileAnalysis.js';
import {MetaConst} from '../../data/MetaConst.js';
import {upload} from '../../rpc/CoreServices.js';
import {WebPlotRequest} from '../../visualize/WebPlotRequest.js';
import {makePdfEntry, makeTextEntry} from '../AnalysisUtils.js';
import {
    dispatchUpdateActiveKey, dispatchUpdateDataProducts, getActiveFileMenuKeyByKey, getActiveMenuKey
} from '../DataProductsCntlr.js';
import {DPtypes} from '../DataProductsType.js';
import {analyzePart} from '../PartAnalyzer.js';
import {doUploadAndAnalysis} from '../UploadAndAnalysis.js';
import {makeSingleDataProductWithMenu} from '../vo/ObsCoreConverter.js';
import {makeUrlFromParams} from '../vo/ServDescProducts.js';

jest.mock('../../rpc/CoreServices.js', () => ({
    ...jest.requireActual('../../rpc/CoreServices.js'),
    upload: jest.fn(),
}));
// keep the real modules, but don't touch the store
jest.mock('../../core/MasterSaga.js', () => ({
    ...jest.requireActual('../../core/MasterSaga.js'),
    dispatchAddActionWatcher: jest.fn(),
    dispatchCancelActionWatcher: jest.fn(),
}));
jest.mock('../../core/AppDataCntlr.js', () => ({
    ...jest.requireActual('../../core/AppDataCntlr.js'),
    getAppOptions: jest.fn(),
}));
jest.mock('../DataProductsCntlr.js', () => ({
    ...jest.requireActual('../DataProductsCntlr.js'),
    dispatchUpdateActiveKey: jest.fn(),
    dispatchUpdateDataProducts: jest.fn(),
    getActiveFileMenuKeyByKey: jest.fn(),
    getActiveMenuKey: jest.fn(),
}));
// These modules pull in UI components that Jest can't load (validator/es, canvas), so they are replaced with stubs;
// each test sets the return values it needs.
jest.mock('../AnalysisUtils.js', () => ({
    makePdfEntry: jest.fn(),
    makePngEntry: jest.fn(),
    makeTarEntry: jest.fn(),
    makeTextEntry: jest.fn(),
}));
jest.mock('../PartAnalyzer.js', () => ({
    analyzePart: jest.fn(),
}));
jest.mock('../ImageDataProductsUtil.js', () => ({
    createSingleImageActivate: jest.fn(),
    createSingleImageExtraction: jest.fn(),
}));
jest.mock('../vo/ObsCoreConverter.js', () => ({
    makeSingleDataProductWithMenu: jest.fn(),
}));
jest.mock('../vo/ServDescProducts.js', () => ({
    makeUrlFromParams: jest.fn(),
}));

// doUploadAndAnalysis keeps every analysis in a module cache keyed by the request, so each test uses its own URL.
const BASE_URL = 'https://irsa.ipac.caltech.edu/data/example/';
const SERVER_FILE_KEY = '${upload-dir}/upload_123.fits'; // the server key of the uploaded file
const DP_ID = 'dpId-1';

/**
 * The upload call returns the server key of the file and the file analysis as a JSON string.
 * @param {Object} fileAnalysis
 * @return {Object}
 */
function makeUploadResult(fileAnalysis) {
    return {status: '200', cacheKey: SERVER_FILE_KEY, fileFormat: fileAnalysis.fileFormat,
        analysisResult: JSON.stringify(fileAnalysis)};
}

// made up: an image part and the result PartAnalyzer gives for it
const IMAGE_PART_A = {type: FileAnalysisType.Image, index: 0, desc: 'W1 coadd'};
const IMAGE_PART_B = {type: FileAnalysisType.Image, index: 1, desc: 'W2 coadd'};
const IMAGE_RESULT_A = {displayType: DPtypes.IMAGE, name: 'Image W1 coadd', activate: 'activate-A'};
const IMAGE_RESULT_B = {displayType: DPtypes.IMAGE, name: 'Image W2 coadd', activate: 'activate-B'};
const TABLE_PART = {type: FileAnalysisType.Table, index: 1, desc: 'catalog'};
const TABLE_RESULT = {displayType: DPtypes.TABLE, name: 'Table catalog'};

describe('doUploadAndAnalysis', () => {
    beforeEach(() => {
        jest.resetAllMocks();
        getAppOptions.mockReturnValue({});
    });

    test('uploads the request for analysis, passing the analyzer params from the table meta', async () => {
        const url = BASE_URL + 'analyzer.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const table = {
            tableMeta: {
                [MetaConst.ANALYZER_ID]: 'irsa-analyzer',
                [MetaConst.ANALYZER_PARAMS]: 'mission = wise, band=1',
                [MetaConst.ANALYZER_COLUMNS]: 'obsid, missing_col',
            },
            highlightedRow: 0,
            tableData: {columns: [{name: 'obsid'}, {name: 'ra'}], data: [['obs-7', '10.5']]},
        };
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.PDF, parts: []}));
        await doUploadAndAnalysis({table, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(upload).toHaveBeenCalledWith(request, 'Details',
            {analyzerId: 'irsa-analyzer', mission: 'wise', band: '1', obsid: 'obs-7'});
    });

    test('passes no analyzer params when the table has no analyzer id', async () => {
        const url = BASE_URL + 'no-analyzer.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const table = {tableMeta: {[MetaConst.ANALYZER_PARAMS]: 'mission=wise'}};
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.PDF, parts: []}));
        await doUploadAndAnalysis({table, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(upload).toHaveBeenCalledWith(request, 'Details', {});
    });

    test('watches the URL for progress messages while uploading', async () => {
        const url = BASE_URL + 'watch.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.PDF, parts: []}));
        await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(dispatchAddActionWatcher).toHaveBeenCalledWith(expect.objectContaining({id: url, params: {url, dpId: DP_ID}}));
        expect(dispatchCancelActionWatcher).toHaveBeenCalledWith(url);
    });

    test('a PDF file gives a PDF entry', async () => {
        const url = BASE_URL + 'paper.pdf';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const pdfEntry = {displayType: DPtypes.DOWNLOAD, name: 'PDF'};
        makePdfEntry.mockReturnValue(pdfEntry);
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.PDF, parts: []}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(makePdfEntry).toHaveBeenCalledWith(url);
        expect(result).toBe(pdfEntry);
    });

    test('a text file gives a text entry', async () => {
        const url = BASE_URL + 'readme.txt';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const textEntry = {displayType: DPtypes.TXT, name: 'Text'};
        makeTextEntry.mockReturnValue(textEntry);
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.TEXT}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(makeTextEntry.mock.calls[0][0]).toBe(url);
        expect(result).toBe(textEntry);
    });

    test('an HTML page is sent to the browser with the service descriptor params', async () => {
        const url = BASE_URL + 'page.html';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const serDef = {serDefParams: [{name: 'ID', ref: 'obs_id'}]};
        makeUrlFromParams.mockReturnValue(url);
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.HTML, parts: []}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, serDef, activateParams: {dpId: DP_ID}});
        expect(result).toEqual({displayType: DPtypes.SEND_TO_BROWSER, url, serDefParams: serDef.serDefParams});
    });

    test('a region file can only be downloaded', async () => {
        const url = BASE_URL + 'shapes.reg';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.REGION, parts: []}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(result.displayType).toBe(DPtypes.MESSAGE);
        expect(result.message).toBe('Cannot display Region file, you may only download it');
        expect(result.menu[0].url).toBe(url);
    });

    test('a file of unknown type can only be downloaded', async () => {
        const url = BASE_URL + 'mystery.bin';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.UNKNOWN, fileName: 'mystery.bin', parts: []}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(result.message).toBe('No displayable data available for this row: Unknown file type');
        expect(result.menu[0]).toMatchObject({name: 'Download File', url});
    });

    test('an error response from a service gives an error message that can reset the menu', async () => {
        const url = BASE_URL + 'cutout?ID=bad';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const serDef = {serDefParams: [{name: 'ID', ref: 'obs_id'}]};
        makeUrlFromParams.mockReturnValue(url);
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.UNKNOWN,
            parts: [{type: FileAnalysisType.ErrorResponse, desc: 'ID not found'}]}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, serDef, menuKey: 'dlt-3',
            activateParams: {dpId: DP_ID}});
        expect(result).toMatchObject({displayType: DPtypes.MESSAGE, message: 'ID not found', badUrl: url,
            serDefParams: serDef.serDefParams, resetMenuKey: 'dlt-3'});
    });

    test('a FITS file with one image gives one image entry in a file menu', async () => {
        const url = BASE_URL + 'one-image.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        analyzePart.mockReturnValue({imageResult: IMAGE_RESULT_A});
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.FITS, filePath: SERVER_FILE_KEY,
            parts: [IMAGE_PART_A]}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'Image W1 coadd', menuKey: 'fm-0'});
        expect(result.fileMenu.menu.map((m) => m.name)).toEqual(['Image W1 coadd']);
        expect(dispatchUpdateActiveKey).toHaveBeenCalledWith({dpId: DP_ID,
            activeFileMenuKeyChanges: {[result.fileMenu.activeItemLookupKey]: 'fm-0'}});
    });

    test('a FITS file with two images gives an all images entry, then each image', async () => {
        const url = BASE_URL + 'two-images.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        analyzePart.mockImplementation(({part}) => ({imageResult: [IMAGE_RESULT_A, IMAGE_RESULT_B][part.index]}));
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.FITS, filePath: SERVER_FILE_KEY,
            parts: [IMAGE_PART_A, IMAGE_PART_B]}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(result.fileMenu.menu.map((m) => m.name)).toEqual(['All Images in File', 'Image W1 coadd', 'Image W2 coadd']);
        expect(result.fileMenu.menu.map((m) => m.menuKey)).toEqual(['fm-0', 'fm-1', 'fm-2']);
    });

    test('a part shown as both a table and an image gives a table entry that also activates the image', async () => {
        const url = BASE_URL + 'image-as-table.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        analyzePart.mockReturnValue({imageResult: IMAGE_RESULT_A, tableResult: TABLE_RESULT});
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.FITS, parts: [IMAGE_PART_A]}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(result.fileMenu.menu).toEqual([expect.objectContaining({name: 'Table catalog', imageActivate: 'activate-A'})]);
    });

    test('an image part and a table part give an entry for each', async () => {
        const url = BASE_URL + 'image-and-table.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        analyzePart.mockImplementation(({part}) =>
            part.type === FileAnalysisType.Image ? {imageResult: IMAGE_RESULT_A} : {tableResult: TABLE_RESULT});
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.FITS, parts: [IMAGE_PART_A, TABLE_PART]}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(result.fileMenu.menu.map((m) => m.name)).toEqual(['Image W1 coadd', 'Table catalog']);
    });

    test('the original title is added to each entry drop down text', async () => {
        const url = BASE_URL + 'titled.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        analyzePart.mockReturnValue({tableResult: TABLE_RESULT});
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.FITS, parts: [TABLE_PART]}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, originalTitle: 'obs 42',
            activateParams: {dpId: DP_ID}});
        expect(result.dropDownText).toBe('obs 42: Table catalog');
    });

    test('the file menu entry the user last picked is the one returned', async () => {
        const url = BASE_URL + 'last-picked.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        analyzePart.mockImplementation(({part}) =>
            part.type === FileAnalysisType.Image ? {imageResult: IMAGE_RESULT_A} : {tableResult: TABLE_RESULT});
        getActiveFileMenuKeyByKey.mockReturnValue('fm-1');
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.FITS, parts: [IMAGE_PART_A, TABLE_PART]}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(result.name).toBe('Table catalog');
    });

    test('a FITS file with only header HDUs can only be downloaded', async () => {
        const url = BASE_URL + 'headers-only.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        analyzePart.mockReturnValue({});
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.FITS,
            parts: [{type: FileAnalysisType.HeaderOnly, index: 0}, {type: FileAnalysisType.HeaderOnly, index: 1}]}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(result).toMatchObject({displayType: DPtypes.DOWNLOAD_MENU_ITEM, url,
            message: 'You may only download this File - Nothing to display - FITS file has only header HDUs'});
    });

    test('service descriptor menu entries are added after the analysis result', async () => {
        const url = BASE_URL + 'with-sd-menu.pdf';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const pdfEntry = {displayType: DPtypes.DOWNLOAD, name: 'PDF'};
        const serviceDescMenuList = [{name: 'Cutout', activeMenuLookupKey: 'lookup-1'}];
        const withMenu = {displayType: DPtypes.DOWNLOAD, name: 'PDF', menu: []};
        makePdfEntry.mockReturnValue(pdfEntry);
        makeSingleDataProductWithMenu.mockReturnValue(withMenu);
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.PDF, parts: []}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, serviceDescMenuList,
            activateParams: {dpId: DP_ID}});
        expect(makeSingleDataProductWithMenu).toHaveBeenCalledWith(DP_ID, pdfEntry, 1, serviceDescMenuList);
        expect(result).toBe(withMenu);
    });

    test('service descriptor params are added to a copy of the request URL', async () => {
        const url = BASE_URL + 'sync';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const serDef = {serDefParams: [{name: 'ID', ref: 'obs_id'}], dataLinkTableRowIdx: 4};
        const userInputParams = {SIZE: '0.1'};
        makeUrlFromParams.mockReturnValue(url + '?ID=obs-7&SIZE=0.1');
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.PDF, parts: []}));
        await doUploadAndAnalysis({table: {}, row: 0, request, serDef, userInputParams, activateParams: {dpId: DP_ID}});
        expect(makeUrlFromParams).toHaveBeenCalledWith(url, serDef, 4, userInputParams);
        expect(upload.mock.calls[0][0].getURL()).toBe(url + '?ID=obs-7&SIZE=0.1');
        expect(request.getURL()).toBe(url);
    });

    test('a second call for the same request uses the cached analysis', async () => {
        const url = BASE_URL + 'cached.pdf';
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.PDF, parts: []}));
        await doUploadAndAnalysis({table: {}, row: 0, request: WebPlotRequest.makeURIPlotRequest(url),
            activateParams: {dpId: DP_ID}});
        await doUploadAndAnalysis({table: {}, row: 0, request: WebPlotRequest.makeURIPlotRequest(url),
            activateParams: {dpId: DP_ID}});
        expect(upload).toHaveBeenCalledTimes(1);
        expect(makePdfEntry).toHaveBeenCalledTimes(2);
    });

    test('an error response is not cached, so the next call uploads again', async () => {
        const url = BASE_URL + 'temporary-error.fits';
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.UNKNOWN,
            parts: [{type: FileAnalysisType.ErrorResponse, desc: 'Error in File Retrieve: Service Unavailable'}]}));
        await doUploadAndAnalysis({table: {}, row: 0, request: WebPlotRequest.makeURIPlotRequest(url),
            activateParams: {dpId: DP_ID}});
        await doUploadAndAnalysis({table: {}, row: 0, request: WebPlotRequest.makeURIPlotRequest(url),
            activateParams: {dpId: DP_ID}});
        expect(upload).toHaveBeenCalledTimes(2);
    });

    test('user input params without a service descriptor use the active row', async () => {
        const url = BASE_URL + 'user-input';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const userInputParams = {SIZE: '0.1'};
        makeUrlFromParams.mockReturnValue(url + '?SIZE=0.1');
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.PDF, parts: []}));
        await doUploadAndAnalysis({table: {}, row: 2, request, userInputParams, activateParams: {dpId: DP_ID}});
        expect(makeUrlFromParams).toHaveBeenCalledWith(url, undefined, 2, userInputParams);
    });

    test('the app client analysis is used when the server has no analyzer for the table', async () => {
        const url = BASE_URL + 'client-analysis.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const pdfEntry = {displayType: DPtypes.DOWNLOAD, name: 'PDF'};
        const clientAnalysis = jest.fn(() => ({fileFormat: Format.PDF, parts: []}));
        getAppOptions.mockReturnValue({dataProducts: {clientAnalysis: {'my-analyzer': clientAnalysis}}});
        makePdfEntry.mockReturnValue(pdfEntry);
        upload.mockResolvedValue(makeUploadResult({fileFormat: Format.FITS, parts: [],
            dataProductsAnalyzerId: 'my-analyzer', analyzerFound: false}));
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, activateParams: {dpId: DP_ID}});
        expect(clientAnalysis).toHaveBeenCalledWith(expect.objectContaining({dataProductsAnalyzerId: 'my-analyzer'}));
        expect(result).toBe(pdfEntry);
    });

    test('a failed upload shows an error for the active menu entry and returns an upload error', async () => {
        const url = BASE_URL + 'fails.fits';
        const request = WebPlotRequest.makeURIPlotRequest(url);
        const error = new Error('503 Service Unavailable');
        const menu = [
            {name: 'Cutout', dropDownText: 'obs 42: Cutout', menuKey: 'dlt-0', activeMenuLookupKey: 'lookup-1'},
            {name: 'Full image', dropDownText: 'obs 42: Full image', menuKey: 'dlt-1', activeMenuLookupKey: 'lookup-1'},
        ];
        getActiveMenuKey.mockReturnValue('dlt-1');
        upload.mockRejectedValue(error);
        jest.spyOn(console, 'log').mockImplementation(() => undefined); // the failure is logged
        const result = await doUploadAndAnalysis({table: {}, row: 0, request, menu, activateParams: {dpId: DP_ID}});
        expect(result).toEqual({displayType: DPtypes.ERROR, error, url});
        expect(dispatchCancelActionWatcher).toHaveBeenCalledWith(url);
        const [dpId, shown] = dispatchUpdateDataProducts.mock.calls[0];
        expect(dpId).toBe(DP_ID);
        expect(shown).toMatchObject({displayType: DPtypes.MESSAGE, message: 'No displayable data available for Full image',
            name: 'Full image', dropDownText: 'obs 42: Full image', activeMenuLookupKey: 'lookup-1', menu});
        expect(shown.details).toEqual([{text: 'Show failed URL', url},
            {title: 'Server Message', text: '503 Service Unavailable'}]);
    });
});
