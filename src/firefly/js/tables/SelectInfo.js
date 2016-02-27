/**
 * Created by loi on 1/15/16.
 */


/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty} from 'lodash';

import {findTblById} from './TableUtil.js';


export class SelectInfo {
    constructor(selectInfo) {
        this.data = selectInfo;
        this.offset = 0;

    }

    setSelectAll(flg) {
        this.data.exceptions.clear();
        this.data.selectAll = flg;
    }

    setRowSelect(idx, flg) {
        idx = idx + this.offset;
        const {selectAll, exceptions, rowCount} = this.data;
        if (flg) {
            if (selectAll) {
                exceptions.delete(idx);
            } else {
                exceptions.add(idx);
                if (exceptions.size == rowCount) {
                    this.setSelectAll(true);
                }
            }
        } else {
            if (selectAll) {
                exceptions.add(idx);
            } else {
                exceptions.delete(idx);
            }
        }

    }

    isSelected(idx) {
        idx = idx + this.offset;
        const {selectAll, exceptions} = this.data;
        if (selectAll) {
            return !exceptions.has(idx);
        } else {
            return exceptions.has(idx);
        }
    }

    getSelected() {
        const {selectAll, exceptions, rowCount} = this.data;
        if (selectAll) {
            const all = new Set();
            for(var i = 0; i < rowCount; i++) {
                if (!exceptions.has(i)) {
                    all.add(i);
                }
            }
            return all;
        } else {
            return new Set(exceptions);
        }
    }

    getSelectedCount() {
        const {selectAll, exceptions, rowCount} = this.data;
        if (rowCount < 1) return 0;
        if (selectAll) {
            return rowCount - exceptions.size;
        } else {
            return exceptions.size;
        }
    }

    isSelectAll() {
        return this.data.selectAll && (this.data.exceptions.size == 0);
    }

    toString() {
        return this.data.selectAll + '-' + this.data.exceptions.toString() + '-' + this.data.rowCount;
    }

    parse(s) {
        var selecInfo = {};
        var parts = s.split('-');
        if (parts.length == 3) {
            selecInfo.selectAll = Boolean(parts[0]);
            if (!isEmpty(parts[1])) {
                selecInfo.exceptions =  parts[1].reduce( (res, cval) => {
                        return res.add(parseInt(cval));
                    }, new Set());
            }
            selecInfo.rowCount = parseInt(parts[2]);
        }
        return SelectInfo.newInstance(selecInfo);
    }

    /**
     * Destructing of the SelectInfo's data.
     * @param selectAll     boolean. Indicates selectAll mode. defaults to false.
     * @param exceptions    Set. A set of exceptions based on selectAll mode.
     * @param rowCount      int. Total number of rows. defaults to zero.
     * @param offset        int. All indices passed into this class's funtion will be offsetted by this given value.
     * @returns {SelectInfo}
     */
    static newInstance({selectAll=false, exceptions=(new Set()), rowCount=0}, offset) {
        var si = new SelectInfo({selectAll, exceptions, rowCount});
        if (offset) si.offset = offset;
        return si;
    }

    /**
     * return the SelectInfo of a Table given the tbl_id..
     * @param tbl_id unique table ID
     * @param root the application state root.  If not given, flux.getState() will be used.
     * @returns {SelectInfo}
     */
    static find(tbl_id, root) {
        var tableModel = findTblById(tbl_id, root);
        return tableModel && SelectInfo.newInstance(tableModel.selectInfo);
    }

}
