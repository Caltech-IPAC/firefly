package edu.caltech.ipac.firefly.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Cookies;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

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

        // do url rewriting if necessary
        LoginManager loginManager = Application.getInstance().getLoginManager();
        if (loginManager != null && paramType==ParamType.QUESTION_MARK) {
            String sessId = Cookies.getCookie("JSESSIONID");
            String appSessId = loginManager.getSessionId();
            if (sessId == null || sessId.trim().length() == 0) {
                baseUrl += appSessId == null ? "" : ";jsessionid=" + appSessId;
            } else {
                if (!sessId.equals(appSessId)) {
                    WebEventManager.getAppEvManager().fireEvent(new WebEvent(WebUtil.class, Name.SESSION_MISMATCH, sessId));
                }
            }
        }

        if (params != null && params.length > 0) {
            for (Param param : params) {
                queryStr += param.getString("=", true) + "&";
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
        Param fn = new Param( "file_name", request.getRequestId());
        return WebUtil.encodeUrl(saveAsIpacUrl, source, fn);

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
