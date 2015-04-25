/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * @author tatianag
 */
"use strict";

import{StringUtil} from "ipac-firefly/util/StringUtils.js";
import{TableMeta} from "./TableMeta.js";

export class RawDataSet {


    /**
     * @param meta TableMeta
     * @param startingIndex
     * @param totalRows
     * @param dataSetString
     */
   constructor(meta, startingIndex, totalRows, dataSetString) {
        this.meta = meta;
        this.startingIndex = startingIndex;
        this.totalRows = totalRows;
        this.dataSetString = dataSetString;
    };


    static parse(s) {
        const SPLIT_TOKEN= "--RawDataSet--";
        const NL_TOKEN=  "---nl---";

        try {
            var sAry = StringUtils.parseHelper(s,4,SPLIT_TOKEN);
            var i= 0;
            var startingIndex= sAry[i++];
            var totalRows=     sAry[i++];
            var meta= TableMeta.parse(sAry[i++]);
            var dsTmp= StringUtils.checkNull(sAry[i++]);
            var dataSetString= dsTmp.replace(NL_TOKEN,"\n");
            return new RawDataSet(meta,startingIndex,totalRows,dataSetString);
        } catch (e) {
            return null;
        }
    }

}
