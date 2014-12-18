package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.astro.conv.CoordConv;
import edu.caltech.ipac.astro.conv.LonLat;
import edu.caltech.ipac.astro.target.DatedPosition;

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * This class is the abstrct base class for all plots.  Almost all classes that 
 * operate on plots use this class not the subclasses that implement it.
 * Publicly this class operatoes in three coordinate system.  
 * A Image coordinate system, a world coordinate system, and a screen
 * coorinate system.
 * <ul>
 * <li>The image coordinate system is the coordinate system of the data. 
 * <li>The world coordindate system is the system that the data represents 
 *        (i.e. the coordinate system of the sky)
 * <li>Screen ccoordinates are the pixel values of the screen.
 * </ul>
 *  We go from screen to image coordinate using the java transform classes.  
 *  We go from image to world using the abstract methods in this class.
 *
 * @author Trey Roby
 * @version $Id: Plot.java,v 1.20 2010/12/10 21:52:10 roby Exp $
 */
public abstract class Plot implements PlotPaintListener {

    /**
     * used for the zoom call.  the call will zoom up.
     */
    public static final int UP   = 1;
    /**
     * used for the zoom call.  the call will zoom down.
     */
    public static final int DOWN = 2;


    public static final String MOVING_TARGET_CTX_ATTR= "MOVING_TARGET_CTX_ATTR";
    public static final String SUGESTED_NAME_ATTR= "SUGESTED_NAME_ATTR";
    public static final String PLOTTED_FILE_PATH= "PLOTTED_FILE_PATH";
    public static final String MOUSE_READOUT_HINTS_ATTR= "MOUSE_READOUT_HINTS_ATTR";
    public static final String REQ_RADIUS_ARCSEC_ATTR= "REQ_RADIUS_ARCSEC_ATTR";
    public static final String REQ_WIDTH_ARCSEC_ATTR= "REQ_WIDTH_ARCSEC_ATTR";
    public static final String REQ_HEIGHT_ARCSEC_ATTR= "REQ_HEIGHT_ARCSEC_ATTR";
    public static final String READOUT_ATTR= "READOUT_ATTR";
    public static final String DISABLE_OVERLAYS_ATTR= "DISABLE_OVERLAYS_ATTR";


    private   List<NewPlotNotificationListener> _plotStatus      =
                                      new ArrayList<NewPlotNotificationListener>(4);
    private   float               _zFactor         = 2.0F;
    private   String              _plotDesc;
    private   String              _shortPlotDesc;
    private   boolean             _show            = true;
    private   float               _initialZoomLevel= 1.0F;
//    private   MovingTargetContext _movingContext   = null;
    protected boolean             _available       = true;
    protected float               _percentOpaque   = 1.0F;
    public    PlotGroup           _plotGroup       = null;
    protected int                 _offsetX         = 0;
    protected int                 _offsetY         = 0;
    protected String              _sugestedFileName= null;
    protected Map<String,Object>  _attributes= new HashMap<String,Object>(3);

    public Plot() { this(null); }

    public Plot(PlotGroup plotGroup) {
       if (plotGroup == null)
          _plotGroup= new PlotGroup(this);
       else {
          _plotGroup= plotGroup;
          plotGroup.addPlot(this);
       }
    }

    public PlotGroup getPlotGroup() { return _plotGroup; }

    /**
     * This method will return the width of the image in screen coordinates.
     * This number will change as the plot is zoomed up and down.
     * @return the width of the plot
     */
    public abstract int     getScreenWidth();

    /**
     *  This method will return the height of the image in screen coordinates.
     *  This number will change as the plot is zoomed up and down.
     * @return the height of the plot
     */
    public abstract int     getScreenHeight();

    /**
     * This method will return the width of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return the width of the image data
     */
    public abstract int     getImageDataWidth();

    /**
     * This method will return the height of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return the height of the image data
     */
    public abstract int     getImageDataHeight();


    /**
     * This method will return the width of the image in the world coordinate
     * system (probably degrees on the sky).
     * @return the width of the image data in world coord system.
     */
    public abstract double  getWorldPlotWidth();

    /**
     * This method will return the height of the image in the world coordinate
     * system (probably degrees on the sky).
     * @return the height of the image data in world coord system.
     */
    public abstract double  getWorldPlotHeight();

    /**
     * This method work like a clone except is makes a plot so it shares
     * some of the objects.  The result of the class will produce a plot that
     * shares the ImageData.  If the ImageData get changed both plots will
     * change. 
     * @return Plot a new plot that shares image data.
     */
    public abstract Plot    makeSharedDataPlot();

    /**
     * This method work like a clone except is makes a plot so it shares
     * some of the objects.  The result of the class will produce a plot that
     * shares the ImageData.  If the ImageData get changed both plots will
     * change.
     * @param plotGroup the PlotGroup to make this plot in
     * @return Plot a new plot that shares image data.
     */
    public abstract Plot    makeSharedDataPlot(PlotGroup plotGroup);

    /**
     * Determine if a world point is in the plot bounderies.
     * @param wpt the point to test.
     * @return boolean true if it is in the bounderies, false if not.
     */
    public abstract boolean pointInPlot(WorldPt wpt);


    /**
     * Determine if a image point is in the plot bounderies.
     * @param pt the point to test.
     * @return boolean true if it is in the bounderies, false if not.
     */
    public abstract boolean pointInPlot(ImageWorkSpacePt pt);



    /**
     * get the coordinate system of the plot.
     * @return  CoordinateSys  the coordinate system.
     */
    public abstract CoordinateSys getCoordinatesOfPlot();

    /**
     * get the flux of a given image point point on the plot.
     * @param pt the image point
     * @return double the flux value
     * @throws PixelValueException if the pixel is invalid
     */

    public abstract double getFlux(ImageWorkSpacePt pt)
                                      throws PixelValueException;

    /**
     * get units that this flux data is in.
     * @return String the units.
     */
    public abstract String getFluxUnits();

    /**
     * get the scale (usaully in arcseconds) that on image pixel of data
     * represents.
     * @return double the scale of one pixel.
     */
    public abstract double getPixelScale();


    public abstract boolean coordsWrap(WorldPt wp1, WorldPt wp2);

    /**
     * Return the image coordinates given screen x & y.
     * @param ev screen coordinates to convert from
     * @return ImagePt the translated coordinates
     * @throws NoninvertibleTransformException if this point can be transformed in the
     *                                conversion process- almost never happens
     */
    public ImagePt getImageCoords(MouseEvent ev)
                                  throws NoninvertibleTransformException {
        return getImageCoords(new Point(ev.getX(), ev.getY()));

    }
    public ImageWorkSpacePt getImageWorkSpaceCoords(MouseEvent ev)
                                  throws NoninvertibleTransformException {
        return getImageWorkSpaceCoords(new Point(ev.getX(), ev.getY()));

    }
    /**
     * Return the image coordinates given screen x & y.
     * @param pt screen coordinates to convert from
     * @return ImagePt the translated coordinates
     * @throws NoninvertibleTransformException if this point can be transformed in the
     *                                conversion process- almost never happens
     */
    public ImagePt getImageCoords(Point2D pt)
                                  throws NoninvertibleTransformException {
        AffineTransform inverse= _plotGroup.getInverseTransform();
        ImagePt retval;
        if (inverse != null) {
            Point2D imagePt= inverse.transform(pt, null);
            retval= new ImagePt(imagePt.getX(),imagePt.getY());
        }
        else {
              throw new
                  NoninvertibleTransformException("no inverse tranform");
        }
        return retval;
    }

    public ImageWorkSpacePt getImageWorkSpaceCoords(Point2D pt)
                                  throws NoninvertibleTransformException {
        AffineTransform inverse= _plotGroup.getInverseTransform();
        ImageWorkSpacePt retval;
        if (inverse != null) {
            Point2D imagePt= inverse.transform(pt, null);
            retval= new ImageWorkSpacePt(imagePt.getX(), imagePt.getY());
        }
        else {
              throw new
                  NoninvertibleTransformException("no inverse tranform");
        }
        return retval;
    }


    /**
     * convert from ImageWorkSpacePt to ImagePt
     * This will be overridden for ImagePlot where the plot might be an overlay
     * @param sipt the ImageWorkSpacePt point 
     * @return ImagePt the converted point
     */

   public ImagePt getImageCoords(ImageWorkSpacePt sipt) {
        return new ImagePt(sipt.getX(), sipt.getY());
   }

   public ImageWorkSpacePt getImageWorkSpaceCoords(ImagePt sipt) {
       return new ImageWorkSpacePt(sipt.getX(), sipt.getY());
   }

    /**
     * convert from ImagePt to ImageWorkSpacePt
     * This will be overridden for ImagePlot where the plot might be an overlay
     * @param sipt the ImagePt point 
     * @return ImagePt the converted point
     */

   public ImageWorkSpacePt getImageCoords(ImagePt sipt) {
        return new ImageWorkSpacePt(sipt.getX(), sipt.getY());
   }


    /**
     * Return the image coordinates given a WorldPt class
     * @param wpt the class containing the point in sky coordinates
     * @return ImagePt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into an ImagePt
     */
    public abstract ImageWorkSpacePt getImageCoords(WorldPt wpt)
                                  throws ProjectionException;


    /**
     * Return the sscreen coordinates given WorldPt
     * @param wpt the world point to translate
     * @return Point2D the screen coordinates
     * @throws ProjectionException if the point cannot be projected into a Point2D
     */
    public Point2D getScreenCoords(WorldPt wpt)
                                  throws ProjectionException {

        Point2D retval;
        if (wpt.getCoordSys().equals(CoordinateSys.SCREEN_PIXEL)) {
            retval= new Point2D.Double(wpt.getX(), wpt.getY());
        }
        else if (wpt.getCoordSys().equals(CoordinateSys.PIXEL)) {
            ImageWorkSpacePt imagePt= new ImageWorkSpacePt(wpt.getX(), wpt.getY());
            retval= getScreenCoords(imagePt);
        }
        else {
            ImageWorkSpacePt iwpt= getImageCoords(wpt);
            retval= getScreenCoords(iwpt);
        }
        return retval;
    }

    /**
     * Return the screen coordinates given ImagePt
     * @param ipt the image point to translate
     * @return Point2D the screen coordinates
     */
    public Point2D getScreenCoords(ImagePt ipt) {
        return  _plotGroup.getTransform().transform(
                                 new Point2D.Double(ipt.getX(), ipt.getY()),
                                 null);
    }

    /**
     * Return the screen coordinates given ImageWorkSpacePt
     * @param ipt the ImageWorkSpace point to translate
     * @return Point2D the screen coordinates
     */
    public Point2D getScreenCoords(ImageWorkSpacePt ipt) {
        return  _plotGroup.getTransform().transform(
                                 new Point2D.Double(ipt.getX(), ipt.getY()),
                                 null);
    }

    /**
     * Return the world coordinates given screen x & y.
     * @param pt the screen coordinates to convert to world coordinates
     * @param outputCoordSys the coordinate system you want this screen coordinates
     *                      translated into
     * @return WorldPt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into a WorldPt
     * @throws NoninvertibleTransformException if this point can be transformed in the
     *                                conversion process- almost never happens
     */
    public WorldPt getWorldCoords(
                                  Point2D pt,
                                  CoordinateSys outputCoordSys)
                                  throws NoninvertibleTransformException,
                                         ProjectionException {
	ImageWorkSpacePt iwspt = getImageWorkSpaceCoords(pt);
        return getWorldCoords(iwspt,outputCoordSys);
    }


    /**
     * Return the world coordinates given screen x & y.
     * @param pt the screen coordinates to convert to world coordinates
     * @return WorldPt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into a WorldPt
     * @throws NoninvertibleTransformException if this point can be transformed in the
     *                                conversion process- almost never happens
     */
    public WorldPt getWorldCoords(Point2D pt)
                                  throws NoninvertibleTransformException,
                                         ProjectionException {
	ImageWorkSpacePt iwspt = getImageWorkSpaceCoords(pt);
        return getWorldCoords(iwspt);
    }

    /**
     * Return the world coordinates given screen x & y.
     * @param ev the screen coordinates to convert to world coordinates
     * @return WorldPt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into a WorldPt
     * @throws NoninvertibleTransformException if this point can be transformed in the
     *                                conversion process- almost never happens
     */
    public WorldPt getWorldCoords(MouseEvent ev)
                                  throws NoninvertibleTransformException,
                                         ProjectionException {
        return getWorldCoords(new Point(ev.getX(), ev.getY()));
    }

    /**
     * Return the J2000 sky coordinates given a image x (fsamp) and  y (fline)
     * package in a ImagePt class
     * @param pt the ImageWorkSpacePt
     * @return WorldPt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into an WorldPt
     */
    public WorldPt getWorldCoords(ImageWorkSpacePt pt)
                                          throws ProjectionException {
       return getWorldCoords(pt, CoordinateSys.EQ_J2000);
    }
    /**
     * Return the sky coordinates given a image x (fsamp) and  y (fline)
     * package in a ImageWorkSpacePt class
     * @param pt  the image point
     * @param outputCoordSys The coordiate system to return
     * @return WorldPt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into an WorldPt
     */
    public abstract WorldPt getWorldCoords(ImageWorkSpacePt pt,
                                  CoordinateSys outputCoordSys)
                                                 throws ProjectionException;


    /**
     * Return a point the represents the passed point with a distance in
     * World coordinates added to it.
     * @param wp the world point WorldPt
     * @param x the x of the world coordinates distance away from the point.
     * @param y the y of the world coordinates distance away from the point.
     * @return ImagePt the new point
     * @throws ProjectionException if the point cannot be projected into an ImagePt
     */
    public ImagePt getDistanceCoords(WorldPt wp, double x, double y)
                                                throws ProjectionException {

       ImageWorkSpacePt iwpt= getImageCoords(wp);
       ImagePt pt= new ImagePt(iwpt.getX(), iwpt.getY());
       return getDistanceCoords(pt, x, y);
    }

    /**
     * Return a point the represents the passed point with a distance in
     * Image coordinates added to it.
     * @param pt the initial image point
     * @param x the x of the world coordinates distance away from the point.
     * @param y the y of the world coordinates distance away from the point.
     * @return ImagePt the new point
     * @throws ProjectionException if the point cannot be projected into an ImagePt
     */
    public abstract ImagePt getDistanceCoords(ImagePt pt,
                                              double  x,
                                              double  y)
                                               throws ProjectionException;

    /**
     * Return a point the represents the passed point with a distance in
     * Image coordinates added to it.
     * @param pt the initial image point
     * @param x the x of the world coordinates distance away from the point.
     * @param y the y of the world coordinates distance away from the point.
     * @return ImagePt the new point
     * @throws ProjectionException if the point cannot be projected into an ImagePt
     */
    public abstract ImageWorkSpacePt getDistanceCoords(ImageWorkSpacePt pt,
                                              double  x,
                                              double  y)
                                               throws ProjectionException;



   public abstract void paint(PlotPaintEvent ev);

    /**
     * zoom this plot
     * @param dir the zoom direction.  Must be Plot.UP or Plot.DOWN.
     */
    public abstract void zoom(int dir);

    /**
     * return if this plot is actually plotted.
     * @return boolean is this Plot class plotted
     */
    public abstract boolean isPlotted();

    /**
     * return the factor this plot is scale to (in other words, how much or little
     * the plot is zoomed).
     * @return the scale factor
     */
    public abstract double getScale();


    /**
     * specificly release any reasources held by this object
     * any subclasses who override this method should do a 
     * super.freeResoureces()
     */
    public void freeResources() {
       _plotGroup.removePlot(this);
       _plotGroup= null;
    }

    /**
     * Get the transform this plot uses
     * @return AffineTransform the transform
     */
    public AffineTransform getTransform() {
       return (AffineTransform )_plotGroup.getTransform().clone();
    }

    /**
     * Get the inverse transform this plot uses
     * @return AffineTransform the transform
     */
    public AffineTransform getInverseTransform() {
       return (AffineTransform )_plotGroup.getInverseTransform().clone();
    }



    public void setAttribute(String key, Object attribute) {
        _attributes.put(key,attribute);
    }

    public Object getAttribute(String key) {
        return _attributes.get(key);
    }

    public boolean containsAttributeKey(String key) {
        return _attributes.containsKey(key);
    }

    /**
     * Set the factor to zoom this plot on the next zoom call.
     * e.g. zoom factor of 3 means that it is zoomed 3x<br>
     *      zoom factor of 2 means that it is zoomed 2x
     * @param zFactor the zoom factor.
     */
    public void  setZoomFactor(float zFactor) { _zFactor= zFactor; }

    /**
     * Get the factor that is plot will be zoomed on the next zoom call
     * in other words, zoom factor of 3 means that it is zoomed 3x<br>
     *      zoom factor of 2 means that it is zoomed 2x
     * @return float the zoom factor.
     */
    public float getZoomFactor()              { return _zFactor; }

    /**
     * Set the level a image will be zoom when it is plotted
     * @param  initialZoomLevel the initial zoom level
     */
    public void setInitialZoomLevel(float initialZoomLevel) {
       _initialZoomLevel= initialZoomLevel;
    }

    /**
     * Get the level a image will be zoom when it is plotted
     * @return float the initial zoom level
     */
    public float getInitialZoomLevel() {
       return _initialZoomLevel;
    }



    /**
     * Get the PlotView.
     * A plot contains a reference to the PlotView that contains it.
     * A plot may be in only one PlotView.
     * @return PlotView the PlotView this plot is in.
     */
    public PlotView getPlotView() { return _plotGroup.getPlotView(); }

    /**
     * Get the base plot.  This return a non-null plot if this plot
     * should be drawn on top of a plot.
     * @return Plot the PlotView this plot is in.
     */
    //public Plot getBasePlot() { return _basePlot; }

    /**
     * repair a potion of this plot.
     * @param r  the area to repair.
     */
    public void repair(Rectangle r) { _plotGroup.repair(r); }
    /**
     * repair the whole plot
     */
    public void repair() { repair(null); }

    /**
     * set whether to show or hide this plot
     * @param show true to show, false to hide.
     */
    public void setShowing(boolean show) {
        if (show != _show) {
            _show = show;
            if (_show) {
                getPlotGroup().fireStatusChanged(PlotGroup.ChangeType.PLOT_SHOWING, this, -1);
            } else {
                getPlotGroup().fireStatusChanged(PlotGroup.ChangeType.PLOT_HIDDEN, this, -1);
            }
        }
    }

    /**
     * return if this plot is showing
     * @return boolean true if showing, false if hiding.
     */
    public boolean isShowing() { return _show; }

    /**
     * Set a description of this plot.
     * @param d the plot description
     */
    public void   setPlotDesc(String d) { _plotDesc= d; }

    /**
     * Get the description of this plot.
     * @return String the plot description
     */
    public String getPlotDesc()         { return _plotDesc; }

    /**
     * Set a very short description of this plot.
     * @param d the very short plot description
     */
    public void   setShortPlotDesc(String d) { _shortPlotDesc= d; }
    /**
     * Get the very short description of this plot.
     * @return String the very short plot description
     */
    public String getShortPlotDesc()         { return _shortPlotDesc; }

    public void setPercentOpaque(float percentOpaque) {
         _percentOpaque= percentOpaque;
    }

    public float getPercentOpaque() {
         return _percentOpaque;
    }

    public String toString() {
        return getPlotDesc();
    }

    /**
     * Convert from one coordinate system to another.
     * @param wpt the world point to convert
     * @param to  the coordinate system to convert to
     * @return WorldPt the world point in the new coordinate system
     */
    public static WorldPt convert(WorldPt wpt, CoordinateSys to) {
       if (wpt==null) return null;
       WorldPt retval;
       CoordinateSys from= wpt.getCoordSys();
       if (from.equals(to)) {
          retval= wpt;
       }
       else {
          double tobs= 0.0;
          if (from.equals(CoordinateSys.EQ_B1950)) tobs= 1983.5;
          LonLat ll= CoordConv.doConv(from.getJsys(), from.getEquinox(),
                                      wpt.getLon(),   wpt.getLat(),
                                      to.getJsys(),   to.getEquinox(),   tobs);
          retval= new WorldPt(ll.getLon(), ll.getLat(), to);
       }
       return retval;
    }

   // ===================================================================
   // -------------------- public Listener Methods ----------------------
   // ===================================================================


    public void addPlotStatusListener(NewPlotNotificationListener l) {
       _plotStatus.add(l);
    }
    public void removePlotStatusListener(NewPlotNotificationListener l) {
       _plotStatus.remove(l);
    }



   // =======================================================================
   // ------------------    Private / Protected / Package Methods   ---------
   // =======================================================================

   void setOffsetX(int x) {_offsetX= x;}
   int  getOffsetX() {return _offsetX;}

   void setOffsetY(int y) {_offsetY= y;}
   int  getOffsetY() {return _offsetY;}

    /**
     * Set the transform this plot uses, this method should only be called 
     * from subclasses of Plot.
     * @param trans the transform
     */
   protected void setTransform(AffineTransform trans) {
       _plotGroup.setTransform(trans);
   }


   protected void firePlotStatusNewPlot() {
        List<NewPlotNotificationListener> newlist;
        NewPlotNotificationEvent    e= new NewPlotNotificationEvent(this);
        synchronized (this) {
            newlist = new ArrayList<NewPlotNotificationListener>(_plotStatus);
        }

        for(NewPlotNotificationListener listener: newlist) {
            listener.newPlot(e);
        }
   }




   // ===================================================================
   // ------------------ Public Inner Classes  --------------------------
   // ===================================================================

    /**
     * This class contains information about the moving target this plot
     * was created for.
     */
    public static class MovingTargetContext {
        private Date          _centerDate= null;
        private List<Integer> _naifIDs   = new ArrayList<Integer>(2);
        private DatedPosition _datedPos[]= null;

        public MovingTargetContext(Date centerDate, DatedPosition datedPos[]) {
            _centerDate= centerDate;
            _datedPos= datedPos;
        }

        public MovingTargetContext(Date           centerDate,
                                   DatedPosition  datedPos[],
                                   int            naifID) {
            this(centerDate, datedPos);
            addNaifID(naifID);
        }

        public DatedPosition [] getDatedPositionArray()  { return _datedPos; }

        public Date getCenterDate()  { return _centerDate; }

        public void  addNaifID(int naifID) {
            _naifIDs.add( naifID );
        }

        public void  removeNaifID(int naifID) {
            int idx= _naifIDs.indexOf(naifID);
            if (idx>-1)  _naifIDs.remove( idx);
        }

        public boolean isNaifOnPlot(int naifID)  {
            return _naifIDs.contains( naifID );
        }

        public int getNaifCount() { return _naifIDs.size(); }
    }

}
