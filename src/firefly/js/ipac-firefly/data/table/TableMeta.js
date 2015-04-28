/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * @author tatianag
 */

"use strict";

import{StringUtil} from "ipac-firefly/util/StringUtils.js";

export class TableMeta {

    constructor() {}

    // takes a string returns TableMeta object
    static parse(s) {
        const SPLIT_TOKEN = "--TableMeta--";
        const ELEMENT_TOKEN = "--TMElement--";

        if (s == null) return null;
        var sAry = s.split(SPLIT_TOKEN, 7);
        retval = new TableMeta();
        if (sAry.length == 7) {
            try {
                var idx = 0;
                retval.source = sAry[idx].equals("null") ? null : sAry[idx];
                idx++;
                retval.fileSize = sAry[idx++];
                retval.isFullyLoaded = sAry[idx++];
                retval.relatedCols = StringUtils.parseStringList(sAry[idx++], ELEMENT_TOKEN);
                retval.groupByCols = StringUtils.parseStringList(sAry[idx++], ELEMENT_TOKEN);
                retval.attributes = StringUtils.parseStringMap(sAry[idx++], ELEMENT_TOKEN);
            } catch (e) {
                retval = null;
            }
        }
        return retval;
    }
}
