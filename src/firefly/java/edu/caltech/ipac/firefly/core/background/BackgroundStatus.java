/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;
/**
 * User: roby
 * Date: 6/6/14
 * Time: 10:26 AM
 */


import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;
import java.util.*;

/**
 * @author Trey Roby
 */
public class BackgroundStatus implements Serializable {
    public static final String BG_STATUS_ACTION = "background.bgStatus";
    public static final String PARAM_SEP = "<<BGSEP>>";
    public static final String KW_VAL_SEP = "==>>";
    public final static String NO_ID = "WARNING:_UNKNOWN_PACKAGE_ID";
    // --------Keys -------------------
    public static final String TYPE = "TYPE";
    public static final String ID = "ID";
    public static final String ATTRIBUTE = "ATTRIBUTES";
    public static final String STATE = "STATE";
    public static final String DATA_SOURCE = "DATA_SOURCE";
    public static final String MESSAGE_BASE = "MESSAGE_";
    public static final String MESSAGE_CNT = "MESSAGE_CNT";
    public static final String PACKAGE_PROGRESS_BASE = "PACKAGE_PROGRESS_";
    public static final String PACKAGE_CNT = "PACKAGE_CNT";
    public static final String CLIENT_REQ = "CLIENT_REQ";
    public static final String SERVER_REQ = "SERVER_REQ";
    public static final String WEB_PLOT_REQ = "WEB_PLOT_REQ";
    public static final String FILE_PATH = "FILE_PATH";
    public static final String TOTAL_BYTES = "TOTAL_BYTES";
    public static final String PUSH_DATA_BASE = "PUSH_DATA_#";
    public static final String PUSH_TYPE_BASE = "PUSH_TYPE_#";
    public static final String PUSH_CNT =       "PUSH_CNT";
    public static final String USER_RESPONSE =  "USER_RESPONSE_#";
    public static final String USER_DESC     =  "USER_DESC_#";
    public static final String RESPONSE_CNT =   "RESPONSE_CNT";
    public static final String ACTIVE_REQUEST_CNT ="ACTIVE_REQUEST_CNT";

    protected static final String URL_SUB = "URL_PARAM_SEP";
    private LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();     // a map of Param keyed by name


    public BackgroundStatus() { this(NO_ID, BackgroundState.STARTING, BgType.UNKNOWN, null ); }
    public BackgroundStatus(String id, BackgroundState state) {  this(id, state, BgType.UNKNOWN, null); }
    // --------End Keys -------------------


    public BackgroundStatus(String id, BackgroundState state, BgType type) {  this(id, state, type, null); }

    public BackgroundStatus(String id, BackgroundState state, BgType type, List<Param> paramList) {
        if (paramList!=null) setParams(paramList);
        if (type==null) {
            if (getBackgroundType()==null)  setBackgroundType(BgType.UNKNOWN);
        }
        else {
            setBackgroundType(type);
        }
        if (state==null) {
            if (getState()==null)  setState(BackgroundState.STARTING);
        }
        else {
            setState(state);
        }
        if (id==null) {
            if (getID()==null)  setID(id);
        }
        else {
            setID(id);
        }
    }

    public static BackgroundStatus cloneWithState(BackgroundState state, BackgroundStatus copyFromStatus)  {
        BackgroundStatus retval= new BackgroundStatus();
        if (copyFromStatus!=null) retval.copyFrom(copyFromStatus);
        retval.setState(state);
        return retval;
    }

//    public BackgroundStatus(BackgroundState state, BackgroundStatus copyFromReq) {
//        if (copyFromReq!=null) this.copyFrom(copyFromReq);
//        setState(state);
//    }

    public static BackgroundStatus createUnknownFailStat() {
        return new BackgroundStatus(NO_ID, BackgroundState.FAIL, BgType.UNKNOWN);
    }

    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to serialize().
     * @param str
     * @return
     */
    public static BackgroundStatus parse(String str) {
        if (str==null) return null;
        BackgroundStatus bgStat= new BackgroundStatus();
        String[] params = str.split(PARAM_SEP);
        if (params.length>0) {
            for(String param : params) {
                String[] kv = param.split(KW_VAL_SEP, 2);
                if (kv.length == 2) {
                    bgStat.setParam(kv[0], kv[1]);
                }
            }
            return bgStat;
        }
        return null;
    }
//====================================================================
//====================================================================
//====================================================================

    protected static void addParam(StringBuffer str, String key, String value) {
        if (value != null) {
            str.append(PARAM_SEP).append(key).append(KW_VAL_SEP).append(value);
        }
    }

    public boolean containsParam(String param) {
        return params.containsKey(param);
    }

    public String getParam(String param) {
        return params.get(param);
    }

    public Map<String, String> getParams() {
        return Collections.unmodifiableMap(params);
    }

    public void setParams(List<Param> params) {
        for(Param p : params) {
            setParam(p);
        }
    }

    public void setParam(String name, String val) {
        setParam(new Param(name, val));
    }

    public void setParam(String name, String... values) {
        setParam(new Param(name, StringUtils.toString(values, ",")));
    }

    private void setParam(Param param) {
        if (param!=null) params.put(param.getName(), param.getValue());
    }

    public void setParam(String name, WorldPt wpt) {
        setParam(name,wpt==null ? null : wpt.toString());
    }

    public void setSafeParam(String name,String val) {
        String newVal= null;
        if (val!=null) newVal= val.replaceAll(PARAM_SEP,URL_SUB);
        setParam(new Param(name, newVal));
    }

    public String getSafeParam(String name) {
        String val= getParam(name);
        String newVal= null;
        if (val!=null) newVal= val.replaceAll(URL_SUB,PARAM_SEP);
        return newVal;
    }

//====================================================================
//====================================================================

    public void removeParam(String name) {
        params.remove(name);
    }

    public BgType getBackgroundType() {
        return StringUtils.getEnum(getParam(TYPE), BgType.UNKNOWN);
    }

    public void setBackgroundType(BgType type) {
        setParam(TYPE, type.toString());
    }

    public boolean hasAttribute(JobAttributes a) {
        String attributes= getParam(ATTRIBUTE);
        return (attributes!=null && attributes.contains(a.toString()));
    }

    public void addAttribute(JobAttributes a) {
        String attributes= getParam(ATTRIBUTE);
        if (attributes==null) {
            attributes= a.toString();
        }
        else if (!attributes.contains(a.toString())) {
            attributes+= "|"+a.toString();
        }
        setParam(ATTRIBUTE, attributes);
    }

    public BackgroundState getState() {
        return StringUtils.getEnum(getParam(STATE), BackgroundState.STARTING);
    }

    public void setState(BackgroundState state) {
        setParam(STATE,state.toString());
    }

    public String getDataSource() {
        String s= getParam(DATA_SOURCE);
        return s!=null ? s : "";
    }

    public void setDataSource(String dataSource) {
        setParam(DATA_SOURCE,dataSource);
    }

    public String getID() { return getParam(ID); }

    public void setID(String id) { setParam(ID,id); }

    public boolean isDone() {
        BackgroundState state= getState();
        return (state == BackgroundState.USER_ABORTED ||
                state == BackgroundState.CANCELED ||
                state == BackgroundState.FAIL ||
                state == BackgroundState.SUCCESS ||
                state == BackgroundState.UNKNOWN_PACKAGE_ID);
    }

    public boolean isFail() {
        BackgroundState state= getState();
        return (state == BackgroundState.FAIL ||
                state == BackgroundState.USER_ABORTED ||
                state == BackgroundState.UNKNOWN_PACKAGE_ID ||
                state == BackgroundState.CANCELED);
    }

    public boolean isSuccess() {
        BackgroundState state= getState();
        return (state == BackgroundState.SUCCESS);
    }

    //------------------------------------------
    //---- Request browser load data -----------
    //------------------------------------------

    public boolean isActive() {
        BackgroundState state= getState();
        return (state == BackgroundState.WAITING ||
                state == BackgroundState.WORKING ||
                state == BackgroundState.STARTING);
    }

    public int getNumPushData() {
        return getIntParam(PUSH_CNT,0);
    }

    public void addPushData(String serializeData, PushType pt) {
        int total= getNumPushData();
        setParam(PUSH_DATA_BASE +total,serializeData);
        setParam(PUSH_TYPE_BASE +total,pt.toString());
        total++;
        setParam(PUSH_CNT,total+"");
    }

    public String getPushData(int idx) {
        return getParam(PUSH_DATA_BASE +idx);
    }

    //------------------------------------------
    //---- Contains responses from User --------
    //------------------------------------------

    public PushType getPushType(int idx) {
        return StringUtils.getEnum(getParam(PUSH_TYPE_BASE + idx), PushType.WEB_PLOT_REQUEST);
    }

    public int getNumResponseData() {
        return getIntParam(RESPONSE_CNT,0);
    }

    public void addResponseData(String desc, String data) {
        int total= getNumResponseData();
        setParam(USER_RESPONSE +total,data);
        setParam(USER_DESC +total,desc);
        total++;
        setParam(RESPONSE_CNT,total+"");
    }

    public String getResponseData(int idx) {
        return getParam(USER_RESPONSE +idx);
    }

    public String getResponseDesc(int idx) {
        return getParam(USER_DESC +idx);
    }

    public int getRequestedCnt() {
        return getIntParam(ACTIVE_REQUEST_CNT,0);
    }

    public void incRequestCnt() {
        int cnt= getRequestedCnt();
        cnt++;
        setParam(ACTIVE_REQUEST_CNT,cnt+"");
    }

    //------------------------------------------
    //------------------------------------------
    //------------------------------------------

    public void decRequestCnt() {
        int cnt= getRequestedCnt();
        if (cnt>0) {
            cnt--;
            setParam(ACTIVE_REQUEST_CNT,cnt+"");
        }
    }

    public void addMessage(String message) {
        int total= getNumMessages();
        setParam(MESSAGE_BASE +total,message);
        total++;
        setParam(MESSAGE_CNT,total+"");
    }

    public int getNumMessages() {
        return getIntParam(MESSAGE_CNT,0);
    }

    public String getMessage(int idx) {
        return getParam(MESSAGE_BASE +idx);
    }
    //------------------------------------------
    //------------------------------------------
    //------------------------------------------

    public List<String> getMessageList() {
        int cnt= getNumMessages();
        List<String> list= new ArrayList<String> (cnt);
        if (cnt>0) {
            for(int i= 0; (i<cnt); i++) {
                String m= getMessage(i);
                if (m!=null) list.add(m);
            }
        }
        return list;
    }

    public PackageProgress getPartProgress(int i) {
        String s= getParam(PACKAGE_PROGRESS_BASE+i);
        PackageProgress retval= PackageProgress.parse(s);
        if (retval==null) retval= new PackageProgress();
        return retval;
    }

    public List<PackageProgress> getPartProgressList() {
        int cnt= getPackageCount();
        List<PackageProgress> list= new ArrayList<PackageProgress> (cnt);
        if (cnt>0) {
            for(int i= 0; (i<cnt); i++) {
                PackageProgress pp= getPartProgress(i);
                if (pp!=null) list.add(pp);
            }
        }
        return list;
    }

    public boolean isMultiPart() {
        return getPackageCount()>1;
    }

    public int getPackageCount() {
        return getIntParam(PACKAGE_CNT,0);
    }

    public void setPackageCount(int cnt) { setParam(PACKAGE_CNT,cnt+""); }

    public void addPackageProgress(PackageProgress progress) {
        int total= getPackageCount();
        setPartProgress(progress,total);
        total++;
        setParam(PACKAGE_CNT,total+"");
    }

    public void setPartProgress(PackageProgress progress, int i) {
        setParam(PACKAGE_PROGRESS_BASE+i,progress.serialize());
    }

    public Request getClientRequest() {
        return Request.parse(getParam(CLIENT_REQ));
    }

    public void setClientRequest(Request request) {
        if (request!=null) setParam(CLIENT_REQ,request.toString());
    }

    public TableServerRequest getServerRequest() {
        return TableServerRequest.parse(getParam(SERVER_REQ));
    }

    public void setServerRequest(TableServerRequest request) {
        if (request!=null) setParam(SERVER_REQ,request.toString());
    }

    public WebPlotRequest getWebPlotRequest() {
        return WebPlotRequest.parse(getParam(WEB_PLOT_REQ));
    }

    public void setWebPlotRequest(WebPlotRequest wpr) {
        if (wpr!=null) setParam(WEB_PLOT_REQ,wpr.toString());
    }

    public String getFilePath() { return getParam(FILE_PATH); }

    public void setFilePath(String filePath) { setParam(FILE_PATH,filePath); }

    /**
     * @return processed bytes if all bundles were processed successfully, otherwise previously estimated size in bytes
     */
    public long getTotalSizeInBytes() {
        long actualProcessSize = 0;
        if (getState() == BackgroundState.SUCCESS && getPackageCount()>0) {
            for(PackageProgress pp : getPartProgressList()) {
                actualProcessSize+= pp.getProcessedBytes();
            }
        }
        if (actualProcessSize > 0) {
            return actualProcessSize;
        } else {
            return getLongParam(TOTAL_BYTES,0L);
        }

    }

//====================================================================
//====================================================================


    public void copyFrom(BackgroundStatus s) {
        params.putAll(s.params);
    }

    /**
     * Serialize this object into its string representation.
     * @return the serialized string
     */
    public String serialize() {

        StringBuilder str=  new StringBuilder();
        str.append(TYPE).append(KW_VAL_SEP).append(getParam(TYPE));

        if (params != null && params.size() > 0) {
            for(Map.Entry<String,String> p :  params.entrySet()) {
                if (!(p.getKey().equals(TYPE))) {
                    str.append(PARAM_SEP).append(p.getKey()).append(KW_VAL_SEP).append(p.getValue());
                }
            }
        }
        return str.toString();
    }

    public String toString() {
        return "packageID: " + getID() + ", state: " + getState() + ", type: " + getBackgroundType();
    }

    public BackgroundStatus cloneWithState(BackgroundState state) {
        BackgroundStatus s = newInstance();
        s.copyFrom(this);
        s.setState(state);
        return s;
    }

    public BackgroundStatus newInstance() {
        return new BackgroundStatus();
    }

    //====================================================================
//  overriding equals
//====================================================================
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BackgroundStatus) {
            return toString().equals(obj.toString());
        }
        return false;
    }

    //====================================================================
//  convenience data converting routines
//====================================================================
    public boolean getBooleanParam(String key, boolean defValue) {
        return containsParam(key) ?
               StringUtils.getBoolean(getParam(key)) :
               defValue;
    }

    public boolean getBooleanParam(String key) {
        return StringUtils.getBoolean(getParam(key));
    }

    public int getIntParam(String key) {
        return StringUtils.getInt(getParam(key));
    }

    public int getIntParam(String key, int def) {
        return StringUtils.getInt(getParam(key),def);
    }

    public long getLongParam(String key, long def) {
        return StringUtils.getLong(getParam(key),def);
    }

    public double getDoubleParam(String key) {
        return StringUtils.getDouble(getParam(key));
    }

    public float getFloatParam(String key) {
        return StringUtils.getFloat(getParam(key));
    }

    public Date getDateParam(String key) {
        return StringUtils.getDate(getParam(key));
    }

    public WorldPt getWorldPtParam(String key) {
        WorldPt wpt;
        try {
            wpt= ResolvedWorldPt.parse(getParam(key));
        } catch (NumberFormatException e) {
            wpt= null;
        }
        return wpt;
    }

    public enum BgType {SEARCH, PACKAGE, UNKNOWN, PERSISTENT}

//====================================================================
//
//====================================================================

//====================================================================
//  convenience data converting routines
//====================================================================


    public enum PushType {WEB_PLOT_REQUEST, REGION_FILE_NAME, TABLE_FILE_NAME, FITS_COMMAND_EXT }





}

