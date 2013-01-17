package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.ui.JSLoad;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.Drawable;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static edu.caltech.ipac.firefly.visualize.ReplotDetails.Reason;

/**
 * @author Trey Roby, Tatiana Goldina
 * A Drawer  can take one data set and draw it on one WebPlotView.
 * @version $Id: Drawer.java,v 1.54 2012/09/21 23:35:38 roby Exp $
 */
public class Drawer implements WebEventListener {

    public static enum DataType {VERY_LARGE, NORMAL, SMALL_AND_SIMPLE}

    public static final String DEFAULT_DEFAULT_COLOR= "red";
    public final static int MAX_DEFER= 1;
    public String _defColor= DEFAULT_DEFAULT_COLOR;
    private List<DrawObj> _data;
    private DrawConnector _drawConnect= null;
    private final WebPlotView _pv;
    private final Drawable _drawable;
    private Graphics _jg;
    private boolean _handleImagesChanges = true;
    private boolean _drawingEnabled= true;
    private boolean _visible= true;
    private final DataType _dataTypeHint;
    private final boolean _vectorGraphicsHint;
    private boolean _cleared = false;
    private DrawingDeferred _drawingCmd= null;
    private String _plotTaskID= null;
    private DataUpdater _dataUpdater= null;

    private static JSLoad _jsLoad = null;

    public static void loadJS(InitComplete ic) {
        if (BrowserUtil.isOldIE()) {
            ic.done();
        }
        else {
            if (_jsLoad ==null) {
                String raphael= GWT.getModuleBaseURL() + "raphael-min.js";
//                String raphael= "raphael-min.js";
//                String jsGraphics= "js/wz_jsgraphics.js";
//                _jsLoad = new JSLoad(new MyLoaded(ic), jsGraphics, raphael);
                _jsLoad = new JSLoad(new MyLoaded(ic), raphael);
            }
            else {
                _jsLoad.addCallback(new MyLoaded(ic));
            }
        }

    }

    public static boolean isAllLoaded() {
        return _jsLoad==null ? false : _jsLoad.isAllLoaded();
    }

    public static boolean isJSLoaded()  { return (_jsLoad!=null && _jsLoad.isAllLoaded()) || BrowserUtil.isOldIE(); }

    public static boolean isModernDrawing() { return true;  }

    public Drawer(WebPlotView pv, boolean vectorGraphics, DataType dataTypeHint) {
        this(pv,pv,vectorGraphics,dataTypeHint);
    }

    /**
     *
     * @param pv the plotview, optional for this constructor
     * @param drawable an alternate drawable to use instead of the WebPlotView
     * @param vectorGraphics a hint that this drawer is going to be using for
*               doing something like a select area.  Where the object might be relocated often.
     * @param dataTypeHint a hint specifiing how big the data is
     */
    public Drawer(WebPlotView pv, Drawable drawable, boolean vectorGraphics, DataType dataTypeHint) {
        WebAssert.argTst(isJSLoaded(), "You must first call loadJS once, before you can use the constructor");
        _pv= pv;
        _drawable= drawable;
        _dataTypeHint = dataTypeHint;
        _vectorGraphicsHint= vectorGraphics;
        makeGraphics();
    }

    private void makeGraphics() {

        if (BrowserUtil.isOldIE()) {
            _jg= new GWTGraphics();
        }
        else {
            if (_vectorGraphicsHint)              _jg= new RaphaelGraphics();
            else if (HtmlGwtCanvas.isSupported()) _jg= new HtmlGwtCanvas();
            else                                  _jg= new GWTGraphics();
        }

        _pv.getEventManager().addListener(Name.REPLOT, this);
        _pv.getEventManager().addListener(Name.PRIMARY_PLOT_CHANGE,this);
        if (_pv==_drawable) {
            _pv.getEventManager().addListener(Name.VIEW_PORT_CHANGE, this);
        }

        _drawable.addDrawingArea(_jg.getWidget());
    }

    public void dispose() {
        if (_pv!=null) {
            _pv.getEventManager().removeListener(Name.REPLOT, this);
            _pv.getEventManager().removeListener(Name.PRIMARY_PLOT_CHANGE, this);
            if (_pv==_drawable) {
                _pv.getEventManager().removeListener(Name.VIEW_PORT_CHANGE, this);
            }
        }
        if (_jg!=null) {
            _drawable.removeDrawingArea(_jg.getWidget());
            _jg= null;
        }
    }


    void setPointConnector(DrawConnector connector) {
        _drawConnect= connector;
    }


    public void setDefaultColor(String c) {
        if (!_defColor.equals(c)) {
            _defColor= c;
            redraw();
        }
    }

    public void setDrawingEnabled(boolean enable) {
        if (_drawingEnabled!=enable) {
            _drawingEnabled= enable;
            if (_drawingEnabled) redraw();
        }

    }

    public void setPlotChangeDataUpdater(DataUpdater dataUpdater) { _dataUpdater= dataUpdater; }

    public boolean getSupportsShapeChange() { return _jg!=null ? _jg.getSupportsShapeChange() : false; }

    public String getDefaultColor() { return _defColor; }


    public WebPlotView getPlotView() { return _pv; }

    private static void draw (Graphics g, AutoColor ac, WebPlot plot, DrawObj obj) {
        draw(g,ac, plot,obj,false);
    }

    private static void draw (Graphics g, AutoColor ac, WebPlot plot, DrawObj obj, boolean front) {
        if (obj.getSupportsWebPlot()) {
            obj.draw(g, plot,  front, ac);
        }
        else {
            obj.draw(g, front, ac);
        }
    }

    private static void drawConnector(Graphics g, AutoColor ac, WebPlot plot, DrawConnector dc, DrawObj obj, DrawObj lastObj) {
        if (lastObj==null) return;
        if (obj.getSupportsWebPlot()) {
            try {
                WorldPt wp1= plot.getWorldCoords(lastObj.getCenterPt());
                WorldPt wp2= plot.getWorldCoords(obj.getCenterPt());
                if (!plot.coordsWrap(wp1,wp2)) {
                    dc.draw(g,plot,ac, wp1,wp2);
                }
            } catch (ProjectionException e) {
                // do nothing
            }
        }
        else {
            if (lastObj.getCenterPt() instanceof ScreenPt && obj.getCenterPt() instanceof ScreenPt) {
                dc.draw(g,ac, (ScreenPt)lastObj.getCenterPt(), (ScreenPt)obj.getCenterPt());
            }
        }
    }

    /**
     * when the image resizes, like a zoom, then fire events to redraw
     * Certain types of data will need to recompute the data when the image size changes so this
     * methods disables the default automactic handling
     * By default, this property is true
     * @param h handle image changes
     */
    public void setHandleImageChanges(boolean h) { _handleImagesChanges = h; }


    private void cancelRedraw() {
        if (_drawingCmd!=null) {
            _drawingCmd.cancelDraw();
            _drawingCmd= null;
        }
    }

    /**
     * set the data object to draw
     * @param data a DataObj that will be drawn
     */
    public void setData(DrawObj data) { setData(data!=null ? Arrays.asList(data) : null); }


    /**
     * set the list of data objects to draw
     * @param data the list of DataObj
     */
    public void setData(List<DrawObj> data) {
        cancelRedraw();
        _data = data;
        if (data!=null) {
            if (_jg==null) makeGraphics();
            redraw();
        }
        else {
            if (_jg!=null) dispose();
        }
    }

    public List<DrawObj> getData() {
        return _data;
    }

//    public boolean getHasData() { return _data!=null; }

    public void setVisible(boolean v) {
        if (_visible!=v) {
            _visible= v;
            cancelRedraw();
            redraw();
        }
    }

    public boolean isVisible() { return _visible && _jg!=null; }

    public void setDataDelta(List<DrawObj> data, List<Integer>changeIdx) {
        _data = data;
        redrawDelta(_jg, changeIdx);
    }

    public void clear() {
//        init();
        _data = null;
        cancelRedraw();
        if (_jg!=null) _jg.clear();
        _cleared= true;
        removeTask();
    }


    public void redrawDelta(Graphics graphics, List<Integer>changeIdx) {
//        init();
        if (graphics==null) return;
        AutoColor autoColor= (_pv==null) ? null : new AutoColor(_pv.getPrimaryPlot(),this);
        if (graphics.getSupportsPartialDraws()) {
            if (canDraw(graphics)) {
                _cleared= false;
                List<DrawObj> onTop= new ArrayList<DrawObj>((int)(changeIdx.size()*.2F+4));
                WebPlot plot= (_pv==null) ? null : _pv.getPrimaryPlot();
                DrawObj obj;
                for(int idx : changeIdx) {
                    obj= _data.get(idx);
                    if (obj.plotOnTop()) {
                        onTop.add(obj);
                    }
                    else {
                        draw(graphics, autoColor, plot, obj);
                    }
                }
                for(DrawObj pt : onTop)  draw(graphics, autoColor, plot, pt, true);
                graphics.paint();
            }
        }
        else {
            redraw(graphics);
        }
    }


    public void redraw() {
        if (_jg!=null) redraw(_jg);
    }


    public void redraw(Graphics graphics) {
//        init();
        if (graphics==null) return;
        graphics.clear();
        if (canDraw(graphics)) {
            _cleared= false;
            AutoColor autoColor= (_pv==null) ? null : new AutoColor(_pv.getPrimaryPlot(),this);
            WebPlot plot= _pv.getPrimaryPlot();  //TODO - how can i support null WebPlotView here?

            Dimension dim= plot.getViewPortDimension();
            graphics.setDrawingAreaSize(dim.getWidth(),dim.getHeight());
            if (_dataTypeHint ==DataType.VERY_LARGE) {
                int maxChunk= BrowserUtil.isBrowser(Browser.SAFARI) || BrowserUtil.isBrowser(Browser.CHROME) ? 50 : 10;
                DrawingParams params= new DrawingParams(graphics, autoColor,plot,_data, maxChunk);
                if (_drawingCmd!=null) _drawingCmd.cancelDraw();
                _drawingCmd= new DrawingDeferred(params);
                _drawingCmd.activate();
                removeTask();
                _plotTaskID= _pv.addTask();

            }
            else {
                DrawingParams params= new DrawingParams(graphics, autoColor, plot,_data,Integer.MAX_VALUE);
                doDrawing(params, false);
            }
        }
        else {
            removeTask();
        }
    }


    private void doDrawing(DrawingParams params, boolean deferred) {
        DrawObj obj;
        if (deferred && params._deferCnt<MAX_DEFER) {
            params._deferCnt++;
            params._done= false;
        }
        else {
            params._deferCnt= 0;
        }
        if (!params._done) {
            if (_drawConnect!=null) _drawConnect.beginDrawing();
            DrawObj lastObj= null;
            for(int i= 0; (params._iterator.hasNext() && i<params._maxChunk ); ) {
                obj= params._iterator.next();
                if (doDraw(params._plot,obj)|| (_drawConnect!=null && doDraw(params._plot,lastObj)) ) {
                    if (obj.plotOnTop())  params._onTop.add(obj);
                    else                  draw(params._graphics, params._ac, params._plot, obj);
                    if (_drawConnect!=null) {
                        drawConnector(params._graphics,params._ac,params._plot,_drawConnect,obj,lastObj);
                    }
                    i++;
                }
                lastObj= obj;
            }
            if (!params._iterator.hasNext()) { //loop finished
                for(DrawObj pt : params._onTop)  draw(params._graphics, params._ac, params._plot, pt, true);
                params._graphics.paint();
                params._done= true;
                removeTask();
            }
        }
        if (params._done && _drawConnect!=null) {
            _drawConnect.endDrawing();
        }
    }

    private void removeTask() {
        if (_pv!=null && _plotTaskID!=null) _pv.removeTask(_plotTaskID);
        _plotTaskID= null;
    }

    /**
     * An optimization of drawing.  Check is the Object is a PointDataObj (most common and simple) and then checks
     * it is draw on a WebPlot, and if it is in the drawing area.
     * @param obj the DrawingObj to check
     * @param plot the WebPlot to draw on
     * @return true is it should be drawn
     */
    private static boolean doDraw(WebPlot plot, DrawObj obj) {
        boolean retval= true;
        if (obj!=null && obj instanceof PointDataObj && plot!=null) {
            Pt pt= ((PointDataObj)obj).getPos();
            if (pt instanceof WorldPt) retval= plot.pointInViewPort(pt);
        }
        return retval;
    }


    private boolean canDraw(Graphics graphics) {//TODO - how can i support null WebPlotView here?
        return (graphics!=null &&
                _visible &&
                _drawingEnabled &&
                _data!=null &&
                _data.size()>0 &&
                _pv!=null &&
                _pv.getPrimaryPlot()!=null);
    }


//=======================================================================
//-------------- Method from WebPlotListener Interface ------------------
//=======================================================================

    public void eventNotify(WebEvent ev) {
        Name name= ev.getName();
        if (name.equals(Name.PRIMARY_PLOT_CHANGE)) {
            if (_dataUpdater!=null) {
                if (_pv.getPrimaryPlot()==null) {
                    clear();
                }
                else {
                    setData(_dataUpdater.getData());
                }
            }
        }
        else if (name.equals(Name.REDRAW_DATA)) {
            if (_pv.getPrimaryPlot()!=null) redraw();
        }
        else if (name.equals(Name.VIEW_PORT_CHANGE)) {
            if (_pv.getPrimaryPlot()!=null) redraw();
        }
        else if (name.equals(Name.REPLOT)) {
            ReplotDetails details= (ReplotDetails)ev.getData();
            Reason reason= details.getReplotReason();
            if (reason==Reason.IMAGE_RELOADED) {
//                (reason==Reason.ZOOM && _handleImagesChanges) ) {
//                redraw();
            }
            else if (reason==Reason.ZOOM || reason==Reason.ZOOM_COMPLETED || reason==Reason.REPARENT) {
                // do nothing

            }
            else {
                if (_jg!=null && _pv!=null && _pv.getPrimaryPlot()!=null) {
                    _jg.clear();
                }
            }
        }
    }


    private class DrawingDeferred extends Timer implements IncrementalCommand {

        DrawingParams _params;
        boolean       _deferred;

        public DrawingDeferred(DrawingParams params) {
            _params= params;
//            _deferred= BrowserUtil.isBrowser(Browser.SAFARI) || BrowserUtil.isBrowser(Browser.CHROME);
            _deferred= true;
        }

        public boolean execute() {
            draw();
            return !_params._done;
        }

        @Override
        public void run() {
            draw();
            if (!_params._done)  this.schedule(100);
        }

        private void draw() {
            if (!_params._plot.isAlive()) _params._done= true;
            if (!_params._done ) doDrawing(_params, _deferred);
        }

        public void cancelDraw() { _params._done= true; }

        public void activate() {
            if (_deferred)  DeferredCommand.addCommand(this);
            else schedule(100);
        }
    }



    private static class DrawingParams {
        final WebPlot _plot;
        private final List<DrawObj> _data;
        final List<DrawObj> _onTop;
        final Iterator<DrawObj> _iterator;
        final int _maxChunk;
        boolean _done= false;
        final AutoColor _ac;
        final Graphics _graphics;
        int _deferCnt= 0;

        DrawingParams(Graphics graphics, AutoColor ac, WebPlot plot, List<DrawObj> data, int maxChunk) {
            _graphics= graphics;
            _ac= ac;
            _plot= plot;
            _data= data;
            _onTop= new ArrayList<DrawObj>((int)(_data.size()*.2F));
            _iterator= _data.iterator();
            _maxChunk= maxChunk;
        }
    }


    public interface InitComplete {
        public void done();
    }

    public static class MyLoaded implements JSLoad.Loaded {
        InitComplete _ic;
        public MyLoaded(InitComplete ic)  { _ic= ic; }
        public void allLoaded() { _ic.done(); }
    }

    public interface DataUpdater {
        List<DrawObj> getData();
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
