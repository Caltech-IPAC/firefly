package edu.caltech.ipac.firefly.core.layout;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.AllPlots;


/**
 * Date: March 21, 2014
 *
 *
 *
 *
 * @author loi
 * @version $Id: ResizableLayoutManager.java,v 1.26 2012/10/03 22:18:11 loi Exp $
 */
public class IrsaLayoutManager extends AbstractLayoutManager {
    private static final int DEF_MIN_WIDTH = 768;
    private static final int DEF_MIN_HEIGHT = 500;
    private DockPanel mainPanel;

    public IrsaLayoutManager() {
        this(DEF_MIN_WIDTH, DEF_MIN_HEIGHT);
    }

    public IrsaLayoutManager(int minWidth, int minHeight) {
        super(minWidth, minHeight);
        mainPanel = new DockPanel();
        Window.addResizeHandler(new ResizeHandler(){
                public void onResize(ResizeEvent event) {
                    Application.getInstance().resize();
                }
            });
        setupStatusRegion(this);
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

        Region menuBar = getRegion(LayoutManager.MENU_REGION);
        Region appIcon = getRegion(LayoutManager.APP_ICON_REGION);
        Region adtlIcon = getRegion(LayoutManager.ADDTL_ICON_REGION);
        Region visTB = getRegion(LayoutManager.VIS_TOOLBAR_REGION);
        Region visRO = getRegion(LayoutManager.VIS_READOUT_REGION);
        Region visPV = getRegion(LayoutManager.VIS_PREVIEW_REGION);

        Widget pvOrIcoArea = visPV.getDisplay();

        if (adtlIcon != null) {
            final DeckPanel previewOrAddlIcon = new DeckPanel();
            previewOrAddlIcon.add(adtlIcon.getDisplay());   // idx 0
            previewOrAddlIcon.add(visPV.getDisplay());      // idx 1

            WebEventManager.getAppEvManager().addListener(Name.REGION_SHOW, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    Region source = (Region) ev.getSource();
                    if (VIS_PREVIEW_REGION.equals(source.getId())) {
                        previewOrAddlIcon.showWidget(1);
                    } else if (ADDTL_ICON_REGION.equals(source.getId())) {
                        previewOrAddlIcon.showWidget(0);
                    } else if (RESULT_REGION.equals(source.getId())) {
                        // do nothing
                    }
                }
            });
            WebEventManager.getAppEvManager().addListener(Name.REGION_HIDE, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    Region source = (Region) ev.getSource();
                    if (VIS_PREVIEW_REGION.equals(source.getId())) {
                        previewOrAddlIcon.showWidget(0);
                    }
                }
            });
            pvOrIcoArea = previewOrAddlIcon;
        }

        Widget readout = visRO == null ? new Label("") : visRO.getDisplay();
        readout.setSize("100%", "100%");


        HTMLPanel appBanner = new HTMLPanel("<div id='container' style='width:100%'>\n" +
                "<div id='app-icon' style='background: url(images/ipac_bar.jpg);height:75px;width:75px;float:left;'></div>\n" +
                "<div id='alt-app-icon' style='background: url(images/ipac_bar.jpg);height:75px;width:148px;float:right;'></div>\n" +
                "<div style='position:absolute;left:75px;right:148px;min-width:820px'>\n" +
                "<div id='readout' style='background: url(images/ipac_bar.jpg);height:45px;width:100%;'></div>\n" +
                "<div id='menu-bar' style='background: url(images/ipac_bar.jpg);height:30px;width:100%'></div>\n" +
                "</div>\n");

        appBanner.add(menuBar.getDisplay(), "menu-bar");
        appBanner.add(readout, "readout");
        appBanner.add(appIcon.getDisplay(), "app-icon");
        appBanner.add(pvOrIcoArea, "alt-app-icon");

//        // now.. add the menu to the top
        Toolbar toolbar = Application.getInstance().getToolBar();
        GwtUtil.setStyles(toolbar, "width", "100%", "position", "absolute");
        GwtUtil.setStyles(toolbar.getDropdown(), "zIndex", "10", "position", "absolute");
        getMenu().setDisplay(toolbar);

        mainPanel.add(appBanner, DockPanel.NORTH);
        mainPanel.setCellHeight(appBanner, "1px");
        mainPanel.add(toolbar.getDropdown(), DockPanel.NORTH);
        mainPanel.setCellHeight(toolbar.getDropdown(), "1px");
//        mainPanel.setCellHeight(toolbar.getDropDownComponent(), "1px");
        mainPanel.add(visTB.getDisplay(), DockPanel.NORTH);
        mainPanel.setCellHeight(visTB.getDisplay(), "1px");
        mainPanel.setSize("100%", "100%");

        // making results area.
        Widget center = makeCenter();
        mainPanel.add(center, DockPanel.CENTER);
        GwtUtil.setStyles(center, "position", "absolute", "left", "10px", "right", "10px", "top", "120px", "bottom", "1px");

        if (rootId != null) {
            RootPanel root = RootPanel.get(rootId);
            if (root == null) {
                throw new RuntimeException("Application is not setup correctly; unable to find " + rootId);
            }
            root.add(mainPanel);
            GwtUtil.setStyles(root, "position", "absolute", "left", "1px", "right", "1px",
                            "top", "40px", "bottom", "1px", "minWidth", getMinWidth()+"px", "minHeight", getMinHeight() + "px");
        } else {
            RootPanel.get().add(mainPanel);
        }

        Image icon = Application.getInstance().getCreator().getMissionIcon();
        if (icon != null) {
            icon.setSize("75px", "75px");
            getRegion(APP_ICON_REGION).setDisplay(icon);
        }

    }

    @Override
    protected Widget makeCenter() {
        Widget c = super.makeCenter();

        WebEventManager.getAppEvManager().addListener(Name.BG_MANAGER_STATE_CHANGED,
                            new WebEventListener(){
                                public void eventNotify(WebEvent ev) {
                                    resize();
                                }
                            });
        return c;
    }

    //====================================================================

    protected double getNorthHeight() {
        return 50;
    }

    protected Region getResizableRegion() {
        Region rr = getResult();
        rr.setMinHeight(300);
        return rr;
    }

}

