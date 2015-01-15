/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/17/11
 * Time: 4:31 PM
 */


import edu.caltech.ipac.firefly.visualize.ZoomType;

/**
* @author Trey Roby
*/
public class ZoomChoice {
    private static final float DEFAULT_MAX_ZOOM_LEVEL = 8.0F;

    private final boolean smartZoom;
    private final ZoomType hint;
    private final float zoomLevel;
    private final int width;
    private final int height;
    private final float arcsecPerScreenPix;
    private boolean hasMaxZoomLevel = true;
    private float maxZoomLevel = DEFAULT_MAX_ZOOM_LEVEL;


    public ZoomChoice(boolean smartZoom,
                      boolean hasMaxZoomLevel,
                      ZoomType hint,
                      float zoomLevel,
                      int width,
                      int height,
                      float arcsecPerScreenPix) {
        this.smartZoom = smartZoom;
        this.hint = hint;
        this.zoomLevel = zoomLevel;
        this.width = width;
        this.height = height;
        this.arcsecPerScreenPix = arcsecPerScreenPix;
        this.hasMaxZoomLevel = hasMaxZoomLevel;
    }

    public boolean isSmartZoom() { return smartZoom; }
    public ZoomType getZoomType() { return hint; }
    public float getZoomLevel() { return zoomLevel; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getArcsecPerScreenPix() { return arcsecPerScreenPix; }
    public boolean hasMaxZoomLevel() {return hasMaxZoomLevel;}
    public float getMaxZoomLevel() {return maxZoomLevel;}
}

