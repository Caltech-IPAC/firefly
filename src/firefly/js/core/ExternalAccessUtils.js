/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import ExternalAccessCntlr from './ExternalAccessCntlr.js';
import {reportUserAction} from '../rpc/SearchServicesJson.js';
import { ImagePt, WorldPt, ScreenPt } from '../visualize/Point.js';



const doExtensionActivate= function(extension, resultData) {
    activate(getRemoteChannel(),extension,resultData);
};



const activate= function(remoteChannel, extension, resultData) {
    if (!extension || !resultData) return;

    if (extension.callback) {
        var cbObj= Object.keys(resultData).reduce((obj,key) => {
            if (key.startsWith('wp'))      obj[key]= WorldPt.parse(resultData[key]);
            else if (key.startsWith('ip')) obj[key]= ImagePt.parse(resultData[key]);
            else if (key.startsWith('sp')) obj[key]= ScreenPt.parse(resultData[key]);
            else                           obj[key]= resultData[key];
            return obj;
        }, {});
        if (!resultData.type && extension.extType) cbObj.type= extension.extType;
        extension.callback(cbObj);
    }

    if (remoteChannel && extension.extType !== 'PLOT_MOUSE_READ_OUT') {
        reportUserAction(remoteChannel,'todo- add desc',JSON.stringify(resultData));
    }
};

const getRemoteChannel= function() {
    return flux.getState()[ExternalAccessCntlr.EXTERNAL_ACCESS_KEY].remoteChannel ;
};

const getExtensionList= function(testPlotId) {
    var {extensionList}= flux.getState()[ExternalAccessCntlr.EXTERNAL_ACCESS_KEY];
    var retList= extensionList.filter((ext) => {
        if (!testPlotId || !ext.plotId || testPlotId === ExternalAccessCntlr.ALL_MPW || ext.plotId === testPlotId) {
            return ext;
        }
    });
    return retList;
};

const extensionAdd= function(extension) {
    flux.process({type: ExternalAccessCntlr.EXTENSION_ADD, payload: {extension}});
};

const extensionActivate= function(extension, resultData) {
    flux.process({type: ExternalAccessCntlr.EXTENSION_ACTIVATE, payload: {extension, resultData}});
};

const channelActivate= function(channelId) {
    flux.process({type: ExternalAccessCntlr.CHANNEL_ACTIVATE, payload: {channelId}});
};


var ExternalAccessUtils= { doExtensionActivate, extensionAdd, extensionActivate, channelActivate,
    getRemoteChannel, getExtensionList };
export default ExternalAccessUtils;
