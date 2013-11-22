package edu.caltech.ipac.frontpage.core;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.frontpage.data.DisplayData;
import edu.caltech.ipac.frontpage.ui.DataSetPanel;
import edu.caltech.ipac.frontpage.ui.FeaturePager;
import edu.caltech.ipac.frontpage.ui.ToolbarPanel;


public class ComponentsCmd extends RequestCmd {

    public  static final String COMMAND = "ComponentsCmd";

    public ComponentsCmd() {
        super(COMMAND, "Components", "Components", true);
    }

    protected void doExecute(final Request req, AsyncCallback<String> callback) {
        getComponents();
    }

    private void getComponents() {
        FrontpageUtils.getURLJSonData(FrontpageUtils.componentURL("frontpage-data/irsa-menu.js"), new FrontpageUtils.DataRet() {
            public void done(JsArray<DisplayData> data) {
                new ToolbarPanel("frontpageMainPageToolbar", data, ToolbarPanel.ToolBarType.LARGE);
            }
        });

        FrontpageUtils.getURLJSonData(FrontpageUtils.componentURL("frontpage-data/feature.js"), new FrontpageUtils.DataRet() {
            public void done(JsArray<DisplayData> data) {
                new FeaturePager("frontpageFeaturePager", data);
            }
        });

        FrontpageUtils.getURLJSonData(FrontpageUtils.componentURL("frontpage-data/datasets.js"), new FrontpageUtils.DataRet() {
            public void done(JsArray<DisplayData> data) {
                new DataSetPanel("frontpageDataSetDisplay", FrontpageUtils.convertToList(data));
            }
        });



    }


}

