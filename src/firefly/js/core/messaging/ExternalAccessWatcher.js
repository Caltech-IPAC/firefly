/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty} from 'lodash';
import ExternalAccessCntlr from '../ExternalAccessCntlr.js';
import {dispatchExtensionActivate} from '../ExternalAccessCntlr.js';
import {getTblRowAsObj, getTblById} from '../../tables/TableUtil.js';
import {DATA_PREFIX, RESULTS_PREFIX} from '../../tables/TablesCntlr.js';
import {dispatchAddActionWatcher} from '../MasterSaga';


export const addExtensionWatcher= () => dispatchAddActionWatcher( {callback:watchExtensionActions, actions:'*'});

function watchExtensionActions(action, cancelSelf, params, dispatch, getState) {
    const {extensionList} = getState()[ExternalAccessCntlr.EXTERNAL_ACCESS_KEY];
    if (isEmpty(extensionList)) return;
    extensionList
        .filter((e) => e.extType === action.type)
        .forEach((ext) => dispatchExtensionActivate(ext, makeExtData(ext, action.payload)));
}

function makeExtData(ext, payload) {
    const data= { id : ext.id, type: ext.extType };
    if (data.type && data.type.startsWith(DATA_PREFIX) || data.type.startsWith(RESULTS_PREFIX)) {
        data.row= getTblRowAsObj( getTblById(payload.tbl_id, payload.highlightedRow));
    }
    return {...data,...payload};
}


