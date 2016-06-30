/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, set, isEmpty, uniqueId, cloneDeep, omit, omitBy, isNil, isPlainObject, isArray} from 'lodash';
import * as TblCntlr from './TablesCntlr.js';
import {SortInfo, SORT_ASC, UNSORTED} from './SortInfo.js';
import {flux} from '../Firefly.js';
import {fetchUrl, encodeServerUrl, encodeParams} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';

export const SEARCH_SRV_PATH = getRootURL() + 'search/json';
const SAVE_TABLE_URL = getRootURL() + 'servlet/SaveAsIpacTable';
const INT_MAX = Math.pow(2,31) - 1;

/*----------------------------< creator functions ----------------------------*/

/**
 * Table options.  All of the options are optional.  These options let you control
 * what data and how it will be returned from the request.
 * @typedef {object} TblReqOptions
 * @prop {number} startIdx  the starting index to fetch.  defaults to zero.
 * @prop {number} pageSize  the number of rows per page.  defaults to 100.
 * @prop {string} filters   list of conditions separted by comma(,). Format:  (col_name|index) operator value.
 *                  operator is one of '> < = ! >= <= IN'.  See DataGroupQueryStatement.java doc for more details.
 * @prop {string} sortInfo  sort information.  Format:  (ASC|DESC),col_name[,col_name]*
 * @prop {string} inclCols  list of columns to select.  Column names separted by comma(,)
 * @prop {string} decimate  decimation information.
 * @prop {object} META_INFO meta information passed as key/value pair to server then returned as tableMeta.
 * @prop {string} use       one of 'catalog_overlay', 'catalog_primary', 'data_primary'.
 * @prop {string} tbl_id    a unique id of a table. auto-create if not given.
 */

/**
 * Creates a table request object for the given id.
 * @param {string} id       required.  SearchProcessor ID.
 * @param {string} [title]  title to display with this table.
 * @param {object} [params] the parameters to include with this request.
 * @param {TblReqOptions} [options] more options.  see TblReqOptions for details.
 * @returns {*}
 */
export function makeTblRequest(id, title, params={}, options={}) {
    var req = {startIdx: 0, pageSize: 100};
    title = title || id;
    const tbl_id = options.tbl_id || uniqueTblId();
    var META_INFO = Object.assign(options.META_INFO || {}, {title, tbl_id});
    options = omit(options, 'tbl_id');
    return omitBy(Object.assign(req, options, params, {META_INFO, tbl_id, id}), isNil);
}

/**
 * @param {string} [title]      title to display with this table.
 * @param {string} source       required; location of the ipac table. url or file path.
 * @param {string} [alt_source] use this if source does not exists.
 * @param {TblReqOptions} [options]  more options.  see TblReqOptions for details.
 * @returns {object}
 */
export function makeFileRequest(title, source, alt_source, options={}) {
    const id = 'IpacTableFromSource';
    var req = {startIdx: 0, pageSize: 100};
    title = title || source;
    const tbl_id = options.tbl_id || uniqueTblId();
    options = omit(options, 'tbl_id');
    var META_INFO = Object.assign(options.META_INFO || {}, {title, tbl_id});
    return omitBy(Object.assign(req, options, {source, alt_source, META_INFO, tbl_id, id}), isNil);
}


/**
 * Parameters for cone search
 * @typedef {object} ConeParams
 * @prop {string} SearchMethod  'Cone'.
 * @prop {string} position  name or coordinates of the search
 * @prop {string} radius    radius of the search in arcsec
 */

/**
 * Parameters for eliptical search
 * @typedef {object} ElipParams
 * @prop {string} SearchMethod  'Eliptical'.
 * @prop {string} position  name or coordinates of the search
 * @prop {string} radius    radius of the search in arcsec
 * @prop {string} radunits  the units for the radius or side, must be arcsec,arcmin,degree, default arcsec
 * @prop {string} ratio     ratio for elliptical request
 * @prop {string} posang    pa for elliptical request
 */

/**
 * Parameters for box search
 * @typedef {object} BoxParams
 * @prop {string} SearchMethod 'Eliptical'.
 * @prop {string} position  name or coordinates of the search
 * @prop {string} size      the length of a side for a box search
 */

/**
 * creates the request to query IRSA catalogs.
 * @param {string} title    title to be displayed with this table result
 * @param {string} project
 * @param {string} catalog  the catalog name to search
 * @param {(ConeParams|BoxParams|ElipParams)} params   one of 'Cone','Eliptical','Box','Polygon','Table','AllSky'.
 * @param {TblReqOptions} [options]
 * @returns {object}
 */
export function makeIrsaCatalogRequest(title, project, catalog, params={}, options={}) {
    var req = {startIdx: 0, pageSize: 100};
    title = title || catalog;
    options.use = options.use || 'catalog_overlay';
    const tbl_id = options.tbl_id || uniqueTblId();
    const id = 'GatorQuery';
    const UserTargetWorldPt = params.UserTargetWorldPt || params.position;  // may need to convert to worldpt.
    const catalogProject = project;
    var META_INFO = Object.assign(options.META_INFO || {}, {title, tbl_id});

    options = omit(options, 'tbl_id');
    params = omit(params, 'position');

    return omitBy(Object.assign(req, options, params, {id, tbl_id, META_INFO, UserTargetWorldPt, catalogProject, catalog}), isNil);
}

export function makeVOCatalogRequest(title, params={}, options={}) {
    var req = {startIdx: 0, pageSize: 100};
    options.use = options.use || 'catalog_overlay';
    const tbl_id = options.tbl_id || uniqueTblId();
    const id = 'ConeSearchByURL';
    const UserTargetWorldPt = params.UserTargetWorldPt || params.position;  // may need to convert to worldpt.
    var META_INFO = Object.assign(options.META_INFO || {}, {title, tbl_id});

    options = omit(options, 'tbl_id');
    params = omit(params, 'position');

    return omitBy(Object.assign(req, options, params, {id, tbl_id, META_INFO, UserTargetWorldPt}), isNil);
}

/*---------------------------- creator functions >----------------------------*/


/**
 *
 * @param tableRequest is a table request params object
 * @param hlRowIdx set the highlightedRow.  default to startIdx.
 * @returns {Promise.<T>}
 */
export function doFetchTable(tableRequest, hlRowIdx) {

    const def = {
        startIdx: 0,
        pageSize : INT_MAX,
    };
    var params = Object.assign(def, tableRequest);
    // encoding for method post
    if (!isEmpty(params.META_INFO)) {
         params.META_INFO = encodeParams(params.META_INFO);
    }

    return fetchUrl(SEARCH_SRV_PATH, {method: 'post', params}).then( (response) => {
        return response.json().then( (tableModel) => {
            const startIdx = get(tableModel, 'request.startIdx', 0);
            if (startIdx > 0) {
                // shift data arrays indices to match partial fetch
                tableModel.tableData.data = tableModel.tableData.data.reduce( (nAry, v, idx) => {
                    nAry[idx+startIdx] = v;
                    return nAry;
                }, []);
            }
            tableModel.highlightedRow = hlRowIdx || startIdx;
            return tableModel;
        });
    });
}

export function doValidate(type, action) {
    if (type !== action.type) {
        error(action, `Incorrect type:${action.type} was sent to a ${type} actionCreator.`);
    }
    if (!action.payload) {
        error(action, 'Invalid action.  Payload is missing.');
    }
    var {request} = action.payload;
    if (type === TblCntlr.TABLE_FETCH ) {
        if (request.id) {
            error(action, 'Required "id" field is missing.');
        }
        if (request.tbl_id) {
            error(action, 'Required "tbl_id" field is missing.');
        }
    } else if(type === TblCntlr.TABLE_HIGHLIGHT) {
        const idx = action.payload.highlightedRow;
        if (!idx || idx<0) {
            error(action, 'highlightedRow must be a positive number.');
        }
    }
    return action;
}

/**
 * update the given action with a new error given by cause.
 * action.err is stored as an array of errors.  Errors may be a String or an Error type.
 * @param action  the actoin to update
 * @param cause  the error to be added.
 */
export function error(action, cause) {
    (action.err = action.err || []).push(cause);
}

/**
 * return true is there is data within the given range.  this is needed because
 * of paging table not loading the full table.
 * @param startIdx
 * @param endIdx
 * @param tableModel
 * @returns {boolean}
 */
export function isTblDataAvail(startIdx, endIdx, tableModel) {
    if (!tableModel) return false;
    endIdx =  endIdx >0 ? Math.min( endIdx, tableModel.totalRows) : startIdx;
    if (startIdx >=0 && endIdx > startIdx) {
        const data = get(tableModel, 'tableData.data', []);
        const dataCount = Object.keys(data.slice(startIdx, endIdx)).length;
        return dataCount === (endIdx-startIdx);
    } else return false;
}


export function getTblById(id) {
    return get(flux.getState(),[TblCntlr.TABLE_SPACE_PATH, 'data', id]);
}

/**
 * returns the group information
 * @param {string} tbl_group    the group to look for
 * @returns {Object}
 */
export function getTableGroup(tbl_group='main') {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'results', tbl_group]);
}

/**
 * returns the table information for the given id and group.
 * @param tbl_id
 * @param tbl_group
 * @returns {Object}
 */
export function getTableInGroup(tbl_id, tbl_group='main') {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'results', tbl_group, 'tables',  tbl_id]);
}

/**
 * get the table working state by tbl_ui_id
 * @param tbl_ui_id
 * @returns {*}
 */
export function getTableUiById(tbl_ui_id) {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'ui', tbl_ui_id]);
}

/**
 * get table's expanded information.
 * @returns {object}
 */
export function getTblExpandedInfo() {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'ui', 'expanded'], {});
}

/**
 * return true if the table referenced by the given tbl_id is fully loaded.
 * @param tbl_id
 * @returns {boolean}
 */
export function isFullyLoaded(tbl_id) {
    return isTableLoaded(getTblById(tbl_id));
}

export function getColumnIdx(tableModel, colName) {
    const cols = get(tableModel, 'tableData.columns', []);
    return cols.findIndex((col) => {
        return col.name === colName;
    });
}

export function getActiveTableId(tbl_group='main') {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH,'results',tbl_group,'active']);
}

/**
 *
 * @param tableModel
 * @param rowIdx
 * @param colName
 * @return {*}
 */
export function getCellValue(tableModel, rowIdx, colName) {
    if (get(tableModel, 'tableData.data.length', 0) > 0) {
        const colIdx = getColumnIdx(tableModel, colName);
        // might be undefined if row is not loaded
        return get(tableModel.tableData.data, [rowIdx, colIdx]);
    }
}


/**
 * return true if the given table is fully loaded.
 * @param tableModel
 * @returns {boolean}
 */
export function isTableLoaded(tableModel) {
    const status = tableModel && !tableModel.isFetching && get(tableModel, 'tableMeta.Loading-Status', 'COMPLETED');
    return status === 'COMPLETED';
}

/**
 * This function transform the json data from the server to fit the need of the UI.
 * For instance, the column's name is repeated after transform.  This is good for the UI.
 * But, it's more efficient to not include it during data transfer from the server.
 * @param tableModel
 * @returns {*}
 */
export function transform(tableModel) {

    if (tableModel.tableData && tableModel.tableData.data) {
        const cols = tableModel.tableData.columns;
        // change row data from [ [val] ] to [ {cname:val} ]
        tableModel.tableData.data = tableModel.tableData.data.map( (row) => {
            return cols.reduce( (nrow, col, cidx) => {
                nrow[col.name] = row[cidx];
                return nrow;
            }, {});
        });
    }
}

/**
 * This function merges the source object into the target object
 * by traversing and comparing every like path.  If a value was
 * merged at any data node in the data graph, the node and all of its
 * parent nodes will be shallow cloned and returned.  Otherwise, the target's value
 * will be returned.
 * @param target
 * @param source
 * @returns {*}
 */
export function smartMerge(target, source) {
    if (!target) return source;

    if (isPlainObject(source)) {
        const objChanges = {};
        Object.keys(source).forEach((k) => {
            const nval = smartMerge(target[k], source[k]);
            if (nval !== target[k]) {
                objChanges[k] = nval;
            }
        });
        return (isEmpty(objChanges)) ? target : Object.assign({}, target, objChanges);
    } else if (isArray(source)){
        const aryChanges = [];
        source.forEach((v, idx) => {
            const nval = smartMerge(target[idx], source[idx]);
            if (nval !== target[idx]) {
                aryChanges[idx] = nval;
            }
        });
        if (isEmpty(aryChanges)) return target;
        else {
            const nAry = target.slice();
            aryChanges.forEach((v, idx) => nAry[idx] = v);
            return nAry;
        }
    } else {
        return (target === source) ? target : source;
    }
}

/**
 * sort the given tableModel based on the given request
 * @param origTableModel original table model.  this is returned when direction is UNSORTED.
 * @param sortInfoStr
 */
export function sortTable(origTableModel, sortInfoStr) {
    var tableModel = cloneDeep(origTableModel);
    set(tableModel, 'request.sortInfo', sortInfoStr);
    const {data, columns} = tableModel.tableData;
    sortTableData(data, columns, sortInfoStr);
    return tableModel;
}

/**
 * sort table data in place.
 * @param tableData
 * @param columns
 * @param sortInfoStr
 * @returns {*}
 */
export function sortTableData(tableData, columns, sortInfoStr) {
    const sortInfoCls = SortInfo.parse(sortInfoStr);
    const colName = get(sortInfoCls, 'sortColumns.0');
    const dir = get(sortInfoCls, 'direction', UNSORTED);
    if (dir === UNSORTED || get(tableData, 'length', 0) === 0) return tableData;

    const multiplier = dir === SORT_ASC ? 1 : -1;
    const colIdx = columns.findIndex( (col) => {return col.name === colName;} );
    const col = columns[colIdx];
    if (!col) return tableData;

    var comparator;
    if (!col.type || ['char', 'c'].includes(col.type) ) {
        comparator = (r1, r2) => {
            const [s1, s2] = [r1[colIdx], r2[colIdx]];
            return multiplier * (s1 > s2 ? 1 : -1);
        };
    } else {
        comparator = (r1, r2) => {
            const [v1, v2] = [r1[colIdx], r2[colIdx]];
            return multiplier * (Number(v1) - Number(v2));
        };
    }
    tableData.sort(comparator);
    return tableData;
}

export function getTblInfoById(tbl_id, aPageSize) {
    const tableModel = getTblById(tbl_id);
    return getTblInfo(tableModel, aPageSize);
}

/**
 * collects all available table information given the tableModel.
 * @param tableModel
 * @param aPageSize  use this pageSize instead of the one in the request.
 * @returns {*}
 */
export function getTblInfo(tableModel, aPageSize) {
    if (!tableModel) return {};
    var {tbl_id, request, highlightedRow=0, totalRows=0, tableMeta={}, selectInfo, error} = tableModel;
    const {title} = tableMeta;
    const pageSize = aPageSize || get(request, 'pageSize', 1);  // there should be a pageSize.. default to 1 in case of error.  pageSize cannot be 0 because it'll overflow.
    if (highlightedRow < 0 ) {
        highlightedRow = 0;
    } else  if (highlightedRow >= totalRows-1) {
        highlightedRow = totalRows-1;
    }
    const currentPage = highlightedRow >= 0 ? Math.floor(highlightedRow / pageSize)+1 : 1;
    const hlRowIdx = highlightedRow >= 0 ? highlightedRow % pageSize : 0;
    const startIdx = (currentPage-1) * pageSize;
    const endIdx = Math.min(startIdx+pageSize, totalRows) || startIdx ;
    var totalPages = Math.ceil((totalRows || 0)/pageSize);
    return { tableModel, tbl_id, title, totalRows, request, startIdx, endIdx, hlRowIdx, currentPage, pageSize,totalPages, highlightedRow, selectInfo, error};
}

/**
 *
 * @param columns
 * @param request
 * @param filename
 * @returns {encoded}
 */
export function getTableSourceUrl(columns, request, filename) {
    const Request = cloneDeep(request);
    const visiCols = columns.filter( (col) => {
                return isNil(col) || col.visibility === 'show';
            }).map( (col) => {
                return col.name;
            } );
    if (visiCols.length !== columns.length) {
        request['inclCols'] = visiCols.toString();
    }
    Request.startIdx = 0;
    Request.pageSize = Number.MAX_SAFE_INTEGER;
    Reflect.deleteProperty(Request, 'tbl_id');
    const file_name = filename || Request.file_name;
    return encodeServerUrl(SAVE_TABLE_URL, {file_name, Request: request});
}

/**
 * returns a map of cname -> width.  The width is the number of characters needed to display
 * the header and the data as a table given columns and dataAry.
 * @param columns  array of column object
 * @param dataAry  array of array.
 * @returns {Object} a map of cname -> width
 */
export function calcColumnWidths(columns, dataAry) {
    return columns.reduce( (pv, cv, idx) => {
        const cname = cv.name;
        var width = Math.max(cname.length, get(cv, 'units.length', 0));
        width = dataAry.reduce( (maxWidth, row) => {
            return Math.max(maxWidth, get(row, [idx, 'length'], 0));
        }, width);  // max width of data
        pv[cname] = width;
        return pv;
    }, {ROWID: 8});
}

export function uniqueTblId() {
    const id = uniqueId('tbl_id-');
    if (getTblById(id)) {
        return uniqueTblId();
    } else {
        return id;
    }
}

export function uniqueTblUiId() {
    return uniqueId('tbl_ui_id-');
}