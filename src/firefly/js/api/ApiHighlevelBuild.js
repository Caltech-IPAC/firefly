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

    const commonPart= buildCommon(llApi);
    const imagePart= buildImagePart(llApi);
    const chartPart= buildChartPart(llApi);
    const tablePart= buildTablePart(llApi);
    return Object.assign({}, commonPart, imagePart,chartPart,tablePart);
}


function buildTablePart(llApi) {
    //todo
    return {};
}

function buildChartPart(llApi) {
    //todo

    /**
     * The general plotting function to plot a FITS image.
     * @param {String|div} targetDiv to put the chart in.
     * @param {Object} parameters the request object literal with the chart parameters
     * @namespace firefly
     */
    const showPlot= (targetDiv, parameters)  => showXYPlot(llApi, targetDiv, parameters);

    return {showPlot};
}

function buildCommon(llApi) {
    /**
     * Sets the root path for any relative URL. If this method has not been called then relative URLs use the page's root.
     * @param {String} rootUrlPath
     * @namespace firefly
     */
    const setRootPath= (rootUrlPath) => llApi.action.dispatchRootUrlPath(rootUrlPath);

    return {setRootPath};
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

    return {showImage, showImageFileOrUrl, setGlobalImageDef};
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
        llApi.action.dispatchPlotImage({plotId, wpRequest:Object.assign({}, globalImageViewDefParams,request)});
    };
}

function showImageInMultiViewer(llApi, targetDiv, request) {
    const {dispatchPlotImage, dispatchAddViewer, dispatchAddImages}= llApi.action;
    const {findInvalidWPRKeys,confirmPlotRequest}= llApi.util.image;
    const {renderDOM,debug}= llApi.util;
    const {MultiImageViewer, MultiViewStandardToolbar}= llApi.ui;

    highlevelImageInit(llApi);
    const testR= Array.isArray(request) ? request : [request];
    testR.forEach( (r) => {
        const badList= findInvalidWPRKeys(r);
        if (badList.length) debug(`plot request has the following bad keys: ${badList}`);
    });
    request= confirmPlotRequest(request,globalImageViewDefParams,targetDiv,makePlotId);

    const plotId= !Array.isArray(request) ? request.plotId : request.find( (r) => r.plotId).plotId;
    dispatchAddViewer(targetDiv,true);
    dispatchPlotImage({plotId, wpRequest:request, viewerId:targetDiv});
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

function showXYPlot(llApi, targetDiv, params) {
    const {dispatchSetupTblTracking, dispatchTableFetch,dispatchLoadPlotData}= llApi.action;
    const {renderDOM}= llApi.util;
    const {ChartsTableViewPanel}= llApi.ui;
    const {xCol, yCol, xyRatio, stretch, xLabel, yLabel, xUnit, yUnit, xOptions, yOptions} = params;
    const xyPlotParams = {
        xyRatio,
        stretch,
        x : { columnOrExpr : xCol, label : xLabel, unit : xUnit, options : xOptions},
        y : { columnOrExpr : yCol, label : yLabel, unit : yUnit, options : yOptions}
    };
    const tblId = `tblid-${targetDiv}`;

    const searchRequest = {
        tbl_id : tblId,
        id:'IpacTableFromSource',
        source: params.source,
        pageSize: 0,
        title:  params.chartTitle,
        META_INFO: {tbl_id: tblId, title: 'Chart Test'}
    };

    dispatchSetupTblTracking(tblId);
    dispatchTableFetch(searchRequest);
    dispatchLoadPlotData(xyPlotParams, searchRequest);

    renderDOM(targetDiv, ChartsTableViewPanel,
        {
            key: `${targetDiv}-chart`,
            tblId,
            closeable: false,
            optionsPopup: false,
            expandedMode: false
        });
}