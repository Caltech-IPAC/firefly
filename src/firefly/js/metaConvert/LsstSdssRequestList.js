/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get, padStart} from 'lodash';
import {getTblById,getTblInfo, getCellValue} from '../tables/TableUtil.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../visualize/RangeValues.js';
import {ZoomType} from '../visualize/ZoomType.js';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';

const colToUse= ['scienceCcdExposureId', 'filterName'];
const rangeValues= RangeValues.make(SIGMA, -2, SIGMA, 10);
rangeValues.algorithm= STRETCH_LINEAR;

const bandMap= {u:'0', g:'1',r:'2',i:'3', z:'4'};


function makeCcdReqBuilder(table, rowIdx) {
    const run= getCellValue(table, rowIdx, 'run');
    const field= padStart(getCellValue(table, rowIdx, 'field'), 4, '0');
    const camcol= getCellValue(table, rowIdx, 'camcol');
    return (plotId, title, filterId) => {

        // id is run + filterId + camcol + field
        // const objId= getCellValue(table, rowIdx, 'scienceCcdExposureId');
        const objId= `${run}${filterId}${camcol}${field}`;
        const url= `http://lsst-qserv-dax01.ncsa.illinois.edu:5000/image/v0/calexp/id?id=${objId}`;
        const r= WebPlotRequest.makeURLPlotRequest(url);
        r.setTitleOptions(TitleOptions.NONE);
        r.setTitle(title);
        r.setPlotId(plotId);
        r.setMultiImageIdx(0);
        r.setPreferenceColorKey('lsst-sdss-color-pref');
        r.setZoomType(ZoomType.TO_WIDTH);
        return r;
    };
}

/**
 * make a list of plot request for wise. This function works with ConverterFactory.
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @param threeColorOps
 * @return {{}}
 */
export function makeLsstSdssPlotRequest(table, row, includeSingle, includeStandard, threeColorOps) {

    // const overlap= get(table, 'request.intersect', '').toUpperCase()==='OVERLAPS';
    // var headerParams= overlap ? ['mission', 'ImageSet', 'ProductLevel'] :
    //                             ['mission', 'ImageSet', 'ProductLevel', 'subsize'];


    const retval= {};
    var builder= makeCcdReqBuilder(table,row);
    if (getCellValue(table, row, 'scienceCcdExposureId')) {
        builder= makeCcdReqBuilder(table,row);
    }
    else {
        return {single:null, standard:[]};
    }
    const filterId= Number(getCellValue(table, row, 'filterId'));

    if (includeSingle) {
        retval.single= builder(table,row, filterId+'');
    }

    if (includeStandard) {
        retval.standard= [
            builder('lsst-sdss-U', 'u', '0'),
            builder('lsst-sdss-G', 'g', '1'),
            builder('lsst-sdss-R', 'r', '2'),
            builder('lsst-sdss-I', 'i', '3'),
            builder('lsst-sdss-Z', 'z', '4'),
        ];
        if (retval.standard[filterId]) retval.highlightPlotId= retval.standard[filterId].getPlotId();
    }

    if (threeColorOps) {
        retval.threeColor= threeColorOps.map( (b) =>
            b && builder('lsst-sdss-threeC', 'SDSS 3 Color', bandMap[b])
        );
    }
    return retval;
}


