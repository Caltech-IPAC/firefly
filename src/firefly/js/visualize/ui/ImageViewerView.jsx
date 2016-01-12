/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import sCompare from 'react-addons-shallow-compare';
import TileDrawer from './TileDrawer.jsx';
import EventLayer from './EventLayer.jsx';
import ImagePlotCntlr, {dispatchProcessScroll,dispatchChangeActivePlotView}
                               from '../ImagePlotCntlr.js';
import {MouseState, dispatchMouseStateChange, makeMouseStatePayload}  from '../VisMouseCntlr.js';
import {PlotViewDrawer}  from '../draw/DrawerComponent.jsx';
import {makeScreenPt} from '../Point.js';
import {flux} from '../../Firefly.js';
import {logError} from '../../util/WebUtil.js';




class ImageViewerView extends React.Component {


    constructor(props) {
        super(props);
        this.plotDrag= null;
        this.state= {width:0,height:0};
    }



    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentDidMount() {
        var e= ReactDOM.findDOMNode(this);
        var {plotId}= this.props.plotView;
        ImagePlotCntlr.dispatchUpdateViewSize(plotId,e.offsetWidth,e.offsetHeight);
    }

    eventCB(plotId,mouseState,screenPt,screenX,screenY) {
        var {drawLayersAry}= this.props;
        var mouseStatePayload= makeMouseStatePayload(plotId,mouseState,screenPt,screenX,screenY);
        var list= drawLayersAry.filter( (dl) => dl.mouseEventMap.hasOwnProperty(mouseState.key));

        var exclusive= list.find((dl) => dl.mouseEventMap[mouseState.key].exclusive);
        if (exclusive) {
            fireMouseEvent(exclusive,mouseState,mouseStatePayload);
        }
        else {
            list.forEach( (dl) => fireMouseEvent(dl,mouseState,mouseStatePayload) );
            this.scroll(plotId,mouseState,screenX,screenY);
        }
        dispatchMouseStateChange(mouseStatePayload);
    }

    scroll(plotId,mouseState,screenX,screenY) {
        if (screenX && screenY) {
            switch (mouseState) {
                case MouseState.DOWN :
                    ImagePlotCntlr.dispatchChangeActivePlotView(plotId);
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
                    //console.log(`end drag ${screenX},${screenY}`);
                    break;
                case MouseState.CLICK:
                    dispatchChangeActivePlotView(plotId);
                    break;
            }
        }
    }


    renderInside() {
        var {plotView,drawLayersAry}= this.props;
        var {primaryPlot:plot,plotId,scrollX,scrollY}= plotView;
        var {dim:{width:viewPortWidth,height:viewPortHeight},x:vpX,y:vpY}= plot.viewPort;
        var {width:sw,height:sh}= plot.screenSize;
        var scrollViewWidth= Math.min(viewPortWidth,sw);
        var scrollViewHeight= Math.min(viewPortHeight,sh);
        var left= vpX-scrollX;
        var top= vpY-scrollY;
        var rootStyle= {left, top,
                        position:'relative',
                        width:scrollViewWidth,
                        height:scrollViewHeight
        };

        var drawingAry= makeDrawingAry(plotView, drawLayersAry);
        var cursor= drawLayersAry.map( (dl) => dl.cursor).find( (c) => (c && c.length));
        if (!cursor || !cursor.length) cursor= 'crosshair';
        return (
                <div className='plot-view-scr-view-window'
                     style={rootStyle}>
                    <div className='plot-view-master-panel'
                         style={{width:viewPortWidth,height:viewPortHeight,
                                 left:0,right:0,position:'absolute', cursor}}>
                        <TileDrawer x={scrollX} y={scrollY} width={viewPortWidth} height={viewPortHeight}
                                    tileData={plot.serverImages}
                                    tileZoomFactor={plot.plotState.getZoomLevel()}
                                    zoomFactor={plot.zoomFactor}
                                    plot={plot}
                                    opacity={plot.percentOpaque}
                        />
                        <div className='drawingArea'
                             style={{width:viewPortWidth, height:viewPortHeight,
                                     left:0, right:0, position:'absolute'}} >
                            {drawingAry}
                        </div>
                        <EventLayer plotId={plotId} viewPort={plot.viewPort}
                                    width={viewPortWidth} height={viewPortHeight}
                                    eventCallback={(plotId,mouseState,screenPt,screenX,screenY) =>
                                                    this.eventCB(plotId,mouseState,screenPt,screenX,screenY)}/>
                    </div>
                </div>
        );
    }




    render() {
        var {primaryPlot,viewDim:{width,height},plotId}= this.props.plotView;
        var insideStuff;

        if (width && height && primaryPlot) {
            insideStuff= this.renderInside();
        }
        var style= {width:'100%',
                    height:'100%',
                    position: 'relative',
                    overflow:'hidden'};
        return (
            <div className='web-plot-view-scr' style={style}>
                {insideStuff}
            </div>
        );
    }


}


ImageViewerView.propTypes= {
    plotView : React.PropTypes.object.isRequired,
    drawLayersAry: React.PropTypes.array.isRequired
};




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


/**
 *
 * @param plotView
 * @param {[]} dlAry - array of drawLayers
 * @return {*}
 */
function makeDrawingAry(plotView,dlAry) {
    if (!dlAry) return [];
    var {width,height}= plotView.primaryPlot.viewPort.dim;
    return dlAry.map( (dl) => <PlotViewDrawer plotView={plotView} drawLayer={dl}
                                              width={width} height={height}
                                              key={dl.drawLayerId}/> );
}



export default ImageViewerView;
