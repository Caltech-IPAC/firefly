/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import {xor,isNil, isEmpty,isString, isFunction, throttle, isNumber, isArray} from 'lodash';
import {getAppOptions} from '../core/AppDataCntlr.js';
import {flux} from '../core/ReduxFlux.js';
import {useColorMode} from '../ui/FireflyRoot.jsx';
import {ImageRender} from './iv/ImageRender.jsx';
import {EventLayer} from './iv/EventLayer.jsx';
import {ImageViewerStatus} from './iv/ImageViewerStatus.jsx';
import {makeScreenPt, makeDevicePt} from './Point.js';
import {DrawerComponent}  from './draw/DrawerComponent.jsx';
import {CCUtil, CysConverter} from './CsysConverter.js';
import {UserZoomTypes} from './ZoomUtil.js';
import {
    primePlot, getPlotViewById, hasLocalStretchByteData, isActivePlotView, getFoV
} from './PlotViewUtil.js';
import {
    isImageViewerSingleLayout, getMultiViewRoot, findViewerWithItemId, IMAGE, getViewer, getExpandedViewerItemIds,
    EXPANDED_MODE_RESERVED
} from './MultiViewCntlr.js';
import {contains, getBoundingBox, intersects} from './VisUtil.js';
import BrowserInfo from '../util/BrowserInfo.js';

import {
    visRoot, ActionScope, dispatchPlotProgressUpdate, dispatchZoom, dispatchRecenter, dispatchProcessScroll,
    dispatchChangeCenterOfProjection, dispatchChangeActivePlotView,
    dispatchUpdateViewSize, dispatchRequestLocalData, MOUSE_CLICK_REASON, ExpandType
} from './ImagePlotCntlr.js';
import {fireMouseCtxChange, makeMouseStatePayload, MouseState} from './VisMouseSync.js';
import {isHiPS, isHiPSAitoff, isImage} from './WebPlot.js';
import {plotMove} from './PlotMove';

const DEFAULT_CURSOR= 'crosshair';

// const {MOVE,DOWN,DRAG,UP, DRAG_COMPONENT, EXIT, ENTER}= MouseState;
const rootStyle= { position:'absolute', left : 0, right : 0, top : 0, bottom : 0, overflow:'hidden' };


export const ImageViewerLayout= memo(({ plotView, drawLayersAry, width, height, externalWidth, externalHeight}) => {
    const [cursor,setCursor]= useState(DEFAULT_CURSOR);
    const {current:eRef}= useRef({
        scroll:makeScroll(),
        handleScrollWheelEvent: makeHandleScrollWheelEvent(),
        mouseOwnerLayerId: undefined,
        previousDim: makePrevDim({width,height,externalWidth,externalHeight})});

    const {scroll,handleScrollWheelEvent}= eRef;
    const {plotId,viewDim={}}= plotView??{};
    const plot= primePlot(plotView);
    const hasPlot= Boolean(plot);
    const plotShowing= Boolean(viewDim.width && viewDim.height && plot && !plotView.nonRecoverableFail);
    const onScreen= !plotShowing || isImageOnScreen(plotView);
    const sizeViewable= !plotShowing || isImageSizeViewable(plotView);
    const loadingRawData= plotShowing && isImage(plot) && !plot?.tileData && !hasLocalStretchByteData(plot);

    useEffect(() => {
        if (width && height) {
            getDataIfNecessary(plotView);
            dispatchUpdateViewSize(plotId,width,height);
        }
        if (plot) {
            const paging= isImageViewerSingleLayout(getMultiViewRoot(), visRoot(), plotId);
            updateZoom(plotId,paging);
        }
    },[]);

    useEffect(() => {
        if (!plotView || !width || !height) return;
        getDataIfNecessary(plotView);
        if (sizeChange(eRef.previousDim,width,height,viewDim)) {
            dispatchUpdateViewSize(plotId,width,height); // case: any resizing
            if (plot) {
                // case: resizing, todo: document how this is different than normal resizing
                const {prevExternalWidth, prevExternalHeight}= eRef.previousDim;
                if (prevExternalWidth!==externalWidth || prevExternalHeight!==externalHeight) {
                    updateZoom(plotId,false);
                }
            }
            eRef.previousDim= makePrevDim({width,height,externalWidth,externalHeight});
        }
    });

    useEffect(() => { // case: a new plot force other plot to zoom match
        if (hasPlot) return; // this effect should only do something in the case where the previous render has not plot
        const paging= isImageViewerSingleLayout(getMultiViewRoot(), visRoot(), plotId);
        updateZoom(plotId,paging);
    }, [hasPlot]);


    const eventCB= (eventPlotId,mouseState,screenPt,screenX,screenY,nativeEv) => {
        const {DOWN,MOVE}= MouseState;
        const shiftDown= nativeEv.shiftKey;
        const mouseStatePayload= makeMouseStatePayload(eventPlotId,mouseState,screenPt,screenX,screenY, {shiftDown});
        const list= drawLayersAry.filter(
            (dl) => dl.visiblePlotIdAry.includes(plotId) && dl.mouseEventMap?.[mouseState.key] );

        if (eRef.mouseOwnerLayerId && draggingOrReleasing(mouseState)) { // use layer from the mouseDown
            const dl= getLayer(drawLayersAry,eRef.mouseOwnerLayerId);
            fireMouseEvent(dl,mouseState,mouseStatePayload);
        }
        else if (isWheel(mouseState)) {
            if (!isActivePlotView(visRoot(),eventPlotId) && isWheelRequireImageActive(eventPlotId)) return;
            handleScrollWheelEvent(plotView,mouseState,screenPt,nativeEv);
            return;
        }
        else {
            const ownerCandidate= !shiftDown && findMouseOwner(list,plot,screenPt);         // see if anyone can own that mouse
            eRef.mouseOwnerLayerId = DOWN.is(mouseState) && ownerCandidate ? ownerCandidate.drawLayerId : null;   // can only happen on mouseDown
            if (eRef.mouseOwnerLayerId) {
                if (DOWN.is(mouseState)) dispatchChangeActivePlotView(eventPlotId);
                const dl= getLayer(drawLayersAry,eRef.mouseOwnerLayerId);
                fireMouseEvent(dl,mouseState,mouseStatePayload);
            }
            else { // fire to all non-exclusive layers, scroll, and determine cursor
                list.filter( (dl) => !dl?.exclusiveDef?.exclusiveOnDown)
                    .forEach( (dl) => fireMouseEvent(dl,mouseState,mouseStatePayload) );
                scroll(plotView,mouseState,screenX,screenY,mouseState===DOWN ? screenPt : null );
                const cursorCandidate= ownerCandidate || findMouseOwner(drawLayersAry,plot,screenPt);
                let newCursor = DEFAULT_CURSOR;
                if (MOVE.is(mouseState)) newCursor = cursorCandidate?.getCursor?.(plotView, screenPt) || DEFAULT_CURSOR;
                setCursor(newCursor);
            }
        }
        fireMouseCtxChange(mouseStatePayload);  // this for anyone listening directly to the mouse
    };

    return (
        <div className='web-plot-view-scr' style={rootStyle}>
            <ImageViewerContents {...{drawLayersAry,plotView,eventCallback:eventCB,cursor,plotShowing}}/>
            <MessageArea {...{pv:plotView,plotShowing,onScreen,sizeViewable,loadingRawData}}/>
        </div>
    );

});



const draggingOrReleasing = (ms) => ms===MouseState.DRAG || ms===MouseState.DRAG_COMPONENT ||
    ms===MouseState.UP || ms===MouseState.EXIT || ms===MouseState.ENTER;

const isWheel= (mouseState) => mouseState===MouseState.WHEEL_DOWN || mouseState===MouseState.WHEEL_UP;

const zoomThrottle= throttle( (params) => {
    dispatchZoom(params);
}, 2, {trailing:false});


const zoomFromWheelOrTrackpad= (usingMouseWheel, params) => {
    usingMouseWheel ?
        dispatchZoom(params):
        zoomThrottle(params);
};

function isWheelRequireImageActive(plotId) {
    const {expandedMode}= visRoot();
    if (getAppOptions()?.wheelScrollRequiresImageActive) return true;
    const mvRoot= getMultiViewRoot();

    if (expandedMode!==ExpandType.COLLAPSE && getExpandedViewerItemIds(mvRoot)?.includes(plotId)) {
        return getViewer(mvRoot,EXPANDED_MODE_RESERVED)?.scroll ?? false;
    }
    else {
        const viewerId= findViewerWithItemId(mvRoot, plotId, IMAGE);
        return getViewer(mvRoot, viewerId)?.scroll ?? false
    }
}


/**
 * when a resize happens and zoom locking is enable then we need to start a zoom level change
 * @param {string} plotId
 * @param {boolean} paging
 */
function updateZoom(plotId, paging) {
    const vr= visRoot();
    const pv= getPlotViewById(vr, plotId);
    const plot= primePlot(pv);

    if (!plot) return;
    if (!pv.plotViewCtx.zoomLockingEnabled) return;

    let doZoom;
    let actionScope= ActionScope.GROUP;
    if (!paging && vr.wcsMatchType && plotId!==vr.mpwWcsPrimId) {
        doZoom= false;
    }
    else if (isImageViewerSingleLayout(getMultiViewRoot(), vr, plotId)) {
        doZoom= plot.viewDim.width>=plot.screenSize.width && plot.viewDim.height>=plot.screenSize.height;
        actionScope= ActionScope.SINGLE;
    }
    else {  // case: not expanded or expand as grid
            // if plot wcsMatchingType is active then only the prime plot will do the zooming for all the group.
            // otherwise zoom each one.
        if (vr.wcsMatchType) {
            doZoom= vr.activePlotId===plotId;
            actionScope= ActionScope.GROUP;
        }
        else {
            doZoom= plot.viewDim.width>=plot.screenSize.width && plot.viewDim.height>=plot.screenSize.height;
            actionScope= ActionScope.SINGLE;
        }
    }
    if (doZoom && isHiPS(plot)) {
        const fov= getFoV(plot);
        doZoom= isHiPSAitoff(plot) ? fov>340 : fov>170;
    }

    if (doZoom) {
        const userZoomType= (paging && vr.wcsMatchType) ? UserZoomTypes.WCS_MATCH_PREV : pv.plotViewCtx.zoomLockingType;
        dispatchZoom({ plotId, userZoomType, zoomLockingEnabled: true, actionScope });
    }
}



function getPlotImageToRequest(pv) {
    const plot= primePlot(pv);
    if (!plot) return undefined;
    const {viewDim:{width,height}}= pv;
    if (!width || !height) return undefined;
    if (plot.dataRequested && !pv.overlayPlotViews?.length) return undefined;
    if (!plot.dataRequested) return {plotImageId:plot.plotImageId};
    if (pv.overlayPlotViews?.length) {
        const overPv= pv.overlayPlotViews.find( (oPv) => oPv?.plot?.dataRequested===false);
        if (!overPv) return undefined;
        return {plotImageId:overPv.plot.plotImageId, imageOverlayId:overPv.imageOverlayId};
    }
    return undefined;

}

function getDataIfNecessary(pv) {
    const result= getPlotImageToRequest(pv);
    if (!result) return;
    const plot= primePlot(pv);
    if (!plot) return;
    const {plotId}= plot;
    dispatchRequestLocalData({plotId,plotImageId:result.plotImageId, imageOverlayId:result.imageOverlayId});
}



ImageViewerLayout.propTypes= {
    plotView : PropTypes.object.isRequired,
    drawLayersAry: PropTypes.array.isRequired,
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    externalWidth: PropTypes.number.isRequired,
    externalHeight: PropTypes.number.isRequired
};

const ImageViewerContents= memo(({drawLayersAry=[],plotView,eventCallback,cursor,plotShowing}) => {
    const colorMode= useColorMode()?.activeMode;
    if (!plotShowing) return;
    const {plotId,viewDim:{width,height}={}}= plotView??{};
    const rootStyle= {left:0, top:0, bottom:0, right:0, position:'absolute', marginRight: 'auto', marginLeft: 0 };
    return (
        <div className='plot-view-scroll-view-window' style={rootStyle}>
            <div className='plot-view-master-panel' style={{width,height, left:0,right:0,position:'absolute', cursor}}>
                {makeTileDrawers(plotView, colorMode)}
                <DrawingLayers
                    key={'DrawingLayers:'+plotId}
                    plotView={plotView} drawLayersIdAry={drawLayersAry?.map( (dl) => dl.drawLayerId)} />
            </div>
            <EventLayer plotId={plotId} key={'EventLayer:'+plotId}
                        transform={plotView.affTrans} eventCallback={eventCallback}/>
        </div>
    );
});


function scrollMove(plotDrag, plotId, screenX,screenY) {
    const pv= getPlotViewById(visRoot(), plotId);
    const newScrollPt= plotDrag(screenX,screenY, pv);
    if (isImage(primePlot(pv))) {
        dispatchProcessScroll({plotId,scrollPt:newScrollPt});
    }
    else {
        const cc= CysConverter.make(primePlot(pv));
        const wp= cc.getWorldCoords(newScrollPt);
        if (!wp) return;
        dispatchChangeCenterOfProjection({plotId,centerProjPt:wp});
    }
}

function makeScroll() {
    let plotDrag= null;
    const scrollMoveThrottled= BrowserInfo.isFirefox() ? throttle(scrollMove,30) : throttle(scrollMove,15);
    const scroll= (plotView,mouseState,screenX,screenY, mouseDownScreenPt) => {
        if (!screenX && !screenY) return;
        const {plotId} = plotView;

        switch (mouseState) {
            case MouseState.DOWN :
                dispatchChangeActivePlotView(plotId, MOUSE_CLICK_REASON);
                const {scrollX, scrollY} = plotView;
                plotDrag = plotMove(screenX, screenY, makeScreenPt(scrollX, scrollY), mouseDownScreenPt, plotView);
                break;
            case MouseState.DRAG :
                if (plotDrag) scrollMoveThrottled(plotDrag, plotId, screenX, screenY);
                break;
            case MouseState.UP :
                plotDrag = null;
                break;
        }
    };
    return scroll;
}


function makeHandleScrollWheelEvent() {
    let mouseWheelDevicePt= undefined;
    let mouseWheelTimeoutId= undefined;

    const handleScrollWheelEvent= (plotView, mouseState, screenPt, nativeEv) => {

        if (!mouseWheelDevicePt) {
            mouseWheelDevicePt= CCUtil.getDeviceCoords(primePlot(plotView),screenPt);
            mouseWheelTimeoutId= setTimeout(() => {
                mouseWheelDevicePt= undefined;
            }, 200);
        }
        else {
            clearTimeout(mouseWheelTimeoutId);
            mouseWheelTimeoutId= setTimeout(() => {
                mouseWheelDevicePt= undefined;
            }, 200);
        }

        const userZoomType= mouseState===MouseState.WHEEL_DOWN ? UserZoomTypes.UP : UserZoomTypes.DOWN;
        nativeEv.preventDefault();
        const plot= primePlot(plotView) ?? {};
        const {screenSize}= plot;
        const {viewDim}= plotView ?? {};
        const smallImage=
            isImage(plot) && screenSize?.width < viewDim?.width && screenSize?.height < viewDim?.height;
        let useDevPt= true;
        if (smallImage) {
            const cc= CysConverter.make(plot);
            useDevPt= cc.pointInPlot(mouseWheelDevicePt);
        }

        const usingMouseWheel= Math.abs(nativeEv.wheelDeltaY)%120 === 0;

        zoomFromWheelOrTrackpad(usingMouseWheel,
            {plotId:plotView?.plotId, userZoomType, devicePt: useDevPt ? mouseWheelDevicePt : undefined,
                upDownPercent:Math.abs(nativeEv.wheelDeltaY)%120===0?1:  isHiPS(plot)? .2 : .5 } );

    };
    return handleScrollWheelEvent;
}



/**
 * Is any part of the image on the screen.
 * It does this by testing to see if set of device points is inside screen rectangle or if a set of screen points
 * is inside the device rectangle.
 * The arrays of points, each corner and the center of the screen pt system and the device point system.
 * @param plotView
 * @return {boolean}
 */
function isImageOnScreen(plotView) {

    const {viewDim}= plotView;
    const plot= primePlot(plotView);
    if (isHiPS(plot)) return true;

    if (isNil(plotView.scrollX) || isNil(plotView.scrollY)) return false;
    const cc= CysConverter.make(plot);
    const {screenSize}= plot;
    const devAsScreenAry= [
        cc.getScreenCoords(makeDevicePt(0,0)),
        cc.getScreenCoords(makeDevicePt(viewDim.width,0)),
        cc.getScreenCoords(makeDevicePt(viewDim.width,viewDim.height)),
        cc.getScreenCoords(makeDevicePt(0,viewDim.height)),
        cc.getScreenCoords(makeDevicePt(viewDim.width/2,viewDim.height/2)),
    ];


    const screenAsDevAry= [
        cc.getDeviceCoords(makeScreenPt(0,0)),
        cc.getDeviceCoords(makeScreenPt(screenSize.width,0)),
        cc.getDeviceCoords(makeScreenPt(screenSize.width,screenSize.height)),
        cc.getDeviceCoords(makeScreenPt(0,screenSize.height)),
        cc.getDeviceCoords(makeScreenPt(screenSize.width/2,screenSize.height/2)),
    ];

    let found=  devAsScreenAry.some( (pt) =>
        pt && (contains(0,0,screenSize.width, screenSize.height,pt.x,pt.y)  ||
        intersects(0,0,screenSize.width, screenSize.height,pt.x,pt.y,1,1)));

    if (!found) {
        found= screenAsDevAry.some( (pt) => pt && (contains(0,0,viewDim.width, viewDim.height,pt.x,pt.y) ||
            intersects(0,0,viewDim.width, viewDim.height,pt.x,pt.y,1,1) ));
        if (!found) {
            const {x,y,w,h}= getBoundingBox(screenAsDevAry);
            found= !found && intersects(0,0,viewDim.width, viewDim.height,x,y,w,h);
        }
    }
    return found;
}

function isImageSizeViewable(plotView) {
    const plot= primePlot(plotView);
    if (!plot) return false;
    const {screenSize:{width,height}}= plot;
    return (width>5 || height>5);
}


/**
 *
 * @param {PlotView} pv
 * @return {Array}
 */
function makeTileDrawers(pv, colorMode) {


    const plot= primePlot(pv);
    const rootDrawer= (
        <ImageRender opacity={1} plot={plot} plotView={pv} key={'TileDrawer:'+pv.plotId} idx={0} colorMode={colorMode}/>
    );
    const drawers= pv.overlayPlotViews
        .filter( (opv) => opv.visible && opv.plot && plot.dataWidth===opv.plot?.dataWidth && plot.dataHeight===opv.plot?.dataHeight)
        .map( (opv,idx) => {
            return (
                <ImageRender opacity={opv.opacity} plot={opv.plot} plotView={pv} colorMode={colorMode}
                             idx={idx+1} key={'TileDrawer-overlay:'+opv.imageOverlayId} />
            );
        });
    if (pv.visible) drawers.unshift(rootDrawer);
    return drawers;
}

/**
 *
 * @param {object} p
 * @param p.width
 * @param p.height
 * @param p.externalWidth
 * @param p.externalHeight
 * @return {{prevWidth, prevHeight, prevExternalWidth, prevExternalHeight}}
 */
function makePrevDim({width,height,externalWidth,externalHeight}) {
    return {
        prevWidth:width,
        prevHeight:height,
        prevExternalWidth:externalWidth,
        prevExternalHeight:externalHeight,
    };
}

function sizeChange(previousDim,width,height,viewDim) {
    const {prevWidth,prevHeight}= previousDim;
    return (prevWidth!==width || prevHeight!==height || (!viewDim.width && !viewDim.height && width && height));
}


function MessageArea({pv,plotShowing,onScreen, sizeViewable, loadingRawData}) {
    if (pv.serverCall==='success' && !pv.nonRecoverableFail) {
        if (loadingRawData) {
            return (
                <ImageViewerStatus message={'Loading Image Rendering'} working={true}
                                   maskWaitTimeMS= {500} messageWaitTimeMS={1000} useMessageAlpha={plotShowing}/>
            );
        }
        else if (!onScreen) {
            return (
                <ImageViewerStatus message={'Center Plot'} working={false} top={45}
                                   useMessageAlpha={false} buttonText='Recenter'
                                   buttonCB={() => dispatchRecenter({plotId:pv.plotId, centerOnImage:true}) } />
            );
        }
        else if (!sizeViewable) {
            return (
                <ImageViewerStatus message={'Minimum zoom'} working={false} top={45}
                                   useMessageAlpha={false} buttonText='Zoom To Fit'
                                   buttonCB={() => dispatchZoom({plotId:pv.plotId, userZoomType:UserZoomTypes.FIT}) }/>
            );
        }
        else {
            return false;
        }
    }
    else if (pv.serverCall==='working') {
        return (
            <ImageViewerStatus message={pv.plottingStatusMsg} working={true}
                               maskWaitTimeMS= {500} messageWaitTimeMS={1000} useMessageAlpha={plotShowing}/>
            );

    }
    else if (pv.serverCall==='fail' || pv.nonRecoverableFail) {
        const buttonCB= plotShowing ?
            () => dispatchPlotProgressUpdate(pv.plotId,'',true, pv.request.getRequestKey()) : undefined;
        return (
            <ImageViewerStatus message={pv.plottingStatusMsg || 'Image Data Plot Fail'}
                               working={false} useMessageAlpha={!pv.nonRecoverableFail}
                               buttonText='OK' buttonCB={buttonCB} />
        );
    }
}


/**
 * Do the following:
 *    1. First look for a layers that has exclusiveDef.exclusiveOnDown as true
 *    2. if any of those has exclusiveDef.type === 'anywhere' then return the last in the list
 *    3. otherwise if any layer has exclusiveDef.type === 'vertexOnly'  or 'vertexThenAnywhere' return the first that has
 *              the mouse click near is one if its vertex (vertexDef.points)
 *    4. otherwise if any layer has exclusiveDef.type === 'vertexThenAnywhere' then return that one
 *    5. otherwise return null
 * @param dlList
 * @param plot
 * @param screenPt
 * @return {*}
 */
function findMouseOwner(dlList, plot, screenPt) {
                    // Step 1
    const exList= dlList.filter((dl) => dl?.exclusiveDef?.exclusiveOnDown);
    if (isEmpty(exList) || !screenPt) return;

                    // Step 2
    const nowList= exList.filter((dl) => dl?.exclusiveDef?.type==='anywhere');

    if (!isEmpty(nowList)) return nowList[nowList.length-1];

                      // Step 3
    const cc= CysConverter.make(plot);

    const getDist = (vertexDef) => {
        const {pointDist} = vertexDef || {};
        return isNumber(pointDist) ?  pointDist : (pointDist?.[cc.plotId] ?? 0.0);
    };

    const getPoints = (vertexDef) => {
        const {points} = vertexDef || {};
        return isArray(points) ? points : points?.[cc.plotId] ?? [];
    };

    const vertexDL= exList
        .filter((dl) => {
            const exType= dl?.exclusiveDef?.type ?? '';
            return (exType==='vertexOnly' || exType==='vertexThenAnywhere') && !isEmpty(dl?.vertexDef?.points);
        })
        .find( ({vertexDef})  => {
            const dist = getDist(vertexDef) || 5;
            const x= screenPt.x- dist;
            const y= screenPt.y- dist;
            const w= dist*2;
            const h= dist*2;
            return getPoints(vertexDef).find( (pt) => {
                const spt= cc.getScreenCoords(pt);
                return spt && contains(x,y,w,h,spt.x,spt.y);
            } );
        });

    if (vertexDL) return vertexDL;

                     // Step 4 and 5
    const anyWhereList= dlList.filter((dl) => dl?.exclusiveDef?.type==='vertexThenAnywhere');
    return isEmpty(anyWhereList) ? undefined : anyWhereList[anyWhereList.length-1];
}

function fireMouseEvent(drawLayer,mouseState,mouseStatePayload) {
    const payload= Object.assign({},mouseStatePayload,{drawLayer});
    const fireObj= drawLayer?.mouseEventMap[mouseState.key];
    if (isString(fireObj)) {
        flux.process({type: fireObj, payload});
    }
    else if (isFunction(fireObj)) {
        fireObj(payload);
    }
}


const getLayer= (list,drawLayerId) => list.find( (dl) => dl.drawLayerId===drawLayerId);

const DrawingLayers= memo( ({plotView:pv, drawLayersIdAry:dlIdAry}) =>{
    const plot= primePlot(pv);
    if (isNil(pv.scrollX) || isNil(pv.scrollY)) return false;
    const {width,height}= pv.viewDim;
    const drawingAry= dlIdAry?.map( (dlId, idx) => (<DrawerComponent plot={plot} drawLayerId={dlId}
                                                                  width={width} height={height}
                                                                  idx={idx} key={dlId}/>) );
    return (
        <div className='drawingArea' style={{width, height, left:0, right:0, position:'absolute'}}>
            {drawingAry}
        </div>
    );

},
(p,np) => {
    return primePlot(p.plotView)===primePlot(np.plotView) &&
        isEmpty(xor(np.drawLayersIdAry,p.drawLayersIdAry)) &&
        np.plotView.scrollX===p.plotView.scrollX &&
        np.plotView.scrollY===p.plotView.scrollY;
});

DrawingLayers.propTypes= {
    plotView: PropTypes.object.isRequired,
    drawLayersIdAry: PropTypes.array
};
