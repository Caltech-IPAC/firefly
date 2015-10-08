/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

import {ServerParams} from '../data/ServerParams.js';
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
export const getColorHistogram= function(state,band,width,height) {
    var paramList = [];
    paramList.push({name:ServerParams.STATE, value: state.serialize()});
    paramList.push({name:ServerParams.WIDTH, value: width+''});
    paramList.push({name:ServerParams.HEIGHT, value: height+''});
    paramList.push({name:ServerParams.BAND, value: band.toString()});

    return doService(doJsonP(), ServerParams.HISTOGRAM, paramList
    ).then(data => {return parse(data); });
};

