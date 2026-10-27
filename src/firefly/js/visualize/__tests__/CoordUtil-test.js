import CoordUtil, {dd2sex, parseDBIdToCoord} from '../CoordUtil.js';
import CoordinateSys from '../CoordSys.js';

const {convertLonToString, convertLatToString, convertStringToLon, convertStringToLat, validLon}= CoordUtil;


describe('Test CoordUtil.dd2sex()', () => {

    test('dd2sex() equatorial longitude is formatted as HMS', () => {
        expect(dd2sex(150, false, true)).toEqual('10h00m00.00s');
        expect(dd2sex(157.5, false, true)).toEqual('10h30m00.00s');
    });

    test('dd2sex() equatorial latitude is formatted as signed DMS', () => {
        expect(dd2sex(30.5, true, true)).toEqual('+30d30m00.0s');
        expect(dd2sex(-30.5, true, true)).toEqual('-30d30m00.0s');
    });

    test('dd2sex() non-equatorial is formatted as decimal degrees', () => {
        expect(dd2sex(10.5, false, false)).toEqual('10.50000d');
        expect(dd2sex(10.5, true, false)).toEqual('+10.50000d');
        expect(dd2sex(-10.5, true, false)).toEqual('-10.50000d');
    });

    test('dd2sex() wraps longitude into [0, 360)', () => {
        expect(dd2sex(-30, false, false)).toEqual('330.00000d');
        expect(dd2sex(370, false, false)).toEqual('10.00000d');
    });

    test('dd2sex() using parameter precision', () => {
        expect(dd2sex(10.25, false, false, 2)).toEqual('10.25d');
        expect(dd2sex(10.25, false, false, 0)).toEqual('10d');
        expect(dd2sex(10.25, false, false, 20)).toEqual('10.25000000d');
        expect(dd2sex(150, false, true, 8)).toEqual('10h00m00.00000s');
    });

    test('dd2sex() rounding carries up through seconds, minutes and hours', () => {
        expect(dd2sex(359.99999999, false, true)).toEqual('0h00m00.00s');
    });
});


describe('Test CoordUtil.convertStringToLon()', () => {

    test('convertStringToLon() equatorial sexagesimal is in hours', () => {
        expect(convertStringToLon('10:00:00', CoordinateSys.EQ_J2000)).toBeCloseTo(150, 10);
        expect(convertStringToLon('10 30 00', CoordinateSys.EQ_J2000)).toBeCloseTo(157.5, 10);
        expect(convertStringToLon('10h30m', CoordinateSys.EQ_J2000)).toBeCloseTo(157.5, 10);
        expect(convertStringToLon('10h30m36s', CoordinateSys.EQ_J2000)).toBeCloseTo(157.65, 10);
    });

    test('convertStringToLon() equatorial with degree separators is in degrees', () => {
        expect(convertStringToLon('150d30m00s', CoordinateSys.EQ_J2000)).toBeCloseTo(150.5, 10);
        expect(convertStringToLon('150.5d', CoordinateSys.EQ_J2000)).toBeCloseTo(150.5, 10);
    });

    test('convertStringToLon() equatorial plain number is in hours', () => {
        expect(convertStringToLon('10.5', CoordinateSys.EQ_J2000)).toBeCloseTo(157.5, 10);
    });

    test('convertStringToLon() non-equatorial plain number is in degrees', () => {
        expect(convertStringToLon('150.5', CoordinateSys.GALACTIC)).toBeCloseTo(150.5, 10);
    });

    test('convertStringToLon() with out of range values throws', () => {
        expect(() => convertStringToLon('24h', CoordinateSys.EQ_J2000)).toThrow('RA is too big');
        expect(() => convertStringToLon('150.5', CoordinateSys.EQ_J2000)).toThrow('RA is too big');
        expect(() => convertStringToLon('360d', CoordinateSys.EQ_J2000)).toThrow('Longitude is too big');
        expect(() => convertStringToLon('-10d', CoordinateSys.EQ_J2000)).toThrow('Longitude cannot be negative');
        expect(() => convertStringToLon('10:60:00', CoordinateSys.EQ_J2000)).toThrow('Greater than 60');
    });

    test('convertStringToLon() with sexagesimal for a non-equatorial system throws', () => {
        expect(() => convertStringToLon('10:00:00', CoordinateSys.GALACTIC)).toThrow('Sexagesimal for non-Equatorial');
    });

    test('convertStringToLon() with invalid strings throws', () => {
        expect(() => convertStringToLon('', CoordinateSys.EQ_J2000)).toThrow();
        expect(() => convertStringToLon('+', CoordinateSys.EQ_J2000)).toThrow('Invalid Input');
        expect(() => convertStringToLon('abc', CoordinateSys.EQ_J2000)).toThrow('Invalid Input');
        expect(() => convertStringToLon('10.5:30', CoordinateSys.EQ_J2000)).toThrow('Invalid input');
        expect(() => convertStringToLon('10:30:00:00', CoordinateSys.EQ_J2000)).toThrow('Invalid Input');
    });
});


describe('Test CoordUtil.convertStringToLat()', () => {

    test('convertStringToLat() equatorial sexagesimal is in degrees', () => {
        expect(convertStringToLat('+30:30:00', CoordinateSys.EQ_J2000)).toBeCloseTo(30.5, 10);
        expect(convertStringToLat('-30d30m', CoordinateSys.EQ_J2000)).toBeCloseTo(-30.5, 10);
        expect(convertStringToLat('- 30 30 36', CoordinateSys.EQ_J2000)).toBeCloseTo(-30.51, 10);
    });

    test('convertStringToLat() plain number is in degrees', () => {
        expect(convertStringToLat('45.25', CoordinateSys.EQ_J2000)).toBeCloseTo(45.25, 10);
        expect(convertStringToLat('-45.25', CoordinateSys.GALACTIC)).toBeCloseTo(-45.25, 10);
    });

    test('convertStringToLat() with only arc minutes or arc seconds', () => {
        expect(convertStringToLat('30m', CoordinateSys.EQ_J2000)).toBeCloseTo(0.5, 10);
        expect(convertStringToLat(`30'`, CoordinateSys.EQ_J2000)).toBeCloseTo(0.5, 10);
        expect(convertStringToLat('36"', CoordinateSys.EQ_J2000)).toBeCloseTo(0.01, 10);
    });

    test('convertStringToLat() with invalid values throws', () => {
        expect(() => convertStringToLat('91', CoordinateSys.EQ_J2000)).toThrow('Latitude is out of range');
        expect(() => convertStringToLat('-91', CoordinateSys.EQ_J2000)).toThrow('Latitude is out of range');
        expect(() => convertStringToLat('10h', CoordinateSys.EQ_J2000)).toThrow('HMS notation not valid for latitude');
    });
});


describe('Test CoordUtil.convertLonToString() and convertLatToString()', () => {

    test('convertLonToString() using an equatorial or non-equatorial system', () => {
        expect(convertLonToString(150, CoordinateSys.EQ_J2000)).toEqual('10h00m00.00s');
        expect(convertLonToString(150, CoordinateSys.GALACTIC)).toEqual('150.00000d');
    });

    test('convertLatToString() using an equatorial or non-equatorial system', () => {
        expect(convertLatToString(-30.5, CoordinateSys.EQ_J2000)).toEqual('-30d30m00.0s');
        expect(convertLatToString(10.5, CoordinateSys.GALACTIC)).toEqual('+10.50000d');
    });

    test('convertLonToString() and convertLatToString() round trip with convertStringToLon() and convertStringToLat()', () => {
        [CoordinateSys.EQ_J2000, CoordinateSys.GALACTIC].forEach((coordSys) => {
            expect(convertStringToLon(convertLonToString(188.73625, coordSys), coordSys)).toBeCloseTo(188.73625, 4);
            expect(convertStringToLat(convertLatToString(-12.5822, coordSys), coordSys)).toBeCloseTo(-12.5822, 4);
        });
    });
});


describe('Test CoordUtil.validLon()', () => {

    test('validLon() with valid and invalid longitudes', () => {
        expect(validLon('10:00:00', CoordinateSys.EQ_J2000)).toBe(true);
        expect(validLon('150.5', CoordinateSys.GALACTIC)).toBe(true);
        expect(validLon('25h', CoordinateSys.EQ_J2000)).toBe(false);
        expect(validLon('10:00:00', CoordinateSys.GALACTIC)).toBe(false);
        expect(validLon('abc', CoordinateSys.EQ_J2000)).toBe(false);
    });
});


describe('Test CoordUtil.parseDBIdToCoord()', () => {

    test('parseDBIdToCoord() with a J2000 id', () => {
        const coord= parseDBIdToCoord('J123456.7+123456');
        expect(coord.cSys).toBe(CoordinateSys.EQ_J2000);
        expect(coord.lon).toBeCloseTo((12 + 34/60 + 56.7/3600) * 15, 8);
        expect(coord.lat).toBeCloseTo(12 + 34/60 + 56/3600, 8);
    });

    test('parseDBIdToCoord() with a negative latitude', () => {
        const coord= parseDBIdToCoord('J123456-123456');
        expect(coord.lat).toBeCloseTo(-(12 + 34/60 + 56/3600), 8);
    });

    test('parseDBIdToCoord() inserts a decimal point into long seconds', () => {
        const coord= parseDBIdToCoord('J12345678+1234567');
        expect(coord.lon).toBeCloseTo((12 + 34/60 + 56.78/3600) * 15, 8);
        expect(coord.lat).toBeCloseTo(12 + 34/60 + 56.7/3600, 8);
    });

    test('parseDBIdToCoord() coordinate system comes from the first character', () => {
        expect(parseDBIdToCoord('B123456+123456').cSys).toBe(CoordinateSys.EQ_B1950);
        expect(parseDBIdToCoord('f123456+123456').cSys).toBe(CoordinateSys.EQ_J2000);
        expect(parseDBIdToCoord('123456+123456').cSys).toBe(CoordinateSys.EQ_J2000);
    });

    test('parseDBIdToCoord() with a galactic id uses decimal degrees', () => {
        expect(parseDBIdToCoord('G123.4+05.6')).toEqual({lon: 123.4, lat: 5.6, cSys: CoordinateSys.GALACTIC});
        expect(parseDBIdToCoord('G123.4-05.6')).toEqual({lon: 123.4, lat: -5.6, cSys: CoordinateSys.GALACTIC});
    });

    test('parseDBIdToCoord() with invalid ids returns false', () => {
        expect(parseDBIdToCoord(undefined)).toBe(false);
        expect(parseDBIdToCoord('')).toBe(false);
        expect(parseDBIdToCoord('X123456+123456')).toBe(false);
        expect(parseDBIdToCoord('J123456')).toBe(false);
        expect(parseDBIdToCoord('J12 3456+123456')).toBe(false);
        expect(parseDBIdToCoord('J123456+abcdef')).toBe(false);
        expect(parseDBIdToCoord('J123+123456')).toBe(false);
        expect(parseDBIdToCoord('J126056+123456')).toBe(false);
    });
});
