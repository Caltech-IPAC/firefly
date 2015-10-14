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

