/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


const PLOT_IMAGES_TOKEN= '--PlotImages--';

import ImageURL from './ImageURL.js';
import ThumbURL from './ThumbURL.js';
import join from 'underscore.string/join';
import {parseInt, parseBoolean, getStringWithNull} from '../util/StringUtils.js';


class PlotImages {
    /**
     *
     * @param {string} templateName
     * @param {number} screenWidth
     * @param {number} screenHeight
     * @param {number} zfact
     */
    constructor(templateName, screenWidth, screenHeight, zfact) {
        this.images= [];   // an array of ImageURL
        this.thumbnailImage= null;
        this.templateName= templateName;
        this.screenWidth= screenWidth;
        this.screenHeight= screenHeight;
        this.zfact= zfact;
    }

    /**
     *
     * @param {ImageURL} image
     */
    add(image) { this.images.push(image); }

    /**
     *
     * @param {ThumbURL} ThumbURL - a url of the thumbnail image
     */
    setThumbnail(image) { this.thumbnailImage= image; }


    /** * @return {number} */
    getScreenWidth() { return this.screenWidth; }

    /** * @return {number} */
    getScreenHeight() { return this.screenHeight; }

    /** * @return {number} */
    getZoomFactor() { return this.zfact; }


    /**
     *
     * @return {ThumbURL} the thumbnail image
     */
    getThumbnail() { return this.thumbnailImage; }

    /**
     *
     * @param {number} idx
     * @return {ImageURL}
     */
    get(idx) { return this.images[idx]; }

    size() { return this.images.length; }

    getTemplateName() { return this.templateName; }

    toString() {
        var part1= join(PLOT_IMAGES_TOKEN,
            this.thumbnailImage, this.templateName,
            this.screenWidth, this.screenHeight,
            this.zfact);

        var part2= this.images.map( image => image ? image.toString() : 'null').join(PLOT_IMAGES_TOKEN);
        return join(PLOT_IMAGES_TOKEN,part1,part2);
    }

    static parse(s) {
        if (!s) return null;
        var sAry= s.split(PLOT_IMAGES_TOKEN,500);
        if (sAry.length<5) return null;

        var i= 0;
        var thumbnailImage= ThumbURL.parse(sAry[i++]);
        var templateName= getStringWithNull(sAry[i++]);
        var screenWidth= parseInt(sAry[i++],0);
        var screenHeight= parseInt(sAry[i++],0);
        var zfact= parseFloat(sAry[i++],1);
        var retval= new PlotImages(templateName, screenWidth, screenHeight,zfact);
        retval.setThumbnail(thumbnailImage);
        while(i<sAry.length) {
            retval.add(ImageURL.parse(sAry[i++]));
        }
        return retval;
    }
}

