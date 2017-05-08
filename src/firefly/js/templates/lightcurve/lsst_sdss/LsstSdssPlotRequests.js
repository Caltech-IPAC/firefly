/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue} from '../../../tables/TableUtil.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../../../visualize/RangeValues.js';
import {WebPlotRequest} from '../../../visualize/WebPlotRequest.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {CoordinateSys} from '../../../visualize/CoordSys.js';
import {ZoomType} from '../../../visualize/ZoomType.js';
import {ServerRequest} from '../../../data/ServerRequest.js';

import {addCommonReqParams} from '../LcConverterFactory.js';
import {convertAngle} from '../../../visualize/VisUtil.js';


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
    const scienceCcdId = id.toString();
    //const run= getCellValue(table, rowIdx, 'run');
    //const field= getCellValue(table, rowIdx, 'field');
    //const camcol= getCellValue(table, rowIdx, 'camcol');
    const filterName= getCellValue(table, rowIdx, 'filterName');

    // cutout center
    const ra = getCellValue(table, rowIdx, 'coord_ra');
    const decl = getCellValue(table, rowIdx, 'coord_decl');

    // convert the default Cutout size in arcmin to deg for WebPlotRequest
    const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize);


    const sr= new ServerRequest('LSSTImageSearch');
    sr.setParam('imageType', 'calexp');
    sr.setParam('imageId', scienceCcdId);
    sr.setParam('ra', ra);
    sr.setParam('dec', decl);
    sr.setParam('subsize', `${cutoutSizeInDeg}`); // in degrees

    const title =scienceCcdId.substr(0, 4) + bandMap[filterName].toString() + scienceCcdId.substr(5, 10)+'-'+filterName+(cutoutSize ? ` size: ${cutoutSize}(arcmin)` : '');
    const plot_desc = `lsst-sdss-${scienceCcdId}`;

    const r  = WebPlotRequest.makeProcessorRequest(sr, plot_desc);
    const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});
    r.setInitialRangeValues(rangeValues);
    // r.setZoomType(ZoomType.TO_WIDTH);  TODO:LLY
    r.setMultiImageIdx(0);

    return addCommonReqParams(r, title, makeWorldPt(ra,decl,CoordinateSys.EQ_J2000));
}
