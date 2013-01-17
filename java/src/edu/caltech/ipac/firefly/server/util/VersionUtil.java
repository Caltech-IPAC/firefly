package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.AppProperties;

import javax.servlet.ServletContext;
/**
 * User: roby
 * Date: May 15, 2009
 * Time: 3:20:38 PM
 */


/**
 * @author Trey Roby
 */
public class VersionUtil {

    public static final String MAJOR = "BuildMajor";
    public static final String MINOR = "BuildMinor";
    public static final String REV = "BuildRev";
    public static final String TYPE = "BuildType";
    public static final String BUILD_NUMBER = "BuildNumber";
    public static final String BUILD_DATE  ="BuildDate";
    public static final String APP_NAME  ="AppName";

    private static final Version _version= new Version();

    public static boolean init= false;

    /**
     * Can be call multiple times but will only init the version on the first call.  All other
     * calls are noop.
     * @param context the context that should contain the version, context comes from web.xml
     */
    public static void initVersion(ServletContext context) {
        if (context!=null && !init) {
            init= true;
            ingestVersion(context);
        }
    }

    public static void ingestVersion(ServletContext context) {

        String appName= context.getInitParameter(APP_NAME);
        String major = AppProperties.getProperty(MAJOR);
        String minor = AppProperties.getProperty(MINOR);
        String rev = AppProperties.getProperty(REV);
        String type = AppProperties.getProperty(TYPE);
        String buildNum = AppProperties.getProperty(BUILD_NUMBER);
        String buildDate= AppProperties.getProperty(BUILD_DATE);

        _version.setAppName(appName);
        _version.setMajor(getNum(major));
        _version.setMinor(getNum(minor));
        _version.setRev(getNum(rev));
        _version.setVersionType(Version.convertVersionType(type));
        _version.setBuild(getNum(buildNum));
        _version.setBuildDate(buildDate);
        _version.setConfigLastModTime(ServerContext.getConfigLastModTime());
    }

    public static Version getAppVersion() { return _version;  }

    private static int getNum(String s) {
        int retval;
        try {
            retval= Integer.parseInt(s);
        } catch (NumberFormatException e) {
            retval= 0;
        }
        return retval;
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
