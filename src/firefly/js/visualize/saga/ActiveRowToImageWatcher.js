/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isString, isObject, once} from 'lodash';
import {TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_REMOVE,TABLE_UPDATE,TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import {findTableCenterColumns} from '../../voAnalyzer/TableAnalysis.js';
import {visRoot, dispatchRecenter, dispatchChangeSubHighPlotView} from '../ImagePlotCntlr.js';
import {
    getTblById, getCellValue, hasSubHighlightRows, isSubHighlightRow, getActiveTableId
} from '../../tables/TableUtil.js';
import {computeBoundingBoxInDeviceCoordsForPlot, isFullyOnScreen} from '../WebPlotAnalysis';
import {makeAnyPt} from '../Point.js';
import { getActivePlotView, hasWCSProjection, primePlot, } from '../PlotViewUtil';
import {CysConverter} from '../CsysConverter';
import ImagePlotCntlr, {dispatchPlotImage, dispatchUseTableAutoScroll} from '../ImagePlotCntlr';
import {isTableUsingRadians} from '../../tables/TableUtil';
import {PlotAttribute} from '../PlotAttribute';
import {isImage} from '../WebPlot';
import {getAppOptions} from '../../core/AppDataCntlr';
import CoordinateSys from '../CoordSys';




function willFitOnScreenAtCurrentZoom(pv) {
    const {w,h}=computeBoundingBoxInDeviceCoordsForPlot(primePlot(pv)) ?? {};
    return (pv.viewDim.width+3>=w && pv.viewDim.height+3>=h);
}

const getTableActions= () =>  [TABLE_LOADED, TABLE_SELECT, TABLE_HIGHLIGHT, TABLE_UPDATE, TBL_RESULTS_ACTIVE, TABLE_REMOVE];
const getImageActions= () =>  [ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW, ImagePlotCntlr.ANY_REPLOT];


/**
 *
 * @returns {TableWatcherDef}
 */
export const getActiveRowToImageDef= once(() => ({
    id : 'ActiveRowToImage',
    watcher : updateImages,
    testTable : (table) => {
        return Boolean(findTableCenterColumns(table) || hasSubHighlightRows(table));
    },
    allowMultiples: false,
    actions: [...getTableActions(), ...getImageActions()]
}));

export function updateImages(tbl_id, action, cancelSelf, params) {

    if (!action) return;

    const {imageScrollsToActiveTableOnLoadOrSelect}= getAppOptions();
    const {payload}= action;


    if (getImageActions().includes(action.type)) {
        if (getActiveTableId()!==tbl_id) return;
        checkSubHighlight(tbl_id);
        return params;
    }

    if (getTableActions().includes(action.type)) {
        if (payload.tbl_id && payload.tbl_id !== tbl_id) return params;
        checkSubHighlight(tbl_id);
        switch (action.type) {
            case TBL_RESULTS_ACTIVE:
            case TABLE_LOADED:
                recenterImageActiveRow(tbl_id, imageScrollsToActiveTableOnLoadOrSelect);
                break;

            case TABLE_HIGHLIGHT:
            case TABLE_UPDATE:
                recenterImageActiveRow(tbl_id);
                break;

            case TABLE_REMOVE:
                cancelSelf();
                break;
        }
        return params;
    }
}

const isImageAitoff= (plot) => (isImage(plot) && plot.projection.isWrappingProjection());

/**
 * return true if the point is not one the display
 * @param {PlotView} pv
 * @param {WorldPt} wp
 * @param {TableModel} tbl
 * @param {boolean} force
 * @return {boolean}
 */
function shouldRecenterSimple(pv,wp, tbl, force) {
    const plot= primePlot(pv);
    if (force) {
        if (!isImageAitoff(plot)) return true; // if not an image aitoff projection
        return !(isFullyOnScreen(plot,pv.viewDim));
    }
    if (!plot) return false;
    const {attributes}= plot;
    if (tbl.tbl_id===attributes[PlotAttribute.RELATED_TABLE_ID] && attributes[PlotAttribute.RELATED_TABLE_ROW]) {
        if (Number(attributes[PlotAttribute.RELATED_TABLE_ROW])!==tbl.highlightedRow) return;
    }
    const cc= CysConverter.make(plot);
    return !cc.pointOnDisplay(wp);
}


/**
 *
 * @param tbl_id the table id
 * @param force force a recenter under all cases
 */
function recenterImageActiveRow(tbl_id, force=false) {

    const vr= visRoot();
    if (!force && !vr.autoScrollToHighlightedTableRow) return;
    const table= getTblById(tbl_id);
    if (!findTableCenterColumns(table)) return;

    if (!vr.useAutoScrollToHighlightedTableRow) {
        dispatchUseTableAutoScroll(true);
        return;
    }
    const pv = getActivePlotView(visRoot());
    const plot = primePlot(pv);

    if (!plot || !hasWCSProjection(plot)) return;
    const wp= getRowCenterWorldPt(table);
    if (!wp) return;

    if (shouldRecenterSimple(pv,wp,table,force)) {
        isImageAitoff(plot) && willFitOnScreenAtCurrentZoom(pv) ?
            dispatchRecenter({plotId: plot.plotId, centerOnImage:true}) : dispatchRecenter({plotId: plot.plotId, centerPt: wp});
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



function toAngle(d, radianToDegree)  {
    const v= Number(d ?? NaN);
    if (isNaN(v)) return v;
    return radianToDegree ? v*180/Math.PI : v;
}


/**
 *
 * @param {TableModel|String} tableOrId
 * @return {WorldPt|Point|undefined}
 */
export function getRowCenterWorldPt(tableOrId) {
    const tbl=  getTableModel(tableOrId);
    if (!tbl) return;
    const cenCol= findTableCenterColumns(tbl,true);
    if (!cenCol) return;
    const {lonCol,latCol,csys}= cenCol;

    let lon;
    let lat;

    if (lonCol===latCol) {
        const latlonAry= getCellValue(tbl,tbl.highlightedRow, lonCol);
        lon= toAngle(latlonAry[0],false);
        lat= toAngle(latlonAry[1],false);
    }
    else {
        const rad= isTableUsingRadians(tbl, [lonCol,latCol]);
        lon= toAngle(getCellValue(tbl,tbl.highlightedRow, lonCol),rad);
        lat= toAngle(getCellValue(tbl,tbl.highlightedRow, latCol),rad);
    }
    if (isNaN(lon) || isNaN(lat)) return;
    return makeAnyPt(lon,lat,csys||CoordinateSys.EQ_J2000);
}

/**
 *
 * @param tableOrId
 * @returns {undefined|TableModel|*}
 */
function getTableModel(tableOrId) {
    if (isString(tableOrId)) return getTblById(tableOrId);  // was passed a table Id
    if (isObject(tableOrId)) return tableOrId;
    return undefined;
}

/**
 *
 * @param tbl_id
 */
function checkSubHighlight(tbl_id) {
    const table= getTblById(tbl_id);
    if (!table?.tableMeta) return;
    if (!hasSubHighlightRows(table)) return;
    const {highlightedRow:row}= table;
    if (isNaN(row) || row<0) return;

    const newSubAry= visRoot().plotViewAry
        .map((pv) => {
            const plot= primePlot(pv);
            if (!plot) return;
            const imRow= Number(plot.attributes[PlotAttribute.RELATED_TABLE_ROW]);
            const imTblId= plot.attributes[PlotAttribute.RELATED_TABLE_ID];
            if (!imTblId || isNaN(imRow) || row<0) return;
            const subHighlight= tbl_id===imTblId && isSubHighlightRow(tbl_id,imRow);
            return {plotId:plot.plotId, subHighlight};
        })
        .filter(Boolean);
    if (newSubAry.length) dispatchChangeSubHighPlotView({subHighlightAry:newSubAry});
}
