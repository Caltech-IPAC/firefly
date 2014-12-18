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

