/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;

/**
 * User: roby
 * Date: May 13, 2009
 * Time: 1:49:23 PM
 */


/**
 * @author Trey Roby
 */
public class Version implements Serializable, HandSerialize {
    private final static String SPLIT_TOKEN= "--Version--";

    public enum VersionType {Final, Development,
                             Alpha, Beta, RC, Unknown}

    private String      _appName= "Web App";
    private int         _major= 0;
    private int         _minor= 0;
    private int         _rev= 0;
    private VersionType _vType= VersionType.Unknown;
    private int         _build = 0;
    private String      _buildDate= "Don't Know";
    private long        _configLastModTime = 0;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public Version() {  }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public long getConfigLastModTime() { return _configLastModTime;}
    public void setConfigLastModTime(long configLastModTime) {_configLastModTime = configLastModTime;}
    public void setAppName(String name) { _appName= name; }
    public void setMajor(int m) { _major= m;}
    public void setMinor(int m) { _minor= m;}
    public void setVersionType(VersionType vt) { _vType= vt; }
    public void setBuildDate(String date) { _buildDate= date;}
    public void setRev(int rev) { _rev = rev;  }

    /**
     * The build number is only use if type is not final.  Finals should not have a build number
     * @param build build number, not used with final
     */
    public void setBuild(int build) { _build = build; }



    public int getMajor() { return _major;}
    public int getMinor() { return _minor;}
    public int getRev() { return _rev;}
    public VersionType getVersionType() { return _vType; }
    public String getBuildDate() { return _buildDate; }
    public int getBuildNumber() { return _build;  }
    public String getAppName() { return _appName; }

    @Override
    public String toString() {
        String retval = _major + "." + _minor;

        if (_rev > 0) {
            retval += "." + _rev;
        }
        retval += " " + convertVersionType(_vType);

        if (_vType!=VersionType.Final && _build > 0) {
            retval += "-" + _build;
        }

        retval +=  ", Built On:" + _buildDate;
        return retval;
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    public static VersionType convertVersionType(String vStr) {
        Version.VersionType retval;
        if (vStr.equalsIgnoreCase("Final")) {
            retval= Version.VersionType.Final;
        }
        else if (vStr.equalsIgnoreCase("Development")) {
            retval= Version.VersionType.Development;
        }
        else if (vStr.equalsIgnoreCase("Alpha")) {
            retval= Version.VersionType.Alpha;
        }
        else if (vStr.equalsIgnoreCase("Beta")) {
            retval= Version.VersionType.Beta;
        }
        else if (vStr.equalsIgnoreCase("RC")) {
            retval= Version.VersionType.RC;
        }
        else {
            retval= Version.VersionType.Unknown;
        }
        return retval;
    }

    public static String convertVersionType(Version.VersionType vt) {
        String  retval;
        if (vt== Version.VersionType.Final) {
            retval= "Final";
        }
        else if (vt== Version.VersionType.Development) {
            retval= "Development";
        }
        else if (vt== Version.VersionType.Alpha) {
            retval= "Alpha";
        }
        else if (vt== Version.VersionType.Beta) {
            retval= "Beta";
        }
        else if (vt== Version.VersionType.RC) {
            retval= "RC";
        }
        else if (vt== Version.VersionType.Unknown) {
            retval= "Unknown";
        }
        else {
            retval= "Unknown";
        }
        return retval;
    }

    public String serialize() {
        return StringUtils.combine(SPLIT_TOKEN,
                                   _appName,
                                   _major+"",
                                   _minor+"",
                                   _rev+"",
                                   _vType.toString(),
                                   _build+"",
                                   _buildDate,
                                   _configLastModTime+"");
    }

    public static Version parse(String s) {
        Version v= new Version();
        try {
            String sAry[]= StringUtils.parseHelper(s,12,SPLIT_TOKEN);
            int i= 0;
            v._appName= StringUtils.checkNull(sAry[i++]);
            v._major= StringUtils.getInt(sAry[i++]);
            v._minor= StringUtils.getInt(sAry[i++]);
            v._rev= StringUtils.getInt(sAry[i++]);
            v._vType= convertVersionType(sAry[i++]);
            v._build= StringUtils.getInt(sAry[i++]);
            v._buildDate= StringUtils.checkNull(sAry[i++]);
            v._configLastModTime= StringUtils.getLong(sAry[i++]);
        } catch (IllegalArgumentException e) {
            v= null;
        }
        return v;
    }
}

