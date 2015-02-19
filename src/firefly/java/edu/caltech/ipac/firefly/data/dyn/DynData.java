/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.DynSearchCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.MenuItemAttrib;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectListTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchGroupTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DynData {

    public static final String COMMAND_NAME = DynUtils.HYDRA_COMMAND_NAME_PREFIX;

    private ProjectListTag data;
    private HashMap<String, ProjectData> projectMap;

    private HashMap<String, SplitLayoutPanelData> slpMap = new LinkedHashMap<String, SplitLayoutPanelData>();


    public DynData() {
        projectMap = new HashMap<String, ProjectData>();
    }


    public ProjectListTag getProjectList() {
        return data;
    }

    public void setProjectList(ProjectListTag data) {
        this.data = data;
    }


    public boolean containsProject(String projectId) {
        return (projectMap.containsKey((projectId)));
    }

    public ProjectTag getProject(String projectId) {
        ProjectData pData = projectMap.get(projectId);
        if (pData != null) {
            return pData.getProject();
        }

        return null;
    }

    public ProjectData getProjectData(String projectId) {
        return projectMap.get(projectId);
    }


    public List<String> getSearchCommands(String projectId) {
        ProjectData pData = projectMap.get(projectId);
        if (pData != null) {
            return pData.getSearchCommands();
        }

        return null;
    }

    public List<MenuItemAttrib> getSearchMenu(String projectId) {
        ProjectData pData = projectMap.get(projectId);
        if (pData != null) {
            List<SearchGroupTag> groups = pData.getProject().getSearchGroups();
            ArrayList<MenuItemAttrib> root = new ArrayList<MenuItemAttrib>();
            for (SearchGroupTag g : groups) {
                MenuItemAttrib item = new MenuItemAttrib(g.getName(), g.getTitle(), g.getTitle(), g.getTitle(), null);
                for(SearchTypeTag type : g.getSearchTypes()) {
                    String cmdName = toGlobalCommandName(pData.getProject().getName(), type.getName());
                    item.addMenuItem(new MenuItemAttrib(cmdName, type.getTitle(), type.getTitle(), type.getTooltip(), null));
                }
                root.add(item);
            }
            return root;
        }

        return null;
    }

    public void setProjectData(String projectId, ProjectTag data) {
        projectMap.put(projectId, new ProjectData(data));
    }


    public void addSplitLayoutPanelItem(String groupId, SplitLayoutPanelData data) {
        slpMap.put(groupId, data);
    }

    public SplitLayoutPanelData getSplitLayoutPanelItem(String groupId) {
        return slpMap.get(groupId);
    }

    public static class SplitLayoutPanelData {
        private DockLayoutPanel parentSLP;
        private Map<String, Widget> widgetMap;
        private Map<String, Double> sizeMap;

        public SplitLayoutPanelData(DockLayoutPanel slp) {
            parentSLP = slp;
        }

        public DockLayoutPanel getSplitLayoutPanel() {
            return parentSLP;
        }

        public void addWidget(String id, Widget w) {
            if (widgetMap == null) {
                widgetMap = new HashMap<String, Widget>();
            }

            widgetMap.put(id, w);
        }

        public Map<String, Widget> getWidgetMap() {
            return widgetMap;
        }

        public void addSize(String id, Double size) {
            if (sizeMap == null) {
                sizeMap = new HashMap<String, Double>();
            }

            sizeMap.put(id, size);
        }

        public Map<String, Double> getSizeMap() {
            return sizeMap;
        }
    }

    public static String toGlobalCommandName(String projId, String searchTypeName) {
        return COMMAND_NAME + projId + "_" + searchTypeName;

    }

    public class ProjectData {
        private Map<String, String> searchCommands;
        private ProjectTag projectTag;

        public ProjectData(ProjectTag data) {
            projectTag = data;
            searchCommands = new LinkedHashMap<String, String>();

            Map<String, GeneralCommand> cmdTable = Application.getInstance().getCommandTable();
            List<SearchTypeTag> stList = projectTag.getSearchTypes();
            for (SearchTypeTag st : stList) {
                boolean access = DynUtils.checkRoleAccess(st.getAccess());
                if (access) {
                    String cmdName = st.getCommandId();
                    if (cmdName != null) {
                        searchCommands.put(cmdName, st.getTitle());

                        // overwrite search panel title, according to XML
                        cmdTable.get(cmdName).setLabel(st.getTitle());

                    } else {
                        String searchTypeCommandName = toGlobalCommandName(projectTag.getName(), st.getName());
                        cmdTable.put(searchTypeCommandName, new DynSearchCmd(projectTag.getName(), searchTypeCommandName, st));
                        searchCommands.put(searchTypeCommandName, null);
                    }
                }
            }
        }


        public String getProjectProperty(String key) {
            String value = null;

            List<ParamTag> params = projectTag.getProperties();
            for (ParamTag param : params) {
                if (key.equalsIgnoreCase(param.getKey())) {
                    return param.getValue();
                }
            }

            return value;
        }

        public ProjectTag getProject() {
            return projectTag;
        }

        public List<String> getSearchCommands() {
            LinkedList<String> ll = new LinkedList<String>();
            for (Map.Entry<String, String> en : searchCommands.entrySet()) {
                String searchCmd = en.getKey();
                String commandTitle = en.getValue();

                // overwrite search panel title, according to XML
                if (commandTitle != null) {
                    Application.getInstance().getCommandTable().get(searchCmd).setLabel(commandTitle);
                }

                ll.add(searchCmd);
            }
            return ll;
        }

    }

}

