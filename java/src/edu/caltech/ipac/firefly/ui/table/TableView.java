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
    private boolean isHidden = false;

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
        return isHidden;
    }

    public void setHidden(boolean flg) {
        this.isHidden = flg;
    }
}
