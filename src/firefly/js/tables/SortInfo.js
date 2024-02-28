import {isArray} from 'lodash';
import {splitCols} from 'firefly/tables/TableUtil.js';

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
export const SORT_ASC = 'ASC';
export const SORT_DESC = 'DESC';
export const UNSORTED = '';


/**
 * convenience function to create the serialized string representation of SortInfo
 * @param {string|array} colName one column's name or multiple columns' names
 * @param {boolean} [isAscending=true] false for descending order, otherwise it's default to ascending.
 */
export function sortInfoString(colName, isAscending=true) {
    if (!colName) return '';
    return !isArray(colName) ? SortInfo.newInstance( (isAscending ? SORT_ASC : SORT_DESC), colName).serialize() :
                               SortInfo.newInstance( (isAscending ? SORT_ASC : SORT_DESC), ...colName).serialize();
}

/**
 * convenience class to handle the table's sort information.
 * data is stored as a string of '(ASC|DESC),col1[,col2]*.  ie.  'ASC,id,band'
 **/
 export class SortInfo {
    constructor(direction=UNSORTED, sortColumns=[]) {
        this.direction = direction;
        this.sortColumns = sortColumns;
    }

    /**
     * returns the sort direction of the given column name based on
     * this SortInfo.
     * @param colName
     * @returns {*}
     */
    getDirection(colName) {
        colName = colName.replace(/^"(.+)"$/, '$1');           // strip quotes if any;
        if (this.sortColumns[0] === colName) {
            return this.direction;
        } else {
            return UNSORTED;
        }
    }

    /**
     * returns the sortInfo of the next toggle state.
     * @param {string[]} cnames an array of the column(s) to sort
     * @returns {SortInfo}
     */
    toggle(cnames=[]) {
        if (cnames.length === 0) return;
        const dir = this.getDirection(cnames[0]);
        const direction = dir === UNSORTED ? SORT_ASC :
                          dir === SORT_ASC ? SORT_DESC : UNSORTED;
        const sortColumns = direction === UNSORTED ? [] : cnames;
        return new SortInfo(direction, sortColumns).serialize();
    }

    serialize() {
        return this.direction === UNSORTED ? '' :
            `${this.direction},${this.sortColumns.map( (c) => c.includes('"') ? c : `"${c}"`).join()}`;         // if there's quotes in column name, don't add quotes.
    }

    static parse(sortInfo) {
        if (sortInfo) {
            const parts = splitCols(sortInfo).map((s) => s.trim());
            if (parts) {
                const direction = parts[0] && parts[0].toUpperCase();
                const sortColumns = parts[1] && parts.slice(1).map( (c) => c.replace(/^"(.+)"$/, '$1'));           // strip quotes if any
                return new SortInfo(direction, sortColumns);
            }
        } else {
            return new SortInfo();
        }
    }

    static newInstance(direction, ...cols) {
        return new SortInfo(direction, cols);
    }
}
