/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isEmpty, isNil} from 'lodash';
import {take} from 'redux-saga/effects';
import {SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo,
        dispatchUpdateLayoutInfo, dropDownHandler} from '../../core/LayoutCntlr.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT,
        dispatchTableRemove, dispatchTableHighlight} from '../../tables/TablesCntlr.js';
import {getCellValue, getTblById, getTblIdsByGroup, findIndex, getActiveTableId} from '../../tables/TableUtil.js';
import {updateSet, updateMerge, logError} from '../../util/WebUtil.js';
import ImagePlotCntlr, {dispatchPlotImage, visRoot, dispatchDeletePlotView,
        dispatchChangeActivePlotView,
        WcsMatchType, dispatchWcsMatch} from '../../visualize/ImagePlotCntlr.js';
import {getPlotViewById} from '../../visualize/PlotViewUtil.js';
import {getMultiViewRoot, dispatchReplaceViewerItems, getViewer} from '../../visualize/MultiViewCntlr.js';
import {CHANGE_VIEWER_LAYOUT} from '../../visualize/MultiViewCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {VALUE_CHANGE, dispatchValueChange, dispatchMultiValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {CHART_ADD, getChartDataElement} from '../../charts/ChartsCntlr.js';
import {getConverter} from './LcConverterFactory.js';

export const LC = {
    RAW_TABLE: 'raw_table',          // raw table id
    PHASE_FOLDED: 'phase_folded',    // phase folded table id
    PERIODOGRAM_TABLE: 'periodogram',// periodogram table id
    PEAK_TABLE: 'peaks_table',        // peak table id
    PERIOD_CNAME: 'Period',          // period column
    POWER_CNAME: 'Power',            // power column
    PEAK_CNAME: 'Peak',              // peak column
    PHASE_CNAME: 'phase',

    IMG_VIEWER_ID: 'lc_image_viewer',
    MAX_IMAGE_CNT: 7,

    META_TIME_CNAME: 'timeCName',
    META_FLUX_CNAME: 'fluxCName',
    META_ERR_CNAME: 'errorCName',

    RESULT_PAGE: 'result',          // result layout
    PERIOD_PAGE: 'period',          // period finding layout with periodogram button
    PERGRAM_PAGE: 'periodogram',    // period finding layout with peak and periodogram tables/charts

    FG_FILE_FINDER: 'LC_FILE_FINDER',
    FG_VIEWER_FINDER: 'LC_VIEWER_FINDER',          // mission and general entries group
    FG_PERIOD_FINDER: 'LC_PERIOD_FINDER',          // period finding group for the form
    FG_PERIODOGRAM_FINDER: 'LC_PERIODOGRAM_FINDER',// periodogram finding group
    FG_TIMESERIES_FINDER: 'LC_TIMESERIES_FINDER',  // result layout panel finding group
    PERIODOGRAM_GROUP: 'LC_PERIODOGRAM_TBL',       // table container, table group

    META_TIME_NAMES: 'timeNames',
    META_FLUX_NAMES: 'fluxNames',
    META_ERR_NAMES: 'errorNames',
    META_MISSION: MetaConst.DATASET_CONVERTER,

    MISSION_DATA: 'missionEntries',
    GENERAL_DATA:'generalEntries',

    TABLE_PAGESIZE: 100
};

const plotIdRoot= 'LC_FRAME-';


var defaultCutout = '0.2';
var defaultFlux = '';

function getFluxColumn(layoutInfo) {
    return get(layoutInfo, ['missionEntries', LC.META_FLUX_CNAME], defaultFlux);
}

function getCutoutSize(layoutInfo) {
    return get(layoutInfo, ['generalEntries', 'cutoutSize'], defaultCutout);
}

export function getConverterData(layoutInfo=getLayouInfo()) {
    const converterId = get(layoutInfo, [LC.MISSION_DATA, LC.META_MISSION]);
    return converterId && getConverter(converterId);
}

export function getViewerGroupKey(missionEntries) {
    return LC.FG_VIEWER_FINDER+get(missionEntries, LC.META_MISSION, '');
}

export function getFullRawTable() {
    return get(getLayouInfo(), 'fullRawTable', {});
}

function getGeneralEntries() {
    return {cutoutSize: defaultCutout};
}

export function updateLayoutDisplay(displayMode, periodState) {
    var updateObj = periodState ? {displayMode, periodState} : {displayMode};
    var newLayoutInfo = Object.assign({}, getLayouInfo(), updateObj);

    dispatchUpdateLayoutInfo(newLayoutInfo);
}

export function removeTablesFromGroup(tbl_group_id = 'main') {
    const tblAry = getTblIdsByGroup(tbl_group_id);

    tblAry&&tblAry.forEach((tbl_id) => {
        dispatchTableRemove(tbl_id);
    });
}

export var getValidValueFrom = (fields, valKey) => {
    var val =  get(fields, [valKey, 'valid']) && get(fields, [valKey, 'value']);

    return val ? val : '';
};

/**
 * This event manager is custom made for light curve viewer.
 * @param {LCMissionConverter} params
 */
export function* lcManager(params={}) {

    while (true) {
        const action = yield take([
            TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT, SHOW_DROPDOWN, SET_LAYOUT_MODE,
            CHANGE_VIEWER_LAYOUT, VALUE_CHANGE, ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW, CHART_ADD
        ]);

        /**
         * This is the current state of the layout store.  Action handlers should return newLayoutInfo if state changes
         * If state has changed, it will be dispatched into the flux.
         * @type {LayoutInfo}
         * @prop {boolean}  layoutInfo.showForm    show form panel
         * @prop {boolean}  layoutInfo.showTables  show tables panel
         * @prop {boolean}  layoutInfo.showXyPlots show charts panel
         * @prop {boolean}  layoutInfo.showImages  show images panel
         * @prop {string}   layoutInfo.searchDesc  optional string describing search criteria used to generate this result.
         * @prop {Object}   layoutInfo.images      images specific states
         * @prop {string}   layoutInfo.images.activeTableId  last active table id that images responded to
         * @prop {string}   layoutInfo.displayMode:'result' (result page), 'period' (period finding page), 'periodogram' or neither
         * @prop {Object}   layoutInfo.missionEntries mission specific entries on result layout panel
         * @prop {Object}   layoutInfo.generalEntries general entries for result layout panel
         * @prop {string}   layoutInfo.periodState  // period or periodogram
         * @prop {Object}   layoutInfo.fullRawTable
         */
        var layoutInfo = getLayouInfo();
        var newLayoutInfo = layoutInfo;

        newLayoutInfo = dropDownHandler(newLayoutInfo, action);
        switch (action.type) {
            case TBL_RESULTS_ADDED:
            case TABLE_LOADED:
                newLayoutInfo = handleTableLoad(newLayoutInfo, action);
                break;
            case TABLE_HIGHLIGHT:
                newLayoutInfo = handleTableHighlight(newLayoutInfo, action);
                break;
            case CHANGE_VIEWER_LAYOUT:
                newLayoutInfo = handleChangeMultiViewLayout(newLayoutInfo);
                break;
            case TBL_RESULTS_ACTIVE :
                newLayoutInfo = handleTableActive(newLayoutInfo, action);
                break;
            case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
                newLayoutInfo = handlePlotActive(newLayoutInfo, action.payload.plotId);
                break;
            case CHART_ADD:
                 newLayoutInfo = handleAddChart(newLayoutInfo, action.payload.chartId);
                break;
            case VALUE_CHANGE:
                newLayoutInfo = handleValueChange(newLayoutInfo, action);
                break;
            default:
                break;
        }

        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);
        }
    }
}

function updateRawTableChart(timeCName, fluxCName) {
    var chartX = get(getChartDataElement(LC.RAW_TABLE), ['options', 'x', 'columnOrExpr']);
    var chartY = get(getChartDataElement(LC.RAW_TABLE), ['options', 'y', 'columnOrExpr']);

    if (chartX === timeCName && chartY === fluxCName) return;

    if (timeCName && fluxCName) {
        const xyPlotParams = {x: {columnOrExpr: timeCName}, y: {columnOrExpr: fluxCName, options: 'grid,flip'}};
        loadXYPlot({chartId: LC.RAW_TABLE, tblId: LC.RAW_TABLE, xyPlotParams, help_id: 'main1TSV.plot'});
    }
}

function updatePhaseTableChart(flux) {
    var chartY = get(getChartDataElement(LC.PHASE_FOLDED), ['options', 'y', 'columnOrExpr']);

    if (chartY === flux) return;

    if (flux) {
        const xyPlotParams = {
            userSetBoundaries: {xMax: 2},
            x: {columnOrExpr: LC.PHASE_CNAME, options: 'grid'},
            y: {columnOrExpr: flux, options: 'grid,flip'}
        };
        loadXYPlot({chartId: LC.PHASE_FOLDED, tblId: LC.PHASE_FOLDED, xyPlotParams, help_id: 'main1TSV.plot'});
    }
}

/**
 * @summary handle value change on time, flux, cutout size
 * @param {object} layoutInfo
 * @param {object} action
 * @returns {*}
 */
function handleValueChange(layoutInfo, action) {
    var {fieldKey, value, valid} = action.payload;

    if (!valid) { return; }

    if (fieldKey === 'cutoutSize') { // cutoutsize changes
        if ((get(layoutInfo, [LC.GENERAL_DATA, fieldKey]) !== value) && (value > 0.0) ) {
            if (get(layoutInfo, ['displayMode']) === LC.RESULT_PAGE) {
                layoutInfo = updateSet(layoutInfo, [LC.GENERAL_DATA, fieldKey], value);
                setupImages(layoutInfo);
            }
        }
    } else {
        const converterData = getConverterData(layoutInfo);
        if (!converterData) {return;}

        const updates = converterData.onFieldUpdate(fieldKey, value);
        if (isEmpty(updates)) {
            return layoutInfo;
        }

        const didChange = (el) => Object.keys(updates).includes(el) && updates[el] !== get(layoutInfo, [LC.MISSION_DATA, fieldKey]);

        const newLayoutInfo = updateMerge(layoutInfo, LC.MISSION_DATA, updates);

        if ([LC.META_TIME_CNAME, LC.META_FLUX_CNAME].some(didChange)) {
            const timeCol = get(newLayoutInfo, [LC.MISSION_DATA, LC.META_TIME_CNAME]);
            const fluxCol = get(newLayoutInfo, [LC.MISSION_DATA, LC.META_FLUX_CNAME]);
            const activeTbl = getActiveTableId();
            if (activeTbl === LC.RAW_TABLE) {
                updateRawTableChart(timeCol, fluxCol);
            } else if (activeTbl === LC.PHASE_FOLDED) {
                updatePhaseTableChart(fluxCol);
            }

            setupImages(newLayoutInfo);

            // update time or flux for period panel field group if it exists
            if (FieldGroupUtils.getGroupFields(LC.FG_PERIOD_FINDER)) {
                dispatchMultiValueChange(LC.FG_PERIOD_FINDER,
                [{fieldKey: 'time', timeCol}, {fieldKey: 'flux', fluxCol}]);
            }
        }
        layoutInfo = newLayoutInfo;
    }
    return layoutInfo;
}

function handleAddChart(layoutInfo, chartId) {
    if (chartId === LC.PHASE_FOLDED) {
        if (get(layoutInfo, ['displaMode']) !== 'result') {
            layoutInfo = updateSet(layoutInfo, ['displayMode'], 'result');
        }
    }
    return layoutInfo;
}

/**
 * @summary highlight the table row & chart after change the active plot view
 * @param layoutInfo
 * @param plotId
 * @returns {*}
 */
function handlePlotActive(layoutInfo, plotId) {
    const tableId = get(layoutInfo, ['images', 'activeTableId']);

    var rowNum= Number(plotId.substring(plotIdRoot.length));
    dispatchTableHighlight(tableId, rowNum);
    return layoutInfo;
}
/*
 * field group, LC_VIEWER_FINDER, includes fieldKey from missionEntries & generalEntries
 * missionEntries & generalEntries is mission specific, more detail to be fixed after raw table metadata is defined. -- TO DO
 */
function handleRawTableLoad(layoutInfo, tblId) {
    const rawTable = getTblById(tblId);

    const metaInfo = rawTable && rawTable.tableMeta;
    const converterId = get(metaInfo, LC.META_MISSION);
    const converterData = converterId && getConverter(converterId);
    if (!converterData) {
        logError('Unknown mission or no converter');
        return;
    }

    const generalEntries = get(layoutInfo, LC.GENERAL_DATA, getGeneralEntries());
    const missionEntries = converterData.onNewRawTable(rawTable, converterData, generalEntries);

    defaultFlux = get(missionEntries, LC.META_FLUX_CNAME);

    return Object.assign(layoutInfo, {missionEntries, generalEntries});
}

/**
 * @summary handle on loaded table, update period and periodogram field groups, update chart
 * @param layoutInfo
 * @param action
 * @returns {*}
 */
function handleTableLoad(layoutInfo, action) {
    const {tbl_id} = action.payload;

    layoutInfo = Object.assign({}, layoutInfo, {showTables: true, showXyPlots: true});
    if (tbl_id === LC.RAW_TABLE) {         // a new raw table is loaded
        if (action.type === TABLE_LOADED) {
            layoutInfo = Object.assign(layoutInfo, {displayMode: LC.RESULT_PAGE, periodState: LC.PERIOD_PAGE});
            layoutInfo = handleRawTableLoad(layoutInfo, tbl_id);
        }
    }
    if (isImageEnabledTable(tbl_id)) {
        layoutInfo = updateSet(layoutInfo, 'showImages', true);
        //layoutInfo = updateSet(layoutInfo, 'images.activeTableId', tbl_id);
        //setupImages(tbl_id);
    }
    dispatchUpdateLayoutInfo(layoutInfo);
    layoutInfo = handleTableActive(layoutInfo, action);

    return layoutInfo;
}

/**
 * @summary handle on changing the active table, update image and chart
 * @param layoutInfo
 * @param action
 * @returns {*}
 */
function handleTableActive(layoutInfo, action) {
    const {tbl_id} = action.payload;
    if (isImageEnabledTable(tbl_id)) {
        layoutInfo = updateSet(layoutInfo, 'images.activeTableId', tbl_id);
        setupImages(layoutInfo);
    }

    if (tbl_id === LC.PERIODOGRAM_TABLE || tbl_id === LC.PEAK_TABLE) {
        const per = getPeriodFromTable(tbl_id);
        if (per) {
            dispatchValueChange({
                fieldKey: (LC.PERIOD_CNAME).toLowerCase(),
                groupKey: LC.FG_PERIOD_FINDER,
                value: `${parseFloat(per)}`
            });
        }
    } else {
        const fluxCol = get(layoutInfo, [LC.MISSION_DATA, LC.META_FLUX_CNAME]);

        if (tbl_id === LC.PHASE_FOLDED) {
            updatePhaseTableChart(fluxCol);
        } else if (tbl_id === LC.RAW_TABLE) {
            const timeCol = get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_CNAME]);
            updateRawTableChart(timeCol, fluxCol);
        }
    }

    return layoutInfo;
    //return Object.assign({}, layoutInfo);
}

function handleTableHighlight(layoutInfo, action) {
    const {tbl_id, highlightedRow} = action.payload;
    const {displayMode} = layoutInfo;
    const activeTableContainer = displayMode && displayMode.startsWith('period') ? LC.PERIODOGRAM_GROUP : undefined;

    // only respond to active table highlight
    if (tbl_id !== getActiveTableId(activeTableContainer)) return layoutInfo;

    if (isImageEnabledTable(tbl_id)) {
        setupImages(layoutInfo);
    }

    // update period field when it's selected from a table with period.
    if (tbl_id === LC.PERIODOGRAM_TABLE || tbl_id === LC.PEAK_TABLE) {
        const per = getPeriodFromTable(tbl_id);
        if (per) {
            dispatchValueChange({
                fieldKey: (LC.PERIOD_CNAME).toLowerCase(),
                groupKey: LC.FG_PERIOD_FINDER,
                value: `${parseFloat(per)}`
            });
        }
    }

    // ensure the highlighted row of the raw and phase-folded tables are in sync.
    if ([LC.PHASE_FOLDED, LC.RAW_TABLE].includes(tbl_id)) {
        let filterInfo, actOn, rowid;
        const tableModel = getTblById(tbl_id);
        if (tbl_id === LC.RAW_TABLE) {
            actOn = LC.PHASE_FOLDED;
            rowid = getCellValue(tableModel, highlightedRow, 'ROWID');
            filterInfo = `RAW_ROWID = ${rowid}`;
        } else {
            rowid = getCellValue(tableModel, highlightedRow, 'RAW_ROWID');
            actOn = LC.RAW_TABLE;
            filterInfo = `ROWID = ${rowid}`;
        }
        findIndex(actOn, filterInfo)
            .then( (index) => {
                if (index >=0) {
                    dispatchTableHighlight(actOn, index);
                }
            });
    }

    return layoutInfo;
    //return Object.assign({}, layoutInfo);
}

/**
 * @summary gets the period from either peak or periodogram table
 * @param {string} tbl_id
 * @returns {string} period value
 */
function getPeriodFromTable(tbl_id) {

    if (tbl_id !== LC.PERIODOGRAM_TABLE && tbl_id !== LC.PEAK_TABLE) {
        return '';
    }

    const tableModel = getTblById(tbl_id);

    if (!tableModel || isNil(tableModel.highlightedRow)) {
        return '';
    }

    return getCellValue(tableModel, tableModel.highlightedRow, LC.PERIOD_CNAME);
}


function isImageEnabledTable(tbl_id) {
    return [LC.PHASE_FOLDED, LC.RAW_TABLE].includes(tbl_id);
}

function handleChangeMultiViewLayout(layoutInfo) {
    setupImages(layoutInfo);
    return layoutInfo;
}

export function setupImages(layoutInfo) {
    try {
        const activeTableId = get(layoutInfo, 'images.activeTableId');

        const tableModel = getTblById(activeTableId);
        if (!tableModel || isNil(tableModel.highlightedRow) || get(tableModel, 'totalRows',0) < 1) return;

        const converterId = get(layoutInfo, [LC.MISSION_DATA,LC.META_MISSION]);
        const converterData = converterId && getConverter(converterId);
        if (!converterId || !converterData) {return;}

        const viewer=  getViewer(getMultiViewRoot(),LC.IMG_VIEWER_ID);
        const count= get(viewer, 'layoutDetail.count', converterData.defaultImageCount);

        var vr= visRoot();
        const hasPlots= vr.plotViewAry.length>0;
        const newPlotIdAry= makePlotIds(tableModel.highlightedRow, tableModel.totalRows,count);
        const maxPlotIdAry= makePlotIds(tableModel.highlightedRow, tableModel.totalRows,LC.MAX_IMAGE_CNT);


        const cutoutSize = getCutoutSize(layoutInfo);
        const fluxCol = getFluxColumn(layoutInfo);
        newPlotIdAry.forEach( (plotId) => {
            var pv = getPlotViewById(vr,plotId);
            const rowNum= Number(plotId.substring(plotIdRoot.length));
            const webPlotReq = converterData.webplotRequestCreator(tableModel,rowNum, cutoutSize, {fluxCol});
            if (!pv || get(pv, ['request', 'params', 'Title']) !== webPlotReq.getTitle())  {
                dispatchPlotImage({
                    plotId, wpRequest: webPlotReq,
                    setNewPlotAsActive: false,
                    holdWcsMatch: true,
                    pvOptions: {userCanDeletePlots: false}
                });
            }
        });


        dispatchReplaceViewerItems(LC.IMG_VIEWER_ID, newPlotIdAry);
        const newActivePlotId= plotIdRoot+tableModel.highlightedRow;
        dispatchChangeActivePlotView(newActivePlotId);


        vr= visRoot();
        if (!vr.wcsMatchType && !hasPlots) {
            dispatchWcsMatch({matchType:WcsMatchType.Target, plotId:newActivePlotId});
        }

        vr= visRoot();

        vr.plotViewAry
            .filter( (pv) => pv.plotId.startsWith(plotIdRoot))
            .filter( (pv) => pv.plotId!==vr.mpwWcsPrimId)
            .filter( (pv) => !maxPlotIdAry.includes(pv.plotId))
            .forEach( (pv) => dispatchDeletePlotView({plotId:pv.plotId, holdWcsMatch:true}));
    } catch (E){
        console.log(E.toString());
    }
}


function makePlotIds(highlightedRow, totalRows, totalPlots)  {
    const plotIds= [];
    const beforeCnt= totalPlots%2===0 ? totalPlots/2-1 : (totalPlots-1)/2;
    const afterCnt= totalPlots%2===0 ? totalPlots/2    : (totalPlots-1)/2;
    var j=0;
    var endRow= Math.min(totalRows-1, highlightedRow+afterCnt);
    var startRow= Math.max(0,highlightedRow-beforeCnt);
    if (startRow===0) endRow= Math.min(totalRows-1, totalPlots-1);
    if (endRow===totalRows-1) startRow= Math.max(0, totalRows-totalPlots);

    for(var i= startRow; i<=endRow; i++) plotIds[j++]= plotIdRoot+i;
    return plotIds;
}
