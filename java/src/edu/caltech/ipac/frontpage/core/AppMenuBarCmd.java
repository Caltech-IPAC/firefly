package edu.caltech.ipac.frontpage.core;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.frontpage.data.DisplayData;
import edu.caltech.ipac.frontpage.ui.ToolbarPanel;


public class AppMenuBarCmd extends RequestCmd {

    public  static final String COMMAND = "AppMenuBarCmd";

    public AppMenuBarCmd() {
        super(COMMAND, "AppMenuBar", "AppMenuBar", true);
    }


    protected void doExecute(final Request req, AsyncCallback<String> callback) {
        getComponents();
    }

    private void getComponents() {
        FrontpageUtils.getURLJSonData("/frontpage-data/irsa-menu.js", new FrontpageUtils.DataRet() {
            public void done(JsArray<DisplayData> data) {
                new ToolbarPanel("irsa-banner", data, ToolbarPanel.ToolBarType.SMALL);
            }
        });
    }

}

