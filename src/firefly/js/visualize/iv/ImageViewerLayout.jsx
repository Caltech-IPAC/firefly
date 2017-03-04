/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {xor,isEmpty,get, isString, isFunction} from 'lodash';
import {TileDrawer} from './TileDrawer.jsx';
import {EventLayer} from './EventLayer.jsx';
import {ImageViewerStatus} from './ImageViewerStatus.jsx';
import {makeScreenPt} from '../Point.js';
import {flux} from '../../Firefly.js';
import {DrawerComponent}  from '../draw/DrawerComponent.jsx';
import {CysConverter, CCUtil}  from '../CsysConverter.js';
import {UserZoomTypes}  from '../ZoomUtil.js';
import {primePlot, plotInActiveGroup} from '../PlotViewUtil.js';
import {isImageViewerSingleLayout, getMultiViewRoot} from '../MultiViewCntlr.js';
import {contains} from '../VisUtil.js';
import {PlotAttribute} from '../WebPlot.js';
import {
    visRoot,
    WcsMatchType,
    ActionScope,
    dispatchPlotProgressUpdate,
    dispatchZoom,
    dispatchRecenter,
    dispatchProcessScroll,
    dispatchChangeActivePlotView,
    dispatchUpdateViewSize} from '../ImagePlotCntlr.js';
import {fireMouseCtxChange, makeMouseStatePayload, MouseState} from '../VisMouseSync.js';

const DEFAULT_CURSOR= 'crosshair';

const {MOVE,DOWN,DRAG,UP, DRAG_COMPONENT, EXIT, ENTER}= MouseState;

const draggingOrReleasing = (ms) => ms==DRAG || ms===DRAG_COMPONENT || ms===UP || ms===EXIT || ms===ENTER;



function updateZoom(pv, paging) {
    if (!primePlot(pv)) return;
    var doZoom= false;
    var actionScope= ActionScope.GROUP;
    const vr= visRoot();
    if (!paging && vr.wcsMatchType && pv.plotId!==vr.mpwWcsPrimId) {
        doZoom= false;
    }
    else if (isImageViewerSingleLayout(getMultiViewRoot(), vr, pv.plotId)) {
        doZoom= true;
        actionScope= ActionScope.SINGLE;
    }
    else {  // case: not expanded or expand as grid
            // if plot are in the group that is active then only the prime plot will do the zooming for all the group.
            // otherwise if we are not in the active group each plot will do a zoom
        const inActive= plotInActiveGroup(vr,pv.plotId);
        const isActive= vr.activePlotId===pv.plotId;
        doZoom= (isActive || !inActive);
        actionScope= isActive ? ActionScope.GROUP : ActionScope.SINGLE;
    }

    if (doZoom) {
        dispatchZoom({
            plotId: pv.plotId,
            userZoomType: (paging && vr.wcsMatchType) ? UserZoomTypes.WCS_MATCH_PREV : pv.plotViewCtx.zoomLockingType,
            zoomLockingEnabled: true,
            forceDelay: true,
            actionScope
        });
    }
}



export class ImageViewerLayout extends Component {

    constructor(props) {
        super(props);
        this.plotDrag= null;
        this.mouseOwnerLayerId= null;
        this.eventCB= this.eventCB.bind(this);
        this.state= {cursor:DEFAULT_CURSOR};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentDidMount() {
        var {width,height}= this.props;
        var {plotView:pv}= this.props;
        this.previousDim= makePrevDim(this.props);
        dispatchUpdateViewSize(pv.plotId,width,height);
        if (pv.plotViewCtx.zoomLockingEnabled && primePlot(pv)) {
            const paging= isImageViewerSingleLayout(getMultiViewRoot(), visRoot(), pv.plotId);
            updateZoom(pv,paging);
        }

        const vr= visRoot();

        if (vr.wcsMatchType===WcsMatchType.Target && vr.activePlotId===pv.plotId && primePlot(vr)) {
            const plot= primePlot(vr);
            const ft=  plot.attributes[PlotAttribute.FIXED_TARGET];
            if (ft) dispatchRecenter({plotId:plot.plotId, centerPt:ft});
        }
    }

    componentWillReceiveProps(nextProps) {
        const {width,height}= nextProps;
        const {viewDim}= nextProps.plotView;
        if (width!==viewDim.width && height!==viewDim.height) {
            dispatchUpdateViewSize(nextProps.plotView.plotId,width,height);
        }
    }

    componentDidUpdate() {
        const {width,height,externalWidth,externalHeight, plotView:pv}= this.props;
        const {prevWidth,prevHeight, prevExternalWidth, prevExternalHeight, prevPlotId}= this.previousDim;
        if (!pv) return;

        if (prevWidth!==width || prevHeight!==height || prevPlotId!==pv.plotId) {
            dispatchUpdateViewSize(pv.plotId,width,height);
            if (pv.plotViewCtx.zoomLockingEnabled && primePlot(pv)) {
                if (prevExternalWidth!==externalWidth || prevExternalHeight!==externalHeight) {
                    updateZoom(pv,false);
                }
            }
            this.previousDim= makePrevDim(this.props);
        }
    }

    eventCB(plotId,mouseState,screenPt,screenX,screenY) {
        var {drawLayersAry,plotView}= this.props;
        var mouseStatePayload= makeMouseStatePayload(plotId,mouseState,screenPt,screenX,screenY);
        var list= drawLayersAry.filter( (dl) => dl.visiblePlotIdAry.includes(plotView.plotId) &&
                                                get(dl,['mouseEventMap',mouseState.key],false) );

        if (this.mouseOwnerLayerId && draggingOrReleasing(mouseState)) { // use layer from the mouseDown
            const dl= getLayer(drawLayersAry,this.mouseOwnerLayerId);
            fireMouseEvent(dl,mouseState,mouseStatePayload);
        }
        else {
            const ownerCandidate= findMouseOwner(list,primePlot(plotView),screenPt);         // see if anyone can own that mouse
            this.mouseOwnerLayerId = DOWN.is(mouseState) && ownerCandidate ? ownerCandidate.drawLayerId : null;   // can only happen on mouseDown
            if (this.mouseOwnerLayerId) {
                if (DOWN.is(mouseState)) dispatchChangeActivePlotView(plotId);
                const dl= getLayer(drawLayersAry,this.mouseOwnerLayerId);
                fireMouseEvent(dl,mouseState,mouseStatePayload);
            }
            else { // fire to all non-exclusive layers, scroll, and determine cursor
                list.filter( (dl) => !get(dl, 'exclusiveDef.exclusiveOnDown',false))
                    .forEach( (dl) => fireMouseEvent(dl,mouseState,mouseStatePayload) );
                this.scroll(plotId,mouseState,screenX,screenY);
                var cursor = DEFAULT_CURSOR;
                const cursorCandidate= ownerCandidate || findMouseOwner(drawLayersAry,primePlot(plotView),screenPt);
                if (MOVE.is(mouseState) && get(cursorCandidate, 'getCursor') ) {
                    cursor = cursorCandidate.getCursor(plotView, screenPt) || DEFAULT_CURSOR;
                }
                if (cursor !== this.state.cursor) this.setState({cursor});
            }
        }
        fireMouseCtxChange(mouseStatePayload);  // this for anyone listening directly to the mouse
    }

    scroll(plotId,mouseState,screenX,screenY) {
        if (!screenX && !screenY) return;

        switch (mouseState) {
            case DOWN :
                dispatchChangeActivePlotView(plotId);
                var {scrollX, scrollY}= this.props.plotView;
                this.plotDrag= plotMover(screenX,screenY,scrollX,scrollY);
                break;
            case DRAG :
                if (this.plotDrag) {
                    const newScrollPt= this.plotDrag(screenX,screenY);
                    dispatchProcessScroll({plotId,scrollPt:newScrollPt});
                }
                break;
            case UP :
                this.plotDrag= null;
                break;
        }
    }



    renderInside() {
        var {plotView,drawLayersAry}= this.props;
        const plot= primePlot(plotView);
        const {plotId}= plotView;
        var {width:viewPortWidth,height:viewPortHeight}= plot.viewPort.dim;
        const {left,top,scrollViewWidth, scrollViewHeight, scrollX,scrollY, viewDim}=  getPositionInfo(plotView);


        var rootStyle= {left, top,
                        position:'relative',
                        width:scrollViewWidth,
                        height:scrollViewHeight,
                        marginRight: 'auto',
                        marginLeft: 0
        };

        const drawLayersIdAry= drawLayersAry ? drawLayersAry.map( (dl) => dl.drawLayerId) : null;


        // var cursor= drawLayersAry.map( (dl) => dl.cursor).find( (c) => (c && c.length));
        const {cursor}= this.state;
        return (
            <div className='plot-view-scroll-view-window' style={rootStyle}>
                <div className='plot-view-master-panel'
                     style={{width:viewPortWidth,height:viewPortHeight,
                                 left:0,right:0,position:'absolute', cursor}}>
                    {makeTileDrawers(plotView,
                                     Math.max(viewPortWidth,viewDim.width),
                                     Math.max(viewPortHeight,viewDim.height),
                                     scrollX,scrollY)}
                    <DrawingLayers
                        key={'DrawingLayers:'+plotId}
                        plot={plot} drawLayersIdAry={drawLayersIdAry} />
                    <EventLayer plotId={plotId} 
                                key={'EventLayer:'+plotId}
                                viewPort={plot.viewPort}
                                width={viewPortWidth} height={viewPortHeight}
                                eventCallback={this.eventCB}/>
                </div>
            </div>
        );
    }


    render() {
        var {plotView:pv}= this.props;
        var {viewDim:{width,height}}= pv;
        var insideStuff;
        var plotShowing= Boolean(width && height && primePlot(this.props.plotView));
        var onScreen= true;

        if (plotShowing ) {
            onScreen= isImageOnScreen(this.props.plotView);
            insideStuff= this.renderInside();

        }

        var style= {
            position:'absolute',
            left : 0,
            right : 0,
            top : 0,
            bottom : 0,
            overflow:'hidden'};
        return (
            <div className='web-plot-view-scr' style={style}>
                {insideStuff}
                {makeMessageArea(pv,plotShowing,onScreen)}
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


function isImageOnScreen(plotView) {

    const {left,top,scrollViewWidth, scrollViewHeight, viewDim}=  getPositionInfo(plotView);
    const visible= (left+scrollViewWidth>=0 && left<=viewDim.width && top+scrollViewHeight>=0 && top<=viewDim.height);
    return visible;
}


function getPositionInfo(plotView) {
    var plot= primePlot(plotView);
    var {dim:{width:viewPortWidth,height:viewPortHeight},x:vpX,y:vpY}= plot.viewPort;
    var {width:sw,height:sh}= plot.screenSize;
    var scrollViewWidth= Math.min(viewPortWidth,sw);
    var scrollViewHeight= Math.min(viewPortHeight,sh);
    var {scrollX,scrollY, viewDim}= plotView;
    var left= vpX-scrollX;
    var top= vpY-scrollY;

    return {left,top,scrollViewWidth, scrollViewHeight, scrollX,scrollY, viewDim};
}


// eslint-disable-next-line valid-jsdoc
/**
 *
 * @param pv
 * @param viewPortWidth
 * @param viewPortHeight
 * @param scrollX
 * @param scrollY
 * @return {Array}
 */
function makeTileDrawers(pv,viewPortWidth, viewPortHeight, scrollX, scrollY ) {


    var plot= primePlot(pv);
    const rootDrawer= (
        <TileDrawer
            opacity={1}
            key={'TileDrawer:'+pv.plotId}
            x={scrollX} y={scrollY}
            width={viewPortWidth} height={viewPortHeight}
            plot={plot}
            rootPlot={plot}
        />
    );
    const drawers= pv.overlayPlotViews.filter( (opv) => opv.visible && opv.plot).map( (opv) => {
        return (
            <TileDrawer
                opacity={opv.opacity}
                key={'TileDrawer-overlay:'+opv.imageOverlayId}
                x={scrollX} y={scrollY}
                width={viewPortWidth} height={viewPortHeight}
                plot={opv.plot}
                rootPlot={plot}
            />
        );
    });
    drawers.unshift(rootDrawer);
    return drawers;
}

/**
 *
 * @param {object} props
 * @return {{prevWidth, prevHeight, prevExternalWidth, prevExternalHeight, prevPlotId: *}}
 */
function makePrevDim(props) {
    var {width,height,externalWidth,externalHeight,plotView}= props;
    return {
        prevWidth:width,
        prevHeight:height,
        prevExternalWidth:externalWidth,
        prevExternalHeight:externalHeight,
        prevPlotId : plotView.plotId
    };
}


function plotMover(screenX,screenY, originalScrollX, originalScrollY) {

    var originalMouseX = screenX;
    var originalMouseY = screenY;

    return (screenX, screenY) => {
        var xdiff= screenX- originalMouseX;
        var ydiff= screenY- originalMouseY;
        var newX= originalScrollX -xdiff;
        var newY= originalScrollY -ydiff;

        // if (newX<0) newX= 0; //todo: reenable
        // if (newY<0) newY= 0; //todo: reenable

        return makeScreenPt(newX, newY);
    };
}

function makeMessageArea(pv,plotShowing,onScreen) {
    if (pv.serverCall==='success') {
        if (onScreen) {
            return false;
        }
        else {
            return (
                <ImageViewerStatus message={'Center Plot'} working={false}
                                   useMessageAlpha={false}
                                   useButton={true}
                                   buttonText='Recenter'
                                   buttonCB={() => dispatchRecenter({plotId:pv.plotId}) }

                />
            );
        }
    }

    if (pv.serverCall==='working') {
        return (
            <ImageViewerStatus message={pv.plottingStatus} working={true}
                               maskWaitTimeMS= {500} messageWaitTimeMS={1000}
                               useMessageAlpha={plotShowing}/>
            );

    }
    else if (pv.plottingStatus) {
        return (
            <ImageViewerStatus message={pv.plottingStatus} working={false}
                               useMessageAlpha={true} useButton={plotShowing}
                               buttonText='OK'
                               buttonCB={() => dispatchPlotProgressUpdate(pv.plotId,'',true, pv.request.getRequestKey())}
                               />
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
    var vertexDL= exList
        .filter((dl) => {
            const exType= get(dl,'exclusiveDef.type','');
            const points= get(dl,'vertexDef.points',null);
            return (exType==='vertexOnly' || exType==='vertexThenAnywhere') && !isEmpty(points);
        })
        .find( (dl)  => {
            const {vertexDef}= dl;
            const dist= vertexDef.pointDist || 5;
            const x= screenPt.x- dist;
            const y= screenPt.y- dist;
            const w= dist*2;
            const h= dist*2;
            return vertexDef.points.find( (pt) => {
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
    var payload= Object.assign({},mouseStatePayload,{drawLayer});
    var fireObj= drawLayer.mouseEventMap[mouseState.key];
    if (isString(fireObj)) {
        flux.process({type: fireObj, payload});
    }
    else if (isFunction(fireObj)) {
        fireObj(payload);
    }
}


const getLayer= (list,drawLayerId) => list.find( (dl) => dl.drawLayerId===drawLayerId);

// ------------ React component

/**
 *
 */
class DrawingLayers extends Component {

    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np) {
        const p= this.props;
        return np.plot!==p.plot || !isEmpty(xor(np.drawLayersIdAry,p.drawLayersIdAry));
    }
    
    render() {
        const {plot,drawLayersIdAry:dlIdAry}= this.props;
        var drawingAry= null;
        var {width,height}= plot.viewPort.dim;
        if (dlIdAry) {
            drawingAry= dlIdAry.map( (dlId) => <DrawerComponent plot={plot} drawLayerId={dlId}
                                                                width={width} height={height}
                                                                key={dlId}/> );
        }
        return (
            <div className='drawingArea'
                 style={{width, height, left:0, right:0, position:'absolute'}} >
                {drawingAry}
            </div>
        );
        
    }
}

DrawingLayers.propTypes= {
    plot: PropTypes.object.isRequired,
    drawLayersIdAry: PropTypes.array.isRequired
};


