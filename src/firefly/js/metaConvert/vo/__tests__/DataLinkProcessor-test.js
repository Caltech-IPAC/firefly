import {TableDataType} from '../../../data/FileAnalysis.js';
import {getPreferCutout} from '../../../ui/tap/Cutout.js';
import {getDataLinkData} from '../../../voAnalyzer/VoDataLinkServDef.js';
import {
    doFileNameAndTypeAnalysis, isNonServerAnalysisType, isUsableDownloadType, makeAnalysisActivateFunc, makeDownloadType
} from '../../AnalysisUtils.js';
import {dispatchUpdateActiveKey, getActiveMenuKey, getCurrentActiveKeyID} from '../../DataProductsCntlr.js';
import {DPtypes} from '../../DataProductsType.js';
import {
    createChartTableActivate, createTableActivate, createTableExtraction, makeMultiTableActivate, makeMultiTableExtraction
} from '../../TableDataProductUtils.js';
import {
    createDataLinkSingleRowItem, createGuessDataType, filterDLList, findMenuKeyWithName, getCutoutSizeWarning,
    getCutoutTotalWarning, hasBandAndLabelInMenuKey, hasBandInMenuKey, hasError, hasLabelInMenuKey, IMAGE, isWarnSize,
    makeBandLabelMenuKey, makeMenuEntry, menuKeysMatchBandLabel, processDatalinkTable, RELATED_IMAGE_GRID,
    sortRelatedGridUsingRequest, SPECTRUM, USE_ALL
} from '../DataLinkProcessor.js';
import {makeServiceDefDataProduct} from '../ServDescProducts.js';

// Loading these modules pulls in UI components that Jest can't load (validator/es, canvas), so they are
// replaced with stubs; each test sets the return values it needs.
jest.mock('../ServDescProducts.js', () => ({
    makeServiceDefDataProduct: jest.fn(),
}));
jest.mock('../../AnalysisUtils.js', () => ({
    doFileNameAndTypeAnalysis: jest.fn(),
    isNonServerAnalysisType: jest.fn(),
    isUsableDownloadType: jest.fn(),
    makeAnalysisActivateFunc: jest.fn(),
    makeDownloadType: jest.fn(),
}));
jest.mock('../../TableDataProductUtils.js', () => ({
    createChartTableActivate: jest.fn(),
    createTableActivate: jest.fn(),
    createTableExtraction: jest.fn(),
    makeMultiTableActivate: jest.fn(),
    makeMultiTableExtraction: jest.fn(),
}));
// keep the real modules, but don't use the store
jest.mock('../../DataProductsCntlr.js', () => ({
    ...jest.requireActual('../../DataProductsCntlr.js'),
    dispatchUpdateActiveKey: jest.fn(),
    getActiveMenuKey: jest.fn(),
    getCurrentActiveKeyID: jest.fn(),
}));
jest.mock('../../../ui/tap/Cutout.js', () => ({
    ...jest.requireActual('../../../ui/tap/Cutout.js'),
    getPreferCutout: jest.fn(),
}));

// cadc-datalink-row-86.vot: the DataLink result for CADC OMM observation C140117_0290
const OMM_ID = 'ivo://cadc.nrc.ca/OMM?C140117_0290/C140117_0290_SCI';
const OMM_FILE_URL = 'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/C140117_0290_SCI.fits';
const OMM_DATALINK_URL = 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink?ID=ivo%3A%2F%2Fcadc.nrc.ca%2FOMM%3FC140117_0290%2FC140117_0290_SCI';
const SODA_SYNC_ID = 'soda-7447b7fc-cba5-4e19-b0a5-e7d6ea7528c2';
const SODA_ASYNC_ID = 'soda-d233879a-470d-4273-b662-ff4cf8585c49';
const DATALINK_COLUMNS = ['ID', 'access_url', 'service_def', 'error_message', 'semantics', 'local_semantics',
    'description', 'content_type', 'content_length'].map((name) => ({name, type: 'char'}));
const OMM_DATALINK_ROWS = [
    [OMM_ID, 'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/C140117_0290_SCI_prev.jpg', '', '', '#preview', '',
        'download cadc:OMM/C140117_0290_SCI_prev.jpg', 'image/jpeg', '539277'],
    [OMM_ID, OMM_FILE_URL, '', '', '#this', '', 'download cadc:OMM/C140117_0290_SCI.fits', 'application/fits', '16790400'],
    [OMM_ID, '', SODA_SYNC_ID, '', '#cutout', '', 'SODA-sync cutout of cadc:OMM/C140117_0290_SCI.fits', 'application/fits', ''],
    [OMM_ID, '', SODA_ASYNC_ID, '', '#cutout', '', 'SODA-async cutout of cadc:OMM/C140117_0290_SCI.fits', 'application/fits', ''],
    [OMM_ID, 'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/C140117_0290_SCI_prev_256.jpg', '', '', '#thumbnail', '',
        'download cadc:OMM/C140117_0290_SCI_prev_256.jpg', 'image/jpeg', '3715'],
];

/**
 * Make a CADC SODA service descriptor resource.
 * @param {string} ID
 * @param {string} standardID
 * @param {string} accessURL
 * @return {Object}
 */
function makeSodaResource(ID, standardID, accessURL) {
    return {
        type: 'meta', utype: 'adhoc:service', ID,
        params: [{name: 'standardID', value: standardID}, {name: 'accessURL', value: accessURL}],
        groups: [{name: 'inputParams', params: [{name: 'ID', UCD: 'meta.id;meta.dataset', value: 'cadc:OMM/C140117_0290_SCI.fits'}]}],
    };
}

/**
 * Build a DataLink links table with the CADC OMM descriptors.
 * @param {Array.<Array>} rows
 * @return {TableModel}
 */
function makeOmmDatalinkTable(rows) {
    return {
        tbl_id: 'cadc-omm-datalink', totalRows: rows.length, tableMeta: {}, request: {},
        tableData: {columns: DATALINK_COLUMNS, data: rows},
        resources: [
            makeSodaResource(SODA_SYNC_ID, 'ivo://ivoa.net/std/SODA#sync-1.0', 'https://ws-uv.canfar.net/caom2ops/sync'),
            makeSodaResource(SODA_ASYNC_ID, 'ivo://ivoa.net/std/SODA#async-1.0', 'https://ws-uv.canfar.net/caom2ops/async'),
        ],
    };
}

// the CADC OMM ObsCore row the DataLink result belongs to
const OMM_OBSCORE_TABLE = {
    tbl_id: 'cadc-omm-obscore', highlightedRow: 0, totalRows: 1, tableMeta: {}, request: {},
    tableData: {
        columns: [{name: 'obs_collection', type: 'char'}, {name: 'dataproduct_type', type: 'char'},
            {name: 'obs_id', type: 'char'}, {name: 'access_url', type: 'char'}, {name: 'access_format', type: 'char'}],
        data: [['OMM', 'image', 'C140117_0290', OMM_DATALINK_URL, 'application/x-votable+xml;content=datalink']],
    },
};

/**
 * Get one usable row of the CADC OMM DataLink result, as getDataLinkData analyzes it.
 * @param {string} semantics
 * @return {DatalinkData}
 */
function getOmmDlData(semantics) {
    return getDataLinkData(makeOmmDatalinkTable(OMM_DATALINK_ROWS), false).find((dlData) => dlData.semantics === semantics);
}

/**
 * Call processDatalinkTable for the CADC OMM row.
 * @param {Object} p
 * @param {TableModel} p.datalinkTable
 * @param {Object} p.options
 * @param {Object} [p.extra] - other processDatalinkTable arguments
 * @return {DataProductsDisplayType}
 */
function processDatalinkTableForOmmRow({datalinkTable, options, extra}) {
    return processDatalinkTable({
        sourceTable: OMM_OBSCORE_TABLE, row: 0, datalinkTable, activateParams: {dpId: 'dpId-test'},
        dlTableUrl: OMM_DATALINK_URL, options, ...extra,
    });
}

/**
 * Stand in for makeServiceDefDataProduct: a product named after its menu key that keeps what it was given.
 */
function useServiceDefProductStandIn() {
    makeServiceDefDataProduct.mockImplementation(({menuKey, serDef, dlData}) =>
        ({displayType: DPtypes.ANALYZE, name: `service descriptor ${menuKey}`, menuKey, serDef, dlData}));
}

// SPHEREx DR1 deep image 2025W17_4B_0021_1/D1 (spherex-datalink-products-row-1.vot), trimmed: the #this image,
// its #cutout, two of its FITS calibration files, the YAML gain factors and one ECSV crosstalk table
const SPHEREX_ID = 'ivo://irsa.ipac/spherex_dr1_deep?2025W17_4B_0021_1/D1';
const SPHEREX_DATA_URL = 'https://irsa.ipac.caltech.edu/ibe/data/spherex/dr1';
const SPHEREX_DATALINK_URL = 'https://irsa.ipac.caltech.edu/datalink/links/spherex?ID=ivo%3A%2F%2Firsa.ipac%2Fspherex_dr1_deep%3F2025W17_4B_0021_1%2FD1';
const SPHEREX_DATALINK_TABLE = {
    tbl_id: 'spherex-datalink', totalRows: 6, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', ID: 'ibe-cutout', desc: 'Link to generate an image cutout',
        params: [{name: 'accessURL', value: 'https://irsa.ipac.caltech.edu/ibe/cutout'}],
        groups: [{name: 'inputParams', params: [
            {name: 'ra', UCD: 'pos.eq.ra;meta.main', ref: 'col_13'},
            {name: 'dec', UCD: 'pos.eq.dec;meta.main', ref: 'col_14'},
            {name: 'size', UCD: 'phys.size', value: '0.01'},
            {name: 'path', ref: 'col_12'},
        ]}],
    }],
    tableData: {
        columns: ['id', 'access_url', 'service_def', 'error_message', 'semantics', 'description', 'content_type',
            'content_length', 'content_qualifier', 'local_semantics', 'link_auth', 'link_authorized', 'path',
            'center_ra', 'center_dec'].map((name, idx) => ({name, type: 'char', ID: `col_${idx}`})),
        data: [
            [SPHEREX_ID, `${SPHEREX_DATA_URL}/level2/2025W17_4B/l2b-v27-2026-196/1/level2_2025W17_4B_0021_1D1_spx_l2b-v27-2026-196.fits`,
                '', '', '#this', `${SPHEREX_ID} science image`, 'image/fits', '66087360', '', '', 'optional', 'T', '', '', ''],
            [SPHEREX_ID, '', 'ibe-cutout', '', '#cutout', `${SPHEREX_ID} science image`, 'image/fits', '', '', '',
                'optional', 'T', 'spherex/dr1/level2/2025W17_4B/l2b-v27-2026-196/1/level2_2025W17_4B_0021_1D1_spx_l2b-v27-2026-196.fits',
                '270.19211155920743', '68.01568985908334'],
            [SPHEREX_ID, `${SPHEREX_DATA_URL}/abs_gain_matrix/cal-agm-v8-2026-191/1/abs_gain_matrix_D1_spx_cal-agm-v8-2026-191.fits`,
                '', '', '#calibration', 'abs_gain_matrix_D1_spx_cal-agm-v8-2026-191', 'application/fits', '16652160', '', '',
                'optional', 'T', '', '', ''],
            [SPHEREX_ID, `${SPHEREX_DATA_URL}/dark/cal-drk-v6-2026-191/1/dark_D1_spx_cal-drk-v6-2026-191.fits`,
                '', '', '#calibration', 'dark_D1_spx_cal-drk-v6-2026-191', 'application/fits', '16652160', '', '',
                'optional', 'T', '', '', ''],
            [SPHEREX_ID, `${SPHEREX_DATA_URL}/gain_factors/base-2026-191/gain_factors_spx_base-2026-191.yaml`,
                '', '', '#calibration', 'gain_factors_spx_base-2026-191', 'application/yaml', '5544', '', '',
                'optional', 'T', '', '', ''],
            [SPHEREX_ID, `${SPHEREX_DATA_URL}/xtalk_class1/base-2026-191/1/xtalk_class1_D1_spx_base-2026-191.ecsv`,
                '', '', '#calibration', 'xtalk_class1_D1_spx_base-2026-191', 'application/ecsv', '1017', '', '',
                'optional', 'T', '', '', ''],
        ],
    },
};

/**
 * Stand in for the client side file type analysis: only YAML is shown in the browser.
 */
function useYamlOnlyTypeAnalysisStandIn() {
    isNonServerAnalysisType.mockImplementation((url, ct) => ct === 'application/yaml');
    doFileNameAndTypeAnalysis.mockImplementation(({url, name}) => ({displayType: DPtypes.TXT, name, url}));
}

// Euclid DR1 tile 101384791 (euclid-datalink-Products-row-1.vot), trimmed to the VIS and NISP Y images: each #this
// image and its #cutout, whose local semantics put it in a related image grid; label and bandpass_name name the band
const EUCLID_DATALINK_URL = 'https://irsa.ipac.caltech.edu/datalink/links/euclid?ID=ivo%3A%2F%2Firsa.ipac%2Feuclid_dr1_deep_DpdMerBksMosaic%3F101384791_798035059';
const EUCLID_VIS_ID = 'ivo://irsa.ipac/euclid_dr1_deep_DpdMerBksMosaic?101384791_798035059_VIS/VIS';
const EUCLID_Y_ID = 'ivo://irsa.ipac/euclid_dr1_deep_DpdMerBksMosaic?101384791_798035059_NISP/Y';
const EUCLID_DATA_URL = 'https://irsacloud.ipac.caltech.edu/ibe/data/euclid/dr1/MER/101384791';
const EUCLID_DATALINK_TABLE = {
    tbl_id: 'euclid-datalink', totalRows: 4, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', ID: 'sd-cutout',
        params: [{name: 'accessURL', value: 'https://irsa.ipac.caltech.edu/ibe/cutout'}],
        groups: [{name: 'inputParams', params: [
            {name: 'size', UCD: 'phys.size', units: 'deg', value: '0.01'},
            {name: 'path', ref: 'col_8'},
        ]}],
    }],
    tableData: {
        columns: ['id', 'access_url', 'service_def', 'error_message', 'semantics', 'local_semantics', 'description',
            'content_type', 'file', 'content_length', 'label', 'bandpass_name']
            .map((name, idx) => ({name, type: 'char', ID: `col_${idx}`})),
        data: [
            [EUCLID_VIS_ID, `${EUCLID_DATA_URL}/VIS/EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791.fits`, '', '', '#this', '',
                'EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791', 'image/fits', '', 1664316480, 'VIS', 'VIS'],
            [EUCLID_VIS_ID, '', 'sd-cutout', '', '#cutout', 'cutout-grid-BLUE', 'EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791',
                'image/fits', 'euclid/dr1/MER/101384791/VIS/EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791.fits', 1664316480,
                'VIS', 'VIS'],
            [EUCLID_Y_ID, `${EUCLID_DATA_URL}/NISP/EUC_MER_BGSUB-MOSAIC-NIR-Y_TILE101384791.fits`, '', '', '#this', '',
                'EUC_MER_BGSUB-MOSAIC-NIR-Y_TILE101384791', 'image/fits', '', 416165760, 'NISP/Y', 'Y'],
            [EUCLID_Y_ID, '', 'sd-cutout', '', '#cutout', 'cutout-grid-GREEN', 'EUC_MER_BGSUB-MOSAIC-NIR-Y_TILE101384791',
                'image/fits', 'euclid/dr1/MER/101384791/NISP/EUC_MER_BGSUB-MOSAIC-NIR-Y_TILE101384791.fits', 416165760,
                'NISP/Y', 'Y'],
        ],
    },
};

// made up: two 1D spectra of one observation, each a #this VOTable in a spectrum grid
const SPECTRA_DATALINK_URL = 'https://example.org/datalink?ID=spectra-1';
const SPECTRA_DATALINK_TABLE = {
    tbl_id: 'spectra-datalink', totalRows: 2, tableMeta: {}, request: {},
    tableData: {
        columns: DATALINK_COLUMNS,
        data: [
            ['spectra-1', 'https://example.org/spectra/blue.xml', '', '', '#this', 'spectrum-grid', 'blue arm spectrum',
                'application/x-votable+xml', '20480'],
            ['spectra-1', 'https://example.org/spectra/red.xml', '', '', '#this', 'spectrum-grid', 'red arm spectrum',
                'application/x-votable+xml', '20480'],
        ],
    },
};

describe('DataLinkProcessor:', () => {

    // clearMocks (jest config) keeps queued mock*Once values, so an unused one would leak into the next test
    beforeEach(() => jest.resetAllMocks());

    test('processDatalinkTable: every usable row becomes a menu entry, #this first', () => {
        const result = processDatalinkTableForOmmRow(
            {datalinkTable: makeOmmDatalinkTable(OMM_DATALINK_ROWS), options: {}, extra: {}});

        // the sync cutout is paired with #this, so it has no entry of its own; the async cutout is not usable
        expect(result.menu.map(({menuKey, displayType}) => [menuKey, displayType]))
            .toEqual([['dlt-1', DPtypes.ANALYZE], ['dlt-0', DPtypes.PNG], ['dlt-4', DPtypes.PNG]]);
        expect(result).toMatchObject({displayType: DPtypes.ANALYZE, menuKey: 'dlt-1', url: OMM_FILE_URL,
            activeMenuLookupKey: OMM_DATALINK_URL});
        expect(result.request.getURL()).toBe(OMM_FILE_URL);
        expect(makeServiceDefDataProduct).not.toHaveBeenCalled();
        expect(dispatchUpdateActiveKey).toHaveBeenCalledWith(
            {dpId: 'dpId-test', activeMenuKeyChanges: {[OMM_DATALINK_URL]: 'dlt-1'}});
    });

    test('processDatalinkTable: with cutouts preferred the #this entry is its paired cutout', () => {
        useServiceDefProductStandIn();
        getPreferCutout.mockReturnValueOnce(true);

        const result = processDatalinkTableForOmmRow(
            {datalinkTable: makeOmmDatalinkTable(OMM_DATALINK_ROWS), options: {dataProductsComponentKey: 'dp-key'}, extra: {}});

        expect(getPreferCutout).toHaveBeenCalledWith('dp-key', 'cadc-omm-obscore');
        expect(result.menu.map(({menuKey}) => menuKey)).toEqual(['dlt-1', 'dlt-0', 'dlt-4']);
        expect(result.menuKey).toBe('dlt-1');
        expect(result.serDef.ID).toBe(SODA_SYNC_ID);
        expect(result.dlData).toMatchObject({semantics: '#cutout', serviceDefRef: SODA_SYNC_ID});
    });

    test('processDatalinkTable: the IMAGE algorithm keeps only the FITS rows', () => {
        const result = processDatalinkTableForOmmRow(
            {datalinkTable: makeOmmDatalinkTable(OMM_DATALINK_ROWS), options: {}, extra: {parsingAlgorithm: IMAGE}});

        // a single entry is shown without a menu
        expect(result).toMatchObject({displayType: DPtypes.ANALYZE, menuKey: 'dlt-1', menu: []});
    });

    test('processDatalinkTable: the active menu key from before is shown again', () => {
        getActiveMenuKey.mockReturnValueOnce('dlt-4');

        const result = processDatalinkTableForOmmRow(
            {datalinkTable: makeOmmDatalinkTable(OMM_DATALINK_ROWS), options: {}, extra: {}});

        expect(getActiveMenuKey).toHaveBeenCalledWith('dpId-test', OMM_DATALINK_URL);
        expect(result).toMatchObject({displayType: DPtypes.PNG, menuKey: 'dlt-4'});
        expect(dispatchUpdateActiveKey).toHaveBeenCalledWith(
            {dpId: 'dpId-test', activeMenuKeyChanges: {[OMM_DATALINK_URL]: 'dlt-4'}});
    });

    test('processDatalinkTable: a grid cell does not change the active menu key', () => {
        const result = processDatalinkTableForOmmRow(
            {datalinkTable: makeOmmDatalinkTable(OMM_DATALINK_ROWS), options: {}, extra: {useForTableGrid: true}});

        expect(result.menuKey).toBe('dlt-1');
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });

    test('processDatalinkTable: without file analysis the FITS file is guessed to be an image', () => {
        const result = processDatalinkTableForOmmRow(
            {datalinkTable: makeOmmDatalinkTable(OMM_DATALINK_ROWS), options: {}, extra: {doFileAnalysis: false}});

        expect(result).toMatchObject({displayType: DPtypes.IMAGE, menuKey: 'dlt-1', url: OMM_FILE_URL});
        expect(result.request.getURL()).toBe(OMM_FILE_URL);
    });

    test('processDatalinkTable: other service descriptors are added to the end of the menu', () => {
        const sodaEntry = {displayType: DPtypes.ANALYZE, menuKey: 'serdesc-dlt-0', name: 'SODA cutout'};

        const result = processDatalinkTableForOmmRow({datalinkTable: makeOmmDatalinkTable(OMM_DATALINK_ROWS), options: {},
            extra: {additionalServiceDescMenuList: [sodaEntry], parsingAlgorithm: USE_ALL}});

        expect(result.menu.map(({menuKey}) => menuKey)).toEqual(['dlt-1', 'dlt-0', 'dlt-4', 'serdesc-dlt-0']);
    });

    test('processDatalinkTable: datalinkDisableMoreDrop shows only the first entry', () => {
        const result = processDatalinkTableForOmmRow({datalinkTable: makeOmmDatalinkTable(OMM_DATALINK_ROWS),
            options: {datalinkDisableMoreDrop: true}, extra: {}});

        expect(result).toMatchObject({menuKey: 'dlt-1', menu: []});
    });

    test('processDatalinkTable: a row with an error_message is an error entry', () => {
        const rows = [OMM_DATALINK_ROWS[1],
            [OMM_ID, '', '', 'NotFoundFault: cadc:OMM/C140117_0290_SCI_prev.jpg', '#preview', '', 'preview', 'image/jpeg', '']];

        const result = processDatalinkTableForOmmRow({datalinkTable: makeOmmDatalinkTable(rows), options: {}, extra: {}});

        expect(result.menu[1]).toMatchObject({displayType: DPtypes.MESSAGE, menuKey: 'dlt-1', complexMessage: false,
            name: 'Error in related data (datalink) row 1'});
    });

    test('processDatalinkTable: an empty DataLink table has no data', () => {
        const result = processDatalinkTableForOmmRow({datalinkTable: makeOmmDatalinkTable([]), options: {}, extra: {}});

        expect(result).toMatchObject({displayType: DPtypes.MESSAGE, message: 'No data available for this row',
            activeMenuLookupKey: OMM_DATALINK_URL});
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });

    test('processDatalinkTable: a file too large to show is offered as a download', () => {
        const threeGigFileRow = [OMM_ID, OMM_FILE_URL, '', '', '#this', '', 'download cadc:OMM/C140117_0290_SCI.fits',
            'application/fits', String(3 * 1024 * 1024 * 1024)];

        const result = processDatalinkTableForOmmRow(
            {datalinkTable: makeOmmDatalinkTable([threeGigFileRow]), options: {}, extra: {}});

        expect(result).toMatchObject({displayType: DPtypes.MESSAGE,
            message: 'No data to display: the data can only be downloaded', activeMenuLookupKey: OMM_DATALINK_URL});
        expect(result.menu).toEqual([expect.objectContaining({displayType: DPtypes.DOWNLOAD, url: OMM_FILE_URL, menuKey: 'dlt-0'})]);
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });

    test('processDatalinkTable: SODA rows with URL semantics become service descriptor entries', () => {
        useServiceDefProductStandIn();
        // rubin-datalink-row-7.vot: #this, its #cutout, and two more SODA cutouts with Rubin's own semantics
        const rubinId = 'ivo://org.rubinobs/lsst-dp1?repo=dp1&id=019b1942';
        const rubinTable = {
            tbl_id: 'rubin-datalink-row-7', totalRows: 4, tableMeta: {}, request: {},
            tableData: {columns: DATALINK_COLUMNS, data: [
                [rubinId, 'https://storage.googleapis.com/visit_image.fits', '', '', '#this', '', 'Primary image', 'application/fits', '110926080'],
                [rubinId, '', 'cutout-sync', '', '#cutout', '', 'Cutout service (SODA sync), only image pixels', 'application/fits', ''],
                [rubinId, '', 'cutout-sync-maskedimage', '', 'https://rsp.lsst.io/guides/api/soda.html#MaskedImage', '',
                    'Cutout service (SODA sync), masked image only', 'application/fits', ''],
                [rubinId, '', 'cutout-sync-exposure', '', 'https://rsp.lsst.io/guides/api/soda.html#Exposure', '',
                    'Cutout service (SODA sync), file with full metadata', 'application/fits', ''],
            ]},
            resources: ['cutout-sync', 'cutout-sync-maskedimage', 'cutout-sync-exposure'].map((ID) =>
                makeSodaResource(ID, 'ivo://ivoa.net/std/SODA#sync-1.0', 'https://data.lsst.cloud/api/cutout/sync')),
        };

        const result = processDatalinkTableForOmmRow({datalinkTable: rubinTable, options: {}, extra: {}});

        expect(result.menu.map(({menuKey}) => menuKey)).toEqual(['dlt-0', 'dlt-2', 'dlt-3']);
        expect(result.menu.slice(1).map(({serDef}) => serDef.ID)).toEqual(['cutout-sync-maskedimage', 'cutout-sync-exposure']);
    });


    test('makeMenuEntry: a service descriptor row becomes a service descriptor product', () => {
        useServiceDefProductStandIn();
        const cutoutDlData = getOmmDlData('#cutout');
        const activateParams = {dpId: 'dpId-test'};
        const options = {};

        const result = makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: cutoutDlData, idx: 2,
            sourceTable: OMM_OBSCORE_TABLE, sourceRow: 0, options, name: 'Cutout', prodTitle: 'C140117_0290 cutout',
            doFileAnalysis: true, activateParams, dropDownText: 'Show: cutout'});

        expect(makeServiceDefDataProduct).toHaveBeenCalledWith({
            serDef: cutoutDlData.serDef, sourceTable: OMM_OBSCORE_TABLE, sourceRow: 0, idx: 2, positionWP: undefined,
            activateParams, options, name: 'C140117_0290 cutout', dropDownText: 'Show: cutout',
            activeMenuLookupKey: OMM_DATALINK_URL, menuKey: 'dlt-2', dlData: cutoutDlData,
        });
        expect(result.serDef.ID).toBe(SODA_SYNC_ID);
    });

    test('makeMenuEntry: without a DataLink table url the lookup key names the row', () => {
        useServiceDefProductStandIn();

        makeMenuEntry({dlTableUrl: undefined, dlData: getOmmDlData('#cutout'), idx: 2, sourceTable: OMM_OBSCORE_TABLE,
            sourceRow: 0, options: {}, name: 'Cutout', prodTitle: undefined, doFileAnalysis: true,
            activateParams: {dpId: 'dpId-test'}, dropDownText: undefined});

        expect(makeServiceDefDataProduct.mock.calls[0][0]).toMatchObject(
            {activeMenuLookupKey: 'no-table-2', name: 'Cutout'});
    });

    test('makeMenuEntry: a JPEG is a PNG entry', () => {
        const previewDlData = getOmmDlData('#preview');

        const result = makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: previewDlData, idx: 0,
            sourceTable: OMM_OBSCORE_TABLE, sourceRow: 0, options: {}, name: 'Preview', prodTitle: undefined,
            doFileAnalysis: true, activateParams: {dpId: 'dpId-test'}, dropDownText: undefined});

        expect(result).toEqual({displayType: DPtypes.PNG, name: 'Preview (PNG image)', url: previewDlData.url,
            menuKey: 'dlt-0', semantics: '#preview', size: '539277', activeMenuLookupKey: OMM_DATALINK_URL,
            dlData: previewDlData});
    });

    test('makeMenuEntry: a file over 2 GB is a download', () => {
        const thisDlData = {...getOmmDlData('#this'), size: 3 * 1024 * 1024 * 1024};

        const result = makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: thisDlData, idx: 1,
            sourceTable: OMM_OBSCORE_TABLE, sourceRow: 0, options: {}, name: 'C140117_0290', prodTitle: undefined,
            doFileAnalysis: true, activateParams: {dpId: 'dpId-test'}, dropDownText: undefined});

        expect(result).toMatchObject({displayType: DPtypes.DOWNLOAD, name: 'Download: C140117_0290 (too large to show)',
            url: OMM_FILE_URL, menuKey: 'dlt-1', fileType: 'fits'});
    });

    test('makeMenuEntry: a type the client can show itself uses the file name and type analysis', () => {
        const textDlData = {...getOmmDlData('#this'), url: 'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/README.txt',
            contentType: 'text/plain'};
        isNonServerAnalysisType.mockReturnValueOnce(true);
        doFileNameAndTypeAnalysis.mockReturnValueOnce({displayType: DPtypes.PNG, name: 'README.txt'});

        const result = makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: textDlData, idx: 1,
            sourceTable: OMM_OBSCORE_TABLE, sourceRow: 0, options: {}, name: 'README', prodTitle: undefined,
            doFileAnalysis: true, activateParams: {dpId: 'dpId-test'}, dropDownText: undefined});

        expect(doFileNameAndTypeAnalysis).toHaveBeenCalledWith(
            {url: textDlData.url, ct: 'text/plain', wrapWithMessage: false, name: 'README'});
        expect(result).toEqual({displayType: DPtypes.PNG, name: 'README.txt', menuKey: 'dlt-1', dlData: textDlData,
            semantics: '#this', size: '16790400', activeMenuLookupKey: OMM_DATALINK_URL});
    });

    test('makeMenuEntry: a spectrum VOTable is a chart and table', () => {
        const spectrumDlData = getDataLinkData(makeOmmDatalinkTable([[OMM_ID,
            'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/C140117_0290_spec.xml', '', '', '#this', 'spectrum',
            'extracted spectrum', 'application/x-votable+xml', '20480']]), false)[0];
        const activateParams = {dpId: 'dpId-test'};
        createChartTableActivate.mockReturnValueOnce('activate-chart');
        createTableExtraction.mockReturnValueOnce('extract-table');

        const result = makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: spectrumDlData, idx: 0,
            sourceTable: OMM_OBSCORE_TABLE, sourceRow: 0, options: {statefulTabComponentKey: 'tabs-key'},
            name: 'Spectrum', prodTitle: 'C140117_0290 spectrum', doFileAnalysis: true, activateParams, dropDownText: undefined});

        expect(createChartTableActivate).toHaveBeenCalledWith({
            chartAndTable: true, source: spectrumDlData.url, titleInfo: 'C140117_0290 spectrum', activateParams,
            dataTypeHint: TableDataType.Spectrum, tbl_id: 'direct-result-tbl-0',
            chartInfo: {useChartChooser: true, showChartTitle: true, tableDataType: TableDataType.Spectrum},
            chartId: 'direct-result-chart-0', statefulTabComponentKey: 'tabs-key',
        });
        expect(result).toMatchObject({displayType: DPtypes.CHOICE_CTI, name: 'C140117_0290 spectrum',
            activate: 'activate-chart', extraction: 'extract-table', menuKey: 'dlt-0',
            tbl_id: 'direct-result-tbl-0', chartId: 'direct-result-chart-0'});
    });

    test('makeMenuEntry: a spectrum table and chart id can come from the options', () => {
        const spectrumDlData = getDataLinkData(makeOmmDatalinkTable([[OMM_ID,
            'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/C140117_0290_spec.xml', '', '', '#this', 'spectrum',
            'extracted spectrum', 'application/x-votable+xml', '20480']]), false)[0];
        const options = {
            tableIdList: [{description: 'extracted spectrum', tbl_id: 'omm-spectrum-table'}],
            chartIdBase: 'omm-chart',
        };

        const result = makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: spectrumDlData, idx: 0,
            sourceTable: OMM_OBSCORE_TABLE, sourceRow: 0, options, name: 'Spectrum', prodTitle: 'C140117_0290 spectrum',
            doFileAnalysis: true, activateParams: {dpId: 'dpId-test'}, dropDownText: undefined});

        expect(result).toMatchObject({tbl_id: 'omm-spectrum-table', chartId: 'omm-chart-0'});
    });

    test('makeMenuEntry: a FITS file is analyzed', () => {
        const thisDlData = getOmmDlData('#this');
        const activateParams = {dpId: 'dpId-test'};
        const options = {};
        makeAnalysisActivateFunc.mockReturnValueOnce('activate-analysis');

        const result = makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: thisDlData, idx: 1,
            sourceTable: OMM_OBSCORE_TABLE, sourceRow: 0, options, name: 'C140117_0290', prodTitle: 'C140117_0290 image',
            doFileAnalysis: true, activateParams, dropDownText: 'Show: C140117_0290'});

        expect(result).toMatchObject({displayType: DPtypes.ANALYZE, name: 'C140117_0290', activate: 'activate-analysis',
            url: OMM_FILE_URL, menuKey: 'dlt-1', semantics: '#this', activeMenuLookupKey: OMM_DATALINK_URL, dlData: thisDlData});
        expect(result.request.getURL()).toBe(OMM_FILE_URL);
        expect(makeAnalysisActivateFunc).toHaveBeenCalledWith({table: OMM_OBSCORE_TABLE, row: 0, request: result.request,
            activateParams, menuKey: 'dlt-1', activeMenuLookupKey: OMM_DATALINK_URL, options, dlData: thisDlData,
            originalTitle: 'Show: C140117_0290', prodTitle: 'C140117_0290 image'});
    });

    test('makeMenuEntry: without file analysis a FITS file is guessed to be an image', () => {
        const thisDlData = getOmmDlData('#this');

        const result = makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: thisDlData, idx: 1,
            sourceTable: OMM_OBSCORE_TABLE, sourceRow: 0, options: {}, name: 'C140117_0290', prodTitle: undefined,
            doFileAnalysis: false, activateParams: {dpId: 'dpId-test'}, dropDownText: undefined});

        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'C140117_0290', url: OMM_FILE_URL, menuKey: 'dlt-1'});
        expect(makeAnalysisActivateFunc).not.toHaveBeenCalled();
    });

    test('makeMenuEntry: a type that can not be shown or analyzed gives no entry', () => {
        const pdfDlData = {...getOmmDlData('#this'), url: 'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/notes.pdf',
            contentType: 'application/pdf'};
        isNonServerAnalysisType.mockReturnValueOnce(false);

        expect(makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: pdfDlData, idx: 1, sourceTable: OMM_OBSCORE_TABLE,
            sourceRow: 0, options: {}, name: 'notes', prodTitle: undefined, doFileAnalysis: true,
            activateParams: {dpId: 'dpId-test'}, dropDownText: undefined})).toBeUndefined();
    });

    test('makeMenuEntry: a row with no service descriptor and no url gives no entry', () => {
        const noLinkDlData = {...getOmmDlData('#this'), url: '', serDef: undefined};

        expect(makeMenuEntry({dlTableUrl: OMM_DATALINK_URL, dlData: noLinkDlData, idx: 1, sourceTable: OMM_OBSCORE_TABLE,
            sourceRow: 0, options: {}, name: 'none', prodTitle: undefined, doFileAnalysis: true,
            activateParams: {dpId: 'dpId-test'}, dropDownText: undefined})).toBeUndefined();
    });


    test('processDatalinkTable: a SPHEREx image comes first, then its calibration files', () => {
        useYamlOnlyTypeAnalysisStandIn();

        const result = processDatalinkTable({sourceTable: OMM_OBSCORE_TABLE, row: 0, datalinkTable: SPHEREX_DATALINK_TABLE,
            activateParams: {dpId: 'dpId-test'}, dlTableUrl: SPHEREX_DATALINK_URL, options: {}});

        // the #cutout is folded into #this
        expect(result).toMatchObject({displayType: DPtypes.ANALYZE, menuKey: 'dlt-0', semantics: '#this'});
        const entriesByKey = Object.fromEntries(result.menu.map((entry) => [entry.menuKey, entry]));
        expect(entriesByKey['dlt-1']).toBeUndefined();
        expect([entriesByKey['dlt-2'].displayType, entriesByKey['dlt-3'].displayType]).toEqual([DPtypes.ANALYZE, DPtypes.ANALYZE]);
        expect(entriesByKey['dlt-2'].dlData.dlAnalysis.isCalibration).toBe(true);
        expect(entriesByKey['dlt-4']).toMatchObject({displayType: DPtypes.TXT, semantics: '#calibration'});
    });

    test('processDatalinkTable: in a related image grid the band and label used last are shown again', () => {
        // the user last looked at the NISP Y image of another row; this row's VIS and Y images get band and label keys
        const lastKey = makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-7');
        getCurrentActiveKeyID.mockReturnValue('other-row-datalink-url');
        getActiveMenuKey.mockImplementation((dpId, lookupKey) =>
            lookupKey === 'other-row-datalink-url' ? lastKey : undefined);

        const result = processDatalinkTable({sourceTable: OMM_OBSCORE_TABLE, row: 0, datalinkTable: EUCLID_DATALINK_TABLE,
            activateParams: {dpId: 'dpId-test'}, dlTableUrl: EUCLID_DATALINK_URL, options: {hasRelatedBands: true}});

        expect(result.menu.map(({menuKey}) => menuKey).sort()).toEqual([
            makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-2'), makeBandLabelMenuKey('VIS', 'VIS', 'dlt-0')]);
        expect(result.menuKey).toBe(makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-2'));
        expect(dispatchUpdateActiveKey).toHaveBeenCalledWith({dpId: 'dpId-test',
            activeMenuKeyChanges: {[EUCLID_DATALINK_URL]: makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-2')}});
    });

    test('processDatalinkTable: relatedGridImageOrder sets the menu order by label', () => {
        const result = processDatalinkTable({sourceTable: OMM_OBSCORE_TABLE, row: 0, datalinkTable: EUCLID_DATALINK_TABLE,
            activateParams: {dpId: 'dpId-test'}, dlTableUrl: EUCLID_DATALINK_URL,
            options: {relatedGridImageOrder: ['NISP/Y', 'VIS']}});

        expect(result.menu.map(({dlData}) => dlData.labelDLExt)).toEqual(['NISP/Y', 'VIS']);
    });

    test('processDatalinkTable: with cutouts preferred a large full image puts a warning on its cutout', () => {
        getPreferCutout.mockReturnValue(true);
        useServiceDefProductStandIn();

        const result = processDatalinkTable({sourceTable: OMM_OBSCORE_TABLE, row: 0, datalinkTable: EUCLID_DATALINK_TABLE,
            activateParams: {dpId: 'dpId-test'}, dlTableUrl: EUCLID_DATALINK_URL, options: {}});

        const entriesByLabel = Object.fromEntries(result.menu.map((entry) => [entry.dlData.labelDLExt, entry]));
        // the VIS image is 1.6 GB, the Y image 400 MB
        expect(entriesByLabel.VIS.dlData).toMatchObject({semantics: '#cutout',
            cutoutToFullWarning: 'Warning: Full image file is 1587.2M, it might take a while to load'});
        expect(entriesByLabel['NISP/Y'].dlData).toMatchObject({semantics: '#cutout', cutoutToFullWarning: undefined});
    });

    test('processDatalinkTable: several spectra are gathered into one chart and table entry', () => {
        createChartTableActivate.mockImplementation(({tbl_id}) => `activate-${tbl_id}`);
        createTableExtraction.mockImplementation((url) => `extract-${url}`);
        makeMultiTableActivate.mockReturnValueOnce('activate-all');
        makeMultiTableExtraction.mockReturnValueOnce('extract-all');
        const activateParams = {dpId: 'dpId-test'};

        const result = processDatalinkTable({sourceTable: OMM_OBSCORE_TABLE, row: 0,
            datalinkTable: SPECTRA_DATALINK_TABLE, activateParams, dlTableUrl: SPECTRA_DATALINK_URL, options: {}});

        expect(result).toMatchObject({displayType: DPtypes.CHOICE_CTI, menuKey: 'multi-table', activate: 'activate-all',
            extraction: 'extract-all', menu: []});
        expect(makeMultiTableActivate).toHaveBeenCalledWith({
            'direct-result-tbl-0': {activate: 'activate-direct-result-tbl-0', chartId: 'direct-result-chart-0'},
            'direct-result-tbl-1': {activate: 'activate-direct-result-tbl-1', chartId: 'direct-result-chart-1'},
        }, activateParams);
        expect(makeMultiTableExtraction).toHaveBeenCalledWith({
            'direct-result-tbl-0': 'extract-https://example.org/spectra/blue.xml',
            'direct-result-tbl-1': 'extract-https://example.org/spectra/red.xml',
        }, activateParams);
    });

    test('filterDLList: USE_ALL keeps every row', () => {
        const dataLinkData = getDataLinkData(makeOmmDatalinkTable(OMM_DATALINK_ROWS), false);

        expect(filterDLList(USE_ALL, dataLinkData)).toBe(dataLinkData);
    });

    test('filterDLList: IMAGE keeps the rows that may be images', () => {
        const dataLinkData = getDataLinkData(makeOmmDatalinkTable(OMM_DATALINK_ROWS), false);

        expect(filterDLList(IMAGE, dataLinkData).map(({semantics}) => semantics)).toEqual(['#this', '#cutout']);
    });

    test('filterDLList: RELATED_IMAGE_GRID keeps the grid images, not the cutouts paired with them', () => {
        const dataLinkData = getDataLinkData(EUCLID_DATALINK_TABLE, false);

        expect(filterDLList(RELATED_IMAGE_GRID, dataLinkData).map(({semantics, labelDLExt}) => [semantics, labelDLExt]))
            .toEqual([['#this', 'VIS'], ['#this', 'NISP/Y']]);
    });

    test('filterDLList: RELATED_IMAGE_GRID keeps a grid cutout that has no image paired with it', () => {
        const [, visCutoutDlData] = getDataLinkData(EUCLID_DATALINK_TABLE, false);
        const unpairedCutout = {...visCutoutDlData, dlAnalysis: {...visCutoutDlData.dlAnalysis, cutoutFullPair: false}};

        expect(filterDLList(RELATED_IMAGE_GRID, [unpairedCutout])).toEqual([unpairedCutout]);
    });

    test('filterDLList: SPECTRUM keeps the spectra', () => {
        const dataLinkData = [...getDataLinkData(SPECTRA_DATALINK_TABLE, false), getOmmDlData('#this')];

        expect(filterDLList(SPECTRUM, dataLinkData).map(({url}) => url))
            .toEqual(['https://example.org/spectra/blue.xml', 'https://example.org/spectra/red.xml']);
    });

    test('filterDLList: an unknown algorithm keeps every row', () => {
        const dataLinkData = getDataLinkData(makeOmmDatalinkTable(OMM_DATALINK_ROWS), false);

        expect(filterDLList('unknownAlgorithm', dataLinkData)).toBe(dataLinkData);
    });

    test('makeBandLabelMenuKey: the label and band are marked in the key', () => {
        expect(makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-2')).toBe('--__LABEL:NISP/Y--__BAND:Y--__dlt-2');
        expect(makeBandLabelMenuKey('VIS', '', undefined)).toBe('--__LABEL:VIS--');
        expect(makeBandLabelMenuKey(undefined, undefined, 'dlt-2')).toBe('--__dlt-2');
    });

    test('hasBandInMenuKey, hasLabelInMenuKey, hasBandAndLabelInMenuKey: find the markers in a key', () => {
        const menuKey = makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-2');

        expect(hasBandInMenuKey(menuKey, 'Y')).toBe(true);
        expect(hasBandInMenuKey(menuKey, 'J')).toBe(false);
        expect(hasLabelInMenuKey(menuKey, 'NISP/Y')).toBe(true);
        expect(hasLabelInMenuKey(menuKey, 'VIS')).toBe(false);
        // with no label or band given, any marked key matches
        expect(hasBandAndLabelInMenuKey(menuKey)).toBe(true);
        expect(hasBandAndLabelInMenuKey(makeBandLabelMenuKey('VIS', '', 'dlt-0'))).toBe(false);
        expect(hasBandAndLabelInMenuKey(undefined)).toBe(false);
    });

    test('menuKeysMatchBandLabel: keys match when their band and label are the same', () => {
        expect(menuKeysMatchBandLabel(makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-7'),
            makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-2'))).toBe(true);
        expect(menuKeysMatchBandLabel(makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-2'),
            makeBandLabelMenuKey('NISP/J', 'J', 'dlt-2'))).toBe(false);
        // keys without markers match each other
        expect(menuKeysMatchBandLabel('dlt-1', 'dlt-2')).toBe(true);
    });

    test('findMenuKeyWithName: the key with that label', () => {
        const keys = [makeBandLabelMenuKey('VIS', 'VIS', 'dlt-0'), makeBandLabelMenuKey('NISP/Y', 'Y', 'dlt-2')];

        expect(findMenuKeyWithName(keys, 'NISP/Y')).toBe(keys[1]);
        expect(findMenuKeyWithName(keys, 'NISP/J')).toBeUndefined();
        expect(findMenuKeyWithName(keys, undefined)).toBeUndefined();
    });

    test('sortRelatedGridUsingRequest: requests in the given title order, then the rest', () => {
        const makeRequest = (title) => ({getTitle: () => title});
        const reqAry = ['VIS', 'Hyper Suprime-Cam/R2', 'NISP/Y', 'NISP/J'].map(makeRequest);

        expect(sortRelatedGridUsingRequest(reqAry, ['NISP/Y', 'NISP/J', 'VIS']).map((r) => r.getTitle()))
            .toEqual(['NISP/Y', 'NISP/J', 'VIS', 'Hyper Suprime-Cam/R2']);
    });

    test('isWarnSize: over 1 GB', () => {
        expect(isWarnSize(1664316480)).toBe(true);
        expect(isWarnSize(416165760)).toBe(false);
        expect(isWarnSize(undefined)).toBe(false);
    });

    test('getCutoutSizeWarning: a warning only for a full image over 1 GB', () => {
        expect(getCutoutSizeWarning({size: 1664316480}))
            .toBe('Warning: Full image file is 1587.2M, it might take a while to load');
        expect(getCutoutSizeWarning({size: 416165760})).toBeUndefined();
    });

    test('getCutoutTotalWarning: a warning when the images add up to over 1 GB', () => {
        // 400 MB each
        expect(getCutoutTotalWarning([{size: 419430400}, {size: 419430400}, {size: 419430400}], 3))
            .toBe('Warning: Loading 3 images with a total size of 1200.0M, it might take a while to load');
        expect(getCutoutTotalWarning([{size: 419430400}, {size: 419430400}], 2)).toBeUndefined();
    });

    test('getCutoutTotalWarning: an image with no size adds nothing to the total', () => {
        // getDataLinkData makes an empty content_length ''
        expect(getCutoutTotalWarning([{size: 419430400}, {size: ''}, {size: 419430400}, {size: 419430400}], 4))
            .toBe('Warning: Loading 4 images with a total size of 1200.0M, it might take a while to load');
        expect(getCutoutTotalWarning([{size: 419430400}, {size: ''}, {size: 419430400}], 3)).toBeUndefined();
    });

    test('hasError: the error message, an unusable row, or a missing service descriptor', () => {
        const thisDlData = getOmmDlData('#this');
        const [, , , asyncCutoutDlData] = getDataLinkData(makeOmmDatalinkTable(OMM_DATALINK_ROWS), true);

        expect(hasError(thisDlData)).toBeUndefined();
        expect(hasError({...thisDlData, error_message: 'NotFoundFault: file not found'}))
            .toBe('NotFoundFault: file not found');
        expect(hasError(asyncCutoutDlData)).toBe('This (datalink) row is not usable by the application');
        // made up: a usable row (it has a url) that names the async descriptor
        expect(hasError({...asyncCutoutDlData, url: 'https://example.org/cutout',
            dlAnalysis: {...asyncCutoutDlData.dlAnalysis, usableEntry: true}}))
            .toBe('Datalink row has an unsupported or missing service descriptor (async service descriptors are not supported)');
    });

    test('createDataLinkSingleRowItem: a usable row becomes its menu entry, named by its semantics', () => {
        const thisDlData = getOmmDlData('#this');
        makeAnalysisActivateFunc.mockReturnValueOnce('activate-analysis');

        const result = createDataLinkSingleRowItem({dlData: thisDlData, activateParams: {dpId: 'dpId-test'},
            options: {}});

        expect(result).toMatchObject({displayType: DPtypes.ANALYZE, name: '#this', url: OMM_FILE_URL, menuKey: 'dlt-1',
            activeMenuLookupKey: 'none', activate: 'activate-analysis'});
    });

    test('createDataLinkSingleRowItem: a row with an error is an error entry', () => {
        const [, , , asyncCutoutDlData] = getDataLinkData(makeOmmDatalinkTable(OMM_DATALINK_ROWS), true);

        const result = createDataLinkSingleRowItem({dlData: asyncCutoutDlData, activateParams: {dpId: 'dpId-test'},
            options: {}});

        expect(result).toMatchObject({displayType: DPtypes.MESSAGE, menuKey: 'dlt-3', complexMessage: false,
            name: 'Error in related data (datalink) row 3',
            message: 'This (datalink) row is not usable by the application'});
        expect(makeServiceDefDataProduct).not.toHaveBeenCalled();
    });

    test('createGuessDataType: a FITS file is an image', () => {
        const result = createGuessDataType({name: 'C140117_0290', menuKey: 'dlt-1', url: OMM_FILE_URL,
            cloudAccess: undefined, ct: 'application/fits', semantics: '#this', activateParams: {imageViewerId: 'viewer-1'},
            positionWP: undefined, table: OMM_OBSCORE_TABLE, row: 0, size: '16790400', dlData: undefined});

        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'C140117_0290', menuKey: 'dlt-1',
            url: OMM_FILE_URL, semantics: '#this', size: '16790400'});
        expect(result.request.getURL()).toBe(OMM_FILE_URL);
    });

    test('createGuessDataType: with no name the ObsCore title is used', () => {
        const result = createGuessDataType({name: undefined, menuKey: undefined, url: OMM_FILE_URL,
            cloudAccess: undefined, ct: 'image/fits', semantics: undefined, activateParams: {},
            positionWP: undefined, table: OMM_OBSCORE_TABLE, row: 0, size: undefined, dlData: undefined});

        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'OMM, C140117_0290', menuKey: 'guess-0'});
    });

    test('createGuessDataType: a table, a spectrum, or an #auxiliary row is a table', () => {
        createTableActivate.mockReturnValue('activate-table');
        createTableExtraction.mockReturnValue('extract-table');
        const activateParams = {dpId: 'dpId-test'};

        const result = createGuessDataType({name: 'source list', menuKey: 'dlt-5', url: 'https://example.org/sources.tbl',
            cloudAccess: undefined, ct: 'text/plain', semantics: '#auxiliary', activateParams,
            positionWP: undefined, table: OMM_OBSCORE_TABLE, row: 0, size: '2048', dlData: undefined});

        expect(result).toMatchObject({displayType: DPtypes.TABLE, name: 'source list', activate: 'activate-table',
            extraction: 'extract-table', menuKey: 'dlt-5', url: 'https://example.org/sources.tbl', semantics: '#auxiliary', size: '2048'});
        expect(createTableActivate)
            .toHaveBeenCalledWith('https://example.org/sources.tbl', '#auxiliary', activateParams, 'text/plain');
        expect(createTableExtraction).toHaveBeenCalledWith('https://example.org/sources.tbl', 'source list');
        expect(createGuessDataType({name: 'spectrum', menuKey: 'dlt-6', url: 'https://example.org/spectrum.xml',
            cloudAccess: undefined, ct: 'application/x-spectrum', semantics: '#this', activateParams,
            positionWP: undefined, table: OMM_OBSCORE_TABLE, row: 0, size: '2048', dlData: undefined}).displayType)
            .toBe(DPtypes.TABLE);
    });

    test('createGuessDataType: a JPEG is a PNG entry', () => {
        const result = createGuessDataType({name: 'preview', menuKey: 'dlt-0',
            url: 'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/C140117_0290_SCI_prev.jpg', cloudAccess: undefined,
            ct: 'image/jpeg', semantics: '#preview', activateParams: {}, positionWP: undefined, table: OMM_OBSCORE_TABLE,
            row: 0, size: '539277', dlData: undefined});

        // image/jpeg contains "image", so it is taken for an image before the PNG check
        expect(result.displayType).toBe(DPtypes.IMAGE);
        expect(createGuessDataType({name: 'preview', menuKey: 'dlt-0', url: 'https://example.org/preview.png',
            cloudAccess: undefined, ct: 'png', semantics: '#preview', activateParams: {}, positionWP: undefined,
            table: OMM_OBSCORE_TABLE, row: 0, size: '539277', dlData: undefined}))
            .toMatchObject({displayType: DPtypes.PNG, name: 'preview', url: 'https://example.org/preview.png'});
    });

    test('createGuessDataType: a download type is a download entry', () => {
        isUsableDownloadType.mockReturnValueOnce(true);
        makeDownloadType.mockReturnValueOnce({displayType: DPtypes.DOWNLOAD, name: 'Download: tar file',
            url: 'https://example.org/files.tar'});

        const result = createGuessDataType({name: 'files', menuKey: 'dlt-8', url: 'https://example.org/files.tar',
            cloudAccess: undefined, ct: 'application/x-tar', semantics: '#this', activateParams: {},
            positionWP: undefined, table: OMM_OBSCORE_TABLE, row: 0, size: '4096', dlData: undefined});

        expect(result).toEqual({displayType: DPtypes.DOWNLOAD, name: 'Download: tar file',
            url: 'https://example.org/files.tar', menuKey: 'dlt-8', semantics: '#this', size: '4096',
            dlData: undefined});
        expect(makeDownloadType).toHaveBeenCalledWith('https://example.org/files.tar', undefined, 'application/x-tar',
            false);
    });

    test('createGuessDataType: a type it does not know gives nothing', () => {
        expect(createGuessDataType({name: 'notes', menuKey: 'dlt-9', url: 'https://example.org/notes.pdf',
            cloudAccess: undefined, ct: 'application/pdf', semantics: '#documentation', activateParams: {},
            positionWP: undefined, table: OMM_OBSCORE_TABLE, row: 0, size: '4096', dlData: undefined})).toBeUndefined();
    });
});
