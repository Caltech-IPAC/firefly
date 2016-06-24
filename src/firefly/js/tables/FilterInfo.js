/**
 * Created by loi on 1/15/16.
 */


/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
const op_regex = new RegExp('(!=|>=|<=|<|>|=|like|in)', 'i');
const cond_regex = new RegExp('^' + op_regex.source + '\\s+(.+)', 'i');
const filter_regex = new RegExp('(\\S+)\\s+' + op_regex.source + '\\s+(.+)', 'i');

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
        if (filterString) {
            filterString && filterString.split(';').forEach( (v) => {
                    const parts = v.trim().match(filter_regex);
                    if (parts.length > 3) filterInfo.addFilter(parts[1], `${parts[2]} ${parts[3]}`);
                });
        }
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
                const opVal = v.replace(/\(|\)| /g, '').split(op_regex);
                var [op, val] = opVal.length > 1 ? [ opVal[1], opVal[2] ] : [ 'like', opVal[0] ];  // defualt to 'like' if no operators found
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
                const parts = v.split(op_regex).map( (s) => s.trim());
                if (parts.length != 3) return v;
                return `${parts[0]} ${FilterInfo.autoCorrect(parts[1]+parts[2])}`;
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
            return rval && cond_regex.test(v.trim());
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
     * validate the filterInfo string
     * @param conditions
     * @param columns array of column definitions
     * @returns {[boolean, string]} isValid plus an error message if isValid is false.
     */
    static isValid(filterInfo, columns = []) {
        const rval = [true, ''];
        const allowCols = columns.concat({name:'ROWID'});
        if (filterInfo && filterInfo.trim().length > 0) {
            return filterInfo.split(';').reduce( ([isValid, msg], v) => {
                    const parts = v.split(op_regex).map( (s) => s.trim());
                    if (parts.length !== 3) {
                        msg += `\n"${v}" is not a valid filter.`;
                    } else if (!allowCols.some( (col) => col.name === parts[0])) {
                        msg +=`\n"${v}" column not found.\n`;
                    }
                    return [!msg, msg];
                }, rval);
        } else {
            return rval;
        }
    }

    /**
     * validator for free-form filters field
     * @param filterInfo string serialized filter list
     * @param columns array of column definitions
     * @returns {{valid: boolean, value: (string|*), message: string}}
     */
    static validator(columns, filterInfo) {
        filterInfo = FilterInfo.autoCorrectFilter(filterInfo);
        const [valid, message] = FilterInfo.isValid(filterInfo, columns);
        return {valid, value: filterInfo, message};
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

    /**
     * add additional conditions to the given column.
     * @param colName
     */
    addFilter(colName, conditions) {
        this[colName] = !this[colName] ? conditions : `${this[colName]}; ${conditions}`;
    }

    setFilter(colName, conditions) {
        Reflect.deleteProperty(this, colName);
        if (conditions) {
            conditions.split(';').forEach( (v) => {
                const parts = v.trim().match(cond_regex);
                if (parts.length > 2) this.addFilter(colName, `${parts[1]} ${parts[2]}`);
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
