/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, PureComponent} from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import {xor,isNil, isEmpty,get, isString, isFunction, throttle, isNumber, isArray} from 'lodash';
import {flux} from '../core/ReduxFlux.js';
import {ImageRender} from './iv/ImageRender.jsx';
import {EventLayer} from './iv/EventLayer.jsx';
import {ImageViewerStatus} from './iv/ImageViewerStatus.jsx';
import {makeScreenPt, makeDevicePt} from './Point.js';
import {DrawerComponent}  from './draw/DrawerComponent.jsx';
import {CCUtil, CysConverter} from './CsysConverter.js';
import {UserZoomTypes}  from './ZoomUtil.js';
import {
    primePlot, getPlotViewById, hasLocalStretchByteData, isActivePlotView, canLoadStretchDataDirect
} from './PlotViewUtil.js';
import {isImageViewerSingleLayout, getMultiViewRoot} from './MultiViewCntlr.js';
import {contains, intersects} from './VisUtil.js';
import BrowserInfo from '../util/BrowserInfo.js';

import {
    visRoot,
    ActionScope,
    dispatchPlotProgressUpdate,
    dispatchZoom,
    dispatchRecenter,
    dispatchProcessScroll,
    dispatchChangeCenterOfProjection,
    dispatchChangeActivePlotView,
    dispatchUpdateViewSize, dispatchRequestLocalData
} from './ImagePlotCntlr.js';
import {fireMouseCtxChange, makeMouseStatePayload, MouseState} from './VisMouseSync.js';
import {isHiPS, isImage} from './WebPlot.js';
import Color from '../util/Color.js';
import {plotMove} from './PlotMove';
import {getAppOptions} from 'firefly/api/ApiUtil.js';

const DEFAULT_CURSOR= 'crosshair';

const {MOVE,DOWN,DRAG,UP, DRAG_COMPONENT, EXIT, ENTER}= MouseState;

const draggingOrReleasing = (ms) => ms===DRAG || ms===DRAG_COMPONENT || ms===UP || ms===EXIT || ms===ENTER;

const isWheel= (mouseState) => mouseState===MouseState.WHEEL_DOWN || mouseState===MouseState.WHEEL_UP;

const zoomThrottle= throttle( (params) => {
    ReactDOM.unstable_batchedUpdates(() => dispatchZoom(params) );
}, 2, {trailing:false});


const zoomFromWheelOrTrackpad= (usingMouseWheel, params) => {
    usingMouseWheel ?
        ReactDOM.unstable_batchedUpdates(() => dispatchZoom(params) ) :
        zoomThrottle(params);
};



/**
 * when a resize happens and zoom locking is enable then we need to start a zoom level change
 * @param {string} plotId
 * @param {boolean} paging
 */
function updateZoom(plotId, paging) {
    const vr= visRoot();
    const pv= getPlotViewById(vr, plotId);

    if (!primePlot(pv)) return;
    if (!pv.plotViewCtx.zoomLockingEnabled) return;

    let doZoom;
    let actionScope= ActionScope.GROUP;
    if (!paging && vr.wcsMatchType && plotId!==vr.mpwWcsPrimId) {
        doZoom= false;
    }
    else if (isImageViewerSingleLayout(getMultiViewRoot(), vr, plotId)) {
        doZoom= true;
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
            doZoom= true;
            actionScope= ActionScope.SINGLE;
        }
    }

    if (doZoom) {
        dispatchZoom({
            plotId,
            userZoomType: (paging && vr.wcsMatchType) ? UserZoomTypes.WCS_MATCH_PREV : pv.plotViewCtx.zoomLockingType,
            zoomLockingEnabled: true,
            forceDelay: !hasLocalStretchByteData(primePlot(pv)),
            actionScope
        });
    }
}


const rootStyle= {
    position:'absolute',
    left : 0,
    right : 0,
    top : 0,
    bottom : 0,
    overflow:'hidden'
};

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

export class ImageViewerLayout extends PureComponent {

    constructor(props) {
        super(props);
        this.plotDrag= null;
        this.mouseOwnerLayerId= null;
        this.eventCB= this.eventCB.bind(this);
        this.state= {cursor:DEFAULT_CURSOR};
    }

    componentDidMount() {
        const {width,height, plotView:pv}= this.props;
        this.previousDim= makePrevDim(this.props);
        if (width && height) {
            getDataIfNecessary(pv);
            dispatchUpdateViewSize(pv.plotId,width,height);
        }
        if (primePlot(pv)) {
            const paging= isImageViewerSingleLayout(getMultiViewRoot(), visRoot(), pv.plotId);
            updateZoom(pv.plotId,paging);
        }
    }

    componentDidUpdate(prevProp) {
        const {width,height,externalWidth,externalHeight, plotView:pv}= this.props;
        const {prevWidth,prevHeight, prevExternalWidth, prevExternalHeight}= this.previousDim;
        if (!pv || !width || !height) return;

        const {viewDim}= pv;
        getDataIfNecessary(pv);
        if (prevWidth!==width || prevHeight!==height || (!viewDim.width && !viewDim.height && width && height)) {
            dispatchUpdateViewSize(pv.plotId,width,height); // case: any resizing

            if (primePlot(pv)) {
                                 // case: resizing, todo: document how this is different than normal resizing
                if (prevExternalWidth!==externalWidth || prevExternalHeight!==externalHeight) {
                    updateZoom(pv.plotId,false);
                }
            }
            this.previousDim= makePrevDim(this.props);
        }

        // case: a new plot force other plot to zoom match
        if (!primePlot(prevProp.plotView) &&  primePlot(pv)) {
            const paging= isImageViewerSingleLayout(getMultiViewRoot(), visRoot(), pv.plotId);
            updateZoom(pv.plotId,paging);
        }
    }

    handleScrollWheelEvent(plotView, mouseState, screenPt, nativeEv) {

        if (!this.mouseWheelDevicePt) {
            this.mouseWheelDevicePt= CCUtil.getDeviceCoords(primePlot(plotView),screenPt);
            this.mouseWheelTimeoutId= setTimeout(() => {
                this.mouseWheelDevicePt= undefined;
            }, 200);
        }
        else {
            clearTimeout(this.mouseWheelTimeoutId);
            this.mouseWheelTimeoutId= setTimeout(() => {
                this.mouseWheelDevicePt= undefined;
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
            useDevPt= cc.pointInPlot(this.mouseWheelDevicePt);
        }

        const usingMouseWheel= Math.abs(nativeEv.wheelDeltaY)%120 === 0;

        zoomFromWheelOrTrackpad(usingMouseWheel,
            {plotId:plotView?.plotId, userZoomType, devicePt: useDevPt ? this.mouseWheelDevicePt : undefined,
                upDownPercent:Math.abs(nativeEv.wheelDeltaY)%120===0?1:  isHiPS(plot)? .2 : .5 } );

    }

    eventCB(plotId,mouseState,screenPt,screenX,screenY,nativeEv) {
        const {drawLayersAry,plotView}= this.props;
        const shiftDown= nativeEv.shiftKey;
        const mouseStatePayload= makeMouseStatePayload(plotId,mouseState,screenPt,screenX,screenY, {shiftDown});
        const list= drawLayersAry.filter( (dl) => dl.visiblePlotIdAry.includes(plotView.plotId) &&
                                                get(dl,['mouseEventMap',mouseState.key],false) );


        if (this.mouseOwnerLayerId && draggingOrReleasing(mouseState)) { // use layer from the mouseDown
            const dl= getLayer(drawLayersAry,this.mouseOwnerLayerId);
            fireMouseEvent(dl,mouseState,mouseStatePayload);
        }
        else if (isWheel(mouseState)) {
            if (!isActivePlotView(visRoot(),plotId) && getAppOptions()?.wheelScrollRequiresImageActive) return;
            this.handleScrollWheelEvent(plotView,mouseState,screenPt,nativeEv);
            return;
        }
        else {
            const ownerCandidate= !shiftDown && findMouseOwner(list,primePlot(plotView),screenPt);         // see if anyone can own that mouse
            this.mouseOwnerLayerId = DOWN.is(mouseState) && ownerCandidate ? ownerCandidate.drawLayerId : null;   // can only happen on mouseDown
            if (this.mouseOwnerLayerId) {
                if (DOWN.is(mouseState)) dispatchChangeActivePlotView(plotId);
                const dl= getLayer(drawLayersAry,this.mouseOwnerLayerId);
                fireMouseEvent(dl,mouseState,mouseStatePayload);
            }
            else { // fire to all non-exclusive layers, scroll, and determine cursor
                list.filter( (dl) => !get(dl, 'exclusiveDef.exclusiveOnDown',false))
                    .forEach( (dl) => fireMouseEvent(dl,mouseState,mouseStatePayload) );
                this.scroll(plotView,mouseState,screenX,screenY,mouseState===DOWN ? screenPt : null );
                let cursor = DEFAULT_CURSOR;
                const cursorCandidate= ownerCandidate || findMouseOwner(drawLayersAry,primePlot(plotView),screenPt);
                if (MOVE.is(mouseState) && get(cursorCandidate, 'getCursor') ) {
                    cursor = cursorCandidate.getCursor(plotView, screenPt) || DEFAULT_CURSOR;
                }
                if (cursor !== this.state.cursor) this.setState({cursor});
            }
        }
        fireMouseCtxChange(mouseStatePayload);  // this for anyone listening directly to the mouse

    }

    scroll(plotView,mouseState,screenX,screenY, mouseDownScreenPt) {
         if (!screenX && !screenY) return;
         const {plotId}= plotView;

         switch (mouseState) {
             case DOWN :
                 dispatchChangeActivePlotView(plotId);
                 const {scrollX, scrollY}= plotView;
                 this.plotDrag= plotMove(screenX,screenY,makeScreenPt(scrollX,scrollY), mouseDownScreenPt, plotView);
                 break;
             case DRAG :
                 if (this.plotDrag) {
                     scrollMoveThrottled(this.plotDrag, plotId, screenX,screenY);
                 }
                 break;
             case UP :
                 this.plotDrag= null;
                 break;
         }
     }



    renderInside() {
        const {plotView,drawLayersAry}= this.props;
        const plot= primePlot(plotView);
        const {plotId, viewDim:{width,height}}= plotView;

        const rootStyle= {left:0, top:0, bottom:0, right:0,
                           position:'absolute', marginRight: 'auto', marginLeft: 0 };

        const drawLayersIdAry= drawLayersAry ? drawLayersAry.map( (dl) => dl.drawLayerId) : undefined;
        const {cursor}= this.state;
        return (
            <div className='plot-view-scroll-view-window' style={rootStyle}>
                <div className='plot-view-master-panel'
                     style={{width,height, left:0,right:0,position:'absolute', cursor}}>
                    {makeTileDrawers(plotView)}
                    <DrawingLayers
                        key={'DrawingLayers:'+plotId} plot={plot} plotView={plotView}
                        drawLayersIdAry={drawLayersIdAry} />
                </div>
                <EventLayer plotId={plotId} key={'EventLayer:'+plotId}
                            transform={plotView.affTrans} eventCallback={this.eventCB}/>
            </div>
        );
    }


    render() {
        const {plotView:pv}= this.props;
        const {viewDim:{width,height}}= pv;
        let insideStuff;
        const plot= primePlot(pv);
        const plotShowing= Boolean(width && height && plot);
        let onScreen= true;
        let sizeViewable= true;
        let loadingRawData= false;

        if (plotShowing ) {
            onScreen= isImageOnScreen(pv);
            sizeViewable= isImageSizeViewable(pv);
            insideStuff= this.renderInside();
            loadingRawData= !plot?.tileData && !hasLocalStretchByteData(plot) && canLoadStretchDataDirect(plot);
        }

        return (
            <div className='web-plot-view-scr' style={rootStyle}>
                {insideStuff}
                {makeMessageArea(pv,plotShowing,onScreen,sizeViewable,loadingRawData)}
            </div>
        );
    }


}


ImageViewerLayout.propTypes= {
    plotView : PropTypes.object.isRequired,
    drawLayersAry: PropTypes.array.isRequired,
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    externalWidth: PropTypes.number.isRequired,
    externalHeight: PropTypes.number.isRequired
};


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

const scrollMoveThrottled= BrowserInfo.isFirefox() ? throttle(scrollMove,30) : throttle(scrollMove,15);


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
                                            intersects(0,0,viewDim.width, viewDim.height,pt.x,pt.y,1,1)));
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
function makeTileDrawers(pv) {


    const plot= primePlot(pv);
    const rootDrawer= (
        <ImageRender opacity={1} plot={plot} plotView={pv} key={'TileDrawer:'+pv.plotId} idx={0}/>
    );
    const drawers= pv.overlayPlotViews.filter( (opv) => opv.visible && opv.plot).map( (opv,idx) => {
        return (
            <ImageRender opacity={opv.opacity} plot={opv.plot} plotView={pv}
                         idx={idx+1}
                         tileAttributes={opv.colorAttributes}
                         shouldProcess={(im, newData, imState, nextImState) => {
                                  if (newData) return imState.color!== imState.srcImageColor;
                                  else         return imState.color!==nextImState.color;
                              }}
                         processor={makeMaskColorProcessor(opv.colorAttributes.color)}
                         key={'TileDrawer-overlay:'+opv.imageOverlayId}
            />
        );
    });
    if (pv.visible) drawers.unshift(rootDrawer);
    return drawers;
}


function makeMaskColorProcessor(colorStr) {
    return (imageData) => {
        const cAry= Color.getRGBA(colorStr);
        const {data}= imageData;
        const len= data.length;
        for(let i= 0; i<len; i+=4) {
            if (data[i+3]) {
                data[i]  = cAry[0];
                data[i+1]= cAry[1];
                data[i+2]= cAry[2];
            }
        }
        return {imageData, compressible:true};
    };
}


/**
 *
 * @param {object} props
 * @return {{prevWidth, prevHeight, prevExternalWidth, prevExternalHeight, prevPlotId: *}}
 */
function makePrevDim(props) {
    const {width,height,externalWidth,externalHeight,plotView}= props;
    return {
        prevWidth:width,
        prevHeight:height,
        prevExternalWidth:externalWidth,
        prevExternalHeight:externalHeight,
        prevPlotId : plotView.plotId
    };
}


function makeMessageArea(pv,plotShowing,onScreen, sizeViewable, loadingRawData) {
    if (pv.serverCall==='success') {
        if (loadingRawData) {
            return (
                <ImageViewerStatus message={'Loading Color Data'} working={true}
                                   maskWaitTimeMS= {500} messageWaitTimeMS={1000} useMessageAlpha={plotShowing}/>
            );
        }
        else if (!onScreen) {
            return (
                <ImageViewerStatus message={'Center Plot'} working={false} top={30}
                                   useMessageAlpha={false} buttonText='Recenter'
                                   buttonCB={() => dispatchRecenter({plotId:pv.plotId, centerOnImage:true}) } />
            );
        }
        else if (!sizeViewable) {
            return (
                <ImageViewerStatus message={'Minimum zoom'} working={false} top={30}
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
    else if (pv.serverCall==='fail') {
        const buttonCB= plotShowing ?
            () => dispatchPlotProgressUpdate(pv.plotId,'',true, pv.request.getRequestKey()) : undefined;
        return (
            <ImageViewerStatus message={pv.plottingStatusMsg || 'Image Data Plot Fail'}
                               working={false} useMessageAlpha={true} buttonText='OK' buttonCB={buttonCB} />
        );
    }
}


/**
 * Do the following:
 *    1. First look for a layers that has exclusiveDef.exclusiveOnDown as true
 *    2. if any of those has exclusiveDef.type === 'anywhere' then return the last in the list
 *    3. otherwise if any any layer has exclusiveDef.type === 'vertexOnly'  or 'vertexThenAnywhere' return the first that has
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
    const exList= dlList.filter((dl) => get(dl,'exclusiveDef.exclusiveOnDown'));
    if (isEmpty(exList) || ! screenPt) return null;

                    // Step 2
    const nowList= exList.filter((dl) => get(dl,'exclusiveDef.type','')==='anywhere');

    if (!isEmpty(nowList)) return nowList[nowList.length-1];

                      // Step 3
    const cc= CysConverter.make(plot);

    const getDist = (vertexDef) => {
        const {pointDist} = vertexDef || {};

        return isNumber(pointDist) ?  pointDist : get(pointDist, [cc.plotId], 0.0);
    };

    const getPoints = (vertexDef) => {
        const {points} = vertexDef || {};

        return isArray(points) ? points : get(points, [cc.plotId], []);
    };

    const vertexDL= exList
        .filter((dl) => {
            const exType= get(dl,'exclusiveDef.type','');
            const points= get(dl,'vertexDef.points',null);
            return (exType==='vertexOnly' || exType==='vertexThenAnywhere') && !isEmpty(points);
        })
        .find( (dl)  => {
            const {vertexDef}= dl;
            const pDist = getDist(vertexDef);

            const dist= pDist || 5;
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
    const anyWhereList= dlList.filter((dl) => get(dl,'exclusiveDef.type','')==='vertexThenAnywhere');
    return isEmpty(anyWhereList) ? null : anyWhereList[anyWhereList.length-1];

}

function fireMouseEvent(drawLayer,mouseState,mouseStatePayload) {
    const payload= Object.assign({},mouseStatePayload,{drawLayer});
    const fireObj= drawLayer.mouseEventMap[mouseState.key];
    if (isString(fireObj)) {
        flux.process({type: fireObj, payload});
    }
    else if (isFunction(fireObj)) {
        fireObj(payload);
    }
}


const getLayer= (list,drawLayerId) => list.find( (dl) => dl.drawLayerId===drawLayerId);

export const DrawingLayers= memo( ({plotView:pv, plot, drawLayersIdAry:dlIdAry}) =>{
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
    return np.plot===p.plot && isEmpty(xor(np.drawLayersIdAry,p.drawLayersIdAry)) &&
        np.plotView.scrollX===p.plotView.scrollX && np.plotView.scrollY===p.plotView.scrollY;
});

DrawingLayers.propTypes= {
    plotView: PropTypes.object.isRequired,
    plot: PropTypes.object.isRequired,
    drawLayersIdAry: PropTypes.array
};

