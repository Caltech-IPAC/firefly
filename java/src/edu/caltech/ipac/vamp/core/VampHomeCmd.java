package edu.caltech.ipac.vamp.core;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.commands.DynHomeCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.dyn.DynData;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectItemTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectListTag;
import edu.caltech.ipac.firefly.ui.GwtUtil;


/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Apr 14, 2010
 * Time: 5:14:58 PM
 * To change this template use File | Settings | File Templates.
 */
@Deprecated
public class VampHomeCmd extends DynHomeCmd {
    public static final String COMMAND_NAME = "VampHome";

    @Override
    protected void doExecute(Request req, AsyncCallback<String> callback) {
        // display list of projects
        VerticalPanel vp = new VerticalPanel();
        DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);
        ProjectListTag obj = hData.getProjectList();
        for (ProjectItemTag objItem : obj.getProjectItems()) {
            boolean active = Boolean.parseBoolean(objItem.getActive());
            if (active) {
                final String projectId = objItem.getId();
                final String xmlFile = objItem.getConfigFile();
                vp.add(GwtUtil.makeLinkButton("  " + objItem.getDisplay(), objItem.getTooltip(), new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        Request req = new Request(null, "Welcome Page", true, false);
                        req = DynUtils.makeHydraRequest(req, projectId);
                        Application.getInstance().processRequest(req);
                    }
                }));


            } else {
                vp.add(new Label(objItem.getDisplay()));
            }

        }

        vp.setWidth("100%");
        vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        vp.setSpacing(10);

        //main.setUrl(HOME_URL);
        registerView(LayoutManager.CONTENT_REGION, vp);

        callback.onSuccess("success!");
    }
}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
