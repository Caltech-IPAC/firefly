/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * @public
 * @summary Build the interface to remotely communicate to the firefly viewer
 * @module firefly
 */
import {take} from 'redux-saga/effects';
import {isArray, get} from 'lodash';

import {debug} from './ApiUtil.js';
import {getRootURL}  from '../util/BrowserUtil.js';
import {dispatchRemoteAction}  from '../rpc/PushServices.js';
import {dispatchPlotImage}  from '../visualize/ImagePlotCntlr.js';
import {RequestType}  from '../visualize/RequestType.js';
import {clone}  from '../util/WebUtil.js';
import {confirmPlotRequest,findInvalidWPRKeys}  from '../visualize/WebPlotRequest.js';
import {dispatchTableSearch, dispatchTableFetch}  from '../tables/TablesCntlr.js';
import {getWsChannel} from '../core/messaging/WebSocketClient.js';
import {getConnectionCount, WS_CONN_UPDATED, GRAB_WINDOW_FOCUS} from '../core/AppDataCntlr.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {DEFAULT_FITS_VIEWER_ID} from '../visualize/MultiViewCntlr.js';

const VIEWER_ID = '__viewer';
var viewerWindow;

/**
 *
 * @return {{getViewer: getViewer, getExternalViewer: getExternalViewer}}
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
 * @return {object} viewer interface
 * @memberof module:firefly
 *
 */
function getViewer(channel= getWsChannel(),file='') {
    channel += VIEWER_ID;
    const dispatch= (action) => dispatchRemoteAction(channel,action);

    return Object.assign({dispatch, channel},
                          buildImagePart(channel,file,dispatch),
                          buildTablePart(channel,file,dispatch),
                          buildXYPlotPart(channel,file,dispatch)
    );
}

/**
 *
 * @deprecated
 * @memberof module:firefly
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
     * @memberof module:firefly
     * @public
     */
    const setDefaultParams= (params)=> defP= params;

    /**
     * @summary show a image in the firefly viewer in another tab
     * @param request Web plot request
     * @memberof module:firefly
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
     * @memberof module:firefly
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

function buildXYPlotPart(channel,file,dispatch) {
    // todo
    return {};
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
    return 'apiPlotRemote-'+plotCnt;
}
