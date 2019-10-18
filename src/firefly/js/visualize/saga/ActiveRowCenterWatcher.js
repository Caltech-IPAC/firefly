/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isString, isObject} from 'lodash'
import {TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_REMOVE,TABLE_UPDATE,TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import {visRoot, dispatchRecenter} from '../ImagePlotCntlr.js';
import {getTblById, getCellValue} from '../../tables/TableUtil.js';
import Point, {makeWorldPt, makeAnyPt} from '../Point.js';
import {isHiPS} from '../WebPlot.js';
import {findTableCenterColumns} from '../../util/VOAnalyzer.js';
import {getActivePlotView, getCenterOfProjection, getFoV, hasWCSProjection, primePlot} from '../PlotViewUtil';
import {computeDistance, toDegrees} from '../VisUtil';
import {CysConverter} from '../CsysConverter';
import {dispatchPlotImage, dispatchUseTableAutoScroll} from '../ImagePlotCntlr';
import {isColRadians} from '../../tables/TableUtil';
import {PlotAttribute} from '../PlotAttribute';
import {isImage} from '../WebPlot';
import {getAppOptions} from '../../core/AppDataCntlr';





/** type {TableWatcherDef} */
export const activeRowCenterDef = {
    id : 'ActiveRowCenter',
    watcher : recenterImages,
    testTable : (table) => {
        return Boolean(findTableCenterColumns(table));
    },
    allowMultiples: false,
    actions: [TABLE_LOADED, TABLE_SELECT, TABLE_HIGHLIGHT, TABLE_UPDATE, TBL_RESULTS_ACTIVE, TABLE_REMOVE]
};

export function recenterImages(tbl_id, action, cancelSelf, params) {

    if (!action) return;

    const {imageScrollsToActiveTableOnLoadOrSelect}= getAppOptions();
    const {payload}= action;
    if (payload.tbl_id && payload.tbl_id!==tbl_id) return params;

    switch (action.type) {
        case TABLE_LOADED:
            recenterImageActiveRow(tbl_id, imageScrollsToActiveTableOnLoadOrSelect);
            break;

        case TABLE_SELECT:
            recenterImageActiveRow(tbl_id, imageScrollsToActiveTableOnLoadOrSelect);
            break;

        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
            recenterImageActiveRow(tbl_id);
            break;

        case TABLE_REMOVE:
            cancelSelf();
            break;

        case TBL_RESULTS_ACTIVE:
            recenterImageActiveRow(tbl_id, imageScrollsToActiveTableOnLoadOrSelect);
            break;

    }
}


/**
 * Return true if the new point is in outside of 90% of the viewing area or it is a HiPS with the new point
 * over a 1/2 degree from the center or it is a HiPS with a FOV greater than 1 degree
 * @param {PlotView} pv
 * @param {WorldPt} wp
 * @return {boolean}
 */
function shouldRecenter(pv,wp) {//todo - keep the function for a year in case we decide to go back to it (7/2/2019)
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

/**
 * return true if the point is not one the display
 * @param pv
 * @param wp
 * @return {boolean}
 */
function shouldRecenterSimple(pv,wp) {
    const plot= primePlot(pv);
    if (!plot) return false;
    const cc= CysConverter.make(plot);
    return !cc.pointOnDisplay(wp);
}


function recenterImageActiveRow(tbl_id, force=false) {

    const vr= visRoot();
    if (!force && !vr.autoScrollToHighlightedTableRow) return;
    if (!vr.useAutoScrollToHighlightedTableRow) {
        dispatchUseTableAutoScroll(true);
        return;
    }
    const tbl= getTblById(tbl_id);
    const pv = getActivePlotView(visRoot());
    const plot = primePlot(pv);

    if (!plot || !hasWCSProjection(plot)) return;
    const wp= getRowCenterWorldPt(tbl);
    if (!wp) return;

    if (force || shouldRecenterSimple(pv,wp)) {
        dispatchRecenter({plotId: plot.plotId, centerPt: wp});
        if (plot && isImage(plot) && plot.attributes[PlotAttribute.REPLOT_WITH_NEW_CENTER]) {
            if (plot.projection.isWrappingProjection()) return; // it is all sky, don't do anything
            const r= pv.request.makeCopy();
            r.setWorldPt(wp);
            r.setPlotId(pv.plotId);
            dispatchPlotImage({plotId:pv.plotId, wpRequest:r, hipsImageConversion:pv.plotViewCtx.hipsImageConversion,
                        attributes:plot.attributes});
        }


    }
}

/**
 *
 * @param {TableModel|String} tableOrId
 * @return {WorldPt|Point|undefined}
 */
export function getRowCenterWorldPt(tableOrId) {
    const tbl=  getTableModel(tableOrId);
    if (!tbl) return;
    const cenCol= findTableCenterColumns(tbl);
    if (!cenCol) return;
    const {lonCol,latCol,csys}= cenCol;
    const lon= Number(getCellValue(tbl,tbl.highlightedRow, lonCol));
    const lat= Number(getCellValue(tbl,tbl.highlightedRow, latCol));
    if (isNaN(lon) || isNaN(lat)) return;

    const tmpPt= makeAnyPt(lon,lat,csys);
    if (tmpPt.type!==Point.W_PT) return tmpPt;
    return makeWorldPt(isColRadians(tbl,lonCol)? toDegrees(lon) : lon, isColRadians(tbl,latCol)? toDegrees(lat): lat, csys);
}

function getTableModel(tableOrId) {
    if (isString(tableOrId)) return getTblById(tableOrId);  // was passed a table Id
    if (isObject(tableOrId)) return tableOrId;
    return undefined;
}
