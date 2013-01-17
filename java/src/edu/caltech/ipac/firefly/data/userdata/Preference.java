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
