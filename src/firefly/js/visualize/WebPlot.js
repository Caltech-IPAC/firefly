/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import {RequestType} from './RequestType.js';
import {isString} from 'lodash';
import CoordinateSys from './CoordSys.js';
import {makeProjection} from './projection/Projection.js';
import PlotState from './PlotState.js';



export const PlotAttribute= {




    MOVING_TARGET_CTX_ATTR:   'MOVING_TARGET_CTX_ATTR',

    /**
     * This will probably be a WebMouseReadoutHandler class
     * @see WebMouseReadoutHandler
     */
    READOUT_ATTR:             'READOUT_ATTR',

    READOUT_ROW_PARAMS:             'READOUT_ROW_PARAMS',

    /**
     * This will probably be a WorldPt
     */
    FIXED_TARGET:             'FIXED_TARGET',

    /**
     * This will probably be a double with the requested size of the plot
     */
    REQUESTED_SIZE:             'REQUESTED_SIZE',


    /**
     * This will probably an object represent a rectangle {pt0: point,pt1: point}
     * @See ./Point.js
     */
    SELECTION:                'SELECTION',

    /**
     * This will probably an object to represent a line {pt0: point,pt1: point}
     * @See ./Point.js
     */
    ACTIVE_DISTANCE:          'ACTIVE_DISTANCE',

    SHOW_COMPASS:          'SHOW_COMPASS',

    /**
     * This will probably an object {pt: point}
     * @See ./Point.js
     */
    ACTIVE_POINT:          'ACTIVE_POINT',


    /**
     * This is a String describing why this plot can't be rotated.  If it is defined then
     * rotating is disabled.
     */
    DISABLE_ROTATE_REASON:          'DISABLE_ROTATE_HINT',

    /**
     * what should happen when multi-fits images are changed.  If set the zoom is set to the same level
     * eg 1x, 2x ect.  If not set then flipping should attempt to make the image the same arcsec/screen pixel.
     */
    FLIP_ZOOM_BY_LEVEL: 'FLIP_ZOOM_BY_LEVEL',

    /**
     * what should happen when multi-fits images are changed.  If set the zoom is set to the same level
     * eg 1x, 2x ect.  If not set then flipping should attempt to make the image the same arcsec/screen pixel.
     */
    FLIP_ZOOM_TO_FILL: 'FLIP_ZOOM_TO_FILL',

    /**
     * if set, when expanded the image will be zoom to no bigger than this level,
     * this should be a subclass of Number
     */
    MAX_EXPANDED_ZOOM_LEVEL : 'MAX_EXPANDED_ZOOM_LEVEL',

    /**
     * if set, this should be the last expanded single image zoom level.
     * this should be a subclass of Number
     */
    LAST_EXPANDED_ZOOM_LEVEL : 'LAST_EXPANDED_ZOOM_LEVEL',

    /**
     * if set, must be one of the string values defined by the enum ZoomUtil.FullType
     * currently is is ONLY_WIDTH, WIDTH_HEIGHT, ONLY_HEIGHT
     */
    EXPANDED_TO_FIT_TYPE : 'MAX_EXPANDED_ZOOM_LEVEL',

    /**
     * if true, the readout will be very small
     */
    MINIMAL_READOUT : 'MINIMAL_READOUT',

    TABLE_ROW : 'TABLE_ROW',


    UNIQUE_KEY : 'UNIQUE_KEY'
};



/**
 * @global
 * @public
 * @typedef {Object} WebPlot
 *
 * This class contains plot information.
 * Publicly this class operations in many coordinate system.
 * Some include a Image coordinate system, a world coordinate system, and a screen
 * coordinate system.
 *
 * @prop {String} plotId - immutable
 * @prop {String} plotImageId - immutable
 * @prop {Object} serverImage - immutable
 * @prop {String} title - the title
 * @prop {PlotState} plotsState - the plot state, immutable
 *
 */
export const WebPlot= {

    /**
     *
     * @param plotId
     * @param wpInit init data returned from server
     * @param attributes any attributes to initialize
     * @param asOverlay
     * @return {WebPlot}
     */
    makeWebPlotData(plotId, wpInit, attributes= {}, asOverlay= false) {

        const projection= makeProjection(wpInit.projectionJson);
        var plotState= PlotState.makePlotStateWithJson(wpInit.plotState);
        var zf= plotState.getZoomLevel();

        var csys= CoordinateSys.parse(wpInit.imageCoordSys);


        var webPlot= {
            plotId,
            plotImageId     : plotId+'---NEEDS___INIT',
            serverImages    : wpInit.initImages,
            imageCoordSys   : csys,
            plotState,
            projection,
            dataWidth       : wpInit.dataWidth,
            dataHeight      : wpInit.dataHeight,
            imageScaleFactor: wpInit.imageScaleFactor,

            title : '',
            plotDesc        : wpInit.desc,
            dataDesc        : wpInit.dataDesc,
            webFitsData     : wpInit.fitsData,
            //=== Mutable =====================
            screenSize: {width:wpInit.dataWidth*zf, height:wpInit.dataHeight*zf},
            zoomFactor: zf,
            // percentOpaque   : 1.0,
            alive    : true,
            attributes,
            viewPort: WebPlot.makeViewPort(0,0,0,0),
            //=== End Mutable =====================
            asOverlay
        };

        return webPlot;
    },

    /**
     *
     * @param wpData
     * @param {{dim: {width: *, height: *}, x: *, y: *}} viewPort
     * @return {object} new webplot data
     */
    setWPViewPort(wpData,viewPort) {
        return Object.assign({},wpData,{viewPort});
    },


    /**
     *
     * @param wpData
     * @param {object} stateJson
     * @param {object} serverImages
     * @return {*}
     */
    setPlotState(wpData,stateJson,serverImages) {
        var plotState= PlotState.makePlotStateWithJson(stateJson);
        var zf= plotState.getZoomLevel();
        var screenSize= {width:wpData.dataWidth*zf, height:wpData.dataHeight*zf};
        var plot= Object.assign({},wpData,{plotState, zoomFactor:zf,screenSize});
        if (serverImages) plot.serverImages= serverImages;
        return plot;
    },


    /**
     * Make a viewport object
     * @param {number} x
     * @param {number} y
     * @param {number} width
     * @param {number} height
     * @return {{dim: {width: number, height: number}, x: number, y: number}}
     */
    makeViewPort(x,y,width,height) { return  {dim:{width,height},x,y}; }

};


/**
 * @param plot
 * @return {boolean}
 */
export function isBlankImage(plot) {
    if (plot.plotState.isThreeColor()) return false;
    var req= plot.plotState.getWebPlotRequest();
    return (req && req.getRequestType()===RequestType.BLANK);
}

/**
 *
 * @param wpData
 * @param {number} zoomFactor
 * @return {*}
 */
export function clonePlotWithZoom(wpData,zoomFactor) {
    var screenSize= {width:wpData.dataWidth*zoomFactor, height:wpData.dataHeight*zoomFactor};
    return Object.assign({},wpData,{zoomFactor,screenSize});
}


export function isRotatable(plot) {
    var proj= plot.getProjection();
    if (!proj || proj.isWrappingProjection()) return false;
    return Boolean(plot.attrributes[PlotAttribute.DISABLE_ROTATE_REASON]);
}


export function getNonRotatableReason(plot) {
    if (isRotatable(plot)) return '';
    var p= plot.projetion;
    var reason= plot.attributes[PlotAttribute.DISABLE_ROTATE_REASON];
    if (reason) {
        return isString(reason) ? reason : `FITS image can\'t be rotated`;
    }
    else {
        if (p.isWrappingProjection()) {
            return `FITS image with projection of type ${p.getProjectionName()} can't be rotated`;
        }
        else {
            return `FITS image can't be rotated`;
        }
    }
}

