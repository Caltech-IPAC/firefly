package edu.caltech.ipac.firefly.data.userdata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tatianag
 * @version $Id: UserInfo.java,v 1.15 2012/07/16 23:30:10 loi Exp $
 */
public class UserInfo implements Serializable, Cloneable {

    public static final String GUEST = "Guest";

    public static final String EMAIL = "email";
    public static final String FIRSTNAME = "first_name";
    public static final String LASTNAME = "last_name";
    public static final String ADDRESS = "address1";
    public static final String CITY = "city";
    public static final String COUNTRY = "country";
    public static final String POSTCODE = "postcode";
    public static final String PHONE = "phone_number";
    public static final String INSTITUTE = "institute";
    private HashMap<String, String> props = new HashMap<String, String>();

    private int userId = -1;
    private String loginName;
    private String pass;
    private HashMap<String, String> preferences = new HashMap<String,String>(3);
    private RoleList roles;


    public UserInfo(String loginname, String pass) {
        this.loginName = loginname;
        this.pass = pass;
    }

    public UserInfo() {
        this(null, null);
    }

    public boolean isGuestUser() {
        return loginName == null;
    }

    public int getUserId() { return userId;}
    public void setUserId(int userId) { this.userId = userId; }

    public void setLoginName(String loginName) { this.loginName = loginName; }
    public String getLoginName() { return loginName == null ? GUEST : loginName; }

    public String getPassword() { return pass; }
    public void setPassword(String password) { this.pass = password;}

    public String getName() { return getFirstName()+" "+getLastName(); }

    public void setProperty(String name, String value) {
        props.put(name, value);
    }

    public Map<String, String> getProperties() {
        return props;
    }

    public String getFirstName() {
        return getProperty(FIRSTNAME);
    }

    public void setFirstName(String firstName) {
        setProperty(FIRSTNAME, firstName);
    }

    public String getLastName() {
        return getProperty(LASTNAME);
    }

    public void setLastName(String lastName) {
        setProperty(LASTNAME, lastName);
    }

    public String getEmail() {
        String email = getProperty(EMAIL);
        return email == null ? getLoginName() : email;
    }

    public void setEmail(String email) {
        setProperty(EMAIL, email);
    }

    public String getAddress() {
        return getProperty(ADDRESS);
    }

    public void setAddress(String address) {
        setProperty(ADDRESS, address);
    }

    public String getCity() {
        return getProperty(CITY);
    }

    public void setCity(String city) {
        setProperty(CITY, city);
    }

    public String getCountry() {
        return getProperty(COUNTRY);
    }

    public void setCountry(String country) {
        setProperty(COUNTRY, country);
    }

    public String getPostcode() {
        return getProperty(POSTCODE);
    }

    public void setPostcode(String postcode) {
        setProperty(POSTCODE, postcode);
    }

    public String getPhone() {
        return getProperty(PHONE);
    }

    public void setPhone(String phone) {
        setProperty(PHONE, phone);
    }

    public String getInstitute() {
        return getProperty(INSTITUTE);
    }

    public void setInstitute(String institute) {
        setProperty(INSTITUTE, institute);
    }

    private String getProperty(String name) {
        return props.get(name);
    }

    public void setPreferences(Map<String, String> preferences) {
        this.preferences.clear();
        if (preferences != null) {
            this.preferences.putAll(preferences);
        }
    }

    public Map<String, String> getPreferences() {
        return this.preferences;
    }

    public RoleList getRoles() {
        return roles == null ? new RoleList() : roles;
    }

    public void setRoles(RoleList roles) {
        this.roles = roles;
    }

    public static UserInfo newGuestUser() {
        UserInfo ui = new UserInfo();
        return ui;
    }

    public boolean isSameUser(UserInfo u) {
        if (u != null) {
            return getLoginName().equals(u.getLoginName());
        }
        return false;
    }

    public String toString() {
        String res = "Login name "+ getLoginName()+"\n"+
                    "First name "+ getFirstName()+"\n"+
                    "Last name "+ getLastName()+"\n"+
                    "Email "+getEmail()+"\n"+
                    "Preferences: \n";
            Map<String, String> prefs = getPreferences();
            for (String k : prefs.keySet()) {
                res += "    "+k+" = "+prefs.get(k)+"\n";
            }
        return res;
    }

    /*
     * Clone with the same reference of preferences
     */
    public UserInfo clone() {
        UserInfo clone = new UserInfo(loginName, pass);
        clone.preferences = preferences == null ? null : (HashMap<String, String>) preferences.clone();
        clone.roles = roles == null ? null : (RoleList) roles.clone();
        clone.props = props == null ? null : (HashMap<String, String>) props.clone();
        return clone;
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
