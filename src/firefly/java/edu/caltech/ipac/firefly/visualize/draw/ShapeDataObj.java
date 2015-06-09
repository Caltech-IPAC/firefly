/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

    public static final int DEF_WIDTH= 1;

    public enum TextLocation {
        DEFAULT,
        LINE_TOP,
        LINE_BOTTOM,
        LINE_MID_POINT,
        LINE_MID_POINT_OR_BOTTOM,
        LINE_MID_POINT_OR_TOP,
        CIRCLE_NE,
        CIRCLE_NW,
        CIRCLE_SE,
        CIRCLE_SW,
        CENTER} // use MID_X, MID_X_LONG, MID_Y, MID_Y_LONG for vertical or horizontal lines
//    public static final int DEF_OFFSET= 15;
    public static final String FONT_SIZE = "9pt";
    private static final String FONT_FALLBACK= ",sans-serif";
    public static final String HTML_DEG= "&deg;";

    public enum Style {STANDARD,HANDLED}
    public enum UnitType {PIXEL,ARCSEC,IMAGE_PIXEL}
    public enum ShapeType {Line, Text,Circle, Rectangle}

    private Pt _pts[];
    private String _text= null;
    private final ShapeType _sType;
    private String fontName = "helvetica";
    private String fontSize = FONT_SIZE;
    private String fontWeight = "normal";
    private String fontStyle = "normal";
    private Style _style= Style.STANDARD;
    private int _size1= Integer.MAX_VALUE;
    private int _size2= Integer.MAX_VALUE;
    private UnitType unitType = UnitType.PIXEL; // only supported by Circle so far
    private TextLocation _textLoc= TextLocation.DEFAULT;
    private OffsetScreenPt _textOffset= null;
    private int lineWidth= DEF_WIDTH;

     private ShapeDataObj(ShapeType sType) {
         super();
         _sType= sType;
    }

    @Override
    public boolean getCanUsePathEnabledOptimization() {
        return _style==Style.STANDARD && (_sType==ShapeType.Line || _sType==ShapeType.Rectangle);
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
        return makeCircle(pt1,radiusInPix, UnitType.PIXEL);
    }
    public static ShapeDataObj makeCircle(Pt pt1, int radius, UnitType unitType) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Circle);
        s._pts= new Pt[] {pt1};
        s._size1 = radius;
        s.unitType = unitType;
        return s;
    }

    public static ShapeDataObj makeRectangle(Pt pt1, Pt pt2) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Rectangle);
        s._pts= new Pt[] {pt1, pt2};
        return s;
    }

    public static ShapeDataObj makeRectangle(Pt pt1, int width, int height, UnitType unitType) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Rectangle);
        s._pts= new Pt[] {pt1};
        s._size1 = width;
        s._size2 = height;
        s.unitType= unitType;
        return s;
    }
    public static ShapeDataObj makeRectangle(Pt pt1, int widthInPix, int heightInPix) {
        ShapeDataObj s= new ShapeDataObj(ShapeType.Rectangle);
        s._pts= new Pt[] {pt1};
        s._size1 = widthInPix;
        s._size2 = heightInPix;
        s.unitType= UnitType.PIXEL;
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
    public int getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(int w) {
        lineWidth= w;
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

    public double getScreenDist(WebPlot plot, ScreenPt pt) {
        double dist = -1;

        ScreenPt testPt= plot.getScreenCoords(_pts[0]);
        if (testPt != null) {
            double dx= pt.getIX() - testPt.getIX();
            double dy= pt.getIY() - testPt.getIY();
            dist= Math.sqrt(dx*dx + dy*dy);
        }
        return dist;
    }


    public void draw(Graphics jg, WebPlot p, AutoColor ac, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        drawShape(jg,p,ac,useStateColor,onlyAddToPath);
    }

    public void draw(Graphics g, AutoColor ac, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        throw new UnsupportedOperationException ("this type only supports drawing with WebPlot");
    }



    private void drawShape(Graphics jg,
                             WebPlot plot,
                             AutoColor ac,
                             boolean useStateColor,
                             boolean onlyAddToPath) {

        String color= calculateColor(ac,useStateColor);
        switch (_sType) {

            case Text:
                drawText(jg,plot,color,_pts[0], _text);
                break;
            case Line:
                drawLine(jg,plot,color,onlyAddToPath);
                break;
            case Circle:
                drawCircle(jg,plot,color);
                break;
            case Rectangle:
                drawRectangle(jg,plot,color,onlyAddToPath);
                break;
        }
    }

    private void drawText(Graphics jg,
                          WebPlot  plot,
                          String   color,
                          Pt       inPt,
                          String   text ) {

        if (inPt==null) return;
        ViewPortPt pt= plot.getViewPortCoords(inPt);
        if (plot.pointInViewPort(pt)) {
            int x= pt.getIX();
            int y= pt.getIY();
            if (x<2) x = 2;
            if (y<2) y = 2;

            int height;
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

            jg.drawText(color, fontName+FONT_FALLBACK, fontSize, fontWeight, fontStyle, x, y,text);
        }
    }

    private void drawLine(Graphics g, WebPlot plot, String  color, boolean onlyAddToPath ) {
        boolean inView= false;
        ViewPortPt pt0= plot.getViewPortCoords(_pts[0]);
        ViewPortPt pt1= plot.getViewPortCoords(_pts[1]);
        if (pt0==null || pt1==null) return;
        if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1)) {
            inView= true;
            if (!onlyAddToPath || _style==Style.HANDLED) g.beginPath(color,lineWidth);
//            g.drawLine(color, lineWidth, pt0.getIX(), pt0.getIY(), pt1.getIX(), pt1.getIY());
            g.pathMoveTo(pt0.getIX(), pt0.getIY());
            g.pathLineTo(pt1.getIX(), pt1.getIY());
            if (!onlyAddToPath || _style==Style.HANDLED) g.drawPath();
        }

        if (_text!=null && inView) {
            ScreenPt textLocPt= makeTextLocationLine(plot, _pts[0], _pts[1]);
            drawText(g, plot, color, plot.getViewPortCoords(textLocPt), _text);
        }

        if (_style==Style.HANDLED && inView) {
            g.fillRec(color,pt0.getIX()-2, pt0.getIY()-2, 5,5);
            g.fillRec(color,pt1.getIX()-2, pt1.getIY()-2, 5,5);
        }
    }

    private static int getValueInScreenPixel(WebPlot plot, int arcsecValue) {
        double retval;
        if (plot!=null) {
            retval= arcsecValue/(plot.getImagePixelScaleInArcSec()/plot.getZoomFact());
        }
        else {
            retval= arcsecValue;
        }
        return retval<2 ? 2 : (int)Math.rint(retval);
    }

    private void drawCircle(Graphics g, WebPlot plot, String  color) {
        boolean inView= false;
        int screenRadius= 1;
        ViewPortPt centerPt=null;

        if (_pts.length==1 && _size1 <Integer.MAX_VALUE) {
            switch (unitType) {
                case PIXEL: screenRadius= _size1; break;
                case IMAGE_PIXEL: screenRadius= (int)(plot.getZoomFact()*_size1); break;
                case ARCSEC: screenRadius= getValueInScreenPixel(plot,_size1); break;
            }
            centerPt= plot.getViewPortCoords(_pts[0]);
            if (plot.pointInViewPort(centerPt)) {
                g.drawCircle(color,lineWidth,centerPt.getIX(),centerPt.getIY(), screenRadius);
                inView= true;
            }
        }
        else {
            ViewPortPt pt0= plot.getViewPortCoords(_pts[0]);
            ViewPortPt pt1= plot.getViewPortCoords(_pts[1]);
            if (pt0==null || pt1==null) return;
            if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1)) {
                inView= true;

                int xDist= Math.abs(pt0.getIX()-pt1.getIX())/2;
                int yDist= Math.abs(pt0.getIY()-pt1.getIY())/2;
                screenRadius= Math.min(xDist,yDist);

                int x= Math.min(pt0.getIX(),pt1.getIX()) + Math.abs(pt0.getIX()-pt1.getIX())/2;
                int y= Math.min(pt0.getIY(),pt1.getIY()) + Math.abs(pt0.getIY()-pt1.getIY())/2;
                centerPt= new ViewPortPt(x,y);

                g.drawCircle(color,lineWidth,x,y,screenRadius );
            }
        }

        if (_text!=null && inView && centerPt!=null) {
            ScreenPt textPt= makeTextLocationCircle(plot,centerPt,screenRadius);
            drawText(g,plot,color,textPt, _text);
        }
        if (_style==Style.HANDLED && inView) {
            // todo
        }
    }

    private void drawRectangle(Graphics g, WebPlot plot, String  color, boolean onlyAddToPath ) {

        boolean inView= false;
        ViewPortPt textPt;
        if (_pts.length==1 && _size1 <Integer.MAX_VALUE && _size2 <Integer.MAX_VALUE) {
            ViewPortPt pt0= plot.getViewPortCoords(_pts[0]);
            textPt= pt0;
            if (plot.pointInViewPort(pt0)) {
                inView= true;
                int w;
                int h;

                switch (unitType) {
                    case PIXEL:
                        w= _size1;
                        h= _size2;
                        break;
                    case ARCSEC:
                        w= getValueInScreenPixel(plot,_size1);
                        h= getValueInScreenPixel(plot,_size2);
                        break;
                    case IMAGE_PIXEL:
                        double scale= plot.getZoomFact();
                        w= (int)(scale*_size1);
                        h= (int)(scale*_size2);
                    break;
                    default:
                        w= _size1;
                        h= _size2;
                        break;
                }


                int x= pt0.getIX();
                int y= pt0.getIY();
                if (h<0) {
                    h*=-1;
                    y-=h;
                }
                if (w<0) {
                    w*=-1;
                    x-=w;
                }
                if (!onlyAddToPath || _style==Style.HANDLED) g.beginPath(color,lineWidth);
                g.rect(x,y,w,h);
                if (!onlyAddToPath || _style==Style.HANDLED) g.drawPath();
            }

        }
        else {
            ViewPortPt pt0= plot.getViewPortCoords(_pts[0]);
            ViewPortPt pt1= plot.getViewPortCoords(_pts[1]);
            if (pt0==null || pt1==null) return;
            textPt= pt1;
            if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1)) {
                inView= true;

                int x= pt0.getIX();
                int y= pt0.getIY();
                int width=  pt1.getIX()-pt0.getIX();
                int height=  pt1.getIY()-pt0.getIY();
                if (!onlyAddToPath || _style==Style.HANDLED) g.beginPath(color,lineWidth);
                g.rect(x,y,width,height);
                if (!onlyAddToPath || _style==Style.HANDLED) g.drawPath();
            }
        }

        if (_text!=null && inView) {
            drawText(g,plot,color,textPt, _text);
        }
        if (_style==Style.HANDLED && inView) {
            // todo
        }
    }


    @Override
    public List<Region> toRegion(WebPlot   plot, AutoColor ac) {
        List<Region> retList= new ArrayList<Region>(10);
        String color= calculateColor(ac,false);
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
        return retList;
    }

    private void makeTextRegion( List<Region> retList,
                                 Pt           inPt,
                                 WebPlot      plot,
                                 String       color) {
        if (_text==null || inPt==null) return;
        WorldPt wp= plot.getWorldCoords(inPt);
        if (wp==null) return;
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
                                  String       color) {
        int radius;
        WorldPt wp;
        ScreenPt textPtScreen;
        UnitType st= UnitType.PIXEL;
        if (_pts.length==1 && _size1 <Integer.MAX_VALUE) {
            st= this.unitType;
            wp= plot.getWorldCoords(_pts[0]);
            radius= _size1;
            textPtScreen= makeTextLocationCircle(plot,wp, _size1);
        }
        else {
            wp= getCenter(plot);
            radius= findRadius(plot);
            textPtScreen= makeTextLocationCircle(plot,wp,radius);
        }

        if (wp!=null) {
            RegionAnnulus ra= new RegionAnnulus(wp,new RegionValue(radius,
                                   st== UnitType.PIXEL ? RegionValue.Unit.SCREEN_PIXEL :RegionValue.Unit.ARCSEC));
            ra.getOptions().setColor(color);
            retList.add(ra);

            WorldPt textPt= plot.getWorldCoords(textPtScreen);
            makeTextRegion(retList,textPt,plot,color);
        }
    }

    private void makeRectangleRegion(List<Region> retList,
                                     WebPlot      plot,
                                     String       color) {

        WorldPt textPt;
        WorldPt wp;
        int w;
        int h;
        if (_pts.length==1 && _size1 <Integer.MAX_VALUE && _size2 <Integer.MAX_VALUE) {
            ScreenPt pt0= plot.getScreenCoords(_pts[0]);
            if (pt0==null) return;
            int x= pt0.getIX();
            int y= pt0.getIY();
            w= _size1;
            h= _size2;
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
            if (pt0==null || pt1==null) return;
            int x= pt0.getIX();
            int y= pt0.getIY();
            w=  pt1.getIX()-pt0.getIX();
            h=  pt1.getIY()-pt0.getIY();
            wp= plot.getWorldCoords(new ScreenPt(x,y));
            textPt= plot.getWorldCoords(pt1);
        }

        if (wp!=null) {
            RegionDimension dim= new RegionDimension(new RegionValue(w, RegionValue.Unit.SCREEN_PIXEL),
                                                     new RegionValue(h, RegionValue.Unit.SCREEN_PIXEL));
            RegionBox rb= new RegionBox(wp,dim,new RegionValue(0, RegionValue.Unit.SCREEN_PIXEL));
            rb.getOptions().setColor(color);
            retList.add(rb);
            makeTextRegion(retList, textPt, plot, color);
        }
    }

    private void makeLineRegion(List<Region> retList,
                                WebPlot      plot,
                                String       color) {
        WorldPt wp0= plot.getWorldCoords(_pts[0]);
        WorldPt wp1= plot.getWorldCoords(_pts[1]);

        if (wp0==null || wp1==null) {
            RegionLines rl= new RegionLines(wp0,wp1);
            rl.getOptions().setColor(color);
            retList.add(rl);

            if (_text!=null) {
                ScreenPt textPt= makeTextLocationLine(plot, _pts[0], _pts[1]);
                makeTextRegion(retList, plot.getWorldCoords(textPt), plot, color);
            }
        }


    }

    private ScreenPt makeTextLocationCircle(WebPlot plot, Pt centerPt, int screenRadius) {
        ScreenPt scrCenPt= plot.getScreenCoords(centerPt);
        if (scrCenPt==null && screenRadius<1) return null;
        OffsetScreenPt opt;
        switch (_textLoc) {
            case CIRCLE_NE:
                opt= new OffsetScreenPt(-1*screenRadius, -1*(screenRadius+10));
                break;
            case CIRCLE_NW:
                opt= new OffsetScreenPt(screenRadius, -1*(screenRadius));
                break;
            case CIRCLE_SE:
                opt= new OffsetScreenPt(-1*screenRadius, screenRadius+5);
                break;
            case CIRCLE_SW:
                opt= new OffsetScreenPt(screenRadius, screenRadius);
                break;
            default:
                opt= new OffsetScreenPt(0,0);
                break;
        }
        return new ScreenPt(scrCenPt.getIX()+opt.getIX(), scrCenPt.getIY()+opt.getIY());

    }



    private ScreenPt makeTextLocationLine(WebPlot plot, Pt inPt0, Pt inPt1) {
        if (inPt0==null || inPt1==null) return null;
        int height;
        ScreenPt pt0= plot.getScreenCoords(inPt0);
        ScreenPt pt1= plot.getScreenCoords(inPt1);

        if (pt0==null || pt1==null) return null;
        try {
            height = (int)Float.parseFloat(fontSize.substring(0, fontSize.length()-2))*14/10;
        } catch (NumberFormatException e) {
            height= 12;
        }
        int x = pt1.getIX()+5;
        int y = pt1.getIY()+5;

        if (_textLoc==TextLocation.LINE_MID_POINT || _textLoc==TextLocation.LINE_MID_POINT_OR_BOTTOM ||
                _textLoc==TextLocation.LINE_MID_POINT_OR_TOP) {
            double dist= VisUtil.computeDistance(pt1,pt0);
            if (_textLoc==TextLocation.LINE_MID_POINT_OR_BOTTOM && dist<100) {
                _textLoc= TextLocation.LINE_BOTTOM;
            }
            if (_textLoc==TextLocation.LINE_MID_POINT_OR_TOP && dist<80) {
                _textLoc= TextLocation.LINE_TOP;
            }
        }

        switch (_textLoc) {
            case LINE_TOP:
                y= pt1.getIY()- (height+5);
                break;
            case LINE_BOTTOM:
            case DEFAULT:
                break;
            case LINE_MID_POINT:
            case LINE_MID_POINT_OR_BOTTOM:
            case LINE_MID_POINT_OR_TOP:
                x= (pt1.getIX()+pt0.getIX())/2;
                y= (pt1.getIY()+pt0.getIY())/2;
                break;
            default:
                break;
        }

        return new ScreenPt(x,y);

    }


    private int findRadius(WebPlot plot) {
        int retval= -1;
        ScreenPt pt0= plot.getScreenCoords(_pts[0]);
        ScreenPt pt1= plot.getScreenCoords(_pts[1]);
        if (pt0!=null && pt1!=null) {
            int xDist= Math.abs(pt0.getIX()-pt1.getIX())/2;
            int yDist= Math.abs(pt0.getIY()-pt1.getIY())/2;
            retval= Math.min(xDist,yDist);
        }
        return retval;
    }

    private WorldPt getCenter(WebPlot plot) {
        ScreenPt pt0= plot.getScreenCoords(_pts[0]);
        ScreenPt pt1= plot.getScreenCoords(_pts[1]);
        if (pt0==null || pt1==null) return null;
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