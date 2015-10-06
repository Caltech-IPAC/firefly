/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */





import PlotState from './PlotState.js';
import PlotImages from './PlotImages.js';
import Band from './Band.js';
import WebFitsData from './WebFitsData.js';
import join from 'underscore.string/join';
import {parseInt, parseBoolean, parseFloat, checkNull,getStringWithNull } from '../util/StringUtils.js';

const SPLIT_TOKEN= "--InsertBandInitializer--";

class InsertBandInitializer {

    /**
     *
     * @param {PlotState} plotState
     * @param {PlotImages} images
     * @param {Band} band
     * @param {WebFitsData} fitsData
     * @param {string} dataDesc
     */
    constructor(plotState, images, band, fitsData, dataDesc) {
        this.plotState= plotState;
        this.initImages= images;
        this.band= band;
        this.fitsData= fitsData;
        this.dataDesc= dataDesc;
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================
    toString() {
        return join(SPLIT_TOKEN, this.initImages, this.plotState, this.fitsData, this.band, this.dataDesc);
    }

    static parse(s) {
        if (!s) return null;
        var sAry= s.split(SPLIT_TOKEN,6);
        if (sAry!==5) return null;

        var i= 0;
        var initImages= PlotImages.parse(sAry[i++]);
        var plotState= PlotState.parse(sAry[i++]);
        var fitsData= WebFitData.parse(sAry[i++]);
        var band= Band.parse(sAry[i++]);
        var dataDesc= getStringWithNull(sAry[i]);
        return new InsertBandInitializer(plotState,initImages,band,fitsData,dataDesc);
    }
}

export default InsertBandInitializer;
