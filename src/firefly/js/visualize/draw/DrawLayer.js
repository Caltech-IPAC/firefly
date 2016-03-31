/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {makeDrawingDef} from './DrawingDef.js';

export default {makeDrawLayer};

const DATA='data';
const HIGHLIGHT_DATA='highlightData';
const SELECTED_IDXS='selectIdxs';

const DISABLE=false;
const DYNAMIC='dynamic';
const STATIC='static';


export const DataTypes= {DATA,HIGHLIGHT_DATA,SELECTED_IDXS};
export const ColorChangeType= {DISABLE,DYNAMIC,STATIC};


/**
 *
 * @param {string} drawLayerId
 * @param {string} drawLayerTypeId
 * @param {string|{}} title can be a string title or an object {plotId:title}
 * @param {object} options a set of options that define this drawing layer All boolean options default to false
 *                 unless specified and string options to the empty string
 * @param {boolean} [options.canHighlight] if the layer can highlight
 * @param {boolean} [options.canSelect]    supports a selected array of objects, must be able to produce a selected data array,
 *                               only used with isPointData
 * @param {boolean} [options.canFilter]    drawing layer can be used with the filter controls,
 *                               only used with canSelect and isPointData
 * @param {boolean} [options.canUseMouse]  drawing layer has mouse interaction, must set up actionTypeAry
 * @param {boolean} [options.canSubgroup]  can be used with subgrouping
 * @param {boolean} [options.hasPerPlotData] drawing layer produces different data for each plot
 * @param {boolean} [options.asyncData] drawing layer uses async operations to get the data
 * @param {boolean} [options.isPointData] drawing layer only uses point data, @see PointDataObj.js
 * @param {boolean|string} [options.canUserChangeColor] drawing layer color can be changed by the user,
 *                                   can be DISABLE,DYNAMIC, or STATIC, default: STATIC
 * @param {boolean} [options.canUserDelete] drawing layer can be deleted by the user, default: true
 * @param {string} [options.helpLine] a one line string describing the operation, for the end user to see
 *
 * @param {object} drawingDef  the defaults that the drawer will use if not overridden by the object @see DrawingDef
 * @param {Array} actionTypeAry extra [actions] that are allow though to the drawing layer reducer
 * @param {object} mouseEventMap object literal with event to function mapping, see documentation below in object
 * @return {*}
 */
function makeDrawLayer(drawLayerId,
                       drawLayerTypeId,
                       title,
                       options={},
                       drawingDef= makeDrawingDef('red'),
                       actionTypeAry= [],
                       mouseEventMap= {}) {
    var drawLayer=  {


         // it section: The types of IDs
         // drawLayerId: unique for each layer
         // drawingGroupLayerId: any layer in this group Id will be controlled together in the UI
         //                       default to the drawLayerId
         // drawingTypeId: allows for multiple layers of same type to be added (such as multiple markers)
         //                if they share the same type id then events mapping marked static will only be fired once
         //                a drawing layer factory def should always pass the same type Id
         //                eg. all catalog overlays will by the same type of have different layer ids


        drawLayerId,
        displayGroupId: drawLayerId,   // all layers that share a display group id will be controlled together, defaults to drawLayerId
        drawLayerTypeId, // layers all have a type string that default to the id, however if multiple of same are added, the type id should be set




        title,
        plotIdAry: [],  // array of plotId that are layered
        visiblePlotIdAry: [], // array of plotId that are visible, only ids in this array are visible
        actionTypeAry,      // what actions that the reducer will allow through the drawing layer reducer
        dataAvailable : true,  //todo
        drawingDef,

        // following to fields deal with subgrouping //todo: the subgroup is not yet finished, I still need to think through it
        subgroups : {},       // subgroupId : plotIdAry todo: decide if this is the right approach
        groupTypes: [],       // id's of types of subgroups such as single, row, all, todo: is this over generalized?



           // The following are the options that the drawing layer supports.
           // should be set in the options parameter
        canHighlight: false,
        canSelect: false,      // todo if true the the default reducer should  handle it. point data only?
        dataTooBigForSelection : false,
        canFilter: false,      // todo if true the the default reducer should  handle it
        canUseMouse: false,
        canSubgroup: false,    // todo
        hasPerPlotData: false,
        asyncData : false,  //todo
        isPointData: false,
        canUserChangeColor: ColorChangeType.STATIC,
        canUserDelete: true,
        helpLine : '',

           // drawData contains the components that may be drawn.
           // Three keys are supported data, highlightData, selectedIdxAry
           // other keys could be added later
           //
           //
           // Key:   data
           // Value: null or [] or plotId:[]
           //              arrays are arrays of drawObj
           //              if data is an array the it applies to all the plots
           //              if data is an object the it applies to the only to the plotId with
           //              the fallback being the ALL_PLOTS key
           //
           // Key:   highlightData
           // Value: null or [] or plotId:[]
           //              arrays are arrays of drawObj
           //              if data is an object the it applies to the only to the plotId with
           //
           // Key: selectIdxs
           // Value:      null or an array of selected indexes or a function, does not support per plot data
           //             the indexes in the array refer to  the indexes of the data array
           //             OPTIONALLY you can return a function for this parameter 
           //             this function should be f(arrayIdx : number) : boolean, true if the index is selected
           //             IMPORTANT: if the selected data changes a new function must be passed.
           //             This key is typically only used with catalogs and PointDataObj
        drawData : {
            [DataTypes.DATA]:{},
            [DataTypes.HIGHLIGHT_DATA]:{},
            [DataTypes.SELECTED_IDXS]: null
        },


           //
           //     Mouse type as the key and the function to call when activated @see MouseState
           //     If the value is an object the a it should define the properties: exclusive:boolean and func:function
           //     The function usually dispatch type functions, but can be anything
           //     value can be an action string. In that case flux.process is call to dispatch that action.
           //     a mouseStatePayload object is always passed.
           //     {
           //         [MouseState.DRAG.key]: {exclusive: true, func:dispatchSelectAreaEdit},
           //         [MouseState.DOWN.key]: {exclusive: true, actionType:DO_SOMETHING_ELSE},
           //         [MouseState.DOWN.key]: dispatchSelectAreaStart,
           //         [MouseState.UP.key]: SELECT_AREA_END
           //         [MouseState.DOWN.key]: {static: true, func:dispatchFindClosestLayer}
           //     };
        mouseEventMap,

           // the cursor style type that should be set as the cursor on the viewer
           // 'nw-resize' or 'se-resize', any css cursor is allowed
        cursor : ''
    };

    return Object.assign(drawLayer,options);
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
