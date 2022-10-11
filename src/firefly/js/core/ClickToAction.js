import {sprintf} from '../externalSource/sprintf.js';

export const SearchTypes =  { point: 'point', pointRadius: 'pointRadius', pointSide: 'pointSide', area: 'area',point_table_only:'point_table_only', table:'table' };
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
 * @param {String} label - should be very short, used to build full label
 * @param {String} tip
 * @param {String} searchType - on of the value default by SearchTypes - one of  'point', 'pointRadius', 'area'
 * @param {number} min
 * @param {number} max
 * @param {Function} execute
 * @param {Function} [supported] a function, default to a return true
 * @param {String} [verb] - defaults to 'Search'
 * @returns {ClickToActionCommand}
 */
export function makeSearchAction(cmd, label, tip, searchType, min, max, execute, supported= DEF_SUPPORTED, verb=DEFAULT_VERB) {

    if (searchType!==SearchTypes[searchType]) searchType= SearchTypes.point;
    return {
        cmd, label, tip, searchType, min, max, supported, execute, verb
    };
}


export function getSearchTypeDesc(sa, wp, size, areaPtsLength) {
    switch (sa.searchType) {
        case SearchTypes.point_table_only:
        case SearchTypes.point:
            return `${sa.verb} ${sa.label} at position center`;
        case SearchTypes.pointRadius:
            const r= size > sa.max ? sa.max : size < sa.min ? sa.min : size;
            return `${sa.verb} (cone) using ${sa.label} with radius of ${sprintf('%.4f',r)} degrees`;
        case SearchTypes.pointSide:
            const s= size > sa.max ? sa.max : size < sa.min ? sa.min : size;
            return `${sa.verb} using ${sa.label}  with side of ${s}`;
        case SearchTypes.area:
            return `${sa.verb} (polygon) using ${sa.label} around an area (${areaPtsLength??0} points)`;
        case SearchTypes.table:
            return `${sa.verb} ${sa.label}`;
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

