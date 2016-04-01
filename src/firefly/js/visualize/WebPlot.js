/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import RequestType from './RequestType.js';
import CoordinateSys from './CoordSys.js';
import VisUtil from './VisUtil.js';
import {makeProjection} from './Projection.js';
import SimpleMemCache from '../util/SimpleMemCache.js';
import {makeRoughGuesser} from './ImageBoundsData.js';
import PlotState from './PlotState.js';
import Point, {makeImageWorkSpacePt, makeViewPortPt, makeImagePt,
               makeScreenPt, isValidPoint} from './Point.js';
//import PlotState, {RotateType} from './PlotState.js';



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



    UNIQUE_KEY : 'UNIQUE_KEY'
};


const convertToCorrect= function(wp) {
    if (!wp) return null;
    var csys= wp.getCoordSys();
    var retPt= wp;
    if (csys===CoordinateSys.SCREEN_PIXEL) {
        retPt= makeScreenPt(wp.x, wp.y);
    }
    else if (csys===CoordinateSys.PIXEL) {
        retPt= makeImagePt(wp.x, wp.y);
    }
    return retPt;
};


const MAX_CACHE_ENTRIES = 38000; // set to never allows the cache array over 48000 with a 80% load factor


/**
 * This class contains plot information.
 * Publicly this class operations in many coordinate system.
 * Some include a Image coordinate system, a world coordinate system, and a screen
 * coordinate system.
 * <ul>
 * <li>The image coordinate system is the coordinate system of the data. 
 * <li>The world coordinate system is the system that the data represents
 *        (i.e. the coordinate system of the sky)
 * <li>Screen coordinates are the pixel values of the screen.
 * </ul>
 *
 * @author Trey Roby
 * @version $Id: WebPlot.java,v 1.68 2012/12/14 23:59:58 roby Exp $
 */
export class WebPlot {

    /**
     *
     * @param {object} wpData
     */
    constructor(wpData)  {
        Object.assign(this,wpData);

    }

    // =======================================================================
    // ------------------    constants for Attributes -------------------------
    // =======================================================================

    /**
     *
     * @param wp world point
     * @param imp Image Point
     */
    putInConversionCache(wp, imp) {
        if (SimpleMemCache.size(this.plotImageId)<MAX_CACHE_ENTRIES) {
            SimpleMemCache.set(this.plotImageId, wp.toString(), imp);
        }
    }



    //public void refreshWidget() { _tileDrawer.refreshWidget(); } //todo - must do something
    //public void refreshWidget(PlotImages images) { _tileDrawer.refreshWidget(images); }

    getPlotState() { return this.plotState; }

    getZoomFact() { return this.plotState.getZoomLevel(); }

    /**
     *
     * @param {Band} band
     * @return {*}
     */
    getFitsDataByBand(band) {
        return this.webFitsData[band.value];
    }


    /**
     *
     * @param {Band} band
     * @return {RangeValues}
     */
    getRangeValuesSerialized(band) {
        var rv = this.plotState.getRangeValues(band);
        return rv ? rv.serialize() : null;
    }


    /**
     * @param {Band} band
     * return {WebFitsData}
     */
    getFitsData(band) {
        return this.webFitsData[band.value];
    }

    /**
     *
     * @return {boolean}
     */
    isCube() {
        return this.plotState.isMultiImageFile() && this.plotState.getCubeCnt()>0;
    }

    getCubeCnt() { return this.plotState.getCubeCnt(); }

    getCubePlaneNumber() { return this.plotState.getCubePlaneNumber(); }

    getImageWidth()  { return this.dataWidth;   }
    getImageHeight()  { return this.dataHeight;   }


    /**
     * returns the first used band. It is possible that this method will return null.  You should always check.
     * @return {Band} the first name used.
     */
    getFirstBand() { return this.plotState.firstBand(); }

    /**
     * Get an array of used band.  It is possible that this routine will return a array of length 0
     * @return {array} the bands in use
     */
    getBands() { return this.plotState.getBands(); }

    /**
     *
     * @return {boolean}
     */
    isThreeColor()  { return this.plotState.isThreeColor(); }

    getColorTableID() { return this.plotState.getColorTableId(); }

    /**
     * This method will return the width of the image in screen coordinates.
     * This number will change as the plot is zoomed up and down.
     * @return {number} the width of the plot
     */
    getScreenWidth() { return this.screenSize.width; }

    /**
     *  This method will return the height of the image in screen coordinates.
     *  This number will change as the plot is zoomed up and down.
     * @return {number} the height of the plot
     */
    getScreenHeight() { return this.screenSize.height; }

    /**
     * This method will return the width of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return {number} the width of the image data
     */
    getImageDataWidth() { return this.dataWidth; }

    /**
     * This method will return the height of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return {number} the height of the image data
     */
    getImageDataHeight() { return this.dataHeight; }

    /**
     * This method will return the width of the image in the world coordinate
     * system (probably degrees on the sky).
     * @return {number} the width of the image data in world coord system.
     */
    getWorldPlotWidth() {
        return this.projection.getPixelWidthDegree() * this.dataWidth;
    }

    /**
     * This method will return the height of the image in the world coordinate
     * system (probably degrees on the sky).
     * @return {number} the height of the image data in world coord system.
     */
    getWorldPlotHeight() {
        return this.projection.getPixelHeightDegree() * this.dataHeight;
    }

    /**
     *
     * @return {Projection}
     */
    getProjection() { return this.projection; }



    /**
     * get the coordinate system of the plot.
     * @return  {CoordinateSys}  the coordinate system.
     */
    getCoordinatesOfPlot() { return this.imageCoordSys; }

    /**
     * get the scale (in arcseconds) that one image pixel of data represents.
     * @return {number} double the scale of one pixel.
     */
    getImagePixelScaleInArcSec(){ return this.projection.getPixelScaleArcSec(); }

    getImagePixelScaleInDeg(){ return this.projection.getPixelScaleArcSec()/3600.0; }



    /**
     */
    freeResources() {
        this.alive= false;
    }

    isAlive() { return this.alive; }

    setAttribute(key, attribute) { this.attributes[key]= attribute; }
    removeAttribute(key) { Reflect.deleteProperty(this.attributes,key); }

    getAttribute(key) { return this.attributes[key]; }

    containsAttributeKey(key) { return this.attributes[key]!==undefined; }

    /**
     * Get the description of this plot.
     * @return String the plot description
     */
    getPlotDesc() { return this.plotDesc; }

    /**
     * Get the description of this fits data for this plot.
     * @return String the plot description
     */
    getDataDesc() { return this.dataDesc; }


    //setPercentOpaque(percentOpaque) {
    //     this.percentOpaque= percentOpaque;
    //}

    getPercentOpaque() { return this.percentOpaque; }

    isRotated() { return this.plotState.isRotated(); }

    getRotationType() {
        return this.plotState.getRotateType();
    }

    getRotationAngle() { return this.plotState.getRotationAngle(); }


    isRotatable() {
        var retval= false;
        var proj= this.getProjection();
        if (proj) {
            retval= !proj.isWrappingProjection();
            if (retval) {
                retval= !this.containsAttributeKey(PlotAttribute.DISABLE_ROTATE_REASON);
            }
        }
        return retval;
    }


    getNonRotatableReason() {
        var retval= null;
        if (!this.isRotatable()) {
            var p= this.getProjection();
            var rKey= PlotAttribute.DISABLE_ROTATE_REASON;
            if (this.containsAttributeKey(rKey)) {
                retval= this.getAttribute(rKey) ? this.getAttribute(rKey) :
                                                  `FITS image can't be rotated`;
            }
            else {
                if (p.isWrappingProjection()) {
                    retval= 'FITS image with projection of type ' +
                            p.getProjectionName() + ` can't be rotated`;
                }
                else {
                    retval= `FITS image can't be rotated`;
                }
            }
        }
        return retval;
    }



    coordsWrap(wp1, wp2) {
        if (!wp1 || !wp2) return false;

        var retval= false;
        if (this.projection.isWrappingProjection()) {
            var  worldDist= VisUtil.computeDistance(wp1, wp2);
            var pix= this.projection.getPixelWidthDegree();
            var value1= worldDist/pix;

            var ip1= this.getImageWorkSpaceCoords(wp1);
            var ip2= this.getImageWorkSpaceCoords(wp2);
            if (ip1 && ip2) {
                var xdiff= ip1.x-ip2.x;
                var ydiff= ip1.y-ip2.y;
                var imageDist= Math.sqrt(xdiff*xdiff + ydiff*ydiff);
                retval= ((imageDist / value1) > 3);
            }
            else {
                retval= false;
            }
        }
        return retval;
    }


    //============================================================
    //============================================================
    //--- Static WebPlot Util methods
    //============================================================
    //============================================================


    /**
     *
     * @param plotId
     * @param wpInit init data returned from server
     * @param asOverlay
     * @return {{plotId: *, plotImageId: string, serverImages: *, imageCoordSys: *, plotState: *, projection: {isWrapperProjection, getPixelWidthDegree, getPixelHeightDegree, getPixelScaleArcSec, isWrappingProjection, getImageCoords, getWorldCoords}, dataWidth: *, dataHeight: *, imageScaleFactor: *, plotDesc: (*|string|string|string|string|string), dataDesc: *, webFitsData: *, screenSize: {width: number, height: number}, percentOpaque: number, alive: boolean, attributes: {}, viewPort: ({dim, x, y}|{dim: {width: number, height: number}, x: number, y: number}), asOverlay: boolean}}
     */
    static makeWebPlotData(plotId, wpInit, asOverlay= false) {

        var projection= makeProjection(wpInit.projection);
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
            percentOpaque   : 1.0,
            alive    : true,
            attributes: {},
            viewPort: WebPlot.makeViewPort(0,0,0,0),
            //=== End Mutable =====================
            asOverlay
        };

        return webPlot;
    }

    /**
     *
     * @param wpData
     * @param {{dim: {width: *, height: *}, x: *, y: *}} viewPort
     * @return {object} new webplot data
     */
    static setWPViewPort(wpData,viewPort) {
        return Object.assign({},wpData,{viewPort});
    }


    /**
     *
     * @param wpData
     * @param {object} stateJson
     * @param {object} serverImages
     * @return {*}
     */
    static setPlotState(wpData,stateJson,serverImages) {
        var plotState= PlotState.makePlotStateWithJson(stateJson);
        var zf= plotState.getZoomLevel();
        var screenSize= {width:wpData.dataWidth*zf, height:wpData.dataHeight*zf};
        var plot= Object.assign({},wpData,{plotState, zoomFactor:zf,screenSize});
        if (serverImages) plot.serverImages= serverImages;
        return plot;
    }


    /**
     * add an attribute to the webplot data a return a new version
     * @param wpData
     * @param {string} attName
     * @param attValue
     * @return {*} new version of webplotdata
     */
    //static addWPAttribute(wpData,attName,attValue) {
    //    var att= Object.assign({},wpData.attributes, {[attName]:attValue});
    //    return Object.assign({},wpData,{attributes:att});
    //}

    /**
     *
     * @param {object} plotData, the plotData as it is kept in the store
     * @return {WebPlot}
     */
    static makeWebPlot(plotData) {
        return new WebPlot(plotData);
    }

    /**
     * Make a viewport object
     * @param {number} x
     * @param {number} y
     * @param {number} width
     * @param {number} height
     * @return {{dim: {width: number, height: number}, x: number, y: number}}
     */
    static makeViewPort(x,y,width,height) { return  {dim:{width,height},x,y}; }

};


/**
 *
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



export default WebPlot;



