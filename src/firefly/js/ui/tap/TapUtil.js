import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {isArray, isUndefined} from 'lodash';
import {getCapabilities} from '../../rpc/SearchServicesJson.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {makeFileRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {doFetchTable, getColumnIdx, sortTableData} from '../../tables/TableUtil.js';
import {Logger} from '../../util/Logger.js';
import {getProp, hashCode} from '../../util/WebUtil.js';

const logger = Logger('TapUtil');
const qFragment = '/sync?REQUEST=doQuery&LANG=ADQL&';

export const ADQL_LINE_LENGTH = 100;
export const ADQL_UPLOAD_TABLE_NAME= 'upload_table';
export const TAP_UPLOAD_SCHEMA= 'TAP_UPLOAD';


// cache objects - they only grow, but I don't think they will ever get too big
const capabilityCache={};
const schemaCache={};
const obsCoreSchemaCache={};
const tableCache={};
const columnCache={};
// end cache objects



export function getMaxrecHardLimit() {
    const defaultValue = Number.parseInt(getProp('tap.maxrec.hardlimit'));
    if (Number.isNaN(defaultValue)) {
        return 5000000;
    } else {
        return defaultValue;
    }
}

export const tapHelpId = (id) => `tapSearches.${id}`;

export function makeUploadSchema(uploadFileName,serverFile, columns, totalRows, fileSize, table=ADQL_UPLOAD_TABLE_NAME, asTable= 'ut') {
    return {
        [uploadFileName] : { serverFile, uploadFileName, totalRows, fileSize, table, asTable, columns}
    };
}


/**
 * @typedef {Object} TapBrowserState
 *
 * @prop  columnsModel
 * @props {String} serviceUrl
 * @prop schemaOptions
 * @prop tableOptions
 * @props {String} schemaName
 * @props {String} tableName
 * @prop {Map<String, String>} constraintFragments
 * @prop obsCoreTableModel
 * @prop {boolean} obsCoreEnabled
 */


/** * @type TapBrowserState */
export const defTapBrowserState= {serviceUrl:undefined, schemaOptions:undefined, schemaName:undefined, tableOptions:undefined,
    tableName:undefined, columnsModel:undefined, obsCoreEnabled:false, obsCoreTableModel:undefined, constraintFragments: new Map()};

export function getColumnsTblId(serviceUrl, tableName) {
    // table name is unique across schemas
    return `${tableName}-tapCols-${hashCode(serviceUrl)}`;
}

export async function loadTapSchemas(serviceUrl) {
    if (schemaCache[serviceUrl]) return schemaCache[serviceUrl];
    const tableModel= await doLoadTapSchemas(serviceUrl);
    schemaCache[serviceUrl]= tableModel;
    return tableModel;
}

async function doLoadTapSchemas(serviceUrl) {

    const url = serviceUrl + qFragment + 'QUERY=SELECT+*+FROM+TAP_SCHEMA.schemas';
    const request = makeFileRequest('schemas', url, null, {pageSize: MAX_ROW});

    try {
        const tableModel= await doFetchTable(request);
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
    } catch(reason) {
        const error = `Failed to get schemas for ${serviceUrl}: ${reason?.message ?? reason}`;
        logger.error(error);
        return {error};
    }
}

export async function loadObsCoreSchemaTables(serviceUrl) {
    if (obsCoreSchemaCache[serviceUrl]) return obsCoreSchemaCache[serviceUrl];
    const tableModel= await doLoadObsCoreSchemaTables(serviceUrl);
    obsCoreSchemaCache[serviceUrl]= tableModel;
    return tableModel;
}

async function doLoadObsCoreSchemaTables(serviceUrl) {

    const url = serviceUrl + qFragment + 'QUERY=SELECT+s.schema_name,+t.table_name+FROM+TAP_SCHEMA.schemas+s+JOIN+TAP_SCHEMA.tables+t+on+(s.schema_name=t.schema_name)+WHERE+s.schema_name=\'ivoa\'+AND+t.table_name+IN+(\'ivoa.obscore\',\'ivoa.ObsCore\')';
    const request = makeFileRequest('schemas', url, null, {pageSize: MAX_ROW});

    try {
        const tableModel= await doFetchTable(request);
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
            const colIdx = getColumnIdx(tableModel, 'table_name');
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
    } catch (reason) {
        const error = `Failed to get ObsCore-like tables for ${serviceUrl}: ${reason?.message ?? reason}`;
        logger.error(error);
        return {error};
    }
}



export async function loadTapTables(serviceUrl, schemaName) {
    const key= serviceUrl+'----'+schemaName;
    if (tableCache[key]) return tableCache[key];
    const tableModel= await doLoadTapTables(serviceUrl,schemaName);
    tableCache[key]= tableModel;
    return tableModel;
}


async function doLoadTapTables(serviceUrl, schemaName) {

    const url = serviceUrl + qFragment + 'QUERY=SELECT+*+FROM+TAP_SCHEMA.tables+WHERE+schema_name+like+\'' + schemaName + '\'';
    const request = makeFileRequest('tables', url, null, {pageSize: MAX_ROW});

    try {
        const tableModel= await doFetchTable(request);
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
    } catch(reason) {
        const error = `Failed to get tables for ${serviceUrl} schema ${schemaName}: ${reason?.message ?? reason}`;
        logger.error(error);
        return {error};
    }
}


export async function loadTapCapabilities(serviceUrl) {
    if (capabilityCache[serviceUrl]) return capabilityCache[serviceUrl];
    const capResult= await getCapabilities(serviceUrl+'/capabilities');
    if (capResult && capResult.success) capabilityCache[serviceUrl]= capResult.data?.tapCapability;
    return capabilityCache[serviceUrl];
}

export const isCapabilityLoaded= (serviceUrl) => Boolean(capabilityCache[serviceUrl]);
export const getLoadedCapability= (serviceUrl) => capabilityCache[serviceUrl];



export async function loadTapColumns(serviceUrl, schemaName, tableName) {
    const key= serviceUrl+'----'+schemaName+'---'+tableName;
    if (columnCache[key]) return columnCache[key];
    const tableModel= await doLoadTapColumns(serviceUrl,schemaName,tableName);
    columnCache[key]= tableModel;
    return tableModel;
}



async function doLoadTapColumns(serviceUrl, schemaName, tableName) {

    const url = serviceUrl + qFragment +
        'QUERY=SELECT+*+FROM+TAP_SCHEMA.columns+WHERE+table_name+like+\'' + tableName + '\'';
        // 'QUERY=SELECT+column_name,description,unit,datatype,ucd,utype,principal+' +
        // 'FROM+TAP_SCHEMA.columns+WHERE+table_name+like+\'' + tableName + '\'';

    const request = makeFileRequest('columns', url, undefined, {tbl_id: getColumnsTblId(serviceUrl, tableName), pageSize: MAX_ROW});

    try {
        const tableModel= await doFetchTable(request);
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

    } catch (reason) {
        const error = `Failed to get columns for ${serviceUrl} schema ${schemaName} table ${tableName}: ${reason?.message ?? reason}`;
        logger.error(error);
        return {error};
    }
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

const hasElements= (a) => Boolean(isArray(a) && a?.length);

function mergeAdditionalServices(tapServices, additional) {
    if (!hasElements(additional)) return tapServices;

    const modifiedOriginal= tapServices.map( (t) => {
        const match= additional.find( (a) => a.label===t.label);
        return match ? {...t,...match} : t;
    });
    const trulyAdditional= additional.filter( (a) => !tapServices.find( (t) => t.label===a.label) );

    return [...trulyAdditional,...modifiedOriginal];
}

export function getTapServices(webApiUserAddedService) {
    const {tap} = getAppOptions();
    const startingTapServices= hasElements(tap?.services) ? [...tap.services] : [...TAP_SERVICES_FALLBACK];
    const mergedServices= mergeAdditionalServices(startingTapServices,tap?.additional?.services);
    webApiUserAddedService && mergedServices.push(webApiUserAddedService);
    return mergedServices;
}

const validTableNameRE= /^[A-Za-z][A-Za-z_0-9]*(\.[A-Za-z][A-Za-z_0-9]*){0,2}$/;
const validColumnNameRE=/^[A-Za-z][A-Za-z_0-9]*(\.[A-Za-z][A-Za-z_0-9]*){0,3}$/;

/**
 * Add quotes around a table or column name if it is not already quoted and if non-standard names.
 * @param {string} name a table or column name
 * @param {boolean} [isTable] - if the the name is a table name otherwise it is a colunm name
 * @return {string}
 */
export function maybeQuote(name, isTable=false) {
    if (!name || (name.startsWith('"') && name.endsWith('"'))) return name;
    const re= isTable ? validTableNameRE : validColumnNameRE;
    return  (name.match(re)) ? name : `"${name}"`;
}


export const defaultADQLExamples=[
    {
        description: 'From the IRSA TAP service, a 1 degree cone search of the 2MASS point source catalog around M101 would be:',
        /* eslint-disable-next-line quotes */
        statement:
            `SELECT * FROM fp_psc 
WHERE CONTAINS(POINT('J2000', ra, dec), CIRCLE('J2000', 210.80225, 54.34894, 1.0)) = 1`
    },
    {
        description: 'From the Gaia TAP service, a 1 degree by 1 degree box of the Gaia data release 3 point source catalog around M101 would be:',
        /* eslint-disable-next-line quotes */
        statement:
            `SELECT * FROM gaiaedr3.gaia_source 
WHERE CONTAINS(POINT('ICRS', ra, dec), BOX('ICRS', 210.80225, 54.34894, 1.0, 1.0))=1`
    },
    {
        description: 'From the IRSA TAP service, a triangle search of the AllWISE point source catalog around M101 would be:',
        /* eslint-disable-next-line quotes */
        statement:
            `SELECT designation, ra, dec, w2mpro 
FROM allwise_p3as_psd 
WHERE CONTAINS (POINT('J2000' , ra , dec), POLYGON('J2000' , 209.80225 , 54.34894 , 209.80225 , 55.34894 , 210.80225 , 54.34894))=1`,
    }
];



// --- keep for reference //todo delete after 2022.2 release
// export const maybeQuoteByService= (serviceURL, item, isTable) =>
//           (serviceURL && serviceURL.includes('VizieR')) ? maybeQuote(item,isTable) : item;

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
/**
 * group key for fieldgroup comp
 */
