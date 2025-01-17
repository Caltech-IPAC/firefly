/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static edu.caltech.ipac.util.StringUtils.getInt;

/**
 * A helper class designed to help simplify the process of JSON to object mapping.
 * This mimic the lodash's get/set behavior.  {@link JsonHelper#getValue(Object, String...)}} will
 * return the value at the given paths, or the given default is paths does not exist.
 * {@link JsonHelper#setValue(Object, String...)} will set the given value at the given paths.
 * Along the paths, if it does not exists, a Map or ArrayList will be created depending its path type.
 * When the path is an integer, it will assume it's an index to an ArrayList.  Otherwise, it's a key to a Map.
 *
 * Date: 2019-03-15
 *
 * @author loi
 * @version $Id: $
 */
public class JsonHelper {

    private Object root;

    public JsonHelper() {}

    public JsonHelper(Object root) {
        this.root = root;
    }


    public static JsonHelper parse(String json) {
        try {
            return json == null ? new JsonHelper() : new JsonHelper(new JSONParser().parse(json));
        } catch (ParseException e) {
            throw new IllegalArgumentException("The given string is not in JSON format:" + (json.length() < 100 ? json : json.substring(0, 99) + "..."));
        }
    }

    public String toJson() {
        return JSONValue.toJSONString(root);
    }

    /**
     * convenience method to get a value using a path.
     * @param def   default value if paths does not exists
     * @param paths
     * @param <T>
     * @return
     * @throws ClassCastException
     */
    public <T> T getValue(T def, String... paths) throws ClassCastException {
        if (paths == null || paths.length == 0) {
            return root == null ? def : (T) root;
        }
        Object current = root;
        for (String p : paths) {
            if (current == null) return def;
            if (current instanceof List) {
                int idx = getInt(p, -1);
                current = idx < 0 ? null : ((List) current).get(idx);
            } else if(current instanceof Map) {
                current = ((Map)current).get(p);
            }
        }
        if (current==null) return def;
        return (T) current;
    }

    /**
     * convenience method to set a value using a path.
     * this will create new node along the path if one does not exists.
     * @param value
     * @param paths
     */
    public JsonHelper setValue(Object value, String... paths) {
        if (paths == null || paths.length == 0) {
            this.root = value;
        } else {
            getContainer(paths).accept(value);
        }
        return this;
    }

    public JsonHelper setValueFromPath(Object value, String path, String pathDelim) {
        return setValue(value, path.split(pathDelim));
    }

    private Consumer getContainer(String[] paths) {

        Object cCont = root = ensureCont(root, paths[0]);;
        String nkey = paths[0];
        for(int i =0; i < paths.length-1; i++) {
            String ckey = paths[i];
            nkey = i+1 >= paths.length ? null : paths[i+1];
            cCont = getCont(cCont, ckey, nkey);
        }

        if (cCont instanceof List) {
            List alist = (List) cCont;
            int idx = getInt(nkey, -1);
            return (v) -> alist.set(idx, v);
        } else if (cCont instanceof Map) {
            Map amap = (Map) cCont;
            String finalkey = nkey;
            return (v) -> amap.put(finalkey, v);
        }
        throw new IndexOutOfBoundsException("The specify paths does not contain a known container.");
    }

    private Object ensureCont(Object cont, String key) {
        int idx = getInt(key, -1);
        if (cont == null) {
            cont = idx == -1 ? new HashMap() :new ArrayList();
        }
        if (idx >= 0 && cont instanceof List)  cont = ensureList((List) cont, idx);
        return cont;
    }

    private Object getCont(Object source, String key, String nkey) {

        if (source instanceof List) {
            List clist = (List) source;
            int idx = getInt(key, -1);
            ensureList(clist, idx);
            Object cont = clist.get(idx);
            if (cont == null) {
                cont = ensureCont(cont, nkey);
                clist.set(idx, cont);
            } else {
                cont = ensureCont(cont, nkey);
            }
            return cont;
        } else if (source instanceof Map) {
            Map cmap = (Map) source;
            Object cont = cmap.get(key);
            if (cont == null) {
                cont = ensureCont(cont, nkey);
                cmap.put(key, cont);
            } else {
                cont = ensureCont(cont, nkey);
            }
            return cont;
        }
        return null;
    }

    private List ensureList(List clist, int key) {
        if (clist.size() <= key) {
            for (int i = clist.size(); i <= key; i++) clist.add(i, null);
        }
        return clist;
    }
}

