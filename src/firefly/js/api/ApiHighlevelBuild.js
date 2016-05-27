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

/*----------------------------< TABLE PART ----------------------------*/

function oldApi(llApi, params, options) {
    const {getBoolean} = llApi.util;
    const {makeFileRequest} = llApi.util.table;

    const oldOpts = params.tableOptions && params.tableOptions.split(',').reduce((rval, s) => {
                        const kval = s && s.trim().split('=');
                        rval[kval[0]] = kval[1];
                        return rval;
                    }, {});      // convert 'key=value' array into {key: value}.
    options.showFilters = getBoolean(oldOpts, 'show-filter');
    options.showTitle = getBoolean(oldOpts, 'show-title');
    options.showToolbar = getBoolean(oldOpts, 'show-toolbar');
    options.showOptionButton = getBoolean(oldOpts, 'show-options');
    options.showPaging = getBoolean(oldOpts, 'show-paging');
    options.showSave = getBoolean(oldOpts, 'show-save');
    options.showUnits = getBoolean(oldOpts, 'show-units');
    // options.?? = getBoolean(oldOpts, 'show-popout');
    // options.?? = getBoolean(oldOpts, 'show-table-view');

    var {Title, source, alt_source, type, filters, sortInfo, pageSize, startIdx, fixedLength} = params;
    var request = makeFileRequest(Title, source, alt_source, {filters, sortInfo, pageSize, startIdx});
    if (type === 'basic') {
        options.selectable = false;
    }
    return request;
}

function doShowTable(llApi, targetDiv, request, options={}) {
    const {dispatchTableSearch}= llApi.action;
    const {renderDOM}= llApi.util;
    const {TablesContainer}= llApi.ui;
    var contProps = options && options.tbl_group ? {tbl_group: options.tbl_group} : {};

    if ((typeof targetDiv).match(/string|HTMLDivElement/) === null) {
        // old api.. need to setup request and options before continue.
        const params = targetDiv;
        targetDiv = request;
        request = oldApi(llApi, params, options);
        contProps.tbl_group = targetDiv;
        options.tbl_group = targetDiv;
    }

    Object.keys(options).forEach( (k) => {
        if (options[k] === undefined) {
            Reflect.deleteProperty(options, k);
        }
    });

    dispatchTableSearch(request, options);
    renderDOM(targetDiv, TablesContainer, contProps);
}

function buildTablePart(llApi) {
    const {dispatchTableFetch}= llApi.action;

    /**
     * @typedef {object} TblOptions    table options
     * @prop {string}  tbl_group    the group this table belongs to.  Defaults to 'main'.
     * @prop {boolean} removable    true if this table can be removed from view.  Defaults to true.
     * @prop {boolean} showUnits    defaults to false
     * @prop {boolean} showFilters  defaults to false
     * @prop {boolean} selectable   defaults to true
     * @prop {boolean} expandable   defaults to true
     * @prop {boolean} showToolbar  defaults to true
     * @prop {boolean} border       defaults to true
     */

    /**
     * The general plotting function to plot a table.
     * @param {string|HTMLDivElement} targetDiv to put the table in.
     * @param {Object} request         request object created from
     * @param {TblOptions} options     table options.
     */
    const showTable= (targetDiv, request, options)  => doShowTable(llApi, targetDiv, request, options);

    return {showTable};
}

/*---------------------------- TABLE PART >----------------------------*/


function buildChartPart(llApi) {

    /**
     * @typedef {object} XYPlotOptions  xy plot options
     * @prop {string}  source       location of the ipac table, url or file path; ignored when XY plot view is added to table
     * @prop {string}  QUERY_ID     required when XY plot view is added to the table. It connects this XY Plot to the table and should be the same string that you specified as the div parameter when you created the table.
     * @prop {string}  chartTitle   title of the chart
     * @prop {string}  xCol         column or expression to use for x values, can contain multiple column names ex. log(col) or (col1-col2)/col3
     * @prop {string}  yCol         column or expression to use for y values, can contain multiple column names ex. sin(col) or (col1-col2)/col3
     * @prop {number}  xyRatio      aspect ratio (must be between 1 and 10)
     * @prop {string}  stretch      fit or fill
     * @prop {string}  xLabel       label to use with x axis
     * @prop {string}  yLabel       label to use with y axis
     * @prop {string}  xUnit        unit for x axis
     * @prop {string}  yUnit        unit for y axis
     * @prop {string}  xOptions     comma separated list of x axis options: grid,flip,log
     * @prop {string}  yOptions     comma separated list of y axis options: grid,flip,log
     */

    /**
     * The general plotting function to plot an XY Plot.
     * @param {string|HTMLDivElement} targetDiv to put the chart in.
     * @param {XYPlotOptions} parameters the request object literal with the chart parameters
     * @namespace firefly
     */
    const showPlot= (targetDiv, parameters)  => showXYPlot(llApi, targetDiv, parameters);

    /**
     * Add XY Plot view of an existing table.
     * @param {string|HTMLDivElement} targetDiv to put the chart in.
     * @param {XYPlotOptions} parameters the request object literal with the chart parameters
     * @namespace firefly
     */
    const addXYPlot= (targetDiv, parameters) => showXYPlot(llApi, targetDiv, parameters);

    return {showPlot, addXYPlot};
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
        llApi.util.image.dispatchApiToolsView(true);
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

function showXYPlot(llApi, targetDiv, params={}) {
    const {dispatchSetupTblTracking, dispatchTableFetch}= llApi.action;
    const {TBL_RESULTS_ACTIVE} = llApi.action.type;
    const {renderDOM} = llApi.util;
    const {makeFileRequest, getActiveTableId, uniqueTblId} = llApi.util.table;
    const {uniqueChartId, loadPlotDataForTbl} = llApi.util.chart;
    const {ChartsContainer, ChartsTableViewPanel}= llApi.ui;
    const {addActionListener} = llApi.util;

    if ((typeof targetDiv).match(/string|HTMLDivElement/) === null) {
        // old api.. need to change targetDiv and params
        const oldApiParams = targetDiv;
        targetDiv = params;
        params = oldApiParams;
    }

    const {xCol, yCol, xyRatio, stretch, xLabel, yLabel, xUnit, yUnit, xOptions, yOptions} = params;
    const xyPlotParams = {
        xyRatio,
        stretch,
        x : { columnOrExpr : xCol, label : xLabel||xCol, unit : xUnit||'', options : xOptions},
        y : { columnOrExpr : yCol, label : yLabel||yCol, unit : yUnit||'', options : yOptions}
    };

    // it is not quite clear how to handle situation when there are multiple tables in a group
    // for now we are connecting to the currently active table in the group
    const tblGroup = params.QUERY_ID || params.tbl_group;
    var tblId = params.tbl_id;
    // standalone plot, not connected to an existing table
    if (!tblGroup && !tblId) {
        tblId = uniqueTblId();
        const searchRequest = makeFileRequest(
            params.chartTitle||'', // title
            params.source,  // source
            null,  // alt_source
            {
                pageSize: 0 // options
            },
            tblId // table id
        );
        dispatchTableFetch(searchRequest);
    }

    const chartId = uniqueChartId(tblId||tblGroup);

    if (tblGroup) {
        tblId = getActiveTableId(tblGroup);
        addActionListener(TBL_RESULTS_ACTIVE, (action, state) => {
            const new_tblId = getActiveTableId(tblGroup);
            if (new_tblId !== tblId) {
                tblId = new_tblId;
                dispatchSetupTblTracking(tblId);
                loadPlotDataForTbl(tblId, chartId, xyPlotParams);
            }
        });
    }
    if (tblId) {
        dispatchSetupTblTracking(tblId);
        loadPlotDataForTbl(tblId, chartId, xyPlotParams);
    }


    renderDOM(targetDiv, ChartsTableViewPanel,
        {
            key: `${targetDiv}-chart`,
            tblId,
            chartId,
            closeable: false,
            expandedMode: false
        }
    );
}
