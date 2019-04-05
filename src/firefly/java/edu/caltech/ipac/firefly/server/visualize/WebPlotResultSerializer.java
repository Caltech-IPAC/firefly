/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 10/13/15
 * Time: 4:37 PM
 */


import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.InsertBandInitializer;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class WebPlotResultSerializer {

    public static String createJson(WebPlotResult res) {
        return createJsonDeepString(res);
    }

    public static String createJson(WebPlotResult resAry[], String requestKey) {  // note- this call only support useDeepJson
        JSONArray ary= new JSONArray();
        for(WebPlotResult res : resAry) ary.add(createJsonDeep(res));

        JSONObject map = new JSONObject();
        map.put( "success", true);
        map.put("data", ary);
        map.put("requestKey", requestKey);

        return map.toString();
    }


    public static String createJsonDeepString(WebPlotResult res) {
        JSONObject obj= createJsonDeep(res);
        JSONArray wrapperAry= new JSONArray();
        wrapperAry.add(obj);
        return wrapperAry.toString();
    }

    public static JSONObject createJsonDeep(WebPlotResult res) {

        JSONObject map = new JSONObject();
        map.put("JSON", true);
        map.put( "success", res.isSuccess());
        String requestKey= res.getRequestKey()==null?"":res.getRequestKey();
        if (res.isSuccess()) {
            if (res.containsKey(WebPlotResult.PLOT_STATE)) {
                map.put(WebPlotResult.PLOT_STATE, VisJsonSerializer.serializePlotState(res.getPlotState()));
            }
            if (res.containsKey(WebPlotResult.PLOT_IMAGES)) {
                PlotImages images= (PlotImages)res.getResult(WebPlotResult.PLOT_IMAGES);
                map.put(WebPlotResult.PLOT_IMAGES, VisJsonSerializer.serializePlotImages(images));
            }
            if (res.containsKey(WebPlotResult.INSERT_BAND_INIT)) {
                InsertBandInitializer init= (InsertBandInitializer)res.getResult(WebPlotResult.INSERT_BAND_INIT);
                map.put(WebPlotResult.INSERT_BAND_INIT, VisJsonSerializer.serializeInsertBandInitializer(init));
            }
            if (res.containsKey(WebPlotResult.PLOT_CREATE)) {
                CreatorResults cr= (CreatorResults)res.getResult(WebPlotResult.PLOT_CREATE);
                JSONArray ary = new JSONArray();
                for(WebPlotInitializer wpInit : cr.getInitializers()) {
                    ary.add(VisJsonSerializer.serializeWebPlotInitializerDeep(wpInit));
                }
                map.put(WebPlotResult.PLOT_CREATE, ary);
                map.put(WebPlotResult.PLOT_CREATE_HEADER, VisJsonSerializer.serializeWebPlotHeaderInitializer(cr.getInitHeader()));
            }
            if (res.containsKey(WebPlotResult.DATA_HIST_IMAGE_URL)) {
                String s= res.getStringResult(WebPlotResult.DATA_HIST_IMAGE_URL);
                map.put(WebPlotResult.DATA_HIST_IMAGE_URL, s);
            }
            if (res.containsKey(WebPlotResult.CBAR_IMAGE_URL)) {
                String s= res.getStringResult(WebPlotResult.CBAR_IMAGE_URL);
                map.put(WebPlotResult.CBAR_IMAGE_URL, s);
            }
            if (res.containsKey(WebPlotResult.STRING)) {
                String s= res.getStringResult(WebPlotResult.STRING);
                map.put(WebPlotResult.STRING, s);
            }
            if (res.containsKey(WebPlotResult.IMAGE_FILE_NAME)) {
                String s= res.getStringResult(WebPlotResult.IMAGE_FILE_NAME);
                map.put(WebPlotResult.IMAGE_FILE_NAME, s);
            }
            if (res.containsKey(WebPlotResult.REGION_FILE_NAME)) {
                String s= res.getStringResult(WebPlotResult.REGION_FILE_NAME);
                map.put(WebPlotResult.REGION_FILE_NAME, s);
            }
            if (res.containsKey(WebPlotResult.DATA_HISTOGRAM)) {
                int intAry[]= (int[])res.getResult(WebPlotResult.DATA_HISTOGRAM);
                JSONArray ary = new JSONArray();
                for(int v : intAry) ary.add(v);
                map.put(WebPlotResult.DATA_HISTOGRAM, ary);
            }
            if (res.containsKey(WebPlotResult.DATA_BIN_MEAN_ARRAY)) {
                double dAry[]= (double[])res.getResult(WebPlotResult.DATA_BIN_MEAN_ARRAY);
                JSONArray ary = new JSONArray();
                for(double v : dAry) ary.add(v);
                map.put(WebPlotResult.DATA_BIN_MEAN_ARRAY, ary);
            }
            if (res.containsKey(WebPlotResult.BAND_INFO)) {
                BandInfo bi= (BandInfo)res.getResult(WebPlotResult.BAND_INFO);
                map.put(WebPlotResult.BAND_INFO, bandInfoDeepSerialize(bi));
            }
            if (res.containsKey(WebPlotResult.REGION_DATA)) {
                String s= res.getStringResult(WebPlotResult.REGION_DATA);
                JSONArray ary = new JSONArray();
                if (s.length()>2) {
                    String regAry[] = s.substring(1, s.length() - 1).split("--STR--");
                    for(String rv : regAry) ary.add(rv);
                }
                map.put(WebPlotResult.REGION_DATA, ary);
            }
            if (res.containsKey(WebPlotResult.REGION_ERRORS)) {
                String s= res.getStringResult(WebPlotResult.REGION_ERRORS);
                map.put(WebPlotResult.REGION_ERRORS, s);
            }
            if (res.containsKey(WebPlotResult.TITLE)) {
                map.put(WebPlotResult.TITLE, res.getStringResult(WebPlotResult.TITLE));
            }
            if (res.containsKey(WebPlotResult.RESULT_ARY)) {
                WebPlotResult resultAry[]= (WebPlotResult[])res.getResult(WebPlotResult.RESULT_ARY);
                JSONArray jResAry= new JSONArray();
                for(WebPlotResult r : resultAry) {
                    jResAry.add(createJsonDeep(r));
                }
                map.put(WebPlotResult.RESULT_ARY, jResAry);

            }
            map.put( "requestKey", requestKey);

        }
        else {
            map.put( "requestKey", requestKey);
            map.put( "briefFailReason", res.getBriefFailReason());
            map.put( "userFailReason", res.getUserFailReason());
            map.put( "detailFailReason", res.getDetailFailReason());
            map.put( "requestKey", requestKey);
            map.put( "plotId", res.getPlotId());
        }

        JSONObject wraperObj= new JSONObject();
        wraperObj.put("success", true);
        wraperObj.put("data", map);


        return wraperObj;

    }

    public static JSONObject bandInfoDeepSerialize(BandInfo bi) {
        Map<Band, HashMap<Metrics, Metric>> metMap= bi.getMetricsMap();
        JSONObject retval= new JSONObject();
        for(Map.Entry<Band,HashMap<Metrics, Metric>> entry : metMap.entrySet()) {
            retval.put(entry.getKey().toString(), convertMetrics(entry.getValue()));
        }
        return retval;
    }


    private static JSONObject convertMetrics(Map<Metrics, Metric> metricMap) {
        JSONObject retval= new JSONObject();
        for(Map.Entry<Metrics, Metric> entry : metricMap.entrySet()) {
            retval.put(entry.getKey().toString(), convertOneMetric(entry.getValue()));
        }
        return retval;
    }

    private static JSONObject convertOneMetric(Metric metric) {
        JSONObject retval= new JSONObject();
        retval.put("desc", metric.getDesc());
        retval.put("value", metric.getValue());
        retval.put("units", metric.getUnits());
        if (metric.getImageWorkSpacePt()!=null) {
            retval.put("ip", metric.getImageWorkSpacePt().toString());
        }
        return retval;
    }


}
