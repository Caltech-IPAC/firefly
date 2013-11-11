package edu.caltech.ipac.frontpage.core;
/**
 * User: roby
 * Date: 11/8/13
 * Time: 2:10 PM
 */


import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.frontpage.data.DisplayData;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Trey Roby
 */
public class FrontpageUtils {


    private static native JsArray<DisplayData> changeToJS(String arg) /*-{
        return eval('('+arg+')');
    }-*/;

    static List<DisplayData> convertToList(JsArray<DisplayData> dataAry) {
        ArrayList<DisplayData> list= new ArrayList<DisplayData>(dataAry.length());
        for(int i=0; (i<dataAry.length()); i++) {
            list.add(dataAry.get(i));
        }
        return list;
    }

    public static void getURLJSonData(final String url, final DataRet dataRet) {

        try {
            RequestBuilder rb= new RequestBuilder(RequestBuilder.GET, url);
            rb.setCallback(new RequestCallback() {
                public void onResponseReceived(com.google.gwt.http.client.Request request, Response response) {
                    try {
                        String jsText = response.getText();
                        dataRet.done(changeToJS(jsText));
                    } catch (Exception e) {
                        GwtUtil.getClientLogger().log(Level.INFO, "error getting request: "+url, e);
                    }
                }

                public void onError(com.google.gwt.http.client.Request request, Throwable e) {
                    GwtUtil.getClientLogger().log(Level.INFO, "error getting request: "+url, e);
                }
            });
            rb.send();
        } catch (RequestException e) {
            GwtUtil.getClientLogger().log(Level.INFO, "error getting request: "+url, e);
        }
    }

    public static native boolean isFrontpage() /*-{
        var retval= false;
        if ("firefly" in $wnd) {
            if ("frontpage" in $wnd.firefly) {
                retval= $wnd.firefly.frontpage;
            }
        }
        return retval;
    }-*/;

    public static native String getURLRoot() /*-{
        var retval= null;
        if ("firefly" in $wnd) {
            if ("rootURL" in $wnd.firefly) {
                retval= $wnd.firefly.rootURL;
            }
        }
        return retval;
    }-*/;

    public static native String getComponentsRoot() /*-{
        var retval= null;
        if ("firefly" in $wnd) {
            if ("rootComponentsURL" in $wnd.firefly) {
                retval= $wnd.firefly.rootComponentsURL;
            }
        }
        return retval;
    }-*/;

    public static native String getSubIcon() /*-{
        var retval= null;
        if ("firefly" in $wnd) {
            if ("subIcon" in $wnd.firefly) {
                retval= $wnd.firefly.subIcon;
            }
        }
        return retval;
    }-*/;


    public static native String getSubIconURL() /*-{
        var retval= null;
        if ("firefly" in $wnd) {
            if ("subIconURL" in $wnd.firefly) {
                retval= $wnd.firefly.subIconURL;
            }
        }
        return retval;
    }-*/;



    public interface DataRet {
        void done(JsArray<DisplayData> data);
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
