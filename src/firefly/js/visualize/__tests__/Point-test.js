import Point, {
    SimplePt, isValidPoint, makeAnyPt, makeDevicePt, makeFitsImagePt, makeImagePt, makeImageWorkSpacePt,
    makeOffsetPt, makeProjectionPt, makeResolvedWorldPt, makeScreenPt, makeWorldPt, makeZeroBasedImagePt,
    parseAnyPt, parseImagePt, parseImageWorkSpacePt, parsePt, parseScreenPt, parseWorldPt, pointEquals
} from '../Point.js';
import CoordinateSys from '../CoordSys.js';
import Resolver from '../../astro/net/Resolver.js';


describe('Test Point.SimplePt', () => {

    test('SimplePt constructor with (x, y)', () => {
        const pt= new SimplePt(1.5, 2.5);
        expect(pt.x).toEqual(1.5);
        expect(pt.y).toEqual(2.5);
    });

    test('SimplePt constructor with (x, y) as strings', () => {
        const pt= new SimplePt('1.5', '2.5');
        expect(pt.x).toEqual(1.5);
        expect(pt.y).toEqual(2.5);
    });

    test('SimplePt constructor with missing or empty values gives NaN', () => {
        expect(new SimplePt(undefined, null).x).toBeNaN();
        expect(new SimplePt(undefined, null).y).toBeNaN();
        expect(new SimplePt('', '').x).toBeNaN();
        expect(new SimplePt('', '').y).toBeNaN();
    });

    test('SimplePt constructor with (x, y, forceToInt)', () => {
        const pt= new SimplePt(1.9, -2.9, true);
        expect(pt.x).toEqual(1);
        expect(pt.y).toEqual(-2);
    });

    test('SimplePt constructor with ([x, y]) and ([x, y], forceToInt)', () => {
        const pt= new SimplePt([3.7, 4.2]);
        expect(pt.x).toEqual(3.7);
        expect(pt.y).toEqual(4.2);

        const intPt= new SimplePt([3.7, 4.2], true);
        expect(intPt.x).toEqual(3);
        expect(intPt.y).toEqual(4);
    });

    test('SimplePt constructor with ({x, y}) and ({x, y}, forceToInt)', () => {
        const pt= new SimplePt({x: '5.5', y: 6.5});
        expect(pt.x).toEqual(5.5);
        expect(pt.y).toEqual(6.5);

        const intPt= new SimplePt({x: 5.5, y: 6.5}, true);
        expect(intPt.x).toEqual(5);
        expect(intPt.y).toEqual(6);
    });

    test('SimplePt toString()', () => {
        expect(new SimplePt(1, 2).toString()).toEqual('1;2');
    });

    test('SimplePt.make() sets x, y and ignores an unknown type', () => {
        const pt= SimplePt.make(1, 2, 'NotAType');
        expect(pt.x).toEqual(1);
        expect(pt.y).toEqual(2);
        expect(pt.type).toBeUndefined();
    });

    test('SimplePt.make() sets a known point type', () => {
        const pt= SimplePt.make(1, 2, Point.SPT);
        expect(pt.x).toEqual(1);
        expect(pt.y).toEqual(2);
        expect(pt.type).toEqual(Point.SPT);
    });
});


describe('Test Point simple point factory functions', () => {

    test('make*Pt() functions set x, y and type', () => {
        const factoryToType= [
            [makeImagePt, Point.IM_PT],
            [makeFitsImagePt, Point.FITS_IM_PT],
            [makeZeroBasedImagePt, Point.ZERO_BASED_IM_PT],
            [makeImageWorkSpacePt, Point.IM_WS_PT],
            [makeScreenPt, Point.SPT],
            [makeDevicePt, Point.DEV_PT],
            [makeProjectionPt, Point.PROJ_PT],
            [makeOffsetPt, Point.OFFSET_PT],
        ];
        factoryToType.forEach(([makeFunc, expectedType]) => {
            const pt= makeFunc(10, '20');
            expect(pt.x).toEqual(10);
            expect(pt.y).toEqual(20);
            expect(pt.type).toEqual(expectedType);
            expect(pt).toBeInstanceOf(SimplePt);
        });
    });
});


describe('Test Point.makeWorldPt()', () => {

    test('makeWorldPt() using parameters (lon, lat) defaults to EQ_J2000', () => {
        const wp= makeWorldPt(10.5, -20.25);
        expect(wp.x).toEqual(10.5);
        expect(wp.y).toEqual(-20.25);
        expect(wp.type).toEqual(Point.W_PT);
        expect(wp.cSys).toBe(CoordinateSys.EQ_J2000);
        expect(wp.getLon()).toEqual(10.5);
        expect(wp.getLat()).toEqual(-20.25);
        expect(wp.getCoordSys()).toBe(CoordinateSys.EQ_J2000);
        expect(wp.getObjName()).toBeUndefined();
        expect(wp.getResolver()).toBeUndefined();
        expect(wp.getObjType()).toEqual('');
    });

    test('makeWorldPt() using parameters (lon, lat, coordSys) with string numbers', () => {
        const wp= makeWorldPt('120', '-5', CoordinateSys.GALACTIC);
        expect(wp.x).toEqual(120);
        expect(wp.y).toEqual(-5);
        expect(wp.cSys).toBe(CoordinateSys.GALACTIC);
    });

    test('makeWorldPt() with non-numeric or missing values returns undefined', () => {
        expect(makeWorldPt('abc', 10)).toBeUndefined();
        expect(makeWorldPt(10, undefined)).toBeUndefined();
        expect(makeWorldPt('', '')).toBeUndefined();
    });

    test('makeWorldPt() with sexagesimal strings requires detectHMS', () => {
        expect(makeWorldPt('10:00:00', '+30:00:00', CoordinateSys.EQ_J2000)).toBeUndefined();

        const wp= makeWorldPt('10:00:00', '+30:00:00', CoordinateSys.EQ_J2000, true);
        expect(wp.x).toBeCloseTo(150, 8);
        expect(wp.y).toBeCloseTo(30, 8);
    });

    test('makeWorldPt() with sexagesimal strings and a non-equatorial system returns undefined', () => {
        expect(makeWorldPt('10:00:00', '+30:00:00', CoordinateSys.GALACTIC, true)).toBeUndefined();
    });

    test('makeWorldPt() with angleInRadian converts to degrees', () => {
        const wp= makeWorldPt(Math.PI, Math.PI/4, CoordinateSys.EQ_J2000, false, true);
        expect(wp.x).toBeCloseTo(180, 10);
        expect(wp.y).toBeCloseTo(45, 10);
    });

    test('makeWorldPt() returns the same object for repeated identical calls', () => {
        const wp1= makeWorldPt(33, 44);
        const wp2= makeWorldPt(33, 44);
        expect(wp2).toBe(wp1);
    });

    test('WorldPt toString()', () => {
        expect(makeWorldPt(10, 20).toString()).toEqual('10;20;EQ_J2000');
        expect(makeWorldPt(10, 20, CoordinateSys.GALACTIC).toString()).toEqual('10;20;GALACTIC');
    });
});


describe('Test Point.makeResolvedWorldPt()', () => {

    test('makeResolvedWorldPt() using parameters (wp, objName, resolver, objType)', () => {
        const wp= makeWorldPt(83.63, 22.01);
        const resolvedWp= makeResolvedWorldPt(wp, 'm1', Resolver.NED, 'SNR');
        expect(resolvedWp).not.toBe(wp);
        expect(resolvedWp.x).toEqual(83.63);
        expect(resolvedWp.y).toEqual(22.01);
        expect(resolvedWp.cSys).toBe(CoordinateSys.EQ_J2000);
        expect(resolvedWp.getObjName()).toEqual('m1');
        expect(resolvedWp.getResolver()).toBe(Resolver.NED);
        expect(resolvedWp.getObjType()).toEqual('SNR');
        expect(resolvedWp.toString()).toEqual('83.63;22.01;EQ_J2000;m1;ned;SNR');
    });

    test('makeResolvedWorldPt() using parameters (wp, objName) defaults to UNKNOWN resolver', () => {
        const resolvedWp= makeResolvedWorldPt(makeWorldPt(1, 2), 'm31');
        expect(resolvedWp.getResolver()).toBe(Resolver.UNKNOWN);
        expect(resolvedWp.toString()).toEqual('1;2;EQ_J2000;m31;unknown');
    });
});


describe('Test Point.makeAnyPt()', () => {

    test('makeAnyPt() with non-celestial CoordinateSys objects', () => {
        expect(makeAnyPt(1, 2, CoordinateSys.SCREEN_PIXEL).type).toEqual(Point.SPT);
        expect(makeAnyPt(1, 2, CoordinateSys.UNDEFINED).type).toEqual(Point.SPT);
        expect(makeAnyPt(1, 2, CoordinateSys.PIXEL).type).toEqual(Point.IM_PT);
        expect(makeAnyPt(1, 2, CoordinateSys.ZEROBASED).type).toEqual(Point.ZERO_BASED_IM_PT);
        expect(makeAnyPt(1, 2, CoordinateSys.FITSPIXEL).type).toEqual(Point.FITS_IM_PT);
    });

    test('makeAnyPt() with non-celestial coordinate system strings', () => {
        expect(makeAnyPt(1, 2, 'SCREEN_PIXEL').type).toEqual(Point.SPT);
        expect(makeAnyPt(1, 2, 'PIXEL').type).toEqual(Point.IM_PT);
        expect(makeAnyPt(1, 2, 'ZERO-BASED').type).toEqual(Point.ZERO_BASED_IM_PT);
        expect(makeAnyPt(1, 2, 'FITSPIXEL').type).toEqual(Point.FITS_IM_PT);
    });

    test('makeAnyPt() with a celestial CoordinateSys makes a WorldPt', () => {
        const wp= makeAnyPt(10, 20, CoordinateSys.GALACTIC);
        expect(wp.type).toEqual(Point.W_PT);
        expect(wp.cSys).toBe(CoordinateSys.GALACTIC);
        expect(wp.x).toEqual(10);
        expect(wp.y).toEqual(20);
    });

    test('makeAnyPt() with a celestial coordinate system string makes a WorldPt', () => {
        const wp= makeAnyPt(10, 20, 'GALACTIC');
        expect(wp.type).toEqual(Point.W_PT);
        expect(wp.cSys).toBe(CoordinateSys.GALACTIC);

        const hmsWp= makeAnyPt('10:00:00', '+30:00:00', 'EQ_J2000');
        expect(hmsWp.cSys).toBe(CoordinateSys.EQ_J2000);
        expect(hmsWp.x).toBeCloseTo(150, 8);
        expect(hmsWp.y).toBeCloseTo(30, 8);
    });

    test('makeAnyPt() with a celestial CoordinateSys detects sexagesimal', () => {
        const wp= makeAnyPt('10:00:00', '+30:00:00', CoordinateSys.EQ_J2000);
        expect(wp.x).toBeCloseTo(150, 8);
        expect(wp.y).toBeCloseTo(30, 8);
    });
});


describe('Test Point.parseWorldPt()', () => {

    test('parseWorldPt() using a (lon;lat;csys) string', () => {
        const wp= parseWorldPt('10.5;-20.5;GALACTIC');
        expect(wp.type).toEqual(Point.W_PT);
        expect(wp.x).toEqual(10.5);
        expect(wp.y).toEqual(-20.5);
        expect(wp.cSys).toBe(CoordinateSys.GALACTIC);
    });

    test('parseWorldPt() using a (lon;lat;csys;objName;resolver;objType) string', () => {
        const wp= parseWorldPt('83.63;22.01;EQ_J2000;m1;simbad;SNR');
        expect(wp.x).toEqual(83.63);
        expect(wp.y).toEqual(22.01);
        expect(wp.cSys).toBe(CoordinateSys.EQ_J2000);
        expect(wp.getObjName()).toEqual('m1');
        expect(wp.getResolver()).toBe(Resolver.Simbad);
        expect(wp.getObjType()).toEqual('SNR');
    });

    test('parseWorldPt() with an object name and no resolver uses UNKNOWN', () => {
        const wp= parseWorldPt('1;2;EQ_J2000;m31');
        expect(wp.getObjName()).toEqual('m31');
        expect(wp.getResolver()).toBe(Resolver.UNKNOWN);
    });

    test('parseWorldPt() round trips WorldPt toString()', () => {
        const wp= makeResolvedWorldPt(makeWorldPt(83.63, 22.01), 'm1', Resolver.NED, 'SNR');
        const parsedWp= parseWorldPt(wp.toString());
        expect(pointEquals(parsedWp, wp)).toBe(true);
        expect(parsedWp.getObjName()).toEqual('m1');
        expect(parsedWp.getResolver()).toBe(Resolver.NED);
        expect(parsedWp.getObjType()).toEqual('SNR');
    });

    test('parseWorldPt() with PIXEL or SCREEN_PIXEL returns an image or screen point', () => {
        expect(parseWorldPt('1;2;PIXEL').type).toEqual(Point.IM_PT);
        expect(parseWorldPt('1;2;SCREEN_PIXEL').type).toEqual(Point.SPT);
    });

    test('parseWorldPt() with a valid point object returns that object', () => {
        const wp= makeWorldPt(1, 2);
        expect(parseWorldPt(wp)).toBe(wp);
    });

    test('parseWorldPt() with invalid input returns undefined', () => {
        expect(parseWorldPt(undefined)).toBeUndefined();
        expect(parseWorldPt('')).toBeUndefined();
        expect(parseWorldPt(42)).toBeUndefined();
        expect(parseWorldPt('10')).toBeUndefined();
        expect(parseWorldPt('a;b;EQ_J2000')).toBeUndefined();
        expect(parseWorldPt('1;2;3;4;5;6;7')).toBeUndefined();
    });
});


describe('Test Point.parseAnyPt()', () => {

    test('parseAnyPt() with a celestial system returns a WorldPt', () => {
        const wp= parseAnyPt('10;20;EQ_B1950');
        expect(wp.type).toEqual(Point.W_PT);
        expect(wp.cSys).toBe(CoordinateSys.EQ_B1950);
    });

    test('parseAnyPt() with non-celestial systems returns the matching point type', () => {
        expect(parseAnyPt('1;2;PIXEL').type).toEqual(Point.IM_PT);
        expect(parseAnyPt('1;2;SCREEN_PIXEL').type).toEqual(Point.SPT);
    });

    test('parseAnyPt() with invalid input returns undefined', () => {
        expect(parseAnyPt('not a point')).toBeUndefined();
        expect(parseAnyPt(undefined)).toBeUndefined();
    });
});


describe('Test Point.parsePt() and typed parsers', () => {

    test('parsePt() using parameters (type, inStr)', () => {
        const pt= parsePt(Point.DEV_PT, '3.5;4.5');
        expect(pt.x).toEqual(3.5);
        expect(pt.y).toEqual(4.5);
        expect(pt.type).toEqual(Point.DEV_PT);
    });

    test('parsePt() with invalid input returns undefined', () => {
        expect(parsePt(Point.IM_PT, undefined)).toBeUndefined();
        expect(parsePt(Point.IM_PT, '')).toBeUndefined();
        expect(parsePt(Point.IM_PT, '1')).toBeUndefined();
        expect(parsePt(Point.IM_PT, '1;2;3')).toBeUndefined();
        expect(parsePt(Point.IM_PT, 'a;2')).toBeUndefined();
    });

    test('parseImagePt(), parseImageWorkSpacePt(), parseScreenPt() set the type', () => {
        expect(parseImagePt('1;2').type).toEqual(Point.IM_PT);
        expect(parseImageWorkSpacePt('1;2').type).toEqual(Point.IM_WS_PT);
        expect(parseScreenPt('1;2').type).toEqual(Point.SPT);
    });

    test('parseImagePt() round trips SimplePt toString()', () => {
        const pt= makeImagePt(7.25, 8.75);
        expect(pointEquals(parseImagePt(pt.toString()), pt)).toBe(true);
    });
});


describe('Test Point.pointEquals()', () => {

    test('pointEquals() with nil points', () => {
        expect(pointEquals(undefined, null)).toBe(true);
        expect(pointEquals(makeImagePt(1, 2), undefined)).toBe(false);
        expect(pointEquals(null, makeImagePt(1, 2))).toBe(false);
    });

    test('pointEquals() compares x, y and type', () => {
        expect(pointEquals(makeImagePt(1, 2), makeImagePt(1, 2))).toBe(true);
        expect(pointEquals(makeImagePt(1, 2), makeImagePt(1, 3))).toBe(false);
        expect(pointEquals(makeImagePt(1, 2), makeScreenPt(1, 2))).toBe(false);
    });

    test('pointEquals() compares the coordinate system of world points', () => {
        expect(pointEquals(makeWorldPt(1, 2, CoordinateSys.EQ_J2000), makeWorldPt(1, 2, CoordinateSys.GALACTIC)))
            .toBe(false);
    });

    test('pointEquals() ignores object name and resolver of world points', () => {
        const wp= makeWorldPt(1, 2);
        const resolvedWp= makeResolvedWorldPt(wp, 'm31', Resolver.NED);
        expect(pointEquals(wp, resolvedWp)).toBe(true);
    });
});


describe('Test Point.isValidPoint()', () => {

    test('isValidPoint() with valid points', () => {
        expect(isValidPoint(makeImagePt(1, 2))).toBe(true);
        expect(isValidPoint(makeWorldPt(1, 2))).toBe(true);
        expect(isValidPoint({x: 1, y: 2, type: Point.SPT})).toBe(true);
    });

    test('isValidPoint() with invalid values', () => {
        expect(isValidPoint(undefined)).toBe(false);
        expect(isValidPoint('1;2;EQ_J2000')).toBe(false);
        expect(isValidPoint({x: 1, y: 2})).toBe(false);
        expect(isValidPoint({x: 1, y: 2, type: 'NotAType'})).toBe(false);
        expect(isValidPoint({x: '1', y: 2, type: Point.IM_PT})).toBe(false);
    });
});
