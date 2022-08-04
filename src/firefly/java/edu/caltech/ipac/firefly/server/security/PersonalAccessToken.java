/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.util.AppProperties;

import java.util.Arrays;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 8/1/22
 *
 * @author loi
 * @version : $
 */
public class PersonalAccessToken implements SsoAdapter {

    private static final String ACCESS_TOKEN  = AppProperties.getProperty("sso.access.token");
    private static final String AUTH_HOSTS    = AppProperties.getProperty(REQ_AUTH_HOSTS, "");
    private static final String[] reqAuthHosts;
    private static Token authToken;
    private static UserInfo userInfo;

    static {
        reqAuthHosts = Arrays.stream(AUTH_HOSTS.split(","))
                            .map(String::trim)
                            .toArray(String[]::new);
        if (!isEmpty(ACCESS_TOKEN)) {
            authToken = new Token(ACCESS_TOKEN);
            userInfo = new UserInfo();
            userInfo.setLoginName(ACCESS_TOKEN.substring(0, Math.min(6, ACCESS_TOKEN.length())));
        }
    }

    public Token getAuthToken() { return authToken; }

    public UserInfo getUserInfo() { return userInfo; }

    public void setAuthCredential(HttpServiceInput inputs) {
        Token token = getAuthToken();
        if (token != null && token.getId() != null) {
            if (SsoAdapter.requireAuthCredential(inputs.getRequestUrl(), reqAuthHosts)) {
                inputs.setHeader("Authorization", "Bearer " + token.getId());
            }
        }
    }
}
