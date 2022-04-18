/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Trey Roby
 * Date: Aug 8, 2008
 */
public record WebPlotResult(boolean success,
                          String briefFailReason,
                          String userFailReason,
                          String detailFailReason,
                          String requestKey,
                          String plotId,
                          HashMap<String, Object> map) {

    public static final String PLOT_CREATE = "PlotCreate";
    public static final String PLOT_CREATE_HEADER = "PlotCreateHeader";
    public static final String STRING= "String";
    public static final String DATA_HISTOGRAM= "DataHistogram";
    public static final String DATA_BIN_MEAN_ARRAY= "DataBinMeanArray";
    public static final String DATA_BIN_COLOR_IDX= "DataBinColorIdx";
    public static final String REGION_FILE_NAME = "RegionFileName";
    public static final String BAND_INFO= "Band_Info";
    public static final String REGION_ERRORS= "RegionErrors";
    public static final String REGION_DATA= "RegionData";
    public static final String TITLE= "Title";
    public static final String RESULT_ARY= "resultAry";

    public static final List<String> keyList= Arrays.asList(
            PLOT_CREATE, PLOT_CREATE_HEADER, STRING, DATA_HISTOGRAM,
            DATA_BIN_MEAN_ARRAY, DATA_BIN_COLOR_IDX, REGION_FILE_NAME, BAND_INFO, REGION_ERRORS,
            REGION_DATA, TITLE, RESULT_ARY);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    
    public WebPlotResult()  { this(true,"", "","","",null, new HashMap<>()); }
    public WebPlotResult(String requestKey)  { this(true,"", "","",requestKey,null, new HashMap<>()); }
    public static WebPlotResult makeFail(String briefFailReason,
                                         String userFailReason,
                                         String detailFailReason,
                                         String requestKey,
                                         String plotId)  {
        return new WebPlotResult(false, briefFailReason, userFailReason,detailFailReason,
                requestKey, plotId, new HashMap<>());
    }

    /**
     * make a new WebPlotResult with the list of entries. The entries should be in pair the key and the value
     */
    public static WebPlotResult make(Object... itemAry) {
        if (itemAry.length % 2 !=0) throw new IllegalArgumentException("parameters much be in pairs, key then value");
        WebPlotResult r= new WebPlotResult();
        for(int i=0; (i<itemAry.length); i+=2) {
            if ((itemAry[i] instanceof String s)) r.putResult(s, itemAry[i+1]);
            else throw new IllegalArgumentException("first of each argument pair must be a string key");
        }
        return r;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void putResult(String key, Object result) {
        if (!keyList.contains(key)) throw new IllegalArgumentException("key must be in the predefined list");
        map.put(key,result);
    }

    public Object getResult(String key) { return map.get(key); }

    public String getStrResult(String key) {
        return  (map.get(key) instanceof String) ? (String)map.get(key) : null;
    }

    public boolean containsKey(String key) { return map.containsKey(key); }
}
