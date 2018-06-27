/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import createSagaMiddleware from 'redux-saga';
import thunkMiddleware from 'redux-thunk';
import {createLogger} from 'redux-logger';
import { createStore, applyMiddleware, combineReducers } from 'redux';
import {masterSaga, dispatchAddSaga} from './MasterSaga.js';
import AppDataCntlr  from './AppDataCntlr.js';
import BackgroundCntlr from './background/BackgroundCntlr.js';
import * as LayoutCntlr  from './LayoutCntlr.js';
import {recordHistory} from './History.js';
import FieldGroupCntlr, {MOUNT_COMPONENT} from '../fieldGroup/FieldGroupCntlr.js';
import MouseReadoutCntlr from '../visualize/MouseReadoutCntlr.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';

import ExternalAccessCntlr from './ExternalAccessCntlr.js';
import * as TableStatsCntlr from '../charts/TableStatsCntlr.js';
import ChartsCntlrDef from '../charts/ChartsCntlr.js';
import TablesCntlr from '../tables/TablesCntlr';
import WorkspaceCntlr from '../visualize/WorkspaceCntlr.js';


import DrawLayerFactory from '../visualize/draw/DrawLayerFactory.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import MultiViewCntlr from '../visualize/MultiViewCntlr.js';
import ComponentCntlr, {DIALOG_OR_COMPONENT_KEY} from '../core/ComponentCntlr.js';



//--- import Sagas
import {imagePlotter} from '../visualize/saga/ImagePlotter.js';
import {watchReadout} from '../visualize/saga/MouseReadoutWatch.js';
import {watchForRelatedActions} from '../fieldGroup/FieldGroupCntlr.js';
import {watchExtensionActions} from '../core/messaging/ExternalAccessWatcher.js';



//--- import drawing Layers
import ActiveTarget from '../drawingLayers/ActiveTarget.js';
import SelectArea from '../drawingLayers/SelectArea.js';
import DistanceTool from '../drawingLayers/DistanceTool.js';
import PointSelection from '../drawingLayers/PointSelection.js';
import StatsPoint from '../drawingLayers/StatsPoint.js';
import NorthUpCompass from '../drawingLayers/NorthUpCompass.js';
import Catalog from '../drawingLayers/Catalog.js';
import Artifact from '../drawingLayers/Artifact.js';
import WebGrid from '../drawingLayers/WebGrid.js';
import HiPSGrid from '../drawingLayers/HiPSGrid.js';

import RegionPlot from '../drawingLayers/RegionPlot.js';
import MarkerTool from '../drawingLayers/MarkerTool.js';
import FootprintTool from '../drawingLayers/FootprintTool.js';
import ImageOutline from '../drawingLayers/ImageOutline.js';
import {showExampleDialog} from '../ui/ExampleDialog.jsx';



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
 * @prop {AppDataStore} app_data - general application information (Controller: AppDataCntlr.js)
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
                                                     Catalog, Artifact, WebGrid, RegionPlot,
                                                     MarkerTool, FootprintTool, HiPSGrid,
                                                     ImageOutline);

/**
 * A collection of reducers keyed by the node's name under the root.
 * @type {Object<string, function>}
 */
const reducers = {
    [LayoutCntlr.LAYOUT_PATH]: LayoutCntlr.reducer,
    [ExternalAccessCntlr.EXTERNAL_ACCESS_KEY]: ExternalAccessCntlr.reducer,
    [TableStatsCntlr.TBLSTATS_DATA_KEY]: TableStatsCntlr.reducer,
    [DIALOG_OR_COMPONENT_KEY]: ComponentCntlr.reducer
};

function registerCntlr(cntlr={}) {
    cntlr.reducers && Object.entries(cntlr.reducers()).forEach(([k,v]) => reducers[k]= v);
    cntlr.actionCreators && Object.entries(cntlr.actionCreators()).forEach(([k,v]) => actionCreators.set(k,v));
}

// registering controllers...
registerCntlr(AppDataCntlr);
registerCntlr(BackgroundCntlr);
registerCntlr(ImagePlotCntlr);
registerCntlr(FieldGroupCntlr);
registerCntlr(MouseReadoutCntlr);
registerCntlr(ExternalAccessCntlr);
registerCntlr(TablesCntlr);
registerCntlr(DrawLayerCntlr.getDrawLayerCntlrDef(drawLayerFactory));
registerCntlr(ChartsCntlrDef);
registerCntlr(MultiViewCntlr);
registerCntlr(WorkspaceCntlr);


let redux = null;

// pre-map a set of action => creator prior to bootstrapping.

actionCreators.set(TableStatsCntlr.LOAD_TBL_STATS, TableStatsCntlr.loadTblStats);



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
    [MOUNT_COMPONENT]: false
};

/**
 * array of action types that will be logged as collapsed
 */
var collapsedLogging= [
    ExternalAccessCntlr.EXTENSION_ACTIVATE
];

window.enableFireflyReduxLogging= false;


/**
 * Can be used for debugging.  Adjust content of filter function to suit your needs
 * @param getState
 * @param action
 * @return {boolean}
 */
// function logFilter(getState,action) {
//     const {type}= action;
//     if (!type) return false;
//     if (type.startsWith('VisMouseCntlr')) return false;
//     if (type.startsWith('EFFECT')) return false;
//     if (type.startsWith('FieldGroupCntlr')) return false;
//     if (type.startsWith('layout')) return false;
//     if (type.startsWith('table_space')) return false;
//     if (type.startsWith('tblstats')) return false;
//     if (type.startsWith('table_ui')) return false;
//     if (type.startsWith('app_data')) return false;
//     if (type.startsWith('ReadoutCntlr')) return false;
//     return window.enableFireflyReduxLogging;
// }

function logFilter(getState,action) {
    return window.enableFireflyReduxLogging;
}


function collapsedFilter(getState,action) {
    return collapsedLogging.includes(action.type);
}


// eslint-disable-next-line
const logger= createLogger({duration:true, predicate:logFilter, collapsed:collapsedFilter}); // developer can add for debugging


function createRedux() {
    // create a rootReducer from all of the registered reducers
    const rootReducer = combineReducers(reducers);
    const sagaMiddleware = createSagaMiddleware();
    const middleWare=  applyMiddleware(thunkMiddleware, logger, sagaMiddleware); //todo: turn off action logging
    const store = createStore(rootReducer, middleWare);
    sagaMiddleware.run(masterSaga);
    return store;
}

function startCoreSagas() {
    dispatchAddSaga( imagePlotter);
    dispatchAddSaga( watchReadout);
    dispatchAddSaga( watchForRelatedActions);
    dispatchAddSaga( watchExtensionActions);
}

function bootstrap() {
    if (redux === null) {
        redux = createRedux();
        startCoreSagas();
    }
    return Promise.resolve('success');
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

