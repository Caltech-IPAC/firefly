/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 4/13/15.
 */
/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

import { Store } from 'flummox';
import { ImagePt, WorldPt } from 'ipac-firefly/visualize/Point.js';
import { AREA_SELECT,LINE_SELECT,POINT,NONE } from 'ipac-firefly/visualize/PlotCmdExtension.js';
import {reportUserAction} from "ipac-firefly/rpc/SearchServicesJson.js";



const ALL_MPW=  "AllMpw";

export class ExternalAccessStore extends Store {
    constructor(flux) {
        super();

        this.state = {
            //commonCommands : [],
            //fitsViewerList : [],
            extensionList : [],
            remoteChannel : null,
        }

        const fitViewActions= flux.getActions('ExternalAccessActions');

        this.register(fitViewActions.extensionAdd, this.addExtension);
        this.register(fitViewActions.extensionActivate, this.activateExtension);
        this.register(fitViewActions.channelActivate, this.updateChannel);
        //this.register(fitViewActions.fitsViewerAdd, this.addFitsViewer);
        //this.register(fitViewActions.fitsViewerRemove, this.removeFitsViewer);
    }


    addExtension(extension) {
        var extensionList= this.state.extensionList;
        extensionList.push(extension);
        this.setState({extensionList});
    }

    activateExtension(data) {

        var netObj= data.resultData;
        if (data.extension && data.extension.callback) {
            var cbObj= Object.keys(netObj).reduce((obj,key) => {
                if (key.startsWith("wp"))      obj[key]= WorldPt.parse(netObj[key]);
                else if (key.startsWith("ip")) obj[key]= ImagePt.parse(netObj[key]);
                else                           obj[key]= netObj[key];
                return obj;
            }, {});
            data.extension.callback(cbObj);
        }

        if (this.state.remoteChannel) {
            reportUserAction(this.state.remoteChannel,"todo- add desc",JSON.stringify(netObj));
            // call remote here
        }

        //switch (data.extension.extType) {
        //    case AREA_SELECT:
        //        break;
        //    case LINE_SELECT:
        //        break;
        //    case POINT:
        //        break;
        //    default:
        //        break;
        //}

    }

    updateChannel(channelId) {
        this.setState({remoteChannel:channelId})
    }


    getExtensionList(testPlotId=ALL_MPW) {
        return this.state.extensionList.filter(ext => {
            if (!ext.plotId || testPlotId === ALL_MPW || ext.plotId === testPlotId) {
                return ext;
            }
        });
    }


    //addFitsViewer(extension) { }

    //removeFitsViewer(extension) { }

}
