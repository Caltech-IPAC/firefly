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

        public void onSuccess(Object result) {
        }

        public void doTask(final AsyncCallback passAlong) {

            String baseUrl = GWT.getModuleBaseURL() + "servlet/FireFly_PropertyDownload";
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
