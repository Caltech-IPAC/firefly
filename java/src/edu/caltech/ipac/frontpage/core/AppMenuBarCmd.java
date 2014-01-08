package edu.caltech.ipac.frontpage.core;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.JsonUtils;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.frontpage.data.DisplayData;
import edu.caltech.ipac.frontpage.ui.ToolbarPanel;

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
        String tbStr= FrontpageUtils.getToolbarType();
        if (tbStr!=null) {
            try {
                barType= Enum.valueOf(ToolbarPanel.ToolBarType.class, tbStr);
            } catch (Exception e) {
                barType= ToolbarPanel.ToolBarType.LARGE;
            }
        }
        else {
            barType= ToolbarPanel.ToolBarType.LARGE;
        }
        getComponents();
    }

    private void getComponents() {



        FrontpageUtils.getURLJSonData(FrontpageUtils.componentURL("frontpage-data/irsa-menu.js"),
                                      new FrontpageUtils.DataRetDetails() {
                                          public void done(JsArray <DisplayData> data) {
                                              new ToolbarPanel("irsa-banner", data, barType);
                                          }

                                          public void fail(int status) {
                                              if (status== Response.SC_NOT_FOUND) {
                                                  getComponentsFallback(FrontpageUtils.componentURL("frontpage-data/irsa-menu.js"));
                                              }
                                          }
                                      }
            );
    }

    private void getComponentsFallback(String failedURL) {
        FrontpageUtils.enableComponentRootFallback();


        List<Param> list= new ArrayList<Param>(3);
        list.add(new Param(ServerParams.FNAME, "${webapp-root}/fallback/frontpage-data/irsa-menu.js"));

        GwtUtil.getClientLogger().log(Level.INFO, "Failed to get "+ failedURL+ " using fallback");
        try {
            JsonUtils.jsonRequest(ServerParams.STATIC_JSON_DATA, list, new RequestCallback() {
                public void onResponseReceived(com.google.gwt.http.client.Request request, Response response) {
                    if (response.getStatusCode()==Response.SC_OK) {
                        String s= response.getText();
                        new ToolbarPanel("irsa-banner", FrontpageUtils.changeToJS(s), barType);
                    }
                }

                public void onError(com.google.gwt.http.client.Request request, Throwable exception) {
                    GwtUtil.getClientLogger().log(Level.INFO, "fallback failed",exception);
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });
        } catch (RequestException e) {
        }



    }



}

