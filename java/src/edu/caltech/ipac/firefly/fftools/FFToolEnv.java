package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 6/22/12
 * Time: 3:31 PM
 */


import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.RootPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.util.StringUtils;


/**
 * @author Trey Roby
 */
public class FFToolEnv {


    private static final String HUB = "FFTOOLS_Hub";
    private static final String ADVERTISE = "FireflyTools.Advertise";
    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();
    private static String _rootPath = null;

    public static native boolean isDebug() /*-{
        return $wnd.firefly.debug;
    }-*/;


    public static boolean isAdvertise() {
        boolean retval = Application.getInstance().getProperties().getBooleanProperty(ADVERTISE, false);
        if (isAdvertiseDefined()) {
            retval = isAdvertise();
        }
        return retval;
    }


    private static native boolean isInternalAdvertise() /*-{
        if ("advertise" in $wnd.firefly) {
            return $wnd.firefly.advertise;
        }
        else {
            return true;
        }
    }-*/;


    private static native boolean isAdvertiseDefined() /*-{
        return ("advertise" in $wnd.firefly);
    }-*/;

    public static TablePreviewEventHub getHub() {
        TablePreviewEventHub hub = (TablePreviewEventHub) Application.getInstance().getAppData(HUB);
        if (hub == null) {
            hub = new TablePreviewEventHub();
            Application.getInstance().setAppData(HUB, hub);
        }
        return hub;
    }


    public static void logParamParseError(String source, String param, String value, String toType) {
        logDebugMsg(source + " could not parse param, " + param +
                            ", with value " + value + ", to type " + toType);
    }

    public static void logDebugMsg(String msg) {
        logDebugMsg(null, msg);
    }

    public static void logDebugMsg(String title, String msg) {
        if (FFToolEnv.isDebug()) {
            if (title == null) title = "FFTools: ";
            PopupUtil.showError(title, msg);
        }
    }

    public static RootPanel getRootPanel(String div) {
        RootPanel rp = (div == null) ? RootPanel.get() : RootPanel.get(div);
        rp.addStyleName(_ffCss.globalSettings());
        return rp;
    }

    /**
     * set the root path so all URL with out a root path are modified to be full with this path
     *
     * @param rootPath the root path of the url
     */
    public static void setRootPath(String rootPath) {
        if (rootPath != null) {
            if (isFull(rootPath)) {
                _rootPath = rootPath;
            } else {
                String docUrl = Document.get().getURL();
                String sAry[] = docUrl.split("/");
                if (sAry.length >= 3) {
                    _rootPath = sAry[0] + "//" + sAry[2] + "/" + rootPath;
                }
            }

        } else {
            _rootPath = null;
        }

    }


    /**
     * Modified a relative root path to a full root path.  The root is the source of the hosting web site by default.
     * If setRootPath has been called then the root the what was set.
     *
     * @param url a url relative or full, the relative is modified to full
     * @return a full path URL
     */
    public static String modifyURLToFull(String url) {
        String retURL = url;
        if (!StringUtils.isEmpty(url)) {
            if (!isFull(url)) {
                if (_rootPath == null) {
                    String docUrl = Document.get().getURL();
                    int lastSlash = docUrl.lastIndexOf("/");
                    if (lastSlash > -1) {
                        String rootURL = docUrl.substring(0, lastSlash + 1);
                        retURL = rootURL + url;
                    } else {
                        retURL = docUrl + "/" + url;
                    }
                } else {
                    retURL = _rootPath + "/" + url;
                }
            }
        }
        return retURL;
    }

    private static boolean isFull(String url) {
        url = url.toLowerCase();
        return url.startsWith("http") ||
                url.startsWith("https") ||
                url.startsWith("/") ||
                url.startsWith("file");
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
