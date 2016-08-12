/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {concat, xor,isEmpty,get, isString, isObject, isFunction, has, findLast} from 'lodash';
import {TileDrawer} from './TileDrawer.jsx';
import {EventLayer} from './EventLayer.jsx';
import {ImageViewerStatus} from './ImageViewerStatus.jsx';
import {makeScreenPt} from '../Point.js';
import {logError} from '../../util/WebUtil.js';
import {flux} from '../../Firefly.js';
import {DrawerComponent}  from '../draw/DrawerComponent.jsx';
import {CysConverter}  from '../CsysConverter.js';
import {primePlot} from '../PlotViewUtil.js';
import {contains} from '../VisUtil.js';
import {
    dispatchPlotProgressUpdate,
    dispatchZoom,
    dispatchProcessScroll,
    dispatchChangeActivePlotView,
    dispatchUpdateViewSize} from '../ImagePlotCntlr.js';
import {fireMouseCtxChange, makeMouseStatePayload, MouseState} from '../VisMouseSync.js';

const DEFAULT_CURSOR= 'crosshair';

const {MOVE,DOWN,DRAG,UP, DRAG_COMPONENT, EXIT, ENTER}= MouseState;

const draggingOrReleasing = (ms) => ms==DRAG || ms===DRAG_COMPONENT || ms===UP || ms===EXIT || ms===ENTER;

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
        if (pv && pv.plotViewCtx.zoomLockingEnabled && primePlot(pv)) {
            // dispatchZoom(pv.plotId,pv.plotViewCtx.zoomLockingType,true,true, true);
            dispatchZoom({
                plotId:pv.plotId,
                userZoomType:pv.plotViewCtx.zoomLockingType,
                zoomLockingEnabled:true, 
                forceDelay:true
            });
        }
    }

    componentDidUpdate() {
        var {width,height,externalWidth,externalHeight, plotView:pv}= this.props;
        var {prevWidth,prevHeight, prevExternalWidth, prevExternalHeight, prevPlotId}= this.previousDim;
        if (prevWidth!==width || prevHeight!==height || prevPlotId!==pv.plotId) {
            dispatchUpdateViewSize(pv.plotId,width,height);
            //console.log('dispatchUpdateViewSize');
            if (pv && pv.plotViewCtx.zoomLockingEnabled && primePlot(pv)) {
                if (prevExternalWidth!==externalWidth || prevExternalHeight!==externalHeight) {
                    //console.log('dispatchZoom');
                    dispatchZoom({
                        plotId:pv.plotId,
                        userZoomType:pv.plotViewCtx.zoomLockingType,
                        zoomLockingEnabled:true,
                        forceDelay:true
                    });
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
        var {plotId,scrollX,scrollY}= plotView;
        var plot= primePlot(plotView);
        var {dim:{width:viewPortWidth,height:viewPortHeight},x:vpX,y:vpY}= plot.viewPort;
        var {width:sw,height:sh}= plot.screenSize;
        var scrollViewWidth= Math.min(viewPortWidth,sw);
        var scrollViewHeight= Math.min(viewPortHeight,sh);
        var left= vpX-scrollX;
        var top= vpY-scrollY;
        var rootStyle= {left, top,
                        position:'relative',
                        width:scrollViewWidth,
                        height:scrollViewHeight,
                        marginLeft: 'auto',
                        marginRight: 'auto'

        };
        
        const drawLayersIdAry= drawLayersAry ? drawLayersAry.map( (dl) => dl.drawLayerId) : null;


        // var cursor= drawLayersAry.map( (dl) => dl.cursor).find( (c) => (c && c.length));
        const {cursor}= this.state;
        return (
            <div className='plot-view-scroll-view-window' style={rootStyle}>
                <div className='plot-view-master-panel'
                     style={{width:viewPortWidth,height:viewPortHeight,
                                 left:0,right:0,position:'absolute', cursor}}>
                    {makeTileDrawers(plotView,viewPortWidth,viewPortHeight,scrollX,scrollY)}
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

        if (plotShowing) {
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
                {makeMessageArea(pv,plotShowing)}
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
            plot={plot} />

        );
    const drawers= pv.overlayPlotViews.filter( (opv) => opv.visible && opv.plot).map( (opv) => {
        return (
            <TileDrawer
                opacity={opv.opacity}
                key={'TileDrawer-overlay:'+opv.imageOverlayId}
                x={scrollX} y={scrollY}
                width={viewPortWidth} height={viewPortHeight}
                plot={opv.plot} />
        );
    });
    drawers.unshift(rootDrawer);
    return drawers;
}


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

        if (newX<0) newX= 0;
        if (newY<0) newY= 0;

        return makeScreenPt(newX, newY);
    };
}

function makeMessageArea(pv,plotShowing) {
    if (pv.serverCall==='success') return false;

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
                               useMessageAlpha={true} canClear={plotShowing}
                               clearCB={() => dispatchPlotProgressUpdate(pv.plotId,'',true)}
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


