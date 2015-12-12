/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import DrawingLayer from './DrawingLayer.js';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import Drawer from './Drawer.js';




function updateDrawer(drawer,plotView, drawingLayer) {
    var {drawData:{data,highlightData,selectIdxAry} }= drawingLayer;
    var {dim:{width,height}}= plotView.primaryPlot.viewPort;
    drawer.isPointData= drawingLayer.isPointData;
    drawer.setData(getDataForPlot(data,plotView.plotId),plotView.primaryPlot,width,height);
    drawer.updateDataHighlightLayer(getDataForPlot(highlightData,plotView.plotId));
}


/**
 *
 * @param drawingLayer
 * @param {object} drawer
 * @param {number} w width
 * @param {number} h height
 * @return {Array}
 */
function makeCanvasLayers(drawingLayer,drawer,w,h) {

    var style={width:w, height:w, left:0, right:0, position:'absolute'};
    var retAry= [];
    var {drawLayerId}= drawingLayer;


    retAry.push(<canvas style={style} key={drawLayerId} ref={(c) => drawer.setPrimCanvas(c,w,h)}/>);

    if (drawingLayer.canSelect) {
        var sId= selectId(drawLayerId);
        retAry.push(<canvas style={style} key={sId} ref={(c) => drawer.setHighlightCanvas(c,w,h)}/>);
    }
    if (drawingLayer.canHighlight) {
        var hId= highlightId(drawLayerId);
        retAry.push(<canvas style={style} key={hId} ref={(c) => drawer.setSelectCanvas(c,w,h)}/>);
    }
    return retAry;
}



const isVisible= (drawingLayer,plotId) => drawingLayer.visiblePlotIdAry.includes(plotId);
const getDataForPlot= (data,plotId) => data ? (data[plotId] || data[DrawingLayer.ALL_PLOTS]) : null;
const selectId = (drawingLayerId) => drawingLayerId+'Select';
const highlightId = (drawingLayerId) => drawingLayerId+'Highlight';


var DrawerComponent= React.createClass(
{


    mixins : [PureRenderMixin],

    storeListenerRemove : null,
    drawer: null,







    //todo: props should set to redraw on replot,primaryChange, or viewport change
    //todo: might not need plotview
    // note that plotView.primaryPlot cannot be null
    propTypes: {
        plotView : React.PropTypes.object.isRequired,
        drawingLayer : React.PropTypes.object.isRequired
    },


    getDefaultProps: function () {
        return { };
    },


    getInitialState() {
        return {width:0,height:0};
    },

    componentWillMount() {
        var {drawingLayer}= this.props;
        this.drawer= Drawer.makeDrawer(drawingLayer.drawingDef);
    },

    componentDidMount() {
        this.componentDidUpdate();
    },



    componentDidUpdate() {
        var {plotView,drawingLayer}= this.props;
        var {drawLayerId}= drawingLayer;

        if (this.drawer) {
            updateDrawer(this.drawer,plotView,drawingLayer);
        }
    },



    render() {
        var {plotView, drawingLayer}= this.props;
        if (!plotView || !drawingLayer) return false;
        if (!isVisible(drawingLayer,plotView.plotId)) return false;
        var {primaryPlot}= plotView;
        if (!primaryPlot) return false;

        var {dim:{width,height}}= primaryPlot.viewPort;
        var canvasLayers= makeCanvasLayers(drawingLayer,this.drawer,width,height);

        var style= {position:'relative',left:0,right:0,width,height};
        if (!canvasLayers.length) return false;


        return (
            <div className='drawingLayer' style={style}>
                <div className='h1'/>
                {canvasLayers}
                <div className='h2'/>
            </div>
        );
    },





});


export default DrawerComponent;

