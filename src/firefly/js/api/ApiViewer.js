/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * @public
 * @summary Build the interface to remotely communicate to the firefly viewer
 */
import {take} from 'redux-saga/effects';
import {isArray, get} from 'lodash';

import {debug} from './ApiUtil.js';
import {getRootURL}  from '../util/BrowserUtil.js';
import {dispatchRemoteAction}  from '../rpc/PushServices.js';
import {dispatchPlotImage}  from '../visualize/ImagePlotCntlr.js';
import {RequestType}  from '../visualize/RequestType.js';
import {clone, logError}  from '../util/WebUtil.js';
import {confirmPlotRequest,findInvalidWPRKeys}  from '../visualize/WebPlotRequest.js';
import {dispatchTableSearch, dispatchTableFetch}  from '../tables/TablesCntlr.js';
import {dispatchChartAdd} from '../charts/ChartsCntlr.js';
import {SCATTER, HISTOGRAM} from '../charts/ChartUtil.js';
import {DT_XYCOLS} from '../charts/dataTypes/XYColsCDT.js';
import {DT_HISTOGRAM} from '../charts/dataTypes/HistogramCDT.js';
import {makeFileRequest}  from '../tables/TableUtil.js';
import {makeXYPlotParams, makeHistogramParams, uniqueChartId} from '../charts/ChartUtil.js';
import {getWsChannel, getWsConnId} from '../core/messaging/WebSocketClient.js';
import {getConnectionCount, WS_CONN_UPDATED, GRAB_WINDOW_FOCUS} from '../core/AppDataCntlr.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {DEFAULT_FITS_VIEWER_ID} from '../visualize/MultiViewCntlr.js';

const VIEWER_ID = '__viewer';
var viewerWindow;

/**
 * @returns {{getViewer: getViewer, getExternalViewer: getExternalViewer}}
 * @ignore
 */
export function buildViewerApi() {
    return {getViewer,getExternalViewer};
}

/**
 *
 * @public
 * @param {string} [channel] the channel id string, if not specified then one will be generated
 * @param file the html of the viewer to launch. In time there will be several
 * @return {object} viewer interface @link{firefly.ApiViewer}
 * @memberof firefly
 *
 */
export function getViewer(channel= getWsChannel(),file='') {
    channel += VIEWER_ID;
    const dispatch= (action) => dispatchRemoteAction(channel,action);

    /**
     * The interface to remotely communicate to the firefly viewer.
     * @public
     * @namespace firefly.ApiViewer
     */
    return Object.assign({dispatch, channel},
                          buildImagePart(channel,file,dispatch),
                          buildTablePart(channel,file,dispatch),
                          buildChartPart(channel,file,dispatch)
    );
}

/**
 *
 * @deprecated
 * @memberof firefly
 * @ignore
 */
function getExternalViewer() {
    debug('getExternalViewer is deprecated, use firefly.getViewer() instead');
    return getViewer();
}



function buildImagePart(channel,file,dispatch) {

    var defP= {};

    /**
     * @summary set the default params the will be add to image plot request
     * @param params
     * @memberof firefly.ApiViewer
     * @public
     */
    const setDefaultParams= (params)=> defP= params;

    /**
     * @summary show a image in the firefly viewer in another tab
     * @param request Web plot request
     * @memberof firefly.ApiViewer
     * @public
     */
    const showImage= (request) => {
        doViewerOperation(channel,file, () => {
            if (isArray(request)) {
                request= request.map( (r) => clone(r,defP));
            }
            else {
                request= clone(request,defP);
            }
            plotRemoteImage(request,dispatch);
        });
    };

    /**
     * @summary show a image in the firefly viewer in another tab, the the file first then the url
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


    return {showImage,showImageFileOrUrl,setDefaultParams, plot,plotURL,plotFile, plotFileOrURL};
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
            dispatchTableFetch(request, hlRowIdx, dispatch);
        });
    };

    return {showTable, fetchTable};
}

function buildChartPart(channel,file,dispatch) {

    /**
     * @summary Show XY Plot
     * @param {XYPlotOptions} xyPlotOptions
     * @memberof firefly.ApiViewer
     * @public
     */
    const showXYPlot= (xyPlotOptions) => {
        doViewerOperation(channel, file, () => {
            plotRemoteXYPlot(xyPlotOptions, dispatch);
        });
    };

    /**
     * @summary Show Histogram
     * @param {HistogramOptions} histogramOptions
     * @memberof firefly.ApiViewer
     * @public
     */
    const showHistogram= (histogramOptions) => {
        doViewerOperation(channel, file, () => {
            plotRemoteHistogram(histogramOptions, dispatch);
        });
    };

    return {showXYPlot, showHistogram};
}


function doViewerOperation(channel,file,f) {
    const cnt = getConnectionCount(channel);
    if (cnt > 0) {
        if (viewerWindow){
            viewerWindow.focus();
        } else {
            dispatchRemoteAction(channel, {type:GRAB_WINDOW_FOCUS});
        }
        f && f();
    } else {
        dispatchAddSaga(doOnWindowConnected, {channel, f});
        const url= `${getRootURL()}${file};wsch=${channel}`;
        viewerWindow = window.open(url, channel);
    }
}

export function* doOnWindowConnected({channel, f}) {
    var isLoaded = false;
    while (!isLoaded) {
        const action = yield take([WS_CONN_UPDATED]);
        const cnt = get(action, ['payload', channel, 'length'], 0);
        isLoaded = cnt > 0;
    }
    f && f();
}


//================================================================
//---------- Private XYPlot functions
//================================================================

/**
 * @param {XYPlotOptions} params - XY plot parameters
 * @param {Function} dispatch - dispatch function
 */
function plotRemoteXYPlot(params, dispatch) {
    const xyPlotParams = makeXYPlotParams(params);
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
            dispatchTableFetch(searchRequest, 0, dispatch);
        } else {
            logError('Either tbl_id or source must be specified in the parameters');
            return;
        }
    }
    const chartId = uniqueChartId();
    // SCATTER
    dispatchChartAdd({chartId, chartType: SCATTER, groupId: 'default',
        chartDataElements: [
            {
                type: DT_XYCOLS,
                options: xyPlotParams,
                tblId
            }
        ],
        dispatcher: dispatch});
}

/**
 * @param {HistogramOptions} params - histogram parameters
 * @param {Function} dispatch - dispatch function
 */
function plotRemoteHistogram(params, dispatch) {
    const histogramParams = makeHistogramParams(params);
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
            dispatchTableFetch(searchRequest, 0, dispatch);
        } else {
            logError('Either tbl_id or source must be specified in the parameters');
            return;
        }
    }
    const chartId = uniqueChartId();
    // HISTOGRAM
    dispatchChartAdd({chartId, chartType: HISTOGRAM, groupId: 'default',
        chartDataElements: [
            {
                type: DT_HISTOGRAM,
                options: histogramParams,
                tblId
            }
        ],
        dispatcher: dispatch});
}

//================================================================
//---------- Private Table functions
//================================================================


//================================================================
//---------- Private Image functions
//================================================================



function plotRemoteImage(request, dispatch) {

    const testR= Array.isArray(request) ? request : [request];
    testR.forEach( (r) => {
        const badList= findInvalidWPRKeys(r);
        if (badList.length) debug(`plot request has the following bad keys: ${badList}`);
    });

    request= confirmPlotRequest(request,{},'remoteGroup',makePlotId);
    dispatchPlotImage({wpRequest:request, viewerId:DEFAULT_FITS_VIEWER_ID, dispatcher:dispatch});
}

var plotCnt= 0;

function makePlotId() {
    plotCnt++;
    return `apiPlot-${getWsConnId()}-${plotCnt}`;
}
