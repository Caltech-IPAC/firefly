import CoordinateSys from '../../../CoordSys.js';
import CysConverter from '../../../CsysConverter.js';
import * as HiPSUtil from '../../../HiPSUtil.js';
import {makeImagePt, makeWorldPt} from '../../../Point.js';
import {makeExtractPtAry, makeHiPSLineExtractionTitle, makeHiPSLinePtAry} from '../ExtractionUIUtil.js';


/*
 * The tests stub the coordinate converter and the HiPS tile lookup so the results can be computed by hand:
 *   - an image point (x,y) is the world point (x/1000, y/1000), so 1 image unit is .001 degrees.
 *     An image point with x<0 has no world point, a world point with x<0 has no image point.
 *   - a FITS tile pixel is .001 degrees, and covers 2x2 image units, so tile pixel= (floor(x/2), floor(y/2))
 *   - each tile is 100 image units wide, there is no tile at y >= 50
 */
const IMAGE_SCALE= 1000;
const isWorldPt= (pt) => pt.type===makeWorldPt(0,0).type;

const stubConverter= () => {
    jest.spyOn(CysConverter, 'make').mockImplementation( () => ({
        getWorldCoords: (pt) => {
            if (isWorldPt(pt)) return pt;
            return pt.x<0 ? null : makeWorldPt(pt.x/IMAGE_SCALE, pt.y/IMAGE_SCALE, CoordinateSys.EQ_J2000);
        },
        getImageCoords: (pt) => {
            if (!isWorldPt(pt)) return pt;
            return pt.x<0 ? null : makeImagePt(pt.x*IMAGE_SCALE+.4, pt.y*IMAGE_SCALE+.7);
        },
    }));
};

const stubTiles= () => {
    jest.spyOn(HiPSUtil, 'getTilePixelAngSize').mockReturnValue(1/IMAGE_SCALE);
    jest.spyOn(HiPSUtil, 'getHiPSFitsTileLocation').mockImplementation( (plot, wpt, norder) => {
        const x= Math.round(wpt.x*IMAGE_SCALE);
        const y= Math.round(wpt.y*IMAGE_SCALE);
        if (y>=50) return {};
        const ipix= Math.floor(x/100);
        return {norder, ipix, tileUrl: `http://hips/Norder${norder}/Npix${ipix}.fits`,
            tilePt: makeImagePt(Math.floor(x/2), Math.floor(y/2))};
    });
};

const makeHiPSPlot= ({cubeDepth=1, cubeIdx=0}={}) =>
    ({plotId:'hips', plotType:'hips', plotImageId:'hips-image', cubeDepth, cubeIdx, hipsProperties:{}});

const makeImagePlot= (wcs) => ({
    plotId:'image', plotType:'image', plotImageId:'image-0', header:{},
    projection: wcs ? {isSpecified: () => true, isImplemented: () => true, coordSys: {isCelestial: () => true}} : undefined,
});

const tileKey= ({tileUrl,tilePt}) => `${tileUrl};${tilePt.x},${tilePt.y}`;

beforeEach(() => {
    stubConverter();
    stubTiles();
});
afterEach(() => jest.restoreAllMocks());


describe('Test ExtractionUIUtil.makeHiPSLinePtAry()', () => {
    const NORDER= 5;

    test('points are about one tile pixel apart and a point in the same tile pixel is skipped', () => {
        // 10 image units is .01 deg, so 10 steps, the tile pixel changes every 2 steps
        const {imPtAry, hipsTileLocAry, hipsWptAry}=
            makeHiPSLinePtAry(makeHiPSPlot(), makeImagePt(0,0), makeImagePt(10,0), NORDER);
        expect(imPtAry.map( (pt) => pt.x)).toEqual([0,2,4,6,8,10]);
        expect(hipsTileLocAry.map( ({tilePt}) => tilePt.x)).toEqual([0,1,2,3,4,5]);
        expect(hipsTileLocAry.every( (tileLoc) => tileLoc.norder===NORDER)).toBe(true);
        expect(hipsWptAry.map( (wpt) => wpt.x)).toEqual([0,.002,.004,.006,.008,.01]);
    });

    test('the three arrays are in the same order', () => {
        const {imPtAry, hipsTileLocAry, hipsWptAry}=
            makeHiPSLinePtAry(makeHiPSPlot(), makeImagePt(90,10), makeImagePt(120,30), NORDER);
        expect(imPtAry.length).toBeGreaterThan(1);
        expect(hipsTileLocAry.length).toBe(imPtAry.length);
        expect(hipsWptAry.length).toBe(imPtAry.length);
        imPtAry.forEach( (imPt,i) => {
            expect(hipsWptAry[i].x).toBeCloseTo(imPt.x/IMAGE_SCALE, 9);
            expect(hipsWptAry[i].y).toBeCloseTo(imPt.y/IMAGE_SCALE, 9);
            expect(hipsTileLocAry[i].tilePt.x).toBe(Math.floor(Math.round(imPt.x)/2));
        });
    });

    test('a line that crosses into another tile has points in both tiles', () => {
        const {hipsTileLocAry}= makeHiPSLinePtAry(makeHiPSPlot(), makeImagePt(90,0), makeImagePt(110,0), NORDER);
        expect(new Set(hipsTileLocAry.map( ({tileUrl}) => tileUrl)))
            .toEqual(new Set([`http://hips/Norder${NORDER}/Npix0.fits`, `http://hips/Norder${NORDER}/Npix1.fits`]));
    });

    test('no two points in a row are in the same tile pixel', () => {
        const {hipsTileLocAry}= makeHiPSLinePtAry(makeHiPSPlot(), makeImagePt(0,0), makeImagePt(37,23), NORDER);
        hipsTileLocAry.slice(1).forEach( (tileLoc,i) => expect(tileKey(tileLoc)).not.toBe(tileKey(hipsTileLocAry[i])));
    });

    test('points that are not in a tile are skipped', () => {
        // there is no tile at y>=50, so the line from y=0 to y=100 only has points in tile pixel rows 0 to 24
        const {imPtAry, hipsTileLocAry}= makeHiPSLinePtAry(makeHiPSPlot(), makeImagePt(0,0), makeImagePt(0,100), NORDER);
        expect(imPtAry.every( (pt) => Math.round(pt.y)<50)).toBe(true);
        expect(hipsTileLocAry.map( ({tilePt}) => tilePt.y)).toEqual([...Array(25)].map( (v,i) => i));
    });

    test('a line with a start or end point outside the projection has no points', () => {
        const empty= {imPtAry:[], hipsTileLocAry:[], hipsWptAry:[]};
        expect(makeHiPSLinePtAry(makeHiPSPlot(), makeImagePt(-1,0), makeImagePt(10,0), NORDER)).toEqual(empty);
        expect(makeHiPSLinePtAry(makeHiPSPlot(), makeImagePt(0,0), makeImagePt(-10,0), NORDER)).toEqual(empty);
    });

    test('a line with the same start and end point has one point', () => {
        const {imPtAry}= makeHiPSLinePtAry(makeHiPSPlot(), makeImagePt(4,4), makeImagePt(4,4), NORDER);
        expect(imPtAry).toEqual([makeImagePt(4,4)]);
    });

    test('a long line is limited in the number of points', () => {
        // 20000 image units would be 10000 tile pixels without the limit
        const {imPtAry}= makeHiPSLinePtAry(makeHiPSPlot(), makeImagePt(0,0), makeImagePt(20000,0), NORDER);
        expect(imPtAry.length).toBeLessThan(10000);
        expect(imPtAry[0].x).toBe(0);
        expect(imPtAry[imPtAry.length-1].x).toBe(20000);
    });
});


describe('Test ExtractionUIUtil.makeHiPSLineExtractionTitle()', () => {
    const wptAry= [makeWorldPt(10.123456,-20.5), makeWorldPt(11,-21.25)];
    test('HiPS that is not a cube', () =>
        expect(makeHiPSLineExtractionTitle(makeHiPSPlot(), wptAry))
            .toBe('Line Extract Preview - (10.12346,-20.50000) to (11.00000,-21.25000)'));
    test('HiPS cube shows the plane', () =>
        expect(makeHiPSLineExtractionTitle(makeHiPSPlot({cubeDepth:3, cubeIdx:1}), wptAry))
            .toBe('Line Extract Preview - Plane #2 - (10.12346,-20.50000) to (11.00000,-21.25000)'));
    test('one point', () =>
        expect(makeHiPSLineExtractionTitle(makeHiPSPlot(), [wptAry[0]]))
            .toBe('Line Extract Preview - (10.12346,-20.50000) to (10.12346,-20.50000)'));
});


describe('Test ExtractionUIUtil.makeExtractPtAry()', () => {
    const NORDER= 3;

    test('undefined plot', () => expect(makeExtractPtAry(undefined, [makeWorldPt(1,1)], NORDER))
        .toEqual({foundPts:false, imPtAry:[]}));

    test('HiPS, the image point is truncated and the arrays are in the same order', () => {
        const ptAry= [makeWorldPt(.004,.002), makeWorldPt(.0101,.0203)];
        const {foundPts, imPtAry, hipsTileLocAry, wptAry}= makeExtractPtAry(makeHiPSPlot(), ptAry, NORDER);
        expect(foundPts).toBe(true);
        expect(imPtAry).toEqual([makeImagePt(4,2), makeImagePt(10,20)]);
        expect(wptAry).toEqual(ptAry);
        expect(hipsTileLocAry.map( ({tilePt}) => [tilePt.x,tilePt.y])).toEqual([[2,1],[5,10]]);
        expect(hipsTileLocAry.every( (tileLoc) => tileLoc.norder===NORDER)).toBe(true);
    });

    test('HiPS, points outside the projection or not in a tile are removed', () => {
        const ptAry= [makeWorldPt(-.001,0), makeWorldPt(.006,.004), makeWorldPt(.001,.08)];
        const {foundPts, imPtAry, hipsTileLocAry, wptAry}= makeExtractPtAry(makeHiPSPlot(), ptAry, NORDER);
        expect(foundPts).toBe(true);
        expect(imPtAry).toEqual([makeImagePt(6,4)]);
        expect(wptAry).toEqual([ptAry[1]]);
        expect(hipsTileLocAry.length).toBe(1);
    });

    test('HiPS, no points in a tile', () => {
        const {foundPts, imPtAry, hipsTileLocAry, wptAry}= makeExtractPtAry(makeHiPSPlot(), [makeWorldPt(.001,.08)], NORDER);
        expect(foundPts).toBe(false);
        expect(imPtAry).toEqual([]);
        expect(hipsTileLocAry).toEqual([]);
        expect(wptAry).toEqual([]);
    });

    test('image with a WCS, the world points are in the same order as the image points', () => {
        const ptAry= [makeWorldPt(.004,.002), makeImagePt(-5,3), makeWorldPt(.0101,.0203)];
        const {foundPts, imPtAry, hipsTileLocAry, wptAry}= makeExtractPtAry(makeImagePlot(true), ptAry, NORDER);
        expect(foundPts).toBe(true);
        expect(imPtAry).toEqual([makeImagePt(4,2), makeImagePt(10,20)]); // the point with no world point is removed
        expect(wptAry).toEqual([ptAry[0], ptAry[2]]);
        expect(hipsTileLocAry).toBeUndefined();
    });

    test('image without a WCS has no world points', () => {
        const ptAry= [makeImagePt(3.9,7.2), makeImagePt(-5,3)];
        const {foundPts, imPtAry, wptAry}= makeExtractPtAry(makeImagePlot(false), ptAry, NORDER);
        expect(foundPts).toBe(true);
        expect(imPtAry).toEqual([makeImagePt(3,7), makeImagePt(-5,3)]);
        expect(wptAry).toBeUndefined();
    });

    test('no points', () => {
        expect(makeExtractPtAry(makeImagePlot(false), [], NORDER).foundPts).toBe(false);
        expect(makeExtractPtAry(makeHiPSPlot(), [], NORDER).foundPts).toBe(false);
    });
});
