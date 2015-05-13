/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.PlotViewStatusEvent;
import edu.caltech.ipac.visualize.plot.PlotViewStatusListener;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class draw a set of shapes (that typically represent a focal plane) 
 * on a image.
 *
 * @author Trey Roby
 * @version $Id: ScalableObject.java,v 1.13 2011/09/01 16:53:08 roby Exp $
 *
 */
public class ScalableObject implements PlotViewStatusListener {


    private static final boolean SPITZER_STYLE_ROTATION=
                            AppProperties.getBooleanProperty("ScalableObject.spitzerStyleRotation.Selected", true);
    private ShapeInfo _worldFocalPlane[];
    private Map<Plot,CachePtInfo>  _plotMap= new HashMap<Plot,CachePtInfo>(20);

    /**
     * Constructor
     * @param worldFocalPlane ShapeInfo[] an array of shape information in world coordinates
     */
    public ScalableObject(ShapeInfo worldFocalPlane[]) {
       _worldFocalPlane= copyWF(worldFocalPlane);
    }    

    /**
     * Set an array of Shapes to draw on the image.
     * @param worldFocalPlane ShapeInfo[] an array of shape information in world coordinates
     */
    public void setWorldFocalPlane(ShapeInfo worldFocalPlane[]) {
       _worldFocalPlane= copyWF(worldFocalPlane);
        for(CachePtInfo cpInfo: _plotMap.values()) {
          cpInfo._cachePt= null;
       }
    }    


    /**
     * Compute the shape in image coordinates and draw it on the image.
     * @param p Plot the plot image to draw on
     * @param g2 Graphics2D we all know what that is
     * @param wpt Plot.Worldpt the point on the image plot to draw the shape
     * @return DrawOnPlotReturn the object contains the bounding box info and
     *                          CachePtInfo
     */
//    public DrawOnPlotReturn drawOnPlot(Plot p, Graphics2D g2, WorldPt wpt) {
//       return drawOnPlot(p, g2, wpt, new RotationInfo(0.0F, wpt),
//                         null,null);
//    }

    /**
     * Compute the shape in image coordinates and draw it on the image.
     * @param p Plot the plot image to draw on
     * @param g2 Graphics2D we all know what that is
     * @param wpt Plot.Worldpt the point on the image plot to draw the shape
     * @param rotation RotationInfo rotation how the shape is rotated
     * @param offset WorldPt how much to offset the shape drawing point
     * @return DrawOnPlotReturn the object contains the bounding box info and
     *                          CachePtInfo
     */
//    public DrawOnPlotReturn drawOnPlot(Plot         p,
//                                       Graphics2D   g2,
//                                       WorldPt      wpt,
//                                       RotationInfo rotation,
//                                       WorldPt      offset) {
//       return drawOnPlot(p,g2,wpt,null,rotation,offset,null);
//    }

    /**
     * Compute the shape in image coordinates and draw it on the image.
     * @param plot the plot image to draw on
     * @param g2 Graphics2D we all know what that is
     * @param wpt Plot.Worldpt the point on the image plot to draw the shape
     * @param rotation RotationInfo - how the shape is rotated
     * @param offset WorldPt - how much to offset the shape drawing point
     * @param overrideCPInfo this object saves the lowlevel computations
                             of the last point it drew on a plot.  If you
                             want to use other cache info then pass the object
     * @return DrawOnPlotReturn the object contains the bounding box info and
     *                          CachePtInfo
     */

    public DrawOnPlotReturn drawOnPlot(Plot         plot,
                                       ActiveFitsReadGroup frGroup,
                                       Graphics2D   g2,
                                       WorldPt      wpt,
                                       WorldPt      wpt2,
                                       RotationInfo rotation,
                                       WorldPt      offset,
                                       CachePtInfo  overrideCPInfo,
                                       boolean      enableDraw) {
        DrawOnPlotReturn retval;
        g2.setPaint(Color.green);   // set any default color
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON  );
        // draw each shape in the list with its specified color
        // scale down the stroke size of it does not make thick lines
        BasicStroke stroke= new BasicStroke();
        stroke= new BasicStroke( stroke.getLineWidth()/ (float)plot.getScale());
        g2.setStroke( stroke);

        if (wpt2==null) {
            retval=   drawPoint(plot,frGroup,g2,wpt,rotation,offset,
                                overrideCPInfo,enableDraw);
        }
        else {
            retval=   drawScan(plot,frGroup,g2,wpt,wpt2,rotation,offset,
                               overrideCPInfo,enableDraw);
        }
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        return retval;
    }

    private DrawOnPlotReturn drawPoint(Plot         plot,
                                       ActiveFitsReadGroup frGroup,
                                       Graphics2D   g2,
                                       WorldPt      wpt,
                                       RotationInfo rotation,
                                       WorldPt      offset,
                                       CachePtInfo  overrideCPInfo,
                                       boolean      enableDraw) {
       Shape   outShape[];
       int     i;
       boolean insideClip= false;
       ImageWorkSpacePt pt;
       CachePtInfo     cpInfo= null;
       AffineTransform savTran      = g2.getTransform();
       AffineTransform trans        = g2.getTransform();
       float           screenRotation;
       boolean         usingCachPt= false;

       try {
          cpInfo= (overrideCPInfo==null) ? _plotMap.get(plot) :  overrideCPInfo;
          if (canUseCache(cpInfo,wpt,null)) { // if in cache, use the shape
             outShape= cpInfo._cacheShape;
             pt= cpInfo._cachePt;
             usingCachPt= true;
          }
          else {
                  // compute the shapes using the shape in world Coordinates
             pt= plot.getImageCoords(wpt);
             outShape= new Shape[_worldFocalPlane.length];
             for(i=0; (i<outShape.length); i++) {
		        ImagePt ipt = new ImagePt(pt.getX(), pt.getY());
                outShape[i]= getImageShape(plot, _worldFocalPlane[i].getShape(),
                                           ipt);
             }
             cpInfo._cachePt      = pt;
             cpInfo._cacheWorldPt = wpt;
             cpInfo._cachePt2     = null;
             cpInfo._cacheWorldPt2= null;
             cpInfo._cacheShape= outShape;
             //System.out.println("outshape: recompute");
          }

           RotationInfo rotationToUse= findRotationToUse(rotation, wpt);

          if (usingCachPt             &&
              rotationToUse   != null &&
              cpInfo._rotation!= null &&
              rotationToUse.equals(cpInfo._rotation) ) {
                  screenRotation= cpInfo._screenRotation;
          }
          else {
                  screenRotation= computeScreenRotation(plot, frGroup, rotationToUse);
                  cpInfo._screenRotation= screenRotation;
                  cpInfo._rotation      = rotationToUse;
          }


          trans.rotate(Math.PI * (screenRotation/180), pt.getX(), pt.getY() );

                       // set the offset of the shape
          if (offset != null) {
             ImageWorkSpacePt ip= plot.getDistanceCoords( pt,
                                     offset.getX(), offset.getY());
             trans.translate(ip.getX()-pt.getX(),ip.getY()-pt.getY());
          }

           Rectangle testR= enableDraw ? null : new Rectangle(0,0,plot.getScreenWidth(),plot.getScreenHeight());
           g2.setTransform(trans);
           for(i=0; (i<outShape.length); i++) {
               Rectangle r= outShape[i].getBounds();
               if (enableDraw) {
                   if (g2.hitClip( r.x, r.y, r.width, r.height)) {
                       insideClip= true;
                       g2.setPaint(_worldFocalPlane[i].getColor() );
                       g2.draw(outShape[i]);
                   }
               }
               else {
                   if (g2.hit(testR,r,true)) insideClip= true;

               }
           }
               // restore the previous transform and turn off Anti-Aliasing
          g2.setTransform(savTran);
       } catch (ProjectionException e) {
           System.out.println("ScalableObject.drawOnPlot: "+ e);
       }
       CachePtInfo retPI= new CachePtInfo(cpInfo);

       return new DrawOnPlotReturn(null, retPI,insideClip);
    }


    private DrawOnPlotReturn drawScan(Plot         plot,
                                      ActiveFitsReadGroup frGroup,
                                      Graphics2D   g2,
                                      WorldPt      wpt,
                                      WorldPt      wpt2,
                                      RotationInfo rotation,
                                      WorldPt      offset,
                                      CachePtInfo  overrideCPInfo,
                                      boolean      enableDraw) {
        Shape   outShape[];
        boolean insideClip= false;
        ImageWorkSpacePt pt;
        ImageWorkSpacePt pt2= null;
        CachePtInfo     cpInfo= null;
        float           screenRotation;


        cpInfo= (overrideCPInfo==null) ? _plotMap.get(plot) :  overrideCPInfo;

        // set the rotation of the shape
        RotationInfo rotationToUse= findRotationToUse(rotation, wpt);

//        if (usingCachPt             &&
//            rotationToUse   != null &&
//            cpInfo._rotation!= null &&
//            rotationToUse.equals(cpInfo._rotation) ) {
//            screenRotation= cpInfo._screenRotation;
//        }
//        else {
//        }
//
//

        try {
            if (canUseCache(cpInfo,wpt,wpt2) &&
                ComparisonUtil.equals(rotationToUse,cpInfo._rotation) ) {

                outShape= cpInfo._cacheShape;
                pt= cpInfo._cachePt;
            }
            else {
                screenRotation= computeScreenRotation(plot, frGroup, rotationToUse);
                cpInfo._screenRotation= screenRotation;
                cpInfo._rotation      = rotationToUse;
                // compute the shapes using the shape in world Coordinates
                pt= plot.getImageCoords(wpt);
                if (wpt2!=null) {
		    pt2 = plot.getImageCoords(wpt2);
		}
                outShape= new Shape[_worldFocalPlane.length];
                for(int i=0; (i<outShape.length); i++) {
		    ImagePt ipt = new ImagePt(pt.getX(), pt.getY());
		    ImagePt ipt2 = new ImagePt(pt2.getX(), pt2.getY());
                    outShape[i]= getScanShape(plot, _worldFocalPlane[i].getShape(),
                                               ipt, ipt2, screenRotation, offset);
                }
                cpInfo._cachePt      = pt;
                cpInfo._cacheWorldPt = wpt;
                cpInfo._cachePt2     = pt2;
                cpInfo._cacheWorldPt2= wpt2;
                cpInfo._cacheShape= outShape;
                //System.out.println("outshape: recompute");
            }


                for(int i=0; (i<outShape.length); i++) {
                    Rectangle r= outShape[i].getBounds();
                    if (g2.hitClip( r.x, r.y, r.width, r.height)) {
                        insideClip= true;
                        g2.setPaint(_worldFocalPlane[i].getColor() );
                        if (enableDraw) {
                            g2.draw(outShape[i]);
                        }
                    }
                }
            // restore the previous transform and turn off Anti-Aliasing
        } catch (ProjectionException e) {
            System.out.println("ScalableObject.drawOnPlot: "+ e);
        }
        CachePtInfo retPI= new CachePtInfo(cpInfo);

        return new DrawOnPlotReturn(null, retPI,insideClip);
    }







   public void addPlotView(PlotContainer container) {
       Plot p;
       Iterator  j= container.iterator();
       while(j.hasNext()) {
          p= (Plot)j.next();
          addPlot(p);
       }
       container.addPlotViewStatusListener( this);
   }







   public void removePlotView(PlotContainer container) {
       Plot p;
       Iterator  j= container.iterator();
       while(j.hasNext()) {
          p= (Plot)j.next();
          removePlot(p);
       }
       container.removePlotViewStatusListener( this);
   }

   // ===================================================================
   // ------------------  Methods  from PlotViewStatusListener -----------
   // ===================================================================
    public void plotAdded(PlotViewStatusEvent ev) {
         addPlot(ev.getPlot());
    }
    public void plotRemoved(PlotViewStatusEvent ev) {
         removePlot(ev.getPlot());
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private RotationInfo findRotationToUse(RotationInfo rotation, WorldPt wpt) {
        RotationInfo rotationToUse;
        if (rotation == null) {
            rotationToUse= new RotationInfo(0.0F, wpt);
        }
        else if (rotation.getPt() == null) {
            rotationToUse= new RotationInfo(rotation.getRotation(), wpt);
        }
        else {
            rotationToUse= rotation;
            // rotation does not need massaging
        }
        return rotationToUse;
    }
 
    private boolean canUseCache(CachePtInfo cpInfo, WorldPt wpt, WorldPt wpt2) {
          boolean retval= (cpInfo._cacheWorldPt != null && 
                           cpInfo._cachePt      != null && 
                           cpInfo._cacheWorldPt.equals(wpt));
          if (retval) {
                if (wpt2                 ==null && 
                    cpInfo._cacheWorldPt2==null &&
                    cpInfo._cachePt2     ==null) {
                        retval= true;
                }
                else {
                        retval= (cpInfo._cacheWorldPt2 != null && 
                                 cpInfo._cachePt2      != null && 
                                 cpInfo._cacheWorldPt2.equals(wpt2));
                }
          }
          return retval;
    }



    void addPlot(Plot p) {
        _plotMap.put(p, new CachePtInfo());
    }

    void removePlot(Plot p) {
        _plotMap.remove(p);
    }


   protected Shape getScanShape(Plot    plot,
                                Shape   inShape,
                                ImagePt pt,
                                ImagePt pt2,
                                float screenRotation,
                                WorldPt offset) throws ProjectionException {

       GeneralPath shape1= getImageShape(plot, inShape,pt);
       GeneralPath shape2= getImageShape(plot, inShape,pt2);


       transfromShape(shape1,plot, pt,screenRotation, offset);
       transfromShape(shape2,plot, pt2,screenRotation, offset);
       return createScanShape(shape1, shape2);
   }

   protected void transfromShape(GeneralPath shape,
                                 Plot plot,
                                 ImagePt pt,
                                 float screenRotation,
                                 WorldPt offset) throws ProjectionException {
       AffineTransform af= new AffineTransform();
       af.rotate(Math.PI * (screenRotation/180), pt.getX(), pt.getY());
       if (offset != null) {
           ImagePt ip= plot.getDistanceCoords( pt,
                                               offset.getX(), offset.getY());
           af.translate(ip.getX()-pt.getX(),ip.getY()-pt.getY());
       }
       shape.transform(af);
   }

   /**
    * take a shape in world coordinates and compute what in looks like in 
    * image coordinates. 
    * @param p the plot we are drawing on
    * @param inShape the shape in world coorindates
    * @param pt the point that all shapes are relative to
    */
   protected GeneralPath getImageShape(Plot p, Shape inShape, ImagePt pt) {
       float info[]= new float[6];
       int    pType;
       double x0=0, x1=0, x2=0;
       double y0=0, y1=0, y2=0;
       ImagePt ip= null;
       GeneralPath fp= new GeneralPath();
       PathIterator pi= inShape.getPathIterator(null);


       for (; (!pi.isDone()); pi.next() ) {
           try {
              pType= pi.currentSegment(info);
              switch (pType) {
                   case PathIterator.SEG_CLOSE :
                               fp.closePath();
                               break;
                   case PathIterator.SEG_CUBICTO :
                               ip= p.getDistanceCoords( pt, info[0], info[1]);
                               x0= ip.getX();
                               y0= ip.getY();
                               ip= p.getDistanceCoords( pt, info[2], info[3]);
                               x1= ip.getX();
                               y1= ip.getY();
                               ip= p.getDistanceCoords( pt, info[4], info[5]);
                               x2= ip.getX();
                               y2= ip.getY();
                                  
                               fp.curveTo((float)x0, (float)y0,
                                          (float)x1, (float)y1,
                                          (float)x2, (float)y2);
                               break;
                   case PathIterator.SEG_LINETO :
                               ip= p.getDistanceCoords( pt, info[0], info[1]);
                               x0= ip.getX();
                               y0= ip.getY();
                               fp.lineTo((float)x0, (float)y0);
                               break;
                   case PathIterator.SEG_MOVETO :
                               ip= p.getDistanceCoords( pt, info[0], info[1]);
                               x0= ip.getX();
                               y0= ip.getY();
                               fp.moveTo((float)x0, (float)y0);
                               break;
                   case PathIterator.SEG_QUADTO :
                               ip= p.getDistanceCoords( pt, info[0], info[1]);
                               x0= ip.getX();
                               y0= ip.getY();
                               ip= p.getDistanceCoords( pt, info[2], info[3]);
                               x1= ip.getX();
                               y1= ip.getY();
                               fp.quadTo((float)x0, (float)y0,
                                         (float)x1, (float)y1);
                               break;
                   default:
                               Assert.tst(false);
                               break;
              } // end switch
          } catch (ProjectionException e) {
              ClientLog.warning(e.toString());
          }
       } // end loop;
       return fp;
   }


   private Shape createScanShape(Shape s1, Shape s2) {
      Shape retShape= null;
      if (isCircle(s1)) {
         retShape= createCircleScanShape(s1,s2);



      }
      else {
         retShape= createSegmentScanShape(s1,s2);
      }
      return retShape;
   }

   private boolean isCircle(Shape s) {
       float info[]= new float[6];
       PathIterator pi= s.getPathIterator(null);
       pi.next();
       int pType= pi.currentSegment(info);
       return (pType==PathIterator.SEG_CUBICTO);
   }

   private Shape createCircleScanShape(Shape s1, Shape s2) {

       CenterRadius cr1= getCenterRadiusOfCircle(s1);
       CenterRadius cr2= getCenterRadiusOfCircle(s2);


       Sides circle1Sides= getPerpandicularSides(cr1.center, cr2.center, 
                                                 cr1.radius);
       Sides circle2Sides= getPerpandicularSides(cr2.center, cr1.center, 
                                                 cr2.radius);

       
       return combineShapes(s1,s2,
                       circle1Sides.side1, circle1Sides.side2,
                       circle2Sides.side1, circle2Sides.side2);
   }



   private CenterRadius getCenterRadiusOfCircle(Shape circle) {

       CenterRadius retval= new CenterRadius();
       float info[]= new float[6];


       PathIterator pi= circle.getPathIterator(null);
       int pType1= pi.currentSegment(info);
       Assert.tst(pType1==PathIterator.SEG_MOVETO);
       float q= info[0];
       float r= info[1];

       pi.next();
       pi.next();
       pType1= pi.currentSegment(info);
       Assert.tst(pType1==PathIterator.SEG_CUBICTO);
       float s= info[4];
       float t= info[5];


            // get radius
       retval.radius= (float)distance( new Point2D.Double(q,r),
                                       new Point2D.Double(s,t) ) / 2;

           // get center 1
//       float x= Math.abs( (q+s)/2);
//       float y= Math.abs( (r+t)/2);
       float x= (q+s)/2;
       float y= (r+t)/2;
       retval.center= new Point2D.Float(x,y);

       return retval;
   }

    private Sides getPerpandicularSides(Point2D center,
                                        Point2D secondPt,
                                        double radius) {

        double x;
        double y;
        Sides sides=new Sides();


        double slope=(center.getY()-secondPt.getY())/
                     (center.getX()-secondPt.getX());

        // the slope will the be the tangent of angle beta
        double beta= Math.atan(slope);

        // sin beta = d1/R   d1=oposite side, R= hypotenus
        // therefore d1= r* sin beta
        //
        // cos beta = d2/R   d1=ajacent side, R= hypotenus
        // therefore d2= r* cos beta
        //
        double d1= radius*Math.sin(beta);
        double d2= radius*Math.cos(beta);


        x=center.getX()+ d1;
        y=center.getY()- d2;

        sides.side1=new Point2D.Double(x, y);

        x=center.getX()- d1;
        y=center.getY()+ d2;

        sides.side2=new Point2D.Double(x, y);

        return sides;
    }



   private Shape createSegmentScanShape(Shape s1, Shape s2) {
       List<Point2D> ptList= new ArrayList<Point2D>(200);
       EdgePoints     yRes1;
       EdgePoints     yRes2;


       addToPointList(ptList,s1);
       addToPointList(ptList,s2);

       yRes1= findExtreme(s1, getAnyPoint(s2) );
       yRes2= findExtreme(s2, getAnyPoint(s1) );


       return combineShapes(s1,s2, yRes1._edgePt1, yRes2._edgePt1,
                                   yRes1._edgePt2,    yRes2._edgePt2);

   }


   private Shape combineShapes(Shape   s1, Shape   s2,
                               Point2D p1, Point2D p2,
                               Point2D p3, Point2D p4) {
       GeneralPath rec= makeRec(p1,p2,p3,p4);
       Area areaShape= new Area(s1);
       areaShape.add(new Area(s2));
       areaShape.add(new Area(rec));

       return areaShape;
   }


   private GeneralPath makeRec( Point2D p1, Point2D p2,
                                Point2D p3, Point2D p4) {

      Line2D line1= new Line2D.Float(p1,p2);
      Line2D line2= new Line2D.Float(p3,p4);

      GeneralPath rec= new GeneralPath();
      rec.moveTo((float)p1.getX(), (float)p1.getY());

      if (line1.intersectsLine(line2)) {
         rec.lineTo((float)p3.getX(), (float)p3.getY());
         rec.lineTo((float)p2.getX(), (float)p2.getY());
         rec.lineTo((float)p4.getX(), (float)p4.getY());
      }
      else {
         rec.lineTo((float)p2.getX(), (float)p2.getY());

         line1= new Line2D.Float(p2,p3);
         line2= new Line2D.Float(p1,p4);
         if (line1.intersectsLine(line2)) {
            rec.lineTo((float)p4.getX(), (float)p4.getY());
            rec.lineTo((float)p3.getX(), (float)p3.getY());
         }
         else {
            rec.lineTo((float)p3.getX(), (float)p3.getY());
            rec.lineTo((float)p4.getX(), (float)p4.getY());
         }
      }
      rec.closePath();
      return rec;
   }


   private double distance(Point2D p1, Point2D p2) {
        double x1= p1.getX();
        double y1= p1.getY();
        double x2= p2.getX();
        double y2= p2.getY();
        return Math.sqrt( (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)  );
   }



   private Point2D getAnyPoint(Shape s) {
      float info[]= new float[6];
//      PathIterator pi= s.getPathIterator(null);
//      return new Point2D.Double(info[0],info[1]);


       float xSum= 0;
       float ySum= 0;
       int div= 0;
       int          pType;
       PathIterator pi= s.getPathIterator(null);
       for (; (!pi.isDone()); pi.next() ) {
           pType= pi.currentSegment(info);
           switch (pType) {
               case PathIterator.SEG_CLOSE :
                   break;
               case PathIterator.SEG_CUBICTO :
                   Assert.tst(false);
                   break;
               case PathIterator.SEG_MOVETO :
               case PathIterator.SEG_LINETO :
                   xSum+= info[0];
                   ySum+=info[1];
                   div++;
                   break;
               case PathIterator.SEG_QUADTO :
                   Assert.tst(false);
                   break;
               default:
                   Assert.tst(false);
                   break;
           } // end switch
       } // end loop;


       return new Point2D.Double(xSum/div, ySum/div);
   }

   private EdgePoints findExtreme(Shape s, Point2D testPt) {
      int          pType;
      Point2D retPt1, retPt2;
      Point2D pt, pt1;
      float info[]= new float[6];
      List<Point2D> ptList= new ArrayList<Point2D>(10);


      PathIterator pi= s.getPathIterator(null);
      for (; (!pi.isDone()); pi.next() ) {
         pType= pi.currentSegment(info);
         switch (pType) {
            case PathIterator.SEG_CLOSE :
               break;
            case PathIterator.SEG_CUBICTO :
               Assert.tst(false);
               break;
            case PathIterator.SEG_MOVETO :
            case PathIterator.SEG_LINETO :
               pt= new Point2D.Double(info[0],info[1]);
               ptList.add(pt);
               break;
            case PathIterator.SEG_QUADTO :
               Assert.tst(false);
               break;
            default:
               Assert.tst(false);
               break;
         } // end switch
      } // end loop;

      double maxDist= 0;
      double minDist= 999999999.0D;
      double dist;
      int maxDistIdx= 0;
      int minDistIdx= 0;
      pt= ptList.get(0);
      pt1= ptList.get(ptList.size()-1);
      if (pt.equals(pt1)) ptList.remove(pt1);
      for(int i= 0; i<ptList.size(); i++) {
         pt= ptList.get(i);
         dist= distance(pt,testPt);
         if(dist>maxDist) {
            maxDist= dist;
            maxDistIdx= i;
         }
         if(dist<minDist) {
            minDist= dist;
            minDistIdx= i;
         }
      }

      int idx1= (maxDistIdx + minDistIdx)/2;
      int lowest= (maxDistIdx < minDistIdx) ? maxDistIdx : minDistIdx;
      int idx2= lowest-(idx1-lowest);
      if (idx2<0) idx2= ptList.size()+idx2;

      retPt1= ptList.get( idx1);
      retPt2= ptList.get( idx2);

      return new EdgePoints(retPt1, retPt2);
   }

   private void addToPointList(List<Point2D> ptList , Shape s) {
       PathIterator pi= s.getPathIterator(null);
       Point2D      pt;
       int          pType;
       float        info[]= new float[6];
       for (; (!pi.isDone()); pi.next() ) {
           pType= pi.currentSegment(info);
           switch (pType) {
                case PathIterator.SEG_CLOSE :
                            break;
                case PathIterator.SEG_CUBICTO :
                            Assert.tst(false);
                            break;
                case PathIterator.SEG_LINETO :
                            pt= new Point2D.Double(info[0],info[1]);
                            ptList.add(pt);
                            break;
                case PathIterator.SEG_MOVETO :
                            pt= new Point2D.Double(info[0],info[1]);
                            ptList.add(pt);
                            break;
                case PathIterator.SEG_QUADTO :
                            Assert.tst(false);
                            break;
                default:
                            Assert.tst(false);
                            break;
           } // end switch
       } // end loop;
   }


    /**
     * Make a copy of the ShapeInfo array
     */
    private ShapeInfo[] copyWF(ShapeInfo inwf[]) {
         ShapeInfo retval[]= new ShapeInfo[inwf.length];
         System.arraycopy(inwf,0,retval,0,inwf.length);
         return retval;
    }

    /**
     * Get the rotation for the screen based on the rotation passed.
     */
    private float computeScreenRotation(Plot plot,  ActiveFitsReadGroup frGroup, RotationInfo rotation) {
        float screenRotation= rotation.getRotation();

        WorldPt j2000p1;
        WorldPt j2000p2;
        WorldPt wpt= rotation.getPt();

        ImageWorkSpacePt p1;
        ImageWorkSpacePt p2;
        ImageWorkSpacePt p3;
        ImagePlot ip = (ImagePlot) plot;
        double cdelt1 = frGroup.getFitsRead(Band.NO_BAND).getImageHeader().cdelt1;
        double degree= 0;


        if (SPITZER_STYLE_ROTATION) {
            if (!wpt.getCoordSys().equals(CoordinateSys.EQ_J2000))
                j2000p1= Plot.convert(wpt, CoordinateSys.EQ_J2000);
            else
                j2000p1= wpt;
            //j2000p2= new WorldPt(wpt.getLon(), wpt.getLat() + 1.0);
            // move over just one pixel to avoid warping at large distances
            j2000p2= new WorldPt(wpt.getLon(), wpt.getLat() + Math.abs(cdelt1));

            try {
                p1= plot.getImageCoords(j2000p1);
                p2= plot.getImageCoords(j2000p2);
                p3= new ImageWorkSpacePt(p2.getX(), p1.getY());

                double lineA= p2.getY() - p3.getY();
                double lineB= p3.getX() - p1.getX();


                //double radian= Math.atan( lineA/lineB );
                double radian= Math.atan2( lineA, lineB );
                degree= radian * 180/Math.PI;

                /* now see if we have a mirror image and need to rotate backwards */
            } catch (ProjectionException e) {
                System.out.println("ScalableObject.computeScreenRotation: " + e);
            }
        }

        if (cdelt1 > 0) screenRotation= (float)(degree - rotation.getRotation() - 180.0);
        else            screenRotation= (float)(degree + rotation.getRotation());

        return screenRotation;
    }

//===================================================================
//------------------------- Inner classes ---------------------------
//===================================================================
 
    /**
     * Store information about the plot
     */
    public static class RotationInfo {
        private float   _rotation;
        private WorldPt _wpt;
        public RotationInfo(float rotation,  WorldPt wpt) {
             _rotation= rotation;
             _wpt     = wpt;
        }
        public float   getRotation()  { return _rotation; }
        public WorldPt getPt()        { return _wpt; }

        public boolean equals(Object o) {
           boolean retval= false; 
           if (o!= null && o instanceof RotationInfo) {
              RotationInfo r= (RotationInfo)o;
              if (getClass() == r.getClass() &&
                  _rotation  == r._rotation         &&
                  _wpt       == r._wpt) {
                      retval= true; 
              } // end if
           }
           return retval;
        }
    }
  
    /**
     * Store information about the plot
     */
    public static class CachePtInfo {
        public WorldPt         _cacheWorldPt   = null;
        public WorldPt         _cacheWorldPt2  = null;
        public ImageWorkSpacePt  _cachePt        = null;
        public ImageWorkSpacePt  _cachePt2       = null;
        public Shape           _cacheShape[]   = null;
        public AffineTransform _startingTrans  = null;
        public AffineTransform _useTrans       = null;
        public RotationInfo    _rotation;
        public float           _screenRotation;
        public WorldPt         _offset;

        public CachePtInfo() {}
        public CachePtInfo(ImageWorkSpacePt cachePt, 
                           WorldPt worldPt, 
                           ImageWorkSpacePt cachePt2, 
                           WorldPt worldPt2, 
                           Shape   cacheShape[]) {
           _cachePt      = cachePt;
           _cachePt2     = cachePt2;
           _cacheWorldPt = worldPt;
           _cacheWorldPt2= worldPt2;
           _cacheShape  = cacheShape;
        }
        public CachePtInfo(CachePtInfo cp) {
           if (cp!=null) {
              _cachePt       = cp._cachePt;
              _cachePt2      = cp._cachePt2;
              _cacheShape    = cp._cacheShape;
              _cacheWorldPt  = cp._cacheWorldPt;
              _cacheWorldPt2 = cp._cacheWorldPt2;
              _startingTrans = cp._startingTrans;
              _useTrans      = cp._useTrans;
              _rotation      = cp._rotation;
              _offset        = cp._offset;
              _screenRotation= cp._screenRotation;
           }
        }
    }

    public static class DrawOnPlotReturn {
        public Rectangle   _repairArea= null;
        public CachePtInfo _cpInfo;
        public boolean     _insideClip;
        public DrawOnPlotReturn( Rectangle   repairArea,
                                 CachePtInfo cpInfo,
                                 boolean     insideClip) {
            _repairArea= repairArea;
            _cpInfo    = cpInfo;
            _insideClip= insideClip;
        }

       public Rectangle   getRepairArea()  { return _repairArea; }
       public CachePtInfo getCachePtInfo() { return _cpInfo; }
    }

    private static class EdgePoints {
       public Point2D _edgePt1;
       public Point2D _edgePt2;

       public EdgePoints(Point2D edgePt1, Point2D edgePt2) {
          _edgePt1= edgePt1;
          _edgePt2= edgePt2;
       }
    }


    private static class Sides {
       public Point2D side1;
       public Point2D side2;
    }

    private static class CenterRadius {
       public Point2D center; 
       public float   radius;
    }
}
