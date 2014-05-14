package edu.caltech.ipac.frontpage.core;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.JsonUtils;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.frontpage.data.DisplayData;
import edu.caltech.ipac.frontpage.ui.ToolbarPanel;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


public class AppMenuBarCmd extends RequestCmd {

    public  static final String COMMAND = "AppMenuBarCmd";
    private ToolbarPanel.ToolBarType barType;

    public AppMenuBarCmd() {
        super(COMMAND, "AppMenuBar", "AppMenuBar", true);
    }


    protected void doExecute(final Request req, AsyncCallback<String> callback) {
        barType= StringUtils.getEnum(FrontpageUtils.getToolbarType(), ToolbarPanel.ToolBarType.LARGE);
        String url= FrontpageUtils.componentURL("frontpage-data/irsa-menu.js");
        if (FrontpageUtils.isDirect()) {
            getComponents(url);
        }
        else {
            getComponentsIndirect(url);

        }
    }

    private void getComponents(final String url) {

        FrontpageUtils.getURLJSonData(url,
                                      new FrontpageUtils.DataRetDetails() {
                                          public void done(JsArray <DisplayData> data) {
                                              new ToolbarPanel("irsa-banner", data, barType);
                                          }

                                          public void fail(int status) {
                                              GwtUtil.getClientLogger().log(Level.INFO, "Failed to get "+ url+ " using fallback method");
                                              getComponentsIndirect(url);
                                          }
                                      }
            );
    }

    private void getComponentsIndirect(String indirectURL) {
        FrontpageUtils.enableComponentRootFallback();

        List<Param> list= new ArrayList<Param>(3);
        list.add(new Param(ServerParams.FILE, "${webapp-root}/fallback/frontpage-data/irsa-menu.js"));
        list.add(new Param(ServerParams.URL, indirectURL));

            JsonUtils.jsonpRequest(ServerParams.STATIC_JSON_DATA, list, new AsyncCallback<JsArray<DisplayData>>() {

                public void onFailure(Throwable exception) {
                    GwtUtil.getClientLogger().log(Level.INFO, "fallback failed", exception);
                }

                public void onSuccess(JsArray<DisplayData> result) {
                    new ToolbarPanel("irsa-banner", result, barType);
                }

//                public void onResponseReceived(com.google.gwt.http.client.Request request, Response response) {
//                    if (response.getStatusCode() == Response.SC_OK) {
//                        String s = response.getText();
//                        new ToolbarPanel("irsa-banner", FrontpageUtils.changeToJS(s), barType);
//                    }
//                }
//
//                public void onError(com.google.gwt.http.client.Request request, Throwable exception) {
//                    GwtUtil.getClientLogger().log(Level.INFO, "fallback failed", exception);
//                }
            });



    }



}

