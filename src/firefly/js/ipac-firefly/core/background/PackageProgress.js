/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/
/**
 * User: roby
 * Date: Apr 2, 2009
 * Time: 9:18:47 AM
 */
"use strict";
import validator from "validator";


const SPLIT_TOKEN= "--BGProgress--";

class PackageProgress {
    /**
     *
     * @param {number} totalFiles
     * @param {number} processedFiles
     * @param {number} totalBytes
     * @param {number} processedBytes
     * @param {number} finalCompressedBytes
     * @param {string} url
     */
    construtor(totalFiles=0, processedFiles=0, totalBytes=0, processedBytes=0, finalCompressedBytes=0, url=null) {
        this.totalFiles = totalFiles;
        this.processedFiles = processedFiles;
        this.totalBytes = totalBytes;
        this.processedBytes = processedBytes;
        this.finalCompressedBytes = finalCompressedBytes;
        this.url= url;
    }

    getTotalFiles() { return this.totalFiles; }

    getProcessedFiles() { return this.processedFiles; }

    getTotalByes() { return this.totalBytes; }

    getProcessedBytes() { return this.processedBytes; }

    getFinalCompressedBytes() { return this.finalCompressedBytes; }

    getURL() { return this.url; }

    /**
     * if a url exist then we are done
     * @return {boolean} true if done
     */
    isDone() { return this.url?true:false; }

    serialize() {
        var retval= [_totalFiles,
                     _processedFiles,
                     _totalBytes,
                     _processedBytes,
                     _finalCompressedBytes,
                     _url].join(SPLIT_TOKEN);

        return retval;
    }

    /**
     * parse a serialized version into a PackageProgress
     * @param s
     * @return {PackageProgress}
     */
    static parse(s) {
        if (!s) return null;
        var p= null;
        var sAry= s.split(SPLIT_TOKEN,6);
        if (sAry.length===6) {
            p= new PackageProgress();
            p._totalFiles= validator.toInt(sAry[i++],0);
            p._processedFiles= validator.toInt(sAry[i++],0);
            p._totalBytes= validator.toInt(sAry[i++],0);
            p._processedBytes= validator.toInt(sAry[i++],0);
            p._finalCompressedBytes= validator.toInt(sAry[i++],0);
            if (p._totalFiles && p._processedFiles && p._totalBytes && p._processedBytes && p._finalCompressedBytes) {
                p._url= sAry[i++];
            }
            else {
                p=null;
            }
        }
        return p;
    }
}

