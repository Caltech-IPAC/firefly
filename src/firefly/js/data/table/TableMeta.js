/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * @author tatianag
 */

import {parseStringList, parseStringMap} from '../../util/StringUtils.js';

export const HAS_ACCESS_CNAME='hasAccessCName';

export class TableMeta {

    constructor() { this.attributesP = new Map(); }

    get source() { return this.sourceP; }
    set source(value) { this.sourceP = value; }

    get fileSize() { return this.fileSizeP; }
	set fileSize(value) { this.fileSizeP = value; }

    get isFullyLoaded() { return this.isFullyLoadedP; }
    set isFullyLoaded(value) { this.isFullyLoadedP = value; }

    get relatedCols() { return this.relatedColsP; }
    set relatedCols(value) { this.relatedColsP = value; }

    get groupByCols() { return this.groupByColsP; }
    set groupByCols(value) { this.groupByColsP = value; }

    get attributes() { return this.attributesP; }
    set attributes(value) { this.attributesP = value; }

    getAttribute(key) {
        return this.attributesP.get(key);
    }

    setAttributes(attributes) {
        if (this.attributesP) {
            attributes.forEach(function (value, key) {
                this.attributesP.set(key,value);
            }.bind(this));
        }
    }

    clone() {
        const ret = new TableMeta();
        ret.attributes(this.attributesP);
    }

    // takes a string returns TableMeta object
    static parse(s) {
        const SPLIT_TOKEN = '--TableMeta--';
        const ELEMENT_TOKEN = '--TMElement--';

        if (!s) {
            return null;
        }
        var sAry = s.split(SPLIT_TOKEN, 7);
        let retval = new TableMeta();
        if (sAry.length === 7) {
            try {
                var idx = 0;
                retval.source = sAry[idx]==='null' ? null : sAry[idx];
                idx++;
                retval.fileSize = sAry[idx++];
                retval.isFullyLoaded = sAry[idx++];
                retval.relatedCols = parseStringList(sAry[idx++], ELEMENT_TOKEN);
                retval.groupByCols = parseStringList(sAry[idx++], ELEMENT_TOKEN);
                retval.attributes = parseStringMap(sAry[idx++], ELEMENT_TOKEN);
            } catch (e) {
                retval = null;
            }
        }
        return retval;
    }
}
