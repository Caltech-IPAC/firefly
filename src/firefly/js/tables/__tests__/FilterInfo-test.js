
import * as TblUtil from '../TableUtil.js';         // used for named import
import {FilterInfo} from '../FilterInfo.js';

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
        expect(value).toBe('> 1.23');      // the validator correctly insert space and no quotes on numeric columns.
    });

});

