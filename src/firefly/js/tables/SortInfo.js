/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
export const SORT_ASC = 'ASC';
export const SORT_DESC = 'DESC';
export const UNSORTED = '';


/**
 * convenience function to create the serialized string representation of SortInfo
 * @param {string} colName column's name
 * @param {boolean} [isAscending=true] false for descending order, otherwise it's default to ascending.
 */
export function sortInfoString(colName, isAscending=true) {
    if (!colName) return '';
    return SortInfo.newInstance( (isAscending ? SORT_ASC : SORT_DESC), colName).serialize();
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
        if (this.sortColumns[0] === colName) {
            return this.direction;
        } else {
            return UNSORTED;
        }
    }

    /**
     * returns the sortInfo string of the next toggle state.
     * @param colName
     * @returns {string}
     */
    toggle(colName) {
        const name = colName.split(',')[0].trim();
        const dir = this.getDirection(name);
        const direction = dir === UNSORTED ? SORT_ASC :
                          dir === SORT_ASC ? SORT_DESC : UNSORTED;
        const sortColumns = UNSORTED ? [] : [colName];
        return direction === UNSORTED ? '' : `${direction},${sortColumns.toString()}`;
    }

    serialize() {
        return this.direction === UNSORTED ? '' : `${this.direction},${this.sortColumns.toString()}`;
    }

    static parse(sortInfo) {
        if (sortInfo) {
            const parts = sortInfo.split(',').map((s) => s.trim());
            if (parts) {
                const direction = parts[0] && parts[0].toUpperCase();
                const sortColumns = parts[1] && parts.slice(1);
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
