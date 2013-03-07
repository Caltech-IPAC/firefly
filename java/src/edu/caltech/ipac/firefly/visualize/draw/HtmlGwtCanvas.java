package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * User: roby
 * Date: Oct 1, 2008
 * Time: 11:21:49 AM
 */


/**
 * @author Trey Roby
 */
public class HtmlGwtCanvas implements Graphics {

    private final List<CanvasShape> _shapeList= new ArrayList<CanvasShape>(20);
    private final List<CanvasLabelShape> _labelList= new ArrayList<CanvasLabelShape>(20);
    private final CanvasPanel panel;
    private final CanvasElement cElement;
    private final Context2d ctx;
    private DrawingCmd _drawingCmd= null;

 //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public HtmlGwtCanvas() {
        panel= new CanvasPanel();
        cElement= panel.getCanvas().getCanvasElement();
        ctx= cElement.getContext2d();
    }

    public Widget getWidget() { return panel; }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public static boolean isSupported() { return CanvasPanel.isSupported();  }


    public Shape drawLine(String color,
                         boolean front,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {
        return drawLine(color,front, DEF_WIDTH,sx,sy,ex,ey);
    }

    public Shape drawLine(String color,
                         boolean front,
                         int lineWidth,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {
        cancelRedraw();
        CanvasLine line= new CanvasLine(makeColor(color),front,lineWidth,sx,sy,ex,ey);
        line.draw(ctx);
        _shapeList.add(line);
        return line;
    }

    public Shape drawCircle(String color, boolean front, int lineWidth, int x, int y, int radius) {
        cancelRedraw();
        CanvasCircle circle= new CanvasCircle(makeColor(color),front,lineWidth,x,y,radius);
        circle.draw(ctx);
        _shapeList.add(circle);
        return circle;
    }

    public Shape drawText(String color,
                          String fontFamily,
                          String size,
                          String fontWeight,
                          String fontStyle,
                          int x,
                          int y,
                          String text) {
        cancelRedraw();
        HTML label= new HTML(text);
        GwtUtil.setStyles(label,
                          "color", color,
                          "fontFamily", fontFamily,
                          "fontSize", size,
                          "fontWeight", fontWeight,
                          "fontStyle", fontStyle,
                          "backgroundColor", "white",
                          "MozBorderRadius", "5px",
                          "borderRadius", "5px",
                          "webkitBorderRadius", "5px");

        CanvasLabelShape labelShape= new CanvasLabelShape(label);
        _labelList.add(labelShape);
        panel.addLabel(label,x,y);
        return labelShape;
    }

    public Shape drawText(String color, String size, int x, int y, String text) {
        return drawText(color, "inherit", size, "normal",  "normal", x, y, text);
    }

    public Shape drawRec(String color,
                        boolean front,
                        int lineWidth,
                        int x,
                        int y,
                        int width,
                        int height) {

        cancelRedraw();
        CanvasRec rec= new CanvasRec(makeColor(color),front,lineWidth,x,y,width,height);
        rec.draw(ctx);
        _shapeList.add(rec);
        return rec;
    }


    public Shape fillRec(String color,
                         boolean front,
                         int x,
                         int y,
                         int width,
                         int height) {

        cancelRedraw();
        CanvasFillRec rec= new CanvasFillRec(makeColor(color),front,x,y,width,height);
        rec.draw(ctx);
        _shapeList.add(rec);
        return rec;
    }


    public void deleteShapes(Shapes shapes) {
        cancelRedraw();
        if (shapes !=null && shapes.getShapes()!=null) {
            CanvasShape gs;
            for(Shape s : shapes.getShapes()) {
                if (s instanceof CanvasShape) {
                    gs= (CanvasShape)s;
                    if (_shapeList.contains(gs))  _shapeList.remove(gs);
                }
            }
        }
    }

    public void clear() {
        cancelRedraw();
        _shapeList.clear();
        for(CanvasLabelShape label : _labelList) {
            panel.removeLabel(label.getLabel());
        }
        ctx.clearRect(0,0,cElement.getWidth(),cElement.getHeight());
    }

    public void paint() { }
    public boolean getDrawingAreaChangeClear() { return true;}

    public void setDrawingAreaSize(int width, int height) {
        Canvas c= panel.getCanvas();
        if(cElement.getWidth()!=width || cElement.getHeight()!=height ||
           c.getCoordinateSpaceWidth()!=width || c.getCoordinateSpaceHeight()!=height) {

            panel.setPixelSize(width,height);
            cElement.setWidth(width);
            cElement.setHeight(height);

            redrawAll();
        }
    }

    public boolean getSupportsPartialDraws() { return true;}
    public boolean getSupportsShapeChange() { return false; }


    public void redrawAll() {
        cancelRedraw();
        if (_shapeList.size()<400) {
            redrawAllNOW();
        }
        else {
            DrawDeferred dd= new DrawDeferred(ctx,_shapeList,200);
            _drawingCmd= new DrawingCmd(dd);
            DeferredCommand.addCommand(_drawingCmd);
        }
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void redrawAllNOW() {
        for(CanvasShape s : _shapeList) {
            if (!s.isFront())  s.draw(ctx);
        }
        for(CanvasShape s : _shapeList) {
            if (s.isFront())  s.draw(ctx);
        }
    }

    private void cancelRedraw() {
        if (_drawingCmd!=null) {
            _drawingCmd.setDone(true);
            _drawingCmd= null;
        }
    }


    private static CssColor makeColor(String c)  {
        if (GwtUtil.isHexColor(c))  {
            c= "#" + c;
        }
        return CssColor.make(c);
    }


    private static class DrawingCmd implements IncrementalCommand {

        private boolean done = false;
        private final DrawDeferred dd;

        public DrawingCmd(DrawDeferred dd) { this.dd = dd; }

        public boolean execute() {
            if (!done) done = dd.drawChunk();
            return !done;
        }

        public void setDone(boolean done) { this.done = done; }
    }

    private static class DrawDeferred {
        private final Iterator<CanvasShape> iterator;
        private final int maxChunk;
        private final List<CanvasShape> data;
        private final Context2d ctx;

        DrawDeferred(Context2d ctx, List<CanvasShape> data, int maxChunk) {
            iterator = data.iterator();
            this.maxChunk = maxChunk;
            this.data = data;
            this.ctx= ctx;
        }

        public boolean drawChunk()  {
            boolean done= false;
            for(int i=0; (i< maxChunk && iterator.hasNext()); i++) {
                CanvasShape s= iterator.next();
                if (!s.isFront())  s.draw(ctx);
            }
            if (!iterator.hasNext()) {
                for(CanvasShape s : data) {
                    if (s.isFront())  s.draw(ctx);
                }
                done= true;
            }
            return done;

        }

    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

// =====================================================================
// -------------------- Native Methods --------------------------------
// =====================================================================


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
