package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.draw.DataConnection;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.google.gwt.user.client.ui.DockLayoutPanel;

/**
 * Date: Jan 8, 2012
 *
 * @author loi
 * @version $Id: TableViewListenerCreator.java,v 1.2 2012/01/26 00:23:10 loi Exp $
 */
public class TableViewListenerCreator implements EventWorkerCreator {

    public static final String TYPE = "TableViewListener";

    enum TargetType {TableRow,QueryCenter,TableRowByPlot}

    public EventWorker create(Map<String, String> params) {
        TableViewListener worker = new TableViewListener();
        worker.setParams(params);
        
        worker.setEvents(StringUtils.asList(params.get(CommonParams.EVENTS_PARAM), ","));
        worker.setQuerySources(StringUtils.asList(params.get(EventWorker.QUERY_SOURCE), ","));

        // need to create the visiMappings.. and hook up events.

        List<String> views = StringUtils.asList(params.get("views"), ",");
        for(String v : views) {
            List<String> shows = StringUtils.asList(params.get(v+".show"), ",");
            List<String> hides = StringUtils.asList(params.get(v+".hide"), ",");
            if (shows != null && shows.size() > 0) {
                worker.addMappings("show", v, null, shows.toArray(new String[shows.size()]));
            }
            if (hides != null && hides.size() > 0) {
                worker.addMappings("hide", v, null, hides.toArray(new String[hides.size()]));
            }
        }

        return worker;
    }


    static class TableViewListener extends BaseEventWorker<DataConnection> {

        private enum Region {north, south, east, west, center};

        /**
         * key by view_name.[show|hide]
         * value is a list of dockpanel_id.direction(north,south,east,west)
         */
        private Map<String, List<String>> visiMappings = new HashMap<String, List<String>>();

        public TableViewListener() {
            super(TYPE);
        }

        @Override
        public void bind(EventHub hub) {
            super.bind(hub);
        }

        public void addMappings(String showHide, String viewName, String panelId, String... directions) {
            viewName = StringUtils.isEmpty(viewName) ? "" : viewName;
            panelId = StringUtils.isEmpty(panelId) ? "" : panelId;

            ArrayList dirs = new ArrayList(directions.length);
            for (String dir : directions) {
                dirs.add(panelId + "." + dir);
            }
            visiMappings.put(viewName + "." + showHide, dirs);
        }

        public void handleEvent(WebEvent ev) {

            if (!(ev.getSource() instanceof TablePanel)) return;

            TablePanel table = (TablePanel) ev.getSource();
            String view = StringUtils.isEmpty(ev.getData()) ? table.getVisibleView().getName() : ev.getData().toString();

            if (!this.getQuerySources().contains(table.getName())){
                return;
            }

            List<String> showList = new ArrayList<String>();
            List<String> hideList = new ArrayList<String>();

            addToList(showList, ".show");
            addToList(hideList, ".hide");
            addToList(showList, view + ".show");
            addToList(hideList, view + ".hide");

            for(DockLayoutPanel p : getEventHub().getLayoutPanels()) {
                for(Region r : Region.values()) {
                    if (showList.indexOf("." + r.name()) >=0 ||
                        showList.indexOf(p.getTitle() + "." + r.name()) >= 0 ) {
                        processRegion(p, r.name(), true);
                    }
                    if (hideList.indexOf("." + r.name()) >=0 ||
                        hideList.indexOf(p.getTitle() + "." + r.name()) >= 0 ) {
                        processRegion(p, r.name(), false);
                    }
                }
            }

        }

        private void addToList(List<String> list, String key) {
            if (visiMappings.containsKey(key)) {
                list.addAll(visiMappings.get(key));
            }
        }

        private static void processRegion(DockLayoutPanel p, String region, boolean show) {

            DockLayoutPanel.Direction dir = region.equals("north") ? DockLayoutPanel.Direction.NORTH :
                                            region.equals("south") ? DockLayoutPanel.Direction.SOUTH :
                                            region.equals("east") ? DockLayoutPanel.Direction.EAST :
                                            region.equals("west") ? DockLayoutPanel.Direction.WEST : DockLayoutPanel.Direction.CENTER;
            for(int i = 0; i < p.getWidgetCount(); i++) {
                DockLayoutPanel.Direction wd = p.getWidgetDirection(p.getWidget(i));
                if (wd == dir) {
                    if (show) {
                        GwtUtil.DockLayout.showWidget(p, p.getWidget(i));
                    } else {
                        GwtUtil.DockLayout.hideWidget(p, p.getWidget(i));
                    }
                }
            }
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

