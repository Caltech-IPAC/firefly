import {get, set} from 'lodash';

import * as TblUtil from '../TableUtil.js';         // used for named import
import TableUtil, {formatValue, fixPageSize, getTblInfo} from '../TableUtil.js';            // using default import
import {SelectInfo} from '../SelectInfo';
import {MetaConst} from '../../data/MetaConst';
import {dataReducer} from '../reducer/TableDataReducer.js';
import {TABLE_LOADED} from '../TablesCntlr.js';
import {NULL_TOKEN} from '../FilterInfo.js';
import {MAX_ROW} from '../TableRequestUtil.js';


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
        const float1 = {name: 'float1', type: 'real'};
        const float2 = {name: 'float2', type: 'float'};
        const float3 = {name: 'float4', type: 'double'};

        expect(TblUtil.isColumnType(float1, TblUtil.COL_TYPE.FLOAT)).toBe(true);
        expect(TblUtil.isColumnType(float2, TblUtil.COL_TYPE.FLOAT)).toBe(true);
        expect(TblUtil.isColumnType(float3, TblUtil.COL_TYPE.FLOAT)).toBe(true);

        const int1 = {name: 'int1', type: 'long'};
        const int2 = {name: 'int2', type: 'int'};
        const int3 = {name: 'int3', type: 'short'};
        const int4 = {name: 'int4', type: 'integer'};

        expect(TblUtil.isColumnType(int1, TblUtil.COL_TYPE.INT)).toBe(true);
        expect(TblUtil.isColumnType(int2, TblUtil.COL_TYPE.INT)).toBe(true);
        expect(TblUtil.isColumnType(int3, TblUtil.COL_TYPE.INT)).toBe(true);
        expect(TblUtil.isColumnType(int4, TblUtil.COL_TYPE.INT)).toBe(true);

        const char1 = {name: 'char2', type: 'char'};

        expect(TblUtil.isColumnType(char1, TblUtil.COL_TYPE.TEXT)).toBe(true);

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

        //for DMS, islat = true therefore latitude (dec)
        res = formatValue({type: 'float', precision: 'DMS'}, '30.263'); //valid DMS range value
        expect(res).toEqual('+30d15m46.8s');

        //for HMS, islat = false therefore longitutde (ra)
        res = formatValue({type: 'float', precision: 'HMS'}, '30.263'); //valid HMS range value
        expect(res).toEqual('2h01m03.12s');

        res = formatValue({type: 'float', precision: 'DMS5'}, '-15.530694'); //n=5 will be ignored
        expect(res).toEqual('-15d31m50.5s');

        res = formatValue({type: 'float', precision: 'HMS3'}, '324.42'); //n=3 will be ignored
        expect(res).toEqual('21h37m40.80s');

        //Testing out of range values for DMS [-90, 90] & HMS [0, 360]
        //These should throw latitude out of range, longitude out of range
        //and longitude cannot be negative errors respectively
        //- uncomment these after ticket FIREFLY-1056 is closed

        //res = formatValue({type: 'float', precision: 'DMS'}, '100.5');
        //res = formatValue({type: 'float', precision: 'HMS'}, '370.2');
        //res = formatValue({type: 'float', precision: 'HMS'}, '-10.0');

        //non-numeric val=null returns '', precision/type does not matter here
        res = formatValue({type: 'float', precision: 'E2'}, null);
        expect(res).toEqual('');

        res = formatValue({type: 'float', precision: 'E3'}, 453.450664); //3 significant digits after decimal
        expect(res).toEqual('4.535E+2');

        res = formatValue({type: 'double', precision: 'E1'}, 57.365); //1 significant digit after decimal
        expect(res).toEqual('5.7E+1');

        res = formatValue({type: 'double', precision: 'E0'}, 46.365); //rounds to 5E+1
        expect(res).toEqual('5E+1');

        res = formatValue({type: 'int', precision: 'E3'}, 453445.678); //post decimal value is truncated, as type=int
        expect(res).toEqual('453445');

        res = formatValue({type: 'long', precision: 'E3'}, 57345); //value should be unchanged for type=long
        expect(res).toEqual('57345');

        res = formatValue({type: 'float', precision: 'G3'}, 43.450664); //3 significant digits
        expect(res).toEqual('43.5'); //rounding post decimal and truncating the rest

        res = formatValue({type: 'float', precision: 'G2'}, 43.6); //2 significant digits
        expect(res).toEqual('44'); //rounds to 44

        res = formatValue({type: 'int', precision: 'G5'}, 43.5); //post decimal value is truncated, as type=int
        expect(res).toEqual('43');

        res = formatValue({type: 'long', precision: 'G1'}, 450); //value should be unchanged for type=long
        expect(res).toEqual('450');

        res = formatValue({type: 'double', precision: 'G5'}, 43); //5 significant digits
        expect(res).toEqual('43.000');

        res = formatValue({type: 'float', precision: 'F2'}, 1.999); //2 significant digits after decimal
        expect(res).toEqual('2.00'); //rounds to 2.00

        res = formatValue({type: 'float', precision: 'F3'}, 43); //2 significant digits after decimal
        expect(res).toEqual('43.000');

        res = formatValue({type: 'int', precision: 'F3'}, 99.87); //post decimal value is truncated, as type=int
        expect(res).toEqual('99');

        res = formatValue({type: 'long', precision: 'F3'}, 125); //value should be unchanged for type=long
        expect(res).toEqual('125');

        res = formatValue({type: 'double', precision: 'F3'}, 45.19256); //3 significant digits after decimal
        expect(res).toEqual('45.193');
    });

    test('fixPageSize', () => {

        expect(fixPageSize(123)).toEqual(123);
        expect(fixPageSize('123')).toEqual(123);
        expect(fixPageSize(12.3)).toEqual(12);
        expect(fixPageSize('12.3')).toEqual(12);
        expect(fixPageSize('12a')).toEqual(12);
        expect(fixPageSize('')).toEqual(MAX_ROW);
        expect(fixPageSize('abc')).toEqual(MAX_ROW);
        expect(fixPageSize(undefined)).toEqual(MAX_ROW);
        expect(fixPageSize(null)).toEqual(MAX_ROW);
        expect(fixPageSize(NaN)).toEqual(MAX_ROW);
    });

    test('getTblInfo: paging', () => {

        const table = {
            totalRows: 999,
            request:{pageSize:100},
            highlightedRow: 0,
        };
        let info = getTblInfo(table);
        expect(info.totalRows).toEqual(999);
        expect(info.highlightedRow).toEqual(0);
        expect(info.startIdx).toEqual(0);
        expect(info.endIdx).toEqual(100);
        expect(info.hlRowIdx).toEqual(0);
        expect(info.currentPage).toEqual(1);
        expect(info.pageSize).toEqual(100);
        expect(info.totalPages).toEqual(10);

        // override pageSize
        info = getTblInfo(table, 200);
        expect(info.startIdx).toEqual(0);
        expect(info.endIdx).toEqual(200);
        expect(info.pageSize).toEqual(200);
        expect(info.totalPages).toEqual(5);

        // highlightedRow in the middle of the table
        table.highlightedRow = 125;
        info = getTblInfo(table);           // page size is still 100
        expect(info.startIdx).toEqual(100);
        expect(info.endIdx).toEqual(200);
        expect(info.hlRowIdx).toEqual(25);      // hlRowIdx is relative to current page
        expect(info.currentPage).toEqual(2);

        // missing pageSize with highlighted row in the middle
        table.request.pageSize = undefined;
        table.highlightedRow = 125;
        info = getTblInfo(table);
        expect(info.hlRowIdx).toEqual(125);
        expect(info.startIdx).toEqual(0);
        expect(info.endIdx).toEqual(999);
        expect(info.pageSize).toEqual(MAX_ROW);
        expect(info.totalPages).toEqual(1);
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

    test('smartMerge', () => {
        const table = {
            totalRows: 3,
            request:{pageSize:100},
            highlightedRow: 0,
            tableData: {
                columns: [ {name: 'a'}, {name: 'b'}, {name: 'c'}],
                data: [
                    ['a-1', 'b-1', 'c-1'],
                    ['a-2', 'b-2', 'c-2'],
                    ['a-3', 'b-3', 'c-3'],
                ],
            }
        };

        // simple update
        let ntable = TblUtil.smartMerge(table, { request:{pageSize:999}} );
        expect(ntable.request.pageSize).toBe(999);

        // remove a path
        ntable = TblUtil.smartMerge(table, { request: undefined});
        expect(ntable.request).toBe(undefined);

        // add rows
        let changes = {totalRows: 5};
        set(changes, 'tableData.data.3', ['a-4', 'b-4', 'c-4']);
        set(changes, 'tableData.data.4', ['a-5', 'b-5', 'c-5']);

        ntable = TblUtil.smartMerge(table, changes);
        expect(ntable.totalRows).toBe(5);
        expect(ntable.tableData.data.length).toBe(5);
        expect(ntable.tableData.data[4][2]).toBe('c-5');

        // clear tableData
        ntable = TblUtil.smartMerge(table, { totalRows: 0, tableData: undefined});
        expect(ntable.totalRows).toBe(0);
        expect(ntable.tableData).toBe(undefined);

    });
});

