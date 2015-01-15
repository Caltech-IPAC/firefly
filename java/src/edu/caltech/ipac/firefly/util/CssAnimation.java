/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;
/**
 * User: roby
 * Date: 9/18/12
 * Time: 3:59 PM
 */


import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;

/**
 * @author Trey Roby
 */
public class CssAnimation {

    public static boolean isSupported() { return BrowserUtil.canSupportAnimation();  }


    public static void setAnimationStyle(Widget w, String s) {
        GwtUtil.setStyle(w, isPrepended() ? getJSPrepend() + "Animation" : "animation", s);
    }

    private static String getJSPrepend() {
        String s;
        if (BrowserUtil.isBrowser(Browser.SAFARI) || BrowserUtil.isBrowser(Browser.CHROME))  s= "Webkit";
        else if (BrowserUtil.isBrowser(Browser.FIREFOX))  s= "Moz";
        else if (BrowserUtil.isBrowser(Browser.OPERA))  s= "O";
        else if (BrowserUtil.isIE() && BrowserUtil.getMajorVersion()==9)  s= "Ms";
        else s= "";
        return s;
    }

    public static String getStylePrepend() {
        String s;
        if (BrowserUtil.isBrowser(Browser.SAFARI) || BrowserUtil.isBrowser(Browser.CHROME))  s= "-webkit-";
        else if (BrowserUtil.isBrowser(Browser.FIREFOX))  s= "-moz-";
        else if (BrowserUtil.isBrowser(Browser.OPERA))  s= "-o-";
        else if (BrowserUtil.isIE() && BrowserUtil.getMajorVersion()==9)  s= "-ms-";
        else s= "";
        return s;
    }

    private static boolean isPrepended() {
        return (BrowserUtil.isBrowser(Browser.SAFARI)  ||
                BrowserUtil.isBrowser(Browser.CHROME)  ||
                BrowserUtil.isBrowser(Browser.FIREFOX) ||
                BrowserUtil.isBrowser(Browser.OPERA)   ||
                (BrowserUtil.isIE() && BrowserUtil.getMajorVersion()==9));
    }

}

