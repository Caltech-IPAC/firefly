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
