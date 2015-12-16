/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import DrawingLayer, {DataTypes} from '../visualize/draw/DrawingLayer.js';
import DrawingLayerCntlr from '../visualize/DrawingLayerCntlr.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import _ from 'lodash';




export default {makeReducer};


/**
 *
 * @param {object} drawingLayerInitValue
 * @param {function} getDrawDataFunc a function that will take the drawing layer and data the type.
 *                   signature: getDrawDataFunc(dataType, plotId, drawingLayer, action, lastDataRet)
 *                   where dataType is a string with constants in DrawingLayers.DataType.
 *                   lastDataRet is the data that was return in the last call. If nothing has change
 *                   then then lastDataRet can be the return value.
 * @param {function} [getLayerChanges] get the changes to incorporate into the drawing layer object
 *                    A function that returns an object literal the that has the field changes.
 *                    <br>signature: getLayerChanges(drawingLayer,action)
 *                    it may return null or empty object if there are no changes
 * @callbackparam {object} drawing layer
 * @callbackparam {action} the action
 *
 * @return {Function}
 */
function makeReducer(drawingLayerInitValue, getDrawDataFunc, getLayerChanges=()=>{}) {
    return (drawingLayer, action={}) => {
        if (!action.payload || !action.type) return drawingLayer;

        if (!drawingLayer) {
            var drawData= getDrawData(getDrawDataFunc,drawingLayerInitValue,action);
            drawingLayer= Object.assign({}, drawingLayerInitValue, {drawData});
        }

        switch (action.type) {
            case DrawingLayerCntlr.CREATE_DRAWING_LAYER:
                return drawingLayer;
                break;
            case DrawingLayerCntlr.CHANGE_VISIBILITY:
                return changeVisibility(drawingLayer,action,getDrawDataFunc,getLayerChanges);
                break;
            case DrawingLayerCntlr.ATTACH_LAYER_TO_PLOT:
                return attachLayerToPlot(drawingLayer,action,getDrawDataFunc,getLayerChanges);
                break;
            case DrawingLayerCntlr.DETACH_LAYER_FROM_PLOT:
                return detachLayerFromPlot(drawingLayer,action);
                break;
            case ImagePlotCntlr.ANY_REPLOT:
                return anyReplot(drawingLayer,action,getDrawDataFunc, getLayerChanges);
                break;
            default:
                return handleOtherAction(drawingLayer,action,getDrawDataFunc, getLayerChanges);
                break;
        }

    };
}






function handleOtherAction(drawingLayer,action, getDrawDataFunc, getLayerChanges) {
    if (drawingLayer.actionTypeAry.includes(action.type)) {
        var changes= getLayerChanges(drawingLayer,action);
        var newDl= (changes && Object.keys(changes).length) ? Object.assign({},drawingLayer,changes): drawingLayer;

        if (newDl.hasPerPlotData) {     //todo- perPlotData does not have the same optimization as normal, look into this
            newDl.plotIdAry.forEach( (id) =>
                newDl.drawData= getDrawData(getDrawDataFunc,newDl, action, id));
        }
        else {
            var d= getDrawData(getDrawDataFunc,newDl, action);
            if (newDl !==drawingLayer) {
                return Object.assign(newDl,{drawData:d}); // already created a new object, just assign
            }
            else if (d.data!==newDl.data ||
                     d.highlightedData!==newDl.highlightedData ||
                     d.selectedIdxAry!==newDl.selectedIdxAry) {
                return Object.assign({},newDl,{drawData:d});
            }
            else {
                return drawingLayer;
            }
        }
    }
    return drawingLayer;
}



function anyReplot(drawingLayer,action, getDrawDataFunc,getLayerChanges) {
    var {plotIdAry}= action.payload;
    drawingLayer= Object.assign({}, drawingLayer, getLayerChanges(drawingLayer,action));
    if (drawingLayer.hasPerPlotData) {
        plotIdAry.forEach( (id) =>
                      drawingLayer.drawData= getDrawData(getDrawDataFunc,drawingLayer, action, id));
    }
    else {
        drawingLayer.drawData= getDrawData(getDrawDataFunc,drawingLayer, action);
    }
    return drawingLayer;
}


/**
 *
 * @param drawingLayer
 * @param action
 * @param getDrawDataFunc
 * @param getLayerChanges
 * @return {*}
 */
function attachLayerToPlot(drawingLayer,action,getDrawDataFunc,getLayerChanges) {
    var {plotIdAry:inputPlotIdAry} = action.payload;
    var {plotIdAry:dlPlotIdAry, visiblePlotIdAry}= drawingLayer;
    //if (dlPlotIdAry.includes(plotId)) return drawingLayer;

    if (!_.difference(inputPlotIdAry,dlPlotIdAry).length) return drawingLayer;



    dlPlotIdAry= _.union(dlPlotIdAry,inputPlotIdAry);
    var addAry= inputPlotIdAry.filter( (plotId) => !visiblePlotIdAry.includes(plotId));
    visiblePlotIdAry= [...visiblePlotIdAry,...addAry];
    drawingLayer= Object.assign({}, drawingLayer,
                             {plotIdAry:dlPlotIdAry,visiblePlotIdAry});

    drawingLayer= Object.assign(drawingLayer, getLayerChanges(drawingLayer,action));
    if (drawingLayer.hasPerPlotData) {
        drawingLayer.plotIdAry.forEach( (id) =>
            drawingLayer.drawData= getDrawData(getDrawDataFunc,drawingLayer, action, id));
    }


    return drawingLayer;
}

function detachLayerFromPlot(drawingLayer,action) {
    var {plotIdAry:inputPlotIdAry} = action.payload;
    var {plotIdAry:dlPlotIdAry, visiblePlotIdAry}= drawingLayer;
    var plotIdAry= dlPlotIdAry.filter( (id) => !inputPlotIdAry.includes(id));
    visiblePlotIdAry= visiblePlotIdAry.filter( (id) => !inputPlotIdAry.includes(id));

    drawingLayer= Object.assign({}, drawingLayer, {plotIdAry, visiblePlotIdAry});
    if (drawingLayer.hasPerPlotData) {
        inputPlotIdAry.forEach( (plotId) =>
                      drawingLayer.drawData= detachPerPlotData(drawingLayer.drawData,plotId));
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






function changeVisibility(drawingLayer,action, getDrawDataFunc, getLayerChanges ) {
    var {visible,plotId} = action.payload;
    var visiblePlotIdAry= drawingLayer.visiblePlotIdAry;

    if (visible) {
        if (visiblePlotIdAry.includes(plotId)) return drawingLayer;
        visiblePlotIdAry= [...visiblePlotIdAry,plotId];
        drawingLayer= Object.assign({}, drawingLayer, {visiblePlotIdAry},
                                         getLayerChanges(drawingLayer,action));
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



//function makeLayer(drawLayerId, actionTypeAry, getDrawDataFunc, options, action) {
//}

/**
 *
 * @param {function} getDrawDataFunc
 * @param {object} drawingLayer
 * @param {object} action
 * @param {string} plotId
 * @return {{}}
 */
function getDrawData(getDrawDataFunc, drawingLayer, action, plotId= DrawingLayer.ALL_PLOTS) {
    if (!getDrawDataFunc) return drawingLayer.drawData;
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
