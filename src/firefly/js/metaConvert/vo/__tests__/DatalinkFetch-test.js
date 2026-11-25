import {doFetchTable} from '../../../tables/TableUtil.js';
import {makeDlUrl} from '../DatalinkProducts.js';
import {
    fetchAllDatalinkTables, fetchDatalinkTable, fetchDatalinkTableSemanticList, fetchSemanticList
} from '../DatalinkFetch.js';

// keep the real module, but return the fetched tables from the test instead of the server
jest.mock('../../../tables/TableUtil.js', () => ({
    ...jest.requireActual('../../../tables/TableUtil.js'),
    doFetchTable: jest.fn(),
}));
// Loading DatalinkProducts pulls in UI components whose dependencies (validator/es) Jest does not transform
jest.mock('../DatalinkProducts.js', () => ({
    makeDlUrl: jest.fn(),
}));

// Rubin DP1 ObsCore rows (rubin-obscore-data.vot, rows 1 and 2): each access_url is the row's DataLink table
const RUBIN_DL_URL_ROW_1 = 'https://data.lsst.cloud/api/datalink/links?ID=ivo%3A%2F%2Forg.rubinobs%2Flsst-dp1%3Frepo%3Ddp1%26id%3D9b663d6d-59b7-5ec9-836a-6c64809d37ef';
const RUBIN_DL_URL_ROW_2 = 'https://data.lsst.cloud/api/datalink/links?ID=ivo%3A%2F%2Forg.rubinobs%2Flsst-dp1%3Frepo%3Ddp1%26id%3D22c88914-3da8-59f4-80e9-c2f3357ba6c8';
const RUBIN_OBSCORE_TABLE = {
    tbl_id: 'rubin-obscore', highlightedRow: 0, totalRows: 2, tableMeta: {}, request: {},
    tableData: {
        columns: [
            {name: 'obs_collection', type: 'char', utype: 'DataID.collection'},
            {name: 'dataproduct_type', type: 'char', utype: 'ObsDataset.dataProductType'},
            {name: 's_ra', type: 'double', UCD: 'pos.eq.ra', units: 'deg'},
            {name: 's_dec', type: 'double', UCD: 'pos.eq.dec', units: 'deg'},
            {name: 'obs_id', type: 'char', utype: 'DataID.observationID'},
            {name: 'access_url', type: 'char', utype: 'Access.reference'},
            {name: 'access_format', type: 'char', utype: 'Access.format'},
        ],
        data: [
            ['LSST.DP1', 'image', '52.87362023749581', '-28.376214735792995', 'CC_O_20241108_000253', RUBIN_DL_URL_ROW_1,
                'application/x-votable+xml;content=datalink'],
            ['LSST.DP1', 'image', '52.8743458333403', '-28.377290832111342', 'CC_O_20241108_000254', RUBIN_DL_URL_ROW_2,
                'application/x-votable+xml;content=datalink'],
        ],
    },
};

// ZTF light curve catalog with its DataLink service descriptor (serDescTbl.xml): id comes from the oid column
const ZTF_TABLE = {
    tbl_id: 'ztf-lc', highlightedRow: 1, totalRows: 2, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', desc: 'Link to ZTF light curve for ztf_dr3 collection',
        params: [
            {name: 'standardID', value: 'ivo://ivoa.net/std/DataLink#links-1.0'},
            {name: 'accessURL', value: 'https://irsa.ipac.caltech.edu/cgi-bin/ZTF/nph_light_curves?collection=ztf_dr3'},
        ],
        groups: [{name: 'inputParams', params: [
            {name: 'id', type: 'char', ref: 'col_0', value: ''},
            {name: 'bad_catflags_mask', type: 'int', value: 0},
        ]}],
    }],
    tableData: {columns: [{name: 'oid', type: 'long', ID: 'col_0'}], data: [['686203400167748'], ['1684211300162373']]},
};

// a plain catalog: not ObsCore and no service descriptors
const PLAIN_TABLE = {
    tbl_id: 'plain', highlightedRow: 0, totalRows: 1, tableMeta: {}, request: {},
    tableData: {columns: [{name: 'id', type: 'char'}], data: [['source-1']]},
};

// Rubin DP1 visit image DataLink table (rubin-datalink-row-8.vot): the #this file and its #cutout
const RUBIN_ID = 'ivo://org.rubinobs/lsst-dp1?repo=dp1&id=019b1997-c4b3-7f3e-9229-9ddc994065b7';
const RUBIN_DATALINK_TABLE = {
    tbl_id: 'rubin-datalink-row-8', highlightedRow: 0, totalRows: 2, tableMeta: {}, request: {},
    resources: [{
        type: 'meta', utype: 'adhoc:service', ID: 'cutout-sync',
        params: [
            {name: 'standardID', value: 'ivo://ivoa.net/std/SODA#sync-1.0'},
            {name: 'accessURL', value: 'https://data-int.lsst.cloud/api/cutout/sync'},
        ],
        groups: [{name: 'inputParams', params: [
            {name: 'ID', type: 'char', UCD: 'meta.id;meta.dataset', value: RUBIN_ID},
            {name: 'CIRCLE', type: 'double', arraySize: '3', UCD: 'pos.outline;obs', units: 'deg', value: '', xtype: 'circle'},
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
            [RUBIN_ID, 'https://storage.googleapis.com/butler-us-central1-dp1/visit_image_2.fits', '', '',
                'Second image file', '#this', 'application/fits', '109186560'],
        ],
    },
};

// The fetched tables are cached for the whole file, so each test fetches its own URLs.
describe('DatalinkFetch:', () => {

    // clearMocks (jest config) keeps queued mock*Once values, so an unused one would leak into the next test
    beforeEach(() => jest.resetAllMocks());

    test('fetchDatalinkTable: the url is fetched as a file request with the request options added', async () => {
        const url = 'https://example.org/datalink?ID=fetch-1';
        doFetchTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        const table = await fetchDatalinkTable(url, {pageSize: 100});

        expect(table).toBe(RUBIN_DATALINK_TABLE);
        expect(doFetchTable).toHaveBeenCalledTimes(1);
        expect(doFetchTable.mock.calls[0][0]).toMatchObject({id: 'IpacTableFromSource', source: url, pageSize: 100,
            META_INFO: {title: 'dl table'}});
    });

    test('fetchDatalinkTable: a second fetch of the same url comes from the cache', async () => {
        const url = 'https://example.org/datalink?ID=cache-1';
        doFetchTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        await fetchDatalinkTable(url);
        const table = await fetchDatalinkTable(url);

        expect(table).toBe(RUBIN_DATALINK_TABLE);
        expect(doFetchTable).toHaveBeenCalledTimes(1);
    });

    test('fetchDatalinkTable: the same url with other request options is fetched again', async () => {
        const url = 'https://example.org/datalink?ID=options-1';
        doFetchTable.mockResolvedValue(RUBIN_DATALINK_TABLE);

        await fetchDatalinkTable(url);
        await fetchDatalinkTable(url, {pageSize: 100});

        expect(doFetchTable).toHaveBeenCalledTimes(2);
    });

    test('fetchDatalinkTable: fetches of the same url at the same time share one request', async () => {
        const url = 'https://example.org/datalink?ID=same-time-1';
        doFetchTable.mockResolvedValue(RUBIN_DATALINK_TABLE);

        const tables = await Promise.all([fetchDatalinkTable(url), fetchDatalinkTable(url)]);

        expect(tables).toEqual([RUBIN_DATALINK_TABLE, RUBIN_DATALINK_TABLE]);
        expect(doFetchTable).toHaveBeenCalledTimes(1);
    });

    test('fetchDatalinkTable: a failed fetch is not cached', async () => {
        const url = 'https://example.org/datalink?ID=fail-1';
        doFetchTable.mockRejectedValueOnce(new Error('service unavailable')).mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        await expect(fetchDatalinkTable(url)).rejects.toThrow('service unavailable');
        const table = await fetchDatalinkTable(url);

        expect(table).toBe(RUBIN_DATALINK_TABLE);
        expect(doFetchTable).toHaveBeenCalledTimes(2);
    });

    test('fetchDatalinkTable: a full cache keeps the most recently used tables', async () => {
        // a fresh module, so the cache is empty; it trims itself at 38 tables down to the 24 used last
        let freshFetchDatalinkTable, freshDoFetchTable;
        jest.isolateModules(() => {
            ({fetchDatalinkTable: freshFetchDatalinkTable} = require('../DatalinkFetch.js'));
            ({doFetchTable: freshDoFetchTable} = require('../../../tables/TableUtil.js'));
        });
        const urls = Array.from({length: 38}, (_, idx) => `https://example.org/datalink?ID=trim-${idx}`);
        let now = 1000;
        const dateNowSpy = jest.spyOn(Date, 'now').mockImplementation(() => now++);
        freshDoFetchTable.mockResolvedValue(RUBIN_DATALINK_TABLE);

        for (const url of urls.slice(0, 37)) await freshFetchDatalinkTable(url);
        await freshFetchDatalinkTable(urls[0]); // used again, so it is now one of the most recent
        await freshFetchDatalinkTable(urls[37]); // the 38th table starts the trim
        freshDoFetchTable.mockClear();

        await freshFetchDatalinkTable(urls[0]);
        await freshFetchDatalinkTable(urls[37]);
        await freshFetchDatalinkTable(urls[15]); // the 24th most recent: kept
        expect(freshDoFetchTable).not.toHaveBeenCalled();
        await freshFetchDatalinkTable(urls[14]); // the 25th most recent: dropped
        expect(freshDoFetchTable).toHaveBeenCalledTimes(1);
        dateNowSpy.mockRestore();
    });

    test('fetchAllDatalinkTables: each row access_url is fetched, in row order', async () => {
        const secondTable = {...RUBIN_DATALINK_TABLE, tbl_id: 'second'};
        doFetchTable.mockImplementation((request) =>
            Promise.resolve(request.source === RUBIN_DL_URL_ROW_1 ? RUBIN_DATALINK_TABLE : secondTable));

        const tables = await fetchAllDatalinkTables(RUBIN_OBSCORE_TABLE, {pageSize: 100});

        expect(tables).toEqual([RUBIN_DATALINK_TABLE, secondTable]);
        expect(doFetchTable.mock.calls.map(([request]) => [request.source, request.pageSize]))
            .toEqual([[RUBIN_DL_URL_ROW_1, 100], [RUBIN_DL_URL_ROW_2, 100]]);
    });

    test('fetchAllDatalinkTables: a table with no data fetches nothing', () => {
        expect(fetchAllDatalinkTables({tbl_id: 'empty'}, {})).toBeUndefined();
        expect(doFetchTable).not.toHaveBeenCalled();
    });

    test('fetchDatalinkTableSemanticList: each semantics is listed once', async () => {
        doFetchTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        const semantics = await fetchDatalinkTableSemanticList('https://example.org/datalink?ID=semantics-1');

        expect(semantics).toEqual(['#this', '#cutout']);
    });

    test('fetchSemanticList: an ObsCore row with a DataLink access_format uses its access_url', async () => {
        doFetchTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        const semantics = await fetchSemanticList(RUBIN_OBSCORE_TABLE, 1);

        expect(semantics).toEqual(['#this', '#cutout']);
        expect(doFetchTable.mock.calls[0][0].source).toBe(RUBIN_DL_URL_ROW_2);
        expect(makeDlUrl).not.toHaveBeenCalled();
    });

    test('fetchSemanticList: a table with a DataLink service descriptor uses the descriptor url', async () => {
        const ztfUrl = 'https://irsa.ipac.caltech.edu/cgi-bin/ZTF/nph_light_curves?collection=ztf_dr3&id=1684211300162373';
        makeDlUrl.mockReturnValueOnce(ztfUrl);
        doFetchTable.mockResolvedValueOnce(RUBIN_DATALINK_TABLE);

        await fetchSemanticList(ZTF_TABLE, 1);

        expect(makeDlUrl).toHaveBeenCalledTimes(1);
        const [dlDescriptor, table, row] = makeDlUrl.mock.calls[0];
        expect(dlDescriptor.standardID).toBe('ivo://ivoa.net/std/DataLink#links-1.0');
        expect(table).toBe(ZTF_TABLE);
        expect(row).toBe(1);
        expect(doFetchTable.mock.calls[0][0].source).toBe(ztfUrl);
    });

    test('fetchSemanticList: a table with no DataLink gives an empty list without fetching', async () => {
        expect(await fetchSemanticList(PLAIN_TABLE, 0)).toEqual([]);
        expect(doFetchTable).not.toHaveBeenCalled();
    });

    test('fetchSemanticList: a failed fetch gives an empty list', async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
        makeDlUrl.mockReturnValueOnce('https://example.org/datalink?ID=fail-semantics-1');
        doFetchTable.mockRejectedValueOnce(new Error('service unavailable'));

        expect(await fetchSemanticList(ZTF_TABLE, 0)).toEqual([]);
        expect(consoleErrorSpy).toHaveBeenCalledWith('fetchSemanticList call failed', expect.any(Error));
        consoleErrorSpy.mockRestore();
    });
});
