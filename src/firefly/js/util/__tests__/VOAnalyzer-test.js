// import initTest from '../InitTest.js';

import {reject} from 'lodash';
import {findTableCenterColumns, isCatalog, hasCoverageData, isMetaDataTable} from '../VOAnalyzer';


describe('Center columns tests, isCatalog test', () => {

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
        expect(actual).toBeTruthy();
        expect(actual.lonCol).toEqual('ra');
        expect(actual.latCol).toEqual('dec');

        // test lon/lat
        table.tableData.columns = [
            {name: 'lon'},
            {name: 'lat'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual).toBeTruthy();
        expect(actual.lonCol).toEqual('lon');
        expect(actual.latCol).toEqual('lat');


        // test ra1/dec1
        table.tableData.columns = [
            {name: 'ra1'},
            {name: 'dec1'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual).toBeTruthy();
        expect(actual.lonCol).toEqual('ra1');
        expect(actual.latCol).toEqual('dec1');


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

        table.tableData.columns=[
            {name: 'ra'},
            {name: 'dec'},
            ];
        table.tableMeta = {
            CatalogCoordColumns: 'ra3;dec3;EQ_J2000'
        };
        actual = findTableCenterColumns(table);
        expect(actual).toBeFalsy();

        table.tableMeta = { };
        actual = findTableCenterColumns(table);
        expect(actual).toBeTruthy();
        expect(actual.lonCol).toEqual('ra');
        expect(actual.latCol).toEqual('dec');


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

    /**
     * check the isCatalog works as expected
     */
    test('test isCatalog', () => {
        const table = {
            tableData: {
                columns: [
                    {name: 'ra'},
                    {name: 'dec'},
                    {name: 'ra1'},
                    {name: 'dec1'},
                    {name: 'uuuura', UCD: 'pos.eq.ra'},
                    {name: 'uuuudec', UCD: 'pos.eq.dec'}
                ]
            },
            tableMeta : {
                CENTER_COLUMN: 'ra1;dec1;EQ_J2000',
                CatalogOverlayType: ''
            }
        };
        let result;
        result = isCatalog(table);
        expect(result).toBeTruthy();

        // check case insensitivity
        table.tableMeta.CatalogOverlayType= 'FalsE';
        result = isCatalog(table);
        expect(result).toBeFalsy();

        // check case any value in CatalogOverlayType
        table.tableMeta.CatalogOverlayType= 'true';
        result = isCatalog(table);
        expect(result).toBeTruthy();


        // check finding ucd
        table.tableMeta= {};
        result = isCatalog(table);
        expect(result).toBeTruthy();


        // check finding other columns
        table.tableData.columns= [ {name: 'x'}, {name: 'y'} ];
        table.tableMeta.CatalogOverlayType= 'true';
        table.tableMeta.CENTER_COLUMN= 'x;y;EQ_J2000';
        result = isCatalog(table);
        expect(result).toBeTruthy();


        // check ucd
        table.tableMeta= {};
        table.tableData.columns= [
            {name: 'uuuura', UCD: 'pos.eq.ra'},
            {name: 'uuuudec', UCD: 'pos.eq.dec'}
        ];
        result = isCatalog(table);
        expect(result).toBeTruthy();

        // check utype
        table.tableData.columns= [
            {name: 'ttttttra1', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'},
            {name: 'tttttdec1', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'},
        ];
        result = isCatalog(table);
        expect(result).toBeTruthy();


        // check CatalogOverlayType not defined
        table.tableData.columns= [
            {name: 'ra'},
            {name: 'dec'},
        ];
        result = isCatalog(table);
        expect(result).toBeFalsy();

    });

    /**
     * check the hasCoverageData works as expected
     */
    test('test hasCoverageData', () => {
        const table = {
            totalRows: 2,
            tableData: {
                columns: [
                    {name: 'ra'},
                    {name: 'dec'},
                    {name: 'ra1'},
                    {name: 'dec1'},
                    {name: 'uuuura', UCD: 'pos.eq.ra'},
                    {name: 'uuuudec', UCD: 'pos.eq.dec'}
                ]
            },
            tableMeta : {
                CENTER_COLUMN: 'ra1;dec1;EQ_J2000',
                CatalogOverlayType: ''
            }
        };
        let result;
        result = hasCoverageData(table);
        expect(result).toBeTruthy();


        // check guessing
        table.tableMeta= {};
        result = hasCoverageData(table);
        expect(result).toBeTruthy();

        // check unrecognized columns
        table.tableData.columns= [ {name: 'x'}, {name: 'y'}, ];
        result = hasCoverageData(table);
        expect(result).toBeFalsy();

        // check meta data defining  columns
        table.tableMeta= {CENTER_COLUMN: 'x;y;GAL'};
        result = hasCoverageData(table);
        expect(result).toBeTruthy();


        // check recognizing columns by utype
        table.tableMeta= {};
        table.tableData.columns= [
            {name: 'ttttttra1', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'},
            {name: 'tttttdec1', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'},
        ];
        result = hasCoverageData(table);
        expect(result).toBeTruthy();

        // check recognizing columns by utype
        table.tableData.columns= [
            {name: 'x', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'},
            {name: 'y', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'},
        ];
        result = hasCoverageData(table);
        expect(result).toBeTruthy();


        // check recognizing columns by meta data
        table.tableData.columns= [
            {name: 'x1'},
            {name: 'y1'},
            {name: 'x2'},
            {name: 'y2'},
            {name: 'x3'},
            {name: 'y3'},
            {name: 'x4'},
            {name: 'y4'},
        ];
        table.tableMeta= {ALL_CORNERS: 'x1;y1;J2000,x2;y2;J2000,x3;y3;J2000,x4;y4;J2000'};
        result = hasCoverageData(table);
        expect(result).toBeTruthy();

        // check recognizing columns mismatching metadata
        table.tableMeta= {ALL_CORNERS: 'a1;b1;J2000,a2;b2;J2000,a3;b3;J2000,a4;b4;J2000'};
        result = hasCoverageData(table);
        expect(result).toBeFalsy();

        // check guessing
        table.tableMeta= {};
        table.tableData.columns= [
            {name: 'ra1'},
            {name: 'dec1'},
            {name: 'ra2'},
            {name: 'dec2'},
            {name: 'ra3'},
            {name: 'dec3'},
            {name: 'ra4'},
            {name: 'dec4'},
        ];
        result = hasCoverageData(table);
        expect(result).toBeTruthy();

        // check guessing
        table.tableMeta= {};
        table.tableData.columns= [
            {name: 'xra1'},
            {name: 'xdec1'},
            {name: 'xra2'},
            {name: 'xdec2'},
            {name: 'xra3'},
            {name: 'xdec3'},
            {name: 'xra4'},
            {name: 'xdec4'},
        ];
        result = hasCoverageData(table);
        expect(result).toBeTruthy();

    });

    /**
     * check the isMetaDataTable works as expected
     */
    test('test isMetaDataTable', () => {
        const table = {
            totalRows: 2,
            tableData: {
                columns: [
                    {name: 'url'},
                    {name: 'desc'},
                ]
            },
            tableMeta: {
                DataSource: 'url'
            }
        };
        let result;
        result= isMetaDataTable(table);
        expect(result).toBeTruthy();


        table.tableMeta= {dAtASouRCE:'url'};
        result= isMetaDataTable(table);
        expect(result).toBeTruthy();



        table.tableMeta= {};
        result= isMetaDataTable(table);
        expect(result).toBeFalsy();

        table.tableMeta= {ImageSourceId:'wise'};
        result= isMetaDataTable(table);
        expect(result).toBeTruthy();

    });

});



