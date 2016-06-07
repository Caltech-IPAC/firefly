/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

import {ServerParams} from '../data/ServerParams.js';
import {doJsonRequest} from '../core/JsonUtils.js';



/**
 *
 * @return {Promise}
 */
export const aliveCheck= function(channelId, tryMS=0) {
    var params= {
        [ServerParams.CHANNEL_ID]: channelId,
        [ServerParams.TRY_MS]: tryMS
    };
    return doJsonRequest(ServerParams.VIS_PUSH_ALIVE_COUNT, params);
};


/**
 *
 * @return {Promise}
 */
export const dispatchRemoteAction= function(channelId, action) {

    const params= {
             [ServerParams.CHANNEL_ID]: channelId,
             [ServerParams.ACTION]: JSON.stringify(action) };
    return doJsonRequest(ServerParams.VIS_PUSH_ACTION, params);
};
