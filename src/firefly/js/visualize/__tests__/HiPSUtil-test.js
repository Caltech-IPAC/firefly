import {
    API_HIPS_CONSTANTS, getAllVisibleHiPSCells, getCatalogNorderlevel, getCornersForCell, getHealpixCellAtNorder,
    getHealpixCornerTool, getHealpixPixelAtNorder, getHiPSFitsTileLocation, getHiPSNorderlevel,
    getHipsPixelCutRangeValues, getHiPSTileExt,
    getMaxDisplayableHiPSGridLevel, getPlotTilePixelAngSize, getProGradeTilePixels, getPropertyItem,
    getTilePixelAngSize, getVisibleHiPSCells, isHiPSFitsCube, isTileInside, loadImageMultiCall,
    makeHiPSAllSkyUrl, makeHiPSAllSkyUrlFromPlot, makeHipsFitsTilePath, makeHiPSPropertiesUrl, makeHiPSTileUrl,
    makeHipsUrl, MAX_SUPPORTED_HIPS_LEVEL, replaceHiPSProjection, resolveHiPSConstant, tileCoordsWrap, wpToSpecVect
} from '../HiPSUtil.js';
import CoordinateSys from '../CoordSys.js';
import {makeWorldPt} from '../Point.js';
import {ABSOLUTE, STRETCH_ASINH, STRETCH_LINEAR, STRETCH_LOG, STRETCH_SQRT, ZSCALE} from '../RangeValues.js';
import {convertCelestial} from '../VisUtil.js';
import * as WebPlot from '../WebPlot.js';
import * as WebUtil from '../../util/WebUtil.js';

// tile pixel size at norder 0: a 512x512 tile covering 1/12 of the sky
const NORDER0_PIX_DEG= Math.sqrt(4*Math.PI / (12*512*512)) * 180/Math.PI;


describe('Test HiPSUtil.getTilePixelAngSize()', () => {

    test('getTilePixelAngSize() at norder 0 is the size of a pixel in a 512 pixel tile of 1/12 of the sky', () => {
        expect(getTilePixelAngSize(0)).toBeCloseTo(NORDER0_PIX_DEG, 12);
    });

    test('getTilePixelAngSize() halves with each norder', () => {
        [1, 3, 8, 12].forEach( (nOrder) => expect(getTilePixelAngSize(nOrder)).toBeCloseTo(NORDER0_PIX_DEG / 2**nOrder, 14));
    });

    test('getTilePixelAngSize() truncates a fractional norder', () => {
        expect(getTilePixelAngSize(8.9)).toEqual(getTilePixelAngSize(8));
    });
});


describe('Test HiPSUtil.getHiPSNorderlevel()', () => {

    afterEach(() => jest.restoreAllMocks());

    test('getHiPSNorderlevel() with no plot', () => {
        expect(getHiPSNorderlevel(undefined)).toEqual({norder: -1, useAllSky: false, isMaxOrder: false, desiredNorder: -1});
    });

    test('getHiPSNorderlevel() zoomed far out uses the all sky image', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(140);
        expect(getHiPSNorderlevel({plotType: 'hips'})).toEqual({useAllSky: true, norder: 2, desiredNorder: 2});
        WebPlot.getScreenPixScaleArcSec.mockReturnValue(120);
        expect(getHiPSNorderlevel({plotType: 'hips'})).toEqual({useAllSky: true, norder: 3, desiredNorder: 3});
    });

    test('getHiPSNorderlevel() picks the shallowest norder whose tile pixels are no bigger than a screen pixel', () => {
        // norder 4 tile pixels are about 25.8", norder 3 about 51.5"
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(30);
        expect(getHiPSNorderlevel({plotType: 'hips'})).toMatchObject({norder: 4, desiredNorder: 4, useAllSky: false});
        // norder 9 tile pixels are about 0.8", norder 8 about 1.6"
        WebPlot.getScreenPixScaleArcSec.mockReturnValue(1);
        expect(getHiPSNorderlevel({plotType: 'hips'})).toMatchObject({norder: 9, desiredNorder: 9});
        expect(getTilePixelAngSize(9)*3600).toBeLessThanOrEqual(1);
        expect(getTilePixelAngSize(8)*3600).toBeGreaterThan(1);
    });

    test('getHiPSNorderlevel() is never deeper than MAX_SUPPORTED_HIPS_LEVEL', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(.000001);
        expect(getHiPSNorderlevel({plotType: 'hips'}).norder).toEqual(MAX_SUPPORTED_HIPS_LEVEL);
    });

    test('getHiPSNorderlevel() with limitToImageDepth stops at hips_order', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(1);
        const plot= {plotType: 'hips', hipsProperties: {hips_order: '6'}};
        expect(getHiPSNorderlevel(plot, true)).toEqual({norder: 6, desiredNorder: 9, useAllSky: false, isMaxOrder: true});
        expect(getHiPSNorderlevel(plot, false)).toEqual({norder: 9, desiredNorder: 9, useAllSky: false, isMaxOrder: false});
    });

    test('getHiPSNorderlevel() with limitToImageDepth and no hips_order uses hips_pixel_scale, then 3', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(1);
        // .0002 deg is .72", the norder for that is 10, deeper than the screen norder 9
        expect(getHiPSNorderlevel({plotType: 'hips', hipsProperties: {hips_pixel_scale: '.0002'}}, true).norder).toEqual(9);
        // .0008 deg is 2.88", the norder for that is 8 (1.6" pixels, norder 7 pixels are 3.2")
        expect(getHiPSNorderlevel({plotType: 'hips', hipsProperties: {hips_pixel_scale: '.0008'}}, true).norder).toEqual(8);
        expect(getHiPSNorderlevel({plotType: 'hips', hipsProperties: {}}, true).norder).toEqual(3);
    });
});


describe('Test HiPSUtil.getMaxDisplayableHiPSGridLevel()', () => {

    afterEach(() => jest.restoreAllMocks());

    test('getMaxDisplayableHiPSGridLevel() is 2 deeper for norder 3 or less, 5 deeper otherwise', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(120);
        expect(getMaxDisplayableHiPSGridLevel({plotType: 'hips'})).toEqual(5);
        WebPlot.getScreenPixScaleArcSec.mockReturnValue(30);
        expect(getMaxDisplayableHiPSGridLevel({plotType: 'hips'})).toEqual(9);
    });

    test('getMaxDisplayableHiPSGridLevel() is never deeper than MAX_SUPPORTED_HIPS_LEVEL', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(.000001);
        expect(getMaxDisplayableHiPSGridLevel({plotType: 'hips'})).toEqual(MAX_SUPPORTED_HIPS_LEVEL);
    });
});


describe('Test HiPSUtil.getPlotTilePixelAngSize()', () => {

    afterEach(() => jest.restoreAllMocks());

    test('getPlotTilePixelAngSize() with no plot is 0', () => {
        expect(getPlotTilePixelAngSize(undefined)).toEqual(0);
    });

    test('getPlotTilePixelAngSize() is the tile pixel size at the plot norder, limited to the image depth', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(1);
        const plot= {plotType: 'hips', hipsProperties: {hips_order: '6'}};
        expect(getPlotTilePixelAngSize(plot)).toEqual(getTilePixelAngSize(6));
    });

    test('getPlotTilePixelAngSize() is computed once per plot object', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(30);
        const plot= {plotType: 'hips', hipsProperties: {hips_order: '9'}};
        const firstSize= getPlotTilePixelAngSize(plot);
        WebPlot.getScreenPixScaleArcSec.mockReturnValue(1);
        expect(getPlotTilePixelAngSize(plot)).toEqual(firstSize);
        expect(getPlotTilePixelAngSize({...plot})).not.toEqual(firstSize);
    });
});


describe('Test HiPSUtil.getCatalogNorderlevel()', () => {

    // results are cached by screen pixel size and grid size, so each test uses its own screen pixel size

    afterEach(() => jest.restoreAllMocks());

    test('getCatalogNorderlevel() with no plot is -1', () => {
        expect(getCatalogNorderlevel(undefined)).toEqual(-1);
    });

    test('getCatalogNorderlevel() zoomed far out is minNOrder', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(101);
        expect(getCatalogNorderlevel({plotType: 'hips'}, 6, 12)).toEqual(6);
    });

    test('getCatalogNorderlevel() is between minNOrder and maxNorder', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(1.1);
        expect(getCatalogNorderlevel({plotType: 'hips'}, 7, 12, 128)).toEqual(11);
        WebPlot.getScreenPixScaleArcSec.mockReturnValue(1.2);
        expect(getCatalogNorderlevel({plotType: 'hips'}, 7, 9, 128)).toEqual(9);
        WebPlot.getScreenPixScaleArcSec.mockReturnValue(1.3);
        expect(getCatalogNorderlevel({plotType: 'hips'}, 13, 15, 128)).toEqual(13);
    });

    test('getCatalogNorderlevel() different limits at the same zoom are not mixed up', () => {
        jest.spyOn(WebPlot, 'getScreenPixScaleArcSec').mockReturnValue(1.4);
        expect(getCatalogNorderlevel({plotType: 'hips'}, 7, 12, 128)).toEqual(11);
        expect(getCatalogNorderlevel({plotType: 'hips'}, 5, 8, 128)).toEqual(8);
        expect(getCatalogNorderlevel({plotType: 'hips'}, 7, 12, 128)).toEqual(11);
    });
});


describe('Test HiPSUtil URL functions', () => {

    const plot= {plotType: 'hips', hipsUrlRoot: 'http://a/b', hipsProperties: {hips_tile_format: 'png fits'},
        cubeDepth: 1, cubeIdx: 0};

    test('getHiPSTileExt() prefers png, otherwise jpg', () => {
        expect(getHiPSTileExt('png jpeg')).toEqual('png');
        expect(getHiPSTileExt('jpeg fits')).toEqual('jpg');
        expect(getHiPSTileExt('jpg')).toEqual('jpg');
        expect(getHiPSTileExt(['png'])).toEqual('png');
        expect(getHiPSTileExt(undefined)).toBeNull();
    });

    test('getHiPSTileExt() with only fits still returns jpg', () => {
        expect(getHiPSTileExt('fits')).toEqual('jpg');
    });

    test('makeHipsUrl() without proxy returns the url unchanged', () => {
        expect(makeHipsUrl('http://a/b/properties', false)).toEqual('http://a/b/properties');
    });

    test('makeHiPSTileUrl() builds the Norder/Dir/Npix path', () => {
        expect(makeHiPSTileUrl(plot, 5, 12345)).toEqual('http://a/b/Norder5/Dir10000/Npix12345.png');
        expect(makeHiPSTileUrl({...plot, hipsUrlRoot: 'http://a/b/'}, 3, 9999))
            .toEqual('http://a/b/Norder3/Dir0/Npix9999.png');
        expect(makeHiPSTileUrl({...plot, hipsProperties: {}}, 3, 1)).toEqual('http://a/b/Norder3/Dir0/Npix1.jpg');
        expect(makeHiPSTileUrl(undefined, 3, 1)).toBeNull();
    });

    test('makeHiPSTileUrl() adds the cube plane after the first plane', () => {
        expect(makeHiPSTileUrl({...plot, cubeDepth: 4, cubeIdx: 2}, 5, 23456))
            .toEqual('http://a/b/Norder5/Dir20000/Npix23456_2.png');
        expect(makeHiPSTileUrl({...plot, cubeDepth: 4, cubeIdx: 0}, 5, 23456))
            .toEqual('http://a/b/Norder5/Dir20000/Npix23456.png');
    });

    test('makeHipsFitsTilePath() uses the fits extension', () => {
        expect(makeHipsFitsTilePath(plot, 5, 12345)).toEqual('http://a/b/Norder5/Dir10000/Npix12345.fits');
        expect(makeHipsFitsTilePath({...plot, cubeDepth: 4, cubeIdx: 2}, 5, 12345))
            .toEqual('http://a/b/Norder5/Dir10000/Npix12345_2.fits');
    });

    test('makeHipsFitsTilePath() for a FITS cube uses one _cube.fits file for all planes', () => {
        const cubePlot= {...plot, cubeDepth: 4, hasFitsCube: true};
        expect(makeHipsFitsTilePath({...cubePlot, cubeIdx: 2}, 5, 12345))
            .toEqual('http://a/b/Norder5/Dir10000/Npix12345_cube.fits');
        expect(makeHipsFitsTilePath({...cubePlot, cubeIdx: 0}, 5, 12345))
            .toEqual('http://a/b/Norder5/Dir10000/Npix12345_cube.fits');
    });

    test('makeHiPSAllSkyUrl() using parameters (urlRoot, exts, cubeIdx)', () => {
        expect(makeHiPSAllSkyUrl('http://a/b', 'png jpeg')).toEqual('http://a/b/Norder3/Allsky.png');
        expect(makeHiPSAllSkyUrl('http://a/b/', 'jpeg fits', 2)).toEqual('http://a/b/Norder3/Allsky_2.jpg');
        expect(makeHiPSAllSkyUrl('', 'png')).toBeNull();
        expect(makeHiPSAllSkyUrl('http://a/b', undefined)).toBeNull();
    });

    test('makeHiPSAllSkyUrlFromPlot() uses the plot url, tile format and cube plane', () => {
        expect(makeHiPSAllSkyUrlFromPlot(plot)).toEqual('http://a/b/Norder3/Allsky.png');
        expect(makeHiPSAllSkyUrlFromPlot({...plot, cubeDepth: 4, cubeIdx: 3})).toEqual('http://a/b/Norder3/Allsky_3.png');
        expect(makeHiPSAllSkyUrlFromPlot({...plot, hipsProperties: {}})).toEqual('http://a/b/Norder3/Allsky.jpg');
        expect(makeHiPSAllSkyUrlFromPlot(undefined)).toBeNull();
    });

    test('makeHiPSPropertiesUrl() without proxy', () => {
        expect(makeHiPSPropertiesUrl('http://a/b', false)).toEqual('http://a/b/properties');
        expect(makeHiPSPropertiesUrl('http://a/b/', false)).toEqual('http://a/b/properties');
    });
});


describe('Test HiPSUtil.isHiPSFitsCube()', () => {

    test('isHiPSFitsCube() needs a HiPS plot with hasFitsCube and more than one plane', () => {
        expect(isHiPSFitsCube({plotType: 'hips', hasFitsCube: true, cubeDepth: 4})).toBe(true);
        expect(isHiPSFitsCube({plotType: 'hips', hasFitsCube: true, cubeDepth: 1})).toBe(false);
        expect(isHiPSFitsCube({plotType: 'hips', hasFitsCube: false, cubeDepth: 4})).toBe(false);
        expect(isHiPSFitsCube({plotType: 'image', hasFitsCube: true, cubeDepth: 4})).toBe(false);
        expect(isHiPSFitsCube(undefined)).toBe(false);
    });
});


describe('Test HiPSUtil.getHealpixPixelAtNorder()', () => {

    test('getHealpixPixelAtNorder() pixel is 9 orders deeper than the tile, inside that tile', () => {
        const result= getHealpixPixelAtNorder(3, makeWorldPt(10, 20), CoordinateSys.EQ_J2000);
        expect(result.tileNorder).toEqual(3);
        expect(result.norder).toEqual(12);
        expect(Math.trunc(result.pixel / 4**9)).toEqual(result.tilePixel);
    });

    test('getHealpixPixelAtNorder() tile coordinates are within a 512x512 tile, image y is flipped', () => {
        const {tileCoords, tileImagePt}= getHealpixPixelAtNorder(5, makeWorldPt(200, -45), CoordinateSys.EQ_J2000);
        [tileCoords.x, tileCoords.y].forEach( (coord) => {
            expect(coord).toBeGreaterThanOrEqual(0);
            expect(coord).toBeLessThan(512);
        });
        expect(tileImagePt.x).toEqual(tileCoords.x);
        expect(tileImagePt.y).toEqual(511 - tileCoords.y);
    });

    test('getHealpixPixelAtNorder() tilePixel matches the cell from getHealpixCellAtNorder()', () => {
        const wp= makeWorldPt(83.63, 22.01);
        expect(getHealpixPixelAtNorder(4, wp, CoordinateSys.EQ_J2000).tilePixel)
            .toEqual(getHealpixCellAtNorder(4, wp, CoordinateSys.EQ_J2000).ipix);
    });

    test('getHealpixPixelAtNorder() converts the point to the data coordinate system', () => {
        const wp= makeWorldPt(10, 20);
        expect(getHealpixPixelAtNorder(3, wp, CoordinateSys.GALACTIC))
            .toEqual(getHealpixPixelAtNorder(3, convertCelestial(wp, CoordinateSys.GALACTIC), CoordinateSys.GALACTIC));
        expect(getHealpixPixelAtNorder(3, wp, CoordinateSys.GALACTIC).pixel)
            .not.toEqual(getHealpixPixelAtNorder(3, wp, CoordinateSys.EQ_J2000).pixel);
    });

    test('getHealpixPixelAtNorder() too deep or missing input returns undefined', () => {
        expect(getHealpixPixelAtNorder(MAX_SUPPORTED_HIPS_LEVEL-8, makeWorldPt(10, 20), CoordinateSys.EQ_J2000))
            .toBeUndefined();
        expect(getHealpixPixelAtNorder(3, undefined, CoordinateSys.EQ_J2000)).toBeUndefined();
        expect(getHealpixPixelAtNorder(3, makeWorldPt(10, 20), undefined)).toBeUndefined();
    });
});


describe('Test HiPSUtil.getHiPSFitsTileLocation()', () => {

    const plot= {plotType: 'hips', hipsUrlRoot: 'http://a/b', dataCoordSys: CoordinateSys.EQ_J2000, cubeDepth: 1};

    test('getHiPSFitsTileLocation() returns the tile, its fits url and the pixel in the tile', () => {
        const wp= makeWorldPt(10, 20);
        const {tilePixel, tileImagePt}= getHealpixPixelAtNorder(3, wp, CoordinateSys.EQ_J2000);
        expect(getHiPSFitsTileLocation(plot, wp, 3)).toEqual({
            norder: 3,
            ipix: tilePixel,
            tileUrl: `http://a/b/Norder3/Dir0/Npix${tilePixel}.fits`,
            tilePt: tileImagePt,
        });
    });

    test('getHiPSFitsTileLocation() with missing input or a point that cannot be found returns {}', () => {
        expect(getHiPSFitsTileLocation(undefined, makeWorldPt(10, 20), 3)).toEqual({});
        expect(getHiPSFitsTileLocation(plot, undefined, 3)).toEqual({});
        expect(getHiPSFitsTileLocation(plot, makeWorldPt(10, 20), undefined)).toEqual({});
        expect(getHiPSFitsTileLocation(plot, makeWorldPt(10, 20), MAX_SUPPORTED_HIPS_LEVEL)).toEqual({});
    });
});


describe('Test HiPSUtil.isTileInside() and getProGradeTilePixels()', () => {

    test('isTileInside() at the same norder only matches the same tile', () => {
        expect(isTileInside(3, 5, 3, 5)).toBe(true);
        expect(isTileInside(3, 5, 3, 6)).toBe(false);
    });

    test('isTileInside() a deeper tile is inside its ancestor', () => {
        // the 16 norder 5 tiles inside norder 3 tile 7 are 112 to 127
        expect(isTileInside(5, 112, 3, 7)).toBe(true);
        expect(isTileInside(5, 127, 3, 7)).toBe(true);
        expect(isTileInside(5, 111, 3, 7)).toBe(false);
        expect(isTileInside(5, 128, 3, 7)).toBe(false);
    });

    test('isTileInside() a shallower tile is never inside a deeper one', () => {
        expect(isTileInside(2, 1, 3, 4)).toBe(false);
    });

    test('getProGradeTilePixels() returns the 4 child tiles', () => {
        expect(getProGradeTilePixels(5)).toEqual([20, 21, 22, 23]);
        expect(getProGradeTilePixels(0)).toEqual([0, 1, 2, 3]);
        getProGradeTilePixels(5).forEach( (childPix) => expect(isTileInside(4, childPix, 3, 5)).toBe(true));
    });
});


describe('Test HiPSUtil.getHealpixCornerTool()', () => {

    test('getHealpixCornerTool() returns the same tool each call', () => {
        expect(getHealpixCornerTool()).toBe(getHealpixCornerTool());
    });

    test('getHealpixCornerTool() getFullCellList() has every cell for norder 1 to 3', () => {
        const tool= getHealpixCornerTool();
        [1, 2, 3].forEach( (norder) => {
            const cells= tool.getFullCellList(norder, CoordinateSys.EQ_J2000);
            expect(cells.length).toEqual(12 * 4**norder);
            expect(cells[7].ipix).toEqual(7);
            expect(cells[7].wpCorners.length).toEqual(4);
        });
        expect(tool.getFullCellList(2, CoordinateSys.GALACTIC)[0].wpCorners[0].cSys).toBe(CoordinateSys.GALACTIC);
    });

    test('getHealpixCornerTool() has no cached cells for other norders or coordinate systems', () => {
        const tool= getHealpixCornerTool();
        expect(tool.getFullCellList(4, CoordinateSys.EQ_J2000)).toBeNull();
        expect(tool.getFullCellList(2, CoordinateSys.ECL_J2000)).toBeNull();
    });

    test('getHealpixCornerTool() makeCornersForPix() for a deeper norder', () => {
        const {ipix, wpCorners}= getHealpixCornerTool().makeCornersForPix(1000, 2**6, CoordinateSys.EQ_J2000);
        expect(ipix).toEqual(1000);
        expect(wpCorners.length).toEqual(4);
        wpCorners.forEach( (wp) => expect(wp.cSys).toBe(CoordinateSys.EQ_J2000));
    });

    test('getHealpixCornerTool() makeCornersForPix() uses the cached cells for norder 1 to 3', () => {
        const tool= getHealpixCornerTool();
        expect(tool.makeCornersForPix(9, 2**2, CoordinateSys.EQ_J2000))
            .toBe(tool.getFullCellList(2, CoordinateSys.EQ_J2000)[9]);
    });
});


describe('Test HiPSUtil.getHealpixCellAtNorder()', () => {

    test('getHealpixCellAtNorder() works before anything else has built the corner cache', () => {
        jest.isolateModules( () => {
            const {getHealpixCellAtNorder: freshGetHealpixCellAtNorder}= require('../HiPSUtil.js');
            const cell= freshGetHealpixCellAtNorder(6, makeWorldPt(10, 20), CoordinateSys.EQ_J2000);
            expect(cell.norder).toEqual(6);
            expect(cell.wpCorners.length).toEqual(4);
        });
    });

    test('getHealpixCellAtNorder() returns the cell holding the point', () => {
        const cell= getHealpixCellAtNorder(6, makeWorldPt(10, 20), CoordinateSys.EQ_J2000);
        expect(cell.norder).toEqual(6);
        expect(cell.wpCorners.length).toEqual(4);
        expect(getHealpixPixelAtNorder(6, makeWorldPt(10, 20), CoordinateSys.EQ_J2000).tilePixel).toEqual(cell.ipix);
        // the point is within the cell's corners
        const lons= cell.wpCorners.map( (wp) => wp.x);
        const lats= cell.wpCorners.map( (wp) => wp.y);
        expect(10).toBeGreaterThanOrEqual(Math.min(...lons));
        expect(10).toBeLessThanOrEqual(Math.max(...lons));
        expect(20).toBeGreaterThanOrEqual(Math.min(...lats));
        expect(20).toBeLessThanOrEqual(Math.max(...lats));
    });
});


describe('Test HiPSUtil.getCornersForCell()', () => {

    test('getCornersForCell() deeper than norder 3 returns the cell with its corners', () => {
        const cell= getCornersForCell(6, 1000, CoordinateSys.EQ_J2000);
        expect(cell.ipix).toEqual(1000);
        expect(cell.wpCorners.length).toEqual(4);
    });

    test('getCornersForCell() norder 1 to 3 returns the cached cell with its corners', () => {
        const cell= getCornersForCell(2, 9, CoordinateSys.EQ_J2000);
        expect(cell).toBe(getHealpixCornerTool().getFullCellList(2, CoordinateSys.EQ_J2000)[9]);
        expect(cell.ipix).toEqual(9);
        expect(cell.wpCorners.length).toEqual(4);
    });

    test('getCornersForCell() norder 1 to 3 in a coordinate system that is not cached', () => {
        const {ipix, wpCorners}= getCornersForCell(2, 9, CoordinateSys.ECL_J2000);
        expect(ipix).toEqual(9);
        expect(wpCorners.length).toEqual(4);
    });
});


describe('Test HiPSUtil.getVisibleHiPSCells() and getAllVisibleHiPSCells()', () => {

    const centerWp= makeWorldPt(10, 20);
    const viewDim= {width: 500, height: 500};

    const centerCellIpix= (norder) => getHealpixPixelAtNorder(norder, centerWp, CoordinateSys.EQ_J2000).tilePixel;

    test('getVisibleHiPSCells() aitoff zoomed out returns every cell', () => {
        expect(getVisibleHiPSCells(2, 2, centerWp, 200, viewDim, CoordinateSys.EQ_J2000, true).length).toEqual(192);
    });

    test('getVisibleHiPSCells() wide field returns some of the cells, including the center one', () => {
        const cells= getVisibleHiPSCells(2, 2, centerWp, 100, viewDim, CoordinateSys.EQ_J2000);
        expect(cells.length).toBeGreaterThan(0);
        expect(cells.length).toBeLessThan(192);
        expect(cells.map( (cell) => cell.ipix)).toContain(centerCellIpix(2));
    });

    test('getVisibleHiPSCells() narrow field returns the cells around the center', () => {
        const cells= getVisibleHiPSCells(6, 6, centerWp, 2, viewDim, CoordinateSys.EQ_J2000);
        expect(cells.length).toBeGreaterThan(0);
        expect(cells.length).toBeLessThan(100);
        expect(cells.map( (cell) => cell.ipix)).toContain(centerCellIpix(6));
        cells.forEach( (cell) => expect(cell.wpCorners.length).toEqual(4));
    });

    test('getVisibleHiPSCells() with a deeper desiredNorder returns unique cells at norder', () => {
        const cells= getVisibleHiPSCells(6, 8, centerWp, 2, viewDim, CoordinateSys.EQ_J2000);
        const ipixAry= cells.map( (cell) => cell.ipix);
        expect(new Set(ipixAry).size).toEqual(ipixAry.length);
        expect(ipixAry).toContain(centerCellIpix(6));
    });

    test('getAllVisibleHiPSCells() aitoff zoomed out returns every cell', () => {
        expect(getAllVisibleHiPSCells(2, centerWp, 200, CoordinateSys.EQ_J2000, true).length).toEqual(192);
    });

    test('getAllVisibleHiPSCells() wide field returns cells with a corner within 90 degrees of the center', () => {
        const cells= getAllVisibleHiPSCells(2, centerWp, 100, CoordinateSys.EQ_J2000, false);
        expect(cells.length).toBeGreaterThan(0);
        expect(cells.length).toBeLessThan(192);
        expect(cells.map( (cell) => cell.ipix)).toContain(centerCellIpix(2));
    });

    test('getAllVisibleHiPSCells() narrow field returns the cells around the center', () => {
        const cells= getAllVisibleHiPSCells(6, centerWp, 2, CoordinateSys.EQ_J2000, false);
        expect(cells.length).toBeGreaterThan(0);
        expect(cells.map( (cell) => cell.ipix)).toContain(centerCellIpix(6));
    });
});


describe('Test HiPSUtil.wpToSpecVect()', () => {

    test('wpToSpecVect() keeps the ra and dec', () => {
        const spatialVector= wpToSpecVect(makeWorldPt(83.63, 22.01));
        expect(spatialVector.ra()).toBeCloseTo(83.63, 8);
        expect(spatialVector.dec()).toBeCloseTo(22.01, 8);
    });
});


describe('Test HiPSUtil.tileCoordsWrap()', () => {

    const corners= ['wp1', 'wp2', 'wp3', 'wp4'];

    test('tileCoordsWrap() checks the 4 edges of the tile', () => {
        const checkedPairs= [];
        const cc= {coordsWrap: (wpA, wpB, factor) => { checkedPairs.push([wpA, wpB, factor]); return false; }};
        expect(tileCoordsWrap(cc, corners)).toBe(false);
        expect(checkedPairs).toEqual([['wp1', 'wp2', 3], ['wp1', 'wp4', 3], ['wp2', 'wp3', 3], ['wp3', 'wp4', 3]]);
    });

    test('tileCoordsWrap() is true when any edge wraps', () => {
        const cc= {coordsWrap: (wpA, wpB) => wpA==='wp3' && wpB==='wp4'};
        expect(tileCoordsWrap(cc, corners)).toBe(true);
    });

    test('tileCoordsWrap() passes the factor through', () => {
        const cc= {coordsWrap: (wpA, wpB, factor) => factor===5};
        expect(tileCoordsWrap(cc, corners, 5)).toBe(true);
    });
});


describe('Test HiPSUtil.resolveHiPSConstant() and getPropertyItem()', () => {

    test('resolveHiPSConstant() with a known constant returns its url', () => {
        expect(resolveHiPSConstant('DSS2_RED')).toEqual(API_HIPS_CONSTANTS.DSS2_RED);
        expect(resolveHiPSConstant('TWO_MASS')).toEqual('http://alasky.u-strasbg.fr/2MASS/Color');
    });

    test('resolveHiPSConstant() with anything else returns it unchanged', () => {
        expect(resolveHiPSConstant('http://my/hips')).toEqual('http://my/hips');
    });

    test('getPropertyItem() ivoid uses creator_did, then publisher_did', () => {
        expect(getPropertyItem({creator_did: 'ivo://c', publisher_did: 'ivo://p'}, 'ivoid')).toEqual('ivo://c');
        expect(getPropertyItem({publisher_did: 'ivo://p'}, 'ivoid')).toEqual('ivo://p');
        expect(getPropertyItem({}, 'ivoid')).toBeUndefined();
    });

    test('getPropertyItem() other keys are read directly', () => {
        expect(getPropertyItem({hips_order: '9', ivoid: 'ignored'}, 'hips_order')).toEqual('9');
        expect(getPropertyItem({}, 'hips_order')).toBeUndefined();
    });
});


describe('Test HiPSUtil.replaceHiPSProjection()', () => {

    test('replaceHiPSProjection() makes a projection in the new coordinate system centered on the point', () => {
        const plot= {plotType: 'hips', plotId: 'replaceTest', dataCoordSys: CoordinateSys.EQ_J2000,
            projection: {header: {maptype: 0}}};
        const wp= makeWorldPt(10, 20);
        const newPlot= replaceHiPSProjection(plot, CoordinateSys.GALACTIC, wp);
        const expectedCenter= convertCelestial(wp, CoordinateSys.GALACTIC);

        expect(newPlot.imageCoordSys).toBe(CoordinateSys.GALACTIC);
        expect(newPlot.projection.coordSys).toBe(CoordinateSys.GALACTIC);
        expect(newPlot.projection.header.crval1).toBeCloseTo(expectedCenter.x, 8);
        expect(newPlot.projection.header.crval2).toBeCloseTo(expectedCenter.y, 8);
        expect(newPlot.allWCSMap).toEqual({'': newPlot.projection});
        expect(newPlot.plotId).toEqual('replaceTest');
        expect(newPlot.dataCoordSys).toBe(CoordinateSys.EQ_J2000);
        expect(plot.projection).toEqual({header: {maptype: 0}});
    });
});


describe('Test HiPSUtil.loadImageMultiCall()', () => {

    afterEach(() => jest.restoreAllMocks());

    test('loadImageMultiCall() loads a url once for calls made while it is loading', async () => {
        const image= {width: 10};
        const loadImageSpy= jest.spyOn(WebUtil, 'loadImage').mockResolvedValue(image);
        const results= await Promise.all([loadImageMultiCall('http://a/once.png'), loadImageMultiCall('http://a/once.png')]);
        expect(results).toEqual([image, image]);
        expect(loadImageSpy).toHaveBeenCalledTimes(1);
    });

    test('loadImageMultiCall() loads again after the first load finished', async () => {
        const loadImageSpy= jest.spyOn(WebUtil, 'loadImage').mockResolvedValue({width: 10});
        await loadImageMultiCall('http://a/again.png');
        await loadImageMultiCall('http://a/again.png');
        expect(loadImageSpy).toHaveBeenCalledTimes(2);
    });

    test('loadImageMultiCall() rejects every caller when the load fails or returns no image', async () => {
        jest.spyOn(WebUtil, 'loadImage').mockRejectedValue(new Error('network'));
        await expect(Promise.all([loadImageMultiCall('http://a/fail.png'), loadImageMultiCall('http://a/fail.png')]))
            .rejects.toThrow('network');

        WebUtil.loadImage.mockResolvedValue(undefined);
        await expect(loadImageMultiCall('http://a/empty.png')).rejects.toThrow('could not load image');
    });
});


describe('Test HiPSUtil.getHipsPixelCutRangeValues()', () => {

    test('getHipsPixelCutRangeValues() with no hips_pixel_cut is a zscale asinh stretch', () => {
        const rv= getHipsPixelCutRangeValues({hipsProperties: {hips_pixel_function: 'sqrt'}});
        expect(rv.lowerWhich).toBe(ZSCALE);
        expect(rv.upperWhich).toBe(ZSCALE);
        expect(rv.algorithm).toBe(STRETCH_ASINH);
        expect(rv.asinhQValue).toBe(4);
    });

    test('getHipsPixelCutRangeValues() with no hipsProperties is a zscale asinh stretch', () => {
        const rv= getHipsPixelCutRangeValues({});
        expect(rv.lowerWhich).toBe(ZSCALE);
        expect(rv.algorithm).toBe(STRETCH_ASINH);
    });

    test('getHipsPixelCutRangeValues() uses hips_pixel_cut as absolute bounds, log by default', () => {
        const rv= getHipsPixelCutRangeValues({hipsProperties: {hips_pixel_cut: '-2.5 300'}});
        expect(rv.lowerWhich).toBe(ABSOLUTE);
        expect(rv.upperWhich).toBe(ABSOLUTE);
        expect(rv.lowerValue).toBe(-2.5);
        expect(rv.upperValue).toBe(300);
        expect(rv.algorithm).toBe(STRETCH_LOG);
    });

    test('getHipsPixelCutRangeValues() uses hips_pixel_function for the algorithm', () => {
        const rv= getHipsPixelCutRangeValues({hipsProperties: {hips_pixel_cut: '0 100', hips_pixel_function: 'sqrt'}});
        expect(rv.algorithm).toBe(STRETCH_SQRT);
    });

    test('getHipsPixelCutRangeValues() uses a third hips_pixel_cut token when there is no hips_pixel_function', () => {
        const rv= getHipsPixelCutRangeValues({hipsProperties: {hips_pixel_cut: '0 100 linear'}});
        expect(rv.algorithm).toBe(STRETCH_LINEAR);
    });

    test('getHipsPixelCutRangeValues() hips_pixel_function wins over a third hips_pixel_cut token', () => {
        const rv= getHipsPixelCutRangeValues({hipsProperties: {hips_pixel_cut: '0 100 linear', hips_pixel_function: 'sqrt'}});
        expect(rv.algorithm).toBe(STRETCH_SQRT);
    });

    test('getHipsPixelCutRangeValues() with an unsupported algorithm falls back to log', () => {
        const rv= getHipsPixelCutRangeValues({hipsProperties: {hips_pixel_cut: '0 100', hips_pixel_function: 'histogram'}});
        expect(rv.algorithm).toBe(STRETCH_LOG);
    });

    test('getHipsPixelCutRangeValues() with asinh sets asinhQValue to 4, in any case', () => {
        ['asinh', 'ASINH', 'Asinh'].forEach( (pixelFunction) => {
            const rv= getHipsPixelCutRangeValues({hipsProperties: {hips_pixel_cut: '0 100', hips_pixel_function: pixelFunction}});
            expect(rv.algorithm).toBe(STRETCH_ASINH);
            expect(rv.asinhQValue).toBe(4);
        });
    });

    test('getHipsPixelCutRangeValues() with bad bounds is the zscale asinh default', () => {
        ['100 0', '50 50', 'abc 100', '100'].forEach( (pixelCut) => {
            const rv= getHipsPixelCutRangeValues({hipsProperties: {hips_pixel_cut: pixelCut, hips_pixel_function: 'sqrt'}});
            expect(rv.lowerWhich).toBe(ZSCALE);
            expect(rv.algorithm).toBe(STRETCH_ASINH);
        });
    });
});
