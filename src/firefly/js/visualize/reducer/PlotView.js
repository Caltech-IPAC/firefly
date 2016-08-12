/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * REDUCER USE ONLY
 * REDUCER USE ONLY
 * REDUCER USE ONLY
 */

import update from 'react-addons-update';
import {isEqual, get} from 'lodash';
import {WebPlot, PlotAttribute} from './../WebPlot.js';
import {clone} from '../../util/WebUtil.js';
import {WPConst} from './../WebPlotRequest.js';
import {RotateType} from './../PlotState.js';
import {makeScreenPt} from './../Point.js';
import {getActiveTarget} from '../../core/AppDataCntlr.js';
import VisUtil from './../VisUtil.js';
import {getPlotViewById, matchPlotView, primePlot, findPlotGroup} from './../PlotViewUtil.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import {PlotPref} from './../PlotPref.js';
import {DEFAULT_THUMBNAIL_SIZE} from '../WebPlotRequest.js';
import SimpleMemCache from '../../util/SimpleMemCache.js';
import {CCUtil, CysConverter} from './../CsysConverter.js';
import {defMenuItemKeys} from '../MenuItemKeys.js';
import {ExpandType} from '../ImagePlotCntlr.js';

const DEF_WORKING_MSG= 'Plotting ';



//============ EXPORTS ===========
//============ EXPORTS ===========

export default {replacePlots,
                updatePlotViewScrollXY,
                findCurrentCenterPoint, findScrollPtForImagePt,
                updatePlotGroupScrollXY};

//============ EXPORTS ===========
//============ EXPORTS ===========



//======================================== Exported Functions =============================
//======================================== Exported Functions =============================
//======================================== Exported Functions =============================


/**
 * @typedef {Object} PlotView
 * There is one PlotView object for each react ImageViewer.  A PlotView is uniquely identified by the plotId. The
 * plot id will not change for the life time of the plotView. A plot view can be connected to a plot group.  That is done
 * by the plotGroupId. The will be several plotViews in a plot group.
 *
 * PlotView is mostly about the viewing of the plot.  The plot data is contained in a WebPlot. A plotView can have an
 * array of WebPlots. The array length will only be one for normals fits files and n for multi image fits and cube fits
 * files. plots[primeIdx] refers to the plot currently showing in the plot view.
 * 
 * @prop {String} plotId, immutable
 * @prop {String} plotGroupId, immutable
 * @prop {String} drawingSubGroupId, immutable
 * @prop {Array} plots all the plots that this plotView can show, usually the image in the fits file
 * @prop {String} plottingStatus, end user description of the what is doing on
 * @prop {String} serverCall, one of 'success', 'working', 'fail'
 * @prop {number} primeIdx, which of the plots array is active
 * @prop {number} wcsMarginX X offset for wcs align
 * @prop {number} wcsMarginY X offset for wcs align
 * @prop {number} scrollX scroll position X
 * @prop {number} scrollY scroll position Y
 * @prop {{x:number, y:number}} viewDim  size of viewable area  (div size: offsetWidth & offsetHeight)
 * @prop {Object} menuItemKeys - which toolbar button are enables for this plotView
 * @prop {Object} overlayPlotViews
 * @prop {Object} options
 * @prop {Object} plotViewCtx
 */


/**
 * @param {string} plotId
 * @param {WebPlotRequest} req
 * @param {object} pvOptions options for this plot view todo- define what pvOptions is, somewhere
 * @return  {PlotView} 
 */
export function makePlotView(plotId, req, pvOptions= {}) {
    var pv= {
        plotId, // should never change
        plotGroupId: req.getPlotGroupId(), //should never change
        drawingSubGroupId: req.getDrawingSubGroupId(), //todo, string, this is an id, should never change
        plots:[],
        request: req,
        plottingStatus:'Plotting...',
        serverCall:'success', // one of 'success', 'working', 'fail'
        primeIdx:-1,
        wcsMarginX: 0, // todo
        wcsMarginY: 0, // todo
        scrollX : -1,
        scrollY : -1,
        viewDim : {width:0, height:0}, // size of viewable area  (div size: offsetWidth & offsetHeight)
        overlayPlotViews: [],
        menuItemKeys: makeMenuItemKeys(req,pvOptions,defMenuItemKeys), // normally wil not change
        plotViewCtx: createPlotViewContextData(req, pvOptions),


        options : {

            acceptAutoLayers : true,
            // many options -- todo figure out how to set and change, some are set by request, how about the others?
            workingMsg      : DEF_WORKING_MSG,
            hasNewPlotContainer: req.getHasNewPlotContainer(), // if image selection dialog come up, allow to create a new MiniPlotWidth, todo control with MenuItemKeys
            saveCorners     : req.getSaveCorners(), // save the four corners of the plot to the ActiveTarget singleton, todo
            expandedTitleOptions: req.getExpandedTitleOptions(),
            annotationOps : req.getAnnotationOps(), // how titles are drawn


            //useLayerOnPlotToolbar: true, // show the Layer control button on the plot toolbar, todo - i now think I can remove this



              // todo- the follow should be removed when implemented, menuItemKeys will now control option visibility
            allowImageLock  : false, // show the image lock button in the toolbar, todo
            allowImageSelect: req.isAllowImageSelection(), // i think i can remove, show the image selection button in the toolbar, user can change image, todo
            catalogButton   : false // show the catalog select button, todo

        }
    };

    return pv;
}



function createPlotViewContextData(req, pvOptions) {
    return {
        userCanDeletePlots: get(pvOptions, 'userCanDeletePlots', true),
        rotateNorthLock : false,
        userModifiedRotate: false, // the user modified the rotate status, todo
        zoomLockingEnabled : false,
        zoomLockingType: UserZoomTypes.FIT, // can be FIT or FILL
        lastCollapsedZoomLevel: 0,
        preferenceColorKey: req.getPreferenceColorKey(),
        preferenceZoomKey:  req.getPreferenceZoomKey(),
        defThumbnailSize: DEFAULT_THUMBNAIL_SIZE,
        containsMultiImageFits : false,
        containsMultipleCubes : false,
        lockPlotHint: false, //todo
        plotCounter:0 // index of how many plots, used for making next ID
    };
}



//todo - this function should determine which menuItem are visible and which are hidden
// for now just return the default
function makeMenuItemKeys(req,pvOptions,defMenuItemKeys) {
    return Object.assign({},defMenuItemKeys, pvOptions.menuItemKeys);
}

export const initScrollCenterPoint= (pv) => updatePlotViewScrollXY(pv,findScrollPtForCenter(pv));


export function changePrimePlot(pv, nextIdx) {
    const {plots}= pv;
    if (!plots[nextIdx]) return pv;
    // var currentCenterImPt= findCurrentCenterPoint(pv);
    var currentScrollImPt= CCUtil.getImageCoords(primePlot(pv),makeScreenPt(pv.scrollX,pv.scrollY));
    //=================
    
    pv= Object.assign({},pv,{primeIdx:nextIdx});
    
    const cc= CysConverter.make(plots[nextIdx]);
    if (cc.pointInData(currentScrollImPt)) {
        pv= updatePlotViewScrollXY(pv,cc.getScreenCoords(currentScrollImPt));
    }
    else {
        pv= initScrollCenterPoint(pv);
    }
    return pv;
}



/**
 *
 * @param pv
 * @param plotAry
 * @param overlayPlotViews
 * @param expandedMode
 * @param keepPrimeIdx
 * @return {Object|*}
 */
function replacePlots(pv, plotAry, overlayPlotViews, expandedMode, keepPrimeIdx=false) {

    pv= clone(pv);
    pv.plotViewCtx= clone(pv.plotViewCtx);

    if (pv.plots && pv.plots.length) {
        pv.plots.forEach( (plot) => {
            SimpleMemCache.clearCache(plot.plotImageId);
            // todo- clean up before resetting the array - go through old array call server to delete plot
            //todo -- somewhere need to call the server with a delete plot - probably needs to be an action or side effect
        });
    }


    if (overlayPlotViews) {
        const oPlotAry= overlayPlotViews.map( (opv) => opv.plot);
        pv.overlayPlotViews= pv.overlayPlotViews.map( (opv) => {
            const plot= oPlotAry.find( (p) => p.plotId===opv.imageOverlayId);
            return plot ? clone(opv, {plot}) : opv;
        });
    }


    pv.plots= plotAry;

    pv.plots.forEach( (plot) => {
        plot.attributes= Object.assign({},plot.attributes, getNewAttributes(plot));
        plot.plotImageId= `${pv.plotId}--${pv.plotViewCtx.plotCounter}`;
        pv.plotViewCtx.plotCounter++;
    });


    if (!keepPrimeIdx || pv.primeIdx<0 || pv.primeIdx>=pv.plots.length) pv.primeIdx=0;
    pv.plottingStatus='';
    pv.serverCall='success';

    PlotPref.putCacheColorPref(pv.plotViewCtx.preferenceColorKey, pv.plots[pv.primeIdx].plotState);
    PlotPref.putCacheZoomPref(pv.plotViewCtx.preferenceZoomKey, pv.plots[pv.primeIdx].plotState);


    setClientSideRequestOptions(pv,pv.plots[pv.primeIdx].plotState.getWebPlotRequest());

    if (pv.plots.length>1) {
        pv.plotViewCtx.containsMultiImageFits= pv.plots.every( (p) => p.plotState.isMultiImageFile());
    }
    pv.plotViewCtx.containsMultipleCubes= pv.plots.every( (p) => p.plotState.getCubeCnt()>1);
    pv.plotViewCtx.rotateNorthLock= pv.plots[pv.primeIdx].plotState.getRotateType()===RotateType.NORTH;  // todo, study this more, understand why
    if (expandedMode===ExpandType.COLLAPSE) pv.plotViewCtx.lastCollapsedZoomLevel= pv.plots[pv.primeIdx].zoomFactor;

    pv= initScrollCenterPoint(pv);

    return pv;
}

/**
 * create a copy of the PlotView with a new scroll position and a new view port if necessary
 * The scroll position is the top left visible point.
 * @param {object} plotView the current plotView
 * @param {object} newScrollPt  the screen point of the scroll position
 * @return {object} new copy of plotView
 */
function updatePlotViewScrollXY(plotView,newScrollPt) {
    if (!plotView || !newScrollPt) return plotView;

    var {scrollX:oldSx,scrollY:oldSy}= plotView;
    var plot= primePlot(plotView);
    var {scrollWidth,scrollHeight}= getScrollSize(plotView);
    if (!plot || !scrollWidth || !scrollHeight) return plotView;

    const cc= CysConverter.make(plot);
    newScrollPt= cc.getScreenCoords(newScrollPt);
    var {x:newSx,y:newSy}= newScrollPt;
    // var {width:oldVPW, height:oldVPH} = plot.viewPort.dim;
    //if (newSx===oldSx && newSy===oldSy && oldVPW && oldVPH) return plotView;

    newSx= checkBounds(newSx,plot.screenSize.width,scrollWidth);
    newSy= checkBounds(newSy,plot.screenSize.height,scrollHeight);

    var newPlotView;
    if (newSx!==oldSx || newSy!==oldSy)  {
        newPlotView= Object.assign({},plotView, {scrollX:newSx, scrollY:newSy});
    }

    if (isRecomputeViewPortNecessary(newSx,newSy,scrollWidth,scrollHeight,plot.viewPort) ) {
        var cp= cc.getScreenCoords(findCurrentCenterPoint(plotView,newSx,newSy));
        var viewPort= computeViewPort(plot,scrollWidth,scrollHeight,cp);
        if (isEqual(viewPort,plot.viewPort) && !newPlotView) return plotView;

        if (!newPlotView) newPlotView= Object.assign({},plotView);
        var newPrimary= WebPlot.setWPViewPort(plot,viewPort);
        newPlotView.plots= plotView.plots.map( (p) => p===plot ? newPrimary : p);
    }

    return newPlotView || plotView;
}

/**
 * replace a plotview in the plotViewAry with the passed plotview whose plotId's match
 * @param {Array} plotViewAry
 * @param {object} newPlotView
 * @return {Array} new plotView array after return a plotview
 */
export function replacePlotView(plotViewAry,newPlotView) {
    return plotViewAry.map( (pv) => pv.plotId===newPlotView.plotId ? newPlotView : pv);
}

/**
 *
 * @param plotView
 * @param primePlot
 * @return {*} return the new PlotView object
 */
export function replacePrimaryPlot(plotView,primePlot) {
    return update(plotView, { plots : {[plotView.primeIdx] : { $set : primePlot } }} );
}

/**
 * scroll a plot view to a new screen pt, if plotGroup.lockRelated is true then all the plot views in the group
 * will be scrolled to match
 * @param plotId plot id to set the scrolling on
 * @param {Array} plotViewAry an array of plotView
 * @param {Array} plotGroupAry the plotGroup array
 * @param newScrollPt a screen point in the plot to scroll to
 * @return {Array}
 */
function updatePlotGroupScrollXY(plotId,plotViewAry, plotGroupAry, newScrollPt) {
    var plotView= updatePlotViewScrollXY(getPlotViewById(plotViewAry, plotId),newScrollPt);
    plotViewAry= replacePlotView(plotViewAry, plotView);
    var plotGroup= findPlotGroup(plotView.plotGroupId,plotGroupAry);
    if (plotGroup && plotGroup.lockRelated) {
        plotViewAry= matchPlotView(plotView,plotViewAry,plotGroup,makeScrollPosMatcher(plotView));
    }
    return plotViewAry;
}


//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================

/**
 * make a function that will match the scroll position of a plotview to the source plotview
 * @param sourcePV the plotview that others will match to
 * @return {Function} a function the takes the plotview to match scrolling as a parameter and
 *                      returns the scrolled matched version
 */
function makeScrollPosMatcher(sourcePV) {
    var {scrollX:srcSx,scrollY:srcSy}= sourcePV;
    var sourcePlot= primePlot(sourcePV);
    var {screenSize:{width:srcScreenWidth,height:srcScreenHeight}}= sourcePlot;
    var {scrollWidth:srcSW,scrollHeight:srcSH}= getScrollSize(sourcePV);
    var percentX= (srcSx+srcSW/2) / srcScreenWidth;
    var percentY= (srcSy+srcSH/2) / srcScreenHeight;

    return (pv) => {
        var retPV= pv;
        var plot= primePlot(pv);
        if (plot) {
            var {screenSize:{width,height}}= plot;
            var {scrollWidth:sw,scrollHeight:sh}= getScrollSize(pv);
            var newSx= width*percentX - sw/2;
            var newSy= height*percentY - sh/2;
            retPV= updatePlotViewScrollXY(pv,makeScreenPt(newSx,newSy));
        }
        return retPV;
    };
}



/**
 *
 * @param pv
 * @param {WebPlotRequest} r
 */
function setClientSideRequestOptions(pv,r) {

        if (!r) return;
        if (r.containsParam(WPConst.SHOW_TITLE_AREA)) {
    //        setTitleAreaAlwaysHidden(!r.getShowTitleArea()); //todo, where to handle?
        }
}






/**
 *
 * @param {object} plot
 * @return {{}}
 */
function getNewAttributes(plot) {

    //todo: figure out active target and how to set it
    var attributes= {};
    var req= plot.plotState.getPrimaryWebPlotRequest();

    var worldPt;
    var circle = req.getRequestArea();

    if (req.getOverlayPosition()) {
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
        if (cc.pointInPlot(worldPt)) {
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
 * @return ImagePt the center point
 * @param {{}} plotView
 * @param [scrollX] optional scrollX if not defined us plotView.scrollX
 * @param [scrollY] optional scrollY if not defined us plotView.scrollY
 */
function findCurrentCenterPoint(plotView,scrollX,scrollY) {
    if (!plotView) return null;
    var {wcsMarginX,wcsMarginY}= plotView;
    var plot= primePlot(plotView);
    if (!plot) return null;
    var {scrollWidth,scrollHeight}= getScrollSize(plotView);
    var sx= (typeof scrollX !== 'undefined') ? scrollX : plotView.scrollX;
    var sy= (typeof scrollY !== 'undefined') ? scrollY : plotView.scrollY;

    var {width:screenW, height:screenH}= plot.screenSize;
    var cX=  (screenW<scrollWidth) ? screenW/2 : sx+scrollWidth/2- wcsMarginX;
    var cY= (screenH<scrollHeight) ? screenH/2 : sy+scrollHeight/2- wcsMarginY;
    var pt= makeScreenPt(cX,cY);
    return CCUtil.getImageCoords(plot,pt);
}


/**
 *
 * @param scrollX
 * @param scrollY
 * @param scrollWidth
 * @param scrollHeight
 * @param viewPort
 * @return {boolean}
 */
function isRecomputeViewPortNecessary(scrollX,scrollY,scrollWidth,scrollHeight, viewPort) {
    var {x,y,dim} = viewPort;
    var needsRecompute= !VisUtil.containsRec(x,y, dim.width,dim.height,
                                      scrollX,scrollY,scrollWidth-1,scrollHeight-1);
    if (!needsRecompute) {
        needsRecompute= (!x && !scrollX && dim.width>scrollWidth) || (!y && !scrollY && dim.width>scrollHeight);
    }
    return needsRecompute;
}


/**
 *
 * @param plot
 * @param {{width: number, height: number}} viewDim
 * @return {{scrollWidth: number, scrollHeight: number}}
 */
function computeScrollSizes(plot,viewDim) {
    var {screenSize}= plot;
    var scrollWidth= Math.min(screenSize.width,viewDim.width);
    var scrollHeight= Math.min(screenSize.height,viewDim.height);

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
 * Compute a view port based on the visibleCenterPt
 * @param plot
 * @param scrollWidth
 * @param scrollHeight
 * @param {object} visibleCenterPt, screen point
 * @return {{dim: {width : number, height : number}, x: number, y: number}}
 */
export function computeViewPort(plot, scrollWidth, scrollHeight, visibleCenterPt) {
    if (!plot) return null;

    var {viewPort}= plot;
    var {width:screenW, height:screenH} = plot.screenSize;

    var vpw = Math.max(Math.min((scrollWidth * 1.5), 2000),screenW);
    var vph = Math.max(Math.min((scrollHeight * 1.5), 1500),screenH);

    var newVpX;
    var newVpY;

    if (vpw > screenW) {
        vpw = screenW;
        newVpX = 0;
    }
    else {
        newVpX = visibleCenterPt.x - vpw / 2;
        if (newVpX < 0)                  newVpX = 0;
        else if (newVpX + vpw > screenW) newVpX = screenW - vpw;
    }

    if (vph > screenH) {
        vph = screenH;
        newVpY = 0;
    }
    else {
        newVpY = visibleCenterPt.y - vph / 2;
        if (newVpY < 0)                  newVpY = 0;
        else if (newVpY + vph > screenH) newVpY = screenH - vph;
    }

    if (vpw && vph) viewPort= WebPlot.makeViewPort(newVpX,newVpY,vpw,vph);

    return viewPort;
}


function checkBounds(pos,screenSize,scrollSize) {
    var max= Math.max(screenSize-scrollSize,0);
    if (pos<0)        return 0;
    else if (pos>max) return max;
    else              return  pos;
}




function findScrollPtForCenter(plotView) {
    var {width,height}= plotView.viewDim;
    var {width:scrW,height:scrH}= primePlot(plotView).screenSize;
    var x= (scrW>width) ? scrW/2- width/2 : 0;
    var y= (scrH>height) ? scrH/2- height/2 : 0;
    return makeScreenPt(x,y);
}

/**
 *
 * @param plotView
 * @param ipt
 */
function findScrollPtForImagePt(plotView, ipt) {
    var {width,height}= plotView.viewDim;
    var plot= primePlot(plotView);
    var {width:scrW,height:scrH}= plot.screenSize;
    var center= CCUtil.getScreenCoords(plot, ipt);
    var x= center.x- width/2;
    var y= center.y- height/2;
    x= checkBounds(x,scrW,width);
    y= checkBounds(y,scrH,height);
    return makeScreenPt(x,y);
}



