// import initTest from '../InitTest.js';

import {reject} from 'lodash';
import {
    findTableCenterColumns, findTableEqCenterColumns, getWorldPtFromTableRow, hasCoverageData, isCatalog,
    isDataProductsTable, makeWorldPtUsingCenterColumns
} from '../../voAnalyzer/TableAnalysis.js';
import {getRowCenterWorldPt} from '../../visualize/saga/ActiveRowToImageWatcher.js';
import {applyLinkSub} from '../../voAnalyzer/VoCoreUtils.js';
import CoordinateSys from '../../visualize/CoordSys.js';


describe('VOAnalyzer:', () => {

    /**
     * guessing column name; ra/dec and lon/lat are equatorial, glon/glat are galactic, elon/elat are ecliptic,
     * sglon/sglat need a UCD to say which coordinate system
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

        // test lon/lat without a UCD are equatorial, Firefly's long-standing default
        table.tableData.columns = [
            {name: 'lon'},
            {name: 'lat'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('lon');
        expect(actual.latCol).toEqual('lat');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);

        // test sg before lon/lat does not name a coordinate system, sglon/sglat are supergalactic
        table.tableData.columns = [
            {name: 'sglon'},
            {name: 'sglat'},
        ];
        expect(findTableCenterColumns(table)).toBeFalsy();

        // test clon/clat, the ra/dec text columns of IRSA catalog searches, are equatorial
        table.tableData.columns = [
            {name: 'clon', type: 'char'},
            {name: 'clat', type: 'char'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('clon');
        expect(actual.latCol).toEqual('clat');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);

        // test galactic names without a UCD, a g or gal before lon/lat means galactic
        for (const [lonName, latName] of [['GLON', 'GLAT'], ['_Glon', '_Glat'], ['gal_lon', 'gal_lat']]) {
            table.tableData.columns = [
                {name: lonName},
                {name: latName},
            ];
            actual = findTableCenterColumns(table);
            expect(actual.lonCol).toEqual(lonName);
            expect(actual.latCol).toEqual(latName);
            expect(actual.csys).toBe(CoordinateSys.GALACTIC);
        }

        // test ecliptic names without a UCD, an e or ecl before lon/lat means ecliptic
        for (const [lonName, latName] of [['ELON', 'ELAT'], ['ecl_lon', 'ecl_lat']]) {
            table.tableData.columns = [
                {name: lonName},
                {name: latName},
            ];
            actual = findTableCenterColumns(table);
            expect(actual.lonCol).toEqual(lonName);
            expect(actual.latCol).toEqual(latName);
            expect(actual.csys).toBe(CoordinateSys.ECL_J2000);
        }

        // test ra/dec without a UCD are used before glon/glat without a UCD
        table.tableData.columns = [
            {name: 'glon'},
            {name: 'glat'},
            {name: 'ra'},
            {name: 'dec'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);


        // test ra1/dec1
        table.tableData.columns = [
            {name: 'ra1'},
            {name: 'dec1'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual).toBeTruthy();
        expect(actual.lonCol).toEqual('ra1');
        expect(actual.latCol).toEqual('dec1');

        // test ra_obj/dec_obj
        table.tableData.columns = [
            {name: 'ra_obj'},
            {name: 'dec_obj'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual).toBeTruthy();
        expect(actual.lonCol).toEqual('ra_obj');
        expect(actual.latCol).toEqual('dec_obj');


        // test fail
        table.tableData.columns = [
            {name: 'my_ra_unrecognized'},
            {name: 'my_dec_unrecognized'}
        ];
        actual = findTableCenterColumns(table);
        expect(actual).toBeFalsy();

        // test fail
        table.tableData.columns = [
            {name: 'rastuff'},
            {name: 'decstuff'}
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

    /**
     * the coordinate system comes from the UCDs, whatever the columns are named
     */
    test('by UCD, coordinate system', () => {
        const table = { tableData: {}};
        let actual;

        // test galactic
        table.tableData.columns = [
            {name: 'l', UCD: 'pos.galactic.lon'},
            {name: 'b', UCD: 'pos.galactic.lat'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('l');
        expect(actual.latCol).toEqual('b');
        expect(actual.csys).toBe(CoordinateSys.GALACTIC);

        // test ecliptic
        table.tableData.columns = [
            {name: 'lambda', UCD: 'pos.ecliptic.lon'},
            {name: 'beta', UCD: 'pos.ecliptic.lat'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('lambda');
        expect(actual.latCol).toEqual('beta');
        expect(actual.csys).toBe(CoordinateSys.ECL_J2000);

        // test equatorial takes precedence when more than one coordinate system is defined
        table.tableData.columns = [
            {name: 'l', UCD: 'pos.galactic.lon'},
            {name: 'b', UCD: 'pos.galactic.lat'},
            {name: 'ura', UCD: 'pos.eq.ra'},
            {name: 'udec', UCD: 'pos.eq.dec'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ura');
        expect(actual.latCol).toEqual('udec');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);

        // test meta.main with the galactic UCD is still the galactic position
        table.tableData.columns = [
            {name: 'l', UCD: 'pos.galactic.lon;meta.main'},
            {name: 'b', UCD: 'pos.galactic.lat;meta.main'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('l');
        expect(actual.latCol).toEqual('b');
        expect(actual.csys).toBe(CoordinateSys.GALACTIC);

        // test ra/dec followed by an arith word are not the position, a space before the word is ignored
        table.tableData.columns = [
            {name: 'ura', UCD: 'pos.eq.ra; arith.ratio'},
            {name: 'udec', UCD: 'pos.eq.dec; arith.ratio'},
        ];
        expect(findTableCenterColumns(table)).toBeFalsy();

        // test two galactic pairs, the UCDs do not decide and the column names do
        table.tableData.columns = [
            {name: 'GLONp', UCD: 'pos.galactic.lon'},
            {name: 'GLATp', UCD: 'pos.galactic.lat'},
            {name: 'GLON', UCD: 'pos.galactic.lon'},
            {name: 'GLAT', UCD: 'pos.galactic.lat'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('GLON');
        expect(actual.latCol).toEqual('GLAT');
        expect(actual.csys).toBe(CoordinateSys.GALACTIC);

        // test two galactic pairs with names that cannot be guessed, no position is found
        table.tableData.columns = [
            {name: 'l1', UCD: 'pos.galactic.lon'},
            {name: 'b1', UCD: 'pos.galactic.lat'},
            {name: 'l2', UCD: 'pos.galactic.lon'},
            {name: 'b2', UCD: 'pos.galactic.lat'},
        ];
        expect(findTableCenterColumns(table)).toBeFalsy();

        // test a galactic pair and an ecliptic pair, the UCDs do not decide and the name guess takes the pair listed first
        table.tableData.columns = [
            {name: 'elon', UCD: 'pos.ecliptic.lon'},
            {name: 'elat', UCD: 'pos.ecliptic.lat'},
            {name: 'glon', UCD: 'pos.galactic.lon'},
            {name: 'glat', UCD: 'pos.galactic.lat'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('elon');
        expect(actual.latCol).toEqual('elat');
        expect(actual.csys).toBe(CoordinateSys.ECL_J2000);

        // test UType and ObsCore name, without a UCD, are equatorial
        table.tableData.columns = [
            {name: 'tra', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'},
            {name: 'tdec', utype: 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'},
        ];
        expect(findTableCenterColumns(table).csys).toBe(CoordinateSys.EQ_J2000);

        table.tableData.columns = [
            {name: 's_ra'},
            {name: 's_dec'},
        ];
        expect(findTableCenterColumns(table).csys).toBe(CoordinateSys.EQ_J2000);
    });

    /**
     * a column whose UCD describes something other than the position is not used, by UCD or by column name guess.
     * The columns are the ones of the real tables named in each test.
     */
    test('columns described as something else are not the position', () => {
        const table = { tableData: {}};
        let actual;

        // test galactic proper motions with names the column name guess matches (VizieR J/A+A/503/1023/vsc)
        table.tableData.columns = [
            {name: 'pmGlon', UCD: 'pos.pm;pos.galactic.lon', type: 'double'},
            {name: 'pmGlat', UCD: 'pos.pm;pos.galactic.lat', type: 'double'},
        ];
        expect(findTableCenterColumns(table)).toBeFalsy();

        // test a spacecraft position around the Sun is not a sky position (VizieR J/ApJ/923/136/table1)
        table.tableData.columns = [
            {name: 'ID', UCD: 'meta.id;meta.main', type: 'char'},
            {name: 'HAE-r', UCD: 'pos.distance;pos.heliocentric', type: 'double'},
            {name: 'HAE-lat', UCD: 'pos.ecliptic.lat;pos.heliocentric', type: 'double'},
            {name: 'HAE-lon', UCD: 'pos.ecliptic.lon;pos.heliocentric', type: 'double'},
        ];
        expect(findTableCenterColumns(table)).toBeFalsy();

        // test a text ecliptic longitude is not the position (VizieR J/A+A/649/A112/wilhiv without its ra/dec)
        table.tableData.columns = [
            {name: 'Elon', UCD: 'pos.ecliptic.lon', type: 'char'},
            {name: 'Elat', UCD: 'pos.ecliptic.lat', type: 'double'},
        ];
        expect(findTableCenterColumns(table)).toBeFalsy();

        // test equatorial proper motions are not ra/dec, even when their names match the guess
        table.tableData.columns = [
            {name: 'pmra', UCD: 'pos.pm;pos.eq.ra', type: 'double'},
            {name: 'pmdec', UCD: 'pos.pm;pos.eq.dec', type: 'double'},
        ];
        expect(findTableCenterColumns(table)).toBeFalsy();

        // test offsets between grid centers do not hide the galactic position (VizieR II/126/headers)
        table.tableData.columns = [
            {name: 'FNAME', UCD: 'meta.id;obs.field', type: 'char'},
            {name: 'GLON', UCD: 'pos.galactic.lon', type: 'double'},
            {name: 'GLAT', UCD: 'pos.galactic.lat', type: 'int'},
            {name: 'dRA', UCD: 'pos.eq.ra;arith.diff', type: 'int'},
            {name: 'dDE', UCD: 'pos.eq.dec;arith.diff', type: 'int'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('GLON');
        expect(actual.latCol).toEqual('GLAT');
        expect(actual.csys).toBe(CoordinateSys.GALACTIC);

        // test position errors marked meta.main are not the position (VizieR J/AJ/168/92/table1)
        table.tableData.columns = [
            {name: 'RAdeg', UCD: 'pos.eq.ra', type: 'double'},
            {name: 'e_RAdeg', UCD: 'stat.error;pos.eq.ra;meta.main', type: 'double'},
            {name: 'DEdeg', UCD: 'pos.eq.dec', type: 'double'},
            {name: 'e_DEdeg', UCD: 'stat.error;pos.eq.dec;meta.main', type: 'double'},
            {name: '_RA', UCD: 'pos.eq.ra;meta.main', type: 'double'},
            {name: '_DE', UCD: 'pos.eq.dec;meta.main', type: 'double'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('_RA');
        expect(actual.latCol).toEqual('_DE');

        // test ra/dec with other words that still describe the position are used
        // (a mean position, Gaia external.skymapperdr2_master)
        table.tableData.columns = [
            {name: 'raj2000', UCD: 'pos.eq.ra;meta.main;stat.mean', type: 'double'},
            {name: 'dej2000', UCD: 'pos.eq.dec;meta.main;stat.mean', type: 'double'},
        ];
        expect(findTableCenterColumns(table).lonCol).toEqual('raj2000');

        // test orbit residuals named like ra/dec are not ra/dec (IRSA gaia_dr2_sso_orbit_residuals)
        table.tableData.columns = [
            {name: 'source_id', UCD: 'meta.id;meta.main', type: 'long'},
            {name: 'residual_ra', UCD: 'arith.diff;pos.angDistance', type: 'double'},
            {name: 'residual_dec', UCD: 'arith.diff;pos.angDistance', type: 'double'},
        ];
        expect(findTableCenterColumns(table)).toBeFalsy();

        // test ra with its UCD and dec without one are still ra/dec
        table.tableData.columns = [
            {name: 'ra', UCD: 'pos.eq.ra', type: 'double'},
            {name: 'dec', type: 'double'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra');
        expect(actual.latCol).toEqual('dec');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);

        // test the position of an emission peak, tagged stat.max, in a query without ra/dec (VizieR J/ApJS/79/77/table2)
        table.tableData.columns = [
            {name: 'pkGLON', UCD: 'pos.galactic.lon;stat.max', type: 'double'},
            {name: 'pkGLAT', UCD: 'pos.galactic.lat;stat.max', type: 'double'},
            {name: 'maxGLON', UCD: 'pos.galactic.lon;stat.max', type: 'double'},
            {name: 'minGLON', UCD: 'pos.galactic.lon;stat.min', type: 'double'},
            {name: 'maxGLAT', UCD: 'pos.galactic.lat;stat.max', type: 'double'},
            {name: 'minGLAT', UCD: 'pos.galactic.lat;stat.min', type: 'double'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('pkGLON');
        expect(actual.latCol).toEqual('pkGLAT');
        expect(actual.csys).toBe(CoordinateSys.GALACTIC);

        // test ra/dec given only as a range are used, the pair listed first (VizieR B/planets/epn_core)
        table.tableData.columns = [
            {name: 'c1min', UCD: 'pos.eq.ra;stat.min', type: 'double'},
            {name: 'c1max', UCD: 'pos.eq.ra;stat.max', type: 'double'},
            {name: 'c2min', UCD: 'pos.eq.dec;stat.min', type: 'double'},
            {name: 'c2max', UCD: 'pos.eq.dec;stat.max', type: 'double'},
        ];
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('c1min');
        expect(actual.latCol).toEqual('c2min');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);
    });

    /**
     * a UCD is read as the IVOA UCD 1.1 standard writes it: case-insensitive, without spaces, with an optional ivoa:
     * namespace, also in the UCD1 words used before UCD1+, and with meta.main in any place
     */
    test.each([
        ['in UCD1', 'POS_EQ_RA', 'POS_EQ_DEC'],
        ['in UCD1 as another position', 'POS_EQ_RA_OTHER', 'POS_EQ_DEC_OTHER'],
        ['in upper case', 'IVOA:POS.EQ.RA;META.MAIN', 'IVOA:POS.EQ.DEC;META.MAIN'],
        ['blank', ' ', ' '],
        ['with only meta words (GAVO rome.time_series)', 'meta.id;meta.main', 'meta.id;meta.main'],
    ])('by column name guess, ra/dec with a UCD written %s', (description, raUCD, decUCD) => {
        const table = {tableData: {columns: [
            {name: 'ra', UCD: raUCD, type: 'double'},
            {name: 'dec', UCD: decUCD, type: 'double'},
        ]}};
        const actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra');
        expect(actual.latCol).toEqual('dec');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);
    });

    test.each([
        ['with meta.main first', 'meta.main;pos.eq.ra', 'meta.main;pos.eq.dec'],
        ['in UCD1', 'POS_EQ_RA_MAIN', 'POS_EQ_DEC_MAIN'],
    ])('by UCD, columns the name guess cannot read, with a UCD written %s', (description, raUCD, decUCD) => {
        const table = {tableData: {columns: [
            {name: 'RAJ2000', UCD: raUCD, type: 'double'},
            {name: 'DEJ2000', UCD: decUCD, type: 'double'},
        ]}};
        const actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('RAJ2000');
        expect(actual.latCol).toEqual('DEJ2000');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);
    });

    /**
     * a UCD word after the coordinate word that keeps the column the position
     */
    test.each([
        ['a mean', 'glon', 'glat', 'pos.galactic.lon;stat.mean', 'pos.galactic.lat;stat.mean', CoordinateSys.GALACTIC],
        ['an ecliptic mean', 'elon', 'elat', 'pos.ecliptic.lon;stat.mean', 'pos.ecliptic.lat;stat.mean',
            CoordinateSys.ECL_J2000],
    ])('by column name guess, %s is the position', (description, lonName, latName, lonUCD, latUCD, csys) => {
        const table = {tableData: {columns: [
            {name: lonName, UCD: lonUCD, type: 'double'},
            {name: latName, UCD: latUCD, type: 'double'},
        ]}};
        const actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual(lonName);
        expect(actual.latCol).toEqual(latName);
        expect(actual.csys).toBe(csys);
    });

    /**
     * a UCD word after the coordinate word that makes the column a different quantity
     */
    test.each([
        ['an error', 'e_GLON', 'e_GLAT', 'pos.galactic.lon;stat.error', 'pos.galactic.lat;stat.error'],
        ['an orbit', 'glon', 'glat', 'pos.galactic.lon;src.orbital', 'pos.galactic.lat;src.orbital'],
        ['a ratio (VizieR VIII/7A/catalog)', 'mGLON', 'mGLAT', 'pos.galactic.lon;arith.ratio', 'pos.galactic.lat;arith.ratio'],
        ['a proper motion', 'ra', 'dec', 'pos.eq.ra;pos.pm', 'pos.eq.dec;pos.pm'],
        ['a UCD1 offset', 'ra', 'dec', 'POS_EQ_RA_OFF', 'POS_EQ_DEC_OFF'],
    ])('by column name guess, %s is not the position', (description, lonName, latName, lonUCD, latUCD) => {
        const table = {tableData: {columns: [
            {name: lonName, UCD: lonUCD, type: 'double'},
            {name: latName, UCD: latUCD, type: 'double'},
        ]}};
        expect(findTableCenterColumns(table)).toBeFalsy();
    });

    /**
     * for callers that use the column values as ra/dec: the equatorial J2000 columns or nothing
     */
    test('equatorial center columns only', () => {
        const table = { tableData: {}};
        let actual;

        // test equatorial columns are returned
        table.tableData.columns = [
            {name: 'ura', UCD: 'pos.eq.ra'},
            {name: 'udec', UCD: 'pos.eq.dec'},
        ];
        actual = findTableEqCenterColumns(table);
        expect(actual.lonCol).toEqual('ura');
        expect(actual.latCol).toEqual('udec');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);

        // test ra/dec without a UCD are returned when the table also has galactic UCDs
        table.tableData.columns = [
            {name: 'ra'},
            {name: 'dec'},
            {name: 'glon', UCD: 'pos.galactic.lon'},
            {name: 'glat', UCD: 'pos.galactic.lat'},
        ];
        expect(findTableCenterColumns(table).lonCol).toEqual('glon');
        actual = findTableEqCenterColumns(table);
        expect(actual.lonCol).toEqual('ra');
        expect(actual.latCol).toEqual('dec');
        expect(actual.csys).toBe(CoordinateSys.EQ_J2000);

        // test meta.main on ra but not on dec, ra and dec still pair up when they are the only ra and the only dec
        table.tableData.columns = [
            {name: 'ra', UCD: 'pos.eq.ra;meta.main'},
            {name: 'dec', UCD: 'pos.eq.dec'},
            {name: 'l', UCD: 'pos.galactic.lon'},
            {name: 'b', UCD: 'pos.galactic.lat'},
        ];
        expect(findTableCenterColumns(table).lonCol).toEqual('ra');

        // test galactic columns without a UCD are not returned
        table.tableData.columns = [
            {name: 'glon'},
            {name: 'glat'},
        ];
        expect(findTableEqCenterColumns(table)).toBeFalsy();

        // test galactic columns defined in tableMeta are not returned
        table.tableData.columns = [
            {name: 'l'},
            {name: 'b'},
        ];
        table.tableMeta = {CENTER_COLUMN: 'l;b;GALACTIC'};
        expect(findTableCenterColumns(table).csys).toBe(CoordinateSys.GALACTIC);
        expect(findTableEqCenterColumns(table)).toBeFalsy();
    });

    /**
     * the position of a row is handed out in J2000, whatever coordinate system the table columns are in.
     * The expected ra/dec values come from astropy.
     */
    test('row position is J2000', () => {
        const makeTable = (columns, row) => ({tableData: {columns, data: [row]}, highlightedRow: 0});
        const expectJ2000 = (wp, ra, dec) => {
            expect(wp.cSys).toBe(CoordinateSys.EQ_J2000);
            expect(wp.x).toBeCloseTo(ra, 3);
            expect(wp.y).toBeCloseTo(dec, 3);
        };

        // test equatorial is unchanged
        const eqTable = makeTable([{name: 'ura', UCD: 'pos.eq.ra'}, {name: 'udec', UCD: 'pos.eq.dec'}],
            [59.61251, 11.86193]);
        expectJ2000(makeWorldPtUsingCenterColumns(eqTable, 0), 59.61251, 11.86193);
        expectJ2000(getWorldPtFromTableRow(eqTable), 59.61251, 11.86193);

        // test galactic
        const galCols = [{name: 'l', UCD: 'pos.galactic.lon'}, {name: 'b', UCD: 'pos.galactic.lat'}];
        const galTable = makeTable(galCols, [178.5, -30.2]);
        expectJ2000(makeWorldPtUsingCenterColumns(galTable, 0), 59.61251, 11.86193);
        expectJ2000(getWorldPtFromTableRow(galTable), 59.61251, 11.86193);
        expectJ2000(getRowCenterWorldPt(galTable), 59.61251, 11.86193);

        // test ecliptic
        const eclCols = [{name: 'lambda', UCD: 'pos.ecliptic.lon'}, {name: 'beta', UCD: 'pos.ecliptic.lat'}];
        const eclTable = makeTable(eclCols, [100, 20]);
        expectJ2000(makeWorldPtUsingCenterColumns(eclTable, 0), 102.89052, 42.99283);
        expectJ2000(getWorldPtFromTableRow(eclTable), 102.89052, 42.99283);

        // test galactic columns in radians: getWorldPtFromTableRow converts the cells to degrees, then to J2000
        const galRadianCols = [
            {name: 'l', UCD: 'pos.galactic.lon', units: 'rad'}, {name: 'b', UCD: 'pos.galactic.lat', units: 'rad'}];
        const radianTable = makeTable(galRadianCols, [178.5 * Math.PI / 180, -30.2 * Math.PI / 180]);
        expectJ2000(getWorldPtFromTableRow(radianTable), 59.61251, 11.86193);
    });

    /**
     * a row with empty position cells has no position. Number() reads null and '' as 0, and 0,0 on a galactic table is
     * a real place on the sky, so a row reader that used it would offer searches there.
     */
    test('row with empty position cells has no position', () => {
        const galCols = [{name: 'l', UCD: 'pos.galactic.lon'}, {name: 'b', UCD: 'pos.galactic.lat'}];

        // test null cells
        const nullTable = {tableData: {columns: galCols, data: [[null, null]]}, highlightedRow: 0};
        expect(makeWorldPtUsingCenterColumns(nullTable, 0)).toBeUndefined();
        expect(getWorldPtFromTableRow(nullTable)).toBeUndefined();

        // test empty string cells
        const emptyStringTable = {tableData: {columns: galCols, data: [['', '']]}, highlightedRow: 0};
        expect(makeWorldPtUsingCenterColumns(emptyStringTable, 0)).toBeUndefined();
        expect(getWorldPtFromTableRow(emptyStringTable)).toBeUndefined();

        // test pixel columns: an image point is made from any numbers, so the row readers check for missing ones first
        const pixelTable = {
            tableData: {columns: [{name: 'x'}, {name: 'y'}], data: [[null, null]]}, highlightedRow: 0,
            tableMeta: {CENTER_COLUMN: 'x;y;PIXEL'}
        };
        expect(getWorldPtFromTableRow(pixelTable)).toBeUndefined();
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

        // ra/dec column name patterns next
        actual = findTableCenterColumns(table);
        expect(actual.lonCol).toEqual('ra1');
        expect(actual.latCol).toEqual('dec1');

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
    test('isCatalog', () => {
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


        table.tableMeta= {CatalogOverlayType: 'true'};
        table.tableData.columns= [  // test that the matching columns match each other
            {name: 'xra1'},
            {name: 'xdec2'},
        ];
        result = isCatalog(table);
        expect(result).toBeFalsy();

        table.tableData.columns= [  // test that the columns that should match
            {name: 'coord_ra1'},
            {name: 'coord_dec1'},
        ];
        result = isCatalog(table);
        expect(result).toBeTruthy();

        table.tableData.columns= [  // test that the columns that should match
            {name: 'coordra'},
            {name: 'coorddec'},
        ];
        result = isCatalog(table);
        expect(result).toBeTruthy();

        table.tableData.columns= [  // test that the columns that should fail
            {name: 'coord_ra1a'},
            {name: 'coord_dec1a'},
        ];
        result = isCatalog(table);
        expect(result).toBeFalsy();



    });

    /**
     * check the hasCoverageData works as expected
     */
    test('hasCoverageData', () => {
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
    test('isMetaDataTable', () => {
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
        result= isDataProductsTable(table);
        expect(result).toBeTruthy();


        table.tableMeta= {dAtASouRCE:'url'};
        result= isDataProductsTable(table);
        expect(result).toBeTruthy();



        table.tableMeta= {};
        result= isDataProductsTable(table);
        expect(result).toBeTruthy();

        table.tableMeta= {ImageSourceId:'wise'};
        result= isDataProductsTable(table);
        expect(result).toBeTruthy();

    });

    /**
     * guessing column name; ra/dec or lon/lat
     */
    test('applyLinkSub', () => {

        const tableModel = {
                tableData: {
                    columns: [ {name: 'a'}, {name: 'b'}, {name: 'c'}],
                    data: [
                        ['a-1', 'b-1', 'b-1'],
                        ['a-2', 'b-2', 'c-2'],
                        ['a-3', '', '1984A&AS...56..381B'],
                    ],
                }
            };

        // no substitution, simply append value to end of href
        let result = applyLinkSub(tableModel, 'https://acme.org/abc?p=', 1, 'b-2');
        expect(result).toBe('https://acme.org/abc?p=b-2');

        // substituting values from column a and c into the href
        result = applyLinkSub(tableModel, 'https://acme.org/abc?x=${a}&y=${c}', 1, 'b-2');
        expect(result).toBe('https://acme.org/abc?x=a-2&y=c-2');

        // null condition
        result = applyLinkSub(tableModel, '${b}', 2, '');
        expect(result).toBe('');


    });

});



