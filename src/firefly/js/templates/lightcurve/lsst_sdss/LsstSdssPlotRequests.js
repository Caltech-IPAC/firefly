/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue} from '../../../tables/TableUtil.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../../../visualize/RangeValues.js';
import {WebPlotRequest} from '../../../visualize/WebPlotRequest.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {CoordinateSys} from '../../../visualize/CoordSys.js';
import {ServerRequest} from '../../../data/ServerRequest.js';

import {addCommonReqParams} from '../LcConverterFactory.js';

const bandMap= {u:0, g:1, r:2, i:3, z:4};


/**
 * make a list of plot request for wise. This function works with ConverterFactory.
 * @param table
 * @param rowIdx
 * @param {Number} cutoutSize
 * @return {{}}
 */
export function makeLsstSdssPlotRequest(table, rowIdx, cutoutSize) {
    var id= getCellValue(table, rowIdx, 'scienceCcdExposureId');
    if (!id) {
        return; // error
    }
    const run= getCellValue(table, rowIdx, 'run');
    const field= getCellValue(table, rowIdx, 'field');
    const camcol= getCellValue(table, rowIdx, 'camcol');

    //const filterId= Number(getCellValue(table, rowIdx, 'filterId'));
    const filterName= getCellValue(table, rowIdx, 'filterName');

    // cutout center
    const ra = getCellValue(table, rowIdx, 'coord_ra');
    const decl = getCellValue(table, rowIdx, 'coord_decl');

    // TODO: add support for cutouts
    const sr= new ServerRequest('LSSTImageSearch');
    sr.setParam('run', `${run}`);
    sr.setParam('camcol', `${camcol}`);
    sr.setParam('field', `${field}`);
    sr.setParam('filterName', `${filterName}`);

    const scienceCcdId = id.toString();
    const title =scienceCcdId.substr(0, 4) + bandMap[filterName].toString() + scienceCcdId.substr(5, 10)+'-'+filterName+(cutoutSize ? ` size: ${cutoutSize}(deg)` : '');
    const plot_desc = `lsst-sdss-${run}`;

    const r  = WebPlotRequest.makeProcessorRequest(sr, plot_desc);
    const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});
    r.setInitialRangeValues(rangeValues);

    return addCommonReqParams(r, title, makeWorldPt(ra,decl,CoordinateSys.EQ_J2000));
}
