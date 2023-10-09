/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.FitsException;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is the major subclass implementation of Plot.  This class plots fits data.
 * @author Trey Roby
 */
public class ImagePlot implements Serializable {

    public static final int  SQUARE = 1500; // this is the size of the image data tiles

    private final Projection projection;
    private final ImageDataGroup imageData;
    private final CoordinateSys imageCoordSys;
    private final boolean threeColor;
    private final boolean useForMask;
    private final int imageDataWidth;
    private final int imageDataHeight;
    public  final PlotGroup plotGroup;
    private final float zFactor= 1.0F; //todo: figure how how to remove this
    private float initialZoomLevel= 1.0F;
    private String plotDesc;


    public ImagePlot(ActiveFitsReadGroup frGroup, int initColorId) throws FitsException{
        this(frGroup,false,initColorId);
    }

    public ImagePlot(ActiveFitsReadGroup frGroup, boolean threeColor) throws FitsException{
        this(frGroup,threeColor,0);
    }

    private ImagePlot(ActiveFitsReadGroup frGroup, boolean threeColor, int initColorId)  throws FitsException{
        plotGroup = new PlotGroup(this);
        this.threeColor = threeColor;
        useForMask = false;
        var fr= threeColor ? frGroup.getRefFitsRead() : frGroup.getNoBandFitsRead();
        projection = new ImageHeader(fr.getHeader()).createProjection();
        imageCoordSys = fr.getImageCoordinateSystem();
        imageDataWidth = fr.getImageDataWidth();
        imageDataHeight = fr.getImageDataHeight();
        imageData = new ImageDataGroup(imageDataWidth, imageDataHeight,
                this.threeColor ? ImageData.ImageType.TYPE_24_BIT : ImageData.ImageType.TYPE_8_BIT,
                initColorId,FitsRead.getDefaultFutureStretch(),SQUARE);
        configureImage();
    }


    /** 07/20/15 LZ
     * Create a ImagePlot with given IndexColorModel
     */
    public ImagePlot(ActiveFitsReadGroup frGroup, ImageMask[] iMasks)  throws FitsException{
        plotGroup = new PlotGroup(this);
        threeColor = false;
        useForMask = true;
        var fr= frGroup.getNoBandFitsRead();
        projection = new ImageHeader(fr.getHeader()).createProjection();
        imageCoordSys = fr.getImageCoordinateSystem();
        imageDataWidth = fr.getImageDataWidth();
        imageDataHeight = fr.getImageDataHeight();
        imageData = new ImageDataGroup(imageDataWidth, imageDataHeight, iMasks,FitsRead.getDefaultFutureStretch(),SQUARE);
        configureImage();
    }

    public boolean isUseForMask() { return useForMask; }
    public PlotGroup getPlotGroup() { return plotGroup; }
    public void   setPlotDesc(String d) { plotDesc = d; }
    public String toString() { return getPlotDesc(); }
    public String getPlotDesc()         { return plotDesc; }
    /**
     * Get the transform this plot uses
     * @return AffineTransform the transform
     */
    public AffineTransform getTransform() { return (AffineTransform ) plotGroup.getTransform().clone(); }

    /**
     * Get the factor that is plot will be zoomed on the next Zoom call
     * in other words, zoom factor of 3 means that it is zoomed 3x<br>
     *      zoom factor of 2 means that it is zoomed 2x
     * @return float the zoom factor.
     */
    public float getZoomFactor()              { return zFactor; }

    /**
     * Set the level an image will be zoom when it is plotted
     * @param  initialZoomLevel the initial zoom level
     */
    public void setInitialZoomLevel(float initialZoomLevel) { this.initialZoomLevel= initialZoomLevel; }
    public float getInitialZoomLevel() { return initialZoomLevel; }


    /**
     * get the coordinate system of the plot.
     * @return  CoordinateSys  the coordinate system.
     */
   public CoordinateSys getCoordinatesOfPlot() { return imageCoordSys; }




    private static SaveG2Stuff prePaint(Graphics2D g2) {
        AffineTransform trans= g2.getTransform();
        AffineTransform saveTrans= (AffineTransform)trans.clone();
        trans.scale(1, 1);
        g2.setTransform(trans);
        Composite savComposite= g2.getComposite();
        Composite workComposite= AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 1);
        g2.setComposite(workComposite);
        return new SaveG2Stuff(g2,savComposite,saveTrans);
    }

    private static void postPaint(SaveG2Stuff saveStuff) {
        Graphics2D g2= saveStuff.g2();
        g2.setComposite(saveStuff.composite());
        g2.setTransform(saveStuff.trans());
    }

    public void preProcessImageTiles(final ActiveFitsReadGroup frGroup) {
        if (imageData.isUpToDate()) return;
        synchronized (this) {
            if (imageData.isUpToDate()) return;
            int coreCnt= ServerContext.getParallelProcessingCoreCnt();
            if (imageData.size()<4 || coreCnt==1) {
                for(ImageData id : imageData)  id.getImage(frGroup.getFitsReadAry());
            }
            else {
                ExecutorService executor = Executors.newFixedThreadPool(coreCnt);
                for(ImageData id : imageData)  {
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
        SaveG2Stuff  saveStuff= prePaint(g2);
        AffineTransform trans= g2.getTransform();
        float zfact= plotGroup.getZoomFact();
        for(ImageData id : imageData) {
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
     * Return the image coordinates given a WorldPt class
     * @param wpt the class containing the point in sky coordinates
     * @return ImageWorkSpacePt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into an ImagePt
     */
    public ImageWorkSpacePt getImageCoords( WorldPt wpt) throws ProjectionException {
        if (wpt.getCoordSys().equals(CoordinateSys.SCREEN_PIXEL)) {
            try {
                return getImageWorkSpaceCoords(new Point2D.Double(wpt.getX(),wpt.getY()));
            } catch (NoninvertibleTransformException e) {
                ProjectionException pe= new ProjectionException("Could not covert screen to image");
                pe.initCause(e);
                throw pe;
            }
        }
        else if (wpt.getCoordSys().equals(CoordinateSys.PIXEL)) {
            return new ImageWorkSpacePt(wpt.getX(),wpt.getY());
        }
        else {
            if (!imageCoordSys.equals(wpt.getCoordSys())) {
                wpt= VisUtil.convert(wpt, imageCoordSys);
            }
            ProjectionPt proj_pt= projection.getImageCoords(wpt.getLon(),wpt.getLat());
            return new ImageWorkSpacePt( proj_pt.getX() + 0.5F ,  proj_pt.getY() + 0.5F);
        }
    }

    public ImageWorkSpacePt getImageWorkSpaceCoords(Point2D pt)
                                  throws NoninvertibleTransformException {
        AffineTransform inverse= plotGroup.getInverseTransform();
        ImageWorkSpacePt retval;
        if (inverse != null) {
            Point2D input  = new Point2D.Double(pt.getX(), pt.getY());
            Point2D imagePt= inverse.transform(input, null);
	    retval= new ImageWorkSpacePt(imagePt.getX(), imagePt.getY() );
        }
        else {
            throw new NoninvertibleTransformException("no inverse transform");
        }
        return retval;
    }

    /**
     * Return the J2000 sky coordinates given an image x (fsamp) and  y (fline)
     * package in a ImagePt class
     * @param pt the ImageWorkSpacePt
     * @return WorldPt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into an WorldPt
     */
    public WorldPt getWorldCoords(ImageWorkSpacePt pt) throws ProjectionException {
        return getWorldCoords(pt, CoordinateSys.EQ_J2000);
    }

    /**
     * Return the screen coordinates given WorldPt
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
     * Return the screen coordinates given ImageWorkSpacePt
     * @param ipt the ImageWorkSpace point to translate
     * @return Point2D the screen coordinates
     */
    public Point2D getScreenCoords(ImageWorkSpacePt ipt) {
        return  plotGroup.getTransform().transform( new Point2D.Double(ipt.getX(), ipt.getY()), null);
    }


    /**
     * Return the world coordinates given screen x & y.
     * @param pt the screen coordinates to convert to world coordinates
     * @return WorldPt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into a WorldPt
     * @throws NoninvertibleTransformException if this point can be transformed in the
     *                                conversion process: almost never happens
     */
    public WorldPt getWorldCoords(Point2D pt) throws NoninvertibleTransformException, ProjectionException {
        return getWorldCoords(getImageWorkSpaceCoords(pt));
    }


    /** 
     * Return a point the represents the passed point with a distance in
     * World coordinates added to it.
     * @param pt the x and y coordinate
     * @param x the x distance away from the point in world coordinates
     * @param y the y distance away from the point in world coordinates
     * @return ImagePt the new point
     */
    public ImagePt getDistanceCoords(ImagePt pt, double x, double y) {
        return projection.getDistanceCoords(pt,x,y);
    }

    /** 
     * Return a point the represents the passed point with a distance in
     * World coordinates added to it.
     * @param pt the x and y coordinate
     * @param x the x distance away from the point in world coordinates
     * @param y the y distance away from the point in world coordinates
     * @return ImagePt the new point
     */
    public ImageWorkSpacePt getDistanceCoords(ImageWorkSpacePt pt, double x, double y) {
        return projection.getDistanceCoords(pt,x,y);
    }


    /**
     * Determine if a world point is in the plot boundaries.
     * @param wpt the world point to test
     * @return boolean true if it is in the boundaries, false if not.
     */
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
     * Determine if an image point is in the plot boundaries.
     * @param ipt image point to test
     * @return boolean true if it is in the boundaries, false if not.
     */
    public boolean pointInPlot( ImageWorkSpacePt ipt) {
       return getPlotGroup().pointInPlot(ipt);
    }

    /**
     * Return the sky coordinates given an image x (fsamp) and  y (fline)
     * package in an ImagePt class
     * @param ipt  the image point
     * @param outputCoordSys The coordinate system to return
     * @return WorldPt the translated coordinates
     * @throws ProjectionException if the point cannot be projected into an WorldPt
     */
   public WorldPt getWorldCoords( ImageWorkSpacePt ipt, CoordinateSys outputCoordSys)
                                               throws ProjectionException {

        double x= ipt.getX();
        double y= ipt.getY();
        WorldPt wpt= projection.getWorldCoords(x - .5F ,y - .5F );
        if (!outputCoordSys.equals(wpt.getCoordSys())) {
            wpt= VisUtil.convert(wpt, outputCoordSys);
        }
        return wpt;
   }

    /**
     * get the scale (usually in arcseconds) that on image pixel of data
     * represents.
     * @return double the scale of one pixel.
     */
    public double getPixelScale(){ return projection.getPixelScaleArcSec(); }

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

    public ImageDataGroup getImageData() { return imageData; }

    //======================
   public boolean coordsWrap(WorldPt wp1, WorldPt wp2) {
         boolean retval= false;
         if (projection.isWrappingProjection()) {
            try { 
                double worldDist= computeDistance(wp1,wp2);
                double pix= projection.getPixelWidthDegree();
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

     if (Math.abs(cosine) > 1.0) cosine = cosine/Math.abs(cosine);
	 return RtoD*Math.acos(cosine);
   }


//======================

   private void configureImage() throws FitsException {
       try {
           PlotGroup plotGroup= getPlotGroup();
           setZoomTo(getInitialZoomLevel());
           plotGroup.addToPlotted();
       } catch (OutOfMemoryError e) {
           throw new FitsException("Out Of Memory");
       }
   }

    public record SaveG2Stuff(Graphics2D g2, Composite composite, AffineTransform trans) { }

}
