/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import TblCntlr from './TablesCntlr.js';
import TblUtil from './TableUtil.js';

/**
 *
 */
export class Table {
    constructor(dataModel) {
        this.model = dataModel;
    }

    /**
     * return a row of data at the given index.  index starts at 0.
     * @param idx
     * @returns row of data(array of strings) or null if index is out of range.
     */
    getRow(idx) {
        return TblUtil.find(this.model, 'tableData', 'data', idx);
    }

    getColumn(idx) {
        return TblUtil.find(this.model, 'tableData', 'columns', idx);
    }

    /**
     * return this TableModel class using the tableModel data.
     * @param tableModel
     */
    static newInstance(tableModel) {
        return new Table(tableModel);
    }

    /**
     * return a Table with the data from the application's table-space.
     * @param tbl_id unique table ID
     * @param root the application state root.  If not given, flux.getState() will be used.
     * @returns {Table}
     */
    static find(tbl_id, root) {
        const state = root || flux.getState();
        var table = TblUtil.find(state, TblCntlr.TABLE_SPACE_PATH, 'main', tbl_id);
        return table && Table.newInstance(table);
    }
}