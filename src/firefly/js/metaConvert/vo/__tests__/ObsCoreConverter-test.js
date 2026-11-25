import {MetaConst} from '../../../data/MetaConst.js';
import {makePngEntry, uploadAndAnalyze} from '../../AnalysisUtils.js';
import {GROUP_BY_DATALINK_RESULT, GROUP_BY_RELATED_COLUMNS, IMAGE_ONLY} from '../../DataProductConst.js';
import {dispatchUpdateActiveKey} from '../../DataProductsCntlr.js';
import {DPtypes} from '../../DataProductsType.js';
import {createGuessDataType} from '../DataLinkProcessor.js';
import {
    createGridResult, getDatalinkRelatedImageGridProduct, getDatalinkSingleDataProduct,
    getObsCoreRelatedDataProductByFilter, makeDlUrl
} from '../DatalinkProducts.js';
import {
    getObsCoreDataProduct, getObsCoreGridDataProduct, getObsCoreRelatedDataProduct, getObsCoreRowMetaInfo,
    makeObsCoreConverter, makeSingleDataProductWithMenu
} from '../ObsCoreConverter.js';
import {createServDescMenuRet} from '../ServDescProducts.js';

// Loading these modules pulls in UI components whose dependencies (validator/es) Jest does not transform,
// so they are replaced with stubs; each test sets the return values it needs.
jest.mock('../../AnalysisUtils.js', () => ({
    makePngEntry: jest.fn(),
    uploadAndAnalyze: jest.fn(),
}));
jest.mock('../DataLinkProcessor.js', () => ({
    createGuessDataType: jest.fn(),
}));
jest.mock('../DatalinkProducts.js', () => ({
    createGridResult: jest.fn(),
    datalinkDescribeThreeColor: jest.fn(),
    getDatalinkRelatedImageGridProduct: jest.fn(),
    getDatalinkSingleDataProduct: jest.fn(),
    getObsCoreRelatedDataProductByFilter: jest.fn(),
    makeDlUrl: jest.fn(),
}));
jest.mock('../ServDescProducts.js', () => ({
    createServDescMenuRet: jest.fn(),
}));
// keep the real module, but don't dispatch into the store
jest.mock('../../DataProductsCntlr.js', () => ({
    ...jest.requireActual('../../DataProductsCntlr.js'),
    dispatchUpdateActiveKey: jest.fn(),
}));

// Rows 1 and 7 of a Rubin DP1 ivoa.ObsCore query, trimmed to the columns ObsCoreConverter reads.
// Every row is a DataLink row: access_format is application/x-votable+xml;content=datalink.
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
const RUBIN_VISIT_IMAGE_URL = 'https://data.lsst.cloud/api/datalink/links?ID=ivo%3A%2F%2Forg.rubinobs%2Flsst-dp1%3Frepo%3Ddp1%26id%3D019b1942-1e77-7652-b47b-f4301cfb6e95';

const RUBIN_RAW_ROW = [
    'LSST.DP1', 'image', 'lsst.raw', 1, 52.87362023749581, -28.376214735792995, 'CC_O_20241108_000253',
    'POLYGON ICRS 52.779071 -28.240995 52.721578 -28.461865 52.968449 -28.511367 53.025383 -28.290376',
    RUBIN_RAW_URL, 'application/x-votable+xml;content=datalink', 'LSSTComCam',
];

const RUBIN_VISIT_IMAGE_ROW = [
    'LSST.DP1', 'image', 'lsst.visit_image', 2, 52.90534765986676, -28.385397899576418, 'CC_O_20241109_000212',
    'POLYGON ICRS 52.810463 -28.250255 52.753310 -28.471344 53.000488 -28.520453 53.057142 -28.299308',
    RUBIN_VISIT_IMAGE_URL, 'application/x-votable+xml;content=datalink', 'LSSTComCam',
];

// The ObsCore table's own service descriptor, as Rubin sends it: accessURL is a ref to the access_url
// column with no value, and inputParams is empty, so getServiceDescriptors does not accept it.
const RUBIN_DATALINK_RESOURCE = {
    type: 'meta', utype: 'adhoc:service', desc: 'Datalink links service',
    params: [
        {name: 'standardID', value: 'ivo://ivoa.net/std/DataLink#links-1.0'},
        {name: 'accessURL', ref: 'col_24'},
    ],
    groups: [{name: 'inputParams', params: []}],
};

// A CADC OMM observation that is not served through DataLink: access_url points straight at a file.
// URLs and sizes are the #this (FITS) and #preview (JPEG) rows of CADC's DataLink response for
// ivo://cadc.nrc.ca/OMM?C140117_0290/C140117_0290_SCI; access_estsize is in kB, per ObsCore.
const CADC_OMM_COLUMNS = [
    {name: 'obs_collection', type: 'char'},
    {name: 'dataproduct_type', type: 'char'},
    {name: 'obs_id', type: 'char'},
    {name: 's_ra', type: 'double', units: 'deg'},
    {name: 's_dec', type: 'double', units: 'deg'},
    {name: 'access_url', type: 'char'},
    {name: 'access_format', type: 'char'},
    {name: 'access_estsize', type: 'long', units: 'kbyte'},
];

const CADC_OMM_FITS_URL = 'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/C140117_0290_SCI.fits';
const CADC_OMM_PREVIEW_URL = 'https://cadc-west-01.canfar.net/raven/files/cadc:OMM/C140117_0290_SCI_prev.jpg';

const CADC_OMM_FITS_ROW = [
    'OMM', 'image', 'C140117_0290', 149.05884191434205, 69.11976141466218,
    CADC_OMM_FITS_URL, 'application/fits', 16397,
];

const CADC_OMM_PREVIEW_ROW = [
    'OMM', 'image', 'C140117_0290', 149.05884191434205, 69.11976141466218,
    CADC_OMM_PREVIEW_URL, 'image/jpeg', 527,
];

// The CADC SODA sync cutout descriptor for this observation
const CADC_SODA_SYNC_RESOURCE = {
    type: 'meta', utype: 'adhoc:service', ID: 'soda-7447b7fc-cba5-4e19-b0a5-e7d6ea7528c2',
    params: [
        {name: 'resourceIdentifier', value: 'ivo://cadc.nrc.ca/OMM'},
        {name: 'standardID', value: 'ivo://ivoa.net/std/SODA#sync-1.0'},
        {name: 'contentType', value: 'application/fits'},
        {name: 'accessURL', value: 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/sync'},
    ],
    groups: [{
        name: 'inputParams',
        params: [
            {name: 'ID', UCD: 'meta.id;meta.dataset', value: 'cadc:OMM/C140117_0290_SCI.fits'},
            {name: 'POS', UCD: 'obs.field'},
            {name: 'CIRCLE', UCD: 'obs.field', units: 'deg'},
            {name: 'POLYGON', UCD: 'obs.field', units: 'deg'},
        ],
    }],
};

// None of the example files has a DataLink descriptor that getServiceDescriptors accepts, so this one
// follows the DataLink 1.1 form (accessURL value plus an ID param), pointed at CADC's links endpoint.
const CADC_DATALINK_RESOURCE = {
    type: 'meta', utype: 'adhoc:service', ID: 'cadc-datalink',
    params: [
        {name: 'standardID', value: 'ivo://ivoa.net/std/DataLink#links-1.1'},
        {name: 'accessURL', value: 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink'},
    ],
    groups: [{
        name: 'inputParams',
        params: [{name: 'ID', UCD: 'meta.id;meta.main', value: 'ivo://cadc.nrc.ca/OMM?C140117_0290/C140117_0290_SCI'}],
    }],
};

// The ObsCore entry from DataProductsFactory's converter list
const OBSCORE_CONVERTER_TEMPLATE = {
    converterId: 'ObsCore',
    threeColor: undefined,
    hasRelatedBands: undefined,
    canGrid: true,
    maxPlots: 8,
};

/**
 * Build an ObsCore TableModel from the Rubin fixture columns.
 * @param {object} props
 * @param props.rows - table data rows
 * @param props.prodTypeEnumVals - enumVals for dataproduct_type, which is how the server reports the distinct values
 * @param props.tableMeta
 * @param props.request
 * @return {TableModel}
 */
function makeRubinObsCoreTable({rows, prodTypeEnumVals, tableMeta, request}) {
    const columns = RUBIN_OBSCORE_COLUMNS.map((col) =>
        col.name === 'dataproduct_type' ? {...col, enumVals: prodTypeEnumVals} : col);
    return {
        tbl_id: 'rubin-obscore',
        highlightedRow: 0,
        totalRows: rows.length,
        tableMeta,
        request,
        tableData: {columns, data: rows},
    };
}

/**
 * Build an ObsCore TableModel from the CADC OMM fixture columns.
 * @param {object} props
 * @param props.rows - table data rows
 * @param props.resources - VOTable RESOURCE elements, i.e. service descriptors
 * @param props.tableMeta
 * @return {TableModel}
 */
function makeCadcOmmObsCoreTable({rows, resources, tableMeta}) {
    return {
        tbl_id: 'cadc-omm-obscore',
        highlightedRow: 0,
        totalRows: rows.length,
        tableMeta,
        request: {},
        resources,
        tableData: {columns: CADC_OMM_COLUMNS, data: rows},
    };
}

describe('ObsCoreConverter:', () => {

    // clearMocks (jest config) keeps queued mock*Once values, so an unused one would leak into the next test
    beforeEach(() => jest.resetAllMocks());

    test('makeObsCoreConverter: no table returns the template unchanged', () => {
        const converter = makeObsCoreConverter(undefined, OBSCORE_CONVERTER_TEMPLATE, {});
        expect(converter).toBe(OBSCORE_CONVERTER_TEMPLATE);
    });

    test('makeObsCoreConverter: default template on an image-only table', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW, RUBIN_VISIT_IMAGE_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const converter = makeObsCoreConverter(table, OBSCORE_CONVERTER_TEMPLATE, {});

        expect(converter.converterId).toBe('ObsCore-rubin-obscore');
        expect(converter.initialLayout).toBe('single');
        expect(converter.canGrid).toBe(true);
        expect(converter.maxPlots).toBe(8);
        expect(converter.threeColor).toBeUndefined();
        expect(converter.describeThreeColor).toBeUndefined();
        expect(converter.hasRelatedBands).toBeUndefined();
    });

    test('makeObsCoreConverter: canGrid is confirmed only when every product is an image or cube', () => {
        const imageAndCubeTable = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image,cube', tableMeta: {}, request: {}});
        expect(makeObsCoreConverter(imageAndCubeTable, OBSCORE_CONVERTER_TEMPLATE, {}).canGrid).toBe(true);

        const mixedTable = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image,spectrum', tableMeta: {}, request: {}});
        expect(makeObsCoreConverter(mixedTable, OBSCORE_CONVERTER_TEMPLATE, {}).canGrid).toBe(false);

        const noEnumValsTable = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: undefined, tableMeta: {}, request: {}});
        expect(makeObsCoreConverter(noEnumValsTable, OBSCORE_CONVERTER_TEMPLATE, {}).canGrid).toBe(false);
    });

    test('makeObsCoreConverter: a dataproduct_type IN filter of images confirms canGrid', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image,spectrum', tableMeta: {},
            request: {filters: 'dataproduct_type IN (\'image\')'}});
        expect(makeObsCoreConverter(table, OBSCORE_CONVERTER_TEMPLATE, {}).canGrid).toBe(true);
    });

    test('makeObsCoreConverter: options can force canGrid on a mixed table', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image,spectrum', tableMeta: {}, request: {}});

        const guaranteed = makeObsCoreConverter(table, OBSCORE_CONVERTER_TEMPLATE, {guaranteeOnlyImages: true});
        expect(guaranteed.canGrid).toBe(true);

        const imageOnly = makeObsCoreConverter(table, OBSCORE_CONVERTER_TEMPLATE, {limitViewerDisplay: IMAGE_ONLY});
        expect(imageOnly.canGrid).toBe(true);
    });

    test('makeObsCoreConverter: a template with canGrid false limits maxPlots to 1', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const template = {...OBSCORE_CONVERTER_TEMPLATE, canGrid: false};
        const converter = makeObsCoreConverter(table, template, {});

        expect(converter.canGrid).toBe(false);
        expect(converter.maxPlots).toBe(1);
    });

    test('makeObsCoreConverter: maxPlots follows the template canGrid, not the confirmed one', () => {
        // maxPlots follows the template canGrid; it only matters when canGrid is true
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image,spectrum', tableMeta: {}, request: {}});
        const converter = makeObsCoreConverter(table, OBSCORE_CONVERTER_TEMPLATE, {});

        expect(converter.canGrid).toBe(false);
        expect(converter.maxPlots).toBe(8);
    });

    test('makeObsCoreConverter: initialLayout and relatedGridImageOrder pass through', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const template = {...OBSCORE_CONVERTER_TEMPLATE, initialLayout: 'grid', relatedGridImageOrder: ['u', 'g', 'r']};
        const converter = makeObsCoreConverter(table, template, {});

        expect(converter.initialLayout).toBe('grid');
        expect(converter.relatedGridImageOrder).toEqual(['u', 'g', 'r']);
    });

    test('makeObsCoreConverter: hasRelatedBands is confirmed on an image-only DataLink table', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW, RUBIN_VISIT_IMAGE_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const template = {...OBSCORE_CONVERTER_TEMPLATE, hasRelatedBands: true};

        const byDatalink = makeObsCoreConverter(table, template, {relatedBandMethod: GROUP_BY_DATALINK_RESULT});
        expect(byDatalink.hasRelatedBands).toBe(true);
        expect(byDatalink.canGrid).toBe(true);
        expect(byDatalink.threeColor).toBe(true);
        expect(byDatalink.describeThreeColor).toEqual(expect.any(Function));

        const byColumns = makeObsCoreConverter(table, template, {relatedBandMethod: GROUP_BY_RELATED_COLUMNS});
        expect(byColumns.hasRelatedBands).toBe(true);
    });

    test('makeObsCoreConverter: hasRelatedBands is not confirmed for an unknown relatedBandMethod', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const template = {...OBSCORE_CONVERTER_TEMPLATE, hasRelatedBands: true};
        const converter = makeObsCoreConverter(table, template, {relatedBandMethod: 'NOT_A_METHOD'});

        expect(converter.hasRelatedBands).toBe(false);
    });

    test('makeObsCoreConverter: hasRelatedBands is not confirmed when the highlighted row is not an image', () => {
        const spectrumRow = [...RUBIN_RAW_ROW];
        spectrumRow[1] = 'spectrum';
        const table = makeRubinObsCoreTable({
            rows: [spectrumRow], prodTypeEnumVals: 'image,spectrum', tableMeta: {}, request: {}});
        const template = {...OBSCORE_CONVERTER_TEMPLATE, hasRelatedBands: true};
        const converter = makeObsCoreConverter(table, template, {});

        expect(converter.hasRelatedBands).toBe(false);
        // canGrid and threeColor follow the template so the toolbar stays stable when one highlighted row fails confirmation
        expect(converter.canGrid).toBe(true);
        expect(converter.threeColor).toBe(true);
    });

    test('makeObsCoreConverter: hasRelatedBands is not confirmed when the highlighted row has no access_url', () => {
        const noUrlRow = [...RUBIN_RAW_ROW];
        noUrlRow[8] = '';
        const table = makeRubinObsCoreTable({
            rows: [noUrlRow], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const template = {...OBSCORE_CONVERTER_TEMPLATE, hasRelatedBands: true};

        expect(makeObsCoreConverter(table, template, {}).hasRelatedBands).toBe(false);
    });

    test('makeObsCoreConverter: hasRelatedBands is not confirmed when the highlighted row is proprietary', () => {
        const table = {
            tbl_id: 'rubin-obscore',
            highlightedRow: 0,
            totalRows: 1,
            tableMeta: {[MetaConst.DATARIGHTS_COL]: 'data_rights'},
            request: {},
            tableData: {
                columns: [
                    ...RUBIN_OBSCORE_COLUMNS.map((col) => col.name === 'dataproduct_type' ? {...col, enumVals: 'image'} : col),
                    {name: 'data_rights', type: 'char'},
                ],
                data: [[...RUBIN_RAW_ROW, 'proprietary']],
            },
        };
        const template = {...OBSCORE_CONVERTER_TEMPLATE, hasRelatedBands: true};

        expect(makeObsCoreConverter(table, template, {}).hasRelatedBands).toBe(false);
    });

    test('makeObsCoreConverter: an explicit threeColor overrides hasRelatedBands', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const template = {...OBSCORE_CONVERTER_TEMPLATE, hasRelatedBands: true, threeColor: false};
        const converter = makeObsCoreConverter(table, template, {});

        expect(converter.threeColor).toBe(false);
        expect(converter.describeThreeColor).toBeUndefined();
    });


    test('getObsCoreRowMetaInfo: no table or a negative row returns an empty object', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        expect(getObsCoreRowMetaInfo(undefined, 0)).toEqual({});
        expect(getObsCoreRowMetaInfo(table, -1)).toEqual({});
    });

    test('getObsCoreRowMetaInfo: a Rubin DataLink row', () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW, RUBIN_VISIT_IMAGE_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        expect(getObsCoreRowMetaInfo(table, 1)).toEqual({
            iName: 'LSSTComCam', obsId: 'CC_O_20241109_000212', size: 0, dataSource: RUBIN_VISIT_IMAGE_URL,
            cloudAccess: undefined, prodType: 'image', isVoTable: true, isDataLinkRow: true, isPng: false,
        });
    });

    test('getObsCoreRowMetaInfo: CADC FITS and JPEG rows', () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW, CADC_OMM_PREVIEW_ROW], resources: [], tableMeta: {}});
        expect(getObsCoreRowMetaInfo(table, 0)).toEqual({
            iName: '', obsId: 'C140117_0290', size: 16397 * 1024, dataSource: CADC_OMM_FITS_URL,
            cloudAccess: undefined, prodType: 'image', isVoTable: false, isDataLinkRow: false, isPng: false,
        });
        expect(getObsCoreRowMetaInfo(table, 1)).toMatchObject({
            dataSource: CADC_OMM_PREVIEW_URL, isDataLinkRow: false, isPng: true,
        });
    });


    test('makeSingleDataProductWithMenu: without a service descriptor menu the primary product is returned as is', () => {
        const primDPType = {displayType: DPtypes.IMAGE, menuKey: 'prim-0'};
        expect(makeSingleDataProductWithMenu('dpId-test', primDPType, 100, undefined)).toBe(primDPType);
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });

    test('makeSingleDataProductWithMenu: the primary product is put first in the menu and made active', () => {
        const primDPType = {displayType: DPtypes.IMAGE, menuKey: 'prim-0'};
        const serviceDescMenuList = [{displayType: DPtypes.MESSAGE, menuKey: 'sd-0', activeMenuLookupKey: 'lookup-key'}];
        const result = makeSingleDataProductWithMenu('dpId-test', primDPType, 100, serviceDescMenuList);

        const expectedPrim = {...primDPType, semantics: '#this', size: 100, activeMenuLookupKey: 'lookup-key'};
        expect(result).toEqual({...expectedPrim, activeMenuLookupKey: 'lookup-key',
            menu: [expectedPrim, serviceDescMenuList[0]]});
        expect(dispatchUpdateActiveKey).toHaveBeenCalledWith(
            {dpId: 'dpId-test', activeMenuKeyChanges: {'lookup-key': 'prim-0'}});
    });

    test('makeSingleDataProductWithMenu: an existing semantics on the primary product is kept', () => {
        const primDPType = {displayType: DPtypes.IMAGE, menuKey: 'prim-0', semantics: '#preview'};
        const serviceDescMenuList = [{menuKey: 'sd-0', activeMenuLookupKey: 'lookup-key'}];
        const result = makeSingleDataProductWithMenu('dpId-test', primDPType, 100, serviceDescMenuList);
        expect(result.semantics).toBe('#preview');
    });

    test('makeSingleDataProductWithMenu: an empty service descriptor menu returns the primary product as is', () => {
        const primDPType = {displayType: DPtypes.IMAGE, menuKey: 'prim-0'};
        expect(makeSingleDataProductWithMenu('dpId-test', primDPType, 100, [])).toBe(primDPType);
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });

    test('makeSingleDataProductWithMenu: with several menu entries the primary leads and the first entry sets the lookup key', () => {
        const primDPType = {displayType: DPtypes.IMAGE, menuKey: 'prim-0'};
        const serviceDescMenuList = [
            {displayType: DPtypes.ANALYZE, menuKey: 'sd-0', activeMenuLookupKey: 'soda-lookup-key'},
            {displayType: DPtypes.PNG, menuKey: 'extraDl-dlt-1', activeMenuLookupKey: 'soda-lookup-key'},
            {displayType: DPtypes.ANALYZE, menuKey: 'extraDl-dlt-2', activeMenuLookupKey: 'soda-lookup-key'},
        ];

        const result = makeSingleDataProductWithMenu('dpId-test', primDPType, 100, serviceDescMenuList);

        expect(result.menu.map(({menuKey}) => menuKey)).toEqual(['prim-0', 'sd-0', 'extraDl-dlt-1', 'extraDl-dlt-2']);
        expect(result.menuKey).toBe('prim-0');
        expect(result.activeMenuLookupKey).toBe('soda-lookup-key');
        expect(dispatchUpdateActiveKey).toHaveBeenCalledWith(
            {dpId: 'dpId-test', activeMenuKeyChanges: {'soda-lookup-key': 'prim-0'}});
    });

    test('makeSingleDataProductWithMenu: the size and lookup key given replace the primary product values', () => {
        const primDPType = {displayType: DPtypes.IMAGE, menuKey: 'prim-0', size: 5, activeMenuLookupKey: 'old-key'};
        const serviceDescMenuList = [{menuKey: 'sd-0', activeMenuLookupKey: 'lookup-key'}];

        const result = makeSingleDataProductWithMenu('dpId-test', primDPType, 16397 * 1024, serviceDescMenuList);

        expect(result.menu[0]).toMatchObject({size: 16397 * 1024, activeMenuLookupKey: 'lookup-key'});
    });

    test('makeSingleDataProductWithMenu: the primary product passed in is not changed', () => {
        const primDPType = {displayType: DPtypes.IMAGE, menuKey: 'prim-0'};
        const serviceDescMenuList = [{menuKey: 'sd-0', activeMenuLookupKey: 'lookup-key'}];

        makeSingleDataProductWithMenu('dpId-test', primDPType, 100, serviceDescMenuList);

        expect(primDPType).toEqual({displayType: DPtypes.IMAGE, menuKey: 'prim-0'});
    });


    test('getObsCoreDataProduct: a DataLink row is handed to the DataLink product code', async () => {
        const table = {
            ...makeRubinObsCoreTable({
                rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}}),
            resources: [RUBIN_DATALINK_RESOURCE],
        };
        const activateParams = {dpId: 'dpId-test'};
        const options = {};
        const datalinkProduct = {displayType: DPtypes.IMAGE, menuKey: 'dl-0'};
        getDatalinkSingleDataProduct.mockResolvedValueOnce(datalinkProduct);

        const result = await getObsCoreDataProduct(table, 0, activateParams, options);

        expect(result).toBe(datalinkProduct);
        expect(getDatalinkSingleDataProduct).toHaveBeenCalledWith({
            dlTableUrl: RUBIN_RAW_URL, options, sourceTable: table, row: 0, activateParams,
            additionalServiceDescMenuList: undefined, doFileAnalysis: true, useForTableGrid: false,
        });
    });

    test('getObsCoreDataProduct: a row with no access_url is not supported', async () => {
        const noUrlRow = [...RUBIN_RAW_ROW];
        noUrlRow[8] = '';
        const table = makeRubinObsCoreTable({
            rows: [noUrlRow], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});

        const result = await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});
        expect(result).toEqual({displayType: DPtypes.MESSAGE, message: 'image is not supported', menuKey: 'message-0'});
    });

    test('getObsCoreDataProduct: a proprietary row with a release date says when it becomes public', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [[...CADC_OMM_FITS_ROW, '2099-01-01']], resources: [],
            tableMeta: {[MetaConst.RELEASE_DATE_COL]: 'obs_release_date'}});
        table.tableData = {...table.tableData, columns: [...CADC_OMM_COLUMNS, {name: 'obs_release_date', type: 'char'}]};

        const result = await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});
        expect(result.message).toBe('Proprietary data: the data publisher states it will become available on 2099-01-01');
    });

    test('getObsCoreDataProduct: a proprietary row without a release date says so', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [[...CADC_OMM_FITS_ROW, 'proprietary']], resources: [],
            tableMeta: {[MetaConst.DATARIGHTS_COL]: 'data_rights'}});
        table.tableData = {...table.tableData, columns: [...CADC_OMM_COLUMNS, {name: 'data_rights', type: 'char'}]};

        const result = await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});
        expect(result.message).toBe('Proprietary data; the data publisher has not provided a release date yet');
    });

    test('getObsCoreDataProduct: a JPEG row becomes a PNG entry', async () => {
        const table = makeCadcOmmObsCoreTable({rows: [CADC_OMM_PREVIEW_ROW], resources: [], tableMeta: {}});
        const pngEntry = {displayType: DPtypes.PNG, url: CADC_OMM_PREVIEW_URL};
        makePngEntry.mockReturnValueOnce(pngEntry);

        const result = await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});
        expect(result).toBe(pngEntry);
        expect(makePngEntry).toHaveBeenCalledWith(CADC_OMM_PREVIEW_URL);
    });

    test('getObsCoreDataProduct: a file over a gigabyte is offered as a download', async () => {
        const bigFitsRow = [...CADC_OMM_FITS_ROW];
        bigFitsRow[7] = 2 * 1024 * 1024; // 2 GB, in kB
        const table = makeCadcOmmObsCoreTable({rows: [bigFitsRow], resources: [], tableMeta: {}});

        const result = await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});
        expect(result.displayType).toBe(DPtypes.MESSAGE);
        expect(result.message).toBe('Data is too large to load');
        expect(result.menu[0].url).toBe(CADC_OMM_FITS_URL);
        expect(uploadAndAnalyze).not.toHaveBeenCalled();
    });

    test('getObsCoreDataProduct: a file just under a gigabyte is still analyzed', async () => {
        const justUnderGigRow = [...CADC_OMM_FITS_ROW];
        justUnderGigRow[7] = 1024 * 1024 - 1; // 1 GB less 1 kB, in kB
        const table = makeCadcOmmObsCoreTable({rows: [justUnderGigRow], resources: [], tableMeta: {}});
        const analyzedProduct = {displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'};
        uploadAndAnalyze.mockResolvedValueOnce(analyzedProduct);

        const result = await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});
        expect(result).toBe(analyzedProduct);
        expect(uploadAndAnalyze).toHaveBeenCalledTimes(1);
    });

    test('getObsCoreDataProduct: a FITS row is analyzed from its access_url', async () => {
        const table = makeCadcOmmObsCoreTable({rows: [CADC_OMM_FITS_ROW], resources: [], tableMeta: {}});
        const activateParams = {dpId: 'dpId-test'};
        const analyzedProduct = {displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'};
        uploadAndAnalyze.mockResolvedValueOnce(analyzedProduct);

        const result = await getObsCoreDataProduct(table, 0, activateParams, {});

        expect(result).toBe(analyzedProduct);
        const {request, ...otherArgs} = uploadAndAnalyze.mock.calls[0][0];
        expect(request.getURL()).toBe(CADC_OMM_FITS_URL);
        expect(otherArgs).toMatchObject({table, row: 0, activateParams, serviceDescMenuList: undefined});
        expect(getDatalinkSingleDataProduct).not.toHaveBeenCalled();
    });

    test('getObsCoreDataProduct: a DataLink row gets the SODA service descriptors as extra menu entries', async () => {
        const table = {
            ...makeRubinObsCoreTable({
                rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}}),
            resources: [CADC_SODA_SYNC_RESOURCE],
        };
        const sodaMenuEntry = {displayType: DPtypes.ANALYZE, menuKey: 'sd-0', activeMenuLookupKey: 'lookup-key'};
        createServDescMenuRet.mockReturnValueOnce([sodaMenuEntry]);
        getDatalinkSingleDataProduct.mockResolvedValueOnce({menuKey: 'dl-0'});

        await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        expect(getDatalinkSingleDataProduct.mock.calls[0][0]).toMatchObject({
            dlTableUrl: RUBIN_RAW_URL,
            additionalServiceDescMenuList: [{...sodaMenuEntry, semantics: '#additional-service-descriptor', size: 0}],
        });
        expect(uploadAndAnalyze).not.toHaveBeenCalled();
    });

    test('getObsCoreDataProduct: the request position is the table search target when there is one', async () => {
        const table = {
            ...makeCadcOmmObsCoreTable({rows: [CADC_OMM_FITS_ROW], resources: [], tableMeta: {}}),
            request: {UserTargetWorldPt: '150.5;70.25;EQ_J2000'},
        };
        uploadAndAnalyze.mockResolvedValueOnce({displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'});

        await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        const positionWP = uploadAndAnalyze.mock.calls[0][0].request.getWorldPt();
        expect(positionWP.getLon()).toBeCloseTo(150.5);
        expect(positionWP.getLat()).toBeCloseTo(70.25);
    });

    test('getObsCoreDataProduct: with no search target the request position is the row s_ra and s_dec', async () => {
        const table = makeCadcOmmObsCoreTable({rows: [CADC_OMM_FITS_ROW], resources: [], tableMeta: {}});
        uploadAndAnalyze.mockResolvedValueOnce({displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'});

        await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        const positionWP = uploadAndAnalyze.mock.calls[0][0].request.getWorldPt();
        expect(positionWP.getLon()).toBeCloseTo(149.05884191434205);
        expect(positionWP.getLat()).toBeCloseTo(69.11976141466218);
    });

    test('getObsCoreDataProduct: cloud_access GCS details are put on the request', async () => {
        const cloudAccess = {gcs: {project: 'test-project', bucket_name: 'test-bucket', object_name: 'omm/C140117_0290_SCI.fits'}};
        const baseTable = makeCadcOmmObsCoreTable({rows: [], resources: [], tableMeta: {}});
        const table = {
            ...baseTable,
            totalRows: 1,
            tableData: {
                columns: [...CADC_OMM_COLUMNS, {name: 'cloud_access', type: 'char'}],
                data: [[...CADC_OMM_FITS_ROW, JSON.stringify(cloudAccess)]],
            },
        };
        uploadAndAnalyze.mockResolvedValueOnce({displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'});

        await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        const {request} = uploadAndAnalyze.mock.calls[0][0];
        expect(request.getURL()).toBe(CADC_OMM_FITS_URL);
        expect([request.getGcsProject(), request.getGcsBucket(), request.getGcsObjName()])
            .toEqual(['test-project', 'test-bucket', 'omm/C140117_0290_SCI.fits']);
    });

    test('getObsCoreDataProduct: a SODA service descriptor is added to the menu after the primary product', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW], resources: [CADC_SODA_SYNC_RESOURCE], tableMeta: {}});
        const activateParams = {dpId: 'dpId-test'};
        const analyzedProduct = {displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'};
        const sodaMenuEntry = {displayType: DPtypes.ANALYZE, menuKey: 'sd-0',
            activeMenuLookupKey: 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/sync--cadc-omm-obscore--0'};
        createServDescMenuRet.mockReturnValueOnce([sodaMenuEntry]);
        uploadAndAnalyze.mockResolvedValueOnce(analyzedProduct);

        const result = await getObsCoreDataProduct(table, 0, activateParams, {});

        const servDescArgs = createServDescMenuRet.mock.calls[0][0];
        expect(servDescArgs.descriptors.map(({ID}) => ID)).toEqual(['soda-7447b7fc-cba5-4e19-b0a5-e7d6ea7528c2']);
        expect(servDescArgs.activeMenuLookupKey).toBe(sodaMenuEntry.activeMenuLookupKey);
        expect(servDescArgs.positionWP.getLon()).toBeCloseTo(149.05884191434205);

        const sodaWithDefaults = {...sodaMenuEntry, semantics: '#additional-service-descriptor', size: 0};
        expect(uploadAndAnalyze.mock.calls[0][0].serviceDescMenuList).toEqual([sodaWithDefaults]);
        expect(result.menu.map(({menuKey}) => menuKey)).toEqual(['analyzed-0', 'sd-0']);
        expect(result.size).toBe(16397 * 1024);
    });

    test('getObsCoreDataProduct: a DataLink service descriptor adds the DataLink entries to the menu', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW], resources: [CADC_SODA_SYNC_RESOURCE, CADC_DATALINK_RESOURCE], tableMeta: {}});
        const activateParams = {dpId: 'dpId-test'};
        const dlTableUrl = 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink?ID=ivo%3A%2F%2Fcadc.nrc.ca%2FOMM%3FC140117_0290%2FC140117_0290_SCI';
        const sodaMenuEntry = {menuKey: 'sd-0', activeMenuLookupKey: 'lookup-key'};
        createServDescMenuRet.mockReturnValueOnce([sodaMenuEntry]);
        makeDlUrl.mockReturnValueOnce(dlTableUrl);
        getDatalinkSingleDataProduct.mockResolvedValueOnce({menu: [{menuKey: 'dl-0'}, {menuKey: 'dl-1'}]});
        uploadAndAnalyze.mockResolvedValueOnce({displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'});

        const result = await getObsCoreDataProduct(table, 0, activateParams, {});

        expect(makeDlUrl.mock.calls[0][0].ID).toBe('cadc-datalink');
        expect(getDatalinkSingleDataProduct).toHaveBeenCalledWith(
            expect.objectContaining({dlTableUrl, sourceTable: table, row: 0, doFileAnalysis: true}));
        expect(result.menu.map(({menuKey}) => menuKey)).toEqual(['analyzed-0', 'sd-0', 'extraDl-dl-0', 'extraDl-dl-1']);
    });

    test('getObsCoreDataProduct: a DataLink service descriptor alone adds the DataLink entries to the menu', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW], resources: [CADC_DATALINK_RESOURCE], tableMeta: {}});
        const activateParams = {dpId: 'dpId-test'};
        const dlTableUrl = 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink?ID=ivo%3A%2F%2Fcadc.nrc.ca%2FOMM%3FC140117_0290%2FC140117_0290_SCI';
        const expectedLookupKey = 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink--cadc-omm-obscore--0';
        makeDlUrl.mockReturnValueOnce(dlTableUrl);
        getDatalinkSingleDataProduct.mockResolvedValueOnce({menu: [{menuKey: 'dl-0'}, {menuKey: 'dl-1'}]});
        uploadAndAnalyze.mockResolvedValueOnce({displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'});

        const result = await getObsCoreDataProduct(table, 0, activateParams, {});

        expect(createServDescMenuRet).not.toHaveBeenCalled();
        expect(makeDlUrl.mock.calls[0][0].ID).toBe('cadc-datalink');
        expect(getDatalinkSingleDataProduct).toHaveBeenCalledWith(
            expect.objectContaining({dlTableUrl, sourceTable: table, row: 0, doFileAnalysis: true}));
        expect(result.menu.map(({menuKey}) => menuKey)).toEqual(['analyzed-0', 'extraDl-dl-0', 'extraDl-dl-1']);
        expect(result.activeMenuLookupKey).toBe(expectedLookupKey);
        expect(dispatchUpdateActiveKey).toHaveBeenCalledWith(
            {dpId: 'dpId-test', activeMenuKeyChanges: {[expectedLookupKey]: 'analyzed-0'}});
    });

    test('getObsCoreDataProduct: a DataLink service descriptor that returns no menu leaves the primary product alone', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW], resources: [CADC_DATALINK_RESOURCE], tableMeta: {}});
        const analyzedProduct = {displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'};
        makeDlUrl.mockReturnValueOnce('https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink?ID=x');
        getDatalinkSingleDataProduct.mockResolvedValueOnce(
            {displayType: DPtypes.MESSAGE, message: 'No data available for this row', menu: undefined});
        uploadAndAnalyze.mockResolvedValueOnce(analyzedProduct);

        const result = await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        expect(result).toBe(analyzedProduct);
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });

    test('getObsCoreDataProduct: a one-row DataLink result that is not #this is added to the menu', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW], resources: [CADC_DATALINK_RESOURCE], tableMeta: {}});
        makeDlUrl.mockReturnValueOnce('https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink?ID=x');
        getDatalinkSingleDataProduct.mockResolvedValueOnce({
            displayType: DPtypes.PNG, menuKey: 'dlt-0', menu: [],
            dlData: {semantics: '#preview', dlAnalysis: {isThis: false}},
        });
        uploadAndAnalyze.mockResolvedValueOnce({displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'});

        const result = await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        expect(result.menu.map(({menuKey}) => menuKey)).toEqual(['analyzed-0', 'extraDl-dlt-0']);
    });

    test('getObsCoreDataProduct: a one-row #this DataLink result leaves the primary product alone', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW], resources: [CADC_DATALINK_RESOURCE], tableMeta: {}});
        const analyzedProduct = {displayType: DPtypes.IMAGE, menuKey: 'analyzed-0'};
        makeDlUrl.mockReturnValueOnce('https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink?ID=x');
        getDatalinkSingleDataProduct.mockResolvedValueOnce({
            displayType: DPtypes.ANALYZE, menuKey: 'dlt-0', menu: [],
            dlData: {semantics: '#this', dlAnalysis: {isThis: true}},
        });
        uploadAndAnalyze.mockResolvedValueOnce(analyzedProduct);

        const result = await getObsCoreDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        expect(result).toBe(analyzedProduct);
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });


    test('getObsCoreGridDataProduct: each plot row is built without file analysis and passed to createGridResult', async () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW, RUBIN_VISIT_IMAGE_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const activateParams = {dpId: 'dpId-test'};
        const options = {};
        const plotRows = [{row: 0, highlight: true}, {row: 1, highlight: false}];
        getDatalinkSingleDataProduct
            .mockResolvedValueOnce({menuKey: 'raw-0'})
            .mockResolvedValueOnce({menuKey: 'visit-0'});
        createGridResult.mockReturnValueOnce('grid-result');

        const result = getObsCoreGridDataProduct(table, plotRows, activateParams, options);

        expect(result).toBe('grid-result');
        const [promiseAry, gridActivateParams, gridTable, gridPlotRows, gridOptions] = createGridResult.mock.calls[0];
        expect(await Promise.all(promiseAry)).toEqual([{menuKey: 'raw-0'}, {menuKey: 'visit-0'}]);
        expect([gridActivateParams, gridTable, gridPlotRows, gridOptions]).toEqual([activateParams, table, plotRows, options]);
        expect(getDatalinkSingleDataProduct.mock.calls.map(([args]) => args.dlTableUrl))
            .toEqual([RUBIN_RAW_URL, RUBIN_VISIT_IMAGE_URL]);
        expect(getDatalinkSingleDataProduct.mock.calls[0][0])
            .toMatchObject({doFileAnalysis: false, useForTableGrid: true});
    });

    test('getObsCoreGridDataProduct: a FITS row is guessed from its content instead of analyzed', async () => {
        const table = makeCadcOmmObsCoreTable({rows: [CADC_OMM_FITS_ROW], resources: [], tableMeta: {}});
        const guessedProduct = {displayType: DPtypes.IMAGE, menuKey: 'guess-0'};
        createGuessDataType.mockReturnValueOnce(guessedProduct);

        getObsCoreGridDataProduct(table, [{row: 0}], {dpId: 'dpId-test'}, {});

        const [promiseAry] = createGridResult.mock.calls[0];
        expect(await Promise.all(promiseAry)).toEqual([guessedProduct]);
        expect(createGuessDataType).toHaveBeenCalledWith(
            expect.objectContaining({url: CADC_OMM_FITS_URL, ct: 'image', size: 16397 * 1024, table, row: 0}));
        expect(uploadAndAnalyze).not.toHaveBeenCalled();
    });

    test('getObsCoreGridDataProduct: each row keeps its slot, and a row with no access_url shows a message there', async () => {
        const noUrlRow = [...CADC_OMM_FITS_ROW];
        noUrlRow[5] = '';
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW, noUrlRow, CADC_OMM_PREVIEW_ROW], resources: [], tableMeta: {}});
        const guessedProduct = {displayType: DPtypes.IMAGE, menuKey: 'guess-0'};
        const pngEntry = {displayType: DPtypes.PNG, menuKey: 'png-0'};
        createGuessDataType.mockReturnValueOnce(guessedProduct);
        makePngEntry.mockReturnValueOnce(pngEntry);

        getObsCoreGridDataProduct(table, [{row: 0}, {row: 1}, {row: 2}], {dpId: 'dpId-test'}, {});

        const [promiseAry] = createGridResult.mock.calls[0];
        const products = await Promise.all(promiseAry);
        expect(products[0]).toBe(guessedProduct);
        expect(products[1]).toMatchObject({displayType: DPtypes.MESSAGE, message: 'image is not supported'});
        expect(products[2]).toBe(pngEntry);
        expect(makePngEntry).toHaveBeenCalledWith(CADC_OMM_PREVIEW_URL);
    });

    test('getObsCoreGridDataProduct: a file over a gigabyte is offered as a download in its slot', async () => {
        const bigFitsRow = [...CADC_OMM_FITS_ROW];
        bigFitsRow[7] = 2 * 1024 * 1024; // 2 GB, in kB
        const table = makeCadcOmmObsCoreTable({rows: [bigFitsRow], resources: [], tableMeta: {}});

        getObsCoreGridDataProduct(table, [{row: 0}], {dpId: 'dpId-test'}, {});

        const [promiseAry] = createGridResult.mock.calls[0];
        const [product] = await Promise.all(promiseAry);
        expect(product).toMatchObject({displayType: DPtypes.MESSAGE, message: 'Data is too large to load'});
        expect(createGuessDataType).not.toHaveBeenCalled();
    });

    test('getObsCoreGridDataProduct: service descriptors are not added to grid products', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW], resources: [CADC_SODA_SYNC_RESOURCE, CADC_DATALINK_RESOURCE], tableMeta: {}});
        const guessedProduct = {displayType: DPtypes.IMAGE, menuKey: 'guess-0'};
        createGuessDataType.mockReturnValueOnce(guessedProduct);

        getObsCoreGridDataProduct(table, [{row: 0}], {dpId: 'dpId-test'}, {});

        const [promiseAry] = createGridResult.mock.calls[0];
        expect(await Promise.all(promiseAry)).toEqual([guessedProduct]);
        expect(createServDescMenuRet).not.toHaveBeenCalled();
        expect(getDatalinkSingleDataProduct).not.toHaveBeenCalled();
        expect(dispatchUpdateActiveKey).not.toHaveBeenCalled();
    });

    test('getObsCoreGridDataProduct: a DataLink row gets no extra service descriptor menu', async () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const options = {hasRelatedBands: true};
        getDatalinkSingleDataProduct.mockResolvedValueOnce({menuKey: 'raw-0'});

        getObsCoreGridDataProduct(table, [{row: 0}], {dpId: 'dpId-test'}, options);

        const [promiseAry] = createGridResult.mock.calls[0];
        expect(await Promise.all(promiseAry)).toEqual([{menuKey: 'raw-0'}]);
        expect(getDatalinkSingleDataProduct).toHaveBeenCalledWith(expect.objectContaining({
            dlTableUrl: RUBIN_RAW_URL, sourceTable: table, row: 0, options,
            additionalServiceDescMenuList: undefined, doFileAnalysis: false, useForTableGrid: true,
        }));
    });

    test('getObsCoreGridDataProduct: no plot rows passes an empty list to createGridResult', () => {
        const table = makeCadcOmmObsCoreTable({rows: [CADC_OMM_FITS_ROW], resources: [], tableMeta: {}});
        const activateParams = {dpId: 'dpId-test'};

        getObsCoreGridDataProduct(table, [], activateParams, {});

        expect(createGridResult).toHaveBeenCalledWith([], activateParams, table, [], {});
    });


    test('getObsCoreRelatedDataProduct: rejects when hasRelatedBands is not set', async () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        await expect(getObsCoreRelatedDataProduct(table, 0, undefined, undefined, {dpId: 'dpId-test'}, undefined))
            .rejects.toBe('related data products not supported');
    });

    test('getObsCoreRelatedDataProduct: an unknown relatedBandMethod returns a message', async () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const options = {hasRelatedBands: true, relatedBandMethod: 'NOT_A_METHOD'};
        const result = await getObsCoreRelatedDataProduct(table, 0, undefined, undefined, {dpId: 'dpId-test'}, options);
        expect(result.message).toBe('related data products not supported (related band method not supported: NOT_A_METHOD)');
    });

    test('getObsCoreRelatedDataProduct: rows that cannot be grouped return a message', async () => {
        const options = {hasRelatedBands: true};
        const activateParams = {dpId: 'dpId-test'};

        const noUrlRow = [...RUBIN_RAW_ROW];
        noUrlRow[8] = '';
        const noUrlTable = makeRubinObsCoreTable({
            rows: [noUrlRow], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        expect((await getObsCoreRelatedDataProduct(noUrlTable, 0, undefined, undefined, activateParams, options)).message)
            .toBe('image is not supported');

        const spectrumRow = [...RUBIN_RAW_ROW];
        spectrumRow[1] = 'spectrum';
        const spectrumTable = makeRubinObsCoreTable({
            rows: [spectrumRow], prodTypeEnumVals: 'image,spectrum', tableMeta: {}, request: {}});
        expect((await getObsCoreRelatedDataProduct(spectrumTable, 0, undefined, undefined, activateParams, options)).message)
            .toBe('spectrum is not supported for grid');

        const previewTable = makeCadcOmmObsCoreTable({rows: [CADC_OMM_PREVIEW_ROW], resources: [], tableMeta: {}});
        expect((await getObsCoreRelatedDataProduct(previewTable, 0, undefined, undefined, activateParams, options)).message)
            .toBe('image must be FITS for related grid support');

        const fitsTable = makeCadcOmmObsCoreTable({rows: [CADC_OMM_FITS_ROW], resources: [], tableMeta: {}});
        expect((await getObsCoreRelatedDataProduct(fitsTable, 0, undefined, undefined, activateParams, options)).message)
            .toBe('datalink is required for related grid support');

        expect(getDatalinkRelatedImageGridProduct).not.toHaveBeenCalled();
        expect(getObsCoreRelatedDataProductByFilter).not.toHaveBeenCalled();
    });

    test('getObsCoreRelatedDataProduct: GROUP_BY_DATALINK_RESULT builds the grid from the DataLink table', async () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const activateParams = {dpId: 'dpId-test'};
        const threeColorOps = [true, false, true];
        const options = {hasRelatedBands: true, relatedBandMethod: GROUP_BY_DATALINK_RESULT};
        getDatalinkRelatedImageGridProduct.mockResolvedValueOnce('related-grid');

        const result = await getObsCoreRelatedDataProduct(table, 0, threeColorOps, 'plot-1', activateParams, options);

        expect(result).toBe('related-grid');
        expect(getDatalinkRelatedImageGridProduct).toHaveBeenCalledWith(
            {dlTableUrl: RUBIN_RAW_URL, activateParams, table, row: 0, threeColorOps, options});
    });

    test('getObsCoreRelatedDataProduct: GROUP_BY_RELATED_COLUMNS without tbl.relatedCols returns a message', async () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const options = {hasRelatedBands: true, relatedBandMethod: GROUP_BY_RELATED_COLUMNS};

        const result = await getObsCoreRelatedDataProduct(table, 0, undefined, 'plot-1', {dpId: 'dpId-test'}, options);

        expect(result.message).toBe('meta data tbl.relatedCols is not configured');
        expect(getObsCoreRelatedDataProductByFilter).not.toHaveBeenCalled();
    });

    test('getObsCoreRelatedDataProduct: GROUP_BY_RELATED_COLUMNS filters the table for related rows', async () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image',
            tableMeta: {'tbl.relatedCols': 'lsst_visit,lsst_detector'}, request: {}});
        const activateParams = {dpId: 'dpId-test'};
        const threeColorOps = [true, false, true];
        const options = {hasRelatedBands: true, relatedBandMethod: GROUP_BY_RELATED_COLUMNS};
        getObsCoreRelatedDataProductByFilter.mockResolvedValueOnce('filtered-grid');

        const result = await getObsCoreRelatedDataProduct(table, 0, threeColorOps, 'plot-1', activateParams, options);

        expect(result).toBe('filtered-grid');
        expect(getObsCoreRelatedDataProductByFilter).toHaveBeenCalledWith(
            table, 0, threeColorOps, 'plot-1', activateParams, options);
        expect(getDatalinkRelatedImageGridProduct).not.toHaveBeenCalled();
    });

    test('getObsCoreRelatedDataProduct: rejects when hasRelatedBands is false', async () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const options = {hasRelatedBands: false, relatedBandMethod: GROUP_BY_DATALINK_RESULT};
        await expect(getObsCoreRelatedDataProduct(table, 0, undefined, undefined, {dpId: 'dpId-test'}, options))
            .rejects.toBe('related data products not supported');
        expect(getDatalinkRelatedImageGridProduct).not.toHaveBeenCalled();
    });

    test('getObsCoreRelatedDataProduct: with no relatedBandMethod the grid comes from the DataLink table', async () => {
        const table = makeRubinObsCoreTable({
            rows: [RUBIN_RAW_ROW], prodTypeEnumVals: 'image', tableMeta: {}, request: {}});
        const activateParams = {dpId: 'dpId-test'};
        const options = {hasRelatedBands: true};
        getDatalinkRelatedImageGridProduct.mockResolvedValueOnce('related-grid');

        const result = await getObsCoreRelatedDataProduct(table, 0, undefined, 'plot-1', activateParams, options);

        expect(result).toBe('related-grid');
        expect(getDatalinkRelatedImageGridProduct).toHaveBeenCalledWith(
            {dlTableUrl: RUBIN_RAW_URL, activateParams, table, row: 0, threeColorOps: undefined, options});
        expect(getObsCoreRelatedDataProductByFilter).not.toHaveBeenCalled();
    });

    test('getObsCoreRelatedDataProduct: a proprietary DataLink row returns a message', async () => {
        const table = makeRubinObsCoreTable({
            rows: [[...RUBIN_RAW_ROW, 'proprietary']], prodTypeEnumVals: 'image',
            tableMeta: {[MetaConst.DATARIGHTS_COL]: 'data_rights'}, request: {}});
        table.tableData = {...table.tableData, columns: [...table.tableData.columns, {name: 'data_rights', type: 'char'}]};
        const options = {hasRelatedBands: true, relatedBandMethod: GROUP_BY_DATALINK_RESULT};

        const result = await getObsCoreRelatedDataProduct(table, 0, undefined, 'plot-1', {dpId: 'dpId-test'}, options);

        expect(result.message).toBe('Proprietary data; the data publisher has not provided a release date yet');
        expect(getDatalinkRelatedImageGridProduct).not.toHaveBeenCalled();
    });

    test('getObsCoreRelatedDataProduct: GROUP_BY_RELATED_COLUMNS also needs a DataLink row', async () => {
        const table = makeCadcOmmObsCoreTable({
            rows: [CADC_OMM_FITS_ROW], resources: [], tableMeta: {'tbl.relatedCols': 'obs_id'}});
        const options = {hasRelatedBands: true, relatedBandMethod: GROUP_BY_RELATED_COLUMNS};

        const result = await getObsCoreRelatedDataProduct(table, 0, undefined, 'plot-1', {dpId: 'dpId-test'}, options);

        expect(result.message).toBe('datalink is required for related grid support');
        expect(getObsCoreRelatedDataProductByFilter).not.toHaveBeenCalled();
    });
});
