/**
 * Created by loi on 1/15/16.
 */


/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
const sortInfo_regex = new RegExp('\\s*SortInfo=(\\S+),(\\S+)');
export const SORT_ASC = 'ASC';
export const SORT_DESC = 'DESC';
export const UNSORTED = '';

/**
 * convenience class to handle the table's sort information.
 * data is stored as a string of 'SortInfo=(ASC|DESC),col1[,col2]*.  ie.  'SortInfo=ASC,id,band'
 **/
 export class SortInfo {
    constructor(direction=UNSORTED, sortColumns=[]) {
        this.direction = direction !== SORT_DESC ? SORT_ASC : direction;
        this.sortColumns = sortColumns;
    }

    getDirection(colName) {
        if (this.sortColumns.includes(colName)) {
            return this.direction;
        } else {
            return UNSORTED;
        }
    }

    toggle(colName) {
        const dir = this.getDirection(colName);
        this.direction = dir === UNSORTED ? SORT_ASC :
                         dir === SORT_ASC ? SORT_DESC : UNSORTED;
        this.sortColumns = [colName];
        return this;
    }

    serialize() {
        return this.direction === UNSORTED ? '' : `SortInfo=${this.direction},${this.sortColumns.toString()}`;
    }

    static parse(sortInfo) {
        if (sortInfo) {
            const parts = sortInfo.trim().match(sortInfo_regex);
            if (parts) {
                const direction = parts[1] && parts[1].toUpperCase();
                const sortColumns = parts[2] && parts[2].split(',');
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
