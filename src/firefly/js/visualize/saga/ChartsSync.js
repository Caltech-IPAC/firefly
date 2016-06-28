/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';

import {has} from 'lodash';

import {flux} from '../../Firefly.js';
import {MetaConst} from '../../data/MetaConst.js';

import * as TablesCntlr from '../../tables/TablesCntlr.js';
import * as TableStatsCntlr from '../TableStatsCntlr.js';
import * as TableUtil from '../../tables/TableUtil.js';

import * as XYPlotCntlr from '../XYPlotCntlr.js';
import * as HistogramCntlr from '../HistogramCntlr.js';
import {hasRelatedCharts} from '../ChartUtil.js';



/**
 * this saga handles chart related side effects
 */
export function* syncCharts() {
    var tableStatsState, xyPlotState, histogramState;

    while (true) {
        const action= yield take([TableStatsCntlr.SETUP_TBL_TRACKING, TablesCntlr.TABLE_NEW_LOADED, TablesCntlr.TABLE_SORT]);
        const request= action.payload.request;
        switch (action.type) {
            case TableStatsCntlr.SETUP_TBL_TRACKING:
                const {tblId} = action.payload;
                if (TableUtil.isFullyLoaded(tblId)) {
                    TableStatsCntlr.dispatchLoadTblStats(TableUtil.getTblById(tblId)['request']);
                    if (!hasRelatedCharts(tblId)) {
                        // default chart is xy plot of coordinate columns or first two numeric columns
                        const defaultParams = getDefaultXYPlotParams(tblId);
                        if (defaultParams) {
                            XYPlotCntlr.dispatchLoadPlotData(tblId, defaultParams, tblId);
                        }
                    }
                }
                break;
            case TablesCntlr.TABLE_SORT:
            case TablesCntlr.TABLE_NEW_LOADED:
                const {tbl_id} = action.payload;
                let hasRelated = false;

                xyPlotState = flux.getState()[XYPlotCntlr.XYPLOT_DATA_KEY];
                Object.keys(xyPlotState).forEach((cid) => {
                    if (xyPlotState[cid].tblId === tbl_id) {
                        const xyPlotParams = xyPlotState[cid].xyPlotParams;
                        XYPlotCntlr.dispatchLoadPlotData(cid, xyPlotParams, tbl_id);
                        hasRelated = true;
                    }
                });

                // table statistics and histogram data do not change on table sort
                if (action.type !== TablesCntlr.TABLE_SORT) {
                    histogramState = flux.getState()[HistogramCntlr.HISTOGRAM_DATA_KEY];
                    Object.keys(histogramState).forEach((cid) => {
                        if (histogramState[cid].tblId === tbl_id) {
                            const histogramParams = histogramState[cid].histogramParams;
                            HistogramCntlr.dispatchLoadColData(cid, histogramParams, tbl_id);
                            hasRelated = true;
                        }
                    });

                    tableStatsState = flux.getState()[TableStatsCntlr.TBLSTATS_DATA_KEY];
                    if (has(tableStatsState, tbl_id)) {
                        TableStatsCntlr.dispatchLoadTblStats(request);
                        if (!hasRelated) {
                            // default chart is xy plot of coordinate columns or first two numeric columns
                            const defaultParams = getDefaultXYPlotParams(tbl_id);
                            if (defaultParams) {
                                XYPlotCntlr.dispatchLoadPlotData(tbl_id, defaultParams, tbl_id);
                            }
                        }
                    }
                }

                break;
        }
    }
}

function colWithName(cols, name) {
    return cols.find((c) => { return c.name===name; });
}

function getNumericCols(cols) {
    const ncols = [];
    cols.forEach((c) => {
        if (c.type !== 'char') {
            ncols.push(c);
        }
    });
    return ncols;
}

function getDefaultXYPlotParams(tbl_id) {

    const {tableMeta, tableData, totalRows}= TableUtil.getTblById(tbl_id);

    if (!totalRows) {
        return;
    }

    // for catalogs use lon and lat columns
    let isCatalog = Boolean(tableMeta[MetaConst.CATALOG_OVERLAY_TYPE] && tableMeta[MetaConst.CATALOG_COORD_COLS]);
    let xCol = undefined, yCol = undefined;

    if (isCatalog) {
        const s = tableMeta[MetaConst.CATALOG_COORD_COLS].split(';');
        if (s.length !== 3) return;
        xCol = colWithName(tableData.columns, s[0]); // longtitude
        yCol = colWithName(tableData.columns, s[1]); // latitude

        if (!xCol || !yCol) {
            isCatalog = false;
        }
    }

    // otherwise use the first one-two numeric columns
    if (!isCatalog) {
        const numericCols = getNumericCols(tableData.columns);
        if (numericCols.length > 2) {
            xCol = numericCols[0];
            yCol = numericCols[1];
        } else if (numericCols.length > 1) {
            xCol = numericCols[0];
            yCol = numericCols[0];
        }
    }

    return (xCol && yCol) ?
    {
        x: {columnOrExpr: xCol.name, label: xCol.name, unit: xCol.units?xCol.units:''},
        y: {columnOrExpr: yCol.name, label: yCol.name, unit: yCol.units?yCol.units:''}
    } : undefined;
}