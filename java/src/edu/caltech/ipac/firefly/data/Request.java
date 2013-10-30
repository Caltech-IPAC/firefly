package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;

public class Request extends TableServerRequest implements Serializable, Cloneable {

    public static final String DO_SEARCH = "DoSearch";
    public static final String SHORT_DESC = "shortDesc";
    public static final String BOOKMARKABLE = "isBookmarkAble";
    public static final String DRILLDOWN = "isDrillDownAble";
    public static final String DRILLDOWN_ROOT = "isDrillDownRoot";
    public static final String SEARCH_RESULT = "isSearchResult";
    private static final String SYS_PARAMS = "|" + StringUtils.toString(new String[]{DO_SEARCH,SHORT_DESC,BOOKMARKABLE,DRILLDOWN,DRILLDOWN_ROOT,SEARCH_RESULT}, "|") + "|";

    private String shortDesc;
    private boolean isBookmarkable;
    private boolean isDrillDown;
    private boolean isDrillDownRoot;
    private boolean isSearchResult;

    private transient Status status;

    public Request() {
        this(null);
    }
    
    public Request(String cmdName) {
        this(cmdName, false, false);
    }

    public Request(String cmdName, boolean bookmarkable, boolean drilldown) {
        this(String.valueOf(System.currentTimeMillis()), cmdName, null, bookmarkable, drilldown);
    }

    public Request(String cmdName, String shortDesc, boolean bookmarkable, boolean drilldown) {
        this(String.valueOf(System.currentTimeMillis()), cmdName, shortDesc, bookmarkable, drilldown);
    }

    public Request(String id, String cmdName, String shortDesc, boolean bookmarkable, boolean drilldown) {
        setRequestId(id);
        setCmdName(cmdName);
        setShortDesc(shortDesc);
        setBookmarkable(bookmarkable);
        setIsDrilldown(drilldown);
        setIsSearchResult(false);
    }

    @Override
    public boolean isInputParam(String paramName) {
        return !SYS_PARAMS.contains("|" + paramName + "|") && super.isInputParam(paramName);
    }

    public boolean isDoSearch() {
        return getBooleanParam(DO_SEARCH);
    }

    public void setDoSearch(boolean doSearch) {
        setParam(DO_SEARCH, String.valueOf(doSearch));
    }

    public boolean isBookmarkable() {
        return isBookmarkable;
    }

    public void setBookmarkable(boolean bookmarkable) {
        isBookmarkable = bookmarkable;
    }

    public boolean isDrilldown() {
        return isDrillDown;
    }

    public void setIsDrilldown(boolean isDrilldown) {
        this.isDrillDown = isDrilldown;
    }

    public boolean isSearchResult() {
        return isSearchResult;
    }

    public void setIsSearchResult(boolean isSearchResult) {
        this.isSearchResult = isSearchResult;
    }

    public boolean isDrilldownRoot() {
        return isDrillDownRoot;
    }

    public void setIsDrilldownRoot(boolean isDrilldownRoot) {
        this.isDrillDownRoot = isDrilldownRoot;
    }

    /**
     * Parses the string argument into a Request object.
     * This method is reciprocal to toString().
     * @param str
     * @return
     */
    public static Request parse(String str) {
        return ServerRequest.parse(str, new Request());
	}

    @Override
    public TableServerRequest newInstance() {
        return new Request();
    }

    @Override
    public void copyFrom(ServerRequest req) {
        super.copyFrom(req);
        if (req instanceof Request) {
            Request rreq = (Request) req;
            shortDesc = rreq.shortDesc;
            isBookmarkable = rreq.isBookmarkable;
            isDrillDown = rreq.isDrillDown;
            isDrillDownRoot = rreq.isDrillDownRoot;
            isSearchResult = rreq.isSearchResult;
            status = rreq.status;    
        }
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer(super.toString());

        if (!StringUtils.isEmpty(shortDesc)) {
            addParam(str, SHORT_DESC, String.valueOf(shortDesc));
        }
        if (isBookmarkable) {
            addParam(str, BOOKMARKABLE, String.valueOf(isBookmarkable));
        }
        if (isDrillDown) {
            addParam(str, DRILLDOWN, String.valueOf(isDrillDown));
        }
        if (isDrillDownRoot) {
            addParam(str, DRILLDOWN_ROOT, String.valueOf(isDrillDownRoot));
        }
        if (isSearchResult) {
            addParam(str, SEARCH_RESULT, String.valueOf(isSearchResult));
        }
        return str.toString();
    }


    @Override
    protected boolean addPredefinedAttrib(Param param) {
        boolean added= super.addPredefinedAttrib(param);
        if (!added) {
            added= true;
            if (param == null || param.getName() == null || param.getValue() == null) return false;

            if (param.getName().equals(SHORT_DESC)) {
                shortDesc = param.getValue();
            } else if (param.getName().equals(BOOKMARKABLE)) {
                isBookmarkable = StringUtils.getBoolean(param.getValue());
            } else if (param.getName().equals(DRILLDOWN)) {
                isDrillDown = StringUtils.getBoolean(param.getValue());
            } else if (param.getName().equals(DRILLDOWN_ROOT)) {
                isDrillDownRoot = StringUtils.getBoolean(param.getValue());
            } else if (param.getName().equals(SEARCH_RESULT)) {
                isSearchResult = StringUtils.getBoolean(param.getValue());
            }
            else {
                added= false;
            }
        }
        return added;
    }

//====================================================================
//
//====================================================================

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getCmdName() {
        return getRequestId();
    }

    public void setCmdName(String cmdName) {
        setRequestId(cmdName);
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public Request makeCopy() {
        Request req = new Request();
        req.status = status;
        req.copyFrom(this);
        req.setRequestId(getRequestId());

        return req;
    }
}