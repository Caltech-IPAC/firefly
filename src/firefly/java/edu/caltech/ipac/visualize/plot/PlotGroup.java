/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class manages groups of plots.  This is primary necessary for overlays.
 * There is a base plot that the overlays are projected onto.  A PlotGroup is a
 * plot and all its overlays.
 */
public class PlotGroup implements Iterable<Plot> {

    private int _imageWidth;     // image width of the mosaic of images
    private int _imageHeight;    // image height of the mosaic of images 
    private int _screenWidth;    // screen width of the mosic of imagges
    private int _screenHeight;   // screen height of the mosic of imagges


            // keep the mosaic image min/max
    private int _minX;     // image min x
    private int _maxX;     // image max x
    private int _minY;     // image min y
    private int _maxY;     // image max y

    private final boolean _overlayEnabled= true;

    private Plot            _basePlot;
    private final List<Plot>  _plotList  = new ArrayList<>(3);
    private PlotContainer    _plotView;
    private AffineTransform _trans;
    private AffineTransform _inverseTrans;
 

    public PlotGroup(Plot basePlot) {
       _basePlot= basePlot;
       addPlot(basePlot);
    }

    /**
     * Get the PlotView.
     * A PlotGroup contains a reference to the PlotView that contains it.
     * A PlotGroup may be in only one PlotView.
     * @return PlotView the PlotView this plot is in.
     */
    public PlotContainer getPlotView() { return _plotView; }

    /**
     * Return the base plot for the plot group.  The base plot is the plot that
     * defines the projection all other plot are reprojected to.
     * @return a Plot
     */
    public Plot getBasePlot() { return _basePlot; }

    public void freeResources() {
        _plotList.clear();
        _plotView= null;
        _trans= null;
        _inverseTrans= null;
        _basePlot= null;
    }
    

    public boolean isOverlayEnabled() { return true; }
    public int getScreenWidth()     { return _screenWidth; }
    public int getScreenHeight()     { return _screenHeight; }
    public int getGroupImageWidth()  { return _imageWidth;   }
    public int getGroupImageHeight() { return _imageHeight;  }

    public int getGroupImageXMin()   { return _minX; }
    public int getGroupImageXMax()   { return _maxX; }
    public int getGroupImageYMin()   { return _minY; }
    public int getGroupImageYMax()   { return _maxY; }

    /**
     * return a iterator of the list of plots
     * @return a iterator of type plot for all the plots in this plot group
     */
    public Iterator<Plot> iterator() { return _plotList.iterator(); }


    /** 
     * called right before the listeners are called
     * @param g2 the graphics class
     */
    public void beginPainting(Graphics2D g2) {
        g2.transform((AffineTransform)_trans.clone());
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        clear(g2);
    }

    /**
     * Determine if a image point is in the plot boundaries.
     * @param ipt image point to test
     * @return boolean true if it is in the boundaries, false if not.
     */
    public boolean pointInPlot( ImageWorkSpacePt ipt) {
        double x= ipt.getX();
        double y= ipt.getY();
        return (x >= _minX && x <= _maxX && y >= _minY && y <= _maxY );
    }


    /**
     * zoom to a certain level.  The flow can be any number. 1.0 is original size,
     * 2.0 is double size, 0.5 is half size.  You even do something such as 3.24.
     * @param level the zoom level
     */
   public void setZoomTo(float level) {
       _screenWidth  = (int)((Math.abs(_minX) + Math.abs(_maxX))  * level);
       _screenHeight = (int)((Math.abs(_minY) + Math.abs(_maxY))  * level);
       if (_screenWidth == 0 && _minX != _maxX) {  _screenWidth = 1; }
       if (_screenHeight == 0 && _minY != _maxY) {  _screenHeight = 1; }
       int maxYscale = (int)(Math.abs(_maxY) * level);
       int minXscale = (int)(Math.abs(_minX) * level);
       double scaleX =  1.0 * level;
       double scaleY = -1.0 * level;
       setTransform(new AffineTransform(scaleX,0,0,scaleY, minXscale,maxYscale) );
   }

    public float getZoomFact() {
       return (float)getTransform().getScaleX();
    }

    /**
     * Is the passed plot the base plot. Returns true if it is. Does not check
     * if the plot is even on the list.
     * @param plot the plot to test
     * @return true if the plot is the base, false otherwise
     */
    public boolean isBasePlot(Plot plot) {
        return (plot == _basePlot);
    }

    /**
     * finds where this image sites on the larger drawing pallet.
     * @param targetPlot the plot we are searching for
     * @return ImageLocation object with the minx,maxX,minY,maxY
     */
    public ImageLocation getImageLocation(Plot targetPlot) {
        int minX, maxX, minY, maxY;
        ImageLocation retval= null;
        for(Plot p : _plotList) {
            if (p==targetPlot) {
                if (p.isPlotted()) {
                    minX= p.getOffsetX();
                    maxX= p.getImageDataWidth() + p.getOffsetX();
                    minY= p.getOffsetY();
                    maxY= p.getImageDataHeight() + p.getOffsetY();
                    retval= new ImageLocation(p,minX,maxX,minY,maxY);
                }
                break;
            }
        }
        return retval;
    }


  // ------------------------------------------------------------
  // ================= Package methods ==========================
  // ------------------------------------------------------------

    void addPlot(Plot p) {
       _plotList.add(p);
    }

    void removePlot(Plot p) { _plotList.remove(p); }



    /**
     * Get the transform this plot uses
     * @return AffineTransform the transform
     */
    AffineTransform getTransform() { return _trans; }

    /**
     * Get the inverse transform this plot uses
     * @return AffineTransform the transform
     */
    AffineTransform getInverseTransform() { return _inverseTrans; }


    /**
     * Set the transform this plot uses, this method should only be called 
     * from subclasses of Plot.
     * @param trans the transform
     */
   void setTransform(AffineTransform trans) { 
       _trans= trans;
       try {
            _inverseTrans= trans.createInverse(); 
       } catch (NoninvertibleTransformException ex) {
            System.out.println(ex.getMessage());
            _inverseTrans= null;
       }
   }




   void addToPlotted() {
      computeMinMax();
      if (_trans==null) setZoomTo(_basePlot.getInitialZoomLevel());
      else              setZoomTo((float)_trans.getScaleX());
   }

   void removeFromPlotted() {
      if (_basePlot.isPlotted()) {
        computeMinMax();
        setZoomTo((float) _trans.getScaleX());
      }
   }


  // ------------------------------------------------------------
  // ================= Private / Protected methods ==============
  // ------------------------------------------------------------
    /**
     * this method will coordinate between all the plots setting up
     * the transform and the width and height as new plots are added.
     * should this be merge with addPlot() ????
     */ 
   private void computeMinMax() {
       int minX, maxX, minY, maxY;
       _minX= 0;
       _maxX= 0;
       _minY= 0;
       _maxY= 0;
        for(Plot p : _plotList) {
          if (p.isPlotted()) {
             minX= p.getOffsetX(); 
             maxX= p.getImageDataWidth() + p.getOffsetX(); 
             minY= p.getOffsetY(); 
             maxY= p.getImageDataHeight() + p.getOffsetY(); 
             if (minX < _minX) _minX= minX;
             if (maxX > _maxX) _maxX= maxX;
             if (minY < _minY) _minY= minY;
             if (maxY > _maxY) _maxY= maxY;
          }
       }
       _imageWidth=  Math.abs(_minX) + Math.abs(_maxX);
       _imageHeight= Math.abs(_minY) + Math.abs(_maxY);
   }

   public void clear(Graphics2D g2) {
       g2.setPaint( Color.black );
       g2.fill( new Rectangle(_minX,_minY, _imageWidth, _imageHeight) );
   }


    /**
     * Inner class for show returning the location of an image on the large
     * plotting pallet. All values are in image coordinates
     */
    public static class ImageLocation  {
        private final Plot _p;
        private final int _minX;     // image min x
        private final int _maxX;     // image max x
        private final int _minY;     // image min y
        private final int _maxY;     // image max y
        private ImageLocation(Plot p, int minX, int maxX, int minY, int maxY) {
            _p= p;
            _minX= minX;
            _maxX= maxX;
            _minY= minY;
            _maxY= maxY;
        }
        public Plot getPlot() {return _p; }
        public int getMinX() { return _minX; }
        public int getMaxX() { return _maxX; }
        public int getMinY() { return _minY; }
        public int getMaxY() { return _maxY; }
    }

}
