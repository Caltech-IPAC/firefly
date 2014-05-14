package edu.caltech.ipac.frontpage.core;
/**
 * User: roby
 * Date: 11/8/13
 * Time: 2:10 PM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.frontpage.data.DisplayData;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Trey Roby
 */
public class FrontpageUtils {


    private static final String HOSTNAME= getHostName();
    private static String componentRootFallback= null;


    public static String componentURL(String url) {
        String cRoot= componentRootFallback==null ? FrontpageUtils.getComponentsRoot() : componentRootFallback;
        return FFToolEnv.modifyURLToFull(url, cRoot, "/");
    }
    public static String refURL(String url) {
        String root= FFToolEnv.getRootPath();
        if (root==null) root= HOSTNAME;

        return FFToolEnv.modifyURLToFull(url,root,"irsa.ipac.caltech.edu");
    }

    private static String getHostName() {
        String root= GWT.getModuleBaseURL();
        String retval= root;
        int idx= -1;
        if (root.startsWith("http://") )       idx= root.indexOf("/", 8);
        else if (root.startsWith("https://") )  idx= root.indexOf("/", 9);

        if (idx>-1) {
            retval= root.substring(0,idx+1);
        }
        return retval;
    }

    public static void enableComponentRootFallback() {
        componentRootFallback= GWT.getModuleBaseURL() + "fallback";
    }

    public static native JsArray<DisplayData> changeToJS(String arg) /*-{
        return eval('('+arg+')');
    }-*/;

    static List<DisplayData> convertToList(JsArray <DisplayData> dataAry) {
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
                        if (response.getStatusCode()==Response.SC_OK) {
                            String jsText = response.getText();
                            dataRet.done(changeToJS(jsText));
                        }
                        else {
                            if (dataRet instanceof DataRetDetails) {
                                ((DataRetDetails)dataRet).fail(response.getStatusCode());
                            }
                        }

                    } catch (Exception e) {
                        GwtUtil.getClientLogger().log(Level.INFO, "error 1 getting request: "+url, e);
                    }
                }

                public void onError(com.google.gwt.http.client.Request request, Throwable e) {
                    GwtUtil.getClientLogger().log(Level.INFO, "error 2 getting request: "+url, e);
                }
            });
            rb.send();
        } catch (RequestException e) {
            GwtUtil.getClientLogger().log(Level.INFO, "error  3 getting request: "+url, e);
        }
    }

    public static native boolean isDirect() /*-{
        var retval= true;
        if ("fireflyToolbar" in $wnd) {
            if ("direct" in $wnd.fireflyToolbar) {
                retval= $wnd.fireflyToolbar.direct;
            }
        }
        return retval;
    }-*/;


    public static native void markNewToolbarRunning() /*-{
        if ("fireflyToolbar" in $wnd) {
            $wnd.fireflyToolbar.running= true;
        }
        else {
            $wnd.fireflyToolbar= {running : true};
        }
    }-*/;


    public static native boolean isFrontpage() /*-{
        var retval= false;
        if ("fireflyToolbar" in $wnd) {
            if ("frontpage" in $wnd.fireflyToolbar) {
                retval= $wnd.fireflyToolbar.frontpage;
            }
        }
        return retval;
    }-*/;

    public static native String getURLRoot() /*-{
        var retval= null;
        if ("fireflyToolbar" in $wnd) {
            if ("rootURL" in $wnd.fireflyToolbar) {
                retval= $wnd.fireflyToolbar.rootURL;
            }
        }
        return retval;
    }-*/;


    public static native String getReplacementImage() /*-{
        var retval= null;
        if ("fireflyToolbar" in $wnd) {
            if ("backgroundImage" in $wnd.fireflyToolbar) {
                retval= $wnd.fireflyToolbar.backgroundImage;
            }
        }
        return retval;
    }-*/;



    public static native String getComponentsRoot() /*-{
        var retval= null;
        if ("fireflyToolbar" in $wnd) {
            if ("rootComponentsURL" in $wnd.fireflyToolbar) {
                retval= $wnd.fireflyToolbar.rootComponentsURL;
            }
        }
        return retval;
    }-*/;

    public static native String getSubIcon() /*-{
        var retval= null;
        if ("fireflyToolbar" in $wnd) {
            if ("subIcon" in $wnd.fireflyToolbar) {
                retval= $wnd.fireflyToolbar.subIcon;
            }
        }
        return retval;
    }-*/;

    public static native int getAppToolbarOffset(int defValue) /*-{
        var retval= defValue;
        if ("fireflyToolbar" in $wnd) {
            if ("appToolbarOffset" in $wnd.fireflyToolbar) {
                retval= $wnd.fireflyToolbar.appToolbarOffset;
            }
        }
        return retval;
    }-*/;

    public static native String getSubIconURL() /*-{
        var retval= null;
        if ("fireflyToolbar" in $wnd) {
            if ("subIconURL" in $wnd.fireflyToolbar) {
                retval= $wnd.fireflyToolbar.subIconURL;
            }
        }
        return retval;
    }-*/;

    public static native String getToolbarType() /*-{
        var retval= null;
        if ("fireflyToolbar" in $wnd) {
            if ("toolbarType" in $wnd.fireflyToolbar) {
                retval= $wnd.fireflyToolbar.toolbarType;
            }
        }
        return retval;
    }-*/;

    public static interface DataRet {
        void done(JsArray<DisplayData> data);
    }

    public static interface DataRetDetails extends DataRet {
        void fail(int status);
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
