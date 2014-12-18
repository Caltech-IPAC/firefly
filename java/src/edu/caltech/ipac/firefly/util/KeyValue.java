package edu.caltech.ipac.firefly.util;

import com.google.gwt.http.client.URL;

/**
 * Date: Mar 13, 2009
 *
 * @author loi
 * @version $Id: KeyValue.java,v 1.2 2009/04/03 23:51:56 loi Exp $
 */
public class KeyValue <Key, Value> implements Comparable {
    private Key key;
    private Value value;

    public KeyValue(Key key, Value value) {
        this.key = key;
        this.value = value;
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getString("=");
    }

    public String getString(String separator) {
        return getString(separator, false);
    }

    public String getString(String separator, boolean doEncoding) {
        String keyStr = key == null ? "" : String.valueOf(key).trim();
        String valStr = value == null ? "" : String.valueOf(value).trim();
        
        if (doEncoding) {
            keyStr = URL.encodeComponent(keyStr);
            valStr = URL.encodeComponent(valStr);
        }

        if (keyStr.length() == 0) {
            return "";
        } else {
            return keyStr + separator + valStr;
        }
    }

    public int compareTo(Object o) {
        return toString().compareTo(String.valueOf(o));
    }
}
