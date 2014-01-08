package edu.caltech.ipac.fftools.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HtmlRegionLoader;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.layout.AbstractLayoutManager;
import edu.caltech.ipac.firefly.core.layout.BaseRegion;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.core.layout.ResizableLayoutManager;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;


/**
 * Date: Feb 23, 2010
 *
 * @author loi
 * @version $Id: ResizableLayoutManager.java,v 1.26 2012/10/03 22:18:11 loi Exp $
 */
public class FFToolsStandaloneLayoutManager extends AbstractLayoutManager {
    private static final int DEF_MIN_WIDTH = 768;
    private static final int DEF_MIN_HEIGHT = 500;
//    private final static String FIREFLY_LOGO= GWT.getModuleBaseURL()+  "images/fftools-logo-48x48.png";
    private final static String FIREFLY_LOGO= GWT.getModuleBaseURL()+  "images/fftools-logo-56x56.png";

    private DockPanel mainPanel;
    private int yOffset = 0;
    private VerticalPanel menuLines= new VerticalPanel();
    FlowPanel south = new FlowPanel();


    public FFToolsStandaloneLayoutManager() {
        this(DEF_MIN_WIDTH, DEF_MIN_HEIGHT);
        getLayoutSelector().setHub(FFToolEnv.getHub());
    }

    public FFToolsStandaloneLayoutManager(int minWidth, int minHeight) {
        super(minWidth, minHeight);
        mainPanel = new DockPanel();

        Window.addResizeHandler(new ResizeHandler(){
                public void onResize(ResizeEvent event) {
                    Application.getInstance().resize();
                }
            });

        ResizableLayoutManager.setupStatusRegion(this);
    }

    protected DockPanel getMainPanel() {
        return mainPanel;
    }

    public Widget getDisplay() {
        return getMainPanel();
    }

    public VerticalPanel getMenuLines() { return menuLines; }
    public FlowPanel getSouth() { return south; }


    protected Widget makeNorth() {


        getMenu().setDisplay(menuLines);



        AbsolutePanel l = new AbsolutePanel();
        DockPanel dlp = new DockPanel();
        dlp.add(l, DockPanel.CENTER);
        dlp.setCellWidth(l, "100%");
        dlp.setCellHeight(l, "77px");
        dlp.setWidth("100%");
//        dlp.setStyleName("menu-bar");
//        getMenu().getDisplay().addStyleName("menu-bar");
        dlp.addStyleName("menu-bar-taller");

        GwtUtil.setStyles(l, "width", "100%",
                             "height", "5px",
                             "overflow", "visible");
        GwtUtil.setStyles(menuLines, "width", "100%");

        menuLines.addStyleName("menu-lines-widget");
        l.add(menuLines, 0, 10);
        return dlp;
    }




    public void layout(String rootId) {

        init();

        // login region setup
        LoginManager lm= Application.getInstance().getLoginManager();


        HorizontalPanel hp = new HorizontalPanel();
        hp.add(getSmallIcon().getDisplay());
        hp.add(getSmallIcon2().getDisplay());
        VerticalPanel vp = new VerticalPanel();

        if (lm!=null) {
            Region loginRegion = lm.makeLoginRegion();
            if (loginRegion!=null) {
                vp.add(loginRegion.getDisplay());
                vp.setCellHeight(loginRegion.getDisplay(), "20px");
            }
        }
        vp.add(hp);
//        vp.setCellHeight(hp, "52px");
        vp.setCellHeight(hp, "58px");
        vp.setCellHorizontalAlignment(hp, VerticalPanel.ALIGN_CENTER);
        vp.setStyleName("user-info");
        RootPanel.get("user-info").add(vp);

        //todo uncomment next 3 lines
//        Image logo= new Image(FIREFLY_LOGO);
//        GwtUtil.setStyle(logo, "padding", "9px 6px");
//        getSmallIcon().setDisplay(logo);


        Widget north = makeNorth();
        Widget center = makeCenter();
        makeSouth();
        if (north != null) {
            mainPanel.add(north, DockPanel.NORTH);
            mainPanel.setCellHeight(north, "10px");
        }

        if (center != null) {
            mainPanel.add(center, DockPanel.CENTER);
            GwtUtil.setStyle(center, "padding", "0 10px");
        }
        if (south != null) {
            mainPanel.add(south, DockPanel.SOUTH);
            mainPanel.setCellHeight(north, "20px");
        }





        if (rootId != null) {
            RootPanel root = RootPanel.get(rootId);
            if (root == null) {
                throw new RuntimeException("Application is not setup correctly; unable to find " + rootId);
            }
            root.add(mainPanel);
        } else {
            RootPanel.get().add(mainPanel);
        }
        mainPanel.setSize("100%", "100%");

//        // now.. add the menu to the top
        BaseRegion visMenuHelp = new BaseRegion(VIS_MENU_HELP_REGION);
        south.add(visMenuHelp.getDisplay());
        getMenu().setDisplay(menuLines);
        addRegion(visMenuHelp);


        ((FFToolsStandaloneCreator)Application.getInstance().getCreator()).getStandaloneUI().init();



        resize();

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

//        final Region download = getDownload();

        VerticalPanel vp = new VerticalPanel();
        vp.setWidth("100%");
        if (query.getDisplay() != null) {
            vp.add(query.getDisplay());
            vp.add(GwtUtil.getFiller(1, 10));
        }

        center.add(vp, DockPanel.NORTH);
        center.setCellHeight(vp, "10px");
        center.add(results.getDisplay(), DockPanel.CENTER);
        center.add(content.getDisplay(), DockPanel.SOUTH);

        WebEventManager.getAppEvManager().addListener(Name.BG_MANAGER_STATE_CHANGED,
                                                      new WebEventListener(){
                                                          public void eventNotify(WebEvent ev) {
                                                              resize();
                                                          }
                                                      });



        return center;
    }


    @Override
    public void resize() {
        int rh = Window.getClientHeight();
        int rw = Window.getClientWidth();

        int h = Math.max(getMinHeight(), rh - mainPanel.getAbsoluteTop() - 30 + yOffset);
        int w = Math.max(getMinWidth(), rw - 20);

        Region rr = getResizableRegion();
        if (rr != null) {
            int rrh = h - rr.getDisplay().getAbsoluteTop() + mainPanel.getAbsoluteTop() ;
            rr.getDisplay().setHeight(rrh + "px");
        }
    }

//    protected Widget makeSouth() {
//        south.setSize("100%", "100%");
//        return south;
//    }

    @Override
    protected Widget makeSouth() {
        Widget s = super.makeSouth();
        HtmlRegionLoader footer= new HtmlRegionLoader();
        footer.load("irsa_footer.html", LayoutManager.FOOTER_REGION);
        return s;
    }


    //====================================================================

    protected Region getResizableRegion() {
        Region rr = getResult();
        rr.setMinHeight(300);
        return rr;
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
