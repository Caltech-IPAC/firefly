/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.messaging.JsonHelper;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.visualize.VisJsonSerializer;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.TableUtil.Format;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.data.TableServerRequest.FF_SESSION_ID;

/**
 * @author Trey Roby
 */

public class SrvParam {

    private final Map<String, String[]> paramMap;

    public SrvParam(Map<String, String[]> paramMap) { this.paramMap=new HashMap<>(paramMap); }

    public Map<String, String> flatten() {
        HashMap<String, String> p = new HashMap<>();
        paramMap.forEach((k, v) -> p.put(k, String.join(",", v)));
        return p;
    }

    public Map<String, String[]> getParamMap() {
        return paramMap;
    }

    public int size() { return paramMap.size(); }

    /**
     * Return any parameters that are not in ignore list. If the parameter has more than one entry, only return the first
     * @param ignoreList an array list of parameters to ignore
     * @return a map of the parameters that are not ignored
     */
    public Map<String, String> getParamMapUsingExcludeList(List<String> ignoreList) {
        Map<String,String> retMap= new HashMap<>();
        for(Map.Entry<String,String[]> entry : this.paramMap.entrySet()) {
            if (!ignoreList.contains(entry.getKey())) {
                retMap.put(entry.getKey(),entry.getValue()[0]);
            }
        }
//        retMap= this.paramMap.entrySet().stream().filter( entry -> !ignoreList.contains(entry.getKey()))
//                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue());
        return retMap;
    }
//    public Map<String, String> getParamMapUsingExcludeList(List<String> ignoreList) {
//        return this.paramMap.entrySet().stream().filter( entry -> !ignoreList.contains(entry.getKey()))
//                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue());
//    }

    public static SrvParam makeSrvParamSimpleMap(Map<String, String> map) {
        Map<String, String[]> targetMap= new HashMap<>();
        for( Map.Entry<String,String> entry : map.entrySet()) {
            targetMap.put(entry.getKey(), new String[] {entry.getValue()} );
        }
        return new SrvParam(targetMap);
    }

    public void setParam(String key, String value) {
        paramMap.put(key, new String[] {value});
    }

    public void addParams(Map<String, String> map) {
        for( Map.Entry<String,String> entry : map.entrySet()) {
            if (!paramMap.containsKey(entry.getKey())) {
                paramMap.put(entry.getKey(), new String[] {entry.getValue()} );
            }
        }
    }

    public boolean contains(String key) { return paramMap.containsKey(key); }

    public String getCommandKey() {
        return getRequired(ServerParams.COMMAND);
    }

    /**
     * Look for the ServerParams.STATE key and parse the value into a PlotState Object
     * Throw an exception if it is not found
     * @return a PlotState object
     */
    public PlotState getState() {
        PlotState state= null;
        String stateStr= getRequired(ServerParams.STATE);
        if (stateStr!=null) state= VisJsonSerializer.deserializePlotStateFromString(stateStr);
        if (state == null) {
            throw new IllegalArgumentException("parameter in wrong format: state, (see PlotState.serialize()");
        }
        return state;
    }

    public PlotState getState(int idx, boolean required) {
        String key=  ServerParams.STATE+idx;
        String stateStr= required? getRequired(key) : getOptional(key);
        PlotState state= null;
        if (stateStr!=null) state= VisJsonSerializer.deserializePlotStateFromString(stateStr);
//        PlotState state= PlotState.parse(getRequired(ServerParams.STATE+idx));
        if (state == null && required) {
            throw new IllegalArgumentException("parameter in wrong format: state"+idx+", (see PlotState.serialize()");
        }
        return state;
    }

    public PlotState[] getStateAry() {
        List<PlotState> stateList= new ArrayList<>();
        PlotState state= getState(0,true);
        stateList.add(state);
        for(int i=1;(state!=null); i++) {
            state= getState(i,false);
            if (state!=null) stateList.add(state);
        }
        return stateList.toArray(new PlotState[0]);
    }

    public List<WebPlotRequest> getRequestList() {
        List<WebPlotRequest> reqList= new ArrayList<>();
        WebPlotRequest wpr= getRequiredWebPlotRequest(ServerParams.REQUEST+"0");
        reqList.add(wpr);
        for(int i=1;(wpr!=null); i++) {
            wpr= getOptionalWebPlotRequest(ServerParams.REQUEST+i);
            if (wpr!=null) reqList.add(wpr);
        }
        return reqList;
    }


    /**
     * Look for the ServerParams.ID key and return the string value
     * Throw an exception if it is not found
     * @return in ID value
     */
    public String getID() { return getRequired(ServerParams.ID); }

    public String getRequired(String key) {
        String[] ary = paramMap.get(key);
        if (ary != null && ary.length>0) {
            return ary[0];
        }
        else {
            throw new IllegalArgumentException("missing parameter: "+ key);
        }
    }

    public List<String> getRequiredList(String key) {
        String[] ary = paramMap.get(key);
        if (ary != null && ary.length>0) {
            return Arrays.asList(ary);
        }
        else {
            throw new IllegalArgumentException("missing parameter: "+ key);
        }
    }

    public List<String> getOptionalList(String key) {
        String[] ary = paramMap.get(key);
        if (ary != null && ary.length>0) {
            return Arrays.asList(ary);
        }
        else {
            return Collections.emptyList();
        }
    }

    public String getOptional(String key) {
        String[] ary = paramMap.get(key);
        if (ary != null && ary.length>0) {
            return ary[0];
        }
        else {
            return null;
        }
    }

    public String getOptional(String key, String defValue) {
        String v= getOptional(key);
        return (v==null) ? defValue : v;
    }

    public float getOptionalFloat(String key, float defValue) {
        String[] ary = paramMap.get(key);
        if (ary != null && ary.length>0) {
            try {
                return Float.parseFloat(ary[0]);
            } catch (NumberFormatException e) {
                return defValue;
            }
        }
        else {
            return defValue;
        }
    }

    public int getOptionalInt(String key, int defValue) {
        String[] ary = paramMap.get(key);
        if (ary != null && ary.length>0) {
            try {
                return Integer.parseInt(ary[0]);
            } catch (NumberFormatException e) {
                return defValue;
            }
        }
        else {
            return defValue;
        }
    }

    public int getRequiredInt(String key) {
        String v= getRequired(key);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "parameter could not be parsed as int: parameter: "+ key + ", value: "+v, e);
        }
    }

    public float getRequiredFloat(String key) {
        String v= getRequired(key);
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "parameter could not be parsed as float: parameter: "+ key + ", value: "+v, e);
        }
    }

    public double getRequiredDouble(String key) {
        String v= getRequired(key);
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "parameter could not be parsed as double: parameter: "+ key + ", value: "+v, e);
        }
    }

    public ImagePt getRequiredImagePt(String key) {
        String v= getRequired(key);
        try {
            return ImagePt.parse(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "parameter could not be parsed as ImagePt: parameter: "+ key + ", value: "+v, e);
        }
    }

    public ImagePt[] getRequiredImagePtAry(String key) {
        String v= getRequired(key);
        try {
            ImagePt[] ptAry= getImagePtAryFromJson(v);
            if (ptAry==null) {
                throw new IllegalArgumentException(
                        "parameter could not be parsed as ImagePt: parameter: "+ key + ", value: "+v);
            }
            return ptAry;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "parameter could not be parsed as ImagePt: parameter: "+ key + ", value: "+v, e);
        }
    }

    public static ImagePt[] getImagePtAryFromJson(String json) {
        if (json==null) return null;
        JsonHelper helper= JsonHelper.parse(json);
        JSONArray list= helper.getValue(new JSONArray());
        if (list==null || list.isEmpty()) return null;
        ImagePt[] ptAry= new ImagePt[list.size()];
        for(int i=0;(i<ptAry.length);i++) {
            ptAry[i]= ImagePt.parse((String)list.get(i));
            if (ptAry[i]==null) return null;
        }
        return ptAry;
    }

    public static WorldPt[] getWorldPtAryFromJson(String json) {
        if (json==null) return null;
        JsonHelper helper= JsonHelper.parse(json);
        JSONArray list= helper.getValue(new JSONArray());
        if (list==null || list.isEmpty()) return null;
        WorldPt[] ptAry= new WorldPt[list.size()];
        for(int i=0;(i<ptAry.length);i++) {
            ptAry[i]= WorldPt.parse((String)list.get(i));
            if (ptAry[i]==null) return null;
        }
        return ptAry;
    }

    public static double[] getDoubleAryFromJson(String json) {
        if (json==null) return null;
        JsonHelper helper= JsonHelper.parse(json);
        JSONArray list= helper.getValue(new JSONArray());
        if (list==null || list.isEmpty()) return null;
        double[] dAry= new double[list.size()];
        for(int i=0;(i<dAry.length);i++) {
            try{
                Object v= list.get(i);
                switch (v) {
                    case Double aDouble -> dAry[i] = aDouble;
                    case Long l -> dAry[i] = l;
                    case Integer integer -> dAry[i] = integer;
                    case null, default -> dAry[i] = Double.NaN;
                }
            } catch (ClassCastException e) {
                return null;
            }
        }
        return dAry;
    }

    public static String[] getStringAryFromJson(String json) {
        if (json==null) return null;
        JsonHelper helper= JsonHelper.parse(json);
        JSONArray list= helper.getValue(new JSONArray());
        if (list==null || list.isEmpty()) return null;
        String[] sAry= new String[list.size()];
        for(int i=0;(i<sAry.length);i++) {
            Object v= list.get(i);
            sAry[i]= v!=null ? v.toString() : null;
        }
        return sAry;
    }

    public boolean getOptionalBoolean(String key, boolean defval) {
        String v= getOptional(key);
        return v==null ? defval : Boolean.parseBoolean(v);
    }

    public boolean getRequiredBoolean(String key) {
        return Boolean.parseBoolean(getRequired(key));
    }

    public WebPlotRequest getOptionalWebPlotRequest(String key) {
        return WebPlotRequest.parse(getOptional(key));
    }
    public WebPlotRequest getRequiredWebPlotRequest(String key) {
        return WebPlotRequest.parse(getRequired(key));
    }


//====================================================================
//  Table related convenience methods
//====================================================================
    public TableServerRequest getTableServerRequest() {
        String reqString = getRequired(ServerParams.REQUEST);
        return QueryUtil.convertToServerRequest(reqString);
    }

    public void insertJobId(String jobId) {
        try {
            TableServerRequest request = getTableServerRequest();
            request.setJobId(jobId);                    // for future reference
            request.setParam(FF_SESSION_ID, jobId);     // for caching
            setParam(ServerParams.REQUEST, JsonTableUtil.toJsonTableRequest(request).toJSONString());
        }catch (Exception e) {
            // just ignore.  will generate error downstream.
        }
    }

    public DownloadRequest getDownloadRequest() {
        String tableReqStr = getRequired(ServerParams.REQUEST);
        String dlReqStr = getRequired(ServerParams.DOWNLOAD_REQUEST);
        String selInfoStr = getOptional(ServerParams.SELECTION_INFO);
        return QueryUtil.convertToDownloadRequest(dlReqStr, tableReqStr, selInfoStr);
    }

    public Format getTableFormat() {
        final String fileFormat = getOptional("file_format", Format.IPACTABLE.name()).toLowerCase();

        Format formatInMap = TableUtil.getAllFormats().get(fileFormat);
        return formatInMap == null ? Format.IPACTABLE : formatInMap;
    }

}

