package edu.caltech.ipac.firefly.core;
/**
 * User: roby
 * Date: 12/8/11
 * Time: 2:06 PM
 */


import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;


/**
 * @author Trey Roby
 */
public class SupportedBrowsers {

    private static boolean _init= false;
    private static boolean _supported= false;
    private static String _unsupportedMsg= null;
//    private static boolean supportIphone= false;


//    public static void setSupportIphone(boolean support) {
//        supportIphone= support;
//    }

    private static void ensureInitialized() {
        if (_init) return;

        Browser b= BrowserUtil.getBrowserType();
        int major = BrowserUtil.getMajorVersion();

        boolean supported= true;
        switch (b) {
            case FIREFOX:
                supported= (major>2);
                if (!supported) _unsupportedMsg= getFFMessage();
                break;
            case SEAMONKEY:
                supported= (major>1);
                if (!supported) _unsupportedMsg= getSeamonkeyMessage();
                break;
            case SAFARI:
                supported= true;
                break;
            case WEBKIT_GENERIC:
                supported= true;
                break;
            case IE:
                supported= (major>7);
                if (!supported) _unsupportedMsg= getIEMessage();
                break;
            case OPERA:
                supported= true;
                break;
            case CHROME:
                supported= true;
                break;
            case UNKNOWN:
                break;
        }


//        if (supported && !supportIphone) {
//            if (BrowserUtil.isPlatform(Platform.IPHONE)) {
//                supported= false;
//                _unsupportedMsg= getiPhoneMessage();
//            }
//
//        }
        _supported= supported;
    }


    public static boolean isSupported() {
        ensureInitialized();
        return _supported;
    }

    public static HTML getUnsupportedMessage() {
        ensureInitialized();
        Widget w= new HTML(_unsupportedMsg);
        w.setPixelSize(500, 500);

        return new HTML(_unsupportedMsg);
    }

    public static void showUnsupportedMessage() {

        VerticalPanel vp= new VerticalPanel();
        GwtUtil.setStyles(vp, "backgroundColor", "white",
                              "border", "3px solid black");
        Label lbottom= new Label();
        lbottom.setHeight("10px");
        vp.add(getUnsupportedMessage());
        vp.add(lbottom);
        vp.setSpacing(10);
        PopupPane popup= new PopupPane("Unsupported",vp, PopupType.STANDARD,false,true,false, PopupPane.HeaderType.NONE);
        popup.setDoRegionChangeHide(false);
        popup.alignToCenter();
        popup.show();
    }

    private static String getFFMessage() {
        return "<p style=\"font-size:14pt; line-height: 1;\">" +
                "<i>Unsupported Browser</i><br><br>" +
                "Your Firefox browser is version " + BrowserUtil.getVersionString() +
                ".<br>" +
                "This is a very old browser and we require at least version 3.<br><br>\n" +
                "You can get the most recent version at <a href=\"http://firefox.com\">Firefox.com </a></p>";
    }

    private static String getSeamonkeyMessage() {

        return "<p style=\"font-size:14pt; line-height: 1;\">" +
                "<i>Unsupported Browser</i><br><br>" +
                "Your Seamonkey browser is version " + BrowserUtil.getVersionString() +
                ".<br>" +
               "This is a very old browser and we require at least version 2.<br><br>\n" +
               "You can get the most recent version at " +
                "<a href=\"http://www.seamonkey-project.org/\">www.seamonkey-project.org</a></p>";
    }

    private static String getIEMessage() {

        return  "<p style=\"font-size:14pt; line-height: 1;\">" +
                "<i>Unsupported Browser</i><br><br>" +
                "Your IE browser is version " +BrowserUtil.getMajorVersion() +
                ".  Microsoft's older browsers do not support web standards very well." +
                " We require at least version 8.<br><br>\n" +
                "You can get the most recent version of IE at \n" +
                "<a href=\"http://windows.microsoft.com/en-US/internet-explorer/downloads/ie/\">Microsoft</a><br>\n" +
                "<br>You might also consider trying one of the following browsers:<br>\n" +
                "<ul style= \"padding-left: 50px; style=\"font-size:12pt;\">\n" +
                "<li><a style=\"font-size:12pt; line-height: 2;\" href=\"https://www.google.com/chrome\">Chrome</a></li>\n" +
                "<li><a style=\"font-size:12pt; line-height: 1;\" href=\"http://firefox.com\">Firefox</a></li>\n" +
                "</ul></p>" ;
    }

    private static String getiPhoneMessage() {
        return  "<p style=\"font-size:12pt; line-height: 1;\">" +
                "<i>IPhone not supported</i><br><br>" +
                "We do not yet support the iPhone, however we work <i>very well</i> on the iPad!";
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
