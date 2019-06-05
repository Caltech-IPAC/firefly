/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_REMOVE,TABLE_UPDATE,TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import {visRoot, dispatchRecenter} from '../ImagePlotCntlr.js';
import {getTblById, isTableUsingRadians, getCellValue} from '../../tables/TableUtil.js';
import {makeWorldPt} from '../Point.js';
import {isHiPS} from '../WebPlot.js';
import {findTableCenterColumns} from '../../util/VOAnalyzer.js';
import {getActivePlotView, getCenterOfProjection, getFoV, hasWCSProjection, primePlot} from '../PlotViewUtil';
import {computeDistance, toDegrees} from '../VisUtil';
import {CysConverter} from '../CsysConverter';





/** type {TableWatcherDef} */
export const activeRowCenterDef = {
    id : 'ActiveRowCenter',
    watcher : recenterImages,
    testTable : (table) => {
        return Boolean(findTableCenterColumns(table))
    },
    actions: [TABLE_LOADED, TABLE_SELECT, TABLE_HIGHLIGHT, TABLE_UPDATE, TBL_RESULTS_ACTIVE, TABLE_REMOVE]
};


export function recenterImages(tbl_id, action, cancelSelf, params) {

    if (!action) return;
    
    const {payload}= action;
    if (payload.tbl_id && payload.tbl_id!==tbl_id) return params;

    switch (action.type) {
        case TABLE_LOADED:
            recenterImageActiveRow(tbl_id);
            break;

        case TABLE_SELECT:
            recenterImageActiveRow(tbl_id);
            break;

        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
            recenterImageActiveRow(tbl_id);
            break;

        case TABLE_REMOVE:
            cancelSelf();
            break;

        case TBL_RESULTS_ACTIVE:
            recenterImageActiveRow(tbl_id);
            break;

    }
}

/**
 *
 * @param {TableModel} tbl
 * @return {WorldPt}
 */
function getRowCenterWorldPt(tbl) {
    if (!tbl) return;
    const cenCol= findTableCenterColumns(tbl);
    if (!cenCol) return;
    const isRad= isTableUsingRadians(tbl);
    const lon= Number(getCellValue(tbl,tbl.highlightedRow, cenCol.lonCol));
    const lat= Number(getCellValue(tbl,tbl.highlightedRow, cenCol.latCol));
    if (isNaN(lon) || isNaN(lat)) return;

    return makeWorldPt(isRad? toDegrees(lon) : lon, isRad? toDegrees(lat): lat, cenCol.csys);
}

/**
 * Return true if the new point is in outside of 90% of the viewing area or it is a HiPS with the new point
 * over a 1/2 degree from the center or it is a HiPS with a FOV greater than 1 degree
 * @param {PlotView} pv
 * @param {WorldPt} wp
 * @return {boolean}
 */
function shouldRecenter(pv,wp) {
    const plot= primePlot(pv);
    if (!plot) return false;
    const cc= CysConverter.make(plot);
    const devPt= cc.getDeviceCoords(wp);
    const {width,height}= pv.viewDim;
    let forceRecenter= false;
    if (isHiPS(plot)) {
        const cenWp= getCenterOfProjection(plot);
        if (getFoV(pv) > 1 || computeDistance(cenWp, wp)>.5) {
            forceRecenter= true;
        }
    }
    if (!forceRecenter && cc.pointOnDisplay(wp)) {
        if (devPt.x > width*.1 && devPt.x<width*.9 && devPt.y>height*.1 && devPt.y<height*.9 ) {
            return false;
        }
    }
    return true;

}

function recenterImageActiveRow(tbl_id) {

    if (!visRoot().autoScrollToHighlightedTableRow) return;
    const tbl= getTblById(tbl_id);
    const pv = getActivePlotView(visRoot());
    const plot = primePlot(pv);

    if (!plot || !hasWCSProjection(plot)) return;
    const wp= getRowCenterWorldPt(tbl);
    if (!wp) return;

    if (shouldRecenter(pv,wp)) dispatchRecenter({plotId: plot.plotId, centerPt: wp});
}
