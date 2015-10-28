/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 */
import ClientFitsHeader from './ClientFitsHeader.js';
import checkNull from '../util/StringUtils.js';

const SPLIT_TOKEN= '--FileAndHeaderInfo --';



class FileAndHeaderInfo {

    /**
     * contains the file and the ClientFitsHeader
     * @param {string} file the file to look at on the server
     * @param {string} Header
     * to ClientFitsHeader.toString()
     */
    constructor(file, header) {
        this.file= file;
        this.header= header;
    }
}

export default FileAndHeaderInfo;
