package edu.caltech.ipac.firefly.data;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

public class NewTabInfo {
    private String tabPaneId;
    private Widget display;
    private String name;
    private String tooltips;
    private boolean removable;
    private Command onLoadedCmd;
    private boolean isLoaded;

    public NewTabInfo(String name) {
        this(null, null, name, name, true);
    }

    public NewTabInfo(String tabPaneId, Widget display, String name, String tooltips, boolean removable) {
        this.tabPaneId = tabPaneId;
        this.display = display;
        this.name = name;
        this.tooltips = tooltips;
        this.removable = removable;
    }

    public String getTabPaneId() {
        return tabPaneId;
    }

    public void setTabPaneId(String tabPaneId) {
        this.tabPaneId = tabPaneId;
    }

    public Widget getDisplay() {
        return display;
    }

    public void setDisplay(Widget display) {
        this.display = display;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTooltips() {
        return tooltips;
    }

    public void setTooltips(String tooltips) {
        this.tooltips = tooltips;
    }

    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public void ready() {
        this.isLoaded = true;
        if (onLoadedCmd != null) onLoadedCmd.execute();
    }

    public void setOnLoadedAction(Command cmd) {
        onLoadedCmd = cmd;
        if (onLoadedCmd != null && isLoaded) {
            onLoadedCmd.execute();
        }
    }
}

