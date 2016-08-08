/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * @author Trey Roby
 */

import {CysConverter} from './CsysConverter.js';
import {PlotState} from './PlotState.js';
import {PlotAttribute} from './WebPlot.js';
import {primePlot} from './PlotViewUtil.js';

export const AREA_SELECT= 'AREA_SELECT';
export const LINE_SELECT= 'LINE_SELECT';
export const POINT= 'POINT';
export const NONE= 'NONE';

export var PlotCmdExtension = ({id, plotId, extType, imageUrl, title, toolTip, callback=null}) => {
    return {id, plotId, imageUrl, title, toolTip, extType, callback};
};

export function makeExtActivateData(ext,pv,dlAry) {
    var plot= primePlot(pv);
    var cc= CysConverter.make(plot);
    var data= {
        id : ext.id,
        plotId : pv.plotId,
        type: ext.extType,
        plotState: PlotState.convertToJSON(plot.plotState)
    };

    switch (ext.extType) {
        case AREA_SELECT:
            data= Object.assign(data,getTwoPointSelectObj(plot,cc,PlotAttribute.SELECTION));
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
        data.ipt1= ipt0.toString();
    }

    var wpt0= cc.getWorldCoords(pt0);
    var wpt1= cc.getWorldCoords(pt1);
    if (wpt0 && wpt1) {
        data.wpt0= wpt0.toString();
        data.wpt1= wpt0.toString();
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



