import {get} from 'lodash';
import {Logger} from '../../util/Logger.js';
import {makeFileRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {doFetchTable, getColumnIdx, sortTableData} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {getProp, hashCode} from '../../util/WebUtil.js';

const logger = Logger('TapUtil');
const qFragment = '/sync?REQUEST=doQuery&LANG=ADQL&';
export const HeaderFont={fontSize: 12, fontWeight: 'bold', alignItems: 'center'};

export const  MJD = 'mjd';
export const  ISO = 'iso';

const tapBrowserComponentKey = 'TAP_BROWSER';

export function getMaxrecHardLimit() {
    const defaultValue = Number.parseInt(getProp('tap.maxrec.hardlimit'));
    if (Number.isNaN(defaultValue)) {
        return 5000000;
    } else {
        return defaultValue;
    }
}

export function getTapBrowserState() {
    const tapBrowserState = getComponentState(tapBrowserComponentKey);
    const {serviceUrl, schemaOptions, schemaName, tableOptions, tableName, columnsModel} = tapBrowserState || {};
    return {serviceUrl, schemaOptions, schemaName, tableOptions, tableName, columnsModel};
}

export function setTapBrowserState({serviceUrl, schemaOptions=undefined, schemaName=undefined,
                                       tableOptions=undefined, tableName=undefined, columnsModel=undefined}) {
    dispatchComponentStateChange(tapBrowserComponentKey,
        {serviceUrl, schemaOptions, schemaName, tableOptions, tableName, columnsModel});
}

export function updateTapBrowserState(updates) {
    const tapBrowserState = getComponentState(tapBrowserComponentKey);
    return Object.assign({}, tapBrowserState, updates);
}

export function getColumnsTblId(serviceUrl, tableName) {
    // table name is unique across schemas
    return `${tableName}-tapCols-${hashCode(serviceUrl)}`;
}

export function loadTapSchemas(serviceUrl) {

    const url = serviceUrl + qFragment + 'QUERY=SELECT+*+FROM+TAP_SCHEMA.schemas';
    const request = makeFileRequest('schemas', url, null, {pageSize: MAX_ROW});

    return doFetchTable(request).then((tableModel) => {
        if (tableModel.error) {
            tableModel.error = `Failed to get schemas for ${serviceUrl}: ${tableModel.error}`;
            logger.error(tableModel.error);
        } else if (tableModel.tableData) {
            // check if schema_index column is present
            // if it is, sort tabledata by schema_index
            if (getColumnIdx(tableModel, 'schema_index') >= 0) {
                sortTableData(tableModel.tableData.data, tableModel.tableData.columns, sortInfoString('schema_index'));
            }
            if (getColumnIdx(tableModel, 'schema_name') < 0) {
                tableModel.error = 'Invalid schemas table';
            }
        } else {
            tableModel.error = 'No schemas available';
        }
        return tableModel;

    }).catch((reason) => {
        const message = get(reason, 'message', reason);
        const error = `Failed to get schemas for ${serviceUrl}: ${message}`;
        logger.error(error);
        return {error};
    });
}

export function loadTapTables(serviceUrl, schemaName) {

    const url = serviceUrl + qFragment + 'QUERY=SELECT+*+FROM+TAP_SCHEMA.tables+WHERE+schema_name+like+\'' + schemaName + '\'';
    const request = makeFileRequest('tables', url, null, {pageSize: MAX_ROW});

    return doFetchTable(request).then((tableModel) => {
        if (tableModel.error) {
            tableModel.error = `Failed to get tables for ${serviceUrl} schema ${schemaName}: ${tableModel.error}`;
            logger.error(tableModel.error);
        } else if (tableModel.tableData) {
            // check if table_index column is present
            // if it is, sort tabledata by table_index
            if (getColumnIdx(tableModel, 'table_index') >= 0) {
                sortTableData(tableModel.tableData.data, tableModel.tableData.columns, sortInfoString('table_index'));
            }
            if (getColumnIdx(tableModel, 'table_name') < 0) {
                tableModel.error = `Invalid tables returned for ${serviceUrl} schema ${schemaName}`;
                logger.error(tableModel.error);
            }
        } else {
            tableModel.error = `No tables available for ${serviceUrl} schema ${schemaName}`;
            logger.error(tableModel.error);
        }
        return tableModel;

    }).catch((reason) => {
        const message = get(reason, 'message', reason);
        const error = `Failed to get tables for ${serviceUrl} schema ${schemaName}: ${message}`;
        logger.error(error);
        return {error};
    });
}


export function loadTapColumns(serviceUrl, schemaName, tableName) {

    const url = serviceUrl + qFragment +
        'QUERY=SELECT+*+FROM+TAP_SCHEMA.columns+WHERE+table_name+like+\'' + tableName + '\'';
        // 'QUERY=SELECT+column_name,description,unit,datatype,ucd,utype,principal+' +
        // 'FROM+TAP_SCHEMA.columns+WHERE+table_name+like+\'' + tableName + '\'';

    const request = makeFileRequest('columns', url, undefined, {tbl_id: getColumnsTblId(serviceUrl, tableName), pageSize: MAX_ROW});

    return doFetchTable(request).then((tableModel) => {
        if (tableModel.error) {
            tableModel.error = `Failed to get columns for ${serviceUrl} schema ${schemaName}: ${tableModel.error}`;
            logger.error(tableModel.error);
        } else if (tableModel.tableData) {
            // check if column_index column is present
            // if it is, sort tabledata by column_index
            if (getColumnIdx(tableModel, 'column_index') >= 0) {
                sortTableData(tableModel.tableData.data, tableModel.tableData.columns, sortInfoString('column_index'));
            }
            if (getColumnIdx(tableModel, 'column_name') < 0) {
                tableModel.error = `Invalid columns table returned for ${serviceUrl} schema ${schemaName} table ${tableName}`;
                logger.error(tableModel.error);
            }
        } else {
            tableModel.error = `No columns available for ${serviceUrl} schema ${schemaName} table ${tableName}`;
            logger.error(tableModel.error);
        }
        return tableModel;

    }).catch((reason) => {
        const message = get(reason, 'message', reason);
        const error = `Failed to get columns for ${serviceUrl} schema ${schemaName} table ${tableName}: ${message}`;
        logger.error(error);
        return {error};
    });
}

/**
 * Get a value of the column attribute using TAP_SCHEMA.columns table
 * @param columnsModel - table model of columns table
 * @param colName - column name
 * @param attrName - column name in the columnsModel table, ex. 'ucd', 'datatype'
 */
export function getColumnAttribute(columnsModel, colName, attrName) {

    const nameIdx = getColumnIdx(columnsModel, 'column_name');
    const attrIdx = getColumnIdx(columnsModel, attrName);
    if (nameIdx < 0 || attrIdx < 0) {
        return;
    }

    const targetRow = get(columnsModel, ['tableData', 'data'], []).find((oneRow) => {
        return (oneRow[nameIdx] === colName);
    });

    if (!targetRow) {
        return;
    }

    return targetRow[attrIdx];
}

