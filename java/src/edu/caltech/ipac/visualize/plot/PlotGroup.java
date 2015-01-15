/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;


import edu.caltech.ipac.util.Assert;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;

/**
 * This class manages groups of plots.  This is primary necessary for overlays.
 * There is a base plot that the overlays are projected onto.  A PlotGroup is a
 * plot and all its overlays.
 */
public class PlotGroup implements Iterable<Plot> {

    enum ChangeType { ADDED, REMOVED, BAND_ADDED,
                      BAND_REMOVED, BAND_SHOWING, BAND_HIDDEN, PLOT_SHOWING, PLOT_HIDDEN }

    private int _imageWidth;     // image width of the mosaic of images 
    private int _imageHeight;    // image height of the mosaic of images 
    private int _screenWidth;    // screen width of the mosic of imagges
    private int _screenHeight;   // screen height of the mosic of imagges


            // keep the mosaic image min/max
    private int _minX;     // image min x
    private int _maxX;     // image max x
    private int _minY;     // image min y
    private int _maxY;     // image max y

    private int _maxYscale;
    private int _minXscale;
    private boolean _overlayEnabled= true;

    private Plot            _basePlot;
    private List<Plot>      _plotList  = new ArrayList<Plot>(3);
    private List<PlotGroupStatusListener> _plotStatus=
                   new ArrayList<PlotGroupStatusListener>(2);
    private PlotView        _plotView;
    private AffineTransform _trans;
    private AffineTransform _inverseTrans;
 
    static private Color    _fillColor= Color.black;


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
    public PlotView getPlotView() { return _plotView; }

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
    
    public boolean isOverlayPlot(Plot p) {
        boolean retval= false;
        if (_plotList.contains(p)) {
            retval= (p!=_basePlot);
        }
        return retval;
    }

    public boolean isOverlayEnabled() { return _overlayEnabled; }
    public void    setOverlayEnabled(boolean enabled) { 
       _overlayEnabled= enabled;
    }

    /**
     * Remove the PlotView.
     * @param pv the PlotView to remove
     */
    public void removePlotView(PlotView pv) {
       Assert.tst(_plotView == pv);
       _plotView= null;
       for(Plot p: _plotList) {
           if (!isBasePlot(p)) pv.removePlotPaintListener(p);
       }
    }

    /**
     * remove all plots and free all plot resources.  Calling this function will
     * free a lot of memory
     */
    public void removeAllAndFree() {
        Plot p;
        if (_plotList.size() > 0) {
            for(Iterator<Plot> i= _plotList.iterator(); (i.hasNext()); ) {
                  p= i.next();
                  i.remove();
                  removePlotCleanup(p);
                  p.freeResources();
            }
        } // end if
    }
    
    public int getScreenWidth()      { return _screenWidth;  }
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
     * Return the plot list. The list if unmodifiable
     * @return unmodifiable list of type Plot
     */
    public List<Plot> getPlotList() {
        return Collections.unmodifiableList(_plotList);
    }


    public void setColorModelOnAllPlots(IndexColorModel colorModel) {
        ImagePlot ip;
        for(Plot p : _plotList) {
             if (p instanceof ImagePlot) {
                 ip= (ImagePlot)p;
                 ip.getImageData().setColorModel(colorModel);
             }
        }
    }

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
     * Determine if a image point is in the plot bounderies.
     * @param ipt image point to test
     * @return boolean true if it is in the bounderies, false if not.
     */
    public boolean pointInPlot( ImageWorkSpacePt ipt) {
        double x= ipt.getX();
        double y= ipt.getY();
        return (x >= _minX && x <= _maxX && y >= _minY && y <= _maxY );
    }

    /**
     * zoom this plot
     * @param dir the zoom direction.  Must be Plot.UP or Plot.DOWN.
     */
   public void zoom(int dir) {
        if ((_screenWidth > 40 && _screenHeight > 40) || dir==Plot.UP) {
            Assert.tst(_basePlot.isPlotted());
            Assert.tst(dir == Plot.UP || dir == Plot.DOWN);
            AffineTransform trans= getTransform();
            double scaleX= trans.getScaleX();
            double scaleY= trans.getScaleY();
            float zfactor= (dir==Plot.UP) ?
                           _basePlot.getZoomFactor() : 1.0F / _basePlot.getZoomFactor();
            _maxYscale    *= zfactor;
            _minXscale    *= zfactor;
            _screenWidth  *= zfactor;
            _screenHeight *= zfactor;
            scaleX        *= zfactor;
            scaleY        *= zfactor;
            setTransform(new AffineTransform(scaleX,0,0,scaleY,
                                             _minXscale,_maxYscale));
            repair();
        }
       //System.out.println("zoom: factor: "+ zfactor);
       //System.out.println("zoom: _minXScale: "+ _minXscale);
       //System.out.println("zoom: _maxYScale: "+ _maxYscale);
       //System.out.println("");
   }

    /**
     * zoom to a certain level.  The flow can be any number. 1.0 is original size,
     * 2.0 is double size, 0.5 is half size.  You even do something such as 3.24.
     * @param level the zoom level
     */
   public void setZoomTo(float level) {
       _screenWidth  = (int)((Math.abs(_minX) + Math.abs(_maxX))  * level);
       _screenHeight = (int)((Math.abs(_minY) + Math.abs(_maxY))  * level);
       _maxYscale = (int)(Math.abs(_maxY) * level);
       _minXscale = (int)(Math.abs(_minX) * level);
       double scaleX =  1.0 * level;
       double scaleY = -1.0 * level;
       setTransform(new AffineTransform(scaleX,0,0,scaleY,
                                        _minXscale,_maxYscale) ); 
       //System.out.println(this);
       //System.out.println("zoomTo: min/max X:"+ _minX+ ","+_maxX);
       //System.out.println("zoomTo: min/max Y:"+ _minY+ ","+_maxY);
       //System.out.println("zoomTo: level: "+ level);
       //System.out.println("zoomTo: scaleX: "+ scaleX);
       //System.out.println("zoomTo: scaleY: "+ scaleY);
       //System.out.println("zoomTo: _minXScale: "+ _minXscale);
       //System.out.println("zoomTo: _maxYScale: "+ _maxYscale);
       //System.out.println("");
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


  // ====================================================================
  // ----------------- Add / remove other listener methods ---------------
  // ====================================================================
  /**
   * Add a PlotViewStatusListener.
   * @param l the listener
   */
   public void addPlotGroupStatusListener(PlotGroupStatusListener l) {
      _plotStatus.add(l); 
   }
  /**
   * Remove a PlotGroupStatusListener.
   * @param l the listener
   */
   public void removePlotGroupStatusListener(PlotGroupStatusListener l) {
      _plotStatus.remove(l); 
   }

  // ------------------------------------------------------------
  // ================= Package methods ==========================
  // ------------------------------------------------------------
    void setPlotView(PlotView plotView) {
       _plotView= plotView;
      for(Plot p : _plotList) {
          if(!isBasePlot(p)) _plotView.addPlotPaintListener(p);
      }
    }

    void addPlot(Plot p) {
       _plotList.add(p);
       if (_plotView!=null && !isBasePlot(p)) {
           _plotView.addPlotPaintListener(p);
       }
       fireStatusChanged(ChangeType.ADDED,p);
    }

    void removePlot(Plot p) {
       if (_plotList.contains(p)) {
           _plotList.remove(p);
           removePlotCleanup(p);
       }
    }


    void repair(Rectangle r) {
       if (_plotView != null) _plotView.repair(r);
    }
    void repair() { repair(null); }


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
            System.out.println(ex);
            _inverseTrans= null;
       }
   }




   void addToPlotted() {
      computeMinMax();
      if (_trans==null) setZoomTo(_basePlot.getInitialZoomLevel());
      else              setZoomTo((float)_trans.getScaleX());
      if (_plotView!=null) _plotView.reconfigure();
   }

   void removeFromPlotted() {
      if (_basePlot.isPlotted()) {
        computeMinMax();
        setZoomTo((float)_trans.getScaleX());
        if (_plotView!=null) _plotView.reconfigure();
        repair();
      }
   }


  // ------------------------------------------------------------
  // ================= Private / Protected methods ==============
  // ------------------------------------------------------------
    private void removePlotCleanup(Plot p) {
       if (_plotView!=null && !isBasePlot(p)) 
                            _plotView.removePlotPaintListener(p);
       fireStatusChanged(ChangeType.REMOVED,p);
    }

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
       //System.out.println("min/max X:"+ _minX+ ","+_maxX);
       //System.out.println("min/max Y:"+ _minY+ ","+_maxY);
   }



  /**
   * fire the <code>PlotGroupStatusListener</code>s. 
   * @param stat which listener to fire must be the constants
   *            <code>ADDED</code> or <code>REMOVED</code>.
   * @param plot the plot that the event is about.
   */
  private void fireStatusChanged(ChangeType stat, Plot plot) {
      fireStatusChanged(stat, plot, -1);
  }



  void fireStatusChanged(ChangeType stat, Plot plot, int band) {
      List<PlotGroupStatusListener> newlist;
      PlotGroupStatusEvent ev;

      if (band==-1) {
          ev= new PlotGroupStatusEvent(this, plot);
      }
      else {
          ev= new PlotGroupStatusEvent(this, plot, band);
      }
      synchronized (this) {
          newlist = new ArrayList<PlotGroupStatusListener>(_plotStatus);
      }

      switch (stat) {
          case ADDED:
              for(PlotGroupStatusListener l : newlist) l.plotAdded(ev);
              break;
          case REMOVED:
              for(PlotGroupStatusListener l : newlist)  l.plotRemoved(ev);
              break;
          case BAND_ADDED:
              for(PlotGroupStatusListener l : newlist)  l.colorBandAdded(ev);
              break;
          case BAND_REMOVED:
              for(PlotGroupStatusListener l : newlist)  l.colorBandRemoved(ev);
              break;
          case BAND_SHOWING:
              for(PlotGroupStatusListener l : newlist)  l.colorBandShowing(ev);
              break;
          case BAND_HIDDEN:
              for(PlotGroupStatusListener l : newlist)  l.colorBandHidden(ev);
              break;
          case PLOT_SHOWING:
              for(PlotGroupStatusListener l : newlist)  l.plotShowing(ev);
              break;
          case PLOT_HIDDEN:
              for(PlotGroupStatusListener l : newlist)  l.plotHidden(ev);
              break;
          default:
              Assert.tst(false);
              break;
      }
  }


//    static boolean _alternate= true;
   public void clear(Graphics2D g2) {
       g2.setPaint( _fillColor );
//       Color c= _alternate ? Color.RED : Color.BLUE;
//       _alternate= !_alternate;
//       g2.setPaint( c );
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
