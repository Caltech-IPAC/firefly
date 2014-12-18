package edu.caltech.ipac.firefly.core;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Alert;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.FlyByAnimation;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;

/**
 * This manager will manage the display of alerts.
 *
 * Date: Apr 25, 2011
 *
 * @author loi
 * @version $Id: AlertManager.java,v 1.14 2012/10/08 18:35:05 loi Exp $
 */
public class AlertManager extends Composite {

//    private static final String EXCLAMATION= "images/gxt/exclamation16x16.gif";
    private static String NO_ALERTS_URL = "no_alerts.html";
    private HTML message;
    private List<Alert> alerts;
    private PopupPane popup= null;
    private Frame view;
    private int curIdx = 0;
    private Label nextButton;
    private Label title;
    private Widget alignTo= null;
    PopupPane.Align alignType= null;
    private DockLayoutPanel main = new DockLayoutPanel(Style.Unit.PX);
    private Timer timer = new Timer(){
                        public void run() {
                            checkForAlerts(false);
                        }
                    };

    public AlertManager() {
        init();
    }

    private void init() {
        view = new Frame();
        title = new Label();
        nextButton = GwtUtil.makeLinkButton("Next", "Show the next alert.",
                new ClickHandler(){
                        public void onClick(ClickEvent event) {
                            showAlerts(++curIdx);
                        }
                    });

        nextButton.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        GwtUtil.setStyles(title, "fontWeight", "bold", "fontSize", "12px", "margin", "0 5px 0 5px");
        GwtUtil.setStyles(nextButton, "fontWeight", "bold", "margin", "0 5px 0 5px");

        HorizontalPanel naviBar = new HorizontalPanel();
        naviBar.add(title);
        naviBar.add(nextButton);
        naviBar.setCellWidth(title, "100%");

        VerticalPanel vp = new VerticalPanel();
        vp.setSize("100%", "100%");
        vp.add(naviBar);
        vp.add(view);
        main.add(vp);
        view.setSize("100%", "100%");

        message = new HTML();
        message.setStyleName("announcement-msg");
        GwtUtil.setStyles(message, "verticalAlign", "middle");
        message.addClickHandler(new ClickHandler(){
            public void onClick(ClickEvent event) {
                if (getPopup().isVisible())  {
                    getPopup().hide();
                } else {
                    checkForAlerts(true);
                }
            }
        });

        timer.scheduleRepeating(5 * 60 * 1000);    // 5 mins
        timer.run();

        Image image = new Image(GwtUtil.EXCLAMATION);
        image.setPixelSize(16,15);
        GwtUtil.setStyle(image, "marginRight", "4px");
        HorizontalPanel p = new HorizontalPanel();
        p.setStyleName("announcement-msg");
        p.add(image);
//        p.add(message);
        initWidget(p);
        GwtUtil.setStyle(this, "opacity", ".9");

    }

    public PopupPane getPopup() {
        if (popup==null) {
            popup = new PopupPane("Alerts", main);
            popup.setDefaultSize(600, 150);
            popup.setHideOnResizeWidget(view);
            popup.setDoRegionChangeHide(false);
            popup.hide();

            final Widget flyer = makeAniIcon();
            popup.addCloseHandler(new CloseHandler<PopupPane>(){
                public void onClose(CloseEvent<PopupPane> popupPanelCloseEvent) {
                    int x = popup.getPopupPanel().getAbsoluteLeft() + view.getOffsetWidth() / 2;
                    int y = popup.getPopupPanel().getAbsoluteTop() + view.getOffsetHeight() / 2;
                    FlyByAnimation ani = new FlyByAnimation(flyer, x, y, message.getAbsoluteTop(),
                                                            message.getAbsoluteLeft());
                    ani.setStartSize(100);
                    ani.run(1000);
                }
            });
        }
        return popup;

    }

    public void showPopup() {
        PopupPane popup= getPopup();
        if (alignTo != null && alignType != null) {
            popup.alignTo(alignTo, alignType);
        }
        popup.show();
    }

    protected Widget makeAniIcon() {
        final DockLayoutPanel flyer = new DockLayoutPanel(Style.Unit.PCT);
        Label header = new Label("Alerts");
        Label body = new Label("Click on top left corner to re-open this dialog");
        body.setStyleName("popup-background");
        body.addStyleName("standard-border");
        header.addStyleName("title-bg-color");
        header.addStyleName("title-color");

        flyer.addNorth(header, 20);
        flyer.add(body);
        GwtUtil.setStyle(flyer, "zIndex", "10");
        return flyer;
    }

    private void checkForAlerts(final boolean doShow) {
        UserServices.App.getInstance().getAlerts(new BaseCallback<List<Alert>>(){
                    @Override
                    public void onFailure(Throwable caught) {
                        if (doShow) {
                            super.onFailure(caught);
                        }
                        // else.. do nothing...  don't report connection error while polling.
                    }

                    public void doSuccess(List<Alert> result) {
                        alerts = result;
                        showMessages();
//                        if (hasNewAlert() || doShow) {
//                            showAlerts(getFirstAlert());
//                        }
                    }
                });
    }

    private boolean hasNewAlert() {
        if (alerts != null) {
            for(Alert a : alerts) {
                if (a.isNew()) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getFirstAlert() {
        curIdx = -1;
        if (alerts.size() > 0) {
            curIdx = 0;
            for (int i = curIdx; i < alerts.size(); i++) {
                if (alerts.get(i).isNew()) {
                    curIdx = i;
                    break;
                }
            }
        }
        return curIdx;
    }

    private void showAlerts(int idx) {
        showPopup();
        idx = idx >= alerts.size() ? 0 : idx;
        if (idx >= 0) {
            Alert a = alerts.get(idx);
            view.setUrl(a.getUrl() + "&d=" + a.getLastModDate());
            String s = (StringUtils.isEmpty(a.getMsg()) ? "Important Announcement" : a.getMsg());
            title.setText(s);
            curIdx = idx;
        }
        ensureNavi();
    }

    private void ensureNavi() {
        if (alerts == null || alerts.size() == 0) {
            view.setUrl(NO_ALERTS_URL);
        }
        nextButton.setVisible(alerts.size() > 1);
    }

    private void showMessages() {

        Region alertsRegion =  Application.getInstance().getLayoutManager().getRegion(LayoutManager.ALERTS_REGION);

        if (alertsRegion != null) {
            if (alerts == null || alerts.size() == 0) {
                alertsRegion.hide();
            } else {
                int idx = getFirstAlert();
                if (idx >= 0) {
                    Alert a = alerts.get(idx);
                    message.setHTML(a.getMsg());
                }
                alertsRegion.setDisplay(message);
            }
        }

    }

}
