import pointInPolygon from 'point-in-polygon';
import {dispatchAddTaskCount, dispatchRemoveTaskCount} from '../../core/AppDataCntlr';
import {FilterInfo} from '../../tables/FilterInfo';
import {
    DATA_NORDER, ensureDataForSelection, getAllWptsIdxsForTile, getHpxIndexData, getValuesForOrder, HPX_WORKING_KEY,
} from '../../tables/HpxIndexCntlr';
import {SelectInfo} from '../../tables/SelectInfo';
import {dispatchTableFilter, dispatchTableSelect} from '../../tables/TablesCntlr';
import {getTblById} from '../../tables/TableUtil';
import BrowserInfo from '../../util/BrowserInfo';
import CoordSys from '../../visualize/CoordSys';
import CysConverter from '../../visualize/CsysConverter';
import {dlRoot} from '../../visualize/DrawLayerCntlr';
import {getCatalogNorderlevel, getCornersForCell} from '../../visualize/HiPSUtil';
import {PlotAttribute} from '../../visualize/PlotAttribute';
import {DEFAULT_COVERAGE_PLOT_ID, getAllDrawLayersForPlot, primePlot} from '../../visualize/PlotViewUtil';
import {detachSelectArea} from '../../visualize/ui/SelectAreaDropDownView';
import {contains, containsEllipse} from '../../visualize/VisUtil';
import SelectArea from '../SelectArea';
import {SelectedShape} from '../SelectedShape';


export const TYPE_ID= 'HPX_CATALOG_TYPE';
export const HPX_GROUP_TYPE_PREF= 'HPX_GROUP_TYPE_PREF';
export const HPX_MIN_GROUP_PREF= 'HPX_MIN_GROUP_PREF';
export const BOX_GROUP_TYPE = 'BOX_GROUP_TYPE';
export const ELLIPSE_GROUP_TYPE = 'ELLIPSE_GROUP_TYPE';
export const HEALPIX_GROUP_TYPE = 'HEALPIX_GROUP_TYPE';
export const HEAT_MAP_GROUP_TYPE = 'HEAT_MAP_GROUP_TYPE';
export const HPX_GRID_SIZE_PREF= 'HPX_GROUP_SIZE_PREF';
export const HPX_HEATMAP_LABEL_PREF= 'HPX_HEATMAP_LABEL_PREF';
export const HPX_HEATMAP_STRETCH_PREF= 'HPX_HEATMAP_STRETCH_PREF';
export const HPX_STRETCH_LINEAR= 'HPX_STRETCH_LINEAR';
export const HPX_STRETCH_LINEAR_COMPRESSED= 'HPX_STRETCH_LINEAR_COMPRESSED';
export const HPX_STRETCH_LOG= 'HPX_STRETCH_LOG';
export const HPX_GRID_SIZE_LARGE= 128;
export const HPX_GRID_SIZE_SMALL= 64;
export const DEFAULT_MIN_HPX_GROUP= 40;
export const DEFAULT_HPX_GRID_SIZE= HPX_GRID_SIZE_LARGE;
export const DEFAULT_HPX_GROUP_TYPE= HEALPIX_GROUP_TYPE;
export const DEFAULT_HPX_STRETCH= HPX_STRETCH_LOG;
export const DEFAULT_HEATMAP_LABELS= true;

export function getHeatMapGridSize() {
    const HPX_GRID_SIZE_VERY_SMALL= 32;
    const HPX_GRID_SIZE_VERY_SMALL_PERFORMANT= 16;
    if (BrowserInfo.isChrome()) return HPX_GRID_SIZE_VERY_SMALL_PERFORMANT;
    if (BrowserInfo.isSafari()) return HPX_GRID_SIZE_VERY_SMALL;
    if (BrowserInfo.isFirefox()) return HPX_GRID_SIZE_VERY_SMALL_PERFORMANT;
    return HPX_GRID_SIZE_VERY_SMALL;
}

export function getHeatMapNorder(largeSizeNorder) {
    if (largeSizeNorder>=DATA_NORDER) return {showLabels:true, norder:DATA_NORDER};
    if (largeSizeNorder===DATA_NORDER-1) return {showLabels:true, norder:DATA_NORDER};
    if (largeSizeNorder===DATA_NORDER-2) return {showLabels:true, norder:DATA_NORDER-1};
    if (largeSizeNorder===DATA_NORDER-3) return {showLabels:true, norder:DATA_NORDER-2};

    let performant= false;
    if (BrowserInfo.isChrome()) performant= true;

    if (performant) {
        return {showLabels:false, norder:largeSizeNorder+3};
    }
    else {
        if (largeSizeNorder<=6) return {showLabels:false, norder:largeSizeNorder+3};
        else return {showLabels:false, norder:largeSizeNorder+2};
    }
}




const getLayers = (pv, dlAry) =>
    getAllDrawLayersForPlot(dlAry, pv.plotId, true).filter((dl) => dl.drawLayerTypeId === TYPE_ID);

export function selectHpxCatalog(pv, dlAry = dlRoot().drawLayerAry) {
    const {p,sel, selectedShape, catDlAry}= setupSelection(pv,dlAry);
    if (!sel || !catDlAry?.length) return;
    catDlAry.forEach((dl) => doSelect(dl,p,sel,selectedShape));
    detachSelectArea(pv);
}

export function filterHpxCatalog(pv, dlAry) {
    const {p,sel, selectedShape, catDlAry}= setupSelection(pv,dlAry);
    if (!sel || !catDlAry?.length) return;
    catDlAry.forEach((dl) => dl.canFilter && doFilter(dl, p, sel, selectedShape));
    detachSelectArea(pv);
}

function setupSelection(pv,dlAry) {
    const p = primePlot(pv);
    const sel = p.attributes[PlotAttribute.SELECTION];
    if (!sel) return {sel, catDlAry: undefined};
    const catDlAry = getLayers(pv, dlAry);
    if (!catDlAry?.length) return {sel, catDlAry: undefined};
    const selectedShape = getSelectedShape(pv);
    return {p,sel, selectedShape, catDlAry};
}


export function unselectHxpCatalog(pv, dlAry) {
    getLayers(pv, dlAry).forEach((dl) => {
        const selectInfoCls = SelectInfo.newInstance({rowCount: dl.drawData.data.length});
        dispatchTableSelect(dl.tbl_id, selectInfoCls.data);
    });
}


async function doSelect(dl, p, sel, selectedShape) {
    const selectInfoCls = SelectInfo.newInstance({rowCount: dl.drawData.data.length});
    const hpxSelectList = await getHpxSelectedPts(dl, p, sel, selectedShape);
    hpxSelectList.forEach(({idx}) => selectInfoCls.setRowSelect(idx, true));
    dispatchTableSelect(dl.tbl_id, selectInfoCls.data, {hpxSelectList, row:undefined});
}

async function doFilter(dl, p, sel, selectedShape) {
    const tbl = getTblById(dl.tbl_id);
    if (!tbl) return;
    const filterInfo = tbl?.request?.filters;
    const filterInfoCls = FilterInfo.parse(filterInfo);
    const hpxSelectList= await getHpxSelectedPts(dl, p, sel, selectedShape);
    const idxAry= hpxSelectList.map( (e) => e.idx);
    const filter = idxAry.length ? `IN (${idxAry.toString()})` : undefined;
    filterInfoCls.setFilter('ROW_IDX', filter);
    const newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
    if (filter) dispatchTableFilter(newRequest);
}


export function clearHpxFilterCatalog(pv, dlAry) {
    const catDlAry = getLayers(pv, dlAry);
    if (!catDlAry.length) return;
    catDlAry.forEach((dl) => dispatchTableFilter({tbl_id: dl.tbl_id, filters: ''}));
}



async function getHpxSelectedPts(dl, p, sel, selectedShape) {
    const minNOrder = 8;
    const idxData = getHpxIndexData(dl.tbl_id);
    const norder = getCatalogNorderlevel(p, minNOrder, idxData.maxInitialLoadNorder,dl.gridSize);
    const allTileDataList = getValuesForOrder(idxData.orderData, norder) ?? [];
    const selectedPixels = await getSelectedHealPixFromShape(dl.tbl_id, sel, p, allTileDataList, norder, selectedShape);
    return selectedPixels;
}

async function getSelectedHealPixFromShape(tbl_id, selection, plot, tileList, norder, selectedShape) {
    if (!selection || !plot && !tileList?.length) return [];
    const cc = CysConverter.make(plot);
    const pt0 = cc.getDeviceCoords(selection.pt0);
    const pt1 = cc.getDeviceCoords(selection.pt1);
    if (!pt0 || !pt1) return [];

    if (selectedShape === SelectedShape.circle.key) {
        return await getSelectedHealPixFromEllipse(tbl_id, cc, pt0, pt1, tileList, norder);
    } else {
        return await getSelectedHealPixFromRect(tbl_id, cc, pt0, pt1, tileList, norder);
    }
}

async function getSelectedHealPixFromEllipse(tbl_id, cc, pt0, pt1, tileList, norder) {
    const c_x = (pt0.x + pt1.x) / 2;
    const c_y = (pt0.y + pt1.y) / 2;
    const r1 = Math.abs(pt0.x - pt1.x) / 2;
    const r2 = Math.abs(pt0.y - pt1.y) / 2;
    const containsTest = (pt) => pt && containsEllipse(pt.x, pt.y, c_x, c_y, r1, r2);
    return await getSelectedHealPix(tbl_id, cc, pt0, pt1, tileList, norder, containsTest);
}

async function getSelectedHealPixFromRect(tbl_id, cc, pt0, pt1, tileList, norder) {
    const x = Math.min(pt0.x, pt1.x);
    const y = Math.min(pt0.y, pt1.y);
    const width = Math.abs(pt0.x - pt1.x);
    const height = Math.abs(pt0.y - pt1.y);
    const containsTest = (pt) => pt && contains(x, y, width, height, pt.x, pt.y);
    return await getSelectedHealPix(tbl_id, cc, pt0, pt1, tileList, norder, containsTest);
}

/**
 *
 * @param {String} tbl_id
 * @param {CysConverter} cc
 * @param {Point} pt0
 * @param {Point} pt1
 * @param {Array.<Number>} tileList
 * @param {number} norder
 * @param {Function} containsTest
 * @return {Promise}
 */
async function getSelectedHealPix(tbl_id, cc, pt0, pt1, tileList, norder, containsTest) {
    const selectedTiles = [];
    let idxData = getHpxIndexData(tbl_id);
    tileList.forEach((tile) => {
        const pix = getCornersForCell(norder, tile.pixel, CoordSys.EQ_J2000);
        const devC = pix.wpCorners.map((wp) => cc.getDeviceCoords(wp)).filter(Boolean);
        if (devC.length < 2) return;
        const polygon= devC.map( ({x,y}) => [x,y]);
        const match= pointInPolygon([pt0.x,pt0.y], polygon)
            || pointInPolygon([pt1.x,pt1.y], polygon)
            || devC.some((pt) => containsTest(pt));
        if (match) selectedTiles.push(tile);
    });

    dispatchAddTaskCount(DEFAULT_COVERAGE_PLOT_ID,HPX_WORKING_KEY);
    idxData= await ensureDataForSelection(tbl_id,norder,selectedTiles);
    dispatchRemoveTaskCount(DEFAULT_COVERAGE_PLOT_ID,HPX_WORKING_KEY);

    // const selectedTableIdxList = [];
    const hpxSelectList= [];
    selectedTiles.forEach((tile) => {
        getAllWptsIdxsForTile(idxData,norder,tile.pixel)
            .forEach(({wp,idx,dataNorderTile}) => {
                const pt = cc.getDeviceCoords(wp); //
                if (containsTest(pt)) hpxSelectList.push({idx,dataNorderTile});
            });
    });
    return hpxSelectList;
}

/**
 * get the selected area shape from the SelectArea layer
 * @param pv
 * @param dlAry
 * @returns {string}
 */
function getSelectedShape(pv, dlAry = dlRoot().drawLayerAry) {
    const selectAreaLayer = getAllDrawLayersForPlot(dlAry, pv.plotId, true)
        .find((dl) => dl.drawLayerTypeId === SelectArea.TYPE_ID);
    return selectAreaLayer?.selectedShape ?? SelectedShape.rect.key;
}
