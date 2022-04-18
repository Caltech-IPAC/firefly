/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import edu.caltech.ipac.visualize.plot.projection.ProjectionUtil;
import nom.tam.fits.FitsException;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is the major subclass implementation of Plot.  This class plots fits data.
 * @author Trey Roby
 */
public class ImagePlot extends Plot implements Serializable {

    public static final int  SQUARE = 1500; // this is the size of the image data tiles

    private Projection     _projection;
    private ImageDataGroup _imageData;
    private CoordinateSys  _imageCoordSys= CoordinateSys.UNDEFINED;
    private Band             refBand;
    private final boolean  _threeColor;
    private final boolean useForMask;
    private int imageDataWidth;
    private int imageDataHeight;


    public ImagePlot(ActiveFitsReadGroup frGroup,
                     float     initialZoomLevel,
                     boolean   threeColor,
                     Band      band,
                     int       initColorID,
                     RangeValues stretch)  throws FitsException{
        super();
        useForMask = false;
        refBand= band;
        setInitialZoomLevel(initialZoomLevel);
        _threeColor= threeColor;
        initDataWidthAndHeight(frGroup);
        _imageData = new ImageDataGroup(imageDataWidth, imageDataHeight,
                _threeColor ? ImageData.ImageType.TYPE_24_BIT : ImageData.ImageType.TYPE_8_BIT,
                initColorID,stretch,SQUARE);
        configureImage(frGroup);
    }

    /** 07/20/15 LZ
     * Create a ImagePlot with given IndexColorModel
     */
    public ImagePlot(ActiveFitsReadGroup frGroup,
                     float     initialZoomLevel,
                     ImageMask[] iMasks,
                     RangeValues stretch)  throws FitsException{
        super();
        refBand= Band.NO_BAND;
        setInitialZoomLevel(initialZoomLevel);
        _threeColor= false;
        useForMask = true;
        initDataWidthAndHeight(frGroup);
       _imageData = new ImageDataGroup(imageDataWidth, imageDataHeight, iMasks,stretch,SQUARE);
        configureImage(frGroup);
    }


    void initDataWidthAndHeight(ActiveFitsReadGroup  frGroup) {
        FitsRead fr= null;
        for(FitsRead testFr : frGroup.getFitsReadAry()) {
            if (testFr!=null) {
                fr= testFr;
                break;
            }
        }
        Assert.argTst(fr, "fitsReadAry must have one non-null element.");
        imageDataWidth = fr.getNaxis1();
        imageDataHeight = fr.getNaxis2();
    }

    public boolean isThreeColor() { return _threeColor; }
    public boolean isUseForMask() { return useForMask; }


    public void setThreeColorBand(FitsRead colorBandFitsRead, Band band, ActiveFitsReadGroup frGroup)
                                   throws GeomException,
                                          FitsException,
                                          IOException {
        threeColorOK(band);
        FitsRead refFitsRead= frGroup.getFitsRead(refBand);

        if (refFitsRead==colorBandFitsRead || ProjectionUtil.isSameProjection(refFitsRead,colorBandFitsRead)) {
            frGroup.setFitsRead(band,colorBandFitsRead);
        }
        else {
            frGroup.setFitsRead(band, FitsReadFactory.createFitsReadWithGeom( colorBandFitsRead, refFitsRead, false));
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

    /**
     * get the coordinate system of the plot.
     * @return  CoordinateSys  the coordinate system.
     */
   public CoordinateSys getCoordinatesOfPlot() { return _imageCoordSys; }




    private static SaveG2Stuff prePaint(Graphics2D g2, float percentOpaque) {
        AffineTransform trans= g2.getTransform();
        AffineTransform saveTrans= (AffineTransform)trans.clone();
        trans.scale(1, 1);
        g2.setTransform(trans);
        Composite savComposite= g2.getComposite();
        Composite workComposite= AlphaComposite.getInstance( AlphaComposite.SRC_OVER, percentOpaque);
        g2.setComposite(workComposite);
        return new SaveG2Stuff(g2,savComposite,saveTrans);
    }

    private static void postPaint(SaveG2Stuff saveStuff) {
        Graphics2D g2= saveStuff.g2();
        g2.setComposite(saveStuff.composite());
        g2.setTransform(saveStuff.trans());
    }

    public void preProcessImageTiles(final ActiveFitsReadGroup frGroup) {
        if (_imageData.isUpToDate()) return;
        synchronized (this) {
            if (_imageData.isUpToDate()) return;
            int coreCnt= ServerContext.getParallelProcessingCoreCnt();
            if (_imageData.size()<4 || coreCnt==1) {
                for(ImageData id : _imageData)  id.getImage(frGroup.getFitsReadAry());
            }
            else {
                ExecutorService executor = Executors.newFixedThreadPool(coreCnt);
                for(ImageData id : _imageData)  {
                    executor.execute(() -> id.getImage(frGroup.getFitsReadAry()));
                }
                executor.shutdown();
                try {
                     boolean normalTermination= executor.awaitTermination(3600, TimeUnit.SECONDS);
                     if (!normalTermination) executor.shutdownNow();
                 } catch (InterruptedException e) {
                     // just return
                 }
            }
        }
    }


    public void paintTile(Graphics2D g2, ActiveFitsReadGroup frGroup, int x, int y, int width, int height) {
        SaveG2Stuff  saveStuff= prePaint(g2, 1);
        AffineTransform trans= g2.getTransform();
        float zfact= _plotGroup.getZoomFact();
        for(ImageData id : _imageData) {
            if (intersect(trans, x,y,width,height,id)) {
                int drawX= (int)(id.getX() - x/zfact);
                int drawY= (int)(id.getY() + y/zfact);
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

    /**
     * This method will return the width of the image in screen coordinates.
     * This number will change as the plot is zoomed up and down.
     * @return the width of the plot
     */
   public int  getScreenWidth()  { return getPlotGroup().getScreenWidth(); }

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
   public int   getImageDataWidth() { return imageDataWidth; }

    /**
     * This method will return the height of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return the height of the image data
     */
   public int  getImageDataHeight(){ return imageDataHeight; }
     
    /**
     * This method will return the width of the image in the world coordinate
     * system- degrees on the sky.
     * @return the width of the image data in degrees
     */
   public double getWorldPlotWidth() { return _projection.getPixelWidthDegree() * getImageDataWidth(); }

    /**
     * This method will return the height of the image in the world coordinate
     * system- degrees on the sky.
     * @return the height of the image data in degrees
     */
   public double getWorldPlotHeight() { return _projection.getPixelHeightDegree() * getImageDataHeight(); }

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
            retval= new ImagePt(imagePt.getX(),imagePt.getY());
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
       double xpass= sipt.getX();
       double ypass= sipt.getY();
       return new ImagePt(xpass, ypass);
   }

    /**
     * convert from ImagePt to ImageWorkSpacePt
     * This will only change the values in an overlay image
     * @param sipt the ImagePt point 
     * @return ImagePt the converted point
     */
    public ImageWorkSpacePt getImageWorkSpaceCoords(ImagePt sipt) {
        return new ImageWorkSpacePt(sipt.getX(), sipt.getY());
    }

    /**
     * Return the screen coordinates given ImagePt
     * @param ipt the Image point to translate
     * @return Point2D the screen coordinates
     */
    @Override
    public Point2D getScreenCoords(ImagePt ipt) {
        AffineTransform trans= (AffineTransform)_plotGroup.getTransform().clone();
        ipt = new ImagePt(ipt.getX(), ipt.getY());
        return trans.transform(  new Point2D.Double(ipt.getX(), ipt.getY()), null);
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
    public ImagePt getDistanceCoords(ImagePt pt, double x, double y) {
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
    public ImageWorkSpacePt getDistanceCoords(ImageWorkSpacePt pt, double x, double y) {
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

    private void threeColorOK(Band band) {
        Assert.argTst( (band==Band.RED || band==Band.GREEN || band==Band.BLUE),
                       "band must be RED, GREEN, or BLUE");
    }

    /**
     * get the scale (usually in arcseconds) that on image pixel of data
     * represents.
     * @return double the scale of one pixel.
     */
   public double getPixelScale(){ return _projection.getPixelScaleArcSec(); }

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
   public double getScale() { return getPlotGroup().getTransform().getScaleX(); }

    /**
     * specifically release any resources held by this object
     */
   public void freeResources() {
       if (_imageData!=null) _imageData.freeResources();
       _projection   = null;
       _imageData    = null;
       _imageCoordSys= CoordinateSys.UNDEFINED;
       if (getPlotGroup()!=null) getPlotGroup().removeFromPlotted();
       super.freeResources();
   }

    public ImageDataGroup getImageData() { return _imageData; }

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
                 return false;
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
           FitsRead fr= frGroup.getFitsRead(refBand);
           _imageCoordSys= fr.getImageCoordinateSystem();
           _projection= new ImageHeader(fr.getHeader()).createProjection();
           setZoomTo(getInitialZoomLevel());
           plotGroup.addToPlotted();
       } catch (OutOfMemoryError e) {
           freeResources();
           FitsException fitsE= new FitsException("Out Of Memory");
           fitsE.initCause(e);
           throw fitsE;
       }
   }

    public record SaveG2Stuff(Graphics2D g2, Composite composite, AffineTransform trans) { }

}
