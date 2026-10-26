import {ang2pixNest, HealpixIndex, polarToRadec, radecToPolar, SpatialVector} from '../HealpixIndex.js';

const {PI}= Math;


describe('Test HealpixIndex.radecToPolar()', () => {

    test('radecToPolar() theta is the angle from the north pole and phi is ra, in radians', () => {
        expect(radecToPolar(0, 90)).toEqual({theta: 0, phi: 0});
        const equatorPolar= radecToPolar(180, 0);
        expect(equatorPolar.theta).toBeCloseTo(PI/2, 12);
        expect(equatorPolar.phi).toBeCloseTo(PI, 12);
        const southPolar= radecToPolar(90, -90);
        expect(southPolar.theta).toBeCloseTo(PI, 12);
        expect(southPolar.phi).toBeCloseTo(PI/2, 12);
    });

    test('radecToPolar() is undone by polarToRadec()', () => {
        [[10, 20], [359.5, -89], [200, 45.25]].forEach( ([ra, dec]) => {
            const {theta, phi}= radecToPolar(ra, dec);
            const radec= polarToRadec(theta, phi);
            expect(radec.ra).toBeCloseTo(ra, 10);
            expect(radec.dec).toBeCloseTo(dec, 10);
        });
    });
});


describe('Test HealpixIndex.ang2pixNest()', () => {

    test('ang2pixNest() at nside 1 returns the 12 base pixels', () => {
        // pixels 0 to 3 are around the north pole, 4 to 7 on the equator, 8 to 11 around the south pole
        expect(ang2pixNest(.1, .1, 1)).toEqual(0);
        expect(ang2pixNest(.1, PI/2+.1, 1)).toEqual(1);
        expect(ang2pixNest(.1, PI+.1, 1)).toEqual(2);
        expect(ang2pixNest(.1, 3*PI/2+.1, 1)).toEqual(3);
        expect(ang2pixNest(PI/2, 0, 1)).toEqual(4);
        expect(ang2pixNest(PI/2, PI/2, 1)).toEqual(5);
        expect(ang2pixNest(PI/2, PI, 1)).toEqual(6);
        expect(ang2pixNest(PI/2, 3*PI/2, 1)).toEqual(7);
        expect(ang2pixNest(PI-.1, .1, 1)).toEqual(8);
        expect(ang2pixNest(PI-.1, 3*PI/2+.1, 1)).toEqual(11);
    });

    test('ang2pixNest() at the poles', () => {
        expect(ang2pixNest(0, 0, 1)).toEqual(0);
        expect(ang2pixNest(PI, 0, 1)).toEqual(8);
    });

    test('ang2pixNest() pixel is inside its parent pixel at a shallower nside', () => {
        const {theta, phi}= radecToPolar(83.63, 22.01);
        const deepPixel= ang2pixNest(theta, phi, 2**10);
        [0, 3, 7].forEach( (order) =>
            expect(Math.trunc(deepPixel / 4**(10-order))).toEqual(ang2pixNest(theta, phi, 2**order)));
    });

    test('ang2pixNest() is the inverse of pix2ang_nest() for every pixel', () => {
        const healpixIdx= new HealpixIndex(8);
        for (let ipix= 0; ipix<healpixIdx.npix; ipix++) {
            const {theta, phi}= healpixIdx.pix2ang_nest(ipix);
            expect(ang2pixNest(theta, phi, 8)).toEqual(ipix);
        }
    });

    test('ang2pixNest() wraps phi into [0, 2PI)', () => {
        expect(ang2pixNest(1, 2*PI+.3, 64)).toEqual(ang2pixNest(1, .3, 64));
        expect(ang2pixNest(1, -.3, 64)).toEqual(ang2pixNest(1, 2*PI-.3, 64));
    });

    test('ang2pixNest() with theta outside [0, PI] throws', () => {
        expect(() => ang2pixNest(4, 0, 4)).toThrow('theta must be between 0 and');
        expect(() => ang2pixNest(-.1, 0, 4)).toThrow('theta must be between 0 and');
    });
});


describe('Test HealpixIndex class', () => {

    test('HealpixIndex constructor sets the pixel counts for nside', () => {
        const healpixIdx= new HealpixIndex(4);
        expect(healpixIdx).toMatchObject({nside: 4, order: 2, npface: 16, npix: 192, ncap: 24});
    });

    test('HealpixIndex.nside2Npix() is 12 * nside^2', () => {
        expect(HealpixIndex.nside2Npix(1)).toEqual(12);
        expect(HealpixIndex.nside2Npix(2)).toEqual(48);
        expect(HealpixIndex.nside2Npix(1024)).toEqual(12582912);
    });

    test('HealpixIndex.nside2Npix() with an nside that is not a power of 2 throws', () => {
        expect(() => HealpixIndex.nside2Npix(3)).toThrow('nside should be >0, power of 2');
    });

    test('HealpixIndex.calculateNSide() picks the nside whose pixels are closest to the size in arcsec', () => {
        expect(HealpixIndex.calculateNSide(360000)).toEqual(1);
        expect(HealpixIndex.calculateNSide(3600)).toEqual(64);
        expect(HealpixIndex.calculateNSide(60)).toEqual(4096);
        expect(HealpixIndex.calculateNSide(1)).toEqual(262144);
    });

    test('HealpixIndex.calculateNSide() stops at the largest nside', () => {
        jest.spyOn(console, 'log').mockImplementation(() => undefined);
        expect(HealpixIndex.calculateNSide(.000001)).toEqual(2**24);
        jest.restoreAllMocks();
    });

    test('HealpixIndex pix2ang_nest() with a pixel out of range throws', () => {
        expect(() => new HealpixIndex(4).pix2ang_nest(192)).toThrow('ipix out of range');
        expect(() => new HealpixIndex(4).pix2ang_nest(-1)).toThrow('ipix out of range');
    });

    test('HealpixIndex nest2ring() and ring2nest() undo each other for every pixel', () => {
        const healpixIdx= new HealpixIndex(8);
        for (let ipix= 0; ipix<healpixIdx.npix; ipix++) {
            expect(healpixIdx.ring2nest(healpixIdx.nest2ring(ipix))).toEqual(ipix);
        }
    });

    test('HealpixIndex pix2ang_ring() returns the same pixel center as pix2ang_nest() for every pixel', () => {
        const healpixIdx= new HealpixIndex(8);
        for (let ipix= 0; ipix<healpixIdx.npix; ipix++) {
            const [theta, phi]= healpixIdx.pix2ang_ring(healpixIdx.nest2ring(ipix));
            const nestCenter= healpixIdx.pix2ang_nest(ipix);
            expect(theta).toBeCloseTo(nestCenter.theta, 12);
            expect(phi).toBeCloseTo(nestCenter.phi, 12);
        }
    });

    test('HealpixIndex.vector() and vec2Ang() undo each other', () => {
        const [theta, phi]= HealpixIndex.vec2Ang(HealpixIndex.vector(1.2, 4));
        expect(theta).toBeCloseTo(1.2, 12);
        expect(phi).toBeCloseTo(4, 12);
        expect(HealpixIndex.vector(1.2, 4).length()).toBeCloseTo(1, 12);
    });

    test('HealpixIndex corners_nest() with step 1 returns 4 unit vectors around the pixel center', () => {
        const healpixIdx= new HealpixIndex(8);
        const {theta, phi}= healpixIdx.pix2ang_nest(100);
        const centerVector= HealpixIndex.vector(theta, phi);
        const corners= healpixIdx.corners_nest(100, 1);
        expect(corners.length).toEqual(4);
        corners.forEach( (corner) => {
            expect(corner.length()).toBeCloseTo(1, 12);
            // an nside 8 pixel is about 7.3 degrees, .128 radians, across
            expect(corner.angle(centerVector)).toBeGreaterThan(0);
            expect(corner.angle(centerVector)).toBeLessThan(.128);
        });
    });

    test('HealpixIndex corners_nest() with a larger step returns more points along the edges', () => {
        expect(new HealpixIndex(8).corners_nest(100, 2).length).toEqual(6);
    });
});


describe('Test HealpixIndex queryDisc()', () => {

    const healpixIdx= new HealpixIndex(8);
    const centerVector= new SpatialVector();
    centerVector.set(10, 20);
    const {theta: centerTheta, phi: centerPhi}= radecToPolar(10, 20);

    test('queryDisc() exact returns only pixels whose centers are inside the disc', () => {
        const pixAry= healpixIdx.queryDisc(centerVector, .1, true, false);
        expect(pixAry.length).toBeGreaterThan(0);
        pixAry.forEach( (ipix) => {
            const {theta, phi}= healpixIdx.pix2ang_nest(ipix);
            expect(HealpixIndex.vector(theta, phi).angle(centerVector)).toBeLessThanOrEqual(.1);
        });
    });

    test('queryDisc() inclusive returns more pixels, including the one holding the center', () => {
        const exactAry= healpixIdx.queryDisc(centerVector, .1, true, false);
        const inclusiveAry= healpixIdx.queryDisc(centerVector, .1, true, true);
        expect(inclusiveAry.length).toBeGreaterThan(exactAry.length);
        exactAry.forEach( (ipix) => expect(inclusiveAry).toContain(ipix));
        expect(inclusiveAry).toContain(ang2pixNest(centerTheta, centerPhi, 8));
    });

    test('queryDisc() ring and nest results are the same pixels', () => {
        const nestAry= healpixIdx.queryDisc(centerVector, .3, true, true);
        const ringAry= healpixIdx.queryDisc(centerVector, .3, false, true);
        expect(ringAry.map( (ipix) => healpixIdx.ring2nest(ipix)).sort()).toEqual([...nestAry].sort());
    });

    test('queryDisc() with a radius of PI returns every pixel', () => {
        expect(healpixIdx.queryDisc(centerVector, PI, true, false).length).toEqual(healpixIdx.npix);
    });

    test('queryDisc() with a radius outside [0, PI] throws', () => {
        expect(() => healpixIdx.queryDisc(centerVector, 4, true, false)).toThrow('angular radius is in RADIAN');
        expect(() => healpixIdx.queryDisc(centerVector, -.1, true, false)).toThrow('angular radius is in RADIAN');
    });
});


describe('Test HealpixIndex.SpatialVector', () => {

    test('SpatialVector set() makes a unit vector from ra and dec in degrees', () => {
        const spatialVector= new SpatialVector();
        spatialVector.set(90, 0);
        expect(spatialVector.x).toBeCloseTo(0, 12);
        expect(spatialVector.y).toBeCloseTo(1, 12);
        expect(spatialVector.z).toBeCloseTo(0, 12);
        expect(spatialVector.ra()).toEqual(90);
        expect(spatialVector.dec()).toEqual(0);
        spatialVector.set(0, 90);
        expect(spatialVector.z).toBeCloseTo(1, 12);
    });

    test('SpatialVector ra() and dec() from x, y, z', () => {
        expect(new SpatialVector(-1, 0, 0).ra()).toEqual(180);
        expect(new SpatialVector(0, -1, 0).ra()).toEqual(270);
        expect(new SpatialVector(1, 1, 0).ra()).toBeCloseTo(45, 10);
        expect(new SpatialVector(0, 0, 1).dec()).toEqual(90);
        expect(new SpatialVector(0, 0, 1).ra()).toEqual(0);
    });

    test('SpatialVector ra() normalizes a vector that is not unit length, in place', () => {
        const spatialVector= new SpatialVector(0, 2, 0);
        expect(spatialVector.ra()).toEqual(90);
        expect(spatialVector.dec()).toEqual(0);
        expect(spatialVector.get()).toEqual([0, 1, 0]);
    });

    test('SpatialVector length(), lengthSquared() and normalized()', () => {
        const spatialVector= new SpatialVector(3, 4, 12);
        expect(spatialVector.lengthSquared()).toEqual(169);
        expect(spatialVector.length()).toEqual(13);
        spatialVector.normalized();
        expect(spatialVector.length()).toBeCloseTo(1, 12);
        expect(spatialVector.x).toBeCloseTo(3/13, 12);
    });

    test('SpatialVector dot(), add(), sub() and mult()', () => {
        const vector1= new SpatialVector(1, 2, 3);
        const vector2= new SpatialVector(4, -5, 6);
        expect(vector1.dot(vector2)).toEqual(12);
        expect(vector1.add(vector2).get()).toEqual([5, -3, 9]);
        expect(vector1.sub(vector2).get()).toEqual([-3, 7, -3]);
        expect(vector1.mult(2).get()).toEqual([2, 4, 6]);
        expect(vector1.get()).toEqual([1, 2, 3]);
    });

    test('SpatialVector angle() is the angle between two vectors in radians', () => {
        expect(new SpatialVector(1, 0, 0).angle(new SpatialVector(0, 1, 0))).toBeCloseTo(PI/2, 12);
        expect(new SpatialVector(1, 0, 0).angle(new SpatialVector(-1, 0, 0))).toBeCloseTo(PI, 12);
        expect(new SpatialVector(1, 1, 0).angle(new SpatialVector(2, 2, 0))).toBeCloseTo(0, 12);
    });

    test('SpatialVector cross() is perpendicular to both vectors and reverses with the order', () => {
        expect(new SpatialVector(1, 0, 0).cross(new SpatialVector(0, 1, 0)).get()).toEqual([0, 0, 1]);
        expect(new SpatialVector(0, 1, 0).cross(new SpatialVector(1, 0, 0)).get()).toEqual([0, 0, -1]);
        const vector1= new SpatialVector(1, 2, 3);
        const vector2= new SpatialVector(4, 5, 6);
        const crossVector= vector1.cross(vector2);
        expect(crossVector.get()).toEqual([-3, 6, -3]);
        expect(crossVector.dot(vector1)).toEqual(0);
        expect(crossVector.dot(vector2)).toEqual(0);
    });

    test('SpatialVector equal(), get() and toString()', () => {
        expect(new SpatialVector(1, 2, 3).equal(new SpatialVector(1, 2, 3))).toBe(true);
        expect(new SpatialVector(1, 2, 3).equal(new SpatialVector(1, 2, 4))).toBe(false);
        expect(new SpatialVector(1, 2, 3).get()).toEqual([1, 2, 3]);
        expect(new SpatialVector(1, 2, 3).toString()).toEqual('SpatialVector[1, 2, 3]');
    });
});
