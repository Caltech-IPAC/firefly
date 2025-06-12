import {isObject, isString} from 'lodash';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr';
import {MetaConst} from '../../data/MetaConst';
import {DEFAULT_DATA_PRODUCTS_COMPONENT_KEY} from '../../metaConvert/DataProductConst';
import {getMetaEntry} from '../../tables/TableUtil';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser';
import {parseWorldPt} from '../../visualize/Point';
import {pointIn} from '../../visualize/projection/Projection';
import {getObsCoreSRegion, getSearchTargetFromTable, isDatalinkTable, isRowTargetCapable, makeWorldPtUsingCenterColumns
} from '../../voAnalyzer/TableAnalysis';
import {findWorldPtInServiceDef} from '../../voAnalyzer/VoDataLinkServDef';
import {getDataServiceOptionByTable} from './DataServicesOptions';

const ALL_DATA_PRODUCT_COMPONENTS= 'ALL_DATA_PRODUCT_COMPONENTS';
const PREFER_CUTOUT_KEY = 'preferCutout';
const SD_CUTOUT_SIZE_KEY = 'sdCutoutSize';
const SD_CUTOUT_WP_OVERRIDE = 'sdCutoutWpOverride';
const SD_CUTOUT_TYPE = 'sdCutoutType';
const SD_DEFAULT_SPACIAL_CUTOUT_SIZE = .01;
const SD_DEFAULT_PIXEL_CUTOUT_SIZE = 200;
export const ROW_POSITION = 'ROW_POSITION';
export const SEARCH_POSITION = 'SEARCH_POSITION';
export const USER_ENTERED_POSITION = 'USER_ENTERED_POSITION';
const NOT_ROW_CAPABLE = 'NOT_ROW_CAPABLE';
const ROW_CAPABLE = 'ROW_CAPABLE';

/**
 *
 * @param {String} dataProductsComponentKey
 * @param {String} tbl_id
 * @return {boolean}
 */
export function getPreferCutout(dataProductsComponentKey, tbl_id) {
    const DEF_PREFER_CUTOUT = getDataServiceOptionByTable(PREFER_CUTOUT_KEY, tbl_id);
    const result = getComponentState(dataProductsComponentKey)[PREFER_CUTOUT_KEY];
    if (!result) return DEF_PREFER_CUTOUT;
    if (!isObject(result)) return DEF_PREFER_CUTOUT;
    return result[tbl_id] ?? result.LAST_PREF ?? DEF_PREFER_CUTOUT;
}

function updateAllDataProductsComponents(tbl_id, dataProductsComponentKey) {
    if (!tbl_id || !dataProductsComponentKey) return;
    const all = getComponentState(ALL_DATA_PRODUCT_COMPONENTS);
    if (all[tbl_id]!==dataProductsComponentKey) {
        dispatchComponentStateChange(ALL_DATA_PRODUCT_COMPONENTS, {...all, [tbl_id]:dataProductsComponentKey});
    }
}

/**
 *
 * @param {String} dataProductsComponentKey
 * @param {String} tbl_id
 * @param {boolean} preferCutout
 */
export function setPreferCutout(dataProductsComponentKey=DEFAULT_DATA_PRODUCTS_COMPONENT_KEY, tbl_id, preferCutout) {
    const result = getComponentState(dataProductsComponentKey)[PREFER_CUTOUT_KEY] ?? {};
    const newState = {...result, [tbl_id]: preferCutout, LAST_PREF: preferCutout};
    dispatchComponentStateChange(dataProductsComponentKey, {[PREFER_CUTOUT_KEY]: newState, tbl_id});
    updateAllDataProductsComponents(tbl_id, dataProductsComponentKey);
}


export function getCutoutSize(dataProductsComponentKey=DEFAULT_DATA_PRODUCTS_COMPONENT_KEY, tbl_id) {
    return getComponentState(dataProductsComponentKey, {})[SD_CUTOUT_SIZE_KEY] ??
        getDataServiceOptionByTable('cutoutDefSizeDeg', tbl_id, SD_DEFAULT_SPACIAL_CUTOUT_SIZE);
}

export function setCutoutSize(dataProductsComponentKey, cutoutSize, tbl_id) {
    const result = getComponentState(dataProductsComponentKey);
    const newState= {
        ...result,
        [SD_CUTOUT_SIZE_KEY]: cutoutSize,
    };
    dispatchComponentStateChange(dataProductsComponentKey, newState);
    updateAllDataProductsComponents(tbl_id, dataProductsComponentKey);
}

/**
 *  equivalent to setPreferCutout, setCutoutSize, setCutoutTargetOverride
 * @param dataProductsComponentKey
 * @param tbl_id
 * @param serDef
 * @param cutoutType
 * @param cutoutSize
 * @param overrideTarget
 * @param preferCutout
 */
export function setAllCutoutParams(dataProductsComponentKey=DEFAULT_DATA_PRODUCTS_COMPONENT_KEY, tbl_id, serDef, cutoutType,
                                   cutoutSize, overrideTarget = undefined, preferCutout = true) {
    const result = getComponentState(dataProductsComponentKey);
    const newPreferState = {...result[PREFER_CUTOUT_KEY], [tbl_id]: preferCutout, LAST_PREF: preferCutout};

    const {lookupTblId} = getIdForCutoutType(tbl_id, serDef);
    const newCutoutTypeState = {...result[SD_CUTOUT_TYPE], [lookupTblId]: cutoutType};
    let wp = overrideTarget;
    if (overrideTarget && isString(overrideTarget)) {
        wp = parseWorldPt(overrideTarget);
    }

    dispatchComponentStateChange(dataProductsComponentKey,
        {
            tbl_id,
            [PREFER_CUTOUT_KEY]: newPreferState,
            [SD_CUTOUT_SIZE_KEY]: cutoutSize,
            [SD_CUTOUT_WP_OVERRIDE]: wp,
            [SD_CUTOUT_TYPE]: newCutoutTypeState,
        });
    updateAllDataProductsComponents(tbl_id, dataProductsComponentKey);
}

export const tblIdToKey= (tbl_id) => getComponentState(ALL_DATA_PRODUCT_COMPONENTS)?.[tbl_id] ?? DEFAULT_DATA_PRODUCTS_COMPONENT_KEY;

export const getCutoutTargetOverride = (dataProductsComponentKey=DEFAULT_DATA_PRODUCTS_COMPONENT_KEY) =>
    getComponentState(dataProductsComponentKey, {})[SD_CUTOUT_WP_OVERRIDE];

function getIdForCutoutType(tbl_id, serDef) {
    let lookupTblId = tbl_id;
    let canDoRow = false;
    if (tbl_id) canDoRow = isRowTargetCapable(tbl_id);

    if (!canDoRow && serDef && !isDatalinkTable(tbl_id)) { //if this is a datalink table (extracted from obs core type tbl, we don't want to change the lookupTblId)
        canDoRow = Boolean(serDef?.rowWP);
        lookupTblId = canDoRow ? ROW_CAPABLE : NOT_ROW_CAPABLE;
    }
    return {lookupTblId, canDoRow};
}

/**
 *
 * @param {String} dataProductsComponentKey
 * @param {String} tbl_id
 * @param {ServiceDescriptorDef} serDef
 * @return {string|*|string}
 */
export function getCutoutTargetType(dataProductsComponentKey=DEFAULT_DATA_PRODUCTS_COMPONENT_KEY, tbl_id, serDef) {

    const {lookupTblId, canDoRow} = getIdForCutoutType(tbl_id, serDef);
    const cutoutTypeObj = getComponentState(dataProductsComponentKey, {})[SD_CUTOUT_TYPE];
    if (cutoutTypeObj?.[lookupTblId]) return cutoutTypeObj[lookupTblId];


    // compute fallback from table
    const tableCutoutPref = getMetaEntry(tbl_id, MetaConst.OBSCORE_CUTOUT_TYPE);
    if (tableCutoutPref === SEARCH_POSITION) return SEARCH_POSITION;
    if (tableCutoutPref === ROW_POSITION && canDoRow) return ROW_POSITION;

    // compute fallback from obscore options
    const typeFromOptions = getDataServiceOptionByTable(SD_CUTOUT_TYPE, tbl_id);
    if (typeFromOptions === SEARCH_POSITION) return SEARCH_POSITION;
    if (typeFromOptions === ROW_POSITION && canDoRow) return ROW_POSITION;

    // compute fallback from the default based on if a search target exist
    if (canDoRow) return Boolean(getSearchTargetFromTable(tbl_id)) ? SEARCH_POSITION : ROW_POSITION;

    // fallback to SEARCH_POSITION
    return SEARCH_POSITION;
}

export function findPreferredCutoutTarget(dataProductsComponentKey=DEFAULT_DATA_PRODUCTS_COMPONENT_KEY, serDef, table, row) {
    const cutoutType = getCutoutTargetType(dataProductsComponentKey, table?.tbl_id, serDef);
    let positionWP;
    let foundType;
    const requestedType = cutoutType;
    if (cutoutType === SEARCH_POSITION) {
        positionWP = getSearchTargetFromTable(table);
        if (!positionWP) positionWP = serDef?.positionWP;
        if (positionWP) {
            foundType = SEARCH_POSITION;
        } else {
            if (isDatalinkTable(table)) positionWP = parseWorldPt(getMetaEntry(table, MetaConst.ROW_TARGET, undefined));
            else positionWP = makeWorldPtUsingCenterColumns(table, row);
            foundType = ROW_POSITION;
        }
    } else if (cutoutType === ROW_POSITION) {
        if (isDatalinkTable(table)) {
            //positionWP to come from metadata
            positionWP = parseWorldPt(getMetaEntry(table, MetaConst.ROW_TARGET, undefined));
        }
        else positionWP = makeWorldPtUsingCenterColumns(table, row);
        if (!positionWP) positionWP = serDef?.rowWP;
        if (positionWP) foundType = ROW_POSITION;
    } else if (getCutoutTargetOverride(dataProductsComponentKey)) {
        positionWP = getCutoutTargetOverride(dataProductsComponentKey);
        if (positionWP) foundType = USER_ENTERED_POSITION;
    }
    return {requestedType, foundType, positionWP: positionWP ?? findWorldPtInServiceDef(serDef, row)};
}

export function findCutoutTarget(dataProductsComponentKey=DEFAULT_DATA_PRODUCTS_COMPONENT_KEY, serDef, table, row) {
    const result = findPreferredCutoutTarget(dataProductsComponentKey, serDef, table, row);
    const {positionWP} = result;
    const sRegion = getObsCoreSRegion(table, row);
    if (!sRegion) return result;

    const {valid, drawObj} = parseObsCoreRegion(sRegion) ?? {valid: false};
    if (!valid) return result;

    if (pointIn(drawObj.pts, positionWP)) return result;
    const fallback = makeWorldPtUsingCenterColumns(table, row);
    return {requestedType: result.requestedType, foundType: fallback ? ROW_POSITION : undefined, positionWP: fallback};
}

export function getCutoutErrorStr(foundType, requestedType) {
    if (foundType === requestedType) return '';
    return (foundType === ROW_POSITION && requestedType === SEARCH_POSITION) ?
        'Warning: Search position is not on this image, using position from row' :
        (foundType === ROW_POSITION && requestedType === USER_ENTERED_POSITION) ?
            'Warning: User entered cutout position is not on this image, using position from row' :
            'Target is not on cutout, using position from row';
}