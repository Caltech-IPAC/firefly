import {primePlot, getPlotViewById, isActivePlotView, getActivePlotView} from '../PlotViewUtil.js';
import {makePlotView} from '../reducer/PlotView.js';
import {WebPlotRequest} from '../WebPlotRequest.js';
import {
    applyToOnePvOrAll, applyToOnePvOrOverlayGroup, canConvertBetweenHipsAndFits, clonePvAry, clonePvAryWithPv,
    convertHDUIdxToImageIdx, convertImageIdxToHDU, currentP, findCurrentCenterPoint, findPlot, findPlotGroup,
    getAllCanvasLayersForPlot, getAllDrawLayersForPlot, getAllPlotViewIdByOverlayLock, getAllWaveLengthsForCube,
    getBandWidthUnits, getCenterOfProjection, getConnectedPlotsIds, getCorners, getCubeLength,
    getCubePlaneFromWavelength, getCubePlaneIdx, getDrawLayerById, getDrawLayerByType, getDrawLayersByDisplayGroup,
    getDrawLayersByType, getFormattedWaveLengthUnits, getFoV, getHDU, getHDUCount, getHDUIndex,
    getHduPlotStartIndexes, getLayerTitle, getMatchingRotationAngle, getNextHiPSPlotId, getNumberOfCubesInPV,
    getOnePvOrGroup, getOverlayById, getOverlayByPvAndId, getPlotGroupIdxById, getPlotStateAry, getPlotViewAry,
    getPlotViewIdListByPositionLock, getPlotViewIdListInOverlayGroup, getPlotViewIdxById, getPlotViewProxyById,
    getPrimaryPlotHdu, getPtSpectralCoords, getPtWavelength, getWavelengthParseFailReason, getWaveLengthUnits,
    getWaveLengthUnitsPrecision, hasImageCubes, hasOverlayColorLock, hasPixelLevelWLInfo, hasPlaneOnlyWLInfo,
    hasWCSProjection, hasWLInfo, isCube, isDefaultCoverageActive, isDrawLayerAttached, isDrawLayerVisible,
    isImageExpanded, isMultiHDUFits, isMultiImageFits, isMultiImageFitsWithSameArea, isPlotIdInPvNewPlotInfoAry,
    isPlotViewArysEqual, isPlotViewsEqual, isRotationMatching, isThreeColor, makeUniquePlotIdFromBase,
    matchPlotViewByPositionGroup, operateOnOthersInOverlayColorGroup, operateOnOthersInPositionGroup,
    plotInActiveGroup, primePlotType, pvEqualExScroll, refreshP, wavelengthInfoParsedSuccessfully
} from '../PlotViewUtil';
import CoordinateSys from '../CoordSys.js';
import CysConverter from '../CsysConverter.js';
import {makeDevicePt, makeImagePt, makeWorldPt} from '../Point.js';
import {makeTransform} from '../PlotTransformUtils.js';
import {HIPS_AITOFF} from '../projection/Projection.js';
import {DEFAULT_COVERAGE_PLOT_ID, DEFAULT_COVERAGE_VIEWER_ID, ExpandType} from '../VisConst.js';
import * as VisStoreRoots from '../VisStoreRoots.js';
import * as WebPlotAnalysis from '../WebPlotAnalysis.js';



const makeMockPv = (plotId, plotGroupId, plotCnt, primeIdx=0) => {
    const req= WebPlotRequest.makeURIPlotRequest('http://a/b/c.fits');
    req.setPlotGroupId(plotGroupId);
    req.setPlotId(plotId);
    const pv= makePlotView(plotId, req);
    pv.primeIdx= primeIdx;
    pv.plots= [];
    for(let i= 0; i<plotCnt; i++) {
        pv.plots.push({
            plotId,
            plotType:'image',
            plotImageId:plotId+'-image'+i,
            title: 'Dummy Plot testId1 - image'+i
        });
    }
    return pv;
};


const ACTIVE_ID='testId-xxxx';

const pvAry= [
    makeMockPv('testId1', 'aTestGroup', 3, 1),
    makeMockPv(ACTIVE_ID, 'aTestGroup', 1),
    makeMockPv('testId-yyyy', 'aTestGroup', 3),
    makeMockPv('testId-zzzz', 'anotherGroup', 4),
];
const visRoot= {plotViewAry: pvAry, activePlotId: ACTIVE_ID};



describe('Test PlotViewUtil.primePlot()', () => {

    test('primePlot() using parameters (plotView)', () => {
        const plot= primePlot(pvAry[0]);
        expect(plot).toBeDefined();
        expect(plot.plotId).toEqual('testId1');
        expect(plot.plotImageId).toEqual('testId1-image1');
    });

    test('primePlot() using parameters (plotView[], plotId)', () => {
        const plot= primePlot(pvAry, 'testId-yyyy');
        expect(plot).toBeDefined();
        expect(plot.plotId).toEqual('testId-yyyy');
        expect(plot.plotImageId).toEqual('testId-yyyy-image0');
    });

    test('primePlot() using parameters (visRoot)', () => {
        const visRoot= {plotViewAry: pvAry, activePlotId: 'testId-xxxx'};
        const plot= primePlot(visRoot);
        expect(plot).toBeDefined();
        expect(plot.plotId).toEqual('testId-xxxx');
        expect(plot.plotImageId).toEqual('testId-xxxx-image0');
    });

    test('primePlot() using parameters (visRoot, plotId)', () => {
        const plot= primePlot(visRoot, 'testId1');
        expect(plot).toBeDefined();
        expect(plot.plotId).toEqual('testId1');
        expect(plot.plotImageId).toEqual('testId1-image1');
    });

    test('primePlot() using parameters (visRoot, plotId) with a plotId that does not exist', () => {
        const plot= primePlot(visRoot, 'IAmNotDefined');
        expect(plot).toBeFalsy();
    });
});


describe('Test PlotViewUtil.getPlotViewById()', () => {

    test('getPlotViewById() using parameters (PlotView[], plotId)', () => {
        const pv= getPlotViewById(pvAry, 'testId-xxxx');
        expect(pv).toBeDefined();
        expect(pv.plotId).toEqual('testId-xxxx');
    });

    test('getPlotViewById() using parameters (visRoot, plotId)', () => {
        const pv= getPlotViewById(visRoot, 'testId-xxxx');
        expect(pv).toBeDefined();
        expect(pv.plotId).toEqual('testId-xxxx');
    });

    test('getPlotViewById() using parameters (visRoot, plotId) with a plotId that does not exist', () => {
        const pv= getPlotViewById(visRoot, 'IAmNotDefined');
        expect(pv).toBeFalsy();
    });

});

describe('Test PlotViewUtil active PlotView functions', () => {
    test('isActivePlotView(visRoot, plotId)', () => {
        expect(isActivePlotView(visRoot, ACTIVE_ID)).toBeTruthy();
        expect(isActivePlotView(visRoot,'testId-zzzz')).toBeFalsy();
    });

    test('getActivePlotView(visRoot) ', () => {
        const pv= getActivePlotView(visRoot);
        expect(pv).toBeDefined();
        expect(pv.plotId).toEqual(ACTIVE_ID);
    });
});

describe('Less used PlotViewUtil functions', () => {
    test('primePlotType)', () => {
        const pv= getPlotViewById(pvAry, 'testId-xxxx');
        expect(primePlotType(pv)).toEqual('image');
    });

    test('findPlot)', () => {
        const pv= getPlotViewById(pvAry, 'testId1');
        const plot= findPlot(pv, 'testId1-image1');
        expect(plot).toBeDefined();
        expect(plot.plotImageId).toEqual('testId1-image1');
    });

});



//=============================================================
//---------- HDU, cube, and wavelength functions
//=============================================================

/**
 * make an image plot
 * @param {Object} p
 * @param {String} p.plotId
 * @param {number} p.idx - index of the plot in the PlotView
 * @param {number} [p.hdu] - the SPOT_EXT header value, no header if undefined
 * @param {number} [p.cubePlane] - plane of a cube, not a cube if undefined
 * @param {number} [p.cubeLength]
 * @param {Object} [p.linearWL] - linear wavelength, {crval, cdelt, units}, wavelength= crval + cdelt*cubePlane
 * @return {Object}
 */
const makeImagePlot= ({plotId, idx, hdu, cubePlane, cubeLength, linearWL}) => ({
    plotId,
    plotType: 'image',
    plotImageId: `${plotId}-image${idx}`,
    header: hdu!==undefined ? {SPOT_EXT: {value: hdu+''}} : {},
    cubeCtx: cubePlane!==undefined ? {cubePlane, cubeLength} : undefined,
    wlData: linearWL ? {hasPlainOnlyCoordInfo: true} : undefined,
    wlDataAry: linearWL ? [{
        spectralCoords: [{
            algorithm: 'LINEAR', N: 3, r_j: [0, 0, 1], pc_3j: [0, 0, 1],
            crval: linearWL.crval, cdelt: linearWL.cdelt, units: linearWL.units
        }]
    }] : undefined,
});

const makeHiPSPlot= ({plotId='hipsId', cubeDepth=1, cubeIdx=0, hipsProperties={}}={}) => ({
    plotId,
    plotType: 'hips',
    plotImageId: plotId+'-hips',
    cubeDepth,
    cubeIdx,
    hipsProperties,
});

const makePv= (plotId, plots, primeIdx=0, hduPlotStartIndexes=[0], multiHdu=false) => {
    const req= WebPlotRequest.makeURIPlotRequest('http://a/b/c.fits');
    req.setPlotId(plotId);
    const pv= makePlotView(plotId, req);
    pv.plots= plots;
    pv.primeIdx= primeIdx;
    pv.plotViewCtx= {...pv.plotViewCtx, hduPlotStartIndexes, multiHdu};
    return pv;
};

/**
 * A multi-HDU FITS file: HDU 0 is an image, HDU 1 is a 3 plane cube, HDU 2 is an image
 * plots: [hdu0, hdu1-plane0, hdu1-plane1, hdu1-plane2, hdu2]
 * @param {number} [primeIdx]
 */
const makeMultiHduPv= (primeIdx=0) => {
    const plotId= 'multiHdu';
    const plots= [
        makeImagePlot({plotId, idx:0, hdu:0}),
        makeImagePlot({plotId, idx:1, hdu:1, cubePlane:0, cubeLength:3}),
        makeImagePlot({plotId, idx:2, hdu:1, cubePlane:1, cubeLength:3}),
        makeImagePlot({plotId, idx:3, hdu:1, cubePlane:2, cubeLength:3}),
        makeImagePlot({plotId, idx:4, hdu:2}),
    ];
    return makePv(plotId, plots, primeIdx, [0,1,4], true);
};

const makeSingleImagePv= () => makePv('single', [makeImagePlot({plotId:'single', idx:0})]);

/**
 * a single HDU, 4 plane cube, wavelength= 2 + .5*plane
 * @param {number} [primeIdx]
 */
const makeWLCubePv= (primeIdx=0) => {
    const plotId= 'wlCube';
    const linearWL= {crval:2, cdelt:.5, units:'um'};
    const plots= [0,1,2,3].map( (i) =>
        makeImagePlot({plotId, idx:i, hdu:0, cubePlane:i, cubeLength:4, linearWL}));
    return makePv(plotId, plots, primeIdx);
};

const makeHiPSPv= (hipsPlot) => makePv(hipsPlot.plotId, [hipsPlot]);


describe('Test PlotViewUtil.isCube()', () => {
    test('undefined plot', () => expect(isCube(undefined)).toBe(false));
    test('image that is not a cube', () => expect(isCube(makeImagePlot({plotId:'a', idx:0}))).toBe(false));
    test('image that is a cube plane', () =>
        expect(isCube(makeImagePlot({plotId:'a', idx:0, cubePlane:1, cubeLength:3}))).toBe(true));
    test('HiPS with one plane', () => expect(isCube(makeHiPSPlot({cubeDepth:1}))).toBe(false));
    test('HiPS cube', () => expect(isCube(makeHiPSPlot({cubeDepth:5}))).toBe(true));
});

describe('Test PlotViewUtil.getCubePlaneIdx()', () => {
    test('undefined plot', () => expect(getCubePlaneIdx(undefined)).toBe(-1));
    test('image that is not a cube', () => expect(getCubePlaneIdx(makeImagePlot({plotId:'a', idx:0}))).toBe(-1));
    test('image cube plane', () =>
        expect(getCubePlaneIdx(makeImagePlot({plotId:'a', idx:0, cubePlane:2, cubeLength:3}))).toBe(2));
    test('HiPS cube', () => expect(getCubePlaneIdx(makeHiPSPlot({cubeDepth:5, cubeIdx:3}))).toBe(3));
});

describe('Test PlotViewUtil.getHDU()', () => {
    test('undefined plot', () => expect(getHDU(undefined)).toBe(0));
    test('image with SPOT_EXT', () => expect(getHDU(makeImagePlot({plotId:'a', idx:0, hdu:2}))).toBe(2));
    test('image without SPOT_EXT', () => expect(getHDU(makeImagePlot({plotId:'a', idx:0}))).toBe(0));
    test('HiPS', () => expect(getHDU(makeHiPSPlot())).toBe(0));
});

describe('Test PlotViewUtil.getHDUIndex()', () => {
    test('undefined PlotView', () => expect(getHDUIndex(undefined)).toBe(0));
    test('prime plot in the first HDU', () => expect(getHDUIndex(makeMultiHduPv(0))).toBe(0));
    test('prime plot is a plane of the cube in the second HDU', () => expect(getHDUIndex(makeMultiHduPv(2))).toBe(1));
    test('passing a plot from the third HDU', () => {
        const pv= makeMultiHduPv(0);
        expect(getHDUIndex(pv, pv.plots[4])).toBe(2);
    });
    test('single image', () => expect(getHDUIndex(makeSingleImagePv())).toBe(0));
});

describe('Test PlotViewUtil.isMultiHDUFits()', () => {
    test('undefined PlotView', () => expect(isMultiHDUFits(undefined)).toBe(false));
    test('multi-HDU file', () => expect(isMultiHDUFits(makeMultiHduPv())).toBe(true));
    test('single image', () => expect(isMultiHDUFits(makeSingleImagePv())).toBe(false));
});

describe('Test PlotViewUtil.convertHDUIdxToImageIdx()', () => {
    test('undefined PlotView', () => expect(convertHDUIdxToImageIdx(undefined, 0)).toBeUndefined());
    test('object that is not a PlotView', () => expect(convertHDUIdxToImageIdx({plots:[]}, 0)).toBeUndefined());
    test('single image always returns 0', () => expect(convertHDUIdxToImageIdx(makeSingleImagePv(), 1)).toBe(0));
    test('first HDU', () => expect(convertHDUIdxToImageIdx(makeMultiHduPv(), 0)).toBe(0));
    test('cube HDU, first plane by default', () => expect(convertHDUIdxToImageIdx(makeMultiHduPv(), 1)).toBe(1));
    test('cube HDU, plane 2', () => expect(convertHDUIdxToImageIdx(makeMultiHduPv(), 1, 2)).toBe(3));
    test('cube HDU, plane past the end of the cube returns the first plane', () =>
        expect(convertHDUIdxToImageIdx(makeMultiHduPv(), 1, 5)).toBe(1));
    test('image HDU ignores the cube index', () => expect(convertHDUIdxToImageIdx(makeMultiHduPv(), 2, 2)).toBe(4));
    test('HDU index past the last HDU returns 0', () => expect(convertHDUIdxToImageIdx(makeMultiHduPv(), 3)).toBe(0));
    test('follow uses the cube plane of the prime plot', () =>
        expect(convertHDUIdxToImageIdx(makeMultiHduPv(3), 1, 'follow')).toBe(3));
});

describe('Test PlotViewUtil.isDrawLayerAttached()', () => {
    const dl= {plotIdAry: ['plot1', 'plot2']};
    test('undefined draw layer', () => expect(isDrawLayerAttached(undefined, 'plot1')).toBe(false));
    test('attached plot', () => expect(isDrawLayerAttached(dl, 'plot2')).toBe(true));
    test('plot that is not attached', () => expect(isDrawLayerAttached(dl, 'plot3')).toBe(false));
});

describe('Test PlotViewUtil.hasWLInfo()', () => {
    test('undefined plot', () => expect(hasWLInfo(undefined)).toBe(false));
    test('image without wavelength info', () => expect(hasWLInfo(makeImagePlot({plotId:'a', idx:0}))).toBe(false));
    test('image with plane only wavelength info', () => expect(hasWLInfo(makeWLCubePv().plots[0])).toBe(true));
    test('image with pixel level wavelength info', () =>
        expect(hasWLInfo({...makeImagePlot({plotId:'a', idx:0}), wlData:{hasPixelLevelCoordInfo:true}})).toBe(true));
    test('HiPS cube with a wavelength for every plane', () =>
        expect(hasWLInfo(makeHiPSPlot({cubeDepth:3, hipsProperties:{data_cube_crval3_arr:'1.5 2.5 3.5'}}))).toBe(true));
    test('HiPS cube without data_cube_crval3_arr', () => expect(hasWLInfo(makeHiPSPlot({cubeDepth:3}))).toBe(false));
    test('HiPS cube with the wrong number of wavelengths', () =>
        expect(hasWLInfo(makeHiPSPlot({cubeDepth:4, hipsProperties:{data_cube_crval3_arr:'1.5 2.5 3.5'}}))).toBe(false));
    test('HiPS cube with a wavelength that is not a number', () =>
        expect(hasWLInfo(makeHiPSPlot({cubeDepth:3, hipsProperties:{data_cube_crval3_arr:'1.5 abc 3.5'}}))).toBe(false));
    test('HiPS that is not a cube', () =>
        expect(hasWLInfo(makeHiPSPlot({cubeDepth:1, hipsProperties:{data_cube_crval3_arr:'1.5'}}))).toBe(false));
});

describe('Test PlotViewUtil.getCubePlaneFromWavelength()', () => {
    const hipsPv= makeHiPSPv(makeHiPSPlot({cubeDepth:3, hipsProperties:{data_cube_crval3_arr:'1.5 2.5 3.5'}}));
    test('HiPS cube, wavelength of the second plane', () => expect(getCubePlaneFromWavelength(hipsPv, 2.5)).toBe(1));
    test('HiPS cube, wavelength not in the cube', () => expect(getCubePlaneFromWavelength(hipsPv, 9)).toBe(-1));
    test('HiPS cube without wavelength info', () =>
        expect(getCubePlaneFromWavelength(makeHiPSPv(makeHiPSPlot({cubeDepth:3})), 2.5)).toBe(-1));
    test('image cube with linear wavelength', () => expect(getCubePlaneFromWavelength(makeWLCubePv(), 3)).toBe(2));
    test('image cube with linear wavelength, wavelength not in the cube', () =>
        expect(getCubePlaneFromWavelength(makeWLCubePv(), 2.25)).toBe(-1));
    test('image cube without wavelength info', () =>
        expect(getCubePlaneFromWavelength(makeMultiHduPv(1), 3)).toBe(-1));
});



//=============================================================
//---------- PlotView and PlotGroup lookup functions
//=============================================================

describe('Test PlotViewUtil.getPlotViewAry()', () => {
    test('undefined ref', () => expect(getPlotViewAry(undefined)).toBeUndefined());
    test('using visRoot', () => expect(getPlotViewAry(visRoot)).toBe(pvAry));
    test('using a PlotView array', () => expect(getPlotViewAry(pvAry)).toBe(pvAry));
    test('using an empty array', () => expect(getPlotViewAry([])).toEqual([]));
    test('filter by plot group', () => {
        const groupAry= getPlotViewAry(visRoot, 'aTestGroup');
        expect(groupAry.map( (pv) => pv.plotId)).toEqual(['testId1', ACTIVE_ID, 'testId-yyyy']);
    });
    test('filter by a plot group that does not exist', () => expect(getPlotViewAry(pvAry, 'noGroup')).toEqual([]));
    test('filter by plot group using an empty array', () => expect(getPlotViewAry([], 'aTestGroup')).toEqual([]));
    test('filter by plot group using an object that is not a visRoot', () =>
        expect(getPlotViewAry({}, 'aTestGroup')).toEqual([]));
});

describe('Test PlotViewUtil.getPlotViewIdxById()', () => {
    test('using visRoot', () => expect(getPlotViewIdxById(visRoot, 'testId-yyyy')).toBe(2));
    test('using a PlotView array', () => expect(getPlotViewIdxById(pvAry, 'testId-zzzz')).toBe(3));
    test('plotId that does not exist', () => expect(getPlotViewIdxById(pvAry, 'noId')).toBe(-1));
    test('undefined plotId', () => expect(getPlotViewIdxById(pvAry, undefined)).toBeUndefined());
});

const plotGroupAry= [
    {plotGroupId: 'aTestGroup', overlayColorLock: true},
    {plotGroupId: 'anotherGroup', overlayColorLock: false},
];

describe('Test PlotViewUtil.getPlotGroupIdxById()', () => {
    test('using an object with a plotGroupAry', () =>
        expect(getPlotGroupIdxById({plotGroupAry}, 'anotherGroup')).toBe(1));
    test('using a PlotGroup array', () => expect(getPlotGroupIdxById(plotGroupAry, 'aTestGroup')).toBe(0));
    test('plotGroupId that does not exist', () => expect(getPlotGroupIdxById(plotGroupAry, 'noGroup')).toBe(-1));
    test('undefined ref', () => expect(getPlotGroupIdxById(undefined, 'aTestGroup')).toBeUndefined());
});

describe('Test PlotViewUtil.findPlotGroup()', () => {
    test('find a group', () => expect(findPlotGroup('anotherGroup', plotGroupAry)).toBe(plotGroupAry[1]));
    test('plotGroupId that does not exist', () => expect(findPlotGroup('noGroup', plotGroupAry)).toBeUndefined());
    test('undefined plotGroupId', () => expect(findPlotGroup(undefined, plotGroupAry)).toBeUndefined());
    test('undefined plotGroupAry', () => expect(findPlotGroup('aTestGroup', undefined)).toBeUndefined());
});

describe('Test PlotViewUtil.getPlotViewProxyById()', () => {
    const proxy= {plotId: 'proxyId'};
    const proxyVisRoot= {...visRoot, plotProxyAry: [{plotId: 'otherProxyId'}, proxy]};
    test('find a proxy', () => expect(getPlotViewProxyById(proxyVisRoot, 'proxyId')).toBe(proxy));
    test('plotId that does not exist', () => expect(getPlotViewProxyById(proxyVisRoot, 'noId')).toBeUndefined());
    test('visRoot without proxies', () => expect(getPlotViewProxyById(visRoot, 'proxyId')).toBeUndefined());
    test('undefined visRoot', () => expect(getPlotViewProxyById(undefined, 'proxyId')).toBeUndefined());
    test('undefined plotId', () => expect(getPlotViewProxyById(proxyVisRoot, undefined)).toBeUndefined());
});

describe('Test PlotViewUtil.getOnePvOrGroup()', () => {
    const [lockedGroup, unlockedGroup]= plotGroupAry;
    const ids= (pvList) => pvList.map( (pv) => pv.plotId);
    test('group with overlay color lock returns the whole group', () =>
        expect(ids(getOnePvOrGroup(pvAry, 'testId1', lockedGroup))).toEqual(['testId1', ACTIVE_ID, 'testId-yyyy']));
    test('group without overlay color lock returns only the PlotView', () =>
        expect(ids(getOnePvOrGroup(pvAry, 'testId-zzzz', unlockedGroup))).toEqual(['testId-zzzz']));
    test('forceAllInGroup returns the whole group without a lock', () => {
        const noLockGroup= {plotGroupId: 'aTestGroup', overlayColorLock: false};
        expect(ids(getOnePvOrGroup(pvAry, 'testId1', noLockGroup))).toEqual(['testId1']);
        expect(ids(getOnePvOrGroup(pvAry, 'testId1', noLockGroup, true))).toEqual(['testId1', ACTIVE_ID, 'testId-yyyy']);
    });
    test('PlotView not in the locked group returns only the PlotView', () =>
        expect(ids(getOnePvOrGroup(pvAry, 'testId-zzzz', lockedGroup))).toEqual(['testId-zzzz']));
});

describe('Test PlotViewUtil.makeUniquePlotIdFromBase()', () => {
    const mockVisRoot= (plotIdAry) => {
        const plotViewAry= plotIdAry.map( (plotId) => makeMockPv(plotId, 'aTestGroup', 1));
        jest.spyOn(VisStoreRoots, 'visRoot').mockReturnValue({plotViewAry, activePlotId: plotIdAry[0]});
    };
    afterEach(() => jest.restoreAllMocks());

    test('plotId that is not used is returned unchanged', () => {
        mockVisRoot(['otherId']);
        expect(makeUniquePlotIdFromBase('plotA')).toBe('plotA');
    });
    test('plotId that is used gets a number', () => {
        mockVisRoot(['plotA']);
        expect(makeUniquePlotIdFromBase('plotA')).toBe('plotA - 1');
    });
    test('number is one more than the highest number used', () => {
        mockVisRoot(['plotA', 'plotA - 1', 'plotA - 3']);
        expect(makeUniquePlotIdFromBase('plotA')).toBe('plotA - 4');
    });
    test('plotIds that only start with the same text are not counted', () => {
        mockVisRoot(['plotA', 'plotAB - 7']);
        expect(makeUniquePlotIdFromBase('plotA')).toBe('plotA - 1');
    });
    test('numbered plotIds that are not a whole number are not counted', () => {
        mockVisRoot(['plotA', 'plotA - 2', 'plotA - 9x']);
        expect(makeUniquePlotIdFromBase('plotA')).toBe('plotA - 3');
    });
});



//=============================================================
//---------- Image type, HDU, and cube functions
//=============================================================

const makeStubProjection= ({specified=true, implemented=true, celestial=true, name='TAN', raOffset=0}={}) => ({
    isSpecified: () => specified,
    isImplemented: () => implemented,
    coordSys: {isCelestial: () => celestial},
    getProjectionName: () => name,
    getWorldCoords: (x,y) => makeWorldPt(10+raOffset+x*.001, 20+y*.001, CoordinateSys.EQ_J2000),
});

const withProjection= (plot, {w=100, h=100, ...projOptions}={}) =>
    ({...plot, dataWidth:w, dataHeight:h, projection: makeStubProjection(projOptions)});

const withThreeColor= (plot, threeColor) => ({...plot, plotState: {isThreeColor: () => threeColor}});

describe('Test PlotViewUtil.hasWCSProjection()', () => {
    const plot= withProjection(makeImagePlot({plotId:'a', idx:0}));
    test('undefined ref', () => expect(hasWCSProjection(undefined)).toBe(false));
    test('plot with a celestial projection', () => expect(hasWCSProjection(plot)).toBe(true));
    test('PlotView with a celestial projection', () => expect(hasWCSProjection(makePv('a', [plot]))).toBe(true));
    test('plot without a projection', () => expect(hasWCSProjection(makeImagePlot({plotId:'a', idx:0}))).toBe(false));
    test('projection that is not specified', () =>
        expect(hasWCSProjection(withProjection(plot, {specified:false}))).toBe(false));
    test('projection that is not implemented', () =>
        expect(hasWCSProjection(withProjection(plot, {implemented:false}))).toBe(false));
    test('non-celestial projection', () => {
        const linearPlot= withProjection(plot, {celestial:false});
        expect(hasWCSProjection(linearPlot)).toBe(false);
        expect(hasWCSProjection(linearPlot, true)).toBe(true);
    });
});

describe('Test PlotViewUtil.isThreeColor()', () => {
    const plot= makeImagePlot({plotId:'a', idx:0});
    test('undefined plot', () => expect(isThreeColor(undefined)).toBe(false));
    test('three color plot', () => expect(isThreeColor(withThreeColor(plot, true))).toBe(true));
    test('single band plot', () => expect(isThreeColor(withThreeColor(plot, false))).toBe(false));
    test('PlotView with a three color plot', () =>
        expect(isThreeColor(makePv('a', [withThreeColor(plot, true)]))).toBe(true));
});

describe('Test PlotViewUtil.isMultiImageFits()', () => {
    test('multi-HDU file', () => expect(isMultiImageFits(makeMultiHduPv())).toBe(true));
    test('single HDU cube', () => expect(isMultiImageFits(makeWLCubePv())).toBe(true));
    test('single image', () => expect(isMultiImageFits(makeSingleImagePv())).toBe(false));
    test('HiPS is never a multi image FITS', () => {
        const pv= makePv('hipsId', [makeHiPSPlot({cubeDepth:3}), makeHiPSPlot({cubeDepth:3})]);
        expect(isMultiImageFits(pv)).toBe(false);
    });
});

describe('Test PlotViewUtil.isMultiImageFitsWithSameArea()', () => {
    const makeAreaPv= (projOptionsAry) => makePv('area',
        projOptionsAry.map( (projOptions,idx) => withProjection(makeImagePlot({plotId:'area', idx}), projOptions)));
    test('all images cover the same area', () => expect(isMultiImageFitsWithSameArea(makeAreaPv([{},{},{}]))).toBe(true));
    test('single image', () => expect(isMultiImageFitsWithSameArea(makeAreaPv([{}]))).toBe(false));
    test('an image with a different size', () =>
        expect(isMultiImageFitsWithSameArea(makeAreaPv([{},{w:50}]))).toBe(false));
    test('an image with a different projection', () =>
        expect(isMultiImageFitsWithSameArea(makeAreaPv([{},{name:'SIN'}]))).toBe(false));
    test('an image covering a different part of the sky', () =>
        expect(isMultiImageFitsWithSameArea(makeAreaPv([{},{raOffset:1}]))).toBe(false));
});

describe('Test PlotViewUtil.getHduPlotStartIndexes()', () => {
    test('undefined PlotView', () => expect(getHduPlotStartIndexes(undefined)).toBeUndefined());
    test('multi-HDU file', () => expect(getHduPlotStartIndexes(makeMultiHduPv())).toEqual([0,1,4]));
});

describe('Test PlotViewUtil.getHDUCount()', () => {
    test('undefined PlotView', () => expect(getHDUCount(undefined)).toBe(0));
    test('multi-HDU file', () => expect(getHDUCount(makeMultiHduPv())).toBe(3));
    test('single image', () => expect(getHDUCount(makeSingleImagePv())).toBe(1));
});

describe('Test PlotViewUtil.getNumberOfCubesInPV() and hasImageCubes()', () => {
    const withCubeCnt= (pv, cubeCnt) => ({...pv, plotViewCtx: {...pv.plotViewCtx, cubeCnt}});
    test('undefined PlotView', () => {
        expect(getNumberOfCubesInPV(undefined)).toBe(0);
        expect(hasImageCubes(undefined)).toBe(false);
    });
    test('image PlotView with a cube', () => {
        const pv= withCubeCnt(makeMultiHduPv(), 1);
        expect(getNumberOfCubesInPV(pv)).toBe(1);
        expect(hasImageCubes(pv)).toBe(true);
    });
    test('image PlotView without a cube', () => {
        const pv= withCubeCnt(makeSingleImagePv(), 0);
        expect(getNumberOfCubesInPV(pv)).toBe(0);
        expect(hasImageCubes(pv)).toBe(false);
    });
    test('HiPS cube is not an image cube', () => {
        const pv= withCubeCnt(makeHiPSPv(makeHiPSPlot({cubeDepth:3})), 1);
        expect(getNumberOfCubesInPV(pv)).toBe(0);
        expect(hasImageCubes(pv)).toBe(false);
    });
});

describe('Test PlotViewUtil.getCubeLength()', () => {
    test('undefined', () => expect(getCubeLength(undefined)).toBe(0));
    test('image cube plot', () =>
        expect(getCubeLength(makeImagePlot({plotId:'a', idx:0, cubePlane:0, cubeLength:4}))).toBe(4));
    test('PlotView with an image cube as the prime plot', () => expect(getCubeLength(makeMultiHduPv(2))).toBe(3));
    test('PlotView with an image that is not a cube as the prime plot', () =>
        expect(getCubeLength(makeMultiHduPv(4))).toBe(0));
    test('HiPS cube', () => expect(getCubeLength(makeHiPSPlot({cubeDepth:6}))).toBe(6));
    test('HiPS that is not a cube', () => expect(getCubeLength(makeHiPSPlot({cubeDepth:1}))).toBe(0));
});

describe('Test PlotViewUtil.getPrimaryPlotHdu()', () => {
    test('undefined PlotView', () => expect(getPrimaryPlotHdu(undefined)).toBe(0));
    test('prime plot in the cube HDU', () => expect(getPrimaryPlotHdu(makeMultiHduPv(2))).toBe(1));
    test('prime plot in the last HDU', () => expect(getPrimaryPlotHdu(makeMultiHduPv(4))).toBe(2));
    test('HiPS', () => expect(getPrimaryPlotHdu(makeHiPSPv(makeHiPSPlot()))).toBe(0));
});

describe('Test PlotViewUtil.convertImageIdxToHDU()', () => {
    test('undefined PlotView', () => expect(convertImageIdxToHDU(undefined, 0).hduIdx).toBeUndefined());
    test('single image', () => {
        const {hduIdx, cubeIdx}= convertImageIdxToHDU(makeSingleImagePv(), 0);
        expect(hduIdx).toBe(0);
        expect(cubeIdx).toBeUndefined();
    });
    test('image index past the last plot', () => expect(convertImageIdxToHDU(makeMultiHduPv(), 9).hduIdx).toBe(0));
    test('image in the first HDU', () => {
        const {hduIdx, cubeIdx}= convertImageIdxToHDU(makeMultiHduPv(), 0);
        expect(hduIdx).toBe(0);
        expect(cubeIdx).toBeFalsy();
    });
    test('third plane of the cube HDU', () => {
        const {hduIdx, cubeIdx, cube}= convertImageIdxToHDU(makeMultiHduPv(), 3);
        expect(hduIdx).toBe(1);
        expect(cubeIdx).toBe(2);
        expect(cube).toBe(true);
    });
    test('image in the last HDU', () => expect(convertImageIdxToHDU(makeMultiHduPv(), 4).hduIdx).toBe(2));
    test('converts back with convertHDUIdxToImageIdx', () => {
        const pv= makeMultiHduPv();
        pv.plots.forEach( (p,imageIdx) => {
            const {hduIdx, cubeIdx}= convertImageIdxToHDU(pv, imageIdx);
            expect(convertHDUIdxToImageIdx(pv, hduIdx, cubeIdx || 0)).toBe(imageIdx);
        });
    });
});



//=============================================================
//---------- Equality and cloning functions
//=============================================================

describe('Test PlotViewUtil.isPlotViewsEqual()', () => {
    const pv= makeMultiHduPv(1);
    test('no keys compares the PlotView object', () => {
        expect(isPlotViewsEqual(pv, pv)).toBe(true);
        expect(isPlotViewsEqual(pv, {...pv})).toBe(false);
    });
    test('copy with the same keys and the same prime plot', () =>
        expect(isPlotViewsEqual(pv, {...pv}, ['plotId','primeIdx'])).toBe(true));
    test('copy with a different PlotView key', () =>
        expect(isPlotViewsEqual(pv, {...pv, primeIdx:2}, ['plotId','primeIdx'])).toBe(false));
    test('different prime plot object without plot keys', () => {
        const pv2= {...pv, plots: pv.plots.map( (p) => ({...p}))};
        expect(isPlotViewsEqual(pv, pv2, ['plotId'])).toBe(false);
    });
    test('different prime plot object with matching plot keys', () => {
        const pv2= {...pv, plots: pv.plots.map( (p) => ({...p}))};
        expect(isPlotViewsEqual(pv, pv2, ['plotId'], ['plotImageId'])).toBe(true);
    });
    test('different prime plot object with a plot key that does not match', () => {
        const pv2= {...pv, plots: pv.plots.map( (p) => ({...p, plotImageId: p.plotImageId+'-x'}))};
        expect(isPlotViewsEqual(pv, pv2, ['plotId'], ['plotImageId'])).toBe(false);
    });
    test('only one PlotView has a plot', () => expect(isPlotViewsEqual(pv, {...pv, plots:[]}, ['plotId'])).toBe(false));
    test('image and HiPS', () => {
        const hipsPv= {...pv, plots: [makeHiPSPlot({plotId:pv.plotId})], primeIdx:0};
        expect(isPlotViewsEqual(pv, hipsPv, ['plotId'], ['plotId'])).toBe(false);
    });
});

describe('Test PlotViewUtil.isPlotViewArysEqual()', () => {
    const pvAry1= [makeMultiHduPv(), makeSingleImagePv()];
    test('both empty', () => expect(isPlotViewArysEqual([], undefined)).toBe(true));
    test('only one empty', () => expect(isPlotViewArysEqual(pvAry1, [])).toBe(false));
    test('different lengths', () => expect(isPlotViewArysEqual(pvAry1, [pvAry1[0]], ['plotId'])).toBe(false));
    test('same PlotViews', () => expect(isPlotViewArysEqual(pvAry1, [...pvAry1])).toBe(true));
    test('copies that match on the keys', () =>
        expect(isPlotViewArysEqual(pvAry1, pvAry1.map( (pv) => ({...pv})), ['plotId'])).toBe(true));
    test('one PlotView that does not match', () =>
        expect(isPlotViewArysEqual(pvAry1, [pvAry1[0], {...pvAry1[1], plotId:'x'}], ['plotId'])).toBe(false));
});

describe('Test PlotViewUtil.pvEqualExScroll()', () => {
    const pv= makeMultiHduPv(1);
    test('same object', () => expect(pvEqualExScroll(pv, pv)).toBe(true));
    test('undefined PlotView', () => expect(pvEqualExScroll(pv, undefined)).toBe(false));
    test('only the scroll position is different', () =>
        expect(pvEqualExScroll(pv, {...pv, scrollX:200, scrollY:300})).toBe(true));
    test('different prime plot index', () => expect(pvEqualExScroll(pv, {...pv, primeIdx:2})).toBe(false));
    test('different rotation', () => expect(pvEqualExScroll(pv, {...pv, rotation:90})).toBe(false));
    test('different plotViewCtx object', () =>
        expect(pvEqualExScroll(pv, {...pv, plotViewCtx:{...pv.plotViewCtx}})).toBe(false));
    test('new plots array with the same prime plot', () =>
        expect(pvEqualExScroll(pv, {...pv, plots:[...pv.plots]})).toBe(true));
    test('prime plot with a different zoom factor', () => {
        const plots= pv.plots.map( (p,idx) => idx===pv.primeIdx ? {...p, zoomFactor:2} : p);
        expect(pvEqualExScroll(pv, {...pv, plots})).toBe(false);
    });
    test('different overlay PlotViews', () =>
        expect(pvEqualExScroll(pv, {...pv, overlayPlotViews:[{plotId:'overlay'}]})).toBe(false));
});

describe('Test PlotViewUtil.clonePvAry()', () => {
    test('undefined ref', () => expect(clonePvAry(undefined, 'testId1', {primeIdx:2})).toBeUndefined());
    test('change one PlotView', () => {
        const newAry= clonePvAry(visRoot, 'testId-yyyy', {primeIdx:2});
        expect(newAry).not.toBe(pvAry);
        expect(newAry[2]).not.toBe(pvAry[2]);
        expect(newAry[2].primeIdx).toBe(2);
        expect(newAry[2].plotId).toBe('testId-yyyy');
        expect(pvAry[2].primeIdx).toBe(0);
        [0,1,3].forEach( (i) => expect(newAry[i]).toBe(pvAry[i]));
    });
    test('plotId that does not exist', () => {
        const newAry= clonePvAry(pvAry, 'noId', {primeIdx:2});
        expect(newAry).not.toBe(pvAry);
        newAry.forEach( (pv,i) => expect(pv).toBe(pvAry[i]));
    });
});

describe('Test PlotViewUtil.clonePvAryWithPv()', () => {
    test('undefined ref', () => expect(clonePvAryWithPv(undefined, pvAry[0])).toBeUndefined());
    test('replace one PlotView', () => {
        const newPv= {...pvAry[3], primeIdx:3};
        const newAry= clonePvAryWithPv(visRoot, newPv);
        expect(newAry).not.toBe(pvAry);
        expect(newAry[3]).toBe(newPv);
        [0,1,2].forEach( (i) => expect(newAry[i]).toBe(pvAry[i]));
    });
    test('PlotView that is not in the array', () => {
        const newAry= clonePvAryWithPv(pvAry, {plotId:'noId'});
        newAry.forEach( (pv,i) => expect(pv).toBe(pvAry[i]));
    });
});

//=============================================================
//---------- Other lookup functions
//=============================================================

describe('Test PlotViewUtil.getPlotStateAry()', () => {
    const state= (name) => ({name});
    test('prime plot only', () => {
        const pv= makePv('a', [{...makeImagePlot({plotId:'a', idx:0}), plotState:state('prime')}]);
        expect(getPlotStateAry(pv)).toEqual([state('prime')]);
    });
    test('prime plot and image overlays', () => {
        const pv= {
            ...makePv('a', [{...makeImagePlot({plotId:'a', idx:0}), plotState:state('prime')}]),
            overlayPlotViews: [{plot:{plotState:state('overlay1')}}, {plot:undefined}, {plot:{plotState:state('overlay2')}}],
        };
        expect(getPlotStateAry(pv)).toEqual([state('prime'), state('overlay1'), state('overlay2')]);
    });
    test('no prime plot', () => expect(getPlotStateAry(makePv('a', []))).toEqual([]));
});

describe('Test PlotViewUtil.isPlotIdInPvNewPlotInfoAry()', () => {
    const pvNewPlotInfoAry= [{plotId:'plot1'}, {plotId:'plot2'}];
    test('plotId in the array', () => expect(isPlotIdInPvNewPlotInfoAry(pvNewPlotInfoAry, 'plot2')).toBe(true));
    test('plotId not in the array', () => expect(isPlotIdInPvNewPlotInfoAry(pvNewPlotInfoAry, 'plot3')).toBe(false));
    test('undefined array', () => expect(isPlotIdInPvNewPlotInfoAry(undefined, 'plot1')).toBe(false));
    test('undefined plotId', () => expect(isPlotIdInPvNewPlotInfoAry(pvNewPlotInfoAry, undefined)).toBe(false));
});

describe('Test PlotViewUtil.getNextHiPSPlotId()', () => {
    test('each call returns the next numbered id', () => {
        const id1= getNextHiPSPlotId();
        const id2= getNextHiPSPlotId();
        expect(id1).toMatch(/^aHiPSId\d+$/);
        expect(id2).toMatch(/^aHiPSId\d+$/);
        expect(Number(id2.substring('aHiPSId'.length))).toBe(Number(id1.substring('aHiPSId'.length))+1);
    });
});



//=============================================================
//---------- Wavelength functions
//=============================================================

/**
 * make an image plot with pixel level wavelength and a band width,
 * wavelength= 2 + .5*(p + cubePlane) where p is the 1 based FITS pixel x, band width= .1
 * @param {number} cubePlane
 */
const makePixelLevelWLPlot= (cubePlane) => ({
    ...makeImagePlot({plotId:'pixWL', idx:cubePlane, hdu:0, cubePlane, cubeLength:4}),
    wlData: {hasPixelLevelCoordInfo: true},
    wlDataAry: [{
        spectralCoords: [
            {algorithm:'LINEAR', N:3, r_j:[0,0,1], pc_3j:[1,0,1], crval:2, cdelt:.5, units:'um'},
            {algorithm:'LINEAR', N:3, r_j:[0,0,1], pc_3j:[0,0,0], crval:.1, cdelt:1, units:'nm'},
        ]
    }],
});

const hipsWLProps= {data_cube_crval3_arr:'1.5 2.5 3.5', data_cube_bunit3:'nm'};

describe('Test PlotViewUtil wavelength parse status functions', () => {
    const failedPlot= {...makeImagePlot({plotId:'a', idx:0}), wlData:{failReason:'bad CTYPE3'}};
    test('wavelengthInfoParsedSuccessfully()', () => {
        expect(wavelengthInfoParsedSuccessfully(makeWLCubePv().plots[0])).toBe(true);
        expect(wavelengthInfoParsedSuccessfully(makeImagePlot({plotId:'a', idx:0}))).toBe(true);
        expect(wavelengthInfoParsedSuccessfully(failedPlot)).toBe(false);
    });
    test('getWavelengthParseFailReason()', () => {
        expect(getWavelengthParseFailReason(failedPlot)).toBe('bad CTYPE3');
        expect(getWavelengthParseFailReason(makeWLCubePv().plots[0])).toBeUndefined();
        expect(getWavelengthParseFailReason(undefined)).toBeUndefined();
    });
});

describe('Test PlotViewUtil.hasPixelLevelWLInfo() and hasPlaneOnlyWLInfo()', () => {
    test('undefined plot', () => {
        expect(hasPixelLevelWLInfo(undefined)).toBe(false);
        expect(hasPlaneOnlyWLInfo(undefined)).toBe(false);
    });
    test('image with plane only wavelength', () => {
        const plot= makeWLCubePv().plots[0];
        expect(hasPixelLevelWLInfo(plot)).toBe(false);
        expect(hasPlaneOnlyWLInfo(plot)).toBe(true);
    });
    test('image with pixel level wavelength', () => {
        const plot= makePixelLevelWLPlot(0);
        expect(hasPixelLevelWLInfo(plot)).toBe(true);
        expect(hasPlaneOnlyWLInfo(plot)).toBe(false);
    });
    test('HiPS cube with wavelengths is neither', () => {
        const plot= makeHiPSPlot({cubeDepth:3, hipsProperties:hipsWLProps});
        expect(hasPixelLevelWLInfo(plot)).toBe(false);
        expect(hasPlaneOnlyWLInfo(plot)).toBe(false);
    });
});

describe('Test PlotViewUtil.getPtSpectralCoords() and getPtWavelength()', () => {
    test('undefined plot', () => {
        expect(getPtSpectralCoords(undefined, undefined, 0)).toEqual([0,0]);
        expect(getPtWavelength(undefined, undefined, 0)).toBe(0);
    });
    test('image without wavelength', () =>
        expect(getPtSpectralCoords(makeImagePlot({plotId:'a', idx:0}), undefined, 0)).toEqual([0,0]));
    test('image with plane only wavelength does not need a point', () => {
        const plot= makeWLCubePv().plots[0];
        expect(getPtWavelength(plot, undefined, 0)).toBe(2);
        expect(getPtWavelength(plot, undefined, 3)).toBe(3.5);
    });
    test('image with pixel level wavelength uses the point', () => {
        const plot= makePixelLevelWLPlot(1);
        expect(getPtSpectralCoords(plot, makeImagePt(4.5, 3.5), 1)).toEqual([5, .1]);
        expect(getPtWavelength(plot, makeImagePt(0.5, 3.5), 1)).toBe(3);
    });
    test('image with pixel level wavelength without a point', () =>
        expect(getPtSpectralCoords(makePixelLevelWLPlot(1), undefined, 1)).toEqual([0,0]));
    test('HiPS cube with wavelengths', () => {
        const plot= makeHiPSPlot({cubeDepth:3, hipsProperties:hipsWLProps});
        expect(getPtSpectralCoords(plot, undefined, 1)).toEqual([2.5,0]);
        expect(getPtWavelength(plot, undefined, 2)).toBe(3.5);
    });
    test('HiPS cube plane past the end', () =>
        expect(getPtWavelength(makeHiPSPlot({cubeDepth:3, hipsProperties:hipsWLProps}), undefined, 5)).toBe(0));
    test('HiPS cube without wavelengths', () =>
        expect(getPtSpectralCoords(makeHiPSPlot({cubeDepth:3}), undefined, 1)).toEqual([0,0]));
});

describe('Test PlotViewUtil.getAllWaveLengthsForCube()', () => {
    test('undefined PlotView', () => expect(getAllWaveLengthsForCube(undefined)).toBeUndefined());
    test('image cube with plane only wavelength', () =>
        expect(getAllWaveLengthsForCube(makeWLCubePv(2))).toEqual([2, 2.5, 3, 3.5]));
    test('image cube with pixel level wavelength uses the point', () => {
        const pv= makePv('pixWL', [0,1,2,3].map( (i) => makePixelLevelWLPlot(i)));
        expect(getAllWaveLengthsForCube(pv, makeImagePt(4.5, 3.5))).toEqual([4.5, 5, 5.5, 6]);
    });
    test('image cube without wavelength', () => expect(getAllWaveLengthsForCube(makeMultiHduPv(1))).toBeUndefined());
    test('image that is not a cube', () => expect(getAllWaveLengthsForCube(makeSingleImagePv())).toBeUndefined());
    test('HiPS cube with wavelengths', () =>
        expect(getAllWaveLengthsForCube(makeHiPSPv(makeHiPSPlot({cubeDepth:3, hipsProperties:hipsWLProps}))))
            .toEqual([1.5, 2.5, 3.5]));
    test('HiPS cube without wavelengths', () =>
        expect(getAllWaveLengthsForCube(makeHiPSPv(makeHiPSPlot({cubeDepth:3})))).toBeUndefined());
});

describe('Test PlotViewUtil.getWaveLengthUnits() and getBandWidthUnits()', () => {
    test('undefined plot', () => {
        expect(getWaveLengthUnits(undefined)).toBe('');
        expect(getBandWidthUnits(undefined)).toBe('');
    });
    test('image without wavelength', () => expect(getWaveLengthUnits(makeImagePlot({plotId:'a', idx:0}))).toBe(''));
    test('image with wavelength', () => expect(getWaveLengthUnits(makeWLCubePv().plots[0])).toBe('um'));
    test('image with a band width', () => {
        const plot= makePixelLevelWLPlot(0);
        expect(getWaveLengthUnits(plot)).toBe('um');
        expect(getBandWidthUnits(plot)).toBe('nm');
    });
    test('HiPS cube uses data_cube_bunit3', () => {
        const plot= makeHiPSPlot({cubeDepth:3, hipsProperties:hipsWLProps});
        expect(getWaveLengthUnits(plot)).toBe('nm');
        expect(getBandWidthUnits(plot)).toBe('');
    });
    test('HiPS cube with a data_cube_bunit3 of null', () =>
        expect(getWaveLengthUnits(makeHiPSPlot({cubeDepth:3, hipsProperties:{...hipsWLProps, data_cube_bunit3:'null'}})))
            .toBe(''));
    test('HiPS cube with data_cube_bunit3 but no wavelengths', () =>
        expect(getWaveLengthUnits(makeHiPSPlot({cubeDepth:3, hipsProperties:{data_cube_bunit3:'nm'}}))).toBe(''));
});

describe('Test PlotViewUtil.getFormattedWaveLengthUnits()', () => {
    const MICRON= 'μm';
    test('micron strings use the micron symbol', () => {
        ['um', 'micron', 'microns', 'Microns', 'micrometers'].forEach( (u) =>
            expect(getFormattedWaveLengthUnits(u)).toBe(MICRON));
    });
    test('other units are unchanged', () => {
        expect(getFormattedWaveLengthUnits('nm')).toBe('nm');
        expect(getFormattedWaveLengthUnits('')).toBe('');
    });
    test('only replace a whole unit string unless anyPartOfStr', () => {
        expect(getFormattedWaveLengthUnits('erg/s/cm2/um')).toBe('erg/s/cm2/um');
        expect(getFormattedWaveLengthUnits('erg/s/cm2/um', true)).toBe(`erg/s/cm2/${MICRON}`);
    });
    test('using a plot', () => {
        expect(getFormattedWaveLengthUnits(makeWLCubePv().plots[0])).toBe(MICRON);
        expect(getFormattedWaveLengthUnits(makeHiPSPlot({cubeDepth:3, hipsProperties:hipsWLProps}))).toBe('nm');
    });
});

describe('Test PlotViewUtil.getWaveLengthUnitsPrecision()', () => {
    test('undefined', () => expect(getWaveLengthUnitsPrecision(undefined)).toBe(4));
    test('empty units', () => expect(getWaveLengthUnitsPrecision('')).toBe(4));
    test('micron units', () => {
        expect(getWaveLengthUnitsPrecision('um')).toBe(3);
        expect(getWaveLengthUnitsPrecision('microns')).toBe(3);
    });
    test('other units', () => expect(getWaveLengthUnitsPrecision('nm')).toBe(4));
    test('using a plot', () => {
        expect(getWaveLengthUnitsPrecision(makeWLCubePv().plots[0])).toBe(3);
        expect(getWaveLengthUnitsPrecision(makeHiPSPlot({cubeDepth:3, hipsProperties:hipsWLProps}))).toBe(4);
    });
});



//=============================================================
//---------- Overlay and position group functions
//=============================================================

const makeGroupPv= (plotId, plotGroupId, plot) => ({...makePv(plotId, plot ? [plot] : []), plotGroupId});
const makeGroupImagePlot= (plotId, threeColor=false) => withThreeColor(makeImagePlot({plotId, idx:0}), threeColor);

/**
 * groupA: two images, a three color image, a HiPS, and a PlotView without plots. groupB: one image
 */
const groupPvAry= [
    makeGroupPv('g1', 'groupA', makeGroupImagePlot('g1')),
    makeGroupPv('g2', 'groupA', makeGroupImagePlot('g2')),
    makeGroupPv('g3Color', 'groupA', makeGroupImagePlot('g3Color', true)),
    makeGroupPv('gHiPS', 'groupA', withThreeColor(makeHiPSPlot({plotId:'gHiPS'}), false)),
    makeGroupPv('gEmpty', 'groupA'),
    makeGroupPv('b1', 'groupB', makeGroupImagePlot('b1')),
];
const [g1Pv]= groupPvAry;

const makeGroupVisRoot= ({overlayColorLock=true, positionLock=false, activePlotId='g1'}={}) => ({
    plotViewAry: groupPvAry,
    activePlotId,
    positionLock,
    plotGroupAry: [{plotGroupId:'groupA', overlayColorLock}, {plotGroupId:'groupB', overlayColorLock:false}],
});

const ids= (pvList) => pvList.map( (pv) => pv.plotId);

/**
 * call operate with a mock function and return the plotIds it was called with
 * @param {Function} operate - called with the mock function
 */
const getOperatedIds= (operate) => {
    const opFunc= jest.fn();
    operate(opFunc);
    return opFunc.mock.calls.map( ([pv]) => pv.plotId);
};

const markChanged= (pv) => ({...pv, changed:true});
const changedIds= (pvList) => pvList.filter( (pv) => pv.changed).map( (pv) => pv.plotId);

describe('Test PlotViewUtil.hasOverlayColorLock()', () => {
    test('PlotView in a locked group', () =>
        expect(hasOverlayColorLock(g1Pv, {plotGroupId:'groupA', overlayColorLock:true})).toBe(true));
    test('PlotView in an unlocked group', () =>
        expect(hasOverlayColorLock(g1Pv, {plotGroupId:'groupA', overlayColorLock:false})).toBe(false));
    test('PlotView in a different group', () =>
        expect(hasOverlayColorLock(g1Pv, {plotGroupId:'groupB', overlayColorLock:true})).toBe(false));
    test('undefined group', () => expect(hasOverlayColorLock(g1Pv, undefined)).toBe(false));
});

describe('Test PlotViewUtil.getPlotViewIdListByPositionLock()', () => {
    test('undefined plotId', () => expect(getPlotViewIdListByPositionLock(makeGroupVisRoot(), undefined)).toEqual([]));
    test('without position lock, using a plotId', () =>
        expect(getPlotViewIdListByPositionLock(makeGroupVisRoot(), 'g2')).toEqual(['g2']));
    test('without position lock, using a PlotView', () =>
        expect(getPlotViewIdListByPositionLock(makeGroupVisRoot(), g1Pv)).toEqual(['g1']));
    test('with position lock, every PlotView with plots in any group', () =>
        expect(getPlotViewIdListByPositionLock(makeGroupVisRoot({positionLock:true}), 'g1'))
            .toEqual(['g1', 'g2', 'g3Color', 'gHiPS', 'b1']));
});

describe('Test PlotViewUtil.getPlotViewIdListInOverlayGroup()', () => {
    test('undefined plotId', () => expect(getPlotViewIdListInOverlayGroup(makeGroupVisRoot(), undefined)).toEqual([]));
    test('plotId that does not exist', () => expect(getPlotViewIdListInOverlayGroup(makeGroupVisRoot(), 'noId')).toEqual([]));
    test('locked group returns every PlotView in the group', () =>
        expect(getPlotViewIdListInOverlayGroup(makeGroupVisRoot(), 'g2'))
            .toEqual(['g1', 'g2', 'g3Color', 'gHiPS', 'gEmpty']));
    test('unlocked group returns only the PlotView', () =>
        expect(getPlotViewIdListInOverlayGroup(makeGroupVisRoot({overlayColorLock:false}), g1Pv)).toEqual(['g1']));
    test('PlotView in an unlocked group', () =>
        expect(getPlotViewIdListInOverlayGroup(makeGroupVisRoot(), 'b1')).toEqual(['b1']));
});

describe('Test PlotViewUtil.getAllPlotViewIdByOverlayLock()', () => {
    const vr= makeGroupVisRoot();
    test('undefined plotId', () => expect(getAllPlotViewIdByOverlayLock(vr, undefined)).toEqual([]));
    test('plotId that does not exist', () => expect(getAllPlotViewIdByOverlayLock(vr, 'noId')).toEqual([]));
    test('unlocked group returns only the PlotView', () =>
        expect(getAllPlotViewIdByOverlayLock(makeGroupVisRoot({overlayColorLock:false}), 'g1')).toEqual(['g1']));
    test('locked group returns every PlotView', () =>
        expect(getAllPlotViewIdByOverlayLock(vr, 'g1')).toEqual(ids(groupPvAry)));
    test('locked group, only PlotViews with plots', () =>
        expect(getAllPlotViewIdByOverlayLock(vr, g1Pv, true)).toEqual(['g1', 'g2', 'g3Color', 'gHiPS', 'b1']));
    test('locked group, only PlotViews with plots of the same type', () =>
        expect(getAllPlotViewIdByOverlayLock(vr, g1Pv, true, true)).toEqual(['g1', 'g2', 'g3Color', 'b1']));
});

describe('Test PlotViewUtil.plotInActiveGroup()', () => {
    test('no active plot', () =>
        expect(plotInActiveGroup(makeGroupVisRoot({activePlotId:null}), 'g1')).toBe(false));
    test('undefined visRoot', () => expect(plotInActiveGroup(undefined, 'g1')).toBe(false));
    test('plot in the active group', () => expect(plotInActiveGroup(makeGroupVisRoot(), 'gHiPS')).toBe(true));
    test('plot in a different group', () => expect(plotInActiveGroup(makeGroupVisRoot(), 'b1')).toBe(false));
});

describe('Test PlotViewUtil.operateOnOthersInOverlayColorGroup()', () => {
    const vr= makeGroupVisRoot();
    test('undefined source PlotView', () =>
        expect(getOperatedIds( (f) => operateOnOthersInOverlayColorGroup(vr, undefined, f))).toEqual([]));
    test('locked group, others in the group of the same type', () =>
        expect(getOperatedIds( (f) => operateOnOthersInOverlayColorGroup(vr, g1Pv, f)))
            .toEqual(['g2', 'g3Color', 'gEmpty']));
    test('ignore three color', () =>
        expect(getOperatedIds( (f) => operateOnOthersInOverlayColorGroup(vr, g1Pv, f, true))).toEqual(['g2', 'gEmpty']));
    test('any plot type', () =>
        expect(getOperatedIds( (f) => operateOnOthersInOverlayColorGroup(vr, g1Pv, f, false, true)))
            .toEqual(['g2', 'g3Color', 'gHiPS', 'gEmpty']));
    test('unlocked group', () =>
        expect(getOperatedIds( (f) =>
            operateOnOthersInOverlayColorGroup(makeGroupVisRoot({overlayColorLock:false}), g1Pv, f))).toEqual([]));
});

describe('Test PlotViewUtil.operateOnOthersInPositionGroup()', () => {
    const vr= makeGroupVisRoot({positionLock:true});
    test('without position lock', () =>
        expect(getOperatedIds( (f) => operateOnOthersInPositionGroup(makeGroupVisRoot(), g1Pv, f))).toEqual([]));
    test('with position lock, others of the same type in any group', () =>
        expect(getOperatedIds( (f) => operateOnOthersInPositionGroup(vr, g1Pv, f)))
            .toEqual(['g2', 'g3Color', 'gEmpty', 'b1']));
    test('ignore three color', () =>
        expect(getOperatedIds( (f) => operateOnOthersInPositionGroup(vr, g1Pv, f, true))).toEqual(['g2', 'gEmpty', 'b1']));
    test('any plot type', () =>
        expect(getOperatedIds( (f) => operateOnOthersInPositionGroup(vr, g1Pv, f, false, true)))
            .toEqual(['g2', 'g3Color', 'gHiPS', 'gEmpty', 'b1']));
});

describe('Test PlotViewUtil.matchPlotViewByPositionGroup()', () => {
    test('without position lock returns the same array', () =>
        expect(matchPlotViewByPositionGroup(makeGroupVisRoot(), g1Pv, groupPvAry, false, markChanged)).toBe(groupPvAry));
    test('with position lock, changes others of the same type', () => {
        const newAry= matchPlotViewByPositionGroup(makeGroupVisRoot({positionLock:true}), g1Pv, groupPvAry, false, markChanged);
        expect(changedIds(newAry)).toEqual(['g2', 'g3Color', 'gEmpty', 'b1']);
        expect(newAry[0]).toBe(g1Pv);
    });
    test('with position lock, any plot type', () => {
        const newAry= matchPlotViewByPositionGroup(makeGroupVisRoot({positionLock:true}), g1Pv, groupPvAry, true, markChanged);
        expect(changedIds(newAry)).toEqual(['g2', 'g3Color', 'gHiPS', 'gEmpty', 'b1']);
    });
});

describe('Test PlotViewUtil.applyToOnePvOrAll()', () => {
    test('plotId that does not exist', () =>
        expect(applyToOnePvOrAll(true, groupPvAry, 'noId', false, markChanged)).toBeUndefined());
    test('only the PlotView', () =>
        expect(changedIds(applyToOnePvOrAll(false, groupPvAry, 'g2', false, markChanged))).toEqual(['g2']));
    test('all PlotViews of the same type', () =>
        expect(changedIds(applyToOnePvOrAll(true, groupPvAry, 'g2', false, markChanged)))
            .toEqual(['g1', 'g2', 'g3Color', 'gEmpty', 'b1']));
    test('all PlotViews of any type', () =>
        expect(changedIds(applyToOnePvOrAll(true, groupPvAry, 'g2', true, markChanged))).toEqual(ids(groupPvAry)));
});

describe('Test PlotViewUtil.applyToOnePvOrOverlayGroup()', () => {
    const [lockedA]= makeGroupVisRoot().plotGroupAry;
    const unlockedA= {plotGroupId:'groupA', overlayColorLock:false};
    test('plotId that does not exist', () =>
        expect(applyToOnePvOrOverlayGroup(groupPvAry, 'noId', lockedA, false, markChanged)).toBeUndefined());
    test('no group, only the PlotView', () =>
        expect(changedIds(applyToOnePvOrOverlayGroup(groupPvAry, 'g2', undefined, false, markChanged))).toEqual(['g2']));
    test('unlocked group, only the PlotView', () =>
        expect(changedIds(applyToOnePvOrOverlayGroup(groupPvAry, 'g2', unlockedA, false, markChanged))).toEqual(['g2']));
    test('locked group, the group PlotViews of the same type', () =>
        expect(changedIds(applyToOnePvOrOverlayGroup(groupPvAry, 'g2', lockedA, false, markChanged)))
            .toEqual(['g1', 'g2', 'g3Color', 'gEmpty']));
    test('locked group, the group PlotViews of any type', () =>
        expect(changedIds(applyToOnePvOrOverlayGroup(groupPvAry, 'g2', lockedA, true, markChanged)))
            .toEqual(['g1', 'g2', 'g3Color', 'gHiPS', 'gEmpty']));
});


//=============================================================
//---------- Overlay and draw layer functions
//=============================================================

const dlAry= [
    {drawLayerId:'dl1', drawLayerTypeId:'typeA', displayGroupId:'dg1', plotIdAry:['p1','p2'], visiblePlotIdAry:['p1'],
        title:'Layer 1'},
    {drawLayerId:'dl2', drawLayerTypeId:'typeB', displayGroupId:'dg2', plotIdAry:['p2'], visiblePlotIdAry:['p2'],
        title:{p2:'Layer 2 on p2'}, autoFormatTitle:true},
    {drawLayerId:'dl3', drawLayerTypeId:'typeA', displayGroupId:'dg1', plotIdAry:['p3'], visiblePlotIdAry:[],
        title:'Layer 3', autoFormatTitle:true},
];
const dlRoot= {drawLayerAry: dlAry};
const dlIds= (list) => list.map( (dl) => dl.drawLayerId);

describe('Test PlotViewUtil.getAllDrawLayersForPlot()', () => {
    test('undefined ref', () => expect(getAllDrawLayersForPlot(undefined, 'p1')).toBeUndefined());
    test('undefined plotId', () => expect(getAllDrawLayersForPlot(dlRoot, undefined)).toEqual([]));
    test('no draw layers', () => expect(getAllDrawLayersForPlot([], 'p1')).toEqual([]));
    test('attached draw layers', () => expect(dlIds(getAllDrawLayersForPlot(dlRoot, 'p2'))).toEqual(['dl1', 'dl2']));
    test('visible draw layers', () => expect(dlIds(getAllDrawLayersForPlot(dlAry, 'p2', true))).toEqual(['dl2']));
    test('attached but not visible', () => expect(getAllDrawLayersForPlot(dlAry, 'p3', true)).toEqual([]));
});

describe('Test PlotViewUtil.getConnectedPlotsIds()', () => {
    test('undefined ref', () => expect(getConnectedPlotsIds(undefined, 'dl1')).toEqual([]));
    test('draw layer that does not exist', () => expect(getConnectedPlotsIds(dlRoot, 'noDl')).toEqual([]));
    test('attached plots', () => expect(getConnectedPlotsIds(dlRoot, 'dl1')).toEqual(['p1', 'p2']));
    test('visible plots', () => expect(getConnectedPlotsIds(dlAry, 'dl1', true)).toEqual(['p1']));
});

describe('Test PlotViewUtil draw layer lookup functions', () => {
    test('getDrawLayerByType() returns the first of the type', () => {
        expect(getDrawLayerByType(dlRoot, 'typeA')).toBe(dlAry[0]);
        expect(getDrawLayerByType(dlAry, 'typeB')).toBe(dlAry[1]);
        expect(getDrawLayerByType(dlAry, 'typeC')).toBeUndefined();
        expect(getDrawLayerByType(undefined, 'typeA')).toBeUndefined();
    });
    test('getDrawLayersByType() returns all of the type', () => {
        expect(dlIds(getDrawLayersByType(dlRoot, 'typeA'))).toEqual(['dl1', 'dl3']);
        expect(getDrawLayersByType(dlAry, 'typeC')).toEqual([]);
        expect(getDrawLayersByType(undefined, 'typeA')).toBeUndefined();
    });
    test('getDrawLayerById()', () => {
        expect(getDrawLayerById(dlRoot, 'dl2')).toBe(dlAry[1]);
        expect(getDrawLayerById(dlAry, 'noDl')).toBeUndefined();
        expect(getDrawLayerById(undefined, 'dl2')).toBeUndefined();
    });
    test('getDrawLayersByDisplayGroup() returns the first draw layer in the display group', () => {
        expect(getDrawLayersByDisplayGroup(dlRoot, 'dg1')).toBe(dlAry[0]);
        expect(getDrawLayersByDisplayGroup(dlAry, 'noGroup')).toBeUndefined();
    });
});

describe('Test PlotViewUtil.isDrawLayerVisible()', () => {
    test('undefined draw layer', () => expect(isDrawLayerVisible(undefined, 'p1')).toBe(false));
    test('visible plot', () => expect(isDrawLayerVisible(dlAry[0], 'p1')).toBe(true));
    test('attached but not visible plot', () => expect(isDrawLayerVisible(dlAry[0], 'p2')).toBe(false));
});

describe('Test PlotViewUtil.getLayerTitle()', () => {
    test('title without auto format', () => expect(getLayerTitle('p1', dlAry[0])).toBe('Layer 1'));
    test('auto format title by plotId', () => expect(getLayerTitle('p2', dlAry[1])).toBe('Layer 2 on p2'));
    test('auto format title that is a string', () => expect(getLayerTitle('p3', dlAry[2])).toBe('Layer 3'));
});

describe('Test PlotViewUtil.getOverlayById() and getOverlayByPvAndId()', () => {
    const overlay1= {imageOverlayId:'overlay1'};
    const overlay2= {imageOverlayId:'overlay2'};
    const overlayPv= {...makePv('withOverlays', [makeImagePlot({plotId:'withOverlays', idx:0})]),
        overlayPlotViews:[overlay1, overlay2]};
    test('getOverlayById()', () => {
        expect(getOverlayById(overlayPv, 'overlay2')).toBe(overlay2);
        expect(getOverlayById(overlayPv, 'noOverlay')).toBeUndefined();
        expect(getOverlayById(undefined, 'overlay1')).toBeUndefined();
    });
    test('getOverlayByPvAndId()', () => {
        const ary= [makeSingleImagePv(), overlayPv];
        expect(getOverlayByPvAndId(ary, 'withOverlays', 'overlay1')).toBe(overlay1);
        expect(getOverlayByPvAndId(ary, 'single', 'overlay1')).toBeUndefined();
        expect(getOverlayByPvAndId(ary, 'noId', 'overlay1')).toBeUndefined();
    });
});

describe('Test PlotViewUtil.getAllCanvasLayersForPlot()', () => {
    const addCanvas= (id) => {
        const c= document.createElement('canvas');
        if (id) c.id= id;
        document.body.appendChild(c);
    };
    afterEach(() => { document.body.innerHTML= ''; });

    test('image layers first then draw layers, each in index order', () => {
        ['dl-1.1-plotX', 'image-1-plotX', 'dl-0.2-plotX', 'image-0-otherPlot', undefined, 'image-0-plotX', 'dl-0.1-plotX']
            .forEach(addCanvas);
        expect(getAllCanvasLayersForPlot('plotX').map( (c) => c.id))
            .toEqual(['image-0-plotX', 'image-1-plotX', 'dl-0.1-plotX', 'dl-0.2-plotX', 'dl-1.1-plotX']);
    });
    test('no canvases for the plot', () => {
        addCanvas('image-0-otherPlot');
        expect(getAllCanvasLayersForPlot('plotX')).toEqual([]);
    });
});



//=============================================================
//---------- Geometry and view functions
//=============================================================

describe('Test PlotViewUtil.getFoV()', () => {
    const DEV_PT_TYPE= makeDevicePt(0,0).type;
    const VIEW_WIDTH= 500;

    /**
     * Stub CysConverter.make so world coordinates are along the equator, then distance in degrees is the
     * difference in RA. A device x is devScale/zoomFactor degrees per pixel, an image x is .001 degrees per pixel.
     * @param {Object} p
     * @param {number} [p.devScale]
     * @param {boolean} [p.deviceFails] - device points are outside the projection
     * @param {boolean} [p.imageFails] - image points are outside the projection
     */
    const stubConverter= ({devScale=.01, deviceFails=false, imageFails=false}={}) => {
        jest.spyOn(CysConverter, 'make').mockImplementation( () => ({
            zoomFactor: 1,
            getWorldCoords(pt) {
                if (pt.type===DEV_PT_TYPE) {
                    return deviceFails ? null : makeWorldPt(10 + pt.x*devScale/this.zoomFactor, 0, CoordinateSys.EQ_J2000);
                }
                return imageFails ? null : makeWorldPt(10 + pt.x*.001, 0, CoordinateSys.EQ_J2000);
            },
        }));
    };
    afterEach(() => jest.restoreAllMocks());

    const makeFoVPlot= ({wrapping=false, viewDim={width:VIEW_WIDTH, height:400}}={}) => ({
        ...makeImagePlot({plotId:'fov', idx:0}),
        viewDim,
        dataWidth: 1000,
        screenSize: {width: 2000, height: 1600},
        projection: {isWrappingProjection: () => wrapping},
    });
    const makeFoVHiPSPlot= (maptype) => ({
        ...makeHiPSPlot({plotId:'fovHiPS'}),
        viewDim: {width:VIEW_WIDTH, height:400},
        projection: {isWrappingProjection: () => false, header: {maptype}},
    });

    test('undefined', () => expect(getFoV(undefined)).toBe(false));
    test('no view size', () => {
        stubConverter();
        expect(getFoV(makeFoVPlot({viewDim:{width:0, height:0}}))).toBe(false);
    });
    test('distance across the view', () => {
        stubConverter();
        expect(getFoV(makeFoVPlot())).toBeCloseTo((VIEW_WIDTH-2)*.01, 6);
    });
    test('using a PlotView', () => {
        stubConverter();
        expect(getFoV(makePv('fov', [makeFoVPlot()]))).toBeCloseTo((VIEW_WIDTH-2)*.01, 6);
    });
    test('alternate zoom factor', () => {
        stubConverter();
        expect(getFoV(makeFoVPlot(), 2)).toBeCloseTo((VIEW_WIDTH-2)*.01/2, 6);
    });
    test('all sky image doubles the distance from the edge to the center', () => {
        stubConverter();
        expect(getFoV(makeFoVPlot({wrapping:true}))).toBeCloseTo((VIEW_WIDTH/2-1)*.01*2, 6);
    });
    test('outside the projection, all sky image', () => {
        stubConverter({deviceFails:true});
        expect(getFoV(makeFoVPlot({wrapping:true}))).toBe(360);
    });
    test('outside the projection, HiPS', () => {
        stubConverter({deviceFails:true});
        expect(getFoV(makeFoVHiPSPlot(undefined))).toBe(180);
    });
    test('outside the projection, Aitoff HiPS', () => {
        stubConverter({deviceFails:true});
        expect(getFoV(makeFoVHiPSPlot(HIPS_AITOFF))).toBe(360);
    });
    test('outside the projection, image uses the image width scaled to the view', () => {
        stubConverter({deviceFails:true});
        // image width is 1000*.001= 1 degree, the view shows 500 of 2000 screen pixels
        expect(getFoV(makeFoVPlot())).toBeCloseTo(VIEW_WIDTH/2000, 6);
    });
    test('outside the projection, image with an alternate zoom factor', () => {
        stubConverter({deviceFails:true});
        // the view shows 500 of 1000*4 screen pixels
        expect(getFoV(makeFoVPlot(), 4)).toBeCloseTo(VIEW_WIDTH/4000, 6);
    });
    test('outside the projection, image corners also outside', () => {
        stubConverter({deviceFails:true, imageFails:true});
        expect(getFoV(makeFoVPlot())).toBe(false);
    });
});


describe('Test PlotViewUtil.getCenterOfProjection()', () => {
    test('undefined plot', () => expect(getCenterOfProjection(undefined)).toBeUndefined());
    test('center is CRVAL1, CRVAL2 in the image coordinate system', () => {
        const plot= {...makeImagePlot({plotId:'a', idx:0}), imageCoordSys: CoordinateSys.GALACTIC,
            projection: {header: {crval1: 120.5, crval2: -30.25}}};
        const wp= getCenterOfProjection(plot);
        expect(wp.x).toBe(120.5);
        expect(wp.y).toBe(-30.25);
        expect(wp.cSys).toBe(CoordinateSys.GALACTIC);
    });
});

describe('Test PlotViewUtil.findCurrentCenterPoint()', () => {
    // view is 400x300, so the device center is (200,150). With no rotation the screen point is device+scroll,
    // then image x= screenX/zoom, image y= dataHeight - screenY/zoom
    const viewDim= {width:400, height:300};
    const ZOOM= 2;
    const DATA_HEIGHT= 800;

    const makeCenterPv= ({scrollX=100, scrollY=50, rotation=0}={}) => {
        const plot= {
            ...makeImagePlot({plotId:'center', idx:0}),
            dataWidth: 1000, dataHeight: DATA_HEIGHT, zoomFactor: ZOOM,
            affTrans: makeTransform(0, 0, scrollX, scrollY, rotation, false, false, viewDim),
        };
        return {...makePv('center', [plot]), viewDim, scrollX, scrollY, rotation, flipX:false, flipY:false};
    };
    const expectImagePt= (pt, x, y) => {
        expect(pt.x).toBeCloseTo(x, 6);
        expect(pt.y).toBeCloseTo(y, 6);
    };

    test('PlotView without a plot', () => expect(findCurrentCenterPoint(makePv('empty', []))).toBeUndefined());
    test('uses the PlotView scroll', () =>
        expectImagePt(findCurrentCenterPoint(makeCenterPv()), (200+100)/ZOOM, DATA_HEIGHT-(150+50)/ZOOM));
    test('passed scroll overrides the PlotView scroll', () =>
        expectImagePt(findCurrentCenterPoint(makeCenterPv(), 300, 250), (200+300)/ZOOM, DATA_HEIGHT-(150+250)/ZOOM));
    test('PlotView without a scroll uses 0,0', () => {
        const pv= {...makeCenterPv(), scrollX:undefined, scrollY:undefined};
        expectImagePt(findCurrentCenterPoint(pv), 200/ZOOM, DATA_HEIGHT-150/ZOOM);
    });
    test('only one passed scroll value uses the PlotView scroll', () =>
        expectImagePt(findCurrentCenterPoint(makeCenterPv(), 300), (200+100)/ZOOM, DATA_HEIGHT-(150+50)/ZOOM));
    test('rotation is around the view center so the center point does not move', () =>
        expectImagePt(findCurrentCenterPoint(makeCenterPv({rotation:90})), (200+100)/ZOOM, DATA_HEIGHT-(150+50)/ZOOM));
});

describe('Test PlotViewUtil.isImageExpanded()', () => {
    test('expanded modes', () => {
        expect(isImageExpanded(true)).toBe(true);
        expect(isImageExpanded(ExpandType.GRID)).toBe(true);
        expect(isImageExpanded(ExpandType.SINGLE)).toBe(true);
    });
    test('not expanded', () => {
        expect(isImageExpanded(ExpandType.COLLAPSE)).toBe(false);
        expect(isImageExpanded(false)).toBe(false);
        expect(isImageExpanded(undefined)).toBe(false);
    });
});


describe('Test PlotViewUtil.getCorners()', () => {
    /**
     * Stub CysConverter.make so an image point converts to a world point with the same x and y
     * @param {Function} [failPt] - return true for an image point that is outside the projection
     */
    const stubConverter= (failPt= () => false) => {
        jest.spyOn(CysConverter, 'make').mockImplementation( () => ({
            getWorldCoords: (pt) => failPt(pt) ? null : makeWorldPt(pt.x, pt.y, CoordinateSys.EQ_J2000),
        }));
    };
    afterEach(() => jest.restoreAllMocks());
    const plot= {...makeImagePlot({plotId:'corners', idx:0}), dataWidth:100, dataHeight:50};

    test('undefined plot', () => expect(getCorners(undefined)).toBeNull());
    test('corners in order: bottom left, bottom right, top right, top left', () => {
        stubConverter();
        expect(getCorners(plot).map( (wp) => [wp.x, wp.y])).toEqual([[0,0], [100,0], [100,50], [0,50]]);
    });
    test('a corner outside the projection', () => {
        stubConverter( (pt) => pt.x===100 && pt.y===50);
        expect(getCorners(plot)).toBeNull();
    });
});

describe('Test PlotViewUtil rotation matching', () => {
    /**
     * Stub the WebPlotAnalysis functions using values set on the test plot:
     * angle- the rotation angle of the image, north- the image is north up, eastLeft- east is left of north
     */
    beforeEach(() => {
        jest.spyOn(WebPlotAnalysis, 'getRotationAngle').mockImplementation( (plot) => plot?.angle ?? 0);
        jest.spyOn(WebPlotAnalysis, 'isPlotNorth').mockImplementation( (plot) => Boolean(plot?.north));
        jest.spyOn(WebPlotAnalysis, 'getCenterPtOfPlot').mockImplementation( () => makeWorldPt(10, 20));
        jest.spyOn(WebPlotAnalysis, 'isEastLeftOfNorth').mockImplementation( (plot) => plot?.eastLeft ?? true);
    });
    afterEach(() => jest.restoreAllMocks());

    const makeRotPv= ({angle=0, rotation=0, north=false, eastLeft=true, flipY=false, rotateNorthLock=false}={}) => {
        const pv= makePv('rot', [{...makeImagePlot({plotId:'rot', idx:0}), angle, north, eastLeft}]);
        return {...pv, rotation, flipY, plotViewCtx:{...pv.plotViewCtx, rotateNorthLock}};
    };

    describe('isRotationMatching()', () => {
        test('PlotView without a plot', () => expect(isRotationMatching(makePv('empty', []), makeRotPv())).toBe(false));
        test('both locked north up', () =>
            expect(isRotationMatching(makeRotPv({angle:30, rotateNorthLock:true}),
                makeRotPv({angle:100, rotateNorthLock:true}))).toBe(true));
        test('both north up and not rotated', () =>
            expect(isRotationMatching(makeRotPv({north:true}), makeRotPv({north:true}))).toBe(true));
        test('image angle plus PlotView rotation is the same', () =>
            expect(isRotationMatching(makeRotPv({angle:30, rotation:10}), makeRotPv({angle:20, rotation:20}))).toBe(true));
        test('within .9 degrees', () =>
            expect(isRotationMatching(makeRotPv({angle:40}), makeRotPv({angle:40.5}))).toBe(true));
        test('more than .9 degrees apart', () =>
            expect(isRotationMatching(makeRotPv({angle:40}), makeRotPv({angle:42}))).toBe(false));
        test('north up but rotated compares the angles', () =>
            expect(isRotationMatching(makeRotPv({north:true, rotation:90}), makeRotPv({north:true}))).toBe(false));
        test('angles that differ by 360', () =>
            expect(isRotationMatching(makeRotPv({angle:10}), makeRotPv({angle:10, rotation:360}))).toBe(true));
        test('angles on either side of 0', () =>
            expect(isRotationMatching(makeRotPv({angle:359.8}), makeRotPv({angle:.1}))).toBe(true));
        test('rotated north sums to 360', () =>
            expect(isRotationMatching(makeRotPv({angle:10, rotation:350}), makeRotPv({angle:359.9}))).toBe(true));
        test('east right of north subtracts the image angle', () =>
            expect(isRotationMatching(makeRotPv({angle:30, rotation:45, eastLeft:false}),
                makeRotPv({angle:10, rotation:25, eastLeft:false}))).toBe(true));
        test('flipped PlotView subtracts the image angle', () =>
            expect(isRotationMatching(makeRotPv({angle:30, rotation:45, flipY:true}),
                makeRotPv({angle:10, rotation:5}))).toBe(true));
    });

    describe('getMatchingRotationAngle()', () => {
        test('PlotView without a plot', () =>
            expect(getMatchingRotationAngle(makeRotPv(), makePv('empty', []))).toBe(0));
        test('same angle and no rotation', () =>
            expect(getMatchingRotationAngle(makeRotPv({angle:30}), makeRotPv({angle:30}))).toBeCloseTo(0, 6));
        test('different image angles', () =>
            expect(getMatchingRotationAngle(makeRotPv({angle:30}), makeRotPv({angle:10}))).toBeCloseTo(340, 6));
        test('master PlotView is rotated', () =>
            expect(getMatchingRotationAngle(makeRotPv({angle:30, rotation:45}), makeRotPv({angle:10}))).toBeCloseTo(295, 6));
        test('both have east right of north', () =>
            expect(getMatchingRotationAngle(makeRotPv({angle:30, rotation:45, eastLeft:false}),
                makeRotPv({angle:10, eastLeft:false}))).toBeCloseTo(25, 6));
        test('east is on different sides of north', () =>
            expect(getMatchingRotationAngle(makeRotPv({angle:30, rotation:45}),
                makeRotPv({angle:10, eastLeft:false}))).toBeCloseTo(85, 6));
        test('only the master PlotView is flipped', () =>
            expect(getMatchingRotationAngle(makeRotPv({angle:30, rotation:45, flipY:true}),
                makeRotPv({angle:10}))).toBeCloseTo(355, 6));
    });
});


describe('Test PlotViewUtil.isDefaultCoverageActive()', () => {
    const mockActivePlot= (activePlotId) => {
        const plotViewAry= [DEFAULT_COVERAGE_PLOT_ID, 'otherPlot'].map( (plotId) => makeMockPv(plotId, 'aTestGroup', 1));
        jest.spyOn(VisStoreRoots, 'visRoot').mockReturnValue({plotViewAry, activePlotId});
    };
    const makeMvr= (mounted) => [{viewerId:'otherViewer', mounted:true}, {viewerId:DEFAULT_COVERAGE_VIEWER_ID, mounted}];
    afterEach(() => jest.restoreAllMocks());

    test('coverage plot is active and the coverage viewer is mounted', () => {
        mockActivePlot(DEFAULT_COVERAGE_PLOT_ID);
        expect(isDefaultCoverageActive(makeMvr(true))).toBe(true);
    });
    test('coverage viewer is not mounted', () => {
        mockActivePlot(DEFAULT_COVERAGE_PLOT_ID);
        expect(isDefaultCoverageActive(makeMvr(false))).toBe(false);
    });
    test('no coverage viewer', () => {
        mockActivePlot(DEFAULT_COVERAGE_PLOT_ID);
        expect(isDefaultCoverageActive([{viewerId:'otherViewer', mounted:true}])).toBe(false);
        expect(isDefaultCoverageActive(undefined)).toBe(false);
    });
    test('another plot is active', () => {
        mockActivePlot('otherPlot');
        expect(isDefaultCoverageActive(makeMvr(true))).toBe(false);
    });
});

describe('Test PlotViewUtil.canConvertBetweenHipsAndFits()', () => {
    const withConversion= (hipsImageConversion) => {
        const pv= makeSingleImagePv();
        return {...pv, plotViewCtx:{...pv.plotViewCtx, hipsImageConversion}};
    };
    test('undefined PlotView', () => expect(canConvertBetweenHipsAndFits(undefined)).toBe(false));
    test('PlotView without HiPS image conversion', () => expect(canConvertBetweenHipsAndFits(makeSingleImagePv())).toBe(false));
    test('HiPS and image request roots', () =>
        expect(canConvertBetweenHipsAndFits(withConversion({hipsRequestRoot:{}, imageRequestRoot:{}}))).toBe(true));
    test('missing the image request root', () =>
        expect(canConvertBetweenHipsAndFits(withConversion({hipsRequestRoot:{}}))).toBe(false));
    test('missing the HiPS request root', () =>
        expect(canConvertBetweenHipsAndFits(withConversion({imageRequestRoot:{}}))).toBe(false));
});



//=============================================================
//---------- Store lookup functions
//=============================================================

describe('Test PlotViewUtil.refreshP() and currentP()', () => {
    // the store holds the visRoot defined at the top of this file: testId1 has 3 plots with primeIdx 1
    beforeEach(() => jest.spyOn(VisStoreRoots, 'visRoot').mockReturnValue(visRoot));
    afterEach(() => jest.restoreAllMocks());

    describe('refreshP()', () => {
        test('undefined', () => expect(refreshP(undefined)).toBeUndefined());
        test('object without a plotId', () => expect(refreshP({})).toBeUndefined());
        test('old copy of a PlotView returns the PlotView in the store', () =>
            expect(refreshP({...pvAry[0]})).toBe(pvAry[0]));
        test('old copy of a plot returns the prime plot in the store', () =>
            expect(refreshP({...pvAry[0].plots[1]})).toBe(pvAry[0].plots[1]));
        test('a plot that is not the prime plot still returns the prime plot', () =>
            expect(refreshP(pvAry[0].plots[2])).toBe(pvAry[0].plots[1]));
        test('PlotView that is no longer in the store', () =>
            expect(refreshP({...pvAry[0], plotId:'noId'})).toBeUndefined());
        test('object with a plotId that is not a PlotView or a plot', () =>
            expect(refreshP({plotId:'testId1'})).toBeUndefined());
    });

    describe('currentP()', () => {
        test('no plotId returns the active PlotView', () => {
            const {pv, plot, plotId, active}= currentP();
            expect(pv).toBe(pvAry[1]);
            expect(plot).toBe(pvAry[1].plots[0]);
            expect(plotId).toBe(ACTIVE_ID);
            expect(active).toBe(true);
        });
        test('plotId of a PlotView that is not active', () => {
            const {pv, plot, plotId, active}= currentP('testId1');
            expect(pv).toBe(pvAry[0]);
            expect(plot).toBe(pvAry[0].plots[1]);
            expect(plotId).toBe('testId1');
            expect(active).toBe(false);
        });
        test('plotId of the active PlotView', () => expect(currentP(ACTIVE_ID).active).toBe(true));
        test('plotId that does not exist', () => {
            const {pv, plot, plotId, active}= currentP('noId');
            expect(pv).toBeUndefined();
            expect(plot).toBeUndefined();
            expect(plotId).toBeUndefined();
            expect(active).toBe(false);
        });
    });
});
