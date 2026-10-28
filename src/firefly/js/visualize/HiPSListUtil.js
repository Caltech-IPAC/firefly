import {get, isArray} from 'lodash';
import Enum from 'enum';
import {getAppOptions} from '../core/AppDataCntlr.js';
import {MetaConst} from '../data/MetaConst';
import {makeTblRequest, MAX_ROW} from '../tables/TableRequestUtil.js';
import {getColumnIdx, doFetchTable} from '../tables/TableUtil.js';
import {ServerParams} from '../data/ServerParams.js';
import {Logger} from '../util/Logger.js';
import {isBlankHiPSURL} from './WebPlot.js';
import {fixHiPSRoot} from './WebPlotRequest';

/**
 * @typedef HiPSDataType
 * @prop image
 * @prop cube
 * @prop catalog
 * @type {Enum}
 */
export const HiPSDataType= new Enum([ 'image', 'cube', 'catalog'], { ignoreCase: true });
export const HiPSData = [HiPSDataType.image, HiPSDataType.cube];
export const HiPSSources = ServerParams.IRSA + ',' + ServerParams.CDS;

export const IVO_ID_COL= 'CreatorID';
export const URL_COL= 'Url';
export const IVOAID_COL= 'CreatorID';
export const TITLE_COL= 'Title';

const BLANK_HIPS_URL= 'https://irsa.ipac.caltech.edu/data/hips/CDS/2MASS/Color'; // todo: only for properties, in future blank should just have hardcoded properties
const HIPS_SEARCH = 'hips';
const HIPS_LIST_TIMEOUT_MS= 5000;
const logger= Logger('HiPSList');
let FULL_HIPS_TABLE;            // saved for efficiency .. used by resolveHiPSIvoURL
let fullHipsTablePromise;       // in-flight fetch, shared so concurrent or timed-out callers don't refetch



export function useForImageSearch() {
    return get(getAppOptions(), ['hips', 'useForImageSearch'], false);
}

// convert comma separated string into trimmed-lowercase.
/**
 *
 * @param {String} items
 * @param {String} sep
 * @return {Array.<String>}
 */
const toCsvLowercase = (items, sep = ',') => {
    return items?.split(sep).map( (s) => s?.trim()?.toLowerCase()).join(sep);
};

/**
 * get all available HiPS sources
 * @returns {string}
 */
export function getHiPSSources() {
    const srcs = getAppOptions()?.[HIPS_SEARCH]?.[ServerParams.HIPS_SOURCES] ?? ServerParams.ALL;
    return (srcs.toLowerCase() === ServerParams.ALL.toLowerCase()) ? HiPSSources : toCsvLowercase(srcs);
}

/**
 * get default sources with source and label.  Put in an array if more than one.
 * i.e. {source: 'abc', label: 'ABC featured'}
 * @returns {Array}
 */
export function defHiPSSources() {
    let defObj = get(getAppOptions(), [HIPS_SEARCH, ServerParams.HIPS_DEFSOURCES]);
    if (!defObj) {
        // if not given, take the first entry from all sources
        defObj = getHiPSSources().split(',')?.map((s='') => ({source: s.trim(), label: s.trim().toUpperCase()}));
        defObj = defObj?.[0];
    }
    return isArray(defObj) ? defObj : [defObj];}


/**
 * get HiPS source priority for the merged list
 * @returns {Array.<string>}
 */
export function getHiPSMergePriority() {
    const mergeP = get(getAppOptions(), [HIPS_SEARCH, ServerParams.HIPS_MERGE_PRIORITY], '');
    return toCsvLowercase(mergeP);
}


export function makeHiPSRequest(tableType,
                                sources=getHiPSSources(), mocSources,
                                adhocMocIncludeAdditionSources, tbl_id,
                                types=HiPSData) {
    const sourceMergePriority = getHiPSMergePriority();
    const sp = sourceMergePriority?.join?.(',') || sourceMergePriority;
    const params=
        {
            [ServerParams.HIPS_DATATYPES]: types?.join(','),
            [ServerParams.HIPS_SOURCES]: sources,
            [ServerParams.HIPS_TABLE_TYPE]: tableType,
            [ServerParams.HIPS_MERGE_PRIORITY]: sp,
        };
    if (mocSources) params[ServerParams.ADHOC_SOURCE]= mocSources.join(',');
    if (adhocMocIncludeAdditionSources) params[ServerParams.ADHOC_MOC_INCLUDE]= adhocMocIncludeAdditionSources;

    return makeTblRequest('HiPSSearch', 'HiPS Maps', params,
        {
            tbl_id,
            pageSize: MAX_ROW,
            META_INFO: {
                'col.Properties.PrefWidth':4,
                'col.Properties.label':'Props',
                [MetaConst.IMAGE_SOURCE_ID]  : 'FALSE'
            }
        });
}

function convertIvoToUrl(tableModel,ivo)  {
    const ivoIdx= getColumnIdx(tableModel, IVO_ID_COL);
    const urlIdx= getColumnIdx(tableModel, URL_COL);
    if (ivoIdx<0 || urlIdx<1) return ivo;
    const lowerIvo= ivo.toLowerCase();
    // now match the table
    const allRows= tableModel?.tableData.data ?? [];
    const foundRow= allRows.find( (row) =>
        row?.[ivoIdx]?.toLowerCase().includes(lowerIvo) );
    return foundRow?.[urlIdx] || ivo;
}

async function ensureInitFullHipsTable() {
    if (FULL_HIPS_TABLE) return;
    fullHipsTablePromise ??= doFetchTable(makeHiPSRequest('hips'))
        .then((tableModel) => { FULL_HIPS_TABLE= tableModel; })
        .catch((error) => logger.error('HiPS list fetch failed', error))
        .finally(() => { fullHipsTablePromise= undefined; });
    const timeout= new Promise((resolve) => setTimeout(resolve, HIPS_LIST_TIMEOUT_MS));
    await Promise.race([fullHipsTablePromise, timeout]);
    if (!FULL_HIPS_TABLE) logger.warn(`HiPS list not available after ${HIPS_LIST_TIMEOUT_MS}ms, continuing without it`);
}


/**
 * resolve a ivo hips id to a URL if a url is passed just return it.
 * @param {string} ivoOrUrl - a url or a IVO id
 * @return {Promise} a promise the resolves to a url
 */
export async function resolveHiPSIvoURL(ivoOrUrl) {
    if (!ivoOrUrl) throw new Error('empty url');
    if (ivoOrUrl.startsWith('http')) return ivoOrUrl;
    if (isBlankHiPSURL(ivoOrUrl)) {
        return BLANK_HIPS_URL;
    }
    await ensureInitFullHipsTable();
    return fixHiPSRoot(convertIvoToUrl(FULL_HIPS_TABLE,ivoOrUrl));
}

export async function isUrlInHipsList(url='') {
    await ensureInitFullHipsTable();
    if (!FULL_HIPS_TABLE?.tableData?.data || !FULL_HIPS_TABLE?.totalRows) return false;
    const urlIdx= getColumnIdx(FULL_HIPS_TABLE, URL_COL);
    if (urlIdx<1) return false;
    const allRows= FULL_HIPS_TABLE.tableData.data;
    const urlLower= url.toLowerCase();
    const foundRow= allRows.find( (row=[]) => row[urlIdx]?.toLowerCase().includes(urlLower) );
    return Boolean(foundRow?.[urlIdx]);
}




let hipsInit= false;

export async function ensureHiPSInit() {
    if (hipsInit) return;
    hipsInit= true;
    void await resolveHiPSIvoURL('ivoa://nothing');
}