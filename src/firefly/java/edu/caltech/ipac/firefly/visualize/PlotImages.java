/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
/**
 * User: roby
 * Date: Aug 8, 2008
 * Time: 2:31:02 PM
 */


/**
 * @author Trey Roby
 */
public class PlotImages implements Serializable,
                                   Iterable<PlotImages.ImageURL> {

    private final static String IMAGE_URL_TOKEN= "--ImageURL--";
    private final static String THUMB_URL_TOKEN= "--ThumbURL--";
    private final static String PLOT_IMAGES_TOKEN= "--PlotImages--";

    private ArrayList<ImageURL> _images;
    private ThumbURL _thumbnailImage;
    private String _templateName;
    private int _screenWidth;
    private int _screenHeight;
    private float _zfact;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    public PlotImages() {this("NONE", 10, 0 , 0, 1F); }

    public PlotImages(String templateName, int size, int screenWidth, int screenHeight, float zfact) {
        _templateName= templateName;
        _screenWidth= screenWidth;
        _screenHeight= screenHeight;
        _zfact= zfact;
        _images= new ArrayList<>(size);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void add(ImageURL image) {
        _images.add(image);
    }

    public void setThumbnail(ThumbURL image) {
        _thumbnailImage= image;
    }

    public int getScreenWidth() { return _screenWidth; }
    public int getScreenHeight() { return _screenHeight; }
    public float getZoomFactor() { return _zfact; }

    public Iterator<PlotImages.ImageURL> iterator() {
        return _images.iterator();
    }

    public PlotImages.ThumbURL getThumbnail() {
       return _thumbnailImage;
    }

    public PlotImages.ImageURL get(int idx) { return _images.get(idx); }

    public String getTemplateName() { return _templateName; }

    public int size() { return _images.size(); }

    public String toString() {
        StringBuilder sb=  new StringBuilder(200);
        sb.append(_thumbnailImage).append(PLOT_IMAGES_TOKEN);
        sb.append(_templateName).append(PLOT_IMAGES_TOKEN);
        sb.append(_screenWidth).append(PLOT_IMAGES_TOKEN);
        sb.append(_screenHeight).append(PLOT_IMAGES_TOKEN);
        sb.append(_zfact).append(PLOT_IMAGES_TOKEN);
        for(int i= 0; (i<_images.size()); i++) {
            sb.append(_images.get(i));
            if (i<_images.size()-1) sb.append(PLOT_IMAGES_TOKEN);
        }
        return sb.toString();
    }

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

    public static class ThumbURL implements Serializable {
        private String _url;
        private int  _width;
        private int  _height;

        public ThumbURL(String url, int width, int height) {
            _url= url;
            _width= width;
            _height= height;
        }
        public String getURL() { return _url; }
        public int getWidth() { return _width; }
        public int getHeight() { return _height; }

        public boolean equals(Object other) {
            boolean retval= false;
            if (other==this) {
                retval= true;
            }
            else if (other!=null && other instanceof ImageURL) {
                ThumbURL iurl= (ThumbURL)other;
                if (ComparisonUtil.equals(_url,iurl._url)  &&
                                          _width==iurl._width &&
                                          _height==iurl._height) {
                    retval= true;
                }
            }
            return retval;
        }

        public String toString() {
            return getURL()+THUMB_URL_TOKEN+
                   getWidth()+THUMB_URL_TOKEN+
                   getHeight();
        }

    }


    public static class ImageURL implements Serializable {
        private String _url;
        private int  _xoff;
        private int  _yoff;
        private int  _width;
        private int  _height;
        private int _index;
        private boolean _created;

        public ImageURL(String url, int xoff, int yoff, int width, int height, int index, boolean created) {
            _url= url;
            _xoff= xoff;
            _yoff= yoff;
            _width= width;
            _height= height;
            _index= index;
            _created= created;
        }

        public int getIndex() { return _index; }
        public int getXoff() { return _xoff; }
        public int getYoff() { return _yoff; }
        public int getWidth() { return _width; }
        public int getHeight() { return _height; }
        public String getURL() { return _url; }
        public boolean isCreated() { return _created; }
        public void setCreated(boolean created) { _created= created; }
        public void updateSizes(int x, int y, int width, int height) {
            _xoff= x;
            _yoff= y;
            _width= width;
            _height= height;
        }

        public boolean equals(Object other) {
            boolean retval= false;
            if (other==this) {
                retval= true;
            }
            else if (other!=null && other instanceof ImageURL) {
                ImageURL iurl= (ImageURL)other;
                if (ComparisonUtil.equals(_url,iurl._url)  &&
                    _xoff==iurl._xoff &&
                    _yoff==iurl._yoff &&
                    _width==iurl._width &&
                    _height==iurl._height) {
                    retval= true;
                }
            }
            return retval;
        }

        public String toString() {

            return getIndex()+IMAGE_URL_TOKEN+
                   getXoff()+IMAGE_URL_TOKEN+
                   getYoff()+IMAGE_URL_TOKEN+
                   getWidth()+IMAGE_URL_TOKEN+
                   getHeight()+IMAGE_URL_TOKEN+
                   getURL()+IMAGE_URL_TOKEN+
                   isCreated();
        }


    }

}

