package edu.caltech.ipac.firefly.fftools;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.visualize.graph.CustomMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
import edu.caltech.ipac.util.StringUtils;


import java.util.HashMap;
import java.util.Map;

/**
 * @author tatianag
 */
public class XYPlotJSInterface {
    public static void plotTable(JscriptRequest jspr, String div) {
        int plotSizeX = 190, plotSizeY= 300;
        Map<String,String> params = new HashMap<String,String>();
        // extract meta parameters from JScriptRequest
        Map<String, String> jsprParams = jspr.asMap();
        for (String key : jsprParams.keySet()) {
            if (CustomMetaSource.isValidParam(key)) {
                params.put(key,jsprParams.get(key));
            }
        }
        String plotTitle = jspr.getParam("plotTitle");
        String maxPointsStr = jspr.getParam("maxPoints");
        final String chartTitle = StringUtils.isEmpty(jspr.getParam("chartTitle")) ? "Sample Chart" : jspr.getParam("chartTitle");

        int maxPoints;
        try {
            maxPoints = Integer.parseInt(maxPointsStr);
        } catch (Exception e) {
            maxPoints = 1000;
        }
        XYPlotMeta meta = new XYPlotMeta(plotTitle, plotSizeX, plotSizeY, new CustomMetaSource(params));
        meta.setMaxPoints(maxPoints);
        final XYPlotWidget xyPlotWidget = new XYPlotWidget(meta);
        RootPanel rp= FFToolEnv.getRootPanel(div);
        if (rp == null) {
            rp= FFToolEnv.getRootPanel(null);
        }
        rp.add(xyPlotWidget);

        BaseTableConfig<TableServerRequest> config =
                new BaseTableConfig<TableServerRequest>(convertToRequest(jspr, 0), "XY plot from source", chartTitle);
        final DataSetTableModel tableModel = new DataSetTableModel(config.getLoader());

        tableModel.getData(new AsyncCallback<TableDataView>() {
            public void onFailure(Throwable throwable) {
                //TODO: something on error
                Window.alert("Unable to get table data: "+throwable.getMessage());
            }

            public void onSuccess(TableDataView tableDataView) {
                xyPlotWidget.makeNewChart(tableModel, chartTitle);
            }
        }, 0);
    }


    private static TableServerRequest convertToRequest(JscriptRequest jspr, int pageSize) {
        TableServerRequest sreq = null;
        if (jspr.containsKey("source")) {
            String url =  FFToolEnv.modifyURLToFull(jspr.getParam("source"));
            sreq = new TableServerRequest("IpacTableFromSource");
            sreq.setParam("source", url);
            sreq.setStartIndex(0);
            sreq.setPageSize(pageSize);
            sreq.setParam("rtime", String.valueOf(System.currentTimeMillis()));
        }
        if (sreq == null) {
            Window.alert("Missing parameter: source");
        }

        return sreq;
    }

    public static void addXYPlot(JscriptRequest jspr, String div) {
        Map<String,String> paramMap= jspr.asMap();
        WidgetFactory factory= Application.getInstance().getWidgetFactory();
        TablePreview xyPrev= factory.createObserverUI(WidgetFactory.XYPLOT,paramMap);
        xyPrev.bind(FFToolEnv.getHub());

        RootPanel root= FFToolEnv.getRootPanel(div);
        if (root!=null) {
            SimplePanel panel= makeCenter();
            root.add(panel);
            panel.add(xyPrev.getDisplay());
        }
        else {
            FFToolEnv.logDebugMsg("Could not find div: " + div + ", XYPlot was not added");
        }

    }

    protected static SimplePanel makeCenter() {
        final SimplePanel center = new SimplePanel();
        center.setSize("100%", "100%");
        return center;
    }

}

