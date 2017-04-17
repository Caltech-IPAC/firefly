/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isString} from 'lodash';
import {loadImage, requestIdleCallback} from '../../util/WebUtil.js';

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
 * @return {Promise} promise with new imageData
 */
/**
 *
 * @param {ImageContainer|Object|String} imageData the current image data or a html image object or the source of the
 *                                       of an image to load
 * @param {Object} nextTileAttributes next state to process image
 * @param shouldProcess
 * @param processor
 * @return {Promise}
 */
export function retrieveAndProcessImage(imageData, nextTileAttributes, shouldProcess, processor) {

    if (isString(imageData)) {
        return loadImage(imageData)
            .then( (image) => modifyImage({image, tileAttributes:nextTileAttributes},nextTileAttributes,true, shouldProcess, processor));
    }
    else if (imageData instanceof HTMLImageElement) {
        return modifyImage({image:imageData, tileAttributes:nextTileAttributes}, nextTileAttributes, true, shouldProcess, processor);

    }
    else {
        if (imageData.image) {
            // console.log('Image From Cache');
            return modifyImage(imageData, nextTileAttributes,false, shouldProcess, processor);
        }
        else if (imageData.dataUrl) {

            // console.log('Image From Store');

            return loadImage(imageData.dataUrl)
                .then( (image) => modifyImage({image, tileAttributes:imageData.tileAttributes},
                                                nextTileAttributes,false, shouldProcess, processor));
        }
    }
    return Promise.reject(new Error('could note identify imageData: not string, HTMLImageElement, imageData.image, or imageData.dataUrl'));
}


/**
 *
 * @param {ImageData} imageData
 * @param {Object} nextTileAttributes
 * @param {boolean} newData
 * @param shouldProcess
 * @param processor
 * @return {ImageData}
 */
function modifyImage(imageData, nextTileAttributes, newData, shouldProcess, processor) {
    if (processor && shouldProcess && shouldProcess(imageData.image, newData, imageData.tileAttributes, nextTileAttributes) ) {
        // console.log('modifyImage');
        return new Promise( (resolve, reject) => {
            requestIdleCallback( () => {
                const results= processImage(imageData.image, nextTileAttributes, processor);
                resolve(
                    {
                        image: results.canvas,
                        compressible: results.compressible,
                        tileAttributes: nextTileAttributes,
                    } );
            }, {timeout:100});
        });
    }
    else {
        return Promise.resolve(imageData);
    }
}


/**
 *
 * @param imageOrCanvas
 * @param tileAttributes
 * @param processor
 * @return {ImageData}
 */
function processImage(imageOrCanvas, tileAttributes, processor) {
    let canvas;
    let ctx;
    if (imageOrCanvas instanceof HTMLImageElement) {
        canvas = document.createElement('canvas');
        const image= imageOrCanvas;
        ctx = canvas.getContext('2d');
        canvas.width = image.width;
        canvas.height = image.height;
        ctx.drawImage(image, 0,0);
    }
    else {
        canvas= imageOrCanvas;
        ctx = canvas.getContext('2d');
    }
    const imageData= ctx.getImageData(0,0,canvas.width,canvas.height);
    const newImageData= processor(imageData);
    ctx.putImageData(newImageData.imageData, 0, 0);
    return {canvas, compressible:newImageData.compressible};
}

