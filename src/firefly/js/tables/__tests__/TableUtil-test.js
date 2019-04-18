import {get} from 'lodash';

import * as TblUtil from '../TableUtil.js';         // used for named import
import TableUtil from '../TableUtil.js';            // using default import
import {FilterInfo} from '../FilterInfo.js';
import {SelectInfo} from '../SelectInfo';
import {MetaConst} from '../../data/MetaConst';
import {hasRowAccess} from '../TableUtil';
import {dataReducer} from '../reducer/TableDataReducer.js';
import {TABLE_LOADED} from '../TablesCntlr.js';


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
                        ['a-1', 'b-1', 'c-1'],
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
        tableInfo.selectInfo = sInfoCls.data;
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

    test('DATARIGHTS_COL', () => {
        const table = {
            tableMeta: {[MetaConst.DATARIGHTS_COL]: 'a'},
            totalRows: 6,
            tableData: {
                columns: [ {name: 'a'}, {name: 'b'}, {name: 'c'}],
                data: [
                    ['true' , 'b-1', 'c-1'],
                    ['false', 'b-2', 'c-2'],
                    ['1'    , 'b-3', 'c-3'],
                    ['public', 'b-4', 'c-4'],
                    ['secure', 'b-5', 'c-5'],           // authenticated, public access is allowed.
                    ['0'    , 'b-6', 'c-6'],
                    [''     , 'b-7', 'c-7'],
                ],
            }
        };

        expect(hasRowAccess(table, 0)).toBe(true);
        expect(hasRowAccess(table, 1)).toBe(false);
        expect(hasRowAccess(table, 2)).toBe(true);
        expect(hasRowAccess(table, 3)).toBe(true);
        expect(hasRowAccess(table, 4)).toBe(true);
        expect(hasRowAccess(table, 5)).toBe(false);
        expect(hasRowAccess(table, 6)).toBe(false);
    });

    test('RELEASE_DATE_COL', () => {
        const table = {
            tableMeta: {[MetaConst.RELEASE_DATE_COL]: 'a'},
            totalRows: 3,
            tableData: {
                columns: [ {name: 'a'}, {name: 'b'}, {name: 'c'}],
                data: [
                    ['2018/01/01', 'b-1', 'c-1'],
                    ['2100/01/01', 'b-2', 'c-2'],
                    [''          , 'b-3', 'c-3'],
                ],
            }
        };

        expect(hasRowAccess(table, 0)).toBe(true);
        expect(hasRowAccess(table, 1)).toBe(false);
        expect(hasRowAccess(table, 2)).toBe(false);
    });

    test('both RELEASE_DATE_COL and DATARIGHTS_COL', () => {
        const table = {
            tableMeta: {
                [MetaConst.RELEASE_DATE_COL]: 'a',
                [MetaConst.DATARIGHTS_COL]: 'b'
            },
            totalRows: 3,
            tableData: {
                columns: [ {name: 'a'}, {name: 'b'}, {name: 'c'}],
                data: [
                    ['2018/01/01', 'false', 'c-1'],     // public release without data rights
                    ['2100/01/01', 'true' , 'c-2'],     // not released, but has data rights
                    [''          , 'false', 'c-3'],     // not released, and no data rights
                ],
            }
        };

        expect(hasRowAccess(table, 0)).toBe(true);
        expect(hasRowAccess(table, 1)).toBe(true);
        expect(hasRowAccess(table, 2)).toBe(false);
    });

    test('Proprietary data by ObsCore cnames', () => {
        const table = {
            tbl_id: 'id123',
            totalRows: 3,
            tableData: {
                columns: [ {name: 'obs_release_date'}, {name: 'data_rights'}, {name: 'obs_id'}],
                data: [
                    ['2018/01/01', 'false', 'c-1'],     // public release without data rights
                    ['2100/01/01', 'true' , 'c-2'],     // not released, but has data rights
                    [''          , 'false', 'c-3'],     // not released, and no data rights
                ],
            }
        };
        TblUtil.getTblById = jest.fn().mockReturnValue(table);

        const dataRoot = dataReducer({data:{id123: table}}, {type: TABLE_LOADED, payload: table});
        const otable = get(dataRoot, 'id123');

        expect(hasRowAccess(otable, 0)).toBe(true);
        expect(hasRowAccess(otable, 1)).toBe(true);
        expect(hasRowAccess(otable, 2)).toBe(false);
    });

    test('Proprietary data by utype', () => {
        const table = {
            tbl_id: 'id123',
            totalRows: 3,
            tableData: {
                columns: [ {name: 'a', utype: 'obscore:Curation.releaseDate'}, {name: 'b', utype: 'obscore:Curation.rights'}, {name: 'c'}],
                data: [
                    ['2018/01/01', 'false', 'c-1'],
                    ['2100/01/01', 'true' , 'c-2'],
                    [''          , 'false', 'c-3'],
                ],
            }
        };
        TblUtil.getTblById = jest.fn().mockReturnValue(table);

        const dataRoot = dataReducer({data:{id123: table}}, {type: TABLE_LOADED, payload: table});
        const otable = get(dataRoot, 'id123');

        expect(hasRowAccess(otable, 0)).toBe(true);
        expect(hasRowAccess(otable, 1)).toBe(true);
        expect(hasRowAccess(otable, 2)).toBe(false);
    });

    test('Proprietary data by UCD', () => {
        const table = {
            tbl_id: 'id123',
            totalRows: 3,
            tableData: {
                columns: [ {name: 'a', UCD: 'time.release'}, {name: 'b'}, {name: 'c'}],
                data: [
                    ['2018/01/01', 'false', 'c-1'],
                    ['2100/01/01', 'true' , 'c-2'],
                    [''          , 'false', 'c-3'],
                ],
            }
        };
        TblUtil.getTblById = jest.fn().mockReturnValue(table);

        const dataRoot = dataReducer({data:{id123: table}}, {type: TABLE_LOADED, payload: table});
        const otable = get(dataRoot, 'id123');

        // only release date matters
        expect(hasRowAccess(otable, 0)).toBe(true);
        expect(hasRowAccess(otable, 1)).toBe(false);
        expect(hasRowAccess(otable, 2)).toBe(false);
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

