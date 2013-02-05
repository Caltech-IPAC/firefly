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
    }

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

    public ZoomChoice(boolean smartZoom,
                      boolean hasMaxZoomLevel,
                      float maxZoomLevel,
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
        this.maxZoomLevel = maxZoomLevel;
    }

    public ZoomChoice(boolean smartZoom,
                      float maxZoomLevel,
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
        this.maxZoomLevel = maxZoomLevel;
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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
