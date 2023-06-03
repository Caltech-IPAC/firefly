/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.security.SsoAdapter;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 5/23/17
 *
 * @version $Id: $
 */
public class HttpServiceInput implements Cloneable, Serializable {
    private String requestUrl;
    private Map<String, String> params;
    private Map<String, String> headers;
    private Map<String, String> cookies;
    private Map<String, File> files;
    private String userId;
    private String passwd;
    private boolean followRedirect = true;

    public HttpServiceInput() {}

    public HttpServiceInput(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestUrl() {
        return requestUrl;
    }
    public HttpServiceInput setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
        return this;
    }

    public String getUserId() {
        return userId;
    }
    public HttpServiceInput setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getPasswd() {
        return passwd;
    }
    public HttpServiceInput setPasswd(String passwd) {
        this.passwd = passwd;
        return this;
    }

    public Map<String, String> getParams() {
        return params;
    }
    public HttpServiceInput setParam(String key, String value) {
        if (StringUtils.isEmpty(key)) return this;
        if (params == null) params = new HashMap<>();
        params.put(key, value);
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
    public HttpServiceInput setHeader(String key, String value) {
        if (StringUtils.isEmpty(key)) return this;
        if (headers == null) headers = new HashMap<>();
        headers.put(key, value);
        return this;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }
    public HttpServiceInput setCookie(String key, String value) {
        if (StringUtils.isEmpty(key)) return this;
        if (cookies == null) cookies = new HashMap<>();
        cookies.put(key, value);
        return this;
    }

    public Map<String, File> getFiles() {
        return files;
    }
    public HttpServiceInput setFile(String key, File value) {
        if (StringUtils.isEmpty(key)) return this;
        if (files == null) files = new HashMap<>();
        files.put(key, value);
        return this;
    }

    public boolean isFollowRedirect() { return followRedirect; }
    public HttpServiceInput setFollowRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
        return this;
    }

    public String getDesc() {
        StringBuilder sb = new StringBuilder();

        if (userId != null) {
            sb.append("\n\tuserId: ").append(userId)
                .append("  passwd:").append(passwd);
        }

        if (params != null) {
            sb.append("\n\tparams: ").append(params.toString());
        }
        if (headers != null) {
            sb.append("\n\theaders: ").append(headers.toString());
        }
        if (cookies != null) {
            sb.append("\n\tcookies: ").append(cookies.toString());
        }
        if (files != null) {
            sb.append("\n\tfiles: ").append(files.toString());
        }
        return sb.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        HttpServiceInput input = (HttpServiceInput) super.clone();
        input.params = params == null ? null : new HashMap<>(params);
        input.headers = headers == null ? null : new HashMap<>(headers);
        input.cookies = cookies == null ? null : new HashMap<>(cookies);
        input.files = files == null ? null : new HashMap<>(files);

        return input;
    }

    /**
     * @return a clone of this input.. like clone except it'll return the right type without thrown exceptions.
     */
    public HttpServiceInput copy() {
        try {
            return (HttpServiceInput) clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

//====================================================================
//  convenience functions
//====================================================================

    @Deprecated     // does not make sense because credential should only be passed to a known backend service(url)
    public static HttpServiceInput createWithCredential() {
        return createWithCredential(null);
    }

    /**
     * returns an HttpServiceInput that contains the required credential to access the given backend service.
     * This credential information is based on the implementer of SsoAdapter.
     * @param requestUrl  URL to access
     * @return
     */
    public static HttpServiceInput createWithCredential(String requestUrl) {
        return applyCredential(new HttpServiceInput(requestUrl));
    }

    /**
     * Include the necessary credentials based on the given input.
     * @param input  HTTP request input
     * @return
     */
    public static HttpServiceInput applyCredential(HttpServiceInput input) {
        // Credential is normally based on the requestUrl.  It does not make sense to pass in a null input.
        // A null input will most likely results in no credential added.
        input = input == null ? new HttpServiceInput() : input;
        SsoAdapter ssoAdapter = ServerContext.getRequestOwner().getSsoAdapter();
        if (ssoAdapter != null) {
            ssoAdapter.setAuthCredential(input);
        }
        return input;
    }

}
