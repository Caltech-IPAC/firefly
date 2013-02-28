package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.OffsetScreenPt;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.ViewPortPt;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionAnnulus;
import edu.caltech.ipac.util.dd.RegionBox;
import edu.caltech.ipac.util.dd.RegionDimension;
import edu.caltech.ipac.util.dd.RegionLines;
import edu.caltech.ipac.util.dd.RegionOptions;
import edu.caltech.ipac.util.dd.RegionText;
import edu.caltech.ipac.util.dd.RegionValue;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.List;


/**
 * User: roby
 * Date: Jun 18, 2008
 * Time: 12:35:43 PM
 */
public class ShapeDataObj extends DrawObj {

    public enum TextLocation {TOP, BOTTOM, MID_POINT,
                              MID_POINT_OR_BOTTOM, MID_POINT_OR_TOP } // use MID_X, MID_X_LONG, MID_Y, MID_Y_LONG for vertical or horizontal lines
    public static final int DEF_OFFSET= 15;
    public static final String FONT_SIZE = "9pt";
    private static final String FONT_FALLBACK= ",sans-serif";
    public static final String HTML_DEG= "&deg;";

    public enum Style {STANDARD,HANDLED}
    public enum ShapeType {Line, Text,Circle, Rectangle}

    private Pt _pts[];
    private String _text= null;
    private final ShapeType _sType;
    private String fontName = "helvetica";
    private String fontSize = FONT_SIZE;
    private String fontWeight = "normal";
    private String fontStyle = "normal";
    private Style _style= Style.STANDARD;
    private int _size1InPix= Integer.MAX_VALUE;
    private int _size2InPix= Integer.MAX_VALUE;
    private TextLocation _textLoc= TextLocation.BOTTOM;
    private OffsetScreenPt _textOffset= null;

     private ShapeDataObj(ShapeType sType) {
         super();
         _sType= sType;
    }

    public static ShapeDataObj makeLine(WorldPt pt1, WorldPt pt2) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Line);
        s._pts= new Pt[] {pt1, pt2};
        return s;
    }

    public static ShapeDataObj makeLine(ImageWorkSpacePt pt1, ImageWorkSpacePt pt2) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Line);
        s._pts= new Pt[] {pt1, pt2};
        return s;
    }

    public static ShapeDataObj makeLine(Pt pt1, Pt pt2) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Line);
        s._pts= new Pt[] {pt1, pt2};
        return s;
    }

    public static ShapeDataObj makeCircle(WorldPt pt1, WorldPt pt2) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Circle);
        s._pts= new Pt[] {pt1, pt2};
        return s;
    }

    public static ShapeDataObj makeCircle(Pt pt1, int radiusInPix) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Circle);
        s._pts= new Pt[] {pt1};
        s._size1InPix= radiusInPix;
        return s;
    }

    public static ShapeDataObj makeRectangle(WorldPt pt1, WorldPt pt2) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Rectangle);
        s._pts= new Pt[] {pt1, pt2};
        return s;
    }

    public static ShapeDataObj makeRectangle(WorldPt pt1, int widthInPix, int heightInPix) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Rectangle);
        s._pts= new Pt[] {pt1};
        s._size1InPix= widthInPix;
        s._size2InPix= heightInPix;
        return s;
    }
    public static ShapeDataObj makeRectangle(ScreenPt pt1, int widthInPix, int heightInPix) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Rectangle);
        s._pts= new Pt[] {pt1};
        s._size1InPix= widthInPix;
        s._size2InPix= heightInPix;
        return s;
    }

    public static ShapeDataObj makeText(Pt pt, String text) {
        return makeText(null,pt,text);
    }

    public static ShapeDataObj makeText(OffsetScreenPt offPt, Pt pt, String text) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Text);
        s._pts= new Pt[] {pt};
        if (offPt!=null ) s._textOffset= offPt;
        s._text= text;
        return s;
    }



    @Override
    public Pt getCenterPt() { return _pts[0]; }

    public Pt[] getPts() { return _pts; }

    public void setStyle(Style s) { _style= s; }
    public Style getStyle() { return _style; }

    public ShapeType getShape() { return _sType; }

    public String getText() { return _text; }
    public void setText(String text) { _text= text; }

    public void setFontName(String font) { fontName = font; }
    public String getFontName() { return fontName; }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontWeight() {
        return fontWeight;
    }

    public void setFontWeight(String fontWeight) {
        this.fontWeight = fontWeight;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }

    public void setTextLocation(TextLocation textLocation) { _textLoc= textLocation; }

    public void setTextOffset(OffsetScreenPt textOffset) { _textOffset= textOffset; }
    public OffsetScreenPt getTextOffset() { return _textOffset; }

    public double getScreenDist(WebPlot plot, ScreenPt pt)
            throws ProjectionException {
        double dist = -1;

        ScreenPt testPt= plot.getScreenCoords(_pts[0]);
        if (testPt != null) {
            double dx= pt.getIX() - testPt.getIX();
            double dy= pt.getIY() - testPt.getIY();
            dist= Math.sqrt(dx*dx + dy*dy);
        }
        return dist;
    }


    public void draw(Graphics jg, WebPlot p, boolean front, AutoColor ac) throws UnsupportedOperationException {
        jg.deleteShapes(getShapes());
        Shapes shapes= drawShape(jg,p,front,ac);
        setShapes(shapes);
    }

    public void draw(Graphics g, boolean front, AutoColor ac) throws UnsupportedOperationException {
        throw new UnsupportedOperationException ("this type only supports drawing with WebPlot");
    }



    private Shapes drawShape(Graphics jg,
                             WebPlot plot,
                             boolean front,
                             AutoColor ac) {

        Shapes retval= null;
        String color= calculateColor(ac);
        Shape s= null;
        try {
            switch (_sType) {

                case Text:
                    s=drawText(jg,plot,color,_pts[0], _text);
                    retval= new Shapes(s);
                    break;
                case Line:
                    retval= drawLine(jg,plot,front,color);
                    break;
                case Circle:
                    retval= drawCircle(jg,plot,front,color);
                    break;
                case Rectangle:
                    retval= drawRectangle(jg,plot,front,color);
                    break;
            }
        } catch (ProjectionException e) {
            retval= null;
        }
        return retval;

    }

    private Shape drawText(Graphics jg,
                           WebPlot plot,
                           String  color,
                           Pt      inPt,
                           String  text ) throws ProjectionException {
        ViewPortPt pt= plot.getViewPortCoords(inPt);
        Shape s= null;
        if (plot.pointInViewPort(pt)) {
            int x= pt.getIX();
            int y= pt.getIY();
            if (x<2) x = 2;
            if (y<2) y = 2;

            int height = 0;
            try {
                height = (int)Float.parseFloat(fontSize.substring(0, fontSize.length()-2))*14/10;
            } catch (NumberFormatException e) {
                height= 12;
            }
            int width = height*_text.length()*8/20;
            if (_textOffset!=null) {
                x+= _textOffset.getIX();
                y+=_textOffset.getIY();

            }
            if (x<2) x = 2;
            if (y<2) y = 2;
            Dimension dim= plot.getViewPortDimension();
            int south = dim.getHeight() - height - 2;
            int east = dim.getWidth() - width - 2;

            if (x > east) x = east;
            if (y > south)  y = south;
            else if (y<height)  y= height;

            s= jg.drawText(color, fontName+FONT_FALLBACK, fontSize, fontWeight, fontStyle, x, y,text);
        }
        return s;
    }

    private Shapes drawLine(Graphics jg,
                          WebPlot plot,
                          boolean front,
                          String  color ) throws ProjectionException {

        Shape s;
        boolean inView= false;
        List<Shape> sList= new ArrayList<Shape>(4);
        ViewPortPt pt0= plot.getViewPortCoords(_pts[0]);
        ViewPortPt pt1= plot.getViewPortCoords(_pts[1]);
        if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1)) {
            inView= true;
            s= jg.drawLine(color, front, 1, pt0.getIX(), pt0.getIY(),
                           pt1.getIX(), pt1.getIY());
            sList.add(s);
        }

        if (_text!=null && inView) {
            ScreenPt textLocPt= makeTextLocation(plot, _pts[0], _pts[1]);
            s= drawText(jg, plot, color, plot.getViewPortCoords(textLocPt), _text);
            sList.add(s);
        }

        if (_style==Style.HANDLED && inView) {
            s= jg.fillRec(color,false,pt0.getIX()-2, pt0.getIY()-2, 5,5);
            sList.add(s);
            s= jg.fillRec(color,false,pt1.getIX()-2, pt1.getIY()-2, 5,5);
            sList.add(s);
        }
        return new Shapes(sList);
    }


    private Shapes drawCircle(Graphics jg,
                              WebPlot plot,
                              boolean front,
                              String  color ) throws ProjectionException {

        Shape s;
        boolean inView= false;
        List<Shape> sList= new ArrayList<Shape>(4);
        ViewPortPt textPt;
        if (_pts.length==1 && _size1InPix<Integer.MAX_VALUE) {
            ViewPortPt pt0= plot.getViewPortCoords(_pts[0]);
            textPt= pt0;
            if (plot.pointInViewPort(pt0)) {
                s= jg.drawCircle(color,front,1,pt0.getIX(),pt0.getIY(),_size1InPix );
                sList.add(s);
            }
        }
        else {
            ViewPortPt pt0= plot.getViewPortCoords(_pts[0]);
            ViewPortPt pt1= plot.getViewPortCoords(_pts[1]);
            textPt= pt1;
            if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1)) {
                inView= true;

                int xDist= Math.abs(pt0.getIX()-pt1.getIX())/2;
                int yDist= Math.abs(pt0.getIY()-pt1.getIY())/2;
                int radius= Math.min(xDist,yDist);

                int x= Math.min(pt0.getIX(),pt1.getIX()) + Math.abs(pt0.getIX()-pt1.getIX())/2;
                int y= Math.min(pt0.getIY(),pt1.getIY()) + Math.abs(pt0.getIY()-pt1.getIY())/2;


                s= jg.drawCircle(color,front,1,x,y,radius );
                sList.add(s);
            }
        }

        if (_text!=null && inView) {
            s= drawText(jg,plot,color,textPt, _text);
            sList.add(s);
        }
        if (_style==Style.HANDLED && inView) {
            // todo
        }
        return new Shapes(sList);
    }

    private Shapes drawRectangle(Graphics jg,
                                 WebPlot plot,
                                 boolean front,
                                 String  color ) throws ProjectionException {

        Shape s;
        boolean inView= false;
        List<Shape> sList= new ArrayList<Shape>(4);
        ViewPortPt textPt;
        if (_pts.length==1 && _size1InPix<Integer.MAX_VALUE && _size2InPix<Integer.MAX_VALUE) {
            ViewPortPt pt0= plot.getViewPortCoords(_pts[0]);
            textPt= pt0;
            if (plot.pointInViewPort(pt0)) {
                int x= pt0.getIX();
                int y= pt0.getIY();
                int w= _size1InPix;
                int h= _size2InPix;
                if (h<0) {
                    h*=-1;
                    y-=h;
                }
                if (w<0) {
                    w*=-1;
                    x-=w;
                }
                s= jg.drawRec(color,front,1,x,y,w,h);
                sList.add(s);
            }

        }
        else {
            ViewPortPt pt0= plot.getViewPortCoords(_pts[0]);
            ViewPortPt pt1= plot.getViewPortCoords(_pts[1]);
            textPt= pt1;
            if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1)) {
                inView= true;

                int x= pt0.getIX();
                int y= pt0.getIY();
                int width=  pt1.getIX()-pt0.getIX();
                int height=  pt1.getIY()-pt0.getIY();
                s= jg.drawRec(color,front,1,x,y,width,height);
                sList.add(s);
            }
        }

        if (_text!=null && inView) {
            s= drawText(jg,plot,color,textPt, _text);
            sList.add(s);
        }
        if (_style==Style.HANDLED && inView) {
            // todo
        }
        return new Shapes(sList);
    }


    @Override
    public List<Region> toRegion(WebPlot   plot,
                                 AutoColor ac) {
        List<Region> retList= new ArrayList<Region>(10);
        String color= calculateColor(ac);
        try {
            switch (_sType) {

                case Text:
                    makeTextRegion(retList, _pts[0], plot,color);
                    break;
                case Line:
                    makeLineRegion(retList,plot,color);
                    break;
                case Circle:
                    makeCircleRegion(retList,plot,color);
                    break;
                case Rectangle:
                    makeRectangleRegion(retList,plot,color);
                    break;
            }
        } catch (ProjectionException e) {
            // ignore - just return empty list
        }
        return retList;
    }

    private void makeTextRegion( List<Region> retList,
                                 Pt           inPt,
                                 WebPlot      plot,
                                 String       color) throws ProjectionException {
        if (_text==null) return;
        WorldPt wp= plot.getWorldCoords(inPt);
        RegionText rt= new RegionText(wp);
        RegionOptions op= rt.getOptions();
        op.setColor(color);
        op.setText(makeNonHtml(_text));
        if (_textOffset!=null) {
            op.setOffsetX(_textOffset.getIX());
            op.setOffsetY(_textOffset.getIY());
        }
        retList.add(rt);
    }

    private void makeCircleRegion(List<Region> retList,
                                  WebPlot      plot,
                                  String       color) throws ProjectionException {
        WorldPt textPt;
        int radius;
        WorldPt wp;
        if (_pts.length==1 && _size1InPix<Integer.MAX_VALUE) {
            wp= plot.getWorldCoords(_pts[0]);
            radius= _size1InPix;
            textPt= wp;
        }
        else {
            wp= getCenter(plot);
            radius= findRadius(plot);
            textPt= plot.getWorldCoords(_pts[1]);
        }
        RegionAnnulus ra= new RegionAnnulus(wp,new RegionValue(radius, RegionValue.Unit.SCREEN_PIXEL));
        ra.getOptions().setColor(color);
        retList.add(ra);

        makeTextRegion(retList,textPt,plot,color);
    }

    private void makeRectangleRegion(List<Region> retList,
                                     WebPlot      plot,
                                     String       color) throws ProjectionException {

        WorldPt textPt;
        WorldPt wp;
        int w;
        int h;
        if (_pts.length==1 && _size1InPix<Integer.MAX_VALUE && _size2InPix<Integer.MAX_VALUE) {
            ScreenPt pt0= plot.getScreenCoords(_pts[0]);
            int x= pt0.getIX();
            int y= pt0.getIY();
            w= _size1InPix;
            h= _size2InPix;
            if (h<0) {
                h*=-1;
                y-=h;
            }
            if (w<0) {
                w*=-1;
                x-=w;
            }
            wp= plot.getWorldCoords(new ScreenPt(x,y));
            textPt= wp;
        }
        else {
            ScreenPt pt0= plot.getScreenCoords(_pts[0]);
            ScreenPt pt1= plot.getScreenCoords(_pts[1]);
            int x= pt0.getIX();
            int y= pt0.getIY();
            w=  pt1.getIX()-pt0.getIX();
            h=  pt1.getIY()-pt0.getIY();
            wp= plot.getWorldCoords(new ScreenPt(x,y));
            textPt= plot.getWorldCoords(pt1);
        }
        RegionDimension dim= new RegionDimension(new RegionValue(w, RegionValue.Unit.SCREEN_PIXEL),
                                                 new RegionValue(h, RegionValue.Unit.SCREEN_PIXEL));
        RegionBox rb= new RegionBox(wp,dim,new RegionValue(0, RegionValue.Unit.SCREEN_PIXEL));
        rb.getOptions().setColor(color);
        retList.add(rb);
        makeTextRegion(retList, textPt, plot, color);
    }

    private void makeLineRegion(List<Region> retList,
                                WebPlot      plot,
                                String       color) throws ProjectionException {
        ScreenPt pt0= plot.getScreenCoords(_pts[0]);
        ScreenPt pt1= plot.getScreenCoords(_pts[1]);
        WorldPt wp0= plot.getWorldCoords(_pts[0]);
        WorldPt wp1= plot.getWorldCoords(_pts[1]);

        RegionLines rl= new RegionLines(wp0,wp1);
        rl.getOptions().setColor(color);
        retList.add(rl);

        if (_text!=null) {
            ScreenPt textPt= makeTextLocation(plot, _pts[0], _pts[1]);
            makeTextRegion(retList, plot.getWorldCoords(textPt), plot, color);
        }

    }

    private ScreenPt makeTextLocation(WebPlot plot, Pt inPt0, Pt inPt1) throws ProjectionException {
        int height;
        ScreenPt pt0= plot.getScreenCoords(inPt0);
        ScreenPt pt1= plot.getScreenCoords(inPt1);
        try {
            height = (int)Float.parseFloat(fontSize.substring(0, fontSize.length()-2))*14/10;
        } catch (NumberFormatException e) {
            height= 12;
        }
        int x = pt1.getIX()+5;
        int y = pt1.getIY()+5;

        if (_textLoc==TextLocation.MID_POINT || _textLoc==TextLocation.MID_POINT_OR_BOTTOM ||
                _textLoc==TextLocation.MID_POINT_OR_TOP) {
            double dist= VisUtil.computeDistance(pt1,pt0);
            if (_textLoc==TextLocation.MID_POINT_OR_BOTTOM && dist<100) {
                _textLoc= TextLocation.BOTTOM;
            }
            if (_textLoc==TextLocation.MID_POINT_OR_TOP && dist<80) {
                _textLoc= TextLocation.TOP;
            }
        }

        switch (_textLoc) {
            case TOP:
                y= pt1.getIY()- (height+5);
                break;
            case BOTTOM:
                break;
            case MID_POINT:
            case MID_POINT_OR_BOTTOM:
            case MID_POINT_OR_TOP:
                x= (pt1.getIX()+pt0.getIX())/2;
                y= (pt1.getIY()+pt0.getIY())/2;
                break;
        }

        return new ScreenPt(x,y);

    }


    private int findRadius(WebPlot plot) throws ProjectionException {
        ScreenPt pt0= plot.getScreenCoords(_pts[0]);
        ScreenPt pt1= plot.getScreenCoords(_pts[1]);
        int xDist= Math.abs(pt0.getIX()-pt1.getIX())/2;
        int yDist= Math.abs(pt0.getIY()-pt1.getIY())/2;
        return Math.min(xDist,yDist);
    }

    private WorldPt getCenter(WebPlot plot) throws ProjectionException {
        ScreenPt pt0= plot.getScreenCoords(_pts[0]);
        ScreenPt pt1= plot.getScreenCoords(_pts[1]);
        int x= Math.min(pt0.getIX(),pt1.getIX()) + Math.abs(pt0.getIX()-pt1.getIX())/2;
        int y= Math.min(pt0.getIY(),pt1.getIY()) + Math.abs(pt0.getIY()-pt1.getIY())/2;
        return plot.getWorldCoords(new ScreenPt(x,y));
    }

    private String makeNonHtml(String s) {
        String retval= s;
        if (s.endsWith(HTML_DEG)) {
            retval= s.substring(0,s.indexOf(HTML_DEG)) + " deg";
        }
        return retval;
    }
}