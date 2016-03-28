import {flux} from '../Firefly.js';

import update from 'react-addons-update';
import {has, omitBy, isUndefined, isString} from 'lodash';


import {doFetchTable, isTableLoaded, findTblById} from '../tables/TableUtil.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';

import {serializeDecimateInfo} from '../tables/Decimate.js';

export const XYPLOT_DATA_KEY = 'xyplot';
export const LOAD_PLOT_DATA = `${XYPLOT_DATA_KEY}/LOAD_COL_DATA`;
export const UPDATE_PLOT_DATA = `${XYPLOT_DATA_KEY}/UPDATE_COL_DATA`;
export const SET_SELECTION = `${XYPLOT_DATA_KEY}/SET_SELECTION`;
export const SET_ZOOM = `${XYPLOT_DATA_KEY}/SET_ZOOM`;
export const RESET_ZOOM = `${XYPLOT_DATA_KEY}/RESET_ZOOM`;

/*
 Possible structure of store:
  /xyplot
    tbl_id: Object - the name of this node matches table id
    {
         // tblXYPlotData
         isPlotDataReady: boolean
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
           selection: {xMin, xMax, yMin, yMax} // currently selected rectangle
           zoom: {xMin, xMax, yMin, yMax} // currently zoomed rectangle
           stretch: string (fit|fill)
           x: {
                columnOrExpr
                label
                unit
                options: [grid,log,flip]
                nbins
                min
                max
              }
           y: {
                columnOrExpr
                label
                unit
                options: [grid,log,flip]
                nbins
                min
                max
           }
     }
 */

/*
 * Load xy plot data
 * @param {Object} xyPlotParams - XY plot options (column names, etc.)
 * @param {ServerRequest} searchRequest - table search request
 */
export const dispatchLoadPlotData = function(xyPlotParams, searchRequest) {
    flux.process({type: LOAD_PLOT_DATA, payload: {xyPlotParams, searchRequest}});
};

/*
 * Update xy plot data
 * @param {boolean} isPlotDataReady - flags that xy plot data are available
 * @param {Number[][]} xyPlotData - an array of the number arrays with rowIdx, x, y, [error]
 * @param {Object} xyPlotParams - XY plot options (column names, etc.)
 * @param {String} tblId - table id
 */
export const dispatchUpdatePlotData = function(isPlotDataReady, xyPlotData, xyPlotParams, tblId) {
    flux.process({type: UPDATE_PLOT_DATA, payload: {isPlotDataReady, xyPlotData, xyPlotParams, tblId}});
};

export const dispatchSetSelection = function(tblId, selection) {
    flux.process({type: SET_SELECTION, payload: {tblId, selection}});
};

export const dispatchSetZoom = function(tblId, selection) {
    flux.process({type: SET_ZOOM, payload: {tblId, selection}});
};

export const dispatchResetZoom = function(tblId) {
    flux.process({type: RESET_ZOOM, payload: {tblId}});
};


/*
 * @param rawAction (its payload should contain searchRequest to get source table and histogram parameters)
 * @returns function which loads plot data (x, y, rowIdx, etc.)
 */
export const loadPlotData = function(rawAction) {
    return (dispatch) => {
        dispatch({ type : LOAD_PLOT_DATA, payload : rawAction.payload });
        if (rawAction.payload.searchRequest && rawAction.payload.xyPlotParams) {
            fetchPlotData(dispatch, rawAction.payload.searchRequest, rawAction.payload.xyPlotParams);
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
        const tblData = state[tblId];
        const newTblData = Object.assign({}, tblData, newProps);
        const newState = Object.assign({}, state);
        newState[tblId] = newTblData;
        return newState;
    }
    return state;
}

export function reducer(state=getInitState(), action={}) {
    switch (action.type) {
        case (TablesCntlr.TABLE_NEW)  :
        case (TablesCntlr.TABLE_LOAD_STATUS)  :
        {
            // in both cases action.payload contains tableMeta, but request is not present in TABLE_LOAD_STATUS
            var {tbl_id, request} = action.payload;
            if (has(state, tbl_id)) {
                if (isTableLoaded(action.payload)) {
                    if (!request) {request = findTblById(tbl_id).request;}
                    // use xyPlotParams with cleared selection box
                    const prevXyPlotParams = state[tbl_id].xyPlotParams;
                    const xyPlotParamsNext = has(prevXyPlotParams, 'selection') ?
                        update(prevXyPlotParams, {selection: {$set: undefined}}) : prevXyPlotParams;
                    action.sideEffect((dispatch) => fetchPlotData(dispatch, request, xyPlotParamsNext));
                }
            }
            return state;
        }
        case (LOAD_PLOT_DATA)  :
        {
            const {searchRequest} = action.payload;
            const newState = Object.assign({}, state);
            newState[searchRequest.tbl_id]= {isPlotDataReady: false};
            return newState;
        }
        case (UPDATE_PLOT_DATA)  :
        {
            const {isPlotDataReady, xyPlotData, xyPlotParams, tblId} = action.payload;
            return stateWithNewData(tblId, state, {
                isPlotDataReady,
                xyPlotData,
                xyPlotParams
            });
        }
        case (SET_SELECTION) :
        {
            const {tblId, selection} = action.payload;
            return update(state, {[tblId] : {xyPlotParams: {selection: {$set: selection}}}});
        }
        case (SET_ZOOM) :
        {
            const {tblId, selection} = action.payload;
            return update(state,
                {[tblId] :
                    {xyPlotParams:
                        {
                            selection: {$set: undefined},
                            zoom: {$set: selection}
                         }
                    }});
        }
        case (RESET_ZOOM) :
        {
            return update(state,
                {[action.payload.tblId] :
                    {xyPlotParams:
                        {
                            selection: {$set: undefined},
                            zoom: {$set: undefined}
                        }
                    }});

        }
        case (TablesCntlr.TABLE_SELECT) :
        {
            tbl_id = action.payload.tbl_id; //also has selectInfo
            if (has(state, tbl_id)) {
                return update(state,
                    {
                        [tbl_id]: {
                            xyPlotParams: {
                                selection: {$set: undefined}
                            }
                        }
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
 * @param data {Object} the data to merge with the xyplot branch
 * @returns {{type: string, payload: object}}
 */
function updatePlotData(data) {
    return { type : UPDATE_PLOT_DATA, payload: data };
}

/**
 * fetches xy plot data
 * set isColStatsReady to true once done.
 * @param dispatch
 * @param activeTableServerRequest table search request to obtain source table
 * @param xyPlotParams object, which contains xy plot parameters

 */
function fetchPlotData(dispatch, activeTableServerRequest, xyPlotParams) {

    if (!xyPlotParams) { return; }

    // todo support expressions
    const req = Object.assign({}, activeTableServerRequest, {'startIdx' : 0, 'pageSize' : 1000000,
        'inclCols' : `${xyPlotParams.x.columnOrExpr},${xyPlotParams.y.columnOrExpr}`,
        'decimate' : serializeDecimateInfo(xyPlotParams.x.columnOrExpr, xyPlotParams.y.columnOrExpr, 10000)
        });

    const tblId = activeTableServerRequest.tbl_id;
    req.tbl_id = 'xyplot-'+tblId;

    doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                const {tableData, tableMeta} = tableModel;
                const xyPlotData = omitBy({
                    rows: tableData.data,
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

                dispatch(updatePlotData(
                    {
                        isPlotDataReady : true,
                        xyPlotParams,
                        xyPlotData,
                        tblId
                    }));
            }
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch XY plot data: ${reason}`);
        }
    );
}


