/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {DataTypes} from '../draw/DrawLayer.js';
import DrawLayerCntlr from '../DrawLayerCntlr.js';
import ImagePlotCntlr from '../ImagePlotCntlr.js';
import union from 'lodash/union';
import difference from 'lodash/difference';




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
            default:
                return handleOtherAction(drawLayer,action,factory);
                break;
        }
    };
}






function handleOtherAction(drawLayer,action,factory) {
    if (drawLayer.actionTypeAry.includes(action.type)) {
        var changes= factory.getLayerChanges(drawLayer,action);
        var newDl= (changes && Object.keys(changes).length) ? Object.assign({},drawLayer,changes): drawLayer;

        if (newDl.hasPerPlotData) {     //todo- perPlotData does not have the same optimization as normal, look into this
            newDl.plotIdAry.forEach( (id) =>
                newDl.drawData= getDrawData(factory,newDl, action, id));
        }
        else {
            var d= getDrawData(factory,newDl, action);
            if (newDl !==drawLayer) {
                return Object.assign(newDl,{drawData:d}); // already created a new object, just assign
            }
            else if (d!==newDl.drawData) {
                return Object.assign({},newDl,{drawData:d});
            }
            else {
                return drawLayer;
            }
        }
    }
    return drawLayer;
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


    return drawLayer;
}

function detachLayerFromPlot(drawLayer,action,factory) {
    var {plotIdAry:inputPlotIdAry} = action.payload;
    var {plotIdAry:dlPlotIdAry, visiblePlotIdAry}= drawLayer;
    var plotIdAry= dlPlotIdAry.filter( (id) => !inputPlotIdAry.includes(id));
    visiblePlotIdAry= visiblePlotIdAry.filter( (id) => !inputPlotIdAry.includes(id));

    drawLayer= Object.assign(drawLayer, factory.getLayerChanges(drawLayer,action), {plotIdAry, visiblePlotIdAry});
    if (drawLayer.hasPerPlotData) {
        inputPlotIdAry.forEach( (plotId) =>
                      drawLayer.drawData= detachPerPlotData(drawLayer.drawData,plotId));
    }
    return drawLayer;
}





function detachPerPlotData(drawData, plotId) {
    var highlightData= null;
    var selectIdxAry= drawData.selectIdxAry;

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

    return {data,highlightData,selectIdxAry};
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
const SELECTED_IDX_ARY= DataTypes.SELECTED_IDX_ARY;

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
    var pId= plotId;
    //var newDD= {[DATA]:{},[HIGHLIGHT_DATA]:{}, [SELECTED_IDX_ARY]: null};
    var newDD= Object.assign({},drawData);

    newDD[DATA][plotId]= factory.getDrawData(DATA, pId, drawLayer, action,
        drawData[DATA][plotId]);

    if (drawLayer.canHighlight) {
        newDD[HIGHLIGHT_DATA][plotId]= factory.getDrawData(HIGHLIGHT_DATA, pId, drawLayer, action,
                                          drawData[HIGHLIGHT_DATA][plotId]);
    }

    if (drawLayer.canSelect) {
        newDD[SELECTED_IDX_ARY]= factory.getDrawData(SELECTED_IDX_ARY, null, drawLayer, action,
                                                 drawData[SELECTED_IDX_ARY]);
    }
    var retval= {
        [DATA]:             Object.assign({},drawData[DATA],newDD[DATA]),
        [HIGHLIGHT_DATA]:   Object.assign({},drawData[HIGHLIGHT_DATA],newDD[HIGHLIGHT_DATA]),
        [SELECTED_IDX_ARY]: newDD[SELECTED_IDX_ARY]
    };

    // check for differences
    if (retval[DataTypes.DATA]!==drawData[DataTypes.DATA] ||
        retval[DataTypes.HIGHLIGHT_DATA]!==drawData[DataTypes.HIGHLIGHT_DATA] ||
        retval[DataTypes.SELECTED_IDX_ARY]!==drawData[DataTypes.SELECTED_IDX_ARY]) {
        return retval;
    }
    else {
        return drawLayer.drawData;
    }
}

