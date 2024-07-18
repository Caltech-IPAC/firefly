/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static edu.caltech.ipac.firefly.data.ServerParams.JOB_ID;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

public class TableServerRequest extends ServerRequest implements Serializable, Cloneable {

    public static final String TBL_FILE_PATH = "tblFilePath";       // this meta if exists contains source of the data
    public static final String TBL_FILE_TYPE = "tblFileType";       // this meta if exists contains storage type, ipac, h2, sqlite, etc
    public static final String FILTER_SEP = " _AND_ ";         // need to match what's defined in FilterInfo.js

    public static final String TBL_ID = "tbl_id";
    public static final String TITLE = "title";
    public static final String FILTERS = "filters";
    public static final String SQL_FILTER = "sqlFilter";
    public static final String SORT_INFO = "sortInfo";
    public static final String PAGE_SIZE = "pageSize";
    public static final String START_IDX = "startIdx";
    public static final String INCL_COLUMNS = "inclCols";
    public static final String FIXED_LENGTH = "fixedLength";
    public static final String META_INFO = "META_INFO";
    public static final String META_OPTIONS = "META_OPTIONS";
    public static final String FF_SESSION_ID = "ffSessionId";
    public static final String TBL_INDEX = "tbl_index";     // the table to show if it's a multi-table file.
    public static final String SELECT_INFO = "selectInfo";  // a property of META_INFO.  deserialize into SelectionInfo.java
    private static final List<String> SYS_PARAMS = Arrays.asList(REQUEST_CLASS,INCL_COLUMNS,SORT_INFO,FILTERS,SQL_FILTER,PAGE_SIZE,START_IDX,FIXED_LENGTH,META_INFO,TBL_ID, FF_SESSION_ID);

    private int pageSize = -1;
    private int startIdx;
    private ArrayList<String> filters;
    private String sqlFilter;
    private SelectionInfo selectInfo;
    private Map<String, String> metaInfo;
    private transient Map<String, String> metaOptions;               // options to apply for this request.  it will not persist nor returned to the client


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
            metaInfo = new HashMap<>();
        }
        metaInfo.put(meta, value);
    }

    public String getOption(String key) {
        return metaOptions == null ? null : metaOptions.get(key);
    }

    public void setOption(String key, String value) {
        if (metaOptions == null) {
            metaOptions = new HashMap<>();
        }
        metaOptions.put(key, value);
    }

    public void setTblId(String id) {
        setMeta(TBL_ID, id);
    }

    public String getTblId() {
        return getMeta(TBL_ID);
    }

    public String getTblTitle() {
        return getMeta(TITLE);
    }

    @Override
    public boolean isInputParam(String paramName) {
        return !SYS_PARAMS.contains(paramName);
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

    public String getInclColumns() { return getParam(INCL_COLUMNS); }
    public void setInclColumns(String ...cols) {
        if (cols == null || cols.length == 0) {
            removeParam(INCL_COLUMNS);
        } else {
            setParam(INCL_COLUMNS, StringUtils.toString(cols, ","));
        }
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

    /**
     * This populate the predefined fields as well as the parameter map.
     * @param name name
     * @param val value
     */
    public void setTrueParam(String name, String val) {
        Param p = new Param(name, val);
        boolean wasSet= addPredefinedAttrib(p);
        if (!wasSet) setParam(p);
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        if (filters != null) {
            if (this.filters == null) {
                this.filters = new ArrayList<>();
            }
            this.filters.clear();
            this.filters.addAll(filters);
        } else {
            this.filters = null;
        }
    }

    /**
     * SQL where clause to be added to filters
     * This is a string of op::sql, where op can be AND or OR with AND being the default if not found
     * @param sqlFilter
     */
    public void setSqlFilter(String sqlFilter) {
        String[] opSql = parseSqlFilter(sqlFilter);
        this.sqlFilter = isEmpty(opSql[1]) ? null : sqlFilter;
    }

    public String getSqlFilter() {
        return sqlFilter;
    }

    /**
     * @return [op, sql] of this sqlFilter request.  op is default to AND if not given.
     */
    public static String[] parseSqlFilter(String sqlFilter) {
        String s = sqlFilter == null ? "" : sqlFilter;
        String[] parts = s.split("::", 2);
        String sql = parts.length == 1 ? parts[0] : parts[1];
        String op = parts.length == 1 ? "AND" : parts[0];
        return new String[]{op, sql};
    }

    public SelectionInfo getSelectInfo() {
        return selectInfo;
    }

    public void setSelectInfo(SelectionInfo selectInfo) {
        this.selectInfo = selectInfo;
    }

    /**
     * use this when you want to get full data in its
     * natural order.
     */
    public void keepBaseParamOnly() {
        setFilters(null);
        setSqlFilter(null);
        setPageSize(Integer.MAX_VALUE);
        setSortInfo(null);
        setSelectInfo(null);
        removeParam(INCL_COLUMNS);
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
            TableServerRequest treq = (TableServerRequest) req;
            pageSize    = treq.pageSize;
            startIdx    = treq.startIdx;
            sqlFilter   = treq.sqlFilter;
            filters     = treq.filters == null ? null : new ArrayList<>(treq.filters);
            metaInfo    = treq.metaInfo == null ? null : new HashMap<>(treq.metaInfo);
            metaOptions = treq.metaOptions == null ? null : new HashMap<>(treq.metaOptions);
            selectInfo  = treq.selectInfo;
        }
    }

    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to toString().
     * @param str string representing table request
     * @return table server request object
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
     * @deprecated  use JSON format instead.  @see edu.caltech.ipac.table.JsonTableUtil#toJsonTableRequest
     */
    @Deprecated
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
        if (isEmpty(s)) return null;
        String[] filters = s.split(FILTER_SEP);
        return Arrays.asList(filters);
    }

    public static String toFilterStr(List<String> l) {
        return StringUtils.toString(l,FILTER_SEP);
    }

    /**
     * returns only the parameters used for searching. This excludes all system parameter, including
     * filtering, sorting, and paging.
     * @return
     */
    @NotNull
    public SortedSet<Param> getSearchParams() {
        TreeSet<Param> params = new TreeSet<>();
        for (Param p : getParams()) {
            if (!SYS_PARAMS.contains(p.getName())) {
                params.add(p);
            }
        }
        return params;
    }

    /**
     * returns the parameters used to modified this dataset.  This includes filtering, sorting, and incl_columns.
     * @return
     */
    @NotNull
    public SortedSet<Param> getResultSetParam() {
        TreeSet<Param> params = new TreeSet<>();
        if (filters != null && filters.size() > 0) {
            params.add(new Param(FILTERS, toFilterStr(filters)));
        }
        if (!isEmpty(sqlFilter)) {
            params.add(new Param(SQL_FILTER, sqlFilter));
        }
        if (getSortInfo() != null) {
            params.add(new Param(SORT_INFO, getSortInfo().toString()));
        }
        if (getInclColumns() != null) {
            params.add(new Param(INCL_COLUMNS, getInclColumns()));
        }
        return params;
    }

    @Override
    public ServerRequest cloneRequest() {
        TableServerRequest tsr = (TableServerRequest) super.cloneRequest();
        try {
            tsr.pageSize = pageSize;
            tsr.startIdx = startIdx;
            tsr.filters = filters == null ? null : new ArrayList<>(filters);
            tsr.sqlFilter = sqlFilter;
            tsr.metaInfo = metaInfo == null ? null : new HashMap<>(metaInfo);
            tsr.metaOptions = metaOptions == null ? null : new HashMap<>(metaOptions);
            tsr.selectInfo = selectInfo == null ? null : (SelectionInfo) selectInfo.clone();
        } catch (CloneNotSupportedException e) {
            // ignore.. should not happen
        }
        return tsr;
    }

    public void setJobId(String jobId) { setMeta(JOB_ID, jobId); }

    public String getJobId() { return getMeta(JOB_ID); }

//====================================================================
//
//====================================================================


    protected boolean addPredefinedAttrib(Param param) {
        if (param == null || param.getName() == null || param.getValue() == null) return false;

        switch (param.getName()) {
            case PAGE_SIZE:
                pageSize = StringUtils.getInt(param.getValue());
                break;
            case START_IDX:
                startIdx = StringUtils.getInt(param.getValue());
                break;
            case FILTERS: {
                String s = param.getValue();
                if (s == null || s.trim().length() == 0) {
                    setFilters(null);
                } else {
                    setFilters(parseFilters(s));
                }
                break;
            }
            case SQL_FILTER: {
                setSqlFilter(param.getValue());
                break;
            }
            case META_INFO: {
                String s = param.getValue();
                if (s == null || s.trim().length() == 0) {
                    metaInfo = null;
                } else {
                    metaInfo = StringUtils.encodedStringToMap(s);
                }
                break;
            }
            default:
                return false;
        }
        return true;
    }

//====================================================================
//  inner classes
//====================================================================

}