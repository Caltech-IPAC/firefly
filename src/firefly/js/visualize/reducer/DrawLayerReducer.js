/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get,difference,union} from 'lodash';
import {DataTypes} from '../draw/DrawLayer.js';
import DrawLayerCntlr from '../DrawLayerCntlr.js';
import ImagePlotCntlr from '../ImagePlotCntlr.js';




export default {makeReducer};


/**
 *
 * @param {DrawLayerFactory} factory
 * @return {function} reducer
 */
function makeReducer(factory) {
    return (drawLayer= {}, action={}) => {
        if (!action.payload || !action.type) return drawLayer;

        switch (action.type) {
            case DrawLayerCntlr.CHANGE_VISIBILITY:
                return changeVisibility(drawLayer,action,factory);
                break;
            case DrawLayerCntlr.CHANGE_DRAWING_DEF:
                return changeDrawingDef(drawLayer,action,factory);
                break;
            case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
                return attachLayerToPlot(drawLayer,action,factory);
                break;
            case DrawLayerCntlr.DETACH_LAYER_FROM_PLOT:
                return detachLayerFromPlot(drawLayer,action,factory);
                break;
            case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            case DrawLayerCntlr.FORCE_DRAW_LAYER_UPDATE:
            case ImagePlotCntlr.ANY_REPLOT:
                return updateFromLayer(drawLayer,action,factory);
                break;
            //LZ
            case DrawLayerCntlr.FORCE_DRAW_GRID_LAYER_UPDATE:
                return updateGridLayer(drawLayer,action,factory);
                break;
            default:
                return handleOtherAction(drawLayer,action,factory);
                break;
        }
    };
}






function handleOtherAction(drawLayer,action,factory) {


    if (!drawLayer.actionTypeAry.includes(action.type)) return drawLayer;

    var changes= factory.getLayerChanges(drawLayer,action);
    var newDl= (changes && Object.keys(changes).length) ? Object.assign({},drawLayer,changes): drawLayer;
    var drawData;

    if (newDl.hasPerPlotData) {     //todo- perPlotData does not have the same optimization as normal, look into this
        newDl.plotIdAry.forEach( (id) => {
            drawData= getDrawData(factory,newDl, action, id);
        });
    }
    else {
        drawData= getDrawData(factory,newDl, action);
    }

    if (newDl !==drawLayer){
        newDl.drawData= drawData;
        return newDl;
    }
    else if (drawData!==newDl.drawData) {
        return Object.assign({},newDl,{drawData});
    }
    else {
        return drawLayer;
    }
}

function updateFromLayer(drawLayer,action,factory) {
    var {plotIdAry}= action.payload;
    drawLayer= Object.assign({}, drawLayer, factory.getLayerChanges(drawLayer,action));
    if (drawLayer.hasPerPlotData) {
        plotIdAry.forEach( (id) =>
            drawLayer.drawData= getDrawData(factory,drawLayer, action, id));
    }
    else {
        drawLayer.drawData= getDrawData(factory,drawLayer, action);
    }
    return drawLayer;
}

function updateGridLayer(drawLayer,action,factory) {
    var {plotIdAry}= action.payload;
    drawLayer= Object.assign({}, drawLayer, factory.getLayerChanges(drawLayer,action));
    if (drawLayer.hasPerPlotData) {
        plotIdAry.forEach( (id) =>
            drawLayer.drawData= getDrawData(factory,drawLayer, action, id));
    }
    else {
        drawLayer.drawData= getDrawData(factory,drawLayer, action);
    }
    return drawLayer;
}



/**
 *
 * @param drawLayer
 * @param action
 * @param factory
 * @return {*}
 */
function attachLayerToPlot(drawLayer,action,factory) {
    var {plotIdAry:inputPlotIdAry} = action.payload;
    var {plotIdAry:dlPlotIdAry, visiblePlotIdAry}= drawLayer;
    //if (dlPlotIdAry.includes(plotId)) return drawLayer;

    if (!difference(inputPlotIdAry,dlPlotIdAry).length) return drawLayer;



    dlPlotIdAry= union(dlPlotIdAry,inputPlotIdAry);
    var addAry= inputPlotIdAry.filter( (plotId) => !visiblePlotIdAry.includes(plotId));
    visiblePlotIdAry= [...visiblePlotIdAry,...addAry];
    drawLayer= Object.assign({}, drawLayer,
                             {plotIdAry:dlPlotIdAry,visiblePlotIdAry});

    drawLayer= Object.assign(drawLayer, factory.getLayerChanges(drawLayer,action));
    if (drawLayer.hasPerPlotData) {
        drawLayer.plotIdAry.forEach( (id) =>
            drawLayer.drawData= getDrawData(factory,drawLayer, action, id));
    }
    else {
        drawLayer.drawData= getDrawData(factory,drawLayer, action);
    }


    return drawLayer;
}

function detachLayerFromPlot(drawLayer,action,factory) {
    var {plotIdAry:inputPlotIdAry} = action.payload;
    var {plotIdAry:dlPlotIdAry, visiblePlotIdAry}= drawLayer;
    var plotIdAry= dlPlotIdAry.filter( (id) => !inputPlotIdAry.includes(id));
    visiblePlotIdAry= visiblePlotIdAry.filter( (id) => !inputPlotIdAry.includes(id));

    drawLayer= Object.assign({}, drawLayer, factory.getLayerChanges(drawLayer,action), {plotIdAry, visiblePlotIdAry});
    if (drawLayer.hasPerPlotData) {
        inputPlotIdAry.forEach( (plotId) =>
                      drawLayer.drawData= detachPerPlotData(drawLayer.drawData,plotId));
    }
    return drawLayer;
}





function detachPerPlotData(drawData, plotId) {
    var highlightData= null;
    var selectIdxs= drawData.selectIdxs;

    var data= Object.keys(drawData.data).reduce( (d,key) => {
        if (key!==plotId) d[key]= drawData.data[key];
        return d;
    },{});

    if (drawData.highlightData) {
        highlightData= Object.keys(drawData.highlightData).reduce( (d,key) => {
            if (key!==plotId) d[key]= drawData.highlightData[key];
            return d;
        },{});
    }

    return {data,highlightData,selectIdxs};
}






function changeVisibility(drawLayer,action,factory) {
    var {visible,plotIdAry} = action.payload;
    var visiblePlotIdAry;

    if (visible) {
        visiblePlotIdAry= union(drawLayer.visiblePlotIdAry,plotIdAry);
        drawLayer= Object.assign({}, drawLayer, {visiblePlotIdAry},
                                         factory.getLayerChanges(drawLayer,action));
        if (drawLayer.hasPerPlotData) {
            plotIdAry.forEach( (id) =>
                drawLayer.drawData= getDrawData(factory,drawLayer, action, id));
        }
        return drawLayer;
    }
    else {
        visiblePlotIdAry= difference(drawLayer.visiblePlotIdAry,plotIdAry);
        return Object.assign({}, drawLayer, {visiblePlotIdAry});
    }
}



function changeDrawingDef(drawLayer,action,factory) {
    var {drawingDef} = action.payload;
    return Object.assign({}, drawLayer, {drawingDef}, factory.getLayerChanges(drawLayer,action));
}

const DATA= DataTypes.DATA;
const HIGHLIGHT_DATA= DataTypes.HIGHLIGHT_DATA;
const SELECTED_IDXS= DataTypes.SELECTED_IDXS;

/**
 *
 * @param {DrawLayerFactory} factory
 * @param {object} drawLayer
 * @param {object} action
 * @param {string} plotId
 * @return {{}}
 */
function getDrawData(factory, drawLayer, action, plotId= null) {
    if (!factory.hasGetDrawData(drawLayer)) return drawLayer.drawData;
    var {drawData}= drawLayer;
    if (!drawData) drawData= {};
    var pId= plotId;
    var newDD= Object.assign({},drawData);

    if (!newDD.data) newDD.data={};
    
    if (plotId) {
        newDD.data[plotId]= factory.getDrawData(DATA, pId, drawLayer, action, get(drawData,`data.${plotId}`));
    }
    else {
        newDD.data= factory.getDrawData(DATA, pId, drawLayer, action, drawData.data);
    }


    if (drawLayer.canHighlight) {
        if (!newDD.highlightData) newDD.highlightData={};
        if (plotId) {
            newDD.highlightData[plotId]= factory.getDrawData(HIGHLIGHT_DATA, pId, drawLayer, action,
                                             get(drawData,`highlightData.${plotId}`));
        }
        else {
            newDD.highlightData= factory.getDrawData(HIGHLIGHT_DATA, pId, drawLayer, action,drawData.highlightData);
        }
    }

    if (drawLayer.canSelect) {
        newDD.selectIdxs= factory.getDrawData(SELECTED_IDXS, null, drawLayer, action, drawData.selectIdxs);
    }


    var retval;
    if (plotId) {
        retval= {
            data:             Object.assign({},drawData.data,newDD.data),
            highlightData:   Object.assign({},drawData.highlightData,newDD.highlightData),
            selectIdxs: newDD.selectIdxs
        };
    }
    else {
        retval= newDD;
    }

    // check for differences
    if (retval.data!==drawData.data ||
        retval.highlightData!==drawData.highlightData ||
        retval.selectIdxs!==drawData.selectIdxs) {
        return retval;
    }
    else {
        return drawLayer.drawData;
    }
}

