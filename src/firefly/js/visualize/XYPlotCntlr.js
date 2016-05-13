/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {flux} from '../Firefly.js';

import {updateSet} from '../util/WebUtil.js';
import {get, has, omit, omitBy, isUndefined, isString} from 'lodash';

import {doFetchTable, getTblById} from '../tables/TableUtil.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {serializeDecimateInfo} from '../tables/Decimate.js';
import {logError} from '../util/WebUtil.js';

export const XYPLOT_DATA_KEY = 'xyplot';
export const LOAD_PLOT_DATA = `${XYPLOT_DATA_KEY}/LOAD_COL_DATA`;
export const UPDATE_PLOT_DATA = `${XYPLOT_DATA_KEY}/UPDATE_COL_DATA`;
export const SET_SELECTION = `${XYPLOT_DATA_KEY}/SET_SELECTION`;
const SET_ZOOM = `${XYPLOT_DATA_KEY}/SET_ZOOM`;
const RESET_ZOOM = `${XYPLOT_DATA_KEY}/RESET_ZOOM`;

/*
 Possible structure of store:
  /xyplot
    tbl_id: Object - the name of this node matches table id
    {
         // tblXYPlotData
         isPlotDataReady: boolean
         decimatedUnzoomed: boolean // tells that unzoomed data are decimated
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
export function dispatchLoadPlotData(xyPlotParams, searchRequest) {
    flux.process({type: LOAD_PLOT_DATA, payload: {xyPlotParams, searchRequest}});
}

/*
 * Set selection to give user choice of actions on selection (zoom, filter, or select points)
 * @param {String} tblId - table id
 * @param {Object} selection - {xMin, xMax, yMin, yMax}
 */
export function dispatchSetSelection(tblId, selection) {
    flux.process({type: SET_SELECTION, payload: {tblId, selection}});
}

/*
 * Zoom XY plot to a given selection or reset zoom if no selection is given
 * @param {String} tblId - table id
 * @param {Object} selection - {xMin, xMax, yMin, yMax}
 */
export function dispatchZoom(tblId, selection) {
    const {xyPlotData, xyPlotParams, decimatedUnzoomed} = get(flux.getState(), [XYPLOT_DATA_KEY,tblId], {});
    if (xyPlotData && xyPlotParams) {
        if (selection) {
            // zoom to selection
            if (xyPlotData.decimateKey) {
                const tableModel = getTblById(tblId);
                if (tableModel) {
                    const paramsWithZoom = Object.assign({}, xyPlotParams, {zoom: xyPlotParams.selection});
                    dispatchLoadPlotData(paramsWithZoom, tableModel.request);
                }
            } else {
                dispatchSetZoom(tblId, selection);
            }
        } else {
            // reset zoom
            if (decimatedUnzoomed || isUndefined(decimatedUnzoomed)) {
                const tableModel = getTblById(tblId);
                if (tableModel) {
                    const paramsWithoutZoom = Object.assign({}, omit(xyPlotParams, 'zoom'));
                    dispatchLoadPlotData(paramsWithoutZoom, tableModel.request);
                }
            } else {
                dispatchResetZoom(tblId);
            }

        }
    }
}

function dispatchSetZoom(tblId, selection) {
    flux.process({type: SET_ZOOM, payload: {tblId, selection}});
}

function dispatchResetZoom(tblId) {
    flux.process({type: RESET_ZOOM, payload: {tblId}});
}


/*
 * @param rawAction (its payload should contain searchRequest to get source table and histogram parameters)
 * @returns function which loads plot data (x, y, rowIdx, etc.)
 */
export function loadPlotData (rawAction) {
    return (dispatch) => {
        const {searchRequest, xyPlotParams} = rawAction.payload;
        dispatch({ type : LOAD_PLOT_DATA, payload : rawAction.payload });
        if (searchRequest && xyPlotParams) {
            fetchPlotData(dispatch, searchRequest, xyPlotParams);
        }

    };
}

/**
 * The data is an object with
 * tblId - string, table id,
 * isPlotDataReady - boolean, flags that xy plot data are available
 * xyPlotData - an array of data rows
 * xyPlotParams - plot parameters
 * decimatedUnzoomed - tells if unzoomed data are decimated
 * @param data {Object} the data to merge with the xyplot branch
 * @returns {{type: string, payload: object}}
 */
function updatePlotData(data) {
    return { type : UPDATE_PLOT_DATA, payload: data };
}

export function reducer(state={}, action={}) {
    switch (action.type) {
        case (TablesCntlr.TABLE_REMOVE)  :
        {
            const tbl_id = action.payload.tbl_id;
            if (has(state, tbl_id)) {
                const newState = Object.assign({}, state);
                Reflect.deleteProperty(newState, tbl_id);
                return newState;
            }
            return state;
        }
        case (LOAD_PLOT_DATA)  :
        {
            const {xyPlotParams, searchRequest} = action.payload;
            const tblId = searchRequest.tbl_id;
            return updateSet(state, tblId,
                { isPlotDataReady: false, xyPlotParams, decimatedUnzoomed: get(state, [tblId,'decimatedUnzoomed'])});
        }
        case (UPDATE_PLOT_DATA)  :
        {
            const {isPlotDataReady, decimatedUnzoomed, xyPlotData, tblId, xyPlotParams} = action.payload;
            if (state[tblId].xyPlotParams === xyPlotParams) {
                const decimatedUnzoomedNext = isUndefined(decimatedUnzoomed) ? state[tblId].decimatedUnzoomed : decimatedUnzoomed;
                const newParams = xyPlotParams.selection ?  updateSet(xyPlotParams, 'selection', undefined) : xyPlotParams;
                return updateSet(state, tblId,
                    {isPlotDataReady, decimatedUnzoomed: decimatedUnzoomedNext, xyPlotData, xyPlotParams: newParams});
            }
            return state;
        }
        case (SET_SELECTION) :
        {
            const {tblId, selection} = action.payload;
            return updateSet(state, [tblId,'xyPlotParams','selection'], selection);
        }
        case (SET_ZOOM) :
        {
            const {tblId, selection} = action.payload;
            const newState = updateSet(state, [tblId,'xyPlotParams','zoom'], selection);
            Reflect.deleteProperty(newState[tblId].xyPlotParams, 'selection');
            return newState;
        }
        case (RESET_ZOOM) :
        {
            const tblId = action.payload.tblId;
            const newParams = Object.assign({}, omit(state[tblId].xyPlotParams, ['selection', 'zoom']));
            return updateSet(state, [tblId,'xyPlotParams'], newParams);
        }
        case (TablesCntlr.TABLE_SELECT) :
        {
            const tbl_id = action.payload.tbl_id; //also has selectInfo
            if (has(state, [tbl_id,'xyPlotParams','selection'])) {
                return updateSet(state, [tbl_id,'xyPlotParams','selection'], undefined);
            }
            return state;
        }
        default:
            return state;
    }
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

    let limits = [];
    if (xyPlotParams.zoom) {
        const {xMin, xMax, yMin, yMax}  = xyPlotParams.zoom;
        limits = [xMin, xMax, yMin, yMax];
    }

    const req = Object.assign({}, omit(activeTableServerRequest, ['tbl_id', 'META_INFO']), {
        'startIdx' : 0,
        'pageSize' : 1000000,
        //'inclCols' : `${xyPlotParams.x.columnOrExpr},${xyPlotParams.y.columnOrExpr}`, // ignored if 'decimate' is present
        'decimate' : serializeDecimateInfo(xyPlotParams.x.columnOrExpr, xyPlotParams.y.columnOrExpr, 10000, 1.0, ...limits)
    });

    const tblId = activeTableServerRequest.tbl_id;
    req.tbl_id = 'xyplot-'+tblId;


    doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                const {tableMeta} = tableModel;
                const xyPlotData = omitBy({
                    rows: tableModel.tableData.data,
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
                        // when zoomed, we don't know if the unzoomed data are decimated or not
                        decimatedUnzoomed: Boolean(tableMeta['decimate_key']) || (xyPlotParams.zoom ? undefined : false),
                        xyPlotParams,
                        xyPlotData,
                        tblId
                    }));
            }
        }
    ).catch(
        (reason) => {
            logError(`Failed to fetch XY plot data: ${reason}`);
        }
    );

}


