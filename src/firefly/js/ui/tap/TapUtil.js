import {Logger} from '../../util/Logger.js';
import {makeFileRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {doFetchTable, getColumnIdx, sortTableData} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {getProp, hashCode} from '../../util/WebUtil.js';
import {get, isUndefined} from 'lodash';

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

export const tapHelpId = (id) => `tapSearches.${id}`;

export function getTapBrowserState() {
    const tapBrowserState = getComponentState(tapBrowserComponentKey);
    const {serviceUrl, schemaOptions, schemaName, tableOptions, tableName, columnsModel, obsCoreEnabled, obsCoreTables} = tapBrowserState || {};
    return {serviceUrl, schemaOptions, schemaName, tableOptions, tableName, columnsModel, obsCoreEnabled, obsCoreTables};
}

export function setTapBrowserState({serviceUrl, schemaOptions=undefined, schemaName=undefined,
                                       tableOptions=undefined, tableName=undefined, columnsModel=undefined,
                                       obsCoreEnabled= false, obsCoreTables=undefined}) {
    dispatchComponentStateChange(tapBrowserComponentKey,
        {serviceUrl, schemaOptions, schemaName, tableOptions, tableName, columnsModel,
            obsCoreEnabled, obsCoreTables});
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
        const error = `Failed to get schemas for ${serviceUrl}: ${reason?.message ?? reason}`;
        logger.error(error);
        return {error};
    });
}

export function loadObsCoreSchemaTables(serviceUrl) {

    const url = serviceUrl + qFragment + 'QUERY=SELECT+s.schema_name,+t.table_name+FROM+TAP_SCHEMA.schemas+s+JOIN+TAP_SCHEMA.tables+t+on+(s.schema_name=t.schema_name)+JOIN+TAP_SCHEMA.columns+c+on+(t.table_name=c.table_name+and+c.column_name+in+(\'s_region\',+\'t_min\',+\'t_max\',+\'em_min\',+\'em_max\',+\'calib_level\',+\'dataproduct_type\',+\'obs_collection\'))+GROUP+BY+s.schema_name,+t.table_name+HAVING+count(c.column_name)=8';
    const request = makeFileRequest('schemas', url, null, {pageSize: MAX_ROW});

    return doFetchTable(request).then((tableModel) => {
        if (tableModel.error) {
            tableModel.error = `Failed to get ObsCore tables for ${serviceUrl}: ${tableModel.error}`;
            logger.error(tableModel.error);
        } else if (tableModel?.tableData?.data) {
            if (getColumnIdx(tableModel, 'schema_name') < 0) {
                tableModel.error = 'Invalid ObsCore discovery result';
            }
            // check if ivoa.ObsCore table is present
            // if it is, use that as the primary table.
            /* For ordering results, ideally we could also order by schema_index, if the column exists,
               but we would need to do a `s.*` in the select list, which not all ADQL implementations like,
               since the column appears optional.
               */
            var colIdx = getColumnIdx(tableModel, 'table_name');
            tableModel.tableData.data.sort((r1, r2) => {
                let [s1, s2] = [r1[colIdx], r2[colIdx]];
                s1 = s1 === '' ? '\u0002' : s1 === null ? '\u0001' : isUndefined(s1) ? '\u0000' : s1;
                s2 = s2 === '' ? '\u0002' : s2 === null ? '\u0001' : isUndefined(s2) ? '\u0000' : s2;
                if(s1.toLowerCase() === 'ivoa.obscore') {
                    return -1;
                }
                return (s1 > s2 ? 1 : -1);
            });
        } else {
            logger.debug(`no obsCore tables found for ${serviceUrl}`);
        }
        return tableModel;

    }).catch((reason) => {
        const message = get(reason, 'message', reason);
        const error = `Failed to get ObsCore-like tables for ${serviceUrl}: ${message}`;
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
        const error = `Failed to get tables for ${serviceUrl} schema ${schemaName}: ${reason?.message ?? reason}`;
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
        const error = `Failed to get columns for ${serviceUrl} schema ${schemaName} table ${tableName}: ${reason?.message ?? reason}`;
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

    const targetRow = columnsModel?.tableData?.data?.find((oneRow) => {
        return (oneRow[nameIdx] === colName);
    });

    if (!targetRow) {
        return;
    }

    return targetRow[attrIdx];
}

export const TAP_SERVICES_FALLBACK = [
    {
        label: 'https://irsa.ipac.caltech.edu/TAP',
        value: 'https://irsa.ipac.caltech.edu/TAP',
        labelOnly: 'IRSA',
        query: 'SELECT * FROM fp_psc WHERE CONTAINS(POINT(\'ICRS\',ra,dec),CIRCLE(\'ICRS\',210.80225,54.34894,1.0))=1'
    },
    {
        label: 'https://ned.ipac.caltech.edu/tap',
        value: 'https://ned.ipac.caltech.edu/tap/',
        labelOnly: '',
        query: 'SELECT * FROM public.ned_objdir WHERE CONTAINS(POINT(\'ICRS\',ra,dec),CIRCLE(\'ICRS\',210.80225,54.34894,0.01))=1'
    },
    {
        label: 'NASA Exoplanet Archive https://exoplanetarchive.ipac.caltech.edu/TAP/',
        value: 'https://exoplanetarchive.ipac.caltech.edu/TAP/',
        labelOnly: 'NED',
    },
    {
        label: 'https://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/tap',
        value: 'https://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/tap',
        labelOnly: 'CADC',
        query: 'SELECT TOP 10000 * FROM ivoa.ObsCore WHERE CONTAINS(POINT(\'ICRS\', s_ra, s_dec),CIRCLE(\'ICRS\', 10.68479, 41.26906, 0.028))=1'
    },
    {
        label: 'https://gea.esac.esa.int/tap-server/tap',
        value: 'https://gea.esac.esa.int/tap-server/tap',
        labelOnly: 'GAIA',
        query: 'SELECT TOP 5000 * FROM gaiadr2.gaia_source'
    },
    {
        label: 'https://vao.stsci.edu/CAOMTAP/TapService.aspx',
        value: 'https://vao.stsci.edu/CAOMTAP/TapService.aspx',
        labelOnly: 'MAST',
        query: 'SELECT * FROM ivoa.obscore WHERE CONTAINS(POINT(\'ICRS\',s_ra,s_dec),CIRCLE(\'ICRS\',32.69,-51.01,1.0))=1'
    },
    {
        label: 'http://atoavo.atnf.csiro.au/tap',
        value: 'http://atoavo.atnf.csiro.au/tap',
        labelOnly: 'CASDA',
        query: 'SELECT * FROM ivoa.obscore WHERE CONTAINS(POINT(\'ICRS\',s_ra,s_dec),CIRCLE(\'ICRS\',32.69,-51.01,1.0))=1'
    },
    {
        label: 'lsp-stable https://lsst-lsp-stable.ncsa.illinois.edu/api/tap',
        value: 'https://lsst-lsp-stable.ncsa.illinois.edu/api/tap',
        labelOnly: 'LSST',
        query: 'SELECT * FROM wise_00.allwise_p3as_psd '+
            'WHERE CONTAINS(POINT(\'ICRS\', ra, decl),'+
            'POLYGON(\'ICRS\', 9.4999, -1.18268, 9.4361, -1.18269, 9.4361, -1.11891, 9.4999, -1.1189))=1'
    },
    {
        label: 'lsp-int https://lsst-lsp-int.ncsa.illinois.edu/api/tap',
        value: 'https://lsst-lsp-int.ncsa.illinois.edu/api/tap',
        labelOnly: 'LSST',
        query: 'SELECT * FROM wise_00.allwise_p3as_psd '+
            'WHERE CONTAINS(POINT(\'ICRS\', ra, decl),'+
            'POLYGON(\'ICRS\', 9.4999, -1.18268, 9.4361, -1.18269, 9.4361, -1.11891, 9.4999, -1.1189))=1'
    }
];
