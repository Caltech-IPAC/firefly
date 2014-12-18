package edu.caltech.ipac.firefly.ui.searchui;
/**
 * User: roby
 * Date: 2/7/14
 * Time: 10:02 AM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.ui.GwtUtil;

/**
 * @author Trey Roby
 */
public class PopularQuickSearchUI implements SearchUI {
    public String getKey() {
        return "DummyPopular";
    }

    public String getPanelTitle() {
        return "Popular";
    }

    public String getDesc() {
        return "Popular Search";
    }

    public String getSearchTitle() {
        return "Some product here";
    }

    public Widget makeUI() {
        HTML panel= new HTML("Popular search goes here");
        GwtUtil.setStyle(panel, "lineHeight", "100px");
//        panel.setSize("400px", "300px");
        return GwtUtil.wrap(panel, 50, 50, 50,20);
    }

    public boolean validate() {
        return true;
    }

    public void makeServerRequest(AsyncCallback<ServerRequest> cb) {
        // todo
    }

    public boolean setServerRequest(ServerRequest request) {
        return true;
    }
}

