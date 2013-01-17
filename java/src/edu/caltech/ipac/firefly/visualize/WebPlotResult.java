package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.data.DataEntry;

import java.io.Serializable;
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
public class WebPlotResult implements Serializable, Iterable<Map.Entry<String,DataEntry>> {


    public static final String PLOT_CREATE = "PlotCreate";
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
    public static final String METRICS_HASH_MAP= "Metrics_HasMap";
    public static final String BAND_INFO= "Band_Info";

    private String _ctxStr;
    private boolean _success;
    private String _briefFailReason;
    private String _userFailReason;
    private String _detailFailReason;
    private HashMap<String, DataEntry> _map= new HashMap<String, DataEntry>(3);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    
    public WebPlotResult()  { this(null, true,"", "",""); }
    public WebPlotResult(String ctxStr)  { this(ctxStr, true,"", "",""); }
    protected WebPlotResult(String briefFailReason,
                            String userFailReason,
                            String detailFailReason)  {
        this(null, false, briefFailReason, userFailReason, detailFailReason);
    }

    public static WebPlotResult makeFail(String briefFailReason,
                                         String userFailReason,
                                         String detailFailReason)  {
        return new WebPlotResult(briefFailReason, userFailReason,detailFailReason);
    }

    private WebPlotResult(String ctxStr,
                          boolean success,
                          String briefFailReason,
                          String userFailReason,
                          String detailFailReason)  {
        _ctxStr= ctxStr;
        _success= success;
        _briefFailReason= briefFailReason;
        _userFailReason= userFailReason;
        _detailFailReason= detailFailReason;
    }
//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void putResult(String key, DataEntry result) {
        _map.put(key,result);
    }

    public DataEntry getResult(String key) {
        return _map.get(key);
    }

    public String getStringResult(String key) {
        String retval= null;
        DataEntry s= getResult(key);
        if (s!=null && s instanceof DataEntry.Str) {
            retval= ((DataEntry.Str) s).getString();
        }
        return retval;
    }

    public boolean isSuccess() { return _success; }
    public String getBriefFailReason() { return _briefFailReason; }
    public String getUserFailReason() { return _userFailReason; }
    public String getDetailFailReason() { return _detailFailReason; }
    public String getContextStr() { return _ctxStr; }

    public Iterator<Map.Entry<String,DataEntry>> iterator() {
        return _map.entrySet().iterator();
    }

    public int getResultsSize() { return _map.size(); }
    public boolean containsKey(String key) { return _map.containsKey(key); }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
