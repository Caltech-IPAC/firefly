/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {flux} from '../Firefly.js';

import {omit} from 'lodash';

import {updateSet, updateMerge} from '../util/WebUtil.js';
import {TableRequest} from '../tables/TableRequest.js';
import * as TableUtil from '../tables/TableUtil.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';

/*
 Possible structure of store:
 /histogram
   chartId: Object - the name of this node matches chart id
   {
         // tblHistogramData
         tblId: string // table id
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
 * @param {ServerRequest} searchRequest - table search request
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export const dispatchLoadColData = function(chartId, histogramParams, searchRequest, dispatcher= flux.process) {
    dispatcher({type: LOAD_COL_DATA, payload: {chartId, histogramParams, searchRequest}});
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
        dispatch({ type : LOAD_COL_DATA, payload : rawAction.payload });
        const {searchRequest, histogramParams, chartId} = rawAction.payload;
        if (searchRequest && histogramParams) {
            fetchColData(dispatch, searchRequest, histogramParams, chartId);
        }

    };
};

function getInitState() {
    return {};
}



export function reducer(state=getInitState(), action={}) {
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
        case (LOAD_COL_DATA)  :
        {
            const {chartId, histogramParams, searchRequest} = action.payload;
            return updateSet(state, chartId, {tblId: searchRequest.tbl_id, isColDataReady: false, histogramParams});
        }
        case (UPDATE_COL_DATA)  :
        {
            const {chartId, isColDataReady, histogramData, histogramParams} = action.payload;
            if (state[chartId].histogramParams === histogramParams) {
                return updateMerge(state, chartId, {
                    isColDataReady,
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
 * @param {Object} activeTableServerRequest table search request to obtain source table
 * @param {Object} histogramParams object, which contains histogram parameters
 * @param {string} chartId - chart id
 */
function fetchColData(dispatch, activeTableServerRequest, histogramParams, chartId) {

    //const {tbl_id} = activeTableServerRequest;
    const sreq = Object.assign({}, omit(activeTableServerRequest, ['tbl_id', 'META_INFO']),
        {'startIdx' : 0, 'pageSize' : 1000000});

    const req = TableRequest.newInstance({id:'HistogramProcessor'});
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

    TableUtil.doFetchTable(req).then(
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