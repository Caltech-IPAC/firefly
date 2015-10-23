/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import ExternalAccessCntlr from './ExternalAccessCntlr.js';
import {reportUserAction} from '../rpc/SearchServicesJson.js';
import { ImagePt, WorldPt, ScreenPt } from '../visualize/Point.js';



const activateExtension= function(extension, resultData) {
    var {remoteChannel}= flux.getState()[ExternalAccessCntlr.EXTERNAL_ACCESS_KEY].removeChannel;
    activate(remoteChannel,extension,resultData);
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




var ExternalAccessUtils= { activateExtension };
export default ExternalAccessUtils;
