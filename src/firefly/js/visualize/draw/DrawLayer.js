/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {makeDrawingDef} from './DrawingDef.js';
import {GroupingScope} from '../DrawLayerCntlr.js';

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
 * @typedef {Object} DrawLayer
 *
 * @prop {String} drawLayerId unique for each layer
 * @prop {String} displayGroupId, any layer in this group Id will be controlled together in the UI.\
 *                           Default to the drawLayerId layers all have a type string that default to the id,
 *                           however if multiple of same are added, the type id should be set
 * @prop {String} drawingTypeId  allows for multiple layers of same type to be added (such as multiple markers)
 *                if they share the same type id
 *                a drawing layer factory def should always pass the same type Id
 *                eg. all catalog overlays will by the same type of have different layer ids
 * @prop {String} title title to show in the ui
 * @prop {String[]} plotIdAry array of plotId that are layered
 * @prop {String[]} visiblePlotIdAry:  array of plotId that are visible, only ids in this array are visible
 * @prop {String[]} actionTypeAry  what actions that the reducer will allow through the drawing layer reducer
 * @prop {DrawingDef} drawingDef
 *
 * @prop {Boolean} canHighlight default: false,  if the layer can highlight
 * @prop {Boolean} canSelect  default: false
 * @prop {Boolean} dataTooBigForSelection   default: false
 * @prop {Boolean} canFilter  default: false
 * @prop {Boolean} canUseMouse  default: false, drawing layer has mouse interaction, must set up actionTypeAry
 * @prop {Boolean} hasPerPlotData  default: false,  drawing layer produces different data for each plot
 * @prop {Boolean} isPointData  default: false
 * @prop {ColorChangeType} canUserChangeColor  default: ColorChangeType.STATIC
 * @prop {Boolean} canUserDelete  default: true
 * @prop {Boolean} destroyWhenAllDetached default: false ,hint to controller, when all plots have been detached, destroy this layer
 * @prop {String} helpLine   default: '', a one line string describing the operation, for the end user to see
 * @prop {Boolean} canAttachNewPlot default: true, can be attached to the new plot created after the drawing layer is created
 *
 * @prop {Object} drawData   the data to draw
 * @prop {Object} mouseEventMap,
 *
 *
 *
 * drawData contains the components that may be drawn.
 * Three keys are supported data, highlightData, selectedIdxAry
 * other keys could be added later
 *
 *
 * Key:   data
 * Value: null or [] or plotId:[]
 *              arrays are arrays of drawObj
 *              if data is an array the it applies to all the plots
 *              if data is an object the it applies to the only to the plotId with
 *              the fallback being the ALL_PLOTS key
 *
 * Key:   highlightData
 * Value: null or [] or plotId:[]
 *              arrays are arrays of drawObj
 *              if data is an object the it applies to the only to the plotId with
 *
 * Key: selectIdxs
 * Value:      null or an array of selected indexes or a function, does not support per plot data
 *             the indexes in the array refer to  the indexes of the data array
 *             OPTIONALLY you can return a function for this parameter
 *             this function should be f(arrayIdx : number) : boolean, true if the index is selected
 *             IMPORTANT: if the selected data changes a new function must be passed.
 *             This key is typically only used with catalogs and PointDataObj
 *
 *
 *
 *
 *
 */


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
 * @param {boolean} [options.hasPerPlotData] drawing layer produces different data for each plot
 * @param {boolean} [options.isPointData] drawing layer only uses point data, @see PointDataObj.js
 * @param {boolean|string} [options.canUserChangeColor] drawing layer color can be changed by the user,
 *                                   can be DISABLE,DYNAMIC, or STATIC, default: STATIC
 * @param {boolean} [options.canUserDelete] drawing layer can be deleted by the user, default: true
 * @param {string} [options.helpLine] a one line string describing the operation, for the end user to see
 * @param {boolean} [options.canAttachNewPlot] can be attached to new plot created after the drawing layer is created
 *
 * @param {object} drawingDef  the defaults that the drawer will use if not overridden by the object @see DrawingDef
 * @param {Array} actionTypeAry extra [actions] that are allow though to the drawing layer reducer
 * @param {object} mouseEventMap object literal with event to function mapping, see documentation below in object
 * @param {object} exclusiveDef
 * @param {function} getCursor
 * @return {DrawLayer}
 */
function makeDrawLayer(drawLayerId,
                       drawLayerTypeId,
                       title,
                       options={},
                       drawingDef= makeDrawingDef('red'),
                       actionTypeAry= [],
                       mouseEventMap= {},
                       exclusiveDef= null,
                       getCursor= null) {
    const drawLayer=  {


         // it section: The types of IDs
         // drawLayerId: unique for each layer
         // drawingGroupLayerId:
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
        drawingDef,

        groupingScope: GroupingScope.SUBGROUP, // only applies if a catalog has supportSubgroups
        titleMatching: false,


           // The following are the options that the drawing layer supports.
           // should be set in the options parameter
        canHighlight: false,
        dataTooBigForSelection : false,
        canUseMouse: false,
        hasPerPlotData: false,
        isPointData: false,
        canUserChangeColor: ColorChangeType.STATIC,
        canUserDelete: true,
        destroyWhenAllDetached : false, // hint to controller, when all plots have been detached, destroy this layer
        helpLine : '',
        decimate: false, // enable decimation
        canAttachNewPlot: true,
        supportSubgroups: false,

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
           //     mouse type as the key and the function to call when activated @see MouseState
           //     if the value is an object the a it should define the properties: exclusive:boolean and func:function
           //     the function usually dispatch type functions, but can be anything
           //     value can be an action string. in that case flux.process is call to dispatch that action.
           //     a mousestatepayload object is always passed.
           //     {
           //         [mousestate.drag.key]: {exclusive: true, func:dispatchselectareaedit},
           //         [mousestate.down.key]: {exclusive: true, actiontype:do_something_else},
           //         [mousestate.down.key]: dispatchselectareastart,
           //         [mousestate.up.key]: select_area_end
           //         [mousestate.down.key]: {static: true, func:dispatchfindclosestlayer}
           //     };
        mouseEventMap,
        
        
        
           // if defined then:
                  // exclusiveOnDown: boolean, true if exclusive control on down
                  // type : 'anywhere',  - has first priority, used with a new layer, like beginning distance or select
        //                  'vertexOnly', - will be exclusive if near a vertex and there are not layers with 'anywhere'
        //                  'vertexThenAnywhere' - will be exclusive if near a vertex, or not near,
        //                                and there are not layers with 'anywhere' or a 'vertexOnly' that matches
        //   1. First look for a layers that has exclusiveDef.exclusiveOnDown as true
        //   2. if any of those has exclusiveDef.type === 'anywhere' then return the last in the list
        //   3. otherwise if any any layer has exclusiveDef.type === 'vertexOnly'  or 'vertexThenAnywhere' return the first that has
        //             the mouse click near is one if its vertex (vertexDef.points)
        //   4. otherwise if any layer has exclusiveDef.type === 'vertexThenAnywhere' then return that one
        //   5. otherwise return null
        exclusiveDef,

        // if defined then Object with:
                  // points: array of points, // if not define or empty the down is exclusive anywhere
                  // pointDist: 10, // how close to the points in the array to match
        vertexDef: null, 
        

           // return the cursor style type that should be set as the cursor on the viewer
           // 'nw-resize' or 'se-resize', any css cursor is allowed
           // parameters - plotView, screenPt
        getCursor
    };

    return Object.assign(drawLayer,options);
}
