/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * User: roby
 * Date: May 15, 2009
 * Time: 3:20:38 PM
 */


/**
 * @author Trey Roby
 */
public class VersionUtil {

    private static final String MAJOR = "BuildMajor";
    private static final String MINOR = "BuildMinor";
    private static final String REV = "BuildRev";
    private static final String TYPE = "BuildType";
    private static final String BUILD_NUMBER = "BuildNumber";
    private static final String BUILD_DATE  ="BuildDate";
    private static final String BUILD_TIME  ="BuildTime";
    private static final String BUILD_TAG  ="BuildTag";
    private static final String BUILD_COMMIT  ="BuildCommit";
    private static final String BUILD_COMMIT_FIREFLY  ="BuildCommitFirefly";
    private static final String BUILD_GIT_TAG_FIREFLY ="BuildGitTagFirefly";

    private static final String VERSION_FILE = "version.tag";
    private static final String unknownVersionUA= "Firefly/development";

    private static final Version _version= new Version();
    private static boolean init= false;

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

        _version.setAppName(context.getServletContextName());
        _version.setConfigLastModTime(ServerContext.getConfigLastModTime());

        File confDir = ServerContext.getWebappConfigDir();
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File(confDir, VERSION_FILE)));
            _version.setMajor(getNum(props.getProperty(MAJOR)));
            _version.setMinor(getNum(props.getProperty(MINOR)));
            _version.setRev(getNum(props.getProperty(REV)));
            _version.setVersionType(Version.convertVersionType(props.getProperty(TYPE)));
            _version.setBuild(getNum(props.getProperty(BUILD_NUMBER)));
            _version.setBuildDate(props.getProperty(BUILD_DATE));
            _version.setBuildTime(props.getProperty(BUILD_TIME));
            _version.setBuildTag(props.getProperty(BUILD_TAG));
            _version.setBuildCommit(props.getProperty(BUILD_COMMIT));
            _version.setBuildCommitFirefly(props.getProperty(BUILD_COMMIT_FIREFLY));
            _version.setBuildGitTagFirefly(props.getProperty(BUILD_GIT_TAG_FIREFLY));
        } catch (IOException e) {
            // just ignore
        }
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

    public static String getUserAgentString() {
        String fUA= getFireflyUA();
        Version v= getAppVersion();
        if (v.getAppName().equalsIgnoreCase("firefly")) return fUA;
        if (v.getMajor()==0) return fUA;
        return fUA + " ("+v.getAppName() + "/" + v.getMajor()+"."+v.getMinor()+"."+v.getRev() + ")";
    }

    private static String getFireflyUA() {
        String tag = getAppVersion().getBuildGitTagFirefly();
        if (StringUtils.isEmpty(tag)) return unknownVersionUA;
        try {
            Matcher m= Pattern.compile("\\d+(\\.\\d+)+").matcher(tag);
            return m.find() ? "Firefly/" +m.group(0) : unknownVersionUA;
        } catch (Exception e) {
            return unknownVersionUA;
        }
    }
}


