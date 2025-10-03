import {uniqueId} from 'lodash';
import {TableDataType} from '../../data/FileAnalysis';
import {sprintf} from '../../externalSource/sprintf.js';
import {getCellValue, getColumn, getMetaEntry} from '../../tables/TableUtil.js';
import {PlotAttribute} from '../../visualize/PlotAttribute.js';
import RangeValues from '../../visualize/RangeValues.js';
import {TitleOptions, WebPlotRequest} from '../../visualize/WebPlotRequest.js';
import {getSSATitle, isSSATable} from '../../voAnalyzer/TableAnalysis.js';

/**
 *
 * @param {Object} p
 * @param p.url
 * @param {CloudAccessData} [p.cloudAccess]
 * @param p.positionWP
 * @param p.titleStr
 * @param {TableModel} p.table
 * @param {number} p.row
 * @param {boolean|undefined} [p.expectStaticFile]
 * @return {undefined|WebPlotRequest}
 */
export function makeObsCoreRequest({ url, cloudAccess={}, positionWP, titleStr, table, row, expectStaticFile=false}) {
    if (!url) return undefined;
    const {gcs={},aws={}}= cloudAccess;
    const {region,bucket_name:awsBucketName,key}= aws;
    const r = WebPlotRequest.makeNetReferencePlotRequest(url, region,awsBucketName,key, 'VO DataProduct');
    if (gcs.bucket_name && gcs.object_name) {
        r.setGcsParams(gcs.project,gcs.bucket_name,gcs.object_name);
    }
    const ssa= isSSATable(table);
    const titleStringToUse= ssa
        ? (getSSATitle(table,row) ?? TableDataType.Spectrum)
        : titleStr;
    if (titleStringToUse?.length > 2) {
        r.setTitleOptions(TitleOptions.NONE);
        r.setTitle(titleStringToUse);
    }
    else {
        r.setTitleOptions(TitleOptions.FILE_NAME);
    }
    r.setPlotId(uniqueId('obscore-'));
    r.setWorldPt(positionWP);
    r.setExpectStaticFile(expectStaticFile);


    const emMinCol = getColumn(table, 'em_max', true);
    const emMaxCol = getColumn(table, 'em_max', true);
    const emMin = emMinCol && Number(getCellValue(table, row, 'em_min'));
    const emMax = emMaxCol && Number(getCellValue(table, row, 'em_max'));
    if (emMinCol && emMinCol && !isNaN(emMin) && !isNaN(emMax)) {
        const v = (emMin + emMax) / 2;
        const {units} = emMaxCol;
        let vToUse;
        if (units === 'm' || units === 'meters') vToUse = v * 1000000;
        if (units === 'um') vToUse = v;
        if (vToUse) r.setAttributes({[PlotAttribute.WAVE_LENGTH_UM]: sprintf('%.2f', vToUse),});
    }
    const bandDesc = getMetaEntry(table, 'bandDesc');
    if (bandDesc) r.setAttributes({[PlotAttribute.WAVE_TYPE]: bandDesc});

    const coverage = getMetaEntry(table, 'coverage');
    if (coverage) r.setAttributes({[PlotAttribute.PROJ_TYPE_DESC]: coverage});

    const helpUrl = getMetaEntry(table, 'helpUrl');
    if (helpUrl) r.setAttributes({[PlotAttribute.DATA_HELP_URL]: helpUrl});

    if (positionWP) r.setOverlayPosition(positionWP);
    r.setInitialRangeValues(RangeValues.make2To10SigmaLinear());
    return r;
}