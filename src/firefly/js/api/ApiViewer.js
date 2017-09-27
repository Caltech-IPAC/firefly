/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * @public
 * @summary Build the interface to remotely communicate to the firefly viewer
 */
import {take} from 'redux-saga/effects';
import {isArray, get} from 'lodash';
import Enum from 'enum';

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
import {dispatchAddCell, dispatchEnableSpecialViewer, LO_VIEW} from '../core/LayoutCntlr.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {modifyURLToFull} from '../util/BrowserUtil.js';
import {DEFAULT_FITS_VIEWER_ID} from '../visualize/MultiViewCntlr.js';
import {REINIT_APP} from '../core/AppDataCntlr.js';



export const ViewerType= new Enum([
    'TriView',  // use what it in the title
    'Grid', // use the plot description key
], { ignoreCase: true });


const VIEWER_ID = '__viewer';
let viewerWindow;

let defaultViewerFile='';
let defaultViewerType=ViewerType.TriView;




/**
 * @returns {{getViewer: getViewer, getExternalViewer: getExternalViewer}}
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
 *
 * @public
 * @param {string} [channel] the channel id string, if not specified then one will be generated
 * @param file the html of the viewer to launch. In time there will be several
 * @return {object} viewer interface @link{firefly.ApiViewer}
 * @memberof firefly
 *
 */
export function getViewer(channel= getWsChannel(),file=defaultViewerFile) {
    channel += VIEWER_ID;
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
    );


    // add anything else
    switch (defaultViewerType) {
        case ViewerType.TriView:
            return viewer;
            break;
        case ViewerType.Grid:
            return Object.assign({}, viewer, buildSlateControl(channel,file,dispatch));
            break;
        default:
            debug('Unknown viewer type: ${defaultViewerType}, returning TriView');
            return viewer;
            break;

    }
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
     * @param request Web plot request
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
            dispatchTableFetch(request, hlRowIdx, undefined, dispatch);
        });
    };

    return {showTable, fetchTable};
}

function buildChartPart(channel,file,dispatch) {

    /**
     * @summary Show a chart
     * @param {XYPlotOptions} xyPlotOptions
     * @param {string} viewerId
     * @memberof firefly.ApiViewer
     * @public
     */
    const showPlot= (xyPlotOptions, viewerId) => {
        doViewerOperation(channel, file, () => {
            plotRemotePlot(xyPlotOptions, viewerId, dispatch);
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

    return {showPlot, showXYPlot, showHistogram};
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
        // const url= `${getRootURL()}${file};wsch=${channel}`;
        const url= `${modifyURLToFull(file,getRootURL())};wsch=${channel}`;
        viewerWindow = window.open(url, channel);
    }
}

export function* doOnWindowConnected({channel, f}) {
    let isLoaded = false;
    while (!isLoaded) {
        const action = yield take([WS_CONN_UPDATED]);
        const cnt = get(action, ['payload', channel, 'length'], 0);
        isLoaded = cnt > 0;
    }
    // Added a half second delay before ready to combat a race condition
    // TODO: loi is going to look it it to determine if application it truely ready
    setTimeout(() => f && f(), 500);
}


//================================================================
//---------- Private XYPlot functions
//================================================================


/**
 * @param {XYPlotOptions} params - XY plot parameters
 * @param {string} viewerId
 * @param {Function} dispatch - dispatch function
 */
function plotRemotePlot(params, viewerId, dispatch) {

    const dispatchParams= clone({
        viewerId:viewerId || 'default',
        chartId: params.chartId || uniqueChartId(),
        chartType: 'plot.ly',
        closeable: true,
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
            dispatchTableFetch(searchRequest, 0, undefined, dispatch);
        } else {
            logError('Either tbl_id or source must be specified in the parameters');
            return;
        }
    }
    const chartId = uniqueChartId();
    // SCATTER
    dispatchChartAdd({chartId, chartType: SCATTER, groupId: viewerId || 'default',
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
 * @param {string} viewerId
 * @param {Function} dispatch - dispatch function
 */
function plotRemoteHistogram(params, viewerId, dispatch) {
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
            dispatchTableFetch(searchRequest, 0, undefined, dispatch);
        } else {
            logError('Either tbl_id or source must be specified in the parameters');
            return;
        }
    }
    const chartId = uniqueChartId();
    // HISTOGRAM
    dispatchChartAdd({chartId, chartType: HISTOGRAM, groupId: viewerId || 'default',
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



function plotRemoteImage(request, viewerId, dispatch) {

    const testR= Array.isArray(request) ? request : [request];
    testR.forEach( (r) => {
        const badList= findInvalidWPRKeys(r);
        if (badList.length) debug(`plot request has the following bad keys: ${badList}`);
    });

    request= confirmPlotRequest(request,{},'remoteGroup',makePlotId);
    dispatchPlotImage({wpRequest:request, viewerId:viewerId || DEFAULT_FITS_VIEWER_ID, dispatcher:dispatch});
}

let plotCnt= 0;

function makePlotId() {
    plotCnt++;
    return `apiPlot-${getWsConnId()}-${plotCnt}`;
}
