/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'immutability-helper';
import Enum from 'enum';
import {PlotAttribute} from '../PlotAttribute';
import {isImage, isHiPS} from '../WebPlot.js';
import {WPConst} from '../WebPlotRequest';
import {makeScreenPt, makeDevicePt, makeImagePt} from '../Point';
import {getActiveTarget} from '../../core/AppDataCntlr.js';
import {getCenterPtOfPlot} from '../VisUtil.js';
import {getPlotViewById, matchPlotViewByPositionGroup, primePlot, findCurrentCenterPoint} from '../PlotViewUtil.js';
import {changeProjectionCenter} from '../HiPSUtil.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import {ZoomType} from '../ZoomType.js';
import {PlotPref} from '../PlotPref.js';
import {DEFAULT_THUMBNAIL_SIZE} from '../WebPlotRequest.js';
import {CCUtil, CysConverter} from '../CsysConverter.js';
import {getDefMenuItemKeys} from '../MenuItemKeys.js';
import {ExpandType, WcsMatchType} from '../ImagePlotCntlr.js';
import {updateTransform, makeTransform} from '../PlotTransformUtils.js';

export const ServerCallStatus= new Enum(['success', 'working', 'fail'], { ignoreCase: true });

/**
 * @global
 * @public
 * @typedef {Object} PlotView
 *
 * There is one PlotView object for each react ImageViewer.  A PlotView is uniquely identified by the plotId. The
 * plot id will not change for the life time of the plotView. A plot view can be connected to a plot group.  That is done
 * by the plotGroupId. There will be several plotViews in a plot group.
 *
 * PlotView is mostly about the viewing of the plot.  The plot data is contained in a WebPlot. A plotView can have an
 * array of WebPlots. The array length will only be one for normals fits files and n for multi image fits and cube fits
 * files. plots[primeIdx] refers to the plot currently showing in the plot view.
 *
 * @prop {String} plotId, immutable
 * @prop {String} plotGroupId, immutable
 * @prop {String} drawingSubGroupId, immutable
 * @peop {boolean} visible true when we draw the base image
 * @prop {Array.<WebPlot>} plots all the plots that this plotView can show, usually the image in the fits file
 * @prop {String} plottingStatusMsg, end user description of the what is doing on
 * @prop {String} serverCall, one of 'success', 'working', 'fail'
 * @prop {number} primeIdx, which of the plots array is active
 * @prop {number} scrollX scroll position X
 * @prop {number} scrollY scroll position Y
 * @prop {{width:number, height:number}} viewDim  size of viewable area  (div size: offsetWidth & offsetHeight)
 * @prop {Object} menuItemKeys - which toolbar button are enables for this plotView
 * @prop {Object} overlayPlotViews
 * @prop {Object} options
 * @prop {number} rotation if > 0 then the plot is rotated by this many degrees
 * @prop {boolean} flipY if true, the the plot is flipped on the Y axis
 * @prop {boolean} flipX - *not implemented*, if true, the the plot is flipped on the X axis
 * @prop {PlotViewContextData} plotViewCtx
 */

/**
 * @global
 * @public
 * @typedef {Object} PlotViewContextData
 * Various properties about this PlotView
 *
 * @prop {boolean} userCanDeletePlots true if this plotView can be deleted by the user
 * @prop {boolean} zoomLockingEnabled the plot will automaticly adjust the zoom when resized
 * @prop {UserZoomTypes} zoomLockingType the type of zoom lockeing
 * @prop {number} lastCollapsedZoomLevel used for returning from expanded mode, keeps recode of the level before expanded
 * @prop {HipsImageConversionSettings} hipsImageConversion -  if defined, then plotview can convert between hips and image
 * @prop {number} plotCounter index of how many plots, used for making next ID
 */

/**
 * @global
 * @public
 * @typedef {Object} HipsImageConversionSettings
 * @summary Parameters to do conversion between hips and images
 *
 * @prop {WebPlotParams|WebPlotRequest} hipsRequestRoot a request object that contains the base parameter to display a HiPS
 * @prop {WebPlotParams|WebPlotRequest} imageRequestRoot a request object that contains the base parameter to display an image. It must be a service type.
 * @prop {number} fovDegFallOver The field of view size to determine when to move between and HiPS and an image
 * @prop {number} fovMaxFitsSize how big this fits image can be
 * @prop {boolean} autoConvertOnZoom do auto convert on zoom
 */

/**
 * @global
 * @public
 * @typedef {Object} PVCreateOptions
 * Object used for creating the PlotView
 *
 * @prop {HipsImageConversionSettings} [hipsImageConversion] If object is defined and populated correctly then
 * the PlotView will convert between HiPS and Image
 * @prop {Object} [menuItemKeys] - defines which menu items shows on the toolbar
 * @prop {boolean} [userCanDeletePlots] - default to true, defines if a PlotView can be deleted by the user
 */


/**
 * @param {string} plotId
 * @param {WebPlotRequest} req
 * @param {PVCreateOptions} pvOptions options for this plot view
 * @return  {PlotView}
 */
export function makePlotView(plotId, req, pvOptions= {}) {
    const pv= {
        plotId, // immutable
        plotGroupId: req.getPlotGroupId(), //immutable
        drawingSubGroupId: req.getDrawingSubGroupId(), //immutable - todo, string, this is an id, should never change
        plots:[],
        visible: true,
        request: req && req.makeCopy(),
        plottingStatusMsg:'Plotting...',
        serverCall:'success', // one of 'success', 'working', 'fail'
        primeIdx: -1,
        scrollX : -1,   // in ScreenCoords
        scrollY : -1,   // in ScreenCoords
        affTrans: null,
        viewDim : {width:0, height:0}, // size of viewable area  (i.e. div size: offsetWidth & offsetHeight)
        overlayPlotViews: [],
        menuItemKeys: makeMenuItemKeys(req,pvOptions,getDefMenuItemKeys()), // normally will not change
        plotViewCtx: createPlotViewContextData(req, pvOptions),
        rotation: 0,
        flipY: false,
        flipX: false,
    };
    return pv;
}



/**
 *
 * @param {WebPlotRequest} req
 * @param {PVCreateOptions} pvOptions
 * @return {PlotViewContextData}
 */
function createPlotViewContextData(req, pvOptions={}) {
    const attributes= req.getAttributes();
    const plotViewCtx= {
        userCanDeletePlots: pvOptions?.userCanDeletePlots ?? true,
        annotationOps : req.getAnnotationOps(), // how titles are drawn
        rotateNorthLock : false,
        zoomLockingEnabled : false,
        zoomLockingType: UserZoomTypes.FIT, // can be FIT or FILL
        displayFixedTarget: pvOptions?.displayFixedTarget ?? true,
        lastCollapsedZoomLevel: 0,
        preferenceColorKey: attributes[PlotAttribute.PREFERENCE_COLOR_KEY],
        defThumbnailSize: DEFAULT_THUMBNAIL_SIZE,
        plotCounter:0 // index of how many plots, used for making next ID
    };

    const {hipsImageConversion:hi}= pvOptions;
    if (hi && hi.hipsRequestRoot && hi.imageRequestRoot && hi.fovDegFallOver) {  // confirm all three parameters are there
        const defaults= {autoConvertOnZoom: false};
        plotViewCtx.hipsImageConversion= {...defaults, ...hi};
        if (!hi.fovMaxFitsSize ) hi.fovMaxFitsSize= hi.fovDegFallOver;
    }
    return plotViewCtx;
}



//todo - this function should determine which menuItem are visible and which are hidden
// for now just return the default
function makeMenuItemKeys(req,pvOptions,defMenuItemKeys) {
    return {...defMenuItemKeys, ...pvOptions.menuItemKeys};
}

/**
 *
 * @param pv
 * @return {PlotView}
 */
export function initScrollCenterPoint(pv)  {
    if (isImage(primePlot(pv))) {
        return updatePlotViewScrollXY(pv,findScrollPtForCenter(pv));
    }
    else {
        const plot= primePlot(pv);
        if (!plot || !plot.attributes[PlotAttribute.FIXED_TARGET]) return pv;
        const wp= CCUtil.getWorldCoords(plot, plot.attributes[PlotAttribute.FIXED_TARGET]);
        return replacePrimaryPlot(pv, changeProjectionCenter(plot,wp));
    }
}


export function changePrimePlot(pv, nextIdx) {
    const {plots}= pv;
    if (!plots[nextIdx]) return pv;
    const currentScrollImPt= CCUtil.getImageCoords(primePlot(pv),makeScreenPt(pv.scrollX,pv.scrollY));
    pv= {...pv,primeIdx:nextIdx};

    const cc= CysConverter.make(plots[nextIdx]);
    if (cc.pointInData(currentScrollImPt)) {
        pv= updatePlotViewScrollXY(pv,cc.getScreenCoords(currentScrollImPt));
    }
    else {
        pv= initScrollCenterPoint(pv);
    }
    return updateTransform(pv);
}

/**
 * Replace the plotAry and overlayPlotViews into the PlotView, return a new PlotView
 * @param {PlotView} pv
 * @param {WebPlot[]} plotAry
 * @param {Array} overlayPlotViews
 * @param {ExpandType} expandedMode
 * @param {boolean} newPlot true, this is a new plot otherwise is is from a flip, rotate, etc
 * @return {PlotView}
 */
export function replacePlots(pv, plotAry, overlayPlotViews, expandedMode, newPlot) {

    pv= {...pv, plotViewCtx:{...pv.plotViewCtx}};

    if (overlayPlotViews) {
        const oPlotAry= overlayPlotViews.map( (opv) => opv.plot);
        pv.overlayPlotViews= pv.overlayPlotViews.map( (opv) => {
            const plot= oPlotAry.find( (p) => p.plotId===opv.imageOverlayId);
            return plot ? {...opv, plot} : opv;
        });
    }

    if (newPlot || pv.plots?.length !== plotAry.length) {
        pv.plots= plotAry;
    }
    else {
        const oldPlots= pv.plots;
        pv.plots= plotAry.map( (p,idx) => ({...p, relatedData:oldPlots[idx].relatedData}) );
    }


    pv.plots.forEach( (plot) => {
        plot.attributes= {...plot.attributes, ...getNewAttributes(plot)};
        plot.plotImageId= `${pv.plotId}--${pv.plotViewCtx.plotCounter}`;
        pv.plotViewCtx.plotCounter++;
    });


    if (pv.primeIdx<0 || pv.primeIdx>=pv.plots.length) pv.primeIdx=0;
    pv.plottingStatusMsg='';
    pv.serverCall='success';

    PlotPref.putCacheColorPref(pv.plotViewCtx.preferenceColorKey, pv.plots[pv.primeIdx].plotState);

    if (expandedMode===ExpandType.COLLAPSE) {
        pv.plotViewCtx.lastCollapsedZoomLevel= pv.plots[pv.primeIdx].zoomFactor;
    }
    else {
        pv.plotViewCtx.zoomLockingEnabled= primePlot(pv).plotState.getWebPlotRequest().getZoomType() !== ZoomType.LEVEL;
    }

    pv= initScrollCenterPoint(pv);

    return pv;
}

/**
 * create a copy of the PlotView with a new scroll position and a new view port if necessary
 * The scroll position is the top left visible point.
 * @param {PlotView} plotView the current plotView
 * @param {Point} newScrollPt  the screen point of the scroll position
 * @return {PlotView} new copy of plotView
 */
export function updatePlotViewScrollXY(plotView,newScrollPt) {
    if (!plotView) return plotView;
    if (!newScrollPt) return {...plotView, scrollX:undefined, scrollY:undefined};

    const plot= primePlot(plotView);
    if (!plot) return plotView;
    const {scrollWidth,scrollHeight}= getScrollSize(plotView);
    if (!scrollWidth || !scrollHeight) return plotView;

    const {x:scrollX,y:scrollY}= CCUtil.getScreenCoords(plot,newScrollPt);
    return updateTransform({...plotView, scrollX, scrollY});
}


/**
 * replace the PlotView in plotview array keyed by plotId
 * @param {Array.<PlotView>} plotViewAry
 * @param {PlotView} newPlotView
 * @return {Array.<PlotView>} new plotView array after return a plotview
 */
export function replacePlotView(plotViewAry,newPlotView) {
    return plotViewAry.map( (pv) => pv.plotId===newPlotView.plotId ? newPlotView : pv);
}

/**
 *
 * @param {PlotView} plotView
 * @param {WebPlot} primePlot
 * @return {PlotView} return the new PlotView object
 */
export function replacePrimaryPlot(plotView,primePlot) {
    return update(plotView, { plots : {[plotView.primeIdx] : { $set : primePlot } }} );
}

/**
 * scroll a plot view to a new screen pt, if positionLock is true then all the plot views in the group
 * will be scrolled to match
 * @param {VisRoot} visRoot
 * @param {string} plotId plot id to set the scrolling on
 * @param {Array} plotViewAry an array of plotView
 * @param {Array} plotGroupAry the plotGroup array
 * @param {ScreenPt} newScrollPt a screen point in the plot to scroll to
 * @return {Array.<PlotView>}
 */
export function updatePlotGroupScrollXY(visRoot, plotId,plotViewAry, plotGroupAry, newScrollPt) {
    const plotView= updatePlotViewScrollXY(getPlotViewById(plotViewAry, plotId), newScrollPt);
    plotViewAry= replacePlotView(plotViewAry, plotView);
    if (!visRoot?.positionLock) return plotViewAry;
    return matchPlotViewByPositionGroup(visRoot, plotView,plotViewAry,false, makeScrollPosMatcher(plotView, visRoot));
}

/**
 * Create a new plotView that will wcs match the scroll position of the master plotView.
 * This function all all the safety checks for undefined plotview or plots. It is
 * always safe to call.
 * @param {WcsMatchType} wcsMatchType
 * @param {PlotView} masterPv - master PlotView
 * @param {PlotView} matchToPv - match to PlotView
 * @return {PlotView} a new version of matchToPv with the scroll position matching
 */
export function updateScrollToWcsMatch(wcsMatchType, masterPv, matchToPv) {
    if (!masterPv || !matchToPv || masterPv===matchToPv) return matchToPv;
    if (masterPv.plotId===matchToPv.plotId || !primePlot(masterPv)|| !primePlot(matchToPv)) return matchToPv;

    const newScrollPoint= findWCSMatchScrollPosition(wcsMatchType, masterPv, matchToPv);
    return updatePlotViewScrollXY(matchToPv, newScrollPoint);
}

/**
 * Find a scroll point that the point puts the plot be scroll the to same wcs or target as the master plot
 * To use this function the plot view objects and the primary plot objects must all be defined.
 * @param {WcsMatchType} wcsMatchType
 * @param {PlotView} masterPv - master PlotView
 * @param {PlotView} matchToPv - match to PlotView
 * @return {ScreenPt} the screen point offset
 */
function findWCSMatchScrollPosition(wcsMatchType, masterPv, matchToPv) {

    const masterP= primePlot(masterPv);
    const matchToP= primePlot(matchToPv);
    const ccMaster= CysConverter.make(masterP);
    const ccMatch= CysConverter.make(matchToP);

    if (wcsMatchType===WcsMatchType.Standard) {
        const centerMasterWorldPt=  ccMaster.getWorldCoords(findCurrentCenterPoint(masterPv));
        return findScrollPtToCenterImagePt( matchToPv,  ccMatch.getImageCoords(centerMasterWorldPt));
    }
    else if (wcsMatchType===WcsMatchType.Target) {
        if (!matchToP.attributes[PlotAttribute.FIXED_TARGET] || !masterP.attributes[PlotAttribute.FIXED_TARGET] ) {
            return makeScreenPt(masterPv.scrollX, masterPv.scrollY);
        }
        const mastDevPt= ccMaster.getDeviceCoords(masterP.attributes[PlotAttribute.FIXED_TARGET]);
        const matchPoint= ccMatch.getImageCoords(matchToP.attributes[PlotAttribute.FIXED_TARGET]);
        return findScrollPtToPlaceOnDevPt( matchToPv, matchPoint, mastDevPt);
    }
    else if (wcsMatchType===WcsMatchType.PixelCenter) {
        const centerMasterImagePt=  findCurrentCenterPoint(masterPv);
        const wDelta= (masterP.dataWidth - matchToP.dataWidth)/2;
        const hDelta= (masterP.dataHeight - matchToP.dataHeight)/2;
        return findScrollPtToCenterImagePt( matchToPv,
            makeImagePt(centerMasterImagePt.x-wDelta, centerMasterImagePt.y-hDelta) );
    }
    else if (wcsMatchType===WcsMatchType.Pixel) {
        const centerMasterImagePt=  findCurrentCenterPoint(masterPv);
        return findScrollPtToCenterImagePt( matchToPv,  centerMasterImagePt);

    }
    else {
        return makeScreenPt(masterPv.scrollX, masterPv.scrollY);
    }

}

/**
 * make a function that will match the scroll position of a plotview to the source plotview
 * @param {PlotView} sourcePV the plotview that others will match to
 * @param {VisRoot} visRoot
 * @return {function} a function the takes the plotview to match scrolling as a parameter and
 *                      returns the scrolled matched version
 */
function makeScrollPosMatcher(sourcePV, visRoot) {
    const {scrollX:srcSx,scrollY:srcSy}= sourcePV;
    const {wcsMatchType}= visRoot;
    const sourcePlot= primePlot(sourcePV);
    const {screenSize:{width:srcScreenWidth,height:srcScreenHeight}}= sourcePlot;
    const {scrollWidth:srcSW,scrollHeight:srcSH}= getScrollSize(sourcePV);
    const percentX= (srcSx+srcSW/2) / srcScreenWidth;
    const percentY= (srcSy+srcSH/2) / srcScreenHeight;

    return (pv) => {
        const plot= primePlot(pv);
        if (!plot) return pv;
        if (wcsMatchType) {
            return updateScrollToWcsMatch(visRoot.wcsMatchType, sourcePV, pv);
        }
        else {
            const {screenSize:{width,height}}= plot;
            const {scrollWidth:sw,scrollHeight:sh}= getScrollSize(pv);
            const newSx= width*percentX - sw/2;
            const newSy= height*percentY - sh/2;
            return updatePlotViewScrollXY(pv,makeScreenPt(newSx,newSy));
        }
    };
}



/**
 *
 * @param {WebPlot} plot
 * @return {{}}
 */
function getNewAttributes(plot) {

    //todo: figure out active target and how to set it
    const attributes= {};
    const req= plot.plotState.getWebPlotRequest();
    if (!req) return attributes;

    let worldPt;
    const circle = req.getRequestArea();

    if (req.containsParam(WPConst.OVERLAY_POSITION)) {
        worldPt= req.getOverlayPosition();
    }
    else if (circle && circle.center) {
        worldPt= circle.center;
    }
    else if (getActiveTarget()) {
        worldPt= getActiveTarget().worldPt;
    }
    else {
        worldPt= getCenterPtOfPlot(plot);
    }

    if (worldPt) {
        const cc= CysConverter.make(plot);
        if (isHiPS(plot) || cc.pointInPlot(worldPt) || req.getOverlayPosition()) {
            attributes[PlotAttribute.FIXED_TARGET]= worldPt;
            if (circle) attributes[PlotAttribute.REQUESTED_SIZE]= circle.radius;  // says radius but really size
        }
    }


    return attributes;
}


/**
 *
 * @param {WebPlot} plot
 * @param {{width: number, height: number}} viewDim
 * @return {{scrollWidth: number, scrollHeight: number}}
 */
function computeScrollSizes(plot,viewDim) {
    const {screenSize}= plot;
    let scrollWidth= Math.min(screenSize.width,viewDim.width);
    let scrollHeight= Math.min(screenSize.height,viewDim.height);

    if (isNaN(scrollWidth)) scrollWidth= 0;
    if (isNaN(scrollHeight)) scrollHeight= 0;

    return {scrollWidth,scrollHeight};
}

/**
 * @param {object} plotView
 * @return {{scrollWidth: number, scrollHeight: number}}
 */
export const getScrollSize = (plotView) => computeScrollSizes(primePlot(plotView),plotView.viewDim);


/**
 *
 * @param {PlotView} plotView
 * @return {ScreenPt}
 */
function findScrollPtForCenter(plotView) {
    const {width,height}= plotView.viewDim;
    const {width:scrW,height:scrH}= primePlot(plotView).screenSize;
    const x= scrW/2- width/2;
    const y= scrH/2- height/2;
    return makeScreenPt(x,y);
}

/**
 * find the scroll screen pt to put the image centered on the passed ImagePt
 * @param {PlotView} plotView
 * @param {ImagePt} ipt - if this is not an image point it will be converted to one
 * @return {ScreenPt} the screen point to use as the scroll position
 */
export function findScrollPtToCenterImagePt(plotView, ipt) {
    const {width,height}= plotView.viewDim;
    return findScrollPtToPlaceOnDevPt(plotView,ipt, makeDevicePt(width/2,height/2));
}


/**
 * Return the scroll point for a PlotView that will place the given image point on the given device point.
 * or another way to say it:
 * Given a device point and an image point, return the scroll point the would make the two line up.
 * @param {PlotView} pv
 * @param {ImagePt} ipt - if this is not an image point it will be converted to one
 * @param {DevicePt} targetDevPtPos - the point on the device that the image
 * @return {ScreenPt} the scroll position the places the image point on to the device point
 */
export function findScrollPtToPlaceOnDevPt(pv, ipt, targetDevPtPos) {
    const plot= primePlot(pv);

                            // make a CsysConverter for a image that has a scroll  position of 0,0
    const altAffTrans= makeTransform(0,0, 0, 0, pv.rotation, pv.flipX, pv.flipY, pv.viewDim);
    const cc= CysConverter.make(plot,altAffTrans);

    const point= cc.getScreenCoords(ipt);
    if (!point) return undefined;

    const target= cc.getScreenCoords(targetDevPtPos);
    if (!target) return undefined;

    const x= point.x - target.x;
    const y= point.y - target.y;

    return makeScreenPt(pv.flipY ? -x : x,pv.flipX ? -y : y);
}
