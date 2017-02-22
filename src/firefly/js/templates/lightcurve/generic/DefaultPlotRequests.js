/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue, getColumnIdx} from '../../../tables/TableUtil.js';
import {WebPlotRequest} from '../../../visualize/WebPlotRequest.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {CoordinateSys} from '../../../visualize/CoordSys.js';
import {addCommonReqParams} from '../LcConverterFactory.js';

export function makeURLPlotRequest(table, rowIdx, cutoutSize, params) {
    const {dataSource, timeCol} = params;

    // not change the current plots in case dataSource or time column does not exist
    if (getColumnIdx(table, dataSource) < 0 || getColumnIdx(table, timeCol) < 0) return null;

    const ra = getCellValue(table, rowIdx, 'ra');
    const dec = getCellValue(table, rowIdx, 'dec');
    const url = getCellValue(table, rowIdx, dataSource );
    const time = getCellValue(table, rowIdx, timeCol);
    const plot_desc = `Mission-${rowIdx}`;

    const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
    const title= `Mission- ${time}`+ (cutoutSize ? ` size: ${cutoutSize}(deg)` : '');
    return addCommonReqParams(reqParams, title, makeWorldPt(ra,dec,CoordinateSys.EQ_J2000));
}
