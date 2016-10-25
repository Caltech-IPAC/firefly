package edu.caltech.ipac.firefly.server.events;

import org.json.simple.JSONObject;
import java.util.Arrays;

/**
 * Date: 1/27/16
 *
 * @author loi
 * @version $Id: $
 */
public class FluxAction {
    public static final String TYPE = "type";
    public static final String PAYLOAD = "payload";


    private JSONObject root;

    public FluxAction(String type) {
        this(type, new JSONObject());
    }

    public FluxAction(String type , JSONObject payload) {
        root = new JSONObject();
        root.put(TYPE, type);
        root.put(PAYLOAD, payload);
    }

    public String getType() {
        return String.valueOf(root.get(TYPE));
    }

    public void setType(String type) {
        root.put(TYPE, type);
    }

    public JSONObject getPayload() {
        return  (JSONObject) root.get(PAYLOAD);
    }

    /**
     * convenience method to set a value using a path.
     * this will create new node along the path if one does not exists.
     * @param value
     * @param path
     */
    public void setValue(Object value, String... path) {

        if (path == null || path.length == 0) {
            throw new IllegalArgumentException("path may not be null");
        }
        String[] npath = path.length == 1 ? null : Arrays.copyOfRange(path, 0, path.length - 1);
        getNode(npath).put(path[path.length-1], value);
    }

    /**
     * returns a JSONObject (map) for the given path.
     * This will create new node along the path if one does not exists.
     * @param path
     * @return
     */
    public JSONObject getNode(String... path) {
        if (path == null || path.length == 0) {
            return getPayload();
        } else {
            JSONObject current = getPayload();
            for(String p : path) {
                JSONObject next = (JSONObject) current.get(p);
                if (next == null) {
                    next = new JSONObject();
                    current.put(p, next);
                }
                current = next;
            }
            return current;
        }
    }

    @Override
    public String toString() {
        return root.toJSONString();
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
