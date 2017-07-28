/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import CoordinateSys from './CoordSys.js';
import VisUtil from './VisUtil.js';
import {makeRoughGuesser} from './ImageBoundsData.js';
import Point, {makeImageWorkSpacePt, makeImagePt,
               makeScreenPt, makeWorldPt, makeDevicePt, isValidPoint} from './Point.js';
import {Matrix} from 'transformation-matrix-js';


function convertToCorrect(wp) {
    if (!wp) return null;
    const csys= wp.getCoordSys();
    if (csys===CoordinateSys.SCREEN_PIXEL) {
        return makeScreenPt(wp.x, wp.y);
    }
    else if (csys===CoordinateSys.PIXEL) {
        return makeImagePt(wp.x, wp.y);
    }
    return wp;
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
 * @public
 */
export class CysConverter {

    /**
     * @param {object} plot
     * @param {object} [altAffTrans] an alternate transform to use
     * @public
     */
    constructor(plot, altAffTrans)  {
        this.plotImageId= plot.plotImageId;
        this.plotId = plot.plotId;
        this.plotState= plot.plotState;
        this.dataWidth= plot.dataWidth;
        this.dataHeight= plot.dataHeight;
        this.projection= plot.projection;
        this.zoomFactor= plot.zoomFactor;
        this.imageCoordSys= plot.imageCoordSys;
        this.inPlotRoughGuess= null;
        this.conversionCache= plot.conversionCache;
        this.screenSize= plot.screenSize;
        this.affTrans= altAffTrans || plot.affTrans;
        this.viewDim= plot.viewDim;
    }

    // isRotated() { return !Matrix.from(this.affTrans).isIdentity(); }

    /**
     *
     * @param {WorldPt} wp world point
     * @param {ImagePt} imp Image Point
     */
    putInConversionCache(wp, imp) {
        if (this.conversionCache.size<MAX_CACHE_ENTRIES) {
            this.conversionCache.set(wp.toString(), imp);
        }
    }

//========================================================================================
//----------------------------- pointIn Methods  -----------------------------------------
//========================================================================================

    /**
     * Determine if a world point is in data Area of the plot and is not null
     * @param {WorldPt} iwPt the point to test.
     * @returns {boolean} true if it is in the data boundaries, false if not.
     */
    imagePointInData(iwPt) {
        let retval= false;
        if (iwPt && this.pointInPlot(iwPt)) {
            const ipt= this.getImageCoords(iwPt);
            const x= ipt.x;
            const y= ipt.y;
            retval= (x >= 0 && x <= this.dataWidth && y >= 0 && y <= this.dataHeight );
        }
        return retval;
    }


    /**
     * Determine if a image point is in the plot boundaries and is not null.
     * @param {Point} pt the point to test.
     * @returns {boolean} true if it is in the boundaries, false if not.
     */
    pointInData(pt) {
        if (!isValidPoint(pt)) return false;
        return this.imagePointInData(this.getImageWorkSpaceCoords(pt));
    }


    /**
     * This method returns false it the point is definitely not in plot.  It returns true if the point might be in the plot.
     * Used for tossing out points that we know that are not in plot without having to do all the math.  It is much faster.
     * @param {WorldPt} wp
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
     * @param {ImagePt} ipt the point to test.
     * @returns {boolean} true if it is in the boundaries, false if not.
     */
    imageWorkSpacePtInPlot(ipt) {
        if (!ipt) return false;
        const {x,y}= ipt;
        return (x >= 0 && x <= this.dataWidth && y >= 0 && y <= this.dataHeight );
    }

    /**
     * Determine if a image point is in the plot boundaries and is not null
     * @param {Point} pt the point to test.
     * @return {boolean} true if it is in the boundaries, false if not.
     */
    pointInPlot(pt) {
        let retval= false;
        if (!isValidPoint(pt)) {
            return false;
        }
        else if (pt.type===Point.W_PT) {
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


    pointOnDisplay(pt) {
        if (!isValidPoint(pt)) return false;
        const devPt= this.getDeviceCoords(pt);
        if (!devPt) return false;
        const {x,y}= devPt;
        const {width,height}= this.viewDim;
        let retval= (x>=0 && y>=0 && x<=width && y<=height);
        if (retval) {
            const minSize= Math.min(width,height);
            const {width:sWidth,height:sHeight}= this.screenSize;
            if (sWidth< minSize || sHeight< minSize) {
               retval= this.pointInPlot(pt);
            }
        }
        return retval;
    }


//========================================================================================
//----------------------------- End pointIn Methods  -------------------------------------
//========================================================================================

//========================================================================================
//========================================================================================
//========================================================================================
//----------------------------- Conversion Methods Begin --------------------------------
//========================================================================================
//========================================================================================

//========================================================================================
//----------------------------- Conversion to ImageWorkSpacePt Methods  ------------------
//========================================================================================

    /**
     * Return the ImageWorkSpacePt coordinates for a given Pt
     * @param {object} pt the point to translate
     * @param {number} [altZoomLevel] only use this parameter it you want to compute the point for a zoom level that
     *                 if different than what the plotted zoom level
     * @return ImageWorkSpacePt the image workspace coordinates
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
            case Point.DEV_PT:
                retval= this.makeIWPtFromSPt(this.getScreenCoords(pt),altZoomLevel);
                break;
            case Point.W_PT:
                const checkedPt= convertToCorrect(pt);
                if (checkedPt.type===Point.W_PT) {
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
     * @description  Return a ImageWorkspacePt from the screen point.
     * @param {ScreenPt} screenPt
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
     * Return the ImagePt coordinates given Pt
     * @param {object} pt the point to translate
     * @return {ImagePt} the image coordinates
     */
    getImageCoords(pt) {
        if (!isValidPoint(pt)) return null;

        let retval = null;

        switch (pt.type) {
            case Point.IM_WS_PT:
                retval= CysConverter.makeIPtFromIWPt(pt);
                break;
            case Point.SPT:
            case Point.DEV_PT:
                retval= CysConverter.makeIPtFromIWPt(this.getImageWorkSpaceCoords(pt));
                break;
            case Point.IM_PT:
                retval = pt;
                break;
            case Point.W_PT:
                retval = this.getImageCoordsFromWorldPt(pt);
                break;
        }
        return retval;
    }


    /**
     * return a ImagePoint from a ImageWorkspace point
     * @param {ImageWpt} iwPt
     * returns {ImagePt}
     */
    static makeIPtFromIWPt(iwPt) {
        if (!iwPt) return null;
        return makeImagePt(iwPt.x, iwPt.y);
    }


    /**
     * @desc Return the image coordinates given a WorldPt class.
     * @param {WorldPt} wpt the class containing the point in sky coordinates
     * @returns {ImagePt} the translated coordinates
     */
    getImageCoordsFromWorldPt(wpt) {
        if (!wpt) return null;

        let retval;
        const checkedPt= convertToCorrect(wpt);
        if (checkedPt.type===Point.W_PT) {
            const originalWp= wpt;
            retval= this.conversionCache.get(checkedPt.toString() );
            if (!retval) {
                if (this.imageCoordSys!==wpt.getCoordSys()) {
                    wpt= VisUtil.convert(wpt,this.imageCoordSys);
                }
                const projPt= this.projection.getImageCoords(wpt.getLon(),wpt.getLat());
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
//----------------------------- Conversion to DevicePt Methods  --------------------------
//========================================================================================

    /**
     * Return the device coordinates given a Pt
     * @param pt the point to translate
     * @param {number} [altZoomLevel] only use this parameter it you want to compute the point for a zoom level that
     *                 if different than what the plotted zoom level
     * @param {object} [altTransform] an alternate affine transform to use
     * @return {DevicePt} the device coordinates
     */
    getDeviceCoords(pt, altZoomLevel, altTransform) {
        if (!isValidPoint(pt)) return null;

        let retval = null;

        switch (pt.type) {
            case Point.DEV_PT:
                retval= pt;
                break;
            case Point.IM_WS_PT:
            case Point.IM_PT:
            case Point.W_PT:
                if (!altZoomLevel && !altTransform) {
                    retval= this.makeDevicePtFromSp(this.getScreenCoords(pt)); // normal case
                }
                else {
                    // special case, used with thumbnail: ignore Viewport
                    retval= this.makeDevicePtFromSp(this.getScreenCoords(pt, altZoomLevel), altTransform);
                }

                break;
            case Point.SPT:
                retval= this.makeDevicePtFromSp(pt, altTransform);
                break;
                break;
        }
        return retval;
    }


    getDevicePtCoordsOptimize(wpt, retPt) {
        const success= this.getScreenCoordsOptimize(wpt,retPt);
        if (!success) return false;
        const {x,y}= Matrix.from(this.affTrans).applyToPoint(retPt.x,retPt.y);
        retPt.x= x;
        retPt.y= y;
        retPt.type= Point.DEV_PT;
        return true;
    }

    /**
     *
     * @param {Object} sp ScreenPt
     * @param {Object} altTransform
     * @return {DevicePt}
     */
     makeDevicePtFromSp(sp, altTransform) {
        if (!sp) return null;
        const {x,y}= Matrix.from(altTransform || this.affTrans).applyToPoint(sp.x,sp.y);
        return makeDevicePt(x,y);
    }

//========================================================================================
//----------------------------- Conversion to ScreenPt Methods  --------------------------
//========================================================================================

    /**
     * Return the screen coordinates given Pt
     * @param pt the point to translate
     * @param {number} [altZoomLevel] only use this parameter it you want to compute the point for a zoom level that
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
            case Point.DEV_PT:
                retval = this.makeSpFromDevPt(pt);
                break;
            case Point.W_PT:
                const checkedPt= convertToCorrect(pt);
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

    makeSpFromDevPt(devPt) {
         if (!devPt) return null;
         const {x,y}= Matrix.from(this.affTrans).inverse().applyToPoint(devPt.x,devPt.y);
         return makeScreenPt(x,y);
     }




    /**
     * @desc An optimized conversion of WorldPt to viewport point.
     * @param {Object} wpt a world pt
     * @param {Object} retPt mutable returned ViewPort Point, this object will be written to
     * @return {boolean} success or failure
     */
    getScreenCoordsOptimize(wpt, retPt) {
        let success= false;

        let imagePt= this.conversionCache.get(wpt.toString());

        if (!imagePt) {
            const csys= wpt.getCoordSys();
            if (csys===CoordinateSys.SCREEN_PIXEL) {
                retPt.x= wpt.x;
                retPt.y= wpt.y;
                success= true;
            }
            else if (csys===CoordinateSys.PIXEL) {
                this.makeScreenPtFromImPtOptimized(wpt,retPt);
            }
            else {
                const originalWp= wpt;
                if (this.imageCoordSys!==wpt.getCoordSys()) {
                    wpt= VisUtil.convert(wpt,this.imageCoordSys);
                }

                const  proj_pt= this.projection.getImageCoords(wpt.getLon(),wpt.getLat());
                if (proj_pt) {
                    const imageX= proj_pt.x  + 0.5;
                    const imageY= proj_pt.y  + 0.5;

                    imagePt= makeImagePt(imageX,imageY);
                    this.putInConversionCache(originalWp, imagePt);

                    this.makeScreenPtFromImPtOptimized(imagePt,retPt);
                    success= true;
                }
            }
        }
        else {
            this.makeScreenPtFromImPtOptimized(imagePt,retPt);
            success= true;
        }

        return success;
    }


    makeScreenPtFromImPtOptimized(imagePt,retPt) {
        if (!imagePt) return null;
        // convert image to image workspace
        const imageWorkspaceX= imagePt.x;
        const imageWorkspaceY= imagePt.y;

        // convert image workspace to screen
        const zFact= this.zoomFactor;
        retPt.x= imageWorkspaceX*zFact;
        retPt.y= (this.dataHeight - imageWorkspaceY) *zFact;
    }



    /**
     *
     * @param {Object} iwpt ImageWorkspacePt
     * @param {number} [altZoomLevel]
     */
    makeSPtFromIWPt(iwpt, altZoomLevel) {
        if (!iwpt) return null;
        const zoom= altZoomLevel || this.zoomFactor;
        return makeScreenPt(iwpt.x*zoom,
                            (this.dataHeight - iwpt.y) *zoom );
    }


//========================================================================================
//----------------------------- Conversion to WorldPt Methods  ---------------------------
//========================================================================================


    /**
     * @desc Return the sky coordinates given a image x (fsamp) and  y (fline)
     * @param {PointPt} pt  the point to convert
     * @param  {CoordinateSys} outputCoordSys (optional) The coordinate system to return, default to coordinate system of image
     * @returns {WorldPt} the translated coordinates
     */
    getWorldCoords(pt, outputCoordSys) {
        if (!isValidPoint(pt)) return null;

        let retval = null;

        switch (pt.type) {
            case Point.IM_PT:
                retval= this.makeWorldPtFromIPt(pt,outputCoordSys);
                break;
            case Point.IM_WS_PT:
            case Point.SPT:
            case Point.DEV_PT:
                retval= this.makeWorldPtFromIPt(this.getImageCoords(pt),outputCoordSys);
                break;
            case Point.W_PT:
                retval=  (outputCoordSys===pt.getCoordSys()) ? pt : VisUtil.convert(pt, outputCoordSys);
                break;
        }
        return retval;
    }


    makeWorldPtFromIPt( ipt, outputCoordSys) {
        if (!ipt) return null;
        let wpt = this.projection.getWorldCoords(ipt.x - .5 ,ipt.y - .5);
        if (wpt && outputCoordSys!==wpt.getCoordSys()) {
            wpt= VisUtil.convert(wpt, outputCoordSys);
        }
        return wpt;
    }



//========================================================================================
//========================================================================================
//----------------------------- Conversion Methods End -----------------------------------
//========================================================================================
//========================================================================================
//========================================================================================
//========================================================================================
//========================================================================================
//========================================================================================

    coordsWrap(wp1, wp2) {
        if (!wp1 || !wp2) return false;

        let retval= false;
        if (this.projection.isWrappingProjection()) {
            const  worldDist= VisUtil.computeDistance(wp1, wp2);
            const pix= this.projection.getPixelWidthDegree();
            const value1= worldDist/pix;

            const ip1= this.getImageWorkSpaceCoords(wp1);
            const ip2= this.getImageWorkSpaceCoords(wp2);
            if (ip1 && ip2) {
                const xDiff= ip1.x-ip2.x;
                const yDiff= ip1.y-ip2.y;
                const imageDist= Math.sqrt(xDiff*xDiff + yDiff*yDiff);
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
     * @param {Object} plot - the image
     * @param {object} [altAffTrans] an alternate transform to use
     * @returns {CysConverter}
     */
    static make(plot, altAffTrans) {
        return plot ? new CysConverter(plot, altAffTrans) : null;
    }
} //end of class definition



/**
 *
 * @param pt
 * @return {*}
 * @public
 * @memberof firefly.util.image.CCUtil
 *
 */
function getWorldPtRepresentation(pt) {
    if (!isValidPoint(pt)) return null;

    if (pt.type===Point.W_PT)           return pt;
    else if (pt.type=== Point.IM_WS_PT) return makeWorldPt(pt.x,pt.y, CoordinateSys.PIXEL);
    else if (pt.type=== Point.IM_PT)    return makeWorldPt(pt.x,pt.y, CoordinateSys.PIXEL);
    else if (pt.type=== Point.SPT)      return makeWorldPt(pt.x,pt.y, CoordinateSys.SCREEN_PIXEL);
}

/** part of lowLevelApi
 * @namespace firefly.util.image.CCUtil
 * @public
 */
export const CCUtil = {
    /**
     * Convert to ImageWorkSpace Point
     * @param {WebPlot} plot - the image
     * @param pt
     * @func  getImageWorkSpaceCoords
     * @memberof   firefly.util.image.CCUtil
     * @public
     */
    getImageWorkSpaceCoords : (plot,pt) => CysConverter.make(plot).getImageWorkSpaceCoords(pt),
    
    /**
     *
     * Convert to Image Point
     * @param {WebPlot} plot - the image
     * @param {object} pt - the point to convert
     * @return {ImagePt}
     * @function getImageCoords
     * @memberof  firefly.util.image.CCUtil
     * @public
     */
    getImageCoords: (plot,pt) => CysConverter.make(plot).getImageCoords(pt),
    
    /**
     *
     * Convert to Device point
     */
    /**
     * @param {WebPlot} plot - the image
     * @param {object} pt - the point to convert
     * @public
     * @memberof firefly.util.image.CCUtil
     */
    getDeviceCoords: (plot,pt) => CysConverter.make(plot).getDeviceCoords(pt),
    
    /*
     *
     * Convert to Screen Point
     * */
    /**
     * @param {WebPlot} plot - the image
     * @param {object} pt - the point to convert
     * @function  getScreenCoords
     * @memberof  firefly.util.image.CCUtil
     */
    getScreenCoords: (plot,pt) => CysConverter.make(plot).getScreenCoords(pt),
    
    /**
     *
     * Convert to World Point
     * @param {WebPlot} plot - the image
     * @param  pt - the point to convert
     * @return {WorldPt}
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

