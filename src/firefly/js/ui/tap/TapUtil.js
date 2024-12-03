import {dispatchAddPreference, getAppOptions, getPreference} from 'firefly/core/AppDataCntlr.js';
import {sprintf} from 'firefly/externalSource/sprintf.js';
import {isArray, memoize, omit, sortBy, uniqBy} from 'lodash';
import {getCapabilities} from '../../rpc/SearchServicesJson.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {makeFileRequest, makeTblRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {
    alterFalsyVal, doFetchTable, getColumnIdx, getTblById, getTblIdsByGroup, sortTableData
} from '../../tables/TableUtil.js';
import {Logger} from '../../util/Logger.js';
import {getProp, hashCode} from '../../util/WebUtil.js';

const logger = Logger('TapUtil');
const qFragment = '/sync?REQUEST=doQuery&LANG=ADQL&';

export const ADQL_LINE_LENGTH = 100;
export const ADQL_UPLOAD_TABLE_NAME= 'upload_table';
export const TAP_UPLOAD_SCHEMA= 'TAP_UPLOAD';
export const ADQL_QUERY_KEY= 'adqlQuery';
export const USER_ENTERED_TITLE= 'USER_ENTERED_TITLE';
const EMPTY_SCHEMA_NAME= 'EMPTY_SCHEMA_NAME';
export const USER_SERVICE_PREFS= 'UserEnteredServices';
export const SERVICE_EXIST_ERROR= 'TAP service does not appear to exist or is not accessible';


// cache objects - they only grow, but I don't think they will ever get too big
const capabilityCache={};
const schemaCache={};
const obsCoreSchemaCache={};
const tableCache={};
const columnCache={};
const obsCoreMetadataCache={};
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
export const defTapBrowserState= {
    serviceUrl:undefined,
    schemaOptions:undefined,
    schemaName:undefined,
    tableOptions:undefined,
    lastServicesShowing: true,
    tableName:undefined,
    columnsModel:undefined,
    obsCoreEnabled:false,
    obsCoreTableModel:undefined,
    constraintFragments: new Map()
};

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
    try {
        const tableModel= await loadSchemaDef(serviceUrl);
        if (tableModel.error || !tableModel.tableData) {
            tableModel.error = `Failed to get schemas for ${serviceUrl}: ${tableModel.error}`;
            logger.error(tableModel.error);
            return tableModel;
        }
        const schemaNameIdx = getColumnIdx(tableModel, 'schema_name');
        const descIdx = getColumnIdx(tableModel, 'table_desc');
        const tableNameIdx = getColumnIdx(tableModel, 'table_name');
        const doSwap= (descIdx>-1) && descIdx<tableNameIdx;
        const rTable = omit(tableModel, 'tableData.data');
        rTable.tableData.data = uniqBy(tableModel.tableData.data, (row) => row[schemaNameIdx])
            .map( ( (row) => {
                const schemaName= row[schemaNameIdx];
                const tableCnt= tableModel.tableData.data.filter( (fr) => fr[schemaNameIdx]===schemaName)?.length ?? 0;
                const nRow= [...row];
                if (doSwap) {
                    const desc= row[descIdx];
                    nRow[descIdx]= row[tableNameIdx];
                    nRow[tableNameIdx]= desc;
                }
                return [...nRow,tableCnt];
            }));
        rTable.tableData.columns= [...rTable.tableData.columns,{name:'table_cnt',ID:'col_table_cnt',type:'int'}];
        if (doSwap) {
            const descCol= rTable.tableData.columns[descIdx];
            rTable.tableData.columns[descIdx]= rTable.tableData.columns[tableNameIdx];
            rTable.tableData.columns[tableNameIdx]= descCol;
        }
        rTable.totalRows = rTable.tableData?.data?.length || 0;

        if (rTable.totalRows === 0) return {error:'No schemas available'};

        return rTable;
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
    try {
        const tableModel= await loadSchemaDef(serviceUrl);
        if (tableModel.error || !tableModel.tableData) {
            tableModel.error = `Failed to get ObsCore tables for ${serviceUrl}: ${tableModel.error}`;
            logger.error(tableModel.error);
            return tableModel;
        }
        const schemaNameIdx = getColumnIdx(tableModel, 'schema_name');
        const tableNameIdx = getColumnIdx(tableModel, 'table_name');

        const origCols = tableModel.tableData.columns;
        const rTable = omit(tableModel, ['tableData.columns', 'tableData.data']);
        rTable.tableData.columns = [origCols[schemaNameIdx], origCols[tableNameIdx]];  // to keep it like it was before, keep only schema_name table_name
        rTable.tableData.data = tableModel.tableData.data
            .filter((row) => row[schemaNameIdx] === 'ivoa')
            .filter((row) => ['ivoa.obscore','ivoa.ObsCore'].includes(row[tableNameIdx]))
            .map((row) => [row[schemaNameIdx], row[tableNameIdx]]);
        rTable.totalRows = rTable.tableData?.data?.length || 0;

        if (rTable.totalRows === 0) logger.debug(`no obsCore tables found for ${serviceUrl}`);
        if (schemaNameIdx < 0)                  return {error:'Invalid ObsCore discovery result'};

        // check if ivoa.ObsCore table is present
        // if it is, use that as the primary table.
        /* For ordering results, ideally we could also order by schema_index, if the column exists,
           but we would need to do a `s.*` in the select list, which not all ADQL implementations like,
           since the column appears optional.
           */
        rTable.tableData.data.sort((r1, r2) => {
            const [s1, s2] = [alterFalsyVal(r1[1]), alterFalsyVal(r2[1])];
            if(s1.toLowerCase() === 'ivoa.obscore') {
                return -1;
            }
            return (s1 > s2 ? 1 : -1);
        });

        return rTable;
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

    try {
        const tableModel= await loadSchemaDef(serviceUrl);
        if (tableModel.error || !tableModel.tableData) {
            tableModel.error = `Failed to get tables for ${serviceUrl} schema ${schemaName}: ${tableModel.error}`;
            logger.error(tableModel.error);
            return tableModel;
        }
        const schemaNameIdx = getColumnIdx(tableModel, 'schema_name');
        const tableNameIdx = getColumnIdx(tableModel, 'table_name');
        const descIdx = getColumnIdx(tableModel, 'table_desc');
        const doSwap= (descIdx>-1) && descIdx<tableNameIdx;

        const rTable = omit(tableModel, 'tableData.data');
        rTable.tableData.data = tableModel.tableData.data.filter((row) => row[schemaNameIdx] === schemaName);
        if (doSwap) {
            rTable.tableData.data = rTable.tableData.data
                .map( (row) => {
                    const nRow= [...row];
                    const desc= row[descIdx];
                    nRow[descIdx]= row[tableNameIdx];
                    nRow[tableNameIdx]= desc;
                    return nRow;
                });
            rTable.tableData.columns= [...rTable.tableData.columns];
            const descCol= rTable.tableData.columns[descIdx];
            rTable.tableData.columns[descIdx]= rTable.tableData.columns[tableNameIdx];
            rTable.tableData.columns[tableNameIdx]= descCol;
        }
        rTable.totalRows = rTable.tableData?.data?.length || 0;

        if (tableNameIdx < 0)  return {error: `Invalid tables returned for ${serviceUrl} schema ${schemaName}`};
        if (rTable.totalRows === 0) {
            const error =`No tables available for ${serviceUrl} schema ${schemaName}`;
            logger.error(error);
            return {error};
        }

        return rTable;
    } catch(reason) {
        const error = `Failed to get tables for ${serviceUrl} schema ${schemaName}: ${reason?.message ?? reason}`;
        logger.error(error);
        return {error};
    }
}


export async function loadTapCapabilities(serviceUrl) {
    if (capabilityCache[serviceUrl]) return capabilityCache[serviceUrl];
    const capResult= await getCapabilities(serviceUrl+'/capabilities');
    if (!capResult.success) throw Error('could not find capabilities');
    if (capResult && capResult.success) {
        if (isOldTap(serviceUrl) && capResult.data?.tapCapability) {
            capResult.data.tapCapability.canUseCircle= false;
            capResult.data.tapCapability.canUsePolygon= false;
            capResult.data.tapCapability.canUseContains= false;
        }
        capabilityCache[serviceUrl]= capResult.data?.tapCapability;
    }
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

export async function loadObsCoreMetadata(serviceUrl, obscoreTable) {
    if (obsCoreMetadataCache[serviceUrl]) return obsCoreMetadataCache[serviceUrl];
    const tableModel= await doLoadObsCoreMetadata(serviceUrl, obscoreTable);
    obsCoreMetadataCache[serviceUrl]= tableModel;
    return tableModel;
}

async function doLoadObsCoreMetadata(serviceUrl, obscoreTable) {
    const columns = ['obs_collection', 'instrument_name', 'facility_name', 'dataproduct_subtype'].join(',');

    try {
        const tableModel = await doFetchTable(makeTblRequest('ObsCoreMetadataQuery', 'loadObsCoreMetadata',
            {serviceUrl, obscoreTable, columns}));
        return tableModel;
    } catch (reason) {
        const error = `Failed to get metadata for any column in [${columns}] for ${serviceUrl} obscore table ${obscoreTable}: ${reason?.message ?? reason}`;
        logger.error(error);
        return {error};
    }
}

function makeTapRequest(serviceUrl, QUERY, title) {
    const url = serviceUrl + qFragment + 'QUERY=' + encodeURIComponent(QUERY);
    return makeFileRequest(title, url, undefined, {pageSize: MAX_ROW});

}

async function loadSchemaDefJoin(serviceUrl) {
    const QUERY = ` SELECT *
                    FROM tap_schema.schemas
                    INNER JOIN tap_schema.tables ON  tap_schema.tables.schema_name = tap_schema.schemas.schema_name
                `.replace(/\s+/g, ' ').trim();

    const tableModel= await doFetchTable(makeTapRequest(serviceUrl, QUERY, 'loadSchemaDefJoin'));

    // manually fix duplicate 'description' column because cannot select individual column to rename
    const schemaDesc = tableModel.tableData.columns.find((col) => col.name.toLowerCase().startsWith('description'));
    if (schemaDesc) schemaDesc.name = 'schema_desc';
    const tableDesc = tableModel.tableData.columns.findLast((col) => col.name.toLowerCase().startsWith('description'));
    if (tableDesc) tableDesc.name = 'table_desc';
    if (isOldTap(serviceUrl)) modifyModelForOldTap(tableModel);

    const schemaNameIdx= getColumnIdx(tableModel,'schema_name');
    if (schemaNameIdx>-1) {
        tableModel.tableData.data.forEach( (row) => {
            if (!row[schemaNameIdx]) row[schemaNameIdx]=EMPTY_SCHEMA_NAME;
        });
    }


    return tableModel;
}

function modifyModelForOldTap(tableModel) {
    const tableTypeIdx= getColumnIdx(tableModel,'table_type');
    const tableNameIdx= getColumnIdx(tableModel,'table_name');
    const schemaNameIdx= getColumnIdx(tableModel,'schema_name');
    const {tableData}= tableModel;
    if (tableNameIdx===-1 || tableTypeIdx===-1 || schemaNameIdx===-1) return;
    if (tableData.data.some( (row) => row[schemaNameIdx])) return;
    tableData.data.forEach( (row) => {
        const tType= row[tableTypeIdx];
        const tnameGeneric= row[tableNameIdx];
        row[tableTypeIdx]= '';
        row[tableNameIdx]= tType;
        if (!row[schemaNameIdx]) row[schemaNameIdx]= tnameGeneric;
    });
    tableData.columns[tableTypeIdx].name= 'table_desc';
}

async function loadSchemaDefNoJoin(serviceUrl) {

    const schemasQuery = 'SELECT * FROM tap_schema.schemas';
    const tablesQuery  = 'SELECT * FROM tap_schema.tables';

    const schemas = await doFetchTable(makeTapRequest(serviceUrl, schemasQuery, 'loadSchemaDefNoJoin-schemas'));
    const tables  = await doFetchTable(makeTapRequest(serviceUrl, tablesQuery, 'loadSchemaDefNoJoin-tables'));

    const schemaIdx = getColumnIdx(schemas, 'schema_index');
    const schemaNameIdx = getColumnIdx(schemas, 'schema_name');
    const schemaDescIdx = getColumnIdx(schemas, 'description');
    const tableSchemaIdx = getColumnIdx(tables, 'schema_name');

    // merge schema data into tables
    // add schema_name and desc
    tables.tableData.columns.unshift({...schemas.tableData.columns[schemaDescIdx],  name:'schema_desc'});
    if (schemaIdx >=0) tables.tableData.columns.unshift({name:'schema_index', type:'int'});

    tables.tableData.data.forEach((row) => {
        const schema_desc = schemas.tableData.data.find((srow) => srow[schemaNameIdx] === row[tableSchemaIdx])
                                                  ?.[schemaDescIdx];
        row.unshift(schema_desc || '');

        if (schemaIdx >= 0) {
            const schema_index = schemas.tableData.data.find((srow) => srow[schemaNameIdx] === row[schemaIdx])
                ?.map((srow) => srow[schemaDescIdx]);
            row.unshift(schema_index);
        }
    });
    const tblDesc = tables.tableData.columns.find((col) => col.name === 'description');
    if (tblDesc) tblDesc.name = 'table_desc';

    return tables;
}

function supportJoin(serviceUrl) {
    // Vizier doesn't support schema.*, table.*.  returning a table of blank rows.
    // heasarc doesn't support schema.*, table.* nor *. need to handle separately.
    // most services does not have schema_index and/or table_index.
    // to make it work for most services, had to use 'select *'.
    return !serviceUrl.toLowerCase().includes('heasarc.gsfc.nasa.gov');
}

function isOldTap(serviceUrl) {
    // norlab is using an old version of TAP and there is no other way to recognize it
    return serviceUrl.toLowerCase().includes('datalab.noirlab.edu');
}

export const loadSchemaDef = memoize(async (serviceUrl) => {

    try {
        if (!serviceUrl) return;
        const tableModel= supportJoin(serviceUrl)
                ? await loadSchemaDefJoin(serviceUrl)
                : await loadSchemaDefNoJoin(serviceUrl);

        if (tableModel.error) throw new Error(tableModel.error);

        const schemaNameIdx = getColumnIdx(tableModel, 'schema_name');
        const tableNameIdx = getColumnIdx(tableModel, 'table_name');
        const schemaIndex = getColumnIdx(tableModel, 'schema_index');
        const tableIndex = getColumnIdx(tableModel, 'table_index');

        if (schemaNameIdx < 0) return {error:'Invalid schemas table'};

        // natural order places uppercase on top.  this forces lowercase to be on top
        const forceLcOnTop = (s) => (s && /^[a-z]/.test(s) ? ' '+s : s)?.toLowerCase() || '';

        // manually sort [schema_index]|schema_name|[table_index]|table_name.
        const makeSortKey = (row) => {
            const sIndex = schemaIndex >= 0 ? row[schemaIndex] : '9999';
            const sName  = forceLcOnTop(row[schemaNameIdx]);
            const tIndex = tableIndex >= 0 ? row[tableIndex] : '9999';
            const tName = forceLcOnTop(row[tableNameIdx]);
            return sprintf('%4s|%s|%4s|%s', sIndex, sName, tIndex, tName);
        };

        tableModel.tableData.data = sortBy(tableModel.tableData.data,(row) => makeSortKey(row));

        return tableModel;
    } catch (reason) {
        const error = `Failed to resolve TAP_SCHEMA: ${reason?.message ?? reason}`;
        logger.error(error);
        return {error};
    }
});

export const loadTapKeys = memoize(async (serviceUrl) => {

    const QUERY = `
        SELECT tap_schema.keys.key_id,
               tap_schema.keys.from_table,
               tap_schema.keys.target_table,
               tap_schema.keys.description,  
               tap_schema.key_columns.from_column,
               tap_schema.key_columns.target_column
        FROM tap_schema.keys INNER JOIN tap_schema.key_columns ON tap_schema.keys.key_id = tap_schema.key_columns.key_id
        `.replace(/\s+/g, ' ').trim();

    const url = serviceUrl + qFragment + 'QUERY=' + encodeURIComponent(QUERY);
    const request = makeFileRequest('loadTapKeys', url, undefined, {pageSize: MAX_ROW});

    try {
        const tableModel= await doFetchTable(request);
        if (tableModel.error) throw new Error(tableModel.error);
        return tableModel;
    } catch (reason) {
        const error = `Failed to resolve TAP_SCHEMA keys info: ${reason?.message ?? reason}`;
        logger.error(error);
        return {error};
    }
});

// Function to search for a node in the tree
export function searchNodeBy(nodes, accept) {
    for (const idx in nodes) {
        if (accept(nodes[idx])) return nodes[idx];
        if (nodes[idx].children) {
            const found = searchNodeBy(nodes[idx].children, accept);
            if (found) return found;
        }
    }
    return null;
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

export const getAsEntryForTableName= (tableName) => tableName?.[0] ?? 'x';

export function mergeAdditionalServices(inServices, additional) {
    if (!hasElements(additional)) return inServices;

    const mergeAdditional= additional.map( (a) => {
        if (a.hide) return false;
        const originalEntry= inServices.find( (t) => a.label===t.label);
        return originalEntry ? {...originalEntry, ...a} : a;
    }).filter( (s) => s);

    const unmodifiedOriginal= inServices
        .map( (t) => {
            const match= additional.find( (a) => a.label===t.label);
            return !match && t;
        })
        .filter( (s) => s);

    return [...mergeAdditional,...unmodifiedOriginal];
}

// let userServices;

export function getTapServices() {
    const {tap} = getAppOptions();
    const startingTapServices= hasElements(tap?.services) ? [...tap.services] : [...TAP_SERVICES_FALLBACK];
    const mergedServices= mergeAdditionalServices(startingTapServices,tap?.additional?.services);
    mergedServices.push(...getUserServiceAry());
    return mergedServices;
}

const baseName= 'User Entered';

function getUserServiceAry() {
    return getPreference(USER_SERVICE_PREFS, []);
}

export function addUserService(serviceUrl) {
    const userServices= getUserServiceAry();
    if (getTapServices().some( (({value}) => value===serviceUrl))) { // don't add if service already exist
        return;
    }
    const usedNumAry= userServices
        .map( (s) => s.label)
        .filter((title) => title && title.startsWith(baseName))
        .map((title) => title.trim().split('-')?.[1])
        .map(Number)
        .filter(Boolean);

    const maxNum = usedNumAry?.length ? Math.max(...usedNumAry) : 0;
    const label= userServices.every( (s) => s.label!==baseName) ? baseName : baseName + ` - ${maxNum + 1}`;
    userServices.push({ label, value: serviceUrl, userAdded: true});
    dispatchAddPreference(USER_SERVICE_PREFS, userServices);
}

export function deleteUserService(serviceUrl) {
    const userServices= getUserServiceAry().filter( (s) => s.value!==serviceUrl);
    dispatchAddPreference(USER_SERVICE_PREFS, userServices);
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


/**
 * group key for fieldgroup comp
 */




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

function getTableNameFromADQL(adql) {
    if (!adql) return;
    const adqlUp = adql.toUpperCase();
    const start = adqlUp.toUpperCase().indexOf('FROM ') + 5;
    const end = adqlUp.lastIndexOf('WHERE');
    if (start > 5) {
        if (end > -1) {
            const tStr = adql.substring(start, end)?.trim();
            if (tStr) return tStr.split(/[\s,]+/)[0]; // pull the first works out between the FROM and the WHERE
        }
        else {
            return adql.substring(start)?.trim();
        }
    }
    return undefined;
}

export function makeTapSearchTitle(adql, serviceUrl='', tableName) {
    const tName = adql ? getTableNameFromADQL(adql) : tableName;
    const host = serviceUrl?.match(/.*:\/\/(.*)\/.*/i)?.[1]; // table name or service url
    const simpleHost= host?.split('.')[0];
    if (tName && simpleHost) return `${tName} - ${simpleHost}`;
    return tName || simpleHost || host;
}

export function makeNumberedTitle(inTitle) {
    if (getTblIdsByGroup().map((tbl_id) => getTblById(tbl_id)?.title).every( (t) => t!==inTitle)) {
       return inTitle;
    }
    const numList = getTblIdsByGroup()
        .map((tbl_id) => getTblById(tbl_id)?.title)
        .filter((title) => title && title.startsWith(inTitle))
        .map((title) => title?.substring(inTitle.length).trim().split('-')?.[1])
        .map(Number)
        .filter(Boolean);

    const maxNum = numList?.length ? Math.max(...numList) : 0;
    return inTitle + ` - ${maxNum + 1}`;
}

/**
 * Assembles an array of objects with column attributes in the format that ColumnFld accepts
 * @param columnsModel
 */
export function getAvailableColumns(columnsModel){
    const attrIdx  = [
        //[<column name in columnModel>, <column attribute name ColumnFld wants>]
        ['column_name', 'name'],
        ['unit', 'units'],
        ['datatype', 'type'],
        ['ucd', 'ucd'],
        ['description', 'desc']
    ].map(([c,k])=>[k,getColumnIdx(columnsModel, c)]);

    const td= columnsModel?.tableData?.data ?? [] ;
    return td.map((r) => {
        const col = {};
        attrIdx.forEach(([k,i]) => { col[k] = r[i]; });
        return col;
    });
}

export function getServiceLabel(serviceUrl) {
    const tapOps= getTapServiceOptions();
    return (serviceUrl && (tapOps.find( (e) => e.value===serviceUrl)?.labelOnly)) || '';
}

export function getServiceHiPS(serviceUrl) {
    const tapOps= getTapServices();
    return (serviceUrl && (tapOps.find( (e) => e.value===serviceUrl)?.hipsUrl)) || '';
}

export const getTapServiceOptions= () =>
    getTapServices().map(({label,value,userAdded=false})=>({label:value, value, labelOnly:label, userAdded}));

export const getServiceNamesAsKey= () => getTapServiceOptions().map(({label}) => label).join('-');
