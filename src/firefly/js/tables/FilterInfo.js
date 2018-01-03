/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {getColumnIdx, getColumn} from './TableUtil.js';
import {Expression} from '../util/expr/Expression.js';
import {isUndefined, get} from 'lodash';

const cond_regex = new RegExp('(!=|>=|<=|<|>|=|like|in)\\s*(.+)');
const cond_only_regex = new RegExp('^' + cond_regex.source, 'i');
const filter_regex = new RegExp('(\\S+)\\s*' + cond_regex.source, 'i');

export const FILTER_CONDITION_TTIPS =
`Valid values are one of (=, >, <, !=, >=, <=, LIKE) followed by a value separated by a space.
Or 'IN', followed by a list of values separated by commas. 
Examples:  > 12345; != 3000; IN a,b,c,d`;

export const FILTER_TTIPS =
`Filters are "column_name operator condition" separated by commas.
${FILTER_CONDITION_TTIPS}`;


/**
 * convenience class to handle the table's filter information.
 * data is stored as a string of 'col op expression' separated by comma.  ie.  'id > 1, id < 100,band <= 2'
 * convert to filterInfo:
 * {
 *      col: ['op expression']
 * }
 * use parse and serialize to object to string and vice-versa
 * in this context:
 * filter is column_name = conditions
 * condition is operator + value(s)
 * multiple conditions are separated by comma.
 * multiple filters are separated by semicolon.
 */
export class FilterInfo {
    constructor() {
    }

    /**
     * parse the given filterString into a FilterInfo
     * @param filterString
     * @returns {FilterInfo}
     */
    static parse(filterString) {
        var filterInfo = new FilterInfo();
        filterString && filterString.split(';').forEach( (v) => {
                let [, cname, op, val] = v.trim().match(filter_regex) || [];
                if (cname && op) {
                    cname = cname.replace(/"(.+?)"/g, '$1');      // strip quotes if any
                    filterInfo.addFilter(cname, `${op} ${val}`);
                }
            });
        return filterInfo;
    }

    /**
     * given a list of conditions separated by semicolon,
     * transform them into valid conditions if they are not already so.
     * @param conditions
     * @returns {string}
     */
    static autoCorrect(conditions) {
        if (conditions) {
            const parts = conditions.split(';').map( (v) => {
                var [, op, val] = v.trim().replace(/[()]/g, '').match(cond_only_regex) || [];
                [op, val] = op ? [op, val] : [ 'like', v.trim() ];  // defualt to 'like' if no operators found
                val = op.toLowerCase() === 'in' ? `(${val})` : val;     // add parentheses when 'in' is used.
                return `${op} ${val}`;
           });
            return parts.join(';');
        } else {
            return conditions;
        }
    }

    /**
     * given a list of filters separated by semicolon,
     * transform them into valid filters if they are not already so.
     * @param filterInfo
     * @returns {string}
     */
    static autoCorrectFilter(filterInfo) {
        if (filterInfo) {
            const filters = filterInfo.split(';').map( (v) => {
                const [, cname, op, val] = v.trim().match(filter_regex) || [];
                if (!cname) return v;
                return `${cname} ${FilterInfo.autoCorrect(op + ' ' + val)}`;
            });
            return filters.join(';');
        } else {
            return filterInfo;
        }
    }

    /**
     * validate the conditions
     * @param conditions
     * @returns {boolean}
     */
    static isConditionValid(conditions) {
        return !conditions || conditions.split(';').reduce( (rval, v) => {
            return rval && (!v || cond_only_regex.test(v.trim()));
        }, true);
    }

    /**
     * validator for column's filter.  it validates only the condition portion of the filter.
     * @param conditions
     * @returns {{valid: boolean, value: (string|*), message: string}}
     */
    static conditionValidator(conditions) {
        conditions = FilterInfo.autoCorrect(conditions);
        const valid = FilterInfo.isConditionValid(conditions);
        return {valid, value: conditions, message: FILTER_CONDITION_TTIPS};
    }

    /**
     * validator for column's filter.  it validates only the condition portion of the filter.
     * @param conditions
     * @returns {{valid: boolean, value: (string|*), message: string}}
     */
    static conditionValidatorNoAutoCorrect(conditions) {
        const valid = FilterInfo.isConditionValid(conditions);
        return {valid, value: conditions, message: FILTER_CONDITION_TTIPS};
    }

    /**
     * validate the filterInfo string
     * @param {string} filterInfo
     * @param {TableColumn[]} columns array of column definitions
     * @returns {Array.<boolean, string>} isValid plus an error message if isValid is false.
     */
    static isValid(filterInfo, columns = []) {
        const rval = [true, ''];
        const allowCols = columns.concat({name:'ROW_IDX'});
        if (filterInfo && filterInfo.trim().length > 0) {
            filterInfo = filterInfo.replace(/"(.+?)"/g, '$1'); // remove quotes
            return filterInfo.split(';').reduce( ([isValid, msg], v) => {
                const [, cname] = v.trim().match(filter_regex) || [];
                if (!cname) {
                        msg += `\n"${v}" is not a valid filter.`;
                    } else if (!allowCols.some( (c) => c.name === cname)) {
                        const expr = new Expression(cname, allowCols.map((s)=>s.name));
                        if (!expr.isValid()) {
                            msg += `\n"${v}" unrecognized column or expression.\n`;
                        }
                    }
                    return [!msg, msg];
                }, rval);
        } else {
            return rval;
        }
    }

    /**
     * validator for free-form filters field
     * @param {TableColumn[]} columns array of column definitions
     * @param {string} filterInfo
     * @returns {{valid: boolean, value: (string|*), message: string}}
     */
    static validator(columns, filterInfo) {
        filterInfo = FilterInfo.autoCorrectFilter(filterInfo);
        const [valid, message] = FilterInfo.isValid(filterInfo, columns);
        return  {valid, value: filterInfo, message};
    }

    /**
     * returns a comparator function that takes a row(string[]) as parameter.
     * This comparator will returns true if the given row passes the given filterStr.
     * @param {string} filterStr
     * @param {TableModel} tableModel
     * @returns {function(): boolean}
     */
    static createComparator(filterStr, tableModel) {
        var [ , cname, op, val] =filterStr.match(filter_regex) || [];
        if (!cname) return () => false;       // bad filter.. returns nothing.

        // remove the double quote around the key
        const removeQuoteAroundString = (str) => {
            return str.replace(/^"(.*)"$/, '$1');
        };

        cname = removeQuoteAroundString(cname);
        op = op.toLowerCase();
        val = val.toLowerCase();
        const cidx = getColumnIdx(tableModel, cname);
        const noROWID = cname === 'ROW_IDX' && cidx < 0;
        const colType = noROWID ? 'int' : get(getColumn(tableModel, cname), 'type', 'char');
        val = op === 'in' ? val.replace(/[()]/g, '').split(',').map((s) => s.trim()) : val;

        return (row, idx) => {
            if (!row) return false;
            var compareTo = noROWID ? idx : row[cidx];
            if (isUndefined(compareTo)) return false;

            if (!['in','like'].includes(op) && colType.match(/^[dfil]/)) {      // int, float, double, long .. or their short form.
                val = Number(val);
                compareTo = Number(compareTo);
            } else {
                compareTo = compareTo.toLowerCase();
            }

            switch (op) {
                case 'like'  :
                    return compareTo.includes(val);
                case '>'  :
                    return compareTo > val;
                case '<'  :
                    return compareTo < val;
                case '='  :
                    return compareTo === val;
                case '!='  :
                    return compareTo !== val;
                case '>='  :
                    return compareTo >= val;
                case '<='  :
                    return compareTo <= val;
                case 'in'  :
                    return val.includes(compareTo);
                default :
                    return false;
            }
        };
    }



    serialize(formatKey) {
        if (!formatKey) {
            // add quotes to key if it does not contains quotes
            formatKey = (k) => k.includes('"') ? k : `"${k}"`;
        }
        return Object.entries(this)
                    .map(([k,v]) => v.split(';')
                                    .filter((f) => f)
                                    .map( (f) => `${formatKey(k)} ${f}`)
                                    .join(';'))
                    .join(';');
    }

    /**
     * add additional conditions to the given column.
     * @param colName
     * @param conditions
     */
    addFilter(colName, conditions) {
        this[colName] = !this[colName] ? conditions : `${this[colName]}; ${conditions}`;
    }

    setFilter(colName, conditions) {
        Reflect.deleteProperty(this, colName);
        if (conditions) {
            conditions.split(';').forEach( (v) => {
                const [, op, val] = v.trim().match(cond_only_regex) || [];
                if (op) this.addFilter(colName, `${op} ${val}`);
            });
        }
    }

    /**
     * returns the string value of this columns filter info.
     * multiple filters are separated by comma.
     * @param colName
     * @returns {string}
     */
    getFilter(colName) {
        return this[colName] && this[colName].toString();
    }

    isEqual(colName, value) {
        const oldVal = this.getFilter(colName);
        return (!oldVal && !value) || oldVal === value;
    }
}
