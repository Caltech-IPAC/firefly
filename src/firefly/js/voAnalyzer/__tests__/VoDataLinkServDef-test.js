import {
    analyzeDatalinkRow, checkForDatalinkServDesc, findWorldPtInServiceDef, getDataLinkData, getDownloadTypeDesc,
    getObsCoreData, getServiceDescriptors, getStandardId, getUtype, hasServiceDescriptors, isAnalysisTableDatalink,
    isDataLinkServiceDesc, isDownloadType, isGzipType, isHtmlType, isJSONType, isPDFType, isPlainTextType,
    isSimpleImageType, isTarType, isVoTable, isYamlType,
} from '../VoDataLinkServDef.js';

// CADC SODA sync descriptor POS-style params: RA and Dec are referenced from s_ra/s_dec in the ObsCore source table
const SOURCE_TABLE = {
    tbl_id: 'obscore-source',
    totalRows: 1,
    tableData: {
        columns: [{name: 's_ra', ID: 'col-s_ra'}, {name: 's_dec', ID: 'col-s_dec'}],
        data: [[149.05884191434205, 2.2196869850869434]],
    },
};
const RA_PARAM = {name: 'RA', UCD: 'pos.eq.ra', ref: 'col-s_ra'};
const DEC_PARAM = {name: 'DEC', UCD: 'pos.eq.dec', ref: 'col-s_dec'};

function findWorldPtInServiceDefFor({serDefParams, sdSourceTable, sourceRow}) {
    return findWorldPtInServiceDef({serDefParams, sdSourceTable}, sourceRow);
}

// CADC DAO observation dao_c182_2022_010463 (cadc-datalink-row-100.vot): a #thumbnail, the #this file, a #cutout through
// the SODA sync descriptor, a #cutout through the SODA async descriptor, and a #preview
const CADC_DAO_ID = 'ivo://cadc.nrc.ca/DAO?dao_c182_2022_010463/dao_c182_2022_010463';
const CADC_SYNC_ID = 'soda-58d69c3c-3fd8-4489-9c94-77f965f5d19d';
const CADC_ASYNC_ID = 'soda-336a46e2-44d8-45c9-861d-efa00b96c7c8';
const CADC_FILES_URL = 'https://cadc-west-01.canfar.net/raven/files/cadc:DAO/dao_c182_2022_010463';
const cadcSodaResource = (ID, standardID, accessURL) => ({
    type: 'meta', utype: 'adhoc:service', ID,
    params: [
        {name: 'resourceIdentifier', value: 'ivo://cadc.nrc.ca/DAO'},
        {name: 'standardID', value: standardID},
        {name: 'contentType', value: 'application/fits'},
        {name: 'accessURL', value: accessURL},
    ],
    groups: [{name: 'inputParams', params: [
        {name: 'ID', type: 'char', UCD: 'meta.id;meta.dataset', value: 'cadc:DAO/dao_c182_2022_010463.fits'},
        {name: 'POS', type: 'char', UCD: 'obs.field', value: ''},
        {name: 'CIRCLE', type: 'double', arraySize: '3', UCD: 'obs.field', units: 'deg', xtype: 'circle', value: ''},
        {name: 'POLYGON', type: 'double', UCD: 'obs.field', units: 'deg', xtype: 'polygon', value: ''},
    ]}],
});
const CADC_DAO_DATALINK_TABLE = {
    tbl_id: 'cadc-dao-datalink', highlightedRow: 1, totalRows: 5, tableMeta: {}, request: {},
    resources: [
        cadcSodaResource(CADC_SYNC_ID, 'ivo://ivoa.net/std/SODA#sync-1.0',
            'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/sync'),
        cadcSodaResource(CADC_ASYNC_ID, 'ivo://ivoa.net/std/SODA#async-1.0',
            'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/async'),
    ],
    tableData: {
        columns: ['ID', 'access_url', 'service_def', 'error_message', 'semantics', 'local_semantics', 'description',
            'content_type', 'content_length', 'content_qualifier', 'link_auth', 'link_authorized']
            .map((name) => ({name, type: 'char'})),
        data: [
            [CADC_DAO_ID, `${CADC_FILES_URL}_256.png`, '', '', '#thumbnail', '',
                'download cadc:DAO/dao_c182_2022_010463_256.png', 'image/png', '37091', '', 'optional', 'true'],
            [CADC_DAO_ID, `${CADC_FILES_URL}.fits`, '', '', '#this', '',
                'download cadc:DAO/dao_c182_2022_010463.fits', 'application/fits', '4800960', '', 'optional', 'true'],
            [CADC_DAO_ID, '', CADC_SYNC_ID, '', '#cutout', '',
                'SODA-sync cutout of cadc:DAO/dao_c182_2022_010463.fits', 'application/fits', '', '', 'optional', 'true'],
            [CADC_DAO_ID, '', CADC_ASYNC_ID, '', '#cutout', '',
                'SODA-async cutout of cadc:DAO/dao_c182_2022_010463.fits', 'application/fits', '', '', 'optional', 'true'],
            [CADC_DAO_ID, `${CADC_FILES_URL}_1024.png`, '', '', '#preview', '',
                'download cadc:DAO/dao_c182_2022_010463_1024.png', 'image/png', '520138', '', 'optional', 'true'],
        ],
    },
};

// Euclid DR1 tile 101384791 (euclid-datalink-Products-row-1.vot), some of its rows: the VIS #this image, its #cutout
// (local semantics cutout-grid-BLUE) and #noise image; the HSC R2 #cutout and the #counterpart image it is paired with.
// The rows carry the label, bandpass_name and cloud_access columns.
const EUCLID_VIS_ID = 'ivo://irsa.ipac/euclid_dr1_deep_DpdMerBksMosaic?101384791_798035059_VIS/VIS';
const EUCLID_HSC_R2_ID = 'ivo://irsa.ipac/euclid_dr1_deep_DpdMerBksMosaic?101384791_798035059_HSC/R2';
const EUCLID_DATA_URL = 'https://irsacloud.ipac.caltech.edu/ibe/data/euclid/dr1/MER/101384791';
const EUCLID_VIS_CLOUD_ACCESS = '{"aws": {"bucket_name": "nasa-irsa-euclid-dr1", "key":"dr1/MER/101384791/VIS/EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791.fits", "region": "us-east-1"}}';
const EUCLID_DATALINK_TABLE = {
    tbl_id: 'euclid-datalink', highlightedRow: 0, totalRows: 5, request: {},
    tableMeta: {SearchTarget: '269.73;66.02;EQ_J2000', ROW_TARGET: '269.7;66.0;EQ_J2000',
        S_REGION: 'POLYGON 269.4 65.8 270.1 65.8 270.1 66.2 269.4 66.2'},
    resources: [{
        type: 'meta', utype: 'adhoc:service', ID: 'sd-cutout',
        params: [{name: 'accessURL', value: 'https://irsa.ipac.caltech.edu/ibe/cutout'}],
        groups: [{name: 'inputParams', params: [
            {name: 'ra', UCD: 'pos.eq.ra;meta.main', value: '269.73'},
            {name: 'dec', UCD: 'pos.eq.dec;meta.main', value: '66.02'},
            {name: 'size', UCD: 'phys.size', units: 'deg', value: '0.01'},
            {name: 'path', ref: 'col_8'},
        ]}],
    }],
    tableData: {
        columns: ['id', 'access_url', 'service_def', 'error_message', 'semantics', 'local_semantics', 'description',
            'content_type', 'file', 'content_length', 'label', 'bandpass_name', 'cloud_access']
            .map((name, idx) => ({name, type: 'char', ID: `col_${idx}`})),
        data: [
            [EUCLID_VIS_ID, `${EUCLID_DATA_URL}/VIS/EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791.fits`, '', '', '#this', '',
                'EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791', 'image/fits', '', '1664316480', 'VIS', 'VIS',
                EUCLID_VIS_CLOUD_ACCESS],
            [EUCLID_VIS_ID, '', 'sd-cutout', '', '#cutout', 'cutout-grid-BLUE', 'EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791',
                'image/fits', 'euclid/dr1/MER/101384791/VIS/EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791.fits', '1664316480',
                'VIS', 'VIS', EUCLID_VIS_CLOUD_ACCESS],
            [EUCLID_VIS_ID, `${EUCLID_DATA_URL}/VIS/EUC_MER_MOSAIC-VIS-RMS_TILE101384791.fits`, '', '', '#noise', '',
                'EUC_MER_MOSAIC-VIS-RMS_TILE101384791', 'image/fits', '', '1664316480', 'VIS', 'VIS', 'not json'],
            [EUCLID_HSC_R2_ID, '', 'sd-cutout', '', '#cutout', 'cutout-grid',
                'EUC_MER_BGSUB-MOSAIC-WISHES-R2_TILE101384791', 'image/fits',
                'euclid/dr1/MER/101384791/HSC/EUC_MER_BGSUB-MOSAIC-WISHES-R2_TILE101384791.fits', '416165760',
                'Hyper Suprime-Cam/R2', 'R2', ''],
            [EUCLID_HSC_R2_ID, `${EUCLID_DATA_URL}/HSC/EUC_MER_BGSUB-MOSAIC-WISHES-R2_TILE101384791.fits`, '', '',
                '#counterpart', '', 'EUC_MER_BGSUB-MOSAIC-WISHES-R2_TILE101384791', 'image/fits', '', '416165760',
                'Hyper Suprime-Cam/R2', 'R2', ''],
        ],
    },
};

// ZTF light curve catalog with its DataLink service descriptor (serDescTbl.xml): id comes from the oid column,
// bad_catflags_mask is fixed at 0
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
        ]}],
    }],
    tableData: {columns: [{name: 'oid', type: 'long', ID: 'col_0'}], data: [['686203400167748'], ['1684211300162373']]},
};

// a plain catalog: no service descriptors and not a DataLink table
const PLAIN_TABLE = {
    tbl_id: 'plain', highlightedRow: 0, totalRows: 1, tableMeta: {}, request: {},
    tableData: {columns: [{name: 'id', type: 'char'}], data: [['source-1']]},
};

// Rubin DP1 ObsCore row (rubin-obscore-data.vot, row 1)
const RUBIN_OBSCORE_TABLE = {
    tbl_id: 'rubin-obscore', highlightedRow: 0, totalRows: 1, tableMeta: {}, request: {},
    tableData: {
        columns: [
            {name: 'obs_collection', type: 'char', utype: 'DataID.collection'},
            {name: 'dataproduct_type', type: 'char', utype: 'ObsDataset.dataProductType'},
            {name: 'obs_id', type: 'char', utype: 'DataID.observationID'},
            {name: 's_region', type: 'char', utype: 'Char.SpatialAxis.Coverage.Support.Area'},
            {name: 'access_url', type: 'char', utype: 'Access.reference'},
            {name: 'access_format', type: 'char', utype: 'Access.format'},
        ],
        data: [['LSST.DP1', 'image', 'CC_O_20241108_000253',
            'POLYGON ICRS 52.779071 -28.240995 52.721578 -28.461865 52.968449 -28.511367 53.025383 -28.290376',
            'https://data.lsst.cloud/api/datalink/links?ID=ivo%3A%2F%2Forg.rubinobs%2Flsst-dp1%3Frepo%3Ddp1%26id%3D9b663d6d-59b7-5ec9-836a-6c64809d37ef',
            'application/x-votable+xml;content=datalink']],
    },
};

describe('findWorldPtInServiceDef', () => {
    test('RA and Dec param values make the point', () => {
        const wp = findWorldPtInServiceDefFor({
            serDefParams: [{...RA_PARAM, value: '10.5'}, {...DEC_PARAM, value: '-20.25'}],
            sdSourceTable: SOURCE_TABLE, sourceRow: 0});
        expect(wp.getLon()).toBeCloseTo(10.5);
        expect(wp.getLat()).toBeCloseTo(-20.25);
    });

    test('RA and Dec refs are read from the source table row', () => {
        const wp = findWorldPtInServiceDefFor({
            serDefParams: [RA_PARAM, DEC_PARAM], sdSourceTable: SOURCE_TABLE, sourceRow: 0});
        expect(wp.getLon()).toBeCloseTo(149.05884191434205);
        expect(wp.getLat()).toBeCloseTo(2.2196869850869434);
    });

    test('only an RA param returns undefined', () => {
        expect(findWorldPtInServiceDefFor({
            serDefParams: [RA_PARAM], sdSourceTable: SOURCE_TABLE, sourceRow: 0})).toBeUndefined();
    });

    test('only a Dec param returns undefined', () => {
        expect(findWorldPtInServiceDefFor({
            serDefParams: [DEC_PARAM], sdSourceTable: SOURCE_TABLE, sourceRow: 0})).toBeUndefined();
    });

    test('no RA or Dec params returns undefined', () => {
        expect(findWorldPtInServiceDefFor({
            serDefParams: [{name: 'ID', UCD: 'meta.id;meta.main', value: 'ivo://x'}],
            sdSourceTable: SOURCE_TABLE, sourceRow: 0})).toBeUndefined();
    });

    test('for a DataLink table the RA and Dec refs are read from the descriptor DataLink row', () => {
        // made up: a DataLink table whose cutout descriptor takes ra and dec from the row, like SPHEREx
        const table = {...CADC_DAO_DATALINK_TABLE,
            tableData: {
                columns: [...CADC_DAO_DATALINK_TABLE.tableData.columns,
                    {name: 'center_ra', type: 'char', ID: 'col-center_ra'},
                    {name: 'center_dec', type: 'char', ID: 'col-center_dec'}],
                data: [[...CADC_DAO_DATALINK_TABLE.tableData.data[1], '148.8', '69.1'],
                    [...CADC_DAO_DATALINK_TABLE.tableData.data[2], '148.9', '69.07']],
            }};
        const serDef = {serDefParams: [{...RA_PARAM, ref: 'col-center_ra'}, {...DEC_PARAM, ref: 'col-center_dec'}],
            sdSourceTable: table, dataLinkTableRowIdx: 1};

        const wp = findWorldPtInServiceDef(serDef, 0); // the source row is not used

        expect(wp.getLon()).toBeCloseTo(148.9);
        expect(wp.getLat()).toBeCloseTo(69.07);
    });
});

describe('analyzeDatalinkRow', () => {
    test('a #this image/fits row is the image', () => {
        expect(analyzeDatalinkRow({semantics: '#this', localSemantics: '', contentQualifier: '',
            contentType: 'image/fits', sourceObsCoreData: undefined}))
            .toMatchObject({isThis: true, isImage: true, maybeImage: true, isCutout: false, isGrid: false,
                isSimpleImage: false, isDownloadOnly: false, cutoutFullPair: false});
    });

    test('an application/fits row is only maybe an image', () => {
        expect(analyzeDatalinkRow({semantics: '#this', localSemantics: '', contentQualifier: '',
            contentType: 'application/fits', sourceObsCoreData: undefined}))
            .toMatchObject({isImage: false, maybeImage: true});
    });

    test('a Euclid cutout-grid-BLUE cutout is a cutout in the blue band of a grid', () => {
        expect(analyzeDatalinkRow({semantics: '#cutout', localSemantics: 'cutout-grid-BLUE', contentQualifier: '',
            contentType: 'image/fits', sourceObsCoreData: undefined}))
            .toMatchObject({isCutout: true, isGrid: true, bBand: true, rBand: false, gBand: false, isThis: false});
    });

    test('semantics are matched ignoring case', () => {
        expect(analyzeDatalinkRow({semantics: '#Counterpart', localSemantics: '', contentQualifier: '',
            contentType: 'IMAGE/FITS', sourceObsCoreData: undefined}))
            .toMatchObject({isCounterpart: true, isImage: true});
    });

    test('#calibration and #auxiliary rows are marked', () => {
        expect(analyzeDatalinkRow({semantics: '#calibration', localSemantics: '', contentQualifier: '',
            contentType: 'application/fits', sourceObsCoreData: undefined})).toMatchObject({isCalibration: true});
        expect(analyzeDatalinkRow({semantics: '#auxiliary', localSemantics: '', contentQualifier: '',
            contentType: 'image/fits', sourceObsCoreData: undefined})).toMatchObject({isAux: true});
    });

    test('a spectrum comes from the local semantics or the ObsCore dataproduct_type', () => {
        expect(analyzeDatalinkRow({semantics: '#this', localSemantics: 'spectrum-1d', contentQualifier: '',
            contentType: 'application/fits', sourceObsCoreData: undefined}).isSpectrum).toBe(true);
        expect(analyzeDatalinkRow({semantics: '#this', localSemantics: '', contentQualifier: '',
            contentType: 'application/fits', sourceObsCoreData: {dataproduct_type: 'Spectrum'}}).isSpectrum).toBe(true);
        expect(analyzeDatalinkRow({semantics: '#this', localSemantics: '', contentQualifier: '',
            contentType: 'application/fits', sourceObsCoreData: {dataproduct_type: 'image'}}).isSpectrum).toBe(false);
    });

    test('CISX primary and concurrent queries come from the semantics, or the local semantics of a #this row', () => {
        expect(analyzeDatalinkRow({semantics: 'ivo://irsa/cisx#primary-query', localSemantics: '', contentQualifier: '',
            contentType: 'application/x-votable+xml', sourceObsCoreData: undefined}).cisxPrimaryQuery).toBe(true);
        expect(analyzeDatalinkRow({semantics: '#this', localSemantics: 'cisx#concurrent-query', contentQualifier: '',
            contentType: 'application/x-votable+xml', sourceObsCoreData: undefined}).cisxConcurrentQuery).toBe(true);
        expect(analyzeDatalinkRow({semantics: '#auxiliary', localSemantics: 'cisx#primary-query', contentQualifier: '',
            contentType: 'application/x-votable+xml', sourceObsCoreData: undefined}).cisxPrimaryQuery).toBe(false);
    });

    test('a PNG is a simple image and a tar file is download only', () => {
        expect(analyzeDatalinkRow({semantics: '#preview', localSemantics: '', contentQualifier: '',
            contentType: 'image/png', sourceObsCoreData: undefined}))
            .toMatchObject({isSimpleImage: true, isDownloadOnly: false, maybeImage: false});
        expect(analyzeDatalinkRow({semantics: '#this', localSemantics: '', contentQualifier: '',
            contentType: 'application/x-tar', sourceObsCoreData: undefined})).toMatchObject({isDownloadOnly: true});
    });

    test('a row with no semantics or content type matches nothing', () => {
        const analysis = analyzeDatalinkRow({sourceObsCoreData: undefined});
        const {cutoutFullPair, ...flags} = analysis;
        expect(cutoutFullPair).toBe(false);
        expect(Object.values(flags).every((flag) => !flag)).toBe(true);
    });
});

describe('content type checks', () => {
    test.each([
        ['application/gzip', isGzipType, true],
        ['application/x-gz', isGzipType, true],
        ['TEXT/PLAIN', isPlainTextType, true],
        ['application/html', isHtmlType, true],
        ['image/jpeg', isSimpleImageType, true],
        ['image/fits', isSimpleImageType, false],
        ['application/x-pdf', isPDFType, true],
        ['application/x-json', isJSONType, true],
        ['application/x-votable+xml', isVoTable, true],
        ['application/x-votable+xml;content=datalink', isVoTable, false],
        ['application/x-tar', isTarType, true],
        ['text/yaml', isYamlType, true],
        ['application/octet-stream', isDownloadType, true],
        ['image/fits', isDownloadType, false],
    ])('%s with %p is %p', (contentType, isType, expected) => {
        expect(isType(contentType)).toBe(expected);
    });

    test('getDownloadTypeDesc: tar, gzip, or empty for a file that is not download only', () => {
        expect(getDownloadTypeDesc('application/x-tar')).toBe('tar');
        expect(getDownloadTypeDesc('application/gzip')).toBe('gzip');
        expect(getDownloadTypeDesc('image/fits')).toBe('');
        expect(getDownloadTypeDesc('application/octet-stream')).toBeUndefined();
    });
});

describe('getServiceDescriptors', () => {
    test('the CADC async descriptor is removed unless asked for', () => {
        expect(getServiceDescriptors(CADC_DAO_DATALINK_TABLE).map(({ID}) => ID)).toEqual([CADC_SYNC_ID]);
        expect(getServiceDescriptors(CADC_DAO_DATALINK_TABLE, false).map(({ID}) => ID))
            .toEqual([CADC_SYNC_ID, CADC_ASYNC_ID]);
    });

    test('a descriptor holds the resource access URL, standardID, and ids', () => {
        const [syncSerDef] = getServiceDescriptors(CADC_DAO_DATALINK_TABLE);

        expect(syncSerDef).toMatchObject({ID: CADC_SYNC_ID, utype: 'adhoc:service',
            internalServiceDescriptorID: 'sd-cadc-dao-datalink---0', sdSourceTable: CADC_DAO_DATALINK_TABLE,
            accessURL: 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/sync',
            standardID: 'ivo://ivoa.net/std/SODA#sync-1.0', positionWP: undefined, rowWP: undefined});
    });

    test('the title comes from the resource desc, then the DataLink row description, then the index', () => {
        const [ztfSerDef] = getServiceDescriptors(ZTF_TABLE);
        const [syncSerDef] = getServiceDescriptors(CADC_DAO_DATALINK_TABLE);
        const [bigTableSerDef] = getServiceDescriptors({...CADC_DAO_DATALINK_TABLE, totalRows: 51});

        expect(ztfSerDef.title).toBe('Link to ZTF light curve for ztf_dr3 collection');
        expect(syncSerDef.title).toBe('SODA-sync cutout of cadc:DAO/dao_c182_2022_010463.fits');
        // the description is only looked up in tables of 50 rows or fewer
        expect(bigTableSerDef.title).toBe('Service Descriptor 0');
    });

    test('the search and row targets come from the table meta', () => {
        const [cutoutSerDef] = getServiceDescriptors(EUCLID_DATALINK_TABLE);

        expect(cutoutSerDef.positionWP.getLon()).toBeCloseTo(269.73);
        expect(cutoutSerDef.positionWP.getLat()).toBeCloseTo(66.02);
        expect(cutoutSerDef.rowWP.getLon()).toBeCloseTo(269.7);
    });

    test('each input parameter says whether it is fixed, referenced, optional, or required', () => {
        // made up: CUTOUTDETAIL has options and no value, like Rubin's without its default
        const table = {...ZTF_TABLE, resources: [{...ZTF_TABLE.resources[0], groups: [{name: 'inputParams', params: [
            {name: 'id', type: 'char', ref: 'col_0', value: ''},
            {name: 'bad_catflags_mask', type: 'int', value: [32768]}, // a one-entry array value is unwrapped
            {name: 'min_mag', type: 'double', value: 0}, // 0 is a real value
            {name: 'time', type: 'char', value: ''},
            {name: 'band', type: 'char', value: ['']}, // unwrapped to empty, so optional
            {name: 'CUTOUTDETAIL', type: 'char', options: 'Image,MaskedImage,Exposure'},
        ]}]}]};

        const [{serDefParams}] = getServiceDescriptors(table);

        expect(serDefParams).toEqual([
            {name: 'id', type: 'char', ref: 'col_0', value: '', colName: 'oid', optionalParam: false,
                allowsInput: false, inputRequired: false},
            {name: 'bad_catflags_mask', type: 'int', value: 32768, colName: undefined, optionalParam: false,
                allowsInput: true, inputRequired: false},
            {name: 'min_mag', type: 'double', value: 0, colName: undefined, optionalParam: false,
                allowsInput: true, inputRequired: false},
            {name: 'time', type: 'char', value: '', colName: undefined, optionalParam: true,
                allowsInput: true, inputRequired: false},
            {name: 'band', type: 'char', value: '', colName: undefined, optionalParam: true,
                allowsInput: true, inputRequired: false},
            {name: 'CUTOUTDETAIL', type: 'char', options: 'Image,MaskedImage,Exposure', value: undefined,
                colName: undefined, optionalParam: false, allowsInput: true, inputRequired: true},
        ]);
    });

    test('CISX resources are read, with their ui and token groups', () => {
        // made up: a CISX descriptor
        const table = {...PLAIN_TABLE, resources: [{
            type: 'meta', utype: 'cisx:adhoc:service', ID: 'cisx-sd',
            params: [{name: 'accessURL', value: 'https://example.org/cisx'}],
            groups: [
                {name: 'inputParams', params: [{name: 'band', value: 'g'}]},
                {name: 'CISX:ui', params: [{name: 'hidden', value: 'band'}]},
                {name: 'CISX:tokenSub', params: [{name: 'token', value: '${band}'}]},
            ],
        }]};

        const [cisxSerDef] = getServiceDescriptors(table);

        expect(cisxSerDef).toMatchObject({ID: 'cisx-sd', utype: 'cisx:adhoc:service',
            cisxUI: [{name: 'hidden', value: 'band'}], cisxTokenSub: [{name: 'token', value: '${band}'}]});
    });

    test('resources that are not service descriptors with an access URL are ignored', () => {
        const table = {...PLAIN_TABLE, resources: [
            {type: 'results', utype: 'adhoc:service', params: [{name: 'accessURL', value: 'https://example.org/a'}]},
            {type: 'meta', utype: 'other', params: [{name: 'accessURL', value: 'https://example.org/b'}]},
            {type: 'meta', utype: 'adhoc:service', params: [{name: 'accessURL', value: ''}]},
        ]};

        expect(getServiceDescriptors(table)).toBe(false);
        expect(getServiceDescriptors(PLAIN_TABLE)).toBe(false);
        expect(hasServiceDescriptors(PLAIN_TABLE)).toBe(false);
        expect(hasServiceDescriptors(ZTF_TABLE)).toBe(true);
    });
});

describe('service descriptor checks', () => {
    test('isDataLinkServiceDesc: a descriptor with a DataLink standardID', () => {
        const [ztfSerDef] = getServiceDescriptors(ZTF_TABLE);
        const [syncSerDef] = getServiceDescriptors(CADC_DAO_DATALINK_TABLE);

        expect(isDataLinkServiceDesc(ztfSerDef)).toBe(true);
        expect(isDataLinkServiceDesc(syncSerDef)).toBe(false);
        expect(isDataLinkServiceDesc(undefined)).toBe(false);
    });

    test('getStandardId and getUtype are lower case, or empty when missing', () => {
        const [syncSerDef] = getServiceDescriptors(CADC_DAO_DATALINK_TABLE);

        expect(getStandardId(syncSerDef)).toBe('ivo://ivoa.net/std/soda#sync-1.0');
        expect(getStandardId({})).toBe('');
        expect(getUtype({utype: 'CISX:adhoc:service'})).toBe('cisx:adhoc:service');
        expect(getUtype(undefined)).toBe('');
    });

    test('checkForDatalinkServDesc: the DataLink descriptor URL and input parameters', () => {
        expect(checkForDatalinkServDesc(ZTF_TABLE)).toEqual({accessURL: ZTF_LIGHT_CURVE_URL, inputParams: {
            id: {value: '', ref: 'col_0'},
            bad_catflags_mask: {value: 0, ref: undefined},
        }});
    });

    test('checkForDatalinkServDesc: null for a table with no DataLink descriptor', () => {
        expect(checkForDatalinkServDesc(CADC_DAO_DATALINK_TABLE)).toBeNull();
        expect(checkForDatalinkServDesc(PLAIN_TABLE)).toBeNull();
    });
});

describe('getDataLinkData', () => {
    test('a table that is not a DataLink table has no entries', () => {
        expect(getDataLinkData(PLAIN_TABLE)).toEqual([]);
        expect(getDataLinkData(undefined)).toEqual([]);
    });

    test('the CADC async cutout is not usable and is left out unless asked for', () => {
        expect(getDataLinkData(CADC_DAO_DATALINK_TABLE).map(({semantics, rowIdx}) => [semantics, rowIdx]))
            .toEqual([['#thumbnail', 0], ['#this', 1], ['#cutout', 2], ['#preview', 4]]);

        const allEntries = getDataLinkData(CADC_DAO_DATALINK_TABLE, true);
        expect(allEntries).toHaveLength(5);
        expect(allEntries[3]).toMatchObject({serviceDefRef: CADC_ASYNC_ID, serDef: undefined,
            dlAnalysis: {usableEntry: false}});
    });

    test('a row holds the DataLink columns, and a service_def row its descriptor with the row index', () => {
        const [, thisEntry, cutoutEntry] = getDataLinkData(CADC_DAO_DATALINK_TABLE);

        expect(thisEntry).toMatchObject({id: CADC_DAO_ID, url: `${CADC_FILES_URL}.fits`, semantics: '#this',
            localSemantics: '', contentType: 'application/fits', size: '4800960', serviceDefRef: '',
            serDef: undefined, error_message: '', description: 'download cadc:DAO/dao_c182_2022_010463.fits'});
        expect(cutoutEntry.serDef).toMatchObject({ID: CADC_SYNC_ID, dataLinkTableRowIdx: 2});
    });

    test('the #this file is paired with its one usable cutout', () => {
        const [, thisEntry, cutoutEntry] = getDataLinkData(CADC_DAO_DATALINK_TABLE);

        expect(thisEntry.relatedDLEntries.cutout).toBe(cutoutEntry);
        expect(cutoutEntry.relatedDLEntries.fullImage).toBe(thisEntry);
        expect(thisEntry.dlAnalysis.cutoutFullPair).toBe(true);
        expect(cutoutEntry.dlAnalysis).toMatchObject({cutoutFullPair: true, isThis: true});
    });

    test('a file with two usable cutouts is not paired', () => {
        // made up: the async cutout made usable by giving it a URL
        const table = {...CADC_DAO_DATALINK_TABLE, tableData: {...CADC_DAO_DATALINK_TABLE.tableData,
            data: CADC_DAO_DATALINK_TABLE.tableData.data.map((row, idx) =>
                idx === 3 ? [row[0], 'https://example.org/cutout', ...row.slice(2)] : row)}};

        const thisEntry = getDataLinkData(table)[1];

        expect(thisEntry.dlAnalysis.cutoutFullPair).toBe(false);
        expect(thisEntry.relatedDLEntries).toEqual({});
    });

    test('a Euclid cutout gives its band and grid to the image it is paired with', () => {
        const [visEntry, visCutoutEntry] = getDataLinkData(EUCLID_DATALINK_TABLE);

        expect(visEntry.relatedDLEntries.cutout).toBe(visCutoutEntry);
        expect(visEntry.dlAnalysis).toMatchObject({cutoutFullPair: true, bBand: true, isGrid: true});
        expect(visCutoutEntry.dlAnalysis).toMatchObject({bBand: true, isGrid: true, isThis: true});
    });

    test('a #counterpart image is paired with its cutout, and the cutout becomes a counterpart', () => {
        const entries = getDataLinkData(EUCLID_DATALINK_TABLE);
        const [hscCutoutEntry, hscEntry] = entries.slice(3);

        expect(hscEntry.relatedDLEntries.cutout).toBe(hscCutoutEntry);
        expect(hscCutoutEntry.dlAnalysis).toMatchObject({cutoutFullPair: true, isCounterpart: true, isThis: false});
    });

    test('the label, bandpass_name and cloud_access columns are read; bad cloud_access JSON is left out', () => {
        const [visEntry, , noiseEntry, hscCutoutEntry] = getDataLinkData(EUCLID_DATALINK_TABLE);

        expect(visEntry).toMatchObject({labelDLExt: 'VIS', bandpassNameDLExt: 'VIS', cloudAccess: {aws: {
            bucket_name: 'nasa-irsa-euclid-dr1', key: 'dr1/MER/101384791/VIS/EUC_MER_BGSUB-MOSAIC-VIS_TILE101384791.fits',
            region: 'us-east-1'}}});
        expect(noiseEntry.cloudAccess).toBeUndefined();
        expect(hscCutoutEntry).toMatchObject({labelDLExt: 'Hyper Suprime-Cam/R2', cloudAccess: undefined});
    });

    test('the search target, row target and s_region come from the table meta', () => {
        const [visEntry] = getDataLinkData(EUCLID_DATALINK_TABLE);

        expect(visEntry.positionWP.getLon()).toBeCloseTo(269.73);
        expect(visEntry.rowWP.getLat()).toBeCloseTo(66.0);
        expect(visEntry.sRegion).toBe('POLYGON 269.4 65.8 270.1 65.8 270.1 66.2 269.4 66.2');
    });

    test('a row with an error message is usable', () => {
        // made up: the #this row with an error and no URL, as DataLink sends when a file is missing
        const table = {...CADC_DAO_DATALINK_TABLE, tableData: {...CADC_DAO_DATALINK_TABLE.tableData,
            data: [[CADC_DAO_ID, '', '', 'NotFoundFault: file not found', '#this', '', '', '', '', '', '', '']]}};

        const [errorEntry] = getDataLinkData(table);

        expect(errorEntry).toMatchObject({error_message: 'NotFoundFault: file not found', url: '',
            dlAnalysis: {usableEntry: true}});
    });

    test('empty cells become empty strings', () => {
        const table = {...CADC_DAO_DATALINK_TABLE, tableData: {...CADC_DAO_DATALINK_TABLE.tableData,
            data: [[CADC_DAO_ID, `${CADC_FILES_URL}.fits`, null, undefined, '#this', null, null, 'application/fits',
                null, null, null, null]]}};

        const [entry] = getDataLinkData(table);

        expect(entry).toMatchObject({serviceDefRef: '', error_message: '', localSemantics: '', description: '',
            size: ''});
    });

    test('the ObsCore source row is added to each entry', () => {
        const [thisEntry] = getDataLinkData(CADC_DAO_DATALINK_TABLE, false, RUBIN_OBSCORE_TABLE, 0).slice(1);

        expect(thisEntry.sourceObsCoreData).toMatchObject({obs_id: 'CC_O_20241108_000253', dataproduct_type: 'image',
            access_format: 'application/x-votable+xml;content=datalink'});
    });
});

describe('getObsCoreData', () => {
    test('an ObsCore row as an object', () => {
        expect(getObsCoreData(RUBIN_OBSCORE_TABLE, 0)).toMatchObject({obs_collection: 'LSST.DP1',
            dataproduct_type: 'image', obs_id: 'CC_O_20241108_000253',
            s_region: 'POLYGON ICRS 52.779071 -28.240995 52.721578 -28.461865 52.968449 -28.511367 53.025383 -28.290376'});
    });

    test('nothing for a missing table, a negative row, or a table that is not ObsCore', () => {
        expect(getObsCoreData(undefined, 0)).toBeUndefined();
        expect(getObsCoreData(RUBIN_OBSCORE_TABLE, -1)).toBeUndefined();
        expect(getObsCoreData(PLAIN_TABLE, 0)).toBeUndefined();
    });
});

describe('isAnalysisTableDatalink', () => {
    // a file analysis report lists a table's columns as rows, the column name first
    const datalinkColumnRows = ['ID', 'access_url', 'service_def', 'error_message', 'semantics', 'description',
        'content_type', 'content_length'].map((name) => [name, 'char']);
    const makeReport = (columnRows, totalTableRows) => ({parts: [{type: 'Table', totalTableRows,
        details: {tableData: {data: columnRows}}}]});

    test('a single small table with the DataLink columns', () => {
        expect(isAnalysisTableDatalink(makeReport(datalinkColumnRows, 5))).toBe(true);
    });

    test('not when the table has 50 rows or more', () => {
        expect(isAnalysisTableDatalink(makeReport(datalinkColumnRows, 50))).toBe(false);
    });

    test('not when a DataLink column is missing', () => {
        expect(isAnalysisTableDatalink(makeReport(datalinkColumnRows.slice(1), 5))).toBe(false);
    });

    test('not when the table part has no rows or no table data', () => {
        expect(isAnalysisTableDatalink(makeReport([], 0))).toBe(false);
        expect(isAnalysisTableDatalink({parts: [{type: 'Table', totalTableRows: 0, details: {}}]})).toBe(false);
    });

    test('not when the report has no parts, more than one part, or an image part', () => {
        const tablePart = makeReport(datalinkColumnRows, 5).parts[0];
        expect(isAnalysisTableDatalink(undefined)).toBe(false);
        expect(isAnalysisTableDatalink({parts: [tablePart, tablePart]})).toBe(false);
        expect(isAnalysisTableDatalink({parts: [{...tablePart, type: 'Image'}]})).toBe(false);
    });
});
