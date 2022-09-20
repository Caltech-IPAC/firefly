/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty, isArray} from 'lodash';
import {Band} from './Band.js';
import {convertBandStateToJSON, makeBandState, makeBandStateWithJson} from './BandState.js';
import Enum from 'enum';


/**
 * @typedef RotateType
 * The type of rotation
 * can be 'NORTH', 'ANGLE', 'UNROTATE'
 * @prop NORTH
 * @prop ANGLE
 * @prop UNROTATE
 * @type {Enum}
 * @public
 * @global
 * */
export const RotateType= new Enum(['NORTH', 'ANGLE', 'UNROTATE']);

/**
 * @typedef Operation
 * can be 'ROTATE', 'CROP', 'FLIP_Y'
 * @prop CROP
 * @prop FLIP_Y
 * @type {Enum}
 */
export const Operation= new Enum(['ROTATE', 'CROP', 'FLIP_Y']);


export class PlotState {

    /**
     * @summary Contains data about the state of a plot.
     * This object is never created directly if is always instantiated from the json sent from the server.
     * @prop {boolean} threeColor - is a three color plot
     * @public
     */
    constructor() {
        this.bandStateAry= [null,null,null];
        this.ctxStr=null;
        this.threeColor= false;
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
                if (this.get(Band.RED).plotRequest)   this.usedBands.push(Band.RED);
                if (this.get(Band.GREEN).plotRequest) this.usedBands.push(Band.GREEN);
                if (this.get(Band.BLUE).plotRequest)  this.usedBands.push(Band.BLUE);
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
     *
     * @return {boolean}
     */
    isThreeColor() { return this.threeColor; }

    /**
     * @summary this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param {Band} [band] the band to get the request for, if not passed the used the primary band
     * @return {WebPlotRequest} the WebPlotRequest
     * @public
     */
    getWebPlotRequest(band) { return this.get(band || this.firstBand()).plotRequest; }
    /**
     * @summary if a cube, checkout how many images it contains
     * @param {Band} [band] the band check for, if not passed the used the primary band
     * @return {number} the WebPlotRequest
     * @public
     */
    getCubeCnt(band) { return this.get(band || this.firstBand()).cubeCnt; }


    getCubePlaneNumber(band) {
        return this.get(band || this.firstBand()).cubePlaneNumber;
    }

    /**
     * Get the range values for the plot.
     * @param {Band} [band] the band get range value for, parameter is unnecessary for non-three color plots
     * @return {RangeValues}
     */
    getRangeValues(band) { return this.get(band || this.firstBand()).rangeValues; }

    setRangeValues(band,rv) { this.get(band).rangeValues= rv;}

    /**
     * @param band
     * @return {string}
     */
    getWorkingFitsFileStr(band=undefined) { return this.get(band || this.firstBand()).workingFitsFileStr;}

    /**
     * @param band
     * @return {string}
     */
    getOriginalFitsFileStr(band) { return this.get(band || this.firstBand()).originalFitsFileStr; }


    getUploadFileName(band) { return this.get(band || this.firstBand()).uploadFileNameStr; }

    /**
     *
     * @param {Operation} op
     */
    hasOperation(op) {
        let newOp;
        if (op.key) {
            newOp= op;
        }
        else if (typeof op.toString === 'function') {
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

        if (!this.bandStateAry[idx]) this.bandStateAry[idx]= makeBandState();
        return this.bandStateAry[idx];
    }

    toJson(includeDirectAccessData= true) {
        return JSON.stringify(PlotState.convertToJSON(this, includeDirectAccessData));
    }

    copy() {
        return PlotState.makePlotStateWithJson(PlotState.convertToJSON(this,true));
    }

    static makePlotState() {
        return new PlotState();
    }

    static parse(jsonStr) {
        return PlotState.makePlotStateWithJson(JSON.parse(jsonStr));
    }

    /**
     * Create a plot state object from the json version of the object.
     * @param psJson
     * @param [overridePlotRequest] - if defined and not three color use this one instead of the on the the json object
     * @param [overrideRV] - if defined and not three color use this one instead of the on the the json object
     * @return {PlotState}
     */
    static makePlotStateWithJson(psJson, overridePlotRequest, overrideRV) {
        if (!psJson) return;
        const state= PlotState.makePlotState();
        // if not include used defaulted values
        state.threeColor= Boolean(psJson.threeColor);
        state.ops= psJson.ops?.map( (op) => Operation.get(op) ) ?? [];

        const {bandStateAry}= psJson;
        const ovPR= !state.threeColor ? overridePlotRequest : undefined;
        const ovRV= !state.threeColor ? overrideRV : undefined;

        if (isArray(bandStateAry)) {
            state.bandStateAry= bandStateAry.map( (bJ) => makeBandStateWithJson(bJ, ovPR,ovRV));
        }
        else {
            state.bandStateAry= [makeBandStateWithJson(bandStateAry, ovPR,ovRV)];
        }

        state.ctxStr=psJson.ctxStr;

        return state;
    }

    /**
     * @summary convert his PlotState to something can be used with JSON.stringify
     * @param {PlotState} s
     * @param {boolean} includeDirectAccessData include the includeDirectAccessData object
     */
    static convertToJSON(s, includeDirectAccessData= true) {
        if (!s) return undefined;
        const json= {};
        json.ctxStr=s.ctxStr;
        json.colorTableId= 0;

        if (!isEmpty(s.ops)) json.ops= s.ops.map( (op) => op.key );
        if (s.threeColor) json.threeColor= true;


        json.bandStateAry= s.bandStateAry.map( (bJ) => convertBandStateToJSON(bJ,includeDirectAccessData));
        return json;
    }

}

export function makePlotStateShimForHiPS(wpRequest) {
    const plotState= PlotState.makePlotState();
    const bandState= makeBandState();
    bandState.plotRequest= wpRequest;
    plotState.bandStateAry= [bandState,null,null];
    return plotState;
}


export default PlotState;


