/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import join from 'underscore.string/join';
import {parseInt, parseBoolean, getStringWithNull} from '../util/StringUtils.js';

const IMAGE_URL_TOKEN= '--ImageURL--';

class ImageURL {
    constructor( xoff, yoff, width, height, index, created) {
        this.url= url;
        this.xoff= xoff;
        this.yoff= yoff;
        this.width= width;
        this.height= height;
        this.index= index;
        this.created= created;
    }

    updateSizes(x, y, width, height) {
        this.xoff= x;
        this.yoff= y;
        this.width= width;
        this.height= height;
    }

    toString() {
        return join(IMAGE_URL_TOKEN,
            this.index, this.xoff, this.yoff,
            this.width(), this.height(),
            this.url, this.created);
    }

    static parse(s) {
        if (!s) return null;
        var sAry= s.split(IMAGE_URL_TOKEN,8);
        if (sAry.length!==7) return null;
        var i= 0;
        var index= parseInt(sAry[i++],0);
        var xoff= parseInt(sAry[i++],0);
        var yoff= parseInt(sAry[i++],0);
        var width=parseInt(sAry[i++],0);
        var height= parseInt(sAry[i++],0);
        var url= getStringWithNull(sAry[i++]);
        var created= parseBoolean(sAry[i]);
        return new ImageURL(url,xoff,yoff,width,height,index,created);
    }

    equals(obj) {
        return (obj instanceof ImageURL) ? this.toString()===obj.toString() : false;
    }
}

export default ImageURL;
