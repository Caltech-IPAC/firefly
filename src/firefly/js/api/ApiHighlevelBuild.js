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
 * @public
 * @desc build highLevelApi using the lowLevelApi as an input
 */

/**
 * @param llApi the lowlevel api
 * @returns {Object}
 * @ignore
 *
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
 * @returns {Object}
 * @ignore
 */
function build(llApi) {

    const commonPart= buildCommon(llApi);
    const imagePart= buildImagePart(llApi);
    const chartPart= buildChartPart(llApi);
    const tablePart= buildTablePart(llApi);
    return Object.assign({}, commonPart, imagePart,chartPart,tablePart);
}

/*----------------------------< TABLE PART ----------------------------*/

var divToGrp = (() => {
    // workaround to support mapping first targetDiv to 'main'
    var main;
    return (div) => {
        if (!main) main = div;
        return div === main ? 'main' : div;
    };
})();

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

    var {Title, source, alt_source, type, filters, sortInfo, pageSize, startIdx, fixedLength, expandable, rowHeight} = params;
    var request = makeFileRequest(Title, source, alt_source, {filters, sortInfo, pageSize, startIdx});
    options.selectable = type === 'selectable';
    options.help_id = 'tables';
    options.expandable = !!expandable;
    options.rowHeight = rowHeight;
    return request;
}

function doShowTable(llApi, targetDiv, request, options={}) {
    const {dispatchTableSearch}= llApi.action;
    const {renderDOM}= llApi.util;
    const {TablesContainer}= llApi.ui;

    if ((typeof targetDiv).match(/string|HTMLDivElement/) === null) {
        // old api.. need to setup request and options before continue.
        const params = targetDiv;
        targetDiv = request;
        request = oldApi(llApi, params, options);
    }

    options = Object.assign({tbl_group: divToGrp(targetDiv)}, options);
    const contProps = {tbl_group: options.tbl_group};

    Object.keys(options).forEach( (k) => {
        if (options[k] === undefined) {
            Reflect.deleteProperty(options, k);
        }
    });

    dispatchTableSearch(request, options);
    renderDOM(targetDiv, TablesContainer, contProps);
}

function buildTablePart(llApi) {

    /**
     * @global
     * @public
     * @typedef {object} TblOptions
     * @prop {string}  tbl_group    the group this table belongs to.  Defaults to 'main'.
     * @prop {number}  pageSize     the starting page size.  Will use the request's pageSize if not given.
     * @prop {boolean} removable    true if this table can be removed from view.  Defaults to true.
     * @prop {boolean} backgroundable    true if this search can be sent to background.  Defaults to false.
     * @prop {boolean} showUnits    defaults to false
     * @prop {boolean} showFilters  defaults to false
     * @prop {boolean} selectable   defaults to true
     * @prop {boolean} expandable   defaults to true
     * @prop {boolean} showToolbar  defaults to true
     * @prop {boolean} border       defaults to true
     * @prop {function[]}  leftButtons   an array of functions that returns a button-like component laid out on the left side of this table header.  Function will be called with table's state.
     * @prop {function[]}  rightButtons  an array of functions that returns a button-like component laid out on the left side of this table header.  Function will be called with table's state.
     */

    /**
     * @param {string|HTMLDivElement} targetDiv to put the table in.
     * @param {Object} request         request object created from
     * @param {TblOptions} options     table options.
     * @memberof firefly
     * @public
     * @example firefly.showTable
     */
    // @param {module:firefly.TblOptions} options     table options.
    const showTable= (targetDiv, request, options)  => doShowTable(llApi, targetDiv, request, options);

    return {showTable};
}

/*---------------------------- TABLE PART >----------------------------*/


function buildChartPart(llApi) {

    /**
     * @summary The general function to plot a Plotly chart.
     * @param {string|HTMLDivElement} targetDiv - div to put the chart in.
     * @param {{data: array.object, layout: object}} parameters - plotly parameters (possibly with firefly extensions)
     * @memberof firefly
     * @public
     */
    const showChart = (targetDiv, parameters)  => doShowChart(llApi, targetDiv, parameters);

    /**
     * @summary The general plotting function to plot an XY Plot.
     * @param {string|HTMLDivElement} targetDiv - div to put the chart in.
     * @param {XYPlotOptions} parameters - object literal with the chart parameters
     * @memberof firefly
     * @public
     * @example firefly.showXYPlot('myDiv', {source: 'mySourceFile', {xCol: 'ra', yCol: 'dec'})
     */
    const showXYPlot= (targetDiv, parameters)  => doShowXYPlot(llApi, targetDiv, parameters);

    /**
     * @summary  Add XYPlot view of a table. Deprecated: use showXYPlot with tbl_id in @link{XYPlotOptions} instead.
     * @param {string|HTMLDivElement} targetDiv - div to put the chart in.
     * @param {XYPlotOptions} parameters - object literal with the chart parameters
     * @memberof firefly
     * @deprecated
     * @example firefly.addXYPlot('myDiv', {tbl_id: <tbl_id>, {xCol: 'ra', yCol: 'dec'})
     */
    const addXYPlot= (targetDiv, parameters) => doShowXYPlot(llApi, targetDiv, parameters);

    /**
     * @summary The general plotting function to plot Histogram.
     * @param {string|HTMLDivElement} targetDiv - div to put the chart in.
     * @param {HistogramOptions} parameters - object literal with the chart parameters
     * @memberof firefly
     * @public
     * @example firefly.showHistogram
     */
    const showHistogram= (targetDiv, parameters)  => doShowHistogram(llApi, targetDiv, parameters);


    return {showChart, showXYPlot, addXYPlot, showHistogram};
}

function buildCommon(llApi) {
    /**
     * @summary Sets the root path for any relative URL. If this method has not been called then relative URLs use the page's root.
     * @param {string} rootUrlPath
     * @memberof firefly
     * @public
     */
    const setRootPath= (rootUrlPath) => llApi.action.dispatchRootUrlPath(rootUrlPath);

    return {setRootPath};
}

function buildImagePart(llApi) {


    const {RequestType}= llApi.util.image;

    /**
     * @summary The general plotting function to plot a FITS image.
     * @param {String|div} targetDiv to put the image in.
     * @param {Object} request the request object literal with the plotting parameters
     * @memberof firefly
     * @public
     * @example firefly.showImage
     *
     */
    const showImage= (targetDiv, request)  => showImageInMultiViewer(llApi, targetDiv, request);

    /**
     * @summary A convenience plotting function to plot a file on the server or a url.  If first looks for the file then
     * the url is the fallback
     * @param {string|div} targetDiv to put the image in.
     * @param {string} file file on server
     * @param {string} url url reference to a fits file
     * @memberof firefly
     * @public
     * @ignore
     * @example firefly.showImageFileOrUrl
     */
    const showImageFileOrUrl= (targetDiv, file,url) =>
              showImageInMultiViewer(llApi, targetDiv,
                                           {'File' : file,
                                            'URL' : url,
                                            'Type' : RequestType.TRY_FILE_THEN_URL
                                           });


    /**
     * @summary set global fallback params for every image plotting call
     * @param {Object} params a object literal such as any image plot or showImage uses
     * @memberof irefly
     * @public
     * @ignore
     * @example firefly.setGlobalImageDef
     */
    const setGlobalImageDef= (params) => globalImageViewDefParams= params;


    /**
     *
     * @param div - targetDiv to put the coverage in.
     * @param options - an object literal containing a list of the coverage options
     * @memberof firefly
     * @public
     * @example firefly.showCoverage
     */
    const showCoverage= (div,options) => initCoverage(llApi,div,options);

    return {showImage, showImageFileOrUrl, setGlobalImageDef, showCoverage};
}

/**
 * Build the deprecated API
 * @param llApi
 * @returns {Object}
 * @deprecated
 * @ignore
 */
function buildDeprecated(llApi) {

    const dApi= {};
    const {RequestType}= llApi.util.image;

    dApi.makeImageViewer= (plotId) => {
        llApi.util.debug('makeImageViewer is deprecated, use firefly.showImage() instead');
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
    const {renderDOM,debug, getWsConnId, getWsChannel}= llApi.util;
    const {MultiImageViewer, MultiViewStandardToolbar}= llApi.ui;

    highlevelImageInit(llApi);
    const testR= Array.isArray(request) ? request : [request];
    testR.forEach( (r) => {
        const badList= findInvalidWPRKeys(r);
        if (badList.length) debug(`plot request has the following bad keys: ${badList}`);
    });
    request= confirmPlotRequest(request,globalImageViewDefParams,targetDiv,makePlotId(getWsConnId));

    const plotId= !Array.isArray(request) ? request.plotId : request.find( (r) => r.plotId).plotId;
    dispatchAddViewer(targetDiv,'create_replace');
    dispatchPlotImage({plotId, wpRequest:request, viewerId:targetDiv});
    renderDOM(targetDiv, MultiImageViewer,
        {viewerId:targetDiv, canReceiveNewPlots:'create_replace', Toolbar:MultiViewStandardToolbar });

}

function initCoverage(llApi, targetDiv,options= {}) {
    const {MultiImageViewer, MultiViewStandardToolbar}= llApi.ui;
    const {dispatchAddSaga}= llApi.action;
    const {renderDOM,debug}= llApi.util;
    const {watchImageMetaData,watchCoverage}= llApi.util.image;
    highlevelImageInit(llApi);

    const {canReceiveNewPlots='create_replace'}= options;


    renderDOM(targetDiv, MultiImageViewer,
        {viewerId:targetDiv, canReceiveNewPlots, canDelete:false, Toolbar:MultiViewStandardToolbar });
    options= Object.assign({},options, {viewerId:targetDiv});
    dispatchAddSaga(watchCoverage, options);
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

function makePlotId(wsConnIdGetter) {
    return () => {
        plotCnt++;
        return `apiPlot-${wsConnIdGetter()}-${plotCnt}`;
    };
}

//================================================================
//---------- Private Table functions
//================================================================




//================================================================
//---------- Private XYPlot or Histogram functions
//================================================================

function doShowChart(llApi, targetDiv, params={}) {
    const {dispatchChartAdd}= llApi.action;
    const {uniqueChartId} = llApi.util.chart;
    const {renderDOM} = llApi.util;
    const {MultiChartViewer}= llApi.ui;

    params = Object.assign({chartId: uniqueChartId(`${targetDiv}`), viewerId: targetDiv, chartType: 'plot.ly'}, params);
    dispatchChartAdd(params);

    renderDOM(targetDiv, MultiChartViewer,
        {
            key: `${targetDiv}-plot`,
            viewerId: targetDiv,
            closeable: false,
            expandedMode: false
        }
    );
}

function doShowXYPlot(llApi, targetDiv, params={}) {
    const {dispatchTableFetch, dispatchChartAdd, dispatchChartRemove}= llApi.action;
    const {TBL_RESULTS_ACTIVE} = llApi.action.type;
    const {renderDOM} = llApi.util;
    const {makeFileRequest, getActiveTableId} = llApi.util.table;
    const {makeXYPlotParams, uniqueChartId} = llApi.util.chart;
    const {ChartsContainer}= llApi.ui;
    const {addActionListener} = llApi.util;

    if ((typeof targetDiv).match(/string|HTMLDivElement/) === null) {
        // old api.. need to change targetDiv and params
        const oldApiParams = targetDiv;
        targetDiv = params;
        params = oldApiParams;
    }

    const xyPlotParams = makeXYPlotParams(params);
    const help_id = params.help_id;

    // it is not quite clear how to handle situation when there are multiple tables in a group
    // for now we are connecting to the currently active table in the group
    const tblGroup = params.QUERY_ID || params.tbl_group; // QUERY_ID is deprecated, should be removed at some point
    var tblId = params.tbl_id;
    // standalone plot, not connected to an existing table
    if (!tblGroup && !tblId) {
        const searchRequest = makeFileRequest(
            params.chartTitle||'', // title
            params.source,  // source
            null,  // alt_source
            {pageSize: 1} // options
        );
        tblId = searchRequest.tbl_id;
        dispatchTableFetch(searchRequest);
    }

    const chartId = targetDiv;

    if (tblGroup) {
        tblId = getActiveTableId(tblGroup);
        addActionListener(TBL_RESULTS_ACTIVE, () => {
            const new_tblId = getActiveTableId(tblGroup);
            if (new_tblId !== tblId) {
                tblId = new_tblId;
                dispatchChartRemove(chartId);
                dispatchChartAdd({chartId, chartType: 'scatter', help_id, deletable: false,
                    mounted: 1,
                    chartDataElements: [
                        {
                            type: 'xycols', //DATATYPE_XYCOLS.id
                            options: xyPlotParams,
                            tblId
                        }
                    ]});
            }
        });
    }

    // SCATTER
    dispatchChartAdd({chartId, chartType: 'scatter', help_id, deletable: false,
        chartDataElements: [
            {
                type: 'xycols', //DATATYPE_XYCOLS.id
                options: xyPlotParams,
                tblId
            }
        ]});

    renderDOM(targetDiv, ChartsContainer,
        {
            key: `${targetDiv}-xyplot`,
            chartId,
            closeable: false,
            expandedMode: false
        }
    );
}

function doShowHistogram(llApi, targetDiv, params={}) {
    const {dispatchTableFetch, dispatchChartAdd, dispatchChartRemove}= llApi.action;
    const {TBL_RESULTS_ACTIVE} = llApi.action.type;
    const {renderDOM} = llApi.util;
    const {makeFileRequest, getActiveTableId} = llApi.util.table;
    const {makeHistogramParams, uniqueChartId} = llApi.util.chart;
    const {ChartsContainer}= llApi.ui;
    const {addActionListener} = llApi.util;

    const histogramParams = makeHistogramParams(params);
    const help_id = params.help_id;

    // it is not quite clear how to handle situation when there are multiple tables in a group
    // for now we are connecting to the currently active table in the group
    const tblGroup = params.tbl_group;
    var tblId = params.tbl_id;
    // standalone plot, not connected to an existing table
    if (!tblGroup && !tblId) {
        const searchRequest = makeFileRequest(
            params.chartTitle||'', // title
            params.source,  // source
            null,  // alt_source
            {pageSize: 1} // options
        );
        tblId = searchRequest.tbl_id;
        dispatchTableFetch(searchRequest);
    }

    const chartId = targetDiv;

    if (tblGroup) {
        tblId = getActiveTableId(tblGroup);
        addActionListener(TBL_RESULTS_ACTIVE, () => {
            const new_tblId = getActiveTableId(tblGroup);
            if (new_tblId !== tblId) {
                tblId = new_tblId;
                dispatchChartRemove(chartId);
                dispatchChartAdd({chartId, chartType: 'histogram', help_id, deletable: false,
                    mounted: 1,
                    chartDataElements: [
                        {
                            type: 'histogram', //DATATYPE_XYCOLS.id
                            options: histogramParams,
                            tblId
                        }
                    ]});
            }
        });
    }
    // HISTOGRAM, DATA_TYPE_HISTOGRAM
    dispatchChartAdd({chartId, chartType: 'histogram', help_id, deletable: false,
        chartDataElements: [
            {
                type: 'histogram',
                options: histogramParams,
                tblId
            }
        ]});

    renderDOM(targetDiv, ChartsContainer,
        {
            key: `${targetDiv}-histogram`,
            chartId,
            closeable: false,
            expandedMode: false
        }
    );
}
