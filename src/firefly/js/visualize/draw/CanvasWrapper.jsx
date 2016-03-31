/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import DrawLayer from './DrawLayer.js';
import sCompare from 'react-addons-shallow-compare';
import Drawer from './Drawer.js';
import {makeDrawingDef} from './DrawingDef.js';




function updateDrawer(drawer,plot, width, height, drawLayer) {
    var data, highlightData, selectIdxs;
    var {drawData}= drawLayer;
    var plotId= plot? plot.plotId : null;
    if (Array.isArray(drawData)) {
        data= drawData;
        highlightData= null;
        selectIdxs= null;
    }
    else {
        data= getDataForPlot(drawData.data,plotId);
        highlightData= drawData.highlightData;
        selectIdxs= drawData.selectIdxs;
    }
    drawer.isPointData= drawLayer.isPointData;
    drawer.setData(data,selectIdxs,plot,width,height,drawLayer.drawingDef);
    if (highlightData) {
        drawer.updateDataHighlightLayer(getDataForPlot(highlightData,plotId),width,height);
    }
}


/**
 *
 * @param {object} drawLayer
 * @param {object} drawer
 * @param {number} w width
 * @param {number} h height
 * @return {Array}
 */
function makeCanvasLayers(drawLayer,drawer,w,h) {

    var {drawLayerId,canSelect,canHighlight}= drawLayer;
    var style={width:w, height:h, left:0, right:0, position:'absolute'};
    var retAry= [];

    retAry.push(<canvas style={style} key={drawLayerId} width={w+''} height={h+''}
                        ref={(c) => drawer.setPrimCanvas(c,w,h)}/>);

    if (canSelect) {
        var sId= drawLayerId+'-Select';
        retAry.push(<canvas style={style} key={sId} width={w+''} height={h+''}
                            ref={(c) => drawer.setSelectCanvas(c,w,h)}/>);
    }
    if (canHighlight) {
        var hId= drawLayerId+'-Highlight';
        retAry.push(<canvas style={style} key={hId} width={w+''} height={h+''}
                            ref={(c) => drawer.setHighlightCanvas(c,w,h)}/>);
    }
    return retAry;
}

const isVisible= (drawLayer,plotId) => drawLayer.visiblePlotIdAry.includes(plotId);


const getDataForPlot= (data,plotId) => {
    if (!data) return null;
    if (Array.isArray(data)) return data;
    else                     return data[plotId];
};

function makeDummyDrawLayer(drawData) {
    return {
        drawLayerId:'no-layer-defined',
        drawingDef:makeDrawingDef(),
        isPointData: false,
        drawData
    };
}


class CanvasWrapper extends React.Component {


    constructor(props) {
        super(props);
        this.drawer= null;
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillMount() {
        var {textUpdateCallback}= this.props;
        this.drawer= Drawer.makeDrawer();
        this.drawer.textUpdateCallback= textUpdateCallback;
    }

    componentDidMount() {
        this.componentDidUpdate();
    }

    componentDidUpdate() {
        var {plot,drawData,drawLayer,width,height}= this.props;
        if (!drawLayer) drawLayer= makeDummyDrawLayer(drawData);
        if (this.drawer) updateDrawer(this.drawer,plot,width,height,drawLayer);

    }


    render() {
        var {plot, drawData,drawLayer,width,height}= this.props;
        if (plot && !isVisible(drawLayer,plot.plotId)) return false;
        if (!drawLayer) drawLayer= makeDummyDrawLayer(drawData);
        var canvasLayers= makeCanvasLayers(drawLayer, this.drawer,width,height);

        var style= {position:'absolute',left:0,right:0,width,height};
        if (!canvasLayers.length) return false;

        return (
            <div className='canvasWrapper' style={style}>
                {canvasLayers}
            </div>
        );
    }
}

CanvasWrapper.propTypes= {
    textUpdateCallback : React.PropTypes.func.isRequired,
    width: React.PropTypes.number.isRequired,
    height: React.PropTypes.number.isRequired,
    plot : React.PropTypes.object,
    drawLayer : React.PropTypes.object, //drawLayer or drawData is Required
    drawData : React.PropTypes.array // only used it drawLayer is not defined
};

export default CanvasWrapper;

