/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isString} from 'lodash';
import {loadCancelableImage} from '../../util/WebUtil.js';

/**
 * @global
 * @public
 * @typedef {Object} ImageContainer
 * Always contains key image and then anything else to specify the state
 *
 * @prop {Object} image
 * @prop {Object} src
 * @prop {String} dataUrl
 * @prop {Object} tileAttributes
 * @prop {Object} canCompress
 *
 */

/**
 *
 * @param {ImageContainer|Object|String} imageData the current image data or a html image object or the source of the
 *                                       of an image to load
 * @return {Promise} promise with new imageData
 */
export function retrieveAndProcessImage(imageData) {
    if (!imageData) {
        return Promise.resolve(imageData);
    }
    else if (imageData instanceof HTMLCanvasElement) {
        return {promise:Promise.resolve(imageData)};
    }
    else if (isString(imageData)) {
        var {promise, cancelImageLoad}= loadCancelableImage(imageData);
        promise=  promise.then( (image) => modifyImage({image}));
        return convertToReturn(promise, cancelImageLoad);
    }
    else if (imageData instanceof HTMLImageElement) {
        const v= modifyImage({image:imageData});
        return convertToReturn(v);
    }
    return convertToReturn(
        Promise.reject(new Error('could note identify imageData: not string, HTMLImageElement, imageData.image, or imageData.dataUrl')));
}


function convertToReturn(obj, cancelImageLoad= undefined) {
    if (obj.then) return {promise:obj,cancelImageLoad};
    else if (obj.promise) return obj;
    else throw new Error('unexpected return object in ImageProcessor.retrieveAndProcessImage');
}

function modifyImage(imageData) { return Promise.resolve(imageData); }
