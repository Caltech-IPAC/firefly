/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import createSagaMiddleware from 'redux-saga';
import thunkMiddleware from 'redux-thunk';
import loggerMiddleware from 'redux-logger';
import { createStore, applyMiddleware, combineReducers } from 'redux';
import * as AppDataCntlr  from './AppDataCntlr.js';
import {recordHistory} from './History.js';
import {LAYOUT_PATH, reducer as layoutReducer}  from './LayoutCntlr.js';
import FieldGroupCntlr from '../fieldGroup/FieldGroupCntlr.js';
import * as MouseReadoutCntlr from '../visualize/MouseReadoutCntlr.js';
import ImagePlotCntlr, {IMAGE_PLOT_KEY,
                        plotImageActionCreator, zoomActionCreator,
                        colorChangeActionCreator, stretchChangeActionCreator,
                        rotateActionCreator, flipActionCreator,
                        cropActionCreator, autoPlayActionCreator, changePrimeActionCreator,
                        restoreDefaultsActionCreator,
                        changePointSelectionActionCreator  } from '../visualize/ImagePlotCntlr.js';

import ExternalAccessCntlr from './ExternalAccessCntlr.js';
import * as TableStatsCntlr from '../visualize/TableStatsCntlr.js';
import * as HistogramCntlr from '../visualize/HistogramCntlr.js';
import * as XYPlotCntlr from '../visualize/XYPlotCntlr.js';
import * as TablesCntlr from '../tables/TablesCntlr';

import DrawLayer, {DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import DrawLayerFactory from '../visualize/draw/DrawLayerFactory.js';
import DrawLayerCntlr, {makeDetachLayerActionCreator} from '../visualize/DrawLayerCntlr.js';
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

import {showExampleDialog} from '../ui/ExampleDialog.jsx';

//==============
// import Perf from 'react-addons-perf';
// window.Perf = Perf;


/**
 * A map to rawAction.type to an ActionCreator
 * @type {Map<string, function>}
 */
const actionCreators = new Map();



const drawLayerFactory= DrawLayerFactory.makeFactory(ActiveTarget,SelectArea,DistanceTool,
                                                     PointSelection, StatsPoint, NorthUpCompass,
                                                     Catalog, WebGrid, RegionPlot);



/**
 * A collection of reducers keyed by the node's name under the root.
 * @type {Object<string, function>}
 */
const reducers = {
    [AppDataCntlr.APP_DATA_PATH]: AppDataCntlr.reducer,
    [LAYOUT_PATH]: layoutReducer,
    [FieldGroupCntlr.FIELD_GROUP_KEY]: FieldGroupCntlr.reducer,
    [IMAGE_PLOT_KEY]: ImagePlotCntlr.reducer,
    [ExternalAccessCntlr.EXTERNAL_ACCESS_KEY]: ExternalAccessCntlr.reducer,
    [TableStatsCntlr.TBLSTATS_DATA_KEY]: TableStatsCntlr.reducer,
    [HistogramCntlr.HISTOGRAM_DATA_KEY]: HistogramCntlr.reducer,
    [XYPlotCntlr.XYPLOT_DATA_KEY]: XYPlotCntlr.reducer,
    [TablesCntlr.TABLE_SPACE_PATH]: TablesCntlr.reducer,
    [DRAWING_LAYER_KEY]: DrawLayer.makeReducer(drawLayerFactory),
    [IMAGE_MULTI_VIEW_KEY]: MultiViewCntlr.reducer,
    [MouseReadoutCntlr.READOUT_KEY]: MouseReadoutCntlr.reducer,
    [DIALOG_OR_COMPONENT_KEY]: ComponentCntlr.reducer
};

let redux = null;


// pre-map a set of action => creator prior to boostraping.
actionCreators.set(AppDataCntlr.APP_LOAD, AppDataCntlr.loadAppData);
actionCreators.set(AppDataCntlr.HELP_LOAD, AppDataCntlr.onlineHelpLoad);
actionCreators.set(FieldGroupCntlr.VALUE_CHANGE, FieldGroupCntlr.valueChangeActionCreator);
actionCreators.set(ExternalAccessCntlr.EXTENSION_ACTIVATE, ExternalAccessCntlr.extensionActivateActionCreator);
actionCreators.set(ImagePlotCntlr.PLOT_IMAGE, plotImageActionCreator);
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
actionCreators.set(DrawLayerCntlr.DETACH_LAYER_FROM_PLOT, makeDetachLayerActionCreator(drawLayerFactory));

actionCreators.set(TablesCntlr.TABLE_SEARCH, TablesCntlr.tableSearch);
actionCreators.set(TablesCntlr.TABLE_FETCH, TablesCntlr.fetchTable);
actionCreators.set(TablesCntlr.TABLE_FETCH_UPDATE, TablesCntlr.fetchTable);
actionCreators.set(TablesCntlr.TABLE_HIGHLIGHT, TablesCntlr.highlightRow);

actionCreators.set(TableStatsCntlr.LOAD_TBL_STATS, TableStatsCntlr.loadTblStats);
actionCreators.set(HistogramCntlr.LOAD_COL_DATA, HistogramCntlr.loadColData);
actionCreators.set(XYPlotCntlr.LOAD_PLOT_DATA, XYPlotCntlr.loadPlotData);



actionCreators.set('exampleDialog', (rawAction) => {
    showExampleDialog();
    return rawAction;
});



/**
 * object with a key that can be filtered out, value should be a boolean or a function that returns a boolean
 */
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


var logger= loggerMiddleware({duration:true, predicate:logFilter, collapsed:collapsedFilter}); // developer can add for debugging


function createRedux() {
    // create a rootReducer from all of the registered reducers
    const rootReducer = combineReducers(reducers);
    const middleWare=  applyMiddleware(thunkMiddleware, createSagaMiddleware(masterSaga));
    
    return createStore(rootReducer, middleWare);
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
 * @param rawAction
 * @param condition
 * @returns {Promise}
 */
function process(rawAction, condition) {
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
    getRedux
};

