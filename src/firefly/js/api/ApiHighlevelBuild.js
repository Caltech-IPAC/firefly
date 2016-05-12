/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

//===================================================================================
//-----------------------------------------------------------------------------------
// Build the firefly high level api
// This file should have has no imports.  It should be build the high level api completely from the lowlevel.
// There are two reasons for this.
//   1. It will be an example of how to use the lowlevel api
//   2. We want to make sure the the lowlevel api is only built with the high level.  That
//      way we know that the lowlevel is complete.
//-----------------------------------------------------------------------------------
//===================================================================================

/**
 * 
 * @param llApi the lowlevel api
 * @return {Object}
 */
export function buildHighLevelApi(llApi) {
    const current= build(llApi);
    const deprecated= buildDeprecated(llApi);

    return Object.assign({}, deprecated, current);
}


var globalImageViewDefParams= {};

/**
 * Build the deprecated API
 * @param llApi
 * @return {Object}
 */
function build(llApi) {

    const imagePart= buildImagePart(llApi);
    const xyPart= buildXYandHistPart(llApi);
    const tablePart= buildTablePart(llApi);
    return Object.assign({}, imagePart,xyPart,tablePart);
}


function buildTablePart(llApi) {
    //todo
    return {};
}

function buildXYandHistPart(llApi) {
    //todo
    return {};
}


function buildImagePart(llApi) {

    const {RequestType}= llApi.util.image;

    /**
     * The general plotting function to plot a FITS image.
     * @param {String|div} targetDiv to put the image in.
     * @param {Object} request the request object literal with the plotting parameters
     * @namespace firefly
     */
    const showImage= (targetDiv, request)  => showImageInMultiViewer(llApi, targetDiv, request);


    /**
     * a convenience plotting function to plot a file on the server or a url.  If first looks for the file then
     * the url is the fallback
     * @param {String|div} targetDiv to put the image in.
     * @param {String} file file on server
     * @param {String} url url reference to a fits file
     * @namespace firefly
     */
    const showImageFileOrUrl= (targetDiv, file,url) =>
              showImageInMultiViewer(llApi, targetDiv,
                                           {'File' : file,
                                            'URL' : url,
                                            'Type' : RequestType.TRY_FILE_THEN_URL
                                           });

    /**
     * set global fallback params for every image plotting call
     * @param {Object} params a object literal such as any image plot or showImage uses
     * @namespace firefly
     */
    const setGlobalImageDef= (params) => globalImageViewDefParams= params;

    /**
     * Sets the root path for any relative URL. If this method has not been called then relative URLs use the page's root.
     * @param {String} rootUrlPath
     * @namespace firefly
     */
    const setRootPath= (rootUrlPath) => llApi.action.dispatchRootUrlPath(rootUrlPath);

    return {showImage, showImageFileOrUrl, setGlobalImageDef, setRootPath};
}

/**
 * Build the deprecated API
 * @param llApi
 * @return {Object}
 * @Deprecated
 */
function buildDeprecated(llApi) {

    const dApi= {};
    const {RequestType}= llApi.util.image;

    dApi.makeImageViewer= (plotId) => {
        llApi.util.debug('makeImageViewer is deprecated, use firefly.showImagePlot() instead');
        highlevelImageInit(llApi);
        const plotSimple= makePlotSimple(llApi,plotId);
        return {
            defP: {},
            setDefaultParams(params) {
                this.defP= params;
            },

            plot(request) {
                plotSimple(Object.assign({},this.defP, request));
            },

            plotFile(file) {
                plotSimple(Object.assign({},this.defP,{'File' : file}));
            },

            plotURL(url) {
                plotSimple(Object.assign({},this.defP,{'URL' : url}));
            },

            plotFileOrURL(file,url) {
                plotSimple(Object.assign({},this.defP,
                              {'File' : file,
                               'URL' : url,
                               'Type' : RequestType.TRY_FILE_THEN_URL
                               }));
            }
        };
    };
    dApi.setGlobalDefaultParams= (params) => globalImageViewDefParams= params;


    //!!!!!!! Add more Deprecated Api for table, histogram, xyplot here


    return dApi;
}




//================================================================
//---------- Private Image functions
//================================================================

function makePlotSimple(llApi, plotId) {
    return (request) => {
        llApi.util.renderDOM(plotId, llApi.ui.ImageViewer, {plotId});
        llApi.action.dispatchPlotImage(plotId, Object.assign({}, globalImageViewDefParams,request));
    };
}

function showImageInMultiViewer(llApi, targetDiv, request) {
    const {dispatchPlotImage, dispatchAddViewer, dispatchAddImages}= llApi.action;
    const {renderDOM}= llApi.util;
    const {MultiImageViewer, MultiViewStandardToolbar}= llApi.ui;

    highlevelImageInit(llApi);
    request= confirmPlotRequest(request,globalImageViewDefParams,targetDiv);

    const plotId= !Array.isArray(request) ? request.plotId : request.find( (r) => r.plotId).plotId;
    dispatchPlotImage(plotId, request, Array.isArray(request));
    dispatchAddViewer(targetDiv,true);
    dispatchAddImages(targetDiv, [plotId]);
    renderDOM(targetDiv, MultiImageViewer,
        {viewerId:targetDiv, canReceiveNewPlots:true, Toolbar:MultiViewStandardToolbar });

}


var imageInit= false;
function highlevelImageInit(llApi) {
    if (!imageInit) {
        llApi.util.image.initImageViewExpanded(llApi.ui.ApiExpandedDisplay);
        llApi.util.image.initAutoReadout();
        imageInit= true;
    }
}


function confirmPlotRequest(request,global,targetDiv) {
    if (Array.isArray(request)) {
        var locked= true;
        const idx= request.findIndex( (r) => r.plotId);

        const plotId= (idx>=0) ? request[idx].plotId : makePlotId();
        var plotGroupId;

        if (idx>=0  && request[idx].plotGroupId) {
            plotGroupId= request[idx].plotGroupId;
            locked= true;
        }
        else {
            plotGroupId= targetDiv;
            locked= false;
        }


        return request.map( (r) => Object.assign({},global,r,{plotId,plotGroupId,GroupLocked:locked}));
    }
    else {
        const r= Object.assign({}, global, request);
        if (!r.plotId) r.plotId= makePlotId();
        if (!r.plotGroupId) r.plotGroupId= targetDiv;
        if (r.plotGroupId===targetDiv) r.GroupLocked= false;
        return r;
    }
}


var plotCnt= 0;

function makePlotId() {
    plotCnt++;
    return 'apiPlot-'+plotCnt;
}

//================================================================
//---------- Private Table functions
//================================================================




//================================================================
//---------- Private XYPlot or Histogram functions
//================================================================

