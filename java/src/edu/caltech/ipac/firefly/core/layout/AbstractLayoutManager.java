package edu.caltech.ipac.firefly.core.layout;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
 * @version $Id: AbstractLayoutManager.java,v 1.38 2012/05/16 20:56:53 loi Exp $
 */
public abstract class AbstractLayoutManager implements LayoutManager {

    /**
     * Use TreeMap to return an ordered list..
     */
    private TreeMap<String, Region> regions = new TreeMap<String, Region>();
    private boolean isInit;
    private int minHeight;
    private int minWidth;
    private LayoutSelector layoutSelector = new LayoutSelector();

    protected AbstractLayoutManager(int minWidth, int minHeight) {
        this.minHeight = minHeight;
        this.minWidth = minWidth;
    }

    protected void init() {
        if (!isInit) {
            Region result = makeResult();
            addRegion(result);
            Region form = makeForm();
            addRegion(form);
            Region menu = makeMenu();
            addRegion(menu);
            Region banner = makeBanner();
            addRegion(banner);
            Region download = makeDownload();
            addRegion(download);
            Region smallIcon = makeSmallIcon();
            addRegion(smallIcon );
            Region smallIcon2 = makeSmallIcon2();
            addRegion(smallIcon2 );
            Region footer = makeFooter();
            addRegion(footer );
            Region searchTitle = makeSearchTitle();
            addRegion(searchTitle );
            Region searchDesc = makeSearchDesc();
            addRegion(searchDesc );
            isInit = true;
        }
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    public void resize() {
    }

    public Region getRegion(String id) {
        return regions.get(id);
    }

    public void addRegion(Region region) {
        regions.put(region.getId(), region);
    }

    public void setRegion(String id, Region region) {
        regions.put(id, region);
    }

    public List<Region> getRegions() {
        return new ArrayList<Region>(regions.values());
    }

    public boolean isLoading() {
        return false;
    }

    public void setLoading(Boolean isLoading, String msg) {
    }

    public Region getBanner() {
        return getRegion(BANNER_REGION);
    }

    public Region getMenu() {
        return getRegion(MENU_REGION);
    }

    public Region getDownload() {
        return getRegion(DOWNLOAD_REGION);
    }

    public Region getSmallIcon() {
        return getRegion(SMALL_ICON_REGION);
    }

    public Region getSmallIcon2() {
        return getRegion(SMALL_ICON_REGION2);
    }

    public Region getFooter() {
        return getRegion(FOOTER_REGION);
    }

    public Region getForm() {
        return getRegion(DROPDOWN_REGION);
    }

    public Region getResult() {
        return getRegion(RESULT_REGION);
    }

    public Region getSearchTitle() {
        return getRegion(SEARCH_TITLE_REGION);
    }

    public Region getSearchDesc() {
        return getRegion(SEARCH_DESC_REGION);
    }

    //====================================================================
//  protected methods that can be overridden to provide customized layout
//====================================================================

    protected RootPanel getRoot(String id) {
        if (id != null) {
            RootPanel root = RootPanel.get(id);
            if (root == null) {
                throw new RuntimeException("Application is not setup correctly; unable to find " + id);
            }
            return root;
        } else {
            return RootPanel.get();
        }
    }

    protected Region makeMenu() {

        final BaseRegion r = new BaseRegion(MENU_REGION);
        Widget w = r.getDisplay();
//        w.setHeight("20px");
        w.setWidth("100%");
        w.addStyleName("menu-bar");
        RegionWrapper wrapper = new RegionWrapper(r) {
            protected Widget makeDisplay() {
                HorizontalPanel hp = new HorizontalPanel();
                hp.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
                hp.add(r.getDisplay());
//                hp.setStyleName("user-info");

                r.getDisplay().setWidth("100%");
                hp.setWidth("100%");
                hp.setCellWidth(r.getDisplay(), "100%");
                return hp;
            }
        };
        return wrapper;
    }


    protected Region makeForm() {
        Toolbar toolbar = Application.getInstance().getToolBar();
        return toolbar.getContentRegion();
    }


    protected Region makeDownload() {
        BaseRegion r = new BaseRegion(DOWNLOAD_REGION);
        r.setAlign(BaseRegion.ALIGN_MIDDLE);
        Widget w = r.getDisplay();
        w.setWidth("100%");
        return r;
    }

    protected Region makeResult() {
        BaseRegion r = new BaseRegion(RESULT_REGION);
//        r.setAlign(BaseRegion.ALIGN_MIDDLE);
        Widget w = r.getDisplay();
        w.setWidth("100%");
        return r;
    }

    protected Region makeBanner() {
        BaseRegion r = new BaseRegion(BANNER_REGION);
        Widget w = r.getDisplay();
        w.setWidth("100%");
        return r;
    }

    protected Region makeSmallIcon() {
        BaseRegion r = new BaseRegion(SMALL_ICON_REGION);
        r.setAlign(BaseRegion.ALIGN_MIDDLE);
        Widget w = r.getDisplay();
        w.setWidth("100%");
        return r;
    }

    protected Region makeSmallIcon2() {
        BaseRegion r = new BaseRegion(SMALL_ICON_REGION2);
        r.setAlign(BaseRegion.ALIGN_MIDDLE);
        Widget w = r.getDisplay();
        w.setWidth("100%");
        return r;
    }

    protected Region makeSearchTitle() {
        BaseRegion r = new BaseRegion(SEARCH_TITLE_REGION);
        Widget w = r.getDisplay();
        w.setStyleName("result-title");
        w.setWidth("100%");
        return r;
    }

    protected Region makeSearchDesc() {
        BaseRegion r = new BaseRegion(SEARCH_DESC_REGION);
        Widget w = r.getDisplay();
        w.setStyleName("result-desc");
        w.setWidth("100%");
        return r;
    }

    protected Region makeFooter() {
        Toolbar toolbar = Application.getInstance().getToolBar();
        if (toolbar != null) {
            return toolbar.getFooterRegion();
        } else {
            return new BaseRegion(LayoutManager.FOOTER_REGION);
        }

    }


    protected Widget makeCenter() {
        final DockPanel center = new DockPanel();
        center.setSize("100%", "100%");

        final BaseRegion content = new BaseRegion(CONTENT_REGION);
        Widget w = content.getDisplay();
        w.setWidth("100%");
        addRegion(content);

        final Region query = getForm();
        final Region results = getResult();


        VerticalPanel vp = new VerticalPanel();
        vp.setWidth("100%");
        if (query.getDisplay() != null) {
            vp.add(GwtUtil.getFiller(1, 10));
        }
        center.add(vp, DockPanel.NORTH);
        center.setCellHeight(vp, "10px");
        center.add(results.getDisplay(), DockPanel.CENTER);
        center.add(content.getDisplay(), DockPanel.SOUTH);


        WebEventManager.getAppEvManager().addListener(Name.REGION_SHOW, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        Region source = (Region) ev.getSource();
                        if (DROPDOWN_REGION.equals(source.getId()) ||
                                RESULT_REGION.equals(source.getId()) ) {
                            content.hide();
                            center.setCellHeight(results.getDisplay(), "100%");
                            center.setCellHeight(content.getDisplay(), "");
                        } else if (CONTENT_REGION.equals(source.getId())) {
                            query.hide();
                            results.hide();
                            center.setCellHeight(content.getDisplay(), "100%");
                            center.setCellHeight(results.getDisplay(), "");
                        }

                    }
                });

        return center;
    }

    public LayoutSelector getLayoutSelector() {
        return layoutSelector;
    }

    protected Widget makeNorth() {

        AbsolutePanel l = new AbsolutePanel();
        DockPanel dlp = new DockPanel();
//        final Image sep = new Image("images/glow_bottom_center.gif");
//        dlp.add(sep, DockPanel.SOUTH);
//        GwtUtil.setStyles(sep, "width", "100%", "height", "5px");
//        dlp.add(getSmallIcon().getDisplay(), DockPanel.EAST);
//        dlp.add(getSmallIcon2().getDisplay(), DockPanel.WEST);
        dlp.add(l, DockPanel.CENTER);
        dlp.setCellWidth(l, "100%");
        dlp.setCellHeight(l, "37px");
        dlp.setWidth("100%");

//        dlp.setCellWidth(getSmallIcon().getDisplay(), "160px");
        dlp.setStyleName("menu-bar");
        getMenu().getDisplay().addStyleName("menu-bar");

        GwtUtil.setStyles(l, "width", "100%", "height", "5px", "overflow", "visible");

        l.add(getMenu().getDisplay(), 0, 10);
//        DeferredCommand.addCommand(new Command(){
//                public void execute() {
//                    int offset = BrowserUtil.isBrowser(Browser.FIREFOX) ? 8 : 11;
//                    RootPanel.get().add(getMenu().getDisplay(), l.getAbsoluteLeft(), l.getAbsoluteTop()+offset);
//                }
//            });
        return dlp;
    }


    protected Widget makeSouth() {
        Region f = getFooter();
        if (f != null) {
            FlowPanel south = new FlowPanel();
            south.setSize("100%", "100%");
            south.add(getFooter().getDisplay());
            return south;
        }
        return null;
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