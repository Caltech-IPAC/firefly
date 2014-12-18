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
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.Arrays;
import java.util.List;


/**
 * @author Trey Roby
 * @version $Id: SelectBox.java,v 1.4 2012/02/10 01:39:40 roby Exp $
 */
public class SelectBox extends DrawObj {

    public enum Style {STANDARD,HANDLED,LIGHT}

    private final Pt _pt1;
    private final Pt _pt2;
    private Style _style= Style.STANDARD;
    private String innerBoxColor= "white";

    public SelectBox(Pt pt1, Pt pt2) {
        super();
        _pt1 = pt1;
        _pt2 = pt2;
        setShadow(new AdvancedGraphics.Shadow(4,1,1,"black"));
    }

    public void setStyle(Style s) { _style= s; }
    public Style getStyle() { return _style; }


    @Override
    public int getLineWidth() { return 0; }

    @Override
    protected boolean getSupportsWebPlot() {
        return (!(_pt1 instanceof ScreenPt));
    }

    public String getInnerBoxColor() { return innerBoxColor; }
    public void setInnerBoxColor(String c) { innerBoxColor = c; }

    public double getScreenDist(WebPlot plot, ScreenPt pt) {
        double retval= -1;
        ScreenPt testPt;
        ScreenPt sp1= plot.getScreenCoords(_pt1);
        ScreenPt sp2= plot.getScreenCoords(_pt2);
        if (sp1!=null && sp2!=null) {
            int width= Math.abs(sp1.getIX() - sp2.getIX());
            int height= Math.abs(sp1.getIY() - sp2.getIY());

            testPt= new ScreenPt(sp1.getIX() + width/2, sp1.getIY() + height/2);

            double dx= pt.getIX() - testPt.getIX();
            double dy= pt.getIY() - testPt.getIY();
            retval= Math.sqrt(dx*dx + dy*dy);
        }
        return retval;
    }

    @Override
    public Pt getCenterPt() {
        double x= (_pt1.getX() + _pt2.getX())/2;
        double y= (_pt1.getY() + _pt2.getY())/2;

        return WebPlot.makePt(_pt1.getClass(), x,y);
    }

    public void draw(Graphics graphics, WebPlot p, AutoColor ac, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        drawImageBox(graphics,p,ac);
    }

    public void draw(Graphics graphics, AutoColor ac, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        if (_pt1 instanceof ScreenPt && _pt2 instanceof ScreenPt) {
            ViewPortPt vp1= new ViewPortPt(((ScreenPt) _pt1).getIX(),((ScreenPt) _pt1).getIY());
            ViewPortPt vp2= new ViewPortPt(((ScreenPt) _pt2).getIX(),((ScreenPt) _pt2).getIY());
            drawBox(graphics,vp1, vp2,ac);
        }
    }


    private void drawImageBox(Graphics graphics, WebPlot plot, AutoColor ac) {
        ViewPortPt pt0=plot.getViewPortCoords(_pt1);
        ViewPortPt pt2=plot.getViewPortCoords(_pt2);
        if (pt0!=null && pt2!=null && crossesViewPort(plot, _pt1,_pt2)) {
            drawBox(graphics,pt0,pt2,ac);
        }
    }


    private boolean crossesViewPort(WebPlot plot, Pt ppt1, Pt ppt2) {
        ViewPortPt pt0=plot.getViewPortCoords(ppt1);
        ViewPortPt pt2=plot.getViewPortCoords(ppt2);
        if (pt0==null || pt2==null) return false;

        int sWidth= (pt2.getIX()-pt0.getIX());
        int sHeight= (pt2.getIY()-pt0.getIY());
        ViewPortPt pt1= new ViewPortPt((pt0.getIX()+sWidth),pt0.getIY());
        ViewPortPt pt3= new ViewPortPt((pt0.getIX()),(pt0.getIY()+sHeight));
        boolean retval= false;
        if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1) ||
                                         plot.pointInViewPort(pt2) ||
                                         plot.pointInViewPort(pt3) ) {
            retval= true;
        }
        else {
            ScreenPt spt= plot.getScreenCoords(ppt1);
            if (spt!=null) {
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

        }
        return retval;
    }



    private void drawBox(Graphics graphics, ViewPortPt pt0, ViewPortPt pt2, AutoColor ac) {


        int lineWidth;

        switch (_style) {
            case STANDARD: lineWidth= 2; break;
            default : lineWidth= 1; break;
        }

        String color= calculateColor(ac,false);
        int sWidth= (pt2.getIX()-pt0.getIX());
        int sHeight= (pt2.getIY()-pt0.getIY());
        graphics.drawRec(color, lineWidth,
                   pt0.getIX(), pt0.getIY(),
                   sWidth, sHeight);

        if (graphics instanceof AdvancedGraphics && getShadow()!=null) {
            ((AdvancedGraphics)graphics).setShadowPerm(getShadow());
        }

        if (_style== Style.HANDLED) {
            DrawUtil.drawInnerRecWithHandles(graphics, ac.getColor(innerBoxColor),
                                                       2, pt0.getIX(), pt0.getIY(),
                                                       pt2.getIX(), pt2.getIY());

            ViewPortPt pt1= new ViewPortPt((pt0.getIX()+sWidth),pt0.getIY());
            ViewPortPt pt3= new ViewPortPt((pt0.getIX()),(pt0.getIY()+sHeight));
            graphics.beginPath(color,3);
            DrawUtil.drawHandledLine(graphics, color,
                                           pt0.getIX(), pt0.getIY(),
                                           pt1.getIX(), pt1.getIY(), true);
            DrawUtil.drawHandledLine(graphics, color,
                                           pt1.getIX(), pt1.getIY(),
                                           pt2.getIX(), pt2.getIY(), true);
            DrawUtil.drawHandledLine(graphics, color,
                                           pt2.getIX(), pt2.getIY(),
                                           pt3.getIX(), pt3.getIY(),true);
            DrawUtil.drawHandledLine(graphics, color,
                                           pt3.getIX(), pt3.getIY(),
                                           pt0.getIX(), pt0.getIY(), true);
            graphics.drawPath();
        }
        if (graphics instanceof AdvancedGraphics && getShadow()!=null) {
            ((AdvancedGraphics)graphics).setShadowPerm(null);
        }
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
        r.getOptions().setColor(calculateColor(ac,false));
        return Arrays.asList(r);
    }

}
