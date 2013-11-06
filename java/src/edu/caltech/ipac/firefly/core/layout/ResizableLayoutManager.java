package edu.caltech.ipac.firefly.core.layout;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
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
public class ResizableLayoutManager extends AbstractLayoutManager {
    private static final int DEF_MIN_WIDTH = 768;
    private static final int DEF_MIN_HEIGHT = 500;

    private DockPanel mainPanel;
    private int yOffset = 0;

//    private Resizer resizer;


    public ResizableLayoutManager() {
        this(DEF_MIN_WIDTH, DEF_MIN_HEIGHT);
    }

    public ResizableLayoutManager(int minWidth, int minHeight) {
        super(minWidth, minHeight);
        mainPanel = new DockPanel();
//        resizer = new Resizer();

        Window.addResizeHandler(new ResizeHandler(){
                public void onResize(ResizeEvent event) {
                    Application.getInstance().resize();
                }
            });

//        WebEventManager.getAppEvManager().addListener(Name.REGION_SHOW,
//                            new WebEventListener(){
//                                public void eventNotify(WebEvent ev) {
//                                    resizer.setVisible(Application.getInstance().hasSearchResult());
//                                }
//                            });
        setupStatusRegion(this);

    }

    public static  void setupStatusRegion(LayoutManager lm) {
        final HorizontalPanel hp = new HorizontalPanel();
        Region statusBar = new BaseRegion(STATUS) {
            @Override
            public void setDisplay(Widget display) {
                GwtUtil.setStyles(display, "fontSize", "12px", "lineHeight", "40px");
                super.setDisplay(display);
            }

            @Override
            public void hide() {
                hp.setVisible(false);
            }

            @Override
            public void show() {
                hp.setVisible(true);
            }
        };

        Image im = new Image("images/gxt/attention.gif");
        im.setSize("16px", "16px");
        GwtUtil.setStyle(im, "marginLeft", "20px");
        hp.add(im);
        hp.add(statusBar.getDisplay());
        hp.getElement().setId("app-status");
        hp.setSize("99%", "40px");
        hp.setCellVerticalAlignment(im, VerticalPanel.ALIGN_MIDDLE);
        hp.setCellVerticalAlignment(statusBar.getDisplay(), VerticalPanel.ALIGN_MIDDLE);
        hp.setVisible(false);

        RootPanel.get("application").add(hp);
        lm.addRegion(statusBar);
    }

    protected DockPanel getMainPanel() {
        return mainPanel;
    }

    public Widget getDisplay() {
        return getMainPanel();
    }

    protected void setyOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public void layout(String rootId) {

        init();

        Region loginRegion = Application.getInstance().getLoginManager().makeLoginRegion();
        
        HorizontalPanel hp = new HorizontalPanel();
        hp.add(getSmallIcon().getDisplay());
        hp.add(getSmallIcon2().getDisplay());
        VerticalPanel fp = new VerticalPanel();
        fp.add(loginRegion.getDisplay());
        fp.setCellHeight(loginRegion.getDisplay(), "20px");
        fp.add(hp);
        fp.setCellHeight(hp, "52px");
        fp.setCellHorizontalAlignment(hp, VerticalPanel.ALIGN_CENTER);
        fp.setStyleName("user-info");
        RootPanel.get("user-info").add(fp);


        Widget north = makeNorth();
        Widget center = makeCenter();
        Widget south = makeSouth();
        if (north != null) {
            mainPanel.add(north, DockPanel.NORTH);
            mainPanel.setCellHeight(north, "10px");
//            GwtUtil.DockLayout.setWidgetSize(north, getNorthHeight());
        }

//        if (south != null) {
//            mainPanel.add(south, DockPanel.SOUTH);
//            mainPanel.setCellHeight(south, "60px");
//        }
//
        if (center != null) {
            mainPanel.add(center, DockPanel.CENTER);
            GwtUtil.setStyle(center, "padding", "0 10px");
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
//        mainPanel.add(resizer, DockPanel.WEST);
//        mainPanel.setCellWidth(resizer, "15px");

        mainPanel.setSize("100%", "100%");

//        // now.. add the menu to the top
        getMenu().setDisplay(Application.getInstance().getToolBar().getWidget());
        resize();

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

    //====================================================================

    protected double getNorthHeight() {
        return 50;
    }

    protected Region getResizableRegion() {
        Region rr = getResult();
        rr.setMinHeight(300);
        return rr;
    }


    class Resizer extends Widget {
        private int oevPos;
        private int cPos;
        private boolean mouseDown;

        public Resizer() {

            setElement(Document.get().createDivElement());
            getElement().getStyle().setPropertyPx("width", 8);
            getElement().getStyle().setPropertyPx("height", 8);
            setStyleName("gwt-SplitLayoutPanel-VDragger");
            GwtUtil.setStyles(getElement(), "margin", "0 5px 0 2px");
            sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEMOVE
                    | Event.ONDBLCLICK);
        }

        @Override
        public void onBrowserEvent(Event event) {
            switch (event.getTypeInt()) {
                case Event.ONMOUSEDOWN:
                    mouseDown = true;
                    oevPos = getEventPosition(event);
                    cPos = oevPos;
                    Event.setCapture(getElement());
                    event.preventDefault();
                    break;

                case Event.ONMOUSEUP:
                    if (mouseDown) {
                        int size = (getElement().getOffsetHeight()-20) * 10;
                        size = Math.max(size, getResizableRegion().getMinHeight());
                        getResizableRegion().getDisplay().setHeight(size + "px");
                        Application.getInstance().resize();
                    }

                    mouseDown = false;
                    Event.releaseCapture(getElement());
                    event.preventDefault();
                    break;

                case Event.ONMOUSEMOVE:
                    if (mouseDown) {
                        int nh = getElement().getOffsetHeight() + (getEventPosition(event) - cPos);
                        cPos = getEventPosition(event);
                        setSize(nh*10);
                    }
                    break;
            }
        }

        public void setSize(int layoutSize) {
            int h = layoutSize / 10;
            h = Math.max(40, Math.min(h, 300));
            getElement().getStyle().setPropertyPx("height", h);
        }

        protected int getAbsolutePosition() {
            return getAbsoluteTop();
        }

        protected int getEventPosition(Event event) {
            return event.getClientY() + Window.getScrollTop();
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
