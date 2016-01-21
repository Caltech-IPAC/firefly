import {flux} from '../Firefly.js';

import {has, get, set} from 'lodash';

import ColValuesStatistics from './ColValuesStatistics.js';

import {TableRequest} from '../tables/TableRequest.js';
import LoadTable from '../tables/reducers/LoadTable.js';
import TableUtil from '../tables/TableUtil.js';

import TablesCntlr from '../tables/TablesCntlr.js';

/*
 Possible structure of store:
 /tblstats
   tbl_id: Object - the name of this node matches table id
   {
     isTblLoaded: boolean - tells if the table is completely loaded
     searchRequest: TableRequest - if source table changes, histogram store should be recreated
     isColStatsReady: boolean
     colStats: [ColValuesStatistics]
   }
 */


const TBLSTATS_DATA_KEY = 'tblstats';
const SETUP_TBL_TRACKING = `${TBLSTATS_DATA_KEY}/SETUP_TBL_TRACKING`;
const LOAD_TBL_STATS = `${TBLSTATS_DATA_KEY}/LOAD_TBL_STATS`;
const UPDATE_TBL_STATS = `${TBLSTATS_DATA_KEY}/UPDATE_TBL_STATS`;

/*
 * Set up store, which will reflect the data relevant to the given table
 * @param {string} tblId - table id
 */
const dispatchSetupTblTracking = function(tblId) {
    flux.process({type: SETUP_TBL_TRACKING, payload: {tblId}});
};

/*
 * Get the number of points, min and max values, units and description for each table column
 * @param {ServerRequest} searchRequest - table search request
 */
const dispatchLoadTblStats = function(searchRequest) {
    flux.process({type: LOAD_TBL_STATS, payload: {searchRequest}});
};

/*
 * The statistics is successfully returned from the server, update the store
 * @param {boolean} isColStatsReady flags that column statistics is now available
 * @param {ColValuesStatistics[]} an array which holds column statistics for each column
 * @param {ServerRequest} table search request
 */
const dispatchUpdateTblStats = function(isColStatsReady,colStats,searchRequest) {
    flux.process({type: UPDATE_TBL_STATS, payload: {isColStatsReady,colStats,searchRequest}});
};

/*
 * @param rawAction (its payload should contain searchRequest to get source table)
 * @returns function which loads statistics (column name, num. values, range of values) for a source table
 */
const loadTblStats = function(rawAction) {
    return (dispatch) => {
        dispatch({ type : LOAD_TBL_STATS, payload : rawAction.payload });
        if (rawAction.payload.searchRequest) {
            fetchTblStats(dispatch, rawAction.payload.searchRequest);
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

function reducer(state=getInitState(), action={}) {
    switch (action.type) {
        case (SETUP_TBL_TRACKING) :
            var {tblId} = action.payload;
            var isTblLoaded;
            if (TableUtil.isFullyLoaded(tblId)) {
                isTblLoaded = true;
                action.sideEffect((dispatch) => fetchTblStats(dispatch, TableUtil.findById(tblId).model.request));

            } else {
                isTblLoaded = false;
            }
            const newState = Object.assign({}, state);
            set(newState, tblId, {isTblLoaded});
            return newState;
        case (TablesCntlr.LOAD_TABLE)  :
            const {tbl_id, tableMeta, request} = action.payload;
            if (has(state, tbl_id)) {
                if (tableMeta.isFullyLoaded && !get(state, [tbl_id, 'isTblLoaded'])){
                    const newState = Object.assign({}, state);
                    set(newState, tbl_id, {isTblLoaded:true});
                    action.sideEffect((dispatch) => fetchTblStats(dispatch,request));
                    return newState;
                }
            }
            return state;
        case (LOAD_TBL_STATS)  :
        {
            let {searchRequest} = action.payload;
            return stateWithNewData(searchRequest.tbl_id, state, {isColStatsReady: false});
        }
        case (UPDATE_TBL_STATS)  :
        {
            let {isColStatsReady, colStats, searchRequest} = action.payload;
            return stateWithNewData(searchRequest.tbl_id, state, {isColStatsReady, colStats, searchRequest});
        }
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
 * fetches histogram data for a column or column expression of an active table
 * set isColStatsReady to true once done.
 * @param dispatch
 * @param activeTableServerRequest table search request to obtain source table
 */
function fetchTblStats(dispatch, activeTableServerRequest) {

    // searchRequest
    const sreq = Object.assign({}, activeTableServerRequest, {'startIdx': 0, 'pageSize': 1000000});

    const req = TableRequest.newInstance({
                    id:'StatisticsProcessor',
                    searchRequest: JSON.stringify(sreq),
                    tbl_id: activeTableServerRequest.tbl_id
                });

    LoadTable.doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                const colStats = tableModel.tableData.data.reduce((colstats, arow) => {
                    colstats.push(new ColValuesStatistics(...arow));
                    return colstats;
                }, []);
                dispatch(updateTblStats(
                    {
                        isColStatsReady: true,
                        colStats,
                        searchRequest: sreq
                    }));
            }
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch table statistics: ${reason}`);
        }
    );
}





var TableStatsCntlr = {
    reducer,
    TBLSTATS_DATA_KEY,
    dispatchSetupTblTracking,
    dispatchLoadTblStats,
    SETUP_TBL_TRACKING,
    loadTblStats,
    LOAD_TBL_STATS,
    UPDATE_TBL_STATS};
export default TableStatsCntlr;