import {getPreferCutout} from '../../../ui/tap/Cutout.js';
import {createDataLinkSingleRowItem} from '../DataLinkProcessor.js';
import {getDatalinkStandAlineDataProduct, makeDatalinkStaneAloneConverter} from '../DataLinkStandAloneConverter.js';

// Loading DataLinkProcessor pulls in UI components whose dependencies (validator/es) Jest does not transform,
// so it is replaced with a stub; each test sets the return value it needs.
jest.mock('../DataLinkProcessor.js', () => ({
    createDataLinkSingleRowItem: jest.fn(),
}));
// keep the real module, but read the cutout preference from the test instead of the store
jest.mock('../../../ui/tap/Cutout.js', () => ({
    ...jest.requireActual('../../../ui/tap/Cutout.js'),
    getPreferCutout: jest.fn(),
}));

// cadc-datalink-row-1.vot loaded as a table on its own: a CADC APASS image (#this), its SODA sync cutout, and
// the SODA async cutout, which getServiceDescriptors drops, so that row is not usable
const APASS_ID = 'ivo://cadc.nrc.ca/APASS?n100421.0123/image';
const APASS_FILE_URL = 'https://cadc-west-01.canfar.net/vault/files/APASS/north/100421/n100421.0123.fits';
const SODA_SYNC_ID = 'soda-35c80f45-d07d-48f7-924b-3d94df02fb8e';
const SODA_ASYNC_ID = 'soda-faaeb4fe-813e-402c-af77-6b896e86e5cc';

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
        groups: [{name: 'inputParams', params: [{name: 'ID', UCD: 'meta.id;meta.dataset', value: 'vos://cadc.nrc.ca!vault/APASS'}]}],
    };
}

const APASS_DATALINK_TABLE = {
    tbl_id: 'cadc-apass-datalink', totalRows: 3, tableMeta: {}, request: {},
    tableData: {
        columns: ['ID', 'access_url', 'service_def', 'error_message', 'semantics', 'local_semantics',
            'description', 'content_type', 'content_length'].map((name) => ({name, type: 'char'})),
        data: [
            [APASS_ID, APASS_FILE_URL, '', '', '#this', '', 'download', 'application/fits', '13731840'],
            [APASS_ID, '', SODA_SYNC_ID, '', '#cutout', '', 'SODA-sync cutout', 'application/fits', ''],
            [APASS_ID, '', SODA_ASYNC_ID, '', '#cutout', '', 'SODA-async cutout', 'application/fits', ''],
        ],
    },
    resources: [
        makeSodaResource(SODA_SYNC_ID, 'ivo://ivoa.net/std/SODA#sync-1.0', 'https://ws-uv.canfar.net/caom2ops/sync'),
        makeSodaResource(SODA_ASYNC_ID, 'ivo://ivoa.net/std/SODA#async-1.0', 'https://ws-uv.canfar.net/caom2ops/async'),
    ],
};

// The DataLinkStandaloneTable entry from DataProductsFactory's converter list
const STANDALONE_CONVERTER_TEMPLATE = {
    converterId: 'DataLinkStandaloneTable', threeColor: false, hasRelatedBands: false, canGrid: false, maxPlots: 1,
};

/**
 * Call getDatalinkStandAlineDataProduct for a row of the APASS DataLink table.
 * @param {Object} p
 * @param {number} p.row
 * @param {boolean} p.preferCutout
 * @return {Promise<DataProductsDisplayType>}
 */
async function getDatalinkStandAlineDataProductForApassRow({row, preferCutout}) {
    getPreferCutout.mockReturnValueOnce(preferCutout);
    createDataLinkSingleRowItem.mockImplementationOnce(({dlData}) => ({menuKey: `dlt-${dlData.rowIdx}`}));
    return getDatalinkStandAlineDataProduct(APASS_DATALINK_TABLE, row, {dpId: 'dpId-test'},
        {dataProductsComponentKey: 'dp-key'});
}

describe('DataLinkStandAloneConverter:', () => {

    // clearMocks (jest config) keeps queued mock*Once values, so an unused one would leak into the next test
    beforeEach(() => jest.resetAllMocks());

    test('makeDatalinkStaneAloneConverter: no table returns the template unchanged', () => {
        expect(makeDatalinkStaneAloneConverter(undefined, STANDALONE_CONVERTER_TEMPLATE, {}))
            .toBe(STANDALONE_CONVERTER_TEMPLATE);
    });

    test('makeDatalinkStaneAloneConverter: the converter id names the table', () => {
        expect(makeDatalinkStaneAloneConverter(APASS_DATALINK_TABLE, STANDALONE_CONVERTER_TEMPLATE, {}))
            .toEqual({...STANDALONE_CONVERTER_TEMPLATE, converterId: 'DatalinkStandalone-cadc-apass-datalink'});
    });


    test('getDatalinkStandAlineDataProduct: the row is made into a product', async () => {
        const activateParams = {dpId: 'dpId-test'};
        const options = {dataProductsComponentKey: 'dp-key'};
        getPreferCutout.mockReturnValueOnce(false);
        createDataLinkSingleRowItem.mockReturnValueOnce('row-product');

        const result = await getDatalinkStandAlineDataProduct(APASS_DATALINK_TABLE, 0, activateParams, options);

        expect(result).toBe('row-product');
        expect(getPreferCutout).toHaveBeenCalledWith('dp-key', 'cadc-apass-datalink');
        const {dlData, ...otherArgs} = createDataLinkSingleRowItem.mock.calls[0][0];
        expect(dlData).toMatchObject({rowIdx: 0, url: APASS_FILE_URL, semantics: '#this'});
        expect(otherArgs).toEqual({activateParams, options});
    });

    test('getDatalinkStandAlineDataProduct: the full image row shows its cutout when cutouts are preferred', async () => {
        expect(await getDatalinkStandAlineDataProductForApassRow({row: 0, preferCutout: true})).toEqual({menuKey: 'dlt-1'});
    });

    test('getDatalinkStandAlineDataProduct: the cutout row shows itself when cutouts are preferred', async () => {
        expect(await getDatalinkStandAlineDataProductForApassRow({row: 1, preferCutout: true})).toEqual({menuKey: 'dlt-1'});
    });

    test('getDatalinkStandAlineDataProduct: the cutout row shows the full image when cutouts are not preferred', async () => {
        expect(await getDatalinkStandAlineDataProductForApassRow({row: 1, preferCutout: false})).toEqual({menuKey: 'dlt-0'});
    });

    test('getDatalinkStandAlineDataProduct: an unusable row is shown as itself', async () => {
        // the async cutout has no usable service descriptor, so it is not paired with the full image
        expect(await getDatalinkStandAlineDataProductForApassRow({row: 2, preferCutout: true})).toEqual({menuKey: 'dlt-2'});
    });

    test('getDatalinkStandAlineDataProduct: a row past the end of the table gives nothing', async () => {
        getPreferCutout.mockReturnValueOnce(false);

        const result = await getDatalinkStandAlineDataProduct(APASS_DATALINK_TABLE, 3, {dpId: 'dpId-test'}, {});

        expect(result).toBeUndefined();
        expect(createDataLinkSingleRowItem).not.toHaveBeenCalled();
    });
});
