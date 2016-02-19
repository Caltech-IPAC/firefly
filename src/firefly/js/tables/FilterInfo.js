/**
 * Created by loi on 1/15/16.
 */


/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty, merge} from 'lodash';


const op_regex = new RegExp('(<|>|>=|<=|=|!=|like|in)');
const cond_regex = new RegExp('^' + op_regex.source + '\\s+(.+)', 'i');
const filter_regex = new RegExp('(\\S+)\\s+' + op_regex.source + '\\s+(.+)', 'i');

/**
 * convenience class to handle the table's filter information.
 * data is stored as a string of 'col op expression' separated by comma.  ie.  'id > 1, id < 100,band <= 2'
 * convert to filterInfo:
 * {
 *      col: ['op expression']
 * }
 * use parse and serialize to object to string and vice-versa
 */
export class FilterInfo {
    constructor() {
    }

    static parse(filterString) {
        var filterInfo = new FilterInfo();
        if (filterString) {
            filterString && filterString.split(';').forEach( (v) => {
                    const parts = v.trim().match(filter_regex);
                    if (parts.length > 3) filterInfo.addFilter(parts[1], `${parts[2]} ${parts[3]}`);
                });
        }
        return filterInfo;
    }

    static isValid(conditions) {
        return isEmpty(conditions) || conditions.split(';').reduce( (rval, v) => {
            return rval && cond_regex.test(v.trim());
        }, true);
    }

    static validator(conditions) {
        const valid = FilterInfo.isValid(conditions);
        const message = valid ? '' : 'Invalid syntax';
        return {valid, message};
    }

    serialize() {
        return Object.keys(this).reduce( (rval, key) => {
            this[key].split(';').forEach((v) => {
                if (v.length) {
                rval = (rval.length ? rval + ';': '') + `${key} ${v.trim()}`;
                }
            } );
            return rval;
        }, '');
    }

    addFilter(colName, conditions) {
        this[colName] = isEmpty(this[colName]) ? conditions : `${this[colName]}; ${conditions}`;
    }

    setFilter(colName, conditions) {
        Reflect.deleteProperty(this, colName);
        if (!isEmpty(conditions)) {
            conditions && conditions.split(';').forEach( (v) => {
                const parts = v.trim().match(cond_regex);
                if (parts.length > 2) this.addFilter(colName, `${parts[1]} ${parts[2]}`);
            });
        }
    }

    getFilter(colName) {
        return this[colName] && this[colName].toString();
    }

    isEqual(colName, value) {
        const oldVal = this.getFilter(colName);
        return (isEmpty(oldVal) && isEmpty(value)) || oldVal === value;
    }
}
