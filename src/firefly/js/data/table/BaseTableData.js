/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * @author tatianag
 */

export class BaseTableData {

    // data - array os strings
    // columns - array of strings
    // attributes - map
    // hasAccessCName - column name with the value 'true' or 'false',
    //                  which tells id the data file linked to this row
    //                  is available for download
    // rowIdxOffset - used to calculate table index in a paged table

    /*
     * @param {Array} columns - an array of columns
     */
    constructor(columns) {
        if (columns) {
            this.columns = columns;
        } else {
            this.columns = [];
        }
        this.data = [];
        this.attributes = new Map();
    }

    /*
    setRowIdxOffset(rowIdxOffset) {
        this.rowIdxOffset = rowIdxOffset;
    }
    */

    setHasAccessCName(hasAccessCName) {
        this.hasAccessCName = hasAccessCName;
    }

    size() {
        return this.data.length;
    }

    setAttribute(key, value) {
        this.attributes.set(key, value);
    }

    getAttribute(key) {
        return this.attributes.get(key);
    }

    getAttributes() {
        const acopy = new Map();
        this.attributes.forEach(function(value,key){
            acopy.set(key,value);
        });
        return acopy;
    }

    addColumn(index, name) {
        this.columns.splice(index, 1, name);
    }

    /**
     * Add this row into the set.
     *
     * @param row - an array of strings
     * @return {Number} length of the data array
     */
    addRow(row) {
        return this.data.push(row);   // data is an array of arrays, first dimension is row, second is column
    }

    /**
     * Remove row with rowIdx from the data array.
     *
     * @param rowIdx
     * @returns {Boolean} true if a row was removed
     */
    removeRow(rowIdx) {
        return this.data.splice(rowIdx, 1).length === 1;
    }

    clear() {
        this.data.splice(0, this.data.length);
    }

    /**
     * Returns a row of data at this index.
     *
     * @param {Number} idx
     * @return {Array} array of strings
     */
    getRow(idx) {
        return this.data[idx];
    }

    indexOf(row) {
        return this.data.indexOf(row);
    }

    // @return {Array} array of rows (each row is an array of strings)
    getRows() {
        return this.data;
    }

    getColumnNames() {
        return this.columns;
    }

    getColumnIndex(colName) {
        const idx = this.columns.indexOf(colName);
        if (idx < 0) {
            throw `This column does not exists: ${colName}`;
        }
        return idx;
    }

    /**
     * shallow clone.. not cloning the RowData objects
     * @return {BaseTableData}
     */
    clone() {
        const newval = new BaseTableData(this.columns.slice(0));
        newval.attributes = this.getAttributes();
        return newval;
    }


//====================================================================
// implements HasAccessInfos
//====================================================================

    getSize() {
        return this.size();
    }

    /**
     * Returns true if the given row is accessible.
     * The row is accessible if a hasAccess column is not defined, or there
     * is a "true" value (ignoring case) in the defined hasAccess column.
     * @param {Number} index
     * @return {Boolean}
     */
    hasAccess(index) {
        if (!this.hasAccessCName) {
            return true;
        } else {
            var idx = this.columns.indexOf(this.hasAccessCName);
            return (this.data[index][idx] === 'true');
        }
    }

//====================================================================


}