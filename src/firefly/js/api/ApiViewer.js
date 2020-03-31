/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * @public
 * @summary Build the interface to remotely communicate to the firefly viewer
 */
import {isArray, get, set} from 'lodash';
import Enum from 'enum';

import {WSCH} from '../core/History.js';
import {debug} from './ApiUtil.js';
import {getRootURL}  from '../util/BrowserUtil.js';
import {dispatchRemoteAction}  from '../core/JsonUtils.js';
import {dispatchPlotImage, dispatchPlotHiPS}  from '../visualize/ImagePlotCntlr.js';
import {RequestType}  from '../visualize/RequestType.js';
import {clone, logError, hashCode, Logger}  from '../util/WebUtil.js';
import {confirmPlotRequest,findInvalidWPRKeys}  from '../visualize/WebPlotRequest.js';
import {dispatchTableSearch, dispatchTableFetch}  from '../tables/TablesCntlr.js';
import {dispatchChartAdd} from '../charts/ChartsCntlr.js';
import {makeFileRequest}  from '../tables/TableRequestUtil.js';
import {uniqueChartId} from '../charts/ChartUtil.js';
import {getWsChannel, getWsConnId} from '../core/AppDataCntlr.js';
import {getConnectionCount, WS_CONN_UPDATED, GRAB_WINDOW_FOCUS,
    NOTIFY_REMOTE_APP_READY, makeViewerChannel} from '../core/AppDataCntlr.js';
import {dispatchAddCell, dispatchEnableSpecialViewer, LO_VIEW} from '../core/LayoutCntlr.js';
import {modifyURLToFull} from '../util/BrowserUtil.js';
import {DEFAULT_FITS_VIEWER_ID, DEFAULT_PLOT2D_VIEWER_ID} from '../visualize/MultiViewCntlr.js';
import {REINIT_APP} from '../core/AppDataCntlr.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga';

const logger = Logger('ApiViewer');

export const ViewerType= new Enum([
    'TriView',  // use what it in the title
    'Grid' // use the plot description key
], { ignoreCase: true });


let viewerWindow;

let defaultViewerFile='';
let defaultViewerType=ViewerType.TriView;




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
        if (viewerType===ViewerType.Grid) {
            htmlFile= 'slate.html';
        }
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
        channel = makeViewerChannel(channel || getWsChannel());
        const dispatch= (action) => dispatchRemoteAction(channel,action);
        const reinitViewer= () => dispatch({ type: REINIT_APP, payload: {}});

        /**
         * The interface to remotely communicate to the firefly viewer.
         * @public
         * @namespace firefly.ApiViewer
         */
        const viewer= Object.assign({dispatch, reinitViewer, channel},
            buildImagePart(channel,file,dispatch),
            buildTablePart(channel,file,dispatch),
            buildChartPart(channel,file,dispatch),
            buildUtilPart(channel,file),
        );


        switch (defaultViewerType) { // add any additional API
            case ViewerType.TriView:
                return viewer;
            case ViewerType.Grid:
                return Object.assign({}, viewer, buildSlateControl(channel,file,dispatch));
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
    const openViewer= () => {
        doViewerOperation(channel,file);
    };

    return {openViewer};
}

function buildSlateControl(channel,file,dispatcher) {



    /**
     *
     * @param {number} row
     * @param {number} col
     * @param {number} width
     * @param {number} height
     * @param {LO_VIEW} type must be 'tables', 'images' or 'xyPlots' (use 'xyPlots' for histograms)
     * @param {string} cellId
     */
    const addCell= (row, col, width, height, type, cellId) => {

        doViewerOperation(channel,file, () => {
            if (LO_VIEW.get(type)===LO_VIEW.tables) {
                if (cellId!=='main') debug('for tables type is force to be "main"');
                cellId= 'main';
            }
            dispatchAddCell({row,col,width,height,type, cellId,dispatcher});
        });


    };


    // const enableSpecialViewer= (viewerType, cellId= undefined, tableGroup= 'main') =>
    //     dispatchEnableSpecialViewer({viewerType, cellId:(cellId || `${viewerType}-${tableGroup}`), dispatcher});


    /**
     *
     * @param {string} cellId  cell id to add to
     * @param {string} [tableGroup] tableGroup to connect to, currently only main supported
     */
    const showCoverage = (cellId, tableGroup= 'main') => {
        doViewerOperation(channel,file, () => {
            dispatchEnableSpecialViewer({viewerType:LO_VIEW.coverageImage,
                cellId:(cellId || `${LO_VIEW.coverageImage}-${tableGroup}`),
                dispatcher});
        });
    };

    /**
     *
     * @param {string} cellId  cell id to add to
     * @param {string} [tableGroup] tableGroup to connect to, currently only main supported
     */
    const showImageMetaDataViewer = (cellId, tableGroup= 'main') => {
        doViewerOperation(channel,file, () => {
            dispatchEnableSpecialViewer({
                viewerType: LO_VIEW.tableImageMeta,
                cellId: (cellId || `${LO_VIEW.tableImageMeta}-${tableGroup}`),
                dispatcher
            });
        });
    };

    return {addCell, showCoverage, showImageMetaDataViewer};

}


function buildImagePart(channel,file,dispatch) {

    let defP= {};

    /**
     * @summary set the default params the will be add to image plot request
     * @param params
     * @memberof firefly.ApiViewer
     * @public
     */
    const setDefaultParams= (params)=> defP= params;

    /**
     * @summary show a image in the firefly viewer in another tab
     * @param {WebPlotParams|WebPlotRequest} request The object contains parameters for web plot request
     * @param {String} viewerId
     * @memberof firefly.ApiViewer
     * @public
     */
    const showImage= (request, viewerId) => {
        doViewerOperation(channel,file, () => {
            if (isArray(request)) {
                request= request.map( (r) => clone(r,defP));
            }
            else {
                request= clone(request,defP);
            }
            plotRemoteImage(request,viewerId, dispatch);
        });
    };

    /**
     * @summary show a HiPS in the firefly viewer in another tab
     * @param {WebPlotParams|WebPlotRequest} request The object contains parameters for web plot request on HiPS type
     * @param {String} viewerId
     * @memberof firefly.ApiViewer
     * @public
     */
    const showHiPS= (request, viewerId) => {
        doViewerOperation(channel,file, () => {
            request= clone(request,defP);
            plotRemoteHiPS(request,viewerId, dispatch);
        });
    };

    /**
     * @summary show a image in the firefly viewer in another tab, the file first then the url
     * @param file a file on the server
     * @param url a url to a fits file
     * @memberof firefly.ApiViewer
     * @public
     */
    const showImageFileOrUrl= (file,url) => showImage({file, url, Type : RequestType.TRY_FILE_THEN_URL});


    //------- deprecated part ---------------------


    const doDepPlot= (request, oldCall, newCall) => {
        debug(`${oldCall} call it deprecated, use ${newCall} instead`);
        showImage(request);
    };

    const plot= (request) => doDepPlot(request,'plot','showImage');

    const plotURL= (url) =>  doDepPlot({url}, 'plotURL', `showImage({url: ${url} })`);
    const plotFile= (file) =>  doDepPlot({file}, 'plotFile', `showImage({url: ${file} })`);
    const plotFileOrURL= (file,url) => doDepPlot({file, url, Type : RequestType.TRY_FILE_THEN_URL},
                                                  'plotFileOrURL', 'showImageFileOrUrl');

    //------- End deprecated part ---------------------


    return {showImage,showHiPS, showImageFileOrUrl,setDefaultParams, plot,plotURL,plotFile, plotFileOrURL};
}


function buildTablePart(channel,file,dispatch) {

    /**
     *
     * @param {Object} request
     * @param {TblOptions} options
     * @memberof firefly.ApiViewer
     * @public
     */
    const showTable= (request, options)  => {
        doViewerOperation(channel,file, () => {
            dispatchTableSearch(request, options, dispatch);
        });
    };

    const fetchTable= (request, hlRowIdx) => {
        doViewerOperation(channel,file, () => {
            dispatchTableFetch(request, hlRowIdx, undefined, dispatch);
        });
    };

    return {showTable, fetchTable};
}

function buildChartPart(channel,file,dispatch) {

    /**
     * @summary Show a chart
     * @param {{chartId: string, data: array.object, layout: object}} options
     * @param {string} viewerId
     * @memberof firefly.ApiViewer
     * @public
     */
    const showChart= (options, viewerId) => {
        doViewerOperation(channel, file, () => {
            plotRemoteChart(options, viewerId, dispatch);
        });
    };



    /**
     * @summary Show XY Plot
     * @param {XYPlotOptions} xyPlotOptions
     * @param {string} viewerId
     * @memberof firefly.ApiViewer
     * @public
     */
    const showXYPlot= (xyPlotOptions, viewerId) => {
        doViewerOperation(channel, file, () => {
            plotRemoteXYPlot(xyPlotOptions, viewerId, dispatch);
        });
    };

    /**
     * @summary Show Histogram
     * @param {HistogramOptions} histogramOptions
     * @param {string} viewerId
     * @memberof firefly.ApiViewer
     * @public
     */
    const showHistogram= (histogramOptions, viewerId) => {
        doViewerOperation(channel, file, () => {
            plotRemoteHistogram(histogramOptions, viewerId, dispatch);
        });
    };

    return {showChart, showXYPlot, showHistogram};
}


function doViewerOperation(channel,file,f) {
    const cnt = getConnectionCount(channel);
    if (cnt > 0) {
        if (viewerWindow){
            viewerWindow.focus();
        } else {
            dispatchRemoteAction(channel, {type:GRAB_WINDOW_FOCUS});
        }
        f?.();
    } else {
        dispatchAddActionWatcher({
            callback:windowReadyWatcher, actions:[WS_CONN_UPDATED, NOTIFY_REMOTE_APP_READY], params:{channel,f}
        });
        const url= `${modifyURLToFull(file,getRootURL())}?${WSCH}=${channel}`;
        viewerWindow = window.open(url, channel);
        set(viewerWindow, 'firefly.options.RequireWebSocketUptime', true);
    }
}

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
 * @param {string} viewerId
 * @param {Function} dispatch - dispatch function
 */
function plotRemoteChart(params, viewerId, dispatch) {

    const dispatchParams= clone({
        groupId: viewerId || 'default',
        viewerId:viewerId || DEFAULT_PLOT2D_VIEWER_ID,
        chartId: params.chartId || uniqueChartId(),
        chartType: 'plot.ly',
        deletable: true,
        dispatcher: dispatch
    }, params);

    dispatchChartAdd(dispatchParams);
}


/**
 * @param {XYPlotOptions} params - XY plot parameters
 * @param {string} viewerId
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
            params = Object.assign({}, params, {tbl_id: tblId});
        } else {
            logError('Either tbl_id or source must be specified in the parameters');
            return;
        }
    }
    // SCATTER
    dispatchChartAdd({
        groupId: viewerId || 'default',
        viewerId:viewerId || DEFAULT_PLOT2D_VIEWER_ID,
        chartId: params.chartId || uniqueChartId(),
        chartType: 'scatter',
        params,
        deletable: true,
        dispatcher: dispatch});
}

/**
 * @param {HistogramOptions} params - histogram parameters
 * @param {string} viewerId
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
            params = Object.assign({}, params, {tbl_id: tblId});
        } else {
            logError('Either tbl_id or source must be specified in the parameters');
            return;
        }
    }
    // HISTOGRAM
    dispatchChartAdd({
        groupId: viewerId || 'default',
        viewerId:viewerId || DEFAULT_PLOT2D_VIEWER_ID,
        chartId: params.chartId || uniqueChartId(),
        chartType: 'histogram',
        params,
        deletable: true,
        dispatcher: dispatch});
}

//================================================================
//---------- Private Table functions
//================================================================


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
    dispatchPlotImage({wpRequest:request, viewerId:viewerId || DEFAULT_FITS_VIEWER_ID, dispatcher:dispatch});
}


function plotRemoteHiPS(request, viewerId, dispatch) {

    const badList= findInvalidWPRKeys(request);
    if (badList.length) debug(`HiPS request has the following bad keys: ${badList}`);

    request= confirmPlotRequest(request,{Type:'HiPS'},'remoteGroup',makePlotId);
    dispatchPlotHiPS({plotId:request.plotId, wpRequest:request,
                      viewerId:viewerId || DEFAULT_FITS_VIEWER_ID, dispatcher:dispatch});
}



let plotCnt= 0;

function makePlotId() {
    plotCnt++;
    return `apiPlot-${getWsConnId()}-${plotCnt}`;
}
