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
