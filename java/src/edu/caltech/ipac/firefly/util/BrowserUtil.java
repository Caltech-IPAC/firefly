package edu.caltech.ipac.firefly.util;

/**
 * Date: Feb 22, 2009
 *
 * Get info about the Browser.
 * Should only be used on the client side.  User BrowserInfo on the server
 *
 *
 * @author Trey
 * @version $Id: BrowserUtil.java,v 1.16 2012/09/18 22:26:16 roby Exp $
 */
public class BrowserUtil {

    public static final int UNKNOWN_VERSION= BrowserInfo.UNKNOWN_VERSION;

    private static final BrowserInfo _bi= new BrowserInfo(getUserAgent());

    public static Browser getBrowserType() {
        return _bi.getBrowserType();
    }

    public static boolean isBrowser(Browser browser) {
        return _bi.isBrowser(browser);
    }


    public static boolean getSupportsShadows() { return _bi.getSupportsShadows(); }


    public static boolean isVersionAtLeast(Browser browser, int version) {
        return _bi.isVersionAtLeast(browser,version);
    }
    public static boolean isVersionAtLeast(Browser browser, int version, int minor) {
        return _bi.isVersionAtLeast(browser,version,minor);
    }

    public static boolean isPlatform(Platform platform) { return _bi.isPlatform(platform); }

    /**
     * check to see fi browser is IE
     * @return true is browser is IE
     */
    public static boolean isIE() { return _bi.isIE(); }
    public static boolean isChrome() { return _bi.isChrome(); }
    public static boolean isFirefox() { return _bi.isFirefox(); }
    public static boolean isSafari() { return _bi.isSafari(); }

    /**
     * Test to see if browser is IE with a version less than 9.
     * As of version 9 IE added better support for web standards
     * * @return boolean true is browser is IE less than version 9
     */
    public static boolean isOldIE() { return _bi.isIE() && _bi.getMajorVersion()<9; }

    public static boolean isBrowser(Browser browser, int version) {
        return _bi.isBrowser(browser,version);
    }

    public static int getMajorVersion()  {
        return _bi.getMajorVersion(); 
    }


    public static int getMinorVersion()  {
        return _bi.getMinorVersion();
    }

    public static boolean isTouchInput() {
        return _bi.isTouchInput();
    }


    public static boolean canSupportAnimation() {
        return _bi.canSupportAnimation();
    }

    public static String getBrowserString() {
        return _bi.getBrowserString();
    }


    public static String getVersionString() {
        return _bi.getVersionString();
    }

    public static String getBrowserDesc() {
        return _bi.getBrowserDesc();
    }

    public static boolean getSupportsCSS3() {
        return _bi.getSupportsCSS3();
    }

    public static boolean getSupportsCORS() {
        return _bi.getSupportsCORS();
    }

    public static native String getUserAgent() /*-{
        return navigator.userAgent.toLowerCase();
    }-*/;


    public static native String getBrowserVersion() /*-{
        return navigator.appVersion.toLowerCase();
    }-*/;


}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
