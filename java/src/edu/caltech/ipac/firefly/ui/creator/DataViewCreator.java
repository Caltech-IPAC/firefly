/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.previews.AbstractPreviewData;
import edu.caltech.ipac.firefly.ui.previews.DataViewerPreview;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Apr 13, 2010
 * Time: 11:06:09 AM
 */


/**
 * @author Trey Roby
 */
public abstract class DataViewCreator implements ObsResultCreator {


    public static final String EVENT_WORKER_ID= "EVENT_WORKER_ID";
    public static final String USE_META_TAG = "UseMetaTag";

    public TablePreview create(Map<String, String> inParams) {
        Map<String,String> params= new HashMap<String, String>(inParams);
        AbstractPreviewData previewData=makePreviewData(params);
        addCommonParams(previewData,params);
        return new DataViewerPreview(previewData);
    }

    public abstract AbstractPreviewData makePreviewData(Map<String, String> params);


    protected void addCommonParams(AbstractPreviewData  prevData,
                                   Map<String, String> params) {


        prevData.setSourceList(getListParam(params, CoverageCreator.QUERY_ID,true));
        prevData.setEventWorkerList(getListParam(params,EVENT_WORKER_ID,true));
        prevData.setTitle(getParamDestruct(params, CommonParams.TITLE));
        prevData.setGroup(getParamDestruct(params, CommonParams.PLOT_GROUP));
        prevData.setLimitTableParams(getListParam(params, CommonParams.ARG_COLS,true));
        prevData.setHeaderParams(getListParam(params, CommonParams.ARG_HEADERS,true));
        prevData.setCenterOnQueryTarget(getBooleanParam(params,CommonParams.CENTER_ON_QUERY_TARGET,false,true));
        prevData.setColorTableID(getIntParam(params,CommonParams.COLOR_TABLE_ID,true));
        prevData.setStretchStr(getParamDestruct(params, CommonParams.STRETCH));
        prevData.setMouseReadout(parseReadoutString(params.get(CommonParams.READOUT)));
        prevData.setLockRelated((getBooleanParam(params,CommonParams.LOCK_RELATED,false,true)));
        prevData.setUseScrollBars((getBooleanParam(params, CommonParams.USE_SCROLL_BARS, true, true)));
        prevData.setImageSelection((getBooleanParam(params, CommonParams.IMAGE_SELECTION, false, true)));
        prevData.setRememberPrefs((getBooleanParam(params, CommonParams.REMEMBER_PREFS, false, true)));
        prevData.setToolBarPlot(getParamDestruct(params, CommonParams.LAYOUT_TOOLBAR_OVER));
        prevData.setRotateNorthUp(getBooleanParam(params, CommonParams.NORTH_UP, false, true));

        parseZoomParam(prevData, getParamDestruct(params, CommonParams.ZOOM));
        parseColorPreferenceKey(prevData, params);



        String dsStr= getParamDestruct(params, CommonParams.DATA_SOURCE);
        if (dsStr!=null) {
            try {
                prevData.setDataSource(Enum.valueOf(CommonParams.DataSource.class,dsStr));
                params.remove(CommonParams.DATA_SOURCE);
            } catch (Exception e) {
                // do nothing, it is just not set
            }
        }

        prevData.setDataColumn(getParamDestruct(params, CommonParams.DATA_COLUMN));



        if (params.containsKey(CommonParams.MIN_SIZE)) {
            String s[]= params.get(CommonParams.MIN_SIZE).split("x",2);
            if (s.length==2) {
                try {
                    int minWidth= Integer.parseInt(s[0]);
                    int minHeight= Integer.parseInt(s[1]);
                    prevData.setMinSize(minWidth,minHeight);
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
            params.remove(CommonParams.MIN_SIZE);
        }

        prevData.setExtraPlotRequestParams(params);
    }


    /**
     * Parse zoom in two ways.  The simple way is to just have a float value, ie key="Zoom" value=".5"
     * The more complex way allows the meta data to define the zoom level by specifying a metadata element and then
     * using its value to help define the zoom level as such:
     * key="Zoom" value="METATAG:ProductLevel:1b=.5,3a=2,Default=.0625"
     * This example show the meta data elements being product level and then if it has a value of 1b the zoom is .5, a value of
     * 3a then the zoom is 2, otherwise the zoom is .0625
     * @param prevData preview data to set
     * @param zoomStr the string from the xml file
     */
    private void parseZoomParam(AbstractPreviewData  prevData, String zoomStr) {
        if (zoomStr!=null) {
            if (zoomStr.startsWith(USE_META_TAG)) {
                String metaTagParts[]= zoomStr.split(":",3);
                if (metaTagParts.length==3) {
                    prevData.setZoomLevel(AbstractPreviewData.DEFAULT,1.0F);
                    prevData.setZoomMetaTag(metaTagParts[1]);
                    List<String> zOpsList= splitAndTrim(metaTagParts[2]);
                    for(String zOp : zOpsList) {
                        String sAry[]= zOp.split("=");
                        if (sAry.length==2) {
                            try {
                                float zl= Float.parseFloat(sAry[1]);
                                if (sAry[0].equalsIgnoreCase(AbstractPreviewData.DEFAULT)) {
                                    prevData.setZoomLevel(AbstractPreviewData.DEFAULT,zl);
                                }
                                else {
                                    prevData.setZoomLevel(sAry[0],zl);
                                }
                            } catch (NumberFormatException e) { /* do nothing */ }
                        }
                    }
                }
            }
            else {
                try {
                    prevData.setZoomLevel(AbstractPreviewData.DEFAULT,Float.parseFloat(zoomStr));
                } catch (NumberFormatException e) { /* do nothing */ }
            }
        }
    }


    private void parseColorPreferenceKey(AbstractPreviewData prevData, Map<String, String> deParams) {
        String prefStr= getParamDestruct(deParams, CommonParams.COLOR_PREFERENCE_KEY);
        if (prefStr!=null) {
            if (prefStr.startsWith(USE_META_TAG)) {
                String metaTagParts[]= prefStr.split(":",3);
                if (metaTagParts.length==3) {
                    prevData.setPrefColorMetaTag(metaTagParts[1]);
                    List<String> prefOpsList= splitAndTrim(metaTagParts[2]);
                    for(String prefOp : prefOpsList) {
                        String sAry[]= prefOp.split("=");
                        if (sAry.length==2) {
                            if (sAry[0].equalsIgnoreCase(AbstractPreviewData.DEFAULT)) {
                                prevData.setColorPreferenceKey(AbstractPreviewData.DEFAULT, sAry[1]);
                            }
                            else {
                                prevData.setColorPreferenceKey(sAry[0], sAry[1]);
                            }
                        }
                    }
                }
            }
            else {
                prevData.setColorPreferenceKey(AbstractPreviewData.DEFAULT, prefStr);
            }
        }
    }


    public static String getParamDestruct(Map<String, String> params, String key) {
        String retval= null;
        if (params.containsKey(key)) {
            retval= params.get(key);
            params.remove(key);
        }
        return retval;
    }

    public static List<String> getListParam(Map<String, String> params, String key, boolean remove) {
        List<String> retval= splitAndTrim(params.get(key));
        if (remove) params.remove(key);
        return retval;
    }

    public static List<String> getListParam(Map<String, String> params, String key) {
        return getListParam(params,key,false);
    }

    public static boolean getBooleanParam(Map<String, String> params, String key) {
        return getBooleanParam(params, key, false);
    }

    public static boolean getBooleanParam(Map<String, String> params, String key, boolean defValue) {
        return getBooleanParam(params,key,defValue,false);
    }

    public static boolean getBooleanParam(Map<String, String> params, String key, boolean defValue, boolean remove) {
        boolean retval= defValue;
        if (params.containsKey(key)) {
            retval= Boolean.parseBoolean(params.get(key));
            if (remove) params.remove(key);
        }
        return retval;
    }

    public static int getIntParam(Map<String, String> params, String key) {
        return params.containsKey(key) ?  StringUtils.getInt(params.get(key), Integer.MAX_VALUE) :
                                          Integer.MAX_VALUE;
    }

    public static int getIntParam(Map<String, String> params, String key, boolean remove) {
        int retval= params.containsKey(key) ?
                     StringUtils.getInt(params.get(key), Integer.MAX_VALUE) :
                     Integer.MAX_VALUE;
        if (remove) params.remove(key);
        return retval;
    }


    public static List<String> splitAndTrim(String inStr) {
        String s[]= null;
        if (inStr!=null) {
            s= inStr.split(",");
            for(int i=0; (i<s.length);i++) s[i]= StringUtils.trim(s[i]);
        }
        return s==null ? null : Arrays.asList(s);
    }

    public static Map<Integer,String> parseReadoutString(String readout) {

        Map<Integer,String> retval= new HashMap<Integer,String>(11);
        if (readout==null ) return retval;
        List<String> l= StringUtils.asList(readout,",");
        for (String s : l) {
            Param param= Param.parse(s);
            if (param!=null) {
                try {
                    int v= Integer.parseInt(param.getName());
                    retval.put(v,param.getValue());
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return retval;
    }
}

