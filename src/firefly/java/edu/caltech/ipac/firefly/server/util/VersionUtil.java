/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.KeyVal;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    private static final String BUILD_FIREFLY_TAG ="BuildFireflyTag";
    private static final String BUILD_FIREFLY_BRANCH ="BuildFireflyBranch";
    private static final String DEV_CYCLE_TAG ="DevCycleTag";

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
            logVersionInfo();
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
            _version.setRev(props.getProperty(REV));
            _version.setVersionType(Version.convertVersionType(props.getProperty(TYPE)));
            _version.setBuild(getNum(props.getProperty(BUILD_NUMBER)));
            _version.setBuildDate(props.getProperty(BUILD_DATE));
            _version.setBuildTime(props.getProperty(BUILD_TIME));
            _version.setBuildTag(props.getProperty(BUILD_TAG));
            _version.setBuildCommit(props.getProperty(BUILD_COMMIT));
            _version.setBuildCommitFirefly(props.getProperty(BUILD_COMMIT_FIREFLY));
            _version.setBuildFireflyTag(props.getProperty(BUILD_FIREFLY_TAG));
            _version.setBuildFireflyBranch(props.getProperty(BUILD_FIREFLY_BRANCH));
            _version.setDevCycleTag(props.getProperty(DEV_CYCLE_TAG));
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
        String releaseTag = getAppVersion().getBuildFireflyTag();
        String devCycle = getAppVersion().getDevCycleTag();
        if (StringUtils.isEmpty(releaseTag) && StringUtils.isEmpty(devCycle)) return unknownVersionUA;
        Matcher m;
        try {
            if (!StringUtils.isEmpty(releaseTag)) {
                m= Pattern.compile("\\d+(\\.\\d+)+").matcher(releaseTag);
                if (m.find()) return "Firefly/" +m.group(0) + (releaseTag.startsWith("pre") ? "-prerelease" : "");
            }
            m= Pattern.compile("\\d+(\\.\\d+)+").matcher(devCycle);
            if (m.find()) return "Firefly/" +m.group(0)+"-development";
            return unknownVersionUA;
        } catch (Exception e) {
            return unknownVersionUA;
        }
    }

    public static List<KeyVal<String, String>> getVersionInfo() {
        Version v = getAppVersion();

        List<KeyVal<String, String>> versionInfo = new ArrayList<>(6);
        versionInfo.add(new KeyVal<>("Version", getVersionStr(v)));
        versionInfo.add(new KeyVal<>("Built On", v.getBuildTime()));
        versionInfo.add(new KeyVal<>("Git commit", v.getBuildCommit()));

        if (v.getMajor()>0)
            versionInfo.add(new KeyVal<>("Firefly Library Version", getFireflyVersionStr(v)));
        if (!StringUtils.isEmpty(v.getBuildCommitFirefly()))
            versionInfo.add(new KeyVal<>("Firefly Git Commit", v.getBuildCommitFirefly()));
        if (!StringUtils.isEmpty(v.getBuildFireflyTag()))
            versionInfo.add(new KeyVal<>("Firefly Git Tag", v.getBuildFireflyTag()));

        return versionInfo;
    }

    private static String getVersionStr(Version v) {
        Integer major = v.getMajor();

        if (major>0) {
            String versionStr = String.format("v%s.%s", major, v.getMinor());
            String buildRev = v.getRev();
            versionStr += buildRev.equals("0") ? "" : String.format(".%s", buildRev);
            return versionStr;
        }
        else return getFireflyVersionStr(v);
    }

    private static String getFireflyVersionStr(Version v) {
        String fireflyTag = v.getBuildFireflyTag();
        String fireflyCommit = v.getBuildCommitFirefly();
        String fireflyBranch = v.getBuildFireflyBranch();
        if (StringUtils.isEmpty(fireflyBranch)) fireflyBranch = "unknown-branch";

        Matcher formalReleaseMatcher = Pattern.compile("release-(\\d+(\\.\\d+)+)").matcher(fireflyTag);
        if (formalReleaseMatcher.find()) return formalReleaseMatcher.group(1);

        Matcher preReleaseMatcher = Pattern.compile("pre-(\\d+)-(\\d+(\\.\\d+)+)").matcher(fireflyTag);
        if (preReleaseMatcher.find()) return String.format("%s-PRE-%s", preReleaseMatcher.group(2), preReleaseMatcher.group(1));

        String devCycleTag = v.getDevCycleTag();
        Matcher cycleMatcher = Pattern.compile("cycle-(\\d+\\.\\d+)").matcher(devCycleTag);
        String devCycle = cycleMatcher.find() ? cycleMatcher.group(1) : "";
        if (!StringUtils.isEmpty(devCycle)) {
            String commit = StringUtils.isEmpty(fireflyCommit) ? v.getBuildCommit() : fireflyCommit;
            return String.format("%s-DEV%s_%s",
                    devCycle,
                    fireflyBranch.equals("dev") ? "" : (":" + fireflyBranch),
                    commit.substring(0, 4));
        }

        return String.format("0.0-%s-development", fireflyBranch);
    }

    private static void logVersionInfo() {
        List<String> versionInfoStrings = VersionUtil.getVersionInfo().stream()
                .map(verInfoKeyVal -> String.format("%-25s : %s", verInfoKeyVal.getKey(), verInfoKeyVal.getValue()))
                .collect(Collectors.toList());
        versionInfoStrings.add(0, ""); // so that log is properly formatted
        Logger.info(versionInfoStrings.toArray(String[]::new)); // pass List as arguments of a single Logger.info call
    }
}


