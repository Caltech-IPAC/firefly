/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Date: 5/23/17
 *
 * @version $Id: $
 */
public class HttpServiceInput {
    private Map<String, String> params;
    private Map<String, String> headers;
    private Map<String, String> cookies;
    private Map<String, File> files;
    private String userId;
    private String passwd;

    public String getUserId() {
        return userId;
    }

    public String getPasswd() {
        return passwd;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public Map<String, File> getFiles() {
        return files;
    }


    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public HttpServiceInput setParam(String key, String value) {
        if (StringUtils.isEmpty(key)) return this;
        if (params == null) params = new HashMap<>();
        params.put(key, value);
        return this;
    }

    public HttpServiceInput setHeader(String key, String value) {
        if (StringUtils.isEmpty(key)) return this;
        if (headers == null) headers = new HashMap<>();
        headers.put(key, value);
        return this;
    }

    public HttpServiceInput setCookie(String key, String value) {
        if (StringUtils.isEmpty(key)) return this;
        if (cookies == null) cookies = new HashMap<>();
        cookies.put(key, value);
        return this;
    }

    public HttpServiceInput setFile(String key, File value) {
        if (StringUtils.isEmpty(key)) return this;
        if (files == null) files = new HashMap<>();
        files.put(key, value);
        return this;
    }

    public String getDesc() {
        StringBuilder sb = new StringBuilder();

        if (userId != null) {
            sb.append("userId: ").append(userId)
                .append("  passwd:").append(passwd).append("\n");
        }

        if (params != null) {
            sb.append("params: ").append(params.toString()).append("\n");
        }
        if (headers != null) {
            sb.append("headers: ").append(headers.toString()).append("\n");
        }
        if (cookies != null) {
            sb.append("cookies: ").append(cookies.toString()).append("\n");
        }
        if (files != null) {
            sb.append("files: ").append(files.toString()).append("\n");
        }
        return sb.toString();
    }

}
