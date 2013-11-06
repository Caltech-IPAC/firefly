package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

/**
* Date: 3/4/13
*
* @author loi
* @version $Id: $
*/
public class TableView implements TablePanel.View {
    public static final Name NAME = new Name("Table View",
                                                        "Display the content in a table format");
    private TablePanel tablePanel = null;
    private TablePanel.View cview;
    private FocusPanel mainPanel;

    public int getViewIdx() {
        return 0;
    }

    public Name getName() {
        return NAME;
    }

    public String getShortDesc() {
        return NAME.getDesc();
    }

    public Widget getDisplay() {
        return mainPanel;

    }

    public void onViewChange(TablePanel.View newView) {
        cview = newView;
        if (newView == this) {
            tablePanel.getTable().scrollHighlightedIntoView();
        }
    }

    public TablePanel getTablePanel() {
        return tablePanel;
    }

    public void onMaximize() {
    }

    public void onMinimize() {
    }

    public ImageResource getIcon() {
        return IconCreator.Creator.getInstance().getTableView();
    }

    public void bind(TablePanel table) {
        this.tablePanel = table;
        mainPanel = new FocusPanel(table.getTable());
        DOM.setStyleAttribute(mainPanel.getElement(), "outline", "yellow dotted thin");
        mainPanel.addStyleName("expand-fully");
        tablePanel.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        if (tablePanel.getTable() != null) {
                            mainPanel.addKeyDownHandler(new TablePanel.HighlightedKeyMove(tablePanel.getTable().getDataTable()));
                        }
                        tablePanel.getEventManager().removeListener(this);
                    }
                });
    }

    public void bind(EventHub hub) {
    }

    public boolean isHidden() {
        return false;
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
