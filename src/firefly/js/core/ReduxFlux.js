/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import createSagaMiddleware from 'redux-saga';
import thunkMiddleware from 'redux-thunk';
import loggerMiddleware from 'redux-logger';
import { createStore, applyMiddleware, combineReducers } from 'redux';
import * as AppDataCntlr  from './AppDataCntlr.js';
import * as LayoutCntlr  from './LayoutCntlr.js';
import {recordHistory} from './History.js';
import FieldGroupCntlr, {valueChangeActionCreator,multiValueChangeActionCreator}
                                      from '../fieldGroup/FieldGroupCntlr.js';
import * as MouseReadoutCntlr from '../visualize/MouseReadoutCntlr.js';
import ImagePlotCntlr, {IMAGE_PLOT_KEY,
                        plotImageActionCreator, plotImageMaskActionCreator, zoomActionCreator,
                        colorChangeActionCreator, stretchChangeActionCreator,
                        rotateActionCreator, flipActionCreator,
                        cropActionCreator, autoPlayActionCreator, changePrimeActionCreator,
                        restoreDefaultsActionCreator, overlayPlotChangeAttributeActionCreator,
                        deletePlotViewActionCreator,
                        changePointSelectionActionCreator, wcsMatchActionCreator}
                        from '../visualize/ImagePlotCntlr.js';

import ExternalAccessCntlr from './ExternalAccessCntlr.js';
import * as TableStatsCntlr from '../charts/TableStatsCntlr.js';
import * as ChartsCntlr from '../charts/ChartsCntlr.js';
import * as TablesCntlr from '../tables/TablesCntlr';

import DrawLayer, {DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import DrawLayerFactory from '../visualize/draw/DrawLayerFactory.js';
import DrawLayerCntlr, {makeDetachLayerActionCreator,
                        selectAreaEndActionCreator,
                        distanceToolEndActionCreator,
                        regionCreateLayerActionCreator,
                        regionDeleteLayerActionCreator,
                        regionUpdateEntryActionCreator,
                        markerToolStartActionCreator,
                        markerToolMoveActionCreator,
                        markerToolEndActionCreator,
                        markerToolCreateLayerActionCreator,
                        footprintCreateLayerActionCreator,
                        footprintStartActionCreator,
                        footprintMoveActionCreator,
                        footprintEndActionCreator} from '../visualize/DrawLayerCntlr.js';
import MultiViewCntlr, {IMAGE_MULTI_VIEW_KEY} from '../visualize/MultiViewCntlr.js';
import ComponentCntlr, {DIALOG_OR_COMPONENT_KEY} from '../core/ComponentCntlr.js';
import {masterSaga} from './MasterSaga.js';

//--- import drawing Layers
import ActiveTarget from '../drawingLayers/ActiveTarget.js';
import SelectArea from '../drawingLayers/SelectArea.js';
import DistanceTool from '../drawingLayers/DistanceTool.js';
import PointSelection from '../drawingLayers/PointSelection.js';
import StatsPoint from '../drawingLayers/StatsPoint.js';
import NorthUpCompass from '../drawingLayers/NorthUpCompass.js';
import Catalog from '../drawingLayers/Catalog.js';
import WebGrid from '../drawingLayers/WebGrid.js';

import RegionPlot from '../drawingLayers/RegionPlot.js';
import MarkerTool from '../drawingLayers/MarkerTool.js';
import FootprintTool from '../drawingLayers/FootprintTool.js';
import {showExampleDialog} from '../ui/ExampleDialog.jsx';

//==============
// import Perf from 'react-addons-perf';
// window.Perf = Perf;


/**
 * @global
 * @public
 * @typedef {Object} ApplicationState
 *
 * @prop {VisRoot} allPlots - image plotting store  (Controller: ImagePlotCntlr.js)
 * @prop {TableSpace} table_space - table data store (Controller: TablesCntlr.js)
 * @prop {Object} charts - information about 2D plots (Controller: ChartsCntlr.js)
 * @prop {FieldGroupStore} fieldGroup - field group data for form and dialog input (Controller: FieldGroupCntlr.js)
 * @prop {Object} readout - mouse readout information (Controller: ReadoutCntlr.js)
 * @prop {Object} app_data - general application information (Controller: AppDataCntlr.js)
 * @prop {Object} drawLayers - information about the drawing layers e.g. select tool, catalogs overlays, regions, etc
 * @prop {Viewer} imageMultiView - data about the various image viewers (Controller: MultiViewCntlr.js)
 * @prop {Object} externalAccess - controls communication events with eternal applications (Controller: ExternalAccessCntlr.js)
 * @prop {Object} layout - information about application layout (Controller: LayoutCntlr.js)
 * @prop {Object} tblstats - stats for histogram, etc (Controller: TableStatsCntlr.js)
 * @prop {Object} dialogOrComponent - hold information about dialog visibility and other components (Controller: ComponentCntlr.js)
 *
 */



/**
 * @typedef {Object} Action
 * @prop {String} type - the action constant, a unique string identifying this action
 * @prop {Object} payload - object with anything, the data
 * @global
 * @public
 */


/**
 * A map to rawAction.type to an ActionCreator
 * @type {Map<string, function>}
 */
const actionCreators = new Map();



const drawLayerFactory= DrawLayerFactory.makeFactory(ActiveTarget,SelectArea,DistanceTool,
                                                     PointSelection, StatsPoint, NorthUpCompass,
                                                     Catalog, WebGrid, RegionPlot, MarkerTool, FootprintTool);



/**
 * A collection of reducers keyed by the node's name under the root.
 * @type {Object<string, function>}
 */
const reducers = {
    [AppDataCntlr.APP_DATA_PATH]: AppDataCntlr.reducer,
    [LayoutCntlr.LAYOUT_PATH]: LayoutCntlr.reducer,
    [FieldGroupCntlr.FIELD_GROUP_KEY]: FieldGroupCntlr.reducer,
    [IMAGE_PLOT_KEY]: ImagePlotCntlr.reducer,
    [ExternalAccessCntlr.EXTERNAL_ACCESS_KEY]: ExternalAccessCntlr.reducer,
    [TableStatsCntlr.TBLSTATS_DATA_KEY]: TableStatsCntlr.reducer,
    [ChartsCntlr.CHART_SPACE_PATH]: ChartsCntlr.reducer,
    [TablesCntlr.TABLE_SPACE_PATH]: TablesCntlr.reducer,
    [DRAWING_LAYER_KEY]: DrawLayer.makeReducer(drawLayerFactory),
    [IMAGE_MULTI_VIEW_KEY]: MultiViewCntlr.reducer,
    [MouseReadoutCntlr.READOUT_KEY]: MouseReadoutCntlr.reducer,
    [DIALOG_OR_COMPONENT_KEY]: ComponentCntlr.reducer
};

let redux = null;


// pre-map a set of action => creator prior to bootstrapping.
actionCreators.set(AppDataCntlr.APP_LOAD, AppDataCntlr.loadAppData);
actionCreators.set(AppDataCntlr.GRAB_WINDOW_FOCUS, AppDataCntlr.grabWindowFocus);
actionCreators.set(AppDataCntlr.HELP_LOAD, AppDataCntlr.onlineHelpLoad);
actionCreators.set(FieldGroupCntlr.VALUE_CHANGE, valueChangeActionCreator);
actionCreators.set(FieldGroupCntlr.MULTI_VALUE_CHANGE, multiValueChangeActionCreator);
actionCreators.set(ExternalAccessCntlr.EXTENSION_ACTIVATE, ExternalAccessCntlr.extensionActivateActionCreator);
actionCreators.set(ImagePlotCntlr.PLOT_IMAGE, plotImageActionCreator);
actionCreators.set(ImagePlotCntlr.PLOT_MASK, plotImageMaskActionCreator);
actionCreators.set(ImagePlotCntlr.OVERLAY_PLOT_CHANGE_ATTRIBUTES, overlayPlotChangeAttributeActionCreator);
actionCreators.set(ImagePlotCntlr.ZOOM_IMAGE, zoomActionCreator);
actionCreators.set(ImagePlotCntlr.COLOR_CHANGE, colorChangeActionCreator);
actionCreators.set(ImagePlotCntlr.STRETCH_CHANGE, stretchChangeActionCreator);
actionCreators.set(ImagePlotCntlr.ROTATE, rotateActionCreator);
actionCreators.set(ImagePlotCntlr.FLIP, flipActionCreator);
actionCreators.set(ImagePlotCntlr.CROP, cropActionCreator);
actionCreators.set(ImagePlotCntlr.CHANGE_PRIME_PLOT , changePrimeActionCreator);
actionCreators.set(ImagePlotCntlr.CHANGE_POINT_SELECTION, changePointSelectionActionCreator);
actionCreators.set(ImagePlotCntlr.RESTORE_DEFAULTS, restoreDefaultsActionCreator);
actionCreators.set(ImagePlotCntlr.EXPANDED_AUTO_PLAY, autoPlayActionCreator);
actionCreators.set(ImagePlotCntlr.WCS_MATCH, wcsMatchActionCreator);
actionCreators.set(ImagePlotCntlr.DELETE_PLOT_VIEW, deletePlotViewActionCreator);
actionCreators.set(DrawLayerCntlr.DETACH_LAYER_FROM_PLOT, makeDetachLayerActionCreator(drawLayerFactory));

actionCreators.set(TablesCntlr.TABLE_SEARCH, TablesCntlr.tableSearch);
actionCreators.set(TablesCntlr.TABLE_FETCH, TablesCntlr.tableFetch);
actionCreators.set(TablesCntlr.TABLE_SORT, TablesCntlr.tableFetch);
actionCreators.set(TablesCntlr.TABLE_FILTER, TablesCntlr.tableFetch);
actionCreators.set(TablesCntlr.TABLE_HIGHLIGHT, TablesCntlr.highlightRow);

actionCreators.set(TableStatsCntlr.LOAD_TBL_STATS, TableStatsCntlr.loadTblStats);
actionCreators.set(ChartsCntlr.CHART_DATA_FETCH, ChartsCntlr.chartDataFetch);
actionCreators.set(ChartsCntlr.CHART_OPTIONS_REPLACE, ChartsCntlr.chartOptionsReplace);
actionCreators.set(ChartsCntlr.CHART_OPTIONS_UPDATE, ChartsCntlr.chartOptionsUpdate);

actionCreators.set(DrawLayerCntlr.SELECT_AREA_END, selectAreaEndActionCreator);
actionCreators.set(DrawLayerCntlr.DT_END, distanceToolEndActionCreator);
actionCreators.set(DrawLayerCntlr.MARKER_START, markerToolStartActionCreator);
actionCreators.set(DrawLayerCntlr.MARKER_MOVE, markerToolMoveActionCreator);
actionCreators.set(DrawLayerCntlr.MARKER_END, markerToolEndActionCreator);
actionCreators.set(DrawLayerCntlr.MARKER_CREATE, markerToolCreateLayerActionCreator);
actionCreators.set(DrawLayerCntlr.FOOTPRINT_CREATE, footprintCreateLayerActionCreator);
actionCreators.set(DrawLayerCntlr.FOOTPRINT_START, footprintStartActionCreator);
actionCreators.set(DrawLayerCntlr.FOOTPRINT_END, footprintEndActionCreator);
actionCreators.set(DrawLayerCntlr.FOOTPRINT_MOVE, footprintMoveActionCreator);

actionCreators.set(DrawLayerCntlr.REGION_CREATE_LAYER, regionCreateLayerActionCreator);
actionCreators.set(DrawLayerCntlr.REGION_DELETE_LAYER, regionDeleteLayerActionCreator);
actionCreators.set(DrawLayerCntlr.REGION_ADD_ENTRY, regionUpdateEntryActionCreator);
actionCreators.set(DrawLayerCntlr.REGION_REMOVE_ENTRY, regionUpdateEntryActionCreator);


actionCreators.set('exampleDialog', (rawAction) => {
    showExampleDialog();
    return rawAction;
});



/**
 * object with a key that can be filtered out, value should be a boolean or a function that returns a boolean
 */
// eslint-disable-next-line
var filterOutOfLogging= {
    [ExternalAccessCntlr.EXTENSION_ACTIVATE]: (action) => !action.payload.extension || action.payload.extension.extType!=='PLOT_MOUSE_READ_OUT',
    [FieldGroupCntlr.MOUNT_COMPONENT]: false
};

/**
 * array of action types that will be logged as collapsed
 */
var collapsedLogging= [
    ExternalAccessCntlr.EXTENSION_ACTIVATE
];

window.enableFireflyReduxLogging= true;


/**
 * Can be used for debugging.  Adjust content of filter function to suit your needs
 * @param getState
 * @param action
 * @return {boolean}
 */
function logFilter(getState,action) { 
    const {type}= action;
    if (!type) return false;
    if (type.startsWith('VisMouseCntlr')) return false;
    if (type.startsWith('EFFECT')) return false;
    if (type.startsWith('FieldGroupCntlr')) return false;
    if (type.startsWith('layout')) return false;
    if (type.startsWith('table_space')) return false;
    if (type.startsWith('tblstats')) return false;
    if (type.startsWith('table_ui')) return false;
    if (type.startsWith('app_data')) return false;
    return true;
}


function collapsedFilter(getState,action) {
    return collapsedLogging.includes(action.type);
}


// eslint-disable-next-line
var logger= loggerMiddleware({duration:true, predicate:logFilter, collapsed:collapsedFilter}); // developer can add for debugging


function createRedux() {
    // create a rootReducer from all of the registered reducers
    const rootReducer = combineReducers(reducers);
    const sagaMiddleware = createSagaMiddleware();
    const middleWare=  applyMiddleware(thunkMiddleware, /*logger,*/ sagaMiddleware);
    const store = createStore(rootReducer, middleWare);
    sagaMiddleware.run(masterSaga);
    return store;
}

function bootstrap() {
    if (redux === null) {
        redux = createRedux();
    }
    return new Promise(
        function (resolve, reject) {
            // there may be async logic here..
            // if not, simply invoke resolve.
            resolve('success');
        });
}

/**
 * Process the rawAction.  This uses the actionCreators map to resolve
 * the ActionCreator given the action.type.  If one is not mapped, then it'll
 * create a simple 'pass through' ActionCreator that returns the rawAction as an action.
 *
 * <i>Note: </i> Often it makes sense to have a utility function call <code>process</code>. In that case
 * the utility function should meet the follow criteria.  This is a good way to document and default the
 * payload parameters.  The utility function should implement the following standard:
 * <ul>
 *     <li>The function name should start with "dispatch"</li>
 *     <li>The action type as the second part of the name</li>
 *     <li>The function should be exported from the controller</li>
 *     <li>The function parameters should the documented with jsdocs</li>
 *     <li>Optional parameters should be clear</li>
 * </ul>
 * Utility function Example - if action type is <code>PLOT_IMAGE</code> and the <code>PLOT_IMAGE</code> action
 * is exported from the ImagePlotCntlr module.  The the name should be <code>processPlotImage</code>.
 *
 *
 * @param {Action} rawAction
 * @returns {Promise}
 */
function process(rawAction) {
    if (!redux) throw Error('firefly has not been bootstrapped');

    var ac = actionCreators.get(rawAction.type);
    if (!rawAction.payload) rawAction= Object.assign({},rawAction,{payload:{}});
    if (ac) {
        redux.dispatch(ac(rawAction));
    } else {
        redux.dispatch( rawAction );
    }
    recordHistory(rawAction);
}

function addListener(listener, ...types) {
    if (!redux) return;
    if (types.length) {
        return () => {
            var appState = redux.getState();
        };
    } else {
        return redux.subscribe(listener);
    }
}

function registerCreator(actionCreator, ...types) {
    if (types) {
        types.forEach( (v) => actionCreators.set(v, actionCreator) );
    }
}

function registerReducer(dataRoot, reducer) {
    reducers[dataRoot] = reducer;
}

function getState() {
    return redux ? redux.getState() : null;
}

function getRedux() {
   return redux;
}

function getDrawLayerFactory() {
    return drawLayerFactory;
}

function registerDrawLayer(factoryDef) {
    drawLayerFactory.register(factoryDef);
}

function setDrawLayerDefaults(typeId,defaults) {
    drawLayerFactory.setDrawLayerDefaults(typeId,defaults);
}

function createDrawLayer(drawLayerTypeId, params) {
    return drawLayerFactory.create(drawLayerTypeId,params);
}


export var reduxFlux = {
    registerCreator,
    registerReducer,
    bootstrap,
    getState,
    process,
    addListener,
    registerDrawLayer,
    createDrawLayer,
    getDrawLayerFactory,
    setDrawLayerDefaults,
    getRedux
};

