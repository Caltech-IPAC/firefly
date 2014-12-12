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
