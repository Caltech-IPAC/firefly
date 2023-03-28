import {isFunction, isString} from 'lodash';
import {sprintf} from '../externalSource/sprintf.js';

export const SearchTypes =  { point: 'point', pointRadius: 'pointRadius', pointSide: 'pointSide', area: 'area',point_table_only:'point_table_only', wholeTable:'wholeTable' };
export const DEFAULT_VERB= 'Search';
const DEF_SUPPORTED= () => true;



/**
 *
 * @typedef {Object} ClickToActionCommand
 * @summary a command to execute from a selection of a search
 *
 * @prop {string} cmd
 * @prop {string} label
 * @prop {string} tip
 * @prop {string} searchType - one of  'point', 'point_table_only', 'pointRadius', 'pointSide', 'area', 'table'
 * @prop {number} [min]
 * @prop {number} [max]
 * @prop {String} verb
 * @prop {Function} execute - for spacial types: execute(searchAction, cenWpt:WorldPt, radius:number, corners:Array.<WorldPt>), for tables execute(searchAction, table)
 * @prop {Function} supported - return boolean call with same parameters as execute
 *
 */
//todo- ClickToActionCommand will need a url builder type config this is only necessary if we want to
//      have configs from an external json object, lsst will probably want this


/**
 *
 * @param {String} cmd
 * @param {String} groupId
 * @param {String} label - should be very short, used to build full label
 * @param {String} tip
 * @param {String} searchType - on of the value default by SearchTypes - one of  'point', 'pointRadius', 'area'
 * @param {number} min
 * @param {number} max
 * @param {Function} execute
 * @param {String|Function} [searchDesc] - if string- the text of the search description, if function return the text call with f(wp, size, areaPtsLength)
 * @param {String} [verb] - defaults to 'Search'
 * @param {Function} [supported] a function, default to a return true
 * @returns {ClickToActionCommand}
 */
export function makeSearchAction(cmd, groupId, label, tip, searchType, min, max, execute, searchDesc=undefined, verb=DEFAULT_VERB, supported= DEF_SUPPORTED) {
    if (searchType!==SearchTypes[searchType]) searchType= SearchTypes.point;
    return { cmd, label, tip, searchType, min, max, supported, execute, verb, searchDesc, groupId};
}

export function makeSearchActionObj({ cmd, groupId, label='', tip, searchType, min, max, execute,
                                        searchDesc=undefined,
                                        verb=DEFAULT_VERB, supported= DEF_SUPPORTED
                                    }, ) {
    if (searchType!==SearchTypes[searchType]) searchType= SearchTypes.point;
    return {cmd, label, tip, searchType, min, max, supported, execute, verb, searchDesc, groupId};
}

export function getSearchTypeDesc(sa, wp, size, areaPtsLength) {

    const {searchDesc, searchType, verb, label, min, max}= sa;
    if (searchDesc) {
        if (isString(searchDesc)) return searchDesc;
        if (isFunction(searchDesc)) {
            const title= searchDesc(wp,size,areaPtsLength);
            if (isString(title)) return title;
        }
    }

    switch (searchType) {
        case SearchTypes.point_table_only:
            return `${verb} ${label} at row`;
        case SearchTypes.point:
            return `${verb} ${label} at region center`;
        case SearchTypes.pointRadius:
            const r= size > max ? max : size < min ? min : size;
            return `${verb} (cone) using ${label} with radius of ${sprintf('%.4f',r)} degrees`;
        case SearchTypes.pointSide:
            const s= size > max ? max : size < min ? min : size;
            return `${verb} using ${label}  with side of ${s}`;
        case SearchTypes.area:
            return `${verb} (polygon) using ${label} around an area (${areaPtsLength??0} points)`;
        case SearchTypes.wholeTable:
            return `${verb} ${label}`;
        default:
            return 'Search';
    }
}


export function searchMatches({searchType}, hasCone,hasArea,hasPt) {
    if (hasCone && searchType===SearchTypes.pointRadius) return true;
    if (hasArea && searchType===SearchTypes.area) return true;
    if (hasPt && searchType===SearchTypes.point) return true;
    return false;
}


export const getValidSize= (sa, size) => size > sa.max ? sa.max : size < sa.min ? sa.min : size;

