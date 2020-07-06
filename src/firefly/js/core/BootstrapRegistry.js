/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {once} from 'lodash';
import {createLogger} from 'redux-logger';
import * as LayoutCntlr from './LayoutCntlr';
import ExternalAccessCntlr from './ExternalAccessCntlr';
import * as TableStatsCntlr from '../charts/TableStatsCntlr';
import ComponentCntlr, {DIALOG_OR_COMPONENT_KEY} from './ComponentCntlr';
import AppDataCntlr from './AppDataCntlr';
import BackgroundCntlr from './background/BackgroundCntlr';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr';
import FieldGroupCntlr, {addFieldGroupRelatedWatcher, MOUNT_COMPONENT} from '../fieldGroup/FieldGroupCntlr';
import MouseReadoutCntlr from '../visualize/MouseReadoutCntlr';
import TablesCntlr from '../tables/TablesCntlr';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr';
import ChartsCntlrDef from '../charts/ChartsCntlr';
import MultiViewCntlr from '../visualize/MultiViewCntlr';
import WorkspaceCntlr from '../visualize/WorkspaceCntlr';
import DataProductsCntlr from '../metaConvert/DataProductsCntlr';
import {showExampleDialog} from '../ui/ExampleDialog';
import DrawLayerFactory from '../visualize/draw/DrawLayerFactory';
import FixedMarker from '../drawingLayers/FixedMarker';
import SelectArea from '../drawingLayers/SelectArea';
import DistanceTool from '../drawingLayers/DistanceTool';
import PointSelection from '../drawingLayers/PointSelection';
import StatsPoint from '../drawingLayers/StatsPoint';
import NorthUpCompass from '../drawingLayers/NorthUpCompass';
import ImageRoot from '../drawingLayers/ImageRoot';
import SearchTarget from '../drawingLayers/SearchTarget';
import Catalog from '../drawingLayers/Catalog';
import Artifact from '../drawingLayers/Artifact';
import WebGrid from '../drawingLayers/WebGrid';
import RegionPlot from '../drawingLayers/RegionPlot';
import MarkerTool from '../drawingLayers/MarkerTool';
import FootprintTool from '../drawingLayers/FootprintTool';
import HiPSGrid from '../drawingLayers/HiPSGrid';
import HiPSMOC from '../drawingLayers/HiPSMOC';
import ImageOutline from '../drawingLayers/ImageOutline';
import ImageLineBasedFootprint from '../drawingLayers/ImageLineBasedFootprint';

//--- import Sagas
import {dispatchAddSaga, masterSaga} from './MasterSaga';
import {imagePlotter} from '../visualize/saga/ImagePlotter';
import {watchReadout} from '../visualize/saga/MouseReadoutWatch';
import {addExtensionWatcher} from './messaging/ExternalAccessWatcher';
import {applyMiddleware, combineReducers, createStore} from 'redux';
import createSagaMiddleware from 'redux-saga';
import thunkMiddleware from 'redux-thunk';


const USE_LOGGING_MIDDLEWARE= false; // logging middleware is useful for debugging but we don't use if much

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



export const getBootstrapRegistry= once(() => {
    const actionCreators = new Map();
    const sagaMiddleware = createSagaMiddleware();
    const reducers = {
        [LayoutCntlr.LAYOUT_PATH]: LayoutCntlr.reducer,
        [ExternalAccessCntlr.EXTERNAL_ACCESS_KEY]: ExternalAccessCntlr.reducer,
        [TableStatsCntlr.TBLSTATS_DATA_KEY]: TableStatsCntlr.reducer,
        [DIALOG_OR_COMPONENT_KEY]: ComponentCntlr.reducer
    };

    const drawLayerFactory= DrawLayerFactory.makeFactory(FixedMarker, SelectArea,DistanceTool,
        PointSelection, StatsPoint, NorthUpCompass, ImageRoot, SearchTarget, Catalog, Artifact, WebGrid,
        RegionPlot, MarkerTool, FootprintTool, HiPSGrid, HiPSMOC, ImageOutline, ImageLineBasedFootprint);


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
        dispatchAddSaga( imagePlotter);
        dispatchAddSaga( watchReadout);
        addFieldGroupRelatedWatcher();
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
    registerCntlr(DrawLayerCntlr.getDrawLayerCntlrDef(drawLayerFactory));
    registerCntlr(ChartsCntlrDef);
    registerCntlr(MultiViewCntlr);
    registerCntlr(WorkspaceCntlr);
    registerCntlr(DataProductsCntlr);

    actionCreators.set(TableStatsCntlr.LOAD_TBL_STATS, TableStatsCntlr.loadTblStats);
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

    const logFilter= (getState,action) => {
        return window.enableFireflyReduxLogging;
    };

    const collapsedFilter= (getState,action) => {
        return collapsedLogging.includes(action.type);
    };
    // eslint-disable-next-line
    return createLogger({duration:true, predicate:logFilter, collapsed:collapsedFilter}); // developer can add for debugging
}
