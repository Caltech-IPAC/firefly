/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
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
import edu.caltech.ipac.firefly.visualize.ViewPortPtMutable;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ui.color.Color;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import static edu.caltech.ipac.firefly.visualize.ReplotDetails.Reason;

/**
 * @author Trey Roby, Tatiana Goldina
 * A Drawer  can take one data set and draw it on one WebPlotView.
 * @version $Id: Drawer.java,v 1.54 2012/09/21 23:35:38 roby Exp $
 */
public class Drawer implements WebEventListener, LayerDrawer {

    public static boolean ENABLE_COLORMAP= false;
    public static enum DataType {VERY_LARGE, NORMAL}

    private static int drawerCnt=0;
    private final int drawerID;
    public static final String DEFAULT_DEFAULT_COLOR= "red";
    private String _defColor= DEFAULT_DEFAULT_COLOR;
    private List<DrawObj> _data;
    private List<DrawObj> _highlightData;
    private DrawConnector _drawConnect= null;
    private final WebPlotView _pv;
    private final Drawable _drawable;
    private Graphics primaryGraphics;
    private Graphics selectLayerGraphics;
    private Graphics highlightLayerGraphics;
//    private HtmlGwtCanvas drawBuffer= null;
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

    private List<DrawObj> decimatedData= null;
    private Dimension decimateDim= null;
    private ScreenPt lastDecimationPt= null;
    private String lastDecimationColor= null;
    private boolean highPriorityLayer;
    private boolean selectedFound= false;

//    private static JSLoad _jsLoad = null;


    public Drawer(WebPlotView pv) { this(pv,pv, false); }

    public Drawer(WebPlotView pv, boolean highPriorityLayer) { this(pv,pv,highPriorityLayer); }

    /**
     *
     * @param pv the plotview, optional for this constructor
     * @param drawable an alternate drawable to use instead of the WebPlotView
     */
    public Drawer(WebPlotView pv, Drawable drawable, boolean highPriorityLayer) {
        drawerCnt++;
        drawerID= drawerCnt;
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
        this.highPriorityLayer = highPriorityLayer;
    }

    private void initGraphics() {
        _pv.addListener(Name.REPLOT, this);
        _pv.addListener(Name.PRIMARY_PLOT_CHANGE,this);
        if (_pv==_drawable) {
            _pv.addListener(Name.VIEW_PORT_CHANGE, this);
        }

        initPrimaryGraphics();
        _drawable.addDrawingArea(primaryGraphics.getWidget(), highPriorityLayer);
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

    public void initPrimaryGraphics() {
        primaryGraphics = makeGraphics(_drawable, "mainDrawingLayer");
//        if (HtmlGwtCanvas.isSupported()) {
//            drawBuffer=  new HtmlGwtCanvas();
//            drawBuffer.setLabelPanel((HtmlGwtCanvas)primaryGraphics);
//            drawBuffer.getWidget().setPixelSize(primaryGraphics.getWidget().getOffsetWidth(), primaryGraphics.getWidget().getOffsetHeight());
//        }

    }


    public static Graphics makeGraphics(Drawable drawable, String layerName) {
        Graphics graphics;
        if (BrowserUtil.isOldIE()) {
            graphics= new GWTGraphics();
        }
        else {
            if (HtmlGwtCanvas.isSupported()) {
                graphics= new HtmlGwtCanvas();
            }
            else {
                graphics= new GWTGraphics();
            }
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

    @Override
    public boolean getSupportsRegions() { return true; }

    public void setEnableDecimationDrawing(boolean d) {
        decimate= d;
    }

    public void setPlotChangeDataUpdater(DataUpdater dataUpdater) { _dataUpdater= dataUpdater; }

    public String getDefaultColor() { return _defColor; }


    public WebPlotView getPlotView() { return _pv; }

    private static void draw (Graphics g,
                              AutoColor ac,
                              WebPlot plot,
                              DrawObj obj,
                              ViewPortPtMutable vpPtM,
                              boolean useStateColor,
                              boolean onlyAddToPath) {
        if (obj==null) return;
        if (obj.getSupportsWebPlot()) {
            if (obj instanceof  PointDataObj) ((PointDataObj)obj).draw(g, plot, ac, useStateColor,vpPtM,onlyAddToPath);
            else                               obj.draw(g, plot, ac, useStateColor, onlyAddToPath);
        }
        else {
            obj.draw(g, ac, useStateColor, onlyAddToPath);
        }
    }

    private static void drawConnector(Graphics g, AutoColor ac, WebPlot plot, DrawConnector dc, DrawObj obj, DrawObj lastObj) {
        if (lastObj==null) return;
        if (obj.getSupportsWebPlot()) {
            WorldPt wp1= plot.getWorldCoords(lastObj.getCenterPt());
            WorldPt wp2= plot.getWorldCoords(obj.getCenterPt());
            if (!plot.coordsWrap(wp1,wp2)) {
                dc.draw(g,plot,ac, wp1,wp2);
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
        if (data!=null && _data!=null &&
                data==_data && data.size()==_data.size() &&
                data.size()>0 && _data.get(0)==data.get(0)) {
            return;
        }
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

    @Override
    public boolean hasData() { return _data!=null; }

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
        redrawSelected(selectLayerGraphics, _pv, _data, true);
    }

    public void updateDataHighlightLayer(List<DrawObj> highlightData) {
        initHighlightedGraphicsLayer();
        redrawHighlight(highlightLayerGraphics, _pv, highlightData);
    }


    public void clearSelectLayer() { if (selectLayerGraphics!=null) selectLayerGraphics.clear(); }
    public void clearHighlightLayer() { if (highlightLayerGraphics!=null) highlightLayerGraphics.clear(); }

    public void clear() {
        _data = null;
        cancelRedraw();
        if (primaryGraphics !=null) primaryGraphics.clear();
        if (selectLayerGraphics !=null) selectLayerGraphics.clear();
        if (highlightLayerGraphics !=null) highlightLayerGraphics.clear();
        _cleared= true;
        removeTask();
    }

    private void clearDrawingAreas() {
        if (primaryGraphics !=null) primaryGraphics.clear();
        if (selectLayerGraphics !=null) selectLayerGraphics.clear();
        if (highlightLayerGraphics !=null) highlightLayerGraphics.clear();

    }

    public void redrawDelta(Graphics g, List<Integer>changeIdx) {
        if (g==null) return;
        AutoColor autoColor= makeAutoColor(_pv);
        if (canDraw(g)) {
            _cleared= false;
            WebPlot plot= (_pv==null) ? null : _pv.getPrimaryPlot();
            DrawObj obj;
            ViewPortPtMutable vpPtM= new ViewPortPtMutable();
            for(int idx : changeIdx) {
                obj= _data.get(idx);
                draw(g, autoColor, plot, obj,vpPtM, true, false);
            }
        }
    }

    public void redrawSelected(Graphics graphics, WebPlotView pv, List<DrawObj> data, boolean force) {
        if (graphics==null) return;
        if (!force && !selectedFound) return;
        List<DrawObj> selectedData= new ArrayList<DrawObj>(data.size()/2);
        graphics.clear();
        AutoColor autoColor= makeAutoColor(pv);
        if (canDraw(graphics)) {
            WebPlot plot= (pv==null) ? null : pv.getPrimaryPlot();
            for(DrawObj pt : data) {
                if (pt.isSelected())  selectedData.add(pt);
            }
            selectedData= decimateData(selectedData,null,false);
            ViewPortPtMutable vpPtM= new ViewPortPtMutable();
            for(DrawObj pt : selectedData) {
                draw(graphics, autoColor, plot, pt, vpPtM, true, false);
            }
            selectedFound= selectedData.size()>0;
        }
    }
    public void redrawHighlight(Graphics graphics, WebPlotView pv, List<DrawObj> highlightData) {
        if (graphics!=null) graphics.clear();
        if (graphics==null || highlightData==null || highlightData.size()==0) return;
        if (pv!=null && pv.getPrimaryPlot()==null) return;
        _highlightData= highlightData;
        AutoColor autoColor= makeAutoColor(pv);
        if (canDraw(graphics)) {
            WebPlot plot= (pv==null) ? null : pv.getPrimaryPlot();
            ViewPortPtMutable vpPtM= new ViewPortPtMutable();
            for(DrawObj pt : highlightData) {
                if (pt!=null) draw(graphics, autoColor, plot, pt, vpPtM, true, false);
            }
        }
    }


    private AutoColor makeAutoColor(WebPlotView pv) {
        return (pv==null) ? null : new AutoColor(pv.getPrimaryPlot().getColorTableID(),_defColor);
    }


    public void redraw() {
        redrawPrimary();
        redrawHighlight(highlightLayerGraphics, _pv, _highlightData);
        redrawSelected(selectLayerGraphics, _pv, _data, false);
    }


    public void redrawPrimary() {
        if (primaryGraphics==null || !alive) return;
        clearDrawingAreas();
        if (canDraw(primaryGraphics)) {
            _cleared= false;
            AutoColor autoColor= makeAutoColor(_pv);
            WebPlot plot= _pv.getPrimaryPlot();
            Dimension dim= plot.getViewPortDimension();
            primaryGraphics.setDrawingAreaSize(dim.getWidth(),dim.getHeight());
            List<DrawObj> drawData= decimateData(_data, true);
            if (_dataTypeHint ==DataType.VERY_LARGE && _data.size()>500) {
                DrawingParams params= new DrawingParams(primaryGraphics,autoColor,plot,drawData, getMaxChunk(drawData));
                if (_drawingCmd!=null) _drawingCmd.cancelDraw();
                _drawingCmd= new DrawingDeferred(params);
                _drawingCmd.activate();
                removeTask();
                if (drawData.size()>15000) _plotTaskID= _pv.addTask();
            }
            else {
                DrawingParams params= new DrawingParams(primaryGraphics, autoColor, plot,drawData,Integer.MAX_VALUE);
                doDrawing(params);
            }
        }
        else {
            removeTask();
        }
    }



    private int getMaxChunk(List<DrawObj> drawData) {
        int maxChunk= 1;
        if (drawData.size()==0) return maxChunk;
        DrawObj d= drawData.get(0);
        if (d instanceof PointDataObj) {
            maxChunk= BrowserUtil.isBrowser(Browser.SAFARI) || BrowserUtil.isBrowser(Browser.CHROME) ? 2000 : 500;
        }
        else {
            maxChunk= BrowserUtil.isBrowser(Browser.SAFARI) || BrowserUtil.isBrowser(Browser.CHROME) ? 1000 : 200;
        }
        return maxChunk;
    }

    private List<DrawObj> decimateData(List<DrawObj> inData, boolean useColormap) {
        WebPlot plot= _pv.getPrimaryPlot();
        if (decimate && plot!=null && inData.size()>150) {
            decimatedData= decimateData(inData,decimatedData, useColormap);
            return decimatedData;
        }
        else {
            return inData;
        }
    }

    private List<DrawObj> decimateData(List<DrawObj> inData, List<DrawObj> oldDecimatedData, boolean useColormap) {
        List<DrawObj> retData= inData;
        WebPlot plot= _pv.getPrimaryPlot();
        if (decimate && plot!=null && inData.size()>150 ) {
            Dimension dim = plot.getViewPortDimension();
            ScreenPt spt= plot.getScreenCoords(new ViewPortPt(0,0));
            if (oldDecimatedData==null ||
                    !dim.equals(decimateDim) ||
                    !_defColor.equals(lastDecimationColor) ||
                    !spt.equals(lastDecimationPt)) {
                retData= doDecimation(inData, plot, useColormap);
                lastDecimationColor= _defColor;
                lastDecimationPt=spt;
                decimateDim= dim;
            }
            else if (decimatedData!=null) {
                retData= decimatedData;
            }
        }
        return retData;
    }

    private static boolean getSupportColorMap(List<DrawObj> data) {

        boolean retval= ENABLE_COLORMAP;
        if (ENABLE_COLORMAP) {
            if (data!=null && data.size()>1) {
                DrawObj d= data.get(0);
                retval= d.getSupportDuplicate();
            }
            else {
                retval= false;
            }
        }
        return retval;
    }

    private List<DrawObj> doDecimation(List<DrawObj> inData, WebPlot plot, boolean useColormap) {
        Dimension dim = plot.getViewPortDimension();

        boolean supportCmap= useColormap && getSupportColorMap(inData);

        float drawArea= dim.getWidth()*dim.getHeight();
        float percentCov= inData.size()/drawArea;

//        int fuzzLevel= (int)(percentCov*100);
//        if (fuzzLevel>7) fuzzLevel= 6;
//        else if (fuzzLevel<3) fuzzLevel= 3;


//        double arcsecPerPix= plot.getImagePixelScaleInArcSec() / plot.getZoomFact();
////        int fuzzLevel= (int)(inData.size() / (drawArea / arcsecPerPix));
//        int fuzzLevel= (int )(arcsecPerPix / 2);
//        if (fuzzLevel<2) fuzzLevel= 2;
        int fuzzLevel= 5;


        Date start = new Date();

        int width= dim.getWidth();
        int height= dim.getHeight();

        DrawObj decimateObs[][]= new DrawObj[width][height];
        ViewPortPtMutable seedPt= new ViewPortPtMutable();
        ViewPortPt vpPt;
        Pt pt;
        int maxEntry= -1;
        int entryCnt;

//        GwtUtil.getClientLogger().log(Level.INFO,"doDecimation: " + (enterCnt++) + ",data.size= "+ _data.size() +
//                ",drawID="+drawerID+
//                ",data="+Integer.toHexString(_data.hashCode()));


        List<DrawObj> first200= new ArrayList<DrawObj>(205);
        int decimatedAddCnt= 0;
        int totalInViewPortCnt= 0;

        for(DrawObj obj : inData) {
            if (obj!=null) {
                pt= obj.getCenterPt();
                if (pt instanceof WorldPt) {
                    vpPt= plot.pointInPlotRoughGuess((WorldPt)pt) ? getViewPortCoords(pt,seedPt,plot) : null;
                }
                else {
                    vpPt= getViewPortCoords(pt,seedPt,plot);
                }

            }
            else {
                vpPt= null;
            }
            if (vpPt!=null) {
                int i= nextPt(vpPt.getIX(),fuzzLevel,width);
                int j= nextPt(vpPt.getIY(), fuzzLevel,height);
                if (i>=0 && j>=0 && i<width && j<height) {
                    if (decimateObs[i][j]==null) {
                        decimateObs[i][j]= supportCmap && obj.getSupportDuplicate() ? obj.duplicate() : obj;
                        if (supportCmap) {
                            decimateObs[i][j].setRepresentCnt(obj.getRepresentCnt());
                            entryCnt= decimateObs[i][j].getRepresentCnt();
                            if (entryCnt>maxEntry) maxEntry= entryCnt;
                        }
                        decimatedAddCnt++;
                    }
                    else {
                        if (supportCmap) {
                            decimateObs[i][j].incRepresentCnt(obj.getRepresentCnt());
                            entryCnt= decimateObs[i][j].getRepresentCnt();
                            if (entryCnt>maxEntry) maxEntry= entryCnt;
                        }
                    }
                    if (totalInViewPortCnt<200) first200.add(obj);
                    totalInViewPortCnt++;
                }
            }
        }

        List <DrawObj> retData;
        if (totalInViewPortCnt<200) {
            retData= first200;
        }
        else {
            retData= new ArrayList<DrawObj>(decimatedAddCnt+5);
            for(int i= 0; (i<decimateObs.length); i++) {
                for(int j= 0; (j<decimateObs[i].length); j++) {
                    if (decimateObs[i][j]!=null) {
                        retData.add(decimateObs[i][j]);
                    }
                }
            }
        }



        if (supportCmap) setupColorMap(retData,maxEntry);

//        GwtUtil.getClientLogger().log(Level.INFO, "Drawer.doDecimation: created "+retData.size()+" objects from "+inData.size()+" in "+
//            ((new Date()).getTime()-start.getTime())+"ms");


        return retData;
    }


    private void setupColorMap(List<DrawObj> data, int maxEntry) {
        String colorMap[]= makeColorMap(maxEntry);
        if (colorMap!=null)  {
            if (maxEntry>colorMap.length) {
                int maxCnt = maxEntry+1; // to include draw obj with cnt==maxEntry into the last color band
                for(DrawObj obj : data) {
                    int cnt= obj.getRepresentCnt();
                    int idx = cnt*colorMap.length/maxCnt;
                    obj.setColor(colorMap[idx]);
                }
            }  else {
                for(DrawObj obj : data) {
                    int cnt= obj.getRepresentCnt();
                    //if (cnt>colorMap.length) cnt=colorMap.length;
                    obj.setColor(colorMap[cnt-1]);
                }
            }
        }
    }

    private static ViewPortPt getViewPortCoords(Pt pt, ViewPortPtMutable mVpPt, WebPlot plot) {
        ViewPortPt retval;
        if (pt instanceof WorldPt) {
            boolean success= plot.getViewPortCoordsOptimize((WorldPt)pt,mVpPt);
            retval= success ? mVpPt : null;
        }
        else {
            retval= plot.getViewPortCoords(pt);
        }
        return retval;
    }

    /*
    private static void setCmapColor(DrawObj obj, String colorMap[], int maxCnt)  {
        int cnt= obj.getRepresentCnt();
        if (maxCnt>colorMap.length) {
            //  linear
            int idx = cnt*colorMap.length/maxCnt;
            obj.setColor(colorMap[idx]);
        } else {
            if (cnt>colorMap.length) cnt=colorMap.length;
            obj.setColor(colorMap[cnt-1]);
        }
    }
    */


    private String[] makeColorMap(int mapSize) {
        AutoColor ac= new AutoColor(_pv.getPrimaryPlot().getColorTableID(),_defColor);
        String base= ac.getColor(_defColor);
        return Color.makeSimpleColorMap(base,mapSize,true);
    }

    private static ViewPortPtMutable getMutableVP(ViewPortPt vpPt) {
        if ((vpPt!=null && vpPt instanceof ViewPortPtMutable)) {
            return (ViewPortPtMutable)vpPt;
        }
        else {
            return new ViewPortPtMutable();
        }
    }

    private static int nextPt(int i,int fuzzLevel, int max) {
        int remainder= i%fuzzLevel;
        int retval= (remainder==0) ? i : i+(fuzzLevel-remainder);
        if (retval==max) retval= max-1;
        return retval;
    }


    private void doDrawing(DrawingParams params) {
        if (params._begin) {
            params._begin= false;
            GwtUtil.setHidden(params._graphics.getWidget(), true);
            params._done= false;
        }
        else {
            params._deferCnt++;
        }


        if (!params._done) {
            if (_drawConnect!=null) _drawConnect.beginDrawing();
            try {
                DrawChunkData nextChunk= getNextChuck(params);
                if (nextChunk.optimize) {
                    drawChunkOptimized(nextChunk.drawList, params, params._graphics);
                    params.opCnt++;
                }
                else {
                    drawChunkNormal(nextChunk.drawList, params, params._graphics);
                }
            } catch (ConcurrentModificationException e) {
                GwtUtil.getClientLogger().log(Level.SEVERE,"size= "+ _data.size(),e);
                params._done= true;
            }
            if (!params._iterator.hasNext()) { //loop finished
                params._done= true;
                removeTask();
            }
        }
        if (params._done ) {
            GwtUtil.setHidden(primaryGraphics.getWidget(),false);
            if (_drawConnect!=null) {
                _drawConnect.endDrawing();
            }
//            if (useBuffer) {
//                ((AdvancedGraphics)params._graphics).copyAsImage((AdvancedGraphics)params.drawBuffer);
//            }
            if (_data.size()>100) { //todo remove
//                long delta= System.currentTimeMillis()-params.startTime;//TODO remove
//                GwtUtil.getClientLogger().log(Level.INFO,"Redraw, Op Cnt:"+params.opCnt+", "+ _data.size() + " rows: "+ delta+ "ms"); //TODO remove
            } // todo remove
        }

    }



    private DrawChunkData getNextChuck(DrawingParams params) {
//        long startTime= System.currentTimeMillis(); //todo remove
        List<DrawObj> retList= new ArrayList<DrawObj>(Math.min(params._maxChunk,params._data.size()));
        DrawObj obj;

        boolean canOptimize= _drawConnect==null;
        String color= null;
        int lineWidth= 0;


        for(int i= 0; (params._iterator.hasNext() && i<params._maxChunk ); ) {
            obj= params._iterator.next();
            if (_drawConnect==null) {
                if (doDraw(params._plot, obj)) {
                    retList.add(obj);
                    if (i==0) {
                        color= obj.calculateColor(params._ac,false);
                        lineWidth= obj.getLineWidth();
                    }
                    if (canOptimize) {
                        canOptimize= obj.getCanUsePathEnabledOptimization() &&
                                     lineWidth>0 &&
                                     lineWidth==obj.getLineWidth() &&
                                     ComparisonUtil.equals(color,obj.calculateColor(params._ac,false));
                    }
                    i++;
                }
            }
            else {
                retList.add(obj);
                i++;
            }
        }
//        long delta= System.currentTimeMillis()-startTime;//TODO remove
//        if (retList.size()>2)GwtUtil.getClientLogger().log(Level.INFO,"Next Chunk:"+ delta+ "ms, from start:"+(System.currentTimeMillis()-params.startTime)+"ms"); //TODO remove
        return new DrawChunkData(retList,canOptimize);
    }

    private void drawChunkOptimized(List<DrawObj> drawList, DrawingParams params, Graphics graphics) {
//        long startTime= System.currentTimeMillis(); //todo remove
        if (drawList.size()==0) return;
        DrawObj d= drawList.get(0);
        graphics.beginPath(d.calculateColor(params._ac, false), d.getLineWidth());
        for(DrawObj obj : drawList) {
            draw(graphics, params._ac, params._plot, obj,params.vpPtM, false,true);
        }
        graphics.drawPath();
//        long delta= System.currentTimeMillis()-startTime;//TODO remove
//        if (drawList.size()>10) GwtUtil.getClientLogger().log(Level.INFO,"draw Chunk Optimize:"+ delta+ "ms, from start:"+(System.currentTimeMillis()-params.startTime)+"ms"); //TODO remove
    }

    private void drawChunkNormal(List<DrawObj> drawList, DrawingParams params, Graphics graphics) {
        DrawObj lastObj= null;
        for(DrawObj obj : drawList) {
            if (_drawConnect!=null) { // in this case doDraw was already called
                draw(graphics, params._ac, params._plot, obj,params.vpPtM, false,false);
            }
            else  {
                if (doDraw(params._plot,obj)) { // doDraw must be call when there is a connector
                    draw(graphics, params._ac, params._plot, obj,params.vpPtM, false, false);
                    if (_drawConnect!=null) {
                        drawConnector(graphics,params._ac,params._plot,_drawConnect,obj,lastObj);
                    }
                }
                lastObj= obj;
            }
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


    private boolean canDraw(Graphics graphics) {
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
        else if (name.equals(Name.VIEW_PORT_CHANGE)) {
            if (_pv.getPrimaryPlot()!=null) redraw();
        }
        else if (name.equals(Name.REPLOT)) {
            ReplotDetails details= (ReplotDetails)ev.getData();
            Reason reason= details.getReplotReason();
            if (reason!=Reason.IMAGE_RELOADED &&  reason!=Reason.ZOOM &&
                reason!=Reason.ZOOM_COMPLETED &&  reason!=Reason.REPARENT) {
                clearDrawingAreas();
            }
        }
    }


    private class DrawingDeferred implements IncrementalCommand {

        DrawingParams _params;

        public DrawingDeferred(DrawingParams params) {
            _params= params;
        }

        public boolean execute() {
            if (!_params._plot.isAlive()) _params._done= true;
            if (!_params._done) doDrawing(_params);
            return !_params._done;
        }

        public void cancelDraw() { _params._done= true; }

        public void activate() {
            DeferredCommand.addCommand(this);
        }
    }


    private static class DrawingParams {
        private long startTime= System.currentTimeMillis(); //todo only for debug
        private int opCnt= 0; //only for debug
        final WebPlot _plot;
        private final List<DrawObj> _data;
//        final List<DrawObj> _onTop;
        final Iterator<DrawObj> _iterator;
        final int _maxChunk;
        boolean _done= false;
        boolean _begin= true;
        final AutoColor _ac;
        final Graphics _graphics;
        int _deferCnt= 0;
        final ViewPortPtMutable vpPtM= new ViewPortPtMutable();

        DrawingParams(Graphics graphics, AutoColor ac, WebPlot plot, List<DrawObj> data, int maxChunk) {
            _graphics= graphics;
            _ac= ac;
            _plot= plot;
            _data= data;
            _iterator= _data.iterator();
            _maxChunk= maxChunk;
        }
    }


    public interface CompleteNotifier {
        public void done();
    }

    public interface DataUpdater {
        List<DrawObj> getData();
    }

    private class DrawChunkData {
        public List<DrawObj> drawList;
        public boolean optimize;

        private DrawChunkData(List<DrawObj> drawList, boolean optimize) {
            this.drawList = drawList;
            this.optimize= optimize;
        }
    }


//    public static class DrawCandidate {
//        private final ViewPortPt vp;
//        private final DrawObj obj;
//
//        public DrawCandidate(ViewPortPt vp, DrawObj obj) {
//            this.vp = vp;
//            this.obj = obj;
//        }
//
//        public DrawObj getObj() { return obj; }
//        public ViewPortPt getPoint() { return vp; }
//    }



//    public static class MyLoaded implements JSLoad.Loaded {
//        CompleteNotifier _ic;
//        public MyLoaded(CompleteNotifier ic)  { _ic= ic; }
//        public void allLoaded() { _ic.done(); }
//    }







}
