/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.util.StringUtils;

/**
 * Date: Feb 22, 2009
 *
 /**
 *
 * Get info about the Browser. Should be used on the server side or when
 * wrapped by Browser Info
 *
 * @author Trey
 * @version $Id: BrowserInfo.java,v 1.17 2012/09/26 22:08:20 roby Exp $
 */
public class BrowserInfo {


    public static final int UNKNOWN_VERSION= -1;
    private static final String SEAMONKEY_KEY= "seamonkey/";
    private static final String FIREFOX_KEY= "firefox/";
    private static final String SIMPLE_VERSION_KEY = "version/";
    private static final String CHROME_KEY= "chrome/";

    private Browser _browser;
    private Platform _platform;
    private int _majorVersion;
    private int _minorVersion= UNKNOWN_VERSION;
    private final String _userAgent;
    private boolean _allRecognized;

    public BrowserInfo(String userAgent) {
        _userAgent= userAgent;
        _platform= getPlatform(userAgent);
        _allRecognized = (_platform!=Platform.UNKNOWN);
        evaluateBrowser(_platform);
    }

    public Browser getBrowserType() { return _browser; }
    public boolean isIE() { return isBrowser(Browser.IE); }
    public boolean isSafari() { return isBrowser(Browser.SAFARI); }
    public boolean isFirefox() { return isBrowser(Browser.FIREFOX); }
    public boolean isChrome() { return isBrowser(Browser.CHROME); }

    public boolean isBrowser(Browser browser) { return isBrowser(browser, UNKNOWN_VERSION); }
    public boolean isBrowser(Browser browser, int version) {
        boolean retval= false;
        if (browser==_browser) {
            retval= (version==UNKNOWN_VERSION || version==_majorVersion);
        }
        return retval;
    }


    public boolean isVersionAtLeast(Browser browser, int version, int minor) {
        boolean retval= false;
        if (browser==_browser) {
            if (version!=UNKNOWN_VERSION) {
                if (_majorVersion>version) {
                    retval= true;
                }
                if (_majorVersion==version) {
                    if (_minorVersion!=UNKNOWN_VERSION) {
                        if (_minorVersion>=minor)  {
                            retval= true;
                        }
                    }
                }
            }
        }
        return retval;
    }


    public boolean isVersionAtLeast(Browser browser, int version) {
        boolean retval= false;
        if (browser==_browser) {
            if (version!=UNKNOWN_VERSION) {
                if (_majorVersion>=version) {
                    retval= true;
                }
            }
        }
        return retval;
    }

    public boolean isVersionBefore(Browser browser, int version, int minor) {
        return !isVersionAtLeast(browser,version,minor);
    }

    public boolean isVersionBefore(Browser browser, int version) {
        return !isVersionAtLeast(browser, version);
    }


    public boolean getSupportsCSS3() {
        boolean retval=  isBrowser(Browser.CHROME);

        if (!retval) {
            retval= isBrowser(Browser.SAFARI) && _majorVersion>=5;
        }

        if (!retval) {
            retval= isTouchInput();
        }

        if (!retval) {
            retval= isBrowser(Browser.FIREFOX) && _majorVersion>3;
        }

        if (!retval) {
            retval= isBrowser(Browser.OPERA) && _majorVersion>=10;
        }

        if (!retval) {
            retval= isIE() && _majorVersion>=9;
        }

        return retval;

    }

    public boolean getSupportsShadows() {
        boolean retval=  true;

        if (isIE() && isVersionBefore(Browser.IE,9)) {
            retval= false;
        }
        else if (isSafari() && isVersionBefore(Browser.SAFARI,5,1)) {
            retval= false;
        }

        return retval;

    }

    public boolean getSupportsCORS() {
        boolean retval=  true;
        if (isFirefox() && isVersionBefore(Browser.FIREFOX,3,5)) {
            retval= false;
        }
        else if (isSafari() && isVersionBefore(Browser.SAFARI,4)) {
            retval= false;
        }
        else if (isIE() && isVersionBefore(Browser.IE,10)) {
            retval= false;
        }
        else if (isBrowser(Browser.OPERA) && isVersionBefore(Browser.OPERA,12)) {
            retval= false;
        }
        return retval;

    }


    public boolean isPlatform(Platform platform) { return  (platform == _platform); }
    public int getMajorVersion()  { return _majorVersion; }
    public int getMinorVersion()  { return _minorVersion; }
    public String getPlatformDesc() { return _platform.getDesc(); }

    public boolean isAllRecognized() { return _allRecognized; }

    public String getBrowserString() { return _browser.getDesc(); }

    public String getVersionString() {
        String retval;
        if (_minorVersion!=UNKNOWN_VERSION) retval= _majorVersion + "." +_minorVersion;
        else                                retval= _majorVersion+"";
        return retval;
    }

    public String getBrowserDesc() {
        String retval;
        if (_minorVersion!=UNKNOWN_VERSION) {
            retval= StringUtils.pad(12, getBrowserString())+
                    "Version: " + StringUtils.pad(15, _majorVersion + "." +_minorVersion);
        }
        else {
            retval= StringUtils.pad(12, getBrowserString())+ "Version: " + StringUtils.pad(15,_majorVersion+"");
        }
        return retval;
    }

    public boolean isTouchInput() {
        return _platform==Platform.IPAD     ||
               _platform==Platform.IPHONE   ||
               _platform==Platform.ANDROID;
    }

    public boolean canSupportAnimation() {
        boolean retval= false;
        switch (_browser) {
            case FIREFOX:
                retval= (_majorVersion>=4) || (_majorVersion==3 && _minorVersion>=5);
                break;
            case SAFARI:
                retval= (_majorVersion>=5);
                break;
            case IE:
                retval= (_majorVersion>=10);
                break;
            case OPERA:
                retval= (_majorVersion>=12);
                break;
            case CHROME:
                retval= true;
                break;
        }
        return retval;

    }

    private void evaluateBrowser(Platform p) {
        _browser= Browser.UNKNOWN;
        _majorVersion= UNKNOWN_VERSION;
        if (_userAgent.contains("msie")) {
            _browser= Browser.IE;
            if (_userAgent.contains("msie 10")) {
                _majorVersion= 10;
            }
            else if (_userAgent.contains("msie 9")) {
                _majorVersion= 9;
            }
            else if (_userAgent.contains("msie 8")) {
                _majorVersion= 8;
            }
            else if (_userAgent.contains("msie 7")) {
                _majorVersion= 7;
            }
            else if (_userAgent.contains("msie 6")) {
                _majorVersion= 6;
            }
        }
        else if (_userAgent.contains(FIREFOX_KEY)) {
            _browser= Browser.FIREFOX;
            _allRecognized= parseVersion(FIREFOX_KEY);
        }
        else if (_userAgent.contains(SEAMONKEY_KEY)) {
            _browser= Browser.SEAMONKEY;
            _allRecognized= parseVersion(SEAMONKEY_KEY);
        }
        else if  (_userAgent.contains("applewebkit")) {
            if (_userAgent.contains(CHROME_KEY)) {
                _browser= Browser.CHROME;
                _allRecognized= parseVersion(CHROME_KEY);
            }
            else if (p==Platform.BLACKBERRY || p==Platform.SYMBIAN_OS || p==Platform.UNKNOWN) {
                _browser= Browser.WEBKIT_GENERIC;
                _allRecognized= parseVersion(SIMPLE_VERSION_KEY);
            }
            else {
                _browser= Browser.SAFARI;
                _allRecognized= parseVersion(SIMPLE_VERSION_KEY);
            }
        }
        else if  (_userAgent.contains("opera")) {
            _browser= Browser.OPERA;
            if (_userAgent.contains("opera/9")) {
                _majorVersion= 9;
                parseVersion(SIMPLE_VERSION_KEY);
            }
            else if (_userAgent.contains("opera/8")) {
                _majorVersion= 8;
            }
            else if (_userAgent.contains("opera/7")) {
                _majorVersion= 7;
            }
            else if (_userAgent.contains("opera/6")) {
                _majorVersion= 6;
            }
        }
        else {
            _browser= Browser.UNKNOWN;
            _allRecognized = false;
        }
    }

    private boolean parseVersion(String key) throws NumberFormatException {
        boolean success= true;
        try {
            int idx= _userAgent.indexOf(key);
            if (idx!=-1) idx+= key.length();
            String ver= _userAgent.substring(idx);

            int len= ver.length();
            boolean found= false;
            int i;
            char c;
            for(i=0; (i<len); i++) {
                c= ver.charAt(i);
                if (!Character.isDigit(c) && c!='.') {
                    found= true;
                    break;
                }
            }
            if (found) ver= ver.substring(0,i);


            String sAry[]= ver.split("\\.");
            _majorVersion= Integer.parseInt(sAry[0]);
            if (StringUtils.allDigits(sAry[1])) {
                _minorVersion= Integer.parseInt(sAry[1]);
            }
        } catch (Exception e) {
            success= false;
        }
        return success;
    }

    private static Platform getPlatform(String ua) {
        Platform platform;
        if  (ua.contains("window")) {
            platform = Platform.WINDOWS;
        }
        else if  (ua.contains("macintosh")) {
            platform = Platform.MAC;
        }
        else if  (ua.contains("ipad")) {
            platform = Platform.IPAD;
        }
        else if  (ua.contains("iphone")) {
            platform = Platform.IPHONE;
        }
        else if  (ua.contains("linux")) {
            if (ua.contains("android")) {
                platform = Platform.ANDROID;
            }
            else {
                platform = Platform.LINUX;
            }
        }
        else if  (ua.contains("solaris")) {
            platform = Platform.SOLARIS;
        }
        else if  (ua.contains("sunos")) {
            platform = Platform.SUNOS;
        }
        else if  (ua.contains("aix")) {
            platform = Platform.AIX;
        }
        else if  (ua.contains("hpux")) {
            platform = Platform.HPUX;
        }
        else if  (ua.contains("freebsd")) {
            platform = Platform.FREE_BSD;
        }
        else if  (ua.contains("symbianos")) {
            platform = Platform.SYMBIAN_OS;
        }
        else if  (ua.contains("j2me")) {
            platform = Platform.J2ME;
        }
        else if  (ua.contains("blackberry")) {
            platform = Platform.BLACKBERRY;
        }
        else {
            platform = Platform.UNKNOWN;
        }
        return platform;
    }

}
