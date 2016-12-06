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
 * @returns {a WebRequest object}
 */
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
 * 11/23/16
 * Lijun Zhang
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

    return (plotId, title, filterName) => {
        sr.setParam('filterName', `${filterName}`);
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

    return (plotId, title, filterName) => {
        sr.setParam('filterName', `${filterName}`);
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
    var titleBase;
    if (getCellValue(table, row, 'scienceCcdExposureId')) {
        builder= makeCcdReqBuilder(table,row);
        titleBase= Number(getCellValue(table, row, 'scienceCcdExposureId'));
    }
    else {
        builder= makeCoadReqBuilder(table, row);
        titleBase= Number(getCellValue(table, row, 'deepCoaddId'));

    }
    const filterId= Number(getCellValue(table, row, 'filterId'));
    const filterName= getCellValue(table, row, 'filterName');

    if (includeSingle) {
       retval.single= builder('lsst-sdss-'+filterName,filterName, filterName);
    }

    if (includeStandard) {
        retval.standard= [
            builder('lsst-sdss-u',  titleBase+'-u', 'u'),
            builder('lsst-sdss-g',  titleBase+'-g', 'g'),
            builder('lsst-sdss-r',  titleBase+'-r', 'r'),
            builder('lsst-sdss-i',  titleBase+'-i', 'i'),
            builder('lsst-sdss-z',  titleBase+'-z', 'z'),
        ];
        if (retval.standard[filterId]) retval.highlightPlotId= retval.standard[filterId].getPlotId();
    }

    if (threeColorOps) {
        retval.threeColor= threeColorOps.map( (b) => b && builder('lsst-sdss-threeC', 'SDSS 3 Color', b) );
    }
    return retval;
}

