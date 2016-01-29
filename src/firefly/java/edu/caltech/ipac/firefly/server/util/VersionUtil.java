/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
     * Can be called multiple times but will only init the version on the first call.  All other
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


