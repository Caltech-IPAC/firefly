package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.ViewPortPt;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionBox;
import edu.caltech.ipac.util.dd.RegionDimension;
import edu.caltech.ipac.util.dd.RegionValue;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.Arrays;
import java.util.List;


/**
 * @author Trey Roby
 * @version $Id: CoordsBoxObj.java,v 1.4 2012/02/10 01:39:40 roby Exp $
 */
public class CoordsBoxObj extends DrawObj {

    public enum Style {STANDARD,HANDLED,LIGHT}

    private final Pt _pt1;
    private final Pt _pt2;
    private Style _style= Style.STANDARD;

    public CoordsBoxObj(Pt pt1, Pt pt2) {
        super();
        _pt1 = pt1;
        _pt2 = pt2;
    }

    public void setStyle(Style s) { _style= s; }
    public Style getStyle() { return _style; }

    @Override
    protected boolean getSupportsWebPlot() {
        return (!(_pt1 instanceof ScreenPt));
    }

    public double getScreenDist(WebPlot plot, ScreenPt pt)
                                          throws ProjectionException {
        ScreenPt testPt;
        ScreenPt sp1= plot.getScreenCoords(_pt1);
        ScreenPt sp2= plot.getScreenCoords(_pt2);
        int width= Math.abs(sp1.getIX() - sp2.getIX());
        int height= Math.abs(sp1.getIY() - sp2.getIY());

        testPt= new ScreenPt(sp1.getIX() + width/2, sp1.getIY() + height/2);

        double dx= pt.getIX() - testPt.getIX();
        double dy= pt.getIY() - testPt.getIY();
        return Math.sqrt(dx*dx + dy*dy);
    }

    @Override
    public Pt getCenterPt() {
        double x= (_pt1.getX() + _pt2.getX())/2;
        double y= (_pt1.getY() + _pt2.getY())/2;

        Pt pt;
        try {
            pt= WebPlot.makePt(_pt1.getClass(), x,y);
        } catch (ProjectionException e) {
            pt= null;
        }

        return pt;
    }

    public void draw(Graphics jg, WebPlot p, boolean front, AutoColor ac) throws UnsupportedOperationException {
        jg.deleteShapes(getShapes());
        Shapes shapes= drawImageBox(jg,p,front,ac);
        setShapes(shapes);
    }

    public void draw(Graphics jg, boolean front, AutoColor ac) throws UnsupportedOperationException {
        if (_pt1 instanceof ScreenPt && _pt2 instanceof ScreenPt) {
            jg.deleteShapes(getShapes());
            ViewPortPt vp1= new ViewPortPt(((ScreenPt) _pt1).getIX(),((ScreenPt) _pt1).getIY());
            ViewPortPt vp2= new ViewPortPt(((ScreenPt) _pt2).getIX(),((ScreenPt) _pt2).getIY());
            Shapes shapes= drawBox(jg,vp1, vp2,front,ac);
            setShapes(shapes);
        }
    }


    private Shapes drawImageBox(Graphics jg,
                                WebPlot plot,
                                boolean front,
                                AutoColor ac) {

        Shapes retval= null;
        try {
            ViewPortPt pt0=plot.getViewPortCoords(_pt1);
            ViewPortPt pt2=plot.getViewPortCoords(_pt2);
            int sWidth= (pt2.getIX()-pt0.getIX());
            int sHeight= (pt2.getIY()-pt0.getIY());
            ViewPortPt pt1= new ViewPortPt((pt0.getIX()+sWidth),pt0.getIY());
            ViewPortPt pt3= new ViewPortPt((pt0.getIX()),(pt0.getIY()+sHeight));
            if (crossesViewPort(plot, _pt1,_pt2)) {
                retval= drawBox(jg,pt0,pt2,front,ac);
            }
//            if (plot.pointInViewPort(pt0) ||
//                plot.pointInViewPort(pt1) ||
//                plot.pointInViewPort(pt2) ||
//                plot.pointInViewPort(pt3) ) {
//                retval= drawBox(jg,pt0,pt2,front,ac);
//            }

        } catch (ProjectionException e) {
           retval= null;
        }
        return retval;
    }


    private boolean crossesViewPort(WebPlot plot, Pt ppt1, Pt ppt2) {
        boolean retval;
        try {
            ViewPortPt pt0=plot.getViewPortCoords(ppt1);
            ViewPortPt pt2=plot.getViewPortCoords(ppt2);
            int sWidth= (pt2.getIX()-pt0.getIX());
            int sHeight= (pt2.getIY()-pt0.getIY());
            ViewPortPt pt1= new ViewPortPt((pt0.getIX()+sWidth),pt0.getIY());
            ViewPortPt pt3= new ViewPortPt((pt0.getIX()),(pt0.getIY()+sHeight));
            if (plot.pointInViewPort(pt0) ||
                plot.pointInViewPort(pt1) ||
                plot.pointInViewPort(pt2) ||
                plot.pointInViewPort(pt3) ) {
                retval= true;
            }
            else {
                ScreenPt spt= plot.getScreenCoords(ppt1);
                int x0= spt.getIX();
                int y0= spt.getIY();
                Dimension dim= plot.getViewPortDimension();
                if (sWidth<0) {
                    x0+=sWidth;
                    sWidth*= -1;
                }
                if (sHeight<0) {
                    y0+=sHeight;
                    sHeight*= -1;
                }

                retval= VisUtil.intersects(x0,y0, sWidth,sHeight,
                                           plot.getViewPortX(), plot.getViewPortY(),
                                           dim.getWidth(), dim.getHeight() );

            }
        } catch (ProjectionException e) {

            retval= false;
        }
        return retval;
    }



    private Shapes drawBox(Graphics jg,
                           ViewPortPt pt0,
                           ViewPortPt pt2,
                           boolean front,
                           AutoColor ac) {


        Shape s;
        Shapes tmpS;
        Shapes retval;
        int lineWidth;

        switch (_style) {
            case STANDARD: lineWidth= 2; break;
            default : lineWidth= 1; break;
        }

        String color= calculateColor(ac);
        int sWidth= (pt2.getIX()-pt0.getIX());
        int sHeight= (pt2.getIY()-pt0.getIY());
        s= jg.drawRec(color, front, lineWidth,
                       pt0.getIX(),pt0.getIY(),
                       sWidth, sHeight);
        retval= new Shapes(s);


        if (_style== Style.HANDLED) {
            ViewPortPt pt1= new ViewPortPt((pt0.getIX()+sWidth),pt0.getIY());
            ViewPortPt pt3= new ViewPortPt((pt0.getIX()),(pt0.getIY()+sHeight));
            tmpS= DrawUtil.drawHandledLine(jg, color,
                                           pt0.getIX(), pt0.getIY(),
                                           pt1.getIX(), pt1.getIY(), front);
            retval= retval.concat(tmpS);
            tmpS= DrawUtil.drawHandledLine(jg, color,
                                           pt1.getIX(), pt1.getIY(),
                                           pt2.getIX(), pt2.getIY(), front);
            retval= retval.concat(tmpS);
            tmpS= DrawUtil.drawHandledLine(jg, color,
                                           pt2.getIX(), pt2.getIY(),
                                           pt3.getIX(), pt3.getIY(), front);
            retval= retval.concat(tmpS);
            tmpS= DrawUtil.drawHandledLine(jg, color,
                                           pt3.getIX(), pt3.getIY(),
                                           pt0.getIX(), pt0.getIY(), front);
            retval= retval.concat(tmpS);
        }

        return retval;
    }

    @Override
    public List<Region> toRegion(WebPlot plot, AutoColor ac) {
        WorldPt wp= WebPlot.getWorldPtRepresentation(_pt1);
        RegionDimension dim;

        int width= (int)(_pt2.getX()-_pt1.getX());
        int height= (int)(_pt2.getY()-_pt1.getY());

        if (_pt1 instanceof ImageWorkSpacePt || _pt1 instanceof ImagePt) {
            dim= new RegionDimension(new RegionValue(width, RegionValue.Unit.IMAGE_PIXEL),
                                     new RegionValue(height, RegionValue.Unit.IMAGE_PIXEL));
        }
        else if (_pt1 instanceof ScreenPt ) {
            dim= new RegionDimension(new RegionValue(width, RegionValue.Unit.SCREEN_PIXEL),
                                     new RegionValue(height, RegionValue.Unit.SCREEN_PIXEL));
        }
        else  {
            dim= new RegionDimension(new RegionValue(width, RegionValue.Unit.DEGREE),
                                     new RegionValue(height, RegionValue.Unit.DEGREE));
        }
        Region r= new RegionBox(wp,dim);
        r.getOptions().setColor(calculateColor(ac));
        return Arrays.asList(r);
    }

}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
