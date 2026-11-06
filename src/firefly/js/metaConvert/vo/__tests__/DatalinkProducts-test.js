import {MAX_ROW} from '../../../tables/TableConst.js';
import {doFetchTable} from '../../../tables/TableUtil.js';
import {getPreferCutout} from '../../../ui/tap/Cutout.js';
import {Band} from '../../../visualize/Band.js';
import WebPlotRequest from '../../../visualize/WebPlotRequest.js';
import {GROUP_BY_RELATED_COLUMNS, IMAGE_ONLY, TABLE_ONLY} from '../../DataProductConst.js';
import {
    dispatchUpdateActiveKey, dispatchUpdateDataProducts, getActiveMenuKey, getCurrentActiveKeyID
} from '../../DataProductsCntlr.js';
import {DPtypes} from '../../DataProductsType.js';
import {
    createGridImagesActivate, createRelatedGridImagesActivate, createSingleImageExtraction
} from '../../ImageDataProductsUtil.js';
import {createObsCoreProductTitle} from '../../VoUITitles.js';
import {fetchAllDatalinkTables, fetchDatalinkTable} from '../DatalinkFetch.js';
import {getServiceDescriptors} from '../../../voAnalyzer/VoDataLinkServDef.js';
import {
    createGridResult, datalinkDescribeThreeColor, getDatalinkRelatedImageGridProduct, getDatalinkSingleDataProduct,
    getObsCoreRelatedDataProductByFilter, makeDlUrl
} from '../DatalinkProducts.js';
import {
    getCutoutSizeWarning, getCutoutTotalWarning, hasBandInMenuKey, hasLabelInMenuKey, IMAGE, makeBandLabelMenuKey,
    makeMenuEntry, menuKeysMatchBandLabel, processDatalinkTable, RELATED_IMAGE_GRID, sortRelatedGridUsingRequest,
    SPECTRUM, USE_ALL
} from '../DataLinkProcessor.js';

// Loading these modules pulls in UI components whose dependencies (validator/es) Jest does not transform,
// so they are replaced with stubs; each test sets the return values it needs.
jest.mock('../DatalinkFetch.js', () => ({
    fetchAllDatalinkTables: jest.fn(),
    fetchDatalinkTable: jest.fn(),
}));
jest.mock('../DataLinkProcessor.js', () => ({
    findMenuKeyWithName: jest.fn(),
    getCutoutSizeWarning: jest.fn(),
    getCutoutTotalWarning: jest.fn(),
    hasBandInMenuKey: jest.fn(),
    hasLabelInMenuKey: jest.fn(),
    makeBandLabelMenuKey: jest.fn(),
    makeMenuEntry: jest.fn(),
    menuKeysMatchBandLabel: jest.fn(),
    processDatalinkTable: jest.fn(),
    sortRelatedGridUsingRequest: jest.fn(),
    IMAGE: 'imageAlgorithm',
    RELATED_IMAGE_GRID: 'relatedImageGridAlgorithm',
    SPECTRUM: 'spectrumAlgorithm',
    USE_ALL: 'useAllAlgorithm',
}));
jest.mock('../../ImageDataProductsUtil.js', () => ({
    createGridImagesActivate: jest.fn(),
    createRelatedGridImagesActivate: jest.fn(),
    createSingleImageExtraction: jest.fn(),
}));
// keep the real modules, but don't use the store
jest.mock('../../DataProductsCntlr.js', () => ({
    ...jest.requireActual('../../DataProductsCntlr.js'),
    dispatchUpdateActiveKey: jest.fn(),
    dispatchUpdateDataProducts: jest.fn(),
    getActiveMenuKey: jest.fn(),
    getCurrentActiveKeyID: jest.fn(),
}));
jest.mock('../../../tables/TableUtil.js', () => ({
    ...jest.requireActual('../../../tables/TableUtil.js'),
    doFetchTable: jest.fn(),
}));
jest.mock('../../../ui/tap/Cutout.js', () => ({
    ...jest.requireActual('../../../ui/tap/Cutout.js'),
    getPreferCutout: jest.fn(),
}));

// A Rubin DP1 ObsCore row served through DataLink (from rubin-obscore-data.vot)
const RUBIN_OBSCORE_COLUMNS = [
    {name: 'obs_collection', type: 'char'},
    {name: 'dataproduct_type', type: 'char'},
    {name: 'dataproduct_subtype', type: 'char'},
    {name: 'calib_level', type: 'int'},
    {name: 's_ra', type: 'double', units: 'deg'},
    {name: 's_dec', type: 'double', units: 'deg'},
    {name: 'obs_id', type: 'char'},
    {name: 's_region', type: 'char'},
    {name: 'access_url', type: 'char'},
    {name: 'access_format', type: 'char'},
    {name: 'instrument_name', type: 'char'},
];
const RUBIN_RAW_URL = 'https://data.lsst.cloud/api/datalink/links?ID=ivo%3A%2F%2Forg.rubinobs%2Flsst-dp1%3Frepo%3Ddp1%26id%3D9b663d6d-59b7-5ec9-836a-6c64809d37ef';
const RUBIN_RAW_ROW = [
    'LSST.DP1', 'image', 'lsst.raw', 1, 52.87362023749581, -28.376214735792995, 'CC_O_20241108_000253',
    'POLYGON ICRS 52.779071 -28.240995 52.721578 -28.461865 52.968449 -28.511367 53.025383 -28.290376',
    RUBIN_RAW_URL, 'application/x-votable+xml;content=datalink', 'LSSTComCam',
];
const RUBIN_OBSCORE_TABLE = {
    tbl_id: 'rubin-obscore', highlightedRow: 0, totalRows: 1, tableMeta: {}, request: {},
    tableData: {columns: RUBIN_OBSCORE_COLUMNS, data: [RUBIN_RAW_ROW]},
};

// the DataLink links table the service returns for that row; only passed through here, so its contents don't matter
const RUBIN_DATALINK_TABLE = {tbl_id: 'rubin-datalink-row-1'};

// Euclid MER tile 85951, shaped like euclid-dl.xml (an older test file): each band has a #this file and a
// #cutout through the sd-cutout service descriptor, marked as a related grid by local_semantics 'cutout-grid'.
// The Euclid DR1 service gives each band its own id (euclid-datalink-Products-row-1.vot).
const EUCLID_DATALINK_COLUMNS = ['id', 'access_url', 'service_def', 'error_message', 'semantics', 'local_semantics',
    'description', 'content_type', 'content_length'].map((name, idx) => ({name, type: 'char', ID: `col_${idx}`}));
const EUCLID_BANDS = ['NIR-H', 'NIR-J', 'NIR-Y', 'VIS'];
const EUCLID_FILE_URL = 'https://irsadev.ipac.caltech.edu/ibe/data/Euclid_TEST/MER_images/TILE85951';
const EUCLID_CUTOUT_RESOURCE = {
    type: 'meta', utype: 'adhoc:service', ID: 'sd-cutout',
    params: [{name: 'accessURL', value: 'https://irsadev.ipac.caltech.edu/ibe/cutouts/cutout'}],
    groups: [{name: 'inputParams', params: [
        {name: 'ra', UCD: 'pos.eq.ra;meta.main', value: '236.05813470000001'},
        {name: 'dec', UCD: 'pos.eq.dec;meta.main', value: '40.499986100000001'},
        {name: 'size', UCD: 'phys.size', value: '0.01'},
    ]}],
};

/**
 * Build the Euclid DataLink links table.
 * @param {Object} p
 * @param {boolean} p.idPerBand - give each band its own id, as Euclid DR1 does, so each band's #this and
 *              #cutout rows are paired; with one id for the whole tile, as in euclid-dl.xml, every #this
 *              matches all the cutouts and none is paired
 * @return {TableModel}
 */
function makeEuclidDatalinkTable({idPerBand}) {
    const data = EUCLID_BANDS.flatMap((band) => {
        const id = idPerBand ? `85951-${band}` : '85951';
        const description = `EUC_MER_BGSUB-MOSAIC-${band}_TILE85951`;
        return [
            [id, `${EUCLID_FILE_URL}/${description}.fits`, '', '', '#this', '', description, 'image/fits', ''],
            [id, '', 'sd-cutout', '', '#cutout', 'cutout-grid', description, 'image/fits', ''],
        ];
    });
    return {
        tbl_id: 'euclid-datalink', totalRows: data.length, resources: [EUCLID_CUTOUT_RESOURCE],
        tableData: {columns: EUCLID_DATALINK_COLUMNS, data},
    };
}

const EUCLID_MER_TABLE = {
    tbl_id: 'euclid-mer', highlightedRow: 0, totalRows: 1, tableMeta: {}, request: {},
    tableData: {columns: [{name: 'tileid', type: 'char'}], data: [['85951']]},
};
const EUCLID_DATALINK_URL = 'https://irsadev.ipac.caltech.edu/datalink/links?ID=85951';

/**
 * Make the image menu entry processDatalinkTable returns for one Euclid band.
 * @param {string} band
 * @param {number} idx - position in the menu
 * @return {Object}
 */
function makeEuclidImageEntry(band, idx) {
    const request = WebPlotRequest.makeURIPlotRequest(`https://irsadev.ipac.caltech.edu/ibe/cutouts/cutout?band=${band}`);
    request.setPlotId(`dl-plot-${idx}`);
    request.setTitle(band);
    return {displayType: DPtypes.ANALYZE, menuKey: `dlt-${idx}`, request, dlData: {semantics: '#cutout', band}};
}

/**
 * Call getDatalinkRelatedImageGridProduct for the Euclid tile.
 * @param {Object} p
 * @param {Array.<string>} [p.threeColorOps]
 * @param {Object} p.options
 * @return {Promise<DataProductsDisplayType>}
 */
function getDatalinkRelatedImageGridProductForEuclidTile({threeColorOps, options}) {
    return getDatalinkRelatedImageGridProduct({
        dlTableUrl: EUCLID_DATALINK_URL, activateParams: {dpId: 'dpId-test', imageViewerId: 'viewer-test'},
        table: EUCLID_MER_TABLE, row: 0, threeColorOps, options,
    });
}

// A Rubin visit_image ObsCore table set up for GROUP_BY_RELATED_COLUMNS: rows with the same lsst_visit and
// lsst_detector are the related images. Values follow rubin-obscore-data.vot.
const RUBIN_RELATED_COLUMNS = [...RUBIN_OBSCORE_COLUMNS,
    {name: 'lsst_visit', type: 'long'}, {name: 'lsst_detector', type: 'int'}, {name: 'lsst_band', type: 'char'}];
const RUBIN_RELATED_BANDS = ['g', 'r', 'i'];

/**
 * Make one Rubin visit_image ObsCore row for a band of visit 2024110900212, detector 0.
 * @param {string} band
 * @return {Array}
 */
function makeRubinVisitImageRow(band) {
    return ['LSST.DP1', 'image', 'lsst.visit_image', 2, 52.90534765986676, -28.385397899576418, `CC_O_20241109_000212_${band}`,
        'POLYGON ICRS 52.810463 -28.250255 52.753310 -28.471344 53.000488 -28.520453 53.057142 -28.299308',
        `https://data.lsst.cloud/api/datalink/links?ID=visit-2024110900212-${band}`,
        'application/x-votable+xml;content=datalink', 'LSSTComCam', 2024110900212, 0, band];
}

const RUBIN_RELATED_TABLE = {
    tbl_id: 'rubin-visit-images', highlightedRow: 0, totalRows: 1,
    tableMeta: {'tbl.relatedCols': 'lsst_visit, lsst_detector'},
    request: {tbl_id: 'rubin-visit-images', id: 'IpacTableFromSource', source: 'rubin-obscore.xml',
        filters: '"lsst_band" = \'g\''},
    tableData: {columns: RUBIN_RELATED_COLUMNS, data: [makeRubinVisitImageRow('g')]},
};

// what the server returns for the related columns filter: one row per band
const RUBIN_FILTERED_TABLE = {
    tbl_id: 'rubin-visit-images-filtered', totalRows: RUBIN_RELATED_BANDS.length, tableMeta: {}, request: {},
    tableData: {columns: RUBIN_RELATED_COLUMNS, data: RUBIN_RELATED_BANDS.map((band) => makeRubinVisitImageRow(band))},
};

const RUBIN_DATALINK_COLUMNS = ['ID', 'access_url', 'service_def', 'error_message', 'description', 'semantics',
    'content_type', 'content_length'].map((name) => ({name, type: 'char'}));
const RUBIN_CUTOUT_RESOURCE = {
    type: 'meta', utype: 'adhoc:service', ID: 'cutout-sync',
    params: [{name: 'standardID', value: 'ivo://ivoa.net/std/SODA#sync-1.0'},
        {name: 'accessURL', value: 'https://data.lsst.cloud/api/cutout/sync'}],
    groups: [{name: 'inputParams', params: [{name: 'ID', UCD: 'meta.id;meta.dataset', value: 'x'}]}],
};

/**
 * Build the DataLink links table for one band, shaped like rubin-datalink-row-7.vot.
 * @param {Object} p
 * @param {string} p.band
 * @param {boolean} p.withCutout - add the #cutout row through the cutout-sync descriptor
 * @return {TableModel}
 */
function makeRubinBandDatalinkTable({band, withCutout}) {
    const id = `ivo://org.rubinobs/lsst-dp1?repo=dp1&id=visit-2024110900212-${band}`;
    const data = [[id, `https://storage.googleapis.com/visit_image_${band}.fits`, '', '', 'Primary image', '#this',
        'application/fits', '110926080']];
    if (withCutout) data.push([id, '', 'cutout-sync', '', 'Cutout service (SODA sync)', '#cutout', 'application/fits', '']);
    return {
        tbl_id: `rubin-datalink-${band}`, totalRows: data.length, resources: [RUBIN_CUTOUT_RESOURCE],
        tableData: {columns: RUBIN_DATALINK_COLUMNS, data},
    };
}

/**
 * Make the menu entry makeMenuEntry returns for a band image.
 * @param {string} band
 * @param {number} idx
 * @return {Object}
 */
function makeRubinBandImageEntry(band, idx) {
    const request = WebPlotRequest.makeURIPlotRequest(`https://storage.googleapis.com/visit_image_${band}.fits`);
    request.setPlotId(`band-plot-${idx}`);
    request.setTitle(band);
    return {displayType: DPtypes.ANALYZE, menuKey: `dlt-${idx}`, request};
}

/**
 * Call getObsCoreRelatedDataProductByFilter for the Rubin g band row.
 * @param {Object} p
 * @param {Array.<string>} [p.threeColorOps]
 * @param {Object} p.options
 * @return {Promise<DataProductsDisplayType>}
 */
function getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps, options}) {
    return getObsCoreRelatedDataProductByFilter(RUBIN_RELATED_TABLE, 0, threeColorOps, 'plot-highlight',
        {dpId: 'dpId-test', imageViewerId: 'viewer-test'}, options);
}

// DataLinkProcessor can't be loaded in Jest, so its band/label menu key helpers are replaced with these copies
// of the real key format: '--__LABEL:<label>--__BAND:<band>--__<menuKey>'
const makeBandLabelMenuKeyCopy = (label = '', band = '', menuKey) =>
    `--${label ? `__LABEL:${label}--` : ''}${band ? `__BAND:${band}--` : ''}${menuKey ? `__${menuKey}` : ''}`;
const hasBandInMenuKeyCopy = (menuKey = '', band = '') => menuKey.includes(`__BAND:${band}`);
const menuKeysMatchBandLabelCopy = (key1, key2) =>
    key1.match(/__BAND:(.*?)--/)?.[1] === key2.match(/__BAND:(.*?)--/)?.[1] &&
    key1.match(/__LABEL:(.*?)--/)?.[1] === key2.match(/__LABEL:(.*?)--/)?.[1];

/**
 * Use the menu key helper copies for the band/label mocks.
 */
function useMenuKeyHelperCopies() {
    makeBandLabelMenuKey.mockImplementation(makeBandLabelMenuKeyCopy);
    hasBandInMenuKey.mockImplementation(hasBandInMenuKeyCopy);
    menuKeysMatchBandLabel.mockImplementation(menuKeysMatchBandLabelCopy);
}

// a grid of two Euclid MER tiles, the second highlighted; each tile's DataLink result has the NIR-H, NIR-J and
// NIR-Y images, labeled with the IRSA bandpass_name and label DataLink extension columns
const GRID_TABLE = {tbl_id: 'euclid-mer-tiles'};
const GRID_PLOT_ROWS = [{row: 0, highlight: false}, {row: 1, highlight: true}];
const GRID_BANDS = ['NIR-H', 'NIR-J', 'NIR-Y'];
const GRID_ACTIVATE_PARAMS = {dpId: 'dpId-test', imageViewerId: 'viewer-test'};

/**
 * Make the menu entry for one band image of a tile, as processDatalinkTable returns it for a related grid.
 * @param {Object} p
 * @param {string} p.tile
 * @param {string} p.band
 * @param {number} p.idx - menu position
 * @param {boolean} p.withBandpass - include the bandpass_name extension
 * @return {Object}
 */
function makeTileBandEntry({tile, band, idx, withBandpass}) {
    const request = WebPlotRequest.makeURIPlotRequest(`https://irsadev.ipac.caltech.edu/ibe/cutouts/cutout?tile=${tile}&band=${band}`);
    request.setTitle(`${tile} ${band}`);
    return {
        displayType: DPtypes.ANALYZE, name: `${band} image`, request,
        menuKey: makeBandLabelMenuKeyCopy(band, withBandpass ? band : '', `dlt-${idx}`),
        dlData: {labelDLExt: band, bandpassNameDLExt: withBandpass ? band : undefined, dlAnalysis: {isImage: true, isGrid: true}},
    };
}

/**
 * Make the DataLink product createGridResult gets for one tile: its band images as a menu.
 * @param {Object} p
 * @param {string} p.tile
 * @param {Array.<string>} p.bands
 * @param {boolean} p.withBandpass
 * @return {Object}
 */
function makeTileProduct({tile, bands, withBandpass}) {
    const menu = bands.map((band, idx) => makeTileBandEntry({tile, band, idx, withBandpass}));
    return {...menu[0], menu};
}

/**
 * Make a related grid image entry with the band flags analyzeDatalinkRow sets from '-red', '-green' and '-blue'
 * in the semantics or local_semantics.
 * @param {Object} p
 * @param {string} p.title
 * @param {Object} p.bandFlags - rBand, gBand and bBand
 * @return {Object}
 */
function makeColorBandEntry({title, bandFlags}) {
    const request = WebPlotRequest.makeURIPlotRequest(`https://irsadev.ipac.caltech.edu/ibe/cutouts/cutout?band=${title}`);
    request.setTitle(title);
    return {displayType: DPtypes.ANALYZE, menuKey: `dlt-${title}`, request,
        dlData: {dlAnalysis: {rBand: false, gBand: false, bBand: false, ...bandFlags}}};
}

/**
 * Call datalinkDescribeThreeColor for the Euclid tile.
 * @param {Object} options
 * @return {Promise<Object>}
 */
function datalinkDescribeThreeColorForEuclidTile(options) {
    return datalinkDescribeThreeColor(EUCLID_DATALINK_URL, EUCLID_MER_TABLE, 0, options);
}

// ZTF light curve catalog with its DataLink service descriptor (serDescTbl.xml): id comes from the oid column,
// bad_catflags_mask is fixed at 0 (an int PARAM, so the server sends a number), and time has no value
const ZTF_LIGHT_CURVE_URL = 'https://irsa.ipac.caltech.edu/cgi-bin/ZTF/nph_light_curves?collection=ztf_dr3';
const ZTF_TABLE = {
    tbl_id: 'ztf-lc', totalRows: 2, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', desc: 'Link to ZTF light curve for ztf_dr3 collection',
        params: [
            {name: 'standardID', value: 'ivo://ivoa.net/std/DataLink#links-1.0'},
            {name: 'accessURL', value: ZTF_LIGHT_CURVE_URL},
        ],
        groups: [{name: 'inputParams', params: [
            {name: 'id', type: 'char', ref: 'col_0', value: ''},
            {name: 'bad_catflags_mask', type: 'int', value: 0},
            {name: 'time', type: 'char', value: ''},
        ]}],
    }],
    tableData: {columns: [{name: 'oid', type: 'long', ID: 'col_0'}], data: [['686203400167748'], ['1684211300162373']]},
};

/**
 * Call getDatalinkSingleDataProduct for the Rubin row with the given options and extra arguments.
 * @param {Object} p
 * @param {Object} p.options
 * @param {Object} [p.extra] - other getDatalinkSingleDataProduct arguments
 * @return {Promise<DataProductsDisplayType>}
 */
function getDatalinkSingleDataProductForRubinRow({options, extra}) {
    return getDatalinkSingleDataProduct({
        dlTableUrl: RUBIN_RAW_URL, options, sourceTable: RUBIN_OBSCORE_TABLE, row: 0,
        activateParams: {dpId: 'dpId-test'}, ...extra,
    });
}

describe('DatalinkProducts:', () => {

    // clearMocks (jest config) keeps queued mock*Once values, so an unused one would leak into the next test
    beforeEach(() => jest.resetAllMocks());

    test('getDatalinkSingleDataProduct: shows a working message, then fetches the DataLink table', async () => {
        const options = {datalinkTblRequestOptions: {pageSize: 100}};
        fetchDatalinkTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        await getDatalinkSingleDataProductForRubinRow({options, extra: {}});

        expect(dispatchUpdateDataProducts).toHaveBeenCalledWith('dpId-test',
            expect.objectContaining({displayType: DPtypes.MESSAGE, message: 'Loading data products...', isWorkingState: true}));
        expect(fetchDatalinkTable).toHaveBeenCalledWith(RUBIN_RAW_URL, {pageSize: 100});
    });

    test('getDatalinkSingleDataProduct: the DataLink table is processed with every algorithm by default', async () => {
        const options = {};
        const activateParams = {dpId: 'dpId-test'};
        const additionalServiceDescMenuList = [{menuKey: 'sd-0'}];
        fetchDatalinkTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);
        processDatalinkTable.mockReturnValueOnce('datalink-product');

        const result = await getDatalinkSingleDataProductForRubinRow(
            {options, extra: {activateParams, additionalServiceDescMenuList, useForTableGrid: false}});

        expect(result).toBe('datalink-product');
        expect(processDatalinkTable).toHaveBeenCalledWith({
            sourceTable: RUBIN_OBSCORE_TABLE, row: 0, datalinkTable: RUBIN_DATALINK_TABLE, activateParams,
            options, parsingAlgorithm: USE_ALL, doFileAnalysis: true,
            dlTableUrl: RUBIN_RAW_URL, additionalServiceDescMenuList, useForTableGrid: false,
        });
    });

    test('getDatalinkSingleDataProduct: grid cells pass on doFileAnalysis and useForTableGrid', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        await getDatalinkSingleDataProductForRubinRow({options: {}, extra: {doFileAnalysis: false, useForTableGrid: true}});

        expect(processDatalinkTable.mock.calls[0][0]).toMatchObject({doFileAnalysis: false, useForTableGrid: true});
    });

    test('getDatalinkSingleDataProduct: limitViewerDisplay IMAGE_ONLY processes only images', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        await getDatalinkSingleDataProductForRubinRow({options: {limitViewerDisplay: IMAGE_ONLY}, extra: {}});

        expect(processDatalinkTable.mock.calls[0][0].parsingAlgorithm).toBe(IMAGE);
    });

    test('getDatalinkSingleDataProduct: limitViewerDisplay TABLE_ONLY processes only spectra', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        await getDatalinkSingleDataProductForRubinRow({options: {limitViewerDisplay: TABLE_ONLY}, extra: {}});

        expect(processDatalinkTable.mock.calls[0][0].parsingAlgorithm).toBe(SPECTRUM);
    });

    test('getDatalinkSingleDataProduct: a server error with an HTTP status is shown as an error', async () => {
        // QueryUtil.checkForFailures reports failed fetches as 'Request failed with status <code> <message>'
        const cause = 'edu.caltech.ipac.firefly.server.query.DataAccessException: Request failed with status 404 Not Found';
        fetchDatalinkTable.mockRejectedValueOnce({cause});

        const result = await getDatalinkSingleDataProductForRubinRow({options: {}, extra: {}});

        expect(result).toMatchObject({
            displayType: DPtypes.MESSAGE, message: cause, complexMessage: true, detailMsgAry: [RUBIN_RAW_URL]});
        expect(processDatalinkTable).not.toHaveBeenCalled();
    });

    test('getDatalinkSingleDataProduct: a server error without a status offers the DataLink table as a download', async () => {
        const reason = {cause: 'edu.caltech.ipac.firefly.server.query.DataAccessException: connection refused'};
        fetchDatalinkTable.mockRejectedValueOnce(reason);

        const result = await getDatalinkSingleDataProductForRubinRow({options: {}, extra: {}});

        expect(result.message).toBe(
            'No data to display: Could not load datalink data, edu.caltech.ipac.firefly.server.query.DataAccessException: connection refused');
        expect(result.menu[0]).toMatchObject({displayType: DPtypes.DOWNLOAD, url: RUBIN_RAW_URL});
    });

    test('getDatalinkSingleDataProduct: any other failure offers the DataLink table as a download', async () => {
        fetchDatalinkTable.mockRejectedValueOnce(new Error('network down'));

        const result = await getDatalinkSingleDataProductForRubinRow({options: {}, extra: {}});

        expect(result.message).toBe('No data to display: Could not load datalink data, Error: network down');
        expect(result.menu[0]).toMatchObject({
            displayType: DPtypes.DOWNLOAD, url: RUBIN_RAW_URL,
            name: 'Download File: ' + createObsCoreProductTitle(RUBIN_OBSCORE_TABLE, 0),
        });
    });

    test('getDatalinkSingleDataProduct: a failure while processing the DataLink table is also caught', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);
        processDatalinkTable.mockImplementationOnce(() => { throw new Error('bad links table'); });

        const result = await getDatalinkSingleDataProductForRubinRow({options: {}, extra: {}});

        expect(result.message).toBe('No data to display: Could not load datalink data, Error: bad links table');
    });


    test('getDatalinkRelatedImageGridProduct: TABLE_ONLY is a configuration error', async () => {
        const result = await getDatalinkRelatedImageGridProductForEuclidTile(
            {threeColorOps: undefined, options: {limitViewerDisplay: TABLE_ONLY}});

        expect(result.message).toBe('Configuration Error: No support for related spectrum');
        expect(fetchDatalinkTable).not.toHaveBeenCalled();
    });

    test('getDatalinkRelatedImageGridProduct: a DataLink table with no grid images returns a message', async () => {
        // the Euclid rows without their 'cutout-grid' local_semantics
        const noGridTable = makeEuclidDatalinkTable({idPerBand: false});
        noGridTable.tableData = {...noGridTable.tableData,
            data: noGridTable.tableData.data.map((row) => row.map((value) => value === 'cutout-grid' ? '' : value))};
        fetchDatalinkTable.mockResolvedValueOnce(noGridTable);

        const result = await getDatalinkRelatedImageGridProductForEuclidTile({threeColorOps: undefined, options: {}});

        expect(result.message).toBe('no support for related grid in datalink file');
        expect(processDatalinkTable).not.toHaveBeenCalled();
    });

    test('getDatalinkRelatedImageGridProduct: the grid cutouts become one image grid product', async () => {
        const datalinkTable = makeEuclidDatalinkTable({idPerBand: false});
        const options = {datalinkTblRequestOptions: {pageSize: 100}, gridForceRowSize: 2};
        const imageEntries = EUCLID_BANDS.map((band, idx) => makeEuclidImageEntry(band, idx));
        const extractEntry = {displayType: DPtypes.EXTRACT, menuKey: 'extract-0'};
        const messageEntry = {displayType: DPtypes.MESSAGE, menuKey: 'message-0', message: 'not an image'};
        fetchDatalinkTable.mockResolvedValueOnce(datalinkTable);
        processDatalinkTable.mockReturnValueOnce({menu: [...imageEntries, extractEntry, messageEntry], serDef: 'grid-serdef'});
        getPreferCutout.mockReturnValueOnce(true);
        createRelatedGridImagesActivate.mockReturnValueOnce('activate-grid');
        createSingleImageExtraction.mockReturnValueOnce('extraction');

        const result = await getDatalinkRelatedImageGridProductForEuclidTile({threeColorOps: undefined, options});

        expect(dispatchUpdateDataProducts).toHaveBeenCalledWith('dpId-test',
            expect.objectContaining({message: 'Loading data products...', isWorkingState: true}));
        expect(fetchDatalinkTable).toHaveBeenCalledWith(EUCLID_DATALINK_URL, {pageSize: 100});
        expect(processDatalinkTable).toHaveBeenCalledWith({
            sourceTable: EUCLID_MER_TABLE, row: 0, datalinkTable, dlTableUrl: EUCLID_DATALINK_URL,
            activateParams: {dpId: 'dpId-test', imageViewerId: 'viewer-test'}, doFileAnalysis: false,
            options, parsingAlgorithm: RELATED_IMAGE_GRID,
        });

        const requestAry = imageEntries.map(({request}) => request);
        expect(requestAry.map((request) => request.getPlotId()))
            .toEqual(['dl-plot-0-related_grid-0', 'dl-plot-1-related_grid-1', 'dl-plot-2-related_grid-2', 'dl-plot-3-related_grid-3']);
        expect(createRelatedGridImagesActivate).toHaveBeenCalledWith({
            requestAry, threeColorReqAry: undefined, imageViewerId: 'viewer-test', tbl_id: 'euclid-mer',
            onActivePvChanged: undefined, highlightPlotId: undefined,
        });
        expect(createSingleImageExtraction).toHaveBeenCalledWith(requestAry);
        expect(result).toMatchObject({
            displayType: DPtypes.IMAGE, name: 'image grid', menuKey: 'image-grid-0',
            activate: 'activate-grid', extraction: 'extraction', enableCutout: true, gridForceRowSize: 2,
            serDef: 'grid-serdef', menu: [extractEntry],
            dlData: {semantics: '#cutout', band: 'NIR-H', cutoutToFullWarning: undefined},
        });
    });

    test('getDatalinkRelatedImageGridProduct: three color requests are copies of the chosen grid images', async () => {
        const imageEntries = EUCLID_BANDS.map((band, idx) => makeEuclidImageEntry(band, idx));
        fetchDatalinkTable.mockResolvedValueOnce(makeEuclidDatalinkTable({idPerBand: false}));
        processDatalinkTable.mockReturnValueOnce({menu: imageEntries});

        // the keys of the three color description: red is image 3, green image 0, no blue
        await getDatalinkRelatedImageGridProductForEuclidTile({threeColorOps: ['3', '0', undefined], options: {}});

        const {threeColorReqAry} = createRelatedGridImagesActivate.mock.calls[0][0];
        expect(threeColorReqAry[0].getTitle()).toBe('VIS');
        expect(threeColorReqAry[1].getTitle()).toBe('NIR-H');
        expect(threeColorReqAry[2]).toBeUndefined();
        expect(threeColorReqAry[0].getPlotId()).toBe('3id_euclid-datalink');
        expect(threeColorReqAry[0]).not.toBe(imageEntries[3].request);
    });

    test('getDatalinkRelatedImageGridProduct: relatedGridImageOrder highlights the last active image', async () => {
        const imageEntries = EUCLID_BANDS.map((band, idx) => makeEuclidImageEntry(band, idx));
        fetchDatalinkTable.mockResolvedValueOnce(makeEuclidDatalinkTable({idPerBand: false}));
        processDatalinkTable.mockReturnValueOnce({menu: imageEntries});
        getCurrentActiveKeyID.mockReturnValueOnce('last-source');
        getActiveMenuKey.mockReturnValueOnce('__LABEL:NIR-J--dlt-1');
        hasLabelInMenuKey.mockImplementation((menuKey, title) => menuKey.includes(`__LABEL:${title}--`));

        await getDatalinkRelatedImageGridProductForEuclidTile(
            {threeColorOps: undefined, options: {relatedGridImageOrder: ['VIS', 'NIR-Y', 'NIR-J', 'NIR-H']}});

        expect(getActiveMenuKey).toHaveBeenCalledWith('dpId-test', 'last-source');
        const {highlightPlotId, onActivePvChanged} = createRelatedGridImagesActivate.mock.calls[0][0];
        expect(highlightPlotId).toBe('dl-plot-1-related_grid-1');
        expect(onActivePvChanged).toEqual(expect.any(Function));
    });

    test('getDatalinkRelatedImageGridProduct: paired cutouts carry a total size warning when cutouts are preferred', async () => {
        const imageEntries = EUCLID_BANDS.map((band, idx) => makeEuclidImageEntry(band, idx));
        fetchDatalinkTable.mockResolvedValueOnce(makeEuclidDatalinkTable({idPerBand: true}));
        processDatalinkTable.mockReturnValueOnce({menu: imageEntries});
        getPreferCutout.mockReturnValueOnce(true);
        getCutoutTotalWarning.mockReturnValueOnce('Warning: 4 full images');

        const result = await getDatalinkRelatedImageGridProductForEuclidTile({threeColorOps: undefined, options: {}});

        expect(getCutoutTotalWarning).toHaveBeenCalledWith(imageEntries.map(({dlData}) => dlData), 4);
        expect(result.dlData.cutoutToFullWarning).toBe('Warning: 4 full images');
    });

    test('getDatalinkRelatedImageGridProduct: unpaired cutouts get no total size warning', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(makeEuclidDatalinkTable({idPerBand: false}));
        processDatalinkTable.mockReturnValueOnce({menu: EUCLID_BANDS.map((band, idx) => makeEuclidImageEntry(band, idx))});
        getPreferCutout.mockReturnValueOnce(true);

        const result = await getDatalinkRelatedImageGridProductForEuclidTile({threeColorOps: undefined, options: {}});

        expect(getCutoutTotalWarning).not.toHaveBeenCalled();
        expect(result.dlData.cutoutToFullWarning).toBeUndefined();
    });

    test('getDatalinkRelatedImageGridProduct: a failure offers the DataLink table as a download', async () => {
        fetchDatalinkTable.mockRejectedValueOnce(new Error('network down'));

        const result = await getDatalinkRelatedImageGridProductForEuclidTile({threeColorOps: undefined, options: {}});

        expect(result.message).toBe('No data to display: Could not load datalink data, Error: network down');
        expect(result.menu[0]).toMatchObject({displayType: DPtypes.DOWNLOAD, url: EUCLID_DATALINK_URL});
    });


    test('getObsCoreRelatedDataProductByFilter: the table is refetched filtered on the related columns of the row', async () => {
        const options = {datalinkTblRequestOptions: {pageSize: 100}};
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockResolvedValueOnce(
            RUBIN_RELATED_BANDS.map((band) => makeRubinBandDatalinkTable({band, withCutout: false})));
        makeMenuEntry.mockImplementation(({idx}) => makeRubinBandImageEntry(RUBIN_RELATED_BANDS[idx], idx));

        await getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps: undefined, options});

        expect(dispatchUpdateDataProducts).toHaveBeenCalledWith('dpId-test',
            expect.objectContaining({message: 'Loading data products...', isWorkingState: true}));
        // the related columns filter replaces the table's own filters
        expect(doFetchTable).toHaveBeenCalledWith({
            id: 'IpacTableFromSource', source: 'rubin-obscore.xml', startIdx: 0, pageSize: MAX_ROW,
            filters: '"lsst_visit" = \'2024110900212\' AND "lsst_detector" = \'0\'',
        });
        expect(fetchAllDatalinkTables).toHaveBeenCalledWith(RUBIN_FILTERED_TABLE, {pageSize: 100});
    });

    test('getObsCoreRelatedDataProductByFilter: each related row\'s #this image becomes a grid image', async () => {
        const options = {gridForceRowSize: 3};
        const dlTableAry = RUBIN_RELATED_BANDS.map((band) => makeRubinBandDatalinkTable({band, withCutout: false}));
        const imageEntries = RUBIN_RELATED_BANDS.map((band, idx) => makeRubinBandImageEntry(band, idx));
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockResolvedValueOnce(dlTableAry);
        makeMenuEntry.mockImplementation(({idx}) => imageEntries[idx]);
        getPreferCutout.mockReturnValueOnce(false);
        createRelatedGridImagesActivate.mockReturnValueOnce('activate-grid');
        createSingleImageExtraction.mockReturnValueOnce('extraction');

        const result = await getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps: undefined, options});

        expect(makeMenuEntry.mock.calls.map(([args]) => [args.dlTableUrl, args.dlData.url, args.sourceRow]))
            .toEqual(RUBIN_RELATED_BANDS.map((band, idx) => [
                `https://data.lsst.cloud/api/datalink/links?ID=visit-2024110900212-${band}`,
                `https://storage.googleapis.com/visit_image_${band}.fits`, idx]));
        const firstEntryArgs = makeMenuEntry.mock.calls[0][0];
        expect(firstEntryArgs).toMatchObject({sourceTable: RUBIN_FILTERED_TABLE, doFileAnalysis: false, options, idx: 0});
        expect(firstEntryArgs.dropDownText).toBe(firstEntryArgs.name);

        const requestAry = imageEntries.map(({request}) => request);
        expect(requestAry.map((request) => request.getPlotId()))
            .toEqual(['band-plot-0-related_grid-0', 'band-plot-1-related_grid-1', 'band-plot-2-related_grid-2']);
        expect(createRelatedGridImagesActivate).toHaveBeenCalledWith({
            requestAry, threeColorReqAry: undefined, imageViewerId: 'viewer-test', tbl_id: 'rubin-visit-images',
            onActivePvChanged: undefined, highlightPlotId: 'plot-highlight',
        });
        expect(createSingleImageExtraction).toHaveBeenCalledWith(requestAry);
        expect(sortRelatedGridUsingRequest).not.toHaveBeenCalled();
        expect(result).toMatchObject({
            displayType: DPtypes.IMAGE, name: 'image grid', menuKey: 'image-grid-0',
            activate: 'activate-grid', extraction: 'extraction', enableCutout: false, gridForceRowSize: 3,
        });
        expect(result.dlData).toMatchObject(
            {url: 'https://storage.googleapis.com/visit_image_g.fits', cutoutToFullWarning: undefined});
    });

    test('getObsCoreRelatedDataProductByFilter: relatedGridImageOrder sorts the grid images', async () => {
        const options = {relatedGridImageOrder: ['i', 'r', 'g']};
        const imageEntries = RUBIN_RELATED_BANDS.map((band, idx) => makeRubinBandImageEntry(band, idx));
        const sortedReqAry = [...imageEntries].reverse().map(({request}) => request);
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockResolvedValueOnce(
            RUBIN_RELATED_BANDS.map((band) => makeRubinBandDatalinkTable({band, withCutout: false})));
        makeMenuEntry.mockImplementation(({idx}) => imageEntries[idx]);
        sortRelatedGridUsingRequest.mockReturnValueOnce(sortedReqAry);

        await getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps: undefined, options});

        expect(sortRelatedGridUsingRequest).toHaveBeenCalledWith(imageEntries.map(({request}) => request), ['i', 'r', 'g']);
        const activateArgs = createRelatedGridImagesActivate.mock.calls[0][0];
        expect(activateArgs.requestAry).toBe(sortedReqAry);
        expect(activateArgs.onActivePvChanged).toEqual(expect.any(Function));
        expect(createSingleImageExtraction).toHaveBeenCalledWith(sortedReqAry);
    });

    test('getObsCoreRelatedDataProductByFilter: three color requests are copies of the chosen grid images', async () => {
        const imageEntries = RUBIN_RELATED_BANDS.map((band, idx) => makeRubinBandImageEntry(band, idx));
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockResolvedValueOnce(
            RUBIN_RELATED_BANDS.map((band) => makeRubinBandDatalinkTable({band, withCutout: false})));
        makeMenuEntry.mockImplementation(({idx}) => imageEntries[idx]);

        // the keys of the three color description: red is i, green is r, blue is g
        await getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps: ['2', '1', '0'], options: {}});

        const {threeColorReqAry} = createRelatedGridImagesActivate.mock.calls[0][0];
        expect(threeColorReqAry.map((request) => request.getTitle())).toEqual(['i', 'r', 'g']);
        expect(threeColorReqAry.map((request) => request.getPlotId()))
            .toEqual(['3id_rubin-visit-images', '3id_rubin-visit-images', '3id_rubin-visit-images']);
    });

    test('getObsCoreRelatedDataProductByFilter: with cutouts preferred the grid uses each band\'s paired cutout', async () => {
        const imageEntries = RUBIN_RELATED_BANDS.map((band, idx) => makeRubinBandImageEntry(band, idx));
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockResolvedValueOnce(
            RUBIN_RELATED_BANDS.map((band) => makeRubinBandDatalinkTable({band, withCutout: true})));
        makeMenuEntry.mockImplementation(({idx}) => imageEntries[idx]);
        getPreferCutout.mockReturnValueOnce(true);
        getCutoutSizeWarning.mockReturnValue('Warning: full image is 105.8 MB');
        getCutoutTotalWarning.mockReturnValueOnce('Warning: 3 full images');

        const result = await getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps: undefined, options: {}});

        const menuDlData = makeMenuEntry.mock.calls.map(([args]) => args.dlData);
        expect(menuDlData.map(({semantics, serviceDefRef}) => [semantics, serviceDefRef]))
            .toEqual([['#cutout', 'cutout-sync'], ['#cutout', 'cutout-sync'], ['#cutout', 'cutout-sync']]);
        expect(menuDlData[0].cutoutToFullWarning).toBe('Warning: full image is 105.8 MB');
        expect(getCutoutTotalWarning).toHaveBeenCalledWith(expect.any(Array), 3);
        expect(result.dlData.cutoutToFullWarning).toBe('Warning: 3 full images');
        expect(result.enableCutout).toBe(true);
    });

    test('getObsCoreRelatedDataProductByFilter: no related rows returns a message', async () => {
        const emptyFilteredTable = {...RUBIN_FILTERED_TABLE, totalRows: 0, tableData: {columns: RUBIN_RELATED_COLUMNS, data: []}};
        doFetchTable.mockResolvedValueOnce(emptyFilteredTable);
        fetchAllDatalinkTables.mockResolvedValueOnce([]);

        const result = await getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps: undefined, options: {}});

        expect(result.message).toBe('no support for related grid for this table');
        expect(createRelatedGridImagesActivate).not.toHaveBeenCalled();
    });

    test('getObsCoreRelatedDataProductByFilter: a related row with no #this image is left out of the grid', async () => {
        // band r only has a preview, so the grid is g and i, each built from its own filtered table row
        const previewOnlyTable = makeRubinBandDatalinkTable({band: 'r', withCutout: false});
        previewOnlyTable.tableData = {...previewOnlyTable.tableData,
            data: [[previewOnlyTable.tableData.data[0][0], 'https://storage.googleapis.com/visit_image_r.png', '', '',
                'Preview image', '#preview', 'image/png', '']]};
        const dlTableAry = [makeRubinBandDatalinkTable({band: 'g', withCutout: false}), previewOnlyTable,
            makeRubinBandDatalinkTable({band: 'i', withCutout: false})];
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockResolvedValueOnce(dlTableAry);
        makeMenuEntry.mockImplementation(({idx}) => makeRubinBandImageEntry(RUBIN_RELATED_BANDS[idx], idx));

        await getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps: undefined, options: {}});

        expect(makeMenuEntry.mock.calls.map(([args]) => [args.idx, args.sourceRow, args.dlTableUrl, args.dlData.url]))
            .toEqual([
                [0, 0, 'https://data.lsst.cloud/api/datalink/links?ID=visit-2024110900212-g', 'https://storage.googleapis.com/visit_image_g.fits'],
                [2, 2, 'https://data.lsst.cloud/api/datalink/links?ID=visit-2024110900212-i', 'https://storage.googleapis.com/visit_image_i.fits'],
            ]);
        const {requestAry} = createRelatedGridImagesActivate.mock.calls[0][0];
        expect(requestAry.map((request) => request.getTitle())).toEqual(['g', 'i']);
    });

    test('getObsCoreRelatedDataProductByFilter: related rows with no #this image at all returns a message', async () => {
        const noImageTables = RUBIN_RELATED_BANDS.map((band) => {
            const dlTable = makeRubinBandDatalinkTable({band, withCutout: false});
            dlTable.tableData = {...dlTable.tableData,
                data: dlTable.tableData.data.map((row) => row.map((value) => value === 'application/fits' ? 'text/plain' : value))};
            return dlTable;
        });
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockResolvedValueOnce(noImageTables);

        const result = await getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps: undefined, options: {}});

        expect(result.message).toBe('no support for related grid for this table');
        expect(makeMenuEntry).not.toHaveBeenCalled();
    });

    test('getObsCoreRelatedDataProductByFilter: a failure returns a message', async () => {
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockRejectedValueOnce(new Error('network down'));

        const result = await getObsCoreRelatedDataProductByFilterForRubinRow({threeColorOps: undefined, options: {}});

        expect(result.message).toBe('can\'t filter table');
        expect(createRelatedGridImagesActivate).not.toHaveBeenCalled();
    });


    test('createGridResult: each row\'s image becomes one cell of the grid', async () => {
        const firstImage = makeTileBandEntry({tile: '85951', band: 'NIR-H', idx: 0, withBandpass: false});
        const secondImage = makeTileBandEntry({tile: '85952', band: 'NIR-H', idx: 0, withBandpass: false});
        const noDataMessage = {displayType: DPtypes.MESSAGE, message: 'No data available for this row', menuKey: 'message-0'};
        const plotRows = [{row: 0, highlight: true}, {row: 1, highlight: false}, {row: 2, highlight: false}];
        getCurrentActiveKeyID.mockReturnValueOnce('last-source');
        getActiveMenuKey.mockReturnValueOnce('dlt-0');
        createGridImagesActivate.mockReturnValueOnce('activate-grid');
        createSingleImageExtraction.mockReturnValueOnce('extraction');

        const result = await createGridResult(
            [Promise.resolve(firstImage), Promise.resolve(noDataMessage), Promise.resolve(secondImage)],
            GRID_ACTIVATE_PARAMS, GRID_TABLE, plotRows, {});

        expect(getActiveMenuKey).toHaveBeenCalledWith('dpId-test', 'last-source');
        expect(createGridImagesActivate).toHaveBeenCalledWith(
            [firstImage.request, secondImage.request], 'viewer-test', 'euclid-mer-tiles', plotRows);
        expect(createSingleImageExtraction).toHaveBeenCalledWith([firstImage.request, secondImage.request]);
        expect(result).toMatchObject({
            displayType: DPtypes.IMAGE, name: 'image grid', activate: 'activate-grid', extraction: 'extraction', menuKey: 'dlt-0'});
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });

    test('createGridResult: hasRelatedBands needs more than one grid image in every row', async () => {
        useMenuKeyHelperCopies();
        const twoBandTile = makeTileProduct({tile: '85951', bands: GRID_BANDS, withBandpass: true});
        const oneBandTile = makeTileProduct({tile: '85952', bands: ['NIR-H'], withBandpass: true});

        const result = await createGridResult([Promise.resolve(twoBandTile), Promise.resolve(oneBandTile)],
            GRID_ACTIVATE_PARAMS, GRID_TABLE, GRID_PLOT_ROWS, {hasRelatedBands: true});

        expect(result.name).toBe('image grid');
        expect(result.menu).toBeUndefined();
        expect(createGridImagesActivate).toHaveBeenCalledTimes(1);
    });

    test('createGridResult: hasRelatedBands needs the bandpass_name extension on every entry', async () => {
        useMenuKeyHelperCopies();
        const tiles = ['85951', '85952'].map((tile) => makeTileProduct({tile, bands: GRID_BANDS, withBandpass: false}));

        const result = await createGridResult(tiles.map((tile) => Promise.resolve(tile)),
            GRID_ACTIVATE_PARAMS, GRID_TABLE, GRID_PLOT_ROWS, {hasRelatedBands: true});

        expect(result.menu).toBeUndefined();
        expect(createGridImagesActivate).toHaveBeenCalledTimes(1);
    });

    test('createGridResult: with related bands each band becomes a grid across the rows', async () => {
        useMenuKeyHelperCopies();
        const tiles = ['85951', '85952'].map((tile) => makeTileProduct({tile, bands: GRID_BANDS, withBandpass: true}));
        getActiveMenuKey.mockReturnValueOnce(makeBandLabelMenuKeyCopy('NIR-J', 'NIR-J', 'dlt-1'));
        createGridImagesActivate.mockImplementation((requestAry) => `activate-${requestAry[0].getTitle()}`);

        const result = await createGridResult(tiles.map((tile) => Promise.resolve(tile)),
            GRID_ACTIVATE_PARAMS, GRID_TABLE, GRID_PLOT_ROWS, {hasRelatedBands: true});

        // the first call is the plain grid, then one grid per band, each with that band from every tile
        expect(createGridImagesActivate.mock.calls.slice(1).map(([requestAry]) => requestAry.map((r) => r.getTitle())))
            .toEqual([['85951 NIR-H', '85952 NIR-H'], ['85951 NIR-J', '85952 NIR-J'], ['85951 NIR-Y', '85952 NIR-Y']]);
        expect(result.menu.map(({menuKey, name}) => [menuKey, name])).toEqual([
            ['--__LABEL:NIR-H--__BAND:NIR-H--__grid-item0', 'NIR-H image'],
            ['--__LABEL:NIR-J--__BAND:NIR-J--__grid-item1', 'NIR-J image'],
            ['--__LABEL:NIR-Y--__BAND:NIR-Y--__grid-item2', 'NIR-Y image'],
        ]);
        // the band that was active before stays active
        expect(result).toMatchObject({menuKey: '--__LABEL:NIR-J--__BAND:NIR-J--__grid-item1',
            activate: 'activate-85951 NIR-J', activeMenuLookupKey: 'euclid-mer-tiles'});
        expect(dispatchUpdateActiveKey).toHaveBeenCalledWith(
            {dpId: 'dpId-test', activeMenuKeyChanges: {'euclid-mer-tiles': '--__LABEL:NIR-J--__BAND:NIR-J--__grid-item1'}});
    });

    test('createGridResult: with related bands and no matching active band the first band is active', async () => {
        useMenuKeyHelperCopies();
        const tiles = ['85951', '85952'].map((tile) => makeTileProduct({tile, bands: GRID_BANDS, withBandpass: true}));
        getActiveMenuKey.mockReturnValueOnce('image-grid-0');

        const result = await createGridResult(tiles.map((tile) => Promise.resolve(tile)),
            GRID_ACTIVATE_PARAMS, GRID_TABLE, GRID_PLOT_ROWS, {hasRelatedBands: true});

        expect(result.menuKey).toBe('--__LABEL:NIR-H--__BAND:NIR-H--__grid-item0');
        expect(dispatchUpdateActiveKey).toHaveBeenCalledWith(
            {dpId: 'dpId-test', activeMenuKeyChanges: {'euclid-mer-tiles': '--__LABEL:NIR-H--__BAND:NIR-H--__grid-item0'}});
    });

    test('createGridResult: a DataLink error row does not stop the related band grids', async () => {
        useMenuKeyHelperCopies();
        const tiles = ['85951', '85952'].map((tile) => makeTileProduct({tile, bands: GRID_BANDS, withBandpass: true}));
        // a failed link comes back from processDatalinkTable as a message entry with no dlData
        tiles[0].menu.push({displayType: DPtypes.MESSAGE, message: 'cutout failed', menuKey: 'dlt-3',
            name: 'Error in related data (datalink) row 3'});
        getActiveMenuKey.mockReturnValueOnce(makeBandLabelMenuKeyCopy('NIR-H', 'NIR-H', 'dlt-0'));

        const result = await createGridResult(tiles.map((tile) => Promise.resolve(tile)),
            GRID_ACTIVATE_PARAMS, GRID_TABLE, GRID_PLOT_ROWS, {hasRelatedBands: true});

        expect(result.menu.map(({menuKey}) => menuKey)).toEqual([
            '--__LABEL:NIR-H--__BAND:NIR-H--__grid-item0',
            '--__LABEL:NIR-J--__BAND:NIR-J--__grid-item1',
            '--__LABEL:NIR-Y--__BAND:NIR-Y--__grid-item2',
        ]);
    });

    test('createGridResult: with related bands and no active key yet the first band is active', async () => {
        useMenuKeyHelperCopies();
        const tiles = ['85951', '85952'].map((tile) => makeTileProduct({tile, bands: GRID_BANDS, withBandpass: true}));
        getActiveMenuKey.mockReturnValueOnce(undefined); // nothing stored yet the first time the grid is shown

        const result = await createGridResult(tiles.map((tile) => Promise.resolve(tile)),
            GRID_ACTIVATE_PARAMS, GRID_TABLE, GRID_PLOT_ROWS, {hasRelatedBands: true});

        expect(result.menuKey).toBe('--__LABEL:NIR-H--__BAND:NIR-H--__grid-item0');
    });

    test('createGridResult: with related bands but no highlighted row the plain grid is returned', async () => {
        useMenuKeyHelperCopies();
        const tiles = ['85951', '85952'].map((tile) => makeTileProduct({tile, bands: GRID_BANDS, withBandpass: true}));
        const plotRows = [{row: 0, highlight: false}, {row: 1, highlight: false}];

        const result = await createGridResult(tiles.map((tile) => Promise.resolve(tile)),
            GRID_ACTIVATE_PARAMS, GRID_TABLE, plotRows, {hasRelatedBands: true});

        expect(result.menu).toBeUndefined();
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });


    test('datalinkDescribeThreeColor: the bands come from the DataLink related grid', async () => {
        const datalinkTable = makeEuclidDatalinkTable({idPerBand: false});
        const options = {datalinkTblRequestOptions: {pageSize: 100}};
        fetchDatalinkTable.mockResolvedValueOnce(datalinkTable);
        processDatalinkTable.mockReturnValueOnce({menu: [
            makeColorBandEntry({title: 'NIR-Y', bandFlags: {bBand: true}}),
            makeColorBandEntry({title: 'NIR-J', bandFlags: {gBand: true}}),
            makeColorBandEntry({title: 'NIR-H', bandFlags: {rBand: true}}),
        ]});

        const result = await datalinkDescribeThreeColorForEuclidTile(options);

        expect(fetchDatalinkTable).toHaveBeenCalledWith(EUCLID_DATALINK_URL, {pageSize: 100});
        expect(processDatalinkTable).toHaveBeenCalledWith({
            sourceTable: EUCLID_MER_TABLE, row: 0, datalinkTable, activateParams: {}, dlTableUrl: EUCLID_DATALINK_URL,
            doFileAnalysis: false, options, parsingAlgorithm: RELATED_IMAGE_GRID,
        });
        expect(result).toEqual({
            0: {title: 'NIR-Y', color: Band.BLUE},
            1: {title: 'NIR-J', color: Band.GREEN},
            2: {title: 'NIR-H', color: Band.RED},
        });
    });

    test('datalinkDescribeThreeColor: without band flags the first three images are red, green and blue', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(makeEuclidDatalinkTable({idPerBand: false}));
        processDatalinkTable.mockReturnValueOnce({menu: EUCLID_BANDS.map((title) => makeColorBandEntry({title, bandFlags: {}}))});

        const result = await datalinkDescribeThreeColorForEuclidTile({});

        expect(result).toEqual({
            0: {title: 'NIR-H', color: Band.RED},
            1: {title: 'NIR-J', color: Band.GREEN},
            2: {title: 'NIR-Y', color: Band.BLUE},
            3: {title: 'VIS', color: undefined},
        });
    });

    test('datalinkDescribeThreeColor: a flagged band keeps its color and the others fill in around it', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(makeEuclidDatalinkTable({idPerBand: false}));
        processDatalinkTable.mockReturnValueOnce({menu: [
            makeColorBandEntry({title: 'NIR-H', bandFlags: {}}),
            makeColorBandEntry({title: 'NIR-J', bandFlags: {}}),
            makeColorBandEntry({title: 'NIR-Y', bandFlags: {rBand: true}}),
        ]});

        const result = await datalinkDescribeThreeColorForEuclidTile({});

        expect(result).toEqual({
            0: {title: 'NIR-H', color: Band.GREEN},
            1: {title: 'NIR-J', color: Band.BLUE},
            2: {title: 'NIR-Y', color: Band.RED},
        });
    });

    test('datalinkDescribeThreeColor: with two images there is no blue', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(makeEuclidDatalinkTable({idPerBand: false}));
        processDatalinkTable.mockReturnValueOnce({menu: [
            makeColorBandEntry({title: 'NIR-H', bandFlags: {}}),
            makeColorBandEntry({title: 'VIS', bandFlags: {}}),
        ]});

        const result = await datalinkDescribeThreeColorForEuclidTile({});

        expect(result).toEqual({0: {title: 'NIR-H', color: Band.RED}, 1: {title: 'VIS', color: Band.GREEN}});
    });

    test('datalinkDescribeThreeColor: GROUP_BY_RELATED_COLUMNS takes the bands from the related rows', async () => {
        const options = {relatedBandMethod: GROUP_BY_RELATED_COLUMNS, datalinkTblRequestOptions: {pageSize: 100},
            dataProductsComponentKey: 'dp-key'};
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockResolvedValueOnce(
            RUBIN_RELATED_BANDS.map((band) => makeRubinBandDatalinkTable({band, withCutout: false})));
        makeMenuEntry.mockImplementation(({idx}) => makeColorBandEntry({title: RUBIN_RELATED_BANDS[idx], bandFlags: {}}));
        getPreferCutout.mockReturnValueOnce(false);

        const result = await datalinkDescribeThreeColor('unused-for-related-columns', RUBIN_RELATED_TABLE, 0, options);

        expect(doFetchTable.mock.calls[0][0].filters).toBe('"lsst_visit" = \'2024110900212\' AND "lsst_detector" = \'0\'');
        expect(fetchAllDatalinkTables).toHaveBeenCalledWith(RUBIN_FILTERED_TABLE, {pageSize: 100});
        expect(getPreferCutout).toHaveBeenCalledWith('dp-key', 'rubin-visit-images');
        expect(fetchDatalinkTable).not.toHaveBeenCalled();
        expect(result).toEqual({
            0: {title: 'g', color: Band.RED},
            1: {title: 'r', color: Band.GREEN},
            2: {title: 'i', color: Band.BLUE},
        });
    });

    test('datalinkDescribeThreeColor: a DataLink error row is left out of the bands', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(makeEuclidDatalinkTable({idPerBand: false}));
        // a failed link comes back from processDatalinkTable as a message entry with no dlData
        processDatalinkTable.mockReturnValueOnce({menu: [
            {displayType: DPtypes.MESSAGE, message: 'cutout failed', menuKey: 'dlt-0'},
            makeColorBandEntry({title: 'NIR-H', bandFlags: {}}),
            makeColorBandEntry({title: 'NIR-J', bandFlags: {}}),
            makeColorBandEntry({title: 'NIR-Y', bandFlags: {}}),
        ]});

        const result = await datalinkDescribeThreeColorForEuclidTile({});

        expect(result).toEqual({
            0: {title: 'NIR-H', color: Band.RED},
            1: {title: 'NIR-J', color: Band.GREEN},
            2: {title: 'NIR-Y', color: Band.BLUE},
        });
    });

    test('datalinkDescribeThreeColor: an entry that is not an image does not shift the band colors', async () => {
        fetchDatalinkTable.mockResolvedValueOnce(makeEuclidDatalinkTable({idPerBand: false}));
        processDatalinkTable.mockReturnValueOnce({menu: [
            {displayType: DPtypes.DOWNLOAD, menuKey: 'dlt-0', url: 'https://irsadev.ipac.caltech.edu/ibe/data/tile.tar',
                dlData: {dlAnalysis: {rBand: false, gBand: false, bBand: false}}},
            makeColorBandEntry({title: 'NIR-H', bandFlags: {}}),
            makeColorBandEntry({title: 'NIR-J', bandFlags: {}}),
            makeColorBandEntry({title: 'NIR-Y', bandFlags: {rBand: true}}),
        ]});

        const result = await datalinkDescribeThreeColorForEuclidTile({});

        expect(result).toEqual({
            0: {title: 'NIR-H', color: Band.GREEN},
            1: {title: 'NIR-J', color: Band.BLUE},
            2: {title: 'NIR-Y', color: Band.RED},
        });
    });

    test('datalinkDescribeThreeColor: GROUP_BY_RELATED_COLUMNS with no images describes no bands', async () => {
        doFetchTable.mockResolvedValueOnce(RUBIN_FILTERED_TABLE);
        fetchAllDatalinkTables.mockResolvedValueOnce([]);

        const result = await datalinkDescribeThreeColor('unused-for-related-columns', RUBIN_RELATED_TABLE, 0,
            {relatedBandMethod: GROUP_BY_RELATED_COLUMNS});

        expect(result).toEqual({});
    });


    test('makeDlUrl: no service descriptor gives no URL', () => {
        expect(makeDlUrl(undefined, ZTF_TABLE, 0)).toBeUndefined();
    });

    test('makeDlUrl: row values and fixed values are added to the accessURL query', () => {
        const [ztfDescriptor] = getServiceDescriptors(ZTF_TABLE);

        expect(makeDlUrl(ztfDescriptor, ZTF_TABLE, 1)).toBe(`${ZTF_LIGHT_CURVE_URL}&id=1684211300162373&bad_catflags_mask=0`);
        expect(makeDlUrl(ztfDescriptor, ZTF_TABLE, 0)).toBe(`${ZTF_LIGHT_CURVE_URL}&id=686203400167748&bad_catflags_mask=0`);
    });

    test('makeDlUrl: a ref without a column name is looked up by column ID', () => {
        const descriptor = {accessURL: 'https://irsa.ipac.caltech.edu/cgi-bin/ZTF/nph_light_curves',
            serDefParams: [{name: 'id', ref: 'col_0'}]};

        expect(makeDlUrl(descriptor, ZTF_TABLE, 1))
            .toBe('https://irsa.ipac.caltech.edu/cgi-bin/ZTF/nph_light_curves?id=1684211300162373');
    });

    test('makeDlUrl: values are URL encoded', () => {
        const [cadcDescriptor] = getServiceDescriptors({
            tbl_id: 'cadc-omm-obscore', totalRows: 0, tableMeta: {}, request: {},
            tableData: {columns: [{name: 'obs_id', type: 'char'}], data: []},
            resources: [{
                type: 'meta', utype: 'adhoc:service', ID: 'cadc-datalink',
                params: [
                    {name: 'standardID', value: 'ivo://ivoa.net/std/DataLink#links-1.1'},
                    {name: 'accessURL', value: 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink'},
                ],
                groups: [{name: 'inputParams',
                    params: [{name: 'ID', UCD: 'meta.id;meta.main', value: 'ivo://cadc.nrc.ca/OMM?C140117_0290/C140117_0290_SCI'}]}],
            }],
        });

        expect(makeDlUrl(cadcDescriptor, undefined, 0)).toBe(
            'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink?ID=ivo%3A%2F%2Fcadc.nrc.ca%2FOMM%3FC140117_0290%2FC140117_0290_SCI');
    });

    test('makeDlUrl: a descriptor with no input params is just the accessURL', () => {
        expect(makeDlUrl({accessURL: ZTF_LIGHT_CURVE_URL, serDefParams: undefined}, ZTF_TABLE, 0)).toBe(ZTF_LIGHT_CURVE_URL);
    });

    test('makeDlUrl: a ref to a column the table does not have is left out', () => {
        const descriptor = {accessURL: ZTF_LIGHT_CURVE_URL,
            serDefParams: [{name: 'id', ref: 'col_missing'}, {name: 'bad_catflags_mask', value: 0}]};

        expect(makeDlUrl(descriptor, ZTF_TABLE, 0)).toBe(`${ZTF_LIGHT_CURVE_URL}&bad_catflags_mask=0`);
    });

    test('makeDlUrl: a null fixed value is left out', () => {
        const descriptor = {accessURL: ZTF_LIGHT_CURVE_URL, serDefParams: [{name: 'RELEASE', value: null}]};

        expect(makeDlUrl(descriptor, ZTF_TABLE, 0)).toBe(ZTF_LIGHT_CURVE_URL);
    });

    test('makeDlUrl: an accessURL that is not an absolute URL gives no URL', () => {
        expect(makeDlUrl({accessURL: '/datalink', serDefParams: []}, ZTF_TABLE, 0)).toBeUndefined();
    });
});
