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
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.InsertBandInitializer;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.WebPlotResultParser;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey Roby
 */
public class WebPlotResultSerializer {

    public static String createJson(WebPlotResult res) {
        return createJson(res,false);

    }

    public static String createJson(WebPlotResult res, boolean useDeepJson) {
        StringBuilder retval= new StringBuilder(5000);
        if (res.isSuccess()) {
            retval.append("[{");
            retval.append( "\"success\" : true," );
            if (res.containsKey(WebPlotResult.PLOT_STATE)) {
                PlotState state= (PlotState)res.getResult(WebPlotResult.PLOT_STATE);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.PLOT_STATE,
                                          VisJsonSerializer.serializePlotState(state,useDeepJson));
            }
            if (res.containsKey(WebPlotResult.PLOT_IMAGES)) {
                PlotImages images= (PlotImages)res.getResult(WebPlotResult.PLOT_IMAGES);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.PLOT_IMAGES, images.toString());
            }
            if (res.containsKey(WebPlotResult.INSERT_BAND_INIT)) {
                InsertBandInitializer init= (InsertBandInitializer)res.getResult(WebPlotResult.INSERT_BAND_INIT);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.INSERT_BAND_INIT, init.toString());
            }
            if (res.containsKey(WebPlotResult.PLOT_CREATE)) {
                CreatorResults cr= (CreatorResults)res.getResult(WebPlotResult.PLOT_CREATE);
//                addJSItem(retval, WebPlotResult.PLOT_CREATE, cr.toString());
                //---
                String sAry[]= WebPlotResultParser.makeCreatorResultStringArray(cr);
                WebPlotResultParser.addJSArray(retval, WebPlotResult.PLOT_CREATE, sAry);
                //---
            }
            if (res.containsKey(WebPlotResult.DATA_HIST_IMAGE_URL)) {
                String s= res.getStringResult(WebPlotResult.DATA_HIST_IMAGE_URL);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.DATA_HIST_IMAGE_URL, s);
            }
            if (res.containsKey(WebPlotResult.CBAR_IMAGE_URL)) {
                String s= res.getStringResult(WebPlotResult.CBAR_IMAGE_URL);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.CBAR_IMAGE_URL, s);
            }
            if (res.containsKey(WebPlotResult.STRING)) {
                String s= res.getStringResult(WebPlotResult.STRING);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.STRING, s);
            }
            if (res.containsKey(WebPlotResult.IMAGE_FILE_NAME)) {
                String s= res.getStringResult(WebPlotResult.IMAGE_FILE_NAME);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.IMAGE_FILE_NAME, s);
            }
            if (res.containsKey(WebPlotResult.REGION_FILE_NAME)) {
                String s= res.getStringResult(WebPlotResult.REGION_FILE_NAME);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.REGION_FILE_NAME, s);
            }
            if (res.containsKey(WebPlotResult.DATA_HISTOGRAM)) {
                int ary[]= ((DataEntry.IntArray)res.getResult(WebPlotResult.DATA_HISTOGRAM)).getArray();
                WebPlotResultParser.addJSArray(retval, WebPlotResult.DATA_HISTOGRAM, ary);
            }
            if (res.containsKey(WebPlotResult.DATA_BIN_MEAN_ARRAY)) {
                double ary[]= ((DataEntry.DoubleArray)res.getResult(WebPlotResult.DATA_BIN_MEAN_ARRAY)).getArray();
                WebPlotResultParser.addJSArray(retval, WebPlotResult.DATA_BIN_MEAN_ARRAY, ary);
            }
            if (res.containsKey(WebPlotResult.BAND_INFO)) {
                BandInfo bi= (BandInfo)res.getResult(WebPlotResult.BAND_INFO);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.BAND_INFO, StringUtils.escapeQuotes(bi.serialize()));
            }
            if (res.containsKey(WebPlotResult.REGION_DATA)) {
                String s= res.getStringResult(WebPlotResult.REGION_DATA);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.REGION_DATA, StringUtils.escapeQuotes(s));
            }
            if (res.containsKey(WebPlotResult.REGION_ERRORS)) {
                String s= res.getStringResult(WebPlotResult.REGION_ERRORS);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.REGION_ERRORS, StringUtils.escapeQuotes(s));
            }
            if (res.containsKey(WebPlotResult.TITLE)) {
                String s= res.getStringResult(WebPlotResult.TITLE);
                WebPlotResultParser.addJSItem(retval, WebPlotResult.TITLE, StringUtils.escapeQuotes(s));
            }
            retval.deleteCharAt(retval.length()-1);

            retval.append("}]");
        }
        else {
            String pKey= res.getProgressKey()==null?"":res.getProgressKey();
            retval.append("[{");
            retval.append( "\"success\" : false," );
            retval.append( "\"briefFailReason\" : " );
            retval.append( WebPlotResultParser.QUOTE).append(StringUtils.escapeQuotes(res.getBriefFailReason())).append( WebPlotResultParser.QUOTE);
            retval.append(",");
            retval.append( "\"userFailReason\" : " );
            retval.append( WebPlotResultParser.QUOTE).append(StringUtils.escapeQuotes(res.getUserFailReason())).append( WebPlotResultParser.QUOTE);
            retval.append(",");
            retval.append( "\"detailFailReason\" : " );
            retval.append( WebPlotResultParser.QUOTE).append(StringUtils.escapeQuotes(res.getDetailFailReason())).append( WebPlotResultParser.QUOTE);
            retval.append(",");
            retval.append( "\"progressKey\" : " );
            retval.append( WebPlotResultParser.QUOTE).append(StringUtils.escapeQuotes(pKey)).append( WebPlotResultParser.QUOTE);
            retval.append("}]");
        }

        return retval.toString();
    }
}
