/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 */
import MiniFitsHeader from './MiniFitsHeader.js';
import checkNull from '../util/StringUtils.js';

const SPLIT_TOKEN= '--FileAndHeaderInfo --';



class FileAndHeaderInfo {

    /**
     * contains the file and the MiniFitsHeader
     * @param {string} file the file to look at on the server
     * @param {string} headerSerialized a string the contains the results of passing MiniFitsHeader
     * to MiniFitsHeader.toString()
     */
    constructor(file, headerSerialized) {
        this.file= file;
        this.headerSerialized= headerSerialized;
    }


    getfileName() { return this.file; }
    getHeader() { return MiniFitsHeader.parse(this.headerSerialized); }

    toString() {
        return this.file+SPLIT_TOKEN+this.headerSerialized;
    }


    static parse(s) {
        if (!s) return null;
        var sAry= s.split(SPLIT_TOKEN,3);
        return  (sAry.length===2) ? new FileAndHeaderInfo(checkNull(sAry[0]),sAry[1]) : null;
    }
}

export default FileAndHeaderInfo;
