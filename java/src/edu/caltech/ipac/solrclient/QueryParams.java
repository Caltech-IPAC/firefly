package edu.caltech.ipac.solrclient;

import edu.caltech.ipac.util.CollectionUtil;
import org.apache.solr.client.solrj.SolrQuery;

import java.util.Arrays;
import java.util.List;

/**
 * Date: Jul 20, 2009
 *
 * @author loi
 * @version $Id: QueryParams.java,v 1.3 2009/08/06 00:24:48 loi Exp $
 */
public class QueryParams {
    public static enum RequestHandler { standard, dismax };


    private boolean includeScore = false;
    private boolean highlight = true;
    private String highlightPre = "<em>";
    private String highlightPost = "</em>";
    private List<String> highlightFields;
    private RequestHandler requestHandler = RequestHandler.dismax;
    private List<String> fieldBoostInfo = Arrays.asList("text^1.0",  "id^5.0");
    private List<String> queryFields;
    private String queryString;
    private String minMatch = "1";
    private int fragmentSize = 0;
    private String doctype = null;

    public QueryParams(String queryString) {
        this.queryString = queryString;
    }

    void setupQuery(SolrQuery query) {

        if (isHighlight()) {
            query.setHighlight(isHighlight());
            if (fragmentSize >= 0) {
                query.setHighlightFragsize(fragmentSize);
            }
            query.setParam("f.text.hl.fragmenter", "reqex");
            query.setHighlightSimplePost(getHighlightPost());
            query.setHighlightSimplePre(getHighlightPre());
            query.setHighlightRequireFieldMatch(true);
            query.setHighlightSnippets(5);
            if (highlightFields != null && highlightFields.size() > 0) {
                query.setParam("hl.fl", CollectionUtil.toString(highlightFields, " "));
            }
        }
        query.setIncludeScore(isIncludeScore());
        query.setParam("defType", requestHandler.name());
        if (fieldBoostInfo != null && fieldBoostInfo.size() > 0) {
            query.setParam("qf", CollectionUtil.toString(fieldBoostInfo, " "));
            query.setParam("pf", CollectionUtil.toString(fieldBoostInfo, " "));
        }
        query.setParam("mm", minMatch);
        if (queryFields != null && queryFields.size() > 0) {
            query.setFields(CollectionUtil.toString(queryFields, " "));
        }
        query.setRows(Integer.MAX_VALUE);
        query.setQuery(getQueryString());

        if (doctype != null) {
            query.setParam("fq", "doctype:" + doctype);
        }

    }

    public String getDoctype() {
        return doctype;
    }

    public void setDoctype(String doctype) {
        this.doctype = doctype;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public boolean isIncludeScore() {
        return includeScore;
    }

    public void setIncludeScore(boolean includeScore) {
        this.includeScore = includeScore;
    }

    public String getMinMatch() {
        return minMatch;
    }

    public int getFragmentSize() {
        return fragmentSize;
    }

    public void setFragmentSize(int fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    /**
     * Set the minimum number of words must match.  Defaults to 1.
     * Specifying this minimum number can be done in complex ways, equating to ideas like...
     * <li> At least 2 of the optional clauses must match, regardless of how many clauses there are: "2"
     * <li> At least 75% of the optional clauses must match, rounded down: "75%"
     * <li> If there are less than 3 optional clauses, they all must match; if there are 3 or more, then 75% must match, rounded up: "2<-25%"
     * <li> If there are less than 3 optional clauses, they all must match; for 3 to 5 clauses, one less than the number of clauses must match, for 6 or more clauses, 80% must match, rounded down: "2<-1 5<80%"
     * @param minMatch
     */
    public void setMinMatch(String minMatch) {
        this.minMatch = minMatch;
    }

    public boolean isHighlight() {
        return highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public String getHighlightPre() {
        return highlightPre;
    }

    public void setHighlightPre(String highlightPre) {
        this.highlightPre = highlightPre;
    }

    public String getHighlightPost() {
        return highlightPost;
    }

    public void setHighlightPost(String highlightPost) {
        this.highlightPost = highlightPost;
    }

    public List<String> getHighlightFields() {
        return highlightFields;
    }

    public void setHighlightFields(List<String> highlightFields) {
        this.highlightFields = highlightFields;
    }

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    public List<String> getQueryFields() {
        return queryFields;
    }

    public void setQueryFields(List<String> queryFields) {
        this.queryFields = queryFields;
    }

    public List<String> getFieldBoostInfo() {
        return fieldBoostInfo;
    }

    public void setFieldBoostInfo(List<String> fieldBoostInfo) {
        this.fieldBoostInfo = fieldBoostInfo;
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
