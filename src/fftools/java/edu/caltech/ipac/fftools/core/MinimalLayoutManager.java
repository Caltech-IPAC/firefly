/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.fftools.core;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.AbstractLayoutManager;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.visualize.AllPlots;


/**
 * Date: July 15, 2015
 *
 * @author loi
 * @version $Id: ResizableLayoutManager.java,v 1.26 2012/10/03 22:18:11 loi Exp $
 */
public class MinimalLayoutManager extends AbstractLayoutManager {
    public static final int DEF_MIN_WIDTH = 640;
    public static final int DEF_MIN_HEIGHT = 320;
    private DockPanel mainPanel;

    public MinimalLayoutManager() {
        super(DEF_MIN_WIDTH, DEF_MIN_HEIGHT);
        mainPanel = new DockPanel();
        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                Application.getInstance().resize();
            }
        });
    }


    protected DockPanel getMainPanel() {
        return mainPanel;
    }

    public Widget getDisplay() {
        return getMainPanel();
    }

    public void layout(String rootId) {

        AllPlots.getInstance().setToolBarIsPopup(false);

        init();

        Region visTB = getRegion(LayoutManager.VIS_TOOLBAR_REGION);
        Region visRO = getRegion(LayoutManager.VIS_READOUT_REGION);
        Region visPV = getRegion(LayoutManager.VIS_PREVIEW_REGION);

        Widget pvOrIcoArea = visPV.getDisplay();
//        GwtUtil.setStyle(visPV.getContent(), "background", "url(images/ipac_bar.jpg)");
        Widget readout = visRO == null ? new Label("") : visRO.getDisplay();
        readout.setSize("100%", "100%");


        HTMLPanel appBanner = new HTMLPanel(
                        "<div id='container' style='min-width:1060px;'>\n" +
                        "    <div style='float:right;'>\n" +
                        "        <div id='alt-app-icon' style='background: url(images/ipac_bar.jpg);height:74px'></div>\n" +
                        "    </div>\n" +
                        "    <div id='readout' style='background: url(images/ipac_bar.jpg);height:45px;'></div>\n" +
                        "    <div id='vis-toolbar' style=''></div>\n" +
                        "</div>");

        appBanner.add(readout, "readout");
        appBanner.add(pvOrIcoArea, "alt-app-icon");
        appBanner.add(visTB.getDisplay(), "vis-toolbar");


// -experiment
        Toolbar toolbar = Application.getInstance().getToolBar();
        GwtUtil.setStyles(toolbar, "width", "100%", "height", "0", "position", "absolute");
        GwtUtil.setStyles(toolbar.getDropdown(), "zIndex", "10", "position", "absolute");
        getMenu().setDisplay(toolbar);
// -experiment



        mainPanel.add(appBanner, DockPanel.NORTH);
// -experiment
        mainPanel.add(toolbar.getDropdown(), DockPanel.NORTH);
// -experiment
        mainPanel.setCellHeight(appBanner, "1px");
        mainPanel.setSize("100%", "100%");

        // making results area.
        Widget center = makeCenter();
        mainPanel.add(center, DockPanel.CENTER);
        GwtUtil.setStyles(center, "position", "absolute", "left", "10px", "right", "10px", "top", "80px", "bottom", "1px");

        RootPanel rp = null;
        if (rootId != null) {
            rp = RootPanel.get(rootId);
        }
        rp = rp == null ? RootPanel.get() : rp;
        rp.add(mainPanel);
        GwtUtil.setStyles(rp,
                "minWidth", getMinWidth() + "px",
                "minHeight", getMinHeight() + "px");

        Application.getInstance().getProperties().setProperty("BackToSearch.show", "false");





        ((FFToolsStandaloneCreator)Application.getInstance().getCreator()).getStandaloneUI().init();

    }

    @Override
    protected Region makeDownload() {
        return null;
    }

    @Override
    protected Region makeBanner() {
        return null;
    }

    @Override
    protected Region makeSmallIcon() {
        return null;
    }

    @Override
    protected Region makeSmallIcon2() {
        return null;
    }

    @Override
    protected Region makeFooter() {
        return null;
    }

    @Override
    protected Widget makeNorth() {
        return null;
    }

    @Override
    protected Widget makeSouth() {
        return null;
    }

    @Override
    protected Region makeSearchTitle() {
        return null;
    }
}

