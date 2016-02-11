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
                      'SMART',         // use smart zoom
                      'SMART_SMALL',   // with smart zoom use sizing algorithm that produces larger size
                      'SMART_LARGE',   // with smart zoom use sizing algorithm that produces smaller size
                      'SMART_FOR_SMALL_FILE', // when smart zoom is not set, use it anyway for smaller files
                      'FULL_SCREEN',       // requires width & height specified
                      'TO_WIDTH',          // requires width
                      'TO_HEIGHT',         // requires height, not yet implemented
                      'ARCSEC_PER_SCREEN_PIX' // arcsec
                      ]);

export default ZoomType;
