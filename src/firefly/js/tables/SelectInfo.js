/**
 * Created by loi on 1/15/16.
 */


/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty} from 'lodash';

import {Table} from './Table.js';


export class SelectInfo {
    constructor(selectInfo) {
        this.data = selectInfo;

    }

    selectAll() {
        this.data.exceptions.clear();
        this.data.selectAll = true;
    }

    deselectAll() {
        this.data.exceptions.clear();
        this.data.selectAll = false;
    }

    select(idx) {
        if (this.data.selectAll) {
            this.data.exceptions.delete(idx);
        } else {
            this.data.exceptions.add(idx);
            if (this.data.exceptions.size == this.data.rowCount) {
                this.selectAll();
            }
        }
    }

    deselect(idx) {
        if (this.data.selectAll) {
            this.data.exceptions.add(idx);
        } else {
            this.data.exceptions.delete(idx);
        }
    }

    isSelected(idx) {
        if (this.data.selectAll) {
            return !this.data.exceptions.has(idx);
        } else {
            return this.data.exceptions.has(idx);
        }
    }

    getSelected() {
        if (this.data.selectAll) {
            const all = new Set();
            for(var i = 0; i < this.data.rowCount; i++) {
                if (!this.data.exceptions.has(i)) {
                    all.add(i);
                }
            }
            return all;
        } else {
            return new Set(this.data.exceptions);
        }
    }

    getSelectedCount() {
        if (this.data.rowCount < 1) return 0;
        if (this.data.selectAll) {
            return this.data.rowCount - this.data.exceptions.size;
        } else {
            return this.data.exceptions.size;
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
     * @returns {SelectInfo}
     */
    static newInstance({selectAll=false, exceptions=(new Set()), rowCount=0} = {}) {
        return arguments[0] ? new SelectInfo(arguments[0]) : new SelectInfo({selectAll, exceptions, rowCount});
    }

    /**
     * return the SelectInfo of a Table given the tbl_id..
     * @param tbl_id unique table ID
     * @param root the application state root.  If not given, flux.getState() will be used.
     * @returns {SelectInfo}
     */
    static find(tbl_id, root) {
        var table = Table.find(tbl_id, root);
        return table && SelectInfo.newInstance(table.selectInfo);
    }

}
