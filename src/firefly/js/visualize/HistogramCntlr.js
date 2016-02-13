/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {flux} from '../Firefly.js';

import {has, get, set} from 'lodash';

import {TableRequest} from '../tables/TableRequest.js';
import * as TableUtil from '../tables/TableUtil.js';

/*
 Possible structure of store:
 /histogram
   tbl_id: Object - the name of this node matches table id
   {
         // tblHistogramData
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
 */
export const dispatchLoadColData = function(histogramParams, searchRequest) {
    flux.process({type: LOAD_COL_DATA, payload: {histogramParams, searchRequest}});
};

/*
 * Get column histogram data
 * @param {boolean} isColDataReady - flags that column histogram data are available
 * @param {Number[][]} histogramData - an array of the number arrays with npoints, binmin, binmax
 * @param {Object} histogramParams - histogram options (column name, etc.)
 * @param {ServerRequest} searchRequest - table search request
 */
const dispatchUpdateColData = function(isColDataReady, histogramData, histogramParams, searchRequest) {
    flux.process({type: UPDATE_COL_DATA, payload: {isColDataReady,histogramData,histogramParams,searchRequest}});
};

/*
 * @param rawAction (its payload should contain searchRequest to get source table and histogram parameters)
 * @returns function which loads statistics (column name, num. values, range of values) for a source table
 */
export const loadColData = function(rawAction) {
    return (dispatch) => {
        dispatch({ type : LOAD_COL_DATA, payload : rawAction.payload });
        if (rawAction.payload.searchRequest && rawAction.payload.histogramParams) {
            fetchColData(dispatch, rawAction.payload.searchRequest, rawAction.payload.histogramParams);
        }

    };
};

function getInitState() {
    return {};
}


/*
 Get the new state related to a particular table (if it's tracked)
 @param tblId {string} table id
 @param state {object} histogram store
 @param newProps {object} new properties
 @return {object} new state
 */
function stateWithNewData(tblId, state, newProps) {
    if (has(state, tblId)) {
        const tblData = get(state, tblId);
        const newTblData = Object.assign({}, tblData, newProps);
        const newState = Object.assign({}, state);
        set(newState, tblId, newTblData);
        return newState;
    }
    return state;
}

export function reducer(state=getInitState(), action={}) {
    switch (action.type) {
        case (LOAD_COL_DATA)  :
        {
            let {histogramParams, searchRequest} = action.payload;
            const newState = Object.assign({}, state);
            set(newState, searchRequest.tbl_id, {isColDataReady: false});
            return newState;
        }
        case (UPDATE_COL_DATA)  :
        {
            let {isColDataReady, histogramData, histogramParams, searchRequest} = action.payload;
            return stateWithNewData(searchRequest.tbl_id, state, {
                isColDataReady,
                histogramData,
                histogramParams,
                searchRequest
            });
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
 * @param dispatch
 * @param activeTableServerRequest table search request to obtain source table
 * @param histogramParams object, which contains histogram parameters

 */
function fetchColData(dispatch, activeTableServerRequest, histogramParams) {

    const sreq = Object.assign({}, activeTableServerRequest, {'startIdx' : 0, 'pageSize' : 1000000});

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
    if (histogramParams.minCutoff) {
        req.min = histogramParams.minCutoff;
    }
    if (histogramParams.maxCutoff) {
        req.max = histogramParams.maxCutoff;
    }

    req.tbl_id = activeTableServerRequest.tbl_id;

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
                        isColDataReady : true,
                        histogramParams,
                        histogramData,
                        searchRequest : sreq
                    }));
            }
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch histogram data: ${reason}`);
        }
    );
}