import {isEmpty, uniq} from 'lodash';
import {isDialogVisible} from '../core/ComponentCntlr.js';
import { getExpandedMode, LO_VIEW, SET_LAYOUT, SET_LAYOUT_MODE, UPDATE_LAYOUT } from '../core/LayoutCntlr.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {dispatchTableAddLocal} from '../tables/TablesCntlr.js';
import {getTblById, getTblInfoById, processRequest} from '../tables/TableUtil.js';
import ImagePlotCntlr, { dispatchChangeActivePlotView, dispatchDeletePlotView, visRoot } from './ImagePlotCntlr.js';
import {
    DEFAULT_FITS_VIEWER_ID, EXPANDED_MODE_RESERVED, IMAGE,
    dispatchAddViewer, dispatchAddViewerItems, dispatchReplaceViewerItems, getMultiViewRoot, getViewerItemIds,
} from './MultiViewCntlr.js';
import {PlotAttribute} from './PlotAttribute.js';
import {
    getFormattedWaveLengthUnits, getPlotViewAry, getPlotViewById, isImageExpanded, primePlot
} from './PlotViewUtil.js';

export const IMAGE_VIEW_TABLE_ID = 'active-image-view-list-table';
export const EXPANDED_OPTIONS_DIALOG_ID= 'ExpandedOptionsPopup';
export const HIDDEN='_HIDDEN';

const [NAME_IDX, WAVE_LENGTH_UM, PID_IDX, STATUS, PROJ_TYPE_DESC, WAVE_TYPE, DATA_HELP_URL, ROW_IDX] = [0, 1, 2, 3, 4, 5, 6, 7];

const columnsTemplate = [];
columnsTemplate[NAME_IDX] = {name: 'Name', type: 'char', width: 22};
columnsTemplate[PID_IDX] = {name: 'plotId', type: 'char', width: 10, visibility: 'hidden'};
columnsTemplate[STATUS] = {name: 'Status', type: 'char', width: 10};
columnsTemplate[PROJ_TYPE_DESC] = {name: 'Type', type: 'char', width: 8};
columnsTemplate[WAVE_TYPE] = {name: 'Band', type: 'char', width: 9};
columnsTemplate[WAVE_LENGTH_UM] = {
    name: 'Wavelength',
    type: 'double',
    width: 8,
    units: getFormattedWaveLengthUnits('um')
};

columnsTemplate[DATA_HELP_URL] = {
    name: 'Help',
    type: 'location',
    width: 4,
    cellRenderer: 'ATag::href=${Help},target="image-doc"'+ `,label=<img src='images/info-16x16.png'/>` // eslint-disable-line
};


const getAttribute = (attributes, attribute, def='') => attributes?.[attribute] ?? def;
const makeEnumValues = (data, idx) => uniq(data.map((d) => d[idx]).filter((d) => d)).join(',');

const hasFilters= (tbl_id) => Boolean(getTblById(tbl_id)?.request?.filters);

const isSorted= (tbl_id) => Boolean(getTblById(tbl_id)?.request?.sortInfo);

const getPlotIdAryFromTable= (tbl_id) => getTblById(tbl_id)?.tableData?.data?.map((d) => d[PID_IDX]) ?? [];

export const removeImageViewDisplaySelected = () =>
    getSelectedPlotIds().forEach((plotId) => dispatchDeletePlotView({plotId}) );


export function getPvAryInViewer(viewerId) {
    const itemIds = getViewerItemIds(getMultiViewRoot(), viewerId) ?? [];
    return getPlotViewAry(visRoot()).filter((pv) => itemIds.includes(pv.plotId));
}

export function getCombinedItemIds(viewerId, hiddenViewerId) {
    const itemIds= getViewerItemIds(getMultiViewRoot(),viewerId) ?? [];
    const moreIds= getViewerItemIds(getMultiViewRoot(),hiddenViewerId) ?? [];
    const allIds=
        [...new Set([...itemIds,...moreIds])]
            .filter((id) => Boolean(getPlotViewById(visRoot(),id)));
    return allIds;
}



export function makeImViewDisplayModel(tbl_id, plotViewAry, allIds, oldModel) {
    const pvAry= allIds.map( (id) => getPlotViewById(visRoot(),id));
    const data = pvAry.map((pv) => {
        const plot = primePlot(pv);
        const attributes = plot ? plot.attributes : pv.request.getAttributes();
        const {plotId, serverCall, plottingStatusMsg, request} = pv;
        const title = plot ? plot.title : request.getTitle() || 'failed image';
        const row = [];
        let stat;
        if (serverCall === 'success') stat = 'Success';
        else if (serverCall === 'fail') stat = 'Fail';
        else stat = plottingStatusMsg;
        row[NAME_IDX] = title;
        row[PID_IDX] = plotId;
        row[STATUS] = stat;
        row[PROJ_TYPE_DESC] = getAttribute(attributes, PlotAttribute.PROJ_TYPE_DESC);
        row[WAVE_TYPE] = getAttribute(attributes, PlotAttribute.WAVE_TYPE);
        row[WAVE_LENGTH_UM] = parseFloat(getAttribute(attributes, PlotAttribute.WAVE_LENGTH_UM, 0.0));
        row[DATA_HELP_URL] = getAttribute(attributes, PlotAttribute.DATA_HELP_URL);
        return row;
    });

    const columns = [...columnsTemplate];
    columns[PROJ_TYPE_DESC].enumVals = makeEnumValues(data, PROJ_TYPE_DESC);
    columns[WAVE_TYPE].enumVals = makeEnumValues(data, WAVE_TYPE);
    columns[STATUS].enumVals = makeEnumValues(data, STATUS);
    columns[WAVE_LENGTH_UM].enumVals = makeEnumValues(data, WAVE_LENGTH_UM);


    const newSi = SelectInfo.newInstance({rowCount: data.length});
    let request;
    if (oldModel?.tableData?.data) {
        const oldSi = SelectInfo.newInstance(oldModel.selectInfo);
        const vr = visRoot();
        let filterStr = '';
        oldModel.tableData.data.forEach((row, idx) => {
            const plotId = row[PID_IDX];
            if (getPlotViewById(vr, plotId) && oldSi.isSelected(idx)) {
                const newIdx = data.findIndex((r) => r[PID_IDX] === plotId);
                newSi.setRowSelect(newIdx, true);
                filterStr += filterStr ? ',' + newIdx : newIdx;
            }
        });
        request = {...oldModel.request};
        if (oldModel?.request?.filters && newSi.data.rowCount > 0) {
            const {filters} = oldModel.request;
            if (filters && filters.indexOf('ROW_IDX' > -1)) {
                request.filters = filters.replace(/"ROW_IDX" IN \(.*\)/, `"ROW_IDX" IN (${filterStr})`);
            }
        }
    }


    const templateModel = {
        tbl_id,
        tableData: {columns, data},
        totalRows: data.length, highlightedRow: 0,
        selectInfo: newSi.data,
        tableMeta: {},
    };
    const tableModel= request ? processRequest(templateModel, request, templateModel.highlightedRow) : templateModel;
    dispatchTableAddLocal(tableModel, undefined, false);
    return tableModel;
}


export function updateImageViewDisplay(tbl_id, viewerId) {
    const model = getTblById(tbl_id);
    if (!model) return;
    const plotIdAry = model.tableData.data.map((d) => d[PID_IDX]);
    if (isEmpty(plotIdAry)) return;

    const currentPlotIdAry = getViewerItemIds(getMultiViewRoot(), viewerId);
    if (plotIdAry.join('') === currentPlotIdAry.join('')) return;
    if (!plotIdAry.includes(visRoot().activePlotId)) {
        dispatchChangeActivePlotView(plotIdAry[0]);
    }
    dispatchReplaceViewerItems(viewerId, plotIdAry, IMAGE);
}





function getSelectedPlotIds(){
    const {tableModel, selectInfo} = getTblInfoById(IMAGE_VIEW_TABLE_ID);

    return selectInfo.selectAll ?
        tableModel.tableData.data.map((row) => !selectInfo.exceptions.has(parseInt(row[ROW_IDX])) ? row[PID_IDX] : '')
        : Array.from(selectInfo.exceptions).map((idx) => tableModel.tableData.data[idx][2]);
}

export function isPlotFilterOrSortOutOfSync(viewerId) {
    if (isDialogVisible(EXPANDED_OPTIONS_DIALOG_ID)) return false;
    const sorted= isSorted(IMAGE_VIEW_TABLE_ID);
    const filtered= hasFilters(IMAGE_VIEW_TABLE_ID);
    if (!sorted && !filtered) return false;
    const itemIds= getViewerItemIds(getMultiViewRoot(),viewerId) ?? [];
    const tblPlotIds= getPlotIdAryFromTable(IMAGE_VIEW_TABLE_ID);
    return itemIds.join('') !== tblPlotIds.join('');
}

export function isOnlyPlotSortOutOfSync(viewerId) {
    if (isDialogVisible(EXPANDED_OPTIONS_DIALOG_ID)) return false;
    const filtered= hasFilters(IMAGE_VIEW_TABLE_ID);
    const sorted= isSorted(IMAGE_VIEW_TABLE_ID);
    if (!sorted || filtered) return false;
    const itemIds= getViewerItemIds(getMultiViewRoot(),viewerId) ?? [];
    const tblPlotIds= getPlotIdAryFromTable(IMAGE_VIEW_TABLE_ID);
    return itemIds.join('') !== tblPlotIds.join('');
}

export function syncSort(viewerId) {
    const plotViewAry = getPvAryInViewer(viewerId);
    const oldModel = getTblById(IMAGE_VIEW_TABLE_ID);
    const allIds = getCombinedItemIds(viewerId, viewerId + HIDDEN);
    makeImViewDisplayModel(IMAGE_VIEW_TABLE_ID, plotViewAry, allIds, oldModel);
    updateImageViewDisplay(IMAGE_VIEW_TABLE_ID, viewerId);
}

export function syncTableModelAndImagesToViewer(viewerId) {
    if (!isPlotFilterOrSortOutOfSync(viewerId)) return;
    const plotViewAry = getPvAryInViewer(viewerId);
    dispatchAddViewer(viewerId + HIDDEN, true, IMAGE, false);
    dispatchAddViewerItems(viewerId + HIDDEN, plotViewAry.map((pv) => pv.plotId), IMAGE);
    const oldModel = getTblById(IMAGE_VIEW_TABLE_ID);
    const allIds = getCombinedItemIds(viewerId, viewerId + HIDDEN);
    makeImViewDisplayModel(IMAGE_VIEW_TABLE_ID, plotViewAry, allIds, oldModel);
    updateImageViewDisplay(IMAGE_VIEW_TABLE_ID, viewerId);
}



let watcherAdded= false;

export function addExpandFilterSyncWatcher() {
    if (watcherAdded) return;
    watcherAdded= true;
    let lastState;
    dispatchAddActionWatcher({ actions:[SET_LAYOUT_MODE, SET_LAYOUT,UPDATE_LAYOUT, ImagePlotCntlr.CHANGE_EXPANDED_MODE],
        callback: () => {
            const expandedMode= getExpandedMode();
            const imageExpanded= isImageExpanded(visRoot().expandedMode);
            const state= (expandedMode===LO_VIEW.images && imageExpanded) ? 'EXPANDED' :
                (expandedMode===LO_VIEW.none && !imageExpanded) ? 'COLLAPSE' : 'IGNORE';
            if (state==='IGNORE' || state===lastState) return;
            syncTableModelAndImagesToViewer(state==='COLLAPSE' ? DEFAULT_FITS_VIEWER_ID : EXPANDED_MODE_RESERVED);
            lastState= state;
        }
        ,} );
}
