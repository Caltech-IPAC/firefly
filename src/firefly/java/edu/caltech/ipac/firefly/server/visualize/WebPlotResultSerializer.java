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
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.InsertBandInitializer;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.util.StringUtils;
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

    private static final String QUOTE= "\"";

    public static String createJson(WebPlotResult res) {
        return createJson(res,false);

    }
    public static String createJson(WebPlotResult res, boolean useDeepJson) {
        return useDeepJson ? createJsonDeepString(res) : createJsonShallow(res);
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
        if (res.isSuccess()) {
            if (res.containsKey(WebPlotResult.PLOT_STATE)) {
                PlotState state= (PlotState)res.getResult(WebPlotResult.PLOT_STATE);
                map.put(WebPlotResult.PLOT_STATE, VisJsonSerializer.serializePlotState(state));
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
                int intAry[]= ((DataEntry.IntArray)res.getResult(WebPlotResult.DATA_HISTOGRAM)).getArray();
                JSONArray ary = new JSONArray();
                for(int v : intAry) ary.add(v);
                map.put(WebPlotResult.DATA_HISTOGRAM, ary);
            }
            if (res.containsKey(WebPlotResult.DATA_BIN_MEAN_ARRAY)) {
                double dAry[]= ((DataEntry.DoubleArray)res.getResult(WebPlotResult.DATA_BIN_MEAN_ARRAY)).getArray();
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
                DataEntry.WebPlotResultAry resultEntry= (DataEntry.WebPlotResultAry)res.getResult(WebPlotResult.RESULT_ARY);
                WebPlotResult resultAry[]= resultEntry.getArray();
                JSONArray jResAry= new JSONArray();
                for(WebPlotResult r : resultAry) {
                    jResAry.add(createJsonDeep(r));
                }
                map.put(WebPlotResult.RESULT_ARY, jResAry);

            }

        }
        else {
            String pKey= res.getProgressKey()==null?"":res.getProgressKey();
            map.put( "briefFailReason", res.getBriefFailReason());
            map.put( "userFailReason", res.getUserFailReason());
            map.put( "detailFailReason", res.getDetailFailReason());
            map.put( "progressKey", pKey);
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


    public static String createJsonShallow(WebPlotResult res) {
        StringBuilder retval= new StringBuilder(5000);
        if (res.isSuccess()) {
            retval.append("[{");
            retval.append( "\"success\" : true," );
            if (res.containsKey(WebPlotResult.PLOT_STATE)) {
                PlotState state= (PlotState)res.getResult(WebPlotResult.PLOT_STATE);
                addJSItem(retval, WebPlotResult.PLOT_STATE, state.toString());
            }
            if (res.containsKey(WebPlotResult.PLOT_IMAGES)) {
                PlotImages images= (PlotImages)res.getResult(WebPlotResult.PLOT_IMAGES);
                addJSItem(retval, WebPlotResult.PLOT_IMAGES, images.toString());
            }
            if (res.containsKey(WebPlotResult.INSERT_BAND_INIT)) {
                InsertBandInitializer init= (InsertBandInitializer)res.getResult(WebPlotResult.INSERT_BAND_INIT);
                addJSItem(retval, WebPlotResult.INSERT_BAND_INIT, init.toString());
            }
            if (res.containsKey(WebPlotResult.PLOT_CREATE)) {
                CreatorResults cr= (CreatorResults)res.getResult(WebPlotResult.PLOT_CREATE);
                String sAry[]= makeCreatorResultStringArray(cr);
                addJSArray(retval, WebPlotResult.PLOT_CREATE, sAry);
            }
            if (res.containsKey(WebPlotResult.DATA_HIST_IMAGE_URL)) {
                String s= res.getStringResult(WebPlotResult.DATA_HIST_IMAGE_URL);
                addJSItem(retval, WebPlotResult.DATA_HIST_IMAGE_URL, s);
            }
            if (res.containsKey(WebPlotResult.CBAR_IMAGE_URL)) {
                String s= res.getStringResult(WebPlotResult.CBAR_IMAGE_URL);
                addJSItem(retval, WebPlotResult.CBAR_IMAGE_URL, s);
            }
            if (res.containsKey(WebPlotResult.STRING)) {
                String s= res.getStringResult(WebPlotResult.STRING);
                addJSItem(retval, WebPlotResult.STRING, s);
            }
            if (res.containsKey(WebPlotResult.IMAGE_FILE_NAME)) {
                String s= res.getStringResult(WebPlotResult.IMAGE_FILE_NAME);
                addJSItem(retval, WebPlotResult.IMAGE_FILE_NAME, s);
            }
            if (res.containsKey(WebPlotResult.REGION_FILE_NAME)) {
                String s= res.getStringResult(WebPlotResult.REGION_FILE_NAME);
                addJSItem(retval, WebPlotResult.REGION_FILE_NAME, s);
            }
            if (res.containsKey(WebPlotResult.DATA_HISTOGRAM)) {
                int ary[]= ((DataEntry.IntArray)res.getResult(WebPlotResult.DATA_HISTOGRAM)).getArray();
                addJSArray(retval, WebPlotResult.DATA_HISTOGRAM, ary);
            }
            if (res.containsKey(WebPlotResult.DATA_BIN_MEAN_ARRAY)) {
                double ary[]= ((DataEntry.DoubleArray)res.getResult(WebPlotResult.DATA_BIN_MEAN_ARRAY)).getArray();
                addJSArray(retval, WebPlotResult.DATA_BIN_MEAN_ARRAY, ary);
            }
            if (res.containsKey(WebPlotResult.BAND_INFO)) {
                BandInfo bi= (BandInfo)res.getResult(WebPlotResult.BAND_INFO);
                addJSItem(retval, WebPlotResult.BAND_INFO, StringUtils.escapeQuotes(bi.serialize()));
            }
            if (res.containsKey(WebPlotResult.REGION_DATA)) {
                String s= res.getStringResult(WebPlotResult.REGION_DATA);
                addJSItem(retval, WebPlotResult.REGION_DATA, StringUtils.escapeQuotes(s));
            }
            if (res.containsKey(WebPlotResult.REGION_ERRORS)) {
                String s= res.getStringResult(WebPlotResult.REGION_ERRORS);
                addJSItem(retval, WebPlotResult.REGION_ERRORS, StringUtils.escapeQuotes(s));
            }
            if (res.containsKey(WebPlotResult.TITLE)) {
                String s= res.getStringResult(WebPlotResult.TITLE);
                addJSItem(retval, WebPlotResult.TITLE, StringUtils.escapeQuotes(s));
            }
            retval.deleteCharAt(retval.length()-1);

            retval.append("}]");
        }
        else {
            String pKey= res.getProgressKey()==null?"":res.getProgressKey();
            retval.append("[{");
            retval.append( "\"success\" : false," );
            retval.append( "\"briefFailReason\" : " );
            retval.append( QUOTE).append(StringUtils.escapeQuotes(res.getBriefFailReason())).append( QUOTE);
            retval.append(",");
            retval.append( "\"userFailReason\" : " );
            retval.append( QUOTE).append(StringUtils.escapeQuotes(res.getUserFailReason())).append( QUOTE);
            retval.append(",");
            retval.append( "\"detailFailReason\" : " );
            retval.append( QUOTE).append(StringUtils.escapeQuotes(res.getDetailFailReason())).append( QUOTE);
            retval.append(",");
            retval.append( "\"progressKey\" : " );
            retval.append( QUOTE).append(StringUtils.escapeQuotes(pKey)).append( QUOTE);
            retval.append("}]");
        }

        return retval.toString();
    }

    private static String[] makeCreatorResultStringArray(CreatorResults cr) {
        WebPlotInitializer wpInit[]= cr.getInitializers();
        String retval[]= new String[wpInit.length];
        for(int i=0; i<wpInit.length; i++) {
            retval[i]= VisJsonSerializer.serializeWebPlotInitializerShallow(wpInit[i]);
        }
        return retval;
    }

    private static void addJSItem(StringBuilder sb, String key, String value) {
        sb.append( QUOTE).append(key).append( QUOTE);
        sb.append(" : ");
        sb.append( QUOTE).append(value).append(QUOTE);
        sb.append(",");
    }

    private static void addJSArray(StringBuilder sb, String key, String ary[]) {
        sb.append( QUOTE).append(key).append( QUOTE);
        sb.append(" : [");
        for(String s : ary) sb.append("\"").append(s).append("\"").append(",");
        sb.deleteCharAt(sb.length() - 1);
        sb.append("],");
    }

    private static void addJSArray(StringBuilder sb, String key, int ary[]) {
        sb.append( QUOTE).append(key).append( QUOTE);
        sb.append(" : [");
        for(int i : ary) sb.append(i).append(",");
        sb.deleteCharAt(sb.length() - 1);
        sb.append("],");
    }

    private static  void addJSArray(StringBuilder sb, String key, double ary[]) {
        sb.append( QUOTE).append(key).append( QUOTE);
        sb.append(" : [");
        for(double v : ary) sb.append(v).append(",");
        sb.deleteCharAt(sb.length()-1);
        sb.append("],");
    }
}
