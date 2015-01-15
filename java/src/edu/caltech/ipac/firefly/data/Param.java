/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

