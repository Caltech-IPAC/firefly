/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.DefaultDrawable;
import edu.caltech.ipac.firefly.visualize.PrintableOverlay;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * User: roby
 * Date: Jun 19, 2008
 * Time: 3:53:56 PM
 */


/**
 * A DrawingManager can take one data set and draw it on many different WebPlotView's.
 * That is One DataConnections to Many WebPlots
 * @author Trey Roby
 */
public class DrawingManager implements AsyncDataLoader {

    public static final int SELECT_DIST = 5;
    private DataConnection _dataConnect;
    private final String _id;



    private static final DrawSymbol DEF_SYMBOL = DrawSymbol.X;
    private static final DrawSymbol DEF_HIGHLIGHT_SYMBOL = DrawSymbol.EMP_SQUARE_X;


    private String _highlightedColor = DrawingDef.COLOR_HIGHLIGHTED_PT;
    private String _areaSelectedColor = DrawingDef.COLOR_SELECTED_PT;
    private String _normalColor = DrawingDef.COLOR_PT_1;
    private final WebEventListener _listener = new TableViewListener();
    private int _lastAreaSelected[] = new int[0];
    private Map<WebPlotView, PVData> _allPV = new HashMap<WebPlotView, PVData>(5);
    private boolean _init = false;
    private boolean _groupByTitleOrID= false;  // if false group by id only , if true group by either id or title

    private DrawSymbol _autoDefHighlightSymbol = DEF_HIGHLIGHT_SYMBOL;
    private String _enablePrefKey= null;
    private final PrintableOverlay _printableOverlay;
    private boolean canDoRegion= true;
    private final SubgroupVisController subVisControl= new SubgroupVisController();
//    private static DrawingManager selectOwner= null;
//    private AreaSelectListener _areaSelectListener= new AreaSelectListener();



//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public DrawingManager(String id, DataConnection dataConnect) {
        this(id, dataConnect, null);
    }

    public DrawingManager(String id,
                          DataConnection dataConnect,
                          PrintableOverlay printableOverlay) {
        _id= id;
        _printableOverlay= printableOverlay;
        setDataConnection(dataConnect, false);
    }

//======================================================================
//----------------------- Method from WebLayerItem.AsyncDataLoader -----
//======================================================================

    public void requestLoad(final LoadCallback cb) {
       if (_dataConnect!=null && _dataConnect.getAsyncDataLoader()!=null) {
           if (isDataAvailable()) {
               for (PVData pvData : _allPV.values()) {
                   Drawer drawer= pvData.getDrawer();
                   drawer.setData(_dataConnect.getData(false,drawer.getPlotView().getPrimaryPlot()));
               }
               cb.loaded();
           }
           else {
               for(PVData pvData : _allPV.values())  pvData.addTask();
               _dataConnect.getAsyncDataLoader().requestLoad(new LoadCallback() {
                   public void loaded() {
                       cb.loaded();
                       for (PVData pvData : _allPV.values()) {
                           Drawer drawer= pvData.getDrawer();
                           drawer.setData(_dataConnect.getData(false,drawer.getPlotView().getPrimaryPlot()));
                           pvData.removeTask();
                       }
                   }
               });
           }
       }
       else if (_dataConnect!=null && _dataConnect.getAsyncDataLoader()==null){
           cb.loaded();
       }
    }

    public void disableLoad() {
        if (_dataConnect!=null && _dataConnect.getAsyncDataLoader()!=null) {
            _dataConnect.getAsyncDataLoader().disableLoad();
        }
    }

    public boolean isDataAvailable() {
        if (_dataConnect!=null && _dataConnect.getAsyncDataLoader()!=null) {
            return _dataConnect.getAsyncDataLoader().isDataAvailable();
        }
        return true;
    }

    public void markStale() {
        if (_dataConnect!=null && _dataConnect.getAsyncDataLoader()!=null) {
            _dataConnect.getAsyncDataLoader().markStale();
        }
    }

    public SubgroupVisController getSubVisControl() {
        return subVisControl;
    }
//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    //======================================================================
    //======================================================================
    //======================================================================
    //======================================================================
    //======================================================================
    //======================================================================


    public boolean isCanDoRegion() { return canDoRegion; }

    public void setCanDoRegion(boolean canDoRegion) {
        this.canDoRegion = canDoRegion;
        for(PVData pvData : _allPV.values()) {
            pvData.getWebLayerItem().setCanDoRegion(canDoRegion);
        }
    }

    public void setDefaultColor(final String color) {
        if (color == null) return;
        Vis.init(new Vis.InitComplete() {
            public void done() {
                _normalColor = color;
                for (PVData data : _allPV.values()) {
                    DrawingDef d= data.getDrawer().getDrawingDef().makeCopy();
                    d.setDefColor(color);
                    data.getDrawer().setDrawingDef(d);
                }
            }
        });
    }

    public void setHighlightedColor(String color) {
        if (color == null) return;
        _highlightedColor= color;
    }

    /**
     * if true then all the WebLayerItem with the same title (as well as ID) will be treated together of actions such
     * as setVisible or changing color.
     * @param g true will treat title or id as a group, false will tread only ID as a group
     */
    public void setGroupByTitleOrID (boolean g) {
        _groupByTitleOrID= g;
        for(PVData pvData : _allPV.values()) {
            pvData.getWebLayerItem().setGroupByTitleOrID(g);
        }

    }

    public void setEnablePrefKey(String prefKey) { _enablePrefKey= prefKey; }

    public String getActiveColor() {
        return _allPV.size()>0 ? _allPV.values().iterator().next().getWebLayerItem().getColor() : _normalColor;
    }

    public void beginBulkUpdate() {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                for (PVData pvData : _allPV.values()) {
                    pvData.getDrawer().setDrawingEnabled(false);
                }
            }
        });
    }

    public void endBulkUpdate() {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                for (PVData pvData : _allPV.values()) {
                    pvData.getDrawer().setDrawingEnabled(true);
                }
            }
        });

    }

    public boolean containsPlotView(WebPlotView pv) {
        return pv != null && _allPV.containsKey(pv);
    }

    public void addPlotView(final WebPlotView pv) {
        if (_allPV.containsKey(pv) || !pv.isAlive()) return;

        String sg= pv.getDrawingSubGroup();
        if (sg!=null) {
            subVisControl.enableSubgroupingIfSupported();
            if (!subVisControl.containsSubgroupKey(sg)) {
               subVisControl.setSubgroupVisibility(sg,false);
            }
        }
        Vis.init(new Vis.InitComplete() {
            public void done() {
                Drawer drawer= connectDrawer(pv);
                updateWebLayerItem();
                redrawAll(pv, drawer, false);
            }
        });


    }

    public void addPlotViewList(final List<WebPlotView> pvList) {

        Vis.init(new Vis.InitComplete() {
            public void done() {
                for (WebPlotView pv : pvList) {
                    if (!_allPV.containsKey(pv) && pv.isAlive())  connectDrawer(pv);
                }

                updateWebLayerItem();

                for (WebPlotView pv : pvList) {
                    if (pv.isAlive()) {
                        redrawAll(pv, _allPV.get(pv).drawer, false);
                    }
                }
            }
        });


    }

    public DataConnection.SelectSupport getSupportsAreaSelect() {
        return _dataConnect!=null ?_dataConnect.getSupportsAreaSelect() : DataConnection.SelectSupport.NO;
    }

    public boolean getSupportsFilter() {
        return _dataConnect!=null &&_dataConnect.getSupportsFilter();
    }

    public boolean isDataInSelection(RecSelection selection) {
        boolean retval= false;
        for(WebPlotView pv : _allPV.keySet()) {
            WebPlot p= pv.getPrimaryPlot();
            if (p!=null) {
                if (VisUtil.getSelectedPts(selection,p,_dataConnect.getData(false,p)).length>0) {
                    retval= true;
                    break;
                }
            }
        }
        return retval;
    }


    public void filter(RecSelection selection, boolean filterIn) {
        for(WebPlotView pv : _allPV.keySet()) {
            WebPlot p= pv.getPrimaryPlot();
            if (p!=null) {
                Integer ptIdxAry[]= VisUtil.getSelectedPts(selection,p,_dataConnect.getData(false,p));
                if (ptIdxAry.length>0)  _dataConnect.filter(ptIdxAry);
            }
        }
    }

    public void select(RecSelection selection) { //todo
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        if (pv==null) return;
        if (selection!=null) {
            WebPlot plot= pv.getPrimaryPlot();
            Integer selectedIdx[]= VisUtil.getSelectedPts(selection, plot, _dataConnect.getData(false,plot));
            _dataConnect.setSelectedIdx(selectedIdx);
        }
        else {
            _dataConnect.setSelectedIdx();
        }
    }

    public int getSelectedCount() {
        return _dataConnect!=null ? _dataConnect.getSelectedCount() : 0;
    }


    private Drawer connectDrawer(WebPlotView pv) {

        WebPlotView.MouseInfo mi = new WebPlotView.MouseInfo(new Mouse(pv),
                                                             "Click to select an Observation");
        boolean highPriority= _dataConnect!=null&&_dataConnect.isPriorityLayer();
        Drawer drawer = new Drawer(pv,highPriority);
        String helpLine = _dataConnect == null ? null : _dataConnect.getHelpLine();
        WebLayerItem item = new WebLayerItem(_id, this, getTitle(pv), helpLine, drawer, mi,
                                             _enablePrefKey,_printableOverlay);
        item.setCanDoRegion(canDoRegion);
        item.setGroupByTitleOrID(_groupByTitleOrID);
        drawer.setDrawingDef(new DrawingDef(_normalColor));

        if (_dataConnect != null) {
            drawer.setPointConnector(_dataConnect.getDrawConnector());
            drawer.setEnableDecimationDrawing(_dataConnect.isPointData());
            if (_dataConnect.isVeryLargeData()) drawer.setDataTypeHint(Drawer.DataType.VERY_LARGE);

            if (_dataConnect.isPointData()) WebLayerItem.addUICreator(_id, new PointUICreator());
        }
        if (pv.getDrawingSubGroup()!=null) subVisControl.enableSubgroupingIfSupported();
        pv.addPersistentMouseInfo(mi);
        pv.addWebLayerItem(item);


        if (_dataConnect != null) {
            if (_dataConnect.getOnlyShowIfDataIsVisible())  item.initDefaultVisibilityTo(_dataConnect.isDataVisible());
            else                                            item.initDefaultVisibility();
        }
        else {
            item.initDefaultVisibility();
        }


//       item.initDefaultVisibility();
        PlotAddedListener l= new PlotAddedListener(pv);
        pv.addListener(Name.PLOT_ADDED,l);
        _allPV.put(pv, new PVData(drawer, mi, item, l));

        checkAndSetupPerPlotData(pv,drawer);

        return drawer;

    }

    public boolean isDataLoadingAsync() {
        return _dataConnect!=null && _dataConnect.getAsyncDataLoader()!=null;
    }



    private void updateWebLayerItem() {
        for (Map.Entry<WebPlotView, PVData> entry : _allPV.entrySet()) {
            WebPlotView pv = entry.getKey();
            PVData pvData = entry.getValue();
            WebLayerItem item = pvData.getWebLayerItem();
            if (_dataConnect == null) {
                pv.setWebLayerItemActive(item, false);
            } else {
                item.setTitle(getTitle(pv));
                pv.setWebLayerItemActive(item, true);
//                if (_dataConnect.getOnlyShowIfDataIsVisible()) updateVisibilityBasedOnTableVisibility();
//                if (subVisControl.isUsingSubgroupVisibility()) updateVisibilityBasedOnSubgroup(pv);
                item.setDrawingManager(this);
            }
        }


    }

    private String getTitle(WebPlotView pv) {
        WebPlot plot= pv!=null ? pv.getPrimaryPlot() : null;
        return _dataConnect == null ? "" : _dataConnect.getTitle(plot);
    }

    public void setHelp(String helpStr) {
        for (Map.Entry<WebPlotView, PVData> entry : _allPV.entrySet()) {
            WebPlotView pv = entry.getKey();
            PVData pvData = entry.getValue();
            for (WebLayerItem item : pv.getUserDrawerLayerSet()) {
                if (item.getDrawer() == pvData.getDrawer()) {
                    item.setHelp(helpStr);
                }
            }
        }
    }

    public void showMouseHelpForAll() {
        for (WebPlotView pv : _allPV.keySet()) {
            showMouseHelp(pv);
        }

    }

    public void showMouseHelp(WebPlotView pv) {
        if (pv == null) return;
        PVData pvData = _allPV.get(pv);
        if (pvData!=null) {
            for (WebLayerItem item : pv.getUserDrawerLayerSet()) {
                if (item.getDrawer() == pvData.getDrawer()) {
                    pv.showMouseHelp(item);
                }
            }
        }

    }

    public void clear() {
        List<WebPlotView> copy = new ArrayList<WebPlotView>(_allPV.size());
        copy.addAll(_allPV.keySet());
        for (WebPlotView pv : copy) removePlotView(pv);
    }

    public void removePlotView(WebPlotView pv) {
        if (_allPV.containsKey(pv)) {
            if (pv.isAlive()) {
                PVData pvData = _allPV.get(pv);
                WebPlotView.MouseInfo mi = pvData.getMouseInfo();
                if (mi != null) pv.removePersistentMouseInfo(mi);


                for (WebLayerItem item : pv.getUserDrawerLayerSet()) {
                    if (item.getDrawer() == pvData.getDrawer()) {
                        pv.removeWebLayerItem(item);
                        break;
                    }
                }
                pvData.getDrawer().dispose();
                pv.removeListener(Name.PLOT_ADDED, pvData.listener);

            }
            _allPV.remove(pv);
        }
        subVisControl.clearPlotView(pv);
    }

    public void dispose() {
        List<WebPlotView> l = new ArrayList<WebPlotView>(_allPV.keySet());
        for (WebPlotView pv : l) removePlotView(pv);
        _allPV.clear();
//        _liGroup.clear();
        if (_dataConnect != null) {
            WebEventManager evM = _dataConnect.getEventManager();
            if (evM != null) {
                evM.removeListener(TablePanel.ON_ROWHIGHLIGHT_CHANGE, _listener);
                evM.removeListener(TablePanel.ON_ROWSELECT_CHANGE, _listener);
//                evM.removeListener(TablePanel.ON_PAGE_LOAD, _listener);
                evM.addListener(TablePanel.ON_SHOW, _listener);
                evM.addListener(TablePanel.ON_HIDE, _listener);
            }
        }
    }


//
//    public void setUpdateEventEnabled(boolean enabled) {
//        _updatesEventsEnabled= enabled;
//    }
//
//    public boolean isUpdateEventEnabled() { return _updatesEventsEnabled; }


    private void doInit() {
        if (!_init) {
            if (!StringUtils.isEmpty(_dataConnect.getInitDefaultColor())) {
                _normalColor = _dataConnect.getInitDefaultColor();
            }

            for (PVData pvData : _allPV.values()) {
                pvData.getDrawer().setDrawingDef(new DrawingDef(_normalColor));
                pvData.getDrawer().setPointConnector(_dataConnect.getDrawConnector());
            }

            for (Map.Entry<WebPlotView, PVData> entry : _allPV.entrySet()) {
                WebPlotView pv = entry.getKey();
                PVData pvData = entry.getValue();
                pvData.getWebLayerItem().setTitle(getTitle(pv));
            }
            _init = true;
        }
    }


    public void setDataConnection(DataConnection dataConnection) {
        setDataConnection(dataConnection, false);
    }

    public void setDataConnection(final DataConnection dataConnection, final boolean redrawNow) {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                setDataConnectionAsync(dataConnection, redrawNow);
            }
        });

    }


    private void setDataConnectionAsync(DataConnection dataConnection, boolean redrawNow) {
        if (_dataConnect == dataConnection) {
            if (_dataConnect!=null && redrawNow) redraw();
            return;
        }

        removeCurrentDataConnection();
        addNewDataConnection(dataConnection);
        updateWebLayerItem();
        if (redrawNow) redraw();
    }

    private void removeCurrentDataConnection() {
        if (_dataConnect != null) {
            WebEventManager evM = _dataConnect.getEventManager();
            if (evM != null) {
                evM.removeListener(TablePanel.ON_ROWHIGHLIGHT_CHANGE, _listener);
                evM.removeListener(TablePanel.ON_ROWSELECT_CHANGE, _listener);
                evM.removeListener(TablePanel.ON_SHOW, _listener);
                evM.removeListener(TablePanel.ON_HIDE, _listener);
                evM.removeListener(TablePanel.ON_DATA_LOAD, _listener);
            }
            subVisControl.setDataConnect(null);
            _dataConnect= null;
        }
    }


    private void addNewDataConnection(DataConnection dataConnection) {
        _dataConnect = dataConnection;

        if (_dataConnect != null) {
            doInit();
            WebEventManager evM = _dataConnect.getEventManager();
            if (evM != null) {
                evM.addListener(TablePanel.ON_ROWHIGHLIGHT_CHANGE, _listener);
                evM.addListener(TablePanel.ON_ROWSELECT_CHANGE, _listener);
                evM.addListener(TablePanel.ON_SHOW, _listener);
                evM.addListener(TablePanel.ON_HIDE, _listener);
                evM.addListener(TablePanel.ON_DATA_LOAD, _listener);
            }
            for (Map.Entry<WebPlotView,PVData> entry : _allPV.entrySet()) {
                final WebPlotView pv= entry.getKey();

                Drawer drawer= entry.getValue().getDrawer();
                drawer.setHighPriorityLayer(_dataConnect.isPriorityLayer());
                drawer.setEnableDecimationDrawing(_dataConnect.isPointData());
                drawer.setDataTypeHint( _dataConnect.isVeryLargeData() ?
                                        Drawer.DataType.VERY_LARGE : Drawer.DataType.NORMAL);


                checkAndSetupPerPlotData(pv, drawer);

                for (WebLayerItem item : pv.getUserDrawerLayerSet()) {
                    if (drawer == item.getDrawer()) {
                        item.setTitle(getTitle(null));
                    }
                }
            }

            subVisControl.setDataConnect(_dataConnect);
        for(WebPlotView pv : _allPV.keySet()) {
            if (pv.getDrawingSubGroup()!=null) subVisControl.enableSubgroupingIfSupported();
        }
        updateVisibilityBasedOnSubgroupAll();
        if (_dataConnect.isPointData()) WebLayerItem.addUICreator(_id, new PointUICreator());
    }
}


    public Set<WebPlotView> getPlotViewSet() {
        return _allPV.keySet();
    }

    private void checkAndSetupPerPlotData(final WebPlotView pv, Drawer drawer) {
        if (_dataConnect!=null && _dataConnect.getHasPerPlotData()) {
            drawer.setPlotChangeDataUpdater(new Drawer.DataUpdater() {
                public List<DrawObj> getData() {
                    PVData pvData=_allPV.get(pv);
                    String title= DrawingManager.this.getTitle(pv);
                    WebLayerItem item= pvData.getWebLayerItem();
                    if (item!=null && !ComparisonUtil.equals(title,item.getTitle())) {
                        item.setTitle(title);
                    }
                    return DrawingManager.this._dataConnect.getData(false, pv.getPrimaryPlot());
                }
            });
        }
        else {
             drawer.setPlotChangeDataUpdater(null);
        }
    }


    public void redraw() {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                Drawer drawer;
                if (_dataConnect != null) {
                    _dataConnect.getData(true, null); // causes graph obj to be recalculated
                    for (WebPlotView pv : _allPV.keySet()) {
                        drawer = _allPV.get(pv).getDrawer();
                        redrawAll(pv, drawer, false);
                    }
                } else {
                    for (WebPlotView pv : _allPV.keySet()) {
                        drawer = _allPV.get(pv).getDrawer();
                        drawer.setData((List<DrawObj>) null);
                    }
                }
            }
        });
    }

    public void redraw(WebPlotView pv) {
        PVData data=_allPV.get(pv);
        if (data!=null && data.getDrawer()!=null) {
           redrawAll(pv, data.getDrawer(),false);
        }

    }

    void redrawAll(final WebPlotView pv,
                   final Drawer drawer,
                   final boolean forceRebuild) {
        if (_dataConnect==null) return;
        Vis.init(new Vis.InitComplete() {
            public void done() {
                GwtUtil.isOnDisplay(pv);
                AsyncDataLoader loader= _dataConnect.getAsyncDataLoader();
                if (drawer.isVisible() && loader!=null && !loader.isDataAvailable()) {
                    final String drawTaskID= pv.addTask();
                    loader.requestLoad(new LoadCallback() {
                        public void loaded() {
                            redrawAllAsync(pv, drawer, forceRebuild);
                            pv.removeTask(drawTaskID);
                        }
                    });
                }
                else {
                    redrawAllAsync(pv, drawer, forceRebuild);
                }
            }
        });
    }


    void redrawAllAsync(final WebPlotView pv,
                        Drawer drawer,
                        boolean forceRebuild) {
        if (_dataConnect == null) {
            drawer.setData((List<DrawObj>) null);
            return;
        }

        List<DrawObj> data = _dataConnect.getData(forceRebuild, pv.getPrimaryPlot());

        if (data == null || data.size() == 0) {
            drawer.setData((DrawObj)null);
        }
        else {
            final int scrollX = pv.getScrollX();
            final int scrollY = pv.getScrollY();
            drawer.setData(data);
            updateHighlightLayer(drawer, pv);

            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    pv.setScrollXY(scrollX, scrollY);
                }
            });
        }
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void updateHighlightLayer(Drawer drawer, WebPlotView pv) {
        if (_dataConnect.getSupportsHighlight()) {
            List<DrawObj> drawList= _dataConnect.getHighlightData(pv.getPrimaryPlot());
            if (drawList!=null) {
                for (DrawObj d : drawList) updateHighlightedSymbol(d);
            }
            drawer.updateDataHighlightLayer(drawList);
        }
    }

    private void redrawHighlightChangeAsync(final WebPlotView pv,
                                            Drawer drawer) {
        if ((_dataConnect != null && _dataConnect.size() == 0) ||
                (_dataConnect != null && !_dataConnect.getSupportsHighlight())) {
            return;
        }

        if (_dataConnect == null) return;

        final int scrollX = pv.getScrollX();
        final int scrollY = pv.getScrollY();

        updateHighlightLayer(drawer,pv);

        DeferredCommand.addCommand(new Command() {
            public void execute() {
                pv.setScrollXY(scrollX, scrollY);
            }
        });
    }


    private void updateVisibilityBasedOnTableVisibility() {
        boolean visible= _dataConnect.isDataVisible();
        if (_init && _allPV.size()>0) {
            PVData data;
            _allPV.values().iterator().next().getWebLayerItem().setVisible(visible);
//            for (Map.Entry<WebPlotView, PVData> entry : _allPV.entrySet()) {
//                data = entry.getValue();
//                if (data.getWebLayerItem().isVisible() != visible) {
//                    data.getWebLayerItem().setVisible(visible);
//                }
//            }
        }
    }


    private void updateVisibilityBasedOnSubgroupAll() {
        if (_init && subVisControl.isUsingSubgroupVisibility() && _dataConnect!=null) {
            for (WebPlotView pv : _allPV.keySet()) {
                updateVisibilityBasedOnSubgroup(pv);
            }
        }
    }


    private void updateVisibilityBasedOnSubgroup(WebPlotView pv) {
        if (_init && pv!=null  && _dataConnect!=null) {
            PVData data= _allPV.get(pv);
            if (data!=null) {
                WebLayerItem wl= data.getWebLayerItem();
                boolean v= subVisControl.isVisibleAtAnyLevel(pv,wl.isVisible());
                if (wl.isVisible() != v) wl.setVisible(v);
            }
        }
    }



    private void redrawSelectAreaChangeAsync(final WebPlotView pv,
                                             Drawer drawer,
                                             int selected[]) {
        if ((_dataConnect != null && _dataConnect.size() == 0) ||
                (_dataConnect != null && _dataConnect.getSupportsAreaSelect()!= DataConnection.SelectSupport.YES)) {
            return;
        }

        if (_dataConnect == null) return;

        List<DrawObj> data = _dataConnect.getData(false, pv.getPrimaryPlot());
        if (selected.length > 0) {

            for(DrawObj d : data) {
                if (d!=null) updateSelected(false,d);
            }


            DrawObj obj;
            int dataLength= data.size();
            for (int sidx : selected) {
                if (sidx > -1 && sidx < dataLength) {
                    obj = data.get(sidx);
                    updateSelected(true, obj);
                }
            }
            drawer.updateDataSelectLayer(data);
        }
        else {
            for(DrawObj d : data) {
                if (d!=null) d.setSelected(false);
            }
            drawer.clearSelectLayer();
        }
    }




    private void updateHighlightedSymbol(DrawObj dObj) {
        if (dObj!=null) {
            dObj.setHighlightColor(_highlightedColor);
            if (dObj instanceof PointDataObj) ((PointDataObj) dObj).setHighlightSymbol(_autoDefHighlightSymbol);
        }
    }

    private void updateSelected(boolean selected, DrawObj dObj) {
        if (dObj==null) return;
        if (selected)  {
            dObj.setSelectColor(_areaSelectedColor);
        }
        dObj.setSelected(selected);

    }


    private void highlightNearest(WebPlotView pv, ScreenPt pt) {
        if (_dataConnect == null || !_dataConnect.getSupportsMouse()) return;
        HighlightNearestWorker worker= new HighlightNearestWorker(pv,pt);
        Scheduler.get().scheduleIncremental(worker);
    }



    private class HighlightNearestWorker implements Scheduler.RepeatingCommand {

        private double dist;
        private double minDist = Double.MAX_VALUE;
        private DrawObj closestPt = null;
        private int idx= 0;
        private int closestIdx= -1;
        private final int _maxChunk= 500;
        private final ScreenPt pt;
        private final WebPlot plot;
        private final Iterator<DrawObj> iterator;

        private HighlightNearestWorker(WebPlotView pv, ScreenPt pt) {
            this.pt = pt;
            plot = pv.getPrimaryPlot();
            iterator= _dataConnect.getData(false, plot).iterator();
        }


        public boolean execute() {
            boolean continueProcessing= true;
            DrawObj obj;
            try {
                for(int i= 0; (iterator.hasNext() && i<_maxChunk ); ) {
                    obj= iterator.next();
                    if (obj!=null) {
                        dist = obj.getScreenDist(plot, pt);
                        if (dist > -1 && dist < minDist) {
                            minDist = dist;
                            closestPt = obj;
                            closestIdx= idx;


                        }
                    }
                    idx++;
                }
                if (!iterator.hasNext()) {
                    continueProcessing= false;
                }
            } catch (ConcurrentModificationException e) {
                continueProcessing= false;
            }

            if (!continueProcessing && closestPt!=null) {
                _dataConnect.setHighlightedIdx(closestIdx);
            }
            return continueProcessing;
        }
    }




    private int[] getAndSaveSelectedArea() {
        int[] selAry = new int[0];
        if (_dataConnect != null && _dataConnect.getSupportsAreaSelect()== DataConnection.SelectSupport.YES) {
            List<Integer> selected= _dataConnect.getSelectedIdx();
            selAry= new int[selected.size()];
            int i= 0;
            for(Integer idx : selected) selAry[i++]= idx;
            _lastAreaSelected = selAry;
        }
        return selAry;
    }


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================


    private class Mouse extends WebPlotView.DefMouseAll {

        private final WebPlotView _pv;

        public Mouse(WebPlotView pv) {
            _pv = pv;
        }

        @Override
        public void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev) {
        }

        @Override
        public void onMouseUp(WebPlotView pv, ScreenPt spt) {
            //TODO: Trey, please fix this... this is not a solution.  -loi
            if (!_dataConnect.getClass().getName().contains("ActiveTargetDisplay")) {
                highlightNearest(_pv, spt);
            }
        }

        @Override
        public void onTouchStart(WebPlotView pv, ScreenPt spt, TouchStartEvent ev) {
            highlightNearest(_pv, spt);
        }
    }


    private static class PVData {
        private final Drawer drawer;
        private final WebPlotView.MouseInfo mi;
        private final WebLayerItem layerItem;
        private String currDrawTaskID= null;
        private int taskCnt= 0;
        private final PlotAddedListener listener;

        public PVData(Drawer drawer, WebPlotView.MouseInfo mi, WebLayerItem layerItem, PlotAddedListener listener) {
            this.drawer = drawer;
            this.mi = mi;
            this.layerItem = layerItem;
            this.listener= listener;
        }

        public Drawer getDrawer() { return drawer; }
        public WebLayerItem getWebLayerItem() { return layerItem; }
        public WebPlotView.MouseInfo getMouseInfo() { return mi; }
        public void addTask() {
            if (taskCnt==0) {
                WebPlotView pv= drawer.getPlotView();
                if (pv!=null) {
                    if (currDrawTaskID!=null) pv.removeTask(currDrawTaskID);
                    currDrawTaskID= pv.addTask();
                }
            }
            taskCnt++;
        }
        public void removeTask() {
            if (taskCnt==1) {
                WebPlotView pv= drawer.getPlotView();
                if (pv!=null && currDrawTaskID!=null) {
                    pv.removeTask(currDrawTaskID);
                    currDrawTaskID= null;
                }
            }
            taskCnt--;
        }


    }

    private class PlotAddedListener implements WebEventListener {
        private final WebPlotView pv;

        public PlotAddedListener(WebPlotView pv) {
            this.pv = pv;
        }

        public void eventNotify(WebEvent ev) {
            String sg= pv.getDrawingSubGroup();
            if (sg!=null && _dataConnect!=null && _dataConnect.getOKForSubgroups()) {
                subVisControl.enableSubgroupingIfSupported();
                if (!subVisControl.containPlotView(pv)) { // updating subgroup here is necessary only if it first time this plot view has been added
                    updateVisibilityBasedOnSubgroup(pv);
                }
            }
        }
    }

    private class TableViewListener implements WebEventListener {

        public void eventNotify(final WebEvent ev) {

            Vis.init(new Vis.InitComplete() {
                public void done() {
                    Name n = ev.getName();
                    if (n.equals(TablePanel.ON_PAGE_LOAD)) {
                        handleDataLoad();
                    } else if (n.equals(TablePanel.ON_DATA_LOAD)) {
                        markStale();
                        handleDataLoad();
                    } else if (n.equals(TablePanel.ON_ROWHIGHLIGHT_CHANGE)) {
                        handleHighlightChange();
                    } else if (n.equals(TablePanel.ON_ROWSELECT_CHANGE)) {
                        handleAreaSelectChange();
                    } else if (n.equals(TablePanel.ON_SHOW)) {
                        if (_dataConnect.getOnlyShowIfDataIsVisible()) {
                            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                public void execute() {
                                    updateVisibilityBasedOnTableVisibility();
                                }
                            });
                        }
                    } else if (n.equals(TablePanel.ON_HIDE)) {
                        if (_dataConnect.getOnlyShowIfDataIsVisible()) {
                            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                public void execute() {
                                    updateVisibilityBasedOnTableVisibility();
                                }
                            });
                        }
                    }
                }
            });
        }

        private void handleDataLoad() {
            if (_dataConnect.isActive()) {
                Drawer drawer;
                for (WebPlotView pv : _allPV.keySet()) {
                    drawer = _allPV.get(pv).getDrawer();
                    redrawAll(pv, drawer, true);
                }
            }
        }

        private void handleHighlightChange() {
            if (_dataConnect.isActive()) {
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        for (WebPlotView pv : _allPV.keySet()) {
                            final WebPlotView tmpPV= pv;
                            final Drawer drawer = _allPV.get(pv).getDrawer();
                            Vis.init(new Vis.InitComplete() {
                                public void done() {
                                    redrawHighlightChangeAsync(tmpPV, drawer);
                                }
                            });
                        }
                    }
                });
            }
        }


        private void handleAreaSelectChange() {
            if (_dataConnect.isActive()) {
                final int selected[] = getAndSaveSelectedArea();
                if (selected.length>0) {
//                    if (selectOwner==null) {
//                        SelectAreaCmd cmd= (SelectAreaCmd)AllPlots.getInstance().getCommand(SelectAreaCmd.CommandName);
//                        cmd.clearSelect();
//                    }
//                    if (selectOwner==DrawingManager.this) selectOwner= null;
                }
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        for (WebPlotView pv : _allPV.keySet()) {
                            final Drawer drawer = _allPV.get(pv).getDrawer();
                            final WebPlotView tmpPV= pv;
                            Vis.init(new Vis.InitComplete() {
                                public void done() {
                                    redrawSelectAreaChangeAsync(tmpPV, drawer, selected);
                                }
                            });
                        }
                    }
                });
            }
        }
    }


//    private class AreaSelectListener implements WebEventListener<Boolean> {
//        public void eventNotify(WebEvent ev) {
//            WebPlotView pv= AllPlots.getInstance().getPlotView();
//            if (pv==null) return;
//            RecSelection selection= (RecSelection)pv.getAttribute(WebPlot.SELECTION);
//            if (selection!=null) {
//                WebPlot plot= pv.getPrimaryPlot();
//                Integer selectedIdx[]= VisUtil.getSelectedPts(selection, plot, _dataConnect.getData(false,plot));
////                if (ev.getSource() instanceof SelectAreaCmd) {
////                    SelectAreaCmd cmd= (SelectAreaCmd)ev.getSource();
////                    if (cmd.getMode() != SelectAreaCmd.Mode.OFF) {
////                        selectOwner= DrawingManager.this;
////                    }
////                }
////                if (selectedIdx.length>0) selectDoneByMe= true;
//                _dataConnect.setSelectedIdx(selectedIdx);
//            }
//            else {
//                if (ev.getData() instanceof Boolean) {
//                    boolean byUser= (Boolean)ev.getData();
//                    if (byUser) _dataConnect.setSelectedIdx();
//                }
//            }
//        }
//    }



    private static class PointUICreator extends WebLayerItem.UICreator {

        public Map<WebLayerItem,HandlerRegistration> handlers= new HashMap<WebLayerItem, HandlerRegistration>(5);

        public Widget makeExtraColumnWidget(final WebLayerItem item) {
            Widget retval= null;
            if (item.getDrawer() instanceof Drawer) {
                final Drawer drawer= (Drawer)item.getDrawer();
                List<DrawObj> l= drawer.getData();
                if (l!=null && l.size()>0 && l.get(0) instanceof PointDataObj) {
                    PointDataObj pd= (PointDataObj)l.get(0);
                    final DrawSymbol symbol= pd.getSymbol();
                    final DefaultDrawable drawable= new DefaultDrawable();

                    redraw(item, drawable,drawer,symbol);
                    retval= drawable.getDrawingPanelContainer();

                    if (handlers.containsKey(item)) handlers.get(item).removeHandler();
                    HandlerRegistration reg= item.addValueChangeHandler(new ValueChangeHandler<String>() {
                        public void onValueChange(ValueChangeEvent<String> ev) {
                            redraw(item, drawable,drawer, symbol);
                        }
                    });
                    handlers.put(item,reg);

                }
            }
            return retval;
        }

        private static void redraw(WebLayerItem item, DefaultDrawable drawable, Drawer drawer, DrawSymbol symbol) {
            WebPlot p= drawer.getPlotView().getPrimaryPlot();
            if (p!=null) {
                Graphics g= Drawer.makeGraphics(drawable, "icon-layer");
                drawable.addDrawingArea(g.getWidget(),false);
                drawable.setPixelSize(12,12);
                PointDataObj pointDataObj= new PointDataObj(new ScreenPt(6,6), symbol);
                pointDataObj.setSize(4);
                pointDataObj.draw(g,drawer.getDrawingDef(),true, false);
            }
        }
    }

}

