/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {difference,xor,isEmpty,get} from 'lodash';
import {TileDrawer} from './TileDrawer.jsx';
import {EventLayer} from './EventLayer.jsx';
import {ImageViewerStatus} from './ImageViewerStatus.jsx';
import {makeScreenPt} from '../Point.js';
import {logError} from '../../util/WebUtil.js';
import {flux} from '../../Firefly.js';
import {DrawerComponent}  from '../draw/DrawerComponent.jsx';
import {primePlot} from '../PlotViewUtil.js';
import {
    dispatchPlotProgressUpdate,
    dispatchZoom,
    dispatchProcessScroll,
    dispatchChangeActivePlotView,
    dispatchUpdateViewSize} from '../ImagePlotCntlr.js';
import {fireMouseCtxChange, makeMouseStatePayload, MouseState} from '../VisMouseSync.js';



export class ImageViewerLayout extends Component {

    constructor(props) {
        super(props);
        this.plotDrag= null;
        this.eventCB= this.eventCB.bind(this);
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentDidMount() {
        var {width,height}= this.props;
        var {plotView:pv}= this.props;
        this.previousDim= makePrevDim(this.props);
        dispatchUpdateViewSize(pv.plotId,width,height);
        if (pv && pv.plotViewCtx.zoomLockingEnabled) {
            dispatchZoom(pv.plotId,pv.plotViewCtx.zoomLockingType,true,true, true);
        }
    }

    componentDidUpdate() {
        var {plotView:pv,width,height,externalWidth,externalHeight, plotView:pv}= this.props;
        var {prevWidth,prevHeight, prevExternalWidth, prevExternalHeight, prevPlotId}= this.previousDim;
        if (prevWidth!==width || prevHeight!==height || prevPlotId!==pv.plotId) {
            dispatchUpdateViewSize(pv.plotId,width,height);
            //console.log('dispatchUpdateViewSize');
            if (pv && pv.plotViewCtx.zoomLockingEnabled) {
                if (prevExternalWidth!==externalWidth || prevExternalHeight!==externalHeight) {
                    //console.log('dispatchZoom');
                    dispatchZoom(pv.plotId,pv.plotViewCtx.zoomLockingType,true,true,true);
                }
            }
            this.previousDim= makePrevDim(this.props);
        }
    }

    eventCB(plotId,mouseState,screenPt,screenX,screenY) {
        var {drawLayersAry}= this.props;
        var mouseStatePayload= makeMouseStatePayload(plotId,mouseState,screenPt,screenX,screenY);
        var list= drawLayersAry.filter( (dl) => get(dl,['mouseEventMap',mouseState.key],false));

        var exclusive= list.find((dl) => get(dl,['mouseEventMap',mouseState.key,'exclusive']));
        if (exclusive) {
            fireMouseEvent(exclusive,mouseState,mouseStatePayload);
        }
        else {
            list.forEach( (dl) => fireMouseEvent(dl,mouseState,mouseStatePayload) );
            this.scroll(plotId,mouseState,screenX,screenY);
        }
        fireMouseCtxChange(mouseStatePayload);
    }

    scroll(plotId,mouseState,screenX,screenY) {
        if (!screenX && !screenY) return;

        switch (mouseState) {
            case MouseState.DOWN :
                dispatchChangeActivePlotView(plotId);
                var {scrollX, scrollY}= this.props.plotView;
                this.plotDrag= plotMover(screenX,screenY,scrollX,scrollY);
                break;
            case MouseState.DRAG :
                if (this.plotDrag) {
                    const newScrollPt= this.plotDrag(screenX,screenY);
                    dispatchProcessScroll(plotId,newScrollPt);
                }
                break;
            case MouseState.UP :
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


        var cursor= drawLayersAry.map( (dl) => dl.cursor).find( (c) => (c && c.length));
        if (!cursor || !cursor.length) cursor= 'crosshair';
        return (
            <div className='plot-view-scroll-view-window' style={rootStyle}>
                <div className='plot-view-master-panel'
                     style={{width:viewPortWidth,height:viewPortHeight,
                                 left:0,right:0,position:'absolute', cursor}}>
                    <TileDrawer
                        key={'TileDrawer:'+plotId}
                                x={scrollX} y={scrollY}
                                width={viewPortWidth} height={viewPortHeight}
                                plot={plot} />
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

function fireMouseEvent(drawLayer,mouseState,mouseStatePayload) {
    var payload= Object.assign({},mouseStatePayload,{drawLayer});
    var fireObj= drawLayer.mouseEventMap[mouseState.key];
    if (typeof fireObj === 'string') {
        flux.process({type: fireObj, payload});
    }
    else if (typeof fireObj === 'function') {
        fireObj(payload);
    }
    else if (typeof fireObj === 'object' && fireObj.func) {
        fireObj.func(payload);
    }
    else if (typeof fireObj === 'object' && fireObj.actionType) {
        flux.process({type: fireObj.actionType, payload});
    }
    else {
        logError(new Error('could not find a way to process MouseState'+mouseState.key),fireObj);
    }
}

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


