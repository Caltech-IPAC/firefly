/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {once} from 'lodash';
import {applyMiddleware, combineReducers, createStore} from 'redux';
import createSagaMiddleware from 'redux-saga';
import thunkMiddleware from 'redux-thunk';
import {createLogger} from 'redux-logger';
import HpxIndexCntlr from '../tables/HpxIndexCntlr.js';

import * as LayoutCntlr from './LayoutCntlr.js';
import ExternalAccessCntlr from './ExternalAccessCntlr.js';
import TableStatsCntlr from '../charts/TableStatsCntlr.js';
import ComponentCntlr, {DIALOG_OR_COMPONENT_KEY} from './ComponentCntlr.js';
import AppDataCntlr from './AppDataCntlr.js';
import BackgroundCntlr from './background/BackgroundCntlr.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import FieldGroupCntlr, {MOUNT_COMPONENT} from '../fieldGroup/FieldGroupCntlr.js';
import MouseReadoutCntlr from '../visualize/MouseReadoutCntlr.js';
import TablesCntlr from '../tables/TablesCntlr.js';
import HpxIndexCntrl from '../tables/HpxIndexCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import ChartsCntlrDef from '../charts/ChartsCntlr.js';
import MultiViewCntlr from '../visualize/MultiViewCntlr.js';
import WorkspaceCntlr from '../visualize/WorkspaceCntlr.js';
import DataProductsCntlr from '../metaConvert/DataProductsCntlr.js';
import {showExampleDialog} from '../ui/ExampleDialog.jsx';
import DrawLayerFactory from '../visualize/draw/DrawLayerFactory.js';
import FixedMarker from '../drawingLayers/FixedMarker.js';
import SelectArea from '../drawingLayers/SelectArea.js';
import DistanceTool from '../drawingLayers/DistanceTool.js';
import ExtractLineTool from '../drawingLayers/ExtractLineTool.js';
import PointSelection from '../drawingLayers/PointSelection.js';
import ExtractPoints from '../drawingLayers/ExtractPointsTool.js';
import SearchSelectTool from '../drawingLayers/SearchSelectTool.js';
import StatsPoint from '../drawingLayers/StatsPoint.js';
import NorthUpCompass from '../drawingLayers/NorthUpCompass.js';
import ImageRoot from '../drawingLayers/ImageRoot.js';
import SearchTarget from '../drawingLayers/SearchTarget.js';
import Catalog from '../drawingLayers/Catalog.js';
import HpxCatalog from '../drawingLayers/hpx/HpxCatalog.js';
import Artifact from '../drawingLayers/Artifact.js';
import WebGrid from '../drawingLayers/WebGrid.js';
import RegionPlot from '../drawingLayers/RegionPlot.js';
import MarkerTool from '../drawingLayers/MarkerTool.js';
import FootprintTool from '../drawingLayers/FootprintTool.js';
import HiPSGrid from '../drawingLayers/HiPSGrid.js';
import HiPSMOC from '../drawingLayers/HiPSMOC.js';
import ImageOutline from '../drawingLayers/ImageOutline.js';
import ImageLineBasedFootprint from '../drawingLayers/ImageLineBasedFootprint.js';

//--- import Sagas
import {dispatchAddSaga, masterSaga} from './MasterSaga.js';
import {watchReadout} from '../visualize/saga/MouseReadoutWatch.js';
import {addExtensionWatcher} from './messaging/ExternalAccessWatcher.js';


const USE_LOGGING_MIDDLEWARE= false; // logging middleware is useful for debugging but we don't use if much

/**
 * @global
 * @public
 * @typedef {Object} ApplicationState
 *
 * @prop {VisRoot} allPlots - image plotting store  (Controller: ImagePlotCntlr.js)
 * @prop {TableSpace} table_space - table data store (Controller: TablesCntlr.js)
 * @prop {HealpixTableIndex} HpxIndexCntlr- table data store (Controller: TablesCntlr.js)
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
 * @typedef {Object} BootstrapRegistry
 * Used for gathering all the reducers and drawing layers and starting redux
 *
 * @prop {Function} registerCntlr - register a controller
 * @prop {Function} createRedux - create the redux instance
 * @prop {Function} startCoreSagas - should only be called at init
 * @prop {Function} registerCreator - register an action creator
 * @prop {Function} registerReducer - register a reducer
 * @prop {Function} getActionCreators - get the action creators maps
 * @prop {Function} getDrawLayerFactory - get the drawing layer factory
 * @prop {Function} registerDrawLayer -  register a drawing layer
 * @prop {Function} setDrawLayerDefaults
 * @prop {Function} createDrawLayer
 *
 */


/**
 * Get the bootstrap registry, one the first call the bootstrap registry will initialize
 * @return {BootstrapRegistry}
 */
export const getBootstrapRegistry= once(() => {
    const actionCreators = new Map();
    const sagaMiddleware = createSagaMiddleware();
    const reducers = {
        [LayoutCntlr.LAYOUT_PATH]: LayoutCntlr.reducer,
        [ExternalAccessCntlr.EXTERNAL_ACCESS_KEY]: ExternalAccessCntlr.reducer,
        [DIALOG_OR_COMPONENT_KEY]: ComponentCntlr.reducer
    };

    const drawLayerFactory= DrawLayerFactory.makeFactory(
        FixedMarker, SelectArea,DistanceTool, ExtractLineTool, ExtractPoints,
        PointSelection, StatsPoint, NorthUpCompass, ImageRoot, SearchTarget, Catalog, HpxCatalog, Artifact, WebGrid,
        RegionPlot, MarkerTool, FootprintTool, SearchSelectTool,
        HiPSGrid, HiPSMOC, ImageOutline, ImageLineBasedFootprint);


    /**
     *  create a rootReducer from all of the registered reducers and set up middle ware
     * @return {Store<S>} the redux store
     */
    const createRedux= () => {
        const mArray= [thunkMiddleware, (USE_LOGGING_MIDDLEWARE && getLogger()),sagaMiddleware].filter( (e) => e);
        return createStore(combineReducers(reducers), applyMiddleware(...mArray));
    };

    /** start the core Saga */
    const startCoreSagas= () => {
        sagaMiddleware.run(masterSaga);
        dispatchAddSaga( watchReadout);
        addExtensionWatcher();
    };

    const registerCntlr= (cntlr = {}) => {
        cntlr.reducers && Object.entries(cntlr.reducers()).forEach(([k, v]) => reducers[k] = v);
        cntlr.actionCreators && Object.entries(cntlr.actionCreators()).forEach(([k, v]) => actionCreators.set(k, v));
    };

    // registering controllers...
    registerCntlr(AppDataCntlr);
    registerCntlr(BackgroundCntlr);
    registerCntlr(ImagePlotCntlr);
    registerCntlr(FieldGroupCntlr);
    registerCntlr(MouseReadoutCntlr);
    registerCntlr(ExternalAccessCntlr);
    registerCntlr(TablesCntlr);
    registerCntlr(HpxIndexCntlr);
    registerCntlr(DrawLayerCntlr.getDrawLayerCntlrDef(drawLayerFactory));
    registerCntlr(ChartsCntlrDef);
    registerCntlr(MultiViewCntlr);
    registerCntlr(WorkspaceCntlr);
    registerCntlr(DataProductsCntlr);
    registerCntlr(TableStatsCntlr);

    actionCreators.set('exampleDialog', (rawAction) => {
        showExampleDialog();
        return rawAction;
    });

    return {
        registerCntlr,
        createRedux,
        startCoreSagas,
        registerCreator: (actionCreator, ...types) => types && types.forEach( (v) => actionCreators.set(v, actionCreator) ),
        registerReducer: (dataRoot, reducer) => reducers[dataRoot] = reducer,
        getActionCreators: () => actionCreators,
        getDrawLayerFactory: () => drawLayerFactory,
        registerDrawLayer: (factoryDef) => drawLayerFactory.register(factoryDef),
        setDrawLayerDefaults: (typeId,defaults) => drawLayerFactory.setDrawLayerDefaults(typeId,defaults),
        createDrawLayer: (drawLayerTypeId, params) => drawLayerFactory.create(drawLayerTypeId,params),
    };
});


/**
 * This function will setup the redux logging middle ware.
 * @return {Function}
 */
function getLogger() {
    // object with a key that can be filtered out, value should be a boolean or a function that returns a boolean
    // eslint-disable-next-line
    const filterOutOfLogging= {
        [ExternalAccessCntlr.EXTENSION_ACTIVATE]: (action) => !action.payload.extension || action.payload.extension.extType!=='PLOT_MOUSE_READ_OUT',
        [MOUNT_COMPONENT]: false
    };

    // array of action types that will be logged as collapsed
    const collapsedLogging= [ ExternalAccessCntlr.EXTENSION_ACTIVATE ];

    window.enableFireflyReduxLogging= false;


    /**
     * Can be used for debugging.  Adjust content of filter function to suit your needs
     * @param getState
     * @param action
     * @return {boolean}
     */
    const logFilterALT= (getState,action) => {
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
        if (type.startsWith('ReadoutCntlr')) return false;
        return window.enableFireflyReduxLogging;
    };

    const logFilter= () => {
        return window.enableFireflyReduxLogging;
    };

    const collapsedFilter= (getState,action) => {
        return collapsedLogging.includes(action.type);
    };
    // eslint-disable-next-line
    return createLogger({duration:true, predicate:logFilter, collapsed:collapsedFilter}); // developer can add for debugging
}
