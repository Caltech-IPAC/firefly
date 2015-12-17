/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import DrawLayer from './DrawLayer.js';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import Drawer from './Drawer.js';




function updateDrawer(drawer,plotView, drawLayer) {
    var {drawData:{data,highlightData,selectIdxAry} }= drawLayer;
    var {dim:{width,height}}= plotView.primaryPlot.viewPort;
    drawer.isPointData= drawLayer.isPointData;
    drawer.setData(getDataForPlot(data,plotView.plotId),plotView.primaryPlot,width,height);
    drawer.updateDataHighlightLayer(getDataForPlot(highlightData,plotView.plotId));
}


/**
 *
 * @param drawLayer
 * @param {object} drawer
 * @param {number} w width
 * @param {number} h height
 * @return {Array}
 */
function makeCanvasLayers(drawLayer,drawer,w,h) {

    var style={width:w, height:w, left:0, right:0, position:'absolute'};
    var retAry= [];
    var {drawLayerId}= drawLayer;


    retAry.push(<canvas style={style} key={drawLayerId} ref={(c) => drawer.setPrimCanvas(c,w,h)}/>);

    if (drawLayer.canSelect) {
        var sId= selectId(drawLayerId);
        retAry.push(<canvas style={style} key={sId} ref={(c) => drawer.setHighlightCanvas(c,w,h)}/>);
    }
    if (drawLayer.canHighlight) {
        var hId= highlightId(drawLayerId);
        retAry.push(<canvas style={style} key={hId} ref={(c) => drawer.setSelectCanvas(c,w,h)}/>);
    }
    return retAry;
}



const isVisible= (drawLayer,plotId) => drawLayer.visiblePlotIdAry.includes(plotId);
const selectId = (drawLayerId) => drawLayerId+'Select';
const highlightId = (drawLayerId) => drawLayerId+'Highlight';


const getDataForPlot= (data,plotId) => {
    if (!data) return null;
    if (Array.isArray(data)) return data;
    else                     return data[plotId] || data[DrawLayer.ALL_PLOTS];
};


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
        drawLayer : React.PropTypes.object.isRequired
    },


    getDefaultProps: function () {
        return { };
    },


    getInitialState() {
        return {width:0,height:0};
    },

    componentWillMount() {
        var {drawLayer}= this.props;
        this.drawer= Drawer.makeDrawer(drawLayer.drawingDef);
    },

    componentDidMount() {
        this.componentDidUpdate();
    },



    componentDidUpdate() {
        var {plotView,drawLayer}= this.props;
        var {drawLayerId}= drawLayer;

        if (this.drawer) {
            updateDrawer(this.drawer,plotView,drawLayer);
        }
    },



    render() {
        var {plotView, drawLayer}= this.props;
        if (!plotView || !drawLayer) return false;
        if (!isVisible(drawLayer,plotView.plotId)) return false;
        var {primaryPlot}= plotView;
        if (!primaryPlot) return false;

        var {dim:{width,height}}= primaryPlot.viewPort;
        var canvasLayers= makeCanvasLayers(drawLayer,this.drawer,width,height);

        var style= {position:'absolute',left:0,right:0,width,height};
        if (!canvasLayers.length) return false;


        return (
            <div className='drawLayer' style={style}>
                <div className='h1'/>
                {canvasLayers}
                <div className='h2'/>
            </div>
        );
    },





});


export default DrawerComponent;

