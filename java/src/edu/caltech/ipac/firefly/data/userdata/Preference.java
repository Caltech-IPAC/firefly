/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.userdata;

import java.io.Serializable;

/**
 * @author tatianag
 * @version $Id: Preference.java,v 1.3 2010/04/06 00:30:03 loi Exp $
 */
public class Preference implements Serializable {
    /** User ID */
    private String loginName;
    /** preference name */
    private String prefname;
    /** preference value */
    private String prefvalue;

    public Preference(String prefname, String prefvalue) {
        this(prefname, prefvalue, "unknown");
    }

    public Preference(String prefname, String prefvalue, String loginName) {
        this.prefname = prefname;
        this.prefvalue = prefvalue;
        this.loginName = loginName;
    }

    public Preference() {
    }

    public String getLoginName() { return loginName; }
    public String getPrefname() { return prefname; }
    public String getPrefvalue() { return prefvalue; }

    public void setLoginName(String loginName) { this.loginName = loginName; }
    public void setPrefname(String prefname) { this.prefname = prefname; }
    public void setPrefvalue(String prefvalue) { this.prefvalue = prefvalue; }

    /*
     * Business key equality - for hibernate
     */
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Preference)) return false;

        final Preference pref = (Preference)other;
        return (pref.getLoginName()== getLoginName()) && pref.getPrefname().equals(getPrefname());
    }

    public int hashCode() {
        return (getLoginName()+getPrefname()).hashCode();
    }

    public String toString() {
        return "Login name "+ getLoginName()+"\n"+
                "Pref name "+getPrefname()+"\n"+
                "Pref value "+getPrefvalue()+"\n";
    }
}
