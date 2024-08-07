/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, intersection, isEmpty, isNaN, isString, isUndefined} from 'lodash';
import {MetaConst} from '../data/MetaConst.js';
import {defDataSourceGuesses} from '../metaConvert/DefaultConverter.js';
import {getCornersColumns} from '../tables/TableInfoUtil.js';
import {
    getBooleanMetaEntry, getCellValue, getColumn, getColumns, getMetaEntry, isTableUsingRadians
} from '../tables/TableUtil.js';
import {isDefined} from '../util/WebUtil.js';
import CoordinateSys from '../visualize/CoordSys.js';
import {makeAnyPt, makeWorldPt, parseWorldPt} from '../visualize/Point.js';
import {
    ACCESS_FORMAT, ACCESS_URL, DEFAULT_TNAME_OPTIONS, obsPrefix, OBSTAP_CNAMES, S_REGION, SERVICE_DESC_COL_NAMES,
    SSA_COV_UTYPE, SSA_TITLE_UTYPE
} from './VoConst.js';
import {getObsTabColEntry, getTableModel} from './VoCoreUtils.js';
import {hasServiceDescriptors} from './VoDataLinkServDef.js';
import {VoTableRecognizer} from './VoTableRecognizer.js';


export function isOrbitalPathTable(tableOrId) {
    const table = getTableModel(tableOrId);
    if (!table) return false;
    return getBooleanMetaEntry(table, MetaConst.ORBITAL_PATH, false);
}

/**
 * find the center column base on the table model of catalog or image metadata
 * Investigate table meta data a return a CoordColsDescription for two columns that represent and object in the row
 * @param {TableModel|undefined} table
 * @param {boolean} acceptArrayCol - if true then allow of a single column with an array entry for RA and Dec
 * @return {CoordColsDescription|null|undefined}
 */
export function findTableCenterColumns(table, acceptArrayCol = false) {
    const tblRecog = get(table, ['tableData', 'columns']) && VoTableRecognizer.newInstance(table);
    return tblRecog && tblRecog.getCenterColumns(acceptArrayCol);
}

export function findImageCenterColumns(tableOrId) {
    const table = getTableModel(tableOrId);
    const tblRecog = get(table, ['tableData', 'columns']) && VoTableRecognizer.newInstance(table);
    return getMetaEntry(table, MetaConst.FITS_FILE_PATH) && tblRecog?.getImagePtColumnsOnMeta();
}

/**
 * If there are center columns defined with this table then return a WorldPt
 * @param table
 * @param row
 * @return {WorldPt|undefined} a world point or undefined it no center columns exist
 */
export function makeWorldPtUsingCenterColumns(table, row) {
    if (!table || isUndefined(row)) return;
    const cen = findTableCenterColumns(table);
    return cen && makeWorldPt(getCellValue(table, row, cen.lonCol), getCellValue(table, row, cen.latCol), cen.csys);
}

/**
 * find ObsCore defined 's_region' column
 * @param table
 * @return {RegionColDescription|null}  return ObsCore defined s_region column
 */
export function findTableRegionColumn(table) {
    const tblRecog = get(table, ['tableData', 'columns']) && VoTableRecognizer.newInstance(table);
    return tblRecog && tblRecog.getRegionColumn();
}

/**
 * @summary check if there is center column or corner columns defined, if so this table has coverage information
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @returns {boolean} True if there  is coverage data in this table
 */
export function hasCoverageData(tableOrId) {
    const table = getTableModel(tableOrId);
    if (!getBooleanMetaEntry(table, MetaConst.COVERAGE_SHOWING, true)) return false;
    if (!table) return false;
    if (!table.totalRows) return false;
    return !isEmpty(findTableRegionColumn(table)) || !isEmpty(findTableCenterColumns(table, true)) || !isEmpty(getCornersColumns(table));
}

/**
 * Test to see it this is a catalog. A catalog must have one of the following:
 *  - CatalogOverlayType meta data entry defined and not equal to 'FALSE' and we must be able to find the columns
 *                            either by meta data or by guessing
 *  - We find the columns by some vo standard
 *
 *  Note- if the CatalogOverlayType meta toUpperCase === 'FALSE' then we will treat it as not a catalog no matter how the
 *  vo columns might be defined.
 *
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean} True if the table is a catalog
 * @see MetaConst.CATALOG_OVERLAY_TYPE
 */
export function isCatalog(tableOrId) {
    const table = getTableModel(tableOrId);

    if (!table) return false;
    if (isTableWithRegion(table)) return false;
    const {tableMeta, tableData} = table;
    if (!get(tableData, 'columns') || !tableMeta) return false;
    // if (getBooleanMetaEntry(table,MetaConst.COVERAGE_SHOWING,false)) return false;

    if (isOrbitalPathTable(table)) return false;

    const catOverType = getMetaEntry(table, MetaConst.CATALOG_OVERLAY_TYPE)?.toUpperCase();
    if (catOverType === 'FALSE') return false;
    if (isString(catOverType)) {
        if (catOverType === 'IMAGE_PTS') return Boolean(findImageCenterColumns(table));
        else return Boolean(VoTableRecognizer.newInstance(table).getCenterColumns());
    } else {
        return Boolean(VoTableRecognizer.newInstance(table).getVODefinedCenterColumns());
    }
}

export function isTableWithRegion(tableOrId) {
    const table = getTableModel(tableOrId);
    if (!table) return false;

    return Boolean(VoTableRecognizer.newInstance(table).getVODefinedRegionColumn());
}

function getObsCoreTableColumn(tableOrId, name) {
    const entry = getObsTabColEntry(name);
    if (!entry) return;
    const table = getTableModel(tableOrId);
    const tblRec = VoTableRecognizer.newInstance(table);
    let cols = tblRec.getTblColumnsOnUType(entry.utype);
    if (cols.length) {
        if (cols.length === 1) return cols[0];
        const prefUtype = cols.find((c) => c.name === name);
        return prefUtype ? prefUtype : cols[0];
    }
    cols = tblRec.getTblColumnsOnDefinedUCDValue(entry.ucd);
    if (cols.length) {
        if (cols.length === 1) return cols[0];
        const prefUcd = cols.find((c) => c.name === name);
        return prefUcd ? prefUcd : cols[0];
    }
    return getColumn(table, name);
}

/**
 * Return the dataproduct_type column
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {TableColumn}
 */
export const getObsCoreProdTypeCol = (tableOrId) => getObsCoreTableColumn(tableOrId, 'dataproduct_type');

/**
 * Return true this table can be access as ObsCore data
 * @param {TableModel|String} tableOrId
 * @return {boolean}
 */
export function hasObsCoreLikeDataProducts(tableOrId) {
    const table = getTableModel(tableOrId);
    const hasUrl = getObsCoreTableColumn(table, ACCESS_URL);
    const hasFormat = getObsCoreTableColumn(table, ACCESS_FORMAT);
    const hasProdType = getObsCoreProdTypeCol(table);
    return Boolean(hasUrl && hasFormat && hasProdType);
}

export function isDatalinkTable(tableOrId) {
    const columns = getTableModel(tableOrId)?.tableData?.columns?.map( (c) => c?.name?.toLowerCase() ?? '') ?? [];
    return SERVICE_DESC_COL_NAMES.every((cname) => columns.includes(cname));
}

function columnMatches(table, cName) {
    if (!table || !cName) return undefined;
    if (getColumn(table, cName)) return cName;
    const cUp = cName.toUpperCase();
    const col = table.tableData.columns.find((c) => cUp === c.name.toUpperCase());
    return col && col.name;
}

/**
 * Find the data source column if it is defined in the metadata and a column exists with that name.
 * The metadata entry 'DataSource' is case-insensitive matched.
 * The column name is also match case-insensitive.
 * The metadata entry can have two forms 'abc' or '[abc,efe,hij]' if it is the second form then the first
 * entry in the array to match a column is returned. The second form is useful when the code defining the DataSource
 * entry is handling a set of table where the data source could be one of several name such
 * as '[url,fileurl,file_url,data_url,data]'
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean|undefined|string} the column name if it exists, if the metadata is not included,
 *                                             false if defined but set to 'false' (case-insensitive)
 */
export function getDataSourceColumn(tableOrId) {
    const table = getTableModel(tableOrId);
    if (!table || !get(table, 'tableData.columns') || !table.tableMeta) return undefined;
    const dsCol = getMetaEntry(table, MetaConst.DATA_SOURCE, '').trim();
    if (!dsCol) return undefined;
    if (dsCol.toLocaleLowerCase() === 'false') return false;

    if (dsCol.startsWith('[') && dsCol.endsWith(']')) {
        return columnMatches(table, dsCol
            .substring(1, dsCol.length - 1)
            .split(',')
            .find((s) => columnMatches(table, s.trim())));
    } else {
        return columnMatches(table, dsCol);
    }
}

/**
 * Guess if this table contains image meta data. It contains image meta data if IMAGE_SOURCE_ID is defined
 * or a DATA_SOURCE column name is defined, or it is an obscore table, or it has service descriptors
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean} true if there is image meta data
 * @see MetaConst.DATA_SOURCE
 * @see MetaConst.IMAGE_SOURCE_ID
 */
export function isDataProductsTable(tableOrId) {
    const table = getTableModel(tableOrId);
    if (isEmpty(table)) return false;
    const {tableMeta, totalRows} = table;
    if (!tableMeta || !totalRows) return false;

    const dataSourceColumn = getDataSourceColumn(table);
    if (dataSourceColumn === false) return false;  // DataSource meta data may be specifically set to false, if so disable all metadata processing

    const checkFalse = getMetaEntry(table, MetaConst.IMAGE_SOURCE_ID)?.toUpperCase();
    if (checkFalse === 'FALSE') return false;

    return Boolean(
        tableMeta[MetaConst.IMAGE_SOURCE_ID] ||
        tableMeta[MetaConst.DATASET_CONVERTER] ||
        dataSourceColumn ||
        hasObsCoreLikeDataProducts(table) ||
        hasServiceDescriptors(table) ||
        hasGuessedDataProductsColumn(table) ||
        isTableWithRegion(tableOrId));
}


function hasGuessedDataProductsColumn(table) {
    const columns= table?.tableData?.columns ?? [];

    const found= defDataSourceGuesses.some( (n) => columns
        .some( (c) => c.name.toUpperCase()===n));
    return found;
}

/**
 * find the ObsCore defined 'access_url' column
 * @param table
 * @return {TableColumn|null} return ObsCore defined access_url column
 */
export function findTableAccessURLColumn(table) {
    const urlCol = getObsCoreTableColumn(table, ACCESS_URL);
    return isEmpty(urlCol) ? undefined : urlCol;
}

/**
 * return access_format cell data
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {string}
 */
export const getObsCoreAccessFormat = (tableOrId, rowIdx) => getObsCoreCellValue(tableOrId, rowIdx, ACCESS_FORMAT);
/**
 * return s_region cell data
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {string}
 */
export const getObsCoreSRegion = (tableOrId, rowIdx) => getObsCoreCellValue(tableOrId, rowIdx, S_REGION);
/**
 * return obs_title cell data
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {string}
 */
export const getObsTitle = (tableOrId, rowIdx) => {
    return getObsCoreCellValue(tableOrId, rowIdx, 'obs_title');
};

export const getObsReleaseDate = (tableOrId, rowIdx) => {
    const table= getTableModel(tableOrId);
    if (!table) return '';
    return getCellValue(table,rowIdx,'obs_release_date');
};



/**
 * return access_url cell data
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {string}
 */
export const getObsCoreAccessURL = (tableOrId, rowIdx) => getObsCoreCellValue(tableOrId, rowIdx, ACCESS_URL);

/**
 * return dataproduct_type cell data.
 * and
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @param alwaysLowerCaseString - if data is defined then always return data as a lowercase string
 * @return {string}
 */
export function getObsCoreProdType(tableOrId, rowIdx, alwaysLowerCaseString = true) {
    const v = getObsCoreCellValue(tableOrId, rowIdx, 'dataproduct_type');
    return (isDefined(v) && alwaysLowerCaseString) ? (v + '').toLowerCase() : v;
}

export function getProdTypeGuess(tableOrId, rowIdx) {
    const table = getTableModel(tableOrId);
    if (getObsCoreProdType(table, rowIdx)) {
        return getObsCoreProdType(table, rowIdx) ?? getCellValue(table, rowIdx, 'dataset_type') ?? '';
    } else if (getColumn(table, 'dataset_type')) {
        return getCellValue(table, rowIdx, 'dataset_type') ?? '';
    }
    return '';
}

/**
 * Guess if this table has enough ObsCore attributes to be considered an ObsCore table.
 * - any column contains utype with 'obscore:' prefix
 * - matches 3 or more of ObsCore column names
 * @param {TableModel} tableModel
 * @returns {boolean}
 */
export function isObsCoreLike(tableModel) {
    const cols = getColumns(tableModel);
    if (cols.findIndex((c) => get(c, 'utype', '').startsWith(obsPrefix)) >= 0) {
        return true;
    }
    const v = intersection(cols.map((c) => c.name), OBSTAP_CNAMES);
    return v.length > 2;
}

/**
 * check to see if dataproduct_type cell a votable
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {boolean}
 */
export const isFormatVoTable = (tableOrId, rowIdx) => getObsCoreAccessFormat(tableOrId, rowIdx).toLowerCase().includes('votable');

/**
 * check to see if dataproduct_type is a datalink
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {boolean}
 */
export function isFormatDataLink(tableOrId, rowIdx) {
    const accessFormat = getObsCoreAccessFormat(tableOrId, rowIdx).toLowerCase();
    return accessFormat.includes('votable') && accessFormat.includes('content=datalink');
}

export function isFormatPng(tableOrId, rowIdx) {
    const accessFormat = getObsCoreAccessFormat(tableOrId, rowIdx).toLowerCase();
    return accessFormat.includes('jpeg') || accessFormat.includes('jpg') || accessFormat.includes('png');
}

export function getWorldPtFromTableRow(table) {
    const centerColumns = findTableCenterColumns(table);
    if (!centerColumns) return undefined;
    const {lonCol, latCol, csys} = centerColumns;
    const ra = Number(getCellValue(table, table.highlightedRow, lonCol));
    const dec = Number(getCellValue(table, table.highlightedRow, latCol));
    const usingRad = isTableUsingRadians(table, [lonCol, latCol]);
    const raDeg = usingRad ? ra * (180 / Math.PI) : ra;
    const decDeg = usingRad ? dec * (180 / Math.PI) : dec;
    return makeAnyPt(raDeg, decDeg, csys || CoordinateSys.EQ_J2000);
}

function getObsCoreTableColumnName(tableOrId,name) {
    const col = getObsCoreTableColumn(tableOrId,name);
    return col ? col.name : '';
}

export function getObsCoreCellValue(tableOrId, rowIdx, obsColName) {
    const table= getTableModel(tableOrId);
    if (!table) return '';
    return getCellValue(table, rowIdx, getObsCoreTableColumnName(table, obsColName)) || '';
}
// moved from java, if we decide to use it this is what we had before
export const findTargetName = (columns) => columns.find( (c) => DEFAULT_TNAME_OPTIONS.includes(c));


export function isSSATable(tableOrId) {
    const table= getTableModel(tableOrId);
    if (!table) return false;
    const foundParts= table.tableData.columns
        .filter((c) => {
            if (c?.utype?.toLowerCase().includes(SSA_COV_UTYPE)) return true;
            if (c?.utype?.toLowerCase().includes(SSA_TITLE_UTYPE )) return true;
        });
    return foundParts.length>=2;
}

export function getSSATitle(tableOrId,row) {
    const table= getTableModel(tableOrId);
    if (!table) return false;
    const foundCol= table.tableData.columns
        .filter((c) => {
            if (c?.utype?.toLowerCase().includes(SSA_TITLE_UTYPE )) return true;
        });
    return foundCol.length>0 ? getCellValue(table,row,foundCol[0].name) : undefined;
}

export function getSearchTarget(r, tableModel, searchTargetStr, overlayPositionStr) {
    if (!r) r = tableModel?.request;
    if (searchTargetStr) return parseWorldPt(searchTargetStr);
    if (overlayPositionStr) return parseWorldPt(overlayPositionStr);
    const pos = getMetaEntry(tableModel, MetaConst.OVERLAY_POSITION);
    if (pos) return parseWorldPt(pos);
    if (!r) return;
    if (r.UserTargetWorldPt) return parseWorldPt(r.UserTargetWorldPt);
    if (r.QUERY) return extractCircleFromADQL(r.QUERY);
    if (r.source?.toLowerCase()?.includes('circle')) return extractCircleFromUrl(r.source);
}

function extractCircleFromUrl(url) {
    const params = new URL(url)?.searchParams;
    if (!params) return;
    if (params.has('ADQL')) return extractCircleFromADQL(params.get('ADQL'));
    if (params.has('POS')) return extractCircleFromPOS(params.get('POS'));
    const pts = [...params.entries()]
        .map(([, v]) => v)
        .filter((v) => v.toLowerCase()?.includes('circle'))
        .map((cStr) => extractCircleFromPOS(cStr))
        .filter((wp) => wp);
    return (pts.length > 0) ? pts[0] : undefined;
}

function extractCircleFromPOS(circleStr) {
    const c = circleStr?.toLowerCase();
    if (!c?.startsWith('circle')) return;
    const cAry = c.split(' ').filter((s) => s);
    const raNum = cAry[1];
    const decNum = cAry[2];
    if (isNaN(raNum) || isNaN(decNum)) return;
    return makeWorldPt(raNum, decNum);
}

function extractCircleFromADQL(adql) {
    const regEx = /CIRCLE\s?\(.*\)/;
    const result = regEx.exec(adql);
    if (!result) return;
    const circle = result[0];
    const parts = circle.split(',');
    if (parts.length < 4) return;
    let cStr = parts[0].split('(')[1];
    if (!cStr) return;
    if (cStr.startsWith(`\'`) && cStr.endsWith(`\'`)) { // eslint-disable-line quotes
        cStr = cStr.substring(1, cStr.length - 1);
    }
    if (!isNaN(Number(parts[1])) && !isNaN(Number(parts[1]))) {
        return makeWorldPt(parts[1], parts[2], CoordinateSys.parse(cStr));
    }
}