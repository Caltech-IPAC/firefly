package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.HasWebEventManager;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.PropertyChangeData;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class is the "gui canvas" that a plot is painted on.  It is a 
 * subclass of Composite so it can provide all the paint and graphics
 * interface into GWT.  It also manages multiple plot classes.  Currently
 * This is one of the most key classes in the all vis packages.
 *
 * Displaying a plot requires managing multiple layers of widgets.  The _masterPanel does this. The _masterPanels sits
 * in a scroll area.
 * The layer go from bottom to top in the following order
 * <ol>
 *     <li>_primaryPlot - the actual fits images</li>
 *     <li>_drawable - the overlays that are drawn on top of the image.  This is made of up multiple layers</li>
 *     <li>_mouseMoveArea - this captures mouse move events</li>
 * </ol>
 *
 * @see WebPlot
 *
 * @author Trey Roby
 * @version $Id: WebPlotView.java,v 1.84 2012/05/31 23:12:16 roby Exp $
 * *
 */
public class WebPlotView extends Composite implements Iterable<WebPlot>, Drawable, RequiresResize, HasWebEventManager {


    enum StatusChangeType {ADDED, REMOVED}
    public  static final String PRIMARY_PLOT= "PrimaryPlot";

    public static final String TASK= "task-";
    private static final int AUTO = 80456;
    private static int _taskCnt=0;

    private ArrayList<WebPlot>      _plots       = new ArrayList<WebPlot>(10);
    private Map<WebPlot,ScrollInfo> _scrollInfo= new HashMap<WebPlot,ScrollInfo>(10);
    private WebPlot                 _primaryPlot= null;
    private ScrollInfo              _primaryScrollInfo= null;

    private final Map<String,Object>  _attributes= new HashMap<String,Object>(3);
    private Widget _maskWidget= null;
    private FocusPanel _mouseMoveArea= new FocusPanel();
    private AbsolutePanel _masterPanel= new AbsolutePanel();

    private ScrollPanel _scrollPanel= new ScrollPanel(_masterPanel);
    private DefaultDrawable _drawable= new DefaultDrawable();
    private final PVMouse _pvMouse;
    private final WebEventManager _eventManager= new WebEventManager();
    private boolean _lockPlotHint= false;
    private Map<String,WebLayerItem> _userDrawLayerMap = new TreeMap<String, WebLayerItem>();
    private MiniPlotWidget _mpw= null;
    private boolean _eventsEnabled= true;
    private List<String> _workingTaskList= new ArrayList<String>(50);
    private List<ScrollHandler> _scrollHandlerList= new ArrayList<ScrollHandler>(3);
    private boolean _fixScrollInProgress= false;
    private boolean _alive= true;
    private boolean scrollBarEnabled= false;





  /**
   * Create a new PlotView.  This constructor is typically used when the
   * PlotView <em>will</em> be placed insides a scrolled window.
   */
  public WebPlotView() {

      initWidget(_scrollPanel);
      _pvMouse= new PVMouse(this, _mouseMoveArea);

      _scrollPanel.addStyleName("web-plot-view");
      _mouseMoveArea.addStyleName("event-layer");
      _masterPanel.addStyleName("plot-view-master-panel");

      _scrollPanel.addScrollHandler(new PVScrollHandler());
  }


    public void freeResources() {
        _pvMouse.freeResources();
        if (_plots!=null) _plots.clear();
        if (_scrollInfo!=null) _scrollInfo.clear();
        _plots= null;
        _scrollInfo= null;
        _eventManager.clear();
        _alive= false;
    }

    public boolean isAlive() { return _alive; }

    public void notifyWidgetShowing() {
        if (_primaryPlot!=null) {
            recomputeViewPort();
        }
    }

    public AbsolutePanel getDrawingPanelContainer() { return _drawable.getDrawingPanelContainer(); }

    //====================================================================
    //------------------- from HasWebEventManager interface
    //====================================================================

    public WebEventManager getEventManager() { return _eventManager; }

    public void addListener(WebEventListener l) { _eventManager.addListener(l); }

    public void addListener(Name eventName, WebEventListener l) {
        _eventManager.addListener(eventName,l);
    }

    public void removeListener(WebEventListener l) {
        _eventManager.removeListener(l);
    }

    public void removeListener(Name eventName, WebEventListener l) {
        _eventManager.removeListener(eventName,l);
    }

    public void fireEvent(WebEvent ev) {
        if (_eventsEnabled) _eventManager.fireEvent(ev);
    }
    //===================================================================
    //===================================================================

    public Widget addDrawingArea(Widget w, boolean highPriority) { return _drawable.addDrawingArea(w, highPriority); }
    public void removeDrawingArea(Widget w) { _drawable.removeDrawingArea(w); }
    public void replaceDrawingArea(Widget old, Widget w) { _drawable.replaceDrawingArea(old,w);}
    public void insertBeforeDrawingArea(Widget before, Widget w) { _drawable.insertBeforeDrawingArea(before, w); }
    public void insertAfterDrawingArea(Widget after, Widget w) { _drawable.insertAfterDrawingArea(after,w); }
    public int getDrawingWidth() { return _drawable.getDrawingWidth(); }
    public int getDrawingHeight() { return _drawable.getDrawingHeight();  }

    public void onResize() {
        _scrollPanel.onResize();
        recomputeWcsOffsets();
    }

    public void setMiniPlotWidget(MiniPlotWidget mpw) { _mpw= mpw; }

    public MiniPlotWidget getMiniPlotWidget() { return _mpw; }


    public void setAttribute(String key, Object attribute) { _attributes.put(key,attribute); }
    public void removeAttribute(String key) { _attributes.remove(key); }
    public Object getAttribute(String key) { return _attributes.get(key); }
    public boolean containsAttributeKey(String key) { return _attributes.containsKey(key); }


    public int getUserDrawerLayerListSize() { return _userDrawLayerMap.size(); }

    public WebLayerItem getItemByID(String id) { return _userDrawLayerMap.get(id); }

    public WebLayerItem getItemByTitle(String title) {
        WebLayerItem retval= null;
        if (title!=null) {
            for(WebLayerItem wl : _userDrawLayerMap.values()) {
                if (title.equals(wl.getTitle())) {
                    retval= wl;
                    break;
                }
            }
        }
        return retval;
    }

    public Collection<WebLayerItem> getUserDrawerLayerSet() {
        return Collections.unmodifiableCollection(_userDrawLayerMap.values());
    }

    public void addWebLayerItem(WebLayerItem item) {
        if (!_userDrawLayerMap.containsKey(item.getID())) {
            item.setPlotView(this);
            _userDrawLayerMap.put(item.getID(),item);
            WebEvent ev= new WebEvent<WebLayerItem>(this, Name.LAYER_ITEM_ADDED, item);
            fireEvent(ev);
        }
    }


    public void removeWebLayerItem(WebLayerItem item) {
        if (_userDrawLayerMap.containsKey(item.getID())) {
            _userDrawLayerMap.remove(item.getID());
            WebEvent ev= new WebEvent<WebLayerItem>(this, Name.LAYER_ITEM_REMOVED, item);
            fireEvent(ev);
        }
    }

    public void setWebLayerItemActive(WebLayerItem item, boolean active) {
        if (_userDrawLayerMap.containsKey(item.getID())) {
            if (item.isActive()!=active) {
                item.setActive(active);
                WebEvent ev= new WebEvent<WebLayerItem>(this, Name.LAYER_ITEM_ACTIVE, item);
                fireEvent(ev);
            }

        }
    }

    public String addTask() {
        _taskCnt++;
        String task= TASK+_taskCnt;
        _workingTaskList.add(task);
        if (_workingTaskList.size()==1) {
            WebEvent ev= new WebEvent<WebPlotView>(this, Name.PLOT_TASK_WORKING, this);
            fireEvent(ev);
        }
        return task;

    }


    public void removeTask(String id) {
        if (id!=null && _workingTaskList.contains(id)) {
            _workingTaskList.remove(id);
            if (_workingTaskList.size()==0) {
                WebEvent ev= new WebEvent<WebPlotView>(this, Name.PLOT_TASK_COMPLETE, this);
                fireEvent(ev);
            }
        }
    }

    public boolean isTaskWorking() { return _workingTaskList.size()>0; }


    public void setScrollBarsEnabled(boolean enabled) {
        scrollBarEnabled= enabled;
        setScrollBarsEnabledInternal(scrollBarEnabled);
    }

    private void setScrollBarsEnabledInternal(boolean enabled) {
        GwtUtil.setStyle(_scrollPanel, "overflow", enabled ?"auto" : "hidden");
    }

    public int getScrollX() {
        int scrollX= 0;
        if (_primaryPlot!=null) {
            scrollX= getScrollScreenPos().getIX();
        }
        return scrollX;
    }

    public int getScrollY() {
        int scrollY= 0;
        if (_primaryPlot!=null) {
            scrollY= getScrollScreenPos().getIY();
        }
        return scrollY;
    }

    public ScreenPt getScrollScreenPos() {
        ScreenPt retval;
        if (_primaryPlot!=null) {
            Element body = _scrollPanel.getElement();
            retval= new ScreenPt(body.getScrollLeft(), body.getScrollTop());
        }
        else {
            retval= new ScreenPt(0,0);
        }
        return retval;

    }

    public void refreshDisplay() {
        if (_primaryPlot!=null) {
            int x= getScrollX();
            int y= getScrollY();
            int w= _scrollPanel.getOffsetWidth();
            int h= _scrollPanel.getOffsetHeight();
            if (w*h>0) {
                _primaryPlot.drawTilesInArea(new ScreenPt(x,y),w,h);
            }
        }
    }

    public void setTouchScrollingEnabled(boolean enable) {
        _scrollPanel.setTouchScrollingDisabled(!enable);
    }

//    public ViewPortPt getScrollViewPortPos() {
//        Element body = _scrollPanel.getElement();
//        return new ViewPortPt(body.getScrollLeft(), body.getScrollTop());
//    }


    public int getScrollWidth() { return DOM.getElementPropertyInt(_scrollPanel.getElement(), "clientWidth"); }
    public int getScrollHeight() { return DOM.getElementPropertyInt(_scrollPanel.getElement(), "clientHeight"); }

    public void scrollDragEnded() { setScrollXY(getScrollScreenPos()); }
    public void setScrollXY(int x, int y) { setScrollXY(new ScreenPt(x,y)); }
    public void setScrollXY(ScreenPt spt) { setScrollXY(spt,false); }

    public void setScrollXY(ScreenPt spt, boolean dragging) {
        if (_primaryPlot!=null) {
            int x= spt.getIX();
            int y= spt.getIY();

            if (x<0) x= 0;
            else if (x>_primaryPlot.getScreenWidth()) x= _primaryPlot.getScreenWidth()-1;

            if (y<0) y= 0;
            else if (y>_primaryPlot.getScreenHeight()) y= _primaryPlot.getScreenHeight()-1;


            spt= new ScreenPt( x,y);



            ViewPortPt vpt= _primaryPlot.getViewPortCoords(spt);
            if (!pointInViewPortBounds(vpt) && !dragging) {
//                ScreenPt other= findOtherExtreme(spt);
//                int avX= (spt.getIX()+other.getIX())/2;
//                int avY= (spt.getIY()+other.getIY())/2;
                recomputeViewPortIfNecessary();
            }
            _scrollPanel.setHorizontalScrollPosition(spt.getIX());
            _scrollPanel.setVerticalScrollPosition(spt.getIY());
            _fixScrollInProgress= false;
        }
    }


    private void recomputeViewPortIfNecessary() {
        if (_primaryPlot==null) return;
        int sw= getScrollWidth();
        int sh= getScrollHeight();
        int x= getScrollX();
        int y= getScrollY();
        Dimension dim= _primaryPlot.getViewPortDimension();
        boolean contains= VisUtil.containsRec(_primaryPlot.getViewPortX(),_primaryPlot.getViewPortY(),
                                              dim.getWidth(),dim.getHeight(),
                                              x,y,sw-1,sh-1);
        if (!contains) {
            recomputeViewPort();
        }
        else {
//            int sx= _scrollPanel.getHorizontalScrollPosition();
//            int sy= _scrollPanel.getVerticalScrollPosition();
//            Dimension vpDim= _primaryPlot.getViewPortDimension();
//            GwtUtil.showDebugMsg("viewport is good<br>"+
//                                 "spos: "+sx+","+sy+"<br>"+
//                                 "sdim: "+getScrollWidth()+ ","+getScrollHeight()+"<br>"+
//                                 "vpos: "+_primaryPlot.getViewPortX()+","+_primaryPlot.getViewPortY()+"<br>"+
//                                 "vdim: "+vpDim.getWidth()+ ","+vpDim.getHeight(),
//                                 true);

        }

    }



    private void recomputeViewPort() {
        if (_primaryPlot!=null) {
            recomputeViewPort(_primaryPlot.getScreenCoords(findCurrentCenterPoint()));
        }
    }

    private void recomputeViewPort(ScreenPt visibleCenterPt) {
        int screenW= _primaryPlot.getScreenWidth();
        int screenH= _primaryPlot.getScreenHeight();
        int vpw= getScrollWidth()*2;
        int vph= getScrollHeight()*2;

        if (vpw> 1500) vpw= (int)(getScrollWidth() * 1.5);
        if (vpw>2700) vpw= 2700;

        if (vph> 1400) vph= (int)(getScrollHeight() * 1.5);
        if (vph>1800) vph= 1800;

        int newX;
        int newY;

        if (vpw>screenW) {
            vpw= screenW;
            newX= 0;
        }
        else {
            newX= visibleCenterPt.getIX()- vpw/2;
            if (newX<0) newX= 0;
            else if (newX+vpw>screenW) newX=  screenW-vpw;
        }

        if (vph>screenH) {
            vph= screenH;
            newY= 0;
        }
        else {
            newY= visibleCenterPt.getIY()- vph/2;
            if (newY<0) newY= 0;
            else if (newY+vph>screenH) newY=  screenH-vph;
        }

        if (vpw>0 && vph>0 && isViewPortDifferent(newX,newY,vpw,vph)) {
            _primaryPlot.setViewPort(newX, newY, vpw, vph);
            recomputeSize();

            WebEvent ev= new WebEvent<WebPlot>(this, Name.VIEW_PORT_CHANGE, _primaryPlot);
            fireEvent(ev);
            int sx= _scrollPanel.getHorizontalScrollPosition();
            int sy= _scrollPanel.getVerticalScrollPosition();
            _primaryPlot.drawTilesInArea(new ScreenPt(sx,sy),getScrollWidth(),getScrollHeight());
//            GwtUtil.showDebugMsg("viewport: <br>"+newX+","+newY + "<br>w:"+vpw+", h:"+vph,true);
        }

    }

    private boolean isViewPortDifferent(int x, int y, int w, int h) {
        Dimension dim= _primaryPlot.getViewPortDimension();
        boolean same=  (x==_primaryPlot.getViewPortX() &&
                        y==_primaryPlot.getViewPortY() &&
                        w==dim.getWidth() &&
                        h==dim.getHeight());
        return !same;
    }


    private void recomputeSize() {
        if (_primaryPlot==null) return;

        _masterPanel.setPixelSize(_primaryPlot.getScreenWidth(), _primaryPlot.getScreenHeight() );

        _mouseMoveArea.setPixelSize(_primaryPlot.getScreenWidth(), _primaryPlot.getScreenHeight() );


        int vpx= _primaryPlot.getViewPortX();
        int vpy= _primaryPlot.getViewPortY();
        Dimension dim= _primaryPlot.getViewPortDimension();

        _masterPanel.setWidgetPosition(_drawable.getDrawingPanelContainer(),vpx,vpy);
        _drawable.setPixelSize(dim.getWidth(),dim.getHeight());

    }

    private void setMarginXY(int x, int y) {
        String lStr= (x==AUTO) ? "auto" : x+"px";
        String tStr= (y==AUTO) ? "auto" : y+"px" ;
        GwtUtil.setStyles(_masterPanel, "marginLeft",  lStr,
                                        "marginRight","auto",
                                        "marginTop",tStr
                                     );
    }

    private boolean pointInViewPortBounds(ViewPortPt vpt) {
        boolean inBounds= _primaryPlot.pointInViewPort(vpt);
        if (inBounds) {
            ScreenPt spt= _primaryPlot.getScreenCoords(vpt);
            ScreenPt otherExtreme= findOtherExtreme(spt);
            inBounds= _primaryPlot.pointInViewPort(otherExtreme);
        }
        return inBounds;
    }

    private ScreenPt findOtherExtreme(ScreenPt spt) {
        int mw= _primaryPlot.getScreenWidth() - spt.getIX();
        int mh= _primaryPlot.getScreenHeight()- spt.getIY();
        int x= spt.getIX()+ (getScrollWidth() <mw ? getScrollWidth()  : mw);
        int y= spt.getIY()+ (getScrollHeight()<mh ? getScrollHeight() : mh);
        return new ScreenPt(x,y);
    }


    public void fixScrollPosition() {
        if (BrowserUtil.isIE() && _fixScrollInProgress) return;
        final Element body = _scrollPanel.getElement();
        final int scrollX = body.getScrollLeft();
        final int scrollY = body.getScrollTop();
        DeferredCommand.addPause();
        _fixScrollInProgress= true;
        Command cmd=  new Command() {
            public void execute() {
                int sx = body.getScrollLeft();
                int sy = body.getScrollTop();
                if (_fixScrollInProgress && sx==0 && sy==0) { // fix is necessary
                    _scrollPanel.setHorizontalScrollPosition(scrollX);
                    _scrollPanel.setVerticalScrollPosition(scrollY);
                }
                _fixScrollInProgress= false;
            }
           };
        DeferredCommand.addCommand(cmd);
    }



    public FocusPanel getMouseMove() { return _mouseMoveArea; }


    public Widget getMaskWidget() {
        return _maskWidget!=null ? _maskWidget : this;
    }

    public void setMaskWidget(Widget w) { _maskWidget= w; }

    /**
     * Add a plot to list of plots this PlotView contains.
     * This method will fire the PlotViewStatusLister.plotAdded() to
     * all listeners.
     * @param p the Plot to add
     */
    public void addPlot(WebPlot p) {
        addPlot(p,true);
    }

    /**
     * Add a plot to list of plots this PlotView contains.
     * This method will fire the PlotViewStatusLister.plotAdded() to
     * all listeners.
     * @param p the Plot to add
     * @param makePrimary make the plot just added primary
     */
    public void addPlot(WebPlot p, boolean makePrimary) {
        addPlot(p,_plots.size(),makePrimary);
    }

    public void addPlot(WebPlot p, int idx, boolean makePrimary) {
        _plots.add(idx, p);
        _scrollInfo.put(p, new ScrollInfo(p));
        p.getPlotGroup().setPlotView(this);
        if (makePrimary) setPrimaryPlot(p);
        fireStatusChanged(StatusChangeType.ADDED,p );
    }

    /**
     * Remove a plot from list of plots this PlotView contains.
     * If the plot to remove is current then the first plot in the list is made
     * current. This method will fire the PlotViewStatusLister.plotRemoved() to
     * all listeners.
     * @param p the Plot to remove
     * @param freeResources removes any resources this plot is using and calls the server to do the same
     */
    public void removePlot(WebPlot p, boolean freeResources) {
        p.freeResources();
        WebPlot primary= getPrimaryPlot();
        p.getPlotGroup().removePlotView(this);
        _plots.remove(p);
        ScrollInfo sInfo= _scrollInfo.get(p);
        if (sInfo!=null) {
            _scrollInfo.remove(p);
        }
        if (p == primary) {
            setPrimaryPlot(_plots.size() > 0 ? _plots.get(0) : null);
        }
        fireStatusChanged(StatusChangeType.REMOVED,p );

        if (freeResources) freePlotResources(p);

    }


    private void freePlotResources(WebPlot p) {
        p.freeResources();
        VisTask.getInstance().deletePlot(p);
    }


    public void clearAllPlots() {
        if (_plots.size()>0) {
            PropertyChangeData data= new PropertyChangeData( PRIMARY_PLOT, _primaryPlot,null);
            WebEvent ev= new WebEvent<PropertyChangeData>(this, Name.PRIMARY_PLOT_CHANGE, data);
            _eventsEnabled= false;

            WebPlot allPlots[]= _plots.toArray(new WebPlot[_plots.size()]);
            setPrimaryPlot(allPlots[allPlots.length-1]);
            for(WebPlot p : allPlots) removePlot(p,true);
            _eventsEnabled= true;
            fireEvent(ev);
        }
    }


    /**
     * Return the plot that is primary.  This is the one that the user sees.
     * @return Plot the primary Plot
     */
    public WebPlot getPrimaryPlot() { return _primaryPlot; }

    public boolean contains(WebPlot p) { return p==null ? false : _plots.contains(p); }

    /**
     * Return a plot in this <code>PlotView</code>'s list at a given index.
     * @param i the index of the plot.
     * @return Plot the plot and the index or null if it is not found.
     */
    public WebPlot getPlot(int i)   {
        WebPlot p= null;
        if (_plots.size() > i) {
            p= _plots.get(i);
        }
        return p;
    }

    /**
     * Return the number plots in this <code>PlotView</code>'s list.
     * @return int the number of plots in the list
     */
    public int size() { return _plots.size(); }

    public int indexOf(WebPlot plot) { return _plots.indexOf(plot);  }

    public WebPlot get(int idx) { return _plots.get(idx);  }

    public void setPrimaryPlot(WebPlot p) {
        WebPlot old= _primaryPlot;

        PropertyChangeData data= new PropertyChangeData( PRIMARY_PLOT, old,p);
        WebEvent ev= new WebEvent<PropertyChangeData>(this, Name.PRIMARY_PLOT_CHANGE, data);

        if (p==null) {
            _primaryPlot= null;
            fireEvent(ev);
            _masterPanel.clear();
            _primaryScrollInfo= null;
        }
        else if (_plots.contains(p)) {


            ScrollInfo recallInfo= _scrollInfo.get(p).makeCopy();
            _masterPanel.clear();
            _masterPanel.add(p.getWidget(),0,0);
            _masterPanel.add(_drawable.getDrawingPanelContainer(),0,0);
            _masterPanel.add(_mouseMoveArea,0,0);

            DOM.setStyleAttribute(_mouseMoveArea.getElement(),"backgroundColor", "transparent");

            _primaryPlot= p;
            _primaryScrollInfo= _scrollInfo.get(p);
            recomputeViewPort(new ScreenPt(p.getScreenWidth()/2,p.getScreenHeight()/2));

            fireEvent(ev);
            recallScrollPos(recallInfo);

            AllPlots ap= AllPlots.getInstance();
            if (ap.getMiniPlotWidget()==getMiniPlotWidget()) {
                ap.setSelectedWidget(getMiniPlotWidget(),true,false);
            }

        }
        else {
            // this plot is not in the list - do nothing
        }
    }


    public void setZoomTo(float zoomLevel,
                          boolean isFullScreen,
                          boolean useDeferredDelay) {
        if (_primaryPlot!=null) {
            float currZoomFact= _primaryPlot.getZoomFact();
            final ImageWorkSpacePt pt= findCurrentCenterPoint();
            if (!ComparisonUtil.equals(zoomLevel,currZoomFact,4)) {
                _primaryPlot.getPlotGroup().activateDeferredZoom(zoomLevel, isFullScreen,useDeferredDelay);
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        recomputeViewPort(_primaryPlot.getScreenCoords(pt));
                        centerOnPoint(pt);
                    }
                });
                DeferredCommand.addPause();
            }
            else {
                if (isWcsSync()) wcsSyncCenter(AllPlots.getInstance().getWcsSyncCenter());
                else             centerOnPoint(pt);
            }

        }
    }


    public void setZoomByArcsecPerScreenPix(float arcsecPerScreenPix,
                                            boolean isFullScreen,
                                            boolean useDeferredDelay) {
        if (_primaryPlot!=null) {
            if (!Float.isNaN(arcsecPerScreenPix)) {
                setZoomTo( (float)_primaryPlot.getImagePixelScaleInArcSec() / arcsecPerScreenPix,
                           isFullScreen,useDeferredDelay );
            }
            else {
                setZoomTo(1F,isFullScreen,useDeferredDelay);
            }
        }
    }

    public void setZoomByPlotWidth(int width, boolean isFullScreen, boolean useDeferredDelay) {
        if (_primaryPlot!=null) {
            if (!Double.isNaN(_primaryPlot.getImagePixelScaleInArcSec())) {
                setZoomTo( (float)_primaryPlot.getImagePixelScaleInArcSec() * width, isFullScreen, useDeferredDelay);
            }
            else {
                setZoomTo(1F,isFullScreen, useDeferredDelay);
            }
        }
    }


    public void showMouseHelp(WebLayerItem item) {
        if (item.getHelp()!=null) {
            showMouseHelp(item.getHelp());
        }
    }


    public void showMouseHelp(String text) {
        showMouseHelp(new HTML(text));
    }

    public void showMouseHelp(Widget w) {
        w.setWidth("100%");
        w.setHeight("100%");
        if (getMiniPlotWidget().isExpanded()) {
            int width= (int)(getOffsetWidth()*.75);
            PopupUtil.showMinimalMsg(this,w,5,PopupPane.Align.VISIBLE_BOTTOM,width);
        }
        else {
            int width= getScrollWidth();
            PopupUtil.showMinimalMsg(this,w,5,PopupPane.Align.BOTTOM_CENTER_POPUP_BOTTOM,width);
        }
    }

  /**
   * return a iterator though all the plots in this <code>PLotView</code>. 
   * @return Iterator iterator through all the plots.
   */
  public Iterator<WebPlot> iterator() { return new PlotIterator(_plots.iterator()); }


    public boolean isLockedHint() {
        return _lockPlotHint;
    }

    public void setLockedHint(boolean lockPlotHint) {
        _lockPlotHint= lockPlotHint;
        WebEvent ev= new WebEvent<Boolean>(this,Name.PLOTVIEW_LOCKED,lockPlotHint);
        fireEvent(ev);
    }





  // ------------------------------------------------------------
  // ================= Private / Protected methods ==============
  // ------------------------------------------------------------



  void reconfigure() {
      recomputeViewPort(_primaryPlot.getScreenCoords(findCurrentCenterPoint()));
  }
  /**
   * fire the <code>PlotViewStatusListener</code>s. 
   * @param stat a StatusChangeType
   * @param plot that the event is about.
   */
  protected void fireStatusChanged(StatusChangeType stat, WebPlot plot) {
      WebEvent ev;
      switch (stat) {
          case ADDED :
              ev= new WebEvent<WebPlot>(this, Name.PLOT_ADDED, plot);
              fireEvent(ev);
              break;
          case REMOVED :
              ev= new WebEvent<WebPlot>(this, Name.PLOT_REMOVED, plot);
              fireEvent(ev);
              break;
          default:
              WebAssert.argTst(false,stat + " is unknown to this switch" );
      }

  }

  /**

  /**
   * Find the point in the <code>WebPlot</code> that is at the center of
   * the display.  The point returned is in ImageWorkSpacePt coordinates.
   * We return it in and ImageWorkSpacePt not screen because if the plot
   * is zoomed the image point will be what we want in the center.
   * The screen coordinates will be completely different.
   * @return ImagePt the center point
   */
   public ImageWorkSpacePt findCurrentCenterPoint() {
      WebPlot      plot= getPrimaryPlot();

      Element body = _scrollPanel.getElement();
      int scrollX = body.getScrollLeft();
      int scrollY = body.getScrollTop();
      int viewWidth = getScrollWidth();
      int viewHeight = getScrollHeight();

      ScreenPt pt= new ScreenPt(
                 (int)(scrollX + viewWidth / 2.0),
                 (int)(scrollY + viewHeight/ 2.0) );
      return plot.getImageWorkSpaceCoords(pt);
  }

    public WorldPt findCurrentCenterWorldPoint() {
        WorldPt wp;
        try {
            wp= _primaryPlot.getWorldCoords(findCurrentCenterPoint());
        } catch (ProjectionException e) {
            wp= null;
        }
        return wp;
    }

    //==============================================================================
    //------------------- Centering Methods ----------------------------------------
    //==============================================================================


    public void clearWcsSync() {
        setMarginXY(AUTO, 0);
        setScrollBarsEnabled(scrollBarEnabled);
    }

    public void enableWcsSyncOutOfBounds() {
        setMarginXY(0, AUTO);
        setScrollBarsEnabled(scrollBarEnabled);
    }

    private boolean isWcsSync() {
        AllPlots ap= AllPlots.getInstance();
        return ap.isWCSSync() && ap.getWcsSyncCenter()!=null;
    }

    private void recomputeWcsOffsets() {
        AllPlots ap= AllPlots.getInstance();
        if (isWcsSync())  wcsSyncCenter(ap.getWcsSyncCenter());
        else              clearWcsSync();

    }


    /**
     * TODO
     * TODO
     */
    private void wcsSyncCenter(WorldPt wcsSyncCenterWP) {
        boolean clearMargin= true;
        if (_primaryPlot!=null && wcsSyncCenterWP !=null) {
            int sw= getScrollWidth();
            int swCen= sw/2;
            int sh= getScrollHeight();
            int shCen= sh/2;
            if (sw>0 && sh>0) {
//                if (_primaryPlot.pointInData(wcsSyncCenterWP)) {
                    try {
                        int extraOffsetX;
                        int extraOffsetY;
                        ScreenPt pt= _primaryPlot.getScreenCoords(wcsSyncCenterWP);

                        int w= _primaryPlot.getScreenWidth();
                        int h= _primaryPlot.getScreenHeight();


                        if (w<sw) {
                            extraOffsetX= swCen-pt.getIX();
                        }
                        else {
                            int leftOff= w-pt.getIX();

                            if (leftOff< swCen)           extraOffsetX= w - pt.getIX() - swCen;
                            else if (pt.getIX() < swCen)  extraOffsetX= swCen-pt.getIX();
                            else                          extraOffsetX= 0;
                        }

                        if (h<sh) {
                            extraOffsetY= shCen-pt.getIY();
                        }
                        else {
                            int bottomOff= h-pt.getIY();
                            if (bottomOff< shCen)         extraOffsetY= h - pt.getIY() - shCen;
                            else if (pt.getIY() < shCen)  extraOffsetY= shCen-pt.getIY();
                            else                          extraOffsetY= 0;
                        }

                        clearMargin= false;
                        setMarginXY(extraOffsetX,extraOffsetY);
                        centerOnPoint(wcsSyncCenterWP);
                        setScrollBarsEnabledInternal(false);

                    } catch (ProjectionException e) {
                        clearMargin= false;
//                        enableWcsSyncOutOfBounds();
                        setMarginXY(-sw,-sh);
                        setScrollBarsEnabledInternal(false);
                        clearMargin= false;
                    }

//                }
//                else {
//                    enableWcsSyncOutOfBounds();
//                }
            }
        }

        if (clearMargin) clearWcsSync();


    }



    /**
     * If the plot has the FIXED_TARGET attribute and it is on the image, then center on the fixed target.
     * Otherwise, center in the middle of the image
     */
    public void smartCenter() {
        WebPlot p = getPrimaryPlot();
        if (p==null) return;

        AllPlots ap= AllPlots.getInstance();
        if (isWcsSync()) {
            wcsSyncCenter(ap.getWcsSyncCenter());
        }
        else if (p.containsAttributeKey(WebPlot.FIXED_TARGET)) {
            Object o = p.getAttribute(WebPlot.FIXED_TARGET);
            if (o instanceof ActiveTarget.PosEntry) {
                ActiveTarget.PosEntry entry = (ActiveTarget.PosEntry) o;
                try {
                    ImageWorkSpacePt ipt = p.getImageWorkSpaceCoords(entry.getPt());
                    if (p.pointInPlot(entry.getPt())) centerOnPoint(ipt);
                    else simpleImageCenter();
                } catch (ProjectionException e) {
                    simpleImageCenter();
                }
            }
        } else {
            simpleImageCenter();
        }
    }

    public void centerOnPoint(Pt pt) {
        try {
            if (pt!=null && _primaryPlot!=null)  {
                ScreenPt spt= _primaryPlot.getScreenCoords(pt);
                setScrollXY(spt.getIX()- getScrollWidth()/ 2, spt.getIY() - getScrollHeight()/ 2);
            }
        } catch (ProjectionException e) { /* do nothing */ }
    }

    private void simpleImageCenter() {
        if (_primaryPlot!=null) {
            int sw = _primaryPlot.getScreenWidth();
            int sh = _primaryPlot.getScreenHeight();
            int w= _scrollPanel.getOffsetWidth();
            int h= _scrollPanel.getOffsetHeight();
            setScrollXY((sw - w) / 2, (sh - h) / 2);
        }
    }

    //==============================================================================
    //------------------- End Centering Methods ----------------------------------------
    //==============================================================================

    public void recallScrollPos() {
        if (_primaryPlot!=null) {
            recallScrollPos(_scrollInfo.get(_primaryPlot).makeCopy());
        }
    }


    private void recallScrollPos(ScrollInfo sInfo) {
        int w= _scrollPanel.getOffsetWidth();
        int h= _scrollPanel.getOffsetHeight();
        if (sInfo._sWidth== w && sInfo._sHeight==h) {
            int newH= sInfo._scrollHPos;
            int newV= sInfo._scrollVPos;
            setScrollXY(newH, newV);
        }
    }



    public void addPersistentMouseInfo(MouseInfo info) { _pvMouse.addPersistentMouseInfo(info); }
    public void removePersistentMouseInfo(MouseInfo info) { _pvMouse.removePersistentMouseInfo(info); }
    public void grabMouse(MouseInfo info) { _pvMouse.grabMouse(info); }
    public void releaseMouse(MouseInfo info) { _pvMouse.releaseMouse(info); }

    public HandlerRegistration addScrollHandler(final ScrollHandler handler) {
        _scrollHandlerList.add(handler);
        return new HandlerRegistration() {
            public void removeHandler() { _scrollHandlerList.remove(handler); }
        };
    }


    public void fireMouseLeave(MouseOutEvent ev) { _pvMouse.onMouseOut(ev); }
    public void fireMouseMove(ScreenPt spt) { _pvMouse.onMouseMove(spt); }
    public void fireMouseUp(MouseUpEvent ev) { _pvMouse.onMouseUp(ev); }
    public void fireMouseDown(MouseDownEvent ev) { _pvMouse.onMouseDown(ev); }
    public void fireMouseEnter(MouseOverEvent ev) { _pvMouse.onMouseOver(ev); }

  // -------------------------------------------------------------------
  // ==================  private Inner classes ==========================
  // -------------------------------------------------------------------


    private static class ScrollInfo implements Cloneable {
        WebPlot _plot;
        int _sWidth= 0;
        int _sHeight= 0;
        int _scrollHPos= 0;
        int _scrollVPos= 0;

        public ScrollInfo( WebPlot plot) {
            _plot= plot;
        }

        public ScrollInfo makeCopy() {
            ScrollInfo si= new ScrollInfo(_plot);
            si._sWidth= _sWidth;
            si._sHeight= _sHeight;
            si._scrollHPos= _scrollHPos;
            si._scrollVPos= _scrollVPos;
            return si;
        }

    }
    

//  private static ViewPortPt makeViewPortPt(MouseEvent ev) { return new ViewPortPt(ev.getX(),ev.getY()); }
//
//  private ScreenPt makeScreenPt(MouseEvent ev) {
//      ScreenPt spt= null;
//      if (_primaryPlot!=null)  spt= _primaryPlot.getScreenCoords(makeViewPortPt(ev));
//      return spt;
//  }

    void enableFocus() {
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();
        _mouseMoveArea.setFocus(true);
        if (BrowserUtil.isIE()) {
            setScrollXY(scrollX, scrollY);
        }
    }

    void disableTextSelect(boolean disable) {
        Element e= RootPanel.get().getElement();
        try {
            disableTextSelectInternal(e,disable);
            disableTextSelectInternal(_mouseMoveArea.getElement(),disable);
        } catch (Throwable t) {
            // ignore
        }
    }

     private native static void disableTextSelectInternal(Element e, boolean disable)/*-{
      if (disable) {
        e.ondrag = function () { return false; };
        e.onselectstart = function () { return false; };
      } else {
        e.ondrag = null;
        e.onselectstart = null;
      }
    }-*/;




  /**
   * This PlotIterator implements iterator and is constructed with a
   * list iterator.  It adds functionality on the delete.  When a plot is
   * deleted is makes sure the primary plot index is moved appropriately.
   */
  private class PlotIterator implements Iterator<WebPlot> {
       Iterator<WebPlot> _iterator;
       WebPlot     _p;
       public PlotIterator(Iterator<WebPlot> iterator) {
          _iterator= iterator;
       }
       public void remove() {
            _p.freeResources();
            WebPlot primary= getPrimaryPlot();
            _p.getPlotGroup().removePlotView(WebPlotView.this);
            _iterator.remove();
            if (_p == primary) {
                 setPrimaryPlot(_plots.get(0));
            }
       }
      public WebPlot  next()    { return _iterator.next(); }
      public boolean hasNext() { return _iterator.hasNext(); }
  }

    // -------------------------------------------------------------------
    // ==================  Public Inner classes ==========================
    // -------------------------------------------------------------------

    public interface MouseAll {
        void onMouseUp(WebPlotView pv, ScreenPt spt);
        void onMouseOut(WebPlotView pv);
        void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev);
        void onMouseOver(WebPlotView pv, ScreenPt spt);
        void onMouseMove(WebPlotView pv, ScreenPt spt);
        void onTouchStart(WebPlotView pv, ScreenPt spt, TouchStartEvent ev);
        void onTouchMove(WebPlotView pv, ScreenPt spt, TouchMoveEvent ev);
        void onTouchEnd(WebPlotView pv);
        void onClick(WebPlotView pv, ScreenPt spt);
    }

    public static class DefMouseAll implements MouseAll {
        public void onMouseUp(WebPlotView pv, ScreenPt spt) { }
        public void onMouseOut(WebPlotView pv) { }
        public void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev) { }
        public void onMouseOver(WebPlotView pv, ScreenPt spt) { }
        public void onMouseMove(WebPlotView pv, ScreenPt spt) { }
        public void onTouchStart(WebPlotView pv, ScreenPt spt, TouchStartEvent ev) { }
        public void onTouchMove(WebPlotView pv, ScreenPt spt, TouchMoveEvent ev) { }
        public void onTouchEnd(WebPlotView pv) { }
        public void onClick(WebPlotView pv, ScreenPt spt) {}
    }

    public static class MouseInfo {
        private final MouseAll _handler;
        private final String _help;
        private boolean _enableAllPersistent = true;
        private boolean _enableAllExclusive = false;
        private boolean _enabled= true;

        public MouseInfo(MouseAll handler, String help) {
            _handler= handler;
            _help= help;
        }

        public MouseAll getHandler() { return _handler; }
        public String getHelp() { return _help; }
        public boolean getEnableAllPersistent() { return _enableAllPersistent; }
        public void setEnableAllPersistent(boolean enable) { _enableAllPersistent = enable; }
        public boolean getEnableAllExclusive() { return _enableAllExclusive; }
        public void setEnableAllExclusive(boolean enable) { _enableAllExclusive = enable; }
        public void setEnabled(boolean enabled) { _enabled= enabled; }
        public boolean isEnabled() { return _enabled; }
    }


    private class PVScrollHandler implements ScrollHandler {

        private int _lastX= -1;
        private int _lastY= -1;
        private int _lastW= -1;
        private int _lastH= -1;

        PVScrollHandler() {}

        public void onScroll(ScrollEvent ev) {
            int x= _scrollPanel.getHorizontalScrollPosition();
            int y= _scrollPanel.getVerticalScrollPosition();

            if (x==0 && y==0 && _fixScrollInProgress) return;

            int w= _scrollPanel.getOffsetWidth();
            int h= _scrollPanel.getOffsetHeight();
            if (_lastX!=x || _lastY!=y || _lastW!=w || _lastH!=h) {
                WebPlot plot= getPrimaryPlot();
                if (plot!=null) {
                    ScreenPt spt= new ScreenPt(x,y);
                    plot.drawTilesInArea(spt,w,h);
                }
                _lastX= x;
                _lastY= y;
                _lastW= w;
                _lastH= h;
            }

            if (_primaryScrollInfo!=null) {
                _primaryScrollInfo._sWidth= w;
                _primaryScrollInfo._sHeight= h;
                _primaryScrollInfo._scrollHPos= x;
                _primaryScrollInfo._scrollVPos= y;
            }
            for(ScrollHandler handler : _scrollHandlerList) {
                handler.onScroll(ev);
            }
            recomputeViewPortIfNecessary();
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
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
