import {getComponentState} from '../../../core/ComponentCntlr.js';
import {findCutoutTarget, getCutoutSize, ROW_POSITION, SEARCH_POSITION, setCutoutSize} from '../../../ui/tap/Cutout.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {PlotAttribute} from '../../../visualize/PlotAttribute.js';
import {getDataLinkData, getServiceDescriptors} from '../../../voAnalyzer/VoDataLinkServDef.js';
import {makeAnalysisActivateFunc} from '../../AnalysisUtils.js';
import {dispatchSetSearchParams, isServiceDescriptorActivated} from '../../DataProductsCntlr.js';
import {DPtypes} from '../../DataProductsType.js';
import {
    canMakeCutoutProduct, createServDescMenuRet, makeCutoutProduct, makeServiceDefDataProduct, makeUrlFromParams,
    SD_DEFAULT_PIXEL_CUTOUT_SIZE, SD_DEFAULT_SPACIAL_CUTOUT_SIZE,
} from '../ServDescProducts.js';

// Loading these modules pulls in UI components whose dependencies (validator/es) Jest does not transform,
// so they are replaced with stubs. makeCircleString is copied from DynamicUISearchPanel.
jest.mock('../../AnalysisUtils.js', () => ({
    makeAnalysisActivateFunc: jest.fn(),
}));
jest.mock('../../../ui/dynamic/DynamicUISearchPanel', () => ({
    CONTEXT_PARAMS_STR: 'CONTEXT_PARAMS_STR',
    makeCircleString: (ra, dec, radius) => `${ra} ${dec} ${radius}`,
}));
// keep the real modules, but read the cutout settings and component state from the test instead of the store
jest.mock('../../../ui/tap/Cutout.js', () => ({
    ...jest.requireActual('../../../ui/tap/Cutout.js'),
    findCutoutTarget: jest.fn(),
    getCutoutSize: jest.fn(),
    setCutoutSize: jest.fn(),
}));
jest.mock('../../../core/ComponentCntlr.js', () => ({
    ...jest.requireActual('../../../core/ComponentCntlr.js'),
    getComponentState: jest.fn(),
}));
jest.mock('../../DataProductsCntlr.js', () => ({
    ...jest.requireActual('../../DataProductsCntlr.js'),
    dispatchSetSearchParams: jest.fn(),
    isServiceDescriptorActivated: jest.fn(),
}));

// ZTF light curve catalog with its DataLink service descriptor (serDescTbl.xml): id comes from the oid column,
// bad_catflags_mask is fixed at 0, and time has no value
const ZTF_LIGHT_CURVE_URL = 'https://irsa.ipac.caltech.edu/cgi-bin/ZTF/nph_light_curves?collection=ztf_dr3';
const ZTF_TABLE = {
    tbl_id: 'ztf-lc', highlightedRow: 1, totalRows: 2, tableMeta: {}, request: {},
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

// The CADC SODA sync descriptor from cadc-datalink-row-86.vot, an older CADC response without xtype: ID is fixed,
// POS, CIRCLE and POLYGON are entered by the user (UCD obs.field)
const CADC_SODA_SYNC_URL = 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/sync';
const CADC_SODA_TABLE = {
    tbl_id: 'cadc-omm-datalink', totalRows: 0, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', ID: 'soda-7447b7fc-cba5-4e19-b0a5-e7d6ea7528c2',
        params: [
            {name: 'standardID', value: 'ivo://ivoa.net/std/SODA#sync-1.0'},
            {name: 'accessURL', value: CADC_SODA_SYNC_URL},
        ],
        groups: [{name: 'inputParams', params: [
            {name: 'ID', UCD: 'meta.id;meta.dataset', value: 'cadc:OMM/C140117_0290_SCI.fits'},
            {name: 'POS', UCD: 'obs.field'},
            {name: 'CIRCLE', UCD: 'obs.field', units: 'deg'},
            {name: 'POLYGON', UCD: 'obs.field', units: 'deg'},
        ]}],
    }],
    tableData: {columns: [{name: 'ID', type: 'char'}], data: []},
};

// Euclid MER tile 85951 (euclid-dl.xml): a #cutout row through the sd-cutout descriptor, which takes ra, dec,
// and size (UCD phys.size), and the file path from the row's file column
const EUCLID_CUTOUT_URL = 'https://irsadev.ipac.caltech.edu/ibe/cutouts/cutout';
const EUCLID_FILE_PATH = 'Euclid_TEST/MER_images/TILE85951/EUC_MER_BGSUB-MOSAIC-NIR-H_TILE85951.fits';
const EUCLID_DATALINK_TABLE = {
    tbl_id: 'euclid-datalink', totalRows: 1, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', ID: 'sd-cutout',
        params: [{name: 'accessURL', value: EUCLID_CUTOUT_URL}],
        groups: [{name: 'inputParams', params: [
            {name: 'ra', UCD: 'pos.eq.ra;meta.main', value: '236.05813470000001'},
            {name: 'dec', UCD: 'pos.eq.dec;meta.main', value: '40.499986100000001'},
            {name: 'size', UCD: 'phys.size', units: 'deg', value: '0.01'},
            {name: 'path', ref: 'col_9'},
        ]}],
    }],
    tableData: {
        columns: ['id', 'access_url', 'service_def', 'error_message', 'semantics', 'local_semantics', 'description',
            'content_type', 'content_length', 'file'].map((name, idx) => ({name, type: 'char', ID: `col_${idx}`})),
        data: [['85951', '', 'sd-cutout', '', '#cutout', 'cutout-grid', 'EUC_MER_BGSUB-MOSAIC-NIR-H_TILE85951',
            'image/fits', '', EUCLID_FILE_PATH]],
    },
};
const EUCLID_MER_TABLE = {
    tbl_id: 'euclid-mer', highlightedRow: 0, totalRows: 1, tableMeta: {}, request: {},
    tableData: {columns: [{name: 'tileid', type: 'char'}], data: [['85951']]},
};

// SPHEREx DR1 deep image 2025W17_4B_0021_1/D1 (spherex-datalink-products-row-1.vot): the #this image and its
// #cutout through ibe-cutout, which takes ra and dec from the row's center_ra and center_dec columns, the file
// path from its path column, and a fixed size (UCD phys.size)
const SPHEREX_ID = 'ivo://irsa.ipac/spherex_dr1_deep?2025W17_4B_0021_1/D1';
const SPHEREX_FILE_PATH = 'spherex/dr1/level2/2025W17_4B/l2b-v27-2026-196/1/level2_2025W17_4B_0021_1D1_spx_l2b-v27-2026-196.fits';
const SPHEREX_DATALINK_TABLE = {
    tbl_id: 'spherex-datalink', highlightedRow: 1, totalRows: 2, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', ID: 'ibe-cutout', desc: 'Link to generate an image cutout',
        params: [{name: 'accessURL', value: 'https://irsa.ipac.caltech.edu/ibe/cutout'}],
        groups: [{name: 'inputParams', params: [
            {name: 'ra', UCD: 'pos.eq.ra;meta.main', ref: 'col_13'},
            {name: 'dec', UCD: 'pos.eq.dec;meta.main', ref: 'col_14'},
            {name: 'size', UCD: 'phys.size', units: 'deg', value: '0.01'},
            {name: 'path', ref: 'col_12'},
        ]}],
    }],
    tableData: {
        columns: ['id', 'access_url', 'service_def', 'error_message', 'semantics', 'description', 'content_type',
            'content_length', 'content_qualifier', 'local_semantics', 'link_auth', 'link_authorized', 'path',
            'center_ra', 'center_dec'].map((name, idx) => ({name, type: 'char', ID: `col_${idx}`})),
        data: [
            [SPHEREX_ID, `https://irsa.ipac.caltech.edu/ibe/data/${SPHEREX_FILE_PATH}`, '', '', '#this',
                `${SPHEREX_ID} science image`, 'image/fits', '66087360', '', '', 'optional', 'T', '', '', ''],
            [SPHEREX_ID, '', 'ibe-cutout', '', '#cutout', `${SPHEREX_ID} science image`, 'image/fits', '', '', '',
                'optional', 'T', SPHEREX_FILE_PATH, '270.19211155920743', '68.01568985908334'],
        ],
    },
};

// Rubin DP1 visit image (rubin-datalink-row-8.vot): the #this file and its #cutout through cutout-sync, whose
// CIRCLE parameter has xtype circle; POLYGON and CIRCLE come with empty values
const RUBIN_ID = 'ivo://org.rubinobs/lsst-dp1?repo=dp1&id=019b1997-c4b3-7f3e-9229-9ddc994065b7';
const RUBIN_CUTOUT_URL = 'https://data-int.lsst.cloud/api/cutout/sync';
const RUBIN_DATALINK_TABLE = {
    tbl_id: 'rubin-datalink-row-8', highlightedRow: 1, totalRows: 2, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', ID: 'cutout-sync',
        params: [
            {name: 'standardID', value: 'ivo://ivoa.net/std/SODA#sync-1.0'},
            {name: 'accessURL', value: RUBIN_CUTOUT_URL},
        ],
        groups: [{name: 'inputParams', params: [
            {name: 'ID', type: 'char', UCD: 'meta.id;meta.dataset', value: RUBIN_ID},
            {name: 'POLYGON', type: 'double', UCD: 'pos.outline;obs', units: 'deg', value: '', xtype: 'polygon'},
            {name: 'CIRCLE', type: 'double', arraySize: '3', UCD: 'pos.outline;obs', units: 'deg', value: '', xtype: 'circle'},
            {name: 'CUTOUTDETAIL', type: 'char', value: 'Image'},
        ]}],
    }],
    tableData: {
        columns: ['ID', 'access_url', 'service_def', 'error_message', 'description', 'semantics', 'content_type',
            'content_length'].map((name) => ({name, type: 'char'})),
        data: [
            [RUBIN_ID, 'https://storage.googleapis.com/butler-us-central1-dp1/visit_image.fits', '', '',
                'Primary image or observation data file', '#this', 'application/fits', '109186560'],
            [RUBIN_ID, '', 'cutout-sync', '', 'Cutout service (SODA sync), only image pixels', '#cutout',
                'application/fits', ''],
        ],
    },
};

// CADC DAO observation dao_c182_2022_010463 (cadc-datalink-row-100.vot): the #this file and its SODA sync #cutout;
// this newer CADC response gives CIRCLE xtype circle (and POLYGON xtype polygon), with empty POS and POLYGON values
const CADC_DAO_ID = 'ivo://cadc.nrc.ca/DAO?dao_c182_2022_010463/dao_c182_2022_010463';
const CADC_DAO_SODA_ID = 'soda-58d69c3c-3fd8-4489-9c94-77f965f5d19d';
const CADC_DAO_DATALINK_TABLE = {
    tbl_id: 'cadc-dao-datalink', highlightedRow: 2, totalRows: 2, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', ID: CADC_DAO_SODA_ID,
        params: [
            {name: 'standardID', value: 'ivo://ivoa.net/std/SODA#sync-1.0'},
            {name: 'accessURL', value: CADC_SODA_SYNC_URL},
        ],
        groups: [{name: 'inputParams', params: [
            {name: 'ID', type: 'char', UCD: 'meta.id;meta.dataset', value: 'cadc:DAO/dao_c182_2022_010463.fits'},
            {name: 'POS', type: 'char', UCD: 'obs.field', value: ''},
            {name: 'CIRCLE', type: 'double', arraySize: '3', UCD: 'obs.field', units: 'deg', xtype: 'circle', value: '',
                maxValue: '148.89045519931568 69.06678644650907 0.21615229824042545'},
            {name: 'POLYGON', type: 'double', UCD: 'obs.field', units: 'deg', xtype: 'polygon', value: ''},
        ]}],
    }],
    tableData: {
        columns: ['ID', 'access_url', 'service_def', 'error_message', 'semantics', 'local_semantics', 'description',
            'content_type', 'content_length'].map((name) => ({name, type: 'char'})),
        data: [
            [CADC_DAO_ID, 'https://cadc-west-01.canfar.net/raven/files/cadc:DAO/dao_c182_2022_010463.fits', '', '', '#this',
                '', 'download cadc:DAO/dao_c182_2022_010463.fits', 'application/fits', '4800960'],
            [CADC_DAO_ID, '', CADC_DAO_SODA_ID, '', '#cutout', '', 'SODA-sync cutout of cadc:DAO/dao_c182_2022_010463.fits',
                'application/fits', ''],
        ],
    },
};

describe('ServDescProducts:', () => {

    // clearMocks (jest config) keeps queued mock*Once values, so an unused one would leak into the next test
    beforeEach(() => jest.resetAllMocks());

    test('makeServiceDefDataProduct: a descriptor that needs input is an analyze entry', () => {
        const [sodaSerDef] = getServiceDescriptors(CADC_SODA_TABLE);
        const activateParams = {dpId: 'dpId-test'};
        const options = {};
        const positionWP = makeWorldPt(149.05884191434205, 69.11976141466218);
        const dlData = {semantics: '#cutout', size: '', serviceDefRef: sodaSerDef.ID,
            sourceObsCoreData: {s_region: 'POLYGON 148.9 69.0 149.2 69.0 149.2 69.2 148.9 69.2'}};
        makeAnalysisActivateFunc.mockReturnValueOnce('activate-analysis');

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: 'SODA cutout', serDef: sodaSerDef,
            sourceTable: CADC_SODA_TABLE, sourceRow: 0, idx: 2, positionWP, activateParams, options,
            activeMenuLookupKey: 'lookup-key', menuKey: 'dlt-2', dlData});

        expect(result).toMatchObject({displayType: DPtypes.ANALYZE, activate: 'activate-analysis', url: CADC_SODA_SYNC_URL,
            serDef: sodaSerDef, menuKey: 'dlt-2', activeMenuLookupKey: 'lookup-key', allowsInput: true,
            serviceDefRef: sodaSerDef.ID, standardID: 'ivo://ivoa.net/std/SODA#sync-1.0', ID: sodaSerDef.ID,
            semantics: '#cutout', sRegion: 'POLYGON 148.9 69.0 149.2 69.0 149.2 69.2 148.9 69.2', dlData});
        expect(result.name).toMatch(/^SODA cutout +\(Input Required\)$/);
        expect(result.dropDownText).toMatch(/^Show: SODA cutout +\(Input Required\)$/);
        expect(result.request.getWorldPt().getLon()).toBeCloseTo(149.05884191434205);
        expect(makeAnalysisActivateFunc).toHaveBeenCalledWith({table: CADC_SODA_TABLE, row: 0, request: result.request,
            activateParams, menuKey: 'dlt-2', activeMenuLookupKey: 'lookup-key', dlData, serDef: sodaSerDef,
            originalTitle: 'SODA cutout', options});
        expect(dispatchSetSearchParams).not.toHaveBeenCalled();
    });

    test('makeServiceDefDataProduct: a given dropDownText is kept', () => {
        const [sodaSerDef] = getServiceDescriptors(CADC_SODA_TABLE);

        const result = makeServiceDefDataProduct({dropDownText: 'Cutout of C140117_0290', name: 'SODA cutout',
            serDef: sodaSerDef, sourceTable: CADC_SODA_TABLE, sourceRow: 0, idx: 2, positionWP: undefined,
            activateParams: {dpId: 'dpId-test'}, options: {}, activeMenuLookupKey: 'lookup-key', menuKey: 'dlt-2',
            dlData: undefined});

        expect(result.dropDownText).toBe('Cutout of C140117_0290');
    });

    test('makeServiceDefDataProduct: with no name the descriptor title is used', () => {
        const [ztfSerDef] = getServiceDescriptors(ZTF_TABLE);

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: undefined, serDef: ztfSerDef,
            sourceTable: ZTF_TABLE, sourceRow: 1, idx: 1, positionWP: undefined, activateParams: {dpId: 'dpId-test'},
            options: {}, activeMenuLookupKey: 'lookup-key', menuKey: 'serdesc-dlt-0', dlData: undefined});

        expect(result.name).toMatch(/^Link to ZTF light curve for ztf_dr3 collection +\(Input Required\)$/);
    });

    test('makeServiceDefDataProduct: with no name and no descriptor title the entry is numbered', () => {
        const [sodaSerDef] = getServiceDescriptors(CADC_SODA_TABLE);
        const untitledSerDef = {...sodaSerDef, title: undefined,
            serDefParams: [{name: 'ID', value: 'cadc:OMM/C140117_0290_SCI.fits', allowsInput: false, inputRequired: false}]};

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: undefined, serDef: untitledSerDef,
            sourceTable: CADC_SODA_TABLE, sourceRow: 0, idx: 2, positionWP: undefined, activateParams: {dpId: 'dpId-test'},
            options: {}, activeMenuLookupKey: 'lookup-key', menuKey: 'dlt-2', dlData: undefined});

        expect(result.name.trim()).toBe('Service #2');
    });

    test('makeServiceDefDataProduct: activateServiceDef calls the service with the row values', () => {
        const [ztfSerDef] = getServiceDescriptors(ZTF_TABLE);
        const activateParams = {dpId: 'dpId-test'};
        const options = {activateServiceDef: true, dataProductsComponentKey: 'dp-key'};
        getComponentState.mockReturnValueOnce({});
        makeAnalysisActivateFunc.mockReturnValueOnce('activate-analysis');

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: 'ZTF light curve', serDef: ztfSerDef,
            sourceTable: ZTF_TABLE, sourceRow: 1, idx: 1, positionWP: undefined, activateParams, options,
            activeMenuLookupKey: 'lookup-key', menuKey: 'serdesc-dlt-0', dlData: undefined});

        const url = new URL(result.url);
        expect(url.origin + url.pathname).toBe('https://irsa.ipac.caltech.edu/cgi-bin/ZTF/nph_light_curves');
        // time has no value, so it is not sent
        expect(Object.fromEntries(url.searchParams))
            .toEqual({collection: 'ztf_dr3', id: '1684211300162373', bad_catflags_mask: '0'});
        expect(result).toMatchObject({displayType: DPtypes.ANALYZE, name: 'Show: ZTF light curve',
            dropDownText: 'Show: ZTF light curve', activate: 'activate-analysis', menuKey: 'serdesc-dlt-0'});
        expect(getComponentState).toHaveBeenCalledWith('dp-key', {});
        expect(dispatchSetSearchParams).toHaveBeenCalledWith(
            {dpId: 'dpId-test', autoActiveStatus: {'sd-ztf-lc---0': true}});
    });

    test('makeServiceDefDataProduct: a cutout row with a size parameter becomes a cutout image', () => {
        const [cutoutDlData] = getDataLinkData(EUCLID_DATALINK_TABLE, false);
        const positionWP = makeWorldPt(236.1, 40.6);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION, positionWP});
        getCutoutSize.mockReturnValueOnce(0.01);
        getComponentState.mockReturnValueOnce({});

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: 'NIR-H', serDef: cutoutDlData.serDef,
            sourceTable: EUCLID_MER_TABLE, sourceRow: 0, idx: 0, positionWP, activateParams: {dpId: 'dpId-test'},
            options: {dataProductsComponentKey: 'dp-key'}, activeMenuLookupKey: 'lookup-key', menuKey: 'dlt-0',
            dlData: cutoutDlData});

        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'Cutout: NIR-H', menuKey: 'dlt-0',
            enableCutout: true, pixelBasedCutout: false, dlData: cutoutDlData});
        const url = new URL(result.request.getURL());
        expect(url.origin + url.pathname).toBe(EUCLID_CUTOUT_URL);
        expect(Object.fromEntries(url.searchParams))
            .toEqual({ra: '236.1', dec: '40.6', size: '0.01', path: EUCLID_FILE_PATH});
        expect(result.request.getAttributes()[PlotAttribute.USER_WARNINGS]).toBeUndefined();
        expect(makeAnalysisActivateFunc).not.toHaveBeenCalled();
    });

    test('makeServiceDefDataProduct: a cutout away from the requested position carries a warning', () => {
        const [cutoutDlData] = getDataLinkData(EUCLID_DATALINK_TABLE, false);
        const positionWP = makeWorldPt(236.058, 40.5);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: ROW_POSITION, positionWP});
        getCutoutSize.mockReturnValueOnce(0.01);
        getComponentState.mockReturnValueOnce({});

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: 'NIR-H', serDef: cutoutDlData.serDef,
            sourceTable: EUCLID_MER_TABLE, sourceRow: 0, idx: 0, positionWP, activateParams: {dpId: 'dpId-test'},
            options: {dataProductsComponentKey: 'dp-key'}, activeMenuLookupKey: 'lookup-key', menuKey: 'dlt-0',
            dlData: cutoutDlData});

        expect(result.request.getAttributes()[PlotAttribute.USER_WARNINGS])
            .toBe('Warning: Search position is not on this image, using position from row');
    });

    test('makeServiceDefDataProduct: a cutout row whose descriptor has no recognized size is an analyze entry', () => {
        // in this older CADC response the parameters have UCD obs.field and no xtype, so none is a cutout circle or size
        const [sodaSerDef] = getServiceDescriptors(CADC_SODA_TABLE);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION,
            positionWP: makeWorldPt(149.05884191434205, 69.11976141466218)});

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: 'SODA cutout', serDef: sodaSerDef,
            sourceTable: CADC_SODA_TABLE, sourceRow: 0, idx: 2, positionWP: undefined, activateParams: {dpId: 'dpId-test'},
            options: {}, activeMenuLookupKey: 'lookup-key', menuKey: 'dlt-2',
            dlData: {semantics: '#cutout', dlAnalysis: {isCutout: true}}});

        expect(result.displayType).toBe(DPtypes.ANALYZE);
        expect(getCutoutSize).not.toHaveBeenCalled();
    });


    test('makeServiceDefDataProduct: a SPHEREx cutout takes its position and file from the DataLink row', () => {
        const cutoutDlData = getDataLinkData(SPHEREX_DATALINK_TABLE, false).find(({dlAnalysis}) => dlAnalysis.isCutout);
        // the real cutout target lookup: no search position, so it falls back to the row's center_ra and center_dec
        findCutoutTarget.mockImplementation(jest.requireActual('../../../ui/tap/Cutout.js').findCutoutTarget);
        getComponentState.mockReturnValue({});
        getCutoutSize.mockReturnValueOnce(0.01);

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: 'D1 science image',
            serDef: cutoutDlData.serDef, sourceTable: SPHEREX_DATALINK_TABLE, sourceRow: 1, idx: 1, positionWP: undefined,
            activateParams: {dpId: 'dpId-test'}, options: {dataProductsComponentKey: 'dp-key'},
            activeMenuLookupKey: 'lookup-key', menuKey: 'dlt-1', dlData: cutoutDlData});

        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'Cutout: D1 science image', enableCutout: true});
        const url = new URL(result.request.getURL());
        expect(url.origin + url.pathname).toBe('https://irsa.ipac.caltech.edu/ibe/cutout');
        expect(Object.fromEntries(url.searchParams)).toEqual(
            {size: '0.01', ra: '270.19211155920743', dec: '68.01568985908334', path: SPHEREX_FILE_PATH});
    });


    test('makeServiceDefDataProduct: a Rubin cutout with an xtype circle parameter becomes a cutout image', () => {
        const cutoutDlData = getDataLinkData(RUBIN_DATALINK_TABLE, false).find(({dlAnalysis}) => dlAnalysis.isCutout);
        const positionWP = makeWorldPt(53.1, -28.1);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION, positionWP});
        getCutoutSize.mockReturnValueOnce(0.01);
        getComponentState.mockReturnValueOnce({});

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: 'visit image',
            serDef: cutoutDlData.serDef, sourceTable: RUBIN_DATALINK_TABLE, sourceRow: 1, idx: 1, positionWP,
            activateParams: {dpId: 'dpId-test'}, options: {dataProductsComponentKey: 'dp-key'},
            activeMenuLookupKey: 'lookup-key', menuKey: 'dlt-0', dlData: cutoutDlData});

        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'Cutout: visit image', enableCutout: true,
            pixelBasedCutout: false});
        const url = new URL(result.request.getURL());
        expect(url.origin + url.pathname).toBe(RUBIN_CUTOUT_URL);
        // the empty POLYGON is not sent
        expect(Object.fromEntries(url.searchParams))
            .toEqual({ID: RUBIN_ID, CUTOUTDETAIL: 'Image', CIRCLE: '53.1 -28.1 0.01'});
    });

    test('makeServiceDefDataProduct: a CADC cutout with an xtype circle parameter becomes a cutout image', () => {
        const cutoutDlData = getDataLinkData(CADC_DAO_DATALINK_TABLE, false).find(({dlAnalysis}) => dlAnalysis.isCutout);
        const positionWP = makeWorldPt(148.89, 69.07);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION, positionWP});
        getCutoutSize.mockReturnValueOnce(0.01);
        getComponentState.mockReturnValueOnce({});

        const result = makeServiceDefDataProduct({dropDownText: undefined, name: 'dao_c182_2022_010463',
            serDef: cutoutDlData.serDef, sourceTable: CADC_DAO_DATALINK_TABLE, sourceRow: 1, idx: 1, positionWP,
            activateParams: {dpId: 'dpId-test'}, options: {dataProductsComponentKey: 'dp-key'},
            activeMenuLookupKey: 'lookup-key', menuKey: 'dlt-0', dlData: cutoutDlData});

        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'Cutout: dao_c182_2022_010463', enableCutout: true});
        const url = new URL(result.request.getURL());
        expect(url.origin + url.pathname).toBe(CADC_SODA_SYNC_URL);
        // the empty POS and POLYGON are not sent
        expect(Object.fromEntries(url.searchParams))
            .toEqual({ID: 'cadc:DAO/dao_c182_2022_010463.fits', CIRCLE: '148.89 69.07 0.01'});
    });

    test('makeUrlFromParams: user input of 0 replaces the descriptor value, empty input is not sent', () => {
        const [ztfSerDef] = getServiceDescriptors(ZTF_TABLE);
        const maskedSerDef = {...ztfSerDef, serDefParams: ztfSerDef.serDefParams.map((param) =>
            param.name === 'bad_catflags_mask' ? {...param, value: 32768} : param)};

        const url = new URL(makeUrlFromParams(ZTF_LIGHT_CURVE_URL, maskedSerDef, 1, {bad_catflags_mask: 0, time: ''}));

        expect(Object.fromEntries(url.searchParams))
            .toEqual({collection: 'ztf_dr3', id: '1684211300162373', bad_catflags_mask: '0'});
    });

    test('canMakeCutoutProduct: a descriptor with a size UCD can make a cutout', () => {
        const [cutoutDlData] = getDataLinkData(EUCLID_DATALINK_TABLE, false);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION,
            positionWP: makeWorldPt(236.1, 40.6)});

        expect(canMakeCutoutProduct(cutoutDlData.serDef, EUCLID_MER_TABLE, 0, {dataProductsComponentKey: 'dp-key'}))
            .toBe(true);
        expect(findCutoutTarget).toHaveBeenCalledWith('dp-key', cutoutDlData.serDef, EUCLID_MER_TABLE, 0);
    });

    test('canMakeCutoutProduct: a descriptor with an xtype circle parameter can make a cutout', () => {
        const cutoutDlData = getDataLinkData(RUBIN_DATALINK_TABLE, false).find(({dlAnalysis}) => dlAnalysis.isCutout);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION,
            positionWP: makeWorldPt(53.1, -28.1)});

        expect(canMakeCutoutProduct(cutoutDlData.serDef, RUBIN_DATALINK_TABLE, 1, {dataProductsComponentKey: 'dp-key'}))
            .toBe(true);
    });

    test('canMakeCutoutProduct: a SIA descriptor can make a cutout', () => {
        const [sodaSerDef] = getServiceDescriptors(CADC_SODA_TABLE);
        const siaSerDef = {...sodaSerDef, standardID: 'ivo://ivoa.net/std/SIA#query-2.0'};
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION,
            positionWP: makeWorldPt(149.06, 69.12)});

        expect(canMakeCutoutProduct(siaSerDef, CADC_SODA_TABLE, 0, {dataProductsComponentKey: 'dp-key'})).toBe(true);
    });

    test('canMakeCutoutProduct: a descriptor with a parameter named size can make a cutout', () => {
        // made up: none of the example files has a size parameter without a UCD
        const pixelSerDef = {accessURL: 'https://example.org/cutout', serDefParams: [
            {name: 'id', value: 'image-1'},
            {name: 'Size', value: '120'},
        ]};
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION,
            positionWP: makeWorldPt(10, 20)});

        expect(canMakeCutoutProduct(pixelSerDef, EUCLID_MER_TABLE, 0, {dataProductsComponentKey: 'dp-key'})).toBe(true);
    });

    test('canMakeCutoutProduct: a descriptor with only obs.field parameters cannot make a cutout', () => {
        const [sodaSerDef] = getServiceDescriptors(CADC_SODA_TABLE);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION,
            positionWP: makeWorldPt(149.06, 69.12)});

        expect(canMakeCutoutProduct(sodaSerDef, CADC_SODA_TABLE, 0, {dataProductsComponentKey: 'dp-key'})).toBe(false);
    });

    test('canMakeCutoutProduct: with no cutout target the descriptor ra and dec are used', () => {
        const [cutoutDlData] = getDataLinkData(EUCLID_DATALINK_TABLE, false);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: undefined, positionWP: undefined});

        expect(canMakeCutoutProduct(cutoutDlData.serDef, EUCLID_MER_TABLE, 0, {dataProductsComponentKey: 'dp-key'}))
            .toBe(true);
    });

    test('canMakeCutoutProduct: with no cutout target and no descriptor ra and dec no cutout is made', () => {
        const cutoutDlData = getDataLinkData(RUBIN_DATALINK_TABLE, false).find(({dlAnalysis}) => dlAnalysis.isCutout);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: undefined, positionWP: undefined});

        expect(canMakeCutoutProduct(cutoutDlData.serDef, RUBIN_DATALINK_TABLE, 1, {dataProductsComponentKey: 'dp-key'}))
            .toBe(false);
    });

    test('makeCutoutProduct: a cutout size of 0 makes nothing', () => {
        const [cutoutDlData] = getDataLinkData(EUCLID_DATALINK_TABLE, false);
        getCutoutSize.mockReturnValueOnce(0);

        const result = makeCutoutProduct({name: 'NIR-H', serDef: cutoutDlData.serDef, sourceTable: EUCLID_MER_TABLE,
            sourceRow: 0, idx: 0, activateParams: {dpId: 'dpId-test'}, dlData: cutoutDlData,
            options: {dataProductsComponentKey: 'dp-key'}, menuKey: 'dlt-0'});

        expect(result).toBeUndefined();
        expect(findCutoutTarget).not.toHaveBeenCalled();
    });

    test('makeCutoutProduct: with no cutout position nothing is made', () => {
        const [cutoutDlData] = getDataLinkData(EUCLID_DATALINK_TABLE, false);
        getCutoutSize.mockReturnValueOnce(0.01);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: undefined, positionWP: undefined});

        const result = makeCutoutProduct({name: 'NIR-H', serDef: cutoutDlData.serDef, sourceTable: EUCLID_MER_TABLE,
            sourceRow: 0, idx: 0, activateParams: {dpId: 'dpId-test'}, dlData: cutoutDlData,
            options: {dataProductsComponentKey: 'dp-key'}, menuKey: 'dlt-0'});

        expect(result).toBeUndefined();
    });

    test('makeCutoutProduct: at the default size the descriptor size is sent and saved as the cutout size', () => {
        const [cutoutDlData] = getDataLinkData(EUCLID_DATALINK_TABLE, false);
        const largerSerDef = {...cutoutDlData.serDef, serDefParams: cutoutDlData.serDef.serDefParams.map((param) =>
            param.name === 'size' ? {...param, value: '0.05'} : param)};
        const positionWP = makeWorldPt(236.1, 40.6);
        getCutoutSize.mockReturnValueOnce(SD_DEFAULT_SPACIAL_CUTOUT_SIZE);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION, positionWP});
        getComponentState.mockReturnValueOnce({});

        const result = makeCutoutProduct({name: 'NIR-H', serDef: largerSerDef, sourceTable: EUCLID_MER_TABLE,
            sourceRow: 0, idx: 0, activateParams: {dpId: 'dpId-test'}, dlData: cutoutDlData,
            options: {dataProductsComponentKey: 'dp-key'}, menuKey: 'dlt-0'});

        expect(new URL(result.request.getURL()).searchParams.get('size')).toBe('0.05');
        expect(setCutoutSize).toHaveBeenCalledWith('dp-key', 0.05, 'euclid-mer');
    });

    test('makeCutoutProduct: a pixel size from the dialog is sent without its px', () => {
        // made up: none of the example files has a size parameter without a UCD
        const pixelSerDef = {accessURL: 'https://example.org/cutout', serDefParams: [
            {name: 'id', value: 'image-1'},
            {name: 'size', value: '120'},
        ]};
        const positionWP = makeWorldPt(10, 20);
        getCutoutSize.mockReturnValueOnce('50px');
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION, positionWP});
        getComponentState.mockReturnValueOnce({});

        const result = makeCutoutProduct({name: 'image 1', serDef: pixelSerDef, sourceTable: EUCLID_MER_TABLE,
            sourceRow: 0, idx: 0, activateParams: {dpId: 'dpId-test'}, dlData: undefined,
            options: {dataProductsComponentKey: 'dp-key'}, menuKey: 'dlt-0'});

        expect(result).toMatchObject({displayType: DPtypes.IMAGE, name: 'Cutout: image 1', pixelBasedCutout: true});
        expect(result.request.getURL()).toBe('https://example.org/cutout?id=image-1&size=50');
        expect(setCutoutSize).not.toHaveBeenCalled();
    });

    test('makeCutoutProduct: a size in degrees for a pixel cutout is replaced by the descriptor pixel size', () => {
        // made up: none of the example files has a size parameter without a UCD
        const pixelSerDef = {accessURL: 'https://example.org/cutout', serDefParams: [
            {name: 'id', value: 'image-1'},
            {name: 'size', value: '120'},
        ]};
        getCutoutSize.mockReturnValueOnce(0.01);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION,
            positionWP: makeWorldPt(10, 20)});
        getComponentState.mockReturnValueOnce({});

        const result = makeCutoutProduct({name: 'image 1', serDef: pixelSerDef, sourceTable: EUCLID_MER_TABLE,
            sourceRow: 0, idx: 0, activateParams: {dpId: 'dpId-test'}, dlData: undefined,
            options: {dataProductsComponentKey: 'dp-key'}, menuKey: 'dlt-0'});

        expect(result.request.getURL()).toBe('https://example.org/cutout?id=image-1&size=120');
        expect(setCutoutSize).toHaveBeenCalledWith('dp-key', '120px', 'euclid-mer');
    });

    test('makeCutoutProduct: a pixel cutout with no descriptor size uses the default pixel size', () => {
        // made up: none of the example files has a size parameter without a UCD
        const pixelSerDef = {accessURL: 'https://example.org/cutout', serDefParams: [
            {name: 'id', value: 'image-1'},
            {name: 'size'},
        ]};
        getCutoutSize.mockReturnValueOnce(0.01);
        findCutoutTarget.mockReturnValue({requestedType: SEARCH_POSITION, foundType: SEARCH_POSITION,
            positionWP: makeWorldPt(10, 20)});
        getComponentState.mockReturnValueOnce({});

        const result = makeCutoutProduct({name: 'image 1', serDef: pixelSerDef, sourceTable: EUCLID_MER_TABLE,
            sourceRow: 0, idx: 0, activateParams: {dpId: 'dpId-test'}, dlData: undefined,
            options: {dataProductsComponentKey: 'dp-key'}, menuKey: 'dlt-0'});

        expect(result.request.getURL()).toBe(`https://example.org/cutout?id=image-1&size=${SD_DEFAULT_PIXEL_CUTOUT_SIZE}`);
        expect(setCutoutSize).toHaveBeenCalledWith('dp-key', `${SD_DEFAULT_PIXEL_CUTOUT_SIZE}px`, 'euclid-mer');
    });

    test('createServDescMenuRet: DataLink descriptors are left out and the rest become menu entries', () => {
        const [ztfSerDef] = getServiceDescriptors(ZTF_TABLE);
        const [sodaSerDef] = getServiceDescriptors(CADC_SODA_TABLE);
        isServiceDescriptorActivated.mockReturnValue(undefined);

        const result = createServDescMenuRet({descriptors: [ztfSerDef, sodaSerDef], positionWP: undefined,
            table: CADC_SODA_TABLE, row: 0, activateParams: {dpId: 'dpId-test'}, activeMenuLookupKey: 'lookup-key',
            options: {}});

        expect(result).toHaveLength(1);
        expect(result[0]).toMatchObject({displayType: DPtypes.ANALYZE, serDef: sodaSerDef, menuKey: 'serdesc-dlt-0',
            activeMenuLookupKey: 'lookup-key', allowsInput: true});
        expect(result[0].dropDownText).toMatch(/^Show: /);
        expect(isServiceDescriptorActivated).toHaveBeenCalledWith('dpId-test', sodaSerDef.internalServiceDescriptorID);
    });

    test('createServDescMenuRet: a descriptor the user already activated stays activated', () => {
        const [sodaSerDef] = getServiceDescriptors(CADC_SODA_TABLE);
        isServiceDescriptorActivated.mockReturnValue(true);
        getComponentState.mockReturnValueOnce({});

        createServDescMenuRet({descriptors: [sodaSerDef], positionWP: undefined, table: CADC_SODA_TABLE, row: 0,
            activateParams: {dpId: 'dpId-test'}, activeMenuLookupKey: 'lookup-key',
            options: {activateServiceDef: false, dataProductsComponentKey: 'dp-key'}});

        expect(dispatchSetSearchParams).toHaveBeenCalledWith(
            {dpId: 'dpId-test', autoActiveStatus: {[sodaSerDef.internalServiceDescriptorID]: true}});
    });

    test('createServDescMenuRet: for a catalog, a descriptor with no spatial input is activated', () => {
        // made up: a catalog with ra and dec columns and one descriptor that only takes the row id
        const catalogTable = {
            tbl_id: 'catalog', highlightedRow: 0, totalRows: 1, tableMeta: {}, request: {},
            resources: [{
                type: 'meta', utype: 'adhoc:service', ID: 'sd-details',
                params: [{name: 'accessURL', value: 'https://example.org/details'}],
                groups: [{name: 'inputParams', params: [{name: 'id', type: 'char', ref: 'col_0', value: ''}]}],
            }],
            tableData: {
                columns: [
                    {name: 'id', type: 'char', ID: 'col_0'},
                    {name: 'ra', type: 'double', UCD: 'pos.eq.ra;meta.main', units: 'deg'},
                    {name: 'dec', type: 'double', UCD: 'pos.eq.dec;meta.main', units: 'deg'},
                ],
                data: [['source-1', '10.5', '20.5']],
            },
        };
        const [detailsSerDef] = getServiceDescriptors(catalogTable);
        isServiceDescriptorActivated.mockReturnValue(undefined);
        getComponentState.mockReturnValueOnce({});

        const [result] = createServDescMenuRet({descriptors: [detailsSerDef], positionWP: undefined,
            table: catalogTable, row: 0, activateParams: {dpId: 'dpId-test'}, activeMenuLookupKey: 'lookup-key',
            options: {dataProductsComponentKey: 'dp-key'}});

        expect(result.url).toBe('https://example.org/details?id=source-1');
        expect(dispatchSetSearchParams).toHaveBeenCalledWith(
            {dpId: 'dpId-test', autoActiveStatus: {[detailsSerDef.internalServiceDescriptorID]: true}});
    });

    test('createServDescMenuRet: for a catalog, a descriptor with a spatial input is not activated', () => {
        // made up: a catalog with ra and dec columns and one descriptor that takes a search circle
        const catalogTable = {
            tbl_id: 'catalog', highlightedRow: 0, totalRows: 1, tableMeta: {}, request: {},
            resources: [{
                type: 'meta', utype: 'adhoc:service', ID: 'sd-cone',
                params: [{name: 'accessURL', value: 'https://example.org/cone'}],
                groups: [{name: 'inputParams', params: [
                    {name: 'CIRCLE', type: 'double', arraySize: '3', xtype: 'circle', units: 'deg', value: ''},
                ]}],
            }],
            tableData: {
                columns: [
                    {name: 'id', type: 'char', ID: 'col_0'},
                    {name: 'ra', type: 'double', UCD: 'pos.eq.ra;meta.main', units: 'deg'},
                    {name: 'dec', type: 'double', UCD: 'pos.eq.dec;meta.main', units: 'deg'},
                ],
                data: [['source-1', '10.5', '20.5']],
            },
        };
        const [coneSerDef] = getServiceDescriptors(catalogTable);
        isServiceDescriptorActivated.mockReturnValue(undefined);

        const [result] = createServDescMenuRet({descriptors: [coneSerDef], positionWP: undefined,
            table: catalogTable, row: 0, activateParams: {dpId: 'dpId-test'}, activeMenuLookupKey: 'lookup-key',
            options: {dataProductsComponentKey: 'dp-key'}});

        expect(result).toMatchObject({displayType: DPtypes.ANALYZE, allowsInput: true});
        expect(dispatchSetSearchParams).not.toHaveBeenCalled();
    });

    test('makeUrlFromParams: with no url nothing is made', () => {
        const [ztfSerDef] = getServiceDescriptors(ZTF_TABLE);

        expect(makeUrlFromParams(undefined, ztfSerDef, 1, {})).toBeUndefined();
    });

    test('makeUrlFromParams: with nothing to send the url is returned as given', () => {
        expect(makeUrlFromParams('https://example.org/service', {serDefParams: [{name: 'time', value: ''}]}, 0, {}))
            .toBe('https://example.org/service');
    });

    test('makeUrlFromParams: the context parameters are added to the url, not sent as one parameter', () => {
        const [ztfSerDef] = getServiceDescriptors(ZTF_TABLE);

        const url = new URL(makeUrlFromParams(ZTF_LIGHT_CURVE_URL, ztfSerDef, 0,
            {CONTEXT_PARAMS_STR: 'band=g&band=r'}));

        expect([...url.searchParams]).toEqual([['collection', 'ztf_dr3'], ['bad_catflags_mask', '0'],
            ['id', '686203400167748'], ['band', 'g'], ['band', 'r']]);
    });
});
