/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../../Firefly.js';

import {updateSet, logError} from '../../util/WebUtil.js';
import {get, omitBy, isEmpty, isString, isUndefined} from 'lodash';

import {MetaConst} from '../../data/MetaConst.js';
import {fetchTable} from '../../rpc/SearchServicesJson.js';
import {getColumn, getTblById, isFullyLoaded, cloneRequest, makeTblRequest} from '../../tables/TableUtil.js';

import {getChartDataElement, dispatchChartAdd, dispatchChartOptionsUpdate, chartDataUpdate} from './../ChartsCntlr.js';
import {colWithName, getNumericCols, SCATTER} from './../ChartUtil.js';
import {serializeDecimateInfo} from '../../tables/Decimate.js';

export const DT_XYCOLS = 'xycols';

/**
 * Chart data type for XY columns
 * @constant
 * @type {ChartDataType}
 */
export const DATATYPE_XYCOLS = {
        id: DT_XYCOLS,
        fetchData: fetchPlotData,
        fetchParamsChanged: serverParamsChanged,
        getUpdatedOptions: getUpdatedParams
};

/*
 Possible structure of store with xy data:
 /data
   chartId: Object - the name of this node matches chart id
   {
      chartDataElements: [
        tblId
        isDataReady
        data: {
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
        meta: {
            tblSource,
            decimatedUnzoomed: boolean // tells that unzoomed data are decimated
        }
        options: XYPlotParams
     ]
   }
 */

/**
 * @global
 * @public
 * @typedef {Object} XYBoundaries - object with X and Y limits
 * @prop {number} xMin - minimum X
 * @prop {number} xMax - maximum X
 * @prop {number} yMin - minimum Y
 * @prop {number} yMax - maximum Y
 */

/**
 * @global
 * @public
 * @typedef {Object} XYPlotParams - scatter plot parameters
 * @prop {string}  [title] - chart title
 * @prop {number}  [xyRatio] - x/y ratio (aspect ratio of the plot), when not defined, all the available space is used
 * @prop {string}  [stretch] - 'fit' to fit plot into available space or 'fill' to fill the available width (applied when xyPlotRatio is defined)
 * @prop {{x: number, y: number}}  [nbins] - number of bins along X and Y axis (applied to decimated plots only)
 * @prop {string}  [shading] - color scale: 'lin' for linear, log - for log scale (applied to decimated plots only)
 * @prop {XYBoundaries}  [userSetBoundaries] - user set axes limits
 * @prop {XYBoundaries}  [selection] - currently selected rectangle
 * @prop {XYBoundaries}  [zoom] -  currently zoomed rectangle
 * @prop {Object}  x - X axis options
 * @prop {string}  x.columnOrExpr - column name or expression, constructed from column names
 * @prop {string}  [x.label] - X axis label, if not defined x.columnOrExpr is used as the label
 * @prop {string}  [x.unit] - X axis unit (if defined, it will be a part of X axis title)
 * @prop {string}  [x.options] - comma separated X axis options: grid, log, and flip
 * @prop {Object}  y - Y axis options
 * @prop {string}  y.columnOrExpr - column name or expression, constructed from column names
 * @prop {string}  [y.label] - Y axis label, if not defined y.columnOrExpr is used as the label
 * @prop {string}  [y.unit] - Y axis unit (if defined, it will be a part of Y axis title)
 * @prop {string}  [y.options] - comma separated Y axis options: grid, log, and flip
 */

export function getDefaultXYPlotOptions(tbl_id) {

    const {tableMeta, tableData, totalRows}= getTblById(tbl_id);

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
        if (numericCols.length >= 2) {
            xCol = numericCols[0];
            yCol = numericCols[1];
        } else if (numericCols.length > 1) {
            xCol = numericCols[0];
            yCol = numericCols[0];
        }
    }

    return (xCol && yCol) ?
            {
                x: {columnOrExpr: xCol.name, options: isCatalog ? 'flip' : '_none_'},
                y: {columnOrExpr: yCol.name}
    } : undefined;
}

/**
 * Load xy plot data Load xy plot data - left for backward compatibility
 *
 * @param {Object} params - dispatch parameters
 * @param {string} params.chartId - if no chart id is specified table id is used as chart id
 * @param {XYPlotParams} params.xyPlotParams - XY plot options (column names, etc.)
 * @param {string} params.tblId - table id
 * @param {Function} [params.dispatcher=flux.process] - only for special dispatching uses such as remote
 */
export function loadXYPlot({chartId, xyPlotParams, tblId, dispatcher=flux.process}) {
    //SCATTER
    dispatchChartAdd({chartId, chartType: SCATTER, groupId: tblId,
        chartDataElements: [
            {
                type: DT_XYCOLS,
                options: xyPlotParams,
                tblId
            }
        ], dispatcher});
}


/**
 * Set selection to give user choice of actions on selection (zoom, filter, or select points).
 *
 * @param {string} chartId - chart id
 * @param {string} chartDataElementId - chart data element id
 * @param {XYBoundaries} [selection] - {xMin, xMax, yMin, yMax}, remove selection when not defined
 */
export function setXYSelection(chartId, chartDataElementId, selection) {
    dispatchChartOptionsUpdate({chartId, chartDataElementId, updates: {selection}, noFetch: true});
}

/**
 * Zoom XY plot to a given selection or reset zoom if no selection is given.
 *
 * @param {string} chartId - chart id
 * @param {string} chartDataElementId - chart data element id
 * @param {XYBoundaries} [selection]
 */
export function setZoom(chartId, chartDataElementId, selection=undefined) {
    const chartDataElement = getChartDataElement(chartId, chartDataElementId);
    if (!chartDataElement) { logError(`[setZoom] Chart data element is not found: ${chartId}, ${chartDataElementId}` ); return;}

    const {data:xyPlotData, options:xyPlotParams, meta} = chartDataElement;
    const decimatedUnzoomed = get(meta, 'decimatedUnzoomed');
    if (xyPlotData && xyPlotParams) {
        let noFetch = true;
        if (selection) {
            // zoom to selection
            if (xyPlotData.decimateKey) {
                noFetch = false;
            }
            dispatchChartOptionsUpdate({chartId, chartDataElementId, updates: {zoom: selection, selection: undefined}, noFetch});
        } else {
            // reset zoom
            if (decimatedUnzoomed || isUndefined(decimatedUnzoomed)) {
                noFetch = false;
            }
            dispatchChartOptionsUpdate({chartId, chartDataElementId, updates: {zoom: xyPlotParams.selection, selection: undefined}, noFetch});
        }
    }
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

function getServerCallParameters(xyPlotParams) {
    if (!xyPlotParams) { return []; }

    // error plot
    if (xyPlotParams.x.error || xyPlotParams.y.error) {
        return [xyPlotParams.x.columnOrExpr, xyPlotParams.x.error, xyPlotParams.y.columnOrExpr, xyPlotParams.y.error];
    }

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

export function getDataBoundaries(xyPlotData) {
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
 *
 * @param {Object} xyPlotParams - object with XY plot params
 * @param {XYBoundaries} boundaries - object with xMin, xMax, yMin, yMax props
 * @param {number} factor - part of the range to add on both sides
 * @returns {XYBoundaries} - padded boundaries
 * @ignore
 */
export function getPaddedBoundaries(xyPlotParams, boundaries, factor=100) {
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
 * Fetches xy plot data,
 * set isColStatsReady to true once done.
 *
 * @param {Function} dispatch
 * @param {string} chartId  - chart id
 * @param {string} chartDataElementId - chart data element id
 */
function fetchPlotData(dispatch, chartId, chartDataElementId) {
    const chartDataElement = getChartDataElement(chartId, chartDataElementId);
    if (!chartDataElement) { logError(`[XYPlot] Chart data element is not found: ${chartId}, ${chartDataElementId}` ); return; }

    const {tblId, options:xyPlotParams} = chartDataElement;
    if (!tblId || !isFullyLoaded(tblId)) {return; }

    if (get(xyPlotParams, 'x.error') || get(xyPlotParams, 'y.error')) {
        fetchXYWithErrors(dispatch, chartId, chartDataElementId);
    } else {
        fetchXYNoErrors(dispatch, chartId, chartDataElementId);
    }
}

function fetchXYNoErrors(dispatch, chartId, chartDataElementId) {
    const chartDataElement = getChartDataElement(chartId, chartDataElementId);

    // tblId - table search request to obtain source table
    // options - options to create chart element
    // meta - table metadata, contains tblSource and decimatedUnzoomed
    const {tblId, meta, options} = chartDataElement;
    let xyPlotParams = options;

    const activeTableModel = getTblById(tblId);
    const activeTableServerRequest = activeTableModel['request'];
    const tblSource = get(activeTableModel, 'tableMeta.tblFilePath');

    if (!xyPlotParams) { xyPlotParams = getDefaultXYPlotOptions(tblId); }

    const req = cloneRequest(activeTableServerRequest, {
            'startIdx' : 0,
            'pageSize' : 1000000,
            //'inclCols' : `${xyPlotParams.x.columnOrExpr},${xyPlotParams.y.columnOrExpr}`, // ignored if 'decimate' is present
            'decimate' : serializeDecimateInfo(...getServerCallParameters(xyPlotParams))
        });
    req.tbl_id = `xy-${chartId}`;

    fetchTable(req).then((tableModel) => {

        // make sure we only save the data from the latest fetch
        const cde = getChartDataElement(chartId, chartDataElementId);
        if (!cde || (cde.options && serverParamsChanged(xyPlotParams,cde.options))) {
            return;
        }

        let xyPlotData = {rows: []};
        // when zoomed, we don't know if the unzoomed data are decimated or not
        let decimatedUnzoomed = xyPlotParams.zoom ? undefined : false;

        if (tableModel.tableData && tableModel.tableData.data) {
            const {tableMeta} = tableModel;
            const decimateKey = tableMeta['decimate_key'];

            // use first 4 or 3 columns renamed
            const colNames = decimateKey ? ['x', 'y', 'rowIdx', 'weight'] : ['x', 'y', 'rowIdx'];

            // change row data from [ [val] ] to [ {cname:val} ] and make them numeric
            const rows = tableModel.tableData.data.map((row) => {
                return colNames.reduce( (arow, name, cidx) => {
                    arow[name] = parseFloat(row[cidx]);
                    return arow;
                }, {});
            });

            xyPlotData = omitBy({
                rows,
                decimateKey,
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

        // need to preserve original decimatedUnzoomed for zoomed plots
        decimatedUnzoomed = isUndefined(decimatedUnzoomed) ? get(meta,'decimatedUnzoomed') : decimatedUnzoomed;

        dispatch(chartDataUpdate(
            {
                chartId,
                chartDataElementId,
                isDataReady: true,
                error: undefined,
                options: getUpdatedParams(xyPlotParams, tblId, xyPlotData),
                data: xyPlotData,
                meta: {tblSource, decimatedUnzoomed}
            }));
    }).catch((reason) => {
        dispatchError(dispatch, chartId, chartDataElementId, reason);
    });
}


function fetchXYWithErrors(dispatch, chartId, chartDataElementId) {
    const chartDataElement = getChartDataElement(chartId, chartDataElementId);

    // tblId - table search request to obtain source table
    // options - options to create chart element
    const {tblId,  options:xyPlotParams} = chartDataElement;

    const activeTableModel = getTblById(tblId);
    const activeTableServerRequest = activeTableModel['request'];
    const tblSource = get(activeTableModel, 'tableMeta.tblFilePath');

    if (!xyPlotParams) {
        dispatchError(dispatch, chartId, chartDataElementId, 'Can not fetch data for unknown parameters');
    }

    const sreq = cloneRequest(activeTableServerRequest, {'startIdx' : 0, 'pageSize' : 1000000});
    const req = makeTblRequest('XYWithErrors');
    req.searchRequest = JSON.stringify(sreq);
    req.xColOrExpr = get(xyPlotParams, 'x.columnOrExpr');
    req.yColOrExpr = get(xyPlotParams, 'y.columnOrExpr');

    if (!req.xColOrExpr || !req.yColOrExpr) {
        dispatchError(dispatch, chartId, chartDataElementId, 'Unknown X/Y column or expression');
    }

    req.xErrLowColOrExpr = get(xyPlotParams, 'x.error');
    req.xErrHighColOrExpr = get(xyPlotParams, 'x.error');
    req.yErrLowColOrExpr = get(xyPlotParams, 'y.error');
    req.yErrHighColOrExpr = get(xyPlotParams, 'y.error');
    req.startIdx = 0;
    req.pageSize = 1000000;

    fetchTable(req).then((tableModel) => {

        // make sure we only save the data from the latest fetch
        const cde = getChartDataElement(chartId, chartDataElementId);
        if (!cde || (cde.options && serverParamsChanged(xyPlotParams,cde.options))) {
            return;
        }

        let xyPlotData = {rows: []};

        if (tableModel.tableData && tableModel.tableData.data) {
            const {tableMeta} = tableModel;

            const colNames = tableModel.tableData.columns.map((col) => col.name);
            colNames[1] = 'x';
            colNames[2] = 'y';

            // make sure all fields are numeric and change row data from [ [val] ] to [ {cname:val} ]
            const rows = tableModel.tableData.data.map((row) => {
                return colNames.reduce( (nrow, name, cidx) => {
                    nrow[name] = parseFloat(row[cidx]);
                    return nrow;
                }, {});
            });

            xyPlotData = omitBy({
                rows,
                xMin: tableMeta['X-MIN'],
                xMax: tableMeta['X-MAX'],
                yMin: tableMeta['Y-MIN'],
                yMax: tableMeta['Y-MAX'],
                idStr: tableMeta['tbl_id']
            }, isUndefined);

            // convert strings with numbers into numbers
            Object.keys(xyPlotData).forEach( (prop) => {
                const val = xyPlotData[prop];
                if (isString(val) && isFinite(val)) { xyPlotData[prop] = Number(val); }
            });

        }

        dispatch(chartDataUpdate(
            {
                chartId,
                chartDataElementId,
                isDataReady: true,
                error: undefined,
                options: getUpdatedParams(xyPlotParams, tblId, xyPlotData),
                data: xyPlotData,
                meta: {tblSource}
            }));
    }).catch((reason) => {
        dispatchError(dispatch, chartId, chartDataElementId, reason);
    });
}

function dispatchError(dispatch, chartId, chartDataElementId, reason) {
    const message = 'Failed to fetch XY plot data';
    logError(`${message}: ${reason}`);
    dispatch(chartDataUpdate(
        {
            chartId,
            chartDataElementId,
            isDataReady: true,
            error: {message, reason},
            data: undefined
        }));
}


/*
 * Label and unit must be specified to display plot,
 * derive them from existing parameters or tableModel.
 * No selection should be present in updated parameters
 */
function getUpdatedParams(xyPlotParams, tblId, data) {
    const tableModel = getTblById(tblId);
    let newParams = xyPlotParams;
    const dataBoundaries = getDataBoundaries(data);

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


