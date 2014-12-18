package edu.caltech.ipac.firefly.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;

/**
 * Date: Mar 13, 2009
 *
 * @author loi
 * @version $Id: WebUtil.java,v 1.9 2012/08/09 21:54:50 tatianag Exp $
 */
public class WebUtil {

    public enum ParamType {POUND, QUESTION_MARK}
    private static String saveAsIpacUrl = GWT.getModuleBaseURL() + "servlet/SaveAsIpacTable";

    /**
     * Returns a string where all characters that are not valid for a complete URL have been escaped.
     * Also, it will do URL rewriting for session tracking if necessary.
     * Fires SESSION_MISMATCH if the seesion ID on the client is different from the one on the server.
     *
     * @param url    this could be a full or partial url.  Delimiter characters will be preserved.
     * @param params parameters to be appended to the url.  These parameters may contain
     *               delimiter characters.  Unlike url, delimiter characters will be encoded as well.
     * @return encoded url
     */
    public static String encodeUrl(String url, Param... params) {
       return encodeUrl(url, ParamType.QUESTION_MARK,params) ;
    }

    /**
     * Returns a string where all characters that are not valid for a complete URL have been escaped.
     * Also, it will do URL rewriting for session tracking if necessary.
     * Fires SESSION_MISMATCH if the seesion ID on the client is different from the one on the server.
     *
     * @param url    this could be a full or partial url.  Delimiter characters will be preserved.
     * @param paramType  if the the parameters are for the server use QUESTION_MARK if the client use POUND
     * @param params parameters to be appended to the url.  These parameters may contain
     *               delimiter characters.  Unlike url, delimiter characters will be encoded as well.
     * @return encoded url
     */
    public static String encodeUrl(String url, ParamType paramType, Param... params) {

        String paramChar= paramType== ParamType.QUESTION_MARK ? "?": "#";
        String[] parts = url.split("\\"+paramChar, 2);
        String baseUrl = parts[0];
        String queryStr = URL.encode(parts.length == 2 ? parts[1] : "");

        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                Param param = params[i];
                if (param != null && !StringUtils.isEmpty(param.getName())) {
                    String key = URL.encodePathSegment(param.getName().trim());
                    String val = param.getValue() == null ? "" : URL.encodePathSegment(param.getValue().trim());
                    queryStr += val.length() == 0 ? key : key + ServerRequest.KW_VAL_SEP + val + (i < params.length ? "&" : "");
                }
            }
        }
        return URL.encode(baseUrl) + (queryStr.length() == 0 ? "" : paramChar + queryStr);
    }

    public static String encodeUrl(String url, List<Param> paramList) {
        return encodeUrl(url, ParamType.QUESTION_MARK,paramList);
    }

    public static String encodeUrl(String url, ParamType type,List<Param> paramList) {
        return encodeUrl(url, type, paramList.toArray(new Param[paramList.size()]));
    }

    public static String encodeUrl(String url) { return encodeUrl(url, new Param[0]); }

    public static String getTableSourceUrl(TableServerRequest request) {
        request.setStartIndex(0);
        request.setPageSize(Integer.MAX_VALUE);
        Param source = new Param( Request.class.getName(), request.toString());
        String filename = request.getParam("file_name");
        if (StringUtils.isEmpty(filename)) { filename = request.getRequestId(); }
        Param fn = new Param( "file_name", filename);
        return WebUtil.encodeUrl(saveAsIpacUrl, source, fn);

    }
}
