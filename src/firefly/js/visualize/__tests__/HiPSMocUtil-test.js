import {isMOCFitsFromUploadAnalsysis} from '../HiPSMocUtil.js';


describe('HiPSMocUtil', () => {
    // upload analysis report of a MOC v2 FITS file; header rows are [#, key, value, comment]
    const mocV2Report = (tfields) => ({
        dataTypes: 'HeaderOnly,Table',
        parts: [
            {type: 'HeaderOnly'},
            {type: 'Table', details: {tableData: {data: [
                ['0', 'MOCVERS', '2.0', ''],
                ['1', 'MOCDIM', 'SPACE', ''],
                ['2', 'ORDERING', 'NUNIQ', ''],
                ['3', 'COORDSYS', 'C', ''],
                ['4', 'MOCORD_S', '10', ''],
                ['5', 'TFIELDS', tfields, ''],
                ['6', 'TTYPE1', 'UNIQ', ''],
                ['7', 'TFORM1', 'K', ''],
            ]}}},
        ],
    });

    test('isMOCFitsFromUploadAnalsysis accepts a MOC v2 table with one column', () => {
        expect(isMOCFitsFromUploadAnalsysis(mocV2Report('1')).valid).toBe(true);
    });

    test('isMOCFitsFromUploadAnalsysis rejects a MOC v2 table with more than one column', () => {
        expect(isMOCFitsFromUploadAnalsysis(mocV2Report('3')).valid).toBe(false);
    });
});
