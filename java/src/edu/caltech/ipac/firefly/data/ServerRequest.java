package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServerRequest implements Serializable, DataEntry, Cloneable {

    public static final String REQUEST_CLASS= "RequestClass";
    public static final String SERVER_REQUEST_CLASS = "ServerRequest";
    public static final String PARAM_SEP = "&";
    protected static final String URL_SUB = "URL_PARAM_SEP";
//    protected static final String REG_EXP_FOR_PARAM_SEP = "[^&]&[^&]";
    public static final String KW_DESC_SEP = "/";
    public static final String KW_VAL_SEP = "=";
    private static final String BACKGROUNDABLE = "bgable";

    public static final String ID_NOT_DEFINED = "ID_NOT_DEFINED";
    public static final String ID_KEY = "id";
    private LinkedHashMap<String, Param> params = new LinkedHashMap<String, Param>();     // a map of Param keyed by name

    public ServerRequest() { this(ID_NOT_DEFINED,(ServerRequest)null ); }

    public ServerRequest(String id) {  this(id,(ServerRequest)null); }

    public ServerRequest(String id, ServerRequest copyFromReq) {
        if (copyFromReq!=null) this.copyFrom(copyFromReq);
        setRequestId(id);
        setRequestClass(SERVER_REQUEST_CLASS);
    }


    public ServerRequest(String id, List<Param> paramList) {
        if (paramList!=null) setParams(paramList);
        if (id==null) {
            if (getRequestId()==null)  setRequestId(ID_NOT_DEFINED );
        }
        else {
            setRequestId(id);
        }
        setRequestClass(SERVER_REQUEST_CLASS);
    }
//====================================================================
//
//====================================================================

    /**
     * return true if this parameter is a user input parameter.
     * @param paramName
     * @return
     */
    public boolean isInputParam(String paramName) {
        return true;
    }


    public String getRequestId() { return getParam(ID_KEY); }

    public void setRequestId(String id) {
        setParam(ID_KEY, id);
    }

    public boolean isBackgroundable() {
        return getBooleanParam(BACKGROUNDABLE, false);
    }

    public void setIsBackgroundable(boolean isBackgroundable) {
        setParam(BACKGROUNDABLE, String.valueOf(isBackgroundable));
    }

    public String getRequestClass() {
        return containsParam(REQUEST_CLASS) ? getParam(REQUEST_CLASS) : SERVER_REQUEST_CLASS;
    }

    protected void setRequestClass(String reqType) { setParam(REQUEST_CLASS,reqType); }

//====================================================================

    public boolean containsParam(String param) {
        return params.containsKey(param);
    }

    public String getParam(String param) {
        Param p = params.get(param);
        return p == null ? null : p.getValue();
    }

    public List<Param> getParams() {
        return new ArrayList<Param>(params.values());
    }

    public void setParam(String name, String val) {
        setParam(new Param(name, val));
    }

    public void setParam(String name, String... values) {
        setParam(new Param(name, StringUtils.toString(values, ",")));
    }


    public void setParam(Param param) {
        if (param!=null) params.put(param.getName(), param);
    }

    public void setParams(List<Param> params) {
        for(Param p : params) {
            setParam(p);
        }
    }

    public void setParams(Map<String,String> paramMap) {
        for(Map.Entry<String,String> entry : paramMap.entrySet()) {
            setParam(new Param(entry.getKey(),entry.getValue()));
        }
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

    public boolean isValid() {
        return !StringUtils.isEmpty(getParam(ID_KEY));
    }

    public void removeParam(String name) {
        params.remove(name);
    }

    /**
     * Add a predefined attribute
     * @param param the param to add
     * @return true if this was a predefined attribute and was set, false it this is an unknow attribute
     */
    protected boolean addPredefinedAttrib(Param param) { return false; }

//====================================================================


    public void copyFrom(ServerRequest req) {
        params.putAll(req.params);
    }




    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to toString().
     * @param str
     * @return
     */
    public static <T extends ServerRequest> T parse(String str, T req) {
        if (str==null) return null;
        String[] params = str.split(PARAM_SEP);
        if (params != null) {
            for(int i = 0; i < params.length; i++) {
                Param p = Param.parse(params[i]);
                boolean wasSet= req.addPredefinedAttrib(p);
                if (!wasSet) req.setParam(p);

            }
            return req;
        }
        return null;
	}

    /**
     * Serialize this object into its string representation.
     * This class uses the url convention as its format.
     * Parameters are separated by '&'.  Keyword and value are separated
     * by '='.  If the keyword contains a '/' char, then the left side is
     * the keyword, and the right side is its description.
     * @return
     */
    public String toString() {

        StringBuffer str=  new StringBuffer(params.size()*20 + 30);
        str.append(ID_KEY).append(KW_VAL_SEP).append(getParam(ID_KEY));

        if (params != null && params.size() > 0) {
            Collection<Param> vals = params.values();
            for(Param p : vals) {
                if (!(p.getName().equals(ID_KEY))) {
                    str.append(PARAM_SEP).append(p);
                }
            }
        }
        return str.toString();
    }


    public ServerRequest cloneRequest() {
        ServerRequest sr = newInstance();
        sr.copyFrom(this);
        return sr;
    }

    public ServerRequest newInstance() {
        return new ServerRequest();
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
        if (obj instanceof ServerRequest) {
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

    public long getLongParam(String key) {
        return StringUtils.getLong(getParam(key));
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

//====================================================================
//
//====================================================================

//====================================================================
//  convenience data converting routines
//====================================================================


    protected static void addParam(StringBuffer str, String key, String value) {
        if (value != null) {
            str.append(PARAM_SEP).append(key).append(KW_VAL_SEP).append(value);
        }
    }


//====================================================================
//  inner classes
//====================================================================

}