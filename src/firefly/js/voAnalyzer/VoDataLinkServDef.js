/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isArray} from 'lodash';
import {
    adhocServiceUtype, cisxAdhocServiceUtype, standardIDs, VO_TABLE_CONTENT_TYPE,
    SERVICE_DESC_COL_NAMES
} from './VoConst.js';
import {columnIDToName, getColumnIdx, getTblRowAsObj} from '../tables/TableUtil.js';
import {getTableModel} from './VoCoreUtils.js';


/**
 * @param {Object} p
 * @param p.semantics
 * @param p.localSemantics
 * @param p.contentQualifier
 * @param p.contentType
 * @return {DlAnalysisData}
 */
export function analyzeDatalinkRow({semantics = '', localSemantics = '', contentQualifier = '', contentType = ''}) {
    const isImage = contentType?.toLowerCase() === 'image/fits';
    const semL = semantics.toLowerCase();
    const locSemL = localSemantics.toLowerCase();
    const isThis = semL.includes('#this');
    const isAux = semL === '#auxiliary';
    const isGrid = semL.includes('-grid') || (locSemL.includes('-grid') || ( locSemL.includes('#grid')));
    const isCutout = semL.includes('cutout') || semL.includes('#cutout') || semL.includes('-cutout') || locSemL.includes('cutout');
    const isSpectrum = locSemL.includes('spectrum');
    const rBand = semL.includes('-red') || locSemL.includes('-red');
    const gBand = semL.includes('-green') || locSemL.includes('-green');
    const bBand = semL.includes('-blue') || locSemL.includes('-blue');
    const cisxPrimaryQuery = semL.endsWith('cisx#primary-query') || (isThis && locSemL.endsWith('cisx#primary-query'));
    const cisxConcurrentQuery = semL.endsWith('cisx#concurrent-query') || (isThis && locSemL.endsWith('cisx#concurrent-query'));
    const isTar= isTarType(contentType);
    const isGzip= isGzipType(contentType);
    const isSimpleImage= isSimpleImageType(contentType);
    const isDownloadOnly=  isDownloadType(contentType);
    return {
        isThis, isImage, isGrid, isAux, isSpectrum, isCutout, rBand, gBand, bBand,
        cisxPrimaryQuery, cisxConcurrentQuery,
        isTar, isGzip, isSimpleImage, isDownloadOnly
    };
}

export const isTarType= (ct='') => ct.includes('tar');
export const isGzipType= (ct='') => ct.includes('gz');
export const isSimpleImageType= (ct='') => ct.includes('jpeg') || ct.includes('png') || ct.includes('jpg') || ct.includes('gif');
export const isVoTable= (ct='') => ct===VO_TABLE_CONTENT_TYPE;
export const isDownloadType= (ct='') => isTarType(ct) || isGzipType(ct) || ct.includes('octet-stream');

/**
 * determine is a service descriptor is a datalink service descriptor
 * @param {ServiceDescriptorDef} sd
 * @return {boolean}
 */
export const isDataLinkServiceDesc = (sd) => sd?.standardID?.includes(standardIDs.datalink);

/**
 * return true if there are service descriptor blocks in this table, false otherwise
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean} true if there are service descriptors
 */
export const hasServiceDescriptors = (tableOrId) => Boolean(getServiceDescriptors(tableOrId));

function getSDDescription(table, ID) {
    if (!ID) return;
    const serviceDefCol = getColumnIdx(table, 'service_def');
    const descriptionCol = getColumnIdx(table, 'description');
    if (descriptionCol === -1 || serviceDefCol === -1) return;
    if (table.totalRows > 50) return;
    const sdRow = table.tableData.data.find((dAry) => dAry[serviceDefCol] === ID);
    if (!sdRow) return;
    return sdRow[descriptionCol];
}

const gNameMatches = (group, name) => group?.name.toLowerCase() === name?.toLowerCase();


/**
 * return a list of service descriptors found in the table or false
 * @param {String|TableModel} tableOrId
 * @param {boolean} removeAsync
 * @return {Array.<ServiceDescriptorDef>|false}
 */
export function getServiceDescriptors(tableOrId, removeAsync = true) {
    const table = getTableModel(tableOrId);
    if (!table || !isArray(table.resources)) return false;
    const sResources = table.resources.filter(
        (r) => {
            if (!r?.utype || r?.type.toLowerCase() !== 'meta') return false;
            const utype = r.utype.toLowerCase();
            return (utype === adhocServiceUtype || utype === cisxAdhocServiceUtype) &&
                r.params.some((p) => (p.name === 'accessURL' && p.value));
        });
    if (!sResources.length) return false;
    const sdAry = sResources.map(({desc, params, ID, groups, utype}, idx) => (
        {
            ID,
            utype,
            sdSourceTable: table,
            title: desc ?? getSDDescription(table, ID) ?? 'Service Descriptor ' + idx,
            accessURL: params.find(({name}) => name==='accessURL')?.value,
            standardID: params.find(({name}) => name==='standardID')?.value,
            serDefParams: groups
                .find((g) => gNameMatches(g, 'inputParams'))
                ?.params.map((p) => {
                    const optionalParam = !p.ref && !p.value && !p.options;
                    return {
                        ...p,
                        value: isArray(p?.value) && p?.value.length===1 ? p.value[0] : p.value,
                        colName: columnIDToName(table, p.ref),
                        optionalParam,
                        allowsInput: !p.ref,
                        inputRequired: !p.ref && !p.value && !optionalParam
                    };
                }),
            cisxUI: groups.find((g) => gNameMatches(g, 'CISX:ui'))?.params.map((p) => ({...p})),
            cisxTokenSub: groups.find((g) => gNameMatches(g, 'CISX:tokenSub'))?.params.map((p) => ({...p})),
        }
    ));
    if (!removeAsync) return sdAry.length ? sdAry : false;
    const sdAryNoAsync = sdAry.filter(({standardID}) => !standardID?.toLowerCase().includes('async')); // filter out async
    return sdAryNoAsync.length ? sdAryNoAsync : false;
}

/**
 * @param {TableModel|String} dataLinkTableOrId - a TableModel or id that is a datalink call result
 * @return {Array.<DatalinkData>}
 */
export function getDataLinkData(dataLinkTableOrId) {
    const dataLinkTable= getTableModel(dataLinkTableOrId);
    const {data}= dataLinkTable?.tableData ?? {};
    if (!data) return [];
    return data.map((r, idx) => {
            const tmpR = getTblRowAsObj(dataLinkTable, idx);
            const rowObj= Object.fromEntries(  // convert any null or undefined to empty string
                Object.entries(tmpR).map(([k,v]) => ([k,v??''])));

            const {
                semantics, local_semantics: localSemantics, service_def: serviceDefRef,
                content_type: contentType, content_qualifier: contentQualifier,
                access_url: url, description, content_length: size, error_message,
            } = rowObj;

            const idKey= Object.keys(rowObj).find((k) => k.toLowerCase()==='id');
            const serDef= getServiceDescriptorForId(dataLinkTable,serviceDefRef,idx);
            const dlAnalysis= analyzeDatalinkRow({semantics, localSemantics, contentType, contentQualifier});
            return {
                id: rowObj[idKey],
                contentType, contentQualifier, semantics, localSemantics, url, error_message,
                description, size, serviceDefRef, serDef, rowIdx: idx, dlAnalysis,
            };
        })
        .filter(({url='', serviceDefRef, error_message}) =>
            serviceDefRef || error_message || url.startsWith('http') || url.startsWith('ftp'));
}

function getServiceDescriptorForId(table, matchId, dataLinkTableRowIdx) {
    if (!table || !matchId) return;
    const servDescriptorsAry = getServiceDescriptors(table);
    if (servDescriptorsAry) {
        const serDefFound = servDescriptorsAry.find(({ID}) => ID===matchId);
        if (serDefFound) return {...serDefFound, dataLinkTableRowIdx};
    }
}


/**
 * Check to see if the file analysis report indicates the file is a service descriptor
 * @param {FileAnalysisReport} report
 * @returns {boolean} true if the file analysis report indicates a service descriptor
 */
export function isAnalysisTableDatalink(report) {
    if (report?.parts.length !== 1 || report?.parts[0]?.type !== 'Table' || !report?.parts[0]?.details) {
        return false;
    }

    /**@type FileAnalysisPart*/
    const part = report.parts[0];
    const {tableData} = part.details;
    if (!tableData.data?.length) return;
    const tabColNames = tableData.data.map((d) => d?.[0]?.toLowerCase());
    const hasCorrectCols = SERVICE_DESC_COL_NAMES.every((cname) => tabColNames.includes(cname));
    if (!hasCorrectCols) return false;
    return hasCorrectCols && part.totalTableRows < 50; // 50 is arbitrary, it is protections from dealing with files that are very big
}


/**
 *
 * @param {ServiceDescriptorDef} serviceDef
 * @return {string} the standard id string or an empty string
 */
export const getStandardId= (serviceDef) => (serviceDef.standardID??'').toLowerCase();
export const getUtype= (serviceDef) => (serviceDef?.utype??'').toLowerCase();
