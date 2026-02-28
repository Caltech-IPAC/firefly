/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take, fork} from 'redux-saga/effects';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {dispatchUpdateLayoutInfo, dropDownManager, getLayouInfo, LO_VIEW} from '../../core/LayoutCntlr.js';
import {getTblIdsByGroup, smartMerge} from '../../tables/TableUtil.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TABLE_REMOVE, TBL_RESULTS_REMOVE} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';
import {visRoot} from 'firefly/visualize/ImagePlotCntlr';
import {CHART_ADD, CHART_REMOVE, dispatchChartAdd, getChartData} from '../../charts/ChartsCntlr.js';
import {getDefaultChartProps} from 'firefly/charts/ChartUtil';

const MAX_ROW = Math.pow(2,31) - 1;

export const ALERT = {
    TABLE_GROUP_MAIN: 'main',
    TABLE_GROUP_DETAILS: 'details',

    TABLE_1_ID: 'alert_table_1',
    TABLE_2_ID: 'alert_table_2',

    IMG_VIEWER_1: 'alert_img_viewer_1',
    IMG_VIEWER_2: 'alert_img_viewer_2',
    IMG_VIEWER_3: 'alert_img_viewer_3',

    IMG_PLOT_1: 'alert_plot_1',
    IMG_PLOT_2: 'alert_plot_2',
    IMG_PLOT_3: 'alert_plot_3',

    CHART_VIEWER_1: 'alert_chart_viewer_1',
    CHART_1_ID: 'alert_chart_1',

    FG_UPLOAD: 'ALERT_UPLOAD',
    MAX_IMAGE_CNT: 3,
    TABLE_PAGESIZE: MAX_ROW,
};

export function* alertManager({views='tables | images | xyPlots'} = {}) {
    const viewMask = LO_VIEW.get(views) || LO_VIEW.none;
    yield fork(dropDownManager);

    while (true) {
        const action = yield take([
            TBL_RESULTS_ADDED, TABLE_LOADED, TABLE_REMOVE, TBL_RESULTS_REMOVE, ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_IMAGE_START,
            ImagePlotCntlr.DELETE_PLOT_VIEW, CHART_ADD, CHART_REMOVE,
        ]);

        //chart creation trigger
        if (action.type === TABLE_LOADED && action.payload?.tbl_id === ALERT.TABLE_1_ID) {
            ensureMainChart();
        }

        const layoutInfo = getLayouInfo();
        const next = computeLayout(layoutInfo, viewMask);

        if (!next.coverageSide) next.coverageSide = getAppOptions()?.triViewCoverageSide;

        if (next !== layoutInfo) {
            dispatchUpdateLayoutInfo(next);
        }
    }
}

function computeLayout(layoutInfo, views) {
    const hasTables = (getTblIdsByGroup(ALERT.TABLE_GROUP_MAIN) || []).length > 0;

    const vr = visRoot();
    const hasImages = Boolean(vr?.plotViewAry?.length);
    const hasXyPlots = Boolean(layoutInfo?.hasXyPlots);

    const showTables = hasTables && views.has(LO_VIEW.tables);
    const showImages = hasImages && views.has(LO_VIEW.images);
    const showXyPlots = hasXyPlots && views.has(LO_VIEW.xyPlots);

    const count = [showTables, showImages, showXyPlots].filter(Boolean).length;
    const closeable = count > 1;

    //preserve expanded + standard; only patch missing defaults
    let expanded = layoutInfo?.mode?.expanded ?? LO_VIEW.none;
    const standard = layoutInfo?.mode?.standard ?? views;

    // If expanded points to something no longer available, collapse it.
    if (expanded !== LO_VIEW.none) {
        const ok =
            (expanded === LO_VIEW.tables && showTables) ||
            (expanded === LO_VIEW.images && showImages) ||
            (expanded === LO_VIEW.xyPlots && showXyPlots);
        if (!ok) expanded = LO_VIEW.none;
    }

    const mode = {expanded, standard, closeable};

    return smartMerge(layoutInfo, {
        hasTables,
        hasImages,
        hasXyPlots,
        showTables,
        showImages,
        showXyPlots,
        autoExpand: false,
        mode,
    });
}

function ensureMainChart() {
    //already created?
    const existing = getChartData(ALERT.CHART_1_ID, null);
    if (existing) return;

    const def = getDefaultChartProps(ALERT.TABLE_1_ID);
    if (!def) return;

    dispatchChartAdd({
        chartId: ALERT.CHART_1_ID,
        viewerId: ALERT.CHART_VIEWER_1, //this matches MultiProductChoice chartViewerId
        groupId: ALERT.TABLE_1_ID,  //group charts with that table
        ...def,
        deletable: false,
    });
}