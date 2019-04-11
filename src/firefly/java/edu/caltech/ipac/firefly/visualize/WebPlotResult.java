/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/**
 * User: roby
 * Date: Aug 8, 2008
 * Time: 1:37:26 PM
 */


/**
 * @author Trey Roby
 */
public class WebPlotResult implements Iterable<Map.Entry<String,Object>> {


    public static final String PLOT_CREATE = "PlotCreate";
    public static final String PLOT_CREATE_HEADER = "PlotCreateHeader";
    public static final String PLOT_STATE = "PlotState";
    public static final String INSERT_BAND_INIT = "InsertBand";
    public static final String STRING= "String";
    public static final String PLOT_IMAGES= "PlotImages";
    public static final String FLUX_VALUE= "FluxValue";
    public static final String RAW_DATA_SET= "RawDataSet";
    public static final String DATA_HISTOGRAM= "DataHistogram";
    public static final String DATA_BIN_MEAN_ARRAY= "DataBinMeanArray";
    public static final String DATA_HIST_IMAGE_URL= "DataHistImageUrl";
    public static final String CBAR_IMAGE_URL= "CBarImageUrl";
    public static final String IMAGE_FILE_NAME = "ImageFileName";
    public static final String REGION_FILE_NAME = "RegionFileName";
    public static final String METRICS_HASH_MAP= "Metrics_HasMap";
    public static final String BAND_INFO= "Band_Info";
    public static final String REGION_ERRORS= "RegionErrors";
    public static final String REGION_DATA= "RegionData";
    public static final String REQUEST_LIST= "RequestList";
    public static final String TITLE= "Title";
    public static final String RESULT_ARY= "resultAry";

    private String _ctxStr;
    private boolean _success;
    private String _briefFailReason;
    private String _userFailReason;
    private String _detailFailReason;
    private String _requestKey;
    private String _plotId;
    private HashMap<String, Object> _map= new HashMap<>(3);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    
    public WebPlotResult()  { this(null, true,"", "","","",null); }
    public WebPlotResult(String ctxStr)  { this(ctxStr, true,"", "","","",null); }
    protected WebPlotResult(String briefFailReason,
                            String userFailReason,
                            String detailFailReason,
                            String progressKey,
                            String plotId)  {
        this(null, false, briefFailReason, userFailReason, detailFailReason,progressKey,plotId);
    }

    public static WebPlotResult makeFail(String briefFailReason,
                                         String userFailReason,
                                         String detailFailReason)  {
        return new WebPlotResult(briefFailReason, userFailReason,detailFailReason,"",null);
    }

    public static WebPlotResult makeFail(String briefFailReason,
                                         String userFailReason,
                                         String detailFailReason,
                                         String progressKey,
                                         String plotId)  {
        return new WebPlotResult(briefFailReason, userFailReason,detailFailReason,progressKey, plotId);
    }

    private WebPlotResult(String ctxStr,
                          boolean success,
                          String briefFailReason,
                          String userFailReason,
                          String detailFailReason,
                          String requestKey,
                          String plotId)  {
        _ctxStr= ctxStr;
        _success= success;
        _briefFailReason= briefFailReason;
        _userFailReason= userFailReason;
        _detailFailReason= detailFailReason;
        _requestKey = requestKey;
        _plotId= plotId;
    }
//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void putResult(String key, Object result) {
        _map.put(key,result);
    }

    public Object getResult(String key) {
        return _map.get(key);
    }

    public String getStringResult(String key) {
        String retval= null;
        Object s= getResult(key);
        if (s!=null && s instanceof String) {
            retval= (String)s;
        }
        return retval;
    }

    public boolean isSuccess() { return _success; }
    public String getBriefFailReason() { return _briefFailReason; }
    public String getUserFailReason() { return _userFailReason; }
    public String getDetailFailReason() { return _detailFailReason; }
    public String getProgressKey() { return _requestKey; }
    public String getPlotId() { return _plotId; }
    public String getContextStr() { return _ctxStr; }
    public void setRequestKey(String requestKey) { _requestKey = requestKey;}
    public String getRequestKey() { return _requestKey; }

    public Iterator<Map.Entry<String,Object>> iterator() {
        return _map.entrySet().iterator();
    }

    public int getResultsSize() { return _map.size(); }
    public boolean containsKey(String key) { return _map.containsKey(key); }

    public PlotState getPlotState() {
        return (PlotState)this.getResult(WebPlotResult.PLOT_STATE);
    }
    public void setPlotState(PlotState state) {
        this.putResult(WebPlotResult.PLOT_STATE, state);
    }


}

