/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.*;
import edu.caltech.ipac.firefly.visualize.graph.SpectrumMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
import edu.caltech.ipac.firefly.visualize.ui.DisableablePlotDeckPanel;
import edu.caltech.ipac.firefly.visualize.ui.NeedsHub;

import java.util.List;
import java.util.Map;

/**
 * Date: Feb 20, 2009
 *
 * @author Trey
 * @version $Id: DataViewerPreview.java,v 1.41 2012/10/11 22:23:56 tatianag Exp $
 */
public class DataViewerPreview extends AbstractTablePreview {

    public static final String NOT_AVAILABLE_MESS=  "FITS image or spectrum is not available";
    public static final String NO_ACCESS_MESS=  "You do not have access to this data";
    public static final String NO_PREVIEW_MESS=  "No Preview";



    private PreviewTimer _pvTimer= new PreviewTimer();
    private Map<Band,WebPlotRequest> _currentReqMap= null;
    private final DeckWidget _plotDeck;
    private TableCtx _table = null;
    private boolean _catalog = false;
    private boolean _init = false;
    private boolean _initPV = false;
    private final PreviewData _previewData;
    private EventHub _hub;
    private boolean _showing= true;


    public DataViewerPreview(PreviewData previewData) {
        super(previewData.getTabTitle(), previewData.getTip());
        _previewData= previewData;
        _plotDeck = new DeckWidget(previewData.isThreeColor(),previewData.getGroup());
        setDisplay(_plotDeck);
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);
        
        _hub= hub;
        WebEventListener wel =  new WebEventListener(){
            public void eventNotify(WebEvent ev) {

                TableCtx tableCtx;
                if (ev.getSource() instanceof TablePanel) {
                    TablePanel table = (TablePanel) ev.getSource();
                    tableCtx= table!=null ? new TableCtx(table) : null;
                    if (updateTabVisible(tableCtx))  updateImage(tableCtx,true);

                }
                if (ev.getSource() instanceof TableCtx) {
                    tableCtx= (TableCtx)ev.getSource();
                    if (updateTabVisible(tableCtx))  updateImage(tableCtx,true);
                }
            }
        };
        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);


        if (_previewData.getExtraPanels()!=null) {
            PlotRelatedPanel ppAry[]=  _previewData.getExtraPanels();
            if (ppAry!=null) {
                for(PlotRelatedPanel pp : ppAry) {
                    if (pp instanceof NeedsHub) {
                        ((NeedsHub)pp).bind(hub);
                    }
                }

            }
        }
    }

    private void initPlotView(WebPlotView pv) {
        if (_hub!=null && !_initPV) {
            _initPV= true;
            _hub.getCatalogDisplay().addPlotView(pv);
            if (_previewData.getEventWorkerList()!=null) {
                _hub.getDataConnectionDisplay().addPlotView(pv, _previewData.getEventWorkerList());
            }
        }
    }


    public boolean isInitiallyVisible() { return false; }

    private boolean updateTabVisible(TableCtx table) {

        boolean show= false;
        if (table!=null && table.hasData()) {
            _catalog =  table.getMeta().containsKey(MetaConst.CATALOG_OVERLAY_TYPE);

            boolean results= _previewData.getHasPreviewData(table.getId(), table.getColumns(), table.getMeta());
            if (results) _table = table;

            show= (_catalog || results || _init);
            if (_catalog && !_init) show= false;

            getEventHub().setPreviewEnabled(this,show);
        }


        return show;
    }

    private void updateImage(TableCtx table, final boolean delay) {
        MiniPlotWidget mpw= _plotDeck.getMPW();
        boolean onDisplay= GwtUtil.isOnDisplay(  getDisplay());

        if( (_showing && onDisplay)  || (mpw!=null && mpw.isExpanded())) {
            String id= null;
            Map<String,String> ma= null;
            List<String> colNames= null;
            if (table!=null) {
                id= table.getId();
                ma= table.getMeta();
                colNames= table.getColumns();
            }
            if(!_catalog && table!=null && table.hasData() &&
                    _previewData.getHasPreviewData(id, colNames, ma)) {
                updatePreview(table, delay);
                _init = true;
            }
            else if(_catalog)  {
                if (!_init && _table != null) {
                    updatePreview(_table, delay);
                    _init = true;
                }
            }
        }


    }


    private void updatePreview(TableCtx tableCtx, boolean delay) {
        updatePreview(tableCtx.getRow(),tableCtx.getMeta(),tableCtx.getColumns(),delay);

    }


    private void updatePreview(TableData.Row<String> row,
                               Map<String,String> meta,
                               List<String> columns,
                               boolean delay) {
        if (row!=null) {
            if (row.hasAccess()) {
                PreviewData.Info info= _previewData.createRequestForRow(row, meta, columns);
                if (info!=null) {
                    Map<Band,WebPlotRequest> reqMap= info.getRequestMap();
                    int width= (getWidget()!=null) ? getWidget().getOffsetWidth()-15 : 0;
                    MiniPlotWidget mpw= _plotDeck.getMPW();
                    if (mpw!=null && mpw.isExpanded() && mpw.getMovablePanel()!=null) {
                        width= mpw.getMovablePanel().getOffsetWidth();
                    }
                    for(WebPlotRequest req : reqMap.values()) {
                        if (req.getZoomType()== ZoomType.TO_WIDTH  ) {
                            if (width>50)  req.setZoomToWidth(width);
                            else  req.setZoomType(ZoomType.SMART);
                        }
                    }
                    if (_currentReqMap==null || !_currentReqMap.equals(reqMap)) {
                        _currentReqMap= reqMap;
                        _pvTimer.cancel();
                        _pvTimer.setupCall(reqMap, info.getType(), _plotDeck);
                        _pvTimer.schedule(delay ? 100 : 1);
                    }
                    else {
//                        AllPlots.getInstance().setSelectedWidget(mpw);
                    }
                }
                else {
                    //showNoPreview(NO_PREVIEW_MESS);
                    showNoPreview(null);
                }
            }
            else {
                showNoPreview(NO_ACCESS_MESS);
            }

        } // end if previewURL
        else {
            showNoPreview(NO_PREVIEW_MESS);
        }

    }



    public void showNoPreview(String mess) {
        _currentReqMap= null;
        _plotDeck.showNoData(mess);
    }

    @Override
    public void onShow() {
        _showing= true;
        if (_plotDeck.getMPW()!=null) {
            _plotDeck.getMPW().setActive(true);
            AllPlots.getInstance().setSelectedMPW(_plotDeck.getMPW());
        }
        if (_plotDeck.getMPW().isInit()) {
            _plotDeck.getMPW().getOps(new MiniPlotWidget.OpsAsync() {
                public void ops(PlotWidgetOps widgetOps) {
                    final WebPlotView pv= widgetOps.getPlotView();
                    if (pv!=null && pv.size()>0 && pv.getPrimaryPlot()==null) {
                        pv.setPrimaryPlot(pv.get(0));
                    }
                    DeferredCommand.addCommand(new Command() {
                        public void execute() {
                            if (pv!=null) pv.notifyWidgetShowing();
                        }
                    });
                }
            });
        }
        super.onShow();

    }

    @Override
    public void onHide() {
        _showing= false;
        if (_plotDeck.getMPW()!=null) _plotDeck.getMPW().setActive(false);
        super.onHide();
    }

    protected void updateDisplay(TablePanel table) {
        if (table!=null) {
            _plotDeck.getMPW().recallScrollPos();
            updateImage(new TableCtx(table), false);
        }
    }



    private static class PreviewTimer extends Timer {
        Map<Band,WebPlotRequest> _reqMap;
        private DeckWidget _deck;
        private PreviewData.Type _type;

        public void run() { _deck.updateDeck(_reqMap, _type);  }

        public void setupCall(Map<Band,WebPlotRequest> reqMap,
                              PreviewData.Type type,
                              DeckWidget deck) {
            _reqMap = reqMap;
            _type = type;
            _deck= deck;
        }
    }

    private MiniPlotWidget makeMPW(boolean threeColor, String group) {
        MiniPlotWidget  retval= new MiniPlotWidget(group);
        retval.setWorkingMsg("Retrieving Image...");
        retval.setImageSelection(threeColor);
        retval.setLockImage(false);

        if (_previewData.getMinWidth()>0 &&_previewData.getMinHeight()>0) {
            retval.setMinSize(_previewData.getMinWidth(), _previewData.getMinHeight());
        }
        if (_previewData.getExtraPanels()!=null) {
            PlotRelatedPanel pp[]=  _previewData.getExtraPanels();
            if (pp!=null)
            retval.getGroup().setExtraPanels(_previewData.getExtraPanels());
        }
        return retval;
    }


    private class DeckWidget extends DisableablePlotDeckPanel {
        private XYPlotWidget _xyPlotWidget;
        private XYPlotMeta _xyPlotMeta;
        private int _xyIdx=-1;

        public DeckWidget(boolean threeColor, String group) {
            super(NOT_AVAILABLE_MESS,   makeMPW(threeColor,group), _previewData.getPlotFailShowPrevious());
        }

        public void updateDeck(final Map<Band,WebPlotRequest> requestMap, PreviewData.Type type) {
            if (type == null) {
                AllPlots.getInstance().setStatus(_xyPlotWidget, AllPlots.PopoutStatus.Disabled);
                AllPlots.getInstance().setStatus(getMPW(), AllPlots.PopoutStatus.Disabled);
                showNoData();
            } else if (type== PreviewData.Type.FITS) {
                Vis.init(new Vis.InitComplete() {
                    public void done() { plotFits(getMPW(), requestMap); }
                });
            } else if (type== PreviewData.Type.SPECTRUM) {
                GWT.runAsync(new GwtUtil.DefAsync() {
                    @Override
                    public void onSuccess() {
                        spectrumAsync(requestMap);
                    }
                });
            } else {
                showNoData();
            }

        }

        private void spectrumAsync(Map<Band,WebPlotRequest> requestMap) {
            if (_xyIdx==-1) {
                _xyPlotMeta = new XYPlotMeta(null, 150, 90, SpectrumMetaSource.getInstance());
                _xyPlotWidget= new XYPlotWidget(_xyPlotMeta);
                AllPlots.getInstance().registerPopout(_xyPlotWidget);
                add(_xyPlotWidget);
                _xyIdx = getWidgetIndex(_xyPlotWidget);
            }
            showWidget(_xyIdx);
            WebPlotRequest wpreq = requestMap.get(Band.NO_BAND);
            TableServerRequest dataReq = getTableServerRequest(wpreq);
            String title = wpreq.getTitle();
            BaseTableConfig<TableServerRequest> config =
                    new BaseTableConfig<TableServerRequest>(dataReq, "XY plot from source", title);
            DataSetTableModel tableModel = new DataSetTableModel(config.getLoader());
            _xyPlotWidget.makeNewChart(tableModel, title);
            AllPlots.getInstance().setStatus(_xyPlotWidget, AllPlots.PopoutStatus.Enabled);
            AllPlots.getInstance().setStatus(getMPW(), AllPlots.PopoutStatus.Disabled);
       }

        private TableServerRequest getTableServerRequest(WebPlotRequest request) {
            RequestType type = request.getRequestType();
            TableServerRequest dataReq = null;
            if (type.equals(RequestType.FILE)) {
                dataReq = new TableServerRequest("IpacTableFromSource");
                dataReq.setParam("source", request.getFileName());
            } else if (type.equals(RequestType.URL)) {
                dataReq = new TableServerRequest("IpacTableFromSource");
                dataReq.setParam("source", request.getURL());
            } else if (type.equals(RequestType.PROCESSOR)) {
                dataReq = new TableServerRequest("IpacTableFromSource", request);
                dataReq.setParam("processor", request.getRequestId());
            }
            return dataReq;
        }

        private void plotFits(MiniPlotWidget mpw,  Map<Band,WebPlotRequest> requestMap) {
            Vis.assertInitialized();
            AllPlots.getInstance().setStatus(_xyPlotWidget, AllPlots.PopoutStatus.Disabled);
            AllPlots.getInstance().setStatus(getMPW(), AllPlots.PopoutStatus.Enabled);
            mpw.setSaveImageCornersAfterPlot(_previewData.getSaveImageCorners());
            showPlot();
            _previewData.prePlot(mpw, _table.getMeta());
            if (requestMap.containsKey(Band.NO_BAND)) {
                mpw.getOps().plot(requestMap.get(Band.NO_BAND),false, new AsyncCallback<WebPlot>() {
                            public void onFailure(Throwable caught) { }
                            public void onSuccess(WebPlot plot) { plotSuccess(plot); }
                        });
            }
            else {
                mpw.getOps().plot3Color(requestMap.get(Band.RED),
                                             requestMap.get(Band.GREEN),
                                             requestMap.get(Band.BLUE),
                                             false, new AsyncCallback<WebPlot>() {
                            public void onFailure(Throwable caught) { }
                            public void onSuccess(WebPlot plot) { plotSuccess( plot); }
                        });
            }
        }

        private void plotSuccess(WebPlot plot) {
            initPlotView(plot.getPlotView());
            _previewData.postPlot(getMPW(), plot);
        }

        public void removeCurrent() {
            getMPW().getOps().removeCurrentPlot();
            _xyPlotWidget.removeCurrentChart();
        }

    }


}
