/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';

import {fetchTable} from '../../rpc/SearchServicesJson.js';
import {getTblById, isFullyLoaded} from '../../tables/TableUtil.js';
import {makeTableFunctionRequest} from '../../tables/TableRequestUtil.js';
import {getChartDataElement, chartDataUpdate} from './../ChartsCntlr.js';
import {logError} from '../../util/WebUtil.js';

export const DT_HISTOGRAM = 'histogram';

/**
 * Chart data type for histogram data
 * @constant
 * @type {ChartDataType}
 */
export const DATATYPE_HISTOGRAM = {
        id: DT_HISTOGRAM,
        fetchData: fetchColData,
        fetchParamsChanged: serverParamsChanged,
        fetchOnTblSort: false
};

/*
 Possible structure of store with histogram data:
 /data
   chartId: Object - the name of this node matches chart id
   {
      chartDataElements: [
        tblId
        isDataReady
        data: [[numInBin: int, min: double, max: double]*]
        meta: {tblSource}
        options: HistogramParams
      ]
   }
 */

/**
 * @global
 * @public
 * @typedef {Object} HistogramParams - histogram parameters
 * @prop {string} columnOrExpr - column or expression to use for histogram, can contain multiple column names ex. log(col) or (col1-col2)/col3
 * @prop {string} algorithm - 'fixedSizeBins' or 'bayesianBlocks'
 * @prop {number} [numBins] - number of bins for fixed bins algorithm (default)
 * @prop {number} [falsePositiveRate] false positive rate for bayesian blocks algorithm
 * @prop {string} [x]   comma separated list of x axis options: flip,log
 * @prop {string} [y]   comma separated list of y axis options: flip,log
 */

function serverParamsChanged(oldParams, newParams) {
    if (oldParams === newParams) { return false; }
    if (!oldParams || !newParams) { return true; }

    const newServerParams = getServerCallParameters(newParams);
    const oldServerParams = getServerCallParameters(oldParams);
    return newServerParams.some((p, i) => {
        return p !== oldServerParams[i];
    });
}

function getServerCallParameters(histogramParams) {
    if (!histogramParams) { return []; }
    return [
        histogramParams.columnOrExpr,
        histogramParams.x && histogramParams.x.includes('log'),
        histogramParams.binWidth,
        histogramParams.numBins,
        histogramParams.fixedBinSizeSelection,
        histogramParams.falsePositiveRate,
        histogramParams.minCutoff,
        histogramParams.maxCutoff
    ];
}


/**
 * Fetches histogram data.
 *
 * @param {Function} dispatch
 * @param {string} chartId  - chart id
 * @param {string} chartDataElementId - chart data element id
 */
function fetchColData(dispatch, chartId, chartDataElementId) {

    const chartDataElement = getChartDataElement(chartId, chartDataElementId);
    if (!chartDataElement) { logError(`[Histogram] Chart data element is not found: ${chartId}, ${chartDataElementId}` ); return; }

    const {tblId, options:histogramParams} = chartDataElement;

    if (!isFullyLoaded(tblId) || !histogramParams) {
        return;
    }

    const activeTableModel = getTblById(tblId);
    const activeTableServerRequest = activeTableModel['request'];
    const tblSource = get(activeTableModel, 'tableMeta.resultSetID');

    const req = makeTableFunctionRequest(activeTableServerRequest, 'HistogramProcessor');

    // histogram parameters
    req.columnExpression = histogramParams.columnOrExpr;
    if (histogramParams.x && histogramParams.x.includes('log')) {
        req.columnExpression = 'lg('+req.columnExpression+')';
    }
    if (histogramParams.fixedBinSizeSelection) { // fixed size bins
        req.fixedBinSizeSelection=histogramParams.fixedBinSizeSelection;
        if (histogramParams.fixedBinSizeSelection==='numBins'){
           req.binSize =  histogramParams.numBins;
        }else{
            req.binSize =  histogramParams.binWidth;
        }
    }
    if (histogramParams.falsePositiveRate) {  // variable size bins using Bayesian Blocks
        req.falsePositiveRate = histogramParams.falsePositiveRate;
    }
    if (histogramParams.minCutoff) {
        req.min = histogramParams.minCutoff;
    }
    if (histogramParams.maxCutoff) {
        req.max = histogramParams.maxCutoff;
    }

    req.tbl_id = 'histogram-'+chartId;

    fetchTable(req).then(
        (tableModel) => {

            // make sure the data are coming from the latest search
            const currentChartDataElement = getChartDataElement(chartId, chartDataElementId);
            if (!currentChartDataElement || serverParamsChanged(histogramParams,currentChartDataElement.options)) {
                return;
            }

            let histogramData = [];
            if (tableModel.tableData && tableModel.tableData.data) {
                // if logarithmic values were requested, convert the returned exponents back
                var toNumber = histogramParams.x.includes('log') ?
                    (val,i)=>{
                        if (i === 0) {
                            return Number(val);
                        }
                        else {
                            return Math.pow(10,Number(val));
                        }
                    } : (val)=>Number(val);
                var nrow, lastIdx;
                histogramData = tableModel.tableData.data.reduce((data, arow, i) => {
                    nrow = arow.map(toNumber);
                    lastIdx = data.length - 1;
                    if (i>0 && data[lastIdx][1]===nrow[1]) {
                        //collapse bins when two bins are the same because of precision loss
                        data[lastIdx] = [data[lastIdx][0]+nrow[0], data[lastIdx][1], nrow[2]];
                    } else {
                        data.push(nrow);
                    }
                    return data;
                }, []);

            }
            dispatch(chartDataUpdate(
                {
                    chartId,
                    chartDataElementId,
                    isDataReady: true,
                    error: undefined,
                    options : histogramParams,
                    data: histogramData,
                    meta: {tblSource}
                }));
        }
    ).catch(
        (reason) => {
            dispatchError(dispatch, chartId, chartDataElementId, reason);
        }
    );
}
function dispatchError(dispatch, chartId, chartDataElementId, reason) {
    const message = 'Failed to fetch histogram data';
    logError(`${message}: ${reason}`);
    let reasonStr = `${reason}`.toLowerCase();
    if (reasonStr.match(/invalid column/)) {
        reasonStr = 'Non-existent column or invalid expression. Please use valid value.';
    } else {
        reasonStr = 'Please contact Help Desk. Check browser console for more information.';
    }
    dispatch(chartDataUpdate(
        {
            chartId,
            chartDataElementId,
            isDataReady: true,
            error: {message, reason: reasonStr},
            data: undefined
        }));
}