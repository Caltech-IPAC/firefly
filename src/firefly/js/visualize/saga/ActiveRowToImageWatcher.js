/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isString, isObject, once} from 'lodash';
import {TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_REMOVE,TABLE_UPDATE,TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import {isDefined} from '../../util/WebUtil';
import {findTableCenterColumns} from '../../voAnalyzer/TableAnalysis.js';
import {visRoot, dispatchRecenter, dispatchChangeSubHighPlotView} from '../ImagePlotCntlr.js';
import {
    getTblById, getCellValue, hasSubHighlightRows, isSubHighlightRow, getActiveTableId
} from '../../tables/TableUtil.js';
import {getMultiViewRoot, getViewer} from '../MultiViewCntlr';
import {computeBoundingBoxInDeviceCoordsForPlot, isFullyOnScreen} from '../WebPlotAnalysis';
import {makeAnyPt} from '../Point.js';
import {
    DEFAULT_COVERAGE_PLOT_ID, DEFAULT_COVERAGE_VIEWER_ID, getActivePlotView, getPlotViewById, hasWCSProjection,
    primePlot,
} from '../PlotViewUtil';
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
 * @param {WorldPt|Point} wp
 * @param {boolean} force
 * @return {boolean}
 */
function shouldCenterOnTableRow(pv, wp, force) {
    const plot= primePlot(pv);
    if (!plot) return false;
    if (pv?.plotViewCtx.ignorePanByTableRow) return false;
    if (force) {
        if (!isImageAitoff(plot)) return true; // if not an image aitoff projection
        return !(isFullyOnScreen(plot,pv.viewDim));
    }
    const cc= CysConverter.make(plot);
    return !cc.pointOnDisplay(wp);
}

function recenterImageActiveRow(tbl_id, force=false) {
    const pv = getActivePlotView(visRoot());
    if (!pv) return;
    recenterPlotViewActiveRow(pv, tbl_id, force);
    if (pv.plotId!==DEFAULT_COVERAGE_PLOT_ID) {
        const covView=getViewer(getMultiViewRoot(),DEFAULT_COVERAGE_VIEWER_ID);
        const covPv = getPlotViewById(visRoot(),DEFAULT_COVERAGE_PLOT_ID);
        if (!covPv || !covView.mounted) return;
        recenterPlotViewActiveRow(covPv, tbl_id, force);

    }
}

/**
 * recenter the image on the active row.
 *
 * for this to happen: autoScrollToHighlightedTableRow===true, the table must exist and have a center ra/dec,
 * the image much exist, and the image cannot be a MultiProductViewer image driven by a data product table.
 *
 * Therefore, types of image that recenter are usually pinned images or coverage images.
 *
 * @param {PlotView} pv PlotView to recenter
 * @param tbl_id the table id
 * @param force force a recenter under all cases
 */
function recenterPlotViewActiveRow(pv, tbl_id, force=false) {

    const vr= visRoot();
    if (!force && !vr.autoScrollToHighlightedTableRow) return;
    const plot = primePlot(pv);
    if (!plot || !hasWCSProjection(plot)) return;
    const wp= getRowCenterWorldPt(tbl_id);
    if (!wp) return;

    const {attributes}= plot;
    if (tbl_id===attributes[PlotAttribute.RELATED_TABLE_ID] &&
        isDefined(attributes[PlotAttribute.RELATED_TABLE_ROW]) &&
        !attributes[PlotAttribute.USER_PINNED_IMAGE]) {
        // Data product table images are very transient and should not recenter
        // When the image is drive by a data product table, then doing this is almost always undesirable.
        // In many grid view cases this will fource some images off the screen
        return;
    }


    if (!vr.useAutoScrollToHighlightedTableRow) {
        dispatchUseTableAutoScroll(true);
        return;
    }

    if (shouldCenterOnTableRow(pv,wp,force)) {
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
