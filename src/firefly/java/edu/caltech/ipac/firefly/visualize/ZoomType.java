/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
* @author Trey Roby
 * Date: 7/7/11
*/
public enum ZoomType {
                      STANDARD,       // use normal zoom, zoom to given zoom level or 1x if not specified
                      LEVEL,       // use normal zoom, zoom to given zoom level or 1x if not specified
                      FORCE_STANDARD, // Like standard, however even expanded mode does not override
                      SMART,         // use smart zoom - this is deprecated, don't use anymore
                      FULL_SCREEN,       // requires width & height specified
                      TO_WIDTH,          // requires width
                      TO_WIDTH_HEIGHT,   // requires width
                      TO_HEIGHT,         // requires height, not yet implemented
                      ARCSEC_PER_SCREEN_PIX // arcsec
                      }

