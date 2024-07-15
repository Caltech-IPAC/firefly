/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * @public
 * @summary Build the interface to remotely communicate to the firefly viewer
 */
import {isArray, get, set,isFunction} from 'lodash';
import Enum from 'enum';
import {WSCH} from '../core/History.js';
import {debug} from './ApiUtil.js';
import {dispatchRemoteAction}  from '../core/JsonUtils.js';
import {dispatchPlotImage, dispatchPlotHiPS}  from '../visualize/ImagePlotCntlr.js';
import {RequestType}  from '../visualize/RequestType.js';
import {hashCode, getRootURL, modifyURLToFull}  from '../util/WebUtil.js';
import {Logger}  from '../util/Logger.js';
import {confirmPlotRequest,findInvalidWPRKeys}  from '../visualize/WebPlotRequest.js';
import {dispatchTableSearch, dispatchTableFetch}  from '../tables/TablesCntlr.js';
import {dispatchChartAdd} from '../charts/ChartsCntlr.js';
import {makeFileRequest}  from '../tables/TableRequestUtil.js';
import {uniqueChartId} from '../charts/ChartUtil.js';
import {getWsChannel, getWsConnId, getConnectionCount, makeViewerChannel,
    REINIT_APP, WS_CONN_UPDATED, GRAB_WINDOW_FOCUS, NOTIFY_REMOTE_APP_READY} from '../core/AppDataCntlr.js';
import {dispatchAddCell, dispatchEnableSpecialViewer, LO_VIEW} from '../core/LayoutCntlr.js';
import {DEFAULT_FITS_VIEWER_ID, DEFAULT_PLOT2D_VIEWER_ID, PINNED_CHART_VIEWER_ID} from '../visualize/MultiViewCntlr.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';
import {WEB_API_CMD} from './WebApi.js';

const logger = Logger('ApiViewer');

/**
 * @typedef ViewerType
 * one of
 * @prop TriView
 * @prop Grid
 * @type {Enum}
 */
export const ViewerType= new Enum([
    'TriView',  // use what it in the title
    'Grid' // use the plot description key
], { ignoreCase: true });

let defaultViewerFile='';
let defaultViewerType=ViewerType.TriView;
const activeViewers = {};     // a map of active viewer windows keyed by channel

/**
 * @returns {{getViewer: function, getExternalViewer: function}}
 * @ignore
 */
export function buildViewerApi() {
    return {getViewer,setViewerConfig,ViewerType};
}


/**
 *
 * @param {Object} viewerType must be either ViewerType.TriView or ViewerType.Grid
 * @param {string} [htmlFile]
 */
export function setViewerConfig(viewerType, htmlFile= '') {
    if (!htmlFile) {
        if (viewerType===ViewerType.Grid) htmlFile= 'slate.html';
    }
    defaultViewerType= viewerType;
    defaultViewerFile= htmlFile;
}

/**
 * wrapper function to return the API's remote Viewer object.  This allow one firefly app to
 * gain access to another app's API.
 * To use this function, it should be loaded first.  @see loadRemoteApi
 * It cannot be implemented as async because the common use case for getViewer is to launch a new Tab/Window.
 * This will be blocked by browser's popup blocker if it's done asynchronously.
 * @param {String} [channel]  the channel id string, default to current connected channel.
 * @param {String} [file]     url path, or the html file to load.  Defaults to blank(index of the app).
 * @param {String} [scriptUrl]  url of the script to load.  When scriptUrl is not given, return the Viewer of the loaded app.
 * @returns {Object} the API's Viewer interface @link{firefly.ApiViewer}
 * @public
 * @memberof firefly
 */
export function getViewer(channel, file=defaultViewerFile, scriptUrl) {
    if (scriptUrl) {
        // requesting for a viewer that's different from the currently loaded app
        const getViewer = get(loadRemoteApi(scriptUrl), 'getViewer');
        return getViewer && getViewer(channel, file);
    } else {
        // return currently loaded app's Viewer
        channel = makeViewerChannel(channel || getWsChannel(), file);
        const dispatch= (action) => dispatchRemoteAction(channel,action);
        const reinitViewer= () => dispatch({ type: REINIT_APP, payload: {}});

        /**
         * The interface to remotely communicate to the firefly viewer.
         * @public
         * @namespace firefly.ApiViewer
         */
        const viewer= {dispatch, reinitViewer, channel,
            ...buildImagePart(channel,file,dispatch),
            ...buildTablePart(channel,file,dispatch),
            ...buildChartPart(channel,file,dispatch),
            ...buildUrlApiLaunchPart(channel,file, dispatch),
            ...buildUtilPart(channel,file),
            addCell: () => undefined //noop
        };


        switch (defaultViewerType) { // add any additional API
            case ViewerType.TriView:
                return viewer;
            case ViewerType.Grid:
                return {...viewer, ...buildSlateControl(channel,file,dispatch)};
            default:
                debug('Unknown viewer type: ${defaultViewerType}, returning TriView');
                return viewer;
        }
    }
}
/**
 * wrapper function to return the API's remote Viewer object.  This allow one firefly app to
 * gain access to another app's API.
 * To use this function, it should be loaded first.  @see loadRemoteApi
 * It cannot be implemented as async because the common use case for getViewer is to launch a new Tab/Window.
 * This will be blocked by browser's popup blocker if it's done asynchronously.
 * @param {object} p            parameter
 * @param {String} [p.channel]  default to current connected channel.
 * @param {String} [p.file]     url path, or the html file to load.  Defaults to blank(index of the app).
 * @param {String} [p.scriptUrl]  url of the script to load.  When scriptUrl is not given, return the Viewer of the loaded app.
 * @returns {Object} the API's getViewer object
 */
function getRemoteViewer({channel, file, scriptUrl}) {
    if (!scriptUrl) return getViewer(channel, file);

    const getViewer = get(loadRemoteApi(scriptUrl), 'getViewer');
    return getViewer && getViewer(channel, file);
}

export function loadRemoteApi(scriptUrl) {
    const frameId = 'id_' + hashCode(scriptUrl);
    let apiFrame = document.getElementById(frameId);
    console.log('apiFrame:' + apiFrame);
    if (!apiFrame) {
        apiFrame = document.createElement('iframe');
        apiFrame.id = frameId;
        apiFrame.style.display = 'none';
        apiFrame.style.width = '0px';
        apiFrame.style.height = '0px';
        document.body.appendChild(apiFrame);
        // apiFrame.onload = () => {}   this event is not fired in Safari when src is blank.  it's treated as synchronous.
            const myscript = apiFrame.contentDocument.createElement('script');
            myscript.type = 'text/javascript';
            myscript.src = scriptUrl;
            const headEl = apiFrame.contentDocument.getElementsByTagName('head')[0];
            headEl.appendChild(myscript);
    }
    return get(apiFrame, 'contentWindow.firefly');
}

function buildUtilPart(channel, file, dispatcher) {
    return {openViewer: () => doViewerOperation(channel,file)};
}

function buildSlateControl(channel,file,dispatcher) {
    const viewOp= makeViewerOp(channel,file);

    /**
     *
     * @param {number} row
     * @param {number} col
     * @param {number} width
     * @param {number} height
     * @param {LO_VIEW} type must be 'tables', 'images' or 'xyPlots' (use 'xyPlots' for histograms)
     * @param {string} cellId
     */
    const addCell= (row, col, width, height, type, cellId) => viewOp(() => {
            if (LO_VIEW.get(type)===LO_VIEW.tables) {
                if (cellId!=='main') debug('for tables type is force to be "main"');
                cellId= 'main';
            }
            dispatchAddCell({row,col,width,height,type, cellId,dispatcher});
        });

    /**
     *
     * @param {string} cellId  cell id to add to
     * @param {string} [tableGroup] tableGroup to connect to, currently only main supported
     */
    const showCoverage = (cellId, tableGroup= 'main') => viewOp(() =>
        dispatchEnableSpecialViewer({
            viewerType:LO_VIEW.coverageImage, cellId:(cellId || `${LO_VIEW.coverageImage}-${tableGroup}`), dispatcher}));


    /**
     *
     * @param {string} cellId  cell id to add to
     * @param {string} [tableGroup] tableGroup to connect to, currently only main supported
     */
    const showImageMetaDataViewer = (cellId, tableGroup= 'main') => viewOp(() => {
            dispatchEnableSpecialViewer({
                viewerType: LO_VIEW.tableImageMeta,
                cellId: (cellId || `${LO_VIEW.tableImageMeta}-${tableGroup}`),
                dispatcher
            });
        });


    return {addCell, showCoverage, showImageMetaDataViewer};
}

function buildUrlApiLaunchPart(channel,file, dispatch) {
    const urlApiLaunch= (paramStr='') => {
        const viewOp= makeViewerOp(channel,file);
        viewOp('launching url api', () => {
            dispatch({type:WEB_API_CMD, payload:{paramStr}});
        });
    };
    return {urlApiLaunch};
}


function buildImagePart(channel,file,dispatch) {
    const viewOp= makeViewerOp(channel,file);
    let defP= {};

    /**
     * @summary set the default params that will be added to image plot request
     * @param params
     * @memberof firefly.ApiViewer
     * @public
     */
    const setDefaultParams= (params)=> defP= params;

    /**
     * @summary show a image in the firefly viewer in another tab
     * @param {WebPlotParams|WebPlotRequest|Array.<WebPlotParams>} request The object contains parameters for web plot request
     * @param {string} viewerId - ignored in triview mode
     * @memberof firefly.ApiViewer
     * @public
     */
    const showImage= (request, viewerId) => viewOp('Loading Image Data...', () => {
            request= isArray(request) ? request.map( (r) => ({...r,...defP})) : {...request,...defP};
            plotRemoteImage(request,viewerId, dispatch);
        });

    /**
     * @summary show a HiPS in the firefly viewer in another tab
     * @param {WebPlotParams|WebPlotRequest} request The object contains parameters for web plot request on HiPS type
     * @param {string} viewerId - ignored in triview mode
     * @memberof firefly.ApiViewer
     * @public
     */
    const showHiPS= (request, viewerId) =>
        viewOp('Loading HiPS Data...', () => plotRemoteHiPS({...request,defP},viewerId, dispatch));

    /**
     * @summary show a image in the firefly viewer in another tab, the file first then the url
     * @param file a file on the server
     * @param url a url to a fits file
     * @memberof firefly.ApiViewer
     * @public
     */
    const showImageFileOrUrl= (file,url) => showImage({file, url, Type : RequestType.TRY_FILE_THEN_URL});

    return {showImage,showHiPS, showImageFileOrUrl,setDefaultParams};
}


function buildTablePart(channel,file,dispatch) {

    const viewOp= makeViewerOp(channel,file, 'Loading Table...');
    /**
     * @param {Object} request
     * @param {TblOptions} options
     * @memberof firefly.ApiViewer
     * @public
     */
    const showTable= (request, options)  => viewOp(() => dispatchTableSearch(request, options, dispatch));

    const fetchTable= (request, hlRowIdx) => viewOp(() => dispatchTableFetch(request, hlRowIdx, undefined, dispatch));

    return {showTable, fetchTable};
}

function buildChartPart(channel,file,dispatch) {
    const viewOp= makeViewerOp(channel,file, 'Loading Chart...');

    /**
     * @summary Show a chart
     * @param {{chartId: string, data: array.object, layout: object}} options
     * @param {string} viewerId - ignored in triview mode
     * @memberof firefly.ApiViewer
     * @public
     */
    const showChart= (options, viewerId) => viewOp(() => plotRemoteChart(options, viewerId, dispatch));

    /**
     * @summary Show XY Plot
     * @param {XYPlotOptions} xyPlotOptions
     * @param {string} viewerId - ignored in triview mode
     * @memberof firefly.ApiViewer
     * @public
     */
    const showXYPlot= (xyPlotOptions, viewerId) => viewOp(() => plotRemoteXYPlot(xyPlotOptions, viewerId, dispatch));

    /**
     * @summary Show Histogram
     * @param {HistogramOptions} histogramOptions
     * @param {string} viewerId - ignored in triview mode
     * @memberof firefly.ApiViewer
     * @public
     */
    const showHistogram= (histogramOptions, viewerId) => viewOp('Loading Histogram...', () =>
        plotRemoteHistogram(histogramOptions, viewerId, dispatch));

    return {showChart, showXYPlot, showHistogram};
}

const doViewerOperation= (() => {
    return (channel,file,initMsg, f) => {
        const cnt = getConnectionCount(channel);
        if (cnt > 0) {
            activeViewers[channel] ? activeViewers[channel]?.focus() : dispatchRemoteAction(channel, {type:GRAB_WINDOW_FOCUS});
            f?.();
        } else {
            dispatchAddActionWatcher({
                callback:windowReadyWatcher, actions:[WS_CONN_UPDATED, NOTIFY_REMOTE_APP_READY], params:{channel,f}
            });
            const url= `${modifyURLToFull(file,getRootURL())}?${WSCH}=${channel}`;
            const win = window.open(url, channel);
            activeViewers[channel] =  win;
            win.onclose = () => Reflect.deleteProperty(activeViewers, channel);
            set(win, 'firefly.options.RequireWebSocketUptime', true);
            initMsg && set(win, 'firefly.options.initLoadingMessage', initMsg);
        }
    };
})();


/**
 * return a wrapper function that calls doViewerOperation. The returned function may be called in one of two ways.
 * f(message,function) or f(function)
 * @param {string} channel
 * @param {string} file
 * @param {string} defMsg - the init loading message to pass to firefly
 * @return {function} a function called with-  f(message,function) or f(function)
 */
const makeViewerOp= (channel,file,defMsg='') => (msgOrFunc,func) =>
    isFunction(msgOrFunc) ?
        doViewerOperation(channel,file,defMsg,msgOrFunc) :
        doViewerOperation(channel,file,msgOrFunc, func);


export function windowReadyWatcher(action, cancelSelf, {channel, f, isLoaded= false, isReady=false}) {
    const {type,payload={}}=action;
    if (type===WS_CONN_UPDATED && payload[channel]?.length > 0) isLoaded = true;
    if (type===NOTIFY_REMOTE_APP_READY && payload.viewerChannel===channel) isReady= true;

    if (isLoaded && isReady) {
        cancelSelf();
        logger.debug('viewer ready');
        f?.();
    }
    return {channel,f,isLoaded,isReady};
}


//================================================================
//---------- Private Chart functions
//================================================================


/**
 * @param {{chartId: string, data: array.object, layout: object}} params - chart parameters
 * @param {string} viewerId - ignored in triview mode
 * @param {Function} dispatch - dispatch function
 */
function plotRemoteChart(params, viewerId=DEFAULT_PLOT2D_VIEWER_ID, dispatch) {
    dispatchChartAdd( {
                groupId: defaultViewerType===ViewerType.TriView ? PINNED_CHART_VIEWER_ID : viewerId || 'default',
                viewerId: defaultViewerType===ViewerType.TriView ? PINNED_CHART_VIEWER_ID : viewerId || DEFAULT_PLOT2D_VIEWER_ID,
                chartId: params.chartId || uniqueChartId(),
                chartType: 'plot.ly',
                deletable: true,
                dispatcher: dispatch,
                ...params}
    );
}


/**
 * @param {XYPlotOptions} params - XY plot parameters
 * @param {string} viewerId - ignored in triview mode
 * @param {Function} dispatch - dispatch function
 */
function plotRemoteXYPlot(params, viewerId, dispatch) {
    let tblId = params.tbl_id;
    if (!tblId) {
        const source = params.source;
        if (source) {
            const searchRequest = makeFileRequest(
                params.chartTitle||'', // title
                params.source,  // source
                null,  // alt_source
                {pageSize: 0} // table options
            );
            tblId = searchRequest.tbl_id;
            dispatchTableFetch(searchRequest, 0, undefined, dispatch);
            params = {...params, tbl_id: tblId};
        } else {
            logger.error('Either tbl_id or source must be specified in the parameters');
            return;
        }
    }
    // SCATTER
    dispatchChartAdd({
        groupId: defaultViewerType===ViewerType.TriView ? PINNED_CHART_VIEWER_ID : viewerId || 'default',
        viewerId: defaultViewerType===ViewerType.TriView ? PINNED_CHART_VIEWER_ID : viewerId || DEFAULT_PLOT2D_VIEWER_ID,
        chartId: params.chartId || uniqueChartId(),
        chartType: 'scatter',
        params,
        deletable: true,
        dispatcher: dispatch});
}

/**
 * @param {HistogramOptions} params - histogram parameters
 * @param {string} viewerId - ignored in triview mode
 * @param {Function} dispatch - dispatch function
 */
function plotRemoteHistogram(params, viewerId, dispatch) {
    let tblId = params.tbl_id;
    if (!tblId) {
        const source = params.source;
        if (source) {
            const searchRequest = makeFileRequest(
                params.chartTitle||'', // title
                params.source,  // source
                null,  // alt_source
                {pageSize: 0} // table options
            );
            tblId = searchRequest.tbl_id;
            dispatchTableFetch(searchRequest, 0, undefined, dispatch);
            params = {...params, tbl_id: tblId};
        } else {
            logger.error('Either tbl_id or source must be specified in the parameters');
            return;
        }
    }
    // HISTOGRAM
    dispatchChartAdd({
        groupId: defaultViewerType===ViewerType.TriView ? PINNED_CHART_VIEWER_ID : 'default',
        viewerId: defaultViewerType===ViewerType.TriView ? PINNED_CHART_VIEWER_ID : viewerId,
        chartId: params.chartId || uniqueChartId(),
        chartType: 'histogram',
        params,
        deletable: true,
        dispatcher: dispatch});
}

//================================================================
//---------- Private Image functions
//================================================================

function plotRemoteImage(request, viewerId, dispatch) {
    const testR= Array.isArray(request) ? request : [request];
    testR.forEach( (r) => {
        const badList= findInvalidWPRKeys(r);
        if (badList.length) debug(`plot request has the following bad keys: ${badList}`);
    });

    request= confirmPlotRequest(request,{},'remoteGroup',makePlotId);
    dispatchPlotImage({wpRequest:request,
        viewerId:defaultViewerType===ViewerType.TriView ? DEFAULT_FITS_VIEWER_ID : viewerId || DEFAULT_FITS_VIEWER_ID,
        dispatcher:dispatch});
}


function plotRemoteHiPS(request, viewerId, dispatch) {

    const badList= findInvalidWPRKeys(request);
    if (badList.length) debug(`HiPS request has the following bad keys: ${badList}`);

    request= confirmPlotRequest(request,{Type:'HiPS'},'remoteGroup',makePlotId);
    dispatchPlotHiPS({
        plotId:request.plotId,
        wpRequest:request,
        viewerId:defaultViewerType===ViewerType.TriView ? DEFAULT_FITS_VIEWER_ID : viewerId || DEFAULT_FITS_VIEWER_ID,
        dispatcher:dispatch});
}

const makePlotId = (() => {
    let plotCnt= 1;
    return () => `apiPlot-${getWsConnId()}-${plotCnt++}`;
})();
