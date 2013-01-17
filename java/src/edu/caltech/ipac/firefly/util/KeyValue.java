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
