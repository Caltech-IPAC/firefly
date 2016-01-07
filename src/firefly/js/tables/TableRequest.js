/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {pick, identity} from 'lodash';



/*
Declaration of parameters used by TableRequest.
 */
export const REQ_PRM = {
    TBL_ID: 'tbl_id',
    START_IDX: 'startIdx',
    PAGE_SIZE: 'pageSize',
    FILTERS: 'filters',
    SORT_INFO: 'sortInfo',
    INCL_COLS: 'inclCols',
    DECIMATE: 'decimate',
    META_INFO: 'META_INFO'
};


export default class TableRequest {
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
     * @param filters
     * @param sortInfo
     * @param inclCols
     * @param decimate
     * @param META_INFO
     * @param rest
     * @param copyFromReq
     * @returns {TableRequest}
     */
    static newInstance({id, tbl_id, startIdx, pageSize, filters, sortInfo, inclCols, decimate, META_INFO, ...rest}, copyFromReq) {
        var params = Object.assign(rest, pick({id, tbl_id, startIdx, pageSize, filters, sortInfo, inclCols, decimate, META_INFO}, identity));
        if (copyFromReq) {
            params = Object.assign(copyFromReq, params);
        }
        return new TableRequest(params);
    }

}
