package edu.caltech.ipac.firefly.core.layout;

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;


/**
 * This is the default layout of a GWT application.  This class should be overriden if this layout
 * does not fit the requirement.
 *
 * This manager uses a DockPanel as its main panel.
 * The top panel contains the menu toolbar.
 * The center panel is hidden behind a ScrollPanel.
 *
 * Date: Nov 1, 2007
 *
 * @author loi
 * @version $Id: FixedLayoutManager.java,v 1.41 2011/09/14 22:29:22 loi Exp $
 */
public class FixedLayoutManager extends AbstractLayoutManager {

    private DockPanel mainPanel;
    private int width;
    private int height;

    public FixedLayoutManager(int width, int height) {
        super(width, height);
        this.width = width;
        this.height = height;
        mainPanel = new DockPanel();
        mainPanel.setSize(width + "px", height + "px");
        mainPanel.setSpacing(0);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Widget getDisplay() {
        return mainPanel;
    }

    public void layout(String loadToDiv) {

        init();
        Region loginRegion = Application.getInstance().getLoginManager().makeLoginRegion();
        if (loginRegion != null) {
            RootPanel.get("user-info").add(loginRegion.getDisplay());
        }

        Widget north = makeNorth();
        mainPanel.add(north, DockPanel.NORTH);

        Widget content = makeCenter();
        mainPanel.add(content, DockPanel.CENTER);

        RootPanel root = getRoot(loadToDiv);
        root.add(mainPanel);

        mainPanel.setCellHeight(content, "100%");

//        // now.. add the menu to the top
        getMenu().setDisplay(Application.getInstance().getToolBar().getWidget());

    }

//====================================================================

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