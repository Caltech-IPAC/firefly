package edu.caltech.ipac.visualize.plot;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class GridPlot extends Plot {

   private double _ra0;
   private double _ra1;
   private double _dec0;
   private double _dec1;

   private int     _originalWidth;
   private int     _originalHeight;
   private int     _width;
   private int     _height;

   public GridPlot(double ra0, double ra1, double dec0, double dec1) {
      this(ra0,ra1,dec0,dec1,400,400);
   }

   public GridPlot(double ra0, 
                   double ra1, 
                   double dec0, 
                   double dec1,
                   int    pixelWidth,
                   int    pixelHeight) {
      _ra0=  ra0;
      _ra1=  ra1;
      _dec0= dec0;
      _dec1= dec1;
      _width=  _originalWidth=  pixelWidth;
      _height= _originalHeight= pixelHeight;
      setTransform(new AffineTransform(1,0,0,-1,0,_height)); 
   }



   public void paint(PlotPaintEvent ev) {
       Graphics2D g2= ev.getGraphics();
       Rectangle2D back = new Rectangle2D.Double(0,0, 400,400 );
       g2.setPaint(Color.black);
       g2.fill(back);
   }

   public int     getScreenWidth()     { return _width; }
   public int     getScreenHeight()    { return _height;}
   public int     getImageDataWidth()  { return _width; }
   public int     getImageDataHeight() { return _height;}
   public double  getWorldPlotWidth()  { return getWorldWidth(); }
   public double  getWorldPlotHeight() { return getWorldHeight(); }
     
   public double  getWorldWidth() {return _ra1 - _ra0; }     // in degrees
   public double  getWorldHeight(){return _dec1 - _dec0; }   // in degrees

   public Plot makeSharedDataPlot(PlotGroup pg) {
        return new GridPlot(_ra0,_ra1,_dec0,_dec1,_width,_height); 
   }
   public Plot makeSharedDataPlot() {
        return new GridPlot(_ra0,_ra1,_dec0,_dec1,_width,_height); 
   }

   public CoordinateSys getCoordinatesOfPlot() { return CoordinateSys.PIXEL; }

   public boolean pointInPlot(ImageWorkSpacePt ipt) {
       return true;
   }

   public boolean pointInPlot(WorldPt wpt) {
      boolean retval;
       double lon= wpt.getLon();
       double lat= wpt.getLat();
      retval=  (lon >= _ra0  && lon <= _ra1 && 
                lat >= _dec0 && lat <= _dec1 );
      return retval;
   }

    /** 
     * Return the image coordinates given a ra and a dec
     * @param wpt the world pt
     * @return ImagePt the translated coordinates
     */
   public ImageWorkSpacePt getImageCoords(WorldPt wpt) {
      return new ImageWorkSpacePt( getImageX(wpt.getLon()), getImageY(wpt.getLat()) );
   } 

   public double getImageX(double worldX) {
      double a= _originalWidth / (_ra1 - _ra0);
      //double degWidth= (_ra1 - _ra0);
      return -a * (worldX-_ra0) + _originalWidth;
   } 
   public double getImageY(double worldY) {
      double a= _originalHeight / (_dec1 - _dec0);
      return a * (worldY-_dec0);
   } 

    /** 
     * Return the sky coordinates given a image x (fsamp) and  y (fline)
     * @return WorldPt the translated coordinates
     */
   public WorldPt getWorldCoords( ImageWorkSpacePt ipt,
                                  CoordinateSys outputCoordSys) {
       return new WorldPt( getWorldX(ipt.getX()), getWorldY(ipt.getY()) );
   }
    /** 
     * Return the world x (ra) coordinate given a image x (fsamp)
     * @param  fsamp (fsamp)
     * @return double world x (ra)
     */
   public double getWorldX(double fsamp) {
      double a= (_ra1 - _ra0) / _originalWidth;
      return  -a * fsamp + _ra1;
   } 

   public ImagePt getDistanceCoords(ImagePt pt, double x, double y)
                                                throws ProjectionException {
      ImageWorkSpacePt dpt= getImageCoords(new WorldPt(x,y));
      return new ImagePt( pt.getX() + (dpt.getX() - pt.getX()),
                          pt.getY() + (dpt.getY() - pt.getY()) );
   } 

   public ImageWorkSpacePt getDistanceCoords(ImageWorkSpacePt pt, double x, double y)
                                                throws ProjectionException {
      ImageWorkSpacePt dpt= getImageCoords(new WorldPt(x,y));
      return new ImageWorkSpacePt( pt.getX() + (dpt.getX() - pt.getX()),
                          pt.getY() + (dpt.getY() - pt.getY()) );
   } 

    /** 
     * Return the world y (dec) coordinate given a image y (fline)
     * @param fline (fline)
     * @return double world y (dec)
     */
   public double getWorldY(double fline) {
      double a= (_dec1 - _dec0) / _originalHeight;
      return a * fline + _dec0;
   }

    public double getFlux(ImageWorkSpacePt ipt) throws PixelValueException {
       return 0.0;
    }
    public String getFluxUnits() { return "None"; }
    public double getPixelScale(){ return 1.0; }

    public boolean coordsWrap(WorldPt wp1, WorldPt wp2) { return false; }

   public boolean isPlotted() {return true; }

   public void zoom(int dir) {
       AffineTransform trans= getTransform();
       double scaleX= trans.getScaleX();
       double scaleY= trans.getScaleY();
       float zfactor= getZoomFactor();
       if (dir == Plot.UP) {
           _width  *= zfactor;
           _height *= zfactor;
           scaleX*=zfactor;
           scaleY*=zfactor;
       }
       else if (dir == Plot.DOWN) {
           zfactor= 1.0F / zfactor;
           _width  *= .5;
           _height *= .5;
           scaleX*=0.5;
           scaleY*=0.5;
       }
       setTransform(new AffineTransform(scaleX,0,0,scaleY,0,_height)); 
       repair();
   }

   public double getScale() { return getTransform().getScaleX(); }

   public void freeResources() {
        super.freeResources();
       _available = false;
   }
}



