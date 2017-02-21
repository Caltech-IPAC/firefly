/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {isEmpty, isUndefined} from 'lodash';
import {flux} from '../../Firefly.js';
import ExternalAccessCntlr from '../ExternalAccessCntlr.js';
import {makePlotSelectionExtActivateData} from '../ExternalAccessUtils.js';
import {dispatchExtensionActivate} from '../ExternalAccessCntlr.js';
import {getTblRowAsObj, getTblById} from '../../tables/TableUtil.js';
import {DATA_PREFIX, RESULTS_PREFIX, TABLE_HIGHLIGHT} from '../../tables/TablesCntlr.js';



export function* watchExtensionActions() {

    while (true) {

        const action= yield take();


        var {extensionList}= flux.getState()[ExternalAccessCntlr.EXTERNAL_ACCESS_KEY];
        if (!isEmpty(extensionList)) {
            extensionList
                .filter((e) => e.extType === action.type)
                .forEach((ext) => dispatchExtensionActivate(ext, makeExtData(ext, action.payload)));
        }

    }
}



function makeExtData(ext, payload) {
    var data= {
        id : ext.id,
        type: ext.extType
   };
   if (data.type && data.type.startsWith(DATA_PREFIX) || data.type.startsWith(RESULTS_PREFIX)) {
       data.row= getTblRowAsObj( getTblById(payload.tbl_id, payload.highlightedRow));
   }
   return Object.assign(data,payload);
}
