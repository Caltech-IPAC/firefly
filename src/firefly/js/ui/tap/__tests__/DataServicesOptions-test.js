import {dispatchAppOptions} from '../../../core/AppDataCntlr.js';
import {MetaConst} from '../../../data/MetaConst.js';
import {getDataServiceOptionByTable} from '../DataServicesOptions.js';

// made up: app options for all services, and for the IRSA service only
const APP_OPTIONS = {dataServiceOptions: {preferCutout: true, productTitleTemplate: '${obs_id}',
    irsa: {cutoutDefSizeDeg: 0.05}}};

/**
 * A table with data service options in its meta.
 * @param {Object} tableOptions - the table's own data service options
 * @param {string} [dataServiceId]
 * @return {TableModel}
 */
function makeTable(tableOptions, dataServiceId) {
    return {tbl_id: 'tbl-1', tableMeta: {[MetaConst.DATA_SERVICE_OPTIONS]: JSON.stringify(tableOptions),
        [MetaConst.DATA_SERVICE_ID]: dataServiceId}};
}

describe('getDataServiceOptionByTable', () => {
    // the test setup loads this module before a mock could replace getAppOptions, so the options go in the store
    beforeAll(() => {
        dispatchAppOptions(APP_OPTIONS);
    });

    test('the table option is used before the app option', () => {
        expect(getDataServiceOptionByTable('productTitleTemplate', makeTable({productTitleTemplate: '${obs_title}'})))
            .toBe('${obs_title}');
    });

    test('a table option set to an empty string or false is kept', () => {
        expect(getDataServiceOptionByTable('productTitleTemplate', makeTable({productTitleTemplate: ''}))).toBe('');
        expect(getDataServiceOptionByTable('preferCutout', makeTable({preferCutout: false}))).toBe(false);
    });

    test('a missing table option falls back to the app option for the table data service', () => {
        expect(getDataServiceOptionByTable('cutoutDefSizeDeg', makeTable({}, 'irsa'))).toBe(0.05);
        expect(getDataServiceOptionByTable('preferCutout', makeTable({}, 'irsa'))).toBe(true);
    });

    test('an option set nowhere gives the default', () => {
        expect(getDataServiceOptionByTable('enableObsCoreDownload', makeTable({}), 'default')).toBe('default');
    });
});
