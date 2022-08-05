import {get} from 'lodash';

import * as TblUtil from '../TableUtil.js';         // used for named import
import TableUtil, {formatValue} from '../TableUtil.js';            // using default import
import {SelectInfo} from '../SelectInfo';
import {MetaConst} from '../../data/MetaConst';
import {dataReducer} from '../reducer/TableDataReducer.js';
import {TABLE_LOADED} from '../TablesCntlr.js';
import {NULL_TOKEN} from '../FilterInfo.js';


describe('TableUtil: ', () => {

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

    test('isColumnType', () => {
        const float1 = {name: 'float1', type: 'f'};
        const float2 = {name: 'float2', type: 'float'};
        const float3 = {name: 'float3', type: 'd'};
        const float4 = {name: 'float4', type: 'double'};

        expect(TblUtil.isColumnType(float1, TblUtil.COL_TYPE.FLOAT)).toBe(true);
        expect(TblUtil.isColumnType(float2, TblUtil.COL_TYPE.FLOAT)).toBe(true);
        expect(TblUtil.isColumnType(float3, TblUtil.COL_TYPE.FLOAT)).toBe(true);
        expect(TblUtil.isColumnType(float4, TblUtil.COL_TYPE.FLOAT)).toBe(true);

        const int1 = {name: 'int1', type: 'i'};
        const int2 = {name: 'int2', type: 'int'};
        const int3 = {name: 'int3', type: 'l'};
        const int4 = {name: 'int4', type: 'long'};

        expect(TblUtil.isColumnType(int1, TblUtil.COL_TYPE.INT)).toBe(true);
        expect(TblUtil.isColumnType(int2, TblUtil.COL_TYPE.INT)).toBe(true);
        expect(TblUtil.isColumnType(int3, TblUtil.COL_TYPE.INT)).toBe(true);
        expect(TblUtil.isColumnType(int4, TblUtil.COL_TYPE.INT)).toBe(true);

        const char1 = {name: 'char1', type: 'c'};
        const char2 = {name: 'char2', type: 'char'};
        const char3 = {name: 'char3', type: 's'};
        const char4 = {name: 'char4', type: 'str'};

        expect(TblUtil.isColumnType(char1, TblUtil.COL_TYPE.TEXT)).toBe(true);
        expect(TblUtil.isColumnType(char2, TblUtil.COL_TYPE.TEXT)).toBe(true);
        expect(TblUtil.isColumnType(char3, TblUtil.COL_TYPE.TEXT)).toBe(true);
        expect(TblUtil.isColumnType(char4, TblUtil.COL_TYPE.TEXT)).toBe(true);

        expect(TblUtil.isColumnType(char1, TblUtil.COL_TYPE.ANY)).toBe(true);
        expect(TblUtil.isColumnType(int1, TblUtil.COL_TYPE.ANY)).toBe(true);
        expect(TblUtil.isColumnType(float1, TblUtil.COL_TYPE.ANY)).toBe(true);

        expect(TblUtil.isColumnType(int1, TblUtil.COL_TYPE.NUMBER)).toBe(true);
        expect(TblUtil.isColumnType(float1, TblUtil.COL_TYPE.NUMBER)).toBe(true);
        expect(TblUtil.isColumnType(char1, TblUtil.COL_TYPE.NUMBER)).toBe(false);

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

    test('formatValue', () => {

        // test java-like String.format using fmtDisp column meta
        let res = formatValue({fmtDisp: 'cost $%.2f'}, 123.3432);
        expect(res).toEqual('cost $123.34');

        // test java-like String.format using format column meta
        res = formatValue({format: 'cost $%.2f'}, 123.3432);
        expect(res).toEqual('cost $123.34');

        // @Kartikeya Puri, please add tests for precision column meta here...
        // for precision, column must be numeric.. i.e.  {type: 'float', precision: 'E3'}
        // see TableUtil.js->formatValue function descriptions from details


    });

});


describe('TableUtil: datarights', () => {

    test('DATARIGHTS_COL', () => {
        const table = {
            tableMeta: {[MetaConst.DATARIGHTS_COL]: 'a'},
            totalRows: 7,
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

        expect(TblUtil.hasRowAccess(table, 0)).toBe(true);
        expect(TblUtil.hasRowAccess(table, 1)).toBe(false);
        expect(TblUtil.hasRowAccess(table, 2)).toBe(true);
        expect(TblUtil.hasRowAccess(table, 3)).toBe(true);
        expect(TblUtil.hasRowAccess(table, 4)).toBe(true);
        expect(TblUtil.hasRowAccess(table, 5)).toBe(false);
        expect(TblUtil.hasRowAccess(table, 6)).toBe(false);
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

        expect(TblUtil.hasRowAccess(table, 0)).toBe(true);
        expect(TblUtil.hasRowAccess(table, 1)).toBe(false);
        expect(TblUtil.hasRowAccess(table, 2)).toBe(false);
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

        expect(TblUtil.hasRowAccess(table, 0)).toBe(true);
        expect(TblUtil.hasRowAccess(table, 1)).toBe(true);
        expect(TblUtil.hasRowAccess(table, 2)).toBe(false);
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

        expect(TblUtil.hasRowAccess(otable, 0)).toBe(true);
        expect(TblUtil.hasRowAccess(otable, 1)).toBe(true);
        expect(TblUtil.hasRowAccess(otable, 2)).toBe(false);
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

        expect(TblUtil.hasRowAccess(otable, 0)).toBe(true);
        expect(TblUtil.hasRowAccess(otable, 1)).toBe(true);
        expect(TblUtil.hasRowAccess(otable, 2)).toBe(false);
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
        expect(TblUtil.hasRowAccess(otable, 0)).toBe(true);
        expect(TblUtil.hasRowAccess(otable, 1)).toBe(false);
        expect(TblUtil.hasRowAccess(otable, 2)).toBe(false);
    });

});


describe('TableUtil: client_table', () => {

    const table = {
        totalRows: 6,
        tableData: {
            columns: [  {name: 'c1', type: 'char', nullString: 'null'},
                        {name: 'c2', type: 'double'},
                        {name: 'c3', type: 'int'}
            ],
            data: [
                ['abc'      , 0.123     ,  100      ],
                [undefined  , -2.34     ,   -1      ],
                ['123'      ,   0.0     , null      ],
                [''         ,  null     ,   50      ],
                [null       , 0.131     ,  -20      ],
                ['ABC'      , undefined ,   undefined]
            ],
        }
    };

    test('Sort', () => {
        // undefined is smallest, then null, then natural order
        let res = TblUtil.processRequest(table, {sortInfo: 'ASC,c1'});
        expect(TblUtil.getColumnValues(res, 'c1')).toEqual([undefined, null, '', '123', 'ABC', 'abc']);

        res = TblUtil.processRequest(table, {sortInfo: 'DESC,c1'});
        expect(TblUtil.getColumnValues(res, 'c1')).toEqual(['abc', 'ABC', '123', '', null, undefined]);

        res = TblUtil.processRequest(table, {sortInfo: 'ASC,c2'});
        expect(TblUtil.getColumnValues(res, 'c2')).toEqual([undefined, null, -2.34, 0.0, 0.123, 0.131]);

        res = TblUtil.processRequest(table, {sortInfo: 'DESC,c2'});
        expect(TblUtil.getColumnValues(res, 'c2')).toEqual([0.131, 0.123, 0.0, -2.34, null, undefined]);

        res = TblUtil.processRequest(table, {sortInfo: 'ASC,c3'});
        expect(TblUtil.getColumnValues(res, 'c3')).toEqual([undefined, null, -20, -1, 50, 100]);

        res = TblUtil.processRequest(table, {sortInfo: 'DESC,c3'});
        expect(TblUtil.getColumnValues(res, 'c3')).toEqual([100, 50, -1, -20, null, undefined]);
    });

    test('filter', () => {

        let res = TblUtil.processRequest(table, {filters: "c1 IN ('abc','')"});
        expect(TblUtil.getColumnValues(res, 'c2')).toEqual([0.123, null, undefined]);       // filtering is not case sensitive. abc === ABC

        res = TblUtil.processRequest(table, {filters: `c1 IN ('abc', ${NULL_TOKEN})`});
        expect(TblUtil.getColumnValues(res, 'c2')).toEqual([0.123, 0.131, undefined]);      // testing special NULL_TOKEN

        res = TblUtil.processRequest(table, {filters: 'c2 > 0'});
        expect(TblUtil.getColumnValues(res, 'c2')).toEqual([0.123, 0.131]);                 // testing greater than (>)

        res = TblUtil.processRequest(table, {filters: 'c2 < 0'});
        expect(TblUtil.getColumnValues(res, 'c2')).toEqual([-2.34]);                        // equality test, null and undefined are ignored

        res = TblUtil.processRequest(table, {filters: 'c1 < a'});
        expect(TblUtil.getColumnValues(res, 'c1')).toEqual(['123', '']);                    // testing less than (<) on string

        res = TblUtil.processRequest(table, {filters: 'c1 IS NULL'});
        expect(TblUtil.getColumnValues(res, 'c2')).toEqual([-2.34, 0.131]);                 // testing IS NULL on a string column

        res = TblUtil.processRequest(table, {filters: 'c2 IS NULL'});
        expect(TblUtil.getColumnValues(res, 'c1')).toEqual(['', 'ABC']);                    // testing IS NULL on numeric column

        res = TblUtil.processRequest(table, {filters: 'c2 IS NOT NULL'});
        expect(TblUtil.getColumnValues(res, 'c1')).toEqual(['abc', undefined, '123', null]); // testing IS NOT NULL
    });

});

