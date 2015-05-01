/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * @author tatianag
 */

"use strict";

import{StringUtil} from "ipac-firefly/util/StringUtils.js";

export class TableMeta {

    constructor() {}

    get source() { return this._source; }
    set source(value) { this._source = value; }

    get fileSize() { return this._fileSize; }
	set fileSize(value) { this._fileSize = value; }

    get isFullyLoaded() { return this._isFullyLoaded; }
  	set isFullyLoaded(value) { this._isFullyLoaded = value; }

    get relatedCols() { return this._relatedCols; }
  	set relatedCols(value) { this._relatedCols = value; }

    get groupByCols() { return this._groupByCols; }
  	set groupByCols(value) { this._groupByCols = value; }

    get attributes() { return this._attributes; }
  	set attributes(value) { this._attributes = value; }

    // takes a string returns TableMeta object
    static parse(s) {
        const SPLIT_TOKEN = "--TableMeta--";
        const ELEMENT_TOKEN = "--TMElement--";

        if (!s) return null;
        var sAry = s.split(SPLIT_TOKEN, 7);
        let retval = new TableMeta();
        if (sAry.length == 7) {
            try {
                var idx = 0;
                retval.source = sAry[idx]==='null' ? null : sAry[idx];
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
