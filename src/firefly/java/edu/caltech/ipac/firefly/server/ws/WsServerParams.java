package edu.caltech.ipac.firefly.server.ws;

import edu.caltech.ipac.firefly.server.SrvParam;

import java.util.HashMap;
import java.util.Map;

/**
 * Defined WS params to be attached to request from client
 */
public class WsServerParams {
    private final HashMap<String, String> map;

    public enum WS_SERVER_PARAMS {
        SHOULD_OVERWRITE, /* overwrite enable flag */
        CURRENTRELPATH, /* relative path / parent on which the action should take place */
        NEWPATH; /* use for uploading or renaming file */

        private final String key;

        WS_SERVER_PARAMS() {
            this.key = this.name().toLowerCase();
        }

        public String getKey() {
            return key;
        }
    }

    WsServerParams() {
        this.map = new HashMap<String, String>();
    }

    Map<String, String> getMap() {
        return this.map;
    }

    /**
     * @return current value of the relative file path
     */
    public String getRelPath() {
        return this.map.get(WS_SERVER_PARAMS.CURRENTRELPATH.getKey());
    }

    /**
     * @return new path
     */
    public String getNewPath() {
        return this.map.get(WS_SERVER_PARAMS.NEWPATH.getKey());
    }

    /**
     * @return false if param is not set to any value, otherwise return boolean value of the string
     */
    public boolean shouldOverwrite() {
        String b = this.map.get(WS_SERVER_PARAMS.SHOULD_OVERWRITE.getKey());
        return b != null ? Boolean.parseBoolean(b.toLowerCase().trim()) : false;
    }

    public void set(WS_SERVER_PARAMS p, String val) {
        map.put(p.getKey(), val);
    }

}