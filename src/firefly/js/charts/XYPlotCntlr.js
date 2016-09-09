/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * Added by LZ when generate doc, it is for testing only.  When module is used, it is list under module
 *
 */
import {flux} from '../Firefly.js';

import {updateSet, updateMerge, updateDelete} from '../util/WebUtil.js';
import {cloneDeep, get, has, omit, omitBy, isEmpty, isString, isUndefined} from 'lodash';

import {doFetchTable, getColumn, getTblById, isFullyLoaded, cloneRequest} from '../tables/TableUtil.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {DELETE} from './ChartsCntlr.js';
import {serializeDecimateInfo} from '../tables/Decimate.js';
import {logError} from '../util/WebUtil.js';
import {getDefaultXYPlotParams, SCATTER, getChartSpace} from './ChartUtil.js';

export const XYPLOT_DATA_KEY = 'charts.xyplot';
export const LOAD_PLOT_DATA = `${XYPLOT_DATA_KEY}/LOAD_COL_DATA`;
export const UPDATE_PLOT_DATA = `${XYPLOT_DATA_KEY}/UPDATE_COL_DATA`;
export const SET_SELECTION = `${XYPLOT_DATA_KEY}/SET_SELECTION`;
const SET_ZOOM = `${XYPLOT_DATA_KEY}/SET_ZOOM`;
const RESET_ZOOM = `${XYPLOT_DATA_KEY}/RESET_ZOOM`;
/**
 * @public
 */

/*
 Possible structure of store:
  /xyplot
    chartId: Object - the name of this node matches chart id
    {
         // tblXYPlotData
         tblId: string // table id
         tblSource: string // source of the table
         isPlotDataReady: boolean
         decimatedUnzoomed: boolean // tells that unzoomed data are decimated
         xyPlotData: {
                    rows: [[x: string, y: string, rowIdx: string]*] ,
                    decimateKey: string,
                    xMin: string,
                    xMax: string,
                    yMin: string,
                    yMax: string,
                    weightMin: string,
                    weightMax: string,
                    idStr: string
         }
         xyPlotParams: {
           title: string
           xyRatio: number
           nbins {x,y}
           shading: string (lin|log)
           selection: {xMin, xMax, yMin, yMax} // currently selected rectangle
           zoom: {xMin, xMax, yMin, yMax} // currently zoomed rectangle
           stretch: string (fit|fill)
           x: {
                columnOrExpr
                label
                unit
                options: [grid,log,flip]
              }
           y: {
                columnOrExpr
                label
                unit
                options: [grid,log,flip]
           }
     }
 */


/**
 * @desc Load xy plot data Load xy plot data
 *
 * @public
 * @function dispatchLoadPlotData
 * @memberof firefly.action
 * @param {Object} params - dispatch parameters
 * @param {string} params.chartId - if no chart id is specified table id is used as chart id
 * @param {Object} params.xyPlotParams - XY plot options (column names, etc.)
 * @param {boolean} params.markAsDefault - are the options considered to be "the default" to reset to
 * @param {string} params.tblId - table id
 * @param {function} params.dispatcher only for special dispatching uses such as remote
 */
export function dispatchLoadPlotData(params) {
    const {chartId, xyPlotParams, markAsDefault=false, tblId, dispatcher=flux.process} = params;
    dispatcher({type: LOAD_PLOT_DATA, payload: {chartId: (chartId||tblId), xyPlotParams, markAsDefault, tblId}});
}

/**
 * Set selection to give user choice of actions on selection (zoom, filter, or select points)
 * @param {String} chartId - chart id
 * @param {Object} selection - {xMin, xMax, yMin, yMax}
 * @public
 * @func dispatchSetSelection
 * @memberof firefly.action
 */
export function dispatchSetSelection(chartId, selection) {
    flux.process({type: SET_SELECTION, payload: {chartId, selection}});
}

/**
 * Zoom XY plot to a given selection or reset zoom if no selection is given
 * @param {String} chartId - chart id
 * @param {String} tblId - table id
 * @param {Object} selection - {xMin, xMax, yMin, yMax}
 * @public
 * @func dispatchZoom
 * @memberof firefly.action
 */
export function dispatchZoom(chartId, tblId, selection) {
    const {xyPlotData, xyPlotParams, decimatedUnzoomed} = get(getChartSpace(SCATTER), chartId, {});
    if (xyPlotData && xyPlotParams) {
        if (selection) {
            // zoom to selection
            if (xyPlotData.decimateKey) {
                const tableModel = getTblById(tblId);
                if (tableModel) {
                    const paramsWithZoom = Object.assign({}, xyPlotParams, {zoom: xyPlotParams.selection});
                    dispatchLoadPlotData({chartId, xyPlotParams: paramsWithZoom, tblId});
                }
            } else {
                dispatchSetZoom(chartId, selection);
            }
        } else {
            // reset zoom
            if (decimatedUnzoomed || isUndefined(decimatedUnzoomed)) {
                const tableModel = getTblById(tblId);
                if (tableModel) {
                    const paramsWithoutZoom = omit(xyPlotParams, 'zoom');
                    dispatchLoadPlotData({chartId, xyPlotParams: paramsWithoutZoom, tblId});
                }
            } else {
                dispatchResetZoom(chartId);
            }

        }
    }
}

function dispatchSetZoom(chartId, selection) {
    flux.process({type: SET_ZOOM, payload: {chartId, selection}});
}

function dispatchResetZoom(chartId) {
    flux.process({type: RESET_ZOOM, payload: {chartId}});
}


/**
 * @param rawAction (its payload should contain searchRequest to get source table and histogram parameters)
 * @returns function which loads plot data (x, y, rowIdx, etc.)
 * @public
 * @func loadPlotData
 * @memberof firefly.action
 */
export function loadPlotData (rawAction) {
    return (dispatch) => {
        let xyPlotParams = rawAction.payload.xyPlotParams;
        const {chartId, tblId, markAsDefault} = rawAction.payload;
        const tblSource = get(getTblById(tblId), 'tableMeta.tblFilePath');

        const chartModel = get(getChartSpace(SCATTER), chartId);
        let serverCallNeeded = !chartModel || !chartModel.tblSource || chartModel.tblSource !== tblSource;

        if (serverCallNeeded || chartModel.xyPlotParams !== xyPlotParams) {
            // when server call parameters do not change but chart parameters change,
            // we do need to update parameters, but we can reuse the old chart data
            serverCallNeeded = serverCallNeeded || serverParamsChanged(chartModel.xyPlotParams, xyPlotParams);

            if (!serverCallNeeded) {
                const tableModel = getTblById(tblId);
                const dataBoundaries = getDataBoundaries(chartModel.xyPlotData);
                xyPlotParams = getUpdatedParams(xyPlotParams, tableModel, dataBoundaries);
            }

            dispatch({ type : LOAD_PLOT_DATA, payload : {chartId, tblId, xyPlotParams, markAsDefault, tblSource, serverCallNeeded}});

            if (serverCallNeeded) {
                fetchPlotData(dispatch, tblId, xyPlotParams, chartId);
            }
        }
    };
}

function serverParamsChanged(oldParams, newParams) {
    if (oldParams === newParams) { return false; }
    if (!oldParams || !newParams) { return true; }

    const newServerParams = getServerCallParameters(newParams);
    const oldServerParams = getServerCallParameters(oldParams);
    return newServerParams.some((p, i) => {
        return p !== oldServerParams[i];
    });
}

/**
 * The data is an object with
 * chartId - string, chart id,
 * isPlotDataReady - boolean, flags that xy plot data are available
 * xyPlotData - an array of data rows
 * xyPlotParams - plot parameters
 * decimatedUnzoomed - tells if unzoomed data are decimated
 * @param data {Object} the data to merge with the xyplot branch
 * @return {{type: string, payload: object}}
 * @memberof firefly.action
 * @public
 * @func updatePlotData
 */
function updatePlotData(data) {
    return { type : UPDATE_PLOT_DATA, payload: data };
}

export function reduceXYPlot(state={}, action={}) {
    switch (action.type) {
        case (TablesCntlr.TABLE_LOADED)  :
        {
            const {tbl_id, invokedBy} = action.payload;
            let updatedState = state;
            if (invokedBy !== TablesCntlr.TABLE_SORT) {
                Object.keys(state).forEach((cid) => {
                    if (state[cid].tblId === tbl_id && get(state, [cid, 'xyPlotParams', 'boundaries'])) {
                        // do we need hard boundaries, which do not go away on new table data?
                        updatedState = updateDelete(updatedState, [cid, 'xyPlotParams'], 'boundaries');
                    }
                });
            }
            return updatedState;
        }
        case (TablesCntlr.TABLE_REMOVE)  :
        {
            const tbl_id = action.payload.tbl_id;
            const chartsToDelete = [];
            Object.keys(state).forEach((cid) => {
                if (state[cid].tblId === tbl_id) {
                    chartsToDelete.push(cid);
                }
            });
            return (chartsToDelete.length > 0) ?
                omit(state, chartsToDelete) : state;
        }
        case (DELETE) :
        {
            const {chartId, chartType} = action.payload;
            if (chartType === 'scatter' && has(state, chartId)) {
                return omit(state, [chartId]);
            }
            return state;
        }
        case (LOAD_PLOT_DATA)  :
        {
            const {chartId, xyPlotParams, markAsDefault, tblId, tblSource, serverCallNeeded} = action.payload;
            if (serverCallNeeded) {
                const defaultParams = markAsDefault ? cloneDeep(xyPlotParams) : get(state, [chartId, 'defaultParams']);
                return updateSet(state, chartId,
                    {
                        tblId,
                        isPlotDataReady: false,
                        tblSource,
                        xyPlotParams,
                        defaultParams,
                        decimatedUnzoomed: get(state, [chartId, 'decimatedUnzoomed'])
                    });
            } else {
                // only plot parameters changed
                return updateSet(state, [chartId, 'xyPlotParams'], xyPlotParams);
            }
        }
        case (UPDATE_PLOT_DATA)  :
        {
            const {chartId, isPlotDataReady, tblSource, decimatedUnzoomed, xyPlotParams, xyPlotData, newParams} = action.payload;
            if (!state[chartId].xyPlotParams || state[chartId].xyPlotParams === xyPlotParams) {
                const decimatedUnzoomedNext = isUndefined(decimatedUnzoomed) ? state[chartId].decimatedUnzoomed : decimatedUnzoomed;
                return updateMerge(state, chartId,
                    {isPlotDataReady, tblSource, decimatedUnzoomed: decimatedUnzoomedNext, xyPlotData, xyPlotParams: newParams});
            }
            return state;
        }
        case (SET_SELECTION) :
        {
            const {chartId, selection} = action.payload;
            return updateSet(state, [chartId,'xyPlotParams','selection'], selection);
        }
        case (SET_ZOOM) :
        {
            const {chartId, selection} = action.payload;
            const newState = updateSet(state, [chartId,'xyPlotParams','zoom'], selection);
            Reflect.deleteProperty(newState[chartId].xyPlotParams, 'selection');
            return newState;
        }
        case (RESET_ZOOM) :
        {
            const chartId = action.payload.chartId;
            const newParams = omit(state[chartId].xyPlotParams, ['selection', 'zoom']);
            return updateSet(state, [chartId,'xyPlotParams'], newParams);
        }
        case (TablesCntlr.TABLE_SELECT) :
        {
            const tbl_id = action.payload.tbl_id; //also has selectInfo
            let newState = state;
            Object.keys(state).forEach((cid) => {
                if (state[cid].tblId === tbl_id || has(state[cid], ['xyPlotParams','selection'])) {
                    newState = updateSet(newState, [cid,'xyPlotParams','selection'], undefined);
                }
            });
            return newState;
        }
        default:
            return state;
    }
}

function getServerCallParameters(xyPlotParams) {
    if (!xyPlotParams) { return []; }

    if (xyPlotParams.zoom) {
        var {xMin, xMax, yMin, yMax}  = xyPlotParams.zoom;
    }

    let maxBins = 10000;
    let xyRatio = xyPlotParams.xyRatio || 1.0;
    if (xyPlotParams.nbins) {
        const {x, y} = xyPlotParams.nbins;
        maxBins = x*y;
        xyRatio = x/y;
    }
    // order should match the order of the parameters in serializeDecimateInfo
    return [xyPlotParams.x.columnOrExpr, xyPlotParams.y.columnOrExpr, maxBins, xyRatio, xMin, xMax, yMin, yMax];
}

function getDataBoundaries(xyPlotData) {
    if (!isEmpty(xyPlotData)) {
        return omitBy({
            xMin: xyPlotData.xMin,
            xMax: xyPlotData.xMax,
            yMin: xyPlotData.yMin,
            yMax: xyPlotData.yMax
        }, isUndefined);
    }
}

function getPaddedRange(min, max, isLog, factor) {
    const range = max - min;
    let paddedMin = min;
    let paddedMax = max;

    if (range > 0) {
        if (isLog) {
            const minLog = Math.log10(min);
            const maxLog = Math.log10(max);
            const padLog = (maxLog - minLog) / factor;
            paddedMin = Math.pow(10, (minLog-padLog));
            paddedMax = Math.pow(10, (maxLog+padLog));
        } else {
            const pad = range / factor;
            paddedMin = min - pad;
            paddedMax = max + pad;
        }
    }
    return {paddedMin, paddedMax};
}

/**
 * Pad and round data boundaries
 * @param {Object} xyPlotParams - object with XY plot params
 * @param {Object} boundaries - object with xMin, xMax, yMin, yMax props
 * @param {Number} factor - part of the range to add on both sides
 * @ignore
 */
function getPaddedBoundaries(xyPlotParams, boundaries, factor=100) {
    if (!isEmpty(boundaries)) {
        let {xMin, xMax, yMin, yMax} = boundaries;

        const xRange = xMax - xMin;
        if (xRange > 0) {
            const xOptions = get(xyPlotParams, 'x.options');
            const xLog = xOptions && xOptions.includes('log') && xMin>0;
            ({paddedMin:xMin, paddedMax:xMax} = getPaddedRange(xMin, xMax, xLog, factor));
        }
        const yRange = yMax - yMin;
        if (yRange > 0) {
            const yOptions = get(xyPlotParams, 'y.options');
            const yLog = yOptions && yOptions.includes('log') && yMin>0;
            ({paddedMin:yMin, paddedMax:yMax} = getPaddedRange(yMin, yMax, yLog, factor));
        }
        if (xRange > 0 || yRange > 0) {
            return {xMin, xMax, yMin, yMax};
        }
    }
    return boundaries;
}

/**
 * fetches xy plot data
 * set isColStatsReady to true once done.
 * @param dispatch
 * @param tblId table search request to obtain source table
 * @param xyPlotParams object, which contains xy plot parameters
 * @param {string} chartId  - chart id
 * @ignore
 */
function fetchPlotData(dispatch, tblId, xyPlotParams, chartId) {

    if (!tblId || !isFullyLoaded(tblId)) {return; }

    const activeTableModel = getTblById(tblId);
    const activeTableServerRequest = activeTableModel['request'];
    const tblSource = get(activeTableModel, 'tableMeta.tblFilePath');

    if (!xyPlotParams) { xyPlotParams = getDefaultXYPlotParams(tblId); }


    const req = cloneRequest(activeTableServerRequest, {
            'startIdx' : 0,
            'pageSize' : 1000000,
            //'inclCols' : `${xyPlotParams.x.columnOrExpr},${xyPlotParams.y.columnOrExpr}`, // ignored if 'decimate' is present
            'decimate' : serializeDecimateInfo(...getServerCallParameters(xyPlotParams))
        });
    req.tbl_id = `xy-${chartId}`;

    doFetchTable(req).then(
        (tableModel) => {
            let xyPlotData = {rows: []};
            // when zoomed, we don't know if the unzoomed data are decimated or not
            let decimatedUnzoomed = xyPlotParams.zoom ? undefined : false;

            if (tableModel.tableData && tableModel.tableData.data) {
                const {tableMeta} = tableModel;
                xyPlotData = omitBy({
                    rows: tableModel.tableData.data,
                    decimateKey: tableMeta['decimate_key'],
                    xMin: tableMeta['decimate.X-MIN'],
                    xMax: tableMeta['decimate.X-MAX'],
                    yMin: tableMeta['decimate.Y-MIN'],
                    yMax: tableMeta['decimate.Y-MAX'],
                    weightMin: tableMeta['decimate.WEIGHT-MIN'],
                    weightMax: tableMeta['decimate.WEIGHT-MAX'],
                    idStr: tableMeta['tbl_id']
                }, isUndefined);

                // convert strings with numbers into numbers
                Object.keys(xyPlotData).forEach( (prop) => {
                    const val = xyPlotData[prop];
                    if (isString(val) && isFinite(val)) { xyPlotData[prop] = Number(val); }
                });

                decimatedUnzoomed = Boolean(tableMeta['decimate_key']) || decimatedUnzoomed;
            }
            dispatch(updatePlotData(
                {
                    isPlotDataReady : true,
                    tblSource,
                    decimatedUnzoomed,
                    xyPlotParams,
                    xyPlotData,
                    chartId,
                    newParams: getUpdatedParams(xyPlotParams, tableModel, getDataBoundaries(xyPlotData))
                }));
        }
    ).catch(
        (reason) => {
            logError(`Failed to fetch XY plot data: ${reason}`);
        }
    );

}

/*
 * Label and unit must be specified to display plot,
 * derive them from existing parameters or tableModel.
 * No selection should be present in updated parameters
 */
function getUpdatedParams(xyPlotParams, tableModel, dataBoundaries) {
    let newParams = xyPlotParams;

    if (!get(xyPlotParams, 'x.label')) {
        newParams = updateSet(newParams, 'x.label', get(xyPlotParams, 'x.columnOrExpr'));
    }
    if (!get(xyPlotParams, 'x.unit')) {
        const xColumn = getColumn(tableModel, get(xyPlotParams, 'x.columnOrExpr'));
        const xUnit = get(xColumn, 'units', '');
        newParams = updateSet(newParams, 'x.unit', xUnit);
    }
    if (!get(xyPlotParams, 'y.label')) {
        newParams = updateSet(newParams, 'y.label', get(xyPlotParams, 'y.columnOrExpr'));
    }
    if (!get(xyPlotParams, 'y.unit')) {
        const yColumn = getColumn(tableModel, get(xyPlotParams, 'y.columnOrExpr'));
        const yUnit = get(yColumn, 'units', '');
        newParams = updateSet(newParams, 'y.unit', yUnit);
    }
    if (get(xyPlotParams, 'selection')) {
        newParams = updateSet(newParams, 'selection', undefined);
    }

    // set plot boundaries,
    // if user set boundaries are undefined, use data boundaries
    const userSetBoundaries = get(xyPlotParams, 'userSetBoundaries', {});
    const boundaries = Object.assign({}, userSetBoundaries);
    if (Object.keys(boundaries).length < 4 && !isEmpty(dataBoundaries)) {
        const paddedDataBoundaries = getPaddedBoundaries(xyPlotParams, dataBoundaries);
        const [xMin, xMax, yMin, yMax] = ['xMin', 'xMax', 'yMin', 'yMax'].map( (v) => {
            return  (Number.isFinite(boundaries[v]) ? boundaries[v] : paddedDataBoundaries[v]);
        });
        const newBoundaries = omitBy({xMin, xMax, yMin, yMax}, isUndefined);
        if (!isEmpty(newBoundaries)) {
            newParams = updateSet(newParams, 'boundaries', newBoundaries);
        }
    }

    return newParams;
}


