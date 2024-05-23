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

const STANDARD= 'standard';
const ENCAPSULATE= 'encapsulate';


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

function doShowClientTable(llApi, targetDiv, tableModel, options={}) {
    const {dispatchTableAddLocal}= llApi.action;
    const {renderDOM}= llApi.util;
    const {TablesContainer}= llApi.ui;

    options = Object.assign({tbl_group: divToGrp(targetDiv)}, options);
    const contProps = {tbl_group: options.tbl_group};

    Object.keys(options).forEach( (k) => {
        if (options[k] === undefined) {
            Reflect.deleteProperty(options, k);
        }
    });
    dispatchTableAddLocal(tableModel, options);
    renderDOM(targetDiv, TablesContainer, contProps);
}

function buildTablePart(llApi) {
    const {dispatchTableFetch}= llApi.action;

    /**
     * @global
     * @public
     * @typedef {object} TblOptions
     * @prop {string}  tbl_group    the group this table belongs to.  Defaults to 'main'.
     * @prop {number}  pageSize     the starting page size.  Will use the request's pageSize if not given.
     * @prop {boolean} removable    true if this table can be removed from view.  Defaults to true.
     * @prop {boolean} backgroundable    true if this search can be sent to background.  Defaults to false.
     * @prop {boolean} showUnits    defaults to true if table contains unit info
     * @prop {boolean} showTypes    defaults to false
     * @prop {boolean} showFilters  defaults to true for all tables except client tables
     * @prop {boolean} selectable   defaults to true
     * @prop {boolean} expandable   defaults to true
     * @prop {boolean} showToolbar  defaults to true
     * @prop {boolean} showTitle    defaults to true
     * @prop {boolean} showPaging   defaults to true
     * @prop {boolean} showSave     defaults to true
     * @prop {boolean} showOptionButton    defaults to true
     * @prop {boolean} showFilterButton    defaults to true
     * @prop {boolean} showInfoButton      defaults to true
     * @prop {boolean} border       defaults to true
     * @prop {boolean} help_id      link to help if applicable
     * @prop {function[]}  leftButtons   an array of functions that returns a button-like component laid out on the left side of this table header.  Function will be called with table's state.
     * @prop {function[]}  rightButtons  an array of functions that returns a button-like component laid out on the left side of this table header.  Function will be called with table's state.
     */


    /**
     * @param {string|HTMLDivElement} targetDiv to put the table in.
     * @param {TableRequest} request   request object created from
     * @param {TblOptions} options     table options.
     * @memberof firefly
     * @public
     * @example var tblReq =  firefly.util.table.makeIrsaCatalogRequest(
     *                 'allwise-500', 'WISE', 'allwise_p3as_psd',
     *                 {position: '10.68479;41.26906;EQ_J2000',
     *                  SearchMethod: 'Cone',
     *                  radius: 300},
     *                 {tbl_id: 'test-tbl',
     *                  META_INFO: {defaultChartDef: JSON.stringify({data: [{x: 'tables::w1mpro', y: 'tables::w2mpro', mode: 'markers'}]})}
     *                 });
     * firefly.showTable('table-1', tblReq, {tbl_group: 'allwise'});
     */
    const showTable = (targetDiv, request, options)  => doShowTable(llApi, targetDiv, request, options);

    /**
     * Fetch and store the table data without displaying it.
     * @param {TableRequest} request   table request to fetch
     * @memberof firefly
     * @public
     */
    const fetchTable = (request)  => dispatchTableFetch(request);

    /**
     * Render the tableModel into the given div
     * @param {string|HTMLDivElement} targetDiv to put the table in.
     * @param {TableModel} tableModel  request object created from
     * @param {TblOptions} options     table options.
     * @memberof firefly
     * @public
     */
    const showClientTable = (targetDiv, tableModel, options)  => doShowClientTable(llApi, targetDiv, tableModel, options);


    return {showTable, showClientTable, fetchTable};
}

/*---------------------------- TABLE PART >----------------------------*/


function buildChartPart(llApi) {

    /**
     * @summary The general function to plot a Plotly chart.
     * @param {string|HTMLDivElement} targetDiv - div to put the chart in.
     * @param {object} parameters
     * @param {array.object} parameters.data - plotly data array (possibly with firefly extensions)
     * @param {object} parameters.layout - plotly layout object (possibly with firefly extensions)
     * @param {boolean} parameters.noChartToolbar - set true for non-interactive chart with no toolbar
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
     * @deprecated
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
     * @deprecated
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

    /**
     * @deprecated
     * @param pref
     */
    const setGlobalPref= (pref)=> {
        const newAppOp= {...llApi.util.getAppOptions(), ...pref};
        llApi.action.dispatchAppOptions(newAppOp);
        // Object.assign(globalPrefs, pref);
    };// deprecated- use app options

    return {setRootPath,setGlobalPref};
}

function buildImagePart(llApi) {


    const {RequestType}= llApi.util.image;


    /**
     * @summary object for web plot request
     * @description Below is a list of predefined parameters available for web plot request.
     * Some parameters control how to get an image, a image can be retrieved from a service, a url, of a file on the server.
     * Others control the zoom, stretch, and color, title, and default overlays. There are also parameters to pre-process an
     * image, such as crop, rotate or flip.
     * @typedef {object} WebPlotParams
     * @prop {string} Type the request type, see available types at {@link RequestType}.
     * @prop {string} File file name of a file on the server if Type=='File'
     * @prop {string} URL  url reference to a fits file if Type=='URL'
     * @prop {string} Service the service type if Type=='SERVICE', see available services at {@link ServiceType}
     * @prop {string} plotId plot ID
     * @prop {string} plotGroupId plot group ID
     * @prop {String} ObjectName object name that can be looked up by NED or Simbad
     * @prop {string} Resolver the object name resolver to use, options are: NED, Simbad, NedThenSimbad, SimbadThenNed, PTF.
     * @prop {string} SizeInDeg the radius or side (in degrees) depending of the service type, used with Type=='SERVICE' or 'HiPS'
     * @prop {string} SurveyKey the survey, used with  Type='SERVICE'
     * @prop {string} SurveyKeyBand the survey band, used with Type=='SERVICE' and Service='WISE', 'TWOMASS' or 'ATLAS'
     * @prop {string} WorldPt target for service request or HiPS request in serialized version
     * @prop {string} PlotId plot Id
     * @prop {string} PlotGroupId plot group id
     * @prop {string} Title plot title
     * @prop {string} TitleOptions title options, see available options at {@link TitleOptions}
     * @prop {string} PreTitle a String to append at the beginning of the title of the plot
     * @prop {string} PostTitle a String to append at the end of the title of the plot
     * @prop {string} hipsRootUrl HiPS root url or IVOID, e.g.: ivo://CDS/P/2MASS/J, used with Type='HiPS'
     * @prop {string} ZoomType zoom type, see {@link ZoomType}
     * @prop {string} ZoomToWidth the width of the viewable area to determine the zoom level, used with ZoomType.TO_WIDTH_HEIGHT, ZoomType.TO_WIDTH
     * @prop {string} ZoomToHeight the height of the viewable area to determine the zoom level, used with ZoomType.TO_WIDTH_HEIGHT, ZoomType.TO_HEIGHT
     * @prop {string} InitZoomLevel initialize zoom level, used with ZoomType.LEVEL
     * @prop {string} ZoomArcsecPerScreenPix the arcseconds per screen pixel that will be used to determine the zoom level, Used with ZoomType.ARCSEC_PER_SCREEN_PIX
     * @prop {boolean} RotateNorth plot should come up rotated north
     * @prop {CoordinateSys} RotateNorthType set to coordinate system for rotate north, eq EQ_J2000 is the default
     * @prop {boolean} Rotate set to rotate, if true, the angle should also be set
     * @prop {string} RotationAngle set the angle to rotate to
     * @prop {boolean} FlipY set if this image should be flipped on the Y axis
     * @prop {boolean} FlipX set if this image should be flipped on the X axis
     * @prop {boolean} PostCrop crop the image before returning it.  If rotation is set then the crop will happen post rotation
     * @prop {boolean} PostCropAndCenter crop and center the image before returning it. Note: SizeInDeg and WorldPt are required
     * @prop {CoordinateSys} PostCropAndCenterType set to coordinate system for crop and center, eq EQ_J2000 is the default
     * @prop {string} CropPt1 one corner of the rectangle, in image coordinates, to crop out of the image.
     * @prop {string} CropPt2 second corner of the rectangle, in image coordinates, to crop out of the image.
     * @prop {string} CropWorldPt1 one corner of the rectangle, in world coordinates, to crop out of the image.
     * @prop {string} CropWorldPt2 second corner of the rectangle, in world coordinates, to crop out of the image.
     * @prop {string} OverlayPosition string of overlay position in world coordinates
     * @prop {string} ColorTable color table id, value 0 - 21 to represent different predefined color tables
     * @prop {string} RangeValues a complex string for specify the stretch of this plot.
     *                            Use the method firefly.serializeRangeValues() to produce this string
     * @prop {string} MultiImageIdx number index of image
     * @prop {string} MultiImageExts image extension list. ex: '3,4,5' for extension 3, 4, 5
     * @prop {string} GridOn turn the coordinate grid on after the image is plotted, 'true' or 'false'
     * @prop {number} thumbnailSize thumbnail size
     * @prop {boolean} ContinueOnFail for 3 color, if this request fails then keep trying to make a plot with the other request
     *
     * @global
     * @public
     */

    /**
     * @summary The general plotting function to plot a FITS image.
     * @param {String|HTMLDivElement} targetDiv to put the image in.
     * @param {WebPlotParams|WebPlotRequest} request a request object with the plotting parameters
     * @param {HipsImageConversionSettings} [hipsImageConversion= undefined] if defined, use these parameter to
     *                                                convert between image and HiPS
     * @param {boolean} userCanDelete User can delete the image
     *
     * @memberof firefly
     * @public
     * @example firefly.showImage('myPlot',
     *                   {Type: 'SERVICE',
     *                    plotId: 'myImage',
     *                    plotGroupId: 'myGroup',
     *                    Service: 'WISE'
     *                    Title: 'Wise'
     *                    GridOn: true,
     *                    SurveyKey: 'Atlas'
     *                    SurveyKeyBand: '2'
     *                    WorldPt: '10.68479;41.26906;EQ_J2000',
     *                    SizeInDeg: '.12'});
     *
     *
     */
    const showImage= (targetDiv, request, hipsImageConversion, userCanDelete)  =>
                   showImageInMultiViewer(llApi, targetDiv, request, false, hipsImageConversion, userCanDelete);

    /**
     * @summary A convenience plotting function to plot a file on the server or a url.  If first looks for the file then
     * the url is the fallback
     * @param {string|HTMLDivElement} targetDiv to put the image in.
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
                                           }, false);


    /**
     * @summary set global fallback params for every image plotting call
     * @param {Object} params a object literal such as any image plot or showImage uses
     * @memberof firefly
     * @public
     * @ignore
     * @example firefly.setGlobalImageDef
     */
    const setGlobalImageDef= (params) => globalImageViewDefParams= params;


    /**
     *
     * @param {string|HTMLDivElement} div - targetDiv to put the coverage in.
     * @param {CoverageOptions} options - an object literal containing a list of the coverage options
     * @memberof firefly
     * @public
     * @example firefly.showCoverage('myDiv', {gridOn: true})
     */
    const showCoverage= (div,options) => initCoverage(llApi,div,options);

    /**
     * @summary The plotting function to display a HiPS
     * @param {String|HTMLDivElement} targetDiv to put the image in.
     * @param {WebPlotParams|WebPlotRequest} request a request object with Type=='HiPS' used to display a HiPS
     * @param {HipsImageConversionSettings} [hipsImageConversion=undefined] if defined, use these parameter to
     *                                                convert between image and HiPS
     * @param {boolean} userCanDelete User can delete the image
     * @memberof firefly
     * @public
     * @example firefly.showHiPS('hipsDIV1',
     *    {
     *      plotId  : 'aHipsID1-1',
     *      WorldPt : '148.892;69.0654;EQ_J2000',
     *      title   : 'A HiPS',
     *      hipsRootUrl: 'CDS/P/SDSS9/color'
     *    },
     *    {
     *      imageRequestRoot: {
     *              Service  : 'WISE',
     *              Title    : 'Wise',
     *              SurveyKey: '3a',
     *              SurveyKeyBand: '2'
     *       },
     *       fovDegFallOver: .5
     *     };
     * );
     *
     *
     */

    const showHiPS= (targetDiv, request, hipsImageConversion, userCanDelete)  =>
                        showImageInMultiViewer(llApi, targetDiv, request, true, hipsImageConversion, userCanDelete);


    /**
     * @summary The plotting function to display a HiPS or an image
     * @param {String|HTMLDivElement} targetDiv to put the image in.
     * @param {WebPlotParams|WebPlotRequest} hipsRequest a request object used to display a HiPS
     * @param {WebPlotParams|WebPlotRequest} imageRequest  a request object used to display an image
     * @param {number} fovDegFallOver the field of view size to determine when to move between a HiPS and an image
     * @param {WebPlotParams|WebPlotRequest} allSkyRequest a request object used to display allsky image.
     * @param {boolean} plotAllSkyFirst if plot allsky first
     * @param {boolean} [userCanDeletePlots] if true the plot has an x so the user can delete it
     *
     * @memberof firefly
     * @public
     * @example firefly.showImageOrHiPS('hipsDiv6',
     *    {
     *       plotId: 'aHipsID6',
     *       title     : 'A HiPS - 0.2',
     *       hipsRootUrl: 'http://alasky.u-strasbg.fr/AllWISE/RGB-W4-W2-W1',
     *       SizeInDeg:.2
     *    },
     *    {
     *       Service: 'WISE',
     *       Title: 'Wise',
     *       SurveyKey: '3a',
     *       SurveyKeyBand: '2',
     *       WorldPt: '148.892;69.0654;EQ_J2000'
     *    }, 0.5
     *  );
     *
     *
     */
    const showImageOrHiPS = (targetDiv, hipsRequest, imageRequest,  fovDegFallOver,
                             allSkyRequest, plotAllSkyFirst, userCanDeletePlots) =>
                        showImageOrHiPSInMultiViewer(llApi, targetDiv, hipsRequest, imageRequest,
                                                     fovDegFallOver, allSkyRequest, plotAllSkyFirst, userCanDeletePlots);

    return {showImage, showHiPS, showImageOrHiPS, showImageFileOrUrl, setGlobalImageDef, showCoverage};
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
        llApi.util.renderDOM(plotId, llApi.ui.ImageViewer, {plotId, toolbarVariant:'soft', });
        llApi.action.dispatchPlotImage({plotId, wpRequest:Object.assign({}, globalImageViewDefParams,request)});
    };
}

function validatePlotRequest(llApi, targetDiv, request) {
    const {findInvalidWPRKeys,confirmPlotRequest}= llApi.util.image;
    const {debug, getWsConnId}= llApi.util;

    const testR= Array.isArray(request) ? request : [request];
    testR.forEach( (r) => {
        const badList= findInvalidWPRKeys(r);
        if (badList.length) debug(`plot request has the following bad keys: ${badList}`);
    });

    return confirmPlotRequest(request,globalImageViewDefParams,targetDiv,makePlotId(getWsConnId));
}

function getPlotIdFromRequest(request) {
    if (Array.isArray(request)) {
        const rWithPId = request.find((r) => r.plotId);

        return rWithPId ? rWithPId.plotId : null;
    } else {
        return request.plotId;
    }
}

function showImageOrHiPSInMultiViewer(llApi, targetDiv, hipsRequest, imageRequest,
                                                fovDegFallOver, allSkyRequest, plotAllSkyFirst, userCanDeletePlots=false) {
    const {dispatchPlotImageOrHiPS, dispatchAddViewer}= llApi.action;
    const {IMAGE, NewPlotMode}= llApi.util.image;
    const {MultiImageViewer, MultiViewStandardToolbar}= llApi.ui;
    const {renderDOM}= llApi.util;


    highlevelImageInit(llApi);
    hipsRequest = validatePlotRequest(llApi, targetDiv, hipsRequest);
    hipsRequest.Type = 'HiPS';
    imageRequest = validatePlotRequest(llApi, targetDiv, imageRequest);

    dispatchAddViewer(targetDiv, NewPlotMode.create_replace.key, IMAGE);

    const plotId= getPlotIdFromRequest(hipsRequest) || getPlotIdFromRequest(imageRequest);
    dispatchPlotImageOrHiPS({plotId, hipsRequest, viewerId: targetDiv,
                             imageRequest, allSkyRequest, plotAllSkyFirst, fovDegFallOver,
                             pvOptions: {userCanDeletePlots}});

    renderDOM(targetDiv, MultiImageViewer,
        {viewerId:targetDiv, canReceiveNewPlots:NewPlotMode.create_replace.key, Toolbar:MultiViewStandardToolbar,
            toolbarVariant:'soft' });

}


var firstShowImage= false;
var imageRenderType;


function showImageInMultiViewer(llApi, targetDiv, request, isHiPS, hipsImageConversion, userCanDelete=true) {
    const {dispatchPlotImage, dispatchPlotHiPS, dispatchAddViewer, dispatchUpdateCustom}= llApi.action;
    const {IMAGE, NewPlotMode}= llApi.util.image;
    const {renderDOM}= llApi.util;
    const {ApiFullImageDisplay, ApiToolbarImageDisplay, MultiViewStandardToolbar}= llApi.ui;
    request = validatePlotRequest(llApi, targetDiv, request);
    const plotId= getPlotIdFromRequest(request);
    const viewerId= targetDiv;


    if (!firstShowImage) {
        firstShowImage= true;
        const appOp= llApi.util.getAppOptions();
        if (appOp.imageDisplayType===STANDARD) {
            imageRenderType= STANDARD;
            highlevelImageInit(llApi);
        }
    }


    dispatchAddViewer(targetDiv, NewPlotMode.create_replace.key, IMAGE);
    dispatchUpdateCustom(viewerId, {independentLayout: true});


    if (isHiPS) {
        request.Type= 'HiPS';
        if (hipsImageConversion && !hipsImageConversion.hipsRequestRoot) {
            hipsImageConversion.hipsRequestRoot= request;
        }
        dispatchPlotHiPS({plotId, wpRequest:request, viewerId,
            hipsImageConversion, pvOptions: { userCanDeletePlots: userCanDelete}
        });
    }
    else {
        if (hipsImageConversion && !hipsImageConversion.imageRequestRoot) {
            hipsImageConversion.imageRequestRoot= request;
        }
        dispatchPlotImage({plotId, wpRequest:request, viewerId,
            hipsImageConversion, pvOptions: { userCanDeletePlots: userCanDelete}
        });
    }

    if (imageRenderType===STANDARD) {
        renderDOM(targetDiv, ApiToolbarImageDisplay,
            {viewerId, canReceiveNewPlots:NewPlotMode.create_replace.key,
                Toolbar:MultiViewStandardToolbar });
    }
    else {
        renderDOM(targetDiv, ApiFullImageDisplay,
            {viewerId, renderTreeId:viewerId,
                canReceiveNewPlots:NewPlotMode.create_replace.key, Toolbar:MultiViewStandardToolbar });
    }

}






function initCoverage(llApi, targetDiv,options= {}) {
    const {MultiViewStandardToolbar, ApiToolbarImageDisplay}= llApi.ui;
    const {renderDOM}= llApi.util;
    const {startCoverageWatcher,NewPlotMode}= llApi.util.image;
    highlevelImageInit(llApi);

    const {canReceiveNewPlots=NewPlotMode.replace_only.key}= options;


    renderDOM(targetDiv, ApiToolbarImageDisplay,
        {viewerId:targetDiv, canReceiveNewPlots, canDelete:false,
            Toolbar:MultiViewStandardToolbar });
    options= Object.assign({},options, {viewerId:targetDiv});
    startCoverageWatcher(options);
}



var imageInit= false;
function highlevelImageInit(llApi) {
    if (!imageInit) {
        llApi.action.dispatchApiToolsView(true);
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
    const {ChartsContainer}= llApi.ui;

    const tbl_group = params.tbl_group;
    // when tbl_group parameter is set, show a default ch
    // for an active table in this table group
    if (!tbl_group) {
        params = Object.assign({
            chartId: uniqueChartId(`${targetDiv}`),
            viewerId: targetDiv,
            chartType: 'plot.ly'
        }, params);
        dispatchChartAdd(params);
    }

    renderDOM(targetDiv, ChartsContainer,
        {
            key: `${targetDiv}-plot`,
            viewerId: targetDiv,
            tbl_group,
            addDefaultChart: Boolean(tbl_group),
            closeable: false,
            useBorder: true,
            expandedMode: false,
            toolbarVariant:'soft',
            noChartToolbar: params.noChartToolbar
        }
    );
}

function doShowXYPlot(llApi, targetDiv, params={}) {
    const {dispatchTableFetch, dispatchChartAdd}= llApi.action;
    const {renderDOM} = llApi.util;
    const {makeFileRequest} = llApi.util.table;
    const {ChartsContainer}= llApi.ui;

    if ((typeof targetDiv).match(/string|HTMLDivElement/) === null) {
        // old api.. need to change targetDiv and params
        const oldApiParams = targetDiv;
        targetDiv = params;
        params = oldApiParams;
    }

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
        params = Object.assign({}, params, {tbl_id: tblId});
    }

    const help_id = params.help_id;

    const chartId = targetDiv;
    dispatchChartAdd({chartId, chartType: 'scatter', help_id, deletable: false, viewerId: targetDiv, params});

    renderDOM(targetDiv, ChartsContainer,
        {
            key: `${targetDiv}-xyplot`,
            viewerId: targetDiv,
            tbl_group: tblGroup,
            addDefaultChart: Boolean(tblGroup),
            closeable: false,
            expandedMode: false
        }
    );
}

function doShowHistogram(llApi, targetDiv, params={}) {
    const {dispatchTableFetch, dispatchChartAdd}= llApi.action;
    const {renderDOM} = llApi.util;
    const {makeFileRequest} = llApi.util.table;
    const {ChartsContainer}= llApi.ui;


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
        params = Object.assign({}, params, {tbl_id: tblId});
    }

    const help_id = params.help_id;

    const chartId = targetDiv;
    dispatchChartAdd({chartId, chartType: 'histogram', help_id, deletable: false, viewerId: targetDiv, params});

    renderDOM(targetDiv, ChartsContainer,
        {
            key: `${targetDiv}-histogram`,
            viewerId: targetDiv,
            tbl_group: tblGroup,
            addDefaultChart: Boolean(tblGroup),
            closeable: false,
            expandedMode: false
        }
    );
}
