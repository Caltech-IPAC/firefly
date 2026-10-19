import {
    calculatePosition, calculatePositionFromLocalOffsets, computeCentralPointAndRadius, computeCentralPtRadiusAverage,
    computeDistance, computeScreenDistance, computeSimpleDistance, computeSimpleSlope, computeSimpleSlopeAngle,
    contains, containsCircle, containsEllipse, containsRec, convertAngle, distanceToPolygon, distToLine,
    doSegmentsCross, findIntersectionPt, getAngleInDeg, getArrowCoords, getBoundingBox, getEllipseArcEndPoints,
    getLatDist, getLinePointAry, getLonDist, getPointOnEllipse, getPositionAngle, getTranslateAndRotatePosition,
    intersects, isAngleUnit, lineCrossesRect, lineIntersect2, normalizeRotation, pointInRec, segmentIntersectRect,
    toDegrees, toRadians, transformToEastNorth
} from '../VisUtil.js';
import Point, {makeImagePt, makeScreenPt, makeWorldPt} from '../Point.js';
import CoordinateSys from '../CoordSys.js';


describe('Test VisUtil.computeScreenDistance()', () => {

    test('computeScreenDistance() using parameters (x1, y1, x2, y2)', () => {
        expect(computeScreenDistance(0, 0, 3, 4)).toEqual(5);
        expect(computeScreenDistance(3, 4, 0, 0)).toEqual(5);
        expect(computeScreenDistance(-1, -1, 2, 3)).toEqual(5);
        expect(computeScreenDistance(7, 7, 7, 7)).toEqual(0);
    });
});


describe('Test VisUtil.computeDistance()', () => {

    // computeDistance() is the exported way to reach the private computeDistanceAngularDistance()

    test('computeDistance() along the equator and along a meridian', () => {
        expect(computeDistance(makeWorldPt(0, 0), makeWorldPt(90, 0))).toBeCloseTo(90, 10);
        expect(computeDistance(makeWorldPt(10, 0), makeWorldPt(11, 0))).toBeCloseTo(1, 10);
        expect(computeDistance(makeWorldPt(0, 0), makeWorldPt(0, 90))).toBeCloseTo(90, 10);
        expect(computeDistance(makeWorldPt(50, -30), makeWorldPt(50, 30))).toBeCloseTo(60, 10);
    });

    test('computeDistance() is the same in both directions', () => {
        const wp1= makeWorldPt(83.63, 22.01);
        const wp2= makeWorldPt(10.68, 41.27);
        expect(computeDistance(wp1, wp2)).toBeCloseTo(computeDistance(wp2, wp1), 10);
    });

    test('computeDistance() across the 0/360 longitude line', () => {
        expect(computeDistance(makeWorldPt(359, 0), makeWorldPt(1, 0))).toBeCloseTo(2, 10);
    });

    test('computeDistance() longitude separation shrinks with latitude', () => {
        // at latitude 60, 2 degrees of longitude is about 1 degree on the sky
        expect(computeDistance(makeWorldPt(0, 60), makeWorldPt(2, 60))).toBeCloseTo(1, 3);
    });

    test('computeDistance() between the poles and antipodal points is 180', () => {
        expect(computeDistance(makeWorldPt(0, 90), makeWorldPt(0, -90))).toBeCloseTo(180, 10);
        expect(computeDistance(makeWorldPt(0, 0), makeWorldPt(180, 0))).toBeCloseTo(180, 10);
    });

    test('computeDistance() of a point to itself is 0', () => {
        expect(computeDistance(makeWorldPt(83.63, 22.01), makeWorldPt(83.63, 22.01))).toBeCloseTo(0, 5);
        expect(computeDistance(makeWorldPt(10, 90), makeWorldPt(200, 90))).toBeCloseTo(0, 5);
    });

    test('computeDistance() returns the same value for repeated calls', () => {
        const firstDist= computeDistance(makeWorldPt(1, 2), makeWorldPt(3, 4));
        expect(computeDistance(makeWorldPt(1, 2), makeWorldPt(3, 4))).toEqual(firstDist);
    });

    test('computeDistance() with a missing point returns 0', () => {
        expect(computeDistance(undefined, makeWorldPt(1, 2))).toEqual(0);
        expect(computeDistance(makeWorldPt(1, 2), null)).toEqual(0);
    });
});


describe('Test VisUtil.computeSimpleDistance()', () => {

    test('computeSimpleDistance() using parameters (p1, p2)', () => {
        expect(computeSimpleDistance(makeScreenPt(0, 0), makeScreenPt(3, 4))).toEqual(5);
        expect(computeSimpleDistance({x: 2, y: 3}, {x: -1, y: -1})).toEqual(5);
        expect(computeSimpleDistance(makeScreenPt(5, 5), makeScreenPt(5, 5))).toEqual(0);
    });
});


describe('Test VisUtil.computeSimpleSlopeAngle()', () => {

    test('computeSimpleSlopeAngle() returns the angle in radians from fromPt to toPt', () => {
        const origin= makeScreenPt(0, 0);
        expect(computeSimpleSlopeAngle(origin, makeScreenPt(1, 0))).toEqual(0);
        expect(computeSimpleSlopeAngle(origin, makeScreenPt(1, 1))).toBeCloseTo(Math.PI/4, 10);
        expect(computeSimpleSlopeAngle(origin, makeScreenPt(0, 1))).toBeCloseTo(Math.PI/2, 10);
        expect(computeSimpleSlopeAngle(origin, makeScreenPt(-1, 0))).toBeCloseTo(Math.PI, 10);
        expect(computeSimpleSlopeAngle(origin, makeScreenPt(0, -1))).toBeCloseTo(-Math.PI/2, 10);
    });

    test('computeSimpleSlopeAngle() reversing the points adds PI', () => {
        const fromPt= makeScreenPt(1, 1);
        const toPt= makeScreenPt(4, 5);
        const forwardAngle= computeSimpleSlopeAngle(fromPt, toPt);
        const backwardAngle= computeSimpleSlopeAngle(toPt, fromPt);
        expect(forwardAngle - backwardAngle).toBeCloseTo(Math.PI, 10);
    });
});


describe('Test VisUtil.computeSimpleSlope()', () => {

    test('computeSimpleSlope() using parameters (fromPt, toPt)', () => {
        expect(computeSimpleSlope(makeScreenPt(0, 0), makeScreenPt(2, 1))).toEqual(.5);
        expect(computeSimpleSlope(makeScreenPt(1, 1), makeScreenPt(3, -3))).toEqual(-2);
        expect(computeSimpleSlope(makeScreenPt(0, 5), makeScreenPt(4, 5))).toEqual(0);
    });

    test('computeSimpleSlope() is the same in both directions', () => {
        const fromPt= makeScreenPt(1, 1);
        const toPt= makeScreenPt(4, 7);
        expect(computeSimpleSlope(fromPt, toPt)).toEqual(computeSimpleSlope(toPt, fromPt));
    });

    test('computeSimpleSlope() of a vertical line is infinite', () => {
        expect(computeSimpleSlope(makeScreenPt(2, 0), makeScreenPt(2, 3))).toEqual(Infinity);
        expect(computeSimpleSlope(makeScreenPt(2, 0), makeScreenPt(2, -3))).toEqual(-Infinity);
    });
});


describe('Test VisUtil.getLonDist()', () => {

    test('getLonDist() returns the signed longitude change from lon1 to lon2', () => {
        expect(getLonDist(10, 20)).toEqual(10);
        expect(getLonDist(20, 10)).toEqual(-10);
        expect(getLonDist(100, 100)).toEqual(0);
    });

    test('getLonDist() with a change of exactly 180', () => {
        expect(getLonDist(0, 180)).toEqual(180);
        expect(getLonDist(180, 0)).toEqual(-180);
    });

    test('getLonDist() takes the shorter way across the 0/360 line', () => {
        expect(getLonDist(350, 10)).toEqual(20);
        expect(getLonDist(10, 350)).toEqual(-20);
        expect(getLonDist(340, 10)).toEqual(30);
        expect(getLonDist(359, 1)).toEqual(2);
        expect(getLonDist(190, 0)).toEqual(170);
        expect(getLonDist(0, 190)).toEqual(-170);
    });
});


describe('Test VisUtil.getLatDist()', () => {

    test('getLatDist() returns the signed latitude change from lat1 to lat2', () => {
        expect(getLatDist(10, 30)).toEqual(20);
        expect(getLatDist(30, 10)).toEqual(-20);
        expect(getLatDist(-45, 45)).toEqual(90);
        expect(getLatDist(12.5, 12.5)).toBeCloseTo(0, 10); // returns -0
    });
});


describe('Test VisUtil.computeCentralPointAndRadius()', () => {

    test('computeCentralPointAndRadius() center is the middle of the lon and lat range', () => {
        const {centralPoint, maxRadius}= computeCentralPointAndRadius(
            [makeWorldPt(10, 20), makeWorldPt(14, 20), makeWorldPt(12, 24), makeWorldPt(12, 16)]);
        expect(centralPoint.x).toBeCloseTo(12, 10);
        expect(centralPoint.y).toBeCloseTo(20, 10);
        expect(centralPoint.cSys).toBe(CoordinateSys.EQ_J2000);
        expect(maxRadius).toBeCloseTo(4, 10);
    });

    test('computeCentralPointAndRadius() maxRadius is the distance to the farthest point', () => {
        const {centralPoint, maxRadius}= computeCentralPointAndRadius([makeWorldPt(0, 0), makeWorldPt(0, 10)]);
        expect(centralPoint.x).toBeCloseTo(0, 10);
        expect(centralPoint.y).toBeCloseTo(5, 10);
        expect(maxRadius).toBeCloseTo(5, 10);
    });

    test('computeCentralPointAndRadius() with one point', () => {
        const {centralPoint, maxRadius}= computeCentralPointAndRadius([makeWorldPt(83.63, 22.01)]);
        expect(centralPoint.x).toBeCloseTo(83.63, 10);
        expect(centralPoint.y).toBeCloseTo(22.01, 10);
        expect(maxRadius).toBeCloseTo(0, 5);
    });

    test('computeCentralPointAndRadius() with two points across the 0/360 line', () => {
        const {centralPoint, maxRadius}= computeCentralPointAndRadius([makeWorldPt(355, 0), makeWorldPt(15, 0)]);
        expect(centralPoint.x).toBeCloseTo(5, 10);
        expect(maxRadius).toBeCloseTo(10, 10);
    });

    test('computeCentralPointAndRadius() with several points across the 0/360 line', () => {
        const centerOf= (lons) => computeCentralPointAndRadius(lons.map( (lon) => makeWorldPt(lon, 0)));
        expect(centerOf([350, 10]).centralPoint.x).toBeCloseTo(0, 10);
        expect(centerOf([350, 10]).maxRadius).toBeCloseTo(10, 10);
        expect(centerOf([350, 0, 10]).centralPoint.x).toBeCloseTo(0, 10);
        expect(centerOf([350, 0, 10]).maxRadius).toBeCloseTo(10, 10);
        expect(centerOf([350, 10, 20]).centralPoint.x).toBeCloseTo(5, 10);
        expect(centerOf([350, 10, 20]).maxRadius).toBeCloseTo(15, 10);
    });

    test('computeCentralPointAndRadius() with points spread more than 180 degrees', () => {
        // the largest gap is 10 to 200, so the points cover 200 through 300 to 10, centered on 285
        const {centralPoint, maxRadius}= computeCentralPointAndRadius(
            [makeWorldPt(10, 0), makeWorldPt(200, 0), makeWorldPt(300, 0)]);
        expect(centralPoint.x).toBeCloseTo(285, 10);
        expect(maxRadius).toBeCloseTo(85, 10);
    });

    test('computeCentralPointAndRadius() with two points exactly 180 apart keeps the 0 to 180 range', () => {
        expect(computeCentralPointAndRadius([makeWorldPt(0, 0), makeWorldPt(180, 0)]).centralPoint.x)
            .toBeCloseTo(90, 10);
    });

    test('computeCentralPointAndRadius() converts points to J2000', () => {
        // the galactic center in J2000
        const {centralPoint}= computeCentralPointAndRadius([makeWorldPt(0, 0, CoordinateSys.GALACTIC)]);
        expect(centralPoint.cSys).toBe(CoordinateSys.EQ_J2000);
        expect(centralPoint.x).toBeCloseTo(266.405, 3);
        expect(centralPoint.y).toBeCloseTo(-28.936, 3);
    });

    test('computeCentralPointAndRadius() with no points has no central point', () => {
        expect(computeCentralPointAndRadius([]).centralPoint).toBeUndefined();
        expect(computeCentralPointAndRadius([undefined]).centralPoint).toBeUndefined();
    });
});


describe('Test VisUtil.computeCentralPtRadiusAverage()', () => {

    test('computeCentralPtRadiusAverage() with no points', () => {
        const emptyResult= {centralPoint: undefined, fovSize: 0, avgOfCenters: undefined};
        expect(computeCentralPtRadiusAverage([])).toEqual(emptyResult);
        expect(computeCentralPtRadiusAverage([[], []])).toEqual(emptyResult);
    });

    test('computeCentralPtRadiusAverage() with only one distinct point uses minSizeDeg', () => {
        const wp= makeWorldPt(10, 20);
        expect(computeCentralPtRadiusAverage([[wp], [makeWorldPt(10, 20), wp]]))
            .toEqual({centralPoint: wp, fovSize: .11, avgOfCenters: wp});
        expect(computeCentralPtRadiusAverage([[wp]], .5).fovSize).toEqual(.5);
    });

    test('computeCentralPtRadiusAverage() with one group', () => {
        const {centralPoint, fovSize, avgOfCenters}=
            computeCentralPtRadiusAverage([[makeWorldPt(10, 0), makeWorldPt(12, 0)]]);
        expect(centralPoint.x).toBeCloseTo(11, 10);
        expect(centralPoint.y).toBeCloseTo(0, 10);
        expect(fovSize).toBeCloseTo(1, 10);
        expect(avgOfCenters).toBe(centralPoint);
    });

    test('computeCentralPtRadiusAverage() fovSize is at least minSizeDeg', () => {
        const smallGroup= [[makeWorldPt(10, 0), makeWorldPt(10.01, 0)]];
        expect(computeCentralPtRadiusAverage(smallGroup).fovSize).toEqual(.11);
        expect(computeCentralPtRadiusAverage(smallGroup, .3).fovSize).toEqual(.3);
    });

    test('computeCentralPtRadiusAverage() avgOfCenters uses the center of each group', () => {
        // all points: lon 10 to 22, center 16. group centers: 10 and 21, center 15.5
        const {centralPoint, fovSize, avgOfCenters}= computeCentralPtRadiusAverage([
            [makeWorldPt(10, 0)],
            [makeWorldPt(20, 0), makeWorldPt(21, 0), makeWorldPt(22, 0)],
        ]);
        expect(centralPoint.x).toBeCloseTo(16, 10);
        expect(fovSize).toBeCloseTo(6, 10);
        expect(avgOfCenters.x).toBeCloseTo(15.5, 10);
        expect(avgOfCenters.y).toBeCloseTo(0, 10);
    });

    test('computeCentralPtRadiusAverage() skips empty groups when averaging centers', () => {
        const {centralPoint, avgOfCenters}= computeCentralPtRadiusAverage([
            [makeWorldPt(10, 0), makeWorldPt(12, 0)],
            [],
        ]);
        expect(centralPoint.x).toBeCloseTo(11, 10);
        expect(avgOfCenters.x).toBeCloseTo(11, 10);
    });
});


describe('Test VisUtil.getPositionAngle()', () => {

    test('getPositionAngle() is measured from north through east', () => {
        expect(getPositionAngle(10, 10, 10, 20)).toBeCloseTo(0, 10);
        expect(getPositionAngle(10, 0, 11, 0)).toBeCloseTo(90, 10);
        expect(getPositionAngle(10, 10, 10, 0)).toBeCloseTo(180, 10);
        expect(getPositionAngle(10, 0, 9, 0)).toBeCloseTo(270, 10);
    });

    test('getPositionAngle() between the compass directions', () => {
        expect(getPositionAngle(0, 0, 1, 1)).toBeCloseTo(45, 1);
        expect(getPositionAngle(0, 0, 1, -1)).toBeCloseTo(135, 1);
        expect(getPositionAngle(0, 0, 359, -1)).toBeCloseTo(225, 1);
        expect(getPositionAngle(0, 0, 359, 1)).toBeCloseTo(315, 1);
    });

    test('getPositionAngle() along a parallel away from the equator bends toward the pole', () => {
        // the great circle from (10,60) to (40,60) starts out heading north of east
        expect(getPositionAngle(10, 60, 40, 60)).toBeCloseTo(76.936, 3);
        expect(getPositionAngle(10, -60, 40, -60)).toBeCloseTo(180-76.936, 3);
    });

    test('getPositionAngle() across the 0/360 RA line', () => {
        expect(getPositionAngle(359, 0, 1, 0)).toBeCloseTo(90, 10);
        expect(getPositionAngle(1, 0, 359, 0)).toBeCloseTo(270, 10);
    });

    test('getPositionAngle() reversed on the equator differs by 180', () => {
        expect(getPositionAngle(20, 0, 25, 0) - getPositionAngle(25, 0, 20, 0)).toBeCloseTo(-180, 10);
    });

    test('getPositionAngle() toward the north pole is 0', () => {
        expect(getPositionAngle(0, 0, 123, 90)).toBeCloseTo(0, 10);
        expect(getPositionAngle(0, 0, 200, 90)).toBeCloseTo(0, 10);
    });

    test('getPositionAngle() keeps precision for very short separations', () => {
        // .0001 deg is .36 arcsec
        expect(getPositionAngle(10, 0, 10, .0001)).toBeCloseTo(0, 10);
        expect(getPositionAngle(10, 0, 10.0001, 0)).toBeCloseTo(90, 10);
        expect(getPositionAngle(10, 0, 10, -.0001)).toBeCloseTo(180, 10);
        expect(getPositionAngle(10, 0, 10.0001, .0001)).toBeCloseTo(45, 6);
    });

    test('getPositionAngle() from a pole is fixed', () => {
        expect(getPositionAngle(0, 90, 10, 0)).toEqual(180);
        expect(getPositionAngle(0, 90, 250, 45)).toEqual(180);
        expect(getPositionAngle(0, -90, 10, 0)).toEqual(0);
        expect(getPositionAngle(0, -90, 250, -45)).toEqual(0);
    });

    test('getPositionAngle() of a point to itself is 0', () => {
        expect(getPositionAngle(10, 20, 10, 20)).toEqual(0);
        expect(getPositionAngle(10, 0, 10, .00001)).toEqual(0);
    });
});


describe('Test VisUtil.getTranslateAndRotatePosition()', () => {

    test('getTranslateAndRotatePosition() moves referencePosition to rotatedReferencePosition', () => {
        const movedWp= getTranslateAndRotatePosition(makeWorldPt(10, 20), makeWorldPt(50, -30), makeWorldPt(10, 20));
        expect(movedWp.x).toBeCloseTo(50, 10);
        expect(movedWp.y).toBeCloseTo(-30, 10);
        expect(movedWp.cSys).toBe(CoordinateSys.EQ_J2000);
    });

    test('getTranslateAndRotatePosition() with the same reference positions does not move the point', () => {
        const movedWp= getTranslateAndRotatePosition(makeWorldPt(10, 20), makeWorldPt(10, 20), makeWorldPt(33, 44));
        expect(movedWp.x).toBeCloseTo(33, 10);
        expect(movedWp.y).toBeCloseTo(44, 10);
    });

    test('getTranslateAndRotatePosition() a change in RA only shifts RA at any dec', () => {
        const movedWp= getTranslateAndRotatePosition(makeWorldPt(10, 40), makeWorldPt(30, 40), makeWorldPt(12, 50));
        expect(movedWp.x).toBeCloseTo(32, 10);
        expect(movedWp.y).toBeCloseTo(50, 10);
    });

    test('getTranslateAndRotatePosition() a change in dec along a meridian shifts dec', () => {
        const movedWp= getTranslateAndRotatePosition(makeWorldPt(100, 0), makeWorldPt(100, 20), makeWorldPt(100, 10));
        expect(movedWp.x).toBeCloseTo(100, 10);
        expect(movedWp.y).toBeCloseTo(30, 10);
    });

    test('getTranslateAndRotatePosition() result RA is in [0, 360)', () => {
        const movedWp= getTranslateAndRotatePosition(makeWorldPt(350, 0), makeWorldPt(10, 0), makeWorldPt(355, 0));
        expect(movedWp.x).toBeCloseTo(15, 10);
        expect(movedWp.y).toBeCloseTo(0, 10);
    });

    test('getTranslateAndRotatePosition() can move a point to the pole', () => {
        const movedWp= getTranslateAndRotatePosition(makeWorldPt(0, 0), makeWorldPt(0, 90), makeWorldPt(0, 0));
        expect(movedWp.y).toBeCloseTo(90, 10);
    });

    test('getTranslateAndRotatePosition() keeps the shape: distances between moved points do not change', () => {
        const refWp= makeWorldPt(100, 0);
        const rotatedRefWp= makeWorldPt(250, 45);
        const shapeWps= [makeWorldPt(100.5, .2), makeWorldPt(99.7, -.4), makeWorldPt(100.1, .6)];
        const movedWps= shapeWps.map( (wp) => getTranslateAndRotatePosition(refWp, rotatedRefWp, wp));

        shapeWps.forEach( (wp, idx) => {
            expect(computeDistance(rotatedRefWp, movedWps[idx])).toBeCloseTo(computeDistance(refWp, wp), 8);
            const nextIdx= (idx+1) % shapeWps.length;
            expect(computeDistance(movedWps[idx], movedWps[nextIdx]))
                .toBeCloseTo(computeDistance(wp, shapeWps[nextIdx]), 8);
        });
    });
});


describe('Test VisUtil.getAngleInDeg()', () => {

    test('getAngleInDeg() along the axes', () => {
        expect(getAngleInDeg(0, 0, 10, 0)).toEqual(0);
        expect(getAngleInDeg(0, 0, 0, 10)).toEqual(90);
        expect(getAngleInDeg(0, 0, -10, 0)).toEqual(180);
        expect(getAngleInDeg(0, 0, 0, -10)).toEqual(270);
    });

    test('getAngleInDeg() in each quadrant', () => {
        expect(getAngleInDeg(0, 0, 10, 10)).toBeCloseTo(45, 10);
        expect(getAngleInDeg(0, 0, -10, 10)).toBeCloseTo(135, 10);
        expect(getAngleInDeg(0, 0, -10, -10)).toBeCloseTo(225, 10);
        expect(getAngleInDeg(0, 0, 10, -10)).toBeCloseTo(315, 10);
        expect(getAngleInDeg(0, 0, 3, 1)).toBeCloseTo(18.4349, 4);
        expect(getAngleInDeg(0, 0, -1, -3)).toBeCloseTo(251.5651, 4);
    });

    test('getAngleInDeg() is measured from the center point (cx, cy)', () => {
        expect(getAngleInDeg(5, 5, 15, 5)).toEqual(0);
        expect(getAngleInDeg(100, 200, 90, 210)).toBeCloseTo(135, 10);
    });

    test('getAngleInDeg() rounds all coordinates to whole pixels first', () => {
        expect(getAngleInDeg(0, 0, .4, 10)).toEqual(90);
        expect(getAngleInDeg(.4, .4, 10.2, 9.8)).toBeCloseTo(45, 10);
        // both points round to the same pixel
        expect(getAngleInDeg(0, 0, .4, .3)).toEqual(0);
    });
});


describe('Test VisUtil.intersects()', () => {

    test('intersects() with overlapping rectangles', () => {
        expect(intersects(0, 0, 10, 10, 5, 5, 10, 10)).toBe(true);
        expect(intersects(5, 5, 10, 10, 0, 0, 10, 10)).toBe(true);
        expect(intersects(0, 0, 10, 10, -5, 2, 30, 3)).toBe(true);
    });

    test('intersects() with one rectangle inside the other', () => {
        expect(intersects(0, 0, 100, 100, 40, 40, 1, 1)).toBe(true);
        expect(intersects(40, 40, 1, 1, 0, 0, 100, 100)).toBe(true);
    });

    test('intersects() with separate rectangles', () => {
        expect(intersects(0, 0, 10, 10, 20, 0, 5, 5)).toBe(false);
        expect(intersects(0, 0, 10, 10, 0, 20, 5, 5)).toBe(false);
        expect(intersects(0, 0, 10, 10, -10, -10, 5, 5)).toBe(false);
    });

    test('intersects() rectangles that only share an edge do not intersect', () => {
        expect(intersects(0, 0, 10, 10, 10, 0, 10, 10)).toBe(false);
        expect(intersects(0, 0, 10, 10, 0, 10, 10, 10)).toBe(false);
    });

    test('intersects() with a zero or negative size is false', () => {
        expect(intersects(0, 0, 0, 10, 0, 0, 10, 10)).toBe(false);
        expect(intersects(0, 0, 10, 10, 2, 2, 5, 0)).toBe(false);
        expect(intersects(0, 0, 10, -10, 0, 0, 10, 10)).toBe(false);
    });
});


describe('Test VisUtil.lineIntersect2()', () => {

    test('lineIntersect2() with crossing segments', () => {
        expect(lineIntersect2(0, 0, 10, 10, 0, 10, 10, 0)).toBe(true);
        expect(lineIntersect2(10, 10, 0, 0, 10, 0, 0, 10)).toBe(true);
        expect(lineIntersect2(0, 0, 10, 0, 3, -1, 3, 1)).toBe(true);
        expect(lineIntersect2(5, 0, 5, 10, 0, 3, 10, 7)).toBe(true);
    });

    test('lineIntersect2() vertical and horizontal segments with fractional coordinates', () => {
        expect(lineIntersect2(.7, 0, .7, 1, .1, .2, .9, .5)).toBe(true);
        expect(lineIntersect2(1.1, 0, 1.1, 2, .3, .4, 1.9, 1.3)).toBe(true);
        expect(lineIntersect2(0, .7, 1, .7, .2, .1, .5, .9)).toBe(true);
        expect(lineIntersect2(0, 1.1, 2, 1.1, .4, .3, 1.3, 1.9)).toBe(true);
    });

    test('lineIntersect2() segments that touch at an end point intersect', () => {
        expect(lineIntersect2(0, 0, 10, 10, 10, 10, 20, 0)).toBe(true);
        expect(lineIntersect2(0, 0, 10, 0, 5, 0, 5, 10)).toBe(true);
    });

    test('lineIntersect2() segments whose lines cross outside the segments do not intersect', () => {
        expect(lineIntersect2(0, 0, 5, 5, 6, 0, 10, -4)).toBe(false);
        expect(lineIntersect2(0, 0, 10, 0, 3, 1, 3, 5)).toBe(false);
    });

    test('lineIntersect2() parallel or collinear segments do not intersect', () => {
        expect(lineIntersect2(0, 0, 10, 0, 0, 5, 10, 5)).toBe(false);
        expect(lineIntersect2(0, 0, 1, 1, 2, 2, 3, 3)).toBe(false);
        // collinear and overlapping is also false
        expect(lineIntersect2(0, 0, 10, 0, 5, 0, 15, 0)).toBe(false);
    });
});


describe('Test VisUtil.contains()', () => {

    test('contains() with points inside and outside the rectangle', () => {
        expect(contains(0, 0, 10, 20, 5, 5)).toBe(true);
        expect(contains(-10, -10, 5, 5, -7, -8)).toBe(true);
        expect(contains(0, 0, 10, 20, 15, 5)).toBe(false);
        expect(contains(0, 0, 10, 20, 5, 25)).toBe(false);
        expect(contains(0, 0, 10, 20, -1, 5)).toBe(false);
    });

    test('contains() includes the top and left edges but not the bottom and right edges', () => {
        expect(contains(0, 0, 10, 20, 0, 0)).toBe(true);
        expect(contains(0, 0, 10, 20, 0, 19.999)).toBe(true);
        expect(contains(0, 0, 10, 20, 10, 5)).toBe(false);
        expect(contains(0, 0, 10, 20, 5, 20)).toBe(false);
    });
});


describe('Test VisUtil.containsRec()', () => {

    test('containsRec() with a rectangle inside', () => {
        expect(containsRec(0, 0, 100, 100, 10, 10, 20, 30)).toBe(true);
        expect(containsRec(0, 0, 100, 100, 0, 0, 50, 50)).toBe(true);
    });

    test('containsRec() with a rectangle partly or fully outside', () => {
        expect(containsRec(0, 0, 100, 100, 90, 10, 20, 20)).toBe(false);
        expect(containsRec(0, 0, 100, 100, -5, 10, 20, 20)).toBe(false);
        expect(containsRec(0, 0, 100, 100, 200, 200, 5, 5)).toBe(false);
        expect(containsRec(10, 10, 5, 5, 0, 0, 100, 100)).toBe(false);
    });

    test('containsRec() a rectangle that reaches the edges is contained', () => {
        expect(containsRec(0, 0, 100, 100, 50, 50, 50, 10)).toBe(true);
        expect(containsRec(0, 0, 100, 100, 50, 50, 10, 50)).toBe(true);
        expect(containsRec(0, 0, 100, 100, 0, 0, 100, 100)).toBe(true);
        expect(containsRec(0, 0, 100, 100, 50, 50, 50.1, 10)).toBe(false);
    });
});


describe('Test VisUtil.containsCircle()', () => {

    test('containsCircle() with points inside and outside the circle', () => {
        expect(containsCircle(10, 10, 10, 10, 5)).toBe(true);
        expect(containsCircle(13, 14, 10, 10, 5.1)).toBe(true);
        expect(containsCircle(14, 14, 10, 10, 5)).toBe(false);
        expect(containsCircle(-20, 10, 10, 10, 5)).toBe(false);
    });

    test('containsCircle() a point on the circle is not contained', () => {
        expect(containsCircle(13, 14, 10, 10, 5)).toBe(false);
        expect(containsCircle(15, 10, 10, 10, 5)).toBe(false);
    });
});


describe('Test VisUtil.containsEllipse()', () => {

    test('containsEllipse() radius1 is along x and radius2 is along y', () => {
        expect(containsEllipse(18, 10, 10, 10, 10, 2)).toBe(true);
        expect(containsEllipse(10, 18, 10, 10, 10, 2)).toBe(false);
        expect(containsEllipse(10, 11.5, 10, 10, 10, 2)).toBe(true);
    });

    test('containsEllipse() with points inside and outside the ellipse', () => {
        expect(containsEllipse(0, 0, 0, 0, 4, 3)).toBe(true);
        expect(containsEllipse(2, 2, 0, 0, 4, 3)).toBe(true);
        expect(containsEllipse(3, 3, 0, 0, 4, 3)).toBe(false);
        expect(containsEllipse(-3, -2.5, 0, 0, 4, 3)).toBe(false);
    });

    test('containsEllipse() a point on the ellipse is not contained', () => {
        expect(containsEllipse(4, 0, 0, 0, 4, 3)).toBe(false);
        expect(containsEllipse(0, -3, 0, 0, 4, 3)).toBe(false);
    });

    test('containsEllipse() with equal radii is the same as containsCircle()', () => {
        [[3, 4], [3.5, 3.5], [0, 4.9], [5, 0]].forEach( ([x, y]) =>
            expect(containsEllipse(x, y, 0, 0, 5, 5)).toBe(containsCircle(x, y, 0, 0, 5)));
    });
});


describe('Test VisUtil.getArrowCoords()', () => {

    const sin20= Math.sin(20 * Math.PI / 180);
    const cos20= Math.cos(20 * Math.PI / 180);

    test('getArrowCoords() passes through the shaft and puts the first barb point at the head', () => {
        const arrow= getArrowCoords(1, 2, 30, 40);
        expect(arrow).toMatchObject({x1: 1, y1: 2, x2: 30, y2: 40, barbX1: 30, barbY1: 40, textBaseline: 'top'});
    });

    test('getArrowCoords() barb is 10 long and 20 degrees off the shaft', () => {
        const rightArrow= getArrowCoords(0, 0, 10, 0);
        expect(rightArrow.barbX2).toBeCloseTo(10 - 10*cos20, 10);
        expect(rightArrow.barbY2).toBeCloseTo(10*sin20, 10);

        const leftArrow= getArrowCoords(10, 0, 0, 0);
        expect(leftArrow.barbX2).toBeCloseTo(10*cos20, 10);
        expect(leftArrow.barbY2).toBeCloseTo(-10*sin20, 10);

        const downArrow= getArrowCoords(0, 0, 0, 10);
        expect(downArrow.barbX2).toBeCloseTo(-10*sin20, 10);
        expect(downArrow.barbY2).toBeCloseTo(10 - 10*cos20, 10);
    });

    test('getArrowCoords() barb length does not depend on the shaft length', () => {
        [[0, 0, 3, 4], [5, 5, -100, 37], [0, 0, 0, -1]].forEach( ([x1, y1, x2, y2]) => {
            const {barbX2, barbY2}= getArrowCoords(x1, y1, x2, y2);
            expect(computeScreenDistance(x2, y2, barbX2, barbY2)).toBeCloseTo(10, 10);
        });
    });

    test('getArrowCoords() text for a vertical or steep arrow is past the head', () => {
        expect(getArrowCoords(0, 0, 0, 10)).toMatchObject({textX: 0, textY: 24});
        expect(getArrowCoords(0, 10, 0, 0)).toMatchObject({textX: 0, textY: -14});
        expect(getArrowCoords(0, 0, 1, 3)).toMatchObject({textX: 1, textY: 17});
        expect(getArrowCoords(0, 0, 1, -3)).toMatchObject({textX: 1, textY: -17});
    });

    test('getArrowCoords() text for a shallow arrow is beside the head', () => {
        expect(getArrowCoords(0, 0, 10, 0)).toMatchObject({textX: 12, textY: -6});
        expect(getArrowCoords(10, 0, 0, 0)).toMatchObject({textX: -8, textY: -6});
        expect(getArrowCoords(0, 0, 10, 29)).toMatchObject({textX: 12, textY: 23});
    });
});


describe('Test VisUtil.getBoundingBox()', () => {

    test('getBoundingBox() of several points', () => {
        expect(getBoundingBox([{x: 3, y: 7}, {x: -2, y: 4}, {x: 10, y: -1}, {x: 5, y: 5}]))
            .toEqual({x: -2, y: -1, w: 12, h: 8});
    });

    test('getBoundingBox() does not depend on point order', () => {
        const pts= [makeScreenPt(1, 9), makeScreenPt(4, 2), makeScreenPt(-3, 6)];
        const expectedBox= {x: -3, y: 2, w: 7, h: 7};
        expect(getBoundingBox(pts)).toEqual(expectedBox);
        expect(getBoundingBox([...pts].reverse())).toEqual(expectedBox);
    });

    test('getBoundingBox() of one point or a line has zero size', () => {
        expect(getBoundingBox([{x: 4, y: 5}])).toEqual({x: 4, y: 5, w: 0, h: 0});
        expect(getBoundingBox([{x: 1, y: 5}, {x: 9, y: 5}])).toEqual({x: 1, y: 5, w: 8, h: 0});
    });

    test('getBoundingBox() sorts numerically, not as strings', () => {
        expect(getBoundingBox([{x: 100, y: 9}, {x: 9, y: 100}, {x: 20, y: 30}]))
            .toEqual({x: 9, y: 9, w: 91, h: 91});
    });
});


describe('Test VisUtil.calculatePosition()', () => {

    test('calculatePosition() with no offset returns the same position', () => {
        const movedWp= calculatePosition(makeWorldPt(10, 20), 0, 0);
        expect(movedWp.x).toBeCloseTo(10, 10);
        expect(movedWp.y).toBeCloseTo(20, 10);
        expect(movedWp.cSys).toBe(CoordinateSys.EQ_J2000);
    });

    test('calculatePosition() offsetDec is north in arcsec', () => {
        const northWp= calculatePosition(makeWorldPt(10, 20), 0, 3600);
        expect(northWp.x).toBeCloseTo(10, 10);
        expect(northWp.y).toBeCloseTo(21, 10);

        const southWp= calculatePosition(makeWorldPt(10, 20), 0, -3600);
        expect(southWp.x).toBeCloseTo(10, 10);
        expect(southWp.y).toBeCloseTo(19, 10);
    });

    test('calculatePosition() offsetRa is east in arcsec', () => {
        const eastWp= calculatePosition(makeWorldPt(10, 0), 3600, 0);
        expect(eastWp.x).toBeCloseTo(11, 10);
        expect(eastWp.y).toBeCloseTo(0, 10);

        const westWp= calculatePosition(makeWorldPt(10, 0), -3600, 0);
        expect(westWp.x).toBeCloseTo(9, 10);
        expect(westWp.y).toBeCloseTo(0, 10);
    });

    test('calculatePosition() offsetRa is a distance on the sky, not a change in RA', () => {
        // at dec 60, 1 degree east on the sky is about 2 degrees of RA
        const centerWp= makeWorldPt(10, 60);
        const eastWp= calculatePosition(centerWp, 3600, 0);
        expect(computeDistance(centerWp, eastWp)).toBeCloseTo(1, 10);
        expect(eastWp.x).toBeCloseTo(12, 2);
        expect(eastWp.y).toBeLessThan(60);
    });

    test('calculatePosition() distance from the center combines both offsets', () => {
        const centerWp= makeWorldPt(10, 30);
        const movedWp= calculatePosition(centerWp, 1800, 2400);
        const expectedDist= Math.acos(Math.cos(.5 * Math.PI/180) * Math.cos(2/3 * Math.PI/180)) * 180/Math.PI;
        expect(computeDistance(centerWp, movedWp)).toBeCloseTo(expectedDist, 10);
        expect(movedWp.x).toBeGreaterThan(10);
        expect(movedWp.y).toBeGreaterThan(30);
    });

    test('calculatePosition() result RA is in [0, 360) across the 0/360 line', () => {
        expect(calculatePosition(makeWorldPt(359.5, 0), 3600, 0).x).toBeCloseTo(.5, 10);
        expect(calculatePosition(makeWorldPt(.5, 0), -3600, 0).x).toBeCloseTo(359.5, 10);
    });
});


describe('Test VisUtil.transformToEastNorth()', () => {

    test('transformToEastNorth() with no rotation, x is east and y is north', () => {
        expect(transformToEastNorth(3, 4)).toEqual({east: 3, north: 4});
        expect(transformToEastNorth(-2, 5, 0)).toEqual({east: -2, north: 5});
    });

    test('transformToEastNorth() rotated 90, y is east and x is south', () => {
        const yAxis= transformToEastNorth(0, 1, 90);
        expect(yAxis.east).toBeCloseTo(1, 10);
        expect(yAxis.north).toBeCloseTo(0, 10);
        const xAxis= transformToEastNorth(1, 0, 90);
        expect(xAxis.east).toBeCloseTo(0, 10);
        expect(xAxis.north).toBeCloseTo(-1, 10);
    });

    test('transformToEastNorth() rotated 180 reverses both axes', () => {
        const {east, north}= transformToEastNorth(3, 4, 180);
        expect(east).toBeCloseTo(-3, 10);
        expect(north).toBeCloseTo(-4, 10);
    });

    test('transformToEastNorth() keeps the length of the offset', () => {
        const {east, north}= transformToEastNorth(3, 4, 30);
        expect(east).toBeCloseTo(3*Math.cos(Math.PI/6) + 4*Math.sin(Math.PI/6), 10);
        expect(north).toBeCloseTo(-3*Math.sin(Math.PI/6) + 4*Math.cos(Math.PI/6), 10);
        expect(Math.hypot(east, north)).toBeCloseTo(5, 10);
    });
});


describe('Test VisUtil.calculatePositionFromLocalOffsets()', () => {

    test('calculatePositionFromLocalOffsets() with no rotation is the same as calculatePosition()', () => {
        const centerWp= makeWorldPt(10, 30);
        const movedWp= calculatePositionFromLocalOffsets(centerWp, 1800, 2400);
        const expectedWp= calculatePosition(centerWp, 1800, 2400);
        expect(movedWp.x).toBeCloseTo(expectedWp.x, 10);
        expect(movedWp.y).toBeCloseTo(expectedWp.y, 10);
        expect(movedWp.cSys).toBe(CoordinateSys.EQ_J2000);
    });

    test('calculatePositionFromLocalOffsets() rotated 90, the y offset goes east', () => {
        const movedWp= calculatePositionFromLocalOffsets(makeWorldPt(10, 0), 0, 3600, 90);
        expect(movedWp.x).toBeCloseTo(11, 10);
        expect(movedWp.y).toBeCloseTo(0, 10);
    });

    test('calculatePositionFromLocalOffsets() rotated 90, the x offset goes south', () => {
        const movedWp= calculatePositionFromLocalOffsets(makeWorldPt(10, 20), 3600, 0, 90);
        expect(movedWp.x).toBeCloseTo(10, 10);
        expect(movedWp.y).toBeCloseTo(19, 10);
    });

    test('calculatePositionFromLocalOffsets() returns the center coordinate system', () => {
        // the offsets are along equatorial east and north, then the result is converted back to galactic
        const centerWp= makeWorldPt(10, 20, CoordinateSys.GALACTIC);
        const movedWp= calculatePositionFromLocalOffsets(centerWp, 3600, 0);
        expect(movedWp.cSys).toBe(CoordinateSys.GALACTIC);
        expect(computeDistance(centerWp, movedWp)).toBeCloseTo(1, 5);
    });
});


describe('Test VisUtil.isAngleUnit()', () => {

    test('isAngleUnit() with angle units, ignoring case', () => {
        ['deg', 'degree', 'arcmin', 'arcsec', 'radian', 'rad', 'DEG', 'ArcSec']
            .forEach( (unit) => expect(isAngleUnit(unit)).toBe(true));
    });

    test('isAngleUnit() with other strings', () => {
        ['', 'degrees', 'arcminute', 'mas', 'hour', 'meter']
            .forEach( (unit) => expect(isAngleUnit(unit)).toBe(false));
    });
});


describe('Test VisUtil.convertAngle()', () => {

    test('convertAngle() between degree, arcmin and arcsec', () => {
        expect(convertAngle('deg', 'arcmin', 2)).toEqual(120);
        expect(convertAngle('degree', 'arcsec', 1)).toEqual(3600);
        expect(convertAngle('arcmin', 'arcsec', 1.5)).toEqual(90);
        expect(convertAngle('arcsec', 'deg', 36)).toBeCloseTo(.01, 12);
        expect(convertAngle('arcmin', 'degree', 30)).toBeCloseTo(.5, 12);
    });

    test('convertAngle() to and from radians', () => {
        expect(convertAngle('rad', 'deg', Math.PI)).toBeCloseTo(180, 10);
        expect(convertAngle('deg', 'radian', 90)).toBeCloseTo(Math.PI/2, 12);
        expect(convertAngle('arcmin', 'rad', 60)).toBeCloseTo(Math.PI/180, 12);
        expect(convertAngle('arcsec', 'rad', 3600)).toBeCloseTo(Math.PI/180, 12);
        expect(convertAngle('rad', 'arcsec', 1, false)).toBeCloseTo(206264.806, 3);
        expect(convertAngle('radian', 'rad', 2)).toBeCloseTo(2, 12);
    });

    test('convertAngle() units ignore case', () => {
        expect(convertAngle('DEG', 'ArcMin', 2)).toEqual(120);
        expect(convertAngle('Rad', 'DEGREE', Math.PI)).toBeCloseTo(180, 10);
    });

    test('convertAngle() parses a string angle', () => {
        expect(convertAngle('deg', 'arcmin', '2.5')).toEqual(150);
        expect(convertAngle('degree', 'deg', '2.5')).toEqual(2.5);
    });

    test('convertAngle() limits arcsec to 1 decimal place and arcmin to 4 by rounding', () => {
        expect(convertAngle('deg', 'arcsec', .0001)).toEqual(.4);
        expect(convertAngle('deg', 'arcmin', .123456789)).toEqual(7.4074);
        expect(convertAngle('deg', 'arcsec', .0001, false)).toBeCloseTo(.36, 12);
        expect(convertAngle('deg', 'arcmin', .123456789, false)).toBeCloseTo(7.40740734, 8);
    });

    test('convertAngle() rounding ignores the case of the target unit', () => {
        expect(convertAngle('deg', 'ARCSEC', .0001)).toEqual(.4);
        expect(convertAngle('deg', 'ArcMin', .123456789)).toEqual(7.4074);
        expect(convertAngle('deg', 'ARCSEC', .0001, false)).toBeCloseTo(.36, 12);
    });

    test('convertAngle() with an unknown unit returns the angle unchanged', () => {
        expect(convertAngle('foo', 'deg', 5)).toEqual(5);
        expect(convertAngle('deg', 'foo', '5')).toEqual(5);
    });
});


describe('Test VisUtil.getLinePointAry()', () => {

    const toXYAry= (ptAry) => ptAry.map( ({x, y}) => [x, y]);

    test('getLinePointAry() returns one image point per pixel along the longer axis', () => {
        const ptAry= getLinePointAry({x: 0, y: 5}, {x: 4, y: 5});
        expect(toXYAry(ptAry)).toEqual([[0, 5], [1, 5], [2, 5], [3, 5], [4, 5]]);
        ptAry.forEach( (pt) => expect(pt.type).toEqual(Point.IM_PT));
    });

    test('getLinePointAry() with a vertical line', () => {
        expect(toXYAry(getLinePointAry({x: 2, y: 1}, {x: 2, y: 4}))).toEqual([[2, 1], [2, 2], [2, 3], [2, 4]]);
    });

    test('getLinePointAry() with shallow, diagonal and steep lines', () => {
        expect(toXYAry(getLinePointAry({x: 0, y: 0}, {x: 4, y: 1})))
            .toEqual([[0, 0], [1, 0], [2, 0], [3, 0], [4, 1]]);
        expect(toXYAry(getLinePointAry({x: 0, y: 0}, {x: 3, y: 3})))
            .toEqual([[0, 0], [1, 1], [2, 2], [3, 3]]);
        expect(toXYAry(getLinePointAry({x: 0, y: 0}, {x: 1, y: 4})))
            .toEqual([[0, 0], [0, 1], [0, 2], [0, 3], [1, 4]]);
    });

    test('getLinePointAry() points are in increasing order whatever the direction of the line', () => {
        expect(getLinePointAry({x: 1, y: 4}, {x: 0, y: 0})).toEqual(getLinePointAry({x: 0, y: 0}, {x: 1, y: 4}));
        expect(getLinePointAry({x: 4, y: 5}, {x: 0, y: 5})).toEqual(getLinePointAry({x: 0, y: 5}, {x: 4, y: 5}));
    });

    test('getLinePointAry() fractional end points are rounded down to whole pixels', () => {
        expect(toXYAry(getLinePointAry({x: .7, y: 2.2}, {x: 3.2, y: 2.9})))
            .toEqual([[0, 2], [1, 2], [2, 2], [3, 2]]);
    });

    test('getLinePointAry() negative coordinates are rounded down, not toward zero', () => {
        // at x=-2 and x=-1 the line is at y=-0.67 and y=-0.33, which are in pixel row -1
        expect(toXYAry(getLinePointAry({x: -3, y: -1}, {x: 0, y: 0})))
            .toEqual([[-3, -1], [-2, -1], [-1, -1], [0, 0]]);
        // a line starting at x=-0.5 starts in pixel column -1
        expect(toXYAry(getLinePointAry({x: -.5, y: 2}, {x: 3, y: 2})))
            .toEqual([[-1, 2], [0, 2], [1, 2], [2, 2], [3, 2]]);
    });

    test('getLinePointAry() with the same point twice returns undefined', () => {
        expect(getLinePointAry({x: 2, y: 2}, {x: 2, y: 2})).toBeUndefined();
    });
});


describe('Test VisUtil.getPointOnEllipse()', () => {

    test('getPointOnEllipse() at the four axis angles', () => {
        // rx is along x and ry is along y, angle is in radians counterclockwise from +x
        const expectedPts= [[0, 15, 20], [Math.PI/2, 10, 22], [Math.PI, 5, 20], [3*Math.PI/2, 10, 18]];
        expectedPts.forEach( ([angle, expectedX, expectedY]) => {
            const {x, y}= getPointOnEllipse(10, 20, 5, 2, angle);
            expect(x).toBeCloseTo(expectedX, 10);
            expect(y).toBeCloseTo(expectedY, 10);
        });
    });

    test('getPointOnEllipse() points satisfy the ellipse equation', () => {
        [.3, 1.1, 2.5, 4, 5.9].forEach( (angle) => {
            const {x, y}= getPointOnEllipse(10, 20, 5, 2, angle);
            expect(((x-10)/5)**2 + ((y-20)/2)**2).toBeCloseTo(1, 10);
        });
    });

    test('getPointOnEllipse() uses the absolute value of the radii', () => {
        const {x, y}= getPointOnEllipse(0, 0, -5, -2, Math.PI/4);
        const expectedPt= getPointOnEllipse(0, 0, 5, 2, Math.PI/4);
        expect(x).toBeCloseTo(expectedPt.x, 10);
        expect(y).toBeCloseTo(expectedPt.y, 10);
    });

    test('getPointOnEllipse() with equal radii is a circle', () => {
        const {x, y}= getPointOnEllipse(0, 0, 3, 3, Math.PI/6);
        expect(x).toBeCloseTo(3*Math.cos(Math.PI/6), 10);
        expect(y).toBeCloseTo(1.5, 10);
    });
});


describe('Test VisUtil.getEllipseArcEndPoints()', () => {

    // getEllipseArcEndPoints() is the exported way to reach the private getEllipsePointForAngle() with a rotation

    test('getEllipseArcEndPoints() without rotation matches getPointOnEllipse()', () => {
        const {x1, y1, x2, y2}= getEllipseArcEndPoints(10, 20, 5, 2, .4, 1.2);
        const startPt= getPointOnEllipse(10, 20, 5, 2, .4);
        const endPt= getPointOnEllipse(10, 20, 5, 2, 1.6);
        expect(x1).toBeCloseTo(startPt.x, 10);
        expect(y1).toBeCloseTo(startPt.y, 10);
        expect(x2).toBeCloseTo(endPt.x, 10);
        expect(y2).toBeCloseTo(endPt.y, 10);
    });

    test('getEllipseArcEndPoints() rotating the ellipse 90 degrees swaps its axes', () => {
        const {x1, y1, x2, y2}= getEllipseArcEndPoints(0, 0, 5, 2, 0, Math.PI/2, Math.PI/2);
        expect(x1).toBeCloseTo(0, 10);
        expect(y1).toBeCloseTo(5, 10);
        expect(x2).toBeCloseTo(-2, 10);
        expect(y2).toBeCloseTo(0, 10);
    });

    test('getEllipseArcEndPoints() large arc and sweep flags', () => {
        expect(getEllipseArcEndPoints(0, 0, 5, 2, 0, Math.PI/2)).toMatchObject({fa: 0, fs: 1});
        expect(getEllipseArcEndPoints(0, 0, 5, 2, 0, -3*Math.PI/2)).toMatchObject({fa: 1, fs: 0});
    });
});


describe('Test VisUtil.normalizeRotation()', () => {

    test('normalizeRotation() keeps the angle between -360 and 360', () => {
        expect(normalizeRotation(45.5)).toEqual(45.5);
        expect(normalizeRotation(370)).toEqual(10);
        expect(normalizeRotation(-370)).toEqual(-10);
        expect(normalizeRotation(720)).toEqual(0);
        expect(normalizeRotation(360)).toEqual(0);
    });

    test('normalizeRotation() parses a string', () => {
        expect(normalizeRotation('45')).toEqual(45);
        expect(normalizeRotation('400')).toEqual(40);
    });

    test('normalizeRotation() with a missing or non-finite value returns 0', () => {
        [undefined, null, NaN, 'abc', Infinity, -Infinity]
            .forEach( (rotation) => expect(normalizeRotation(rotation)).toEqual(0));
        expect(normalizeRotation()).toEqual(0);
    });
});


// a stand-in for a CysConverter: screen points pass through, other points are scaled by 2
const scaleByTwoConverter= {
    getScreenCoords: (pt) => pt.type===Point.SPT ? pt : makeScreenPt(pt.x*2, pt.y*2)
};


// like scaleByTwoConverter, but a point with a negative x can't be converted, like a point off the projection
const partialConverter= {
    getScreenCoords: (pt) => pt.x<0 ? null : scaleByTwoConverter.getScreenCoords(pt)
};

describe('Test VisUtil.distToLine()', () => {

    const segment= [{x: 0, y: 0}, {x: 10, y: 0}];

    test('distToLine() with a point beside the segment is the perpendicular distance', () => {
        expect(distToLine(segment, undefined, {x: 5, y: 3})).toBeCloseTo(3, 10);
        expect(distToLine(segment, undefined, {x: 2, y: -4})).toBeCloseTo(4, 10);
        expect(distToLine([{x: 0, y: 0}, {x: 10, y: 10}], undefined, {x: 0, y: 10})).toBeCloseTo(Math.sqrt(50), 10);
    });

    test('distToLine() with a point past either end is the distance to that end', () => {
        expect(distToLine(segment, undefined, {x: 13, y: 4})).toBeCloseTo(5, 10);
        expect(distToLine(segment, undefined, {x: -3, y: -4})).toBeCloseTo(5, 10);
        expect(distToLine(segment, undefined, {x: 0, y: 3})).toBeCloseTo(3, 10);
    });

    test('distToLine() with a point on the segment is 0', () => {
        expect(distToLine(segment, undefined, {x: 4, y: 0})).toBeCloseTo(0, 10);
        expect(distToLine(segment, undefined, {x: 10, y: 0})).toBeCloseTo(0, 10);
    });

    test('distToLine() with a zero length segment is the distance to that point', () => {
        expect(distToLine([{x: 2, y: 2}, {x: 2, y: 2}], undefined, {x: 5, y: 6})).toBeCloseTo(5, 10);
    });

    test('distToLine() with a converter measures in screen coordinates', () => {
        const imSegment= [makeImagePt(0, 0), makeImagePt(10, 0)];
        expect(distToLine(imSegment, scaleByTwoConverter, makeImagePt(5, 3))).toBeCloseTo(6, 10);
    });

    test('distToLine() with a point that cannot be converted to screen coordinates returns NaN', () => {
        const imSegment= [makeImagePt(0, 0), makeImagePt(10, 0)];
        expect(distToLine(imSegment, partialConverter, makeImagePt(-5, 3))).toBeNaN();
        expect(distToLine([makeImagePt(-1, 0), makeImagePt(10, 0)], partialConverter, makeImagePt(5, 3))).toBeNaN();
        expect(distToLine([makeImagePt(0, 0), makeImagePt(-10, 0)], partialConverter, makeImagePt(5, 3))).toBeNaN();
    });

    test('distToLine() with a missing point returns NaN', () => {
        expect(distToLine(segment, undefined, undefined)).toBeNaN();
        expect(distToLine(undefined, undefined, {x: 1, y: 1})).toBeNaN();
        expect(distToLine([{x: 0, y: 0}], undefined, {x: 1, y: 1})).toBeNaN();
    });
});


describe('Test VisUtil.distanceToPolygon()', () => {

    const square= [{x: 0, y: 0}, {x: 10, y: 0}, {x: 10, y: 10}, {x: 0, y: 10}];

    test('distanceToPolygon() with a point inside is the distance to the nearest edge', () => {
        expect(distanceToPolygon(square, undefined, {x: 5, y: 5})).toBeCloseTo(5, 10);
        expect(distanceToPolygon(square, undefined, {x: 5, y: 2})).toBeCloseTo(2, 10);
        expect(distanceToPolygon(square, undefined, {x: 9, y: 4})).toBeCloseTo(1, 10);
    });

    test('distanceToPolygon() with a point outside', () => {
        expect(distanceToPolygon(square, undefined, {x: 15, y: 5})).toBeCloseTo(5, 10);
        expect(distanceToPolygon(square, undefined, {x: 13, y: 14})).toBeCloseTo(5, 10);
    });

    test('distanceToPolygon() with a point on an edge is 0', () => {
        expect(distanceToPolygon(square, undefined, {x: 0, y: 4})).toBeCloseTo(0, 10);
    });

    test('distanceToPolygon() includes the edge from the last point back to the first', () => {
        const triangle= [{x: 0, y: 0}, {x: 10, y: 0}, {x: 0, y: 10}];
        // nearest to the closing edge (0,10)-(0,0)
        expect(distanceToPolygon(triangle, undefined, {x: 1, y: 5})).toBeCloseTo(1, 10);
    });

    test('distanceToPolygon() with a converter measures in screen coordinates', () => {
        const imSquare= square.map( ({x, y}) => makeImagePt(x, y));
        expect(distanceToPolygon(imSquare, scaleByTwoConverter, makeImagePt(5, 2))).toBeCloseTo(4, 10);
    });

    test('distanceToPolygon() with fewer than 3 points returns Number.MAX_VALUE', () => {
        expect(distanceToPolygon([{x: 0, y: 0}, {x: 10, y: 0}], undefined, {x: 5, y: 5})).toEqual(Number.MAX_VALUE);
    });
});


describe('Test VisUtil.toRadians() and toDegrees()', () => {

    test('toRadians() converts degrees to radians', () => {
        expect(toRadians(0)).toEqual(0);
        expect(toRadians(180)).toBeCloseTo(Math.PI, 12);
        expect(toRadians(90)).toBeCloseTo(Math.PI/2, 12);
        expect(toRadians(-45)).toBeCloseTo(-Math.PI/4, 12);
        expect(toRadians(720)).toBeCloseTo(4*Math.PI, 12);
    });

    test('toDegrees() converts radians to degrees', () => {
        expect(toDegrees(0)).toEqual(0);
        expect(toDegrees(Math.PI)).toBeCloseTo(180, 12);
        expect(toDegrees(-Math.PI/2)).toBeCloseTo(-90, 12);
        expect(toDegrees(1)).toBeCloseTo(57.29577951, 8);
    });

    test('toDegrees() undoes toRadians()', () => {
        [0, 12.5, -270, 359.999].forEach( (angle) => expect(toDegrees(toRadians(angle))).toBeCloseTo(angle, 10));
    });
});


describe('Test VisUtil.findIntersectionPt()', () => {

    test('findIntersectionPt() with crossing segments', () => {
        expect(findIntersectionPt(0, 0, 10, 10, 0, 10, 10, 0)).toEqual({x: 5, y: 5, onSeg1: true, onSeg2: true});
        const result= findIntersectionPt(0, 0, 4, 0, 1, -1, 1, 3);
        expect(result.x).toBeCloseTo(1, 10);
        expect(result.y).toBeCloseTo(0, 10);
        expect(result).toMatchObject({onSeg1: true, onSeg2: true});
    });

    test('findIntersectionPt() when the lines cross outside one or both segments', () => {
        // lines cross at (5,0), past the end of the first segment but on the second
        expect(findIntersectionPt(0, 0, 1, 0, 5, -1, 5, 1)).toEqual({x: 5, y: 0, onSeg1: false, onSeg2: true});
        // lines cross at (5,0), past the end of both segments
        expect(findIntersectionPt(0, 0, 1, 0, 5, 3, 5, 4)).toMatchObject({x: 5, y: 0, onSeg1: false, onSeg2: false});
    });

    test('findIntersectionPt() segments that touch at an end point are on both segments', () => {
        expect(findIntersectionPt(0, 0, 10, 0, 10, 0, 10, 10)).toEqual({x: 10, y: 0, onSeg1: true, onSeg2: true});
    });

    test('findIntersectionPt() with parallel or collinear segments returns false', () => {
        expect(findIntersectionPt(0, 0, 10, 0, 0, 5, 10, 5)).toBe(false);
        expect(findIntersectionPt(0, 0, 10, 0, 5, 0, 15, 0)).toBe(false);
    });
});


describe('Test VisUtil.doSegmentsCross()', () => {

    test('doSegmentsCross() with crossing segments', () => {
        expect(doSegmentsCross(0, 0, 10, 10, 0, 10, 10, 0)).toBe(true);
        expect(doSegmentsCross(0, 0, 10, 0, 5, -5, 5, 5)).toBe(true);
    });

    test('doSegmentsCross() when the lines cross outside both segments', () => {
        expect(doSegmentsCross(0, 0, 1, 0, 5, 3, 5, 4)).toBe(false);
    });

    test('doSegmentsCross() when the lines cross on only one segment', () => {
        // the lines meet at (5,0), which is on the second segment but past the end of the first
        expect(doSegmentsCross(0, 0, 1, 0, 5, -1, 5, 1)).toBe(false);
        expect(doSegmentsCross(5, -1, 5, 1, 0, 0, 1, 0)).toBe(false);
    });

    test('doSegmentsCross() with parallel segments', () => {
        expect(doSegmentsCross(0, 0, 10, 0, 0, 5, 10, 5)).toBe(false);
    });
});


describe('Test VisUtil.lineCrossesRect()', () => {

    // the rectangle is x 10 to 20, y 0 to 10

    test('lineCrossesRect() with an end point inside the rectangle', () => {
        expect(lineCrossesRect(12, 5, 14, 6, 10, 0, 10, 10)).toBe(true);
        expect(lineCrossesRect(0, 5, 15, 5, 10, 0, 10, 10)).toBe(true);
        expect(lineCrossesRect(10, 0, -5, -5, 10, 0, 10, 10)).toBe(true);
    });

    test('lineCrossesRect() with a line through the rectangle', () => {
        expect(lineCrossesRect(0, 5, 30, 5, 10, 0, 10, 10)).toBe(true);
        expect(lineCrossesRect(15, -5, 15, 15, 10, 0, 10, 10)).toBe(true);
        expect(lineCrossesRect(5, -5, 25, 15, 10, 0, 10, 10)).toBe(true);
    });

    test('lineCrossesRect() with a line that does not point toward the rectangle', () => {
        expect(lineCrossesRect(0, 20, 2, 30, 10, 0, 10, 10)).toBe(false);
    });

    test('lineCrossesRect() with a line that points toward the rectangle but stops short', () => {
        expect(lineCrossesRect(0, 5, 2, 5, 10, 0, 10, 10)).toBe(false);
        expect(lineCrossesRect(0, 20, 30, 20, 10, 0, 10, 10)).toBe(false);
        expect(lineCrossesRect(15, 30, 15, 12, 10, 0, 10, 10)).toBe(false);
    });
});


describe('Test VisUtil.segmentIntersectRect()', () => {

    const corners= [{x: 10, y: 0}, {x: 20, y: 0}, {x: 20, y: 10}, {x: 10, y: 10}];

    test('segmentIntersectRect() with an end point inside the rectangle', () => {
        expect(segmentIntersectRect({x: 12, y: 5}, {x: 14, y: 6}, corners)).toBe(true);
        expect(segmentIntersectRect({x: 0, y: 5}, {x: 15, y: 5}, corners)).toBe(true);
    });

    test('segmentIntersectRect() with a segment through the rectangle', () => {
        expect(segmentIntersectRect({x: 0, y: 5}, {x: 30, y: 5}, corners)).toBe(true);
        expect(segmentIntersectRect({x: 5, y: -5}, {x: 25, y: 15}, corners)).toBe(true);
    });

    test('segmentIntersectRect() with a segment that touches an edge or corner', () => {
        expect(segmentIntersectRect({x: 5, y: 0}, {x: 10, y: 0}, corners)).toBe(true);
        expect(segmentIntersectRect({x: 0, y: -10}, {x: 20, y: 10}, corners)).toBe(true);
    });

    test('segmentIntersectRect() with a segment outside the rectangle', () => {
        expect(segmentIntersectRect({x: 0, y: 5}, {x: 2, y: 5}, corners)).toBe(false);
        expect(segmentIntersectRect({x: 0, y: 0}, {x: 10, y: -5}, corners)).toBe(false);
        expect(segmentIntersectRect({x: 0, y: 20}, {x: 30, y: 20}, corners)).toBe(false);
    });

    test('segmentIntersectRect() corner order does not matter', () => {
        const reorderedCorners= [corners[2], corners[0], corners[3], corners[1]];
        expect(segmentIntersectRect({x: 12, y: 5}, {x: 14, y: 6}, reorderedCorners)).toBe(true);
        expect(segmentIntersectRect({x: 0, y: 5}, {x: 2, y: 5}, reorderedCorners)).toBe(false);
    });
});


describe('Test VisUtil.pointInRec()', () => {

    const corners= [{x: 10, y: 0}, {x: 20, y: 0}, {x: 20, y: 10}, {x: 10, y: 10}];

    test('pointInRec() with points inside, on the edge and outside', () => {
        expect(pointInRec({x: 15, y: 5}, corners)).toBe(true);
        expect(pointInRec({x: 10, y: 0}, corners)).toBe(true);
        expect(pointInRec({x: 20, y: 7}, corners)).toBe(true);
        expect(pointInRec({x: 9.9, y: 5}, corners)).toBe(false);
        expect(pointInRec({x: 15, y: 10.1}, corners)).toBe(false);
    });

    test('pointInRec() uses the bounding box of the corners in any order', () => {
        expect(pointInRec({x: 15, y: 5}, [corners[2], corners[0]])).toBe(true);
        expect(pointInRec({x: 25, y: 5}, [corners[3], corners[1], corners[0]])).toBe(false);
    });
});
