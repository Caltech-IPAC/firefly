package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.JavaScriptObject;
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
