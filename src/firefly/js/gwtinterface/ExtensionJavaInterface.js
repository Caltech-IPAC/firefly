/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

//import { appFlux } from '../core/Globals.js';
import ExternalAccessUtils from '../core/ExternalAccessUtils.js';
import {flux} from '../Firefly.js';

export class ExtensionJavaInterface {

    constructor(javaObject,cb) {
        if (cb && javaObject) {
            //this.cb= cb.bind(this, javaObject);
            this.storeListenerRemove= flux.addListener(cb.bind(this, javaObject));
        }
        this.getExtensionList= ExternalAccessUtils.getExtensionList;
        this.getRemoteChannel= ExternalAccessUtils.getRemoteChannel;
        this.fireExtAction= ExternalAccessUtils.extensionActivate;
        this.fireExtAdd= ExternalAccessUtils.extensionAdd;
        this.fireChannelActivate= ExternalAccessUtils.channelActivate;
    }

    clearListener() {
         if (this.storeListenerRemove) this.storeListenerRemove();
    }

    //getExtensionList(testPlotId) {
    //    ExternalAccessUtils.getExtensionList(testPlotId);
    //}
    //
    //getRemoteChannel() {
    //    ExternalAccessUtils.getRemoteChannel();
    //}
    //
    //
    //fireExtAction(extension, extData) {
    //    ExternalAccessUtils.extensionActivate(extension,extData);
    //}
    //
    //fireExtAdd(extension) {
    //    ExternalAccessUtils.extensionAdd(extension);
    //}
    //
    //fireChannelActivate(channelId) {
    //    ExternalAccessUtils.channelActivate(channelId);
    //}
}
