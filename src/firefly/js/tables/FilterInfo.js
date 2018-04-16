/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {getColumnIdx, getColumn, isNumericType, getTblById} from './TableUtil.js';
import {Expression} from '../util/expr/Expression.js';
import {isUndefined, get, isArray, isEmpty} from 'lodash';
import {showInfoPopup} from '../ui/PopupUtil.jsx';

const operators = /(!=|>=|<=|<|>|=| like | in | is not | is )/i;

export const FILTER_CONDITION_TTIPS =
`Valid values are one of (=, >, <, !=, >=, <=, LIKE, IS, IS NOT) followed by a value separated by a space.
Or 'IN', followed by a list of values separated by commas. 
Examples:  > 12345; != 3000; IN a,b,c,d`;

export const FILTER_TTIPS =
`Filters are "column_name operator condition" separated by commas.
${FILTER_CONDITION_TTIPS}`;

/**
 * return [column_name, operator, value] triplet.
 * @param {string} input
 * @param {object} options
 * @prop {boolean} options.removeQuotes  remove double-quotes from column name if present
 * @returns {string[]}
 */
function parseInput(input, options={}) {
    const {removeQuotes=false} = options;
    let [cname='', op='', val='', ...rest] = (' '+input).split(operators);
    op = op.trim();
    val = op ? val.trim() : cname.trim();      // when op is missing, val is returned as cname(index 0).
    cname = op ? cname.trim() : '';
    if (!isEmpty(rest)) val += rest.join(); // value contains operators.  just put it back.
    if (removeQuotes) {
        cname = cname.replace(/^"(.+)"$/, '$1');
    }
    return [cname, op, val];
}


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
 * multiple filters are separated by semicolon.
 */
export class FilterInfo {
    constructor() {
        this.filters={};
    }

    /**
     * parse the given filterString into a FilterInfo
     * @param filterString
     * @returns {FilterInfo}
     */
    static parse(filterString) {
        var filterInfo = new FilterInfo();
        filterString && filterString.split(';').forEach( (v) => {
                const [cname, op, val] = parseInput(v, {removeQuotes: true});
                if (cname && op) {
                    filterInfo.addFilter(cname, `${op} ${val}`);
                }
            });
        return filterInfo;
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
                const [cname, op, val] = parseInput(v);
                if (!cname) return v;
                return `${cname} ${autoCorrectCondition(op + ' ' + val)}`;
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
                const [cname, op, val] = parseInput(v);
                return rval && op && val && !cname;
        }, true);
    }

    /**
     * validator for column's filter.  it validates only the condition portion of the filter.
     * @param conditions
     * @param tbl_id
     * @param cname
     * @returns {{valid: boolean, value: (string|*), message: string}}
     */
    static conditionValidator(conditions, tbl_id, cname) {
        conditions = autoCorrectConditions(conditions, tbl_id, cname);
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
                const [cname] = parseInput(v);
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
        let [cname, op, val] = parseInput(filterStr);
        if (!cname) return () => false;       // bad filter.. returns nothing.

        // remove the double quote or the single quote around cname and val (which is added in auto-correction)
        const removeQuoteAroundString = (str, quote = "'") => {
            if (str.startsWith(quote)) {
                const reg = new RegExp('^' + quote + '(.*)' + quote + '$');
                return str.replace(reg, '$1');
            } else {
                return str;
            }
        };

        cname = removeQuoteAroundString(cname, '"');
        op = op.toLowerCase();
        val = val.toLowerCase();

        const cidx = getColumnIdx(tableModel, cname);
        const noROWID = cname === 'ROW_IDX' && cidx < 0;
        const colType = noROWID ? 'int' : get(getColumn(tableModel, cname), 'type', 'char');

        // bRemoveQuote: if remove the single quote enclosing the string
        const convertStrToNumber = (str, bRemoveQuote=true) => {
            const numStr = bRemoveQuote ? removeQuoteAroundString(str) : str;
            const isCastInt = bRemoveQuote && str.startsWith("'") && str.endsWith("'");

            // cast to integer in case it is single quoted
            const num = colType.match(/^[i]/)&&isCastInt ? parseInt(numStr) : parseFloat(numStr);
            return isNaN(num) ? str : num;
        };


        // update val of operator 'in' into an array of value
        if (op === 'in') {
            if (val.match(/^\(.*\)$/)) {
                val = val.substring(1, val.length-1);
            }
            val = val.split(',').map((s) => s.trim());
        }

        // remove single quote enclosing the string for the value of operater 'like' or char type column
        if (op === 'like' || colType.match(/^[sc]/)) {
            val = isArray(val) ? val.map((s) => removeQuoteAroundString(s)) : removeQuoteAroundString(val);
        } else if (colType.match(/^[dfil]/)) {    // convert to number by removing single quote except 'in'
            val =  isArray(val) ? val.map((s) => convertStrToNumber(s, false)) : convertStrToNumber(val);
        }

        return (row, idx) => {
            if (!row) return false;
            var compareTo = noROWID ? idx : row[cidx];
            if (isUndefined(compareTo)) return false;

            if (op !== 'like' && colType.match(/^[dfil]/)) {      // int, float, double, long .. or their short form.
                compareTo = Number(compareTo);
            } else {
                compareTo = compareTo.toLowerCase();
            }

            switch (op) {
                case 'like' :
                    const reg = likeToRegexp(val);
                    return reg.test(compareTo);
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
        return Object.entries(this.filters)
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
        this.filters[colName] = !this.filters[colName] ? conditions : `${this.filters[colName]}; ${conditions}`;
    }

    setFilter(colName, conditions) {
        Reflect.deleteProperty(this.filters, colName);
        if (conditions) {
            conditions.split(';').forEach( (v) => {
                const [, op, val] = parseInput(v);
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
        return this.filters[colName] && this.filters[colName].toString();
    }

    isEqual(colName, value) {
        const oldVal = this.getFilter(colName);
        return (!oldVal && !value) || oldVal === value;
    }
}

/**
 * convert sql like condition to RegExp, the following character cases in like condition are changed
 * - usage of special characters for regular expression
 * - usage of wildcard, % & _, escape wildcard, \% and \_,
 * - usage of \\ is kept same in regular expression
 * @param text
 * @returns {RegExp}
 */
function likeToRegexp(text) {
    const specials = ['/', '.', '*', '+', '?', ':', '!',
        '(', ')', '[', ']', '{', '}', '^', '$', '-', '='
    ];

    const sRE = new RegExp('(\\' + specials.join('|\\') + ')', 'g');

    // r1 is the replacement for w, r2 is the replacement for \w
    function replaceWildCard(str, w, r1, r2, sIdx = 0) {

        const idx = str.indexOf(w, sIdx);

        if (idx < 0) return str;

        const escape = '\\';
        let newStr;
        let totalEscape = 0;

        for (let i = idx-1; i >= sIdx; i--) {
            if (str.charAt(i) !== escape) break;
            totalEscape++;
        }

        if (totalEscape%2 === 0) {    // _=>. %=>.*
            newStr = str.substring(0, idx) + r1 + str.substring(idx+1);
            sIdx = idx + r1.length;
        } else {                      // \_ => _, \% => %
            newStr = str.substring(0, idx-1) + r2 + str.substring(idx+1);
            sIdx = idx - 1 + r2.length;
        }
        return replaceWildCard(newStr, w, r1, r2, sIdx);
    }

    let  newText = text.replace(sRE, '\\$1');

    newText = replaceWildCard(newText, '%', '.*', '%');
    newText = replaceWildCard(newText, '_', '.', '_');

    try {
        return new RegExp('^' + newText + '$');
    }
    catch (e) {
        showInfoPopup('invalid filtering condition: ' + text, 'table filtering');
        return new RegExp();
    }
}

/*-----------------------------------------------------------------------------------------*/

/**
 * Attempt to auto-correct the given condition(s).
 * @param {string} conditions  one or more conditions, separated by ';'
 * @param {string} tbl_id   table ID.
 * @param {string} cname    column name.
 * @returns {string}
 */
function autoCorrectConditions(conditions, tbl_id, cname) {
    const isNumeric = isNumericType(getColumn(getTblById(tbl_id), cname));
    if (conditions) {
        return conditions.split(';')                                // separate them into parts
            .map( (v) => autoCorrectCondition(v, isNumeric))      // auto correct if needed
            .join(';');                                // put them back
    }
}

/**
 * auto correction on filter string in case it is not a valid sql statement.
 * the correction following the rules as follows
 * op : case 1: not specified or 'like' (for text column)
 *              if the value part is not enclosed by single quotes:
 *                  convert %, _, | to be \%, \_ or \\. (escape the wildcard and escape character)
 *                  enclose the string by '%' and then by single quotes.
 *                  ex: abc% => '%abc\%%' after auto-correction.
 *              not specified (for numeric column)
 *                  set op to be '=', no correction on value.
 *      case 2: 'in',
 *              enclose each value in signle quotes in case they are missing (for text column only)
 *              enclose the entire value string in braces, (), if they area missing (for all kinds of columns)
 *              ex: in a, b  => in ('a', 'b') after auto=correction for text column
 *              ex: in a, b  => in (a, b) after auto-correction for numeric column
 *     case 3: other operaters, >, <, >=, <=, =, !=
 *              enclose value in single quotes if they are missing (for text Columns and non-numeric values)
 *              ex: =abc => ='abc' after auto-correction for text column
 *                  =abc => =abc   after auto-correction for numeric column
 *
 * @param v
 * @param isNumeric
 * @returns {*}
 */
function autoCorrectCondition(v, isNumeric=false) {

    const encloseByQuote = (txt, quote="'") => {
        return `${quote}${txt}${quote}`;
    };

    const encloseString = (txt) => {
        return (txt.match(/^'.*'$/) ? txt : encloseByQuote(txt));
    };

    let [, op, val] = parseInput(v);

    // empty string or string with no value
    if (!op && !val) return v.trim();

    op = op ? op.toLowerCase() : (isNumeric ? '=' : 'like');      // no operator is treated as 'like'

    switch (op) {
        case 'like':
            if (!val.match(/^'.*'$/)) {
                val = val.replace(/([_|%|\\])/g, '\\$1');

                val = encloseByQuote(encloseByQuote(val, '%'));
            }
            break;
        case 'in':
            let valList = val.match(/^\((.*)\)$/) ? val.substring(1, val.length-1) : val;

            valList = valList.split(',').map((s) => {
                return isNumeric ? s : encloseString(s.trim());
            }).join(', ');

            val = `(${valList})`;
            break;
        case '=':
        case '!=':
        case '>':
        case '<':
        case '>=':
        case '<=':
            val = isNumeric && !isNaN(val) ? val : encloseString(val);
            break;

        default:
            return '';
    }
    return `${op} ${val}`;
}
