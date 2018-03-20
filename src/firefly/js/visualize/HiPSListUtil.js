import {get, isArray, cloneDeep, set, isFunction, isUndefined, omit} from 'lodash';
import {hipsSURVEYS} from './HiPSUtil.js';
import Enum from 'enum';
import {getAppOptions} from '../core/AppDataCntlr.js';
import {getTblById, isTableLoaded} from '../tables/TableUtil.js';
import {dispatchTableFetch, dispatchTableHighlight} from '../tables/TablesCntlr.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {getColumnIdx} from '../tables/TableUtil.js';
import {ServerParams} from '../data/ServerParams.js';

export const HiPSSurveyTableColumn = new Enum(['Url', 'Title', 'Type', 'Order', 'Coverage', 'Frame', 'Source']);
export const HiPSPopular = 'popular';
export const _HiPSPopular = '_popular';
export const HiPSId = 'hips';
export const HiPSDataType= new Enum([ 'image', 'cube', 'catalog'], { ignoreCase: true });
export const HiPSData = [HiPSDataType.image, HiPSDataType.cube];

export const HiPSMasterTable = {
    defaultFilter: defaultHiPSPopularFilter
};

const HiPSSurvey = 'HiPS_Surveys_';

export function makeHiPSSurveysTableName(hipsId, isPopular = false) {
    const nHipsId = updateHiPSId(hipsId||HiPSId, isPopular);


    return nHipsId.startsWith(HiPSSurvey) ? nHipsId : (HiPSSurvey + nHipsId);
}

export function getAppHiPSConfig() {
    return get(getAppOptions(), ['hips', 'useForImageSearch'], true);
}

/**
 * update HiPS surveys id based on if it is for popular surveys
 * @param id
 * @param isPopular
 * @returns {*}
 */
function updateHiPSId(id, isPopular=false) {

    const sId = id.endsWith(_HiPSPopular) ? id.substring(0, (id.length - _HiPSPopular.length)) : id;
    return isPopular ? sId+_HiPSPopular : sId;
}

/**
 * get HiPS list, used as a callback for HiPS selection
 * @param {Object} params defining dataTypes, id, isPopular, sources, sortOrder for HiPS master query.
 * @param {array} params.dataTypes
 * @param {string} params.id
 * @param {boolean} params.isPopular
 * @param {string} params.sources
 * @param {string} params.sortOrder
 */
export function onHiPSSurveys({dataTypes, id, isPopular, sources = ServerParams.ALL, sortOrder=''}) {
    const tbl_id = makeHiPSSurveysTableName(id, isPopular);
    const types = isArray(dataTypes) ? dataTypes.join(',') : dataTypes;
    const ss = isArray(sources) ? sources.join(',') : sources;
    const so = isArray(sortOrder) ? sortOrder.join(',') : sortOrder;
    if (!getHiPSSurveys(tbl_id)) {
        const req = makeTblRequest('HiPSSearch', 'HiPS Maps',
                                  {[ServerParams.HIPS_DATATYPES]: types,
                                   [ServerParams.HIPS_SOURCES]: ss,
                                   [ServerParams.SORT_ORDER]: so},
                                   {tbl_id, removable: true, pageSize: 1000000});

        dispatchTableFetch(req, 0);
    }
}

export function getPopularHiPSTable(hipsId, hipsUrl) {
    const masterTbl =  getTblById(makeHiPSSurveysTableName(hipsId));
    const hFilter = getHiPSFilter();

    return  (masterTbl && hFilter && hFilter.getPopularTable) ? hFilter.getPopularTable(masterTbl, hipsUrl) : null;
}

export function isOnePopularSurvey(url) {
    const hFilter = getHiPSFilter();

    return (hFilter && hFilter.isOnePopularSurvey) ? hFilter.isOnePopularSurvey(url) : false;
}

function getHiPSFilter() {
    const hFilter = defaultHiPSPopularFilter;  // hard code, hips filter will be configured later

    if (isFunction(hFilter)) {
        return hFilter();
    } else {
        return HiPSMasterTable[hFilter]();
    }
}

function stripTrailingSlash(url) {
    if (typeof url === 'string') {
        return url.trim().replace(/\/$/, '');
    }
    return url;
}


function defaultHiPSPopularFilter() {
    const isOnePopularSurvey = (url) => {
        const defaultUrls =  hipsSURVEYS.map((oneSurvey) => stripTrailingSlash(oneSurvey.url).toLowerCase());

        return defaultUrls.findIndex((dUrl) => dUrl === stripTrailingSlash(url).toLowerCase()) >= 0;
    };

    const createPopularTable = (tableModel, popular_tblId, hipsUrl) => {
        const newTable = omit(cloneDeep(tableModel), ['request', 'origTableModel', 'selectInfo', 'tableMeta'] );
        newTable.tbl_id = popular_tblId;
        set(newTable, ['tableMeta', 'tbl_id'], popular_tblId);

        const cIdx = getColumnIdx(tableModel, HiPSSurveyTableColumn.Source.key);
        if (cIdx < 0 || tableModel.error) {
            return newTable;
        }

        const retData = newTable.tableData.data.reduce((prev, oneRow) => {
            if (oneRow[cIdx].toLowerCase() === 'irsa') {
                prev.push(oneRow);
            }
            return prev;
        }, []);

        newTable.tableData = Object.assign(newTable.tableData, {data: retData});
        newTable.totalRows = retData.length;
        const idx = hipsUrl ? indexInHiPSSurveys(newTable, hipsUrl) : 0;
        set(newTable, 'highlightedRow', (idx < 0 ? 0 : idx));
        set(newTable, 'title', 'default popular HiPS');
        return newTable;
    };

    const getPopularTable = (tableModel, hipsUrl) => {
        const popular_tblId = updateHiPSId(tableModel.tbl_id, true);
        const popularTable = getTblById(popular_tblId);

        return popularTable ? popularTable : createPopularTable(tableModel, popular_tblId, hipsUrl);
    };

    return {
        isOnePopularSurvey,
        getPopularTable
    };
}


/**
 *
 * @param tbl table
 * @param url
 * @returns {*}
 */
export function indexInHiPSSurveys(tbl, url) {
    if (!url || typeof url !== 'string') return 0;
    const cIdx = tbl ? getColumnIdx(tbl, HiPSSurveyTableColumn.Url.key) : -1;

    if (tbl && cIdx >= 0) {
        const newUrl = stripTrailingSlash(url).toLowerCase();

        const data = get(tbl, ['tableData', 'data']);

        return data.findIndex((s) => {
            return (stripTrailingSlash(s[cIdx]).toLowerCase() === newUrl);
        });
    }
    return 0;
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

    if (!tbl || tbl.isFetching) {
        return true;
    }

    return !isTableLoaded(tbl);
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
 * @param url
 * @param hipsId
 * @param isPopular
 */
export function updateHiPSTblHighlightOnUrl(url, hipsId, isPopular=undefined) {
    const hipsOpt =  isUndefined(isPopular) ? [false, true] : [!!isPopular];

    hipsOpt.forEach((cond) => {
        const tblId = makeHiPSSurveysTableName(hipsId, cond);
        const tblHiPS = getHiPSSurveys(tblId);
        if (tblHiPS) {
            let hIdx = indexInHiPSSurveys(tblHiPS, url);

            if (hIdx < 0) {
                hIdx = 0;
            }
            if (hIdx !== tblHiPS.highlightedRow) {
                dispatchTableHighlight(tblId, hIdx);
            }
        }
    });
}