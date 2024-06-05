import {uniqueId} from 'lodash';
import {sprintf} from '../../externalSource/sprintf.js';
import {getCellValue, getColumn, getMetaEntry} from '../../tables/TableUtil.js';
import {PlotAttribute} from '../../visualize/PlotAttribute.js';
import RangeValues from '../../visualize/RangeValues.js';
import {TitleOptions, WebPlotRequest} from '../../visualize/WebPlotRequest.js';
import {ZoomType} from '../../visualize/ZoomType.js';
import {getSSATitle, isSSATable} from '../../voAnalyzer/TableAnalysis.js';

/**
 *
 * @param dataSource
 * @param positionWP
 * @param titleStr
 * @param {TableModel} table
 * @param {number} row
 * @return {undefined|WebPlotRequest}
 */
export function makeObsCoreRequest(dataSource, positionWP, titleStr, table, row) {
    if (!dataSource) return undefined;
    const r = WebPlotRequest.makeURLPlotRequest(dataSource, 'DataProduct');
    r.setZoomType(ZoomType.FULL_SCREEN);
    const ssa= isSSATable(table);
    const titleStringToUse= ssa ? getSSATitle(table,row) ?? 'spectrum' : titleStr;
    if (titleStringToUse?.length > 7) {
        r.setTitleOptions(TitleOptions.NONE);
        r.setTitle(titleStringToUse);
    }
    else {
        r.setTitleOptions(TitleOptions.FILE_NAME);
    }
    r.setPlotId(uniqueId('obscore-'));

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