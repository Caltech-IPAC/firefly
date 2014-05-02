package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.visualize.ScreenPt;

import java.util.List;

/**
 * @author Trey Roby
 * @version $Id: JSGraphics.java,v 1.15 2012/02/17 23:00:20 roby Exp $
 */
public class JSGraphics implements Graphics {

    public static final String DRAWER_NAME = "DrawerLayer-";
    private JavaScriptObject _jg= null;
    private Widget _widget;
    String _name= makeName();
    public static int _cnt= 0;



    public JSGraphics() {
        _widget= new SimplePanel();
        DOM.setElementProperty(_widget.getElement(),"id", _name);
    }

    public Widget getWidget() { return _widget; }

    public boolean init() {
        boolean retval= true;
        try {
            if (_jg==null) _jg= createJSGraphics(_name);
        } catch (Exception e) {
            retval= false;
        }
        return retval;
    }


    private static String makeName() {
        return DRAWER_NAME+ (_cnt++);
    }


    public void drawLine(String color,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {
        drawLine(color,DEF_WIDTH,sx,sy,ex,ey);
    }

    public void drawLine(String color,
                         int lineWidth,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {

        if (GwtUtil.isHexColor(color))  color= "#" + color;
        drawLine(_jg,color,lineWidth,sx,sy,ex,ey);
    }

    public void drawPath(String color, int lineWidth, List<ScreenPt> pts, boolean close) {  }

    public void drawCircle(String color, int lineWidth, int x, int y, int radius) {
        if (GwtUtil.isHexColor(color))  color= "#" + color;
        // modify  the x and y since JSGraphics wants the top, left corner
        x-=radius;
        y-=radius;
        int diameter= radius*2;
        drawCircle(_jg,color,lineWidth,x,y,diameter,diameter);
    }

    public void drawRec(String color,
                        int lineWidth,
                        int x,
                        int y,
                        int width,
                        int height) {


        int nx= x;
        int ny= y;

        if (width<0) {
            nx= x+width;
            width= -1*width;
        }
        if (height<0) {
            ny= y+height;
            height= -1*height;
        }
        if (GwtUtil.isHexColor(color))  color= "#" + color;

        if (BrowserUtil.isIE()) {
            drawLine(_jg, color, lineWidth, x-width,y-height, x+width, y-height);
            drawLine(_jg, color, lineWidth, x+width,y-height, x+width, y+height);
            drawLine(_jg, color, lineWidth, x+width,y+height, x-width, y+height);
            drawLine(_jg, color, lineWidth, x-width,y+height, x-width, y-height);
        }
        else {
            drawRec(_jg,color,lineWidth,nx,ny,width,height);
        }
    }




    public void fillRec(String color,
                        int x,
                        int y,
                        int width,
                        int height) {

        int nx= x;
        int ny= y;

        if (width<0) {
            nx= x+width;
            width= -1*width;
        }
        if (height<0) {
            ny= y+height;
            height= -1*height;
        }
        if (GwtUtil.isHexColor(color))  color= "#" + color;
        fillRec(_jg,color,nx,ny,width,height);
    }




    public void fillArc(String color,
                        int x,
                        int y,
                        int width,
                        int height,
                        float startAngle,
                        float endAngle) {
        // not supported - do nothing
    }

    public void drawText(String color, String size, int x, int y, String text) {
        if (GwtUtil.isHexColor(color))  color= "#" + color;
        drawText(_jg,color,size,x,y,text);
    }

    /**
     * passes through to the simplier drawText, ignores family, weight, style
     *
     * @param color
     * @param fontFamily
     * @param size
     * @param fontWeight
     * @param fontStyle
     * @param x
     * @param y
     * @param text
     * @return a null
     */
    public void drawText(String color,
                         String fontFamily,
                         String size,
                         String fontWeight,
                         String fontStyle,
                         int x,
                         int y,
                         String text) {
        drawText(color,size,x,y,text);
    }

    public void clear() { clear(_jg); }
    public void paint() { paint(_jg); }

    public void setDrawingAreaSize(int width, int height) { }

    public native JavaScriptObject createJSGraphics(String canvas) /*-{
           var jg = new $wnd.jsGraphics(canvas);
          jg.setStroke(2);
           return jg;
     }-*/;

    public native void drawLine(JavaScriptObject jg,
                                String color,
                                int lineWidth,
                                int sx,
                                int sy,
                                int ex,
                                int ey) /*-{
           jg.setStroke(lineWidth);
           jg.setColor(color);
           jg.drawLine(sx, sy, ex, ey); // co-ordinates related to canvas
    }-*/;

    public native void drawCircle(JavaScriptObject jg,
                                  String color,
                                  int lineWidth,
                                  int x,
                                  int y,
                                  int width,
                                  int height) /*-{
        jg.setStroke(lineWidth);
        jg.setColor(color);
        jg.drawEllipse(x, y, width, height); // co-ordinates related to canvas
    }-*/;

    public native void fillArc(JavaScriptObject jg,
                               String color,
                               int x,
                               int y,
                               int width,
                               int height,
                               float startAngle,
                               float endAngle) /*-{
        jg.setColor(color);
        jg.fillArc(x,y,width,height,startAngle,endAngle);
    }-*/;

    public native void drawRec(JavaScriptObject jg,
                               String color,
                               int lineWidth,
                               int x,
                               int y,
                               int width,
                               int height) /*-{
          jg.setStroke(lineWidth);
          jg.setColor(color);
          jg.drawRect(x, y, width, height); // co-ordinates related to canvas

    }-*/;


    public native void fillRec(JavaScriptObject jg,
                               String color,
                               int x,
                               int y,
                               int width,
                               int height) /*-{
          jg.setColor(color);
          jg.fillRect(x, y, width, height); // co-ordinates related to canvas

    }-*/;




    public native void drawText(JavaScriptObject jg,
                                String color,
                                String size,
                               int x,
                               int y,
                               String text) /*-{
          jg.setFont("sans-serif",size,$wnd.Font.PLAIN);
          jg.setColor(color);
          jg.drawString(text, x, y); // co-ordinates related to canvas

    }-*/;




    public native void clear(JavaScriptObject jg) /*-{
           jg.clear();
    }-*/;

    public native void paint(JavaScriptObject jg) /*-{
           jg.paint();
    }-*/;



}