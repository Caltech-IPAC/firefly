package edu.caltech.ipac.firefly.ui.searchui;
/**
 * User: roby
 * Date: 2/7/14
 * Time: 10:02 AM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.dyn.xstream.FormTag;
import edu.caltech.ipac.firefly.fuse.data.config.MissionTag;
import edu.caltech.ipac.firefly.fuse.data.config.ImageSetTag;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.List;

/**
 * @author Trey Roby
 */
public class DummyInventoryUI implements SearchUI {
    public String getKey() {
        return "DummyInventory";
    }

    public String getPanelTitle() {
        return "Inventory";
    }

    public String getSearchTitle() {
        return "Some Inventory Search";
    }

    public String getDesc() {
        return "Inventory Search";
    }

    public Widget makeUI() {
        final SimplePanel panel = new SimplePanel();
        GwtUtil.setStyle(panel, "lineHeight", "100px");

        UserServices.App.getInstance().getMissionConfig("planck", new BaseCallback() {
            @Override
            public void doSuccess(Object result) {
                MissionTag dt = (MissionTag) result;
                List<ImageSetTag> iltag = dt.getImagesetList();
                if (iltag.size() > 0) {
                    FormTag ftag = iltag.get(0).getForm();
                    Form form = GwtUtil.createSearchForm(ftag, null);
                    form.setStyleName("shadow");
                    panel.add(form);
                }
            }
        });

        return GwtUtil.wrap(panel, 50, 50, 50, 20);
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

