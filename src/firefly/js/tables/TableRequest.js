/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {pick, identity} from 'lodash';


export class TableRequest {
    constructor(params) {
        Object.keys(params).forEach((key) => {
            this[key] = params[key];
        });
    }

    addFilter(...filters) {
        this.filters = (this.filters || []).push(filters);
    }

    /**
     *
     * @param id
     * @param tbl_id
     * @param startIdx
     * @param pageSize
     * @param filters  List of conditions separted by comma(,). Format:  (col_name|index) operator value.
     *                 operator is one of '> < = ! >= <= IN'.  See DataGroupQueryStatement.java doc for more details.
     * @param sortInfo Sort information.  Format:  SortInfo=(ASC|DESC),col_name[,col_name]*
     * @param inclCols
     * @param decimate
     * @param META_INFO
     * @param rest
     * @param copyFromReq
     * @returns {TableRequest}
     */
    static newInstance({id, tbl_id, startIdx, pageSize, filters, sortInfo, inclCols, decimate, META_INFO, ...rest}, copyFromReq) {
        var params = Object.assign(rest, pick({id, tbl_id, startIdx, pageSize, filters, sortInfo, inclCols, decimate, META_INFO}, identity));   // take only defined params
        if (copyFromReq) {
            params = Object.assign(copyFromReq, params);
        }
        return new TableRequest(params);
    }
}

TableRequest.keys = {
    id: 'id',
    tbl_id: 'tbl_id',
    startIdx: 'startIdx',
    pageSize: 'pageSize',
    filters: 'filters',
    sortInfo: 'sortInfo',
    inclCols: 'inclCols',
    decimate: 'decimate',
    META_INFO: 'META_INFO'
};



