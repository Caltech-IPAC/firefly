/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

