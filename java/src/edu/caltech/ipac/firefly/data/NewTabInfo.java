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
