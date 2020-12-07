import {get, isArray, isEmpty} from 'lodash';
import Enum from 'enum';
import {getAppOptions} from '../core/AppDataCntlr.js';
import {getTblById, isTableLoaded} from '../tables/TableUtil.js';
import {dispatchTableFetch, dispatchTableHighlight} from '../tables/TablesCntlr.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {getColumnIdx} from '../tables/TableUtil.js';
import {ServerParams} from '../data/ServerParams.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';
import {TABLE_LOADED} from '../tables/TablesCntlr';
import {isBlankHiPSURL} from './WebPlot.js';

export const HiPSId = 'hips';
export const HiPSDataType= new Enum([ 'image', 'cube', 'catalog'], { ignoreCase: true });
export const HiPSData = [HiPSDataType.image, HiPSDataType.cube];
export const HiPSSources = ServerParams.IRSA + ',' + ServerParams.CDS;

const HiPSSurvey = 'HiPS_Surveys_';
export const IVO_ID_COL= 'CreatorID';
export const URL_COL= 'Url';
const BLANK_HIPS_URL= 'ivo://CDS/P/2MASS/color';

export function makeHiPSSurveysTableName(hipsId, sources) {
    const nHipsId = updateHiPSId(hipsId||HiPSId, (sources===ServerParams.ALL ? HiPSSources : sources));


    return nHipsId.startsWith(HiPSSurvey) ? nHipsId : (HiPSSurvey + nHipsId);
}

export function getAppHiPSConfig() {
    return get(getAppOptions(), ['hips', 'useForImageSearch'], false);
}

const itemStrToAry = (items, sep = ',') => {
    return !items ? [] : items.split(sep).reduce((prev, oneItem) => {
        if (oneItem.trim()) {
            prev.push(oneItem.trim().toLowerCase());
        }
        return prev;
    }, []);
};

const HIPS_SEARCH = 'hips';
/**
 * get all available HiPS sources
 * @returns {string}
 */
export function getHiPSSources() {
    let srcs =  get(getAppOptions(), [HIPS_SEARCH, ServerParams.HIPS_SOURCES], ServerParams.ALL);

    if (srcs.toLowerCase() === ServerParams.ALL.toLowerCase()) {
        srcs = HiPSSources;
    }

    return itemStrToAry(srcs).join(',');
}

/**
 * get default sources info including source and label for the checkbox
 * @returns {Array}
 */
export function defHiPSSources() {
    const defObj = get(getAppOptions(), [HIPS_SEARCH, ServerParams.HIPS_DEFSOURCES]);
    const allSources = getHiPSSources();

    if (allSources.length < 1) return [];
    if (isEmpty(defObj)) {
        return [{source: allSources[0], label: allSources[0].toUpperCase()}];
    }

    return (isArray(defObj) ? defObj : [defObj]).reduce((prev, oneDef) => {
        if (oneDef.source && oneDef.source.trim()) {
            const source = oneDef.source.trim().toLowerCase();

            if (allSources.split(',').includes(source)) {
                const label = oneDef.label && oneDef.label.trim() ? oneDef.label.trim() : '';

                prev.push({source, label});
            }
        }
        return prev;
    }, []);
}

/**
 * get default HiPS source
 * @returns {string}
 */
export function getDefHiPSSources() {
    const defInfo = defHiPSSources();

    return defInfo.map((oneSource) => (oneSource.source)).join(',');
}

/**
 * get default HiPS source label for the checkbox
 * @returns {string}
 */
export function getDefHiPSLabel() {
    const defInfo = defHiPSSources();

    return defInfo.map((oneSource) => (oneSource.label)).join('.');
}

/**
 * get HiPS source priority for the merged list
 * @returns {string}
 */
export function getHiPSMergePriority() {
    const mergeP = get(getAppOptions(), [HIPS_SEARCH, ServerParams.HIPS_MERGE_PRIORITY], '');

    return itemStrToAry(mergeP).join('.');
}


/**
 * update HiPS surveys id based on if it is for popular surveys
 * @param id
 * @param sources
 * @returns {*}
 */
function updateHiPSId(id, sources) {
    const sortedSources = sources.split(',').filter((s) => s)
                                            .map((s) => s.toLowerCase())
                                            .sort();
    const idSuffix = '__' + sortedSources.join('_');  // suffix in lower case
    return id.endsWith(idSuffix) ? id : (id + idSuffix);
}

/**
 * @summary get HiPS list, used as a callback for HiPS selection
 *  @param {Object} params defining dataTypes, id, isPopular, sources, sortOrder for HiPS master query.
 *  @param {string} params.dataTypes HiPS data types to be searched
 *  @param {string} params.id  for table id
 *  @param {string} params.sources HiPS sources to be searched, the sequence determines the order in the merged list
 *  @param {string} params.sourceMergePriority the sequence determines the priority in the merged list
 */
function onHiPSSurveys({dataTypes, id, sources=getDefHiPSSources(),
                                              sourceMergePriority = getHiPSMergePriority()}) {
    if (!sources) return;

    const joinItems = (its) => (isArray(its) ? its.filter((s)=>s).join(',') : its);
    const tbl_id = makeHiPSSurveysTableName(id, sources);
    const types = joinItems(dataTypes);
    const ss = joinItems(sources);
    const sp = joinItems(sourceMergePriority);

    if (!getHiPSSurveys(tbl_id)) {
        const req = makeTblRequest('HiPSSearch', 'HiPS Maps',
                                  {[ServerParams.HIPS_DATATYPES]: types,
                                   [ServerParams.HIPS_SOURCES]: ss,
                                   [ServerParams.HIPS_MERGE_PRIORITY]: sp},
                                   {tbl_id, removable: true, pageSize: 10000});

        dispatchTableFetch(req, 0);
    }
}

export function loadHiPSSurverysWithHighlight({dataTypes, id, sources=getDefHiPSSources(),
                                  ivoOrUrl, columnName = IVO_ID_COL}) {
    getHiPSSurveysTable(dataTypes, id, sources)
        .then((tableModel) => {
            if (ivoOrUrl && tableModel && tableModel.tableData) {
                const hIdx = indexInHiPSSurveys(tableModel, ivoOrUrl, columnName);

                if (hIdx >= 0 && hIdx !== tableModel.highlightedRow) {
                    dispatchTableHighlight(tableModel.tbl_id, hIdx);
                }
            }
        });
}

//todo
export function getHiPSSurveysTable(dataTypes, id, sources= getHiPSSources()) {

    const hipsSurveys = getHiPSSurveys(makeHiPSSurveysTableName(id, sources));
    if (hipsSurveys && hipsSurveys.tableData) return Promise.resolve(hipsSurveys);


    return new Promise((resolve) => {
        const watcher= (action, cancelSelf) =>{
            const {tbl_id}= action.payload;
            if (tbl_id!==makeHiPSSurveysTableName(id, sources)) return;
            const loadSurveys = getHiPSSurveys(tbl_id);
            resolve(loadSurveys);
            cancelSelf();
        };
        dispatchAddActionWatcher({actions:[TABLE_LOADED], callback: watcher});
        onHiPSSurveys({dataTypes,id,sources});
    });
}


/**
 * resolve a ivo hips id to a URL if a url is passed just return it.
 * @param {string} ivoOrUrl - a url or a IVO id
 * @return {Promise} a promise the resolves to a url
 */
export function resolveHiPSIvoURL(ivoOrUrl) {
    if (!ivoOrUrl) return Promise.reject(new Error('empty url'));
    if (ivoOrUrl.startsWith('http')) return Promise.resolve(ivoOrUrl);
    if (isBlankHiPSURL(ivoOrUrl)) ivoOrUrl= BLANK_HIPS_URL;

    return getHiPSSurveysTable([HiPSDataType.image, HiPSDataType.cube], 'hipsResolveTable')
        .then( (tableModel) => {
            const ivoIdx= getColumnIdx(tableModel, IVO_ID_COL);
            const urlIdx= getColumnIdx(tableModel, URL_COL);
            if (ivoIdx<0 || urlIdx<1) return undefined;
            const lowerIvo= ivoOrUrl.toLowerCase();
            // now match the table
            const foundRow= get(tableModel,'tableData.data', []).find( (row) =>
                                row[ivoIdx] && row[ivoIdx].toLowerCase().includes(lowerIvo) );
            const replaceUrl= foundRow && foundRow[urlIdx];
            return replaceUrl || ivoOrUrl;
        });
}


function stripTrailingSlash(url) {
    if (typeof url === 'string') {
        return url.trim().replace(/\/$/, '');
    }
    return url;
}

/**
 *
 * @param tbl table
 * @param cellVal url or ivoId
 * @param columnName  URL_COL or IVO_ID_COL
 * @returns {*}
 */
export function indexInHiPSSurveys(tbl, cellVal, columnName = URL_COL) {
    if (!cellVal || typeof cellVal !== 'string') return 0;
    const cIdx = tbl ? getColumnIdx(tbl, columnName) : -1;

    if (tbl && cIdx >= 0) {
        const newVal = columnName === URL_COL ? stripTrailingSlash(cellVal).toLowerCase() : cellVal.toLowerCase();

        const data = get(tbl, ['tableData', 'data']);

        return data.findIndex((s) => {
            return columnName === URL_COL ? (stripTrailingSlash(s[cIdx]).toLowerCase() === newVal):
                   s[cIdx].toLowerCase().includes(newVal);
        });

    }
    return -1;
}

/**
 * get HiPS survey list from the store
 * @param id table id
 * @returns {*}
 */
export function getHiPSSurveys(id) {
    return getTblById(id);
}


/**
 * check if HiPS survey list is under loading
 * @param id table id
 * @returns {*}
 */
export function isLoadingHiPSSurverys(id) {
    if (!id.startsWith(HiPSSurvey)) {
        id = HiPSSurvey + id;
    }
    const tbl = getHiPSSurveys(id);

    return tbl ? (tbl.isFetching ? true : !isTableLoaded(tbl)) : false;
}


/**
 * get loading message if there is
 * @param id
 * @returns {*}
 */
export function getHiPSLoadingMessage(id) {
    //return get(flux.getState(), [HIPSSURVEY_PATH, id, 'message']);
    const tbl = getHiPSSurveys(id);

    return get(tbl, 'error', '');
}

/**
 *  update highlighted row of both original master hips table and popular hips table if there is
 * @param val   url or ivoId
 * @param hipsId
 * @param sources
 * @param colName URL_COL or IVO_ID_COL
 */
export function updateHiPSTblHighlightOnUrl(val, hipsId, sources, colName=URL_COL) {
    const tblId = makeHiPSSurveysTableName(hipsId, sources);
    const tblHiPS = getHiPSSurveys(tblId);

    if (tblHiPS) {
        const hIdx = indexInHiPSSurveys(tblHiPS, val, colName);
        if (hIdx >= 0 && hIdx !== tblHiPS.highlightedRow) {
            dispatchTableHighlight(tblId, hIdx);
        }
    }

}

