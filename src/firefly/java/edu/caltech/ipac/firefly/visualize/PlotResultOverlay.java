/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;
/**
 * User: roby
 * Date: Aug 8, 2008
 * Time: 1:37:26 PM
 */


/**
 * @author Trey Roby
 */
public class PlotResultOverlay  extends JavaScriptObject {


    protected PlotResultOverlay() {}

    public final native boolean isSuccess() /*-{
        if ("success" in this) {
            return this.success;
        }
        else {
            return false;
        }
    }-*/;

    public final native boolean isArrayResult() /*-{
        return  ("resultAry" in this);
    }-*/;

    public final native JsArray<PlotResultOverlay> getResultAry() /*-{
        return this.resultAry;
    }-*/;

    public final native String getData()  /*-{
        if ("data" in this) {
            return this.data;
        }
        else {
            return null;
        }
    }-*/;


    public final native String getBriefFailReason()  /*-{
        if ("briefFailReason" in this) {
            return this.briefFailReason;
        }
        else {
            return null;
        }
    }-*/;

    public final native String getUserFailReason()  /*-{
        if ("userFailReason" in this) {
            return this.userFailReason;
        }
        else {
            return null;
        }
    }-*/;

    public final native String getDetailFailReason()  /*-{
        if ("detailFailReason" in this) {
            return this.detailFailReason;
        }
        else {
            return null;
        }
    }-*/;

    public final native String getProgressKey()  /*-{
        if ("progressKey" in this) {
            return this.progressKey;
        }
        else {
            return "";
        }
    }-*/;

    public final native String getResult(String key) /*-{
        if (key in this) {
            return this[key];
        }
        else {
            return null;
        }
    }-*/;

    public final native JsArrayInteger getIntArrayResult(String key) /*-{
        if (key in this) {
            return this[key];
        }
        else {
            return null;
        }
    }-*/;

    public final native JsArrayString getStringArrayResult(String key) /*-{
        if (key in this) {
            return this[key];
        }
        else {
            return null;
        }
    }-*/;

    public final native JsArrayNumber getDoubleArrayResult(String key) /*-{
        if (key in this) {
            return this[key];
        }
        else {
            return null;
        }
    }-*/;
}

