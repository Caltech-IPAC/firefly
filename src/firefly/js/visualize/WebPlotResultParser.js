/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import PlotState from './PlotState.js';
import PlotImages from './PlotImages.js';
import InsertBandInitializer from './InsertBandInitializer.js';

export const RConst = {
    PLOT_CREATE : 'PlotCreate',
    PLOT_STATE : 'PlotState',
    INSERT_BAND_INIT : 'InsertBand',
    STRING : 'String',
    PLOT_IMAGES : 'PlotImages',
    FLUX_VALUE : 'FluxValue',
    RAW_DATA_SET : 'RawDataSet',
    DATA_HISTOGRAM : 'DataHistogram',
    DATA_BIN_MEAN_ARRAY : 'DataBinMeanArray',
    DATA_HIST_IMAGE_URL : 'DataHistImageUrl',
    CBAR_IMAGE_URL : 'CBarImageUrl',
    IMAGE_FILE_NAME : 'ImageFileName',
    REGION_FILE_NAME : 'RegionFileName',
    METRICS_HASH_MAP : 'Metrics_HasMap',
    BAND_INFO : 'Band_Info',
    REGION_ERRORS : 'RegionErrors',
    REGION_DATA : 'RegionData',
    REQUEST_LIST : 'RequestList',
    TITLE : 'Title'
};





export const parse= function(inData) {
    var retval= {success:inData.success, progressKey:inData.progressKey };
    if (inData.success) {
        var stateStr= inData[RConst.PLOT_STATE];
        if (stateStr) retval[RConst.PLOT_STATE]= PlotState.parse(stateStr);


        var imagesStr= inData[RConst.PLOT_IMAGES];
        if (imagesStr) retval[RConst.PLOT_IMAGES]= PlotImages.parse(imagesStr);


        var wpInitStr= inData[RConst.INSERT_BAND_INIT];
        if (wpInitStr) retval[RConst.INSERT_BAND_INIT]= InsertBandInitializer.parse(wpInitStr);

        var creatorAry= inData[RConst.PLOT_CREATE];
        if (creatorAry) {
            //TODO: finish this.
            //TODO: convert: WebPlotInitilizer
            //TODO: convert: CreateResults
            //TODO: go look up in the java class

            var wpInitList= creatorAry.map( s => WebPlotInitializer.parse(s));

            retval[RConst.PLOT_CREATE]= new CreatorResults(wpInitList);
        }


        retval[RConst.STRING]= inData[RConst.STRING];
        retval[RConst.DATA_HIST_IMAGE_URL]= inData[RConst.DATA_HIST_IMAGE_URL];
        retval[RConst.CBAR_IMAGE_URL]= inData[RConst.CBAR_IMAGE_URL];
        retval[RConst.IMAGE_FILE_NAME]= inData[RConst.IMAGE_FILE_NAME];
        retval[RConst.REGION_FILE_NAME]= inData[RConst.REGION_FILE_NAME];
        retval[RConst.REGION_DATA]= inData[RConst.REGION_DATA];
        retval[RConst.REGION_ERRORS]= inData[RConst.REGION_ERRORS];
        retval[RConst.TITLE]= inData[RConst.TITLE];
        retval[RConst.DATA_HISTOGRAM]= inData[RConst.DATA_HISTOGRAM];
        retval[RConst.DATA_BIN_MEAN_ARRAY]= inData[RConst.DATA_BIN_MEAN_ARRAY];

        var biStr= inData[RConst.BAND_INFO];
        if (biStr) {
            //todo: parse BandInfo
            retval[RConst.BAND_INFO]= BandInfo.parse(biStr);
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

const checkForStringResult= function(key, retval, inData) {
    var s= inData[key];
    retval[key]= s;
};
