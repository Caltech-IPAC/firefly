/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 1/4/12
 * Time: 3:04 PM
 */


import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;
import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class WebPlotResultParser {

    public static final String QUOTE= "\"";

    public static WebPlotResult convert(PlotResultOverlay res) {
        WebPlotResult  retval;
        if (res.isSuccess()) {
            retval= new WebPlotResult();
            String stateStr= res.getResult(WebPlotResult.PLOT_STATE);
            if (stateStr!=null) {
                PlotState state= PlotState.parse(stateStr);
                retval.putResult(WebPlotResult.PLOT_STATE, state);
            }
            String imagesStr= res.getResult(WebPlotResult.PLOT_IMAGES);
            if (imagesStr!=null) {
                PlotImages images= PlotImages.parse(imagesStr);
                retval.putResult(WebPlotResult.PLOT_IMAGES, images);
            }

            String wpInitStr= res.getResult(WebPlotResult.INSERT_BAND_INIT);
            if (wpInitStr!=null) {
                InsertBandInitializer ibInit= InsertBandInitializer.parse(wpInitStr);
                retval.putResult(WebPlotResult.INSERT_BAND_INIT, ibInit);
            }

            JsArrayString creatorAry= res.getStringArrayResult(WebPlotResult.PLOT_CREATE);
//            String wpCreateStr= res.getResult(WebPlotResult.PLOT_CREATE);
            if (creatorAry!=null) {
//                CreatorResults cr= CreatorResults.parse(wpCreateStr);
//                retval.putResult(WebPlotResult.PLOT_CREATE, cr);
                    String ary[]= new String[creatorAry.length()];
                    for(int i= 0; (i<ary.length); i++) ary[i]= creatorAry.get(i);
                    List<WebPlotInitializer> wpInitList= new ArrayList<WebPlotInitializer>(ary.length);
                    for(String s : ary) {
                        WebPlotInitializer wpInit= WebPlotInitializer.parse(s);
                        if (wpInit!=null) wpInitList.add(wpInit);
                    }
                    CreatorResults cr= new CreatorResults(wpInitList.toArray(new WebPlotInitializer[wpInitList.size()]));
                    retval.putResult(WebPlotResult.PLOT_CREATE, cr);
            }

            checkForStringResult(WebPlotResult.STRING,retval,res);
            checkForStringResult(WebPlotResult.DATA_HIST_IMAGE_URL,retval,res);
            checkForStringResult(WebPlotResult.CBAR_IMAGE_URL,retval,res);
            checkForStringResult(WebPlotResult.IMAGE_FILE_NAME,retval,res);
            checkForStringResult(WebPlotResult.REGION_FILE_NAME,retval,res);
            checkForStringResult(WebPlotResult.REGION_DATA,retval,res);
            checkForStringResult(WebPlotResult.REGION_ERRORS,retval,res);
            checkForStringResult(WebPlotResult.TITLE,retval,res);
            checkForIntAryResult(WebPlotResult.DATA_HISTOGRAM, retval, res);
            checkForDoubleAryResult(WebPlotResult.DATA_BIN_MEAN_ARRAY, retval, res);


            String biStr= res.getResult(WebPlotResult.BAND_INFO);
            if (biStr!=null) {
                BandInfo bi= BandInfo.parse(biStr);
                retval.putResult(WebPlotResult.BAND_INFO, bi);
            }
        }
        else {
            retval= WebPlotResult.makeFail(res.getBriefFailReason(),
                                           res.getUserFailReason(),
                                           res.getDetailFailReason(),
                                           res.getProgressKey());

        }
        return retval;
    }


    private static void checkForStringResult(String key, WebPlotResult wpRes,PlotResultOverlay res)  {
        String s= res.getResult(key);
        if (s!=null) {
            wpRes.putResult(key,new DataEntry.Str(res.getResult(key)) );
        }

    }

    private static void checkForIntAryResult(String key, WebPlotResult wpRes,PlotResultOverlay res)  {
        JsArrayInteger jsAry= res.getIntArrayResult(key);
        if (jsAry!=null) {
            int ary[]= new int[jsAry.length()];
            for(int i= 0; (i<ary.length); i++) ary[i]= jsAry.get(i);
            wpRes.putResult(key,new DataEntry.IntArray(ary));
        }

    }

    private static void checkForDoubleAryResult(String key, WebPlotResult wpRes,PlotResultOverlay res)  {
        JsArrayNumber jsAry= res.getDoubleArrayResult(key);
        if (jsAry!=null) {
            double ary[]= new double[jsAry.length()];
            for(int i= 0; (i<ary.length); i++) ary[i]= jsAry.get(i);
            wpRes.putResult(key,new DataEntry.DoubleArray(ary));
        }
    }



    public static WebPlotResult convert(String res) {
       return convert(changeToJS(res).get(0));
    }

    public static native JsArray<PlotResultOverlay> changeToJS(String arg) /*-{
        return eval(arg);
    }-*/;


    public static String createJS(WebPlotResult res) {
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
//                addJSItem(retval, WebPlotResult.PLOT_CREATE, cr.toString());
                //---
                String sAry[]= makeCreatorResultStringArray(cr);
                addJSArray(retval, WebPlotResult.PLOT_CREATE, sAry);
                //---
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
                addJSItem(retval,WebPlotResult.BAND_INFO, StringUtils.escapeQuotes(bi.serialize()));
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

    public static String[] makeCreatorResultStringArray(CreatorResults cr) {
        WebPlotInitializer wpInit[]= cr.getInitializers();
        String retval[]= new String[wpInit.length];
        for(int i=0; i<wpInit.length; i++) {
            retval[i]= wpInit[i].toString();
        }
        return retval;
//        StringBuilder sb= new StringBuilder(wpInit.length*200);
//        sb.append("[");
//        for(int i=0; i<wpInit.length; i++) {
//            sb.append( "\"" );
//            sb.append(wpInit[i]);
//            sb.append( "\"" );
//            if (i<wpInit.length-1) sb.append(",");
//        }
//        sb.append("]");
//        return sb.toString();
    }

    public static void addJSItem(StringBuilder sb, String key, String value) {
        sb.append( QUOTE).append(key).append( QUOTE);
        sb.append(" : ");
        sb.append( QUOTE).append(value).append(QUOTE);
        sb.append(",");
    }

    public static void addJSArray(StringBuilder sb, String key, String ary[]) {
        sb.append( QUOTE).append(key).append( QUOTE);
        sb.append(" : [");
        for(String s : ary) sb.append("\"").append(s).append("\"").append(",");
        sb.deleteCharAt(sb.length() - 1);
        sb.append("],");
    }

    public static void addJSArray(StringBuilder sb, String key, int ary[]) {
        sb.append( QUOTE).append(key).append( QUOTE);
        sb.append(" : [");
        for(int i : ary) sb.append(i).append(",");
        sb.deleteCharAt(sb.length() - 1);
        sb.append("],");
    }

    public static  void addJSArray(StringBuilder sb, String key, double ary[]) {
        sb.append( QUOTE).append(key).append( QUOTE);
        sb.append(" : [");
        for(double v : ary) sb.append(v).append(",");
        sb.deleteCharAt(sb.length()-1);
        sb.append("],");
    }
}

