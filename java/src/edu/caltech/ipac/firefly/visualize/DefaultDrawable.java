package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;
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
    private List<Widget> highPriorityList= new ArrayList<Widget>(6);

    private int _width= 1;
    private int _height= 1;
    private final String id;
    private static int idCnt= 1;
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
        id= "DP"+idCnt;
        idCnt++;
    }

    public AbsolutePanel getDrawingPanelContainer() { return _drawingPanel; }


    public Widget addDrawingArea(Widget w, boolean highPriority) {
        w.setPixelSize(_width,_height);
        w.addStyleName("drawingArea");
        if (highPriority) {
            _drawingPanel.add(w,0,0);
            highPriorityList.add(w);
        }
        else {
            if (highPriorityList.size()>0) {
                int pos= 0;
                boolean insert= false;
                for(Widget panel : _drawingPanel) {
                    if (highPriorityList.contains(panel)) {
                        insert= true;
                        break;
                    }
                    pos++;
                }
                if (insert && pos<_drawingPanel.getWidgetCount()) {
                    _drawingPanel.insert(w, pos);
                    _drawingPanel.setWidgetPosition(w,0,0);
                }
                else {
                    _drawingPanel.add(w,0,0);
                }
            }
            else {
                _drawingPanel.add(w,0,0);
            }

        }
        addID(w);
        return w;
    }

    private void addID(Widget w) {
        DOM.setElementProperty(w.getElement(),"id", id);
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
        addID(w);
    }

    public void insertBeforeDrawingArea(Widget before, Widget w) {
        int idx=_drawingPanel.getWidgetIndex(before);
        if (idx>-1) {
            w.addStyleName("drawingArea");
            w.setPixelSize(_width,_height);
            _drawingPanel.insert(w, idx);
            _drawingPanel.setWidgetPosition(w,0,0);
        }
        addID(w);
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
        addID(w);
    }

    public void removeDrawingArea(Widget w) {
        _drawingPanel.remove(w);
    }

    public void setPixelSize(int width, int height) {

        if (_width!=width || _height!=height) {
            _width= width;
            _height= height;
            _drawingPanel.setPixelSize(width,height);

            for(Widget da : _drawingPanel) {
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

