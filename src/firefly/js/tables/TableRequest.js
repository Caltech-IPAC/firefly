/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {ServerRequest} from '../data/ServerRequest.js';

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


export default class TableRequest extends ServerRequest {
    constructor(id, copyFromReq) {
        super(id, copyFromReq);
    }

    addFilter(...filters) {
        this[REQ_PRM.FILTERS] = (this[REQ_PRM.FILTERS] || []).push(filters);
    }

    static newInstance(id, copyFromReq) {
        return new TableRequest(id, copyFromReq);
    }
}
