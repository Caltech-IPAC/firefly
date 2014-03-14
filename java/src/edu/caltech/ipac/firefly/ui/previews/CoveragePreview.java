package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.CoverageChooser;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.LoadCallback;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.firefly.visualize.draw.TableDataConnection;
import edu.caltech.ipac.firefly.visualize.ui.DisableablePlotDeckPanel;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Date: Feb 20, 2009
 *
 * @author Trey
 * @version $Id: CoveragePreview.java,v 1.35 2012/11/26 22:08:22 roby Exp $
 */
public class CoveragePreview extends AbstractTablePreview {

    private static final WebClassProperties _prop= new WebClassProperties(CoveragePreview.class);

    private static final String COVERAGE_TARGET = "COVERAGE_TARGET";
    public static final String COVERAGE_RADIUS = "COVERAGE_RADIUS";
    public static final String DRAWER_ID = "COVERAGE-";
    public static final String ALL = "ALL";
    public static int idCnt;

    private Map<String,DrawingManager> _drawerMap= new HashMap<String, DrawingManager>(5);
    private TablePanel _lastTable= null;
    private final DisableablePlotDeckPanel _plotDeck;
    private Map<TablePanel,TablePlotInfo> _activeTables= new HashMap<TablePanel,TablePlotInfo>(5);
    private Map<TablePanel,CoverageConnection> _relatedOverlays= new HashMap<TablePanel,CoverageConnection>(5);
    private TablePlotInfo _multiTablePlotInfo= null;

    private TablePanel _currentTable= null;
    private TablePanel _initTable= null;
    private List<TablePanel> _multiTableList= new ArrayList<TablePanel>(7);
    private boolean _externalReplot= true;
    private boolean _catalog = false;
    private boolean _init= false;
    private boolean _initPV= false;
    private String _overlayTitle;
    private final CoverageData _covData;
    private EventHub _hub;
    private boolean isMultiTable= false;
    private WebEventListener wel;


    public CoveragePreview(CoverageData covData) {
        super(covData.getTitle(),covData.getTip());
        _covData= covData;
        setName(covData.getTitle());
        String group= covData.getGroup();
        MiniPlotWidget mpw= new MiniPlotWidget(group);

        if (_covData.getMinWidth()>0 &&_covData.getMinHeight()>0) {
            mpw.setMinSize(_covData.getMinWidth(), _covData.getMinHeight());
        }


        mpw.setImageSelection(true);
        mpw.setRemoveOldPlot(false);
        _plotDeck= new DisableablePlotDeckPanel(_prop.getName("noplot"), mpw,true);
        setDisplay(_plotDeck);
        isMultiTable= covData.isMultiCoverage();
    }

    public MiniPlotWidget getMPW() { return _plotDeck.getMPW();  }


    private void initPlotViewListeners() {

        if (_initPV) return;

        _initPV= true;

        _plotDeck.getMPW().getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(PlotWidgetOps widgetOps) {
                final WebPlotView pv= widgetOps.getPlotView();

                assert (_hub!=null);
                if (_covData.isTreatCatalogsAsOverlays()) {
                    _hub.getCatalogDisplay().addPlotView(pv);
                }
                if (_covData.getEventWorkerList()!=null) {
                    _hub.getDataConnectionDisplay().addPlotView(pv, _covData.getEventWorkerList());
                }

                pv.addListener(Name.PRIMARY_PLOT_CHANGE, new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        if (_currentTable!=null && _externalReplot) {
                            _catalog = false;
                            TablePlotInfo info= getInfo(_currentTable);
                            info.setPlot(pv.getPrimaryPlot());
                        }
                    }
                });


                pv.addListener(Name.PLOTVIEW_LOCKED, new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        if (!pv.isLockedHint() && _currentTable!=null) {
                            _catalog = false;
                            updateCoverage(_currentTable);
                        }
                    }
                });

            }
        });

    }

    public void cleanup() {
        _hub.getEventManager().removeListener(EventHub.ON_TABLE_SHOW, wel);
        _hub.getEventManager().removeListener(EventHub.ON_DATA_LOAD, wel);
        _hub.getEventManager().removeListener(EventHub.ON_TABLE_REMOVED, wel);
        AllPlots.getInstance().delete(_plotDeck.getMPW());
        unbind();
    }


    //=============================================================
    //=============================================================
    //=============================================================
    //=============================================================


    @Override
    public boolean isInitiallyVisible() { return false; }

    @Override
    public void onShow() {
        super.onShow();
        if (_plotDeck.getMPW()!=null) {
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    _plotDeck.getMPW().notifyWidgetShowing();
                }
            });
        }
    }

    @Override
    protected void updateDisplay(TablePanel table) {
        if (table!=null) {
            _plotDeck.getMPW().recallScrollPos();
            AllPlots.getInstance().setSelectedWidget(_plotDeck.getMPW());
            updateCoverage(table);
        }
    }


    @Override
    public void bind(EventHub hub) {
        super.bind(hub);

        _hub = hub;
        wel =  new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                Name evName= ev.getName();

                if (evName.equals(EventHub.ON_TABLE_SHOW)) {
                    TablePanel table = (TablePanel) ev.getSource();
                    updatePanelVisible(table);
                    updateCoverage(table);
                }
                else if (evName.equals(EventHub.ON_DATA_LOAD)) {
                    TablePanel table = (TablePanel) ev.getSource();
                    if (_activeTables.containsKey(table)) {
                        markStale(table);
                        updateCoverage(table);
                    }
                }
                else if (evName.equals(EventHub.ON_TABLE_REMOVED)) {
                    TablePanel table = (TablePanel) ev.getData();
                    if (_activeTables.containsKey(table)) {
                        tableRemoved(table);
                    }
                }
            }
        };
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);
        hub.getEventManager().addListener(EventHub.ON_DATA_LOAD, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_REMOVED, wel);
    }


   //=============================================================
    //=============================================================
    //=============================================================
    //=============================================================


//    private void updateCoverageCenterAndRadius(TablePanel table) {
//        if (_covData.getHasCoverageData(new TableCtx(table))) {
//            TablePlotInfo info= getInfo(table);
//            VisUtil.CentralPointRetval val;
//            if (isMultiTable) {
//                addMultiTable(table);
//                val= calculateMulti();
//            }
//            else {
//                val= _covData.canDoCorners(new TableCtx(table)) ? calculateCentralPointUsingBox(table) :
//                                                                  calculateCentralPointUsingPoint(table);
//            }
//
//            if (val!=null) info.setCircle(val.getWorldPt(), val.getRadius());
//            else           info.setCircle(null,0);
//        }
//    }

    private void tableRemoved(final TablePanel table) {
        _plotDeck.getMPW().getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(PlotWidgetOps ops) {
                WebPlotView pv= ops.getPlotView();
                TablePlotInfo info= getInfo(table);
                if (pv.contains(info.getPlot())) {
                    pv.removePlot(info.getPlot(),true);
                    removeInfo(table);
                }

            }
        });
    }



    private void updatePanelVisible(TablePanel table) {
        if (table.getDataset()==null) return;
        
        TableMeta meta= table.getDataset().getMeta();
        TableCtx tableCtx= new TableCtx(table);
        _catalog =  meta.contains(MetaConst.CATALOG_OVERLAY_TYPE);
        boolean results= _covData.getHasCoverageData(tableCtx);
        if (results && _currentTable==null) _initTable= table;


        boolean show= (_catalog || results || _init);
        if (_covData.isTreatCatalogsAsOverlays()) {
            if (_catalog && _initTable==null && !_init) show= false;
        }

        getEventHub().setPreviewEnabled(this,show);
    }

    private void updateCoverage(final TablePanel table) {

        if ( !GwtUtil.isOnDisplay(getDisplay()) && !GwtUtil.isOnDisplay(table) )  return;
        if (table.getTable()!=null) {
            if (_covData.getHasCoverageData(new TableCtx(table))) {
                _currentTable= table;
                updatePlotAndOverlay(table);
            }
            else if (_catalog){
                if (_currentTable==null && _initTable!=null) {
                    _currentTable= _initTable;
                    updatePlotAndOverlay(table);
                }
            }
        }
    }

    private void updatePlotAndOverlay(final TablePanel table) {
        refreshOverlayData(table, new LoadCallback() {
            public void loaded() {
                _plotDeck.getMPW().getOps(new MiniPlotWidget.OpsAsync() {
                    public void ops(PlotWidgetOps widgetOps) {
                        updatePlotAndOverlayAsync(table, widgetOps);
                    }
                });
            }
        });
    }


    private void addMultiTable(TablePanel table) {
        if (isMultiTable) {
            if (!_multiTableList.contains(table)) {
                _multiTableList.add(table);
            }
        }
    }

    private void updatePlotAndOverlayAsync(TablePanel table, PlotWidgetOps ops) {

        WorldPt plottedCenter;
        Double plottedRadius;
        boolean replotBoth= false;

        if (isMultiTable) addMultiTable(table);


        WebPlot plot= ops.getCurrentPlot();
        WebPlotView pv= ops.getPlotView();
        TablePlotInfo info= getInfo(table);

        if (plot!=null) {

            WebPlot lastPlot= info.getPlot();
            if (plot!= lastPlot && pv.contains(lastPlot)) {
                plot= lastPlot;
                pv.setPrimaryPlot(plot);
                replotBoth= false;
//                _plotDeck.getMPW().setTitle(info.getTitle());
                _plotDeck.getMPW().setTitle(plot.getPlotDesc());
            }

            plottedCenter= (WorldPt)plot.getAttribute(COVERAGE_TARGET);
            plottedRadius= (Double)plot.getAttribute(COVERAGE_RADIUS);
            if (!ComparisonUtil.equals(info.getCenter(),plottedCenter) ||
                info.getRadius()!=plottedRadius ||
                !_plotDeck.isPlotShowing()) {
                     replotBoth= true;
            }
        }
        else {
            replotBoth= true;
        }

        boolean locked= (pv!=null) && pv.isLockedHint() && pv.size()>0;

        if (getRowCount(table)>0) {
            if (replotBoth && !locked) {
                replotCoverageImage(table);
            }
            else {
                _plotDeck.showPlot();
                if (isMultiTable) {
                    for(TablePanel t : _multiTableList) replotCoverageCatalog(t);
                }
                else {
                    replotCoverageCatalog(table);
                }
            }
        }
        else {
            _plotDeck.showNoData(_prop.getName("nocov"));
        }
        _init= true;


    }

    private int getRowCount(TablePanel table) {
        int cnt= 0;
        if (isMultiTable) {
            for(TablePanel t : _multiTableList) {
                cnt+=getRowCountFromTable(t);
            }
        }
        else {
           cnt= getRowCountFromTable(table);
        }
        return cnt;
    }


    private int getRowCountFromTable(TablePanel t) {
        int retval= 0;
        if (!isStale(t)) {
            CoverageConnection conn= _relatedOverlays.get(t);
            if (conn.getTableDatView()!=null) {
                retval= conn.getTableDatView().getSize();
            }
        }
        return retval;
    }



    private void replotCoverageCatalog(TablePanel table) {
        DrawingManager drawer= _drawerMap.get(isMultiTable ? table.getName() : ALL);

        if (drawer==null) {
            String id= DRAWER_ID+idCnt;
            idCnt++;
            drawer= new DrawingManager(id,null);
            drawer.setDefaultColor(_covData.getColor(table.getName()));
            drawer.setHighlightedColor(_covData.getHighlightedColor(table.getName()));
            _drawerMap.put(isMultiTable ? table.getName() : ALL, drawer);
        }
        if (!drawer.containsPlotView(_plotDeck.getMPW().getPlotView())) {
            drawer.addPlotView(_plotDeck.getMPW().getPlotView());
        }

        updateOverlayTitleTitle(table);
        drawer.setDataConnection(_relatedOverlays.get(table));
        _lastTable= table;
        drawer.redraw();
    }


    private void replotCoverageImage(TablePanel table) {
        Widget w= getWidget();
        if (w==null || !GwtUtil.isOnDisplay(w)) return;

        TableCtx tableCtx= new TableCtx(table);
        TablePlotInfo info= getInfo(table);
        double radiusD= info.getRadius();

        if (Double.isInfinite(radiusD) || Double.isNaN(radiusD) || radiusD==0F) {
            _plotDeck.showNoData(_prop.getName("cantCompute"));
        }
        else {
            WorldPt wp= info.getCenter();


            TableMeta meta= table.getDataset().getMeta();
            _catalog =  meta.contains(MetaConst.CATALOG_OVERLAY_TYPE);
            _overlayTitle= _covData.getUseBlankPlot() ? _covData.getTitle() : _covData.getCoverageBaseTitle(tableCtx);
            updateOverlayTitleTitle(table);

            int width=  w.getOffsetWidth()-15;
            int height= w.getOffsetHeight()-10;
            WebPlotRequest request= new CoverageChooser().getRequest(wp,(float)radiusD,_overlayTitle+" ",
                                                                     _covData.getSmartZoomHint(),
                                                                     _covData.getUseBlankPlot(),
                                                                     _covData.getGridOn(),
                                                                     width);
            if (_covData.isMinimalReadout()) request.setMinimalReadout(true);
            if (_covData.getQueryCenter()!=null) {
                request.setOverlayPosition(_covData.getQueryCenter());
            }
            else if (tableCtx.getOverlayPosition()!=null) {
                request.setOverlayPosition(tableCtx.getOverlayPosition());
            }

            if (width>50 && _covData.getFitType()== CoverageData.FitType.WIDTH) {
                request.setZoomType(ZoomType.TO_WIDTH);
                request.setZoomToWidth(width);
            }
            else if (width>50 && height>50 && _covData.getFitType()== CoverageData.FitType.WIDTH_HEIGHT) {
                request.setZoomType(ZoomType.FULL_SCREEN);
                request.setZoomToWidth(width);
                request.setZoomToHeight(height);
            }



            _plotDeck.showPlot();
            if (!request.equals(info.getActivePlottingRequest())) {
                info.setActivePlottingRequest(request);
                plot(table, request);
            }
        }
    }

    private void updateOverlayTitleTitle(TablePanel table) {
        TableCtx tableCtx= new TableCtx(table);
        _overlayTitle= _covData.getUseBlankPlot() ? _covData.getTitle() : _covData.getCoverageBaseTitle(tableCtx);

    }

    private void plot(final TablePanel table,
                      final WebPlotRequest request) {


        AsyncCallback<WebPlot> notify= new AsyncCallback<WebPlot>() {
            public void onFailure(Throwable caught) {
                _externalReplot= true;
                _plotDeck.showNoData();
            }

            public void onSuccess(WebPlot plot) {
                boolean allSky= (request.getRequestType()== RequestType.ALL_SKY);
                initPlotViewListeners();
                TablePlotInfo info= getInfo(table);

                MiniPlotWidget mpw= _plotDeck.getMPW();
                if (mpw.contains(info.getPlot())) {
                    mpw.getOps().removePlot(info.getPlot());
                }

                info.setPlot(plot);
                info.setTitle(request.getTitle());
                info.setAllSky(allSky);
                info.setActivePlottingRequest(null);

                plot.setAttribute(COVERAGE_TARGET,info.getCenter());
                plot.setAttribute(COVERAGE_RADIUS, info.getRadius());
                if (isMultiTable) {
                    for(TablePanel t : _multiTableList) replotCoverageCatalog(t);
                }
                else {
                    replotCoverageCatalog(table);
                }
                _externalReplot= true;
                plot.setPlotDesc(request.getTitle());
            }
        };

        _externalReplot= false;
        _plotDeck.getMPW().getOps().plot(request,false, notify);
    }



    private boolean isCatalogShowing() { return true; }


    private VisUtil.CentralPointRetval calculateMulti() {
        TableCtx tableCtx;
        VisUtil.CentralPointRetval retval;
        ArrayList<WorldPt> wpList= new ArrayList<WorldPt>(3000) ;
        for(TablePanel table : _multiTableList) {
            tableCtx= new TableCtx(table);
            List<WorldPt> tList= null;
            if (_covData.canDoCorners(tableCtx)) {
                tList= getPointAryUsingBox(table);
            }
            else {
                tList= getPointAryUsingPoint(table);
            }
            if (tList!=null) wpList.addAll(tList);

        }
        if (CoveragePreview.isOnePoint(wpList)) {
            retval= new VisUtil.CentralPointRetval(wpList.get(0), .05D);
        }
        else {
            retval = VisUtil.computeCentralPointAndRadius(wpList);
        }
        return retval;
    }

    private VisUtil.CentralPointRetval calculateCentralPointUsingBox(TablePanel table) {
        VisUtil.CentralPointRetval retval= null;
        List<TableData.Row> rows = getRows(table);
        if (rows != null )  {
            if (rows.size() > 0) {
                List<WorldPt> wpList= getPointAryUsingBox(table);
                retval = VisUtil.computeCentralPointAndRadius(wpList);
            }
        }
        return retval;
    }


    private List<WorldPt> getPointAryUsingBox(TablePanel table) {
        List<WorldPt> wpList= Collections.emptyList();
        TableCtx tableCtx= new TableCtx(table);
        TableMeta.LonLatColumns cornerCols[]= _covData.getCornersColumns(tableCtx);
        List<TableData.Row> rows = getRows(table);
        if (rows != null )  {
            if (rows.size() > 0) {
                wpList  = new ArrayList<WorldPt>();
                for(TableData.Row row : rows) {
                    try {
                        for(TableMeta.LonLatColumns  corner : cornerCols) {
                            String raS = String.valueOf(row.getValue(corner.getLonCol()));
                            double ra = Double.parseDouble(raS);
                            String decS = String.valueOf(row.getValue(corner.getLatCol()));
                            double dec = Double.parseDouble(decS);
                            if (ra != -1 && dec != -1) {
                                wpList.add( new WorldPt(ra,dec,corner.getCoordinateSys()));
                            }
                        }
                    } catch(NumberFormatException ex) {
                        // skip bad data
                    }
                }

            }
        }
        return wpList;
    }

    private VisUtil.CentralPointRetval calculateCentralPointUsingPoint(TablePanel table) {
        VisUtil.CentralPointRetval retval= null;
        List<TableData.Row> rows = getRows(table);
        if (rows != null )  {
            if (rows.size() > 0) {
                List<WorldPt> wpList= getPointAryUsingPoint(table);
                if (isOnePoint(wpList)) {
                    retval= new VisUtil.CentralPointRetval(wpList.get(0), .05D);
                }
                else {
                    retval = VisUtil.computeCentralPointAndRadius(wpList);
                }
            }
        }
        return retval;
    }


    private List<WorldPt> getPointAryUsingPoint(TablePanel table) {
        TableCtx tableCtx= new TableCtx(table);
        ArrayList<WorldPt> wpList= null;
        TableMeta.LonLatColumns cols= _covData.getCenterColumns(tableCtx);
        List<TableData.Row> rows = getRows(table);
        if (rows != null )  {
            if (rows.size() > 0) {
                wpList  = new ArrayList<WorldPt>();
                for(TableData.Row row : rows) {
                    try {
                        String raS = String.valueOf(row.getValue(cols.getLonCol()));
                        double ra = Double.parseDouble(raS);
                        String decS = String.valueOf(row.getValue(cols.getLatCol()));
                        double dec = Double.parseDouble(decS);
                        if (ra != -1 && dec != -1) {
                            wpList.add( new WorldPt(ra,dec,cols.getCoordinateSys()));
                        }
                    } catch(NumberFormatException ex) {
                        // skip bad data
                    }
                }

            }
        }
       return wpList;
    }



    public static boolean isOnePoint(List<WorldPt> wpList) {
        boolean retval= false;
        if (wpList.size()>0) {
            retval= true;
            WorldPt first= wpList.get(0);
            for(WorldPt wp : wpList) {
                if (!wp.equals(first)) {
                    retval= false;
                    break;
                }
            }
        }
        return retval;
    }

    public CoverageData.CoverageType getCoverageType(TablePanel table)  {
        CoverageData.CoverageType retval= CoverageData.CoverageType.X;
        if (!getInfo(table).isAllSky() && _covData.canDoCorners(new TableCtx(table))) retval= CoverageData.CoverageType.BOX;
        return retval;
    }


    public TablePlotInfo getInfo(TablePanel table)  {
        TablePlotInfo retval;
        if (isMultiTable) {
            if (_multiTablePlotInfo==null) {
                _multiTablePlotInfo= new TablePlotInfo();
            }
            retval= _multiTablePlotInfo;
        }
        else {
            if (_activeTables.containsKey(table)) {
                retval= _activeTables.get(table);
            }
            else {
                retval= new TablePlotInfo();
                _activeTables.put(table,retval);
            }
        }
        return retval;
    }

    public void removeInfo(TablePanel table) {
        if (!isMultiTable) {
            if (_activeTables.containsKey(table)) {
                _activeTables.remove(table);
            }
        }
    }

    public boolean isStale(TablePanel table) {
        return !_relatedOverlays.containsKey(table) || _relatedOverlays.get(table).getTableDatView()==null;
    }

    public void markStale(TablePanel table) {
        if (_relatedOverlays.containsKey(table))  _relatedOverlays.remove(table);
    }

    public List<TableData.Row> getRows(TablePanel table) {
        List<TableData.Row> rows= null;
        if (!isStale(table)) {
            TableDataView tableDataView=_relatedOverlays.get(table).getTableDatView();
            TableCtx tableCtx= new TableCtx(table);
            rows = tableDataView.getModel().getRows();
        }
        return rows;
    }

    public void refreshOverlayData(final TablePanel table, final LoadCallback cb) {
        if (isStale(table)) {
            CoverageConnection connection= new CoverageConnection(table);
            _relatedOverlays.put(table,connection);
            connection.getAsyncDataLoader().requestLoad(new LoadCallback() {
                public void loaded() {
                    if (_covData.getHasCoverageData(new TableCtx(table))) {
                        TablePlotInfo info= getInfo(table);
                        VisUtil.CentralPointRetval val;
                        if (isMultiTable) {
                            addMultiTable(table);
                            val= calculateMulti();
                        }
                        else {
//                            val= _covData.canDoCorners(new TableCtx(table)) ? calculateCentralPointUsingBox(table) :
//                                 calculateCentralPointUsingPoint(table);
                            val= getCoverageType(table)== CoverageData.CoverageType.BOX ? calculateCentralPointUsingBox(table) :
                                                                                          calculateCentralPointUsingPoint(table);
                        }

                        if (val!=null) info.setCircle(val.getWorldPt(), val.getRadius());
                        else           info.setCircle(null,0);
                    }
                    cb.loaded();
                }
            });
        }
        else {
           cb.loaded();
        }
    }


// ===============================================================================
// ------------------------- Inner Classes ---------------------------------------
// ===============================================================================



    public class CoverageConnection extends TableDataConnection {

        private List<DrawObj> _graphObj=  new ArrayList<DrawObj>(100);
        private final CoverageData.CoverageType covType;

        CoverageConnection(TablePanel table) {
            super(table,_prop.getName("mouseHelp"), true, getCoverageType(table)== CoverageData.CoverageType.X,
                                                          getCoverageType(table)== CoverageData.CoverageType.X,
                                                       true, false);
            covType= getCoverageType(table);
        }

        @Override
        public String getTitle(WebPlot plot) {
            if (isMultiTable) {
                return StringUtils.isEmpty(_overlayTitle) ? "Coverage: "+getTable().getName() : _overlayTitle;
            }
            else {
                return StringUtils.isEmpty(_overlayTitle) ? "Coverage" : _overlayTitle;
            }
        }


        public List<DrawObj> getDataImpl() {
            _graphObj=  new ArrayList<DrawObj>(100);

            int tabSize= size();
            TablePanel table= getTable();
            TableCtx tableCtx= new TableCtx(table);
            TableData model= getTableDatView().getModel();

            if (covType == CoverageData.CoverageType.X) {
                TableMeta.LonLatColumns cols= _covData.getCenterColumns(tableCtx);
                int raIdx= model.getColumnIndex(cols.getLonCol());
                int decIdx= model.getColumnIndex(cols.getLatCol());

                for(int i= 0; i<tabSize; i++) {
                    _graphObj.add(makePointObj(table, i, raIdx,decIdx,cols.getCoordinateSys()));
                }

            } else if (covType == CoverageData.CoverageType.BOX) {
                for(int i= 0; i<tabSize; i++) {
                    _graphObj.add(makeFootprintObj(tableCtx,i,model));
                }
            }

            return _graphObj;
        }
        @Override
        public boolean isPointData() { return covType== CoverageData.CoverageType.X; }


        public List<DrawObj> getHighlightDataImpl() {
            DrawObj retval= null;

            TablePanel table= getTable();
            TableCtx tableCtx= new TableCtx(table);
            TableData model= getTableDatView().getModel();
            int rowIdx= getTable().getDataModel().getCurrentData().getHighlighted();

            if (covType == CoverageData.CoverageType.X) {
                TableMeta.LonLatColumns cols= _covData.getCenterColumns(tableCtx);
                int raIdx= model.getColumnIndex(cols.getLonCol());
                int decIdx= model.getColumnIndex(cols.getLatCol());
                retval= makePointObj(table, rowIdx, raIdx,decIdx,cols.getCoordinateSys());

            } else if (covType == CoverageData.CoverageType.BOX) {
                retval= makeFootprintObj(tableCtx,rowIdx,model);
            }

            if (retval!=null) retval.setHighlighted(true);
            return Arrays.asList(retval);


        }

        private PointDataObj makePointObj(TablePanel table, int rowIdx, int raIdx, int decIdx, CoordinateSys csys) {
            PointDataObj retval= null;
            DrawSymbol symbol= _covData.getShape(table.getName());
            WorldPt graphPt = getWorldPt(rowIdx, raIdx, decIdx, csys);
            if (graphPt != null) {
                retval= new PointDataObj(graphPt, symbol);
                int size= _covData.getSymbolSize(table.getName());
                retval.setSize(size);
            }
            return retval;
        }


        private FootprintObj makeFootprintObj(TableCtx tableCtx, int rowIdx, TableData model) {
            TableMeta.LonLatColumns cornerCols[]= _covData.getCornersColumns(tableCtx);
            WorldPt [] wpts = new WorldPt[cornerCols.length];
            int idx= 0;
            for(TableMeta.LonLatColumns  corner : cornerCols) {
                int raIdx= model.getColumnIndex(corner.getLonCol());
                int decIdx= model.getColumnIndex(corner.getLatCol());

                WorldPt graphPt = getWorldPt(rowIdx, raIdx, decIdx, corner.getCoordinateSys());
                if (graphPt != null)
                    wpts[idx++] = graphPt;
                else
                    break;
            }
            List<WorldPt[]> cAry= _covData.modifyBox(wpts,tableCtx,getTableDatView().getModel().getRow(rowIdx));
            return new FootprintObj(cAry);
        }


        protected List<String> getDataColumns() {
            List<String> colList= new ArrayList<String>(8);
            TableCtx tableCtx= new TableCtx(getTable());
            if (covType == CoverageData.CoverageType.X) {
                TableMeta.LonLatColumns cols= _covData.getCenterColumns(tableCtx);
                colList.add(cols.getLonCol());
                colList.add(cols.getLatCol());
            } else if (covType == CoverageData.CoverageType.BOX) {
                TableMeta.LonLatColumns cornerCols[]= _covData.getCornersColumns(tableCtx);
                for(TableMeta.LonLatColumns  corner : cornerCols) {
                    colList.add(corner.getLonCol());
                    colList.add(corner.getLatCol());
                }
            }

            colList.addAll(_covData.getExtraColumns());
            return colList;
        }

        public boolean isActive() { return isCatalogShowing(); }

        private WorldPt getWorldPt(int row, int raIdx, int decIdx, CoordinateSys csys) {
            TableData.Row r=getTableDatView().getModel().getRow(row);
            WebAssert.argTst(r!=null, "row : " +row+" should not be null");
            String raStr= (String)r.getValue(raIdx);
            String decStr= (String)r.getValue(decIdx);

            try {
                double ra= Double.parseDouble(raStr);
                double dec= Double.parseDouble(decStr);
                return new WorldPt(ra,dec,csys);
            } catch (NumberFormatException e) {
                return null;
            }
        }

    }

    public static class TablePlotInfo {
        private WebPlotRequest _activeRequest;
        private WebPlot _plot= null;
        private String _title= null;
        private double  _radius= 0F;
        private WorldPt _center= null;
        private boolean _allSky= false;
        private WorldPt _tableQueryCenter= null;

        public TablePlotInfo() {  }


        public WebPlot getPlot() { return _plot; }
        public void setPlot(WebPlot plot) { _plot= plot; }


        public boolean isAllSky() { return _allSky; }
        public void setAllSky(boolean allSky) { _allSky= allSky; }


        public String getTitle() { return _title; }
        public void setTitle(String title) { _title= title; }

        public WorldPt getCenter() { return _center; }
        public double getRadius() { return _radius; }

        public void setCircle(WorldPt center, double radius) {
            _radius= radius;
            _center= center;
        }

        public void setActivePlottingRequest(WebPlotRequest request) {
            _activeRequest = request;
        }

        public WebPlotRequest getActivePlottingRequest() { return _activeRequest;}

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
