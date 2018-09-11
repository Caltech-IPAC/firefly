/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.background.ActivationFactory;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.ui.BadgeButton;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.CssAnimation;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
/**
 * User: roby
 * Date: Oct 24, 2008
 * Time: 9:34:26 AM
 * $Id: BackgroundManager.java,v 1.30 2012/10/24 18:35:15 roby Exp $
 */


/**
 * @author Trey Roby
 */
public class BackgroundManager {

    public static final String EMAIL_PREF = "DownloadStatusEmail";

    private static WebClassProperties _prop= new WebClassProperties(BackgroundManager.class);
    private final static String ONE_GEAR_ICON= GWT.getModuleBaseURL()+  "images/One_gear-32x32.gif";
    private final static String ONE_GEAR_ICON_LARGE= GWT.getModuleBaseURL()+  "images/One_gear-55x55.gif";

    private final static String ATTENTION_ICON= GWT.getModuleBaseURL()+"images/gxt/attention.gif";
    private final static String FAIL_TXT= _prop.getName("fail");
    private final static String WORKING_TXT= _prop.getName("working");
    private final static String MANY_READY_TXT= _prop.getName("ready.plual");
    private final static String ONE_READY_TXT= _prop.getName("ready.singular");
    private final static String ADD_READY_TXT= _prop.getName("ready.addition");
    private final static int SPACE = 20;

    enum AttnState { NONE, WORKING, READY, READY_WORKING, FAIL}

    private Toolbar.CmdButton button;
    private Image _workingIcon= null;
    private Image _readyIcon= null;
    private static Image _animationIcon = null;
    private StyleElement styleElement= null;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public BackgroundManager() {
        WebEventManager.getAppEvManager().addListener(new DownloadListener());
//        changeState(null, VisState.HIDDEN);
        layout();
        if (BrowserUtil.canSupportAnimation()) initCssAnimationElement();
    }

    public Toolbar.CmdButton getButton() { return button;  }

    public void animateToManager(final int startX, final int startY, final int mills) {
        WebEventManager.getAppEvManager().fireEvent(WebEvent.createWebEvent(this,Name.BG_MANAGER_PRE_ANIMATE));
        DeferredCommand.add(new Command() {
            public void execute() {
                if (BrowserUtil.canSupportAnimation()) {
                    animationIconCSS(mills,startX,startY);
                }
                else {
                    BackAnimation ani= new BackAnimation(startX,startY);
                    ani.run(mills);
                }

            }
        });
    }

    public void show() {
        BackgroundUIOps.getOps(button,  new BackgroundGroupsDialog.OpsHandler() {
            public void dialogOps(BackgroundUIOps ops) {
                ops.setVisible(true);
            }
        });
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void layout() {

        Command bgCommandOpen= new Command(){
            public void execute() {
                show();
            }
        };
        button= new Toolbar.CmdButton("background", null, bgCommandOpen, _prop.getTitle(), _prop.getTip() );


        button.setText("");
        adjustTitleToNone();

    }


    private void adjustTitleToNone() {
        button.setText(_prop.getTitle());
        button.setIconLeft(null);
        button.setIconRight(null);
    }

    private void adjustTitle(BackgroundUIOps ops, AttnState attn) {
        int readyCnt= ops.getGroup().getUndownloadCnt();
        int workCnt= ops.getGroup().getWorkingCnt();
        String txt;
        switch (attn) {
            case READY_WORKING :
                txt= WORKING_TXT + " "+ (workCnt>0 ? workCnt : "");
                if (readyCnt>0) txt+= ", " + readyCnt +" "+ ADD_READY_TXT;
                button.setIconLeft(getWorkingIcon());
                button.setIconRight(null);
                button.setText(txt);
                break;
            case READY :
                button.setIconLeft(null);
                button.setIconRight(getReadyIcon(ops.getGroup().getUndownloadCnt()));
                button.setText(_prop.getTitle());
                break;
            case WORKING :
                txt= WORKING_TXT + " "+ (workCnt>0 ? workCnt : "");
                button.setIconLeft(getWorkingIcon());
                button.setIconRight(null);
                button.setText(txt);
                break;
            case FAIL :
                button.setIconLeft(getFailIcon());
                button.setText(FAIL_TXT);
                button.setIconRight(null);
                break;
            case  NONE :
                adjustTitleToNone();
                break;
        }
    }




    private AttnState getAttentionState(BackgroundUIOps ops) {
        boolean ready= false;
        boolean working= false;
        boolean fail= false;
        AttnState attn;

        for(DownloadGroupPanel panel : ops.getGroup().panels()) {
            attn= panel.getAttentionState();
            if (attn==AttnState.READY_WORKING) {
                ready= true;
                working= true;
                break;
            }
            else if (attn==AttnState.READY) {
                ready= true;
            }
            else if (attn==AttnState.WORKING) {
                working= true;
            }
            else if (attn==AttnState.FAIL) {
                fail= true;
            }
        }

        AttnState attnState;
        if (ready && working) attnState= AttnState.READY_WORKING;
        else if (ready)       attnState= AttnState.READY;
        else if (working)     attnState= AttnState.WORKING;
        else if (fail)        attnState= AttnState.FAIL;
        else                  attnState= AttnState.NONE;

        return attnState;
    }


    private void createNewGroup(BackgroundUIOps ops, MonitorItem monItem) {
        DownloadGroupPanel panel= new DownloadGroupPanel(monItem);
        if (ops !=null) ops.insertPanel(monItem,panel);
//        changeState(ops, VisState.EXPANDED);
    }


    private void addGroup(BackgroundUIOps ops, MonitorItem monItem) {
        createNewGroup(ops, monItem);
        adjustTitle(ops, getAttentionState(ops));
        updateGroup(ops, monItem);
    }

    private void removeItem(BackgroundUIOps ops, MonitorItem monItem) {
        ops.remove(monItem.getID());
        adjustTitle(ops, getAttentionState(ops));
    }

    private void updateGroup(BackgroundUIOps ops, MonitorItem monItem) {
        if (ops.getGroup().containsItem(monItem)) {
            DownloadGroupPanel group= ops.getGroup().getPanel(monItem);
            group.updateUI();
            adjustTitle(ops, getAttentionState(ops));
            ops.update(monItem);
        }
        else {
            monitorCreate(ops, monItem);
        }
    }


    private void monitorCreate(BackgroundUIOps ops, MonitorItem item) {
        BackgroundStatus bgStat= item.getStatus();
        if (bgStat.getBackgroundType()== BackgroundStatus.BgType.PACKAGE) {
            if (bgStat.getState()==BackgroundState.SUCCESS && !bgStat.isMultiPart() && item.getImmediately()) {
                if (bgStat.getTotalSizeInBytes() == 0) {
                    PopupUtil.showInfo("No data available for download.");
                } else {
                    ActivationFactory.getInstance().activate(item);
                }
            }
            else {
                addGroup(ops, item);
            }
        }
        else {
            addGroup(ops, item);
        }
    }

    private String makeReadyStr(BackgroundUIOps ops) {
        String retval= "";
        int cnt= ops.getGroup().getUndownloadCnt();
        if (cnt>0) {
            String txt= cnt>1 ? MANY_READY_TXT : ONE_READY_TXT;
            retval= cnt +" "+ txt;
        }
        return retval;
    }





    private class DownloadListener implements WebEventListener {
        public void eventNotify(final WebEvent ev) {
            if (ev.getData() instanceof MonitorItem) {

                BackgroundUIOps.getOps(button,  new BackgroundGroupsDialog.OpsHandler() {
                    public void dialogOps(BackgroundUIOps ops) {
                        Name evName= ev.getName();
                        MonitorItem item= (MonitorItem)ev.getData();

                        if (evName.equals(Name.MONITOR_ITEM_UPDATE) && item.isWatchable()) {
                            updateGroup(ops, item);
                        }
                        if (evName.equals(Name.MONITOR_ITEM_REMOVED)) {
                            removeItem(ops, item);
                        }
                        else if (evName.equals(Name.REGION_CHANGE)) {
                            // todo: should i do anything
                        }
                    }
                });
            }
        }
    }

    private Widget getReadyIcon(int cnt) {

        Widget badge= BadgeButton.makeBadge(cnt);
        GwtUtil.setStyles(badge, "right", "0", "top", "0");
        Widget panel= new SimplePanel(badge);
        GwtUtil.setStyle(panel, "position", "relative");
        return panel;
//
//
//
//
//
//        if (_readyIcon==null) {
//            _readyIcon= new Image(ATTENTION_ICON);
//            _readyIcon.setPixelSize(SPACE,SPACE);
//            _readyIcon.addStyleName("download-manager-status-icon");
//        }
//        return _readyIcon;
    }

    private Widget getFailIcon() {
        if (_readyIcon==null) {
            _readyIcon= new Image(ATTENTION_ICON);
            _readyIcon.setPixelSize(SPACE,SPACE);
            _readyIcon.addStyleName("download-manager-status-icon");
        }
        return _readyIcon;
    }

    private Image getWorkingIcon() {
        if (_workingIcon==null) {
            _workingIcon= new Image(ONE_GEAR_ICON);
            _workingIcon.setPixelSize(SPACE, SPACE);
            _workingIcon.addStyleName("download-manager-status-icon");
        }
        return _workingIcon;

    }
    private void animationIconCSS(int mills, int startX, int startY) {
        Image icon = new Image(ONE_GEAR_ICON_LARGE);
        final PopupPanel popup= new PopupPanel();
        popup.setStyleName("");
        popup.addStyleName("animationLevel");
        popup.setAnimationEnabled(false);
        popup.setWidget(icon);
        Widget w= button.getIcon()!=null ? button.getIcon() : button;
        int endX= w.getAbsoluteLeft();
        int endY= w.getAbsoluteTop();
        setupCssAnimation(startX,startY,endX,endY);
        int extra= 35;
        CssAnimation.setAnimationStyle(popup,"iconAnimate "+ (mills+extra) +"ms ease-in-out 1 normal");
        popup.setPopupPosition(endX, endY);
        popup.show();
        Timer t= new Timer() {
            @Override
            public void run() {
                popup.hide();
            }
        };
        t.schedule( mills);
    }

    private void initCssAnimationElement() {
        styleElement= Document.get().createStyleElement();
        styleElement.setInnerText("");
        styleElement.setId("iconAnimation");
        styleElement.setType("text/css");
        Document.get().getBody().appendChild(styleElement);
    }

    private void setupCssAnimation(int sx, int sy, int ex, int ey) {

        String css="@"+CssAnimation.getStylePrepend()+"keyframes iconAnimate { \n" +
                "0% { left:" + sx + "px; :top"+sy+"px;  width:55px; height: 55px; } \n" +
                "100% { left:" + ex + "px; top:"+ey+"px; "+ CssAnimation.getStylePrepend()+"transform: scaleX(.10) scaleY(.10) } \n"+
                "}";
        styleElement.setInnerText(css);
    }


    private static Image getAnimationIcon() {
        if (_animationIcon==null) {
            _animationIcon = new Image(ONE_GEAR_ICON_LARGE);
            _animationIcon.addStyleName("animationLevel");
        }
        return _animationIcon;
    }

    private class BackAnimation extends Animation {

        private PopupPanel _popup= new PopupPanel();
        private int _startX;
        private int _startY;
        private int _endX;
        private int _endY;
        private float _slope;
        private final int _startSize= 55;
        private final int _endSize= 15;
        private final int _totalDiff= _startSize-_endSize;


        private BackAnimation(int startX, int startY) {
            _startX= startX;
            _startY= startY;
            Widget w= button.getIcon()!=null ? button.getIcon() : button;
            _endX= w.getAbsoluteLeft();
            _endY= w.getAbsoluteTop();
            _slope= (float)(_endY-startY)/(float)(_endX-startX);
        }

        @Override
        protected void onStart() {
            _popup.setStyleName("");
            _popup.setAnimationEnabled(false);
            _popup.setWidget(getAnimationIcon());
            _popup.setPopupPosition(_startX,_startY);
            _popup.show();
        }

        @Override
        protected void onComplete() {
            _popup.hide();
        }

        @Override
        protected void onUpdate(double progress) {
            double x= ((_endX-_startX)*progress) + _startX;
            double y= _slope*(x-_endX) + _endY;
            _popup.setPopupPosition((int)x,(int)y);
            int pixDown= (int)(_totalDiff*progress);
            getAnimationIcon().setPixelSize(_startSize - pixDown, _startSize - pixDown);

        }
    }

    public interface ManagerReady {
        public void ready();
    }
}

