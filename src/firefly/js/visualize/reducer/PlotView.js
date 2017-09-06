/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'immutability-helper';
import {PlotAttribute} from './../WebPlot.js';
import {get, isUndefined} from 'lodash';
import Enum from 'enum';
import {clone} from '../../util/WebUtil.js';
import {WPConst} from './../WebPlotRequest.js';
import {makeScreenPt, makeDevicePt} from './../Point.js';
import {getActiveTarget} from '../../core/AppDataCntlr.js';
import VisUtil from './../VisUtil.js';
import {getPlotViewById, matchPlotView, primePlot, findPlotGroup} from './../PlotViewUtil.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import {ZoomType} from '../ZoomType.js';
import {PlotPref} from './../PlotPref.js';
import {DEFAULT_THUMBNAIL_SIZE} from '../WebPlotRequest.js';
import {CCUtil, CysConverter} from './../CsysConverter.js';
import {getDefMenuItemKeys} from '../MenuItemKeys.js';
import {ExpandType, WcsMatchType} from '../ImagePlotCntlr.js';
import {updateTransform, makeTransform} from '../PlotTransformUtils.js';

const DEF_WORKING_MSG= 'Plotting ';

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
 * @prop {WebPlot[]} plots all the plots that this plotView can show, usually the image in the fits file
 * @prop {String} plottingStatus, end user description of the what is doing on
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
 * @prop {boolean} containsMultiImageFits is a multi image fits file
 * @prop {boolean} containsMultipleCubes  is a multi cube
 */


/**
 * @param {string} plotId
 * @param {WebPlotRequest} req
 * @param {object} pvOptions options for this plot view todo- define what pvOptions is, somewhere
 * @return  {PlotView}
 */
export function makePlotView(plotId, req, pvOptions= {}) {
    const pv= {
        plotId, // should never change
        plotGroupId: req.getPlotGroupId(), //should never change
        drawingSubGroupId: req.getDrawingSubGroupId(), //todo, string, this is an id, should never change
        plots:[],
        request: req && req.makeCopy(),
        plottingStatus:'Plotting...',
        serverCall:'success', // one of 'success', 'working', 'fail'
        primeIdx:-1,
        scrollX : -1,   // in ScreenCoords
        scrollY : -1,   // in ScreenCoords
        affTrans: null,
        viewDim : {width:0, height:0}, // size of viewable area  (div size: offsetWidth & offsetHeight)
        overlayPlotViews: [],
        menuItemKeys: makeMenuItemKeys(req,pvOptions,getDefMenuItemKeys()), // normally will not change
        plotViewCtx: createPlotViewContextData(req, pvOptions),
        rotation: 0,
        flipY: false,
        flipX: false,
        options : {
            acceptAutoLayers : true,
            workingMsg      : DEF_WORKING_MSG, // the working message that the users sees
            saveCorners     : req.getSaveCorners(),
            expandedTitleOptions: req.getExpandedTitleOptions(),
            annotationOps : req.getAnnotationOps(), // how titles are drawn
            allowImageLock  : false, // show the image lock button in the toolbar, todo, this may go away
        }
    };

    return pv;
}



/**
 *
 * @param {WebPlotRequest} req
 * @param {Object} pvOptions
 * @return {PlotViewContextData}
 */
function createPlotViewContextData(req, pvOptions) {
    return {
        userCanDeletePlots: get(pvOptions, 'userCanDeletePlots', true),
        rotateNorthLock : false,
        zoomLockingEnabled : false,
        zoomLockingType: UserZoomTypes.FIT, // can be FIT or FILL
        lastCollapsedZoomLevel: 0,
        preferenceColorKey: req.getPreferenceColorKey(),
        preferenceZoomKey:  req.getPreferenceZoomKey(),
        defThumbnailSize: DEFAULT_THUMBNAIL_SIZE,
        containsMultiImageFits : false,
        containsMultipleCubes : false,
        lockPlotHint: false, //todo - i may remove this option
        plotCounter:0 // index of how many plots, used for making next ID
    };
}



//todo - this function should determine which menuItem are visible and which are hidden
// for now just return the default
function makeMenuItemKeys(req,pvOptions,defMenuItemKeys) {
    return Object.assign({},defMenuItemKeys, pvOptions.menuItemKeys);
}

export const initScrollCenterPoint= (pv) =>
                       updatePlotViewScrollXY(pv,findScrollPtForCenter(pv));


export function changePrimePlot(pv, nextIdx) {
    const {plots}= pv;
    if (!plots[nextIdx]) return pv;
    const currentScrollImPt= CCUtil.getImageCoords(primePlot(pv),makeScreenPt(pv.scrollX,pv.scrollY));
    //=================

    pv= Object.assign({},pv,{primeIdx:nextIdx});

    const cc= CysConverter.make(plots[nextIdx]);
    if (cc.pointInData(currentScrollImPt)) {
        pv= updatePlotViewScrollXY(pv,cc.getScreenCoords(currentScrollImPt));
    }
    else {
        pv= initScrollCenterPoint(pv);
    }
    pv= updateTransform(pv);
    return pv;
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

    pv= clone(pv);
    pv.plotViewCtx= clone(pv.plotViewCtx);


    if (overlayPlotViews) {
        const oPlotAry= overlayPlotViews.map( (opv) => opv.plot);
        pv.overlayPlotViews= pv.overlayPlotViews.map( (opv) => {
            const plot= oPlotAry.find( (p) => p.plotId===opv.imageOverlayId);
            return plot ? clone(opv, {plot}) : opv;
        });
    }

    if (newPlot || get(pv, 'plots.length') !== plotAry.length) {
        pv.plots= plotAry;
    }
    else {
        const oldPlots= pv.plots;
        pv.plots= plotAry.map( (p,idx) => clone(p, {relatedData:oldPlots[idx].relatedData}) );
    }


    pv.plots.forEach( (plot) => {
        plot.attributes= Object.assign({},plot.attributes, getNewAttributes(plot));
        plot.plotImageId= `${pv.plotId}--${pv.plotViewCtx.plotCounter}`;
        pv.plotViewCtx.plotCounter++;
    });


    if (pv.primeIdx<0 || pv.primeIdx>=pv.plots.length) pv.primeIdx=0;
    pv.plottingStatus='';
    pv.serverCall='success';

    PlotPref.putCacheColorPref(pv.plotViewCtx.preferenceColorKey, pv.plots[pv.primeIdx].plotState);
    PlotPref.putCacheZoomPref(pv.plotViewCtx.preferenceZoomKey, pv.plots[pv.primeIdx].plotState);

    if (pv.plots.length>1) {
        pv.plotViewCtx.containsMultiImageFits= pv.plots.every( (p) => p.plotState.isMultiImageFile());
    }
    pv.plotViewCtx.containsMultipleCubes= pv.plots.every( (p) => p.plotState.getCubeCnt()>1);
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
    if (!newScrollPt) return Object.assign({},plotView, {scrollX:undefined, scrollY:undefined});

    const plot= primePlot(plotView);
    if (!plot) return plotView;
    const {scrollWidth,scrollHeight}= getScrollSize(plotView);
    if (!scrollWidth || !scrollHeight) return plotView;

    const cc= CysConverter.make(plot);
    newScrollPt= cc.getScreenCoords(newScrollPt);
    const {x:newSx,y:newSy}= newScrollPt;

    const newPlotView= Object.assign({},plotView, {scrollX:newSx, scrollY:newSy});
    return updateTransform(newPlotView);
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
 * scroll a plot view to a new screen pt, if plotGroup.lockRelated is true then all the plot views in the group
 * will be scrolled to match
 * @param {VisRoot} visRoot
 * @param {string} plotId plot id to set the scrolling on
 * @param {Array} plotViewAry an array of plotView
 * @param {Array} plotGroupAry the plotGroup array
 * @param {ScreenPt} newScrollPt a screen point in the plot to scroll to
 * @return {Array}
 */
export function updatePlotGroupScrollXY(visRoot, plotId,plotViewAry, plotGroupAry, newScrollPt) {
    const plotView= updatePlotViewScrollXY(getPlotViewById(plotViewAry, plotId), newScrollPt);
    plotViewAry= replacePlotView(plotViewAry, plotView);
    const plotGroup= findPlotGroup(plotView.plotGroupId,plotGroupAry);
    if (get(plotGroup,'lockRelated')) {
        plotViewAry= matchPlotView(plotView,plotViewAry,plotGroup,makeScrollPosMatcher(plotView, visRoot));
    }
    return plotViewAry;
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
    const sourcePlot= primePlot(sourcePV);
    const {screenSize:{width:srcScreenWidth,height:srcScreenHeight}}= sourcePlot;
    const {scrollWidth:srcSW,scrollHeight:srcSH}= getScrollSize(sourcePV);
    const percentX= (srcSx+srcSW/2) / srcScreenWidth;
    const percentY= (srcSy+srcSH/2) / srcScreenHeight;

    return (pv) => {
        let retPV= pv;
        const plot= primePlot(pv);
        if (plot) {
            if (visRoot.wcsMatchType) {
                retPV= updateScrollToWcsMatch(visRoot.wcsMatchType, sourcePV, pv);
            }
            else {
                const {screenSize:{width,height}}= plot;
                const {scrollWidth:sw,scrollHeight:sh}= getScrollSize(pv);
                const newSx= width*percentX - sw/2;
                const newSy= height*percentY - sh/2;
                retPV= updatePlotViewScrollXY(pv,makeScreenPt(newSx,newSy));
            }
        }
        return retPV;
    };
}



/**
 *
 * @param {object} plot
 * @return {{}}
 */
function getNewAttributes(plot) {

    //todo: figure out active target and how to set it
    const attributes= {};
    const req= plot.plotState.getWebPlotRequest();

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
        worldPt= VisUtil.getCenterPtOfPlot(plot);
    }

    if (worldPt) {
        const cc= CysConverter.make(plot);
        if (cc.pointInPlot(worldPt) || req.getOverlayPosition()) {
            attributes[PlotAttribute.FIXED_TARGET]= worldPt;
            if (circle) attributes[PlotAttribute.REQUESTED_SIZE]= circle.radius;  // says radius but really size
        }
    }
    if (req.getUniqueKey())     attributes[PlotAttribute.UNIQUE_KEY]= req.getUniqueKey();
    if (req.isMinimalReadout()) attributes[PlotAttribute.MINIMAL_READOUT]=true;

    return attributes;
}





/**
 * Given the scrollX and scrollY then find the point in the plot that is at the center of
 * the display.  The point returned is in ImagePt coordinates.
 * We return it in and ImagePt not screen because if the plot
 * is zoomed the image point will be what we want in the center.
 * The screen coordinates will be completely different.
 * @param {PlotView} plotView
 * @param {number} [scrollX] optional scrollX if not defined us plotView.scrollX
 * @param {number} [scrollY] optional scrollY if not defined us plotView.scrollY
 * @return {ImagePt} the center point
 */
export function findCurrentCenterPoint(plotView,scrollX,scrollY) {
    const plot= primePlot(plotView);
    if (!plot) return null;
    const {viewDim}= plotView;

    let cc;
    if (!isUndefined(scrollX) && isUndefined(!scrollY)) {
        const trans= makeTransform(0,0, scrollX, scrollY,  plotView.rotation, plotView.flipX, plotView.flipY, viewDim);
        cc= CysConverter.make(plot,trans);
    }
    else if (isUndefined(plotView.scrollX) || isUndefined(plotView.scrollY)) {
        const trans= makeTransform(0,0, 0, 0,  plotView.rotation, plotView.flipX, plotView.flipY, viewDim);
        cc= CysConverter.make(plot,trans);
    }
    else {
        cc= CysConverter.make(plot);
    }
    const pt= makeDevicePt(viewDim.width/2, viewDim.height/2);
    return cc.getImageCoords(pt);
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
 * return the scroll point for a PlotView that will place the passed image point on the passed device point
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
    if (!point) return null;

    const target= cc.getScreenCoords(targetDevPtPos);
    if (!target) return null;

    const x= point.x - target.x;
    const y= point.y - target.y;

    return makeScreenPt(pv.flipY ? -x : x,pv.flipX ? -y : y);
}
