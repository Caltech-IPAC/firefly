package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.hydro4ge.raphaelgwt.client.PathBuilder;
import com.hydro4ge.raphaelgwt.client.RaphaelJS;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * User: roby
 * Date: Mar 26, 2010
 * Time: 10:35:21 AM
 */


/**
 * Use the raphael javascript library
 * This is wrapped on top of the com.hydro4ge.raphaelgwt.client.RaphaelJS, which only a java script wrapper around raphael
 * Uses com.hydro4ge.raphaelgwt module. The model has a higher level class that we do not use.
 * @author Trey Roby
 */
public class RaphaelGraphics implements Graphics {


    private final List <GWTLabelShape> _labelList= new ArrayList<GWTLabelShape>(20);
    private final RaphaelCanvas _canvas;

    public RaphaelGraphics() {
        _canvas= new RaphaelCanvas();
    }
    public Widget getWidget() { return _canvas; }


    public Shape drawLine(String color, int sx, int sy, int ex, int ey) {
        return drawLine(color,DEF_WIDTH,sx,sy,ex,ey);
    }

    public Shape drawLine(String color, int lineWidth, int sx, int sy, int ex, int ey) {
        PathBuilder pb= new PathBuilder().M(sx,sy).L(ex,ey);
        return drawPath(pb,color,lineWidth,false);
    }

    public Shape drawCircle(String color, int lineWidth, int x, int y, int radius) {
        if (GwtUtil.isHexColor(color))  color= "#" + color;
        RaphaelJS.Element e= _canvas.circle(x,y,radius);
        e.attr("stroke" , color);
        e.attr("stroke-width" , lineWidth);
        return new RShape(e);
    }

    private Shape drawPath(PathBuilder pb,
                           String color,
                           int lineWidth,
                           boolean fill) {
        if (GwtUtil.isHexColor(color))  color= "#" + color;
        RaphaelJS.Element e= _canvas.makePath(pb);
        e.attr("stroke" , color);
        if (fill) {
            e.attr("fill" , color);
        }
        else {
            e.attr("stroke-width" , lineWidth);
        }
        return new RShape(e);
    }

    public Shape drawRec(String color,
                         int lineWidth,
                         int x,
                         int y,
                         int width,
                         int height) {
        PathBuilder pb= new PathBuilder().M(x,y).h(width).v(height).h(-width).Z();
        return drawPath(pb,color,lineWidth,false);
    }

    public Shape fillRec(String color,
                         int x,
                         int y,
                         int width,
                         int height) {
        PathBuilder pb= new PathBuilder().M(x,y).h(width).v(height).h(-width).Z();
        return drawPath(pb,color,1,true);
    }

    public Shape drawText(String color, String size, int x, int y, String text) {
        return drawText(color, "inherit", size, "normal",  "normal", x, y, text);
    }

    public Shape drawText(String color,
                          String fontFamily,
                          String size,
                          String fontWeight,
                          String fontStyle,
                          int x,
                          int y,
                          String text) {
         HTML label= new HTML(text);
        com.google.gwt.user.client.Element e= label.getElement();
        DOM.setStyleAttribute(e, "color", color);
        DOM.setStyleAttribute(e, "fontFamily", fontFamily);
        DOM.setStyleAttribute(e, "fontSize", size);
        DOM.setStyleAttribute(e, "fontWeight", fontWeight);
        DOM.setStyleAttribute(e, "fontStyle", fontStyle);
        DOM.setStyleAttribute(e, "backgroundColor", "white");
        DOM.setStyleAttribute(e, "MozBorderRadius", "5px");
        DOM.setStyleAttribute(e, "borderRadius", "5px");
        DOM.setStyleAttribute(e, "webkitBorderRadius", "5px");

        GWTLabelShape labelShape= new GWTLabelShape(label);
        _labelList.add(labelShape);
        _canvas.addLabel(label,x,y);
        return labelShape;


    }

    public void deleteShapes(Shapes shapes) {
        if (shapes!=null) {
            for(edu.caltech.ipac.firefly.visualize.draw.Shape s : shapes) {
                if (s instanceof RShape) {
                    if (((RShape)s).getShape()!=null) {
                        try {
                            ((RShape)s).getShape().remove();
                        } catch (JavaScriptException ignore) {}
                    }
                }
                else if (s instanceof GWTLabelShape) {
                    GWTLabelShape ls= (GWTLabelShape)s;
                    if (_labelList.contains(ls)) {
                        _labelList.remove(ls);
                        _canvas.removeLabel(ls.getLabel());
                    }

                }
            }
        }
    }

    public void clear() {
        _canvas.clear();
        for(GWTLabelShape label : _labelList) {
            _canvas.removeLabel(label.getLabel());
        }
    }

    public void paint() { }

    public void setDrawingAreaSize(int width, int height) {
        _canvas.setPixelSize(width,height);
    }

    public boolean getSupportsPartialDraws() { return true; }

    public static class RShape extends edu.caltech.ipac.firefly.visualize.draw.Shape<RaphaelGraphics>  {
        final RaphaelJS.Element _s;
        RShape(RaphaelJS.Element s) { _s= s;  }

        RaphaelJS.Element getShape() { return _s; }

        public void draw(RaphaelGraphics surfaceWidget) { }
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
