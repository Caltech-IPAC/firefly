/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Band from './Band.js';
import RangeValues from './RangeValues.js';
import WebPlotRequest from './WebPlotRequest.js';
import {WPConst} from './WebPlotRequest.js';
import CoordinateSys from './CoordSys.js';
import MiniFitsHeader from './MiniFitsHeader.js';
import join from 'underscore.string/join';
import {parseInt, parseBoolean, parseFloat, checkNull,getStringWithNull } from '../util/StringUtils.js';


const SPLIT_TOKEN= '--PlotState--';
const NO_CONTEXT = 'NoContext';
const MAX_BANDS= 3;


export const RotateType= new Enum(['NORTH', 'ANGLE', 'UNROTATE']);
export const Operation= new Enum(['ROTATE', 'CROP', 'FLIP_Y']);


export const MultiImageAction = new Enum([ 'GUESS',      // Default, guess between load first, and use all, depending on three color params
                                           'USE_FIRST',   // only valid option if loading a three color with multiple Request
                                           'MAKE_THREE_COLOR', // make a three color out of the first three images, not yet implemented
                                           'USE_ALL' // only valid in non three color, make a array of WebPlots
                                         ]);




/**
 * @author Trey Roby
 */
class PlotState {


    /**
     *
     * @param {object} params
     */
    constructor(params) {
        this.bandStateAry= [undefined,undefined,undefined];
        this.multiImage= MultiImageAction.GUESS;
        this.ctxStr=null;
        this.newPlot= false;
        this.zoomLevel= 1;
        this.threeColor= false;
        this.colorTableId= 0;
        this.rotationType= RotateType.UNROTATE;
        this.rotaNorthType= CoordinateSys.EQ_J2000;
        this.flippedY= false;
        this.rotationAngle= NaN;
        _ops= [];

        this.newPlot= true;
        if (!params) return;

        if (params.request) {
            this.setWebPlotRequest(params.request, Band.NO_BAND);
            this.multiImage= MultiImageAction.USE_ALL;


        }
        else if (params.threeColor) {
            this.threeColor= params.threeColor;
            this.multiImage= params.threeColor ? MultiImageAction.USE_FIRST : MultiImageAction.USE_ALL;
        }
        else if (params.redReq || params.greenReq || params.blueReq) {
            this.threeColor= true;
            this.multiImage= MultiImageAction.USE_FIRST;
            if (params.redReq) setWebPlotRequest(params.redReq, Band.RED);
            if (params.greenReq) setWebPlotRequest(params.greenReq, Band.GREEN);
            if (params.blueReq) setWebPlotRequest(params.blueReq, Band.BLUE);
        }
        else {

        }

    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    /**
     * @return {MultiImageAction}
     */
    getMultiImageAction() { return this.multiImage; }

    /**
     *
     * @param {MultiImageAction} multiImage
     */
    setMultiImageAction(multiImage) { this.multiImage= multiImage; }

    /**
     *
     * @return {Band}
     */
    firstBand() {
        var bandAry= getBands();
        return (bandAry) ? bandAry[0] : null;
    }

    /**
     *
     * @return {Array} an array of Band
     */
    getBands() {
        if (!this.usedBands) {
            this.usedBands= [];
            if (this.threeColor) {
                if (get(Band.RED).hasRequest())   this.usedBands.push(Band.RED);
                if (get(Band.GREEN).hasRequest()) this.usedBands.push(Band.GREEN);
                if (get(Band.BLUE).hasRequest())  this.usedBands.push(Band.BLUE);
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
     * @param {strig} ctxStr
     */
    setContextString(ctxStr) { this.ctxStr= ctxStr; }

    /**
     *
     * @return {boolean}
     */
    isNewPlot() { return this.newPlot; }

    /**
     *
     * @param {bolean} newPlot
     */
    setNewPlot(newPlot) { this.newPlot= newPlot; }

    /**
     *
     * @return {number}
     */
    getColorTableId() { return this.colorTableId; }

    /**
     *
     * @param {number} id
     */
    setColorTableId(id) { this.colorTableId= id; }

    /**
     *
     * @param {boolean} threeColor
     */
    setThreeColor(threeColor) { this.threeColor= threeColor; }

    /**
     *
     * @return {boolean}
     */
    isThreeColor() { return this.threeColor; }

    /**
     *
     * @return {Number}
     */
    getThumbnailSize() {
        return this.get(firstBand()).getWebPlotRequest().getThumbnailSize();
    }

    /**
     *
     * @param {number} z
     */
    setZoomLevel(z) {this.zoomLevel= z;}

    /**
     *
     * @return {number}
     */
    getZoomLevel() {return this.zoomLevel;}

    /**
     *
     * @param {RotateType} rotationType
     */
    setRotateType(rotationType) { this.rotationType= rotationType; }

    /**
     *
     * @return {RotateType}
     */
    getRotateType() {return this.rotationType;}

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
     * @param {CoordinateSys } csys
     */
    setRotateNorthType(csys) { this.rotaNorthType= csys; }

    /**
     *
     * @return {CoordinateSys}
     */
    public getRotateNorthType() {
        return this.rotaNorthType;
    }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param {WebPlotRequest}  plotRequests copy this request
     * @param {Band} band the band to set the request for
     * @param {boolean} initStretch - initialize the stretch, default to true
     */
    setWebPlotRequest(plotRequests, band, initStretch=true) {
        this.get(band).setWebPlotRequest(plotRequests);
        this._usedBands = null;
        if (initStretch) this.initColorStretch(plotRequests,band);
    }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param band the band to get the request for
     * @return {WebPlotRequest} the WebPlotRequest
     */
    getWebPlotRequest(band) { return this.get(band).getWebPlotRequest(); }

    /**
     * this method will make a copy of the primary WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @return {WebPlotRequest} the WebPlotRequest
     */
    getPrimaryWebPlotRequest() { return this.get(this.firstBand()).getWebPlotRequest(); }


    setBandVisible(band, visible) { this.get(band).setBandVisible(visible); }
    isBandVisible(band) { return  this.get(band).isBandVisible(); }


    isMultiImageFile(band) { return this.get(band).isMultiImageFile(); }

    /**
     *
     * @param {boolean} multiImageFile
     * @param {Band} band
     */
    setMultiImageFile(multiImageFile, band) { this.get(band).setMultiImageFile(multiImageFile); }


    getCubeCnt(band) { return this.get(band).getCubeCnt(); }
    setCubeCnt(cubeCnt, band) { this.get(band).setCubeCnt(cubeCnt); }


    getCubePlaneNumber(band) { return this.get(band).getCubePlaneNumber(); }
    setCubePlaneNumber(cubeIdx, band) { this.get(band).setCubePlaneNumber(cubeIdx); }


    /**
     *
     * @param {RangeValues} rangeValues
     * @param {boolean} multiImageFile
     * @param {Band} band
     */
    setRangeValues(rangeValues, band) { this.get(band).setRangeValues(rangeValues); }
    /**
     *
     * @return {RangeValues}
     */
    getRangeValues(band) { return this.get(band).getRangeValues(); }

    /**
     *
     * @return {RangeValues}
     */
    getPrimaryRangeValues() { return this.get(firstBand()).getRangeValues(); }


    /**
     * @param {MiniFitsHeader} header
     * @param {Band} band
     */
   setFitsHeader(header, band) { this.get(band).setFitsHeader(header); }


    /**
     *
     * @param band
     * @return {FileAndHeaderInfo}
     */
    public getFileAndHeaderInfo(band) {
        return this.get(band).getFileAndHeaderInfo();
    }


    /**
     *
     * @param band
     * @return {MiniFitsHeader}
     */
    getHeader(band) { return this.get(band).getHeader(); }


    /**
     * @return {string}
     */
    getWorkingFitsFileStr(band) { return band ? this.get(band).getWorkingFitsFileStr() : null; }
    setWorkingFitsFileStr(fileStr, band) { this.get(band).setWorkingFitsFileStr(fileStr); }

    /**
     * @return {string}
     */
    getOriginalFitsFileStr(band) { return band ? this.get(band).getOriginalFitsFileStr() : null; }
    setOriginalFitsFileStr(fileStr, band) { this.get(band).setOriginalFitsFileStr(fileStr); }


    getUploadFileName(band) { return band ? this.get(band).getUploadedFileName() : null; }
    setUploadFileName(fileStr, band) { this.get(band).setUploadedFileName(fileStr); }

    setImageIdx(idx, band) { this.get(band).setImageIdx(idx);}
    getImageIdx(band) { return this.get(band).getImageIdx(); }


    setOriginalImageIdx(idx, band) { this.get(band).setOriginalImageIdx(idx); }
    getOriginalImageIdx(band) { return this.get(band).getOriginalImageIdx(); }


    /**
     *
     * @param {Operation} op
     */
    addOperation(op) {if (!this.ops.indexOf(op)>-1) this.ops.push(op); }

    /**
     *
     * @param {Operation} op
     */
    removeOperation(op) {
        idx= this.ops.indexOf(op);
        if (idx>-1) this.ops.splice(idx,1);
    }

    /**
     *
     * @param {Operation} op
     */
    hasOperation(op) {return _ops.indexOf(op)>-1; }

    clearOperations() {this.ops=[]; }

    /**
     *
     * @return {Array} array of Operation
     */
    getOperations() { return _ops;}


    isFilesOriginal() {
        return this.getBands().every( band => this.get(band).isFileOriginal());
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

    toString() {

        var part1= join(SPLIT_TOKEN,
                this.multiImage,
                this.ctxStr,
                this.newPlot,
                this.zoomLevel,
                this.threeColor,
                this.colorTableId,
                this.rotationType,
                this.rotationAngle,
                this.flippedY,
                this.rotaNorthType.toString());

        var part2= this.bandStateAry.map( bandState => bandState ? bandState.serialize() : 'null').join(SPLIT_TOKEN);

        return join(SPLIT_TOKEN,part1,part2);

    }

    static parse(s) {
        if (!s) return null;

        var sAry= s.split(SPLIT_TOKEN,13);
        if (sAry.length<13) return null;

        var i= 0;
        var multiImage= MultiImageAction.get(sAry[i++]);
        var ctxStr= getStringWithNull(sAry[i++]);
        var newPlot= parseBoolean(sAry[i++]);
        var zoomLevel= parseFloat(sAry[i++], 1);
        var threeColor = parseBoolean(sAry[i++],false);
        var colorTableId= parseInt(sAry[i++], 0);
        var rotationType= RotateType.get(sAry[i++]);
        var rotationAngle= parseFloat(sAry[i++]);
        var flippedY= parseBoolean(sAry[i++],false);
        var rotaNorthType= CoordinateSys.parse(getStringWithNull(sAry[i++]));

        var bandStateAry= [undefined,undefined,undefined];
        for(let j= 0; (j<MAX_BANDS);j++) {
            bandStateAry[j]= BandState.parse(getStringWithNull(sAry[i++]));
        }

        var retval= new PlotState();
        retval.multiImage= multiImage;
        retval.ctxStr= ctxStr;
        retval.newPlot= newPlot;
        retval.zoomLevel= zoomLevel;
        retval.threeColor= threeColor;
        retval.colorTableId= colorTableId;
        retval.rotationType= rotationType;
        retval.rotaNorthType= rotaNorthType;
        retval.rotationAngle= rotationAngle;
        retval.flippedY = flippedY;
        retval.bandStateAry= bandStateAry;

        return retval;
    }

    equals(obj) {
        return (obj instanceof PlotState) ? this.toString()===obj.toString() : false;
    }


    /**
     * @param {Band} band
     * @return {BandState}
     */
    clearBand(band) {
        var idx= band.value;
        if (this.bandStateAry[idx]) {
            this.bandStateAry[idx]= null;
            this.setWebPlotRequest(null, band);
        }
    }

// =====================================================================
// -------------------- private Methods --------------------------------
// =====================================================================

    /**
     * @param {Band} band
     * @return {BandState}
     */
    get(band) {
        var idx= band.value;
        if (!this.bandStateAry[idx]) this.bandStateAry[idx]= new BandState();
        return this.bandStateAry[idx];
    }


    /**
     * @param {WebPlotRequest} request
     * @param {Band} band
     */
    initColorStretch(request, band) {
        if (request) {
            this.colorTableId= request.getInitialColorTable();
            if (request.containsParam(WPConst.INIT_RANGE_VALUES)) {
                var rvStr= request.getParam(WPConst.INIT_RANGE_VALUES);
                rv= RangeValues.parse(rvStr);
                if (rv) this.get(band).setRangeValues(rv);
            }
        }

    }



}

