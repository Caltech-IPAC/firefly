/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
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
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
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


    public static final String GRID_ID = "GRID_ID";
    public static final String DATASET_INFO_CONVERTER = "DATASET_INFO_CONVERTER";
    public static final String EXTENSION_LIST=   "EXTENSION_LIST";
    public static final String ACTION_REPORTER=   "ACTION_REPORTER";

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

//    private ScrollPanel _scrollPanel= new ScrollPanel(_masterPanel);
    private DefaultDrawable _drawable= new DefaultDrawable();
    private PVMouse _pvMouse;
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
    private String drawingSubGroup= null;

    private List<OverlayPlotView> overlayPlotViews= null;

    private boolean containsMultiImageFits = false;
    private boolean containsMultipleCubes= false;

    private int wcsMarginX= 0;
    private int wcsMarginY= 0;

    private int scrollPositionX;
    private int scrollPositionY;

    private int scrollWidth= 0;
    private int scrollHeight= 0;
//    private AbsolutePanel _fakeScrollPanel= new AbsolutePanel();
    private SimplePanel _fakeScrollPanel= new SimplePanel();
    private SimplePanel _scrollViewWindow= new SimplePanel();
    SimplePanel _pvRoot= new SimplePanel();



  /**
   * Create a new PlotView.  This constructor is typically used when the
   * PlotView <em>will</em> be placed insides a scrolled window.
   */
  public WebPlotView() {
      initWidgets();
      _fakeScrollPanel.addDomHandler(new MouseDownHandler() {
          public void onMouseDown(MouseDownEvent ev) {
              if (_mpw != null) _mpw.selectSelf();
          }
      }, MouseDownEvent.getType());

      _fakeScrollPanel.addDomHandler(new TouchStartHandler() {
          public void onTouchStart(TouchStartEvent event) {
              if (_mpw!=null) _mpw.selectSelf();
          }
      }, TouchStartEvent.getType());

  }

    private void initWidgets() {
//        _fakeScrollPanel.add(_scrollViewWindow, 0, 0);
        _fakeScrollPanel.add(_scrollViewWindow);
        _scrollViewWindow.setWidget(_masterPanel);
        initWidget(_fakeScrollPanel);
        _pvMouse= new PVMouse(this, _mouseMoveArea);

        GwtUtil.setStyles(_scrollViewWindow, "margin", "0 auto" );

        _fakeScrollPanel.addStyleName("web-plot-view-scr");
        _mouseMoveArea.addStyleName("event-layer");
        _masterPanel.addStyleName("plot-view-master-panel");
        _scrollViewWindow.addStyleName("plot-view-scr-view-window");
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
            computePosition();
        }
    }

    public AbsolutePanel getDrawingPanelContainer() { return _drawable.getDrawingPanelContainer(); }



    public List<WebPlot> getOverlayPlotList() {
        if (overlayPlotViews==null || overlayPlotViews.size()==0) {
            return new ArrayList<WebPlot>(0);
        }
        else {
            List<WebPlot> opList= new ArrayList<WebPlot>();
            WebPlot p;
            for(OverlayPlotView opv : overlayPlotViews) {
                p= opv.getMaskPlot();
                if (p!=null) opList.add(opv.getMaskPlot());
            }
            return opList;
        }
    }

    public List<OverlayPlotView> getOverlayPlotViewList() { return overlayPlotViews; }

    public void addOverlayPlotView(OverlayPlotView opv) {
        if (overlayPlotViews==null) overlayPlotViews= new ArrayList<OverlayPlotView>(5);
        overlayPlotViews.add(opv);
    }

    public void removeOverlayPlotView(OverlayPlotView opv) {
        if (overlayPlotViews!=null) overlayPlotViews.remove(opv);
    }

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
    public void replaceDrawingArea(Widget old, Widget w) { _drawable.replaceDrawingArea(old, w);}
    public void insertBeforeDrawingArea(Widget before, Widget w) { _drawable.insertBeforeDrawingArea(before, w); }
    public void insertAfterDrawingArea(Widget after, Widget w) { _drawable.insertAfterDrawingArea(after,w); }
    public int getDrawingWidth() { return _drawable.getDrawingWidth(); }
    public int getDrawingHeight() { return _drawable.getDrawingHeight();  }

    public void setDrawingSubGroup(String drawingSubGroup) {
        this.drawingSubGroup= drawingSubGroup;
    }

    public String getDrawingSubGroup() { return drawingSubGroup; }

    public void onResize() {
        recomputeSize();
        computeScrollSizes();
        recomputeWcsOffsets();
    }



    public void computeScrollSizes() {
        int spWidth= _fakeScrollPanel.getOffsetWidth();
        int spHeight= _fakeScrollPanel.getOffsetHeight();

        scrollWidth= _fakeScrollPanel.getOffsetWidth();
        scrollHeight= _fakeScrollPanel.getOffsetHeight();
        if (_primaryPlot!=null) {
            if (_primaryPlot.getScreenWidth()<scrollWidth) {
                scrollWidth= _primaryPlot.getScreenWidth();
                GwtUtil.setStyle(_scrollViewWindow, "position", "relative");
            }
            else {
                GwtUtil.setStyle(_scrollViewWindow, "position", "relative");
                scrollWidth= spWidth;
            }

            if (_primaryPlot.getScreenHeight()<scrollHeight) {
                scrollHeight= _primaryPlot.getScreenHeight();
            }
            else {
                scrollHeight= spHeight;
            }
        }
    }




    public void setMiniPlotWidget(MiniPlotWidget mpw) { _mpw= mpw; }

    public MiniPlotWidget getMiniPlotWidget() { return _mpw; }


    public void setAttribute(String key, Object attribute) { _attributes.put(key,attribute); }
    public void removeAttribute(String key) { _attributes.remove(key); }
    public Object getAttribute(String key) { return _attributes.get(key); }
    public boolean containsAttributeKey(String key) { return _attributes.containsKey(key); }


    public int getUserDrawerLayerListSize() { return _userDrawLayerMap==null? 0 : _userDrawLayerMap.size(); }

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
        // todo remove this method
//        scrollBarEnabled= enabled;
//        setScrollBarsEnabledInternal(scrollBarEnabled);
    }

//    private void setScrollBarsEnabledInternal(boolean enabled) {
//        GwtUtil.setStyle(_scrollPanel, "overflow", enabled ?"auto" : "hidden");
//    }

    public int getScrollX() {
        return scrollPositionX;
//        int scrollX= 0;
//        if (_primaryPlot!=null) {
//            scrollX= getScrollScreenPos().getIX();
//        }
//        return scrollX;
    }

    public int getScrollY() {
        return scrollPositionY;
//        int scrollY= 0;
//        if (_primaryPlot!=null) {
//            scrollY= getScrollScreenPos().getIY();
//        }
//        return scrollY;
    }

    public ScreenPt getScrollScreenPos() {
        return new ScreenPt(scrollPositionX,scrollPositionY);
//        ScreenPt retval;
//        if (_primaryPlot!=null) {
//            Element body = _scrollPanel.getElement();
//            retval= new ScreenPt(body.getScrollLeft(), body.getScrollTop());
//        }
//        else {
//            retval= new ScreenPt(0,0);
//        }
//        return retval;

    }

    public void refreshDisplay() {
        if (_primaryPlot!=null) {
            if (getScrollWidth()*getScrollHeight()>0) {
                Dimension d= _primaryPlot.getViewPortDimension();
                drawTilesInArea(new ScreenPt(_primaryPlot.getViewPortX(),_primaryPlot.getViewPortY()),
                        d.getWidth(), d.getHeight());
            }
        }
    }

    private void drawTilesInArea(ScreenPt viewPortLocation, int w, int h) {
        _primaryPlot.drawTilesInArea(viewPortLocation,w,h);
        for(WebPlot p : getOverlayPlotList()) {
            p.drawTilesInArea(viewPortLocation,w,h);
        }
    }

    private void setViewPort(int x, int y, int w, int h) {
        _primaryPlot.setViewPort(x,y,w,h);
        for(WebPlot p : getOverlayPlotList()) {
            p.setViewPort(x,y,w,h);
        }
    }

    public void setTouchScrollingEnabled(boolean enable) {
        //todo - turn off scrolling I think
//        _scrollPanel.setTouchScrollingDisabled(!enable);
    }


    public int getScrollWidth() {
        if (scrollWidth==0) computeScrollSizes();
        return scrollWidth;
    }
    public int getScrollHeight() {
        if (scrollHeight==0) computeScrollSizes();
        return scrollHeight;
    }

    public void scrollDragEnded() { setScrollXY(getScrollScreenPos()); }
    public void setScrollXY(int x, int y) { setScrollXY(new ScreenPt(x,y)); }
    public void setScrollXY(ScreenPt spt) { setScrollXY(spt,false); }

    public void setScrollXY(ScreenPt spt, boolean dragging) {
        if (_primaryPlot==null) return;
        if (spt.getIX()==scrollPositionX && spt.getIY()==scrollPositionY) return;
        if (getScrollWidth()*getScrollHeight()<=0) return;



        int x= spt.getIX();
        int y= spt.getIY();

//        GwtUtil.getClientLogger().log(Level.INFO, "setScrollXY: sx= "+x+", sy= "+y);

        int maxX= _primaryPlot.getScreenWidth()-getScrollWidth();
        int maxY= _primaryPlot.getScreenHeight()-getScrollHeight();

        if (maxX<0) maxX=0;
        if (maxY<0) maxY=0;

        if (x<0) x= 0;
        else if (x>maxX) x= maxX;

        if (y<0) y= 0;
        else if (y>maxY) y= maxY;


        spt= new ScreenPt( x,y);



        ViewPortPt vpt= _primaryPlot.getViewPortCoords(spt);

        scrollPositionX= x;
        scrollPositionY= y;

//            GwtUtil.getClientLogger().log(Level.INFO, "WebPlotView: scrollX="+ x+"  scrollY="+y);

//            scrollPositionX= spt.getIX();
//            scrollPositionY= spt.getIY();


//            if (!pointInViewPortBounds(vpt) && !dragging) {
//                recomputeViewPortOnScrollingIfNecessary();
//            }
        computePosition();
//            _scrollPanel.setHorizontalScrollPosition(spt.getIX());
//            _scrollPanel.setVerticalScrollPosition(spt.getIY());
        _fixScrollInProgress= false;
        onScroll();
    }


//    private void recomputeViewPortOnScrollingIfNecessary() {
//        if (_primaryPlot==null) return;
//        int sw= getScrollWidth();
//        int sh= getScrollHeight();
//        int x= getScrollX();
//        int y= getScrollY();
//        Dimension dim= _primaryPlot.getViewPortDimension();
//        boolean contains= VisUtil.containsRec(_primaryPlot.getViewPortX(),_primaryPlot.getViewPortY(),
//                                              dim.getWidth(),dim.getHeight(),
//                                              x,y,sw-1,sh-1);
////        if (!contains) recomputeViewPort();
//        recomputeViewPort();
//
//
//    }


    private boolean isRecomputeViewPortNecessary() {
        if (_primaryPlot==null) return false;
        int sw= getScrollWidth();
        int sh= getScrollHeight();
        int x= getScrollX();
        int y= getScrollY();
        Dimension dim= _primaryPlot.getViewPortDimension();
        boolean contains= VisUtil.containsRec(_primaryPlot.getViewPortX(),_primaryPlot.getViewPortY(),
                dim.getWidth(),dim.getHeight(),
                x,y,sw-1,sh-1);

        return !contains;
    }




    public boolean isContainsMultiImageFits() { return containsMultiImageFits; }

    public void setContainsMultiImageFits(boolean containMultiImageFits) {
        this.containsMultiImageFits = containMultiImageFits;
    }

    public boolean isContainsMultipleCubes() { return containsMultipleCubes; }

    public void setContainsMultipleCubes(boolean containsMultipleCubes) {
        this.containsMultipleCubes = containsMultipleCubes;
    }


    private void computePosition() {
        if (_primaryPlot!=null) {
            computePosition(_primaryPlot.getScreenCoords(findCurrentCenterPoint()), false);
        }
    }



    private void computePosition(ScreenPt visibleCenterPt, boolean forceNewViewport) {
        if (visibleCenterPt == null) return;

        if (isRecomputeViewPortNecessary() || forceNewViewport) {
            int screenW = _primaryPlot.getScreenWidth();
            int screenH = _primaryPlot.getScreenHeight();

            int vpw = getScrollWidth() * 2;
            int vph = getScrollHeight() * 2;

            if (vpw > 1500) vpw = Math.max((int) (getScrollWidth() * 1.5),2700);

            if (vph > 1400) vph = Math.max((int) (getScrollHeight() * 1.5),1800);

            int newVpX;
            int newVpY;

            if (vpw > screenW) {
                vpw = screenW;
                newVpX = 0;
            } else {
                newVpX = visibleCenterPt.getIX() - vpw / 2;
                if (newVpX < 0) newVpX = 0;
                else if (newVpX + vpw > screenW) newVpX = screenW - vpw;
            }

            if (vph > screenH) {
                vph = screenH;
                newVpY = 0;
            } else {
                newVpY = visibleCenterPt.getIY() - vph / 2;
                if (newVpY < 0) newVpY = 0;
                else if (newVpY + vph > screenH) newVpY = screenH - vph;
            }

            if (vpw > 0 && vph > 0) {
                // todo- only scroll  & VP position need to change
//                GwtUtil.getClientLogger().log(Level.INFO, "VP & Scroll");
                setViewPort(newVpX, newVpY, vpw, vph);
                recomputeSize();

                WebEvent ev = new WebEvent<WebPlot>(this, Name.VIEW_PORT_CHANGE, _primaryPlot);
                Dimension dim= _primaryPlot.getViewPortDimension();
                drawTilesInArea(new ScreenPt(newVpX, newVpY), dim.getWidth(), dim.getHeight());
                fireEvent(ev);
                int scrollWidth= getScrollWidth();
                int scrollHeight= getScrollHeight();

                int x = vpw <= scrollWidth ? 0 : -1 * ((vpw - scrollWidth) / 2);
                int y = vph <= scrollHeight ? 0 : -1 * ((vph - scrollHeight) / 2);

                //---------- HERE
//                GwtUtil.setStyles(_fakeScrollPanel, "left", x+"px", "top", y+"px");
                GwtUtil.setStyles(_scrollViewWindow, "left", x+"px", "top", y+"px");


                //---DEBUG Code
//                int sx = getScrollX();
//                int sy = getScrollY();
//                ViewPortPt vpPt = _primaryPlot.getViewPortCoords(new ScreenPt(sx, sy));
//                GwtUtil.getClientLogger().log(Level.INFO, "VP & Scr: screen: "+ _primaryPlot.getScreenWidth()+ ","+_primaryPlot.getScreenHeight()+
//                        "   vpX,Y: "+ vpPt.getIX()+","+vpPt.getIY()+
//                        "   vpDim: "+ dim.getWidth()+","+dim.getHeight()+
//                        "   scrollX,Y: "+ sx+","+sy+
//                        "   scrollViewWindowX,Y: "+ x+","+y+
//                        "   scrollDim: "+ scrollWidth+","+scrollHeight);
                //---DEBUG Code


            }
        } else {
            // todo- only scroll position need to change
//            GwtUtil.getClientLogger().log(Level.INFO, "Scroll");
            int sx = getScrollX();
            int sy = getScrollY();
            int vpX = _primaryPlot.getViewPortX();
            int vpY = _primaryPlot.getViewPortY();
            int x = vpX-sx;
            int y = vpY-sy;

            //---------- HERE
            GwtUtil.setStyles(_scrollViewWindow, "left", x+"px", "top", y+"px");
//            GwtUtil.setStyles(_fakeScrollPanel, "left", x+"px", "top", y+"px");



            //---DEBUG Code
//            Dimension dim= _primaryPlot.getViewPortDimension();
//            int scrollWidth= getScrollWidth();
//            int scrollHeight= getScrollHeight();
//            GwtUtil.getClientLogger().log(Level.INFO, "     Scr: screen: "+ _primaryPlot.getScreenWidth()+ ","+_primaryPlot.getScreenHeight()+
//                          "   vpX,Y: "+ vpX+","+vpY+
//                          "   vpDim: "+ dim.getWidth()+","+dim.getHeight()+
//                          "   scrollX,Y: "+ sx+","+sy+
//                          "   scrollViewWindowX,Y: "+ x+","+y+
//                          "   scrollDim: "+ scrollWidth+","+scrollHeight);
            //---DEBUG Code
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

//        _masterPanel.setPixelSize(_primaryPlot.getScreenWidth(), _primaryPlot.getScreenHeight() );
//
//        _mouseMoveArea.setPixelSize(_primaryPlot.getScreenWidth(), _primaryPlot.getScreenHeight() );


//        int vpx= _primaryPlot.getViewPortX();
//        int vpy= _primaryPlot.getViewPortY();
        Dimension dim= _primaryPlot.getViewPortDimension();

        _masterPanel.setWidgetPosition(_drawable.getDrawingPanelContainer(),0,0);


        int vpW= dim.getWidth();
        int vpH= dim.getHeight();


        _scrollViewWindow.setPixelSize(Math.min(vpW,_primaryPlot.getScreenWidth()),
                                       Math.min(vpH,_primaryPlot.getScreenHeight()));
        _masterPanel.setPixelSize(vpW, vpH );
        _mouseMoveArea.setPixelSize(vpW, vpH);
        _drawable.setPixelSize(vpW,vpH);
    }

    private void setMarginXY(int x, int y) {
        String lStr= (x==AUTO) ? "auto" : x+"px";
        String tStr= (y==AUTO) ? "auto" : y+"px" ;
        GwtUtil.setStyles(_masterPanel, "marginLeft",  lStr,
                                        "marginRight","auto",
                                        "marginTop",tStr,
                                        "cursor","crosshair"
                                     );
    }

    private boolean pointInViewPortBounds(ViewPortPt vpt) {
        if (vpt==null) return false;
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
        //todo evaluate if this this needs to be here anymore
//        if (BrowserUtil.isIE() && _fixScrollInProgress) return;
//        final Element body = _scrollPanel.getElement();
//        final int scrollX = body.getScrollLeft();
//        final int scrollY = body.getScrollTop();
//        DeferredCommand.addPause();
//        _fixScrollInProgress= true;
//        Command cmd=  new Command() {
//            public void execute() {
//                int sx = body.getScrollLeft();
//                int sy = body.getScrollTop();
//                if (_fixScrollInProgress && sx==0 && sy==0) { // fix is necessary
//                    _scrollPanel.setHorizontalScrollPosition(scrollX);
//                    _scrollPanel.setVerticalScrollPosition(scrollY);
//                }
//                _fixScrollInProgress= false;
//            }
//           };
//        DeferredCommand.addCommand(cmd);
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
            _masterPanel.add(p.getWidget(), 0, 0);
            _masterPanel.add(_drawable.getDrawingPanelContainer(),0,0);
            _masterPanel.add(_mouseMoveArea, 0, 0);

            GwtUtil.setStyle(_mouseMoveArea, "backgroundColor", "transparent");

            _primaryPlot= p;
            _primaryScrollInfo= _scrollInfo.get(p);
            computeScrollSizes();
            computePosition(new ScreenPt(p.getScreenWidth() / 2, p.getScreenHeight() / 2), true);

            fireEvent(ev);
            recallScrollPos(recallInfo);

            AllPlots ap= AllPlots.getInstance();
            if (ap.getMiniPlotWidget()==getMiniPlotWidget()) {
                ap.setSelectedMPW(getMiniPlotWidget(), true, false);
            }

        }
//        else {
//        }
    }


    public void setZoomTo(float zoomLevel,
                          boolean isFullScreen,
                          boolean useDeferredDelay) {
        if (_primaryPlot!=null) {
            float currZoomFact= _primaryPlot.getZoomFact();
            final ImageWorkSpacePt pt= findCurrentCenterPoint();
            if (!ComparisonUtil.equals(zoomLevel,currZoomFact,4)) {
                _primaryPlot.getPlotGroup().activateDeferredZoom(zoomLevel, isFullScreen,
                                                                 useDeferredDelay, getOverlayPlotList());
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        computePosition(_primaryPlot.getScreenCoords(pt), true);
                        if (isWcsSync()) wcsSyncCenter(computeWcsSyncCenter());
                        else             centerOnPoint(pt);
                    }
                });
                DeferredCommand.addPause();
            }
            else {
                if (isWcsSync()) wcsSyncCenter(computeWcsSyncCenter());
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

    public List<WebPlot> getPlotList() { return new ArrayList<WebPlot>(_plots); }



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
      computePosition(_primaryPlot.getScreenCoords(findCurrentCenterPoint()), false);
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


       int screenW= plot.getScreenWidth();
       int screenH= plot.getScreenHeight();
       int sw= getScrollWidth();
       int sh= getScrollHeight();
       int cX;
       int cY;
       if (screenW<sw) {
           cX= screenW/2;
       }
       else {
           int scrollX = getScrollX();
           cX= scrollX+sw/2- wcsMarginX;
       }

       if (screenH<sh) {
           cY= screenH/2;
       }
       else {
           int scrollY = getScrollY();
           cY= scrollY+sh/2- wcsMarginY;
       }

       ScreenPt pt= new ScreenPt(cX,cY);

      return plot.getImageWorkSpaceCoords(pt);
  }

    public WorldPt findCurrentCenterWorldPoint() {
        return _primaryPlot.getWorldCoords(findCurrentCenterPoint());
    }

    //==============================================================================
    //------------------- Centering Methods ----------------------------------------
    //==============================================================================


    public ScreenPt getWcsMargins() {  return new ScreenPt(wcsMarginX,wcsMarginY); }

    public void clearWcsSync() {
        int oldMX= wcsMarginX;
        int oldMY= wcsMarginY;
        wcsMarginX= 0;
        wcsMarginY= 0;
        setMarginXY(AUTO, 0);
        setScrollBarsEnabled(scrollBarEnabled);
        if (oldMX!=0 || oldMY!=0) {
            _primaryPlot.refreshWidget();
        }
    }

    public void enableWcsSyncOutOfBounds() {
        setMarginXY(0, AUTO);
        setScrollBarsEnabled(scrollBarEnabled);
    }

    private boolean isWcsSync() {
        return  AllPlots.getInstance().isWCSMatch() && computeWcsSyncCenter()!=null;
    }


    private WorldPt computeWcsSyncCenter() {
        AllPlots ap= AllPlots.getInstance();
        WorldPt retval= ap.getWcsMatchCenter();
        if (retval==null && _primaryPlot.containsAttributeKey(WebPlot.MOVING_TARGET_CTX_ATTR)) {
            MovingTargetContext mtc= (MovingTargetContext)_primaryPlot.getAttribute(WebPlot.MOVING_TARGET_CTX_ATTR);
            retval= mtc.getPositionOnImage();
        }
        return retval;
    }



    private void recomputeWcsOffsets() {
        if (isWcsSync())  wcsSyncCenter(computeWcsSyncCenter());
        else              clearWcsSync();

    }


    /**
     * TODO
     * TODO
     */
    private void wcsSyncCenter(WorldPt wcsSyncCenterWP) {
        boolean clearMargin= true;
        int oldMX= wcsMarginX;
        int oldMY= wcsMarginY;
        if (_primaryPlot!=null && wcsSyncCenterWP !=null) {
            int sw= getScrollWidth();
            int swCen= sw/2;
            int sh= getScrollHeight();
            int shCen= sh/2;
            if (sw>0 && sh>0) {
                int extraOffsetX;
                int extraOffsetY;
                ScreenPt pt= _primaryPlot.getScreenCoords(wcsSyncCenterWP);
                if (pt!=null) {
                    extraOffsetX= swCen-pt.getIX();
                    extraOffsetY= shCen-pt.getIY();


                    clearMargin= false;
                    wcsMarginX= extraOffsetX;
                    wcsMarginY= extraOffsetY;
                    setMarginXY(extraOffsetX,extraOffsetY);
                    setScrollXY(0,0);
//                    setScrollBarsEnabledInternal(false);
                }
                else {
                    wcsMarginX= -sw;
                    wcsMarginY= -sh;
                    setMarginXY(-sw,-sh);
//                    setScrollBarsEnabledInternal(false);
                    clearMargin= false;
                }
            }

            if (clearMargin) {
                clearWcsSync();
            }
            else {
                if (oldMX!=wcsMarginX || oldMY!=wcsMarginY) {
                    _primaryPlot.refreshWidget();
                }
            }
        }
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
            wcsSyncCenter(computeWcsSyncCenter());
        }
        else if (p.containsAttributeKey(WebPlot.FIXED_TARGET)) {
            Object o = p.getAttribute(WebPlot.FIXED_TARGET);
            if (o instanceof ActiveTarget.PosEntry) {
                ActiveTarget.PosEntry entry = (ActiveTarget.PosEntry) o;
                ImageWorkSpacePt ipt = p.getImageWorkSpaceCoords(entry.getPt());
                if (ipt!=null && p.pointInPlot(entry.getPt())) centerOnPoint(ipt);
                else simpleImageCenter();
            }
        } else {
            simpleImageCenter();
        }
    }

    public void centerOnPoint(Pt pt) {
        if (pt!=null && _primaryPlot!=null)  {
            ScreenPt spt= _primaryPlot.getScreenCoords(pt);
            if (spt!=null) {
                setScrollXY(spt.getIX()- getScrollWidth()/ 2, spt.getIY() - getScrollHeight()/ 2);
            }
        }
    }

    private void simpleImageCenter() {
        if (_primaryPlot!=null) {
            int sw = _primaryPlot.getScreenWidth();
            int sh = _primaryPlot.getScreenHeight();
            int w= getScrollWidth();
            int h= getScrollHeight();
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
        int w= getScrollWidth();
        int h= getScrollHeight();
        if (sInfo._sWidth== w && sInfo._sHeight==h) {
            int newH= sInfo._scrollHPos;
            int newV= sInfo._scrollVPos;
            setScrollXY(newH, newV);
        }
    }


    public boolean isMultiImageFitsWithSameArea() {
        if (!containsMultiImageFits) return false;
        boolean retval= true;
        int w= _primaryPlot.getImageDataWidth();
        int h= _primaryPlot.getImageDataHeight();

        ImagePt ic1= new ImagePt(0,0);
        ImagePt ic2= new ImagePt(w,0);
        ImagePt ic3= new ImagePt(0,h);
        ImagePt ic4= new ImagePt(w,h);

        String projName= _primaryPlot.getProjection().getProjectionName();

        WorldPt c1= _primaryPlot.getWorldCoords(ic1);
        WorldPt c2= _primaryPlot.getWorldCoords(ic2);
        WorldPt c3= _primaryPlot.getWorldCoords(ic3);
        WorldPt c4= _primaryPlot.getWorldCoords(ic4);
        if (c1==null || c2==null || c3==null || c4==null) return false;


        WorldPt iwc1;
        WorldPt iwc2;
        WorldPt iwc3;
        WorldPt iwc4;

        for(WebPlot p : _plots) {
            if (w!=p.getImageDataWidth() || h!=p.getImageDataHeight()) {
                retval= false;
                break;
            }


            iwc1= p.getWorldCoords(ic1);
            iwc2= p.getWorldCoords(ic2);
            iwc3= p.getWorldCoords(ic3);
            iwc4= p.getWorldCoords(ic4);
            if (iwc1==null || iwc2==null || iwc3==null || iwc4==null) {
                retval= false;
                break;
            }

            if (!iwc1.equals(c1) || !iwc2.equals(c2) || !iwc3.equals(c3) || !iwc4.equals(c4) ) {
                retval= false;
                break;
            }

            if (!projName.equals(p.getProjection().getProjectionName())) {
                retval= false;
                break;
            }
        }
        return retval;
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
    public void fireMouseMove(ScreenPt spt, MouseMoveEvent ev) { _pvMouse.onMouseMove(spt,ev); }
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

    @Override
    public String toString() {
        WebPlot p= getPrimaryPlot();
        return p!=null ? p.getPlotDesc() : "no plots";
    }

    // -------------------------------------------------------------------
    // ==================  Public Inner classes ==========================
    // -------------------------------------------------------------------

    public interface MouseAll {
        void onMouseUp(WebPlotView pv, ScreenPt spt);
        void onMouseOut(WebPlotView pv);
        void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev);
        void onMouseOver(WebPlotView pv, ScreenPt spt);
        void onMouseMove(WebPlotView pv, ScreenPt spt, MouseMoveEvent ev);
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
        public void onMouseMove(WebPlotView pv, ScreenPt spt, MouseMoveEvent ev) { }
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


    private int _lastX= -1;
    private int _lastY= -1;
    private int _lastW= -1;
    private int _lastH= -1;

    public void onScroll() {
        int x= getScrollX();
        int y= getScrollY();

        if (x==0 && y==0 && _fixScrollInProgress) return;

        int w= getScrollWidth();
        int h= getScrollHeight();
        if (_lastX!=x || _lastY!=y || _lastW!=w || _lastH!=h) {
            //todo - no lose this optimization - i think it needs to be somewhere else now
            WebPlot plot= getPrimaryPlot();
            if (plot!=null) {
//                plot.drawTilesInArea(new ViewPortPt(x,y),w,h);
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
//            handler.onScroll(ev);
            handler.onScroll(null);
        }
//        recomputeViewPortOnScrollingIfNecessary();
    }




//    private class PVScrollHandler implements ScrollHandler {
//
//        private int _lastX= -1;
//        private int _lastY= -1;
//        private int _lastW= -1;
//        private int _lastH= -1;
//
//        PVScrollHandler() {}
//
//        public void onScroll(ScrollEvent ev) {
//            int x= _scrollPanel.getHorizontalScrollPosition();
//            int y= _scrollPanel.getVerticalScrollPosition();
//
//            if (x==0 && y==0 && _fixScrollInProgress) return;
//
//            int w= _scrollPanel.getOffsetWidth();
//            int h= _scrollPanel.getOffsetHeight();
//            if (_lastX!=x || _lastY!=y || _lastW!=w || _lastH!=h) {
//                WebPlot plot= getPrimaryPlot();
//                if (plot!=null) {
//                    plot.drawTilesInArea(new ViewPortPt(x,y),w,h);
//                }
//                _lastX= x;
//                _lastY= y;
//                _lastW= w;
//                _lastH= h;
//            }
//
//            if (_primaryScrollInfo!=null) {
//                _primaryScrollInfo._sWidth= w;
//                _primaryScrollInfo._sHeight= h;
//                _primaryScrollInfo._scrollHPos= x;
//                _primaryScrollInfo._scrollVPos= y;
//            }
//            for(ScrollHandler handler : _scrollHandlerList) {
//                handler.onScroll(ev);
//            }
//            recomputeViewPortOnScrollingIfNecessary();
//        }
//    }



}
