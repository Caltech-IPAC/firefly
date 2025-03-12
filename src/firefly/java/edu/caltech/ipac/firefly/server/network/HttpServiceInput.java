/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.security.SsoAdapter;
import edu.caltech.ipac.util.KeyVal;

import java.io.File;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 5/23/17
 *
 * @version $Id: $
 */
public class HttpServiceInput implements Cloneable, Serializable {
    private String requestUrl;
    private Map<String, List<String>> params = new HashMap<>();
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

    /**
     * @return a {@code Map<String, String>} where each key maps to a comma-separated string of its values
     */
    public Map<String, String> getParams() {
        return params.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        ent -> String.join(",", ent.getValue())
                ));
    }

    /**
     * This method creates a {@link KeyVal} for each individual key-value pair,
     * so if a key maps to multiple values, there will be multiple {@code KeyVal} entries.
     * @return a list of key-value pairs representing all parameters stored in this input.
     */
    public List<KeyVal<String, String>> getParamPairs() {
        List<KeyVal<String, String>> result = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                result.add(new KeyVal<>(key, value));
            }
        }
        return result;
    }

    /**
     * Sets one or more values for the specified parameter.
     * @param key       the parameter key to set; ignored if {@code null} or empty
     * @param values    the parameter values to associate with the key; can be {@code null} or empty to remove the key
     * @return this {@code HttpServiceInput} instance for method chaining
     */
    public HttpServiceInput setParam(String key, String ...values) {
        if (isEmpty(key)) return this;
        if (values == null || values.length == 0) {
            params.remove(key);
        } else {
            Arrays.stream(values).forEach(v -> addParam(key, v));
        }
        return this;
    }

    /**
     * Adds a value to the parameter list.
     * @param key   the parameter key to set
     * @param value the value to add
     * @return this {@code HttpServiceInput} instance for method chaining
     */
    public HttpServiceInput addParam(String key, String value) {
        if (isEmpty(key)) return this;
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
    public HttpServiceInput setHeader(String key, String value) {
        if (isEmpty(key)) return this;
        if (headers == null) headers = new HashMap<>();
        headers.put(key, value);
        return this;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }
    public HttpServiceInput setCookie(String key, String value) {
        if (isEmpty(key)) return this;
        if (cookies == null) cookies = new HashMap<>();
        cookies.put(key, value);
        return this;
    }

    public Map<String, File> getFiles() {
        return files;
    }
    public HttpServiceInput setFile(String key, File value) {
        if (isEmpty(key)) return this;
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

    /**
     * @return this HttpServiceInput with necessary credentials added
     */
    public HttpServiceInput applyCredential() {
        SsoAdapter ssoAdapter = ServerContext.getRequestOwner().getSsoAdapter();
        if (ssoAdapter != null) {
            ssoAdapter.setAuthCredential(this);
        }
        return this;
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
        return new HttpServiceInput(requestUrl).applyCredential();
    }

    public String getUniqueKey() {
        String key = isEmpty(requestUrl) ? "" : requestUrl;
        String args = ( params  == null ? "" : toKeyString(params) ) +
                    ( headers == null ? ""   : toKeyString(headers) ) +
                    ( cookies == null ? ""   : toKeyString(cookies) ) +
                    ( files == null ? ""     : toKeyString(files) ) +
                    ( isEmpty(userId) ? "" : userId) +
                    ( isEmpty(passwd) ? "" : passwd);
        try {
            return key + (isEmpty(args) ? "" : new String(MessageDigest.getInstance("MD5").digest(args.getBytes())));
        } catch (NoSuchAlgorithmException e) {
            return key + args;
        }
    }

    private static String toKeyString(Map<String,?> map ) {
        return map.entrySet().stream().map((ent) -> ent.getKey()+"="+ent.getValue()).collect(Collectors.joining("|"));
    }

}
