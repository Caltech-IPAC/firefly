package edu.caltech.ipac.firefly.core.task;
/**
 * User: roby
 * Date: 2/7/12
 * Time: 9:19 AM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.JsonUtils;
import edu.caltech.ipac.firefly.core.NetworkMode;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class CoreTask {


    private static NetworkMode mode = Application.getInstance().getNetworkMode();

    public static class LoadProperties extends ServerTask {
        final boolean saOnly;

        public LoadProperties() { this(false); }

        public LoadProperties(boolean saOnly) {
            this.saOnly = saOnly;
        }

        public void onSuccess(Object result) {
        }

        public void doTask(final AsyncCallback passAlong) {

            String baseUrl = GWT.getModuleBaseURL() + "servlet/FireFly_PropertyDownload?saOnly="+saOnly;
            if (mode != NetworkMode.JSONP) {
                String url = WebUtil.encodeUrl(baseUrl);
                RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url);
                rb.setCallback(new RequestCallback() {
                    public void onResponseReceived(com.google.gwt.http.client.Request request,
                                                   Response response) {
                        Application.getInstance().setProperties(new WebAppProperties(response.getText()));
                        passAlong.onSuccess(null);
                    }

                    public void onError(com.google.gwt.http.client.Request request, Throwable e) {
                        PopupUtil.showInfo("Could not load properties: " + e.toString());
                    }
                });
                try {
                    rb.send();
                } catch (RequestException e) {
                    PopupUtil.showInfo("Could not load properties: " + e.toString());
                }

            } else {
                List<Param> paramList = new ArrayList<Param>(2);
//                paramList.add(bdate);
//                paramList.add(clmdate);
                JsonUtils.jsonpRequest(baseUrl, null, paramList, new AsyncCallback<JsonUtils.SimpleResultOverlay>() {
                    public void onFailure(Throwable e) {
                        PopupUtil.showInfo("Could not load properties: " + e.toString());
                        passAlong.onFailure(e);
                    }

                    public void onSuccess(JsonUtils.SimpleResultOverlay result) {
                        String s = result.getData();
                        String sMod = s.replace("--NL--", "\n");
                        Application.getInstance().setProperties(new WebAppProperties(sMod));
                        passAlong.onSuccess(null);
                    }
                });

            }

        }
    }


}
