/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import join from 'underscore.string/join';
import {parseInt, parseBoolean, getStringWithNull} from '../util/StringUtils.js';

const THUMB_URL_TOKEN= '--ThumbURL--';

class ThumbURL {

    /**
     *
     * @param {string} url
     * @param {number} width
     * @param {number} height
     */
    constructor(url, width, height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }

    toString() {
        return join(IMAGE_URL_TOKEN,
            this.url, this.width, this.height);
    }

    static parse(s) {
        if (!s) return null;
        var sAry = s.split(THUMB_URL_TOKEN, 4);
        if (sAry.length !== 3) return null;
        var i = 0;
        var url = getStringWithNull(sAry[i++]);
        var width = parseInt(sAry[i++], 0);
        var height = parseInt(sAry[i++], 0);
        return new ThumbURL(url, width, height);
    }

    equals(obj) {
        return (obj instanceof ThumbURL) ? this.toString() === obj.toString() : false;
    }
}


export default ThumbURL;
