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
public class PlotGroup implements Iterable<ImagePlot> {

    private int _imageWidth;     // image width of the mosaic of images
    private int _imageHeight;    // image height of the mosaic of images 
    private int _screenWidth;    // screen width of the mosaic of images
    private int _screenHeight;   // screen height of the mosaic of images


            // keep the mosaic image min/max
    private int _minX;     // image min x
    private int _maxX;     // image max x
    private int _minY;     // image min y
    private int _maxY;     // image max y

    private final ImagePlot        _basePlot;
    private final List<ImagePlot>  _plotList  = new ArrayList<>(3);
    private AffineTransform _trans;
    private AffineTransform _inverseTrans;
 

    public PlotGroup(ImagePlot basePlot) {
       _basePlot= basePlot;
       addPlot(_basePlot);
    }

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
    public Iterator<ImagePlot> iterator() { return _plotList.iterator(); }


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


  // ------------------------------------------------------------
  // ================= Package methods ==========================
  // ------------------------------------------------------------

    void addPlot(ImagePlot p) {
       _plotList.add(p);
    }

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
        for(ImagePlot p : _plotList) {
             minX= 0;
             maxX= p.getImageDataWidth();
             minY= 0;
             maxY= p.getImageDataHeight();
             if (minX < _minX) _minX= minX;
             if (maxX > _maxX) _maxX= maxX;
             if (minY < _minY) _minY= minY;
             if (maxY > _maxY) _maxY= maxY;
       }
       _imageWidth=  Math.abs(_minX) + Math.abs(_maxX);
       _imageHeight= Math.abs(_minY) + Math.abs(_maxY);
   }

   public void clear(Graphics2D g2) {
       g2.setPaint( Color.black );
       g2.fill( new Rectangle(_minX,_minY, _imageWidth, _imageHeight) );
   }

}
