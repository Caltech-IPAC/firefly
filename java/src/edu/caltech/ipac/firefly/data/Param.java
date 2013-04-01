package edu.caltech.ipac.firefly.data;

import com.google.gwt.http.client.URL;
import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;
/**
 * User: roby
 * Date: Apr 7, 2010
 * Time: 4:58:59 PM
 */


/**
 * @author Trey Roby
*/
public class Param implements Comparable, Serializable {

    private String name;
    private String value;

    public Param() {
    }

    public Param(String name, String value) {
        setName(name);
        setValue(value);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name= name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value= value;
    }

//====================================================================
//      implements comparable
//====================================================================
    public static Param parse(String str) {
        String[] kv = str.split(ServerRequest.KW_VAL_SEP, 2);
        if (kv != null && kv.length == 2) {
            return new Param(kv[0], kv[1]);
        }
        return null;
    }

    @Override
    public String toString() {
        return getString(ServerRequest.KW_VAL_SEP, false);
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public boolean equals(Object obj) {

        boolean retval;
        if (this==obj) {
            retval= true;
        }
        else {
            retval= false;
            if (obj instanceof Param) {
                Param p= (Param)obj;
                retval= ComparisonUtil.equals(this.getName(),p.getName());
            }
        }
        return retval;
    }

    public String getString(String separator) {
        return getString(separator, false);
    }

    public String getString(String separator, boolean doEncoding) {
        String nameStr = name==null ? "" : name;
        String valStr = value==null ? "" : value;

        if (doEncoding) {
            nameStr = URL.encodePathSegment(nameStr);
            valStr = URL.encodePathSegment(valStr);
        }

        if (nameStr.length() == 0) {
            return "";
        } else {
            return nameStr + separator + valStr;
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
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
