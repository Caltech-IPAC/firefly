/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import PlotState from './PlotState.js';
import PlotImages from './PlotImages.js';
import WebPlotInitializer from './WebPlotInitializer.js';
import WebPlotResult from './WebPlotResult.js';
import InsertBandInitializer from './InsertBandInitializer.js';


export const parse= function(inData) {
    var retval= {success:inData.success, progressKey:inData.progressKey };
    if (inData.success) {
        var stateStr= inData[WebPlotResult.PLOT_STATE];
        if (stateStr) retval[WebPlotResult.PLOT_STATE]= PlotState.parse(stateStr);


        var imagesStr= inData[WebPlotResult.PLOT_IMAGES];
        if (imagesStr) retval[WebPlotResult.PLOT_IMAGES]= PlotImages.parse(imagesStr);


        var wpInitStr= inData[WebPlotResult.INSERT_BAND_INIT];
        if (wpInitStr) retval[WebPlotResult.INSERT_BAND_INIT]= InsertBandInitializer.parse(wpInitStr);

        var creatorAry= inData[WebPlotResult.PLOT_CREATE];
        if (creatorAry) {
            //TODO: finish this.
            //TODO: convert: WebPlotInitializer
            //TODO: convert: CreateResults
            //TODO: go look up in the java class

            var wpInitList= creatorAry.map( (s) => WebPlotInitializer.parse(s));

            retval[WebPlotResult.PLOT_CREATE]= new CreatorResults(wpInitList);
        }


        retval[WebPlotResult.STRING]= inData[WebPlotResult.STRING];
        retval[WebPlotResult.DATA_HIST_IMAGE_URL]= inData[WebPlotResult.DATA_HIST_IMAGE_URL];
        retval[WebPlotResult.CBAR_IMAGE_URL]= inData[WebPlotResult.CBAR_IMAGE_URL];
        retval[WebPlotResult.IMAGE_FILE_NAME]= inData[WebPlotResult.IMAGE_FILE_NAME];
        retval[WebPlotResult.REGION_FILE_NAME]= inData[WebPlotResult.REGION_FILE_NAME];
        retval[WebPlotResult.REGION_DATA]= inData[WebPlotResult.REGION_DATA];
        retval[WebPlotResult.REGION_ERRORS]= inData[WebPlotResult.REGION_ERRORS];
        retval[WebPlotResult.TITLE]= inData[WebPlotResult.TITLE];
        retval[WebPlotResult.DATA_HISTOGRAM]= inData[WebPlotResult.DATA_HISTOGRAM];
        retval[WebPlotResult.DATA_BIN_MEAN_ARRAY]= inData[WebPlotResult.DATA_BIN_MEAN_ARRAY];

        var biStr= inData[WebPlotResult.BAND_INFO];
        if (biStr) {
            //todo: parse BandInfo
            retval[WebPlotResult.BAND_INFO]= BandInfo.parse(biStr);
            console.log('todo: parse BandInfo');
        }


    }
    else {
        retval.briefFailReason= inData.briefFailReason;
        retval.userFailReason= inData.userFailReason;
        retval.detailFailReason= inData.detailFailReason;
    }

    return retval;
};

//const checkForStringResult= function(key, retval, inData) {
//    var s= inData[key];
//    retval[key]= s;
//};
