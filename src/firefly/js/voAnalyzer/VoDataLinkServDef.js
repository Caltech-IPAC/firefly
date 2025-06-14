/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isArray} from 'lodash';
import {MetaConst} from '../data/MetaConst';
import {isDefined} from '../util/WebUtil';
import {makeWorldPt, parseWorldPt} from '../visualize/Point';
import {
    getObsCoreAccessFormat, getObsCoreAccessURL, getObsCoreProdType, getObsCoreSRegion, getSearchTarget,
    isDatalinkTable, isObsCoreLike,
} from './TableAnalysis';
import {
    adhocServiceUtype, cisxAdhocServiceUtype, standardIDs, VO_TABLE_CONTENT_TYPE,
    SERVICE_DESC_COL_NAMES, RA_UCDs, DEC_UCDs
} from './VoConst.js';
import {
    columnIDToName, getCellValue, getColumnByRef, getColumnIdx, getMetaEntry, getTblRowAsObj
} from '../tables/TableUtil.js';
import {getTableModel} from './VoCoreUtils.js';


/**
 * @param {Object} p
 * @param p.semantics
 * @param p.localSemantics
 * @param p.contentQualifier
 * @param p.contentType
 * @param p.sourceObsCoreData
 * @return {DlAnalysisData}
 */
export function analyzeDatalinkRow({semantics = '', localSemantics = '',
                                       contentQualifier = '', contentType = '',
                                       sourceObsCoreData}) {
    const isImage = contentType?.toLowerCase() === 'image/fits';
    const maybeImage = contentType?.toLowerCase().includes('fits');
    const semL = semantics.toLowerCase();
    const locSemL = localSemantics.toLowerCase();
    const isThis = semL.includes('#this');
    const isCounterpart = semL.includes('#counterpart');
    const isCalibration = semL.includes('#calibration');
    const isAux = semL === '#auxiliary';
    const isGrid = semL.includes('-grid') || (locSemL.includes('-grid') || ( locSemL.includes('#grid')));
    const isCutout = semL.includes('cutout') || semL.includes('#cutout') || semL.includes('-cutout') || locSemL.includes('cutout');
    const isSpectrum = locSemL.includes('spectrum') ||
        sourceObsCoreData?.dataproduct_type?.toLowerCase()?.includes('spectrum');

    const rBand = semL.includes('-red') || locSemL.includes('-red');
    const gBand = semL.includes('-green') || locSemL.includes('-green');
    const bBand = semL.includes('-blue') || locSemL.includes('-blue');
    const cisxPrimaryQuery = semL.endsWith('cisx#primary-query') || (isThis && locSemL.endsWith('cisx#primary-query'));
    const cisxConcurrentQuery = semL.endsWith('cisx#concurrent-query') || (isThis && locSemL.endsWith('cisx#concurrent-query'));
    const isSimpleImage= isSimpleImageType(contentType);
    const isDownloadOnly=  isDownloadType(contentType);
    return {
        isThis, isCounterpart, isCalibration, isImage, maybeImage, isGrid, isAux, isSpectrum, isCutout, rBand, gBand, bBand,
        cisxPrimaryQuery, cisxConcurrentQuery, isSimpleImage, isDownloadOnly, cutoutFullPair:false,
    };
}

export const isGzipType= (ct='') => ct.includes('gzip') || ct.includes('gz');
export const isPlainTextType= (ct='') => ct.toLowerCase()==='text/plain';
export const isHtmlType= (ct='') => ct.toLowerCase()==='text/html' || ct.toLowerCase()==='application/html';
export const isSimpleImageType= (ct='') => ct.includes('jpeg') || ct.includes('png') || ct.includes('jpg') || ct.includes('gif');
export const isPDFType= (ct='') => ct.toLowerCase()==='application/pdf' || ct.toLowerCase()==='application/x-pdf';
export const isJSONType= (ct='') => ct.toLowerCase()==='application/json' || ct.toLowerCase()==='application/x-json';
export const isVoTable= (ct='') => ct===VO_TABLE_CONTENT_TYPE;
export const isTarType= (ct='') => ct.toLowerCase()==='application/tar' || ct.toLowerCase()==='application/x-tar';
export const isYamlType= (ct='') => {
    const ymlList= ['application/yaml', 'application/x-yaml', 'text/yaml'];
    const ctL= ct.toLowerCase();
    return ymlList.some( (y) => ctL===y);
};
export const isDownloadType= (ct='') => isTarType(ct) || isGzipType(ct) || ct.includes('octet-stream');
export function getDownloadTypeDesc(contentType) {
    if (!isDownloadType(contentType)) return '';
    if (isTarType(contentType)) return 'tar';
    if (isGzipType(contentType)) return 'gzip';
}

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
            internalServiceDescriptorID: `sd-${table.tbl_id}---${idx}`,
            sdSourceTable: table,
            positionWP: parseWorldPt(getMetaEntry(table, MetaConst.SEARCH_TARGET, undefined)),
            rowWP: parseWorldPt(getMetaEntry(table, MetaConst.ROW_TARGET, undefined)),
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
 * @param {boolean} [includeUnusable] include entries that have errors, are async service descriptors, or have other problems
 * @param {TableModel} [sourceObsCoreTbl]
 * @param {number} [sourceObsCoreRow]
 * @return {Array.<DatalinkData>}
 */
export function getDataLinkData(dataLinkTableOrId, includeUnusable= false, sourceObsCoreTbl=undefined, sourceObsCoreRow=-1) {
    const dataLinkTable= getTableModel(dataLinkTableOrId);
    const {data}= dataLinkTable?.tableData ?? {};
    if (!data || !isDatalinkTable(dataLinkTable)) return [];
    const sourceObsCoreData= getObsCoreData(sourceObsCoreTbl,sourceObsCoreRow);
    const positionWP= parseWorldPt(getMetaEntry(dataLinkTable, MetaConst.SEARCH_TARGET, undefined));
    const rowWP= parseWorldPt(getMetaEntry(dataLinkTable, MetaConst.ROW_TARGET, undefined));
    const sRegion= getMetaEntry(dataLinkTable, MetaConst.S_REGION, undefined);

    const dlDataAryAll=  data
        .map((r, idx) => {
            const tmpR = getTblRowAsObj(dataLinkTable, idx);
            const rowObj= Object.fromEntries(  // convert any null or undefined to empty string
                Object.entries(tmpR).map(([k,v]) => ([k,v??''])));

            const {
                semantics, local_semantics: localSemantics, service_def: serviceDefRef,
                content_type: contentType, content_qualifier: contentQualifier,
                access_url: url, description, content_length: size, error_message,
                label: labelDLExt, bandpass_name: bandpassNameDLExt
            } = rowObj;

            const idKey= Object.keys(rowObj).find((k) => k.toLowerCase()==='id');
            const serDef= getServiceDescriptorForId(dataLinkTable,serviceDefRef,idx);
            const dlAnalysis= analyzeDatalinkRow({semantics, localSemantics,
                contentType, contentQualifier,sourceObsCoreData});
            dlAnalysis.usableEntry= Boolean((serviceDefRef && serDef) || error_message || url.startsWith('http') || url.startsWith('ftp'));
            return {
                id: rowObj[idKey],
                contentType:contentType?.toLowerCase(), contentQualifier, semantics, localSemantics, url, error_message,
                description, size, serviceDefRef, serDef, rowIdx: idx, dlAnalysis,
                sourceObsCoreData, relatedDLEntries: {}, positionWP, rowWP, sRegion,
                labelDLExt, bandpassNameDLExt
            };
        });

    const dlDataAry= includeUnusable ? dlDataAryAll : dlDataAryAll.filter((dl) => dl.dlAnalysis.usableEntry);

    dlDataAry.forEach( (dlData) => {
        if (!dlData.dlAnalysis.usableEntry) return;
        const {dlAnalysis:{isThis,isCounterpart,maybeImage,isCutout}, id}= dlData;
        if ((isThis || isCounterpart) && maybeImage && !isCutout) {
            const foundCutout= dlDataAry.filter( (testData) => testData.id===id && testData.dlAnalysis.isCutout);
            if (foundCutout.length===1) setupRelatedCutout(dlData,foundCutout[0]);
            // todo in future search for a related region for main image or cutout here
        }
    });
    return dlDataAry;
}

function setupRelatedCutout(prim,cutout) {
    prim.relatedDLEntries.cutout= cutout;
    cutout.relatedDLEntries.fullImage= prim;
    prim.dlAnalysis.cutoutFullPair= true;
    cutout.dlAnalysis.cutoutFullPair= true;
    const rBand= prim.dlAnalysis.rBand || cutout.dlAnalysis.rBand;
    const gBand= prim.dlAnalysis.gBand || cutout.dlAnalysis.gBand;
    const bBand= prim.dlAnalysis.bBand || cutout.dlAnalysis.bBand;
    prim.dlAnalysis.rBand= cutout.dlAnalysis.rBand= rBand;
    prim.dlAnalysis.gBand= cutout.dlAnalysis.gBand= gBand;
    prim.dlAnalysis.bBand= cutout.dlAnalysis.bBand= bBand;

    //?? todo determine if this is right
    if (prim.dlAnalysis.isThis) cutout.dlAnalysis.isThis= true;
    if (prim.dlAnalysis.isCounterpart) cutout.dlAnalysis.isCounterpart= true;
    if (prim.dlAnalysis.isGrid || cutout.dlAnalysis.isGrid) {
        prim.dlAnalysis.isGrid= true;
        cutout.dlAnalysis.isGrid= true;
    }
}

export function getObsCoreData(tbl,row) {


    if (!tbl || row<0) return undefined;
    if (!isObsCoreLike(tbl)) return undefined;
    const obsCoreData= getTblRowAsObj(tbl, row);
    if (!obsCoreData) return undefined;

    obsCoreData.dataproduct_type ??= getObsCoreProdType(tbl,row);
    obsCoreData.s_region ??= getObsCoreSRegion(tbl,row);
    obsCoreData.access_url ??= getObsCoreAccessURL(tbl,row);
    obsCoreData.access_format ??= getObsCoreAccessFormat(tbl,row);
    return obsCoreData;
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

export function findWorldPtInServiceDef(serDef,sourceRow) {
    const {serDefParams,sdSourceTable, dataLinkTableRowIdx} = serDef ?? {};
    if (!serDefParams || !sdSourceTable) return;
    const raParam= serDefParams.find( ({UCD=''}) =>
        RA_UCDs.find( (testUcd) => UCD.toLowerCase().includes(testUcd)) );
    const decParam= serDefParams.find( ({UCD=''}) =>
        DEC_UCDs.find( (testUcd) => UCD.toLowerCase().includes(testUcd)) );
    if (!raParam && !decParam) return;

    let raVal= raParam.value;
    let decVal= decParam.value;

    if (raVal && decVal) return makeWorldPt(raVal,decVal);

    const hasDLTable= isDatalinkTable(sdSourceTable);
    const hasDLRow= isDefined(dataLinkTableRowIdx);
    const hasSourceRow= isDefined(sourceRow);
    const row= hasDLTable && hasDLRow ? dataLinkTableRowIdx : hasSourceRow ? sourceRow : undefined;

    if (!raVal && raParam.ref) {
        const col = getColumnByRef(sdSourceTable, raParam.ref);
        if (col && row > -1) raVal = getCellValue(sdSourceTable, row, col.name);
    }

    if (!decVal && decParam.ref) {
        const col = getColumnByRef(sdSourceTable, decParam.ref);
        if (col && row > -1) decVal = getCellValue(sdSourceTable, row, col.name);
    }

    return (raVal && decVal) ? makeWorldPt(raVal,decVal) : undefined;
}


/**
 *
 * @param {ServiceDescriptorDef} serviceDef
 * @return {string} the standard id string or an empty string
 */
export const getStandardId= (serviceDef) => (serviceDef.standardID??'').toLowerCase();
export const getUtype= (serviceDef) => (serviceDef?.utype??'').toLowerCase();
