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


/**
 *
 * @param drawLayerId
 * @param {object} options a set of options that define this drawing layer
 * <ul>
 *          <li>The following are the boolean options, all default to false:
 *
 *     <ul>
 *               <li>canHighlight:   supports a highlighted object, must be able to produce highlighted data for this option
 *               <li>canSelect:      supports a selected array of objects, must be able to produce a selected data array,
 *                               only used with isPointData
 *               <li>canFilter:      drawing layer can be used with the filter controls,
 *                               only used with canSelect and isPointData
 *               <li>canUseMouse:    drawing layer has mouse interaction, must set up actionIdAry
 *               <li>canSubgroup:    can be used with subgrouping
 *               <li>hasPerPlotData: drawing layer produces different data for each plot
 *               <li>asyncData :     drawing layer uses async operations to get the data
 *               <li>isPointData:    drawing layer only uses point data, @see PointDataObj.js
 *     </ul>
 *
 *
 *          <li>The following are the string options, all default to empty string
 *     <ul>
 *               <li>helpLine:       a one line string describing the operation, for the end user to see
 *
 *     </ul>
 *
 * </ul>
 *
 *
 * @param {object} drawingDef  the defaults that the drawer will use if not overridded @see DrawingDef
 * @param actionIdAry extra actions that are allow though to the drawing layer reducer
 * @return {*}
 */
function makeDrawingLayer(drawLayerId, options, drawingDef= makeDrawingDef('red'),actionIdAry= []) {
    var drawingLayer=  {
        drawLayerId,
        displayGroupId:drawLayerId,   // all layers that share a display group id will be controlled together, defaults to drawLayerId
        plotIdAry: [ALL_PLOTS],  // array of plotId that are layered
        visiblePlotIdAry: [], // array of plotId that are visible
        actionIdAry,      // what actions that the reducer will allow through the drawing layer reducer
        dataAvailable : true,  //todo
        drawingDef,

        // following to fields deal with subgrouping
        subgroups : {},       // subgroupId : plotIdAry todo: decide if this is the right approach
        groupTypes: [],       // id's of types of subgroups such as single, row, all, todo: is this over generalized?



           // The following are the options that the drawing layer supports.
           // should be set in the options parameter
        canHighlight: false,
        canSelect: false,      // todo if true the the default reducer should  handle it. point data only?
        canFilter: false,      // todo if true the the default reducer should  handle it
        canUseMouse: false,    // todo
        canSubgroup: false,    //todo
        hasPerPlotData: false,
        asyncData : false,  //todo
        isPointData: false,
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

    return Object.assign(drawingLayer,options);
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
