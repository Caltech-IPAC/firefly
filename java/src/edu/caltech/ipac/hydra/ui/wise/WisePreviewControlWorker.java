package edu.caltech.ipac.hydra.ui.wise;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.dyn.DynData;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.ResultUIComponent;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseFormEventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.previews.DataViewerPreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class WisePreviewControlWorker extends BaseFormEventWorker {

    private static final String DIFF_SPIKES_ID = "diffspikes_ID";
    private static final String HALOS_ID = "halos_ID";
    private static final String GHOSTS_ID = "ghosts_ID";
    private static final String LATENTS_ID = "latents_ID";
    private static final String GROUP_ID = "groupId";
    private static final String PRESERVE_SIZE = "preserveImageSizeOnBandSelect";
    private static final String EVENT_TAB_PANE_NAME = "eventTabPaneName";
    private static final String LEVEL_MAP = "levelMap";

    private static final String BAND_ID = "band_ID";

    private static String[] idAry = new String[]{};
    private static String[] idAryHidden = new String[]{};

//    private FormHub fHub;
    private EventHub tHub;
    private Map<String, List<String>> _artifactGroups = new HashMap<String, List<String>>(7);

    private String groupId;
    private boolean preserveSize = false;
    private DynData.SplitLayoutPanelData panelData;

    private String selectedBands = "_all_";
    private Map<String, String> levelMap = new HashMap<String, String>();
    private String level;
    private String tabPaneName;
    private Set<String> prefInitialized = new HashSet<String>();


    WisePreviewControlWorker(Map<String, String> params) {
        addGroup(DIFF_SPIKES_ID, params.get(DIFF_SPIKES_ID));
        addGroup(HALOS_ID, params.get(HALOS_ID));
        addGroup(GHOSTS_ID, params.get(GHOSTS_ID));
        addGroup(LATENTS_ID, params.get(LATENTS_ID));

        groupId = params.get(GROUP_ID);

        String pSizeStr = params.get(PRESERVE_SIZE);
        if (pSizeStr.equalsIgnoreCase("true")) {
            preserveSize = true;
        }

        tabPaneName = params.get(EVENT_TAB_PANE_NAME);

        String lvlMap = params.get(LEVEL_MAP);
        String[] lvlArr = lvlMap.split(";");
        for (String lvlItem : lvlArr) {
            String[] lvlItemArr = lvlItem.split("=");
            if (lvlItemArr.length == 2) {
                levelMap.put(lvlItemArr[0], lvlItemArr[1]);
            }
        }

        configureBands(params.get(BAND_ID));
    }

    private DynData.SplitLayoutPanelData getPanelData() {
        if (panelData == null && groupId != null) {
            DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);
            panelData = hData.getSplitLayoutPanelItem(groupId);
        }
        return panelData;
    }

    private void configureBands(String bands) {
        if (!StringUtils.isEmpty(bands)) {
            String[] bandArr = bands.split(",");
            selectedBands = "";
            int i = 0;
            for (String b : bandArr) {
                if (i > 0) {
                    selectedBands += ",";
                }
                selectedBands += "prev_band_" + b;
                i++;
            }
        }
    }

    private void configureForm(String tabName) {
        level = levelMap.get(tabName);
        if (StringUtils.isEmpty(level)) {
            String msg = "Bad configuration; level is missing from Level Map";
            GWT.log(msg, null);
            throw new NullPointerException(msg);
        }

        if (!prefInitialized.contains(level)) {
            String artVals = Preferences.get("wise.artifacts." + level);
            if (!StringUtils.isEmpty(artVals)) {
                if (containsField("artifactShow_" + level)) {
                    // for the time being we are going to force level 3 artifacts to be off by default
                    if (containsField("artifactShow_1")) {
                        setValue(new Param("artifactShow_" + level, artVals));
                    }
                    else {
                        setValue(new Param("artifactShow_" + level, "false"));
                    }
                }
            }

            prefInitialized.add(level);
        }

        if (level.equalsIgnoreCase("1b")) {
            idAry = new String[]{DIFF_SPIKES_ID, GHOSTS_ID, LATENTS_ID};
            idAryHidden = new String[]{HALOS_ID};
            setVisible("artifactShow_1b", true);
            setVisible("artifactShow_3a", false);

        } else if (level.equalsIgnoreCase("3a")) {
            idAry = new String[]{DIFF_SPIKES_ID, HALOS_ID, GHOSTS_ID, LATENTS_ID};
            idAryHidden = new String[]{};
            setVisible("artifactShow_1b", false);
            setVisible("artifactShow_3a", true);

        } else if (level.equalsIgnoreCase("3o")) {
            idAry = new String[]{};
            idAryHidden = new String[]{DIFF_SPIKES_ID, HALOS_ID, GHOSTS_ID, LATENTS_ID};
            setVisible("artifactShow_1b", false);
            setVisible("artifactShow_3a", false);
        }
    }

    public void bind(FormHub hub) {
//        if (hub != null) {
//            addHub(hub);
//
//            hub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, new WebEventListener() {
//                public void eventNotify(WebEvent ev) {
//                    if (level != null) {
//                        updatePreviews();
//                        updateArtifacts();
//                    }
//                }
//            });
//
//            hub.getEventManager().addListener(FormHub.FORM_REINIT, new WebEventListener() {
//                public void eventNotify(WebEvent ev) {
//                    level = null;
//                    if (containsField("previewShow")) {
//                        setValue(new Param("previewShow", selectedBands));
//                    }
//                }
//            });
//        }
    }

    public void bind(EventHub hub) {
//        tHub = hub;
//
//        tHub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, new WebEventListener() {
//            public void eventNotify(WebEvent ev) {
//                String title = tHub.getActiveTable().getTitle();
//
//                // ignore tabs that are not ours (e.g. Catalog Search)
//                if (!StringUtils.isEmpty(title) && levelMap.containsKey(title)) {
//                    configureForm(title);
//
//                    updatePreviews();
//                    updateArtifacts();
//                }
//            }
//        });
//
//        tHub.getEventManager().addListener(EventHub.ON_TAB_SELECTED, new WebEventListener() {
//            public void eventNotify(WebEvent ev) {
//                TabPane.Tab tab = (TabPane.Tab) ev.getData();
//                if (tab != null) {
//                    String name = tab.getName();
//
//                    // ignore tabs that are not ours (e.g. Catalog Search)
//                    if (!StringUtils.isEmpty(name) && tab.getPaneName().equals(tabPaneName) && levelMap.containsKey(name)) {
//                        configureForm(name);
//
//                        updatePreviews();
//                        updateArtifacts();
//                    }
//                }
//            }
//        });
    }

    private void updateArtifacts() {
//        for (String id : idAry) {
//            boolean enabled = isArtifactEnabled(id);
//            for (EventWorker evW : findWorkerList(id)) {
//                evW.setEnabled(enabled);
//            }
//        }
//        for (String id : idAryHidden) {
//            for (EventWorker evW : findWorkerList(id)) {
//                evW.setEnabled(false);
//            }
//        }
//
//        String artVals = getValue("artifactShow_" + level);
//        Preferences.set("wise.artifacts." + level, artVals);
    }

    private void updatePreviews() {
        // OLD METHOD
        //for (TablePreview p : tHub.getPreviews()) {
        //    if (p instanceof DataViewerPreview) {
        //        DataViewerPreview pp = (DataViewerPreview) p;
        //        pp.setPreviewVisible(isPreviewEnabled(pp.getID()));
        //    }
        //}

        // selected items
        String v = getValue("previewShow");
        List<String> selList = new ArrayList<String>(Arrays.asList(v.split(",")));
        DynData.SplitLayoutPanelData pdata = getPanelData();
        if (pdata == null) return;

        Map<String, Widget> wMap = pdata.getWidgetMap();
        Map<String, Double> dMap = pdata.getSizeMap();
        DockLayoutPanel slp = pdata.getSplitLayoutPanel();

        if (wMap == null) return;

        if (preserveSize) {
            // find out which changed
            boolean makeVisible = false;
            Widget deltaWidget = null;
            double deltaSize = 0;

            for (Map.Entry<String, Widget> wEntry : wMap.entrySet()) {
                String id = wEntry.getKey();
                Widget w = wEntry.getValue();

                if (selList.contains(id) && GwtUtil.SplitPanel.isHidden(w)) {
                    makeVisible = true;
                    deltaWidget = w;
                    showWidget(slp, w);
                    deltaSize = GwtUtil.SplitPanel.getDockWidgetSize(deltaWidget);

                    break;

                } else if (!selList.contains(id) && !GwtUtil.SplitPanel.isHidden(w)) {
                    makeVisible = false;
                    deltaWidget = w;
                    deltaSize = GwtUtil.SplitPanel.getDockWidgetSize(deltaWidget);
                    hideWidget(slp, w);

                    break;
                }
            }

            // get affected widgets
            ArrayList<String> affectedWidgetIds = new ArrayList<String>();
            for (Map.Entry<String, Widget> wEntry : wMap.entrySet()) {
                String id = wEntry.getKey();
                Widget w = wEntry.getValue();

                if (w == deltaWidget) {
                    continue;

                } else if (!GwtUtil.SplitPanel.isHidden(w)) {
                    affectedWidgetIds.add(id);
                }
            }

            // adjust affected widgets
            for (String wId : affectedWidgetIds) {
                Widget w = wMap.get(wId);
                Double wOrigSize = dMap.get(wId);

                double newSize;
                if (makeVisible) {
                    newSize = GwtUtil.SplitPanel.getDockWidgetSize(w) - (deltaSize / affectedWidgetIds.size());
                } else {
                    newSize = GwtUtil.SplitPanel.getDockWidgetSize(w) + (deltaSize / affectedWidgetIds.size());
                }

                // compare newSize with original size
                if (newSize < wOrigSize) {
                    newSize = wOrigSize;
                }

                GwtUtil.SplitPanel.setWidgetChildSize(w, newSize);
            }

            slp.forceLayout();

        } else {
            if (selList.size() == wMap.size()) {
                for (Map.Entry<String, Widget> wEntry : wMap.entrySet()) {
                    String id = wEntry.getKey();
                    Widget w = wEntry.getValue();
                    Double d = dMap.get(id);

                    showWidget(slp, w);
                    GwtUtil.SplitPanel.setWidgetChildSize(w, d);
                }

            } else {
                // get original sizes
                double totalSize = 0;
                for (Map.Entry<String, Double> dEntry : dMap.entrySet()) {
                    Double d = dEntry.getValue();
                    totalSize += d;
                }

                // show/hide
                for (Map.Entry<String, Widget> wEntry : wMap.entrySet()) {
                    String id = wEntry.getKey();
                    Widget w = wEntry.getValue();
                    if (selList.contains(id)) {
                        showWidget(slp, w);
                        GwtUtil.SplitPanel.setWidgetChildSize(w, (totalSize / selList.size()));

                    } else {
                        hideWidget(slp, w);
                    }
                }
            }

            slp.forceLayout();
        }
    }


    private void showWidget(DockLayoutPanel slp, Widget w) {
        if (slp.getWidgetIndex(w) >= 0) {
            GwtUtil.SplitPanel.showWidget(slp, w);
        }
        DataViewerPreview dvp = findDataviewer(w);
        if (dvp != null) dvp.setPreviewVisible(true);
        notify(w, true);
    }

    private void hideWidget(DockLayoutPanel slp, Widget w) {
        if (slp.getWidgetIndex(w) >= 0) {
            GwtUtil.SplitPanel.hideWidget(slp, w);
        }
        DataViewerPreview dvp = findDataviewer(w);
        if (dvp != null) dvp.setPreviewVisible(false);
        notify(w, false);
    }


    private DataViewerPreview findDataviewer(Widget inW) {
        DataViewerPreview retval = null;

        if (inW instanceof DataViewerPreview) {
            return (DataViewerPreview) inW;
        } else {
            if (inW != null && inW instanceof HasWidgets) {
                HasWidgets p = (HasWidgets) inW;
                for (Widget w : p) {
                    if (w instanceof DataViewerPreview) {
                        retval = (DataViewerPreview) w;
                        break;
                    } else if (w instanceof HasWidgets) {
                        Widget tstW = findDataviewer(w);
                        if (tstW instanceof DataViewerPreview) {
                            retval = (DataViewerPreview) tstW;
                            break;
                        }
                    }
                }
            }
        }
        return retval;
    }

    private void notify(Widget parent, boolean v) {
        if (parent instanceof ResultUIComponent) {
            if (v) ((ResultUIComponent) parent).onShow();
            else ((ResultUIComponent) parent).onHide();
        } else {
            if (parent instanceof HasWidgets) {
                for (Widget w : ((HasWidgets) parent)) {
                    notify(w, v);
                }
            }
        }
    }

    private void addGroup(String key, String data) {
        if (!StringUtils.isEmpty(data)) {
            List<String> l = new ArrayList<String>(5);
            for (String s : data.split(",")) {
                l.add(s);
            }
            _artifactGroups.put(key, l);
        }
    }

    private List<EventWorker> findWorkerList(String groupID) {
        List<String> idList = _artifactGroups.get(groupID);
        List<EventWorker> workList = tHub.getEventWorkers();
        List<EventWorker> retList = new ArrayList<EventWorker>(4);
        if (idList != null && idList.size() > 0) {
            for (String id : idList) {
                if (!StringUtils.isEmpty(id)) {
                    for (EventWorker evw : workList) {
                        if (id.equals(evw.getID())) {
                            retList.add(evw);
                            break;
                        }
                    }
                }

            }
        }

        return retList;
    }

    private boolean isArtifactEnabled(String id) {
        if (id == null || level == null || level.length() == 0)
            return false;

        String v = getValue("artifactShow_" + level);
        if (!StringUtils.isEmpty(v)) {
            List<String> l = new ArrayList<String>(Arrays.asList(v.split(",")));
            return l.contains(id);

        } else {
            return false;
        }
    }

    private boolean isPreviewEnabled(String id) {
        if (id == null)
            return true;

        String v = getValue("previewShow");
        if (!StringUtils.isEmpty(v)) {
            List<String> l = new ArrayList<String>(Arrays.asList(v.split(",")));
            return l.contains(id);

        } else {
            return false;
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
