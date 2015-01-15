/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionBox;
import edu.caltech.ipac.util.dd.RegionDimension;
import edu.caltech.ipac.util.dd.RegionValue;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.Arrays;
import java.util.List;


/**
 * @author Trey Roby
 * @version $Id: ImageCoordsBoxObj.java,v 1.8 2012/02/10 01:39:40 roby Exp $
 */
public class ImageCoordsBoxObj extends DrawObj {

    public enum Style {STANDARD,HANDLED,LIGHT}

    private final Pt _pt;
    private final int _width;
    private final int _height;
    private Style _style= Style.STANDARD;

    private ImageCoordsBoxObj(Pt pt, int width, int height) {
        super();
        _pt = pt;
        _width= width;
        _height= height;
    }

    public ImageCoordsBoxObj(ImageWorkSpacePt pt, int width, int height) {
        this((Pt)pt,width,height);
    }

    public ImageCoordsBoxObj(ScreenPt pt, int width, int height) {
        this((Pt)pt,width,height);
    }


    @Override
    public int getLineWidth() {
        switch (_style) {
            case STANDARD: return 2;
            default : return 1;
        }
    }

    public int getWidth() { return _width; }
    public int getHeight() { return _height; }

    public void setStyle(Style s) { _style= s; }
    public Style getStyle() { return _style; }

    @Override
    protected boolean getSupportsWebPlot() {
        return (_pt instanceof ImageWorkSpacePt);
    }

    @Override
    public Pt getCenterPt() { return _pt; }

    public double getScreenDist(WebPlot plot, ScreenPt pt) {
        double retval= -1;
        ScreenPt testPt;
        if (_pt instanceof ImageWorkSpacePt ) {
            ImageWorkSpacePt newPt= new ImageWorkSpacePt(_pt.getX() + _width/2, _pt.getY() + _height/2);
            testPt= plot.getScreenCoords(newPt);
        }
        else if (_pt instanceof ScreenPt ) {
            ScreenPt spt= (ScreenPt)_pt;
            testPt= new ScreenPt(spt.getIX() + _width/2, spt.getIY() + _height/2);
        }
        else {
            testPt= null;
            assert false;
        }

        if (testPt!=null) {
            double dx= pt.getIX() - testPt.getIX();
            double dy= pt.getIY() - testPt.getIY();
            retval= Math.sqrt(dx*dx + dy*dy);
        }
        return retval;
    }


    public void draw(Graphics graphics, WebPlot p, AutoColor ac, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        drawImageCoordsBox(graphics,p,ac,useStateColor);
    }

    public void draw(Graphics graphics, AutoColor ac, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        drawScreenCoordsBox(graphics,ac,useStateColor);
    }




    private void drawImageCoordsBox(Graphics graphics,
                                      WebPlot plot,
                                      AutoColor ac,
                                      boolean useStateColor) {


        drawImageBox(graphics, plot, ac, useStateColor);
    }



    private void drawScreenCoordsBox(Graphics graphics,
                                       AutoColor ac,
                                       boolean useStateColor) {

        drawScreenBox(graphics, ac, useStateColor);
    }




    private void drawImageBox(Graphics graphics,
                                WebPlot plot,
                                AutoColor ac,
                                boolean useStateColor) {
        ImageWorkSpacePt hypPt= new ImageWorkSpacePt(_pt.getX()+_width, _pt.getY()+_height);

        ScreenPt pt0=plot.getScreenCoords((ImageWorkSpacePt)_pt);
        ScreenPt pt2=plot.getScreenCoords(hypPt);
        drawBox(graphics,pt0,pt2,ac,useStateColor);
    }


    private void drawScreenBox(Graphics graphics,
                                AutoColor ac,
                                boolean useStateColor) {
        ScreenPt pt2= new ScreenPt(((ScreenPt)_pt).getIX()+_width,
                                     ((ScreenPt)_pt).getIY()+_height);
        drawBox(graphics,(ScreenPt)_pt,pt2,ac,useStateColor);
    }


    private void drawBox(Graphics graphics,
                         ScreenPt pt0,
                         ScreenPt pt2,
                         AutoColor ac,
                         boolean useStateColor) {


        int lineWidth;

        lineWidth= getLineWidth();

        String color= calculateColor(ac,useStateColor);
        int sWidth= (pt2.getIX()-pt0.getIX());
        int sHeight= (pt2.getIY()-pt0.getIY());
        ScreenPt pt1= new ScreenPt((pt0.getIX()+sWidth),pt0.getIY());
        ScreenPt pt3= new ScreenPt((pt0.getIX()),(pt0.getIY()+sHeight));

        if (graphics instanceof AdvancedGraphics && getShadow()!=null) {
            ((AdvancedGraphics)graphics).setShadowPerm(getShadow());
        }
        graphics.drawRec(color, lineWidth,
                       pt0.getIX(), pt0.getIY(),
                       sWidth, sHeight);

        if (_style==Style.HANDLED) {
            DrawUtil.drawHandledLine(graphics, color,
                                           pt0.getIX(), pt0.getIY(),
                                           pt1.getIX(), pt1.getIY());
            DrawUtil.drawHandledLine(graphics, color,
                                           pt1.getIX(), pt1.getIY(),
                                           pt2.getIX(), pt2.getIY());
            DrawUtil.drawHandledLine(graphics, color,
                                           pt2.getIX(), pt2.getIY(),
                                           pt3.getIX(), pt3.getIY());
            DrawUtil.drawHandledLine(graphics, color,
                                           pt3.getIX(), pt3.getIY(),
                                           pt0.getIX(), pt0.getIY());
        }

        if (graphics instanceof AdvancedGraphics && getShadow()!=null) {
            ((AdvancedGraphics)graphics).setShadowPerm(null);
        }
    }

    @Override
    public List<Region> toRegion(WebPlot plot, AutoColor ac) {
        WorldPt wp= WebPlot.getWorldPtRepresentation(_pt);
        RegionDimension dim;
        if (_pt instanceof ImageWorkSpacePt ) {
            dim= new RegionDimension(new RegionValue(_width, RegionValue.Unit.IMAGE_PIXEL),
                                     new RegionValue(_height, RegionValue.Unit.IMAGE_PIXEL));
        }
        else if (_pt instanceof ScreenPt ) {
            dim= new RegionDimension(new RegionValue(_width, RegionValue.Unit.SCREEN_PIXEL),
                                     new RegionValue(_height, RegionValue.Unit.SCREEN_PIXEL));
        }
        else {
            dim= null;
            WebAssert.argTst(false, "unexpected point type");
        }
        Region r= new RegionBox(wp,dim);
        r.getOptions().setColor(calculateColor(ac,false));
        return Arrays.asList(r);
    }
}
