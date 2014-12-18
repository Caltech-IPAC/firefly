package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.Assert;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Iterator;
import java.util.List;

/**
 * This class is the "gui canvas" that a plot is painted on.  It is a 
 * subclass of JComponent so it can provide all the paint and graphics 
 * interface into Swing.  It also manages multiple plot classes.  Currently
 * only one plot class is primary.  When a paintComponent event comes through
 * it passes the event (and the Graphics2D) to the Plot class that is primary.
 * This is one of the most key classes in the all vis packages.
 *
 * @see Plot
 *
 * @author Trey Roby
 * @version $Id: PlotView.java,v 1.25 2011/01/24 03:56:22 roby Exp $
 * *
 */
public class PlotView extends JComponent implements PlotContainer,
                                                    NewPlotNotificationListener,
                                                    PlotPaintListener,
                                                    Iterable<Plot> {



    private static final int MAX_CACHE_WIDTH = 1500;
    private static final int MAX_CACHE_HEIGHT= 1500;
    private static final long MAX_CACHE_SIZE= MAX_CACHE_WIDTH*MAX_CACHE_HEIGHT;
    public  static final String PRIMARY_PLOT_IDX= "PrimaryPlotIndex";

  private int                   _primaryIndex= 0;
  private PropertyChangeSupport _propChange= new PropertyChangeSupport(this);
 // private MouseCentral          _mouse     = new MouseCentral(this);
  private boolean               _mayChangeSize;
  private Color                 _fillColor= new JLabel().getBackground();

  private BufferedImage         _cacheImage     = null;
  private int                   _cacheWidth;
  private int                   _cacheHeight;
  private boolean               _paintFromCache= false;
  private boolean               _paintByRepair = false;
  private PlotContainerImpl _plotContainer = new PlotContainerImpl();


  /**
   * Create a new PlotView.  This constructor is typically used when the
   * PlotView <em>will</em> be placed insides a scrolled window.
   */
  public PlotView() {
      this(true);
  }

  public Color getFillColor() {
      return _fillColor;
  }



  /**
   * Create a new PlotView.  This constructor is typically used when the
   * PlotView <em>will not</em> be placed insides a scrolled window.
   * @param mayChangeSize passed false when you do not want this PlotView to
   *                change size to fit the plot.
   */
  public PlotView(boolean mayChangeSize) {
    setOpaque(true);
    setBackground(Color.red);
    setForeground(Color.blue);
    _mayChangeSize= mayChangeSize;
    addPlotPaintListener(this);
  }

    public void freeResources() {
        _plotContainer.freeResources();
        _propChange= null;
        //_mouse= null;
        _fillColor= null;
        _cacheImage= null;
    }

  /**
   * Add a plot to list of plots this PlotView contains.
   * This method will fire the PlotViewStatusLister.plotAdded() to
   * all listeners.
   * @param p the Plot to add
   */
  public void addPlot(Plot p) {
    List<Plot> plots= _plotContainer.getPlotList();
    plots.add(p);
    p.getPlotGroup().setPlotView(this);
    p.addPlotStatusListener(this);
    fireStatusChanged(PlotContainer.StatusChangeType.ADDED, p);
    if (p == getPrimaryPlot()) configureForPlot(p);
  }

  /**
   * Remove a plot from list of plots this PlotView contains.
   * If the plot to remove is current then the first plot in the list is made
   * current. This method will fire the PlotViewStatusLister.plotRemoved() to
   * all listeners.
   * @param p the Plot to remove
   */
  public void removePlot(Plot p) {
    p.freeResources();
    Plot primary= getPrimaryPlot();
    if (p.getPlotGroup()!=null) {
          p.getPlotGroup().removePlotView(this);
    }
    List<Plot> plots= _plotContainer.getPlotList();
    plots.remove(p);
    if (p == primary) {
         _paintFromCache= false;
         _primaryIndex= 0;
         if (plots.size() > 1) configureForPlot(plots.get(0));
    }
    else {
         _primaryIndex= plots.indexOf(primary);
    }
    p.removePlotStatusListener(this);
    fireStatusChanged(PlotContainer.StatusChangeType.REMOVED, p);
  }

    public void clearAllPlots() {
        List<Plot> plots= _plotContainer.getPlotList();
        Plot allPlots[]= plots.toArray(new Plot[plots.size()]);
        for(Plot p : allPlots) removePlot(p);
    }

  /**
   * Repair (redraw) an area of the primary plot.
   * @param x the x coordinate
   * @param y the y coordinate
   * @param width the width
   * @param height the height
   */
  public void repair(int x, int y, int width, int height) {
     repair( new Rectangle(x,y,width, height) );
  }

  /**
   * Repair (redraw) the whole primary plot.
   */
  public void repair() {
     Plot p= getPrimaryPlot();
     if (p!=null)
         repair( new Rectangle(0,0, p.getScreenWidth(), p.getScreenHeight() ) );
  }
  /**
   * Repair (redraw) an area of the primary plot. Do the repair by calling
   * the base class repaint.  This will eventually call 
   * <code>paintComponent</code>.
   * @param r the are to repair
   */
  public void repair(Rectangle r) {
     _paintFromCache= false;
     _paintByRepair = true;
     if (r == null) repaint();
     else           repaint(r); 
  }


  /**
   * This method is called by the base class in response to a repaint call.
   * Therefore any repair call ultimately ends up here. <i>WARNING- never
   * call this method directly.</i> It is only public because the base class
   * requires it. <br>
   * paintComponent does the following:
   * <ul> 
   * <li>Loops through all it plots searching for the primary plot.  The
   *     reason for the loop is because i plan (possibly) in the future 
   *     to allow multiple plots to draw.
   * <li>Find the plot to draw.
   * <li>Make sure the clip is not greater then the size of the plot to draw.
   * <li>If this <code>PlotView</code> is showing then call the 
         <code>Plot</code>'s paint method.
   * <li>If this <code>PlotView</code> is not showing the call the 
          <code>Plot</code>'s paint method anyway but make sure the 
          <code>Plot</code>'s showing flag set to false. (This allows the
          <code>Plot</code> to draw its overlays).  Make sure the 
          <code>Plot</code>'s showing flag is returned to its original state.
   * </ul> 
   */
  public void paintCache() {

     Plot primaryP= getPrimaryPlot();
     int  pWidth  = primaryP.getScreenWidth();
     int  pHeight = primaryP.getScreenHeight();

     long testSize= (long)pWidth*(long)pHeight;
     if (testSize < MAX_CACHE_SIZE  && !GraphicsEnvironment.isHeadless() ) {
        //if (_cacheImage == null             ||
        //    _cacheImage.getWidth() < pWidth ||
        //    _cacheImage.getHeight()< pHeight ) {
              //_cacheImage= new BufferedImage( pWidth, pHeight,
              //                             BufferedImage.TYPE_BYTE_INDEXED);
         GraphicsEnvironment ge=GraphicsEnvironment.getLocalGraphicsEnvironment();
         GraphicsDevice gd= ge.getDefaultScreenDevice();
         GraphicsConfiguration config= gd.getDefaultConfiguration();
         try {
             _cacheImage= config.createCompatibleImage(pWidth,pHeight);
             //}
             //else {
             //   _cacheImage.flush();
             //}
             Graphics2D g2= _cacheImage.createGraphics();
             g2.setTransform(new AffineTransform());
             g2.clearRect(0,0,_cacheImage.getWidth(),_cacheImage.getHeight());
             g2.setClip(0,0, pWidth, pHeight );
             _cacheWidth=  pWidth;
             _cacheHeight= pHeight;

             doPaint(g2);
             _paintFromCache= true;
         } catch (IllegalArgumentException e) {
             _paintFromCache= false;
             _cacheImage= null;
         }
     }
     else {
        _paintFromCache =false;
        _cacheImage= null;
     }
//--------------------
  }

  public void doPaint(Graphics2D g2) {
       PlotGroup plotGroup;
       //clearOuterArea(g2);
       for(Plot p: _plotContainer.getPlotList()) {
           plotGroup= p.getPlotGroup();
           if (p == getPrimaryPlot()) {
               g2.clip( new Rectangle(0,0,p.getScreenWidth(),
                                      p.getScreenHeight() ) );

               plotGroup.beginPainting(g2);
               firePlotPaint(g2);
               break;
           } // end if
       } // end loop
  }

  public void paintComponent(Graphics g) {
       Graphics2D g2 = (Graphics2D)g;
       clearOuterArea(g2);
       if (_paintFromCache) {
           g2.drawImage(_cacheImage, 0, 0, _cacheWidth, _cacheHeight, this);
       }
       else {
           doPaint(g2);
       }
       if (!_paintFromCache && !_paintByRepair && isComplexPaint() ) 
                paintCache();
 
       _paintByRepair= false;
  }


  //public MouseCentral getMouseCentral() { return _mouse; }

  /**
   * Return the plot that is primary.  This is the one that the user sees.
   * @return Plot the primary Plot
   */
  public Plot getPrimaryPlot() { 
      Plot p= null;
      List<Plot> plots= _plotContainer.getPlotList();
      if (plots.size() > 0)  p= plots.get(_primaryIndex);
      return p;
  }
  /**
   * Return a plot in this <code>PlotView</code>'s list at a given index.
   * @param i the index of the plot.
   * @return Plot the plot and the index or null if it is not found.
   */
  public Plot getPlot(int i)   { 
      Plot p= null;
      List<Plot> plots= _plotContainer.getPlotList();
      if (plots.size() > i)  p= plots.get(i);
      return p;
  }

  /**
   * Return the number plots in this <code>PlotView</code>'s list.
   * @return int the number of plots in the list
   */
  public int getPlotCount() { return _plotContainer.getPlotList().size(); }

  /**
   * Set the plot that the user sees.  This plot must already be in the list.
   * If it is not in the list then the primary plot does not change.
   * This new primary plot will be displayed on the <code>PLotView</code>.
   * This method call <code>setPrimaryPlotIndex</code> which will call 
   * a fire a PropertyChange event "PRIMARY_PLOT_IDX".
   * @param p the Plot to becomes primary
   */
  public void setPrimaryPlot(Plot p) { 
     int idx= _plotContainer.getPlotList().indexOf(p);
     if (idx > -1) {
         setPrimaryPlotIndex(idx);
     }
  }

  /**
   * Change the primary plot by setting a new index. 
   * This new primary plot will be displayed on the <code>PLotView</code>.
   * This method will fire a PropertyChange event "PRIMARY_PLOT_IDX".
   * @param i the index of the Plot to becomes primary
   */
  public void setPrimaryPlotIndex(int i) {
      List<Plot> plots= _plotContainer.getPlotList();
      Assert.tst(i < plots.size() && i >=0);
      int oldIndex= _primaryIndex;
      Plot oldPlot;
      _primaryIndex= i;
      _propChange.firePropertyChange (PRIMARY_PLOT_IDX, oldIndex, _primaryIndex);
      try {
         oldPlot= plots.get(oldIndex);
      } catch (IndexOutOfBoundsException e) {
         oldPlot= null;
      }
      configureForPlot(plots.get(_primaryIndex), oldPlot );
  }

  /**
   * Get the index of the primary plot. 
   * @return int the index of the primary plot
   */
  public int  getPrimaryPlotIndex()      { return _primaryIndex;}

  /**
   * return a iterator though all the plots in this <code>PLotView</code>. 
   * @return Iterator iterator through all the plots.
   */
  public Iterator<Plot> iterator() {
      return new PlotIterator(_plotContainer.getPlotList().iterator());
  }


  /**
   * Zoom the primary plot up or down.  
   * The zoom factor is the one set on that plot.
   * @param dir the direction to zoom.  Must be either the constant
   * <code>Plot.UP</code> or <code>Plot.DOWN</code>.
   */
  public void zoom(int dir) {
      //DialogSupport.setWaitCursor(true);
      Assert.tst( dir==Plot.UP || dir==Plot.DOWN);
      Plot      p= getPrimaryPlot();
      Component c= getParent();
      ImagePt ipt= null;
      if (c instanceof JViewport) {
            try {
               ipt= findCurrentCenterPoint((JViewport)c); 
               p.zoom(dir);
            } catch (NoninvertibleTransformException e) {
               p.zoom(dir);
            }
      }
      else {
         p.zoom(dir);
      }
      configureForPlot(p);
      if (c instanceof JViewport && ipt != null) {
          setScrollToPoint(ipt, (JViewport)c);
      }
      //SwingUtilities.invokeLater( new Runnable() {
      //                             public void run() {
      //                               DialogSupport.setWaitCursor(false);
      //                             } } );
  }

  /**
   * Zoom the primary all the plots up or down.  
   * The zoom factor is set on the individual plots.
   * @param dir the direction to zoom.  Must be either the constant
   * <code>Plot.UP</code> or <code>Plot.DOWN</code>.
   */
  public void zoomAll(int dir) {
      //DialogSupport.setWaitCursor(true);
      Assert.tst( dir==Plot.UP || dir==Plot.DOWN);
      Plot p;
      Plot primary= getPrimaryPlot();
      zoom(dir);
      for(Iterator i= _plotContainer.getPlotList().iterator(); (i.hasNext()) ;) {
         p= (Plot)i.next();
         if (p!=primary) p.zoom(dir);
      }
      configureForPlot(getPrimaryPlot());
      //DialogSupport.setWaitCursor(false);
  }

   public void firePlotPaint(Graphics2D g2) { _plotContainer.firePlotPaint(getPrimaryPlot(), g2); }

    public void firePlotPaint(Plot p, Graphics2D g2) {
        _plotContainer.firePlotPaint(getPrimaryPlot(), g2);
    }
    // ========================================================================
  // ----------------- PlotPaintListener listener methods -------------------
  // ========================================================================

//    /**
//     *  -- for testing
//     */
//    public void addPlotPaintListenerAtTop(PlotPaintListener l) {
//       _plotPaint.add(0,l);
//    }
    public void addPlotPaintListener(PlotPaintListener l) { _plotContainer.addPlotPaintListener(l); }
    public void removePlotPaintListener(PlotPaintListener l) { _plotContainer.removePlotPaintListener(l); }

    /**
     * Pass a list of PlotPaintListeners and attempt to reorder the
     * the listeners to that order.  Any listener not in the passed
     * list will be put at the end.  Any listener in the passed list
     * not in the listener list will be ignored.
     */
    public void reorderPlotPaintListeners(PlotPaintListener  reorderAry[]) {
        _plotContainer.reorderPlotPaintListeners(reorderAry);
    }

  // ====================================================================
  // ----------------- Add / remove other listener methods ---------------
  // ====================================================================

  /**
   * Add a PlotViewStatusListener.
   * @param l the listener
   */
   public void addPlotViewStatusListener(PlotViewStatusListener l) { _plotContainer.addPlotViewStatusListener(l); }
  /**
   * Remove a PlotViewStatusListener.
   * @param l the listener
   */
   public void removePlotViewStatusListener(PlotViewStatusListener l) { _plotContainer.removePlotViewStatusListener(l); }

    /**
     * Add a property changed listener.
     * @param p listener
     */
   public void addPropertyChangeListener (PropertyChangeListener p) {
      _propChange.addPropertyChangeListener (p);
   }

    /**
     * Remove a property changed listener.
     * @param p listener
     */
   public void removePropertyChangeListener (PropertyChangeListener p) {
      _propChange.removePropertyChangeListener (p);
   }

  // ========================================================================
  // ----------------- Methods form PlotPaintListener interface -------------
  // ========================================================================
   public void paint(PlotPaintEvent ev) {
       getPrimaryPlot().paint(ev);
   }

  // ------------------------------------------------------------
  // ================= Private / Protected methods ==============
  // ------------------------------------------------------------

  void reconfigure() {
     configureForPlot(getPrimaryPlot());
  }

  /**
   * fire the <code>PlotViewStatusListener</code>s. 
   * @param stat a StatusChangeType
   * @param plot that the event is about.
   */
  public void fireStatusChanged(PlotContainer.StatusChangeType stat, Plot plot) {
      _plotContainer.fireStatusChanged(stat,plot);
  }

  /**
   * If the p and oldP have different widths or different heights then
   * call the other configure for plot that actually does the work.
   */
  private void configureForPlot(Plot p, Plot oldP) {
     if (oldP == null ||
         (p.getScreenWidth() != oldP.getScreenWidth() &&
          p.getScreenHeight() != oldP.getScreenHeight()) ) {
              configureForPlot(p);
     }
  }

  /**
   * This method changes the preferred size to the new plot and and does
   * a invalidate/validate.  The has the effect of cause the gui to do all
   * the necessary resizing for the new primary plot.
   */
   private void configureForPlot(Plot p) {
     int width= p.getScreenWidth();
     int height= p.getScreenHeight();
     if (_mayChangeSize) {
         Dimension dim= new Dimension(width, height);
         setPreferredSize( dim);
         setMinimumSize( dim);
         setMaximumSize( dim);
         setSize( dim);
     }
     paintCache();
      if (getParent()!=null) {
          (getParent()).invalidate();
          (getParent().getParent()).validate();
      }
  }


  /**
   * Find the point in the <code>Plot</code> that at the center of
   * the display.  The point returned is in the plot image coordinates.
   * We return it in image coordinates and not screen because if the plot
   * is zoomed the image point will be what we want in the center.
   * The screen coordinates will be completely different.
   * This is accomplished by:
   * <ol>
   * <li>Getting the position of the plot in the <code>JViewPort</code>.  This
   *     position is the upper right hand corner.  This in the screen
   *     coordinates.
   * <li>Getting the size of the <code>JViewport</code>.
   *     This in the screen coordinates.
   * <li>Since we have the x, y, width, and height we can compute the center
   *     point in the screen coordinates. 
   * <li>Get the plot plot transform and create a inverse transform to 
   *     go from screen to image coordinates.
   * <li>Convert the center point to image coordinates.
   * </ol>
   */
  public ImagePt findCurrentCenterPoint(JViewport viewport)
                             throws NoninvertibleTransformException {
      Plot      plot= getPrimaryPlot();
      Rectangle rec= viewport.getViewRect();        // size of the viewport
      Point2D pt= new Point2D.Double(          // compute center
                 rec.getX() + rec.getWidth() / 2.0, 
                 rec.getY() + rec.getHeight()/ 2.0 );
                  // convert the point
      return plot.getImageCoords(pt);
  }

  /**
   * Scroll the scrolled window to the point passed.
   * The point is in image coordinates so in must be translated into screen
   * coordinates.  To compute where we actually scroll to we must get the 
   * size of the window.  The target point for the viewport call is the 
   * upper left had corner.  This is what we compute.
   * <ol>
   * <li>Transform the pt passed from image coordinates to screen coordinates.
   * <li>Get the width and Height of the viewport
   * <li>Determine what the newX,newY for moving the scrolled window.
   * </ol>
   * @param ipt the point to scroll to in image coordinates.
   * @param viewport the viewport
   */
  private void setScrollToPoint(ImagePt ipt, JViewport viewport) {
      Plot      plot   = getPrimaryPlot();
      ImageWorkSpacePt iwspt = new ImageWorkSpacePt(ipt.getX(), ipt.getY());
      Point2D   pixelPt= plot.getScreenCoords(iwspt);
      double    sWidth = viewport.getWidth();
      double    sHeight= viewport.getHeight();
      double    newX   = pixelPt.getX() - sWidth  * 0.5;
      double    newY   = pixelPt.getY() - sHeight * 0.5;
      if ( (newX + sWidth) > plot.getScreenWidth() ) {
                newX= plot.getScreenWidth() - sWidth;
      }
      else if (newX < 0) {
                newX= 0;
      }
      if ( (newY + sHeight) > plot.getScreenHeight() ) {
                newY= plot.getScreenHeight() - sHeight;
      }
      else if (newY < 0) {
                newY= 0;
      }
      Point setPt= new Point((int)newX,(int)newY);
      if (sWidth < plot.getScreenWidth() && sHeight <plot.getScreenHeight()) {
          viewport.setViewPosition(setPt);
      }
  }

  private void clearOuterArea(Graphics2D g2) {
      Component c= getParent();
      g2.setPaint( _fillColor );
      Plot p= getPrimaryPlot();
      if (p!=null) {
           int pWidth= p.getScreenWidth();
           int pHeight= p.getScreenHeight();
           int x,y,width,height;
           Rectangle viewrec;

           if (c instanceof JViewport) {
               JViewport viewport= (JViewport)c;
               viewrec = viewport.getViewRect();
           }
           else {
               Dimension dim= getSize();
               viewrec= new Rectangle(0,0, (int)dim.getWidth(), 
                                           (int)dim.getHeight() );
           }
           if (viewrec.getWidth() > pWidth) {
              x=pWidth;
              y=0;
              width=   (int)(viewrec.getWidth() - pWidth);
              height=  (int)viewrec.getHeight();
              g2.clearRect(x,y,width,height);
              g2.fill( new Rectangle(x,y, width, height) ); 
           }
           if (viewrec.getHeight() > pHeight) {
              x=0;
              y=      pHeight;
              width=  (int)viewrec.getWidth();
              height= (int)(viewrec.getHeight() - pHeight);
              g2.fill( new Rectangle(x,y, width, height) ); 
           }
      }

  }
    public boolean isComplexPaint() { return _plotContainer.isComplexPaint(); }


  // ------------------------------------------------------------
  // ========== Listener methods for NewPlotNotificationListener =========
  // ------------------------------------------------------------
  /**
   * the newPlot from NewPlotNotificationListener
   */
  public void newPlot(NewPlotNotificationEvent e) {
     if (e.getPlot() == getPrimaryPlot()) {
         configureForPlot(getPrimaryPlot());
     }

  }

  // -------------------------------------------------------------------
  // ==================  public Inner classes ==========================
  // -------------------------------------------------------------------
 
  /**
   * This PlotIterator implements iterator and is constructed with a
   * list iterator.  It adds functionality on the delete.  When a plot is
   * deleted is makes sure the primary plot index is moved appropriately.
   */
  private class PlotIterator implements Iterator<Plot> {
       Iterator<Plot> _iterator;
       Plot     _p;
       public PlotIterator(Iterator<Plot> iterator) {
          _iterator= iterator;
       }
       public void remove() {
            //_p.freeResources();
            List<Plot> plots= _plotContainer.getPlotList();
            Plot primary= getPrimaryPlot();
            if (_p.getPlotGroup()!=null) {
               _p.getPlotGroup().removePlotView(PlotView.this);
            }
            _iterator.remove();
            if (_p == primary) {
                 _primaryIndex= 0;
                 if (plots.size() > 1) configureForPlot(plots.get(0));
            }
            else {
                 _primaryIndex= plots.indexOf(primary);
            }
            _p.removePlotStatusListener(PlotView.this);
            _p.freeResources();           
            fireStatusChanged(PlotContainer.StatusChangeType.REMOVED, _p);
       }
      public Plot  next()    {
           _p=  _iterator.next();
           return _p;
      }
      public boolean hasNext() { return _iterator.hasNext(); }
  }


}
