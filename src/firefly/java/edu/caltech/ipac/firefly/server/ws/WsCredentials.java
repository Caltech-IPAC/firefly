package edu.caltech.ipac.firefly.server.ws;

import java.util.Map;

/**
 * Created by ejoliet on 6/19/17.
 */
public class WsCredentials {

    private String wsId;
    private Map<String, String> cookies;
    private String password;

    public Map<String, String> getCookies() {
        return cookies;
    }

    public String getWsId() {
        return wsId;
    }

    public String getPassword() {
        return this.password;
    }

    enum AUTH {NONE, TOKEN, DIGEST, BASIC}

    public WsCredentials(String wsId, Map<String, String> cookies) {
        this.wsId = wsId;
        this.cookies = cookies;
    }

    public WsCredentials(String wsId, String pass) {
        this.wsId = wsId;
        this.password = pass;
        this.cookies = null;
    }

    public WsCredentials(String userKey) {
        this.wsId = userKey;
        this.cookies = null;
    }
}
