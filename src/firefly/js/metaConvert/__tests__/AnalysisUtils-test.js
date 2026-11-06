import {MetaConst} from '../../data/MetaConst.js';
import {PlotAttribute} from '../../visualize/PlotAttribute.js';
import {WebPlotRequest} from '../../visualize/WebPlotRequest.js';
import {
    doFileNameAndTypeAnalysis, getExtensionFromUrl, isNonServerAnalysisType, isUsableDownloadType,
    makeAnalysisActivateFunc, makeAnalysisGetGridDataProduct, makeAnalysisGetSingleDataProduct, makeAnyEntry,
    makeDownloadType, makeGzipEntry, makeHtmlEntry, makeJsonEntry, makePdfEntry, makePngEntry, makeTarEntry,
    makeTextEntry, makeYamlEntry, uploadAndAnalyze
} from '../AnalysisUtils.js';
import {dispatchActivateFileMenuItem, dispatchUpdateDataProducts} from '../DataProductsCntlr.js';
import {DPtypes} from '../DataProductsType.js';
import {createGridImagesActivate} from '../ImageDataProductsUtil.js';
import {doUploadAndAnalysis} from '../UploadAndAnalysis.js';

// These modules pull in UI components that Jest can't load (validator/es, canvas), so they are replaced with stubs;
// each test sets the return values it needs.
jest.mock('../ImageDataProductsUtil.js', () => ({
    createGridImagesActivate: jest.fn(),
}));
jest.mock('../UploadAndAnalysis.js', () => ({
    doUploadAndAnalysis: jest.fn(),
}));
// keep the real module, but don't dispatch into the store
jest.mock('../DataProductsCntlr.js', () => ({
    ...jest.requireActual('../DataProductsCntlr.js'),
    dispatchActivateFileMenuItem: jest.fn(),
    dispatchUpdateDataProducts: jest.fn(),
}));

beforeEach(() => {
    jest.resetAllMocks();
});

// test-datalink-1-votable.xml: a made up DataLink table with one row for each kind of file;
// each row is [access_url, description, content_type]
const TEST_DIR = 'http://web.ipac.caltech.edu/staff/roby/data-products-test/';
const FITS_ROW = [TEST_DIR + '1904-66_SFL.fits', 'SFL.fits', 'application/fits'];
const VO_TABLE_ROW = [TEST_DIR + 'objsearch_ext.xml', 'vo table', 'text/xml-votable'];
const PDF_ROW = [TEST_DIR + 'hipsdoc.pdf', 'pdf table', 'application/pdf'];
const TEXT_ROW = [TEST_DIR + 'test_text_file.txt', 'text file', 'text/plain'];
const JSON_ROW = [TEST_DIR + 'text_json.json', 'json file', 'application/json'];
const YAML_ROW = [TEST_DIR + 'xx.yaml', 'yaml file', 'application/yaml'];
const TAR_ROW = ['https://irsa.ipac.caltech.edu/data/SOFIA/GREAT/L4/p4903/data/' +
    'Cycle5_GR_OT_05_0122_JJackson_NESSIE-C_CII.great.tar', 'tar data', 'application/tar'];
const HTML_ROW = ['https://irsa.ipac.caltech.edu/irsaviewer/dce?api=dl&id=spitzer_mipsgal', 'Open DCE', 'text/html'];
const MULTI_VO_TABLE_ROW = [TEST_DIR + 'multiTables_Ned.xml', 'multi vo table', 'text/xml-votable'];

describe('doFileNameAndTypeAnalysis', () => {
    // the DataLink menu calls it this way: no message wrapper, the row description as the name
    test.each([
        ['FITS', FITS_ROW],
        ['VOTable', VO_TABLE_ROW],
        ['plain text', TEXT_ROW],
        ['multi table VOTable', MULTI_VO_TABLE_ROW],
    ])('a %s file is left for the server to analyze', (label, [url, name, ct]) => {
        expect(doFileNameAndTypeAnalysis({url, ct, wrapWithMessage: false, name})).toBeUndefined();
    });

    test.each([
        ['PDF', PDF_ROW, DPtypes.DOWNLOAD, 'pdf'],
        ['tar', TAR_ROW, DPtypes.DOWNLOAD, 'tar'],
        ['HTML', HTML_ROW, DPtypes.DOWNLOAD, 'html'],
        ['JSON', JSON_ROW, DPtypes.TXT, 'json'],
        ['YAML', YAML_ROW, DPtypes.TXT, 'yaml'],
    ])('a %s file is handled in the browser', (label, [url, name, ct], displayType, fileType) => {
        const item = doFileNameAndTypeAnalysis({url, ct, wrapWithMessage: false, name});
        expect(item).toMatchObject({displayType, fileType, url, contentType: ct});
    });

    test('a download is wrapped in a message unless asked not to', () => {
        const [url, name, ct] = PDF_ROW;
        const item = doFileNameAndTypeAnalysis({url, ct, name});
        expect(item.displayType).toBe(DPtypes.MESSAGE);
        expect(item.menu[0]).toMatchObject({displayType: DPtypes.DOWNLOAD, fileType: 'pdf', url});
    });

    test('a file type is found from the file extension when there is no content type', () => {
        const item = doFileNameAndTypeAnalysis({url: TEST_DIR + 'xx.yaml', ct: '', wrapWithMessage: false});
        expect(item).toMatchObject({displayType: DPtypes.TXT, fileType: 'yaml'});
    });

    test('a FITS file is left for the server to analyze when its URL query has a download type in it', () => {
        const url = 'https://irsa.ipac.caltech.edu/getImage?id=star_field';
        expect(doFileNameAndTypeAnalysis({url, ct: 'application/fits', wrapWithMessage: false})).toBeUndefined();
    });

    test('a gzipped FITS file is left for the server to analyze', () => {
        expect(doFileNameAndTypeAnalysis({url: TEST_DIR + 'image.fits.gz', ct: 'application/gzip'})).toBeUndefined();
    });

    test('no URL gives nothing', () => {
        expect(doFileNameAndTypeAnalysis({url: undefined, ct: 'application/pdf'})).toBeUndefined();
    });
});

describe('isNonServerAnalysisType', () => {
    test('is true only for files handled in the browser', () => {
        expect(isNonServerAnalysisType(PDF_ROW[0], PDF_ROW[2])).toBe(true);
        expect(isNonServerAnalysisType(JSON_ROW[0], JSON_ROW[2])).toBe(true);
        expect(isNonServerAnalysisType(FITS_ROW[0], FITS_ROW[2])).toBe(false);
        expect(isNonServerAnalysisType(TEXT_ROW[0], TEXT_ROW[2])).toBe(false);
    });
});

describe('getExtensionFromUrl', () => {
    test('returns the lower case file extension', () => {
        expect(getExtensionFromUrl(TEST_DIR + 'hipsdoc.PDF')).toBe('pdf');
        expect(getExtensionFromUrl(TAR_ROW[0])).toBe('tar');
    });

    test('ignores the host, the query and the fragment', () => {
        expect(getExtensionFromUrl(HTML_ROW[0])).toBe('');
        expect(getExtensionFromUrl('https://irsa.ipac.caltech.edu/files/m31.fits?ver=2.pdfcopy')).toBe('fits');
        expect(getExtensionFromUrl('https://irsa.ipac.caltech.edu/datalink?ID=ivo://irsa.ipac/pdfarchive')).toBe('');
        expect(getExtensionFromUrl(TEST_DIR + 'hipsdoc.pdf#page=2')).toBe('pdf');
    });

    test('works on a server file path', () => {
        expect(getExtensionFromUrl('${upload-dir}/upload_123.fits')).toBe('fits');
    });

    test('returns an empty string when there is no URL or it ends with a dot', () => {
        expect(getExtensionFromUrl(undefined)).toBe('');
        expect(getExtensionFromUrl('file.')).toBe('');
    });
});

describe('isUsableDownloadType', () => {
    test('is true for a download extension or content type', () => {
        expect(isUsableDownloadType('pdf', undefined)).toBe(true);
        expect(isUsableDownloadType('', 'application/x-tar')).toBe(true);
        expect(isUsableDownloadType('', 'application/gzip')).toBe(true);
        expect(isUsableDownloadType('', 'text/html')).toBe(true);
    });

    test('is false for other files', () => {
        expect(isUsableDownloadType('fits', 'application/fits')).toBe(false);
        expect(isUsableDownloadType('json', undefined)).toBe(false);
    });
});

// made up: one ObsCore row
const OBS_URL = 'https://irsa.ipac.caltech.edu/data/example/obs42.fits';
const DP_ID = 'dpId-1';

/**
 * A one row ObsCore table; access_format decides if the file is shown in the browser or analyzed by the server.
 * @param {string} accessFormat
 * @param {Object} tableMeta
 * @param {string} dataRights - the value of the data rights column
 * @return {TableModel}
 */
function makeObsCoreTable(accessFormat, tableMeta, dataRights) {
    return {
        tbl_id: 'obscore-tbl', tableMeta, highlightedRow: 0,
        tableData: {
            columns: ['access_url', 'access_format', 'obs_title', 'data_rights'].map((name) => ({name})),
            data: [[OBS_URL, accessFormat, 'obs 42', dataRights]],
        },
    };
}

describe('makeAnalysisGetSingleDataProduct', () => {
    test('the returned function analyzes the single request the request maker gives', async () => {
        const request = WebPlotRequest.makeURIPlotRequest(OBS_URL);
        const table = makeObsCoreTable('application/fits', {}, 'public');
        const makeReq = jest.fn(() => ({single: request}));
        const getDataProduct = makeAnalysisGetSingleDataProduct(makeReq);
        const result = await getDataProduct(table, 0, {dpId: DP_ID}, {});
        expect(makeReq).toHaveBeenCalledWith(table, 0, true);
        expect(result).toMatchObject({displayType: DPtypes.PROMISE, request});
        expect(doUploadAndAnalysis).toHaveBeenCalledWith(expect.objectContaining({request, table, row: 0}));
    });

    test('a request maker can return the request itself', async () => {
        const request = WebPlotRequest.makeURIPlotRequest(OBS_URL);
        const table = makeObsCoreTable('application/fits', {}, 'public');
        const getDataProduct = makeAnalysisGetSingleDataProduct(() => request);
        const result = await getDataProduct(table, 0, {dpId: DP_ID}, {});
        expect(result.request).toBe(request);
    });
});

describe('uploadAndAnalyze', () => {
    test('a row the user has no access to gives a message and is not uploaded', async () => {
        const request = WebPlotRequest.makeURIPlotRequest(OBS_URL);
        const table = makeObsCoreTable('application/fits', {[MetaConst.DATARIGHTS_COL]: 'data_rights'}, 'proprietary');
        const result = await uploadAndAnalyze({request, table, row: 0, activateParams: {dpId: DP_ID}});
        expect(result.message).toBe('You do not have access to this data.');
        expect(doUploadAndAnalysis).not.toHaveBeenCalled();
    });

    test('a file the browser can handle is not uploaded, and its title has the observation title', async () => {
        const request = WebPlotRequest.makeURIPlotRequest(TEST_DIR + 'hipsdoc.pdf');
        const table = makeObsCoreTable('application/pdf', {}, 'public');
        const result = await uploadAndAnalyze({request, table, row: 0, activateParams: {dpId: DP_ID}});
        expect(result.displayType).toBe(DPtypes.MESSAGE);
        expect(result.menu[0]).toMatchObject({displayType: DPtypes.DOWNLOAD, fileType: 'pdf',
            name: 'Open PDF file (obs 42)'});
        expect(doUploadAndAnalysis).not.toHaveBeenCalled();
    });

    test('other files are uploaded for analysis, with a loading message until the analysis is done', async () => {
        const request = WebPlotRequest.makeURIPlotRequest(OBS_URL);
        const table = makeObsCoreTable('application/fits', {}, 'public');
        const analysisPromise = Promise.resolve({displayType: DPtypes.IMAGE});
        const serviceDescMenuList = [{name: 'Cutout'}];
        doUploadAndAnalysis.mockReturnValue(analysisPromise);
        const result = await uploadAndAnalyze({request, table, row: 0, activateParams: {dpId: DP_ID}, options: {},
            serviceDescMenuList, originalTitle: 'obs 42'});
        expect(result).toMatchObject({displayType: DPtypes.PROMISE, message: 'Loading...', request,
            promise: analysisPromise});
        expect(doUploadAndAnalysis).toHaveBeenCalledWith({table, row: 0, request, activateParams: {dpId: DP_ID},
            options: {}, originalTitle: 'obs 42', serviceDescMenuList});
    });
});

describe('makeAnalysisGetGridDataProduct', () => {
    const IMAGE_PARTS = [{type: 'Image', additionalImageParams: {MultiImageExts: '1'}}];
    const TABLE_PARTS = [{type: 'Table'}];

    /**
     * A table with one row per grid cell; rows with data_rights 'proprietary' can't be accessed.
     * @param {Array.<string>} dataRightsByRow
     * @param {number} highlightedRow
     * @return {TableModel}
     */
    function makeGridTable(dataRightsByRow, highlightedRow) {
        return {
            tbl_id: 'grid-tbl', highlightedRow, tableMeta: {[MetaConst.DATARIGHTS_COL]: 'data_rights'},
            tableData: {columns: [{name: 'data_rights'}], data: dataRightsByRow.map((rights) => [rights])},
        };
    }

    /**
     * doUploadAndAnalysis returns the file analysis parts in the file menu of its result.
     * @param {Array.<Object>} parts
     * @return {Object}
     */
    const makeAnalysisResult = (parts) => ({fileMenu: {fileAnalysis: {parts}}});

    test('makes an image grid from the rows the user can access, tagging each request with its row', async () => {
        const table = makeGridTable(['public', 'proprietary', 'public'], 0);
        const plotRows = [{row: 0, plotId: 'plot-0'}, {row: 1, plotId: 'plot-1'}, {row: 2, plotId: 'plot-2'}];
        const makeReq = (tbl, row) => ({single: WebPlotRequest.makeURIPlotRequest(`${TEST_DIR}row${row}.fits`)});
        const activate = jest.fn();
        doUploadAndAnalysis.mockResolvedValue(makeAnalysisResult(IMAGE_PARTS));
        createGridImagesActivate.mockReturnValue(activate);
        const getGrid = makeAnalysisGetGridDataProduct(makeReq);
        const working = await getGrid(table, plotRows, {imageViewerId: 'viewer-1'});
        const result = await working.promise;
        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'Image', activate});

        const uploadedRequests = doUploadAndAnalysis.mock.calls.map(([{request}]) => request);
        expect(uploadedRequests.map((r) => r.getPlotId())).toEqual(['plot-0', 'plot-2']);
        expect(uploadedRequests[1].getAttributes()).toMatchObject(
            {[PlotAttribute.RELATED_TABLE_ROW]: '2', [PlotAttribute.RELATED_TABLE_ID]: 'grid-tbl'});

        const [gridRequests, viewerId, tblId, rows] = createGridImagesActivate.mock.calls[0];
        expect(gridRequests.map((r) => r.getParam('MultiImageExts'))).toEqual(['1', '1']);
        expect([viewerId, tblId, rows]).toEqual(['viewer-1', 'grid-tbl', plotRows]);
    });

    test('files with no images cannot be shown in a grid', async () => {
        const table = makeGridTable(['public'], 0);
        const makeReq = (tbl, row) => ({single: WebPlotRequest.makeURIPlotRequest(`${TEST_DIR}table${row}.fits`)});
        doUploadAndAnalysis.mockResolvedValue(makeAnalysisResult(TABLE_PARTS));
        const working = await makeAnalysisGetGridDataProduct(makeReq)(table, [{row: 0, plotId: 'plot-0'}], {});
        const result = await working.promise;
        expect(result).toMatchObject({displayType: DPtypes.MESSAGE, gridNotSupported: true,
            message: 'This product cannot be shown in image grid'});
    });

    test('a grid is not made when the highlighted row has no image', async () => {
        const table = makeGridTable(['public', 'public'], 1);
        const plotRows = [{row: 0, plotId: 'plot-0'}, {row: 1, plotId: 'plot-1'}];
        const makeReq = (tbl, row) => ({single: WebPlotRequest.makeURIPlotRequest(`${TEST_DIR}mixed${row}.fits`)});
        doUploadAndAnalysis.mockImplementation(({request}) =>
            Promise.resolve(makeAnalysisResult(request.getPlotId() === 'plot-0' ? IMAGE_PARTS : TABLE_PARTS)));
        const working = await makeAnalysisGetGridDataProduct(makeReq)(table, plotRows, {});
        const result = await working.promise;
        expect(result.gridNotSupported).toBe(true);
    });

    test('a failed analysis gives a message', async () => {
        const table = makeGridTable(['public'], 0);
        const makeReq = (tbl, row) => ({single: WebPlotRequest.makeURIPlotRequest(`${TEST_DIR}fails${row}.fits`)});
        doUploadAndAnalysis.mockRejectedValue(new Error('503 Service Unavailable'));
        const working = await makeAnalysisGetGridDataProduct(makeReq)(table, [{row: 0, plotId: 'plot-0'}], {});
        const result = await working.promise;
        expect(result).toMatchObject({displayType: DPtypes.MESSAGE, message: 'No displayable data available for this row'});
    });
});

describe('makeAnalysisActivateFunc', () => {
    const MENU = [{name: 'Full image', menuKey: 'dlt-0'}, {name: 'Cutout', menuKey: 'dlt-1'}];
    const SER_DEF = {serDefParams: [{name: 'ID', ref: 'obs_id'}]};

    test('shows a loading message, then analyzes with the menu and the user input', async () => {
        const request = WebPlotRequest.makeURIPlotRequest(OBS_URL);
        const table = makeObsCoreTable('application/fits', {}, 'public');
        const userInputParams = {SIZE: '0.1'};
        doUploadAndAnalysis.mockResolvedValue({displayType: DPtypes.MESSAGE, message: 'No data'});
        const activate = makeAnalysisActivateFunc({table, row: 0, request, activateParams: {dpId: DP_ID},
            menuKey: 'dlt-1', activeMenuLookupKey: 'lookup-1', serDef: SER_DEF, originalTitle: 'obs 42', options: {}});
        await activate(MENU, userInputParams);
        expect(dispatchUpdateDataProducts.mock.calls[0]).toEqual([DP_ID,
            expect.objectContaining({displayType: DPtypes.MESSAGE, message: 'Loading...', isWorkingState: true,
                menuKey: 'dlt-1'})]);
        expect(doUploadAndAnalysis).toHaveBeenCalledWith(expect.objectContaining({request, menu: MENU,
            userInputParams, serDef: SER_DEF, originalTitle: 'obs 42', menuKey: 'dlt-1',
            analysisActivateFunc: activate}));
    });

    test('a message result is shown with the menu', async () => {
        const request = WebPlotRequest.makeURIPlotRequest(OBS_URL);
        doUploadAndAnalysis.mockResolvedValue({displayType: DPtypes.MESSAGE, message: 'No data'});
        const activate = makeAnalysisActivateFunc({table: {}, row: 0, request, activateParams: {dpId: DP_ID},
            menuKey: 'dlt-1', activeMenuLookupKey: 'lookup-1', serDef: SER_DEF});
        await activate(MENU);
        expect(dispatchUpdateDataProducts.mock.calls[1]).toEqual([DP_ID, {displayType: DPtypes.MESSAGE,
            message: 'No data', menu: MENU, menuKey: 'dlt-1', activeMenuLookupKey: 'lookup-1', serDef: SER_DEF,
            analysisActivateFunc: activate}]);
    });

    test.each([
        ['text', {displayType: DPtypes.TXT, name: 'Show plain text file', url: TEXT_ROW[0], fileType: 'text'}],
        ['PDF', {displayType: DPtypes.DOWNLOAD, name: 'Open PDF file', url: PDF_ROW[0], fileType: 'pdf'}],
        ['PNG', {displayType: DPtypes.PNG, url: TEST_DIR + 'preview.png'}],
    ])('a %s file found by the analysis is shown with the menu', async (label, analysisResult) => {
        const request = WebPlotRequest.makeURIPlotRequest(analysisResult.url);
        doUploadAndAnalysis.mockResolvedValue(analysisResult);
        const activate = makeAnalysisActivateFunc({table: {}, row: 0, request, activateParams: {dpId: DP_ID},
            menuKey: 'dlt-1', activeMenuLookupKey: 'lookup-1', serDef: SER_DEF});
        await activate(MENU);
        expect(dispatchUpdateDataProducts.mock.calls[1]).toEqual([DP_ID, {...analysisResult, menu: MENU,
            menuKey: 'dlt-1', activeMenuLookupKey: 'lookup-1', serDef: SER_DEF, analysisActivateFunc: activate}]);
    });

    test('a web page is opened in a new tab', async () => {
        const request = WebPlotRequest.makeURIPlotRequest(HTML_ROW[0]);
        const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
        doUploadAndAnalysis.mockResolvedValue({displayType: DPtypes.SEND_TO_BROWSER, url: HTML_ROW[0]});
        const activate = makeAnalysisActivateFunc({table: {}, row: 0, request, activateParams: {dpId: DP_ID},
            menuKey: 'dlt-1', activeMenuLookupKey: 'lookup-1', serDef: SER_DEF});
        await activate(MENU);
        expect(openSpy).toHaveBeenCalledWith(HTML_ROW[0], '_blank');
        expect(dispatchUpdateDataProducts.mock.calls[1][1]).toMatchObject({displayType: DPtypes.MESSAGE,
            message: 'Loaded in new tab', menu: MENU, menuKey: 'dlt-1', resetMenuKey: 'dlt-1', serDef: SER_DEF});
        openSpy.mockRestore();
    });

    test('a file menu result is activated, each file menu entry keeping the menu lookup key', async () => {
        const request = WebPlotRequest.makeURIPlotRequest(OBS_URL);
        const fileMenu = {menu: [{name: 'Image', menuKey: 'fm-0'}, {name: 'Table', menuKey: 'fm-1'}],
            activeItemLookupKey: 'file-lookup'};
        doUploadAndAnalysis.mockResolvedValue({displayType: DPtypes.IMAGE, name: 'Image', fileMenu});
        const activate = makeAnalysisActivateFunc({table: {}, row: 0, request, activateParams: {dpId: DP_ID},
            menuKey: 'dlt-1', activeMenuLookupKey: 'lookup-1'});
        await activate(MENU);
        expect(dispatchActivateFileMenuItem).toHaveBeenCalledWith({dpId: DP_ID, menu: MENU, currentMenuKey: 'dlt-1',
            fileMenu: {activeItemLookupKey: 'file-lookup', menu: [
                {name: 'Image', menuKey: 'fm-0', activeMenuLookupKey: 'lookup-1'},
                {name: 'Table', menuKey: 'fm-1', activeMenuLookupKey: 'lookup-1'}]}});
    });
});

describe('makeDownloadType', () => {
    test.each([
        ['tar', 'tar', undefined],
        ['pdf', '', 'application/pdf'],
        ['gzip', '', 'application/gzip'],
        ['html', 'html', undefined],
        ['unknown', 'zip', 'application/zip'],
    ])('makes a %s download entry from the extension or the content type', (fileType, ext, ct) => {
        const item = makeDownloadType(TEST_DIR + 'file', ext, ct, false);
        expect(item).toMatchObject({displayType: DPtypes.DOWNLOAD, fileType, url: TEST_DIR + 'file'});
    });

    test('wraps the entry in a message with the entry message', () => {
        const result = makeDownloadType(TAR_ROW[0], 'tar', 'application/tar', true);
        expect(result).toMatchObject({displayType: DPtypes.MESSAGE, message: 'This is a TAR file. It may only be downloaded'});
        expect(result.menu[0].fileType).toBe('tar');
    });
});

describe('make entry functions', () => {
    const URL = TEST_DIR + 'file';

    test.each([
        ['makePdfEntry', makePdfEntry, DPtypes.DOWNLOAD, 'pdf', 'Open PDF file (obs 42)', 'pdf table (obs 42) (PDF file)'],
        ['makeHtmlEntry', makeHtmlEntry, DPtypes.DOWNLOAD, 'html', 'Open HTML file (obs 42)', 'pdf table (obs 42) (HTML file)'],
        ['makeTarEntry', makeTarEntry, DPtypes.DOWNLOAD, 'tar', 'Download TAR file (obs 42)', 'pdf table (obs 42) (TAR file)'],
        ['makeGzipEntry', makeGzipEntry, DPtypes.DOWNLOAD, 'gzip', 'Download GZip file (obs 42)', 'pdf table (obs 42) (GZip file)'],
        ['makeJsonEntry', makeJsonEntry, DPtypes.TXT, 'json', 'Show JSON file (obs 42)', 'pdf table (obs 42) (JSON file)'],
        ['makeYamlEntry', makeYamlEntry, DPtypes.TXT, 'yaml', 'Show YAML file (obs 42)', 'pdf table (obs 42) (YAML file)'],
        ['makeTextEntry', makeTextEntry, DPtypes.TXT, 'text', 'Show plain text file (obs 42)', 'pdf table (obs 42) (plain text file)'],
    ])('%s makes its entry with titles from the name and observation title',
        (label, makeEntry, displayType, fileType, name, dropDownText) => {
            expect(makeEntry(URL, 'pdf table', 'obs 42')).toMatchObject({displayType, fileType, url: URL, name, dropDownText});
        });

    test('makePdfEntry and makeHtmlEntry can be opened in the browser', () => {
        expect(makePdfEntry(URL, undefined, 'obs 42').loadInBrowserMsg).toBe('Open PDF File (obs 42)');
        expect(makeHtmlEntry(URL, undefined, 'obs 42').loadInBrowserMsg).toBe('Open Page (obs 42)');
    });

    test('makePngEntry uses the name for its title', () => {
        expect(makePngEntry(URL, 'preview', 'obs 42')).toMatchObject({displayType: DPtypes.PNG, url: URL,
            name: 'preview (obs 42) (PNG image)'});
        expect(makePngEntry(URL).name).toBeUndefined();
    });

    test('makeAnyEntry makes a download of unknown type', () => {
        expect(makeAnyEntry(URL, 'pdf table', 'obs 42')).toMatchObject({displayType: DPtypes.DOWNLOAD,
            fileType: 'unknown', url: URL, name: 'Download Unknown file (obs 42)',
            dropDownText: 'pdf table (obs 42) (Unknown file)', message: 'This file may only be downloaded'});
    });

    test('with no name there is no drop down text', () => {
        expect(makeTarEntry(URL).dropDownText).toBeUndefined();
    });
});
