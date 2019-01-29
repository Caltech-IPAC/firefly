import * as TblUtil from '../TableUtil.js';
import {FilterInfo} from '../FilterInfo.js';


describe('Table Utils', () => {

    test('isTableLoaded', () => {
        // a simple test to ensure when a table is loaded.
        const table = {
            isFetching: true,
            tableMeta: {'Loading-Status': 'COMPLETED'}
        };

        let actual = TblUtil.isTableLoaded(table);
        expect(actual).toBe(false);                                     // it's not loaded because isFetching is true

        Reflect.deleteProperty(table, 'isFetching');        // remove that prop from the table ...
        actual = TblUtil.isTableLoaded(table);
        expect(actual).toBe(true);                                      // now it's loaded.. with status of COMPLETED

        table.tableMeta = {'Loading-Status': 'LOADING'};                // change status to something else...
        actual = TblUtil.isTableLoaded(table);
        expect(actual).toBe(false);                                     // not loaded because status is not COMPLETED

    });

});


describe('FilterInfo', () => {

    test('autoCorrectConditions: string columns', () => {

        /*
        *  This test is a little trickier.   conditionValidator() takes a tbl_id that uses TblUtil.getTblById to resolve a tableModel.
        *  This involves a fully functional redux store which is not available to our JS test environment at the moment.
        *  So, we will 'fake' the return value of getTblById by 'mocking' that function
        */
        const aStringColumn = {
            tableData: {
                columns: [ {name: 'desc', type: 'char'}],
            }
        };

        TblUtil.getTblById = jest.fn().mockReturnValue(aStringColumn);          // mock getTblById to return the aStringColumn table

        let actual = FilterInfo.conditionValidator('=abc', 'a_fake_tbl_id', 'desc');
        expect(actual.valid).toBe(true);
        expect(actual.value).toBe("= 'abc'");      // the validator correctly insert space and quote around a value of a string column.

        actual = FilterInfo.conditionValidator('abc', 'a_fake_tbl_id', 'desc');
        expect(actual.valid).toBe(true);
        expect(actual.value).toBe("like '%abc%'");      // the validator correctly convert it into a LIKE operator
    });

    test('autoCorrectConditions: numeric columns', () => {

        const aNumericColumn = {
            tableData: {
                columns: [ {name: 'ra', type: 'double'}],
            }
        };

        TblUtil.getTblById = jest.fn().mockReturnValue(aNumericColumn);          // once again mock getTblById to return the different (numeric) table

        const {valid, value} = FilterInfo.conditionValidator('>1.23', 'a_fake_tbl_id', 'ra');
        expect(valid).toBe(true);
        expect(value).toBe('> 1.23');      // the validator correctly insert space and quote around a value of a string column.
    });


});

