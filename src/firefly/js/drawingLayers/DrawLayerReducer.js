/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import DrawingLayer, {DataTypes} from '../visualize/draw/DrawingLayer.js';
import DrawingLayerCntlr from '../visualize/DrawingLayerCntlr.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';




export default {makeReducer};


/**
 *
 * @param getDrawDataFunc a function that will take the drawing layer and data the type.
 *
 * @param actionIdAry a list of actions that will cause this drawing layer update by calling the drawDataReducer
 * @param options currently title,helpLine,defColor- all are strings
 * @return {Function}
 */
function makeReducer(getDrawDataFunc, actionIdAry=[], options) {
    return (drawingLayer, action={}) => {
        if (!action.payload || !action.type) return drawingLayer;

        if (!drawingLayer) {
            drawingLayer= makeLayer(action.payload.drawLayerId,actionIdAry,getDrawDataFunc,options,action);
        }

        switch (action.type) {
            case DrawingLayerCntlr.CREATE_DRAWING_LAYER:
                return drawingLayer;
                break;
            case DrawingLayerCntlr.CHANGE_VISIBILITY:
                return changeVisibility(drawingLayer,action,getDrawDataFunc);
                break;
            case DrawingLayerCntlr.ATTACH_LAYER_TO_PLOT:
                return attachLayerToPlot(drawingLayer,action,getDrawDataFunc);
                break;
            case DrawingLayerCntlr.DETACH_LAYER_FROM_PLOT:
                return detachLayerFromPlot(drawingLayer,action);
                break;
            case ImagePlotCntlr.ANY_REPLOT:
                return anyReplot(drawingLayer,action,getDrawDataFunc);
                break;
            default:
                return handleOtherAction(drawingLayer,action,getDrawDataFunc);
                break;
        }

    };
}

function handleOtherAction(drawingLayer,action, drawDataReducer) {
    if (drawingLayer.actionIdAry.indexOf(action.type)>-1) {
        var d= drawDataReducer(drawingLayer,action);
        if (d.data!==drawingLayer.data ||
            d.highlightedData!==drawingLayer.highlightedData ||
            drawingLayer.selectedIdxAry!==d.selectedIdxAry) {
            return Object.assign({},drawingLayer,{drawData:d});
        }
        else {
            return drawingLayer;
        }
    }
    return drawingLayer;
}



function anyReplot(drawingLayer,action, getDrawDataFunc) {
    var {plotIdAry}= action.payload;

    if (drawingLayer.hasPerPlotData) {
        var drawData= plotIdAry.reduce( (d,id) => {
            return Object.assign({}, d, getDrawData(getDrawDataFunc,drawingLayer,action,id));
        }, Object.assign({},drawData.data));
        return Object.assign({},drawingLayer,{drawData});
    }
    return drawingLayer;


}






function attachLayerToPlot(drawingLayer,action,getDrawDataFunc) {
    var {plotId} = action.payload;
    var {plotIdAry, visiblePlotIdAry}= drawingLayer;
    if (plotIdAry.includes(plotId)) return drawingLayer;
    plotIdAry= [...plotIdAry,plotId];
    if (!visiblePlotIdAry.includes(plotId)) {
        visiblePlotIdAry= [...visiblePlotIdAry,plotId];
    }
    drawingLayer= Object.assign({}, drawingLayer, {plotIdAry,visiblePlotIdAry});

    if (drawingLayer.hasPerPlotData) {
        drawingLayer.drawData= getDrawData(getDrawDataFunc,drawingLayer, action, plotId);
    }
    return drawingLayer;
}

function detachLayerFromPlot(drawingLayer,action) {
    var {plotId} = action.payload;
    var plotIdAry= plotIdAry.filter( (id) => id!==plotId);
    var visiblePlotIdAry= visiblePlotIdAry.filter( (id) => id!==plotId);

    drawingLayer= Object.assign({}, drawingLayer, {plotIdAry, visiblePlotIdAry});
    if (drawingLayer.hasPerPlotData) {
        drawingLayer.drawData= detachPerPlotData(drawingLayer.drawData,plotId);
    }
    return drawingLayer;
}




//function attachPlot(drawData, action) {
//    var {plotId}= action.payload;
//    var drawDataAry= computeDrawingLayer(plotId);
//    return {data:Object.assign({}, drawData.data, {[plotId]:drawDataAry})};
//}

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






function changeVisibility(drawingLayer,action, getDrawDataFunc) {
    var {visible,plotId} = action.payload;
    var visiblePlotIdAry= drawingLayer.visiblePlotIdAry;

    if (visible) {
        if (visiblePlotIdAry.includes(plotId)) return drawingLayer;
        visiblePlotIdAry= [...visiblePlotIdAry,plotId];
        drawingLayer= Object.assign({}, drawingLayer, {visiblePlotIdAry});
        if (drawingLayer.hasPerPlotData) {
            drawingLayer.drawData= getDrawData(getDrawDataFunc,drawingLayer, action,plotId);
        }
        return drawingLayer;
    }
    else {
        visiblePlotIdAry= visiblePlotIdAry.filter( (id) => id!==plotId);
        return Object.assign({}, drawingLayer, {visiblePlotIdAry});
    }
}



function makeLayer(drawLayerId, actionIdAry, getDrawDataFunc, options, action) {
    var layer= DrawingLayer.makeDrawingLayer(drawLayerId);
    var drawData= getDrawData(getDrawDataFunc,layer,action);
    return Object.assign(layer, options, {actionIdAry,drawData});
}

/**
 *
 * @param {function} getDrawDataFunc
 * @param {object} drawingLayer
 * @param {object} action
 * @param {string} plotId
 * @return {{}}
 */
function getDrawData(getDrawDataFunc, drawingLayer, action, plotId= DrawingLayer.ALL_PLOTS) {
    var pId= plotId===DrawingLayer.ALL_PLOTS ? null : plotId;
    var newDD= {[DataTypes.DATA]:{},[DataTypes.HIGHLIGHT_DATA]:{}, [DataTypes.SELECTED_IDX_ARY]: null};

    newDD[DataTypes.DATA][plotId]= getDrawDataFunc(DataTypes.DATA, pId, drawingLayer, action,
        drawingLayer.drawData[DataTypes.DATA][plotId]);

    if (drawingLayer.canHighlight) {
        newDD[DataTypes.HIGHLIGHT_DATA][plotId]= getDrawDataFunc(DataTypes.HIGHLIGHT_DATA, pId, drawingLayer, action,
            drawingLayer.drawData[DataTypes.HIGHLIGHT_DATA][plotId]);
    }

    if (drawingLayer.canSelect && plotId===DrawingLayer.ALL_PLOTS) {
        newDD[DataTypes.SELECTED_IDX_ARY]= getDrawDataFunc(DataTypes.SELECTED_IDX_ARY, null, drawingLayer, action,
            drawingLayer.drawData[DataTypes.SELECTED_IDX_ARY]);
    }
    var retval= {};
    retval[DataTypes.DATA]= Object.assign({},drawingLayer.drawData[DataTypes.DATA],newDD[DataTypes.DATA]);
    retval[DataTypes.HIGHLIGHT_DATA]= Object.assign({},drawingLayer.drawData[DataTypes.HIGHLIGHT_DATA],newDD[DataTypes.HIGHLIGHT_DATA]);
    retval[DataTypes.SELECTED_IDX_ARY]= newDD[DataTypes.SELECTED_IDX_ARY];
    return retval;

}



//function makeDataMaker(getDataFunc, getHighlightFunc, getSelectedIdxFunc) {
//    return {
//        getData:getDataFunc,
//        getHighlightData:getHighlightFunc,
//        getSelectedIdx:getSelectedIdxFunc
//    };
//}
