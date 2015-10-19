/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import PlotImages from './PlotImages.js';
import CoordinateSys from './CoordSys.js';
import PlotState from './PlotState.js';
import WebFitsData from './WebFitsData.js';
import join from 'underscore.string/join';
import {parseInt, checkNull,getStringWithNull } from '../util/StringUtils.js';


// todo: ProjectionSerializer
// todo: Projection

const SPLIT_TOKEN= '--WebPlotInitializer--';


class WebPlotInitializer {
    /**
     *
     * @param {PlotState} plotState
     * @param {PlotImages} images
     * @param {CoordinateSys} imageCoordSys
     * @param {Projection} projection, a projection object
     * @param {number} dataWidth
     * @param {number} dataHeight
     * @param {number} imageScaleFactor
     * @param {array} fitsData, an array by band of WebFitsData
     * @param {string} desc
     * @param {string} dataDesc
     */
    constructor(plotState, images, imageCoordSys, projection, dataWidth, dataHeight,
                imageScaleFactor, fitsData, desc, dataDesc) {

        this.plotState= plotState;
        this.initImages= images;
        this.imageCoordSys= imageCoordSys;
        this.projectionSerialized= ProjectionSerializer.serializeProjection(projection);
        this.dataWidth= dataWidth;
        this.dataHeight= dataHeight;
        this.imageScaleFactor= imageScaleFactor;
        this.fitsData= fitsData;
        this.desc= desc;
        this.dataDesc= dataDesc;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    /**
     *
     * @return {PlotState}
     */
    getPlotState() { return this.plotState; }


    /**
     * @return {CoordinateSys}
     */
    getCoordinatesOfPlot() { return this.imageCoordSys; }


    /**
     * @return {*}
     */
    getInitImages() { return this.initImages; }

    /**
     *
     * @return {Projection}
     */
    getProjection() {
        return ProjectionSerializer.deserializeProjection(this.projectionSerialized);
    }

    /**
     *
     * @return {number|*}
     */
    getDataWidth() { return this.dataWidth; }
    /**
     *
     * @return {number|*}
     */
    getDataHeight() { return this.dataHeight; }
    /**
     *
     * @return {number|*}
     */
    getImageScaleFactor() { return this.imageScaleFactor; }


    /**
     *
     * @return {WebFitsData[]}
     */
    getFitsData()  { return this.fitsData; }

    /**
     * @return {string}
     */
    getPlotDesc() { return this.desc; }


    /**
     *
     * @param {string} d the description
     */
    setPlotDesc(d) { this.desc= d; }

    /**
     *
     * @return {string}
     */
    getDataDesc() { return this.dataDesc; }

    toString() {
        var part1= join(SPLIT_TOKEN,
            this.imageCoordSys, this.projectionSerialized,
            this.dataWidth, this.dataHeight,
            this.imageScaleFactor, this.initImages,
            this.plotState, this.desc,
            this.dataDesc);

        this.fitsData.length= 3;
        var part2= this.fitsData.map( (fd) => fd ? fd.toString() : 'null');

        return join(SPLIT_TOKEN, part1, part2);
    }

    static parse(s) {
        if (!s) return null;
        var sAry= s.split(SPLIT_TOKEN,13);
        if (sAry.length<10 || sAry.length>12) return null;

        var i= 0;
        var imageCoordSys= CoordinateSys.parse(sAry[i++]);
        var projection= ProjectionSerializer.deserializeProjection(sAry[i++]);
        var dataWidth= parseInt(sAry[i++]);
        var dataHeight= parseInt(sAry[i++]);
        var imageScaleFactor= parseInt(sAry[i++]);
        var initImages= PlotImages.parse(sAry[i++]);
        var plotState= PlotState.parse(sAry[i++]);
        var desc= getStringWithNull(sAry[i++]);
        var dataDesc= getStringWithNull(sAry[i++]);
        var fitsData= [undefined,undefined,undefined];
        while (i<sAry.length) {
            fitsData.push(WebFitsData.parse(sAry[i++]));
        }
        return new WebPlotInitializer(
            plotState,initImages,imageCoordSys,projection,
            dataWidth,dataHeight,
            imageScaleFactor,fitsData,desc,dataDesc);
    }

}

export default WebPlotInitializer;
