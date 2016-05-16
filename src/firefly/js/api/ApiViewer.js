/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {has, isArray} from 'lodash';
import {debug} from './ApiUtil.js';
import {getRootURL}  from '../util/BrowserUtil.js';
import {getCookie}  from '../util/WebUtil.js';
import {aliveCheck, dispatchRemoteAction}  from '../rpc/PushServices.js';
import {dispatchPlotImage}  from '../visualize/ImagePlotCntlr.js';
import {RequestType}  from '../visualize/RequestType.js';
import {clone}  from '../util/WebUtil.js';
import {confirmPlotRequest,findInvalidWPRKeys}  from '../visualize/WebPlotRequest.js';


/**
 * Build the interface to remotely communicate to the firefly viewer
 * @return {{getViewer: getViewer, getExternalViewer: getExternalViewer}}
 */
export function buildViewerApi() {
    return {getViewer,getExternalViewer};
}

function getDefChannel() {
    var root= getCookie('usrkey');
    if (root) {
        const idx= root.indexOf('/');
        if (idx>-1) root= root.substring(0,idx);
    }
    else {
        root= 'remote';
    }
    return `${root}-viewer`;
}

/**
 *
 * @param {string} [channel] the channel id string, if not specified then one will be generated
 * @param file the html of the viewer to launch. In time there will be several
 * @return {object} viewer interface
 */
function getViewer(channel= getDefChannel(),file='') {
    const dispatch= (action) => dispatchRemoteAction(channel,action);

    return Object.assign({dispatch},
                          buildImagePart(channel,file,dispatch),
                          buildTablePart(channel,file,dispatch),
                          buildXYPlotPart(channel,file,dispatch)
    );
}

/**
 *
 * @deprecated
 */
function getExternalViewer() {
    debug('getExternalViewer is deprecated, use firefly.getViewer() instead');
    return getViewer();
}



function buildImagePart(channel,file,dispatch) {

    var defP= {};

    /**
     * set the default params the will be add to image plot request
     * @param params
     */
    const setDefaultParams= (params)=> defP= params;

    /**
     * show a image in the firefly viewer in another tab
     * @param request Web plot request
     */
    const showImage= (request) => {
        doViewerOperation(channel,file, () => {
            if (isArray(request)) {
                request= request.map( (r) => clone(r,defP));
            }
            else {
                request= clone(request,defP);
            }
            plotRemoteImage(request,channel,dispatch);
        });
    };

    /**
     * show a image in the firefly viewer in another tab, the the file first then the url
     * @param file a file on the server
     * @param url a url to a fits file
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
    // todo
    return {};
}

function buildXYPlotPart(channel,file,dispatch) {
    // todo
    return {};
}



function doViewerOperation(channel,file,f) {
    aliveCheck(channel)
        .then( (result) => {
            if (result.activeCount) {
                f();
            }
            else {
                const url= `${getRootURL()}${file};wsch=${channel}`;
                window.open(url, channel);
                aliveCheck(channel,5000).then( (result) => {
                    if (result.activeCount) {
                        f();
                    }
                    else {
                        debug('Operation fail: Could not launch a new browser tab.');
                    }
                } );
            }
        });
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
    dispatchPlotImage({wpRequest:request, dispatcher:dispatch});
}


var plotCnt= 0;

function makePlotId() {
    plotCnt++;
    return 'apiPlotRemote-'+plotCnt;
}
