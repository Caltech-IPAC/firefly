/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {getColumnIdx, getColumn, isColumnType, COL_TYPE, getTblById, stripColumnNameQuotes} from './TableUtil.js';
import {Expression} from '../util/expr/Expression.js';
import {isNil, get, isArray, isEmpty, isNumber} from 'lodash';
import {showInfoPopup} from '../ui/PopupUtil.jsx';
import {strictParseInt} from '../util/WebUtil.js';

const operators = /(!=|>=|<=|<|>|=| like | in | is not | is )/i;

// filter group separator
export const FILTER_SEP = ' _AND_ ';        // internal use need to match what's defined in TableServerRequest.FILTER_SEP

export const FILTER_CONDITION_TTIPS =
`Valid values are one of (=, >, <, !=, >=, <=, LIKE, IS, IS NOT) followed by a value separated by a space.
Or 'IN', followed by a list of values separated by commas.
You may combine conditions with either 'and' or 'or'. 
Examples:  > 12345 or != 3000 and IN a,b,c,d`;

export const FILTER_TTIPS =
`Conditional statements in the form of "column_name" operator value separated by semicolon.
* operator is one of =, >, <, !=, >=, <=, LIKE, IN, IS, IS NOT
* column_name must be enclosed in double quotes
* string value must be enclosed in single quotes
* when IN is used, enclose the values in parentheses
* you may combine conditions with either 'and' or 'or'
* grouping by parentheses is not supported at the moment
Examples:  
  "ra" > 5 and "color" != 'blue' or "band" IN (1,2,3) 
`;

// ('${FILTER_SEP}' serves as groups separator for now when grouping is necessary)
// "ra" < 5 or "ra" > 6 _AND_ "dec" < -1 or "dec" > 1


export const NULL_TOKEN = '%NULL';          // need to match DbAdapter.NULL_TOKEN

// condition separator
const COND_SEP = new RegExp('( and | or )', 'i');

export function getFiltersAsSql(tbl_id) {
    const filters = get(getTblById(tbl_id), 'request.filters', '').split(FILTER_SEP);
    if (filters.length === 0) return;
    else if (filters.length === 1) return filters[0];
    else return filters.map( (f) => `(${f})`).join(' AND ');
}

/**
 * returns the number of individual filters
 * @param request   table request
 * @returns {number}
 */
export function getNumFilters(request) {
    const {filters, sqlFilter} = request || {};
    let count = 0;
    if (sqlFilter) count++;
    if (filters) {
        count += filters.split(new RegExp(` and | or |${FILTER_SEP}`, 'i')).length;
    }
    return count;
}


/**
 * return [column_name, operator, value] triplet.
 * @param {string} input
 * @param {object} options
 * @prop {boolean} options.removeQuotes  remove double-quotes from column name if present
 * @returns {string[]}
 */
export function parseInput(input, options={}) {
    const {removeQuotes=false} = options;
    let [cname='', op='', val='', ...rest] = (' '+input).split(operators);
    op = op.trim();
    val = op ? val.trim() : cname.trim();      // when op is missing, val is returned as cname(index 0).
    cname = op ? cname.trim() : '';
    if (!isEmpty(rest)) val += rest.join(); // value contains operators.  just put it back.
    if (removeQuotes) {
        cname = stripColumnNameQuotes(cname);
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
        const filterInfo = new FilterInfo();
        if (filterString) {
            filterString.split(FILTER_SEP).forEach( (filter) => {
                const parts = filter.split(COND_SEP);
                for (let i = 0; i < parts.length; i+=2) {
                    const [cname, op, val] = parseInput(parts[i], {removeQuotes: true});
                    if (cname && op) {
                        filterInfo.addFilter(cname, `${op} ${val}`, parts[i-1]);
                    }
                }
            });
        }
        return filterInfo;
    }

    /**
     * given a list of filters separated by condition separator,
     * transform them into valid filters if they are not already so.
     * @param filterInfo
     * @param columns
     * @returns {string}
     */
    static autoCorrectFilter(filterInfo, columns) {
        if (filterInfo) {
            filterInfo.split(FILTER_SEP).map( (filter) => {
                const parts = filter.split(COND_SEP);
                for (let i = 0; i < parts.length; i += 2) {
                    const [cname, op, val] = parseInput(parts[i]);
                    if (cname) {
                        const col = columns?.find((c) => c.name === cname);
                        parts[i] = `${cname} ${autoCorrectCondition(op + ' ' + val, col)}`;
                    }
                }
                return parts.join('');
            }).join(FILTER_SEP);
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
        if (conditions) {
            const parts = conditions.split(COND_SEP);
            for (let i = 0; i < parts.length; i += 2) {
                const [cname, op, val] = parseInput(parts[i]);
                if (cname || !op || !val) return false;
            }
        }
        return true;
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
        const allowCols = columns.concat({name:'ROW_IDX'});
        if (filterInfo) {
            let msg = '';
            filterInfo.split(FILTER_SEP).forEach( (filter) => {
                const parts = filter.split(COND_SEP);
                for (let i = 0; i < parts.length; i += 2) {
                    const [cname] = parseInput(parts[i]);
                    if (!cname) {
                        msg += `\n"${parts[i]}" is not a valid filter.`;
                    } else if (!allowCols.some((c) => c.name === cname)) {
                        const expr = new Expression(cname, allowCols.map((s) => s.name));
                        if (!expr.isValid()) {
                            msg += `\n"${parts[i]}" unrecognized column or expression.\n`;
                        }
                    }
                    if (msg) return [false, msg];
                }
            });
        }
        return [true, ''];
    }

    /**
     * validator for free-form filters field
     * @param {TableColumn[]} columns array of column definitions
     * @param {string} filterInfo
     * @returns {{valid: boolean, value: (string|*), message: string}}
     */
    static validator(columns, filterInfo) {
        filterInfo = FilterInfo.autoCorrectFilter(filterInfo, columns);
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
        filterStr = filterStr.replace(/ and /i, '#-#AND#-#')
                             .replace(/ or /i, '#-#OR#-#');
        const comparators = filterStr.split('#-#')
                            .map( (fstr) => {
                                if (fstr === 'AND' || fstr === 'OR') return fstr;
                                else return createOneComparator(fstr, tableModel);
                            });

        return (row, idx) => {
            let retval = comparators[0](row, idx);
            if (comparators.length > 2) {
                for (let i = 1; i < comparators.length-1; i+=2) {
                    if (comparators[i] === 'AND') {
                        retval = retval && comparators[i+1](row, idx);
                    } else if (comparators[i] === 'OR') {
                        retval = retval || comparators[i+1](row, idx);
                    }
                }
            }
            return  retval;
        };
    }


    serialize(formatKey) {
        if (!formatKey) {
            // add quotes to key if it does not contains quotes
            formatKey = (k) => k.includes('"') ? k : `"${k}"`;
        }

        return Object.entries(this.filters)
                    .map(([k,v]) => {
                        const parts = v.split(COND_SEP);
                        for (let i = 0; i < parts.length; i += 2) {
                            parts[i] = `${formatKey(k)} ${parts[i]}`;
                        }
                        return parts.join('');
                    }).join(FILTER_SEP);
    }

    /**
     * add additional conditions to the given column.
     * @param colName
     * @param conditions
     * @param andOr     logical operator to prepend to this filter.  defautls to 'and'
     */
    addFilter(colName, conditions, andOr=' and ') {
        this.filters[colName] = !this.filters[colName] ? conditions : `${this.filters[colName]}${andOr}${conditions}`;
    }

    setFilter(colName, conditions) {
        Reflect.deleteProperty(this.filters, colName);
        if (conditions) {
            const parts = conditions.split(COND_SEP);
            for (let i = 0; i < parts.length; i += 2) {
                const [, op, val] = parseInput(parts[i]);
                if (op) this.addFilter(colName, `${op} ${val}`, parts[i-1]);
            }
        }
    }

    /**
     * returns the string value of this columns filter info.
     * multiple filters are separated by comma.
     * @param colName
     * @returns {string}
     */
    getFilter(colName) {
        let filter =  this.filters[colName] && this.filters[colName].toString();
        if (!filter) {
            const quotedColName = `"${colName}"`;
            filter =  this.filters[quotedColName] && this.filters[quotedColName].toString();
        }
        return filter;
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
        return new RegExp('^' + newText + '$', 'm'); //allow multiline
    }
    catch (e) {
        showInfoPopup('invalid filtering condition: ' + text, 'table filtering');
        return new RegExp();
    }
}

/*-----------------------------------------------------------------------------------------*/

/**
 * Attempt to auto-correct the given condition(s).
 * @param {string} conditions  one or more conditions, separated by COND_SEP
 * @param {string} tbl_id   table ID.
 * @param {string} cname    column name.
 * @returns {string}
 */
function autoCorrectConditions(conditions, tbl_id, cname) {
    const col = getColumn(getTblById(tbl_id), cname);
    if (conditions) {
        const parts = conditions.split(COND_SEP);
        for (let i = 0; i < parts.length; i += 2) {                       // separate them into parts
                parts[i] = autoCorrectCondition(parts[i], col);        // auto correct if needed
        }
        return parts.join('');                                              // put them back
    } else {
        return conditions;
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
 * @param v     filter expression
 * @param col   column to operate on
 * @returns {*}
 */
function autoCorrectCondition(v, col) {

    const useQuote = col && isColumnType(col, COL_TYPE.USE_STRING);

    const encloseByQuote = (txt, quote="'") => {
        return `${quote}${txt}${quote}`;
    };

    const encloseString = (txt) => {
        return (txt.match(/^'.*'$/) ? txt : encloseByQuote(txt));
    };

    let [, op, val] = parseInput(v);

    // empty string or string with no value
    if (!op && !val) return v.trim();

    op = op ? op.toLowerCase() : (!col?.type || useQuote ? 'like' : '=');      // use 'like' when column type is string-like or not defined

    switch (op) {
        case 'like':
            if (!val.match(/^'.*'$/)) {
                val = val.replace(/([_|%\\])/g, '\\$1');

                val = encloseByQuote(encloseByQuote(val, '%'));
            }
            break;
        case 'in':
            let valList = val.match(/^\((.*)\)$/) ? val.substring(1, val.length-1) : val;

            valList = valList.split(',').map((s) => {
                return useQuote ? encloseString(s.trim()) : s;
            }).join(', ');

            val = `(${valList})`;
            break;
        case '=':
        case '!=':
        case '>':
        case '<':
        case '>=':
        case '<=':
            val = useQuote ? encloseString(val) : val;
            break;
        case 'is':
        case 'is not':
            val = 'NULL';
            break;

        default:
    }
    return `${op} ${val}`;
}

/**
 * returns a comparator for a single, simple `column operator value` filter
 * @param filterStr
 * @param tableModel
 * @returns {function(): boolean}
 */
function createOneComparator(filterStr, tableModel) {
    let [cname, op, val] = parseInput(filterStr);
    if (!cname) return () => false;       // bad filter.. returns nothing.

    // remove the double quote or the single quote around cname and val (which is added in auto-correction)
    const removeQuoteAroundString = (str, quote = "'") => {
        if (str && str.startsWith(quote)) {
            const reg = new RegExp('^' + quote + '(.*)' + quote + '$');
            return str.replace(reg, '$1');
        } else {
            return str;
        }
    };

    cname = removeQuoteAroundString(cname, '"');
    op = op.toLowerCase();
    val = val.toLowerCase();

    const col = getColumn(tableModel, cname);
    const cidx = getColumnIdx(tableModel, cname);
    const noROWID = cname === 'ROW_IDX' && cidx < 0;
    const colType = noROWID ? 'int' : get(col, 'type', 'char');

    // bRemoveQuote: if remove the single quote enclosing the string
    const convertStrToNumber = (str, bRemoveQuote=true) => {
        const numStr = bRemoveQuote ? removeQuoteAroundString(str) : str;
        const isCastInt = bRemoveQuote && str.startsWith("'") && str.endsWith("'");

        // cast to integer in case it is single quoted
        const num = colType.match(/^[i]/)&&isCastInt ? strictParseInt(numStr) : Number(numStr);
        return isNaN(num) ? str : num;
    };


    // update val of operator 'in' into an array of value
    if (op === 'in') {
        if (val.match(/^\(.*\)$/)) {
            val = val.substring(1, val.length-1);
        }
        val = val.split(',').map((s) => removeQuoteAroundString(s.trim()) === NULL_TOKEN.toLowerCase() ? null : s.trim());
    }

    // remove single quote enclosing the string for the value of operater 'like' or char type column
    if (op === 'like' || colType.match(/^[sc]/)) {
        val = isArray(val) ? val.map((s) => removeQuoteAroundString(s)) : removeQuoteAroundString(val);
    } else if (colType.match(/^[dfil]/)) {    // convert to number by removing single quote except 'in'
        val =  isArray(val) ? val.map((s) => convertStrToNumber(s, false)) : convertStrToNumber(val);
    }


    // catch known exceptions server-backed tables encountered to ensure behavior is consistent.
    if (isColumnType(col, COL_TYPE.NUMBER)) {
        if ( (op === 'in') && isArray(val)) {
            for(let i=0; i<val.length; i++) {
                if (!isNumber(val[i])) {
                    tableModel.error = 'data exception: invalid character value for cast';
                    return () => false;
                }
            }
        } else if ( !['like','in','is not','is'].includes(op) ) {
            if (!isNumber(val)) {
                tableModel.error = 'data exception: invalid character value for cast';
                return () => false;
            }
        }
    }

    return (row, idx) => {
        if (!row) return false;
        let compareTo = noROWID ? idx : row[cidx];
        compareTo = (compareTo === get(getColumn(tableModel, cname), 'nullString', '')) ? null : compareTo;     // resolve nullString

        if (op !== 'like' && colType.match(/^[dfil]/)) {      // int, float, double, long .. or their short form.
            compareTo = compareTo ? Number(compareTo) : compareTo;
        } else if (colType.includes('char') && !isNil(compareTo)) {
            compareTo = `${compareTo}`.toLowerCase();
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
            case 'is'  :
                return val === 'null' && isNil(compareTo);
            case 'is not'  :
                return val === 'null' && !isNil(compareTo);
            default :
                return false;
        }
    };
}
