package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;
/**
 * User: roby
 * Date: Sep 30, 2009
 * Time: 10:02:45 AM
 */


/**
 * @author Trey Roby
 */
public class DefaultDrawable implements Drawable {

    private AbsolutePanel _drawingPanel = new AbsolutePanel();
    private int _width= 1;
    private int _height= 1;
//    private List<DrawingLayer> _layers= new ArrayList<DrawingLayer>(5);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public int getDrawingWidth() { return _width; }
    public int getDrawingHeight() { return _height; }

    public DefaultDrawable() {
        _drawingPanel.addStyleName("drawingArea");
        _drawingPanel.setPixelSize(_width,_height);
    }

    public AbsolutePanel getDrawingPanelContainer() { return _drawingPanel; }


    public Widget addDrawingArea(Widget w) {
        w.setPixelSize(_width,_height);
        w.addStyleName("drawingArea");
        _drawingPanel.add(w,0,0);
        return w;
    }

    public void replaceDrawingArea(Widget old, Widget w) {
        int idx=_drawingPanel.getWidgetIndex(old);
        if (idx>-1) {
            w.addStyleName("drawingArea");
            w.setPixelSize(_width,_height);
            _drawingPanel.insert(w, idx);
            _drawingPanel.setWidgetPosition(w,0,0);
            _drawingPanel.remove(idx+1);
        }
    }

    public void insertBeforeDrawingArea(Widget before, Widget w) {
        int idx=_drawingPanel.getWidgetIndex(before);
        if (idx>-1) {
            w.addStyleName("drawingArea");
            w.setPixelSize(_width,_height);
            _drawingPanel.insert(w, idx);
            _drawingPanel.setWidgetPosition(w,0,0);
        }
    }

    public void insertAfterDrawingArea(Widget after, Widget w) {
        int idx=_drawingPanel.getWidgetIndex(after);
        int cnt= _drawingPanel.getWidgetCount();
        if (idx==-1 || idx>=cnt-1) {
            _drawingPanel.add(w);
        }
        else {
            _drawingPanel.insert(w, idx+1);
        }
        w.addStyleName("drawingArea");
        w.setPixelSize(_width,_height);
        _drawingPanel.setWidgetPosition(w,0,0);
    }

    public void removeDrawingArea(Widget w) {
        _drawingPanel.remove(w);
    }

    public void setPixelSize(int width, int height) {

        if (_width!=width || _height!=height) {
            _width= width;
            _height= height;
            _drawingPanel.setPixelSize(width,height);

            for(Iterator i= _drawingPanel.iterator(); (i.hasNext());) {
                Widget da= (Widget)i.next();
                da.setPixelSize(width,height);
            }
        }
    }

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

//    private class DrawingLayer {
//
//        private final Graphics _graphics;
//        private final String _desc;
//        private boolean _showing= true;
//
//        DrawingLayer(Graphics drawable,
//                     String desc) {
//            _graphics= drawable;
//            _desc= desc;
//        }
//
//        public Graphics getGraphics() { return _graphics; }
//        public String getDesc() { return _desc; }
//
//        public void setShowing(boolean s) { _showing= s; }
//        public boolean isShowing() { return _showing; }
//
//    }

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
