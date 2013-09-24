package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.VisibleListener;

/**
 * Date: Jun 1, 2009
 *
 * @author loi
 * @version $Id: AbstractTablePreview.java,v 1.9 2011/04/27 19:33:01 roby Exp $
 */
public abstract class AbstractTablePreview extends Composite implements RequiresResize, TablePreview, VisibleListener {

    private String id= null;
    private String name;
    private String shortDesc;
    private EventHub eventHub;

    protected AbstractTablePreview() {
    }

    protected AbstractTablePreview(String name, String shortDesc) {
        this(null, name, shortDesc);
    }

    protected AbstractTablePreview(Widget display, String name, String shortDesc) {
        this.name = name;
        this.shortDesc = shortDesc;
        if (display != null) {
            setDisplay(display);
        }
    }


    protected EventHub getEventHub() {
        return eventHub;
    }

    public void bind(EventHub hub) {
        this.eventHub = hub;
        eventHub.bind(this);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public String getName() {
        return name;
    }

    public Widget getDisplay() {
        return this;
    }

    public void setPreviewVisible(boolean v) {
        Widget w= getDisplay();
        if (w!=null && v!=w.isVisible()) {
            w.setVisible(v);
            if (v) onShow();
            else onHide();
        }
    }

    public void setID(String id) {
        this.id= id;
    }

    public String getID() {
        return id;
    }

    protected void setDisplay(Widget display) {
        this.initWidget(display);
    }

    public void onShow() {
        if (getDisplay() != null) {
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    updateDisplay(eventHub.getActiveTable());
                }
            });
        }
    }

        public void onHide() {
    }

    public boolean isInitiallyVisible() { return true; }

    abstract protected void updateDisplay(TablePanel table);


    public int getPrefHeight() { return 0; }

    public int getPrefWidth() { return 0; }

    public void onResize() {
        Widget w= getWidget();
        if (w instanceof RequiresResize) ((RequiresResize)w).onResize();
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
