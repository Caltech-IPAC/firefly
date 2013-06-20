package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionBox;
import edu.caltech.ipac.util.dd.RegionDimension;
import edu.caltech.ipac.util.dd.RegionValue;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
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

    public double getScreenDist(WebPlot plot, ScreenPt pt)
                                          throws ProjectionException {
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
        double dx= pt.getIX() - testPt.getIX();
        double dy= pt.getIY() - testPt.getIY();
        return Math.sqrt(dx*dx + dy*dy);
    }


    public void draw(Graphics jg, WebPlot p, AutoColor ac, boolean useStateColor) throws UnsupportedOperationException {
        jg.deleteShapes(getShapes());
        Shapes shapes= drawImageCoordsBox(jg,p,ac,useStateColor);
        setShapes(shapes);
    }

    public void draw(Graphics jg, AutoColor ac, boolean useStateColor) throws UnsupportedOperationException {
        jg.deleteShapes(getShapes());
        Shapes shapes= drawScreenCoordsBox(jg,ac,useStateColor);
        setShapes(shapes);
    }




    private Shapes drawImageCoordsBox(Graphics jg,
                                      WebPlot plot,
                                      AutoColor ac,
                                      boolean useStateColor) {


        return drawImageBox(jg, plot, ac, useStateColor);
    }



    private Shapes drawScreenCoordsBox(Graphics jg,
                                       AutoColor ac,
                                       boolean useStateColor) {

        return drawScreenBox(jg, ac, useStateColor);
    }




    private Shapes drawImageBox(Graphics jg,
                                WebPlot plot,
                                AutoColor ac,
                                boolean useStateColor) {
        ImageWorkSpacePt hypPt= new ImageWorkSpacePt(_pt.getX()+_width, _pt.getY()+_height);

        ScreenPt pt0=plot.getScreenCoords((ImageWorkSpacePt)_pt);
        ScreenPt pt2=plot.getScreenCoords(hypPt);
        return drawBox(jg,pt0,pt2,ac,useStateColor);
    }


    private Shapes drawScreenBox(Graphics jg,
                                AutoColor ac,
                                boolean useStateColor) {
        ScreenPt pt2= new ScreenPt(((ScreenPt)_pt).getIX()+_width,
                                     ((ScreenPt)_pt).getIY()+_height);
        return drawBox(jg,(ScreenPt)_pt,pt2,ac,useStateColor);
    }


    private Shapes drawBox(Graphics jg,
                           ScreenPt pt0,
                           ScreenPt pt2,
                           AutoColor ac,
                           boolean useStateColor) {


        Shape s;
        Shapes tmpS;
        Shapes retval;
        int lineWidth;

        switch (_style) {
            case STANDARD: lineWidth= 2; break;
            default : lineWidth= 1; break;
        }

        String color= calculateColor(ac,useStateColor);
        int sWidth= (pt2.getIX()-pt0.getIX());
        int sHeight= (pt2.getIY()-pt0.getIY());
        ScreenPt pt1= new ScreenPt((pt0.getIX()+sWidth),pt0.getIY());
        ScreenPt pt3= new ScreenPt((pt0.getIX()),(pt0.getIY()+sHeight));

        s= jg.drawRec(color, lineWidth,
                       pt0.getIX(),pt0.getIY(),
                       sWidth, sHeight);
        retval= new Shapes(s);


        if (_style==Style.HANDLED) {
            tmpS= DrawUtil.drawHandledLine(jg, color,
                                           pt0.getIX(), pt0.getIY(),
                                           pt1.getIX(), pt1.getIY());
            retval= retval.concat(tmpS);
            tmpS= DrawUtil.drawHandledLine(jg, color,
                                           pt1.getIX(), pt1.getIY(),
                                           pt2.getIX(), pt2.getIY());
            retval= retval.concat(tmpS);
            tmpS= DrawUtil.drawHandledLine(jg, color,
                                           pt2.getIX(), pt2.getIY(),
                                           pt3.getIX(), pt3.getIY());
            retval= retval.concat(tmpS);
            tmpS= DrawUtil.drawHandledLine(jg, color,
                                           pt3.getIX(), pt3.getIY(),
                                           pt0.getIX(), pt0.getIY());
            retval= retval.concat(tmpS);
        }

        return retval;
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
