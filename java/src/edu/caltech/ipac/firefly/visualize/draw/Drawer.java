package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.JSLoad;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.Drawable;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.ViewPortPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.visualize.ReplotDetails.Reason;

/**
 * @author Trey Roby, Tatiana Goldina
 * A Drawer  can take one data set and draw it on one WebPlotView.
 * @version $Id: Drawer.java,v 1.54 2012/09/21 23:35:38 roby Exp $
 */
public class Drawer implements WebEventListener {

    public static enum DataType {VERY_LARGE, NORMAL}

    public static final String DEFAULT_DEFAULT_COLOR= "red";
    public final static int MAX_DEFER= 1;
    private String _defColor= DEFAULT_DEFAULT_COLOR;
    private List<DrawObj> _data;
    private List<DrawObj> decimatedData= null;
    private Dimension decimateDim= null;
    private DrawConnector _drawConnect= null;
    private final WebPlotView _pv;
    private final Drawable _drawable;
    private Graphics primaryGraphics;
    private Graphics selectLayerGraphics;
    private Graphics highlightLayerGraphics;
    private boolean alive= true;
    private boolean _handleImagesChanges = true;
    private boolean _drawingEnabled= true;
    private boolean _visible= true;
    private DataType _dataTypeHint= DataType.NORMAL;
    private boolean _cleared = false;
    private DrawingDeferred _drawingCmd= null;
    private String _plotTaskID= null;
    private DataUpdater _dataUpdater= null;
    private boolean decimate= false;
    private boolean highPriorityLayer;

//    private static JSLoad _jsLoad = null;


    public Drawer(WebPlotView pv) { this(pv,pv, false); }

    public Drawer(WebPlotView pv, boolean highPriorityLayer) { this(pv,pv,highPriorityLayer); }

    /**
     *
     * @param pv the plotview, optional for this constructor
     * @param drawable an alternate drawable to use instead of the WebPlotView
     */
    public Drawer(WebPlotView pv, Drawable drawable, boolean highPriorityLayer) {
        _pv= pv;
        _drawable= drawable;
        this.highPriorityLayer= highPriorityLayer;
        initGraphics();
    }


    public static boolean isModernDrawing() { return true;  }

    public void setDataTypeHint(DataType dataTypeHint) {
        _dataTypeHint= dataTypeHint;
    }

    public void setHighPriorityLayer(boolean highPriorityLayer) {
        this.highPriorityLayer = highPriorityLayer; //todo
    }

    private void initGraphics() {
        _pv.addListener(Name.REPLOT, this);
        _pv.addListener(Name.PRIMARY_PLOT_CHANGE,this);
        if (_pv==_drawable) {
            _pv.addListener(Name.VIEW_PORT_CHANGE, this);
        }

        primaryGraphics = makeGraphics(_drawable, "mainDrawingLayer");
//        selectLayerGraphics = makeGraphics(_drawable, "selectLayer");
        _drawable.addDrawingArea(primaryGraphics.getWidget(), highPriorityLayer);
//        _drawable.addDrawingArea(selectLayerGraphics.getWidget(), highPriorityLayer);
    }

    private void initSelectedGraphicsLayer() {
        if (selectLayerGraphics==null) {
            selectLayerGraphics = makeGraphics(_drawable, "selectLayer");
            _drawable.insertAfterDrawingArea(primaryGraphics.getWidget(),selectLayerGraphics.getWidget());

        }
    }

    private void initHighlightedGraphicsLayer() {
        if (highlightLayerGraphics==null) {
            highlightLayerGraphics = makeGraphics(_drawable, "highlightLayer");
            Widget w= selectLayerGraphics==null ? primaryGraphics.getWidget() : selectLayerGraphics.getWidget();
            _drawable.insertAfterDrawingArea(w, highlightLayerGraphics.getWidget());
        }
    }


    public static Graphics makeGraphics(Drawable drawable, String layerName) {
        Graphics graphics;
        if (BrowserUtil.isOldIE()) {
            graphics= new GWTGraphics();
        }
        else {
            if (HtmlGwtCanvas.isSupported()) graphics= new HtmlGwtCanvas();
            else                                  graphics= new GWTGraphics();
        }
        if (drawable.getDrawingWidth()>0 && drawable.getDrawingHeight()>0) {
            graphics.getWidget().setPixelSize(drawable.getDrawingWidth(), drawable.getDrawingHeight());
        }
        graphics.getWidget().addStyleName(layerName);
        return graphics;
    }

    public void dispose() {
        if (_pv!=null) {
            _pv.removeListener(Name.REPLOT, this);
            _pv.removeListener(Name.PRIMARY_PLOT_CHANGE, this);
            if (_pv==_drawable) {
                _pv.removeListener(Name.VIEW_PORT_CHANGE, this);
            }
        }
        removeFromDrawArea(primaryGraphics);
        removeFromDrawArea(highlightLayerGraphics);
        removeFromDrawArea(selectLayerGraphics);

        primaryGraphics= null;
        highlightLayerGraphics= null;
        selectLayerGraphics= null;

        alive= false;
    }

    private void removeFromDrawArea(Graphics g) {
        if (g!=null) _drawable.removeDrawingArea(g.getWidget());
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

    public void setEnableDecimationDrawing(boolean d) {
        decimate= d;
    }

    public void setPlotChangeDataUpdater(DataUpdater dataUpdater) { _dataUpdater= dataUpdater; }

    public String getDefaultColor() { return _defColor; }


    public WebPlotView getPlotView() { return _pv; }

    private static void draw (Graphics g, AutoColor ac, WebPlot plot, DrawObj obj, boolean useStateColor) {
        if (obj.getSupportsWebPlot()) obj.draw(g, plot, ac, useStateColor);
        else                          obj.draw(g, ac, useStateColor);
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
        decimatedData= null;
        cancelRedraw();
        _data = data;
        if (data!=null) {
            if (primaryGraphics ==null) initGraphics();
            redraw();
        }
    }

    public List<DrawObj> getData() {
        return _data;
    }

    public void setVisible(boolean v) {
        if (alive) {
            if (_visible!=v || (v && primaryGraphics ==null)) {
                if (primaryGraphics ==null && _data!=null) initGraphics();
                _visible= v;
                cancelRedraw();
                redraw();
            }
        }
    }

    public boolean isVisible() { return _visible; }

    public void setDataDelta(List<DrawObj> data, List<Integer>changeIdx) {
        _data = data;
        redrawDelta(primaryGraphics, changeIdx);
    }

    public void updateDataSelectLayer(List<DrawObj> data) {
        initSelectedGraphicsLayer();
        _data = data;
        redrawSelected(selectLayerGraphics, _pv, _data);
    }

    public void updateDataHighlightLayer(List<DrawObj> data) {
        initHighlightedGraphicsLayer();
        _data = data;
        redrawHighlight(highlightLayerGraphics, _pv, _data);
    }


    public void clearSelectLayer() { selectLayerGraphics.clear(); }
    public void clearHighlightLayer() { highlightLayerGraphics.clear(); }

    public void clear() {
//        init();
        _data = null;
        cancelRedraw();
        if (primaryGraphics !=null) primaryGraphics.clear();
        if (selectLayerGraphics !=null) selectLayerGraphics.clear();
        if (highlightLayerGraphics !=null) selectLayerGraphics.clear();
        _cleared= true;
        removeTask();
    }

    private void clearDrawingAreas() {
        if (primaryGraphics !=null) primaryGraphics.clear();
        if (selectLayerGraphics !=null) selectLayerGraphics.clear();
        if (highlightLayerGraphics !=null) selectLayerGraphics.clear();

    }

    public void redrawDelta(Graphics g, List<Integer>changeIdx) {
        if (g==null) return;
        AutoColor autoColor= makeAutoColor(_pv);
        if (getSupportsPartialDraws(g)) {
            if (canDraw(g)) {
                _cleared= false;
                WebPlot plot= (_pv==null) ? null : _pv.getPrimaryPlot();
                DrawObj obj;
                for(int idx : changeIdx) {
                    obj= _data.get(idx);
                    draw(g, autoColor, plot, obj,true);
                }
                g.paint();
            }
        }
        else {
            redrawPrimary();
        }
    }

    public boolean getSupportsPartialDraws(Graphics g) {
        return !(g instanceof JSGraphics);
    }

    public void redrawSelected(Graphics graphics, WebPlotView pv, List<DrawObj> data) {
        if (graphics==null) return;
        List<DrawObj> selectedData= new ArrayList<DrawObj>(data.size()/2);
        graphics.clear();
        AutoColor autoColor= makeAutoColor(pv);
        if (canDraw(graphics)) {
            WebPlot plot= (pv==null) ? null : pv.getPrimaryPlot();
            for(DrawObj pt : data) {
                if (pt.isSelected())  selectedData.add(pt);
            }
            selectedData= decimateData(selectedData,null);
            for(DrawObj pt : selectedData) {
                draw(graphics, autoColor, plot, pt, true);
            }
            graphics.paint();
        }
    }
    public void redrawHighlight(Graphics graphics, WebPlotView pv, List<DrawObj> data) {
        if (graphics==null) return;
        graphics.clear();
        AutoColor autoColor= makeAutoColor(pv);
        if (canDraw(graphics)) {
            WebPlot plot= (pv==null) ? null : pv.getPrimaryPlot();
            for(DrawObj pt : data) {
                if (pt.isHighlighted())  draw(graphics, autoColor, plot, pt, true);
            }
            graphics.paint();
        }
    }


    private AutoColor makeAutoColor(WebPlotView pv) {
        return (pv==null) ? null : new AutoColor(pv.getPrimaryPlot().getColorTableID(),_defColor);
    }


    public void redraw() {
        redrawPrimary();
    }


    public void redrawPrimary() {
//        init();
        if (primaryGraphics==null || !alive) return;
        clearDrawingAreas();
        if (canDraw(primaryGraphics)) {
            _cleared= false;
            AutoColor autoColor= makeAutoColor(_pv);
            WebPlot plot= _pv.getPrimaryPlot();  //TODO - how can i support null WebPlotView here?

            Dimension dim= plot.getViewPortDimension();
            primaryGraphics.setDrawingAreaSize(dim.getWidth(),dim.getHeight());
            List<DrawObj> drawData= decimateData(_data);
            if (_dataTypeHint ==DataType.VERY_LARGE) {
                int maxChunk= BrowserUtil.isBrowser(Browser.SAFARI) || BrowserUtil.isBrowser(Browser.CHROME) ? 200 : 100;
//                Graphics g= makeGraphics();
//                _drawable.insertBeforeDrawingArea(primaryGraphics.getWidget(), g.getWidget());
                DrawingParams params= new DrawingParams(primaryGraphics, autoColor,plot,drawData, maxChunk);
                if (_drawingCmd!=null) _drawingCmd.cancelDraw();
                _drawingCmd= new DrawingDeferred(params);
                _drawingCmd.activate();
                removeTask();
                _plotTaskID= _pv.addTask();

            }
            else {
//                Graphics g= makeGraphics();
//                _drawable.insertBeforeDrawingArea(primaryGraphics.getWidget(), g.getWidget());
                DrawingParams params= new DrawingParams(primaryGraphics, autoColor, plot,drawData,Integer.MAX_VALUE);
                doDrawing(params, false);
            }
        }
        else {
            removeTask();
        }
    }



    private List<DrawObj> decimateData(List<DrawObj> inData) {
        WebPlot plot= _pv.getPrimaryPlot();
        if (decimate && plot!=null && inData.size()>150) {
            decimatedData= decimateData(inData,decimatedData);
            return decimatedData;
        }
        else {
            return inData;
        }
    }

    private List<DrawObj> decimateData(List<DrawObj> inData, List<DrawObj> oldDecimatedData) {
        List<DrawObj> retData= inData;
        WebPlot plot= _pv.getPrimaryPlot();
        if (decimate && plot!=null && inData.size()>150 ) {
            Dimension dim = plot.getViewPortDimension();
            if (oldDecimatedData==null || !dim.equals(decimateDim)) {

                decimateDim= dim;
                float drawArea= dim.getWidth()*dim.getHeight();

                float percentCov= inData.size()/drawArea;

                int fuzzLevel= (int)(percentCov*100);
                if (fuzzLevel>7) fuzzLevel= 7;
                else if (fuzzLevel<3) fuzzLevel= 3;

                Map<FuzzyVPt, DrawObj> foundPtMap= new HashMap<FuzzyVPt, DrawObj>(inData.size()*2);
                for(DrawObj candidate : inData) {
                    FuzzyVPt cenPt;
                    try {
                        cenPt = new FuzzyVPt(plot.getViewPortCoords(candidate.getCenterPt()),fuzzLevel);
                        if (!foundPtMap.containsKey(cenPt)){
                            foundPtMap.put(cenPt, candidate);
                        }
                    } catch (ProjectionException e) {
                    }
                }
                retData= new ArrayList<DrawObj>(foundPtMap.values());
            }
            else if (decimatedData!=null) {
                retData= decimatedData;
            }
        }
        return retData;

    }


    private void doDrawing(DrawingParams params, boolean deferred) {
        GwtUtil.setHidden(params._graphics.getWidget(), true);
//        params._graphics.getWidget().setVisible(false);
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
                    draw(params._graphics, params._ac, params._plot, obj,false);
                    if (_drawConnect!=null) {
                        drawConnector(params._graphics,params._ac,params._plot,_drawConnect,obj,lastObj);
                    }
                    i++;
                }
                lastObj= obj;
            }
            if (!params._iterator.hasNext()) { //loop finished
                params._graphics.paint();
                params._done= true;
                removeTask();
            }
        }
        if (params._done ) {
//            final Widget tmp= primaryGraphics.getWidget();

//            DeferredCommand.addCommand(new Command() {
//                public void execute() {
//                    _drawable.removeDrawingArea(tmp);
//                }
//            });
//            primaryGraphics = params._graphics;
            GwtUtil.setHidden(primaryGraphics.getWidget(),false);
            if (_drawConnect!=null) {
                _drawConnect.endDrawing();
            }
        }

//        GwtUtil.setHidden(primaryGraphics.getWidget(), false);
//        params._graphics.getWidget().setVisible(true);
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
                clearDrawingAreas();
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

    private static class FuzzyVPt {
        private ViewPortPt pt;
        private final int fuzzLevel;

        private FuzzyVPt(ViewPortPt pt) { this(pt,2);  }

        private FuzzyVPt(ViewPortPt pt,int fuzzLevel) {
            this.pt = pt;
            this.fuzzLevel= fuzzLevel;
        }

        public boolean equals(Object o) {
            boolean retval= false;
            if (o instanceof FuzzyVPt) {
                FuzzyVPt p= (FuzzyVPt)o;
                if (getClass() == p.getClass()) {
                    if (nextPt(p.pt.getIX()) == nextPt(this.pt.getIX()) &&
                        nextPt(p.pt.getIY()) == nextPt(this.pt.getIY())) {
                              retval= true;
                    }
                } // end if
            }
            return retval;
        }

        @Override
        public String toString() {
            return "x:"+nextPt(pt.getIX()) +",y:" +nextPt(pt.getIY());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

//        private int nextEven(int i) {
//            return  (i%2==0) ? i : i+1;
//        }

        private int nextPt(int i) {
            int remainder= i%fuzzLevel;
            return  (remainder==0) ? i : i+(fuzzLevel-remainder);
        }
    }


    private static class DrawingParams {
        final WebPlot _plot;
        private final List<DrawObj> _data;
//        final List<DrawObj> _onTop;
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
//            _onTop= new ArrayList<DrawObj>((int)(_data.size()*.2F));
            _iterator= _data.iterator();
            _maxChunk= maxChunk;
        }
    }

    public static class DrawCandidate {
        private final ViewPortPt vp;
        private final DrawObj obj;

        public DrawCandidate(ViewPortPt vp, DrawObj obj) {
            this.vp = vp;
            this.obj = obj;
        }

        public DrawObj getObj() { return obj; }
        public ViewPortPt getPoint() { return vp; }
    }


    public interface CompleteNotifier {
        public void done();
    }

    public static class MyLoaded implements JSLoad.Loaded {
        CompleteNotifier _ic;
        public MyLoaded(CompleteNotifier ic)  { _ic= ic; }
        public void allLoaded() { _ic.done(); }
    }

    public interface DataUpdater {
        List<DrawObj> getData();
    }


//    private List<DrawObj> decimateDataORIGINAL(List<DrawObj> inData) {
//        List<DrawObj> retData= inData;
//        WebPlot plot= _pv.getPrimaryPlot();
//        if (decimate && plot!=null) {
//            Map<FuzzyVPt, DrawObj> foundPtMap= new HashMap<FuzzyVPt, DrawObj>(inData.size()*2);
//            for(DrawObj d : inData) {
//                try {
//                    ViewPortPt vPt= plot.getViewPortCoords(d.getCenterPt());
//                    FuzzyVPt cenPt= new FuzzyVPt(vPt);
//                    if (!foundPtMap.containsKey(cenPt)){
//                        foundPtMap.put(cenPt, d);
//                    }
//                } catch (ProjectionException e) {
//                    // ignore
//                }
//            }
//            retData= new ArrayList<DrawObj>(foundPtMap.values());
//        }
//        return retData;
//    }
//
//    private List<DrawObj> decimateDataTRY2(List<DrawObj> inData) {
//        List<DrawObj> retData= inData;
//        WebPlot plot= _pv.getPrimaryPlot();
//        if (decimate && plot!=null && inData.size()>30 && decimatedData==null) {
//            List<DrawCandidate> candidateList= new ArrayList<DrawCandidate>(inData.size());
//            for(DrawObj d : inData) {
//                try {
//                    ViewPortPt vPt= plot.getViewPortCoords(d.getCenterPt());
//                    if (plot.pointInViewPort(vPt)) {
//                        candidateList.add(new DrawCandidate(vPt,d));
//                    }
//                } catch (ProjectionException e) {
//                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                }
//            }
//
//            Dimension dim = plot.getViewPortDimension();
//            float drawArea= dim.getWidth()*dim.getHeight();
//
//            float percentCov= candidateList.size()/drawArea;
//
//            int fuzzLevel= (int)(percentCov*100);
//            if (fuzzLevel>8) fuzzLevel= 8;
//            else if (fuzzLevel<3) fuzzLevel= 3;
//
//            Map<FuzzyVPt, DrawObj> foundPtMap= new HashMap<FuzzyVPt, DrawObj>(inData.size()*2);
//            for(DrawCandidate candidate : candidateList) {
//                FuzzyVPt cenPt= new FuzzyVPt(candidate.getPoint(),fuzzLevel);
//                if (!foundPtMap.containsKey(cenPt)){
//                    foundPtMap.put(cenPt, candidate.getObj());
//                }
//            }
//            retData= new ArrayList<DrawObj>(foundPtMap.values());
//            decimatedData= retData;
//        }
//        else if (decimatedData!=null) {
//            retData= decimatedData;
//        }
//        return retData;
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
