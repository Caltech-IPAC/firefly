import {MetaConst} from '../../../data/MetaConst.js';
import {getActiveMenuKey} from '../../DataProductsCntlr.js';
import {DPtypes} from '../../DataProductsType.js';
import {
    createGridResult, datalinkDescribeThreeColor, getDatalinkRelatedImageGridProduct, getDatalinkSingleDataProduct,
    makeDlUrl
} from '../DatalinkProducts.js';
import {
    getServiceDescGridDataProduct, getServiceDescRelatedDataProduct, getServiceDescSingleDataProduct,
    makeServDescriptorConverter
} from '../ServDescConverter.js';
import {createServDescMenuRet} from '../ServDescProducts.js';

// Loading these modules pulls in UI components whose dependencies (validator/es) Jest does not transform,
// so they are replaced with stubs; each test sets the return values it needs.
jest.mock('../DatalinkProducts.js', () => ({
    createGridResult: jest.fn(),
    datalinkDescribeThreeColor: jest.fn(),
    getDatalinkRelatedImageGridProduct: jest.fn(),
    getDatalinkSingleDataProduct: jest.fn(),
    makeDlUrl: jest.fn(),
}));
jest.mock('../ServDescProducts.js', () => ({
    createServDescMenuRet: jest.fn(),
}));
// keep the real module, but read the active menu key from the test instead of the store
jest.mock('../../DataProductsCntlr.js', () => ({
    ...jest.requireActual('../../DataProductsCntlr.js'),
    getActiveMenuKey: jest.fn(),
}));

// ZTF light curve catalog (serDescTbl.xml): each row is an object id, and a DataLink service descriptor
// builds the links URL from the oid column.
const ZTF_COLUMNS = [{name: 'oid', type: 'long', ID: 'col_0', UCD: 'meta.id;src;meta.main'}];
const ZTF_ROWS = [['686203400167748'], ['1684211300162373']];
const ZTF_LIGHT_CURVE_URL = 'https://irsa.ipac.caltech.edu/cgi-bin/ZTF/nph_light_curves?collection=ztf_dr3';
const ZTF_INPUT_PARAMS = [
    {name: 'id', type: 'char', ref: 'col_0', value: ''},
    {name: 'bad_catflags_mask', type: 'int', value: '0'},
    {name: 'time', type: 'char', value: ''},
];
const ZTF_DATALINK_RESOURCE = {
    type: 'meta', utype: 'adhoc:service', desc: 'Link to ZTF light curve for ztf_dr3 collection',
    params: [
        {name: 'standardID', value: 'ivo://ivoa.net/std/DataLink#links-1.0'},
        {name: 'accessURL', value: ZTF_LIGHT_CURVE_URL},
    ],
    groups: [{name: 'inputParams', params: ZTF_INPUT_PARAMS}],
};
// the second descriptor in ztf-search-result-enhanced.xml: opens the light curve in Firefly, not a DataLink service
const ZTF_FIREFLY_LC_RESOURCE = {
    type: 'meta', utype: 'adhoc:service', desc: 'Show ZTF light curve in Firefly',
    params: [
        {name: 'standardID', value: 'abc'},
        {name: 'accessURL', value: 'http://localhost:8080/firefly/ts.html?api=lc'},
    ],
    groups: [{name: 'inputParams', params: [
        ...ZTF_INPUT_PARAMS,
        {name: 'dataset', type: 'char', value: 'ztf'},
        {name: 'execute', type: 'char', value: 'true'},
    ]}],
};
// Rubin's ObsCore DataLink descriptor (rubin-obscore-data.vot) takes accessURL from the access_url column,
// so the PARAM has a ref and no value
const RUBIN_DATALINK_RESOURCE = {
    type: 'meta', utype: 'adhoc:service', desc: 'Datalink links service',
    params: [
        {name: 'standardID', value: 'ivo://ivoa.net/std/DataLink#links-1.0'},
        {name: 'accessURL', ref: 'col_24'},
    ],
    groups: [{name: 'inputParams', params: []}],
};

const SERVICE_DESCRIPTORS_TEMPLATE = {
    converterId: 'ServiceDescriptors', threeColor: undefined, hasRelatedBands: undefined, canGrid: undefined, maxPlots: 8,
};

/**
 * Build a ZTF catalog TableModel with the given service descriptor resources.
 * @param {Object} p
 * @param {Array.<Object>} p.resources
 * @param {Object} p.tableMeta
 * @param {Object} p.request
 * @return {TableModel}
 */
function makeZtfTable({resources, tableMeta, request}) {
    return {
        tbl_id: 'ztf-lc', highlightedRow: 0, totalRows: ZTF_ROWS.length, tableMeta, request, resources,
        tableData: {columns: ZTF_COLUMNS, data: ZTF_ROWS},
    };
}

/**
 * Build a ZTF catalog TableModel whose rows have a data_rights column.
 * @param {Object} p
 * @param {Array.<Object>} p.resources
 * @param {Array.<string>} p.dataRights - one entry per row
 * @return {TableModel}
 */
function makeZtfTableWithDataRights({resources, dataRights}) {
    return {
        tbl_id: 'ztf-lc', highlightedRow: 0, totalRows: ZTF_ROWS.length,
        tableMeta: {[MetaConst.DATARIGHTS_COL]: 'data_rights'}, request: {}, resources,
        tableData: {
            columns: [...ZTF_COLUMNS, {name: 'data_rights', type: 'char'}],
            data: ZTF_ROWS.map((row, idx) => [...row, dataRights[idx]]),
        },
    };
}

describe('ServDescConverter:', () => {

    // clearMocks (jest config) keeps queued mock*Once values, so an unused one would leak into the next test
    beforeEach(() => jest.resetAllMocks());

    test('makeServDescriptorConverter: no table returns the template unchanged', () => {
        expect(makeServDescriptorConverter(undefined, SERVICE_DESCRIPTORS_TEMPLATE, {})).toBe(SERVICE_DESCRIPTORS_TEMPLATE);
    });

    test('makeServDescriptorConverter: a table without a DataLink service descriptor returns the template unchanged', () => {
        const table = makeZtfTable({resources: [ZTF_FIREFLY_LC_RESOURCE], tableMeta: {}, request: {}});
        expect(makeServDescriptorConverter(table, SERVICE_DESCRIPTORS_TEMPLATE, {})).toBe(SERVICE_DESCRIPTORS_TEMPLATE);
    });

    test('makeServDescriptorConverter: a DataLink descriptor whose accessURL is only a column ref is ignored', () => {
        const table = makeZtfTable({resources: [RUBIN_DATALINK_RESOURCE], tableMeta: {}, request: {}});
        expect(makeServDescriptorConverter(table, SERVICE_DESCRIPTORS_TEMPLATE, {})).toBe(SERVICE_DESCRIPTORS_TEMPLATE);
    });

    test('makeServDescriptorConverter: the default template on a DataLink table is single view with no related bands', () => {
        const table = makeZtfTable({resources: [ZTF_DATALINK_RESOURCE], tableMeta: {}, request: {}});
        const converter = makeServDescriptorConverter(table, SERVICE_DESCRIPTORS_TEMPLATE, {relatedGridImageOrder: ['g', 'r']});

        expect(converter).toEqual({
            ...SERVICE_DESCRIPTORS_TEMPLATE,
            initialLayout: 'single',
            describeThreeColor: undefined,
            threeColor: false,
            canGrid: false,
            maxPlots: 8,
            hasRelatedBands: false,
            converterId: 'ServiceDef-ztf-lc',
            relatedGridImageOrder: ['g', 'r'],
        });
    });

    test('makeServDescriptorConverter: hasRelatedBands turns on grid and three color', () => {
        const table = makeZtfTable({resources: [ZTF_DATALINK_RESOURCE], tableMeta: {}, request: {}});
        const template = {...SERVICE_DESCRIPTORS_TEMPLATE, hasRelatedBands: true};
        const converter = makeServDescriptorConverter(table, template, {});

        expect(converter).toMatchObject({hasRelatedBands: true, canGrid: true, threeColor: true});
        expect(converter.describeThreeColor).toEqual(expect.any(Function));
    });

    test('makeServDescriptorConverter: template values override the hasRelatedBands defaults', () => {
        const table = makeZtfTable({resources: [ZTF_DATALINK_RESOURCE], tableMeta: {}, request: {}});
        const template = {
            converterId: 'ServiceDescriptors', hasRelatedBands: true, canGrid: false, threeColor: false,
            maxPlots: undefined, initialLayout: 'gridRelated',
        };
        const converter = makeServDescriptorConverter(table, template, {});

        expect(converter).toMatchObject(
            {hasRelatedBands: true, canGrid: false, threeColor: false, maxPlots: 1, initialLayout: 'gridRelated'});
        expect(converter.describeThreeColor).toBeUndefined();
    });

    test('makeServDescriptorConverter: describeThreeColor asks the DataLink table for the row', async () => {
        const table = makeZtfTable({resources: [ZTF_DATALINK_RESOURCE], tableMeta: {}, request: {}});
        const template = {...SERVICE_DESCRIPTORS_TEMPLATE, hasRelatedBands: true};
        const options = {hasRelatedBands: true};
        const dlTableUrl = `${ZTF_LIGHT_CURVE_URL}&id=1684211300162373&bad_catflags_mask=0`;
        makeDlUrl.mockReturnValueOnce(dlTableUrl);
        datalinkDescribeThreeColor.mockResolvedValueOnce('three-color-description');

        const {describeThreeColor} = makeServDescriptorConverter(table, template, {});

        expect(await describeThreeColor(table, 1, options)).toBe('three-color-description');
        expect(makeDlUrl.mock.calls[0][0].accessURL).toBe(ZTF_LIGHT_CURVE_URL);
        expect(makeDlUrl.mock.calls[0].slice(1)).toEqual([table, 1]);
        expect(datalinkDescribeThreeColor).toHaveBeenCalledWith(dlTableUrl, table, 1, options);
    });

    test('makeServDescriptorConverter: describeThreeColor gives nothing for a row without access', () => {
        const table = makeZtfTableWithDataRights({resources: [ZTF_DATALINK_RESOURCE], dataRights: ['public', 'proprietary']});
        const template = {...SERVICE_DESCRIPTORS_TEMPLATE, hasRelatedBands: true};

        const {describeThreeColor} = makeServDescriptorConverter(table, template, {});

        expect(describeThreeColor(table, 1, {})).toBeUndefined();
        expect(datalinkDescribeThreeColor).not.toHaveBeenCalled();
    });


    test('getServiceDescSingleDataProduct: a table without service descriptors returns a message', async () => {
        const table = makeZtfTable({resources: [], tableMeta: {}, request: {}});
        const result = await getServiceDescSingleDataProduct(table, 0, {dpId: 'dpId-test'}, {});
        expect(result.message).toBe('Could not find any service descriptors');
    });

    test('getServiceDescSingleDataProduct: a row without access returns a message', async () => {
        const table = makeZtfTableWithDataRights({resources: [ZTF_DATALINK_RESOURCE], dataRights: ['public', 'proprietary']});
        const result = await getServiceDescSingleDataProduct(table, 1, {dpId: 'dpId-test'}, {});
        expect(result.message).toBe('You do not have access to this data.');
        expect(createServDescMenuRet).not.toHaveBeenCalled();
    });

    test('getServiceDescSingleDataProduct: a DataLink descriptor alone hands the row to the DataLink product code', async () => {
        const table = makeZtfTable({resources: [ZTF_DATALINK_RESOURCE], tableMeta: {}, request: {}});
        const activateParams = {dpId: 'dpId-test'};
        const options = {};
        const dlTableUrl = `${ZTF_LIGHT_CURVE_URL}&id=1684211300162373&bad_catflags_mask=0`;
        const datalinkProduct = {displayType: DPtypes.CHOICE_CTI, menuKey: 'dlt-0'};
        createServDescMenuRet.mockReturnValueOnce([]); // the real one leaves out DataLink descriptors
        makeDlUrl.mockReturnValueOnce(dlTableUrl);
        getDatalinkSingleDataProduct.mockResolvedValueOnce(datalinkProduct);

        const result = await getServiceDescSingleDataProduct(table, 1, activateParams, options);

        expect(result).toBe(datalinkProduct);
        expect(createServDescMenuRet.mock.calls[0][0]).toMatchObject(
            {table, row: 1, activateParams, options, activeMenuLookupKey: `${ZTF_LIGHT_CURVE_URL}--ztf-lc--1`});
        expect(makeDlUrl.mock.calls[0][0].accessURL).toBe(ZTF_LIGHT_CURVE_URL);
        expect(getDatalinkSingleDataProduct).toHaveBeenCalledWith({
            dlTableUrl, options, sourceTable: table, row: 1, activateParams, additionalServiceDescMenuList: undefined,
        });
    });

    test('getServiceDescSingleDataProduct: other service descriptors are added to the DataLink menu', async () => {
        const table = makeZtfTable({
            resources: [ZTF_DATALINK_RESOURCE, ZTF_FIREFLY_LC_RESOURCE], tableMeta: {}, request: {}});
        const fireflyLcEntry = {displayType: DPtypes.ANALYZE, menuKey: 'serdesc-dlt-0'};
        createServDescMenuRet.mockReturnValueOnce([fireflyLcEntry]);
        makeDlUrl.mockReturnValueOnce(`${ZTF_LIGHT_CURVE_URL}&id=686203400167748&bad_catflags_mask=0`);
        getDatalinkSingleDataProduct.mockResolvedValueOnce({menuKey: 'dlt-0'});

        await getServiceDescSingleDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        expect(getDatalinkSingleDataProduct.mock.calls[0][0].additionalServiceDescMenuList).toEqual([fireflyLcEntry]);
    });

    test('getServiceDescSingleDataProduct: the menu position is the table search target when there is one', async () => {
        const table = makeZtfTable({
            resources: [ZTF_DATALINK_RESOURCE], tableMeta: {}, request: {UserTargetWorldPt: '150.5;70.25;EQ_J2000'}});
        createServDescMenuRet.mockReturnValueOnce([]);
        makeDlUrl.mockReturnValueOnce(`${ZTF_LIGHT_CURVE_URL}&id=686203400167748&bad_catflags_mask=0`);
        getDatalinkSingleDataProduct.mockResolvedValueOnce({menuKey: 'dlt-0'});

        await getServiceDescSingleDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        const {positionWP} = createServDescMenuRet.mock.calls[0][0];
        expect(positionWP.getLon()).toBeCloseTo(150.5);
        expect(positionWP.getLat()).toBeCloseTo(70.25);
    });

    test('getServiceDescSingleDataProduct: with no DataLink descriptor a single menu entry is returned by itself', async () => {
        const table = makeZtfTable({resources: [ZTF_FIREFLY_LC_RESOURCE], tableMeta: {}, request: {}});
        const fireflyLcEntry = {displayType: DPtypes.ANALYZE, menuKey: 'serdesc-dlt-0'};
        createServDescMenuRet.mockReturnValueOnce([fireflyLcEntry]);

        const result = await getServiceDescSingleDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        expect(result).toBe(fireflyLcEntry);
        expect(createServDescMenuRet.mock.calls[0][0].activeMenuLookupKey)
            .toBe('http://localhost:8080/firefly/ts.html?api=lc--ztf-lc--allRows');
        expect(getDatalinkSingleDataProduct).not.toHaveBeenCalled();
    });

    test('getServiceDescSingleDataProduct: with no DataLink descriptor several entries make a menu on the active one', async () => {
        const table = makeZtfTable({resources: [ZTF_FIREFLY_LC_RESOURCE], tableMeta: {}, request: {}});
        const lookupKey = 'http://localhost:8080/firefly/ts.html?api=lc--ztf-lc--allRows';
        const menu = [
            {displayType: DPtypes.ANALYZE, menuKey: 'serdesc-dlt-0'},
            {displayType: DPtypes.ANALYZE, menuKey: 'serdesc-dlt-1'},
        ];
        createServDescMenuRet.mockReturnValueOnce(menu);
        getActiveMenuKey.mockReturnValueOnce('serdesc-dlt-1');

        const result = await getServiceDescSingleDataProduct(table, 0, {dpId: 'dpId-test'}, {});

        expect(getActiveMenuKey).toHaveBeenCalledWith('dpId-test', lookupKey);
        expect(result).toEqual({...menu[1], activeMenuLookupKey: lookupKey, menu});
    });


    test('getServiceDescGridDataProduct: each plot row is built and passed to createGridResult', async () => {
        const table = makeZtfTable({resources: [ZTF_DATALINK_RESOURCE], tableMeta: {}, request: {}});
        const activateParams = {dpId: 'dpId-test'};
        const options = {};
        const plotRows = [{row: 0, highlight: true}, {row: 1, highlight: false}];
        createServDescMenuRet.mockReturnValue([]);
        makeDlUrl
            .mockReturnValueOnce(`${ZTF_LIGHT_CURVE_URL}&id=686203400167748&bad_catflags_mask=0`)
            .mockReturnValueOnce(`${ZTF_LIGHT_CURVE_URL}&id=1684211300162373&bad_catflags_mask=0`);
        getDatalinkSingleDataProduct
            .mockResolvedValueOnce({menuKey: 'lc-0'})
            .mockResolvedValueOnce({menuKey: 'lc-1'});
        createGridResult.mockResolvedValueOnce('grid-result');

        const result = await getServiceDescGridDataProduct(table, plotRows, activateParams, options);

        expect(result).toBe('grid-result');
        const [promiseAry, gridActivateParams, gridTable, gridPlotRows, gridOptions] = createGridResult.mock.calls[0];
        expect(await Promise.all(promiseAry)).toEqual([{menuKey: 'lc-0'}, {menuKey: 'lc-1'}]);
        expect([gridActivateParams, gridTable, gridPlotRows, gridOptions]).toEqual([activateParams, table, plotRows, options]);
        expect(getDatalinkSingleDataProduct.mock.calls.map(([args]) => args.row)).toEqual([0, 1]);
    });

    test('getServiceDescGridDataProduct: a row without access shows a message in its slot', async () => {
        const table = makeZtfTableWithDataRights({resources: [ZTF_DATALINK_RESOURCE], dataRights: ['public', 'proprietary']});
        createServDescMenuRet.mockReturnValue([]);
        makeDlUrl.mockReturnValueOnce(`${ZTF_LIGHT_CURVE_URL}&id=686203400167748&bad_catflags_mask=0`);
        getDatalinkSingleDataProduct.mockResolvedValueOnce({menuKey: 'lc-0'});

        await getServiceDescGridDataProduct(table, [{row: 0}, {row: 1}], {dpId: 'dpId-test'}, {});

        const [promiseAry] = createGridResult.mock.calls[0];
        const products = await Promise.all(promiseAry);
        expect(products[0]).toEqual({menuKey: 'lc-0'});
        expect(products[1].message).toBe('You do not have access to this data.');
    });


    test('getServiceDescRelatedDataProduct: a table without service descriptors returns a message', async () => {
        const table = makeZtfTable({resources: [], tableMeta: {}, request: {}});
        const result = await getServiceDescRelatedDataProduct(table, 0, undefined, 'plot-1', {dpId: 'dpId-test'}, {});
        expect(result.message).toBe('Could not find any service descriptors');
    });

    test('getServiceDescRelatedDataProduct: a row without access returns a message', async () => {
        const table = makeZtfTableWithDataRights({resources: [ZTF_DATALINK_RESOURCE], dataRights: ['public', 'proprietary']});
        const result = await getServiceDescRelatedDataProduct(table, 1, undefined, 'plot-1', {dpId: 'dpId-test'}, {});
        expect(result.message).toBe('You do not have access to these data.');
        expect(getDatalinkRelatedImageGridProduct).not.toHaveBeenCalled();
    });

    test('getServiceDescRelatedDataProduct: without a DataLink descriptor returns a message', async () => {
        const table = makeZtfTable({resources: [ZTF_FIREFLY_LC_RESOURCE], tableMeta: {}, request: {}});
        const result = await getServiceDescRelatedDataProduct(table, 0, undefined, 'plot-1', {dpId: 'dpId-test'}, {});
        expect(result.message).toBe('a datalink service descriptor that returns images is required for related grid');
        expect(getDatalinkRelatedImageGridProduct).not.toHaveBeenCalled();
    });

    test('getServiceDescRelatedDataProduct: a DataLink descriptor builds the grid from the DataLink table', async () => {
        const table = makeZtfTable({resources: [ZTF_DATALINK_RESOURCE], tableMeta: {}, request: {}});
        const activateParams = {dpId: 'dpId-test'};
        const threeColorOps = [true, false, true];
        const options = {hasRelatedBands: true};
        const dlTableUrl = `${ZTF_LIGHT_CURVE_URL}&id=686203400167748&bad_catflags_mask=0`;
        makeDlUrl.mockReturnValueOnce(dlTableUrl);
        getDatalinkRelatedImageGridProduct.mockResolvedValueOnce('related-grid');

        const result = await getServiceDescRelatedDataProduct(table, 0, threeColorOps, 'plot-1', activateParams, options);

        expect(result).toBe('related-grid');
        expect(makeDlUrl.mock.calls[0][0].accessURL).toBe(ZTF_LIGHT_CURVE_URL);
        expect(getDatalinkRelatedImageGridProduct).toHaveBeenCalledWith(
            {dlTableUrl, activateParams, table, row: 0, threeColorOps, options});
    });
});
