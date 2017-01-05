/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue} from '../tables/TableUtil.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../visualize/RangeValues.js';
import {ZoomType} from '../visualize/ZoomType.js';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';
import {ServerRequest} from '../data/ServerRequest.js';

/**
 * This method returns a WebRequest object
 * @param sr - {Object}
 * @param plotId - {String}
 * @param title - {String}
 * @returns {WebPlotRequest} a web plot request
 */
const bandMap= {u:0, g:1,r:2,i:3, z:4};
function makeWebRequest(sr,  plotId, title) {
    const r  = WebPlotRequest.makeProcessorRequest(sr, 'lsst-sdss');
    const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});
    r.setTitleOptions(TitleOptions.NONE);
    r.setTitle(title);
    r.setPlotId(plotId);
    r.setMultiImageIdx(0);
    r.setPreferenceColorKey('lsst-sdss-color-pref');
    r.setZoomType(ZoomType.TO_WIDTH);
    r.setInitialRangeValues(rangeValues);
    return r;
}

/**
 * @param table - {Object }
 * @param rowIdx - {int}
 * @returns {Function}
 */
function makeCcdReqBuilder(table, rowIdx) {

    const run= getCellValue(table, rowIdx, 'run');
    const field= getCellValue(table, rowIdx, 'field');
    const camcol= getCellValue(table, rowIdx, 'camcol');

    const sr= new ServerRequest('LSSTImageSearch');
    sr.setParam('run', `${run}`);
    sr.setParam('camcol', `${camcol}`);
    sr.setParam('field', `${field}`);

    return (plotId, id, filterName) => {
        sr.setParam('filterName', `${filterName}`);
        const scienceCCCdId = id.toString();
        const title =scienceCCCdId.substr(0, 4) + bandMap[filterName].toString() + scienceCCCdId.substr(5, 10)+'-'+filterName;
        return makeWebRequest(sr, plotId,  title);
    };
}

/**
 * @desc This function makes the WebRequest for DeepCoadd database
 *
 * @param table  - {Object }
 * @param rowIdx - {int}
 * @returns {Function}
 */
function makeCoadReqBuilder(table, rowIdx) {


    const tract= getCellValue(table, rowIdx, 'tract');
    const patch= getCellValue(table, rowIdx, 'patch');

    const sr= new ServerRequest('LSSTImageSearch');
    sr.setParam('tract', `${tract}`);
    sr.setParam('patch', `${patch}`);

    return (plotId, id, filterName) => {
        sr.setParam('filterName', `${filterName}`);
        const deepCoaddId = id + bandMap[filterName];
        const title = deepCoaddId+'-'+filterName;
        return makeWebRequest(sr, plotId,  title);
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

    const retval= {};
    var builder;
    var id;
    if (getCellValue(table, row, 'scienceCcdExposureId')) {
          builder= makeCcdReqBuilder(table,row);
         id= Number(getCellValue(table, row, 'scienceCcdExposureId'));
    }
    else {
        builder= makeCoadReqBuilder(table, row);
        const deepCoaddId= Number(getCellValue(table, row, 'deepCoaddId'));
        id = deepCoaddId - deepCoaddId%8;

    }
    const filterId= Number(getCellValue(table, row, 'filterId'));
    const filterName= getCellValue(table, row, 'filterName');

    if (includeSingle) {
       retval.single= builder('lsst-sdss-'+filterName,filterName, filterName);
    }

    if (includeStandard) {
        retval.standard= [
            builder('lsst-sdss-u', id, 'u'),
            builder('lsst-sdss-g', id, 'g'),
            builder('lsst-sdss-r', id, 'r'),
            builder('lsst-sdss-i', id, 'i'),
            builder('lsst-sdss-z', id, 'z'),
        ];
        if (retval.standard[filterId]) retval.highlightPlotId= retval.standard[filterId].getPlotId();
    }

    if (threeColorOps) {
        retval.threeColor= threeColorOps.map( (b) => b && builder('lsst-sdss-threeC', 'SDSS 3 Color', b) );
    }
    return retval;
}

