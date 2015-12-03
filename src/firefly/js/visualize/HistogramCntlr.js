/*
 Possible structure of histogram store:
 /histogram
   searchReq: ServerRequest - if source table changes, histogram store should be cleared
   isColStatsReady: boolean
   colStats: [ColValuesStatistics]
   isColDataReady: boolean
   histogramData: [[numInBin: int, min: double, max: double]*]
   histogramParams: {
          columnOrExpr: column name or column expression
          algorithm: 'fixedSizeBins' or 'byesianBlocks'
          numBins: int - for 'fixedSizeBins' algorithm
          falsePositiveRate: double - for 'byesianBlocks' algorithm (default 0.05)
          minCutoff: double
          maxCutoff: double
 */

import {flux} from '../Firefly.js';
import ColValuesStatistics from './ColValuesStatistics.js';
import {ServerRequest} from '../data/ServerRequest.js';
import {getRawDataSet} from '../rpc/SearchServicesJson.js';
import {parseRawDataSet} from '../util/DataSetParser.js';

const HISTOGRAM_DATA_KEY = 'histogram';
const LOAD_TBL_STATS = 'histogram/LOAD_TBL_STATS';
const UPDATE_TBL_STATS = 'histogram/UPDATE_TBL_STATS';
const LOAD_COL_DATA = 'histogram/LOAD_COL_DATA';
const UPDATE_COL_DATA = 'histogram/UPDATE_COL_DATA';


/*
 * Get the number of points, min and max values, units and description for each table column
 * @param {ServerRequest} searchRequest - table search request
 */
const dispatchLoadTblStats = function(searchRequest) {
    flux.process({type: LOAD_TBL_STATS, payload: {searchRequest }});
};

/*
 * The statistics is successfully returned from the server, update the store
 * @param {boolean} isColStatsReady flags that column statistics is now available
 * @param {ColValuesStatistics[]} an array which holds column statistics for each column
 * @param {ServerRequest} table search request
 */
const dispatchUpdateTblStats = function(isColStatsReady,colStats,searchReq) {
    flux.process({type: UPDATE_TBL_STATS, payload: {isColStatsReady,colStats,searchReq}});
};

/*
 * Get column histogram data
 * @param {Object} histogramParams - histogram options (column name, etc.)
 * @param {ServerRequest} searchRequest - table search request
 */
const dispatchLoadColData = function(histogramParams, searchRequest) {
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
    flux.process({type: LOAD_COL_DATA, payload: {isColDataReady,histogramData,histogramParams,searchRequest}});
};

/*
 * @param rawAction (its payload should contain searchRequest to get source table)
 * @returns function which loads statistics (column name, num. values, range of values) for a source table
 */
const loadTblStats = function(rawAction) {
    return (dispatch) => {
        dispatch({ type : LOAD_TBL_STATS });
        if (rawAction.payload.searchRequest) {
            fetchTblStats(dispatch, rawAction.payload.searchRequest);
        }
    };
};

/*
 * @param rawAction (its payload should contain searchRequest to get source table and histogram parameters)
 * @returns function which loads statistics (column name, num. values, range of values) for a source table
 */
const loadColData = function(rawAction) {
    return (dispatch) => {
        dispatch({ type : LOAD_COL_DATA });
        if (rawAction.payload.searchRequest && rawAction.payload.histogramParams) {
            fetchColData(dispatch, rawAction.payload.searchRequest, rawAction.payload.histogramParams);
        }

    };
};

function getInitState() {
    return {
        isColStatsReady : false
    };
}

function reducer(state=getInitState(), action={}) {

    switch (action.type) {
        case (LOAD_TBL_STATS)  :
            return Object.assign({}, state, getInitState());

        case (UPDATE_TBL_STATS)  :
            return Object.assign({}, state, action.payload);

        case (LOAD_COL_DATA)  :
            return Object.assign({}, state, {isColDataReady : false});

        case (UPDATE_COL_DATA)  :
            return Object.assign({}, state, action.payload);

        default:
            return state;
    }
}


/**
 *
 * @param statsData {Object} The table statistics object to merge with the histogram branch under root
 * @returns {{type: string, payload: object}}
 */
function updateTblStats(statsData) {
    return { type : UPDATE_TBL_STATS, payload: statsData };
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
 * fetches histogram data for a column or column expression of an active table
 * set isColStatsReady to true once done.
 * @param dispatch
 * @param activeTableServerRequest table search request to obtain source table
 */
function fetchTblStats(dispatch, activeTableServerRequest) {

    /*
     // Sample TableServerRequest
     const sreq = new ServerRequest('IpacTableFromSource');
     req.setParam({name : 'source', value : 'http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl'});
     req.setParam({name : 'startIdx', value : '0'});
     req.setParam({name : 'pageSize', value : '1000000'});
     */
    const sreq = Object.assign({}, activeTableServerRequest, {'startIdx': 0, 'pageSize': 1000000});

    const req = new ServerRequest('IpacTableFromSource');
    req.setParam({name: 'processor', value: 'StatisticsProcessor'});
    req.setParam({name: 'searchRequest', value: JSON.stringify(sreq.params)});
    req.setParam({name: 'startIdx', value: '0'});
    req.setParam({name: 'pageSize', value: '10000'});
    getRawDataSet(req).then(
        (rawDataSet) => {
            const dataSet = parseRawDataSet(rawDataSet);
            const model = dataSet.getModel();
            const colStats = [];
            for (let i = 0; i < model.size(); i++) {
                const arow = model.getRow(i);
                colStats.push(new ColValuesStatistics(...arow));
            }
            dispatch(updateTblStats(
                {
                    isColStatsReady: true,
                    colStats,
                    searchReq: sreq
                }));
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch table statistics: ${reason}`);
        }
    );
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

    const req = new ServerRequest('IpacTableFromSource');
    req.setParam({name : 'processor', value : 'HistogramProcessor'});
    req.setParam({name : 'searchRequest', value : JSON.stringify(sreq.params)});

    // histogarm parameters
    req.setParam({name : 'columnExpression', value : histogramParams.columnOrExpr});
    if (histogramParams.numBins) { // fixed size bins
        req.setParam({name: 'numBins', value: histogramParams.numBins});
    }
    if (histogramParams.falsePositiveRate) {  // variable size bins using Bayesian Blocks
        req.setParam({name: 'falsePositiveRate', value: histogramParams.falsePositiveRate});
    }
    if (histogramParams.minCutoff) {
        req.setParam({name: 'min', value: histogramParams.minCutoff});
    }
    if (histogramParams.maxCutoff) {
        req.setParam({name: 'max', value: histogramParams.maxCutoff});
    }

    req.setParam({name : 'startIdx', value : '0'});
    req.setParam({name : 'pageSize', value : '10000'});
    getRawDataSet(req).then(
        (rawDataSet) => {
            const histogramData = [];
            const dataSet = parseRawDataSet(rawDataSet);
            const model = dataSet.getModel();
            var toNumber = (val)=>Number(val);
            for (let i = 0; i < model.size(); i++) {
                const arow = model.getRow(i);
                histogramData.push(arow.map(toNumber));
            }

            dispatch(updateColData(
                {
                    isColDataReady : true,
                    histogramParams,
                    histogramData,
                    searchReq : sreq
                }));
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch histogram data: ${reason}`);
        }
    );

}



var HistogramCntlr = {
    reducer,
    HISTOGRAM_DATA_KEY,
    dispatchLoadTblStats,
    dispatchLoadColData,
    loadTblStats,
    LOAD_TBL_STATS,
    UPDATE_TBL_STATS,
    loadColData,
    LOAD_COL_DATA,
    UPDATE_COL_DATA };
export default HistogramCntlr;