/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * User: roby
 * Date: 7/7/11
 * Time: 12:46 PM
 */

import Enum from 'enum';
export const ZoomType= new Enum([
                      'STANDARD',       // use normal zoom, zoom to given zoom level or 1x if not specified
                      'LEVEL',       // use normal zoom, zoom to given zoom level or 1x if not specified
                      'FULL_SCREEN',       // requires width & height specified. deprecated, same as TO_WIDTH_HEIGHT
                      'TO_WIDTH_HEIGHT',   // requires width & height specified
                      'TO_WIDTH',          // requires width
                      'TO_HEIGHT',         // requires height, not yet implemented
                      'ARCSEC_PER_SCREEN_PIX' // arcsec
                      ]);

const whArray= [ZoomType.TO_WIDTH, ZoomType.TO_HEIGHT, ZoomType.FULL_SCREEN,
                ZoomType.TO_WIDTH_HEIGHT, ZoomType.ARCSEC_PER_SCREEN_PIX];

/**
 * Return true if zoom type requires width and height
 * @param zoomType
 */
export const requiresWidthHeight= (zoomType) => whArray.includes( zoomType);
