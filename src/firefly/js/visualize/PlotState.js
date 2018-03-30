/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {Band} from './Band.js';
import {BandState} from './BandState.js';
import CoordinateSys from './CoordSys.js';
import Enum from 'enum';


/**
 * The type of rotation
 * can be 'NORTH', 'ANGLE', 'UNROTATE'
 * @public
 * @global
 * */
export const RotateType= new Enum(['NORTH', 'ANGLE', 'UNROTATE']);

/**
 * can be 'ROTATE', 'CROP', 'FLIP_Y'
 *
 */
export const Operation= new Enum(['ROTATE', 'CROP', 'FLIP_Y']);







export class PlotState {

    /**
     * @summary Contains data about the state of a plot.
     * This object is never created directly if is always instantiated from the json sent from the server.
     * @prop {number} zoomLevel - the zoomlevel of the image
     * @prop {boolean} threeColor - is a three color plot
     * @prop {number} colorTableId - the id of the color table in use
     * @prop {boolean} flippedY - fliped on the y axis
     * @prop {number} rotationAngle - if rotated by angle, then the angle  of the rotation
     * @prop {RotateType} rotationType the type of rotations
     * @public
     */
    constructor() {

        this.bandStateAry= [null,null,null];
        this.ctxStr=null;
        this.newPlot= true;
        this.zoomLevel= 1;
        this.threeColor= false;
        this.colorTableId= 0;
        this.rotationType= RotateType.UNROTATE;
        this.rotaNorthType= CoordinateSys.EQ_J2000;
        this.flippedY= false;
        this.rotationAngle= NaN;
        this.ops= [];
        this.newPlot= true;
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    /**
     * @summary returns the first used band. It is possible that this method will return null.  You should always check.
     * @return {Band} the first name used.
     * @public
     */
    firstBand() {
        const bandAry= this.getBands();
        return bandAry && bandAry[0];
    }

    /**
     * @summary Get an array of used band.  It is possible that this routine will return a array of length 0
     * @return {Array} the bands in use
     * @public
     */
    getBands() {
        if (!this.usedBands) {
            this.usedBands= [];
            if (this.threeColor) {
                if (this.get(Band.RED).hasRequest())   this.usedBands.push(Band.RED);
                if (this.get(Band.GREEN).hasRequest()) this.usedBands.push(Band.GREEN);
                if (this.get(Band.BLUE).hasRequest())  this.usedBands.push(Band.BLUE);
            }
            else {
                this.usedBands.push(Band.NO_BAND);
            }
        }
        return this.usedBands;
    }

    /**
     * @param {String|Band} [band] the bad to test
     * @return {boolean}
     * @public
     */
    isBandUsed(band) {
        return this.getBands().indexOf(band)>-1;
    }

    /**
     * Get the number of the color table
     * @return {number}
     */
    getColorTableId() { return this.colorTableId; }

    /**
     *
     * @return {boolean}
     */
    isThreeColor() { return this.threeColor; }


    /**
     *
     * @return {number}
     */
    getZoomLevel() {return this.zoomLevel; }

    /**
     *
     * @return {boolean}
     */
    isFlippedY() { return this.flippedY; }


    /**
     * @summary this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param {Band} [band] the band to get the request for, if not passed the used the primary band
     * @return {WebPlotRequest} the WebPlotRequest
     * @public
     */
    getWebPlotRequest(band) { return this.get(band || this.firstBand()).getWebPlotRequest(); }


    /**
     * @summary Check to see it this plot is from a multi image file
     * @param {Band} [band] the band check for, if not passed the used the primary band
     * @return {boolean} the WebPlotRequest
     * @public
     */
    isMultiImageFile(band = undefined) { return this.get(band || this.firstBand()).isMultiImageFile(); }

    /**
     * @summary if a cube, checkout how many images it contains
     * @param {Band} [band] the band check for, if not passed the used the primary band
     * @return {number} the WebPlotRequest
     * @public
     */
    getCubeCnt(band) { return this.get(band || this.firstBand()).getCubeCnt(); }


    getCubePlaneNumber(band) {
        return this.get(band || this.firstBand()).getCubePlaneNumber();
    }

    /**
     * Get the range values for the plot.
     * @param {band} [band] the band get range value for, parameter is unnecessary for non-three color plots
     * @return {RangeValues}
     */
    getRangeValues(band) { return this.get(band || this.firstBand()).getRangeValues(); }


    /**
     *
     * @param band
     * @return {ClientFitsHeader}
     */
    getHeader(band) { return this.get(band).getHeader(); }


    /**
     * @param band
     * @return {string}
     */
    getWorkingFitsFileStr(band) { return band ? this.get(band).getWorkingFitsFileStr() : null; }

    /**
     * @param band
     * @return {string}
     */
    getOriginalFitsFileStr(band) { return band ? this.get(band).getOriginalFitsFileStr() : null; }


    getUploadFileName(band) { return band ? this.get(band).getUploadedFileName() : null; }

    getImageIdx(band) { return this.get(band).getImageIdx(); }


    getOriginalImageIdx(band) { return this.get(band).getOriginalImageIdx(); }

    /**
     *
     * @param {Operation} op
     */
    hasOperation(op) {
        let newOp;
        if (op.key) {
            newOp= op;
        }
        else if (op.toString === 'function') {
            newOp= Operation.get(op.toString());
        }
        else if (typeof op === 'string') {
            newOp= Operation.get(op);
        }
        return newOp ? this.ops.indexOf(newOp)>-1  : false;
    }

    equals(obj) {
        return (obj instanceof PlotState) ? this.toJson()===obj.toJson() : false;
    }


    /**
     * @param {object} band
     * @return {BandState}
     */
    get(band) {
        let idx;
        if (band.value) {
            idx= band.value;
        }
        else if (typeof idx === 'number') {
            idx= band;
        }
        else {
            const b= Band.get(band.toString());
            idx= b ? b.value : Band.NO_BAND.value;
        }

        if (!this.bandStateAry[idx]) this.bandStateAry[idx]= BandState.makeBandState();
        return this.bandStateAry[idx];
    }

    toJson() {
        return JSON.stringify(PlotState.convertToJSON(this));
    }

    static makePlotState() {
        return new PlotState();
    }


    static makePlotStateWithJson(psJson) {
        if (!psJson) return null;
        const state= PlotState.makePlotState();

        state.bandStateAry= psJson.bandStateAry.map( (bJ) => BandState.makeBandStateWithJson(bJ));

        state.multiImage= psJson.multiImage;
        state.tileCompress = psJson.tileCompress;
        state.rotationType= RotateType.get(psJson.rotationType);
        state.rotaNorthType= CoordinateSys.parse(psJson.rotaNorthType);
        state.ops= psJson.ops.map( (op) => Operation.get(op) );
        state.ctxStr=psJson.ctxStr;
        state.zoomLevel= psJson.zoomLevel;
        state.threeColor= psJson.threeColor;
        state.colorTableId= psJson.colorTableId;
        state.flippedY= psJson.flippedY;
        state.rotationAngle= psJson.rotationAngle;
        state.newPlot= psJson.newPlot;

        return state;
    }

    /**
     * @summary convert his PlotState to something can be used with JSON.stringify
     * @param {PlotState} s
     */
    static convertToJSON(s) {
        if (!s) return null;
        const json= {};
        json.JSON=true;
        json.bandStateAry= s.bandStateAry.map( (bJ) => BandState.convertToJSON(bJ));
        json.multiImage= s.multiImage;
        json.tileCompress = s.tileCompress;
        json.rotationType= s.rotationType.key;
        json.rotaNorthType= s.rotaNorthType.toString();
        json.ops= s.ops.map( (op) => op.key );
        json.ctxStr=s.ctxStr;
        json.newPlot= s.newPlot;
        json.zoomLevel= s.zoomLevel;
        json.threeColor= s.threeColor;
        json.colorTableId= s.colorTableId;
        json.flippedY= s.flippedY;
        json.rotationAngle= s.rotationAngle;
        return json;
    }

}


export default PlotState;


