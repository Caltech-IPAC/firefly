package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.DynHomeCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HtmlRegionLoader;
import edu.caltech.ipac.firefly.core.LoginToolbar;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.MenuItemAttrib;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.dyn.DynData;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.dyn.xstream.AccessTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.rpc.DynService;
import edu.caltech.ipac.firefly.rpc.DynServiceAsync;
import edu.caltech.ipac.firefly.ui.panels.SearchPanel;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DynProjectTask extends ServerTask<ProjectTag> {

    private String projectId;
    private String searchId;
    private boolean logoutFlag;


    /**
     *  moved logic to DynRequestHandler
     */
    @Deprecated
    public static void prepare(Widget maskW, String projectId) {
        DynProjectTask task = new DynProjectTask(maskW, projectId, null, false);
        task.start();
    }

    /**
     *  moved logic to DynRequestHandler
     */
    @Deprecated
    public static void prepare(Widget maskW, String projectId, String searchId, boolean logoutFlag) {
        DynProjectTask task = new DynProjectTask(maskW, projectId, searchId, logoutFlag);
        task.start();
    }


    protected DynProjectTask(Widget maskW, String projectId, String searchId, boolean logoutFlag) {
        super(maskW, "Preparing Data...", true);
        this.projectId = projectId;
        this.searchId = searchId;
        this.logoutFlag = logoutFlag;
    }

    public static void setOverrideProperties(List<ParamTag> properties) {
        WebAppProperties wap = Application.getInstance().getProperties();
        for (ParamTag p : properties) {
            wap.setPreference(p.getKey(), p.getValue());
        }
    }

    /**
     *  Deprecated
     *  moved logic to DynRequestHandler  -- should remove eventually.
     */
    @Override
    public void onSuccess(ProjectTag data) {
        if (data == null) {
            Request req = new Request(DynHomeCmd.COMMAND_NAME, "Welcome Page", true, false);
            Application.getInstance().processRequest(req);

            PopupUtil.showInfo("XML configuration file is invalid!  Verify that the XML file was created with the latest project.dtd");

            return;
        }

        DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);
        hData.setProjectData(projectId, data);

        String title = data.getTitle();

        // set project override properties
        setOverrideProperties(data.getOverrideProperties());

        HtmlRegionLoader f = new HtmlRegionLoader();

        String smIconRegion = data.getPropertyValue(LayoutManager.SMALL_ICON_REGION);
        if (!StringUtils.isEmpty(smIconRegion)) {
            f.load(smIconRegion, LayoutManager.SMALL_ICON_REGION);
        } else {
            f.unload(LayoutManager.SMALL_ICON_REGION);
        }

        String smIconRegion2 = data.getPropertyValue(LayoutManager.SMALL_ICON_REGION2);
        if (!StringUtils.isEmpty(smIconRegion2)) {
            f.load(smIconRegion2, LayoutManager.SMALL_ICON_REGION2);
        } else {
            f.unload(LayoutManager.SMALL_ICON_REGION2);
        }

        String footerRegion = data.getPropertyValue(LayoutManager.FOOTER_REGION);
        if (!StringUtils.isEmpty(footerRegion)) {
            f.load(footerRegion, LayoutManager.FOOTER_REGION);
        } else {
            f.unload(LayoutManager.FOOTER_REGION);
        }

        List<MenuItemAttrib> searchMenu = hData.getSearchMenu(projectId);

        Param p = new Param(DynUtils.HYDRA_PROJECT_ID, projectId);
        ArrayList<Param> pList = new ArrayList<Param>();
        pList.add(p);

        SearchPanel.getInstance().setApplicationContext(pList, searchMenu.toArray(new MenuItemAttrib[searchMenu.size()]));

        List<String> sCmds = hData.getSearchCommands(projectId);
        Request req;
        if (logoutFlag) {
            if (!sCmds.contains(searchId)) {
                searchId = null;
            }
        }
        if (searchId == null) {
            searchId = sCmds.get(0);
        }

        // check role access
        SearchTypeTag tag = data.getSearchType(searchId);
        if (tag != null) {
            AccessTag aTag = tag.getAccess();
            if (aTag != null) {
                LoginToolbar ltb = Application.getInstance().getLoginManager().getToolbar();
                if (!DynUtils.checkRoleAccess(aTag) && ltb.getCurrentUser().isGuestUser()) {
                    ltb.login();
                    return;
                }
            }
        }

        req = new Request(searchId, "Welcome Page", true, false);
        req.setIsDrilldownRoot(true);

        req = DynUtils.makeHydraRequest(req, projectId);
        Application.getInstance().processRequest(req);
    }


    @Override
    public void doTask(AsyncCallback<ProjectTag> passAlong) {
        DynServiceAsync cserv = DynService.App.getInstance();
        cserv.getProjectConfig(projectId, passAlong);
    }

}