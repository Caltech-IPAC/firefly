/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.astro.conv.CoordConv;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is the major subclass implementation of Plot.  This class plots 
 * fits data.
 *
 * @author Trey Roby
 */
public class ImagePlot extends Plot implements Serializable {

//    private static final int CORE_CNT= Runtime.getRuntime().availableProcessors();
    private static final int CORE_CNT= 1;
    public static final int  SQUARE = 1500; // this is the size of the image data tiles

    private Projection     _projection;
    private ImageDataGroup _imageData;
    private boolean        _isPlotted    = false;
    private CoordinateSys  _imageCoordSys= CoordinateSys.UNDEFINED;
    private ImagePt        _minPt;
    private ImagePt        _maxPt;
    private   int            imageScaleFactor;
    private Band             refBand;

//    private ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
    private boolean  _threeColor;
    private boolean _isSharedDataPlot = false;



   public ImagePlot(PlotGroup plotGroup) { super(plotGroup); }


    public ImagePlot(PlotGroup plotGroup,
                     ActiveFitsReadGroup frGroup,
                     float     initialZoomLevel,
                     boolean   threeColor,
                     Band      band,
                     int       initColorID,
                     RangeValues stretch)  throws FitsException{
        super(plotGroup);
        refBand= band;
        setInitialZoomLevel(initialZoomLevel);
        imageScaleFactor= frGroup.getFitsRead(band).getImageScaleFactor();
        _threeColor= threeColor;
        if (_threeColor) {
            _imageData = new ImageDataGroup(frGroup.getFitsReadAry(),  ImageData.ImageType.TYPE_24_BIT,
                                            initColorID,stretch,SQUARE, false);
        }
        else {
            _imageData = new ImageDataGroup(frGroup.getFitsReadAry(),  ImageData.ImageType.TYPE_8_BIT,
                                            initColorID,stretch,SQUARE, false);
        }
        configureImage(frGroup);
    }

    public boolean isThreeColor() { return _threeColor; }


    public void setThreeColorBand(FitsRead colorBandFitsRead, Band band, ActiveFitsReadGroup frGroup)
                                   throws GeomException, 
                                          FitsException,
                                          IOException {
        threeColorOK(band);
//        ImagePlot basePlot= (ImagePlot)getPlotGroup().getBasePlot();
        FitsRead refFitsRead= frGroup.getFitsRead(refBand);

        if (refFitsRead.isSameProjection(colorBandFitsRead)) {
            frGroup.setFitsRead(band,colorBandFitsRead);
        }
        else {
            frGroup.setFitsRead(band,FitsRead.createFitsReadWithGeom( colorBandFitsRead, refFitsRead, false));
        }
        _imageData.markImageOutOfDate();
    }


    public void removeThreeColorBand(Band band, ActiveFitsReadGroup frGroup) {
        threeColorOK(band);
        if (band==refBand) { // replace the ref band
            for(Band b : new Band[] {Band.RED,Band.GREEN,Band.BLUE}) {
                if (b!=refBand && frGroup.getFitsRead(b)!=null) {
                    refBand= b;
                    break;
                }
            }
        }
        frGroup.setFitsRead(band, null);
        _imageData.markImageOutOfDate();
    }


    public Projection getProjection() { return _projection; }

    public boolean isColorBandInUse(Band band, ActiveFitsReadGroup frGroup) {
        threeColorOK(band);
        return frGroup.getFitsRead(band)!=null;
    }

    public boolean isColorBandVisible(Band band, ActiveFitsReadGroup frGroup) {
        threeColorOK(band);
        return frGroup.getFitsRead(band)!=null;
    }


   /**
    * Writes a FITS file with (possible) FITS extension images 
    *
    * @param stream Output File Stream
    * @param fitsRead of FitsRead objects
    * @throws FitsException if problem is fits related
    * @throws IOException  if problem is in writing the file
    */

   static public void writeFile(OutputStream stream, FitsRead[] fitsRead)
				   throws FitsException, IOException{
      Fits output_fits = new Fits();
       for(FitsRead fr : fitsRead) {
	  Fits one_fits = fr.getFits();
	  BasicHDU one_image_hdu = one_fits.getHDU(0);
	  Header header = one_image_hdu.getHeader();
	  Data data = one_image_hdu.getData();
	  ImageHDU image_hdu = new ImageHDU(header, data);
	  output_fits.addHDU(image_hdu);
      }
      output_fits.write(new DataOutputStream(stream));
   }

    /**
     * get the coordinate system of the plot.
     * @return  CoordinateSys  the coordinate system.
     */
   public CoordinateSys getCoordinatesOfPlot() { return _imageCoordSys; }


//    public void paint(PlotPaintEvent ev, ActiveFitsReadGroup frGroup) {
//        Graphics2D g2= ev.getGraphics();
//        if (shouldPaint()) {
//            SaveG2Stuff  saveStuff= prePaint(g2,imageScaleFactor, getPercentOpaque());
//            for(ImageData id : _imageData) {
//                g2.drawImage(id.getImage(frGroup.getFitsReadAry()),
//                             Math.round(getOffsetX()/imageScaleFactor) + id.getX(),
//                             Math.round(getOffsetY()/imageScaleFactor) + id.getY(),
//                             null);
//            }
//
//            postPaint(saveStuff);
//        }
//    }



    public static SaveG2Stuff prePaint(Graphics2D g2, int imageScaleFactor, float percentOpaque) {
        AffineTransform trans= g2.getTransform();
        AffineTransform saveTrans= (AffineTransform)trans.clone();
        trans.scale(imageScaleFactor, imageScaleFactor);
        g2.setTransform(trans);
        Composite savComposite= g2.getComposite();
        Composite workComposite= AlphaComposite.getInstance( AlphaComposite.SRC_OVER, percentOpaque);
        g2.setComposite(workComposite);
        return new SaveG2Stuff(g2,savComposite,saveTrans);
    }

    public static void postPaint(SaveG2Stuff saveStuff) {
        Graphics2D g2= saveStuff._g2;
        g2.setComposite(saveStuff._composite);
        g2.setTransform(saveStuff._trans);
    }

    public void preProcessImageTiles(final ActiveFitsReadGroup frGroup) {
        if (_imageData.isUpToDate()) return;
        synchronized (this) {
            if (_imageData.size()<4 || CORE_CNT<4) {
                for(ImageData id : _imageData)  id.getImage(frGroup.getFitsReadAry());
            }
            else {
                ExecutorService executor = Executors.newFixedThreadPool((CORE_CNT/2)+1);
                for(ImageData id : _imageData)  {
                    final ImageData idSave= id;
                    Runnable worker = new Runnable() {
                        public void run() {
                            idSave.getImage(frGroup.getFitsReadAry());
                        }
                    };
                    executor.execute(worker);
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(5000, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    // just return
                }
            }
        }
    }


    public void paintTile(Graphics2D g2, ActiveFitsReadGroup frGroup, int x, int y, int width, int height) {
        SaveG2Stuff  saveStuff= prePaint(g2, imageScaleFactor, getPercentOpaque());
        AffineTransform trans= g2.getTransform();
        float zfact= _plotGroup.getZoomFact();
        int cnt= 0;
        for(ImageData id : _imageData) {
            if (intersect(trans, x,y,width,height,id)) {
                int drawX= (int)((float)getOffsetX()/(float)imageScaleFactor + id.getX() - x/zfact);
                int drawY= (int)((float)getOffsetY()/(float)imageScaleFactor + id.getY() + y/zfact);
                g2.drawImage(id.getImage(frGroup.getFitsReadAry()), drawX, drawY, null);
            }
        }
        postPaint(saveStuff);
    }


    private boolean intersect(AffineTransform trans,
                             int x, int y, int width, int height,
                             ImageData id) {

        boolean contains= false;
        boolean containsOpposite= false;

        Rectangle testRec= new Rectangle(x,y,width,height);
        Rectangle imageRecSource= new Rectangle(id.getX(), id.getY(),
                                                id.getWidth(), id.getHeight());
        Rectangle imageRec= trans.createTransformedShape(imageRecSource).getBounds();

        boolean intersects= testRec.intersects(imageRec.getX(), imageRec.getY(),
                                       imageRec.getWidth(), imageRec.getHeight());
        if (!intersects) {
            contains= testRec.contains(imageRec.getX(),
                                       imageRec.getY(),
                                       imageRec.getWidth(),
                                       imageRec.getHeight());
            if (!contains) {
                containsOpposite= imageRec.contains(testRec.getX(), 
                                                    testRec.getY(),
                                                    testRec.getWidth(),
                                                    testRec.getHeight());
            }
        }


        return contains || intersects || containsOpposite;
    }





    private boolean  shouldPaint() {
        return isShowing() && _isPlotted;
    }

    /**
     * This method will return the width of the image in screen coordinates.
     * This number will change as the plot is zoomed up and down.
     * @return the width of the plot
     */
   public int     getScreenWidth()  { return getPlotGroup().getScreenWidth(); }

    /**
     *  This method will return the height of the image in screen coordinates.
     * This number will change as the plot is zoomed up and down.
     * @return the height of the plot
     */
   public int     getScreenHeight() { return getPlotGroup().getScreenHeight();}

    /**
     * This method will return the width of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return the width of the image data
     */
   public int     getImageDataWidth() { 
      return _imageData.getImageWidth() * imageScaleFactor;
   }

    /**
     * This method will return the height of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return the height of the image data
     */
   public int     getImageDataHeight(){ 
      return _imageData.getImageHeight() * imageScaleFactor;
   }
     
    /**
     * This method will return the width of the image in the world coordinate
     * system- degrees on the sky.
     * @return the width of the image data in degrees
     */
   public double getWorldPlotWidth() {
       return _projection.getPixelWidthDegree() *_imageData.getImageWidth();
   }

    /**
     * This method will return the height of the image in the world coordinate
     * system- degrees on the sky.
     * @return the height of the image data in degrees
     */
   public double getWorldPlotHeight() {
       return _projection.getPixelHeightDegree() *_imageData.getImageHeight();
   }

    /**
     * Return the image coordinates given a WorldPt class
     * @param wpt the class containing the point in sky coordinates
     * @return ImageWorkSpacePt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into an ImagePt
     */
    @Override
    public ImageWorkSpacePt getImageCoords( WorldPt wpt)
                                  throws ProjectionException {


        ImageWorkSpacePt retval;
        if (wpt.getCoordSys().equals(CoordinateSys.SCREEN_PIXEL)) {
            try {
                retval= getImageWorkSpaceCoords(new Point2D.Double(wpt.getX(),wpt.getY()));
            } catch (NoninvertibleTransformException e) {
                ProjectionException pe= new ProjectionException("Could not covert screen to image");
                pe.initCause(e);
                throw pe;
            }
        }
        else if (wpt.getCoordSys().equals(CoordinateSys.PIXEL)) {
            retval= new ImageWorkSpacePt(wpt.getX(),wpt.getY());
        }
        else {
            if (!_imageCoordSys.equals(wpt.getCoordSys())) {
                wpt= convert(wpt,_imageCoordSys);
            }
            ProjectionPt proj_pt= _projection.getImageCoords(wpt.getLon(),wpt.getLat());
            retval= new ImageWorkSpacePt( proj_pt.getX() + 0.5F ,  proj_pt.getY() + 0.5F);
        }
        return retval;
    }


    /**
     * Return the image coordinates given screen x & y.
     * @param pt screen coordinates to convert from
     * @return ImagePt the translated coordinates
     */
    @Override
    public ImagePt getImageCoords(Point2D pt)
                                  throws NoninvertibleTransformException {
        AffineTransform inverse= _plotGroup.getInverseTransform();
        ImagePt retval;
        if (inverse != null) {
            Point2D input  = new Point2D.Double(pt.getX(), pt.getY());
            Point2D imagePt= inverse.transform(input, null);
            if (imageScaleFactor > 0) {
                retval= new ImagePt( imagePt.getX() / imageScaleFactor,
                                     imagePt.getY() / imageScaleFactor);
            }
            else {
                retval= new ImagePt(imagePt.getX(),imagePt.getY());
            }
        }
        else {
            throw new NoninvertibleTransformException("no inverse tranform");
        }
        return retval;
    }

    public ImageWorkSpacePt getImageWorkSpaceCoords(Point2D pt)
                                  throws NoninvertibleTransformException {
        AffineTransform inverse= _plotGroup.getInverseTransform();
        ImageWorkSpacePt retval;
        if (inverse != null) {
            Point2D input  = new Point2D.Double(pt.getX(), pt.getY());
            Point2D imagePt= inverse.transform(input, null);
	    retval= new ImageWorkSpacePt(imagePt.getX(), imagePt.getY() );
        }
        else {
            throw new NoninvertibleTransformException("no inverse tranform");
        }
        return retval;
    }


    /**
     * convert from ImageWorkSpacePt to ImagePt
     * This will only change the values in an overlay image
     * @param sipt the ImageWorkSpacePt point 
     * @return ImagePt the converted point
     */

   public ImagePt getImageCoords(ImageWorkSpacePt sipt) {
       double xpass= (sipt.getX()- ((double)getOffsetX()))/imageScaleFactor;
       double ypass= (sipt.getY()- ((double)getOffsetY()))/imageScaleFactor;
       return new ImagePt(xpass, ypass);
   }

    /**
     * convert from ImagePt to ImageWorkSpacePt
     * This will only change the values in an overlay image
     * @param sipt the ImagePt point 
     * @return ImagePt the converted point
     */

   public ImageWorkSpacePt getImageWorkSpaceCoords(ImagePt sipt) {
       double xpass = sipt.getX() * imageScaleFactor + getOffsetX();
       double ypass = sipt.getY() * imageScaleFactor + getOffsetY();
       return new ImageWorkSpacePt(xpass, ypass);
   }


    /**
     * Return the screen coordinates given ImagePt
     * @param ipt the Image point to translate
     * @return Point2D the screen coordinates
     */
    @Override
    public Point2D getScreenCoords(ImagePt ipt) {
        AffineTransform trans=
                                      (AffineTransform)_plotGroup.getTransform().clone();

	double x = ipt.getX() * imageScaleFactor + (double)getOffsetX();
	double y = ipt.getY() * imageScaleFactor + (double)getOffsetY();
	ipt = new ImagePt(x, y);

        return trans.transform(  new Point2D.Double(ipt.getX(), ipt.getY()),
                                 null);
    }


    /** 
     * Return a point the represents the passed point with a distance in
     * World coordinates added to it.
     * @param pt the x and y coordinate
     * @param x the x distance away from the point in world coordinates
     * @param y the y distance away from the point in world coordinates
     * @return ImagePt the new point
     */
    @Override
    public ImagePt getDistanceCoords(ImagePt pt, double x, double y)
                                  throws ProjectionException {
        return _projection.getDistanceCoords(pt,x,y);
    }

    /** 
     * Return a point the represents the passed point with a distance in
     * World coordinates added to it.
     * @param pt the x and y coordinate
     * @param x the x distance away from the point in world coordinates
     * @param y the y distance away from the point in world coordinates
     * @return ImagePt the new point
     */
    @Override
    public ImageWorkSpacePt getDistanceCoords(ImageWorkSpacePt pt, double x, double y)
                                  throws ProjectionException {
        return _projection.getDistanceCoords(pt,x,y);
    }


    /**
     * Determine if a world point is in the plot boundaries.
     * @param wpt the world point to test
     * @return boolean true if it is in the boundaries, false if not.
     */
    @Override
    public boolean pointInPlot(WorldPt wpt) {
        boolean retval;
        try {
            ImageWorkSpacePt ipt= getImageCoords(wpt);
            retval= pointInPlot(ipt);
        } catch (ProjectionException e) {
            retval= false;
        }
        return retval;
    }

    /**
     * Determine if a image point is in the plot boundaries.
     * @param ipt image point to test
     * @return boolean true if it is in the boundaries, false if not.
     */
    @Override
    public boolean pointInPlot( ImageWorkSpacePt ipt) {
       return getPlotGroup().pointInPlot(ipt);
    }

    /**
     * Return the sky coordinates given a image x (fsamp) and  y (fline)
     * package in a ImagePt class
     * @param ipt  the image point
     * @param outputCoordSys The coordinate system to return
     * @return WorldPt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into an WorldPt
     */
   public WorldPt getWorldCoords( ImageWorkSpacePt ipt, CoordinateSys outputCoordSys)
                                               throws ProjectionException {

        double x= ipt.getX();
        double y= ipt.getY();
        WorldPt wpt= _projection.getWorldCoords(x - .5F ,y - .5F );
        if (!outputCoordSys.equals(wpt.getCoordSys())) {
            wpt= convert(wpt, outputCoordSys);
        }
        return wpt;
   }


    /**
     * get the flux of a given image point point on the plot.
     * @param iwspt the image pt
     * @return double the flux value
     * @throws PixelValueException if the pixel value is not on the image
     */

    public double getFlux(ImageWorkSpacePt iwspt, ActiveFitsReadGroup frGroup) throws PixelValueException {

        return  getFluxFromFitsRead(frGroup.getFitsRead(refBand), frGroup, iwspt);
    }



    /**
     * get the flux of a given image point point on the plot.
     * @param frGroup fits read group
     * @param band the three color band to get the flux for
     * @param iwspt the image pt
     * @return double the flux value
     * @throws PixelValueException if the pixel value is not on the image
     */

    public double getFlux(ActiveFitsReadGroup frGroup, Band band, ImageWorkSpacePt iwspt) throws PixelValueException {
        double retval;
        if (band==Band.NO_BAND && !_threeColor) {
            retval= getFlux(iwspt, frGroup);
        }
        else {
            threeColorOK(band);
            retval= getFluxFromFitsRead(frGroup.getFitsRead(band), frGroup, iwspt);

        }
        return  retval;
    }

    /**
     * get three color band source description
     * @param band the three color band to get source description for
     * @return String source description
     */
    public String getPlotDesc(Band band, ActiveFitsReadGroup frGroup) {
        threeColorOK(band);
        return frGroup.getFitsRead(band).getSourceDec();
    }

    
    private void acceptFitsRead(FitsRead fr, ActiveFitsReadGroup frGroup) {
        Assert.argTst(fr==null      ||
                      fr==frGroup.getFitsRead(Band.NO_BAND) ||
                      fr==frGroup.getFitsRead(Band.GREEN) ||
                      fr==frGroup.getFitsRead(Band.BLUE),
                      "You must pass a FitsRead that you have register via "+
                      "the constructor or addThreeColorBand()");
    }


    private void threeColorOK(Band band) {
        Assert.tst(_threeColor,
                   "Must be in three color mode to use this routine");
        Assert.argTst( (band==Band.RED || band==Band.GREEN || band==Band.BLUE),
                       "band must be RED, GREEN, or BLUE");
    }


    /**
     * get the flux of a given image point point on the plot.
     * @param fr the FitsRead to get the flux from
     * @param sipt the point you want the flux for
     * @return double the flux value
     * @throws PixelValueException if the pixel value is not on the image
     */

   private double getFluxFromFitsRead(FitsRead fr, ActiveFitsReadGroup frGroup, ImageWorkSpacePt sipt)
                   throws PixelValueException {
       acceptFitsRead(fr,frGroup);
       double retval= Double.NaN;
       if (fr!=null) {
           int iScaleFactor= fr.getImageScaleFactor();
           double xpass= (sipt.getX()- ((double)getOffsetX()))/iScaleFactor;
           double ypass= (sipt.getY()- ((double)getOffsetY()))/ iScaleFactor;
           ImagePt ipt = new ImagePt(xpass, ypass);

           retval=  fr.getFlux(ipt);
       }
       return retval;
   }

   private String getFluxUnitsFromFitsRead(FitsRead fr, ActiveFitsReadGroup frGroup) {
       acceptFitsRead(fr,frGroup);
       String retval= null;
       if (fr!=null)  retval= fr.getFluxUnits();
       return retval;
   }

    /**
     * get units that this flux data is in.
     * @return String the units.
     */
   public String getFluxUnits(ActiveFitsReadGroup frGroup) {  return getFluxUnits(refBand,frGroup);}

    /**
     * get units that this flux data is in.
     * @param band the band to get the flux unit for
     * @return String the units.
     */
    public String getFluxUnits(Band band, ActiveFitsReadGroup frGroup) {
        return getFluxUnitsFromFitsRead(frGroup.getFitsRead(band), frGroup);
    }

    /**
     * get the scale (usually in arcseconds) that on image pixel of data
     * represents.
     * @return double the scale of one pixel.
     */
   public double getPixelScale(){ return _projection.getPixelScaleArcSec(); }

    /**
     * return if this plot is actually plotted.
     * @return boolean is this Plot class plotted
     */
   public boolean isPlotted() {return _isPlotted; }


   /**
    * zoom to an exact Size
    * @param level the new zoom level
    */
   public void setZoomTo(float level) { getPlotGroup().setZoomTo(level); }

    /**
     * return the factor this plot is scaled to (in other words, how much or little
     * the plot is zoomed).
     * @return the scale factor
     */
   public double getScale() { 
        return getPlotGroup().getTransform().getScaleX();
   }

    /**
     * specifically release any resources held by this object
     */
   public void freeResources() {
       Logger.info("free resources called");
       if (_isPlotted) {
           if (_imageData!=null) {
               if (_isSharedDataPlot)
                    _imageData = null;
               else
                    _imageData.freeResources();
           }


           Logger.info("freeing frGroup: " + !_isSharedDataPlot);

           _projection   = null;
           _imageData    = null;
           _available    = false;
           _isPlotted    = false;
           _imageCoordSys= CoordinateSys.UNDEFINED;
           getPlotGroup().removeFromPlotted();
           super.freeResources();
       }
   }

    /**
     * This method work like a clone except is makes a plot so it shares
     * some of the objects.  The result of the class will produce a plot that
     * shares the ImageData.  If the ImageData get changed both plots will
     * change. 
     * @return Plot a new plot that shares image data.
     */
   public Plot makeSharedDataPlot(ActiveFitsReadGroup frGroup) {
        return makeSharedDataPlot(null,frGroup);
   }

    /**
     * This method works like a clone except is makes a plot so it shares
     * some of the objects.  The result of the class will produce a plot that
     * shares the ImageData.  If the ImageData get changed both plots will
     * change. 
     * @return Plot a new plot that shares image data.
     */
   public Plot makeSharedDataPlot(PlotGroup plotGroup, ActiveFitsReadGroup frGroup) {
        ImagePlot p         = new ImagePlot(plotGroup);
        p._projection       = _projection;
        p._imageData        = _imageData;
        p._isPlotted        = _isPlotted;
        p._available        = _available;
        p._imageCoordSys    = _imageCoordSys;
        p._minPt            = _minPt;
        p._maxPt            = _maxPt;
        p._threeColor       = _threeColor;
        p._attributes       = _attributes;
        p.imageScaleFactor  = imageScaleFactor;
        p.setZoomTo(        1.0F );
        if (plotGroup != null) p.computeOffsetXY(frGroup);
        p.getPlotGroup().addToPlotted();
        p.setPlotDesc(      getPlotDesc() );
        p.setShortPlotDesc( getShortPlotDesc() );
        p._isSharedDataPlot = true;
        return p;
   }
    
//======================

    public ImageDataGroup getImageData() { return _imageData; }


    public HistogramOps getHistogramOps(Band band, ActiveFitsReadGroup frGroup) {
        FitsRead fr= frGroup.getFitsRead(band);
        Assert.argTst(fr!=null, "You have not set a fits read for the passed band");
        return new HistogramOps(frGroup.getFitsReadAry(),band, _imageData);
    }

    //======================
   public boolean coordsWrap(WorldPt wp1, WorldPt wp2) {
         boolean retval= false;
         if (_projection.isWrappingProjection()) {
            try { 
                double worldDist= computeDistance(wp1,wp2);
                double pix= _projection.getPixelWidthDegree();
                double value1= worldDist/pix;
       
                ImageWorkSpacePt ip1= getImageCoords(wp1);
                ImageWorkSpacePt ip2= getImageCoords(wp2);
                
                double xdiff= ip1.getX()-ip2.getX();
                double ydiff= ip1.getY()-ip2.getY();
                double imageDist= Math.sqrt(xdiff*xdiff + ydiff*ydiff);

                retval= ((imageDist / value1) > 3);
             } catch (ProjectionException e) {
                 retval= false;
             }
          }
          return retval;
   }

//=======================================================================
//--------------------- Private / Protected Methods ---------------------
//=======================================================================

   private static final double    DtoR      = Math.PI/180.0;
   private static final double    RtoD      = 180.0/Math.PI;

   private static double computeDistance(WorldPt p1, WorldPt p2) {
	 double lon1Radius  = p1.getLon() * DtoR;
	 double lon2Radius  = p2.getLon() * DtoR;
	 double lat1Radius  = p1.getLat() * DtoR;
	 double lat2Radius  = p2.getLat() * DtoR;
         double cosine =
	    Math.cos(lat1Radius)*Math.cos(lat2Radius)*
	    Math.cos(lon1Radius-lon2Radius)
	    + Math.sin(lat1Radius)*Math.sin(lat2Radius);

	 if (Math.abs(cosine) > 1.0)
	    cosine = cosine/Math.abs(cosine);
	 return RtoD*Math.acos(cosine);
   }


//======================

   private void configureImage(ActiveFitsReadGroup frGroup) throws FitsException {
      try {

         PlotGroup plotGroup= getPlotGroup();
         if (!plotGroup.isOverlayEnabled() && !plotGroup.isBasePlot(this)) {
            throw new FitsException(
                "You may not overlay this image over an image that does " +
                "not contain projection information");
         }
         ImageHeader imageHeader= frGroup.getFitsRead(refBand).getImageHeader();
         _isPlotted= true;
         computeMinMaxPoint();
         try {
             determineCoordSys(frGroup);
             _projection= imageHeader.createProjection(_imageCoordSys );
             if (plotGroup.isBasePlot(this)) {
                 setZoomTo(getInitialZoomLevel());
             }
             else {
                 computeOffsetXY(frGroup);
             }
             plotGroup.addToPlotted();
             firePlotStatusNewPlot();
         } catch (FitsException fe) {
             if (plotGroup.isBasePlot(this)) {
                 _projection= imageHeader.createProjection(_imageCoordSys );
                 plotGroup.setOverlayEnabled(false);
                 setZoomTo(getInitialZoomLevel());
                 plotGroup.addToPlotted();
                 firePlotStatusNewPlot();
             }
             else {
                 FitsException fitsE= new FitsException(
                    "This image does not contain any projection information, "+
                    "you cannot show this image as an overlay");
                  fitsE.initCause(fe);
                  throw fitsE;
             }
         }
      } catch (OutOfMemoryError e) {
         freeResources();
         FitsException fitsE= new FitsException("Out Of Memory");
         fitsE.initCause(e);
         throw fitsE;
      }
   }

   private void computeOffsetXY(ActiveFitsReadGroup frGroup) { //todo: if we ever do offset again this method must be fix, see spot-common
         ImageHeader baseHDR= frGroup.getFitsRead(refBand).getImageHeader();
         ImageHeader thisHDR= frGroup.getFitsRead(refBand).getImageHeader();


	 /* subtract 0.5 from crpix to put it in ImagePt coordinates */
         setOffsetX( (int) Math.round((baseHDR.crpix1-0.5) - 
	     (thisHDR.crpix1-0.5)*imageScaleFactor ));
         setOffsetY( (int) Math.round((baseHDR.crpix2-0.5) - 
	     (thisHDR.crpix2-0.5)*imageScaleFactor ));
   }

   private void determineCoordSys(ActiveFitsReadGroup frGroup) throws FitsException {
       ImageHeader   hdr=  frGroup.getFitsRead(refBand).getImageHeader();
       int sys= hdr.getJsys();
       _imageCoordSys= CoordinateSys.makeCoordinateSys( sys, hdr.getEquinox() );

       // tmp
       if ((hdr.getEquinox() == 2000.0) && (sys == -1) ) {      // tmp
          _imageCoordSys= CoordinateSys.EQ_J2000;  // tmp
       }                                           // tmp
       // tmp

             // if we don't know the coordinate system
             // this next blocks figures out what to say and
             // then throws an exception
       if (_imageCoordSys == CoordinateSys.UNDEFINED) {
            String coordDesc;
            if      (sys == CoordConv.EQUATORIAL_J) coordDesc= "Equatorial J";
            else if (sys == CoordConv.EQUATORIAL_B) coordDesc= "Equatorial B";
            else if (sys == CoordConv.ECLIPTIC_J)   coordDesc= "Ecliptic J";
            else if (sys == CoordConv.ECLIPTIC_B)   coordDesc= "Ecliptic B";
            else if (sys == CoordConv.GALACTIC)     coordDesc= "Galactic";
            else if (sys == CoordConv.SUPERGALACTIC)coordDesc= "SuperGalactic";
            else                                    coordDesc= "Unknown";

            throw new FitsException(
                 "Not yet suporting the coordinate system: " + 
                  coordDesc + " " + hdr.getEquinox());
       }
   }


   private void computeMinMaxPoint() {
      _minPt= new ImagePt(0, 0);
      _maxPt= new ImagePt(getImageDataWidth()-1, getImageDataHeight()-1);
   }


    public static class SaveG2Stuff {
        public final Graphics2D _g2;
        public final Composite _composite;
        public final AffineTransform _trans;
        public SaveG2Stuff ( Graphics2D g2, Composite composite, AffineTransform trans) {
            _g2= g2;
            _composite= composite;
            _trans= trans;
        }
    }

}
