/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../visualize/RangeValues.js';
import {getCellValue} from '../tables/TableUtil.js';
import {makeWebPlotRequestViaZtfIbe} from 'firefly/templates/lightcurve/ztf/IbeZTFPlotRequests.js';

const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});


function makeZtfRequest(plotId, table, row, size, subsize) {
    const req= makeWebPlotRequestViaZtfIbe(table,row, size, subsize);
    req.setPlotId(plotId);
    req.setInitialRangeValues(rangeValues);
    return req;
}


export function makeZtfPlotRequest(table, row, includeSingle, includeStandard) {
    const pid = getCellValue(table, row, 'pid');
    const nid = getCellValue(table, row, 'nid');
    const expid = getCellValue(table, row, 'expid');
    const plotId= `ztf-${pid}-${nid}-${expid}`;

    const retval= {};
    if (includeSingle) retval.single= makeZtfRequest(plotId, table,row, table.request.size, table.request.subsize);
    if (includeStandard) {
        retval.standard= [ makeZtfRequest(`group-1-${plotId}`,table,row, table.request.size, table.request.subsize) ];
        retval.highlightPlotId= 0;
    }
    return retval;
}