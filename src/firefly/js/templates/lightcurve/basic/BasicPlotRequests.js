/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue, getColumnIdx} from '../../../tables/TableUtil.js';
import {WebPlotRequest} from '../../../visualize/WebPlotRequest.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {CoordinateSys} from '../../../visualize/CoordSys.js';
import {addCommonReqParams, COORD_SYSTEM_OPTIONS} from '../LcConverterFactory.js';

export function basicURLPlotRequest(table, rowIdx, cutoutSize, params = {}) {


    const {dataSource, timeCol} = params;
    try {
        // create plot request on either valid or invalid parameters
        const url = getCellValue(table, rowIdx, dataSource);
        const time = getCellValue(table, rowIdx, timeCol);
        const plot_desc = `Table-${time || 'null'}-${dataSource || 'null'}`;


        const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
        const title = `Table-${time || 'null'}`;

        return addCommonReqParams(reqParams, title, null);
    } catch (E) {
        throw new Error('Datasource field is empty, don\'t show images');
    }
}
