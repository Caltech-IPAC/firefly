package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
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
import edu.caltech.ipac.firefly.visualize.draw.DataConnection;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.TabularDrawingManager;
import edu.caltech.ipac.firefly.visualize.ui.DisableablePlotDeckPanel;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.Pt;
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
 * @version $Id: DataSourceCoveragePreview.java,v 1.10 2012/11/30 23:17:01 roby Exp $
 */
public class DataSourceCoveragePreview extends AbstractTablePreview {

    private static final WebClassProperties _prop= new WebClassProperties(DataSourceCoveragePreview.class);

    private static final String COVERAGE_TARGET = "COVERAGE_TARGET";
    public static final String COVERAGE_RADIUS = "COVERAGE_RADIUS";

    private final DisableablePlotDeckPanel _plotDeck;

    private HTML _details = new HTML();
    private ScrollPanel _detailsView = null;
    private boolean _initPV= false;
    private boolean _externalReplot= true;
    private final DataSourceCoverageData _covData;
    private TablePreviewEventHub _hub;
    private Map<String,DrawData> _plottedData= new HashMap<String, DrawData>(3);
    private PlotInfo _info= new PlotInfo();


    public DataSourceCoveragePreview(DataSourceCoverageData covData) {
        super(covData.getTitle(),covData.getTip());
        _covData= covData;
        setName(covData.getTitle());
        String group= covData.getGroup();
        MiniPlotWidget mpw=   new MiniPlotWidget(group);
        mpw.setImageSelection(true);
        mpw.setRemoveOldPlot(false);
        _plotDeck= new DisableablePlotDeckPanel(_prop.getName("noplot"), mpw,true);
        if (covData.getEnableDetails()) {
            _detailsView = new ScrollPanel(_details);
            SplitLayoutPanel display = new SplitLayoutPanel();
            display.addSouth(_detailsView, 120);
            GwtUtil.setStyle(_detailsView,"borderTop", "1px solid gray");
            GwtUtil.setStyle(_detailsView,"paddingTop", "3px");
            display.add(_plotDeck);
            setDisplay(display);
        }
        else {
            setDisplay(_plotDeck);
        }

    }




    private void initPlotViewListeners() {

        if (_initPV) return;

        _initPV= true;

        _plotDeck.getMPW().getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(PlotWidgetOps widgetOps) {
                final WebPlotView pv= widgetOps.getPlotView();

                assert (_hub!=null);
                _hub.getCatalogDisplay().addPlotView(pv);
                pv.addListener(Name.PRIMARY_PLOT_CHANGE, new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        if (_plottedData.size()>0 && _externalReplot) {
                            _info.setPlot(pv.getPrimaryPlot());
                        }
                    }
                });



                pv.addListener(Name.PLOTVIEW_LOCKED, new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        if (!pv.isLockedHint() && _plottedData.size()>0) {
                            updateCoverage();
                        }
                    }
                });

            }
        });

    }

    @Override
    protected void updateDisplay(TablePanel table) { }

    @Override
    public void bind(TablePreviewEventHub hub) {
        super.bind(hub);

        _hub = hub;
        WebEventListener wel =  new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                Name evName= ev.getName();
                Object data= ev.getData();
                Object source= ev.getSource();

                if (source instanceof EventWorker &&
                    (data==null || data instanceof DataConnection || data instanceof DataSet) &&
                    evName.equals(TablePreviewEventHub.ON_EVENT_WORKER_COMPLETE)    ) {
                    EventWorker    ew= (EventWorker)source;

                    if (_covData.getEventWorkerList().contains(ew.getID()) ) {
                        if (data != null && data instanceof DataSet) {
                            updateDetails((DataSet)data);
                        } else {
                            DataConnection dc= (DataConnection)data;
                            if (dc==null || dc.size()==0) {
                                _plottedData.remove(ew.getID());
                            }
                            else {
                                _plottedData.put(ew.getID(),new DrawData(ew.getID(), null,dc));
                            }
                            updateArea();
                            updateCoverage();
                            updateArea();
                        }
                    }
                }
            }
        };
        hub.getEventManager().addListener(TablePreviewEventHub.ON_EVENT_WORKER_COMPLETE, wel);
    }

    private void updateDetails(DataSet ds) {
         List<TableDataView.Column> columns = ds.getColumns();
         TableData data = ds.getModel();
         StringBuilder sb = new StringBuilder();
         if (data.size() > 0) {
             TableData.Row row = data.getRow(0);
             for (TableDataView.Column c : columns) {
                 sb.append(makeEntry(c.getTitle(), row.getValue(c.getName())));
             }
         }
         _details.setHTML(sb.toString());
     }

     private String makeEntry(String key, Object val) {
         return "&nbsp;&nbsp;<font color='darkBlue'>" + key + ": </font>" + String.valueOf(val) + "<br>";
     }


    private void updateArea() {
//        PlotInfo info= getInfo(dc);
        VisUtil.CentralPointRetval val=  calculateCentralPoint(_info);

        if (val!=null) _info.setCircle(val.getWorldPt(), val.getRadius());
        else           _info.setCircle(null,0);
    }

//    private void updatePanelVisible() {
//        getEventHub().setPreviewEnabled(this,true);
//    }

    private void updateCoverage() {

        if (!GwtUtil.isOnDisplay(getDisplay()) ) return;

        updateCoverageObsCoverage();
    }

    private void updateCoverageObsCoverage() {
        _plotDeck.getMPW().getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(PlotWidgetOps widgetOps) {
                updateCoverageObsCoverageAsync(widgetOps);
            }
        });

    }



    private void updateCoverageObsCoverageAsync(PlotWidgetOps ops) {

        WorldPt plottedCenter;
        Double plottedRadius;
        boolean replotBoth= false;

        WebPlot plot= ops.getCurrentPlot();
        WebPlotView pv= ops.getPlotView();

        if (plot!=null) {

            WebPlot lastPlot= _info.getPlot();
            if (plot!= lastPlot && pv.contains(lastPlot)) {
                plot= lastPlot;
                pv.setPrimaryPlot(plot);
                replotBoth= false;
                _plotDeck.getMPW().setTitle(plot.getPlotDesc());
            }

            plottedCenter= (WorldPt)plot.getAttribute(COVERAGE_TARGET);
            plottedRadius= (Double)plot.getAttribute(COVERAGE_RADIUS);
            if (!ComparisonUtil.equals(_info.getCenter(),plottedCenter) ||
                _info.getRadius()!=plottedRadius ||
                !_plotDeck.isPlotShowing()) {
                     replotBoth= true;
            }
        }
        else {
            replotBoth= true;
        }

        boolean locked= (pv!=null) && pv.isLockedHint() && pv.size()>0;

        if (_plottedData.size()>0) {
            if (replotBoth && !locked) {
                replotCoverageImage();
            }
            else {
                _plotDeck.showPlot();
                replotCoverageCatalog();
            }
        }
        else {
            _plotDeck.showNoData(_prop.getName("nocov"));
        }


    }

    @Override
    public void onShow() {
        super.onShow();
        if (_plotDeck.getMPW()!=null) {
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    updateDisplay();
                    _plotDeck.getMPW().notifyWidgetShowing();
                }
            });
        }
    }

    public void onHide() {
//        if (_drawer!=null) _drawer.setUpdateEventEnabled(false);
    }

    protected void updateDisplay() {
        if (_plottedData.size()>0) {
            _plotDeck.getMPW().recallScrollPos();
            AllPlots.getInstance().setSelectedWidget(_plotDeck.getMPW());
            updateCoverage();
        }
    }


    private void replotCoverageCatalog() {
        for(DrawData dd : _plottedData.values()) {
            boolean firstTime= false;
            TabularDrawingManager drawer= dd.getDrawer();
            if (drawer==null) {
                firstTime= true;
                drawer= new TabularDrawingManager(dd.getId(),  dd.getDataConnection(),null);
                dd.setDrawer(drawer);
            }

            if (!drawer.containsPlotView(_plotDeck.getMPW().getPlotView())) {
                drawer.addPlotView(_plotDeck.getMPW().getPlotView());
            }

            if (firstTime) {
                drawer.setDataConnection(dd.getDataConnection());
            }
            drawer.redraw();
        }
    }

    private void replotCoverageImage() {
        double radiusD= _info.getRadius();

        if (Double.isInfinite(radiusD) || Double.isNaN(radiusD) || radiusD==0F) {
            _plotDeck.showNoData(_prop.getName("cantCompute"));
        }
        else {
            WorldPt wp= _info.getCenter();

            String base= _covData.getCoverageBaseTitle();
            Widget w= getWidget();
            int width= (w!=null) ? w.getOffsetWidth()-15 : 40;

            WebPlotRequest request= new CoverageChooser().getRequest(wp,(float)radiusD,base+" ",
                                                                     _covData.getSmartZoomHint(),
                                                                     _covData.getUseBlankPlot(),
                                                                     width);
            if (w!=null && width>50) {
                request.setZoomType(ZoomType.TO_WIDTH);
                request.setZoomToWidth(width);
            }
            _plotDeck.showPlot();
            if (!request.equals(_info.getActivePlottingRequest())) {
                _info.setActivePlottingRequest(request);
                plot(request);
            }
        }
    }


    private void plot(final WebPlotRequest request) {


        AsyncCallback<WebPlot> notify= new AsyncCallback<WebPlot>() {
            public void onFailure(Throwable caught) {
                _externalReplot= true;
                _plotDeck.showNoData();
            }

            public void onSuccess(WebPlot plot) {
                boolean allSky= (request.getRequestType()== RequestType.ALL_SKY);
                initPlotViewListeners();

                MiniPlotWidget mpw= _plotDeck.getMPW();
                if (mpw.contains(_info.getPlot())) {
                    mpw.getOps().removePlot(_info.getPlot());
                }

                _info.setPlot(plot);
                _info.setTitle(request.getTitle());
                _info.setAllSky(allSky);
                _info.setActivePlottingRequest(null);

                plot.setAttribute(COVERAGE_TARGET,_info.getCenter());
                plot.setAttribute(COVERAGE_RADIUS, _info.getRadius());
                replotCoverageCatalog();
                _externalReplot= true;
            }
        };

        _externalReplot= false;
        _plotDeck.getMPW().getOps().plot(request,false, notify);
    }




    private VisUtil.CentralPointRetval calculateCentralPoint(PlotInfo info) {
        WebPlot plot= info.getPlot();
        VisUtil.CentralPointRetval retval= null;
        ArrayList<WorldPt> wpList ;

        List<DrawObj> list= new ArrayList<DrawObj>(2000);
        for(DrawData dd : _plottedData.values()) {
            if (dd.getDataConnection()!=null) {
                DataConnection connect= dd.getDataConnection();
                if (connect.getHasPerPlotData()) {
                    list.addAll(connect.getData(false,plot));
                }
                else {
                    list.addAll(connect.getData(false));
                }
            }
        }


        if (list.size() > 0) {
            Pt pt;
            wpList  = new ArrayList<WorldPt>(list.size());
            for(DrawObj obj : list) {
                if (obj instanceof FootprintObj) {
                    FootprintObj fpObj= (FootprintObj)obj;
                    List<WorldPt []> allFP= fpObj.getPos();
                    if (allFP!=null) {
                        for (WorldPt [] wpAry : allFP) {
                            wpList.addAll(Arrays.asList(wpAry));
                        }
                    }
                }
                else {
                    pt= obj.getCenterPt();
                    if (pt instanceof  WorldPt) {
                        wpList.add((WorldPt)pt);
                    }

                }
            }

            if (CoveragePreview.isOnePoint(wpList)) {
                retval= new VisUtil.CentralPointRetval(wpList.get(0), .05D);
            }
            else {
                retval = VisUtil.computeCentralPointAndRadius(wpList);
            }
        }
        return retval;
    }


//    public PlotInfo getInfo(DataConnection dc)  {
//        PlotInfo retval;
//        if (_activeTables.containsKey(dc)) {
//            retval= _activeTables.get(dc);
//        }
//        else {
//            retval= new PlotInfo();
//            _activeTables.put(dc,retval);
//            updateArea(dc);
//
//        }
//        return retval;

//    }


    public static class DrawData {
        private TabularDrawingManager drawer;
        private DataConnection        dc;
        private String                id;

        public DrawData(String id, TabularDrawingManager drawer, DataConnection dc) {
            this.drawer = drawer;
            this.id= id;
            this.dc = dc;
        }

        public TabularDrawingManager getDrawer() { return drawer; }

        public DataConnection getDataConnection() { return dc; }
        public String getId() { return id; }

        public void setDrawer(TabularDrawingManager drawer) {
            this.drawer = drawer;
        }

    }


    public static class PlotInfo {
        private WebPlotRequest _activeRequest;
        private WebPlot _plot= null;
        private String _title= null;
        private double  _radius= 0F;
        private WorldPt _center= null;
        private boolean _allSky= false;

        public PlotInfo() {  }


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
