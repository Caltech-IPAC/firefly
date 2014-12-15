package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.astro.conv.CoordConv;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayFuncs;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * This is the major subclass implementation of Plot.  This class plots 
 * fits data.
 *
 * @author Trey Roby
 */
public class ImagePlot extends Plot implements Serializable {

    public static final int  SQUARE = 1500; // this is the size of the image data tiles

    protected Projection     _projection;
    protected ImageDataGroup _imageData;
    protected FitsRead       _fitsRead;
    protected boolean        _isPlotted    = false;
    protected CoordinateSys  _imageCoordSys= CoordinateSys.UNDEFINED;
    protected ImagePt        _minPt;
    protected ImagePt        _maxPt;


    public static final int RED= ImageData.RED;
    public static final int GREEN= ImageData.GREEN;
    public static final int BLUE= ImageData.BLUE;
    public static final int NO_BAND= -1;

    private File _fitsFile;
    private boolean _freeImagesAfterCreation= false;
    private FitsRead _threeColorFitsRead[]= new FitsRead[3];
    private boolean  _threeColor;
    private boolean  _makeSharedDataPlot = false;


   public ImagePlot() { }

   public ImagePlot(PlotGroup plotGroup) { super(plotGroup); }

   public ImagePlot(PlotGroup plotGroup,
                    FitsRead  fitsRead,
                    float     initialZoomLevel)  throws FitsException{
       this(plotGroup,fitsRead, initialZoomLevel,false);
   }


    public ImagePlot(PlotGroup plotGroup,
                     FitsRead  fitsRead,
                     float     initialZoomLevel,
                     boolean   threeColor)  throws FitsException{

        this(plotGroup,fitsRead,initialZoomLevel,threeColor,0,
             FitsRead.getDefaultFutureStretch(),true);
    }




    public ImagePlot(PlotGroup plotGroup,
                     FitsRead  fitsRead,
                     float     initialZoomLevel,
                     boolean   threeColor,
                     int       initColorID,
                     RangeValues stretch,
                     boolean     constructNow)  throws FitsException{
        super(plotGroup);
        setInitialZoomLevel(initialZoomLevel);
        _fitsRead= fitsRead;
        _threeColor= threeColor;
        _freeImagesAfterCreation= !constructNow;
        if (_threeColor) {
            _imageData = new ImageDataGroup(fitsRead,  ImageData.ImageType.TYPE_24_BIT,
                                            initColorID,stretch,SQUARE, constructNow);
            configureThreeColor();
        }
        else {
            _imageData = new ImageDataGroup(fitsRead,  ImageData.ImageType.TYPE_8_BIT,
                                            initColorID,stretch,SQUARE, constructNow);
        }
        configureImage();
    }


    public int getDefTileSize() {  return SQUARE; }

    public void setFreeImagesAfterCreation(boolean free) {
        _freeImagesAfterCreation= free;
    }

    public boolean isThreeColor() { return _threeColor; }

    private void configureThreeColor() {
        _threeColorFitsRead[RED]= _fitsRead;
    }
    
    public void addThreeColorBand(FitsRead colorBandFitsRead, int band)
                                   throws GeomException, 
                                          FitsException,
                                          IOException {
        threeColorOK(band);
        ImagePlot basePlot= (ImagePlot)getPlotGroup().getBasePlot();

        //if (needToReproject(colorBandFitsRead, basePlot._fitsRead)) {
        if (basePlot._fitsRead.isSameProjection(colorBandFitsRead)) {
            _threeColorFitsRead[band]=colorBandFitsRead;
        }
        else {
            _threeColorFitsRead[band]=FitsRead.createFitsReadWithGeom(
                           colorBandFitsRead, basePlot._fitsRead, false);
        }
        _imageData.setFitsRead(_threeColorFitsRead[band], band);
        getPlotGroup().fireStatusChanged(PlotGroup.ChangeType.BAND_ADDED,
                                         this, band);
    }

    public void setRefFitsFile(File f) {
        _fitsFile= f;
    }

    public File getRefFittsFile() { return _fitsFile; }

    public void removeThreeColorBand(int band) {
        threeColorOK(band);
        _threeColorFitsRead[band]= null;
        _imageData.setFitsRead(null, band);
        getPlotGroup().fireStatusChanged(PlotGroup.ChangeType.BAND_REMOVED,
                                         this, band);
    }

    public void setThreeColorBand(FitsRead colorBandFitsRead, int band) {
        int oldIdx= -1;
        for(int i=0; i<_threeColorFitsRead.length && oldIdx==-1; i++) {
            if (_threeColorFitsRead[i]==colorBandFitsRead) oldIdx= i;
        }
        Assert.argTst(oldIdx>-1, "You must add this FitsRead object using "+
                                   "addThreeColorBand() before setting it " +
                                   "to another color");
        _imageData.setFitsRead(colorBandFitsRead, band);

        _threeColorFitsRead[oldIdx]= _threeColorFitsRead[band];
        _threeColorFitsRead[band]= colorBandFitsRead;


        _imageData.setFitsRead(_threeColorFitsRead[oldIdx], oldIdx);
        _imageData.setFitsRead(_threeColorFitsRead[band], band);

        if (_threeColorFitsRead[oldIdx]!=null) {
            getPlotGroup().fireStatusChanged(
                           PlotGroup.ChangeType.BAND_ADDED, this, oldIdx);
        }
        if (_threeColorFitsRead[band]!=null) {
            getPlotGroup().fireStatusChanged(
                           PlotGroup.ChangeType.BAND_ADDED, this, band);
        }
    }

    public Projection getProjection() { return _projection; }

    public boolean isColorBandInUse(int band) {
        threeColorOK(band);
        return (_threeColorFitsRead[band]!=null);
    }

    public boolean isColorBandVisible(int band) {
        threeColorOK(band);
        return ((_threeColorFitsRead[band]!=null) &&
                (_imageData.getFitsRead(band)!=null) );
    }

    public void clearThreeColorBand(int band) {
        threeColorOK(band);
        _imageData.setFitsRead(null, band);
        _threeColorFitsRead[band]= null;
    }


    public void setThreeColorBandVisible(int band, boolean visible) {
        threeColorOK(band);
        if (visible) {
            if (_imageData.getFitsRead(band)==null) {
                _imageData.setFitsRead(_threeColorFitsRead[band], band);
                getPlotGroup().fireStatusChanged(PlotGroup.ChangeType.BAND_SHOWING, this, band);
            }
        }
        else {
            if (_imageData.getFitsRead(band)!=null) {
                _imageData.setFitsRead(null, band);
                getPlotGroup().fireStatusChanged(PlotGroup.ChangeType.BAND_HIDDEN, this, band);
            }
        }
    }

    public void releaseImage() {
        _imageData.releaseImage();
    }

    public long getDataSize() {
        long retval= 0;
        if (_threeColor) {
            for(FitsRead fr : _threeColorFitsRead) {
                if (fr!=null)  retval+= fr.getDataSize();
            }
        }
        else {
            retval= (_fitsRead!=null) ? _fitsRead.getDataSize() : 0;
        }
        return retval;
    }


   /**
    * Writes a FITS file with (possible) FITS extension images 
    *
    * @param stream Output File Stream
    * @param fitsRead of FitsRead objects
    * @throws FitsException if problem is fits related
    * @throws IOException  if problem is in writting the file
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


   public void writeFile(OutputStream stream) throws FitsException {
      Fits myFits = _fitsRead.getFits();

       //TODO: figure out why this next block is necessary and what the comments mean - Trey
      /* The next several lines make sure that FITS stuff is in memory */
      BasicHDU[] myHDUs = myFits.read();
       for (BasicHDU myHDU : myHDUs) {
           //myHDUs[i].info();

           try {
               Data myData = myHDU.getData();
               if (myData.getData() != null) {
                   String description =
                           ArrayFuncs.arrayDescription(myData.getData());
               }
           }
           catch (Exception e) {
               //System.out.println("      Unable to get data");
           }
       }
      /* OK, it's in memory - now write it out */
      myFits.write(new DataOutputStream(stream));
   }


    /**
     * get the coordinate system of the plot.
     * @return  CoordinateSys  the coordinate system.
     */
   public CoordinateSys getCoordinatesOfPlot() { return _imageCoordSys; }


    public void paint(PlotPaintEvent ev) {
        Graphics2D g2= ev.getGraphics();
        if (shouldPaint()) {
            int imageScaleFactor= _fitsRead.getImageScaleFactor();
            SaveG2Stuff  saveStuff= prePaint(g2,_fitsRead.getImageScaleFactor(), getPercentOpaque());
            for(ImageData id : _imageData) {
                g2.drawImage(id.getImage(),
                             Math.round(getOffsetX()/imageScaleFactor) + id.getX(),
                             Math.round(getOffsetY()/imageScaleFactor) + id.getY(),
                             getPlotGroup().getPlotView());
            }

            postPaint(saveStuff);
        }
    }



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

    public void paintTile(Graphics2D g2, int x, int y, int width, int height) {
        int imageScaleFactor= _fitsRead.getImageScaleFactor();
        SaveG2Stuff  saveStuff= prePaint(g2, _fitsRead.getImageScaleFactor(), getPercentOpaque());
        AffineTransform trans= g2.getTransform();
        float zfact= _plotGroup.getZoomFact();
        for(ImageData id : _imageData) {
            if (intersect(trans, x,y,width,height,id)) {
                int drawX= (int)((float)getOffsetX()/(float)imageScaleFactor + id.getX() - x/zfact);
                int drawY= (int)((float)getOffsetY()/(float)imageScaleFactor + id.getY() + y/zfact);
                g2.drawImage(id.getImage(), drawX, drawY, null);
                if (_freeImagesAfterCreation) id.releaseImage();
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
        PlotView pv= getPlotGroup().getPlotView();
        boolean overlayOK= true;
        if (pv!=null) overlayOK= (pv.getPrimaryPlot()==getPlotGroup().getBasePlot());
        return isShowing() && _isPlotted && overlayOK;
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
      return _imageData.getImageWidth() * _fitsRead.getImageScaleFactor();
   }

    /**
     * This method will return the height of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return the height of the image data
     */
   public int     getImageDataHeight(){ 
      return _imageData.getImageHeight() * _fitsRead.getImageScaleFactor();
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
        double imageScaleFactor= _fitsRead.getImageScaleFactor();
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
        //double imageScaleFactor= _fitsRead.getImageScaleFactor();
        ImageWorkSpacePt retval;
        if (inverse != null) {
            Point2D input  = new Point2D.Double(pt.getX(), pt.getY());
            Point2D imagePt= inverse.transform(input, null);
	    retval= new ImageWorkSpacePt(imagePt.getX(), imagePt.getY() );
	    /*
		System.out.println(
		"RBH ZZZ1 ImagePlot.getImageCoords:  image = " +
		_fitsRead.getSourceDec() +
		" input = " + input.getX() + " " + input.getY() + 
		"  imagePt = " + imagePt.getX() + " " + imagePt.getY() +
		"  retval =  " + retval.getX() + " " + retval.getY());
	    System.out.println("    getScreenCoords() = " +
		getScreenCoords(new ImageWorkSpacePt(imagePt.getX(), imagePt.getY())));
	    */
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
       int imageScaleFactor= _fitsRead.getImageScaleFactor();
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
       int imageScaleFactor= _fitsRead.getImageScaleFactor();
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
        int imageScaleFactor= _fitsRead.getImageScaleFactor();

	/*
	System.out.println(
	    "RBH ImagePlot.getScreenCoords FIRST  imageScaleFactor = " + 
	    imageScaleFactor + 
	    "  getOffsetX() = " + getOffsetX() +
	    "  getOffsetY() = " + getOffsetY() +
	    "  ipt.getX() = " + ipt.getX() + 
	    "  ipt.getY() = " + ipt.getY());
	*/
	//Thread.currentThread().dumpStack();

        //trans.scale(imageScaleFactor, imageScaleFactor);

	double x = ipt.getX() * imageScaleFactor + (double)getOffsetX();
	double y = ipt.getY() * imageScaleFactor + (double)getOffsetY();
	ipt = new ImagePt(x, y);

        return trans.transform(  new Point2D.Double(ipt.getX(), ipt.getY()),
                                 null);
    }


//----------------------------
//----------------------------
//----------------------------
//----------------------------
//----------------------------

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
     * Determine if a world point is in the plot bounderies.
     * @param wpt the world point to test
     * @return boolean true if it is in the bounderies, false if not.
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
     * Determine if a image point is in the plot bounderies.
     * @param ipt image point to test
     * @return boolean true if it is in the bounderies, false if not.
     */
    @Override
    public boolean pointInPlot( ImageWorkSpacePt ipt) {
       return getPlotGroup().pointInPlot(ipt);
    }

    /**
     * Return the sky coordinates given a image x (fsamp) and  y (fline)
     * package in a ImagePt class
     * @param ipt  the image point
     * @param outputCoordSys The coordiate system to return
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

    public double getFlux(ImageWorkSpacePt iwspt) throws PixelValueException {

        return  getFluxFromFitsRead(_fitsRead, iwspt);
    }



    /**
     * get the flux of a given image point point on the plot.
     * @param band the three color band to get the flux for
     * @param iwspt the image pt
     * @return double the flux value
     * @throws PixelValueException if the pixel value is not on the image
     */

    public double getFlux(int band, ImageWorkSpacePt iwspt) throws PixelValueException {
        double retval;
        if (band==NO_BAND && !_threeColor) {
            retval= getFlux(iwspt);
        }
        else {
            threeColorOK(band);
            retval= getFluxFromFitsRead(_threeColorFitsRead[band], iwspt);

        }
        return  retval;
    }

    /**
     * get three color band source description
     * @param band the three color band to get source description for
     * @return String source description
     */
    public String getPlotDesc(int band) {
        threeColorOK(band);
        return getImageData().getFitsRead(band).getSourceDec();
    }

    
    private void acceptFitsRead(FitsRead fr) {
        Assert.argTst(fr==null      ||
                      fr==_fitsRead ||
                      fr==_threeColorFitsRead[RED] ||
                      fr==_threeColorFitsRead[GREEN] ||
                      fr==_threeColorFitsRead[BLUE],
                      "You must pass a FitsRead that you have register via "+
                      "the constructor or addThreeColorBand()");
    }


    private void threeColorOK(int band) {
        Assert.tst(_threeColor,
                   "Must be in three color mode to use this routine");
        Assert.argTst( (band==RED || band==GREEN || band==BLUE),
                       "band must be RED, GREEN, or BLUE");
    }


    /**
     * get the flux of a given image point point on the plot.
     * @param fr the FitsRead to get the flux from
     * @param sipt the point you want the flux for
     * @return double the flux value
     * @throws PixelValueException if the pixel value is not on the image
     */

   private double getFluxFromFitsRead(FitsRead fr, ImageWorkSpacePt sipt)
                   throws PixelValueException {
       acceptFitsRead(fr);
       double retval= Double.NaN;
       if (fr!=null) {
           int imageScaleFactor= fr.getImageScaleFactor();
           double xpass= (sipt.getX()- ((double)getOffsetX()))/imageScaleFactor;
           double ypass= (sipt.getY()- ((double)getOffsetY()))/ imageScaleFactor;
	   ImagePt ipt = new ImagePt(xpass, ypass);

           retval=  fr.getFlux(ipt);
       }
       return retval;
   }

   private String getFluxUnitsFromFitsRead(FitsRead fr) {
       acceptFitsRead(fr);
       String retval= null;
       if (fr!=null)  retval= fr.getFluxUnits();
       return retval;
   }

    /**
     * get units that this flux data is in.
     * @return String the units.
     */
   public String getFluxUnits() {  return getFluxUnits(NO_BAND);}

    /**
     * get units that this flux data is in.
     * @param band the band to get the flux unit for
     * @return String the units.
     */
    public String getFluxUnits(int band) {
        String unitsStr;
        if (band==NO_BAND) {
            unitsStr= getFluxUnitsFromFitsRead(_fitsRead);
        }
        else {
            threeColorOK(band);
            unitsStr= getFluxUnitsFromFitsRead(_threeColorFitsRead[band]);
        }
        return unitsStr;
    }

    /**
     * get the scale (usaully in arcseconds) that on image pixel of data
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
     * zoom this plot
     * @param dir the zoom direction.  Must be Plot.UP or Plot.DOWN.
     */
   public void zoom(int dir) { getPlotGroup().zoom(dir); }

   /**
    * zoom to an exact Size
    * @param level the new zoom level
    */
   public void setZoomTo(float level) { getPlotGroup().setZoomTo(level); }

   public boolean isProjectionImplemented() {
      return (_projection != null) && _projection.isImplemented();
   }

   public boolean isProjectionSpecified() {
      return (_projection != null) && _projection.isSpecified();
   }

    /**
     * return the factor this plot is scaled to (in other words, how much or little
     * the plot is zoomed).
     * @return the scale factor
     */
   public double getScale() { 
        return getPlotGroup().getTransform().getScaleX();
   }

    /**
     * specificly release any reasources held by this object
     */
   public void freeResources() {
       if (_isPlotted) {
           if (_imageData!=null) {
               if (_makeSharedDataPlot)
                    _imageData = null;
               else
                    _imageData.freeResources();
           }


           if (!_makeSharedDataPlot && _fitsRead!=null) _fitsRead.freeResources();
           _fitsRead= null;
           if (_threeColorFitsRead!=null) {
               for(int i= 0; (i<_threeColorFitsRead.length);i++) {
                   if (!_makeSharedDataPlot && _threeColorFitsRead[i]!=null) {
                       _threeColorFitsRead[i].freeResources();
                   }
                   _threeColorFitsRead[i]= null;
               }
               _threeColorFitsRead= null;
           }

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
   public Plot makeSharedDataPlot() {
        return makeSharedDataPlot(null);
   }

    /**
     * This method works like a clone except is makes a plot so it shares
     * some of the objects.  The result of the class will produce a plot that
     * shares the ImageData.  If the ImageData get changed both plots will
     * change. 
     * @return Plot a new plot that shares image data.
     */
   public Plot makeSharedDataPlot(PlotGroup plotGroup) {
        ImagePlot p         = new ImagePlot(plotGroup); 
        p._projection       = _projection;
        p._imageData        = _imageData;
        p._fitsRead         = _fitsRead;
        p._isPlotted        = _isPlotted;
        p._available        = _available;
        p._imageCoordSys    = _imageCoordSys;
        p._minPt            = _minPt;
        p._maxPt            = _maxPt;
        p._threeColor       = _threeColor;
        p._attributes       = _attributes;
        if (_threeColor) {
            System.arraycopy(_threeColorFitsRead,0,
                             p._threeColorFitsRead,0,
                             _threeColorFitsRead.length);
//            for(int i=0; i<_threeColorFitsRead.length; i++) {
//                p._threeColorFitsRead[i]= _threeColorFitsRead[i];
//            }
        }
        p.setZoomTo(        1.0F );
        if (plotGroup != null) p.computeOffsetXY();
        p.getPlotGroup().addToPlotted();
        p.setPlotDesc(      getPlotDesc() );
        p.setShortPlotDesc( getShortPlotDesc() );
        p._makeSharedDataPlot = true;
        return p;
   }
    
//======================

    public ImageDataGroup getImageData() { return _imageData; }

    public FitsRead getFitsRead() { return _fitsRead; }


    public HistogramOps getHistogramOps() {
        return getHistogramOps(NO_BAND);
    }

    public HistogramOps getHistogramOps(int band) {
        HistogramOps retval;
        Assert.argTst(band==NO_BAND || _threeColorFitsRead[band]!=null,
                      "You have not set a fits read for the passed band");
        if (band==NO_BAND) {
            retval= new HistogramOps(_fitsRead, _imageData);
        }
        else {
            threeColorOK(band);
            retval= new HistogramOps(_threeColorFitsRead[band], _imageData);
        }
        return retval;
    }

    public static void setDefaultFutureStretch(RangeValues rangeValues) {
        FitsRead.setDefaultFutureStretch(rangeValues);
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

                //System.out.println("worldDist / pix="+value1 + 
                //                   "    imageDist="+ imageDist);
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

   private void configureImage() throws FitsException {
      try {

         PlotGroup plotGroup= getPlotGroup();
         if (!plotGroup.isOverlayEnabled() && !plotGroup.isBasePlot(this)) {
            throw new FitsException(
                "You may not overlay this image over an image that does " +
                "not contain projection information");
         }
         ImageHeader imageHeader= _fitsRead.getImageHeader();
         _isPlotted= true;
         computeMinMaxPoint();
         try {
             determineCoordSys();
             _projection= imageHeader.createProjection(_imageCoordSys );
             if (plotGroup.isBasePlot(this)) {
                 setZoomTo(getInitialZoomLevel());
             }
             else {
                 computeOffsetXY();
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

   private void computeOffsetXY() {
         ImagePlot p= (ImagePlot)getPlotGroup().getBasePlot();
         ImageHeader baseHDR= p._fitsRead.getImageHeader();
         ImageHeader thisHDR= _fitsRead.getImageHeader();

         int imageScaleFactor= _fitsRead.getImageScaleFactor();



	 /* subtract 0.5 from crpix to put it in ImagePt coordinates */
         setOffsetX( (int) Math.round((baseHDR.crpix1-0.5) - 
	     (thisHDR.crpix1-0.5)*imageScaleFactor ));
         setOffsetY( (int) Math.round((baseHDR.crpix2-0.5) - 
	     (thisHDR.crpix2-0.5)*imageScaleFactor ));
   }

   private void determineCoordSys() throws FitsException {
       ImageHeader   hdr=  _fitsRead.getImageHeader();
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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
