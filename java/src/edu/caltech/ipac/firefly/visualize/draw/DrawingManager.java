package edu.caltech.ipac.firefly.visualize.draw;

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
import edu.caltech.ipac.firefly.visualize.DefaultDrawable;
import edu.caltech.ipac.firefly.visualize.PrintableOverlay;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ProjectionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Jun 19, 2008
 * Time: 3:53:56 PM
 */


/**
 * A DrawingManager can take one data set and draw it on many different WebPlotView's.
 * @author Trey Roby
 */
public class DrawingManager implements AsyncDataLoader {

    public static final int SELECT_DIST = 5;
    private DataConnection _dataConnect;
    private final String _id;

    private static final DrawSymbol DEF_SYMBOL = DrawSymbol.X;
    private static final DrawSymbol DEF_HIGHLIGHT_SYMBOL = DrawSymbol.SQUARE_X;


    private String _highlightedColor = AutoColor.HIGHLIGHTED_PT;
    private String _areaSelectedColor = AutoColor.SELECTED_PT;
    private String _normalColor = AutoColor.PT_1;
    private final WebEventListener _listener = new TableViewListener();
    private int _lastHighlighted = -1;
    private int _lastAreaSelected[] = new int[0];
    private Map<WebPlotView, PVData> _allPV = new HashMap<WebPlotView, PVData>(5);
    private boolean _init = false;
    private boolean _groupByTitleOrID= false;  // if false group by id only , if true group by either id or title

    private DrawSymbol _autoDefHighlightSymbol = DEF_HIGHLIGHT_SYMBOL;
    private String _enablePrefKey= null;
    private final PrintableOverlay _printableOverlay;
    private boolean canDoRegion= true;
    private AreaSelectListener _areaSelectListener= new AreaSelectListener();



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
           _dataConnect.getAsyncDataLoader().requestLoad(new LoadCallback() {
               public void loaded() {
                   cb.loaded();
                   for (PVData pvData : _allPV.values()) {
                       Drawer drawer= pvData.getDrawer();
                       drawer.setData(_dataConnect.getData(false,drawer.getPlotView().getPrimaryPlot()));
                       drawer.redraw();
                   }
               }
           });
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
    //======================================================================
//----------------------- Public Methods -------------------------------
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
                    data.getDrawer().setDefaultColor(color);
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

        Vis.init(new Vis.InitComplete() {
            public void done() {
                Drawer drawer= connectDrawer(pv);
                updateWebLayerItem();
                redrawAll(pv, drawer, false, getAndSaveHighlighted());
                if (_dataConnect!=null && _dataConnect.getSupportsAreaSelect()) pv.addListener(Name.AREA_SELECTION, _areaSelectListener);
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
                        redrawAll(pv, _allPV.get(pv)._drawer, false, getAndSaveHighlighted());
                    }
                }
            }
        });


    }

    public boolean getSupportsAreaSelect() {
        return _dataConnect!=null &&_dataConnect.getSupportsAreaSelect();
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
                if (ptIdxAry.length>0)  _dataConnect.filter(filterIn, ptIdxAry);
            }
        }
    }




    private Drawer connectDrawer(WebPlotView pv) {
        WebPlotView.MouseInfo mi = new WebPlotView.MouseInfo(new Mouse(pv),
                                                             "Click to select an Observation");
        boolean highPriority= _dataConnect!=null&&_dataConnect.isPriorityLayer();
        Drawer drawer = new Drawer(pv,highPriority);
        String helpLine = _dataConnect == null ? null : _dataConnect.getHelpLine();
        WebLayerItem item = new WebLayerItem(_id, getTitle(pv), helpLine, drawer, mi,
                                             _enablePrefKey,_printableOverlay);
        if (_dataConnect != null) {
            drawer.setPointConnector(_dataConnect.getDrawConnector());
            drawer.setEnableDecimationDrawing(_dataConnect.isPointData());
            if (_dataConnect.isVeryLargeData()) drawer.setDataTypeHint(Drawer.DataType.VERY_LARGE);

            if (_dataConnect.getAsyncDataLoader()!=null) item.setDrawingManager(this);
            if (_dataConnect.isPointData()) WebLayerItem.addUICreator(_id, new PointUICreator());
        }
        item.setCanDoRegion(canDoRegion);
        item.setGroupByTitleOrID(_groupByTitleOrID);
        drawer.setDefaultColor(_normalColor);
        pv.addPersistentMouseInfo(mi);
        pv.addWebLayerItem(item);
        _allPV.put(pv, new PVData(drawer, mi, item));

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
                if (_dataConnect.getOnlyIfDataVisible()) updateVisibility(_dataConnect.isDataVisible());
                if (_dataConnect.getAsyncDataLoader()!=null) item.setDrawingManager(this);
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

                if (_dataConnect.getSupportsAreaSelect())  {
                    pv.removeListener(Name.AREA_SELECTION, _areaSelectListener);
                }
            }
            _allPV.remove(pv);
        }
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
                pvData.getDrawer().setDefaultColor(_normalColor);
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
//                evM.removeListener(TablePanel.ON_PAGE_LOAD, _listener);
                evM.removeListener(TablePanel.ON_SHOW, _listener);
                evM.removeListener(TablePanel.ON_HIDE, _listener);
                evM.removeListener(TablePanel.ON_DATA_LOAD, _listener);
            }
            for (Map.Entry<WebPlotView,PVData> entry : _allPV.entrySet()) {
                WebPlotView pv= entry.getKey();
                if (_dataConnect.getSupportsAreaSelect())  pv.removeListener(Name.AREA_SELECTION, _areaSelectListener);
            }
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
//                evM.addListener(TablePanel.ON_PAGE_LOAD, _listener);
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

                if (_dataConnect.getSupportsAreaSelect()) pv.addListener(Name.AREA_SELECTION, _areaSelectListener);

                checkAndSetupPerPlotData(pv, drawer);

                for (WebLayerItem item : pv.getUserDrawerLayerSet()) {
                    if (drawer == item.getDrawer()) {
                        item.setTitle(getTitle(null));
                    }
                }


            }
            if (_dataConnect.isPointData()) WebLayerItem.addUICreator(_id, new PointUICreator());
        }
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
                    return DrawingManager.this.getData(false, pv.getPrimaryPlot());
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
                        redrawAll(pv, drawer, false, getAndSaveHighlighted());
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
           redrawAll(pv, data.getDrawer(),true);
        }

    }

    void redrawAll(final WebPlotView pv,
                   final Drawer drawer,
                   final boolean forceRebuild,
                   final int... selected) {
        if (_dataConnect==null) return;
        Vis.init(new Vis.InitComplete() {
            public void done() {
                GwtUtil.isOnDisplay(pv);
                AsyncDataLoader loader= _dataConnect.getAsyncDataLoader();
                if (drawer.isVisible() && loader!=null && !loader.isDataAvailable()) {
                    final String drawTaskID= pv.addTask();
                    loader.requestLoad(new LoadCallback() {
                        public void loaded() {
                            redrawAllAsync(pv, drawer, forceRebuild, selected);
                            pv.removeTask(drawTaskID);
                        }
                    });
                }
                else {
                    redrawAllAsync(pv, drawer, forceRebuild, selected);
                }
            }
        });
    }


    void redrawAllAsync(final WebPlotView pv,
                        Drawer drawer,
                        boolean forceRebuild,
                        int... selected) {


        if (_dataConnect == null) {
            drawer.setData((List<DrawObj>) null);
            return;
        }

        List<DrawObj> data = getData(forceRebuild, pv.getPrimaryPlot());

        if (data == null || data.size() == 0) {
            drawer.setData(data);
            return;
        }

        if (data.size() < selected.length) {
            data = getData(true, pv.getPrimaryPlot());
        }

        if (_dataConnect.getSupportsHighlight()) {
            for (DrawObj obj : data) {
                updateHighlighted(false, obj);
            }

            DrawObj obj;
            for (int sidx : selected) {
                if (sidx > -1) {
                    obj = data.get(sidx);
                    updateHighlighted(true, obj);
                }
            }
        }
        final int scrollX = pv.getScrollX();
        final int scrollY = pv.getScrollY();

        drawer.setData(data);

        DeferredCommand.addCommand(new Command() {
            public void execute() {
                pv.setScrollXY(scrollX, scrollY);
            }
        });
    }


    private void updateVisibility(boolean visible) {
        if (_init) {
            PVData data;
            for (Map.Entry<WebPlotView, PVData> entry : _allPV.entrySet()) {
                data = entry.getValue();
                if (data.getWebLayerItem().isVisible() != visible) {
                    data.getWebLayerItem().setVisible(visible);
                }
            }
        }
    }


//    void redrawShapeChange(final WebPlotView pv,
//                               Drawer drawer,
//                               int unSelected[],
//                               int selected[],
//                               PointDataObj.Symbols oldSymbol,
//                               PointDataObj.Symbols newSymbol) {
//        if (_table!=null && _table.getRowCount()==0) return;
//        if (unSelected.length>0 || selected.length>0) {
//            List<DrawObj> data = _dataConnect.getData(false);
//
//            ArrayList<Integer> idxs= new ArrayList<Integer>(unSelected.length+selected.length);
//            DrawObj obj;
//
//            for(int sidx : unSelected) {
//                obj= data.get(sidx);
//                if (obj instanceof PointDataObj) ((PointDataObj)obj).setSymbol(oldSymbol);
//                updateHighlighted(false,obj);
//                idxs.add(sidx);
//            }
//
//            for(int sidx : selected) {
//                obj= data.get(sidx);
//                if (obj instanceof PointDataObj) ((PointDataObj)obj).setSymbol(newSymbol);
//                updateHighlighted(true,obj);
//                idxs.add(sidx);
//            }
//            final int scrollX = pv.getScrollX();
//            final int scrollY = pv.getScrollY();
//
//
//            drawer.setDataDelta(data,idxs);
//
//            DeferredCommand.addCommand(new Command() {
//                public void execute() {
//                    pv.setScrollX(scrollX);
//                    pv.setScrollY(scrollY);
//                }
//            });
//        }
//    }


    private void redrawHighlightChangeAsync(final WebPlotView pv,
                                            Drawer drawer,
                                            int unHighlighted,
                                            int highlighted) {
        if ((_dataConnect != null && _dataConnect.size() == 0) ||
                (_dataConnect != null && !_dataConnect.getSupportsHighlight())) {
            return;
        }

        if (_dataConnect == null) return;

        if (unHighlighted> -1 || highlighted> -1) {
            List<DrawObj> data = getData(false, pv.getPrimaryPlot());

            DrawObj obj;
            if (unHighlighted>-1 && data.size()>unHighlighted) updateHighlighted(false, data.get(unHighlighted));
            if (highlighted>-1 && data.size()>highlighted) updateHighlighted(true, data.get(highlighted));

            final int scrollX = pv.getScrollX();
            final int scrollY = pv.getScrollY();

//            drawer.setDataDelta(data, Arrays.asList(unHighlighted,highlighted));
            drawer.updateDataHighlightLayer(data);

            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    pv.setScrollXY(scrollX, scrollY);
                }
            });
        }
    }


    private void redrawSelectAreaChangeAsync(final WebPlotView pv,
                                             Drawer drawer,
                                             int selected[]) {
        if ((_dataConnect != null && _dataConnect.size() == 0) ||
                (_dataConnect != null && !_dataConnect.getSupportsAreaSelect())) {
            return;
        }

        if (_dataConnect == null) return;

        if (selected.length > 0) {
            List<DrawObj> data = getData(false, pv.getPrimaryPlot());

            for(DrawObj d : data) updateSelected(false,d);


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
            drawer.clearSelectLayer();
        }
    }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void updateHighlighted(boolean highlighted, DrawObj dObj) {
        if (highlighted)  {
            dObj.setHighlightColor(_highlightedColor);
            if (dObj instanceof PointDataObj) ((PointDataObj) dObj).setHighlightSymbol(_autoDefHighlightSymbol);
        }
        dObj.setHighlighted(highlighted);

    }

    private void updateSelected(boolean selected, DrawObj dObj) {
        if (selected)  {
            dObj.setSelectColor(_areaSelectedColor);
        }
        dObj.setSelected(selected);

    }


    private void highlightNearest(WebPlotView pv, ScreenPt pt) {

        if (_dataConnect == null || !_dataConnect.getSupportsMouse()) return;

        WebPlot plot = pv.getPrimaryPlot();
        double dist;
        double minDist = Double.MAX_VALUE;
        List<DrawObj> data = getData(false, plot);
        if (data == null) return;
        DrawObj closestPt = null;
        int idx= 0;
        int closestIdx= -1;
        for (DrawObj obj : data) {
            try {
                dist = obj.getScreenDist(plot, pt);
            } catch (ProjectionException e) {
                dist = -1;
            }

            if (dist > -1 && dist < minDist) {
                minDist = dist;
                closestPt = obj;
                closestIdx= idx;


            }
            idx++;
        }

        if (closestPt != null) {
            setTableHighlightRows(closestIdx);
        }
    }

    private void setTableHighlightRows(int idx) {
        if (_dataConnect != null) {
            _dataConnect.setHighlightedIdx(idx);
        }
    }


    private int getAndSaveHighlighted() {
        if (_dataConnect != null) {
            _lastHighlighted = _dataConnect.getHighlightedIdx();
        } else {
            _lastHighlighted= -1;
        }
        return _lastHighlighted;
    }
    private int[] getAndSaveSelectedArea() {
        int[] selAry = new int[0];
        if (_dataConnect != null && _dataConnect.getSupportsAreaSelect()) {
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
        private final Drawer _drawer;
        private final WebPlotView.MouseInfo _mi;
        private final WebLayerItem _layerItem;

        public PVData(Drawer drawer, WebPlotView.MouseInfo mi, WebLayerItem layerItem) {
            _drawer = drawer;
            _mi = mi;
            _layerItem = layerItem;
        }

        public Drawer getDrawer() { return _drawer; }
        public WebLayerItem getWebLayerItem() { return _layerItem; }
        public WebPlotView.MouseInfo getMouseInfo() { return _mi; }

    }


    private class TableViewListener implements WebEventListener {

        public void eventNotify(final WebEvent ev) {
//            if (!_updatesEventsEnabled) return;

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
                        if (_dataConnect.getOnlyIfDataVisible()) updateVisibility(true);
                    } else if (n.equals(TablePanel.ON_HIDE)) {
                        if (_dataConnect.getOnlyIfDataVisible()) updateVisibility(false);
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
                final int lastHighlighted = _lastHighlighted;
                final int highlighted = getAndSaveHighlighted();
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        for (WebPlotView pv : _allPV.keySet()) {
                            final WebPlotView tmpPV= pv;
                            final Drawer drawer = _allPV.get(pv).getDrawer();
                            Vis.init(new Vis.InitComplete() {
                                public void done() {
                                    redrawHighlightChangeAsync(tmpPV, drawer, lastHighlighted, highlighted);
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


    private class AreaSelectListener implements WebEventListener<WebPlotView> {
        public void eventNotify(WebEvent<WebPlotView> ev) {
            WebPlotView pv= ev.getData();
            RecSelection selection= (RecSelection)pv.getAttribute(WebPlot.SELECTION);
            if (selection!=null) {
                WebPlot plot= pv.getPrimaryPlot();
                Integer selectedIdx[]= VisUtil.getSelectedPts(selection, plot, _dataConnect.getData(false,plot));
                _dataConnect.setSelectedIdx(selectedIdx);
            }
            else {
                _dataConnect.setSelectedIdx();
            }
        }
    }



    private List<DrawObj> getData(boolean forceRebuild, WebPlot plot) {
        List<DrawObj> retval;
        retval = _dataConnect.getData(forceRebuild, plot);
        return retval;
    }

    private static class PointUICreator extends WebLayerItem.UICreator {

        public Map<WebLayerItem,HandlerRegistration> handlers= new HashMap<WebLayerItem, HandlerRegistration>(5);

        public Widget makeExtraColumnWidget(final WebLayerItem item) {
            List<DrawObj> l= item.getDrawer().getData();
            Widget retval= null;
            if (l!=null && l.size()>0 && l.get(0) instanceof PointDataObj) {
                PointDataObj pd= (PointDataObj)l.get(0);
                final DrawSymbol symbol= pd.getSymbol();
                final DefaultDrawable drawable= new DefaultDrawable();

                redraw(item, drawable,symbol);
                retval= drawable.getDrawingPanelContainer();

                if (handlers.containsKey(item)) handlers.get(item).removeHandler();
                HandlerRegistration reg= item.addValueChangeHandler(new ValueChangeHandler<String>() {
                    public void onValueChange(ValueChangeEvent<String> ev) {
                        redraw(item, drawable,symbol);
                    }
                });
                handlers.put(item,reg);

            }
            return retval;
        }

        private static void redraw(WebLayerItem item, DefaultDrawable drawable, DrawSymbol symbol) {
            WebPlot p= item.getDrawer().getPlotView().getPrimaryPlot();
            if (p!=null) {
                Graphics g= Drawer.makeGraphics(drawable, "icon-layer");
                drawable.addDrawingArea(g.getWidget(),false);
                drawable.setPixelSize(12,12);
                PointDataObj pointDataObj= new PointDataObj(new ScreenPt(6,6), symbol);
                pointDataObj.setSize(4);
                pointDataObj.draw(g,new AutoColor(p.getColorTableID(),item.getDrawer().getDefaultColor()),true);
            }

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
