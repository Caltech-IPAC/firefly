import * as TblUtil from '../TableUtil.js';         // used for named import
import TableUtil from '../TableUtil.js';            // using default import
import {FilterInfo} from '../FilterInfo.js';
import {SelectInfo} from '../SelectInfo';


describe('TableUtil:', () => {

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

    test('getSelectedData', () => {
        // this case is a bit more complicated.  it needs to test getSelectedData and
        // mock getTblInfoById, but both are in TableUtil.js module.
        // you cannot mock a function referenced internally by the tested function
        // so, we have to use 'default' import instead of 'named' import to mock
        // the referenced function.
        // this also demonstrate testing a function returning a promise.

        const tableInfo = {
            totalRows: 3,
            selectInfo: SelectInfo.newInstance({selectAll:true, rowCount: 3}).data,
            tableModel: {
                totalRows: 3,
                tableData: {
                    columns: [ {name: 'a'}, {name: 'b'}, {name: 'c'}],
                    data: [
                        ['a-1', 'b-1', 'b-1'],
                        ['a-2', 'b-2', 'c-2'],
                        ['a-3', 'b-3', 'c-3'],
                    ],
                }
            }
        };

        TableUtil.getTblInfoById = jest.fn().mockReturnValue(tableInfo);

        // test all selected from column 'a' and 'b' in reversed ordered
        TableUtil.getSelectedData('mocked', ['b', 'a'])
            .then(({totalRows, tableData}) => {

                expect(totalRows).toBe(3);

                const {columns, data} = tableData;
                expect(columns).toHaveLength(2);
                expect(data).toEqual(
                    [
                        ['b-1', 'a-1'],
                        ['b-2', 'a-2'],
                        ['b-3', 'a-3'],
                    ]
                );
            });

        // test row 2-3 selected from column 'a', 'b', and 'd' where 'd' does not exists
        const sInfoCls = SelectInfo.newInstance({rowCount: 3});
        sInfoCls.setRowSelect(1, true);
        sInfoCls.setRowSelect(2, true);
        TableUtil.getSelectedData('mocked', ['a', 'b', 'd'])
            .then(({totalRows, tableData}) => {

                expect(totalRows).toBe(2);

                const {columns, data} = tableData;
                expect(columns).toHaveLength(2);
                expect(data).toEqual(
                    [
                        ['a-2', 'b-2'],
                        ['a-3', 'b-3'],
                    ]
                );
            });
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
        expect(value).toBe('> 1.23');      // the validator correctly insert space and no quotes on numeric columns.
    });


});

