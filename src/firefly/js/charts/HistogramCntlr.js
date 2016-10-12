/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {flux} from '../Firefly.js';

import {get} from 'lodash';

import {doFetchTable, getTblById, isFullyLoaded, makeTblRequest, cloneRequest} from '../tables/TableUtil.js';
import {getChartDataElement, chartDataLoaded, dispatchChartAdd} from './ChartsCntlr.js';
import {logError} from '../util/WebUtil.js';

/**
 * Returns chart data type based
 * @returns {ChartDataType}
 */
export const DATATYPE_HISTOGRAM = {
        id: 'histogram',
        fetchData: fetchColData,
        fetchParamsChanged: serverParamsChanged
};

/*
 Possible structure of store:
 /histogram
   chartId: Object - the name of this node matches chart id
   {
         // tblHistogramData
         tblId: string // table id
         tblSource: string // source of the table
         isColDataReady: boolean
         histogramData: [[numInBin: int, min: double, max: double]*]
         histogramParams: {
           columnOrExpr: column name or column expression
           algorithm: 'fixedSizeBins' or 'bayesianBlocks'
           numBins: int - for 'fixedSizeBins' algorithm
           x: [log,flip] x (domain) axis options
           y: [log,flip] y (counts) axis options
           falsePositiveRate: double - for 'bayesianBlocks' algorithm (default 0.05)
           minCutoff: double
           maxCutoff: double
         }
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

/**
 * Load histogram data.
 *
 * @param {Object} params - dispatch parameters
 * @param {string} params.chartId - if no chart id is specified table id is used as chart id
 * @param {HistogramParams} params.histogramParams - histogram options (column name, etc.)
 * @param {boolean} [params.markAsDefault=false] - are the options considered to be "the default" to reset to
 * @param {string} params.tblId - table id
 * @param {Function} [params.dispatcher] - only for special dispatching uses such as remote
 * @public
 * @function dispatchLoadColData
 * @memberof firefly.action
 */
export function dispatchLoadColData({chartId, histogramParams, markAsDefault=false, tblId, dispatcher=flux.process}) {
    // HISTOGRAM
    dispatchChartAdd({chartId, chartType: 'histogram', groupId: tblId,
        chartDataElements: [
            {
                type: 'histogram', //DATA_TYPE_HISTOGRAM.id
                options: histogramParams,
                tblId
            }
        ], dispatcher});
}

/*
 * Get column histogram data
 * @param {string} chartId - chart id
 * @param {boolean} isColDataReady - flags that column histogram data are available
 * @param {number[][]} histogramData - an array of the number arrays with npoints, binmin, binmax
 * @param {Object} histogramParams - histogram options (column name, etc.)
const dispatchUpdateColData = function(chartId, isColDataReady, histogramData, histogramParams) {
    flux.process({type: UPDATE_COL_DATA, payload: {chartId,isColDataReady,histogramData,histogramParams}});
};
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

    const serverParams = [];
    serverParams.push(histogramParams.columnOrExpr);
    serverParams.push(histogramParams.x && histogramParams.x.includes('log'));
    serverParams.push(histogramParams.numBins);
    serverParams.push(histogramParams.falsePositiveRate);
    //serverParams.push(histogramParams.minCutoff);
    //serverParams.push(histogramParams.maxCutoff);
    return serverParams;
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
    const tblSource = get(activeTableModel, 'tableMeta.tblFilePath');

    const sreq = cloneRequest(activeTableServerRequest, {'startIdx' : 0, 'pageSize' : 1000000});

    const req = makeTblRequest('HistogramProcessor');
    req.searchRequest = JSON.stringify(sreq);

    // histogram parameters
    req.columnExpression = histogramParams.columnOrExpr;
    if (histogramParams.x && histogramParams.x.includes('log')) {
        req.columnExpression = 'log('+req.columnExpression+')';
    }
    if (histogramParams.numBins) { // fixed size bins
        req.numBins = histogramParams.numBins;
    }
    if (histogramParams.falsePositiveRate) {  // variable size bins using Bayesian Blocks
        req.falsePositiveRate = histogramParams.falsePositiveRate;
    }
    /*
    if (histogramParams.minCutoff) {
        req.min = histogramParams.minCutoff;
    }
    if (histogramParams.maxCutoff) {
        req.max = histogramParams.maxCutoff;
    }
    */

    req.tbl_id = 'histogram-'+chartId;

    doFetchTable(req).then(
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
                histogramData = tableModel.tableData.data.reduce((data, arow) => {
                    data.push(arow.map(toNumber));
                    return data;
                }, []);

            }
            dispatch(chartDataLoaded(
                {
                    chartId,
                    chartDataElementId,
                    options : histogramParams,
                    data: histogramData,
                    meta: {tblSource}
                }));
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch histogram data: ${reason}`);
        }
    );
}
