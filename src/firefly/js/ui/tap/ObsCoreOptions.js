import {isObject, isString} from 'lodash';
import {getAppOptions} from '../../core/AppDataCntlr';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr';
import {MetaConst} from '../../data/MetaConst';
import {getMetaEntry} from '../../tables/TableUtil';
import {parseWorldPt} from '../../visualize/Point';
import {getSearchTarget, makeWorldPtUsingCenterColumns} from '../../voAnalyzer/TableAnalysis';
import {findWorldPtInServiceDef} from '../../voAnalyzer/VoDataLinkServDef';

const PREFER_CUTOUT_KEY = 'preferCutout';
const SD_CUTOUT_SIZE_KEY = 'sdCutoutSize';
const SD_CUTOUT_WP_OVERRIDE = 'sdCutoutWpOverride';
const SD_DEFAULT_SPACIAL_CUTOUT_SIZE = .01;
const SD_DEFAULT_PIXEL_CUTOUT_SIZE = 200;



export const getTapObsCoreOptions = (obsCoreSource) =>
    getAppOptions().tapObsCore?.[obsCoreSource] ?? getAppOptions().tapObsCore ?? {};

/**
 * @param key
 * @param [obsCoreSource]
 * @param [defVal]
 * @return {*}
 */
export function getObsCoreOption(key, obsCoreSource = undefined, defVal) {
    const slOps = obsCoreSource ? getAppOptions().tapObsCore?.[obsCoreSource] ?? {} : {};
    const ops = getAppOptions().tapObsCore ?? {};
    return slOps[key] ?? ops[key] ?? defVal;
}

export function getTapObsCoreOptionsGuess(serviceLabelGuess) {
    const {tapObsCore = {}} = getAppOptions();
    if (!serviceLabelGuess) return tapObsCore;
    const guessKey = Object.entries(tapObsCore)
        .find(([key, value]) => isObject(value) && serviceLabelGuess.includes(key))?.[0];
    return getTapObsCoreOptions(guessKey);
}

/**
 *
 * @param {String} dataProductsComponentKey
 * @param {String} tbl_id
 * @return {boolean}
 */
export function getPreferCutout(dataProductsComponentKey, tbl_id) {
    const obsCoreSource = getMetaEntry(tbl_id, MetaConst.OBSCORE_SOURCE_ID);
    const DEF_PREFER_CUTOUT = getObsCoreOption(PREFER_CUTOUT_KEY, obsCoreSource, true);
    const result = getComponentState(dataProductsComponentKey)[PREFER_CUTOUT_KEY];
    if (!result) return DEF_PREFER_CUTOUT;
    if (!isObject(result)) return DEF_PREFER_CUTOUT;
    return result[tbl_id] ?? result.LAST_PREF ?? DEF_PREFER_CUTOUT;
}

/**
 *
 * @param {String} dataProductsComponentKey
 * @param {String} tbl_id
 * @param {boolean} preferCutout
 */
export function setPreferCutout(dataProductsComponentKey, tbl_id, preferCutout) {
    const result = getComponentState(dataProductsComponentKey)[PREFER_CUTOUT_KEY] ?? {};
    const newState = {...result, [tbl_id]: preferCutout, LAST_PREF: preferCutout};
    dispatchComponentStateChange(dataProductsComponentKey, {[PREFER_CUTOUT_KEY]: newState});
}

export function getCutoutSize(dataProductsComponentKey, tbl_id) {
    const obsCoreSource = getMetaEntry(tbl_id, MetaConst.OBSCORE_SOURCE_ID);
    return getComponentState(dataProductsComponentKey, {})[SD_CUTOUT_SIZE_KEY] ??
        getObsCoreOption('cutoutDefSizeDeg', obsCoreSource, SD_DEFAULT_SPACIAL_CUTOUT_SIZE);
}

export const setCutoutSize= (dataProductsComponentKey, cutoutSize) =>
    dispatchComponentStateChange(dataProductsComponentKey, { [SD_CUTOUT_SIZE_KEY]: cutoutSize});

/**
 *  equivalent to setPreferCutout, setCutoutSize, setCutoutTargetOverride
 * @param dataProductsComponentKey
 * @param tbl_id
 * @param cutoutSize
 * @param overrideTarget
 * @param preferCutout
 */
export function setAllCutoutParams(dataProductsComponentKey, tbl_id, cutoutSize, overrideTarget = undefined, preferCutout) {
    const result = getComponentState(dataProductsComponentKey)[PREFER_CUTOUT_KEY] ?? {};
    const newPreferState = {...result, [tbl_id]: preferCutout, LAST_PREF: preferCutout};
    let wp= overrideTarget;
    if (overrideTarget && isString(overrideTarget)) {
        wp= parseWorldPt(overrideTarget);
    }
    dispatchComponentStateChange(dataProductsComponentKey,
        {
            [PREFER_CUTOUT_KEY]: newPreferState,
            [SD_CUTOUT_SIZE_KEY]: cutoutSize,
            [SD_CUTOUT_WP_OVERRIDE]: wp,
        });
}

export const setCutoutTargetOverride= (dataProductsComponentKey, wp)=>
    dispatchComponentStateChange(dataProductsComponentKey, {[SD_CUTOUT_WP_OVERRIDE]: wp});

export const getCutoutTargetOverride= (dataProductsComponentKey) =>
    getComponentState(dataProductsComponentKey, {})[SD_CUTOUT_WP_OVERRIDE];

export function findCutoutTarget(dataProductsComponentKey, serDef, table, row) {
    let positionWP = getSearchTarget(table?.request, table) ?? makeWorldPtUsingCenterColumns(table, row);
    if (!positionWP) positionWP= serDef?.positionWP;
    if (getCutoutTargetOverride(dataProductsComponentKey)) return getCutoutTargetOverride(dataProductsComponentKey);
    return positionWP ?? findWorldPtInServiceDef(serDef, row);
}