/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import TileDrawer from './TileDrawer.jsx';
import EventLayer from './EventLayer.jsx';
import ImagePlotCntlr from '../ImagePlotCntlr.js';
import VisMouseCntlr  from '../VisMouseCntlr.js';
import PlotViewUtil  from '../PlotViewUtil.js';
import PlotGroup  from '../PlotGroup.js';
import DrawerComponent  from '../draw/DrawerComponent.jsx';
import {makeScreenPt} from '../Point.js';




var ImageViewerView= React.createClass(
{


    mixins : [PureRenderMixin],

    storeListenerRemove : null,


    propTypes: {
        plotView : React.PropTypes.object.isRequired,
        drawLayersAry: React.PropTypes.array.isRequired
    },

    plotDrag: null,


    getDefaultProps: function () {
        return { };
    },


    getInitialState() {
        return {width:0,height:0};
    },


    componentWillUnmount() {
    },


    componentDidMount() {
        var e= ReactDOM.findDOMNode(this);
        //this.setState({width:e.offsetWidth, height:e.offsetHeight});
        var {plotId}= this.props.plotView;
        ImagePlotCntlr.dispatchUpdateViewSize(plotId,e.offsetWidth,e.offsetHeight);
    },


    eventCB(plotId,mouseState,spt,screenX,screenY) {
        if (screenX && screenY) {
            switch (mouseState) {
                case VisMouseCntlr.MouseState.DOWN :
                    ImagePlotCntlr.dispatchChangeActivePlotView(plotId);
                    var {scrollX, scrollY}= this.props.plotView;
                    this.plotDrag= plotMover(screenX,screenY,scrollX,scrollY);
                    //console.log(`begin drag ${screenX},${screenY}`);
                    break;
                case VisMouseCntlr.MouseState.DRAG :
                    if (this.plotDrag) {
                        let newScrollPt= this.plotDrag(screenX,screenY);
                        ImagePlotCntlr.dispatchProcessScroll(plotId,newScrollPt);
                    }
                    break;
                case VisMouseCntlr.MouseState.UP :
                    this.plotDrag= null;
                    //console.log(`end drag ${screenX},${screenY}`);
                    break;
                case VisMouseCntlr.MouseState.CLICK:
                    ImagePlotCntlr.dispatchChangeActivePlotView(plotId);
                    break;
            }
        }
    },


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
        return (
                <div className='plot-view-scr-view-window'
                     style={rootStyle}>
                    <div className='plot-view-master-panel'
                         style={{width:viewPortWidth,height:viewPortHeight,
                                 left:0,right:0,position:'absolute', cursor:'crosshair'}}>
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
                        <EventLayer plotId={plotId} width={viewPortWidth} height={viewPortHeight}
                                    eventCallback={this.eventCB}/>
                    </div>
                </div>
        );
    },




    render() {
        var {primaryPlot,viewDim:{width,height},plotId}= this.props.plotView;
        //var {width,height}= this.state;
        var insideStuff;

        if (width && height && primaryPlot) {
            insideStuff= this.renderInside();
        }
        //var style= {width:'calc(100% - 4px)',
        //            height:'calc(100% - 5px)',
        //            left: 2,
        //            top: 3,
        //            position: 'relative',
        //            overflow:'hidden'};
        var style= {width:'100%',
            height:'100%',
            position: 'relative',
            overflow:'hidden'};

        //var border= {
        //    borderStyle: 'ridge',
        //    borderWidth: '3px 2px 2px 2px',
        //    borderColor: getBorderColor(plotId)
        //};
        //Object.assign(style,border);

        return (
            <div className='web-plot-view-scr' style={style}>
                {insideStuff}
            </div>
        );
    }


});


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

function getBorderColor(plotId) {
    if (!plotId) return 'rgba(0,0,0,.4)';
    if (PlotViewUtil.isActivePlotView(plotId)) return 'orange';

    var group= PlotGroup.getPlotGroupById(plotId);

    if (group && group.lockRelated) return '#005da4';
    else return 'rgba(0,0,0,.4)';



}


/**
 *
 * @param plotView
 * @param {[]} dlAry - array of drawingLayers
 * @return {*}
 */
function makeDrawingAry(plotView,dlAry) {
    if (!dlAry) return [];
    return dlAry.map( (dl) => <DrawerComponent plotView={plotView} drawingLayer={dl} key={dl.drawLayerId}/> );
}



export default ImageViewerView;
