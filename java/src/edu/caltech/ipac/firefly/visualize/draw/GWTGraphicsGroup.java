package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
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
public class GWTGraphicsGroup {

    private final List<GWTShape> _shapeList= new ArrayList<GWTShape>(20);
    private final List<GWTLabelShape> _labelList= new ArrayList<GWTLabelShape>(20);
    private final GWTCanvas _surfaceW;
    private final GWTCanvasPanel _canvasPanel;
    private DrawingCmd _drawingCmd= null;

 //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public GWTGraphicsGroup(GWTCanvasPanel canvasPanel) {
        _canvasPanel= canvasPanel;
        _surfaceW= canvasPanel.getCanvas();
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public Shape drawLine(String color,
                         boolean front,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {
        cancelRedraw();
        return drawLine(color,front, Graphics.DEF_WIDTH,sx,sy,ex,ey);
    }

    public Shape drawLine(String color,
                         boolean front,
                         int lineWidth,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {

        cancelRedraw();
        GWTLine line= new GWTLine(makeColor(color),front,lineWidth,sx,sy,ex,ey);
        line.draw(_surfaceW);
        _shapeList.add(line);
        return line;
    }


    public Shape drawCircle(String color, boolean front, int lineWidth, int x, int y, int radius) {
        cancelRedraw();
        GWTCircle circle= new GWTCircle(makeColor(color),front,lineWidth,x,y,radius);
        circle.draw(_surfaceW);
        _shapeList.add(circle);
        return circle;
    }


    public Shape drawRec(String color,
                         boolean front,
                         int lineWidth,
                         int x,
                         int y,
                         int width,
                         int height) {
        cancelRedraw();
        GWTRec rec= new GWTRec(makeColor(color),front,lineWidth,x,y,width,height);
        rec.draw(_surfaceW);
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
        GWTFillRec rec= new GWTFillRec(makeColor(color),front,x,y,width,height);
        rec.draw(_surfaceW);
        _shapeList.add(rec);
        return rec;
    }

    private static Color makeColor(String c)  {
        if (GwtUtil.isHexColor(c))  {
            c= "#" + c;
        }
        return new Color(c);
    }



    public Shape drawText(String text,
                        String size,
                        String color,
                        int x,
                        int y) {
        cancelRedraw();
        HTML label= new HTML(text);
        Element e= label.getElement();
        DOM.setStyleAttribute(e, "color", "white");
        DOM.setStyleAttribute(e, "fontSize", size);
        DOM.setStyleAttribute(e, "backgroundColor", color);
        DOM.setStyleAttribute(e, "MozBorderRadius", "5px");
        DOM.setStyleAttribute(e, "borderRadius", "5px");
        DOM.setStyleAttribute(e, "webkitBorderRadius", "5px");
//        DOM.setStyleAttribute(e, "opacity", ".5");
//        DOM.setStyleAttribute(e, "filter", "alpha(opacity=40)");


//        DOM.setInnerHTML(e, "<div style=\"opacity:.9; filter:alpha(opacity=40); background-color:white;\">" +
//                "<span style=" +
//                "color:" + color + ";" +
//                "font-size:" + size + ";" +
//                 ">" +
//                text + "</span>" +
//                "</div>");

        GWTLabelShape labelShape= new GWTLabelShape(label);
        _labelList.add(labelShape);
//        DOM.setStyleAttribute(label.getElement(), "border", "1px solid red");
        _canvasPanel.addLabel(label,x,y);
        return labelShape;
    }


    public void deleteShapes(Shapes shapes) {
        cancelRedraw();
        if (shapes !=null && shapes.getShapes()!=null) {
            GWTShape gs;
            for(Shape s : shapes.getShapes()) {
                if (s instanceof GWTShape) {
                    gs= (GWTShape)s;
                    if (_shapeList.contains(gs)) {
                        _shapeList.remove(gs);
                    }
                }
            }
        }
    }


    public void clear() {
        cancelRedraw();
        _shapeList.clear();
        for(GWTLabelShape label : _labelList) {
            _canvasPanel.removeLabel(label.getLabel());
        }
    }

    public void redrawAll() {
        cancelRedraw();
        if (_shapeList.size()<400) {
            redrawAllNOW();
        }
        else {
            DrawDeferred dd= new DrawDeferred(_surfaceW,_shapeList,200);
            _drawingCmd= new DrawingCmd(dd);
            DeferredCommand.addCommand(_drawingCmd);
        }
    }


    private void redrawAllNOW() {
        for(GWTShape s : _shapeList) {
            if (!s.isFront())  s.draw(_surfaceW);
        }
        for(GWTShape s : _shapeList) {
            if (s.isFront())  s.draw(_surfaceW);
        }
    }

    private void cancelRedraw() {
        if (_drawingCmd!=null) {
            _drawingCmd.setDone(true);
            _drawingCmd= null;
        }
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static class DrawingCmd implements IncrementalCommand {

        private boolean _done= false;
        DrawDeferred _dd;

        public DrawingCmd(DrawDeferred dd) { _dd= dd; }

        public boolean execute() {
            if (!_done) _done= _dd.drawChunk();
            return !_done;
        }

        public void setDone(boolean done) { _done= done; }
    }

    private static class DrawDeferred {
        private final Iterator<GWTShape> _iterator;
        private final int _maxChunk;
        private final List<GWTShape>  _data;
        private final GWTCanvas _surfaceW;
        private boolean _done= false;

        DrawDeferred(GWTCanvas surfaceW, List<GWTShape> data, int maxChunk) {
            _iterator= data.iterator();
            _maxChunk= maxChunk;
            _data= data;
            _surfaceW= surfaceW;
        }

        public boolean drawChunk()  {
            boolean done= false;
            for(int i=0; (i<_maxChunk && _iterator.hasNext()); i++) {
                GWTShape s= _iterator.next();
                if (!s.isFront())  s.draw(_surfaceW);
            }
            if (!_iterator.hasNext()) {
                for(GWTShape s : _data) {
                    if (s.isFront())  s.draw(_surfaceW);
                }
                done= true;
            }
            return done;

        }

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
