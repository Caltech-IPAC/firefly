package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableServerRequest extends ServerRequest implements Serializable, DataEntry, Cloneable {

    public static final String DECIMATE_INFO = "decimate";
    public static final String FILTERS = "filters";
    public static final String SORT_INFO = "sortInfo";
    public static final String PAGE_SIZE = "pageSize";
    public static final String START_IDX = "startIdx";
    public static final String INCL_COLUMNS = "inclCols";
    public static final String FIXED_LENGTH = "fixedLength";
    public static final String META_INFO = "META_INFO";
    private static final String SYS_PARAMS = "|" + StringUtils.toString(new String[]{FILTERS,SORT_INFO,PAGE_SIZE,START_IDX,INCL_COLUMNS,FIXED_LENGTH,META_INFO}, "|") + "|";

    private int pageSize;
    private int startIdx;
    private ArrayList<String> filters;
    private Map<String, String> metaInfo;

    public TableServerRequest() {
    }

    public TableServerRequest(String id) { super(id); }

    public TableServerRequest(String id, ServerRequest copyFromReq) {
        super(id,copyFromReq);
    }
//====================================================================
//
//====================================================================

    public Map<String, String> getMeta() {
        return metaInfo;
    }

    public String getMeta(String key) {
        return metaInfo == null ? null : metaInfo.get(key);
    }

    public void setMeta(String meta, String value) {
        if (metaInfo == null) {
            metaInfo = new HashMap<String, String>();
        }
        metaInfo.put(meta, value);
    }

    @Override
    public boolean isInputParam(String paramName) {
        return !SYS_PARAMS.contains("|" + paramName + "|");
    }

    public int getStartIndex() {
        return startIdx;
    }

    public void setStartIndex(int startIndex) {
        this.startIdx = startIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public SortInfo getSortInfo() {
        return containsParam(SORT_INFO) ? SortInfo.parse(getParam(SORT_INFO)) : null;
    }

    public void setSortInfo(SortInfo sortInfo) {
        if (sortInfo == null) {
            removeParam(SORT_INFO);
        } else {
            setParam(SORT_INFO, sortInfo.toString());
        }
    }

    public DecimateInfo getDecimateInfo() {
        return containsParam(DECIMATE_INFO) ? DecimateInfo.parse(getParam(DECIMATE_INFO)) : null;
    }

    public void setDecimateInfo(DecimateInfo decimateInfo) {
        if (decimateInfo == null) {
            removeParam(DECIMATE_INFO);
        } else {
            setParam(DECIMATE_INFO, decimateInfo.toString());
        }
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        if (filters != null) {
            if (this.filters == null) {
                this.filters = new ArrayList<String>();
            }
            this.filters.clear();
            this.filters.addAll(filters);
        } else {
            this.filters = null;
        }
    }

    @Override
    public ServerRequest newInstance() {
        return new TableServerRequest();
    }

    //====================================================================

//====================================================================


    public void copyFrom(ServerRequest req) {
        super.copyFrom(req);
        if (req instanceof TableServerRequest) {
            TableServerRequest sreq = (TableServerRequest) req;
            pageSize = sreq.pageSize;
            startIdx = sreq.startIdx;
            if (sreq.filters == null) {
                filters = null;
            } else {
                filters = new ArrayList<String>(sreq.filters.size());
                filters.addAll(sreq.filters);
            }
            if (sreq.metaInfo == null) {
                metaInfo = null;
            } else {
                metaInfo = new HashMap<String, String>(sreq.metaInfo.size());
                metaInfo.putAll(sreq.metaInfo);
            }
        }
    }

    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to toString().
     * @param str
     * @return
     */
    public static TableServerRequest parse(String str) {
        return ServerRequest.parse(str,new TableServerRequest());
	}



    /**
     * Serialize this object into its string representation.
     * This class uses the url convention as its format.
     * Parameters are separated by '&'.  Keyword and value are separated
     * by '='.  If the keyword contains a '/' char, then the left side is
     * the keyword, and the right side is its description.
     * @return the serialize version of the class
     */
    public String toString() {

        StringBuffer str = new StringBuffer(super.toString());

        if (startIdx != 0) {
            addParam(str, START_IDX, String.valueOf(startIdx));
        }
        if (pageSize != 0) {
            addParam(str, PAGE_SIZE, String.valueOf(pageSize));
        }
        if ( filters != null && filters.size() > 0) {
            addParam(str, FILTERS, toFilterStr(filters));
        }
        if ( metaInfo != null && metaInfo.size() > 0) {
            addParam(str, META_INFO, StringUtils.mapToEncodedString(metaInfo));
        }
        return str.toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public static List<String> parseFilters(String s) {
        if (StringUtils.isEmpty(s)) return null;
        String[] filters = s.split(";");
        return filters == null ? null : Arrays.asList(filters);
    }

    public static String toFilterStr(List<String> l) {
        return StringUtils.toString(l,";");
    }

//====================================================================
//
//====================================================================


    protected boolean addPredefinedAttrib(Param param) {
        if (param == null || param.getName() == null || param.getValue() == null) return false;

//        if (param.getName().equals(SORT_INFO)) {
//            sortInfo = SortInfo.parse(param.getValue());
//    }
         if (param.getName().equals(PAGE_SIZE)) {
            pageSize = StringUtils.getInt(param.getValue());
        } else if (param.getName().equals(START_IDX)) {
            startIdx = StringUtils.getInt(param.getValue());
        } else if (param.getName().equals(FILTERS)) {
            String s = param.getValue();
            if (s == null || s.trim().length() == 0) {
                setFilters(null);
            } else {
                setFilters(parseFilters(s));
            }
         } else if (param.getName().equals(META_INFO)) {
             String s = param.getValue();
             if (s == null || s.trim().length() == 0) {
                 metaInfo = null;
             } else {
                 metaInfo = StringUtils.encodedStringToMap(s);
             }
        } else {
            return false;
        }
        return true;
    }

//====================================================================
//  inner classes
//====================================================================

}