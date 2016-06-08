/**
 * Created by loi on 1/15/16.
 */


/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
const op_regex = new RegExp('(<|>|>=|<=|=|!=|like|in)');
const cond_regex = new RegExp('^' + op_regex.source + '\\s+(.+)', 'i');
const filter_regex = new RegExp('(\\S+)\\s+' + op_regex.source + '\\s+(.+)', 'i');

export const FILTER_TTIPS = 'Valid values are one of (=, >, <, !=, >=, <=, LIKE) followed by a value separated by a space. \n' +
    `Or 'IN', followed by a list of values separated by commas. \n` +
    'Examples:  > 12345; != 3000; IN a,b,c,d';

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
     * validate the conditions
     * @param conditions
     * @returns {boolean}
     */
    static isValid(conditions) {
        return !conditions || conditions.split(';').reduce( (rval, v) => {
            return rval && cond_regex.test(v.trim());
        }, true);
    }

    static validator(conditions) {
        conditions = FilterInfo.autoCorrect(conditions);
        const valid = FilterInfo.isValid(conditions);
        return {valid, value: conditions, message: FILTER_TTIPS};
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
