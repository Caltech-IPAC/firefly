package edu.caltech.ipac.hydra.ui.finderchart;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseEventWorker;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.Arrays;
import java.util.List;

/**
* Date: 9/12/14
*
* @author loi
* @version $Id: $
*/
public class FinderChartResultsController extends BaseEventWorker {
    private TabPane sourceTab;
    private TablePanel sourceTable;
    private Widget imageGrid;
    private TabPane catalogTab;
    private DockLayoutPanel layoutPanel;

    public FinderChartResultsController() {

        super(ID);
        setEventsByName(Arrays.asList(EventHub.ON_TABLE_ADDED, EventHub.ON_ROWHIGHLIGHT_CHANGE, EventHub.ON_TABLE_REMOVED));
    }

    protected void handleEvent(WebEvent ev) {
        List<DockLayoutPanel> lap = getEventHub().getLayoutPanels();
        if (lap != null && lap.size() > 0) {
            if (layoutPanel == null) {
                layoutPanel = lap.get(0);
            }
            if (sourceTab == null) {
                sourceTab = (TabPane) GwtUtil.findById(layoutPanel, "fc_source_tab");
            }

            if (sourceTable == null) {
                sourceTable = (TablePanel) GwtUtil.findById(layoutPanel, "fc_source_table");
                Widget downloadBtn = GwtUtil.findById(sourceTable, "FinderChartDownload");
                sourceTable.clearToolButtons(true, false, false);
            }

            if (imageGrid == null) {
                imageGrid = GwtUtil.findById(layoutPanel, "fc_image_grid");
            }
            if (catalogTab == null) {
                catalogTab = (TabPane) GwtUtil.findById(layoutPanel, "fc_catalog_tab");
            }
        }

        if (sourceTable.isInit()) {
            handleSourceTable();
        } else {
            sourceTable.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    handleSourceTable();
                    sourceTable.getEventManager().removeListener(TablePanel.ON_INIT, this);
                }
            });
        }

        if (catalogTab != null && catalogTab.getWidgetCount() > 0) {
            GwtUtil.SplitPanel.showWidget(layoutPanel, catalogTab);
        } else {
            GwtUtil.SplitPanel.hideWidget(layoutPanel, catalogTab);
        }
    }

    private void handleSourceTable() {
        if (sourceTable != null && sourceTable.getDataModel().getTotalRows() > 1) {
            GwtUtil.SplitPanel.showWidget(layoutPanel, sourceTab);
        } else {
            GwtUtil.SplitPanel.hideWidget(layoutPanel, sourceTab);
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
