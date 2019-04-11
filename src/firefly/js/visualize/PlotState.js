/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty, isArray} from 'lodash';
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
        this.zoomLevel= 1;
        this.threeColor= false;
        this.colorTableId= 0;
        this.rotationType= RotateType.UNROTATE;
        this.rotaNorthType= CoordinateSys.EQ_J2000;
        this.flippedY= false;
        this.rotationAngle= NaN;
        this.ops= [];
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
    getDirectFileAccessData(band) { return this.get(band).getDirectFileAccessData(); }


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
        else if (typeof band === 'number') {
            idx= band;
        }
        else {
            const b= Band.get(band.toString());
            idx= b ? b.value : Band.NO_BAND.value;
        }

        if (!this.bandStateAry[idx]) this.bandStateAry[idx]= BandState.makeBandState();
        return this.bandStateAry[idx];
    }

    toJson(includeDirectAccessData= true) {
        return JSON.stringify(PlotState.convertToJSON(this, includeDirectAccessData));
    }

    static makePlotState() {
        return new PlotState();
    }


    static makePlotStateWithJson(psJson) {
        if (!psJson) return null;
        const state= PlotState.makePlotState();

        if (isArray(psJson.bandStateAry)) {
            state.bandStateAry= psJson.bandStateAry.map( (bJ) => BandState.makeBandStateWithJson(bJ));
        }
        else {
            state.bandStateAry= [BandState.makeBandStateWithJson(psJson.bandStateAry)];
        }

        state.ctxStr=psJson.ctxStr;
        state.zoomLevel= psJson.zoomLevel;
        state.colorTableId= psJson.colorTableId || 0;

        // if not include used defaulted values
        state.multiImage= psJson.multiImage; // if multiImage is not default we don't care
        state.rotationType= psJson.rotationType ? RotateType.get(psJson.rotationType) : RotateType.UNROTATE;
        state.rotaNorthType= psJson.rotaNorthType ? CoordinateSys.parse(psJson.rotaNorthType) : CoordinateSys.EQ_J2000;
        state.rotationAngle= psJson.rotationAngle ? psJson.rotationAngle : NaN;
        state.flippedY= Boolean(psJson.flippedY);
        state.threeColor= Boolean(psJson.threeColor);
        state.ops= psJson.ops ? psJson.ops.map( (op) => Operation.get(op) ) :[];

        return state;
    }

    /**
     * @summary convert his PlotState to something can be used with JSON.stringify
     * @param {PlotState} s
     * @param {boolean} includeDirectAccessData include the includeDirectAccessData object
     */
    static convertToJSON(s, includeDirectAccessData= true) {
        if (!s) return null;
        const json= {};
        json.ctxStr=s.ctxStr;
        json.zoomLevel= s.zoomLevel;
        json.colorTableId= s.colorTableId;
        // json.rotationType= s.rotationType.key;
        // json.rotaNorthType= s.rotaNorthType.toString();
        // json.ops= s.ops.map( (op) => op.key );
        // json.threeColor= s.threeColor;
        // json.flippedY= s.flippedY;
        // json.rotationAngle= s.rotationAngle;


        // if not defaulted values, don't include

        if (s.multiImage) json.multiImage= s.multiImage;
        if (s.rotationType!==RotateType.UNROTATE) json.rotationType= s.rotationType.key;
        if (s.rotaNorthType!==CoordinateSys.EQ_J2000) json.rotaNorthType= s.rotaNorthType.toString();
        if (!isEmpty(s.ops)) json.ops= s.ops.map( (op) => op.key );
        if (s.threeColor) json.threeColor= true;
        if (s.flippedY) json.flippedY= true;
        if (!isNaN(s.rotationAngle)) json.rotationAngle= s.rotationAngle;


        json.bandStateAry= s.bandStateAry.map( (bJ) => BandState.convertToJSON(bJ,includeDirectAccessData));
        return json;
    }

}


export default PlotState;


