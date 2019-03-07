import {logError} from '../../util/WebUtil.js';
import {makeFileRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {doFetchTable, getColumnIdx, sortTableData} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';

const qFragment = '/sync?REQUEST=doQuery&LANG=ADQL&';
export const HeaderFont={fontSize: 12, fontWeight: 'bold', alignItems: 'center'};

export const  MJD = 'mjd';
export const  ISO = 'iso';

const tapBrowserComponentKey = 'TAP_BROWSER';

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

export function getColumnsTblId(tableName) {
    // table name is unique across schemas
    return `${tableName}-tapCols`;
}

export function loadTapSchemas(serviceUrl) {

    const url = serviceUrl + qFragment + 'QUERY=SELECT+*+FROM+TAP_SCHEMA.schemas';
    const request = makeFileRequest('schemas', url, null, {pageSize: MAX_ROW});

    return doFetchTable(request).then((tableModel) => {
        if (tableModel.error) {
            tableModel.error = `Failed to get schemas for ${serviceUrl}`, `${tableModel.error}`;
            logError(tableModel.error);
        } else if (tableModel.tableData) {
            // check if schema_index column is present
            // if it is, sort tabledata by schema_index
            if (getColumnIdx(tableModel, 'schema_index') >= 0) {
                sortTableData(tableModel.tableData.data, tableModel.tableData.columns, sortInfoString('schema_index'));
            }
        } else {
            tableModel.error = 'No schemas available';
        }
        return tableModel;

    }).catch((reason) => {
        const error = `Failed to get schemas for ${serviceUrl}: ${reason}`;
        logError(error);
        return {error};
    });
}

export function loadTapTables(serviceUrl, schemaName) {

    const url = serviceUrl + qFragment + 'QUERY=SELECT+*+FROM+TAP_SCHEMA.tables+WHERE+schema_name+like+\'' + schemaName + '\'';
    const request = makeFileRequest('tables', url, null, {pageSize: MAX_ROW});

    return doFetchTable(request).then((tableModel) => {
        if (tableModel.error) {
            tableModel.error = `Failed to get tables for ${serviceUrl} schema ${schemaName}`, `${tableModel.error}`;
            logError(tableModel.error);
        } else if (tableModel.tableData) {
            // check if table_index column is present
            // if it is, sort tabledata by table_index
            if (getColumnIdx(tableModel, 'table_index') >= 0) {
                sortTableData(tableModel.tableData.data, tableModel.tableData.columns, sortInfoString('table_index'));
            }
        } else {
            tableModel.error = `No tables available for ${serviceUrl} schema ${schemaName}`;
            logError(tableModel.error);
        }
        return tableModel;

    }).catch((reason) => {
        const error = `Failed to get tables for ${serviceUrl} schema ${schemaName}: ${reason}`;
        logError(error);
        return {error};
    });
}


export function loadTapColumns(serviceUrl, schemaName, tableName) {

    const url = serviceUrl + qFragment +
        'QUERY=SELECT+*+FROM+TAP_SCHEMA.columns+WHERE+table_name+like+\'' + tableName + '\'';
        // 'QUERY=SELECT+column_name,description,unit,datatype,ucd,utype,principal+' +
        // 'FROM+TAP_SCHEMA.columns+WHERE+table_name+like+\'' + tableName + '\'';

    const request = makeFileRequest('columns', url, undefined, {tbl_id: getColumnsTblId(tableName), pageSize: MAX_ROW});

    return doFetchTable(request).then((tableModel) => {
        if (tableModel.error) {
            tableModel.error = `Failed to get columns for ${serviceUrl} schema ${schemaName}`, `${tableModel.error}`;
            logError(tableModel.error);
        } else if (tableModel.tableData) {
            // check if column_index column is present
            // if it is, sort tabledata by column_index
            if (getColumnIdx(tableModel, 'column_index') >= 0) {
                sortTableData(tableModel.tableData.data, tableModel.tableData.columns, sortInfoString('column_index'));
            }
        } else {
            tableModel.error = `No columns available for ${serviceUrl} schema ${schemaName} table ${tableName}`;
            logError(tableModel.error);
        }
        return tableModel;

    }).catch((reason) => {
        const error = `Failed to get columns for ${serviceUrl} schema ${schemaName} table ${tableName}: ${reason}`;
        logError(error);
        return {error};
    });
}

