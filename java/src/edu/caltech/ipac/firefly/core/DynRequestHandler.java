package edu.caltech.ipac.firefly.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.DynHomeCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.MenuItemAttrib;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.dyn.DynData;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.dyn.xstream.AccessTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectListTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.rpc.DynService;
import edu.caltech.ipac.firefly.rpc.DynServiceAsync;
import edu.caltech.ipac.firefly.ui.DynProjectTask;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.panels.SearchPanel;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DynRequestHandler extends DefaultRequestHandler {
    private static String currentProject = "";
    private ProjectTag curProjData = null;


    public static String getCurrentProject() {
        return currentProject;
    }

    @Override
    public SearchDescResolver getSearchDescResolver() {
        Application app = Application.getInstance();
        return app.getWidgetFactory().createSearchDescResolver(getCurrentProject());
    }

    protected void processRequest(final Request req, final boolean createHistory) {
        // make sure DynData is initialized.. then proceed.
        DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);
        if (hData == null) {
            // DynData is not initialized... init.. then proceed.
            ServerTask<ProjectListTag> initData = new ServerTask<ProjectListTag>() {
                public void onSuccess(ProjectListTag result) {
                    DynData newHData = new DynData();
                    newHData.setProjectList(result);
                    Application.getInstance().setAppData(DynUtils.HYDRA_APP_DATA, newHData);
                    loadProjDataThenProceed(req, createHistory);
                }

                public void doTask(AsyncCallback<ProjectListTag> passAlong) {
                    DynServiceAsync hService = DynService.App.getInstance();
                    hService.getAllProjects(passAlong);
                }
            };
            initData.start();
        } else {
            loadProjDataThenProceed(req, createHistory);
        }
    }

    /**
     * make sure the valid project data is loaded for the currently logged in user.
     * then proceed.  if projectId is null, then it's not a Dyn request.  Defer to
     * default handler.
     *
     * @param req
     * @param createHistory
     */
    void loadProjDataThenProceed(final Request req, final boolean createHistory) {

        String id = req.getRequestId();
        // special handling for home page
        if (!StringUtils.isEmpty(id) && id.equals(DynHomeCmd.COMMAND_NAME)) {
            currentProject = "";

            HtmlRegionLoader f = new HtmlRegionLoader();
            f.unload(LayoutManager.SMALL_ICON_REGION);
            f.unload(LayoutManager.SMALL_ICON_REGION2);
            f.unload(LayoutManager.FOOTER_REGION);

            super.processRequest(req, createHistory);

        } else {
            final String projectId = req.getParam(DynUtils.HYDRA_PROJECT_ID);

            if (projectId == null) {
                // not a Dyn request... let default handler handle it.
                super.processRequest(req, createHistory);

            } else {
                currentProject = projectId;

                DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);
                if (!hData.containsProject(projectId)) {
//                    GWT.log("Loading project xml data from server " + projectId, null);

                    LayoutManager lman = Application.getInstance().getLayoutManager();
                    Widget maskW = lman.getRegion(LayoutManager.DROPDOWN_REGION).getDisplay();
                    DynProjectTask task = new DynProjectTask(maskW, projectId, "", true) {
                        public void onSuccess(ProjectTag data) {
                            DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);
                            hData.setProjectData(projectId, data);
                            changeProject(data);

                            // add project about to help menu - only do once
                            String about = data.getPropertyValue("about");
                            if (!StringUtils.isEmpty(about)) {
                                String[] aboutItems = about.split(";");
                                if (aboutItems.length == 3) {
                                    BannerHelp.setUrlHelp(aboutItems[0], aboutItems[1], 0, aboutItems[2], true);
                                }
                            }

                            // add project to help menu - only do once
                            String help = data.getPropertyValue("helpMenu");
                            if (!StringUtils.isEmpty(help)) {
                                String[] helpItems = help.split(";");
                                if (helpItems.length == 3) {
                                    if (StringUtils.isEmpty(about)) {
                                        BannerHelp.setHelp(helpItems[0], helpItems[1], 0, helpItems[2], true);
                                    } else {
                                        BannerHelp.setHelp(helpItems[0], helpItems[1], 1, helpItems[2], false);
                                    }
                                }
                            }

                            String catSearchMethod = data.getPropertyValue("catalogSearchMethod");
                            if (!StringUtils.isEmpty(catSearchMethod)) {
                                try {
                                    CatalogRequest.Method sm= Enum.valueOf(CatalogRequest.Method.class, catSearchMethod);
                                    CatalogPanel.setDefaultSearchMethod(sm);
                                } catch (Exception e) {
                                    // ignore
                                }
                            }

                            doDynProcessReq(req, createHistory);
                        }
                    };
                    task.start();

                } else {
                    ProjectTag data = hData.getProject(projectId);
                    changeProject(data);

                    doDynProcessReq(req, createHistory);
                }
            }
        }
    }

    private void changeProject(ProjectTag data) {

        if (curProjData == data) return;

        curProjData = data;

        DynProjectTask.setOverrideProperties(data.getOverrideProperties());

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

        DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);

        List<MenuItemAttrib> searchMenu = hData.getSearchMenu(data.getName());

        Param p = new Param(DynUtils.HYDRA_PROJECT_ID, data.getName());
        ArrayList<Param> pList = new ArrayList<Param>();
        pList.add(p);

        String title = data.getTitle();
        Request home = new Request("", "Welcome Page", true, false);
        DynUtils.makeHydraRequest(home, data.getName());
        
        Creator creator = Application.getInstance().getCreator();
        if (creator instanceof DefaultCreator) ((DefaultCreator)creator).setAppDesc(title);
        Application.getInstance().setHomeRequest(home);
        SearchPanel.getInstance().setApplicationContext(pList, searchMenu.toArray(new MenuItemAttrib[searchMenu.size()]));
    }

    /**
     * it's a Dyn request.  do all custom steps required of a Dyn request.
     * Once satisfied, forward to default handler.
     *
     * @param req
     * @param createHistory
     */
    void doDynProcessReq(Request req, boolean createHistory) {

        String projectId = req.getParam(DynUtils.HYDRA_PROJECT_ID);

//        GWT.log("Processing Dyn request: " + req, null);

        DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);
        ProjectTag proj = hData.getProject(projectId);

        String searchId = req.getRequestId();

        if (searchId == null || searchId.length() == 0) {
            if (SearchPanel.getInstance().getCommandIds().size() != 0) {
                String def = SearchPanel.getInstance().getCommandIds().get(0);
                SearchPanel.getInstance().processCommandRequest(def, false);
            } else {
                handleNoAccess();
            }

        } else {
            if (SearchPanel.getInstance().getCommandIds().size() == 0) {
                handleNoAccess();
                return;
            }

            if (hData.getSearchCommands(projectId).contains(searchId)) {
                // check role access
                SearchTypeTag tag = proj.getSearchType(searchId);
                if (tag != null) {
                    AccessTag aTag = tag.getAccess();
                    if (aTag != null) {
                        if (!DynUtils.checkRoleAccess(aTag)) {
                            handleNoAccess();
                            return;
                        }
                    }
                }
            }

            super.processRequest(req, createHistory);
        }
    }

    void handleNoAccess() {
        if (!Application.getInstance().getLoginManager().isLoggedIn()) {
            // if not logged in, forward to login page
            LoginToolbar ltb = Application.getInstance().getLoginManager().getToolbar();
            ltb.login();
        } else {
            PopupUtil.showInfo("You do not have access to this page.");
        }
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
