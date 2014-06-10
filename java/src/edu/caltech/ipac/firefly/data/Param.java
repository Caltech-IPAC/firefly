package edu.caltech.ipac.firefly.data;

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

    public boolean isKey(String testName) {
        return ComparisonUtil.equals(name,testName);
    }

//====================================================================
//      implements comparable
//====================================================================

    public static Param parse(String str, String separator) {
        String[] kv = str.split(separator, 2);
        if (kv != null && kv.length == 2) {
            return new Param(kv[0], kv[1]);
        }
        return null;
    }


    public static Param parse(String str) {
        return parse(str,ServerRequest.KW_VAL_SEP);
    }

    public String serialize(String separator) {
        String nameStr = name==null ? "" : name;
        String valStr = value==null ? "" : value;

        if (nameStr.length() == 0) {
            return "";
        } else {
            return nameStr + separator + valStr;
        }
    }

    @Override
    public String toString() {
        return serialize(ServerRequest.KW_VAL_SEP);
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
