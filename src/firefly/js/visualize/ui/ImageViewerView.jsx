/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import TileDrawer from './TileDrawer.jsx';
import EventLayer from './EventLayer.jsx';
import ImagePlotCntlr from '../ImagePlotCntlr.js';
import VisMouseCntlr  from '../VisMouseCntlr.js';
import {makeScreenPt} from '../Point.js';





var ImageViewerView= React.createClass(
{


    mixins : [React.addons.PureRenderMixin],

    storeListenerRemove : null,


    propTypes: {
        plotView : React.PropTypes.object.isRequired
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
        var e= React.findDOMNode(this);
        //this.setState({width:e.offsetWidth, height:e.offsetHeight});
        var {plotId}= this.props.plotView;
        ImagePlotCntlr.dispatchUpdateViewSize(plotId,e.offsetWidth,e.offsetHeight);
    },


    eventCB(plotId,mouseState,spt,screenX,screenY) {
        if (screenX && screenY) {
            switch (mouseState) {
                case VisMouseCntlr.MouseState.DOWN :
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
            }
        }
    },


    renderInside() {
        var {primaryPlot:plot,plotId,scrollX,scrollY}= this.props.plotView;
        var {dim:{width:viewPortWidth,height:viewPortHeight},x:vpX,y:vpY}= plot.viewPort;
        var {width:sw,height:sh}= plot.screenSize;
        var scrollViewWidth= Math.min(viewPortWidth,sw);
        var scrollViewHeight= Math.min(viewPortHeight,sh);
        var left= vpX-scrollX;
        var top= vpY-scrollY;

        return (
                <div className='plot-view-scr-view-window'
                     style={{left, top, width:scrollViewWidth+'px',height:scrollViewHeight+'px',position:'relative'}}>
                    <div className='plot-view-master-panel'
                         style={{width:viewPortWidth,height:viewPortHeight, left:0,right:0,position:'absolute', cursor:'crosshair'}}>
                        <TileDrawer x={scrollX} y={scrollY} width={viewPortWidth} height={viewPortHeight}
                                    tileData={plot.serverImages}
                                    tileZoomFactor={plot.plotState.getZoomLevel()}
                                    zoomFactor={plot.zoomFactor}
                                    plot={plot}
                                    opacity={plot.percentOpaque}
                        />
                        <div className='drawingArea'
                             style={{width:viewPortWidth, height:viewPortHeight, left:0, right:0, position:'absolute'}} >
                            <div className='drawingLayer'></div>
                            <div className='drawingLayer'></div>
                            <div className='drawingLayer'></div>
                            <div className='drawingLayer'></div>
                        </div>
                        <EventLayer plotId={plotId} width={viewPortWidth} height={viewPortHeight}
                                    eventCallback={this.eventCB.bind(this)}/>
                    </div>
                </div>
        );
    },




    render() {
        var {primaryPlot,viewDim:{width,height}}= this.props.plotView;
        //var {width,height}= this.state;
        var insideStuff;

        if (width && height && primaryPlot) {
            insideStuff= this.renderInside();
        }

        return (
            <div className='web-plot-view-scr' style={{width:'100%',height:'100%', overflow:'hidden'}}>
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





export default ImageViewerView;
