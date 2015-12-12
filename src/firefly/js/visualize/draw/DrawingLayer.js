/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {makeDrawingDef} from './DrawingDef.js';
const ALL_PLOTS= 'AllPlots';

export default {makeDrawingLayer,ALL_PLOTS};

const DATA='data';
const HIGHLIGHT_DATA='highlightData';
const SELECTED_IDX_ARY='selectIdxAry';

export const DataTypes= {DATA,HIGHLIGHT_DATA,SELECTED_IDX_ARY};

function makeDrawingLayer(drawLayerId, overrideOptions) {
    var drawingLayer=  {
        drawLayerId,
        controlGroupId:drawLayerId,   // all layers that share a control group id will be controlled together, defaults to drawLayerId
        plotIdAry: [ALL_PLOTS],  // array of plotId that are layered
        visiblePlotIdAry: [], // array of plotId that are visible
        actionIdAry :[],      // what actions that the reducer all thought to the drawing layer reducer

        // following to fields deal with subgrouping
        subgroups : {},       // subgroupId : plotIdAry todo: decide if this is the right approach
        groupTypes: [],       // id's of types of subgroups such as single, row, all, todo: is this over generalized?

        canHighlight: false,    // todo if true the the default reducer should  handle it. point data only?
        canSelect: false,      // todo if true the the default reducer should  handle it. point data only?
        selectedCnt: 0,
        canFilter: false,      // todo if true the the default reducer should  handle it
        canUseMouse: false,      // todo this might be unnecessary now
        canSubgroup: false,
        hasPerPlotData: false,
        asyncData : false,
        dataAvailable : true,
        isPointData: false,
        drawingDef: makeDrawingDef('red'),
        helpLine : '',

           // drawData contains the components that may be drawn.
           // Three keys are supported data, highlightData, selectedIdxAry
           // other keys could be added later
           //
           //
           // Key:   data
           // Value: null or [] or plotId:[] or ALL_PLOTS:[]
           //              arrays are arrays of drawObj
           //              if data is an array the it applies to all the plots
           //              if data is an object the it applies to the only to the plotId with
           //              the fallback being the ALL_PLOTS key
           //
           // Key:   highlightData
           // Value: null or [] or plotId:[] or ALL_PLOTS:[]
           //              arrays are arrays of drawObj
           //              if data is an object the it applies to the only to the plotId with
           //              the fallback being the ALL_PLOTS key
           //
           // Key: selectIdxAry
           // Value:      null or an array of selected indexes, does not support per plot data
           //             the indexes in the array refer to  the indexes of the data array
           //             This key is typically only used with catalogs and PointDataObj
        drawData : {
            [DataTypes.DATA]:{},
            [DataTypes.HIGHLIGHT_DATA]:{},
            [DataTypes.SELECTED_IDX_ARY]: null
        }
    };

    return Object.assign(drawingLayer,overrideOptions);
}

/**
 *
 * @param drawLayerId
 * @param plotId
 * @return if async and !dataAvailable return false otherwise return true
 */
function isDataAvailable(drawLayerId, plotId) {

}


/**
 * may return null is !isDataAvailable()
 * @param drawLayerId
 * @param plotId
 */
function getData(drawLayerId, plotId) {

}
