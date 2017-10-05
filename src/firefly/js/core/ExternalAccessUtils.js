/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty, isUndefined} from 'lodash';
import {flux} from '../Firefly.js';
import ExternalAccessCntlr from './ExternalAccessCntlr.js';
import {reportUserAction} from '../rpc/SearchServicesJson.js';
import {parseImagePt, parseWorldPt, parseScreenPt} from '../visualize/Point.js';
import {getWsChannel} from './messaging/WebSocketClient.js';
import {CysConverter} from '../visualize/CsysConverter.js';
import {getTblById, getTblRowAsObj} from '../tables/TableUtil.js';
import {PlotState} from '../visualize/PlotState.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import {primePlot} from '../visualize/PlotViewUtil.js';

const EMPTY_ARRAY=[];

export const doExtensionActivate= function(extension, resultData) {
    activate(getWsChannel(),extension,resultData);
};

export const AREA_SELECT= 'AREA_SELECT';
export const LINE_SELECT= 'LINE_SELECT';
export const POINT= 'POINT';
export const NONE= 'NONE';

const plotUIExtension= [AREA_SELECT, LINE_SELECT, POINT];


const activate= function(remoteChannel, extension, resultData) {
    if (!extension || !resultData) return;

    if (extension.callback) {
        var cbObj= Object.keys(resultData).reduce((obj,key) => {
            if (key.startsWith('wp'))      obj[key]= parseWorldPt(resultData[key]);
            else if (key.startsWith('ip')) obj[key]= parseImagePt(resultData[key]);
            else if (key.startsWith('sp')) obj[key]= parseScreenPt(resultData[key]);
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

export const getPlotUIExtensionList= function(testPlotId) {
    var {extensionList}= flux.getState()[ExternalAccessCntlr.EXTERNAL_ACCESS_KEY];
    var retList= extensionList.filter((ext) => {
        if (plotUIExtension.includes(ext.extType)) {
            if (!testPlotId || !ext.plotId || testPlotId === ExternalAccessCntlr.ALL_MPW || ext.plotId === testPlotId) {
                return ext;
            }
        }
    });
    return isEmpty(retList) ? EMPTY_ARRAY : retList;
};


export const getTblExtensionList= function(testTblId) {
    var {extensionList}= flux.getState()[ExternalAccessCntlr.EXTERNAL_ACCESS_KEY];
    var retList= extensionList.filter((ext) => {
        if (!isUndefined(ext.tbl_id)) {
            if (!testTblId || !ext.tbl_id || ext.tbl_id === testTblId) {
                return ext;
            }
        }
    });
    return isEmpty(retList) ? EMPTY_ARRAY : retList;
};




export const extensionAdd= function(extension) {
    flux.process({type: ExternalAccessCntlr.EXTENSION_ADD, payload: {extension}});
};

export const extensionRemove= function(extensionId) {
    flux.process({type: ExternalAccessCntlr.EXTENSION_REMOVE, payload: {id: extensionId}});
};


export function makePlotSelectionExtActivateData(ext, pv, dlAry) {
    var plot= primePlot(pv);
    var cc= CysConverter.make(plot);
    var data= {
        id : ext.id,
        plotId : pv.plotId,
        type: ext.extType,
        plotState: PlotState.convertToJSON(plot.plotState)
    };

    if (plot.attributes.tbl_id) {
        const table = getTblById(plot.attributes.tbl_id);
        data.table= {
            highlightedRow: table.highlightedRow,
            row : getTblRowAsObj(table)
        };
    }


    switch (ext.extType) {
        case AREA_SELECT:
            data= Object.assign(data,getTwoPointSelectObj(plot,cc,PlotAttribute.IMAGE_BOUNDS_SELECTION));
            break;
        case LINE_SELECT:
            data= Object.assign(data,getTwoPointSelectObj(plot,cc,PlotAttribute.ACTIVE_DISTANCE));
            break;
        case POINT:
            data= Object.assign(data,getPointSelectObj(plot,cc));
            break;
    }

    return data;
}

function getTwoPointSelectObj(plot,cc,attribute) {
    if (!plot.attributes[attribute]) return {};
    var {pt0,pt1}=plot.attributes[attribute];
    if (!pt0 && !pt1) return {};

    var data= {};
    var ipt0= cc.getImageCoords(pt0);
    var ipt1= cc.getImageCoords(pt1);
    if (ipt0 && ipt1) {
        data.ipt0= ipt0.toString();
        data.ipt1= ipt1.toString();
    }

    var wpt0= cc.getWorldCoords(pt0);
    var wpt1= cc.getWorldCoords(pt1);
    if (wpt0 && wpt1) {
        data.wpt0= wpt0.toString();
        data.wpt1= wpt1.toString();
    }
    return data;

}



function getPointSelectObj(plot,cc) {
    if (!plot.attributes[PlotAttribute.ACTIVE_POINT]) return {};
    var {pt}=plot.attributes[PlotAttribute.ACTIVE_POINT];
    if (!pt) return {};

    var data= {};
    var ipt= cc.getImageCoords(pt);
    if (ipt) data.ipt= ipt.toString();

    var wpt= cc.getWorldCoords(pt);
    if (wpt) data.wpt= wpt.toString();
    return data;

}



