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
import {ServerRequest} from '../data/ServerRequest.js';

/**
 * 11/23/16
 * Lijun Zhang
 * @param table - {Object }
 * @param rowIdx - {int}
 * @returns {Function}
 */

function makeCcdReqBuilder(table, rowIdx) {

    const run= getCellValue(table, rowIdx, 'run');
    const field= padStart(getCellValue(table, rowIdx, 'field'), 4, '0');
    const camcol= getCellValue(table, rowIdx, 'camcol');
    const filterId= getCellValue(table, rowIdx, 'filterId');

    const sr= new ServerRequest('LSSTImageSearch');
    sr.setParam('run', `${run}`);
    sr.setParam('camcol', `${camcol}`);
    sr.setParam('filterId', `${filterId}`);
    sr.setParam('field', `${field}`);

    return (plotId, title, filterName) => {

        sr.setParam('filterName', `${filterName}`);
        return makeWebRequest(sr,plotId,  title);
    };
}

/**
 * This method returns a WebRequest object
 * @param sr - {Object}
 * @param plotId - {String}
 * @param title - {String}
 * @returns {a WebRequest object}
 */

function makeWebRequest(sr,  plotId, title) {
    const r  = WebPlotRequest.makeProcessorRequest(sr, 'lsst');
    const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});
    r.setTitleOptions(TitleOptions.NONE);
    r.setTitle(title);
    r.setPlotId(plotId);
    r.setMultiImageIdx(0);
    r.setPreferenceColorKey('lsst-coadd-color-pref');
    r.setZoomType(ZoomType.TO_WIDTH);
    r.setInitialRangeValues(rangeValues);
    return r;
}
/**
 * This method builds the Coadd search string
 * @param table - {Object }
 * @param rowIdx - {int}
 * @returns {Function}
 */
function makeCoadReqBuilder(table, rowIdx) {

    const bandMap= {u:'0', g:'1',r:'2',i:'3', z:'4'};

    const deepCoaddId = Number(getCellValue(table, rowIdx, 'deepCoaddId'));
    const searchIdBase = deepCoaddId - deepCoaddId%8;
    const sr= new ServerRequest('LSSTImageSearch');

    return (plotId, title, filterName) => {

        const searchId = searchIdBase + Number(bandMap[filterName]);
        sr.setParam('deepCoaddId', `${searchId}`);
        return makeWebRequest(sr,plotId,  title);
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
export function makeLsstImagePlotRequest(table, row, includeSingle, includeStandard, threeColorOps) {

    const retval= {};
    var builder;
    var plotId;
    if (getCellValue(table, row, 'scienceCcdExposureId')) {
        builder= makeCcdReqBuilder(table,row);
        plotId = 'lsst-sdss-';
    }
    else {
        builder= makeCoadReqBuilder(table, row);
        plotId = 'lsst-coadd-';

    }
    const filterId= Number(getCellValue(table, row, 'filterId'));
    const filterName= getCellValue(table, row, 'filterName');

    if (includeSingle) {
       retval.single= builder(plotId+filterName,filterName, filterName);
    }

    if (includeStandard) {
        retval.standard= [
            builder(plotId+'u', 'u', 'u'),
            builder(plotId+'g', 'g', 'g'),
            builder(plotId+'r', 'r', 'r'),
            builder(plotId+'i', 'i', 'i'),
            builder(plotId+'z', 'z', 'z'),
        ];
        if (retval.standard[filterId]) retval.highlightPlotId= retval.standard[filterId].getPlotId();
    }

    if (threeColorOps) {
        retval.threeColor= threeColorOps.map( (b) => b && builder(plotId+'-threeC', 'SDSS 3 Color', b) );
    }
    return retval;
}

