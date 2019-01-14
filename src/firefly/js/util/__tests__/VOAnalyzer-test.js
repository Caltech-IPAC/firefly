// import initTest from '../InitTest.js';

import {reject} from 'lodash';
import {findTableCenterColumns} from '../VOAnalyzer.js';


describe('Center columns tests', () => {

    /**
     * guessing column name; ra/dec or lon/lat
     */
    test('by column name guess', () => {
        const table = { tableData: {}};
        let actual;

        // test ra/dec
        table.tableData.columns = [
            {name: 'ra'},
            {name: 'dec'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra');
        expect(actual.latCol).toEqual('dec');

        // test lon/lat
        table.tableData.columns = [
            {name: 'lon'},
            {name: 'lat'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('lon');
        expect(actual.latCol).toEqual('lat');

        // test fail
        table.tableData.columns = [
            {name: 'ra_unrecognized'},
            {name: 'dec_unrecognized'}
        ];
        actual = findTableCenterColumns(table);
        expect(actual).toBeFalsy();

    });

    /**
     * CENTER_COLUMN and CatalogCoordColumns in tableMeta
     */
    test('by tableMeta', () => {
        const table = {
            tableData: {
                columns: [
                    {name: 'ra1'},
                    {name: 'dec1'},
                    {name: 'ra2'},
                    {name: 'dec2'}
                ]
            }
        };
        let actual;

        // test CENTER_COLUMN
        table.tableMeta = {
            CENTER_COLUMN: 'ra1;dec1;EQ_J2000',
        };
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra1');
        expect(actual.latCol).toEqual('dec1');

        // test CatalogCoordColumns
        table.tableMeta = {
            CatalogCoordColumns: 'ra2;dec2;EQ_J2000'
        };
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra2');
        expect(actual.latCol).toEqual('dec2');

        // test missing columns
        table.tableMeta = {
            CatalogCoordColumns: 'ra3;dec3;EQ_J2000'
        };
        actual = findTableCenterColumns(table);
        expect(actual).toBeFalsy();
    });

    test('by UCD', () => {
        const table = {
            tableData: {
                columns: [
                    {name: 'ura', UCD: 'pos.eq.ra'},
                    {name: 'udec', UCD: 'pos.eq.dec'},
                ]
            }
        };
        const actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ura');
        expect(actual.latCol).toEqual('udec');
    });

    test('by UType', () => {
        const table = {
            tableData: {
                columns: [
                    {name: 'tra', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'},
                    {name: 'tdec', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'},
                ]
            }
        };
        const actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('tra');
        expect(actual.latCol).toEqual('tdec');
    });

    test('by ObsCore Name', () => {
        const table = {
            tableData: {
                columns: [
                    {name: 's_ra'},
                    {name: 's_dec'}
                ]
            }
        };

        // ObsCore column name table precedence over ra/dec
        const actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('s_ra');
        expect(actual.latCol).toEqual('s_dec');
    });

    test('precedence', () => {
        const table = {
            tableMeta: {
                CENTER_COLUMN: 'ra1;dec1;EQ_J2000',
                CatalogCoordColumns: 'ra2;dec2;EQ_J2000'
            },
            tableData: {
                columns: [
                    {name: 'lon'},
                    {name: 'lat'},
                    {name: 'ra'},
                    {name: 'dec'},
                    {name: 's_ra'},
                    {name: 's_dec'},
                    {name: 'tra', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'},
                    {name: 'tdec', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'},
                    {name: 'ura', UCD: 'pos.eq.ra'},
                    {name: 'udec', UCD: 'pos.eq.dec'},
                    {name: 'URA', UCD: 'POS_EQ_RA_MAIN'},
                    {name: 'UDEC', UCD: 'POS_EQ_DEC_MAIN'},
                    {name: 'ra1'},
                    {name: 'dec1'},
                    {name: 'ra2'},
                    {name: 'dec2'}
                ]
            }
        };
        let actual;

        // CENTER_COLUMN meta take highest precedence
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra1');
        expect(actual.latCol).toEqual('dec1');
        Reflect.deleteProperty(table.tableMeta, 'CENTER_COLUMN');

        // CatalogCoordColumns meta next
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra2');
        expect(actual.latCol).toEqual('dec2');
        Reflect.deleteProperty(table.tableMeta, 'CatalogCoordColumns');

        // alt UCD; POS_EQ_RA_MAIN/POS_EQ_DEC_MAIN next
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('URA');
        expect(actual.latCol).toEqual('UDEC');
        table.tableData.columns = reject(table.tableData.columns, (c) => ['URA', 'UDEC'].includes(c.name));

        // UCD next
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ura');
        expect(actual.latCol).toEqual('udec');
        table.tableData.columns = reject(table.tableData.columns, (c) => ['ura', 'udec'].includes(c.name));

        // UType next
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('tra');
        expect(actual.latCol).toEqual('tdec');
        table.tableData.columns = reject(table.tableData.columns, (c) => ['tra', 'tdec'].includes(c.name));

        // obscore/tap column name next
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('s_ra');
        expect(actual.latCol).toEqual('s_dec');
        table.tableData.columns = reject(table.tableData.columns, (c) => ['s_ra', 's_dec'].includes(c.name));

        // ra/dec column names combo next
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra');
        expect(actual.latCol).toEqual('dec');
        table.tableData.columns = reject(table.tableData.columns, (c) => ['ra', 'dec'].includes(c.name));

        // lon/lat column names combo next
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('lon');
        expect(actual.latCol).toEqual('lat');
        table.tableData.columns = reject(table.tableData.columns, (c) => ['lon', 'lat'].includes(c.name));

    });

    /**
     * check that left-most columns pair of the same UTypes is chosen
     */
    test('left most columns (UTypes)', () => {
        const table = {
            tableData: {
                columns: [
                    {name: 'tra1', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'},
                    {name: 'tdec1', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'},
                    {name: 'tra', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'},
                    {name: 'tdec', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'}
                ]
            }
        };

        const actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('tra1');
        expect(actual.latCol).toEqual('tdec1');
    });

    /**
     * check that left-most columns pair of the same UCDs is chosen
     */
    test('left most columns (UCDs)', () => {
        const table = {
            tableData: {
                columns: [
                    {name: 'ura1', UCD: 'pos.eq.ra'},
                    {name: 'udec1', UCD: 'pos.eq.dec'},
                    {name: 'ura', UCD: 'pos.eq.ra'},
                    {name: 'udec', UCD: 'pos.eq.dec'}
                ]
            }
        };

        const actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ura1');
        expect(actual.latCol).toEqual('udec1');
    });


});



