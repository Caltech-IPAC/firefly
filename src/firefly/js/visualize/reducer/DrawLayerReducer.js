/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import DrawLayer, {DataTypes} from '../draw/DrawLayer.js';
import DrawLayerCntlr from '../DrawLayerCntlr.js';
import ImagePlotCntlr from '../ImagePlotCntlr.js';
import _ from 'lodash';




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
            case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
                return attachLayerToPlot(drawLayer,action,factory);
                break;
            case DrawLayerCntlr.DETACH_LAYER_FROM_PLOT:
                return detachLayerFromPlot(drawLayer,action);
                break;
            case ImagePlotCntlr.ANY_REPLOT:
                return anyReplot(drawLayer,action,factory);
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
            else if (d.data!==newDl.data ||
                     d.highlightedData!==newDl.highlightedData ||
                     d.selectedIdxAry!==newDl.selectedIdxAry) {
                return Object.assign({},newDl,{drawData:d});
            }
            else {
                return drawLayer;
            }
        }
    }
    return drawLayer;
}



function anyReplot(drawLayer,action,factory) {
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

    if (!_.difference(inputPlotIdAry,dlPlotIdAry).length) return drawLayer;



    dlPlotIdAry= _.union(dlPlotIdAry,inputPlotIdAry);
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

function detachLayerFromPlot(drawLayer,action) {
    var {plotIdAry:inputPlotIdAry} = action.payload;
    var {plotIdAry:dlPlotIdAry, visiblePlotIdAry}= drawLayer;
    var plotIdAry= dlPlotIdAry.filter( (id) => !inputPlotIdAry.includes(id));
    visiblePlotIdAry= visiblePlotIdAry.filter( (id) => !inputPlotIdAry.includes(id));

    drawLayer= Object.assign({}, drawLayer, {plotIdAry, visiblePlotIdAry});
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
    var {visible,plotId} = action.payload;
    var visiblePlotIdAry= drawLayer.visiblePlotIdAry;

    if (visible) {
        if (visiblePlotIdAry.includes(plotId)) return drawLayer;
        visiblePlotIdAry= [...visiblePlotIdAry,plotId];
        drawLayer= Object.assign({}, drawLayer, {visiblePlotIdAry},
                                         factory.getLayerChanges(drawLayer,action));
        if (drawLayer.hasPerPlotData) {
            drawLayer.drawData= getDrawData(factory,drawLayer, action,plotId);
        }
        return drawLayer;
    }
    else {
        visiblePlotIdAry= visiblePlotIdAry.filter( (id) => id!==plotId);
        return Object.assign({}, drawLayer, {visiblePlotIdAry});
    }
}



/**
 *
 * @param {DrawLayerFactory} factory
 * @param {object} drawLayer
 * @param {object} action
 * @param {string} plotId
 * @return {{}}
 */
function getDrawData(factory, drawLayer, action, plotId= DrawLayer.ALL_PLOTS) {
    if (!factory.hasGetDrawData(drawLayer)) return drawLayer.drawData;
    var pId= plotId===DrawLayer.ALL_PLOTS ? null : plotId;
    var newDD= {[DataTypes.DATA]:{},[DataTypes.HIGHLIGHT_DATA]:{}, [DataTypes.SELECTED_IDX_ARY]: null};

    newDD[DataTypes.DATA][plotId]= factory.getDrawData(DataTypes.DATA, pId, drawLayer, action,
        drawLayer.drawData[DataTypes.DATA][plotId]);

    if (drawLayer.canHighlight) {
        newDD[DataTypes.HIGHLIGHT_DATA][plotId]= factory.getDrawData(DataTypes.HIGHLIGHT_DATA, pId, drawLayer, action,
            drawLayer.drawData[DataTypes.HIGHLIGHT_DATA][plotId]);
    }

    if (drawLayer.canSelect && plotId===DrawLayer.ALL_PLOTS) {
        newDD[DataTypes.SELECTED_IDX_ARY]= factory.getDrawData(DataTypes.SELECTED_IDX_ARY, null, drawLayer, action,
            drawLayer.drawData[DataTypes.SELECTED_IDX_ARY]);
    }
    var retval= {};
    retval[DataTypes.DATA]= Object.assign({},drawLayer.drawData[DataTypes.DATA],newDD[DataTypes.DATA]);
    retval[DataTypes.HIGHLIGHT_DATA]= Object.assign({},drawLayer.drawData[DataTypes.HIGHLIGHT_DATA],newDD[DataTypes.HIGHLIGHT_DATA]);
    retval[DataTypes.SELECTED_IDX_ARY]= newDD[DataTypes.SELECTED_IDX_ARY];
    return retval;

}

