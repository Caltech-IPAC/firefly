package edu.caltech.ipac.firefly.fftools;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.graph.CustomMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
//import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author tatianag
 *         $Id: XYPlotJSInterface.java,v 1.1 2013/01/07 21:32:28 tatianag Exp $
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
        String chartTitle = jspr.getParam("chartTitle");
        int maxPoints;
        try {
            maxPoints = Integer.parseInt(maxPointsStr);
        } catch (Exception e) {
            maxPoints = 1000;
        }
        XYPlotMeta meta = new XYPlotMeta(plotTitle, plotSizeX, plotSizeY, new CustomMetaSource(params));
        XYPlotWidget xyPlotWidget = new XYPlotWidget(meta);
        RootPanel rp= FFToolEnv.getRootPanel(div);
        if (rp == null) {
            rp= FFToolEnv.getRootPanel(null);
        }
        rp.add(xyPlotWidget);
        xyPlotWidget.makeNewChart(convertToRequest(jspr, maxPoints), chartTitle);

    }


    private static WebPlotRequest convertToRequest(JscriptRequest jspr, int pageSize) {
        WebPlotRequest wpr = null;

        if (jspr.containsKey("source")) {
            String url =  FFToolEnv.modifyURLToFull(jspr.getParam("source"));
            TableServerRequest sreq = new TableServerRequest("IpacTableFromSource");
            sreq.setParam("source", url);
            sreq.setStartIndex(0);
            sreq.setPageSize(pageSize);
            sreq.setParam("rtime", String.valueOf(System.currentTimeMillis()));
            wpr = WebPlotRequest.makeRawDatasetProcessorRequest(sreq,"get XY plot data from source");
        }
        if (wpr == null) {
            Window.alert("Missing parameter: source");
        }

        return wpr;
    }
}
