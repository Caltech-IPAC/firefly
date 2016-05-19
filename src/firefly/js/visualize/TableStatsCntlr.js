import {flux} from '../Firefly.js';

import {has, omit} from 'lodash';

import {updateSet, updateMerge} from '../util/WebUtil.js';
import ColValuesStatistics from './ColValuesStatistics.js';

import * as TableUtil from '../tables/TableUtil.js';

import * as TablesCntlr from '../tables/TablesCntlr.js';

/*
 Possible structure of store:
 /tblstats
   tbl_id: Object - the name of this node matches table id
   {
     isColStatsReady: boolean
     colStats: [ColValuesStatistics]
   }
 */


export const TBLSTATS_DATA_KEY = 'tblstats';
export const SETUP_TBL_TRACKING = `${TBLSTATS_DATA_KEY}/SETUP_TBL_TRACKING`;
export const LOAD_TBL_STATS = `${TBLSTATS_DATA_KEY}/LOAD_TBL_STATS`;
export const UPDATE_TBL_STATS = `${TBLSTATS_DATA_KEY}/UPDATE_TBL_STATS`;

/*
 * Set up store, which will reflect the data relevant to the given table
 * @param {string} tblId - table id
 */
export const dispatchSetupTblTracking = function(tblId) {
    flux.process({type: SETUP_TBL_TRACKING, payload: {tblId}});
};

/*
 * Get the number of points, min and max values, units and description for each table column
 * @param {ServerRequest} searchRequest - table search request
 */
export const dispatchLoadTblStats = function(searchRequest) {
    flux.process({type: LOAD_TBL_STATS, payload: {searchRequest}});
};

/*
 * The statistics is successfully returned from the server, update the store
 * @param {Number} tblId - table id
 * @param {boolean} isColStatsReady flags that column statistics is now available
 * @param {ColValuesStatistics[]} an array which holds column statistics for each column
 */
const dispatchUpdateTblStats = function(tblId,isColStatsReady,colStats) {
    flux.process({type: UPDATE_TBL_STATS, payload: {tblId,isColStatsReady,colStats}});
};

/*
 * @param rawAction (its payload should contain searchRequest to get source table)
 * @returns function which loads statistics (column name, num. values, range of values) for a source table
 */
export const loadTblStats = function(rawAction) {
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


export function reducer(state=getInitState(), action={}) {
    switch (action.type) {
        case (SETUP_TBL_TRACKING) :
        {
            const {tblId} = action.payload;
            return updateSet(state, tblId, {isColStatsReady: false});
        }
        case (TablesCntlr.TABLE_REMOVE)  :
        {
            const {tbl_id} = action.payload;
            if (has(state, tbl_id)) {
                const newState = Object.assign({}, state);
                Reflect.deleteProperty(newState, tbl_id);
                return newState;
            }
            return state;
        }
        case (LOAD_TBL_STATS)  :
        {
            return updateSet(state, action.payload.tblId, {isColStatsReady: false});
        }
        case (UPDATE_TBL_STATS)  :
        {
            const {tblId, isColStatsReady, colStats} = action.payload;
            return updateMerge(state, tblId, {isColStatsReady, colStats});
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

    const {tbl_id} = activeTableServerRequest;

    // searchRequest
    const sreq = Object.assign({}, omit(activeTableServerRequest, ['tbl_id', 'META_INFO']),
        {'startIdx': 0, 'pageSize': 1000000});

    const req = TableUtil.makeTblRequest('StatisticsProcessor', null,
                            { searchRequest: JSON.stringify(sreq) },
                            'tblstats-'+tbl_id);

    TableUtil.doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                const colStats = tableModel.tableData.data.reduce((colstats, arow) => {
                    colstats.push(new ColValuesStatistics(...arow));
                    return colstats;
                }, []);
                dispatch(updateTblStats(
                    {
                        tblId: tbl_id,
                        isColStatsReady: true,
                        colStats
                    }));
            }
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch table statistics: ${reason}`);
        }
    );
}