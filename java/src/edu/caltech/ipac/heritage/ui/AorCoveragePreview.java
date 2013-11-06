package edu.caltech.ipac.heritage.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.CoverageChooser;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.firefly.visualize.task.PlotFileTask;
import edu.caltech.ipac.firefly.visualize.ui.DisableablePlotDeckPanel;
import edu.caltech.ipac.heritage.commands.HeritageRequestCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.rpc.SearchServices;
import edu.caltech.ipac.heritage.rpc.SearchServicesAsync;
import edu.caltech.ipac.heritage.searches.HeritageSearch;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoveragePolygons;
import edu.caltech.ipac.visualize.plot.ImageCorners;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Date: Feb 20, 2009
 *
 * @author Trey
 * @version $Id: AorCoveragePreview.java,v 1.55 2012/10/02 22:22:57 roby Exp $
 */
public class AorCoveragePreview extends AbstractTablePreview {

    private static final String DRAWER_ID= "AorCoverage";
    private static final String LOCK_COLUMN= "reqkey";
    public enum CoverageType { X, BOX, HIDE}
    public static final String AOR_CENTER= "AOR_CENTER";
    public static final String AOR_RADIUS= "AOR_RADIUS";
    private static final WebClassProperties _prop= new WebClassProperties(AorCoveragePreview.class);
    private static final String FOOTPRINT_BASE= _prop.getTitle("footprint");

    private DrawingManager _drawer= null;
    private PreviewTimer _pvTimer= new PreviewTimer();
    private String _currentKey = "";
    private final DisableablePlotDeckPanel _plotDeck;
    private final List<DrawObj> _graphObj=  new ArrayList<DrawObj>(100);
    private TablePanel _initTable = null;
    private TablePanel _currentTable= null;
    private CoveragePolygons _currentCovArea= null;
    private boolean _catalog = false;
    private boolean _init= false;
    private boolean _initPV= false;
    private String _currTitle;
    private EventHub _hub= null;
    private Map<String,LockDataValue> _lockedImages= new HashMap<String, LockDataValue>(10);


    public AorCoveragePreview() {
        super(_prop.getName(), _prop.getTip());
        MiniPlotWidget mpw= new MiniPlotWidget();
        mpw.setImageSelection(true);
        mpw.setRemoveOldPlot(false);
        _plotDeck= new DisableablePlotDeckPanel(_prop.getName("noplot"), mpw,  true );

        setDisplay(_plotDeck);


    }


    private void initPlotViewListeners() {

        if (_initPV) return;

        _initPV= true;

        _plotDeck.getMPW().getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(PlotWidgetOps widgetOps) {
                final WebPlotView pv= widgetOps.getPlotView();
                _hub.getCatalogDisplay().addPlotView(pv);
                _hub.getDataConnectionDisplay().addPlotView(pv,
                                         Arrays.asList(HeritageRequestCmd.ACTIVE_TARGET_ID));

                pv.addListener(Name.PLOTVIEW_LOCKED, new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        TableData.Row row = _currentTable.getTable().getHighlightedRow();
                        if (pv.isLockedHint()) {
                            setImageLocked(row,true);
                        }
                        else {
                            setImageLocked(row,false);
                            recomputePlot(_currentCovArea,_currentTable);
                        }
                    }
                });
                pv.addListener(Name.PLOT_REQUEST_COMPLETED, new WebEventListener<List<WebPlot>>() {
                    public void eventNotify(WebEvent<List<WebPlot>> ev) {
                        List<WebPlot> successList= ev.getData();
                        if (successList.size()>0) {
                            TableData.Row row = _currentTable.getTable().getHighlightedRow();
                            PlotFileTask source= (PlotFileTask)ev.getSource();
                            if (!source.isThreeColor()) {
                                setLockImageRequest(row, source.getRequest(Band.NO_BAND));
                            }
                        }
                    }
                });

            }
        });


    }


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
    public void bind(EventHub hub) {
        super.bind(hub);

        _hub= hub;
        WebEventListener wel =  new WebEventListener(){
            public void eventNotify(WebEvent ev) {

                final TablePanel table = (TablePanel) ev.getSource();
                if (ev.getName().equals(EventHub.ON_TABLE_SHOW)) {
                    updateTabVisible(table);
                }
                if (GwtUtil.isOnDisplay(getDisplay())) {
                    updateCoverage(table, true);
                }
            }
        };
        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);
    }

    public boolean isInitiallyVisible() { return false; }


    private void updateTabVisible(TablePanel table) {

        if (table.getDataset()==null) return;

        TableMeta meta= table.getDataset().getMeta();
        _catalog =  meta.contains(MetaConst.CATALOG_OVERLAY_TYPE);
        DataType dType= DataType.parse(meta.getAttribute(HeritageSearch.DATA_TYPE));


        if (dType==DataType.AOR) _initTable = table;

        boolean v= (_catalog || dType==DataType.AOR || _init);
        if (_catalog && _initTable ==null && !_init) v= false;

        getEventHub().setPreviewEnabled(this, v);
    }


    private void updateCoverage(TablePanel table, boolean delay) {

//        if (!updateTabVisible(table)) return;
        if (!GwtUtil.isOnDisplay(getDisplay())) return;

        AllPlots.getInstance().setSelectedWidget(_plotDeck.getMPW());

        TableMeta meta= table.getDataset().getMeta();
        DataType dType= DataType.parse(meta.getAttribute(HeritageSearch.DATA_TYPE));

        if (dType==DataType.AOR) {
            _currentTable= table;
            updateAORFootprintCoverage(table,delay);
            _init= true;
        }
        else if (_catalog){
            if (_currentTable==null && _initTable !=null) {
                _currentTable= _initTable;
                updateAORFootprintCoverage(_initTable,delay);
                _init= true;
            }
        }
    }

    private void updateAORFootprintCoverage(TablePanel table, boolean delay) {
        TableData model= table.getDataset().getModel();
        TableData.Row row = table.getTable().getHighlightedRow();

        String key= (row==null) ? null : (String)row.getValue("reqkey");
        if (row!=null && key != null) {

            if (!_currentKey.equals(key)) {
                _currentKey = key;
                _pvTimer.cancel();
                _pvTimer.setupCall(table,key);
                _pvTimer.schedule(delay ? 500 : 1);
            }
        } // end if previewURL
        else {
            _currentKey = "";
            _plotDeck.showNoData("No row selected");
        }
    }




//    public void onResize(int width, int height) {
//        _plotDeck.getMPW().setPixelSize(width,height);
//    }

    protected void updateDisplay(TablePanel table) {
        if (table!=null) {
            _plotDeck.getMPW().recallScrollPos();
            updateCoverage(table, true);
        }
    }



    public void update(  final TablePanel table,
                         final int key) {

        CoverageTask covTask= new CoverageTask(table,key);
        covTask.start();
    }


    private void recomputePlot(CoveragePolygons covArea, TablePanel table) {

        if (covArea!=null) {
            Circle c= covArea.getCircle();

            TableData model= table.getDataset().getModel();
            TableData.Row row = table.getTable().getHighlightedRow();
            if (row!=null) {
                _currTitle= row.getValue(model.getColumnIndex("reqkey")) + "";
            }
            else {
                _currTitle= "AOR Coverage: ";
            }

            WebPlotRequest request;
            if (row!=null && isImageLocked(row)) {
                request= getLockedRequest(row);
            }
            else {
                request= new CoverageChooser().getRequest(
                        c.getCenter(), (float)(c.getRadius()), _currTitle+ ": ",
                        ZoomType.SMART);
            }

            if (getWidget()!=null) {
                int width= getWidget().getOffsetWidth()-15;
                if (width>50) {
                    request.setZoomType(ZoomType.TO_WIDTH);
                    request.setZoomToWidth(width);
                }
            }
            _plotDeck.showPlot();
            plot(covArea,table,request);
        }
        else {
            _plotDeck.showNoData();
        }
    }



//    private void recomputePlot(CoveragePolygons covArea,
//                               TablePanel table) {
//
//
//
//        WebPlotView pv= _plotDeck.getMPW().getPlotView();
//        boolean locked= (pv!=null) && pv.isLockedHint() && pv.size()>0;
//
//        if (covArea!=null) {
//            Circle c= covArea.getCircle();
//            if (locked) {
//                WebPlot plot= pv.getPrimaryPlot();
//
//                plot.setAttribute(AOR_CENTER,c.getCenter().toString());
//                plot.setAttribute(AOR_RADIUS, c.getRadius()+"");
//                replotCoverageOverlay(covArea,table);
//                _plotDeck.showPlot();
//            }
//            else {
//
//                TableData model= table.getDataset().getModel();
//                TableData.Row[] hrows = table.getTable().getHighlightRows();
//                TableData.Row row = (hrows.length>0) ? hrows[0] : null;
//                if (row!=null) {
////                    _currTitle= FOOTPRINT_BASE+ row.getValue(model.getColumnIndex("reqkey"));
//                    _currTitle= row.getValue(model.getColumnIndex("reqkey")) + "";
//                }
//                else {
//                    _currTitle= "AOR Coverage: ";
//                }
//
//                WebPlotRequest request= new CoverageChooser().getRequest(
//                        c.getCenter(), (float)(c.getRadius()), _currTitle+ ": ",
//                        ZoomType.SMART);
//                if (getWidget()!=null) {
//                    int width= getWidget().getOffsetWidth()-15;
//                    if (width>50) {
//                        request.setZoomType(ZoomType.TO_WIDTH);
//                        request.setZoomToWidth(width);
//                    }
//                }
//                _plotDeck.showPlot();
//                plot(covArea,table,request);
//            }
//        }
//        else {
//            _plotDeck.showNoData();
//        }
//    }


    private void plot(final CoveragePolygons covArea,
                      final TablePanel table,
                      WebPlotRequest request) {

        final boolean mustInitPV= !_plotDeck.getMPW().isInit();

        AsyncCallback<WebPlot> notify= new AsyncCallback<WebPlot>() {
            public void onFailure(Throwable caught) {
                _plotDeck.showNoData();
            }

            public void onSuccess(WebPlot plot) {
                initPlotViewListeners();
                Circle c= covArea.getCircle();

                plot.setAttribute(AOR_CENTER,c.getCenter().toString());
                plot.setAttribute(AOR_RADIUS, c.getRadius()+"");
                replotCoverageOverlay(covArea,table);

                TableData.Row row = table.getTable().getHighlightedRow();
                boolean lockHint= isImageLocked(row);
                if (lockHint!=plot.getPlotView().isLockedHint()) {
                    plot.getPlotView().setLockedHint(isImageLocked(row));
                }
            }
        };

        _plotDeck.getMPW().getOps().plot(request,false, notify);
    }

    private boolean isImageLocked(TableData.Row row) {
        String key= (String)row.getValue(LOCK_COLUMN);
        return isImageLocked(key);
    }

    private boolean isImageLocked(String key) {
        boolean retval = false;
        if (key!=null && _lockedImages.containsKey(key)) {
            LockDataValue v= _lockedImages.get(key);
            retval= v.locked && (v.request!=null);
        }
        return retval;
    }

    private WebPlotRequest getLockedRequest(TableData.Row row) {
        String key= (String)row.getValue(LOCK_COLUMN);
        return getLockedRequest(key);
    }

    private WebPlotRequest getLockedRequest(String key) {
        WebPlotRequest retval = null;
        if (key!=null && _lockedImages.containsKey(key)) {
            LockDataValue v= _lockedImages.get(key);
            if (v.locked)  retval= v.request;
        }
        return retval;
    }


    private void setImageLocked(TableData.Row row, boolean locked) {
        String key= (String)row.getValue(LOCK_COLUMN);
        setImageLocked(key,locked);
    }

    private void setImageLocked(String key, boolean locked) {
        if (key!=null) {
            LockDataValue v;
            if (_lockedImages.containsKey(key)) {
                v= _lockedImages.get(key);
                v.locked= locked;
            }
            else {
                v= new LockDataValue();
                v.locked= locked;
                _lockedImages.put(key,v);
            }
            processLockRequest(v);
        }
    }

    private void setLockImageRequest(TableData.Row row, WebPlotRequest request) {
        String key= (String)row.getValue(LOCK_COLUMN);
        setLockImageRequest(key,request);
    }

    private void setLockImageRequest(String key, WebPlotRequest request) {
        if (key!=null) {
            LockDataValue v;
            if (_lockedImages.containsKey(key)) {
                v= _lockedImages.get(key);
                v.request= request;
            }
            else {
                v= new LockDataValue();
                v.request= request;
                _lockedImages.put(key,v);
            }
            processLockRequest(v);
        }
    }


    private void processLockRequest(LockDataValue v) {
        if (v.locked && v.request!=null) {
           // save it here
        }
        else {
            // remove it here
        }

    }



    private void replotCoverageOverlay(CoveragePolygons covArea,
                                       TablePanel table) {

        if (_drawer==null) {
            _drawer= new DrawingManager(DRAWER_ID, new DetailData(covArea));
            _drawer.setDefaultColor(AutoColor.DRAW_1);
        }
        if (!_drawer.containsPlotView(_plotDeck.getMPW().getPlotView())) {
            _drawer.addPlotView(_plotDeck.getMPW().getPlotView());
        }

        _drawer.setDataConnection(new DetailData(covArea), true);

    }


    public class DetailData extends SimpleDataConnection {

        private final CoveragePolygons _covArea;

        DetailData(CoveragePolygons covArea) {
            super("Aor Coverage", "", "red");
            _covArea= covArea;
        }

        @Override
        public String getTitle(WebPlot plot) { return _currTitle; }

        public List<DrawObj> getData(boolean rebuild, WebPlot p) {

            if (rebuild) {
                _graphObj.clear();

                List<ImageCorners> corners= _covArea.getCorners();
                List<WorldPt []> ptList= new ArrayList<WorldPt []>(corners.size());

                for(ImageCorners cornerGroup : corners) {
                    List<WorldPt> c= cornerGroup.getCorners();
                    ptList.add(c.toArray(new WorldPt[c.size()]));
                }
                _graphObj.add(new FootprintObj(ptList));

            }
            return _graphObj;
        }

        public boolean isActive() { return true; }
    }


    private class PreviewTimer extends Timer {
        private String _key;
        private TablePanel _table;

        public void run() {
            int keyInt= Integer.parseInt(_key);
            update(_table, keyInt);
        }

        public void setupCall(TablePanel table,
                              String key) {
            _key= key;
            _table= table;
        }
    }

    private class CoverageTask extends ServerTask<CoveragePolygons> {

        private final int _key;
        private final TablePanel _table;

        public CoverageTask(TablePanel table,
                            int key) {
            super(_plotDeck.getMPW().getMaskWidget(),
                  _prop.getName("loadCoverage"), true);
            _key= key;
            _table= table;

        }

        public void onSuccess(CoveragePolygons covArea) {
            _currentCovArea= covArea;
            recomputePlot(covArea,_table);
        }

        @Override
        protected void onFailure(Throwable caught) {
            _plotDeck.showNoData();
        }


        public void doTask(AsyncCallback passAlong) {
            SearchServicesAsync pserv= SearchServices.App.getInstance();
            pserv.getAorCoverage(_key, passAlong);
        }
    }


    private class LockDataValue {
        boolean locked= false;
        WebPlotRequest request= null;
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
