/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, slice} from 'lodash';
import {flux} from '../Firefly.js';
import * as TblCntlr from './TablesCntlr.js';
import * as TblUtil from './TableUtil.js';

/**
 *
 */
export class Table {
    constructor(dataModel) {
        this.data = dataModel;
    }

    /**
     * return a row of data at the given index.  index starts at 0.
     * @param idx
     * @returns row of data(array of strings) or null if index is out of range.
     */
    getRow(idx) {
        return get(this.data, ['tableData.data', idx]);
    }

    getColumn(idx) {
        return get(this.data, ['tableData.columns', idx]);
    }

    has(startIdx, endIdx) {
        endIdx =  endIdx >0 ? Math.min( endIdx, this.data.totalRows) : startIdx;
        if (startIdx >=0 && endIdx > startIdx) {
            const data = get(this.data, 'tableData.data', []);
            const aslice = data.slice(startIdx, endIdx).filter( (v) => v  );
            return aslice.length === (endIdx-startIdx);
        } else return false;
    }

    /**
     * return this TableModel class using the tableModel data.
     * @param tableModel
     */
    static newInstance(tableModel) {
        return new Table(tableModel);
    }

    /**
     * return a Table with the data from the application's table_space.
     * @param tbl_id unique table ID
     * @param root the data root.  If not given, flux.getState()[TblCntlr.TABLE_SPACE_PATH] will be used.
     * @returns {Table}
     */
    static findTblById(tbl_id, root) {
        const state = root || flux.getState()[TblCntlr.TABLE_SPACE_PATH];
        var table = state[tbl_id];
        return table && Table.newInstance(table);
    }
}