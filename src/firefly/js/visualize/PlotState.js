/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {Band} from './Band.js';
import {BandState} from './BandState.js';
import {RangeValues} from './RangeValues.js';
import CoordinateSys from './CoordSys.js';
import Enum from 'enum';


export const RotateType= new Enum(['NORTH', 'ANGLE', 'UNROTATE']);
export const Operation= new Enum(['ROTATE', 'CROP', 'FLIP_Y']);


/**
 * private enum, just for consistency with server
 */
const MultiImageAction = new Enum([ 'GUESS',      // Default, guess between load first, and use all, depending on three color params
                                    'USE_FIRST',   // only valid option if loading a three color with multiple Request
                                    'MAKE_THREE_COLOR', // make a three color out of the first three images, not yet implemented
                                    'USE_ALL' // only valid in non three color, make a array of WebPlots
                                    ]);

export class PlotState {

    /**
     * new plot state
     */
    constructor() {
        this.bandStateAry= [null,null,null];
        this.multiImage= MultiImageAction.GUESS;
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
     * returns the first used band. It is possible that this method will return null.  You should always check.
     * @return {Band} the first name used.
     */
    firstBand() {
        var bandAry= this.getBands();
        return (bandAry) ? bandAry[0] : null;
    }

    /**
     * Get an array of used band.  It is possible that this routine will return a array of length 0
     * @return {Array} the bands in use
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
     * @return {boolean}
     */
    isBandUsed(band) {
        return this.getBands().indexOf(band)>-1;
    }

    /**
     *
     * @return {string}
     */
    getContextString() { return this.ctxStr; }

    /**
     *
     * @param {string} ctxStr
     */
    setContextString(ctxStr) { this.ctxStr= ctxStr; }

    /**
     *
     * @return {boolean}
     */
    isNewPlot() { return this.newPlot; }


    /**
     *
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
    getThumbnailSize() {
        return this.get(this.firstBand()).getWebPlotRequest().getThumbnailSize();
    }


    /**
     *
     * @return {number}
     */
    getZoomLevel() {return this.zoomLevel; }


    /**
     *
     * @return {RotateType}
     */
    getRotateType() {return this.rotationType; }

    isRotated() {return this.rotationType!==RotateType.UNROTATE;}

    /**
     *
     * @param {boolean} flippedY
     */
    setFlippedY(flippedY) { this.flippedY= flippedY; }

    /**
     *
     * @return {boolean}
     */
    isFlippedY() { return this.flippedY; }

    /**
     *
     * @param {Number} angle
     */
    setRotationAngle(angle) { this.rotationAngle= angle; }

    /**
     *
     * @return {Number}
     */
    getRotationAngle() { return this.rotationAngle; }


    /**
     *
     * @return {CoordinateSys}
     */
    getRotateNorthType() {
        return this.rotaNorthType;
    }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param [band] the band to get the request for
     * @return {WebPlotRequest} the WebPlotRequest
     */
    getWebPlotRequest(band) { return this.get(band || this.firstBand()).getWebPlotRequest(); }

    /**
     * this method will make a copy of the primary WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @return {WebPlotRequest} the WebPlotRequest
     */
    getPrimaryWebPlotRequest() { return this.get(this.firstBand()).getWebPlotRequest(); }


    setBandVisible(band, visible) { this.get(band).setBandVisible(visible); }
    isBandVisible(band) { return  this.get(band).isBandVisible(); }


    isMultiImageFile(band) { return this.get(band || this.firstBand()).isMultiImageFile(); }
    isPrimaryMultiImageFile() { return this.get(this.firstBand()).isMultiImageFile(); }


    getCubeCnt(band) { return this.get(band || this.firstBand()).getCubeCnt(); }
    getPrimaryCubeCnt() { return this.get(this.firstBand()).getCubeCnt(); }


    getCubePlaneNumber(band) {
        return this.get(band || this.firstBand()).getCubePlaneNumber();
    }
    getPrimaryCubePlaneNumber() { return this.get(this.firstBand()).getCubePlaneNumber(); }


    /**
     *
     * @param {RangeValues} rangeValues
     * @param {Band} band
     */
    setRangeValues(rangeValues, band) { this.get(band).setRangeValues(rangeValues); }
    /**
     *
     * @return {RangeValues}
     */
    getRangeValues(band) { return this.get(band || this.firstBand()).getRangeValues(); }

    /**
     *
     * @return {RangeValues}
     */
    getPrimaryRangeValues() { return this.get(this.firstBand()).getRangeValues(); }


    /**
     * @param {ClientFitsHeader} header
     * @param {Band} band
     */
   setFitsHeader(header, band) { this.get(band).setFitsHeader(header); }


    /**
     *
     * @param band
     * @return {FileAndHeaderInfo}
     */
    getFileAndHeaderInfo(band) {
        return this.get(band).getFileAndHeaderInfo();
    }


    /**
     *
     * @param band
     * @return {ClientFitsHeader}
     */
    getHeader(band) { return this.get(band).getHeader(); }


    /**
     * @return {string}
     */
    getWorkingFitsFileStr(band) { return band ? this.get(band).getWorkingFitsFileStr() : null; }

    /**
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
        var newOp;
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

    isFilesOriginal() {
        return this.getBands().every( (band) => this.get(band).isFileOriginal());
    }

    //toPrettyString() {
    //    s= "PlotState: ";
    //    var pr;
    //    for(Band band : getBands() ) {
    //        pr= get(band).getWebPlotRequest();
    //        if (pr!=null) s+= pr.prettyString() + ", ";
    //    }
    //    s+= "ctxStr: " + this.ctxStr +
    //        ", zoom: " + this.zoomLevel +
    //        ", color id: " + _colorTableId +
    //        ", 3 color: " + this.threeColor;
    //    return s;
    //
    //}

    serialize() { return this.toString(); }

    //toString() {
    //
    //    var part1= join(SPLIT_TOKEN,
    //            this.multiImage,
    //            this.ctxStr,
    //            this.newPlot,
    //            this.zoomLevel,
    //            this.threeColor,
    //            this.colorTableId,
    //            this.rotationType,
    //            this.rotationAngle,
    //            this.flippedY,
    //            this.rotaNorthType.toString());
    //
    //    var part2= this.bandStateAry.map( (bandState) => bandState ? bandState.serialize() : 'null').join(SPLIT_TOKEN);
    //
    //    return join(SPLIT_TOKEN,part1,part2);
    //
    //}
    //
    //static parse(s) {
    //    if (!s) return null;
    //
    //    var sAry= s.split(SPLIT_TOKEN,13);
    //    if (sAry.length<13) return null;
    //
    //    var i= 0;
    //    var multiImage= MultiImageAction.get(sAry[i++]);
    //    var ctxStr= getStringWithNull(sAry[i++]);
    //    var newPlot= parseBoolean(sAry[i++]);
    //    var zoomLevel= parseFloat(sAry[i++], 1);
    //    var threeColor = parseBoolean(sAry[i++],false);
    //    var colorTableId= parseInt(sAry[i++], 0);
    //    var rotationType= RotateType.get(sAry[i++]);
    //    var rotationAngle= parseFloat(sAry[i++]);
    //    var flippedY= parseBoolean(sAry[i++],false);
    //    var rotaNorthType= CoordinateSys.parse(getStringWithNull(sAry[i++]));
    //
    //    var bandStateAry= [undefined,undefined,undefined];
    //    for(let j= 0; (j<MAX_BANDS);j++) {
    //        bandStateAry[j]= BandState.parse(getStringWithNull(sAry[i++]));
    //    }
    //
    //    var retval= new PlotState();
    //    retval.multiImage= multiImage;
    //    retval.ctxStr= ctxStr;
    //    retval.newPlot= newPlot;
    //    retval.zoomLevel= zoomLevel;
    //    retval.threeColor= threeColor;
    //    retval.colorTableId= colorTableId;
    //    retval.rotationType= rotationType;
    //    retval.rotaNorthType= rotaNorthType;
    //    retval.rotationAngle= rotationAngle;
    //    retval.flippedY = flippedY;
    //    retval.bandStateAry= bandStateAry;
    //
    //    return retval;
    //}

    equals(obj) {
        return (obj instanceof PlotState) ? this.toJson()===obj.toJson() : false;
    }


    /**
     * @param {object} band
     * @return {BandState}
     */
    get(band) {
        var idx;
        if (band.value) {
            idx= band.value;
        }
        else if (typeof idx === 'number') {
            idx= band;
        }
        else {
            var b= Band.get(band.toString());
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
        var state= PlotState.makePlotState();

        state.bandStateAry= psJson.bandStateAry.map( (bJ) => BandState.makeBandStateWithJson(bJ));

        state.multiImage= MultiImageAction.get(psJson.multiImage);
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
     * convert his PlotState to something can be used with JSON.stringify
     * @param {PlotState} s
     */
    static convertToJSON(s) {
        if (!s) return null;
        var json= {};
        json.JSON=true;
        json.bandStateAry= s.bandStateAry.map( (bJ) => BandState.convertToJSON(bJ));
        json.multiImage= s.multiImage.key;
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


