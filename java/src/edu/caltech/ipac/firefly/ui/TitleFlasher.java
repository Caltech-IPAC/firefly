package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 5/15/13
 * Time: 1:11 PM
 */


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey Roby
 */
public class TitleFlasher {


    private static JavaScriptObject focusFunc= null;
    private static FlashTitleTimer flashTimer= null;
    private static String favIcon= null;
    private static LinkElement favIconElement= null;


    public static void flashTitle(String altTitle) {

        if (!getHasFocus()) {
            if (focusFunc==null) {
                focusFunc= makeFocusFunction();
                addFocusFunction(focusFunc);
            }
            if (favIconElement==null) {
                findFavIcon();
            }
            if (flashTimer!=null) flashTimer.stop();

            flashTimer= new FlashTitleTimer(altTitle, Window.getTitle());
            flashTimer.schedule(300);
        }

    }

    private static void hasFocus() {
        if (flashTimer!=null) flashTimer.stop();
    }

    private static class FlashTitleTimer extends Timer {
        private String altTitle;
        private String mainTitle;

        private FlashTitleTimer(String altTitle, String mainTitle) {
            this.altTitle = altTitle;
            this.mainTitle = mainTitle;
        }

        @Override
        public void run() {
            String t= Window.getTitle();
            Window.setTitle(t.equals(mainTitle) ? altTitle : mainTitle);
            if (t.equals(mainTitle)) {
                Window.setTitle(altTitle);
//                if (favIconElement!=null) {
//                    favIconElement.setHref(GWT.getModuleBaseURL()+"images/stop.gif");
//                }

            }
            else {
                Window.setTitle(mainTitle);
//                if (favIconElement!=null) {
//                    favIconElement.setHref(favIcon);
//                }
            }
            this.schedule(2000);
        }

        public void stop() {
            Window.setTitle(mainTitle);
//            if (favIconElement!=null) {
//                favIconElement.setHref(favIcon);
//            }
            this.cancel();
        }
    }


    private static void findFavIcon() {
        Document doc= Document.get();
        NodeList<Element> eleList=doc.getElementsByTagName("link");
        for (int i= 0; (i<eleList.getLength()); i++) {
            Element e= eleList.getItem(i);
            LinkElement le= LinkElement.as(e);
            if ("image/x-icon".equals(le.getType())) {
                if (!StringUtils.isEmpty(le.getHref())) {
                    favIconElement= le;
                    favIcon= le.getHref();
                    break;
                }
            }
        }
    }

    private static native void addFocusFunction(JavaScriptObject f) /*-{
        $wnd.addEventListener("focus",f,false);
    }-*/;

    private static native JavaScriptObject makeFocusFunction() /*-{
        return function() {
            @edu.caltech.ipac.firefly.ui.TitleFlasher::hasFocus()();
        };
    }-*/;

    private static native boolean getHasFocus() /*-{
        return $wnd.document.hasFocus();
    }-*/;


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
