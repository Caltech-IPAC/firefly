/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.fftools;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.data.ServerParams;
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
    private static int xyIdCnt=0;

    public static void plotTable(JscriptRequest jspr, String div) {
        plotTable(jspr.asMap(), div);
    }

    public static void plotTable(Map<String,String> params, String div) {

        final XYPlotWidget xyPlotWidget = getXYPlotWidget(params);
        if (div==null) {
            String id="xyplot" + xyIdCnt;
            xyIdCnt++;
            final SimplePanel panel = makeCenter();
            panel.add(xyPlotWidget);
            FFToolEnv.addToPanel(id,panel,"XY Plot");
        } else {
            FFToolEnv.addToPanel(div, xyPlotWidget, "XY Plot");
        }
        loadData(xyPlotWidget, params);
    }

    public static XYPlotWidget getXYPlotWidget(Map<String,String> params) {
        Map<String,String> plotParams = new HashMap<String,String>();
        for (String key : params.keySet()) {
            if (CustomMetaSource.isValidParam(key)) {
                plotParams.put(key, params.get(key));
            }
        }
        String plotTitle = params.get("plotTitle");
        if (plotTitle == null) plotTitle = "none";
        String maxPointsStr = params.get("maxPoints");

        int plotSizeX = 600, plotSizeY= 400;
        XYPlotMeta meta = new XYPlotMeta(plotTitle, plotSizeX, plotSizeY, new CustomMetaSource(plotParams));
        if (maxPointsStr != null) {
            try {
                int maxPoints = Integer.parseInt(maxPointsStr);
                meta.setMaxPoints(maxPoints);
            } catch (Exception ignored) {}
        }
        XYPlotWidget xyPlotWidget = new XYPlotWidget(meta);
        xyPlotWidget.setTitleAreaAlwaysHidden(true);
        return xyPlotWidget;
    }

    public static void loadData(final XYPlotWidget xyPlotWidget, final Map<String,String> params) {
        final String chartTitle = StringUtils.isEmpty(params.get("chartTitle")) ? "XY Plot" : params.get("chartTitle");

        BaseTableConfig<TableServerRequest> config =
                new BaseTableConfig<TableServerRequest>(convertToRequest(params, 0), "XY plot from source", chartTitle);
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

    public static TableServerRequest convertToRequest(Map<String,String> params, int pageSize) {
        TableServerRequest sreq = null;
        if (params.containsKey("source")) {
            String fileOrUrl = params.get("source").trim();
            if (fileOrUrl.charAt(0) != '/' && fileOrUrl.charAt(0) != '$') {
                fileOrUrl =  FFToolEnv.modifyURLToFull(fileOrUrl);
            }
            sreq = new TableServerRequest("IpacTableFromSource");
            sreq.setParam(ServerParams.SOURCE, fileOrUrl);
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

        SimplePanel panel= makeCenter();
        panel.add(xyPrev.getDisplay());
        FFToolEnv.addToPanel(div, panel, "XY Plot");

        if (xyPrev.getDisplay() instanceof RequiresResize) {
            final RequiresResize resizer= (RequiresResize)xyPrev.getDisplay();
            Window.addResizeHandler(new ResizeHandler() {
                public void onResize(ResizeEvent event) {
                    resizer.onResize();
                }
            });
        }
    }

    protected static SimplePanel makeCenter() {
        final SimplePanel center = new SimplePanel();
        center.setSize("100%", "100%");
        return center;
    }

}

