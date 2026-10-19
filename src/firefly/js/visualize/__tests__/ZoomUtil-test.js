import {
    convertZoomToString, getArcSecPerPix, getEstimatedFullZoomFactor, getNextZoomLevel, getZoomDesc, getZoomLevelForScale,
    getZoomMax, hipsLevelsReversed, imageLevels, imageLevelsReversed, makeFoVString
} from '../ZoomUtil.js';
import {PlotAttribute} from '../PlotAttribute.js';
import * as PlotViewUtil from '../PlotViewUtil.js';
import {HIPS_AITOFF} from '../projection/Projection.js';
import {FullType, UserZoomTypes} from '../VisConst.js';


describe('Test ZoomUtil zoom level lists', () => {

    test('imageLevels is ascending and imageLevelsReversed is its reverse', () => {
        imageLevels.slice(1).forEach((level, idx) => expect(level).toBeGreaterThan(imageLevels[idx]));
        expect(imageLevels[0]).toEqual(1/64);
        expect(imageLevels[imageLevels.length-1]).toEqual(32);
        expect(imageLevels).toContain(1);
        expect(imageLevelsReversed).toEqual([...imageLevels].reverse());
    });

    test('hipsLevelsReversed is descending, from 2.25 by steps of 1.5 then .75', () => {
        hipsLevelsReversed.slice(1).forEach((level, idx) => expect(level).toBeLessThan(hipsLevelsReversed[idx]));
        expect(hipsLevelsReversed.slice(0, 5)).toEqual([2.25, 1.5, 1, .75, .75*.75]);
    });
});


describe('Test ZoomUtil.getNextZoomLevel()', () => {

    test('getNextZoomLevel() with ONE always returns 1', () => {
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 4}, UserZoomTypes.ONE)).toEqual(1);
        expect(getNextZoomLevel({plotType: 'hips', zoomFactor: .2}, UserZoomTypes.ONE)).toEqual(1);
    });

    test('getNextZoomLevel() image UP goes to the next level', () => {
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 1}, UserZoomTypes.UP)).toEqual(1.189);
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 1.1}, UserZoomTypes.UP)).toEqual(1.189);
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: .001}, UserZoomTypes.UP)).toEqual(1/64);
    });

    test('getNextZoomLevel() image UP stops at the max level', () => {
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 32}, UserZoomTypes.UP)).toEqual(32);
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 40}, UserZoomTypes.UP)).toEqual(32);
    });

    test('getNextZoomLevel() image UP using upDownPercent moves part of the way to the next level', () => {
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 1}, UserZoomTypes.UP, .5)).toBeCloseTo(1.0945, 10);
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: .001}, UserZoomTypes.UP, .5)).toEqual(1/64);
    });

    test('getNextZoomLevel() image DOWN goes to the previous level', () => {
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 1}, UserZoomTypes.DOWN)).toEqual(.841);
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: .9}, UserZoomTypes.DOWN)).toEqual(.841);
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 40}, UserZoomTypes.DOWN)).toEqual(32);
    });

    test('getNextZoomLevel() image DOWN stops at the min level', () => {
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 1/64}, UserZoomTypes.DOWN)).toEqual(1/64);
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: .001}, UserZoomTypes.DOWN)).toEqual(1/64);
    });

    test('getNextZoomLevel() image DOWN using upDownPercent at the ends returns the level', () => {
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 40}, UserZoomTypes.DOWN, .5)).toEqual(32);
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: .02}, UserZoomTypes.DOWN, .5)).toEqual(1/64);
    });

    test('getNextZoomLevel() hips uses the hips levels', () => {
        expect(getNextZoomLevel({plotType: 'hips', zoomFactor: 1}, UserZoomTypes.UP)).toEqual(1.5);
        expect(getNextZoomLevel({plotType: 'hips', zoomFactor: 2.25}, UserZoomTypes.UP)).toEqual(2.25);
        expect(getNextZoomLevel({plotType: 'hips', zoomFactor: 1}, UserZoomTypes.DOWN)).toEqual(.75);
        expect(getNextZoomLevel({plotType: 'hips', zoomFactor: .75}, UserZoomTypes.DOWN)).toEqual(.75*.75);
    });

    test('getNextZoomLevel() with an unsupported zoom type returns 1', () => {
        expect(getNextZoomLevel({plotType: 'image', zoomFactor: 4}, UserZoomTypes.FIT)).toEqual(1);
    });
});


describe('Test ZoomUtil.getEstimatedFullZoomFactor()', () => {

    // zoom to fit width is 400/1000= .4, zoom to fit height is 800/500= 1.6
    const imagePlot= {plotType: 'image', attributes: {}, dataWidth: 1000, dataHeight: 500};
    const viewDim= {width: 400, height: 800};

    test('getEstimatedFullZoomFactor() image using each FullType', () => {
        expect(getEstimatedFullZoomFactor(imagePlot, viewDim, FullType.WIDTH_HEIGHT)).toBeCloseTo(.4, 10);
        expect(getEstimatedFullZoomFactor(imagePlot, viewDim, FullType.ONLY_WIDTH)).toBeCloseTo(.4, 10);
        expect(getEstimatedFullZoomFactor(imagePlot, viewDim, FullType.ONLY_HEIGHT)).toBeCloseTo(1.6, 10);
    });

    test('getEstimatedFullZoomFactor() image with a zero view dimension uses the other one', () => {
        expect(getEstimatedFullZoomFactor(imagePlot, {width: 400, height: 0}, FullType.WIDTH_HEIGHT)).toBeCloseTo(.4, 10);
        expect(getEstimatedFullZoomFactor(imagePlot, {width: 0, height: 800}, FullType.WIDTH_HEIGHT)).toBeCloseTo(1.6, 10);
    });

    test('getEstimatedFullZoomFactor() EXPANDED_TO_FIT_TYPE attribute overrides fullType', () => {
        const plot= {...imagePlot, attributes: {[PlotAttribute.EXPANDED_TO_FIT_TYPE]: 'ONLY_HEIGHT'}};
        expect(getEstimatedFullZoomFactor(plot, viewDim, FullType.WIDTH_HEIGHT)).toBeCloseTo(1.6, 10);
    });

    test('getEstimatedFullZoomFactor() unknown EXPANDED_TO_FIT_TYPE attribute is ignored', () => {
        const plot= {...imagePlot, attributes: {[PlotAttribute.EXPANDED_TO_FIT_TYPE]: 'nonsense'}};
        expect(getEstimatedFullZoomFactor(plot, viewDim, FullType.ONLY_HEIGHT)).toBeCloseTo(1.6, 10);
    });

    test('getEstimatedFullZoomFactor() hips aitoff', () => {
        // width is scaled by 2.7, so zoom to fit width is 2700/(1000*2.7)= 1, height is .7 * 800/500= 1.12
        const aitoffPlot= {plotType: 'hips', attributes: {}, dataWidth: 1000, dataHeight: 500,
            projection: {header: {maptype: HIPS_AITOFF}}};
        const aitoffViewDim= {width: 2700, height: 800};
        expect(getEstimatedFullZoomFactor(aitoffPlot, aitoffViewDim, FullType.ONLY_WIDTH)).toBeCloseTo(1, 10);
        expect(getEstimatedFullZoomFactor(aitoffPlot, aitoffViewDim, FullType.WIDTH_HEIGHT)).toBeCloseTo(1.12, 10);
        expect(getEstimatedFullZoomFactor(aitoffPlot, aitoffViewDim, FullType.ONLY_HEIGHT)).toBeCloseTo(1.12, 10);
    });

    test('getEstimatedFullZoomFactor() hips not aitoff is the same as an image', () => {
        const hipsPlot= {plotType: 'hips', attributes: {}, dataWidth: 1000, dataHeight: 500,
            projection: {header: {maptype: 0}}};
        expect(getEstimatedFullZoomFactor(hipsPlot, viewDim, FullType.WIDTH_HEIGHT)).toBeCloseTo(.4, 10);
    });
});


describe('Test ZoomUtil.getZoomMax()', () => {

    test('getZoomMax() for image and hips', () => {
        expect(getZoomMax({plotType: 'image'})).toEqual(32);
        expect(getZoomMax({plotType: 'hips'})).toEqual(2.25);
    });
});


describe('Test ZoomUtil.getArcSecPerPix() and getZoomLevelForScale()', () => {

    // one arcsec per image pixel
    const plot= {plotType: 'image', projection: {getPixelScaleDegree: () => 1/3600}};

    test('getArcSecPerPix() using parameters (plot, zoomFact)', () => {
        expect(getArcSecPerPix(plot, 1)).toBeCloseTo(1, 10);
        expect(getArcSecPerPix(plot, 2)).toBeCloseTo(.5, 10);
        expect(getArcSecPerPix(plot, .25)).toBeCloseTo(4, 10);
    });

    test('getZoomLevelForScale() using parameters (plot, arcsecPerPix)', () => {
        expect(getZoomLevelForScale(plot, .25)).toBeCloseTo(4, 10);
        expect(getZoomLevelForScale(plot, 2)).toBeCloseTo(.5, 10);
    });

    test('getZoomLevelForScale() is the inverse of getArcSecPerPix()', () => {
        expect(getZoomLevelForScale(plot, getArcSecPerPix(plot, 3.364))).toBeCloseTo(3.364, 10);
    });
});


describe('Test ZoomUtil.convertZoomToString()', () => {

    test('convertZoomToString() at 1 or more', () => {
        expect(convertZoomToString(1)).toEqual('1x');
        expect(convertZoomToString(2)).toEqual('2x');
        expect(convertZoomToString(1.95)).toEqual('2x');
        expect(convertZoomToString(1.05)).toEqual('1x');
        expect(convertZoomToString(1.5)).toEqual('1.500x');
        expect(convertZoomToString(1.189)).toEqual('1.189x');
    });

    test('convertZoomToString() with common fractions', () => {
        expect(convertZoomToString(.75)).toEqual('¾x');
        expect(convertZoomToString(.5)).toEqual('½x');
        expect(convertZoomToString(.25)).toEqual('¼x');
        expect(convertZoomToString(1/8)).toEqual('1/8x');
        expect(convertZoomToString(1/16)).toEqual('1/16x');
        expect(convertZoomToString(1/32)).toEqual('1/32x');
        expect(convertZoomToString(1/64)).toEqual('1/64x');
        expect(convertZoomToString(1/128)).toEqual('1/128x');
        expect(convertZoomToString(1/256)).toEqual('1/256x');
    });

    test('convertZoomToString() with other values below 1', () => {
        expect(convertZoomToString(.707)).toEqual('0.7x');
        expect(convertZoomToString(.297)).toEqual('0.3x');
        expect(convertZoomToString(.1)).toEqual('0.100x');
    });
});


describe('Test ZoomUtil.makeFoVString()', () => {

    test('makeFoVString() above 1 degree is in degrees', () => {
        expect(makeFoVString(2.5)).toEqual('2.5°');
        expect(makeFoVString(15)).toEqual('15°');
    });

    test('makeFoVString() above 1 arcmin is in arcmin', () => {
        expect(makeFoVString(1)).toEqual(`60'`);
        expect(makeFoVString(.5)).toEqual(`30'`);
        expect(makeFoVString(.05)).toEqual(`3.0'`);
    });

    test('makeFoVString() at or below 1 arcmin is in arcsec', () => {
        expect(makeFoVString(.01)).toEqual('36"');
        expect(makeFoVString(.0005)).toEqual('1.8"');
    });

    test('makeFoVString() with NaN returns an empty string', () => {
        expect(makeFoVString(NaN)).toEqual('');
    });
});


describe('Test ZoomUtil.getZoomDesc()', () => {

    afterEach(() => jest.restoreAllMocks());

    // the fields primePlot() needs to recognize a PlotView
    const pvFields= {primeIdx: 0, viewDim: {width: 100, height: 100}, overlayPlotViews: [], plotViewCtx: {}};

    test('getZoomDesc() image returns zoom level and field of view', () => {
        jest.spyOn(PlotViewUtil, 'getFoV').mockReturnValue(.5);
        const pv= {...pvFields, plotId: 'zoomDescImage',
            plots: [{plotId: 'zoomDescImage', plotType: 'image', zoomFactor: 2, viewDim: {width: 100, height: 100}}]};
        expect(getZoomDesc(pv)).toEqual({zoomLevelFormatted: '2x', fovFormatted: `30'`});
    });

    test('getZoomDesc() hips returns only field of view', () => {
        jest.spyOn(PlotViewUtil, 'getFoV').mockReturnValue(180);
        const pv= {...pvFields, plotId: 'zoomDescHiPS',
            plots: [{plotId: 'zoomDescHiPS', plotType: 'hips', zoomFactor: 1, viewDim: {width: 100, height: 100}}]};
        expect(getZoomDesc(pv)).toEqual({zoomLevelFormatted: '', fovFormatted: '180°'});
    });

    test('getZoomDesc() with no plot or no viewDim returns empty strings', () => {
        const emptyDesc= {fovFormatted: '', zoomLevelFormatted: ''};
        expect(getZoomDesc(undefined)).toEqual(emptyDesc);
        expect(getZoomDesc({...pvFields, plotId: 'zoomDescNoPlot', plots: []})).toEqual(emptyDesc);
        const noViewDimPv= {...pvFields, plotId: 'zoomDescNoViewDim',
            plots: [{plotId: 'zoomDescNoViewDim', plotType: 'image', zoomFactor: 1}]};
        expect(getZoomDesc(noViewDimPv)).toEqual(emptyDesc);
    });
});
