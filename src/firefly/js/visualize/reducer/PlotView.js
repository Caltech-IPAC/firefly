/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * REDUCER USE ONLY
 * REDUCER USE ONLY
 * REDUCER USE ONLY
 */

//import ImagePlotCntlr from './ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import WebPlot from './../WebPlot.js';
import {GridOnStatus, ExpandedTitleOptions, WPConst} from './../WebPlotRequest.js';
import {RotateType} from './../PlotState.js';
import {makeScreenPt} from './../Point.js';
import AppDataCntlr from '../../core/AppDataCntlr.js';
import ImagePlotCntlr from './../ImagePlotCntlr.js';
import VisUtil from './../VisUtil.js';
import PlotViewUtil from './../PlotViewUtil.js';
import PlotPref from './../PlotPref.js';
import {DEFAULT_THUMBNAIL_SIZE} from './../WebPlotRequest.js';
import SimpleMemCache from '../../util/SimpleMemCache.js';
import {CCUtil} from './../CsysConverter.js';

export const DATASET_INFO_CONVERTER = 'DATASET_INFO_CONVERTER';

const DEF_WORKING_MSG= 'Plotting ';



//============ EXPORTS ===========
//============ EXPORTS ===========

export default {makePlotView, replacePlots,
                updateViewDim, updatePlotViewScrollXY, replacePrimary, replacePrimaryInAry,
                findCurrentCenterPoint, findScrollPtForImagePt,
                replacePlotView, updatePlotGroupScrollXY};

//============ EXPORTS ===========
//============ EXPORTS ===========



//======================================== Exported Functions =============================
//======================================== Exported Functions =============================
//======================================== Exported Functions =============================



/**
 * @param {string} plotId
 * @param {WebPlotRequest} req
 * @return {{plotId: *, plotGroupId: *, drawingSubGroupId: *, plots: Array, primaryPlot: null, plotCounter: number, wcsMarginX: number, wcsMarginY: number, scrollX: number, scrollY: number, scrollWidth: number, scrollHeight: number, viewDim: {width: number, height: number}, overlayPlotViews: Array, containsMultiImageFits: boolean, containsMultipleCubes: boolean, lockPlotHint: boolean, attributes: {}, taskCnt: number, preferenceColorKey: *, preferenceZoomKey: *, defThumbnailSize, options: {showInlineTitle: boolean, inlineTitleAlwaysOnIfCollapsed: boolean, workingMsg: string, removeOldPlot: boolean, allowImageSelect: boolean, hasNewPlotContainer: *, allowImageLock: boolean, rotateNorth: boolean, userModifiedRotate: boolean, autoTearDown: boolean, saveCorners: boolean, active: boolean, boxSelection: boolean, catalogButton: boolean, hideTitleDetail: boolean, useInlineToolbar: boolean, showUnexpandedHighlight: boolean, useLayerOnPlotToolbar: boolean, turnOnGridAfterPlot: GridOnStatus, expandedTitleOptions: ExpandedTitleOptions}}}
 */
function makePlotView(plotId, req) {
    var pv= {
        plotId,
        plotGroupId: req.getPlotGroupId(),
        drawingSubGroupId: req.getDrawingSubGroupId(), //todo, string, this is an id
        plots:[],
        primaryPlot: null,
        plotCounter:0, // index of how many plots, used for making next ID
        wcsMarginX: 0, // todo
        wcsMarginY: 0, // todo
        scrollX : 0,
        scrollY : 0,
        viewDim : {width:0, height:0}, // size of viewable area  (div size: offsetWidth & offsetHeight)
        overlayPlotViews: [], //todo
        containsMultiImageFits : false,
        containsMultipleCubes : false,
        lockPlotHint: false, //todo
        attributes: {},
        taskCnt: 0, //todo,
        preferenceColorKey: req.getPreferenceColorKey(),
        preferenceZoomKey:  req.getPreferenceZoomKey(),
        defThumbnailSize: DEFAULT_THUMBNAIL_SIZE,

        options : {
            showInlineTitle : false,
            inlineTitleAlwaysOnIfCollapsed : false,

            // many options -- todo figure out how to set and change, some are set by request, how about the others?
            workingMsg      : DEF_WORKING_MSG,
            removeOldPlot   : true, // if false keep the last plot for flipping, if true remove the old one before plotting, todo
            allowImageSelect: req.isAllowImageSelection(), // show the image selection button in the toolbar, user can change image, todo
            hasNewPlotContainer: req.getHasNewPlotContainer(), // if image selection dialog come up, allow to create a new MiniPlotWidth, todo
            allowImageLock  : false, // show the image lock button in the toolbar, todo
            rotateNorth     : false, // rotate this plot north when plotting,
            userModifiedRotate: false, // the user modified the rotate status, todo
            autoTearDown    : true,  // tear down when there is a new search, todo
            saveCorners     : req.getSaveCorners(), // save the four corners of the plot to the ActiveTarget singleton, todo
            active          : true,  // this is the active MiniPlotWidget, todo
            boxSelection    : false, // type of highlighting used when user selects this widget todo
            catalogButton   : false, // show the catalog select button, todo
            hideTitleDetail : req.getHideTitleDetail(), // hide the zoom level and rotation shown in the title, todo
            useInlineToolbar: true, // show the Tool bar inline instead of on the title bar, todo
            showUnexpandedHighlight: true, // show the selected image highlight when not expanded, todo
            useLayerOnPlotToolbar: true, // show the Layer button on the plot toolbar, todo
            turnOnGridAfterPlot: req.getGridOn(), // turn on the grid after plot, todo
            expandedTitleOptions: req.getExpandedTitleOptions()
        }
    };

    if (req.containsParam(WPConst.GRID_ID)) {
        pv.attributes[WPConst.GRID_ID]= req.getGridId();
    }


    return pv;
}


const initScrollCenterPoint= (pv) => updatePlotViewScrollXY(pv,findScrollPtForCenter(pv));

/**
 *
 * @param pv
 * @param plotId
 * @param plotAry
 * @param addToHistory
 */
function replacePlots(pv, plotAry) {

    //pv= pv || makePlotView(plotId);
    pv= Object.assign({},pv);

    if (pv.plots && pv.plots.length) {
        pv.plots.forEach( (plot) => {
            SimpleMemCache.clearCache(plot.plotImageId);
            // todo- clean up before resetting the array - go through old array call server to delete plot
            //todo -- somewhere need to call the server with a delete plot - probably needs to be an action or side effect
        });
    }



    pv.plots= plotAry;

    pv.plots.forEach( (plot) => {
        Object.assign(plot.attributes, getNewAttributes(plot));
        plot.plotImageId= `${pv.plotId}--${pv.plotCounter}`;
        pv.plotCounter++;
    });


    pv.primaryPlot= pv.plots[0];

    PlotPref.putCacheColorPref(pv.preferenceColorKey, pv.primaryPlot.plotState);
    PlotPref.putCacheZoomPref(pv.preferenceZoomKey, pv.primaryPlot.plotState);


    setClientSideRequestOptions(pv,pv.primaryPlot.plotState.getWebPlotRequest());

    pv.containsMultiImageFits= pv.plots.every( (p) => p.plotState.isMultiImageFile());
    pv.containsMultipleCubes= pv.plots.every( (p) => p.plotState.getCubeCnt()>1);
    pv.options.rotateNorth= pv.primaryPlot.plotState.getRotateType()===RotateType.NORTH;

    //--------- set initialized viewport here
    //var {scrollWidth,scrollHeight} = computeScrollSizes(pv.primaryPlot,pv.viewDim);
    //pv.scrollWidth= scrollWidth;
    //pv.scrollHeight= scrollHeight;
    pv= initScrollCenterPoint(pv);

    return pv;
}

/**
 * update the offset with and height of the primary div
 * @param {object} pv
 * @param {{width : number, height : number}} viewDim
 * @return {object} the PlotView with the new viewDim
 */
function updateViewDim(pv,viewDim) {
    return Object.assign({}, pv, {viewDim});
}


/**
 * create a copy of the PlotView with a new scroll position and a new view port if necessary
 * @param {object} plotView the current plotView
 * @param {object} newScrollPt  the screen point of the scroll position
 * @return {object} new copy of plotView
 */
function updatePlotViewScrollXY(plotView,newScrollPt) {
    if (!plotView || !newScrollPt) return plotView;

    var {primaryPlot:plot,scrollX:oldSx,scrollY:oldSy}= plotView;
    var {scrollWidth,scrollHeight}= getScrollSize(plotView);
    if (!plot || !scrollWidth || !scrollHeight) return plotView;

    var {x:newSx,y:newSy}= newScrollPt;
    if (newSx===oldSx && newSy===oldSy) return plotView;

    newSx= checkBounds(newSx,plot.screenSize.width,scrollWidth);
    newSy= checkBounds(newSy,plot.screenSize.height,scrollHeight);

    var newPlotView= Object.assign({},plotView, {scrollX:newSx, scrollY:newSy});

    if (isRecomputeViewPortNecessary(newSx,newSy,scrollWidth,scrollHeight,plot.viewPort) ) {
        var cp= CCUtil.getScreenCoords(plot,findCurrentCenterPoint(plotView,newSx,newSy));
        var viewPort= computeViewPort(plot,scrollWidth,scrollHeight,cp);
        var newPrimary= WebPlot.setWPViewPort(plot,viewPort);
        newPlotView.plots= plotView.plots.map( (p) => p===plot ? newPrimary : p);
        newPlotView.primaryPlot= newPrimary;
    }

    return newPlotView;
}

/**
 * replace a plotview in the plotViewAry with the passed plotview whose plotId's match
 * @param {[]} plotViewAry
 * @param {object} newPlotView
 * @return {[]} new plotView array after return a plotview
 */
function replacePlotView(plotViewAry,newPlotView) {
    return plotViewAry.map( (pv) => pv.plotId===newPlotView.plotId ? newPlotView : pv);
}

/**
 *
 * @param plotView
 * @param primaryPlot
 * @return {*} return the new PlotView object
 */
function replacePrimary(plotView,primaryPlot) {
    var newPlotView= Object.assign({},plotView, {primaryPlot});
    newPlotView.plots= plotView.plots.map( (p) => p===plotView.primaryPlot? primaryPlot : p);
    return newPlotView;
}


function replacePrimaryInAry(plotViewAry, pv, plot) {
    //var pv= PlotViewUtil.findPlotView(plot.plotId, plotViewAry);
    if (!pv) return plotViewAry;
    pv= replacePrimary(pv,plot);
    return replacePlotView(plotViewAry,pv);
}






/**
 * scroll a plot view to a new screen pt, if plotGroup.lockRelated is true then all the plotviews in the group
 * will be scrolled to match
 * @param plotId plot id to set the scrolling on
 * @param {[]} plotViewAry an array of plotView
 * @param {[]} plotGroupAry the plotGroup array
 * @param newScrollPt a screen point in the plot to scroll to
 * @return {[]}
 */
function updatePlotGroupScrollXY(plotId,plotViewAry, plotGroupAry, newScrollPt) {
    var plotView= updatePlotViewScrollXY(PlotViewUtil.findPlotView(plotId,plotViewAry),newScrollPt);
    plotViewAry= replacePlotView(plotViewAry, plotView);
    var plotGroup= PlotViewUtil.findPlotGroup(plotView.plotGroupId,plotGroupAry);
    if (plotGroup && plotGroup.lockRelated) {
        plotViewAry= PlotViewUtil.matchPlotView(plotView,plotViewAry,plotGroup,makeScrollPosMatcher(plotView));
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
    var {primaryPlot:{screenSize:{width:srcScreenWidth,height:srcScreenHeight}},
        scrollX:srcSx,scrollY:srcSy}= sourcePV;
    var {scrollWidth:srcSW,scrollHeight:srcSH}= getScrollSize(sourcePV);
    var percentX= (srcSx+srcSW/2) / srcScreenWidth;
    var percentY= (srcSy+srcSH/2) / srcScreenHeight;

    return (pv) => {
        var retPV= pv;
        if (pv && pv.primaryPlot) {
            var {primaryPlot:{screenSize:{width,height}}}= pv;
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
    else if (AppDataCntlr.getActiveTarget()) {
        worldPt= AppDataCntlr.getActiveTarget();
    }
    else {
        worldPt= VisUtil.getCenterPtOfPlot(plot);
    }

    if (worldPt) attributes[WPConst.FIXED_TARGET]= worldPt;
    if (circle) attributes[WPConst.REQUESTED_SIZE]= circle.radius;  // says radius but really size
    if (req.getUniqueKey())     attributes[WPConst.UNIQUE_KEY]= req.getUniqueKey();
    if (req.isMinimalReadout()) attributes[WPConst.MINIMAL_READOUT]=true;

    return attributes;
}





/**
 * Given the scrollX and scrollY then find the point in the plo that is at the center of
 * the display.  The point returned is in ImagePt coordinates.
 * We return it in and ImagePt not screen because if the plot
 * is zoomed the image point will be what we want in the center.
 * The screen coordinates will be completely different.
 * @return ImagePt the center point
 * @param plotView
 * @param [scrollX] optional scrollX if not defined us plotView.scrollX
 * @param [scrollY] optional scrollY if not defined us plotView.scrollY
 */
function findCurrentCenterPoint(plotView,scrollX,scrollY) {
    var {wcsMarginX,wcsMarginY, primaryPlot}= plotView;
    var {scrollWidth,scrollHeight}= getScrollSize(plotView);
    var sx= (typeof scrollX !== 'undefined') ? scrollX : plotView.scrollX;
    var sy= (typeof scrollY !== 'undefined') ? scrollY : plotView.scrollY;
    if (!primaryPlot) return null;

    var {width:screenW, height:screenH}= primaryPlot.screenSize;
    var cX=  (screenW<scrollWidth) ? screenW/2 : sx+scrollWidth/2- wcsMarginX;
    var cY= (screenH<scrollHeight) ? screenH/2 : sy+scrollHeight/2- wcsMarginY;
    var pt= makeScreenPt(cX,cY);
    return CCUtil.getImageCoords(primaryPlot,pt);
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
    var contains= VisUtil.containsRec(x,y, dim.width,dim.height,
                                      scrollX,scrollY,scrollWidth-1,scrollHeight-1);
    return !contains;
}


/**
 *
 * @param primaryPlot
 * @param {{width: number, height: number}} viewDim
 * @return {{scrollWidth: number, scrollHeight: number}}
 */
function computeScrollSizes(primaryPlot,viewDim) {
    var {screenSize}= primaryPlot;
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
const getScrollSize = (plotView) => computeScrollSizes(plotView.primaryPlot,plotView.viewDim);




/**
 *
 * Compute a view port based on the visibleCenterPt
 * @param primaryPlot
 * @param scrollWidth
 * @param scrollHeight
 * @param {object} visibleCenterPt, screen point
 * @return {{dim: {width : number, height : number}, x: number, y: number}}
 */
function computeViewPort(primaryPlot, scrollWidth, scrollHeight, visibleCenterPt) {
    if (!primaryPlot) return null;

    var {viewPort}= primaryPlot;
    var {width:screenW, height:screenH} = primaryPlot.screenSize;

    var vpw = scrollWidth * 2;
    var vph = scrollHeight * 2;

    if (vpw > 1500) vpw = Math.max((scrollWidth * 1.5), 2700);
    if (vph > 1400) vph = Math.max((scrollHeight * 1.5), 1800);

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
    var {width:scrW,height:scrH}= plotView.primaryPlot.screenSize;
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
    var {width:scrW,height:scrH}= plotView.primaryPlot.screenSize;
    var center= CCUtil.getScreenCoords(plotView.primaryPlot, ipt);
    var x= center.x- width/2;
    var y= center.y- height/2;
    x= checkBounds(x,scrW,width);
    y= checkBounds(y,scrH,height);
    return makeScreenPt(x,y);
}



