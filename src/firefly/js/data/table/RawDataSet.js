/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * @author tatianag
 */

import {TableMeta} from './TableMeta.js';
import {parseHelper, checkNull} from '../../util/StringUtils.js';


export class RawDataSet {


    /**
     * @param {TableMeta} meta
     * @param {Number} startingIndex
     * @param {Number} totalRows
     * @param {String} dataSetString
     */
    constructor(meta, startingIndex, totalRows, dataSetString) {
        this.metaP = meta;
        this.startingIndexP = startingIndex;
        this.totalRowsP = totalRows;
        this.dataSetStringP = dataSetString;
    }

    get meta() { return this.metaP; }
    set meta(value) { this.metaP = value; }
    get startingIndex() { return this.startingIndexP; }
    set startingIndex(value) { this.startingIndexP = value; }
    get totalRows() { return this.totalRowsP; }
    set totalRows(value) { this.totalRowsP = value; }
    get dataSetString() { return this.dataSetStringP; }
    set dataSetString(value) { this.dataSetStringP = value; }


    static parse(s) {
        const SPLIT_TOKEN = '--RawDataSet--';
        const NL_TOKEN = /---nl---/g;

        try {
            var sAry = parseHelper(s,4,SPLIT_TOKEN);
            var i= 0;
            var startingIndex = sAry[i++];
            var totalRows = sAry[i++];
            var meta= TableMeta.parse(sAry[i++]);
            var dsTmp= checkNull(sAry[i]);
            var dataSetString= dsTmp.replace(NL_TOKEN,'\n');
            return new RawDataSet(meta,startingIndex,totalRows,dataSetString);
        } catch (e) {
            console.log(e);
            return null;
        }
    }

}
