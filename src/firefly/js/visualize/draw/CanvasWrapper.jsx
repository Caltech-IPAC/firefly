/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {pick} from 'lodash';
import {Drawer} from './Drawer.js';
import shallowequal from 'shallowequal';
import {makeDrawingDef} from './DrawingDef.js';
import {CANVAS_DL_ID_START} from '../PlotViewUtil.js';


function updateDrawer(drawer,plot, width, height, drawLayer,force=false) {
    let data, highlightData, selectIdxs;
    if (!drawLayer) return;
    const {drawData}= drawLayer;
    if (!drawData) return;
    drawer.decimate= drawLayer.decimate;
    const plotId= plot? plot.plotId : null;
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
    drawer.setData(data,selectIdxs,plot,width,height,drawLayer.drawingDef,force);
    if (highlightData) {
        drawer.updateDataHighlightLayer(getDataForPlot(highlightData,plotId));
    }
}


/**
 *
 * @param {object} drawLayer
 * @param {object} drawer
 * @param {number} w width
 * @param {number} h height
 * @param {number} idx
 * @param {String} plotId
 * @return {Array} array of canvas elements
 */
function makeCanvasLayers(drawLayer,drawer,w,h, idx, plotId) {

    const {drawLayerId,canSelect,canHighlight, canShowSelect}= drawLayer;
    const style={width:w, height:h, left:0, right:0, position:'absolute'};
    const retAry= [];

    const mainId= plotId && `${CANVAS_DL_ID_START}${idx}.1-${plotId}`;
    retAry.push(<canvas style={style} key={drawLayerId} width={w+''} height={h+''}
                        id={mainId}
                        ref={(c) => drawer.setPrimCanvas(c,w,h)}/>);

    if (canSelect || canShowSelect) {
        const sKey= drawLayerId+'-Select';
        const sId= plotId && `${CANVAS_DL_ID_START}${idx}.2-${plotId}`;
        retAry.push(<canvas style={style} key={sKey} width={w+''} height={h+''}
                            id={sId} ref={(c) => drawer.setSelectCanvas(c,w,h)}/>);
    }
    if (canHighlight) {
        const hKey= drawLayerId+'-Highlight';
        const hId= plotId && `${CANVAS_DL_ID_START}${idx}.3-${plotId}`;
        retAry.push(<canvas style={style} key={hKey} width={w+''} height={h+''}
                            id={hId} ref={(c) => drawer.setHighlightCanvas(c,w,h)}/>);
    }
    return retAry;
}

const isVisible= (drawLayer,plotId) => drawLayer && drawLayer.visiblePlotIdAry.includes(plotId);


const getDataForPlot= (data,plotId) => {
    if (!data) return null;
    if (Array.isArray(data)) return data;
    else                     return data[plotId];
};

export function makeDummyDrawLayer(drawData) {
    return {
        drawLayerId:'no-layer-defined',
        drawingDef:makeDrawingDef(),
        isPointData: false,
        drawData
    };
}

const pickList= ['drawData','drawingDef', 'plotIdAry', 'visiblePlotIdAry'];

class CanvasWrapper extends Component {


    constructor(props) {
        super(props);
        this.drawer= null;
        this.lastDrawLayer= null;
        this.drawer= Drawer.makeDrawer();
    }

    shouldComponentUpdate(nProps) {

        const {plot,width,height}= nProps;
        const p= this.props;

        const update= (Math.floor(width)!==Math.floor(p.width) ||
                       Math.floor(height)!==Math.floor(p.height) ||
                       plot!==p.plot);

        if (!update) {
            this.updateDrawLayer(nProps);
        }
        return update;
    }

    
    updateDrawLayer(props,force=false) {
        const {plot,drawLayer,width,height,getDrawLayer}= props;
        let dl= getDrawLayer ? getDrawLayer() : drawLayer;
        if (!force &&
            Math.floor(width)===this.lastWidth && Math.floor(height)===this.lastHeight &&
            shallowequal(pick(dl,pickList),pick(this.lastDrawLayer,pickList))) {
            return;
        }
        this.lastDrawLayer= dl;
        this.lastWidth= Math.floor(width);
        this.lastHeight= Math.floor(height);
        if (Array.isArray(dl)) dl= makeDummyDrawLayer(dl);

        window.requestAnimationFrame(() => {
            if (this.drawer) updateDrawer(this.drawer,plot,width,height,dl,force);
        });
    }

    componentDidMount() {
        this.componentDidUpdate();
    }

    componentDidUpdate() {
        this.updateDrawLayer(this.props,true);
    }


    render() {
        const {plot, drawLayer,width,height, getDrawLayer, idx}= this.props;
        this.lastDrawLayer= getDrawLayer ? getDrawLayer() : drawLayer;
        let dl= this.lastDrawLayer;
        if (plot && !isVisible(dl,plot.plotId)) return false;
        if (!dl) return false;

        if (Array.isArray(dl)) dl= makeDummyDrawLayer(this.lastDrawLayer);

        const canvasLayers= makeCanvasLayers(dl, this.drawer,width,height, idx, plot && plot.plotId);

        const style= {position:'absolute',left:0,right:0,width,height};
        if (!canvasLayers.length) return false;

        return (
            <div className='canvasWrapper' style={style}>
                {canvasLayers}
            </div>
        );
    }
}

CanvasWrapper.propTypes= {
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    plot : PropTypes.object,
    drawLayer : PropTypes.object, //drawLayer or drawData is Required
    idx : PropTypes.number,
    getDrawLayer: PropTypes.func  // can be used instead of passing the data
};

export default CanvasWrapper;

