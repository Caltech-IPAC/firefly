/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import CoordinateSys from './CoordSys.js';
import VisUtil from './VisUtil.js';
import SimpleMemCache from '../util/SimpleMemCache.js';
import {makeRoughGuesser} from './ImageBoundsData.js';
import Point, {makeImageWorkSpacePt, makeViewPortPt, makeImagePt,
               makeScreenPt, makeWorldPt, isValidPoint} from './Point.js';

function convertToCorrect(wp) {
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
}

const MAX_CACHE_ENTRIES = 38000; // set to never allows the cache array over 48000 with a 80% load factor


/**
 * This class is for conversion
 * Some include a Image coordinate system, a world coordinate system, and a screen
 * coordinate system.
 * <ul>
 * <li>The image coordinate system is the coordinate system of the data. 
 * <li>The world coordinate system is the system that the data represents
 *        (i.e. the coordinate system of the sky)
 * <li>Screen coordinates are the pixel values of the screen.
 * </ul>
 *
 */
export class CysConverter {

    /**
     *
     * @param {object} plot
     */
    constructor(plot)  {
        this.plotImageId= plot.plotImageId;
        this.plotState= plot.plotState;
        this.dataWidth= plot.dataWidth;
        this.dataHeight= plot.dataHeight;
        this.projection= plot.projection;
        this.viewPort= plot.viewPort;
        this.zoomFactor= plot.zoomFactor;
        this.imageCoordSys= plot.imageCoordSys;
        this.inPlotRoughGuess= null;
    }


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

//========================================================================================
//----------------------------- pointIn Methods  -----------------------------------------
//========================================================================================

    /**
     * Determine if a world point is in data Area of the plot and is not null
     * @param iwPt the point to test.
     * @return {boolean} true if it is in the data boundaries, false if not.
     */
    imagePointInData(iwPt) {
        var retval= false;
        if (iwPt && this.pointInPlot(iwPt)) {
            var ipt= this.getImageCoords(iwPt);
            var x= ipt.x;
            var y= ipt.y;
            retval= (x >= 0 && x <= this.dataWidth && y >= 0 && y <= this.dataHeight );
        }
        return retval;
    }


    /**
     * Determine if a image point is in the plot boundaries and is not null.
     * @param pt the point to test.
     * @return {boolean} true if it is in the boundaries, false if not.
     */
    pointInData(pt) {
        if (!isValidPoint(pt)) return false;
        return this.imagePointInData(this.getImageWorkSpaceCoords(pt));
    }


    /**
     * This method returns false it the point is definitely not in plot.  It returns true if the point might be in the plot.
     * Used for tossing out points that we know that are not in plot without having to do all the math.  It is much faster.
     * @return {boolean} true in we guess it might be in the bounds, false if we know that it is not in the bounds
     */
    pointInPlotRoughGuess(wp) {

        if (!this.projection.isWrappingProjection()) {
            if (!this.inPlotRoughGuess) this.inPlotRoughGuess= makeRoughGuesser( this);
            return this.inPlotRoughGuess(wp);
        }
        else {
            return true;
        }
    }


    /**
     * Determine if a image point is in the plot boundaries and is not null.
     * @param ipt the point to test.
     * @return boolean true if it is in the boundaries, false if not.
     */
    imageWorkSpacePtInPlot(ipt) {
        if (!ipt) return false;
        const {x,y}= ipt;
        return (x >= 0 && x <= this.dataWidth && y >= 0 && y <= this.dataHeight );
    }

    /**
     * Determine if a image point is in the plot boundaries and is not null
     * @param pt the point to test.
     * @return {boolean} true if it is in the boundaries, false if not.
     */
    pointInPlot(pt) {
        var retval= false;
        if (!isValidPoint(pt)) {
            return false;
        }
        else if (pt.type== Point.W_PT) {
            retval= this.pointInPlotRoughGuess(pt);
            if (retval) {
                retval= this.imageWorkSpacePtInPlot(this.getImageWorkSpaceCoords(pt));
            }
        }
        else  {
            retval= this.imageWorkSpacePtInPlot(this.getImageWorkSpaceCoords(pt));
        }
        return retval;
    }

    /**
     * test to see if the input is in the view port and is not null
     * @param vpt
     * @return {boolean}
     */
    viewPortPointInViewPort(vpt) {
        if (!vpt || !vpt.type || vpt.type!==Point.VP_PT) return false;
        const {x,y}= vpt;
        const {width,height}= this.viewPort.dim;
        return (x>=0 && y>=0 && x<=width && y<=height);
    }


    /**
     * Determine if a point is in the view port boundaries and is not null.
     * @param pt the point to test.
     * @return {boolean} true if it is in the boundaries, false if not.
     */
    pointInViewPort(pt) {
        if (!isValidPoint(pt)) return false;
        else return this.viewPortPointInViewPort(this.getViewPortCoords(pt));
    }


//========================================================================================
//----------------------------- End pointIn Methods  -------------------------------------
//========================================================================================

//========================================================================================
//----------------------------- Conversion to ImageWorkSpacePt Methods  ------------------
//========================================================================================

    /**
     * Return the ImageWorkSpacePt coordinates for a given Pt
     * @param {object} pt the point to translate
     * @param {number} [altZoomLevel], only use this parameter it you want to compute the point for a zoom level that
     *                 if different than what the plotted zoom level
     * @return getImageWorkSpaceCoords
     */
    getImageWorkSpaceCoords(pt, altZoomLevel) {
        if (!isValidPoint(pt)) return null;


        let retval= null;
        switch (pt.type) {
            case Point.IM_WS_PT:
                retval= pt;
                break;
            case Point.IM_PT:
                retval= makeImageWorkSpacePt(pt.x, pt.y);
                break;
            case Point.SPT:
                retval= this.makeIWPtFromSPt(pt,altZoomLevel);
                break;
            case Point.VP_PT: //first get screen point, then convert
                retval= this.makeIWPtFromSPt(this.getScreenCoords(pt),altZoomLevel);
                break;
            case Point.W_PT:
                var checkedPt= convertToCorrect(pt);
                if (checkedPt.type==Point.W_PT) {
                    retval= this.getImageWorkSpaceCoords(this.getImageCoords(checkedPt));
                }
                else {
                    retval= this.getImageWorkSpaceCoords(checkedPt, altZoomLevel);
                }
                break;
        }
        return retval;
    }

    /**
     * return a ImageWorkspacePt from the screen point
     * @param screenPt
     * @param {number} [altZoomLevel]
     */
    makeIWPtFromSPt(screenPt, altZoomLevel) {
        if (!screenPt) return null;
        const zoom= altZoomLevel || this.zoomFactor;
        return makeImageWorkSpacePt(screenPt.x / zoom, this.dataHeight-screenPt.y/zoom);
    }

//========================================================================================
//----------------------------- Conversion to ImageSpacePt Methods  ----------------------
//========================================================================================


    /**
     * @param {object} pt the point to translate
     * @return ImagePt the image coordinates
     */
    getImageCoords(pt) {
        if (!isValidPoint(pt)) return null;
        let retval = null;

        switch (pt.type) {
            case Point.IM_WS_PT:
                retval= CysConverter.makeIPtFromIWPt(pt);
                break;
            case Point.SPT:
                retval= CysConverter.makeIPtFromIWPt(this.getImageWorkSpaceCoords(pt));
                break;
            case Point.IM_PT:
                retval = pt;
                break;
            case Point.VP_PT:
                retval= CysConverter.makeIPtFromIWPt(this.getImageWorkSpaceCoords(pt));
                break;
            case Point.W_PT:
                retval = this.getImageCoordsFromWorldPt(pt);
                break;
        }
        return retval;
    }


    /**
     * return a ImagePoint from a ImageWorkspace point
     * @param iwPt
     */
    static makeIPtFromIWPt(iwPt) {
        if (!iwPt) return null;
        return makeImagePt(iwPt.x, iwPt.y);
    }


    /**
     * Return the image coordinates given a WorldPt class
     * @param wpt the class containing the point in sky coordinates
     * @return ImagePt the translated coordinates
     */
    getImageCoordsFromWorldPt(wpt) {
        if (!wpt) return null;

        var retval;
        var checkedPt= convertToCorrect(wpt);
        if (checkedPt.type===Point.W_PT) {
            var originalWp= wpt;
            retval= SimpleMemCache.get(this.plotImageId, checkedPt.toString() );
            if (!retval) {
                if (this.imageCoordSys!==wpt.getCoordSys()) {
                    wpt= VisUtil.convert(wpt,this.imageCoordSys);
                }
                var projPt= this.projection.getImageCoords(wpt.getLon(),wpt.getLat());
                retval= projPt ? makeImagePt( projPt.x+ 0.5 ,  projPt.y+ 0.5) : null;
                this.putInConversionCache(originalWp,retval);
            }
        }
        else {
            retval= this.getImageCoords(checkedPt);
        }
        return retval;
    }

//========================================================================================
//----------------------------- Conversion to ViewPortPt Methods  ------------------------
//========================================================================================


    /**
     * Return the ViewPortCoords coordinates given for Pt
     * @param pt the point to translate
     * @param {number} [altZoomLevel], only use this parameter it you want to compute the point for a zoom level that
     *                 if different than what the plotted zoom level
     * @return WorldPt the world coordinates
     */
    getViewPortCoords(pt, altZoomLevel) {
        if (!isValidPoint(pt)) return null;

        let retval = null;

        switch (pt.type) {
            case Point.IM_WS_PT:
                retval= this.makeVPtFromSPt(this.getScreenCoords(pt, altZoomLevel));
                break;
            case Point.SPT:
                retval= this.makeVPtFromSPt(pt);
                break;
            case Point.IM_PT:
                retval= this.makeVPtFromSPt(this.getScreenCoords(pt, altZoomLevel));
                break;
            case Point.VP_PT:
                retval = pt;
                break;
            case Point.W_PT:
                retval= this.makeVPtFromSPt(this.getScreenCoords(pt, altZoomLevel));
                break;
        }
        return retval;
    }


    /**
     * return a ViewPort Point from a ScreenPt
     * @param pt
     */
    makeVPtFromSPt(pt) {
        if (!pt) return null;
        var {x:vpX,y:vpY}= this.viewPort;
        return makeViewPortPt( pt.x-vpX, pt.y-vpY);
    }


    /**
     * An optimized conversion of WorldPt to viewport point.
     * @param {object} wpt a world pt
     * @param {object} retPt mutable returned ViewPort Point, this object will be written to
     * @return {boolean} success or failure
     */
    getViewPortCoordsOptimize(wpt, retPt) {
        var success= false;

        var  imagePt= SimpleMemCache.get(this.plotImageId, wpt.toString);

        if (!imagePt) {
            const csys= wpt.getCoordSys();
            if (csys===CoordinateSys.SCREEN_PIXEL) {
                retPt.x= wpt.x-this.viewPort.x;
                retPt.y= wpt.y- this.viewPort.y;
                success= true;
            }
            else if (csys===CoordinateSys.PIXEL) {
                const sp= this.getScreenCoords(makeImagePt(wpt.x,wpt.y));
                retPt.x= sp.x-this.viewPort.x;
                retPt.y= sp.y- this.viewPort.y;
                success= true;
            }
            else {
                const originalWp= wpt;
                if (this.imageCoordSys!==wpt.getCoordSys()) {
                    wpt= VisUtil.convert(wpt,this.imageCoordSys);
                }

                var  proj_pt= this.projection.getImageCoords(wpt.getLon(),wpt.getLat());
                if (proj_pt) {
                    const imageX= proj_pt.x  + 0.5;
                    const imageY= proj_pt.y  + 0.5;

                    imagePt= makeImagePt(imageX,imageY);
                    this.putInConversionCache(originalWp, imagePt);

                    this.makeVPtFromImPtOptimized(imagePt,retPt);
                    success= true;
                }
            }
        }
        else {
            this.makeVPtFromImPtOptimized(imagePt,retPt);
            success= true;
        }

        return success;
    }

    makeVPtFromImPtOptimized(imagePt,retPt) {
        if (!imagePt) return null;
        // convert image to image workspace
        var imageWorkspaceX= imagePt.x;
        var imageWorkspaceY= imagePt.y;

        // convert image workspace to screen
        const zFact= this.zoomFactor;
        var sx= Math.floor(imageWorkspaceX*zFact);
        var sy= Math.floor((this.dataHeight - imageWorkspaceY) *zFact);

        // convert screen to viewPort
        retPt.x=sx-this.viewPort.x;
        retPt.y=sy-this.viewPort.y;
    }


//========================================================================================
//----------------------------- Conversion to ScreenPt Methods  --------------------------
//========================================================================================

    /**
     * Return the screen coordinates given Pt
     * @param pt the point to translate
     * @param {number} [altZoomLevel], only use this parameter it you want to compute the point for a zoom level that
     *                 if different than what the plotted zoom level
     * @return ScreenPt the screen coordinates
     */
    getScreenCoords(pt, altZoomLevel) {
        if (!isValidPoint(pt)) return null;

        let retval = null;

        switch (pt.type) {
            case Point.IM_WS_PT:
                retval= this.makeSPtFromIWPt(pt, altZoomLevel);
                break;
            case Point.SPT:
                retval= pt;
                break;
            case Point.IM_PT:
                retval= this.makeSPtFromIWPt(this.getImageWorkSpaceCoords(pt), altZoomLevel);
                break;
            case Point.VP_PT:
                retval = makeScreenPt(pt.x+this.viewPort.x, pt.y+this.viewPort.y);
                break;
            case Point.W_PT:
                var checkedPt= convertToCorrect(pt);
                if (checkedPt) {
                    if (checkedPt.type===Point.W_PT) {
                        retval= this.makeSPtFromIWPt(this.getImageWorkSpaceCoords(checkedPt),altZoomLevel);
                    }
                    else {
                        retval= this.getScreenCoords(checkedPt, altZoomLevel);
                    }
                }
                break;
        }
        return retval;
    }


    /**
     *
     * @param {object} iwpt ImageWorkspacePt
     * @param {number} [altZoomLevel]
     */
    makeSPtFromIWPt(iwpt, altZoomLevel) {
        if (!iwpt) return null;
        const zoom= altZoomLevel || this.zoomFactor;
        return makeScreenPt(Math.floor(iwpt.x*zoom),
                            Math.floor((this.dataHeight - iwpt.y) *zoom) );
    }


//========================================================================================
//----------------------------- Conversion to WorldPt Methods  ---------------------------
//========================================================================================



    /**
     * Return the sky coordinates given a image x (fsamp) and  y (fline)
     * package in a ImageWorkSpacePt class
     */
    /**
     * @param pt  the point to convert
     * @param outputCoordSys (optional) The coordinate system to return, default to coordinate system of image
     * @return WorldPt the translated coordinates
     */
    getWorldCoords(pt, outputCoordSys) {
        if (!isValidPoint(pt)) return null;

        let retval = null;

        switch (pt.type) {
            case Point.IM_WS_PT:
                retval= this.makeWorldPtFromIPt(this.getImageCoords(pt),outputCoordSys);
                break;
            case Point.SPT:
                retval= this.makeWorldPtFromIPt(this.getImageCoords(pt),outputCoordSys);
                break;
            case Point.IM_PT:
                retval= this.makeWorldPtFromIPt(pt,outputCoordSys);
                break;
            case Point.VP_PT:
                retval= this.makeWorldPtFromIPt(this.getImageCoords(pt),outputCoordSys);
                break;
            case Point.W_PT:
                retval=  (outputCoordSys===pt.getCoordSys()) ? pt : VisUtil.convert(pt, outputCoordSys);
                break;
        }
        return retval;
    }


    makeWorldPtFromIPt( ipt, outputCoordSys) {
        if (ipt==null) return null;
        var wpt = this.projection.getWorldCoords(ipt.x - .5 ,ipt.y - .5);
        if (wpt && outputCoordSys!==wpt.getCoordSys()) {
            wpt= VisUtil.convert(wpt, outputCoordSys);
        }
        return wpt;
    }




//========================================================================================
//----------------------------- Conversion Methods End -----------------------------------
//========================================================================================

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
                var xDiff= ip1.x-ip2.x;
                var yDiff= ip1.y-ip2.y;
                var imageDist= Math.sqrt(xDiff*xDiff + yDiff*yDiff);
                retval= ((imageDist / value1) > 3);
            }
            else {
                retval= false;
            }
        }
        return retval;
    }

    getImagePixelScaleInDeg(){ return this.projection.getPixelScaleArcSec()/3600.0; }

    /**
     *
     * @param {object} plot - the image
     * @return {CysConverter}
     */
    static make(plot) {
        return plot ? new CysConverter(plot) : null;
    }
} //end of class definition



/**
 *
 * @param pt
 * @return {*}
 * @memberof firefly.util.image.CCUtil
 * @func getWorldPtRepresentation
 * @public
 */
function getWorldPtRepresentation(pt) {
    if (!isValidPoint(pt)) return null;

    if (pt.type===Point.W_PT)           return pt;
    else if (pt.type=== Point.IM_WS_PT) return makeWorldPt(pt.x,pt.y, CoordinateSys.PIXEL);
    else if (pt.type=== Point.IM_PT)    return makeWorldPt(pt.x,pt.y, CoordinateSys.PIXEL);
    else if (pt.type=== Point.SPT)      return makeWorldPt(pt.x,pt.y, CoordinateSys.SCREEN_PIXEL);
    else if (pt.type=== Point.VP_PT)    return makeWorldPt(pt.x,pt.y, CoordinateSys.SCREEN_PIXEL);
}

/** part of lowLevelApi
 * @public
 * @type {{getImageWorkSpaceCoords: CCUtil.getImageWorkSpaceCoords, getImageCoords: CCUtil.getImageCoords, getViewPortCoords: CCUtil.getViewPortCoords, getScreenCoords: CCUtil.getScreenCoords, getWorldCoords: CCUtil.getWorldCoords, getWorldPtRepresentation: getWorldPtRepresentation}}
 */
export const CCUtil = {
    /**
     * Convert to ImageWorkSpace Point
     * @param {object} plot - the image
     * @func  getImageWorkSpaceCoords
     * @memberof   firefly.util.image.CCUtil
     * @public
     *
     */

    getImageWorkSpaceCoords : (plot,pt) => CysConverter.make(plot).getImageWorkSpaceCoords(pt),
    
    /**
     *
     * Convert to Image Point
     * @param {object} plot - the image
     * @param {object} pt - the point to convert
     * @function getImageCoords
     * @memberof  firefly.util.image.CCUtil
     * @public
     */
    getImageCoords: (plot,pt) => CysConverter.make(plot).getImageCoords(pt),
    
    /**
     *
     * Convert to ViewPoint point
     */
    /**
     * @param {object} plot - the image
     * @param {object} pt - the point to convert
     * @function getViewPortCoords
     * @public
     * @memberof firefly.util.image.CCUtil
     */
    getViewPortCoords: (plot,pt) => CysConverter.make(plot).getViewPortCoords(pt),
    
    /*
     *
     * Convert to Screen Point
     * */
    /**
     * @param {object} plot - the image
     * @param {object} pt - the point to convert
     * @function  getScreenCoords
     * @memberof  firefly.util.image.CCUtil
     */
    getScreenCoords: (plot,pt) => CysConverter.make(plot).getScreenCoords(pt),
    
    /**
     *
     * Convert to World Point
     * @param {object} plot - the image
     * @param {object} pt - the point to convert
     * @function getWorldCoords
     * @memberof  firefly.util.image.CCUtil
     * @public
     */
    getWorldCoords: (plot,pt) => CysConverter.make(plot).getWorldCoords(pt),

    /**
     *
     * @ignore
     *
     */
    getWorldPtRepresentation
};


export default CysConverter;



