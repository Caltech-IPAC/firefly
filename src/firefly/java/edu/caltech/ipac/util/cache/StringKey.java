/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.cache;

import java.io.Serializable;

/**
 * A quick and convenience way to create a CacheKey from a collection of
 * Objects based on their string representation(toString()).
 * 
 * Date: Jul 18, 2008
 * @author loi
 * @version $Id: StringKey.java,v 1.3 2008/11/13 23:50:36 loi Exp $
 */
public class StringKey implements CacheKey, Comparable, Serializable {
    private String key = "";

    public StringKey() {}

    public StringKey(Object... ids) {
        appendToKey(ids);
    }

    public String getUniqueString() {
        return key.toString();
    }

    public StringKey appendToKey(Object... ids) {
        StringBuffer sb = new StringBuffer(key);
        sb.append(makeKey(ids));
        key = sb.toString();
        return this;
    }

    private String makeKey(Object... ids) {
        StringBuffer sb = new StringBuffer();
        if (ids != null) {
            for(Object o : ids) {
                if (o != null) {
                    if(sb.length() > 0) {
                        sb.append("|");
                    }
                    if (o.getClass().isArray()) {
                        sb.append(makeKey((Object[])o));
                    } else {
                        sb.append(String.valueOf(o));
                    }
                }
            }
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringKey) {
            return key.equals(((StringKey)obj).key);
        }
        return false;
    }

    @Override
    public String toString() {
        return key;
    }

    public int compareTo(Object o) {
        if (o instanceof StringKey) {
            return key.compareTo(((StringKey)o).key);
        }
        return -1;
    }
}
