/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * User: roby
 * Date: 7/7/11
 * Time: 12:46 PM
 */

import Enum from 'enum';

/**
 * @typedef ZoomType
 * @summary zoom type
 * @description can be 'STANDARD', 'LEVEL', 'FULL_SCREEN', 'TO_WIDTH_HEIGHT', 'TO_WIDTH', 'TO_HEIGHT', 'ARCSEC_PER_SCREEN_PIX'
 * @prop STANDARD
 * @prop LEVEL
 * @prop FULL_SCREEN
 * @prop TO_WIDTH_HEIGHT
 * @prop TO_WIDTH
 * @prop TO_HEIGHT
 * @prop ARCSEC_PER_SCREEN_PIX
 * @type {Enum}
 * @public
 * @global
 */
export const ZoomType= new Enum([
                      'STANDARD',       // use normal zoom, zoom to given zoom level or 1x if not specified
                      'LEVEL',       // use normal zoom, zoom to given zoom level or 1x if not specified
                      'FULL_SCREEN',       // requires width & height specified. deprecated, same as TO_WIDTH_HEIGHT
                      'TO_WIDTH_HEIGHT',   // requires width & height specified
                      'TO_WIDTH',          // requires width
                      'TO_HEIGHT',         // requires height, not yet implemented
                      'ARCSEC_PER_SCREEN_PIX' // arcsec
                      ]);
