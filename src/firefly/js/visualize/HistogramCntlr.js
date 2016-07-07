/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {flux} from '../Firefly.js';

import {get, has, omit} from 'lodash';

import {updateSet, updateMerge} from '../util/WebUtil.js';
import {doFetchTable, getTblById, isFullyLoaded, makeTblRequest} from '../tables/TableUtil.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {DELETE} from './ChartsCntlr.js';

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
           algorithm: 'fixedSizeBins' or 'byesianBlocks'
           numBins: int - for 'fixedSizeBins' algorithm
           x: [log,flip] x (domain) axis options
           y: [log,flip] y (counts) axis options
           falsePositiveRate: double - for 'byesianBlocks' algorithm (default 0.05)
           minCutoff: double
           maxCutoff: double
         }
   }
 */


export const HISTOGRAM_DATA_KEY = 'histogram';
export const LOAD_COL_DATA = `${HISTOGRAM_DATA_KEY}/LOAD_COL_DATA`;
export const UPDATE_COL_DATA = `${HISTOGRAM_DATA_KEY}/UPDATE_COL_DATA`;


/*
 * Get column histogram data
 * @param {Object} histogramParams - histogram options (column name, etc.)
 * @param {string} tblId - table id
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export const dispatchLoadColData = function(chartId, histogramParams, tblId, dispatcher= flux.process) {
    dispatcher({type: LOAD_COL_DATA, payload: {chartId, histogramParams, tblId}});
};

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

/*
 * @param rawAction (its payload should contain searchRequest to get source table and histogram parameters)
 * @returns function which loads statistics (column name, num. values, range of values) for a source table
 */
export const loadColData = function(rawAction) {
    return (dispatch) => {

        const {chartId, histogramParams, tblId} = rawAction.payload;
        const tblSource = get(getTblById(tblId), 'tableMeta.source');

        const chartModel = get(flux.getState(), [HISTOGRAM_DATA_KEY, chartId]);
        let serverCallNeeded = !chartModel || !chartModel.tblSource || chartModel.tblSource !== tblSource;

        if (serverCallNeeded || chartModel.histogramParams !== histogramParams) {
            // when server call parameters do not change but chart parameters change,
            // we do need to update parameters, but we can reuse the old chart data
            serverCallNeeded = serverCallNeeded || serverParamsChanged(chartModel.histogramParams, histogramParams);

            dispatch({type: LOAD_COL_DATA, payload: {...rawAction.payload, tblSource, serverCallNeeded}});
            if (serverCallNeeded) {
                fetchColData(dispatch, tblId, histogramParams, chartId);
            }
        }
    };
};

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

export function reducer(state={}, action={}) {
    switch (action.type) {
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
                Object.assign({}, omit(state, chartsToDelete)) : state;
        }
        case (DELETE) :
        {
            const {chartId, chartType} = action.payload;
            if (chartType === 'histogram' && has(state, chartId)) {
                return Object.assign({}, omit(state, [chartId]));
            }
            return state;
        }
        case (LOAD_COL_DATA)  :
        {
            const {chartId, tblId, histogramParams, tblSource, serverCallNeeded} = action.payload;
            if (serverCallNeeded) {
                return updateSet(state, chartId, {tblId, isColDataReady: false, tblSource, histogramParams});
            } else {
                // only histogram parameters changed
                return updateSet(state, [chartId, 'histogramParams'], histogramParams);
            }
        }
        case (UPDATE_COL_DATA)  :
        {
            const {chartId, isColDataReady, tblSource, histogramData, histogramParams} = action.payload;
            if (state[chartId].histogramParams === histogramParams) {
                return updateMerge(state, chartId, {
                    isColDataReady,
                    tblSource,
                    histogramData
                });
            } else {
                return state;
            }
        }
        default:
            return state;
    }
}

/**
 *
 * @param data {Object} the data to merge with the histogram branch under root
 * @returns {{type: string, payload: object}}
 */
function updateColData(data) {
    return { type : UPDATE_COL_DATA, payload: data };
}

/**
 * fetches active table statistics data
 * set isColStatsReady to true once done.
 * @param {function} dispatch
 * @param {Object} tblId table id of the source table
 * @param {Object} histogramParams object, which contains histogram parameters
 * @param {string} chartId - chart id
 */
function fetchColData(dispatch, tblId, histogramParams, chartId) {

    if (!isFullyLoaded(tblId) || !histogramParams) {
        return;
    }

    const activeTableModel = getTblById(tblId);
    const activeTableServerRequest = activeTableModel['request'];
    const tblSource = get(activeTableModel, 'tableMeta.source');

    const sreq = Object.assign({}, omit(activeTableServerRequest, ['tbl_id', 'META_INFO']),
        {'startIdx' : 0, 'pageSize' : 1000000});

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
                const histogramData = tableModel.tableData.data.reduce((data, arow) => {
                    data.push(arow.map(toNumber));
                    return data;
                }, []);

                dispatch(updateColData(
                    {
                        chartId,
                        isColDataReady : true,
                        tblSource,
                        histogramParams,
                        histogramData
                    }));
            }
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch histogram data: ${reason}`);
        }
    );
}