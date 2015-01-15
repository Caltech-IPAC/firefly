/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;
/**
 * User: roby
 * Date: 3/5/12
 * Time: 12:16 PM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.util.WebUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class JsonUtils {


    public static final int TIMEOUT = 10 * 60 * 1000;  // 10 min
    public static final String DEF_BASE_URL = GWT.getModuleBaseURL() + "sticky/FireFly_CommandService";

    public static String makeURL(String baseUrl, String cmd, List<Param> paramList, boolean isJsonp) {
        if (cmd != null) paramList.add(new Param(ServerParams.COMMAND, cmd));
        if (isJsonp) paramList.add(new Param(ServerParams.DO_JSONP, "true"));
        return WebUtil.encodeUrl(baseUrl, paramList);
    }


    public static <T extends JavaScriptObject> void jsonpRequest(String cmd, AsyncCallback<T> cb) {
        jsonpRequest(cmd, new ArrayList<Param>(2), cb);
    }

    public static <T extends JavaScriptObject> void jsonpRequest(String cmd,
                                                                 List<Param> paramList,
                                                                 AsyncCallback<T> cb) {
        jsonpRequest(DEF_BASE_URL, cmd, paramList, cb);
    }

    public static <T extends JavaScriptObject> void jsonpRequest(String baseUrl,
                                                                 String cmd,
                                                                 List<Param> paramList,
                                                                 AsyncCallback<T> cb) {
        String url = makeURL(baseUrl, cmd, paramList, true);
        JsonpRequestBuilder builder = new JsonpRequestBuilder();
        builder.setTimeout(TIMEOUT);
        builder.requestObject(url, cb);
    }


    public static void jsonRequest(String cmd, RequestCallback cb) throws RequestException {
        jsonRequest(cmd, new ArrayList<Param>(2), cb);
    }

    public static void jsonRequest(String cmd,
                                   List<Param> paramList,
                                   RequestCallback cb) throws RequestException {
        jsonRequest(DEF_BASE_URL, cmd, paramList, cb);
    }

    public static void jsonRequest(String baseUrl,
                                   String cmd,
                                   List<Param> paramList,
                                   RequestCallback cb) throws RequestException {
        String url = makeURL(baseUrl, cmd, paramList, false);
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
        builder.setCallback(cb);
        builder.send();
    }

    public static void doService(boolean doJsonP,
                                 String cmd,
                                 AsyncCallback<String> async) {
        List<Param> paramList = new ArrayList<Param>(2);
        doService(doJsonP, cmd, paramList, async, new Converter<String>() {
            public String convert(String s) {
                return s;
            }
        });
    }


    public static <T> void doService(boolean doJsonP,
                                     String cmd,
                                     List<Param> paramList,
                                     AsyncCallback<T> async,
                                     JsonUtils.Converter<T> converter) {
        if (doJsonP) {
            jsonpRequest(cmd, paramList, new JsonUtils.Acb<T>(async, converter));
        } else {
            try {
                jsonRequest(cmd, paramList, new JsonUtils.Rcb<T>(async, converter));
            } catch (RequestException e) {
                async.onFailure(e);
            }
        }
    }


    public static SimpleResultOverlay changeToJS(String arg) {
        return changeToJSAry(arg).get(0);
    }

    private static native JsArray<SimpleResultOverlay> changeToJSAry(String arg) /*-{
        return eval(arg);
    }-*/;


    public static class Acb<T> implements AsyncCallback<JsArray<SimpleResultOverlay>> {
        AsyncCallback<T> cb;
        private Converter<T> converter;

        public Acb(AsyncCallback<T> cb, Converter<T> converter) {
            this.cb = cb;
            this.converter = converter;
        }

        public void onFailure(Throwable e) {
            cb.onFailure(e);
        }

        public void onSuccess(JsArray<SimpleResultOverlay> result) {
            try {
                cb.onSuccess(converter.convert(result.get(0).getData()));
            } catch (Exception e) {
                cb.onFailure(e);
            }
        }
    }

    public static class Rcb<T> implements RequestCallback {
        private AsyncCallback<T> cb;
        private Converter<T> converter;


        public Rcb(AsyncCallback<T> cb, Converter<T> converter) {
            this.cb = cb;
            this.converter = converter;
        }

        public void onError(com.google.gwt.http.client.Request request, Throwable e) {
            cb.onFailure(e);
        }

        public void onResponseReceived(com.google.gwt.http.client.Request request, Response response) {
            try {
                SimpleResultOverlay result = JsonUtils.changeToJS(response.getText());
                cb.onSuccess(converter.convert(result.getData()));
            } catch (Exception e) {
                onError(request,e);
            }
        }
    }


    public interface Converter<T> {
        public T convert(String s);
    }


    public static class SimpleResultOverlay extends JavaScriptObject {
        protected SimpleResultOverlay() {
        }

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

        public final native String getException(int i)  /*-{
            var excepNum = "e" + i;
            if (excepNum in this) {
                return this[excepNum];
            }
            else {
                return null;
            }
        }-*/;
    }

}

