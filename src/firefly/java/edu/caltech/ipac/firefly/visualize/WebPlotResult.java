/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.util.HashMap;

/**
 * @author Trey Roby
 * Date: Aug 8, 2008
 */
public record WebPlotResult(String ctxStr,
                          boolean success,
                          String briefFailReason,
                          String userFailReason,
                          String detailFailReason,
                          String requestKey,
                          String plotId,
                          HashMap<String, Object> map) {

    public static final String PLOT_CREATE = "PlotCreate";
    public static final String PLOT_CREATE_HEADER = "PlotCreateHeader";
    public static final String STRING= "String";
    public static final String PLOT_IMAGES= "PlotImages";
    public static final String DATA_HISTOGRAM= "DataHistogram";
    public static final String DATA_BIN_MEAN_ARRAY= "DataBinMeanArray";
    public static final String DATA_BIN_COLOR_IDX= "DataBinColorIdx";
    public static final String REGION_FILE_NAME = "RegionFileName";
    public static final String BAND_INFO= "Band_Info";
    public static final String REGION_ERRORS= "RegionErrors";
    public static final String REGION_DATA= "RegionData";
    public static final String TITLE= "Title";
    public static final String RESULT_ARY= "resultAry";

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    
    public WebPlotResult()  { this(null, true,"", "","","",null, new HashMap<>()); }
    public WebPlotResult(String ctxStr)  { this(ctxStr, true,"", "","","",null, new HashMap<>()); }
    public static WebPlotResult makeFail(String briefFailReason,
                                         String userFailReason,
                                         String detailFailReason,
                                         String requestKey,
                                         String plotId)  {
        return new WebPlotResult(null, false, briefFailReason, userFailReason,detailFailReason,
                requestKey, plotId, new HashMap<>());
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void putResult(String key, Object result) { map.put(key,result); }

    public Object getResult(String key) { return map.get(key); }

    public String getStringResult(String key) {
        return  (map.get(key) instanceof String) ? (String)map.get(key) : null;
    }

    public boolean containsKey(String key) { return map.containsKey(key); }
}
