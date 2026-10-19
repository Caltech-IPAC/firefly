import CoordinateSys, {
    ECLIPTIC_B, ECLIPTIC_J, EQUATORIAL_B, EQUATORIAL_J, GALACTIC_JSYS, NONCELESTIAL, SUPERGALACTIC_JSYS,
    findCoordSys, findValidCoordSysStr
} from '../CoordSys.js';


describe('Test CoordinateSys constants', () => {

    test('celestial coordinate systems', () => {
        const expectedProps= [
            // [coordSys, toString, isEquatorial, jsys, equinox]
            [CoordinateSys.EQ_J2000, 'EQ_J2000', true, EQUATORIAL_J, 2000],
            [CoordinateSys.EQ_B2000, 'EQ_B2000', true, EQUATORIAL_B, 2000],
            [CoordinateSys.EQ_B1950, 'EQ_B1950', true, EQUATORIAL_B, 1950],
            [CoordinateSys.GALACTIC, 'GALACTIC', false, GALACTIC_JSYS, 2000],
            [CoordinateSys.SUPERGALACTIC, 'SUPERGALACTIC', false, SUPERGALACTIC_JSYS, 2000],
            [CoordinateSys.ECL_J2000, 'EC_J2000', false, ECLIPTIC_J, 2000],
            [CoordinateSys.ECL_B1950, 'EC_B1950', false, ECLIPTIC_B, 1950],
        ];
        expectedProps.forEach(([coordSys, desc, isEquatorial, jsys, equinox]) => {
            expect(coordSys.toString()).toEqual(desc);
            expect(coordSys.isEquatorial()).toBe(isEquatorial);
            expect(coordSys.isCelestial()).toBe(true);
            expect(coordSys.getJsys()).toEqual(jsys);
            expect(coordSys.getEquinox()).toEqual(equinox);
        });
    });

    test('non-celestial coordinate systems', () => {
        const expectedDesc= [
            [CoordinateSys.PIXEL, 'PIXEL'],
            [CoordinateSys.SCREEN_PIXEL, 'SCREEN_PIXEL'],
            [CoordinateSys.ZEROBASED, 'ZERO-BASED'],
            [CoordinateSys.FITSPIXEL, 'FITSPIXEL'],
            [CoordinateSys.UNDEFINED, 'UNDEFINED'],
        ];
        expectedDesc.forEach(([coordSys, desc]) => {
            expect(coordSys.toString()).toEqual(desc);
            expect(coordSys.isEquatorial()).toBe(false);
            expect(coordSys.isCelestial()).toBe(false);
            expect(coordSys.getJsys()).toEqual(NONCELESTIAL);
            expect(coordSys.getEquinox()).toEqual(0);
        });
    });
});


describe('Test CoordinateSys.parse()', () => {

    test('parse() using the toString() of each coordinate system', () => {
        const parseableCoordSysAry= [
            CoordinateSys.EQ_J2000, CoordinateSys.EQ_B2000, CoordinateSys.EQ_B1950, CoordinateSys.GALACTIC,
            CoordinateSys.SUPERGALACTIC, CoordinateSys.ECL_J2000, CoordinateSys.ECL_B1950, CoordinateSys.PIXEL,
            CoordinateSys.SCREEN_PIXEL, CoordinateSys.ZEROBASED, CoordinateSys.FITSPIXEL, CoordinateSys.UNDEFINED,
        ];
        parseableCoordSysAry.forEach((coordSys) => expect(CoordinateSys.parse(coordSys.toString())).toBe(coordSys));
    });

    test('parse() using equatorial aliases', () => {
        ['EQJ2000', 'EQJ', 'J2000', 'J', 'ICRS', 'eq', 'equatorial', 'equatorial j2000', 'equ j', 'eq_j2000']
            .forEach((desc) => expect(CoordinateSys.parse(desc)).toBe(CoordinateSys.EQ_J2000));
        ['EQB1950', 'EQB', 'B1950', 'b', 'equatorial b1950', 'eq_b1950']
            .forEach((desc) => expect(CoordinateSys.parse(desc)).toBe(CoordinateSys.EQ_B1950));
        expect(CoordinateSys.parse('EQB2000')).toBe(CoordinateSys.EQ_B2000);
    });

    test('parse() using galactic aliases', () => {
        ['GAL', 'gal', 'galactic', 'G', ' galactic ']
            .forEach((desc) => expect(CoordinateSys.parse(desc)).toBe(CoordinateSys.GALACTIC));
    });

    test('parse() using ecliptic aliases', () => {
        ['ECJ2000', 'ECLJ2000', 'ECJ', 'ecliptic', 'ecl', 'ecl j2000', 'ECL_J2000']
            .forEach((desc) => expect(CoordinateSys.parse(desc)).toBe(CoordinateSys.ECL_J2000));
        ['ECLB1950', 'ECB', 'ecl b1950', 'ecliptic b']
            .forEach((desc) => expect(CoordinateSys.parse(desc)).toBe(CoordinateSys.ECL_B1950));
    });

    test('parse() ignores case and surrounding spaces', () => {
        expect(CoordinateSys.parse('icrs')).toBe(CoordinateSys.EQ_J2000);
        expect(CoordinateSys.parse('eq_b2000')).toBe(CoordinateSys.EQ_B2000);
        expect(CoordinateSys.parse('SuperGalactic')).toBe(CoordinateSys.SUPERGALACTIC);
        expect(CoordinateSys.parse('pixel')).toBe(CoordinateSys.PIXEL);
        expect(CoordinateSys.parse('zero-based')).toBe(CoordinateSys.ZEROBASED);
        expect(CoordinateSys.parse(' fitspixel ')).toBe(CoordinateSys.FITSPIXEL);
        expect(CoordinateSys.parse('screen_pixel')).toBe(CoordinateSys.SCREEN_PIXEL);
    });

    test('parse() using ZERO_BASED alias', () => {
        expect(CoordinateSys.parse('ZERO_BASED')).toBe(CoordinateSys.ZEROBASED);
    });

    test('parse() with unrecognized or empty input returns undefined', () => {
        expect(CoordinateSys.parse(undefined)).toBeUndefined();
        expect(CoordinateSys.parse('')).toBeUndefined();
        expect(CoordinateSys.parse('nonsense')).toBeUndefined();
        expect(CoordinateSys.parse('equatorial x2000')).toBeUndefined();
    });

    test('parse() using UNDEFINED returns the UNDEFINED coordinate system', () => {
        expect(CoordinateSys.parse('UNDEFINED')).toBe(CoordinateSys.UNDEFINED);
        expect(CoordinateSys.parse('undefined')).toBe(CoordinateSys.UNDEFINED);
    });
});


describe('Test CoordSys.findCoordSys()', () => {

    test('findCoordSys() using parameters (jsys, equinox)', () => {
        expect(findCoordSys(EQUATORIAL_J, 2000)).toBe(CoordinateSys.EQ_J2000);
        expect(findCoordSys(EQUATORIAL_B, 2000)).toBe(CoordinateSys.EQ_B2000);
        expect(findCoordSys(EQUATORIAL_B, 1950)).toBe(CoordinateSys.EQ_B1950);
        expect(findCoordSys(ECLIPTIC_J, 2000)).toBe(CoordinateSys.ECL_J2000);
        expect(findCoordSys(ECLIPTIC_B, 1950)).toBe(CoordinateSys.ECL_B1950);
    });

    test('findCoordSys() galactic and supergalactic ignore equinox', () => {
        expect(findCoordSys(GALACTIC_JSYS, 2000)).toBe(CoordinateSys.GALACTIC);
        expect(findCoordSys(GALACTIC_JSYS, 1950)).toBe(CoordinateSys.GALACTIC);
        expect(findCoordSys(SUPERGALACTIC_JSYS, 0)).toBe(CoordinateSys.SUPERGALACTIC);
    });

    test('findCoordSys() with an unknown combination returns UNDEFINED', () => {
        expect(findCoordSys(EQUATORIAL_J, 1950)).toBe(CoordinateSys.UNDEFINED);
        expect(findCoordSys(ECLIPTIC_B, 2000)).toBe(CoordinateSys.UNDEFINED);
        expect(findCoordSys(NONCELESTIAL, 0)).toBe(CoordinateSys.UNDEFINED);
    });

    test('findCoordSys() round trips getJsys() and getEquinox()', () => {
        [CoordinateSys.EQ_J2000, CoordinateSys.EQ_B2000, CoordinateSys.EQ_B1950, CoordinateSys.GALACTIC,
            CoordinateSys.SUPERGALACTIC, CoordinateSys.ECL_J2000, CoordinateSys.ECL_B1950]
            .forEach((coordSys) => expect(findCoordSys(coordSys.getJsys(), coordSys.getEquinox())).toBe(coordSys));
    });
});


describe('Test CoordSys.findValidCoordSysStr()', () => {

    test('findValidCoordSysStr() with a combined system and equinox', () => {
        expect(findValidCoordSysStr('EQJ2000')).toEqual('EQ_J2000');
        expect(findValidCoordSysStr('eq_b1950')).toEqual('EQ_B1950');
        expect(findValidCoordSysStr('ECLIPTICB')).toEqual('EC_B1950');
        expect(findValidCoordSysStr('ecj')).toEqual('EC_J2000');
    });

    test('findValidCoordSysStr() with a system and equinox separated by a space', () => {
        expect(findValidCoordSysStr('equatorial b1950')).toEqual('EQ_B1950');
        expect(findValidCoordSysStr('EQ J')).toEqual('EQ_J2000');
        expect(findValidCoordSysStr('ecliptic b')).toEqual('EC_B1950');
        expect(findValidCoordSysStr('ECL J2000')).toEqual('EC_J2000');
    });

    test('findValidCoordSysStr() with a system only defaults to J2000', () => {
        expect(findValidCoordSysStr('equatorial')).toEqual('EQ_J2000');
        expect(findValidCoordSysStr('E')).toEqual('EQ_J2000');
        expect(findValidCoordSysStr('ecliptic')).toEqual('EC_J2000');
    });

    test('findValidCoordSysStr() with an equinox only is equatorial', () => {
        expect(findValidCoordSysStr('j2000')).toEqual('EQ_J2000');
        expect(findValidCoordSysStr('J20')).toEqual('EQ_J2000');
        expect(findValidCoordSysStr('B1950')).toEqual('EQ_B1950');
        expect(findValidCoordSysStr('b19')).toEqual('EQ_B1950');
    });

    test('findValidCoordSysStr() with galactic', () => {
        ['galactic', 'GAL', 'ga', 'G'].forEach((desc) => expect(findValidCoordSysStr(desc)).toEqual('GALACTIC'));
    });

    test('findValidCoordSysStr() with unrecognized input returns undefined', () => {
        expect(findValidCoordSysStr('equatorial x')).toBeUndefined();
        expect(findValidCoordSysStr('ecliptic x')).toBeUndefined();
        expect(findValidCoordSysStr('ICRS')).toBeUndefined();
        expect(findValidCoordSysStr('SUPERGALACTIC')).toBeUndefined();
        expect(findValidCoordSysStr('EQ_B2000')).toBeUndefined();
        expect(findValidCoordSysStr('PIXEL')).toBeUndefined();
    });
});
