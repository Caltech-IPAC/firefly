/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue, getColumnIdx} from '../../../tables/TableUtil.js';
import {WebPlotRequest} from '../../../visualize/WebPlotRequest.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {CoordinateSys} from '../../../visualize/CoordSys.js';
import {addCommonReqParams, COORD_SYSTEM_OPTIONS} from '../LcConverterFactory.js';

export function makeURLPlotRequest(table, rowIdx, cutoutSize, params) {
    const {dataSource, timeCol, ra, dec, coordSys} = params;

    // create plot request on either valid or invalid parameters
    const url = getCellValue(table, rowIdx, dataSource );
    const time = getCellValue(table, rowIdx, timeCol);
    const plot_desc = `Mission-${time||'null'}-${dataSource||'null'}-${ra||'null'}-${dec||'null'}-${coordSys||'null'}`;
    const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
    const title= `Mission-${time||'null'}`;
    const sys = COORD_SYSTEM_OPTIONS.includes(coordSys) ? CoordinateSys.parse(coordSys) : null;
    const wpt = (getColumnIdx(table, ra) < 0 || getColumnIdx(table, dec) < 0 || !sys) ? undefined :
                makeWorldPt(getCellValue(table, rowIdx, ra),
                            getCellValue(table, rowIdx, dec),
                            sys);

    return addCommonReqParams(reqParams, title, wpt);
}
