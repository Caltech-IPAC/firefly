/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 4/15/15
 * Time: 11:10 AM
 */


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

/**
 * @author Trey Roby
 */
public class Ext {

    public static final String AREA_SELECT= "AREA_SELECT";
    public static final String LINE_SELECT= "LINE_SELECT";
    public static final String REGION_SELECT= "REGION_SELECT";
    public static final String POINT= "POINT";
    public static final String NONE= "NONE";

    @JsType
    public interface Extension {
        @JsProperty String id();
        @JsProperty String plotId();
        @JsProperty String imageUrl();
        @JsProperty String title();
        @JsProperty String toolTip();
        @JsProperty String extType();
        @JsProperty Object callback();
    }

    @JsType
    public interface ExtensionResult {
        void setExtValue(String key, String value);
    }



    @JsType public interface ExtensionInterface {
//        Extension[] getExtensionListTEST();
        Extension[] getExtensionList(String id);
//        int getExtLength();
        String getRemoteChannel();
//        Extension getExtension(int idx);
        void clearListener();
        void fireExtAdd(Extension ext);
        void fireExtAction(Extension ext, ExtensionResult result);
        void fireChannelActivate(String bgIdChannel);
    }



    @JsExport
    @JsType
    public interface StoreEvent {
        public void storeChange();
    }




    public static void fireExtAction(Extension ext, ExtensionResult result) {
        Ext.makeExtensionInterface().fireExtAction(ext,result);
    }


    public static native Ext.Extension makeExtension(String id,
                                                     String plotId,
                                                     String extType,
                                                     String imageUrl,
                                                     String title,
                                                     String toolTip) /*-{
        if ($wnd.firefly && $wnd.firefly.gwt) {
            return new $wnd.firefly.gwt.PlotCmdExtension(id, plotId, extType, imageUrl, title, toolTip);
        }
        else {
            return null;
        }
    }-*/;



//    public static native int getLength(JavaScriptObject o) /*-{
//        if (o && $wnd.firefly && $wnd.firefly.gwt) {
//            return o.length;
//        }
//        else {
//            return 0;
//        }
//    }-*/;
//
//
//    public static native Extension getExtension(JavaScriptObject o, int i) /*-{
//        if ($wnd.firefly && $wnd.firefly.gwt) {
//            return o[i];
//        }
//        else {
//            return null;
//        }
//    }-*/;
//

    public static native Ext.ExtensionInterface makeExtensionInterface() /*-{
        if ($wnd.firefly && $wnd.firefly.gwt) {
            return new $wnd.firefly.gwt.ExtensionJavaInterface();
        }
        else {
            return null;
        }
    }-*/;


    public static native Ext.ExtensionInterface makeExtensionInterfaceWithListener(Object retObj, JavaScriptObject cb) /*-{
        if ($wnd.firefly && $wnd.firefly.gwt) {
            return new $wnd.firefly.gwt.ExtensionJavaInterface(retObj,cb);
        }
        else {
            return null;
        }
    }-*/;

    public static native Ext.ExtensionResult makeExtensionResult() /*-{
        if ($wnd.firefly && $wnd.firefly.gwt) {
            return new $wnd.firefly.gwt.ExtensionResult();
        }
        else {
            return null;
        }
    }-*/;

}
