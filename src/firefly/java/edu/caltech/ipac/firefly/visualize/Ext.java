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
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * @author Trey Roby
 */
public class Ext {

    public static final String AREA_SELECT= "AREA_SELECT";
    public static final String LINE_SELECT= "LINE_SELECT";
    public static final String REGION_SELECT= "REGION_SELECT";
    public static final String PLOT_MOUSE_READ_OUT= "PLOT_MOUSE_READ_OUT";
    public static final String POINT= "POINT";
    public static final String NONE= "NONE";

    @JsType
    public interface Extension {
        String id();
        String plotId();
        String imageUrl();
        String title();
        String toolTip();
        String extType();
        Object callback();
    }

    @JsType
    public interface ExtensionResult {
        void setExtValue(String key, String value);
        void setNumberExtValue(String key, double value);
    }



    @JsType public interface ExtensionInterface {
        Extension[] getExtensionList(String id);
        String getRemoteChannel();
        void clearListener();
        void fireExtAdd(Extension ext);
        void fireExtAction(Extension ext, ExtensionResult result);
        void fireChannelActivate(String bgIdChannel);
    }




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
