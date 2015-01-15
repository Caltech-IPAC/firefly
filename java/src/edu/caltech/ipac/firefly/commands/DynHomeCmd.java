/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.dyn.DynData;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectItemTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectListTag;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.panels.SearchPanel;
import edu.caltech.ipac.firefly.util.PropConst;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DynHomeCmd extends RequestCmd {
    public static final String COMMAND_NAME = "HydraHome";
//    public static final String HOME_URL = WebUtil.encodeUrl(GWT.getModuleBaseURL() + "hydra_home.html");

    private Frame main;

    public DynHomeCmd() {
        super(COMMAND_NAME);
    }


    @Override
    public boolean init() {
        return true;
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {

        String projName = GwtUtil.getGwtProperty("app-name");
        if (!StringUtils.isEmpty(projName) && !projName.equals("hydra")) {
            Request home = new Request("", "Welcome Page", true, false);
            DynUtils.makeHydraRequest(home, projName);
            Application.getInstance().setHomeRequest(home);
            Application.getInstance().goHome();
            return;
        }

        // display list of projects
        VerticalPanel vp = new VerticalPanel();

        // add DemoSearch2MassPosCmd for comparison
//        vp.add(new Label("Original Demo:"));
//        vp.add(GwtUtil.makeLinkButton("  DemoSearch2MassPosCmd", "DemoSearch2MassPosCmd", new ClickHandler() {
//            public void onClick(ClickEvent ev) {
//                List<String> sCmds = new ArrayList<String>();
//                sCmds.add("DemoSearch2MassPosCmd");
//                SearchPanel.getInstance().setApplicationContext("Search 2Mass", sCmds);
//                Application.getInstance().processRequest((new Request("DemoSearch2MassPosCmd", "Welcome Page", true, false)));
//            }
//        }));


//        vp.add(new Label(""));
//        vp.add(new Label("XML Configuration:"));

        DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);

        final Application app = Application.getInstance();

        // reset small logo
        app.getLayoutManager().getRegion(LayoutManager.SMALL_ICON_REGION).setDisplay(new HTML(""));

        ProjectListTag obj = hData.getProjectList();
        for (ProjectItemTag objItemTag : obj.getProjectItems()) {
            boolean active = Boolean.parseBoolean(objItemTag.getActive());
            if (active) {
                final String id = objItemTag.getId();

                boolean isCommand = Boolean.parseBoolean(objItemTag.getIsCommand());
                if (isCommand) {
                    final String display = objItemTag.getDisplay();
                    final String tooltip = objItemTag.getTooltip();

                    vp.add(GwtUtil.makeLinkButton("  " + display, tooltip, new ClickHandler() {
                        public void onClick(ClickEvent ev) {
                            // set original command title
                            app.getCommandTable().get(id).setLabel(app.getProperties().getProperty(id + "." + PropConst.TITLE));

                            List<String> sCmds = new ArrayList<String>();
                            sCmds.add(id);
                            SearchPanel.getInstance().setApplicationContext(display, sCmds);

                            Request req = new Request(id, "Welcome Page", true, false);

                            // allow breadcrumb to be visible
                            req.setIsDrilldownRoot(true);

                            app.processRequest(req);
                        }
                    }));

                } else {
                    vp.add(GwtUtil.makeLinkButton("  " + objItemTag.getDisplay(), objItemTag.getTooltip(), new ClickHandler() {
                        public void onClick(ClickEvent ev) {
                            Request req = new Request(null, "Welcome Page", true, false);
                            app.processRequest(DynUtils.makeHydraRequest(req, id));
                        }
                    }));
                }

            } else {
                vp.add(new Label(objItemTag.getDisplay()));
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

