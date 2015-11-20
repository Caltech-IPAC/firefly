/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

import ServerParams from '../data/ServerParams.js';
import {doService} from '../core/JsonUtils.js';
import {parse} from '../visualize/WebPlotResultParser.js';


const doJsonP= function() {
    return false;
    //return application.networkMode===NetworkMode.JSON;
};


/**
 *
 * @return {Promise}
 */
const getColorHistogram= function(state,band,width,height) {
    var paramList = [];
    paramList.push({name:ServerParams.STATE, value: state.serialize()});
    paramList.push({name:ServerParams.WIDTH, value: width+''});
    paramList.push({name:ServerParams.HEIGHT, value: height+''});
    paramList.push({name:ServerParams.BAND, value: band.toString()});
    paramList.push({name:ServerParams.JSON_DEEP,value:'true'});

    return doService(doJsonP(), ServerParams.HISTOGRAM, paramList
    ).then((data) => parse(data) );
};

/**
 * @param {WebPlotRequest} redRequest
 * @param {WebPlotRequest} greenRequest
 * @param {WebPlotRequest} blueRequest
 * @return {Promise}
 */
const getWebPlot3Color= function(redRequest, greenRequest, blueRequest) {
    var paramList = [];
    if (redRequest) paramList.push({name:ServerParams.RED_REQUEST, value:redRequest.toString()});
    if (greenRequest) paramList.push({name:ServerParams.GREEN_REQUEST, value:greenRequest.toString()});
    if (blueRequest) paramList.push({name:ServerParams.BLUE_REQUEST, value:blueRequest.toString()});
    paramList.push({name:ServerParams.JSON_DEEP,value:'true'});
    return doService(doJsonP(), ServerParams.CREATE_PLOT, paramList)
        .then((data) => parse(data) );
};


/**
 * @param {WebPlotRequest} request
 * @return {Promise}
 */
const getWebPlot= function(request) {
    var paramList = [{name: ServerParams.NOBAND_REQUEST, value:request.toString()}];
    paramList.push({name:ServerParams.JSON_DEEP,value:'true'});
    return doService(doJsonP(), ServerParams.CREATE_PLOT, paramList);
};


const getWebPlotGroup= function(requestList, progressKey) {
    //todo
};

const getOneFileGroup= function(requestList, progressKey) {
    //todo
};

var PlotServicesJson= {getColorHistogram, getWebPlot3Color, getWebPlot, getWebPlotGroup, getOneFileGroup};
export default PlotServicesJson;
