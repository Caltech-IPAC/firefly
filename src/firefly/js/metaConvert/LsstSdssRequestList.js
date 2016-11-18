/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {padStart} from 'lodash';
import {getCellValue} from '../tables/TableUtil.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../visualize/RangeValues.js';
import {ZoomType} from '../visualize/ZoomType.js';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';


const DAX_URL= 'http://lsst-qserv-dax01.ncsa.illinois.edu:5000';


function makeCcdReqBuilder(table, rowIdx) {
    const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});
    const run= getCellValue(table, rowIdx, 'run');
    const field= padStart(getCellValue(table, rowIdx, 'field'), 4, '0');
    const camcol= getCellValue(table, rowIdx, 'camcol');
    const baseUrl= `${DAX_URL}/image/v0/calexp/ids`;

    return (plotId, title, filterName) => {

        // id is run + filterId + camcol + field
        // const objId= getCellValue(table, rowIdx, 'scienceCcdExposureId');
        // const objId= `${run}${filterId}${camcol}${field}`;
        // const url= `http://lsst-qserv-dax01.ncsa.illinois.edu:5000/image/v0/calexp/id?id=${objId}`;
        const url= `${baseUrl}?run=${run}&camcol=${camcol}&field=${field}&filter=${filterName}`;
        const r= WebPlotRequest.makeURLPlotRequest(url);
        r.setTitleOptions(TitleOptions.NONE);
        r.setTitle(title);
        r.setPlotId(plotId);
        r.setMultiImageIdx(0);
        r.setPreferenceColorKey('lsst-sdss-color-pref');
        r.setZoomType(ZoomType.TO_WIDTH);
        r.setInitialRangeValues(rangeValues);
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

    const bandMap= {u:'0', g:'1',r:'2',i:'3', z:'4'};
    const retval= {};
    var builder;
    if (getCellValue(table, row, 'scienceCcdExposureId')) {
        builder= makeCcdReqBuilder(table,row);
    }
    else {
        return {single:null, standard:[]};
    }
    const filterId= Number(getCellValue(table, row, 'filterId'));
    const filterName= getCellValue(table, row, 'filterName');

    if (includeSingle) {
        retval.single= builder('lsst-sdss-'+filterName,filterName, filterName);
    }

    if (includeStandard) {
        retval.standard= [
            builder('lsst-sdss-u', 'u', 'u'),
            builder('lsst-sdss-g', 'g', 'g'),
            builder('lsst-sdss-r', 'r', 'r'),
            builder('lsst-sdss-i', 'i', 'i'),
            builder('lsst-sdss-z', 'z', 'z'),
        ];
        if (retval.standard[filterId]) retval.highlightPlotId= retval.standard[filterId].getPlotId();
    }

    if (threeColorOps) {
        retval.threeColor= threeColorOps.map( (b) => b && builder('lsst-sdss-threeC', 'SDSS 3 Color', b) );
    }
    return retval;
}


