/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitorPolling;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.JobAttributes;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Sep 10, 2010
 * Time: 11:45:11 AM
 */


/**
 * @author Trey Roby
 */
public class BackgroundGroupsDialog extends BaseDialog {

    interface PFile extends PropFile { @Source("BackgroundGroupsDialog.prop") TextResource get(); }

    private static final WebClassProperties _prop= new WebClassProperties(BackgroundGroupsDialog.class, (PFile) GWT.create(PFile.class));
    private final static String ALL_HIDDEN_TXT= _prop.getName("allHidden");
    private final static String EMPTY_TXT= _prop.getName("empty");
    private final static String NO_EMAIL_SET= _prop.getName("noEmailSet");
    private static final int CONTENT= 0;
    private static final int NOTHING_TO_SHOW = 1;


    private final VerticalPanel _contentPanel= new VerticalPanel();
    private final VerticalPanel _groupsW= new VerticalPanel();
    private final MonitorGroup _groups;
    private final DockLayoutPanel _main= new DockLayoutPanel(Style.Unit.PX);
    private boolean _hideCompleted= false;
    private final FireflyCss _ffCss=CssData.Creator.getInstance().getFireflyCss();
    private final DeckPanel _deck= new DeckPanel();
    private final Label _noneToShow= new Label();
    private final Map<String,String> _idEmailMap= new HashMap<String,String>(17);

    private final SimplePanel _emailPanel = new SimplePanel();
    private final Label _currentEmailLabel= new Label(NO_EMAIL_SET);
    private Label _updateEmailLink;
    private final SimpleInputField _emailInput = SimpleInputField.createByProp(_prop.makeBase("emailInput"));
    private String _currentEmail= null;
    private String _lastEmailingID= null;
    private BackgroundUIOps _ops= null;

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public BackgroundGroupsDialog(Widget p, MonitorGroup groups) {
        super(p, ButtonType.REMOVE,_prop.getTitle(), "downloads.Backgroundmonitor");
        _groups= groups;
        this.setWidget(_main);
        createContents();
        Button b= this.getButton(ButtonID.REMOVE);
        b.setText("Hide");
        setDefaultContentSize(500,250);
    }


//=======================================================================
//-------------- Methods from BaseDialog ----------------------
//=======================================================================

    private void createContents() {

        makeEmailPanel();
        SimplePanel sp= new SimplePanel();

        _contentPanel.add(_groupsW);

        HorizontalPanel allHiddenMsg;
        allHiddenMsg = new HorizontalPanel();
        allHiddenMsg.setWidth("100%");

        allHiddenMsg.add(GwtUtil.centerAlign(_noneToShow));
        GwtUtil.setStyle(allHiddenMsg, "paddingTop", "40px");

        _deck.add(_contentPanel); // must be at index CONTENT (0)
        _deck.add(allHiddenMsg); // must be at index NOTHING_TO_SHOW (1)

        ScrollPanel scroll= new ScrollPanel(_deck);

        sp.setWidget(_emailPanel);
        _main.add(scroll);

        GwtUtil.setStyle(scroll, "borderBottom", "2px solid black");
        GwtUtil.setStyle(sp , "paddingLeft", "40px");

        this.addButtonAreaWidget(sp);

        update();
    }

    void insertPanel(MonitorItem item, DownloadGroupPanel panel) {
        _groupsW.insert(panel,0);
        _groups.putItem(item, panel);
        updateEmail(item.getID());
        if (item.getStatus().hasAttribute(JobAttributes.CanSendEmail)) {
            _lastEmailingID= item.getStatus().getID();
        }
    }

    @Override
    protected void inputCanceled() { animateHide(); }

    @Override
    protected void inputComplete() { animateHide(); }

    private void animateHide() {
        Widget w= getDialogWidget();
        int cX= w.getAbsoluteLeft()+ w.getOffsetWidth()/2;
        int cY= w.getAbsoluteTop()+ w.getOffsetHeight()/2;

        if (getVisibleCnt()>0) {
            Application.getInstance().getBackgroundManager().animateToManager(cX,cY,500);
        }
    }

    private void setHideFinishedDownloads(boolean hide) {
        _hideCompleted= hide;
        adjustHidingOnFinishedDownloads();
    }

    public void remove(String id) {
        if (_groups.containsKey(id)) {
            _groupsW.remove(_groups.getPanel(id));
            _groups.remove(id);
        }
        update();
    }

    private void makeEmailPanel() {
        HorizontalPanel showHp= new HorizontalPanel();
        GwtUtil.setStyle(_currentEmailLabel, "paddingRight", "10px");
        showHp.add(_currentEmailLabel);
        _updateEmailLink=  (Label)GwtUtil.makeLinkButton(_prop.makeBase("editEmail"), new ClickHandler() {
            public void onClick(ClickEvent event) {
                showEmailChangeDialog();
            }
        });

        showHp.add(_updateEmailLink);
        GwtUtil.setStyle(_emailInput, "paddingRight", "10px");
        GwtUtil.setStyle(_emailPanel, "paddingBottom", "10px");


        _emailPanel.setWidget(showHp);
        _emailPanel.setVisible(false);
    }

   private void showEmailChangeDialog() {

       _emailInput.setValue(_currentEmail);
        ClickHandler ok= new ClickHandler() {
            public void onClick(ClickEvent event) {
                setDialogEmailAddress(_emailInput.getValue());
                setAllEmailOnServer(_currentEmail);
                sendEmailAgain(_currentEmail);
            }
        };
        ClickHandler cancel= new ClickHandler() {
            public void onClick(ClickEvent event) {

            }
        };

        PopupUtil.showInputDialog(getDialogWidget(), "Set Email", _emailInput, ok, cancel);
    }

    void update() { update(null); }

    void update(MonitorItem item) {
        boolean on= true;
        for(Widget w : _groupsW) {
            if (w.isVisible() && w instanceof DownloadGroupPanel) {
                setBackground(w, on);
                on= !on;
            }
        }
        if (getVisibleCnt()==0) {
            _deck.showWidget(NOTHING_TO_SHOW);
            if (_groups.size()==0) {
                _noneToShow.setText(EMPTY_TXT);
            }
            else {
                _noneToShow.setText(ALL_HIDDEN_TXT);
            }
        }
        else {
            _deck.showWidget(CONTENT);
        }

        boolean v= false;
        for(DownloadGroupPanel panel : _groups.panels()) {
            BackgroundStatus bgStat = panel.getMonitorItem().getStatus();
            if (bgStat!=null && bgStat.hasAttribute(JobAttributes.CanSendEmail)) {
                v= true;
                break;
            }
        }
        _emailPanel.setVisible(v);

        if (StringUtils.isEmpty(_currentEmail) && item!=null) {
            updateEmail(item.getID());
        }
    }

    private int getVisibleCnt() {
        int cnt= 0;
        for(Widget w : _groupsW) {
            if (w.isVisible() && w instanceof DownloadGroupPanel) {
                DownloadGroupPanel panel= (DownloadGroupPanel)w;
                cnt+= panel.getPartsLines();
            }
        }
        return cnt;
    }

    private void setBackground(Widget w, boolean on) {
        if (on) {
            w.removeStyleName(_ffCss.tableRowBackAlt2());
            w.addStyleName(_ffCss.tableRowBackAlt1());
        }
        else {
            w.removeStyleName(_ffCss.tableRowBackAlt1());
            w.addStyleName(_ffCss.tableRowBackAlt2());
        }
    }


    void adjustHidingOnFinishedDownloads() {
        boolean vis;
        for(DownloadGroupPanel panel : _groups.panels()) {
            if (_hideCompleted) {
                vis= panel.getAttentionState()!= BackgroundManager.AttnState.NONE;
                panel.setVisible(vis);
            }
            else {
                panel.setVisible(true);
            }
        }
        update();
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void updateEmail(String setterID) {
        SearchServicesAsync pserv= SearchServices.App.getInstance();
        boolean emailVisible= false;
        for(Map.Entry<String,DownloadGroupPanel> entry : _groups.getEntries()) {
            BackgroundStatus bgStat = entry.getValue().getMonitorItem().getStatus();

            boolean canSend= bgStat.hasAttribute(JobAttributes.CanSendEmail);
            emailVisible= emailVisible || canSend;

            if (canSend && !_idEmailMap.containsKey(bgStat.getID()) &&  (!bgStat.isDone() || _currentEmail==null) ) {
                pserv.getEmail(entry.getKey(), new EmailCallback(bgStat.getID(),setterID));
            }
        }
        _emailPanel.setVisible(emailVisible);
    }


    private void setDialogEmailAddress(String email) {
        _currentEmail= email;
        _currentEmailLabel.setText(email);
        if (!StringUtils.isEmpty(email)) {
            Preferences.set(BackgroundManager.EMAIL_PREF, email);
            _updateEmailLink.setText(_prop.getName("editEmail.change"));
        }
        else {
            _updateEmailLink.setText(_prop.getName("editEmail"));
        }
    }


    private void setAllEmailOnServer(String email) {
        List<String> idList= new ArrayList<String>(_groups.getIDs());
        BackgroundTask.setEmail(idList,email);
    }

    private void setEmailOnServer(String email, String id) {
        BackgroundTask.setEmail(id,email);
    }


    private void sendEmailAgain(String email) {
        List<String> idList= new ArrayList<String>(_groups.getIDs());
        BackgroundTask.resendEmail(idList,email);
        if (Application.getInstance().getBackgroundMonitor() instanceof BackgroundMonitorPolling) {
            ((BackgroundMonitorPolling)Application.getInstance().getBackgroundMonitor()).forceUpdates();
        }
    }

    private class EmailCallback implements AsyncCallback<String> {

        private final String statusID;
        private final String updateEmailID;

        public EmailCallback(String statusID, String updateEmailID) {
            this.statusID= statusID;
            this.updateEmailID = updateEmailID;
        }

        public void onFailure(Throwable caught) { /* ignore*/ }


        /**
         *
         * If the call finds and email then
         * we need to set _currentEmail if it has never need set or is the report we are checking is the
         * last one the user created.
         *
         * if this id is updateEmailID and we did not find an email address for it and we has a )currentEmail then
         * then set it on the server
         */
        public void onSuccess(String email) {
            boolean foundEmail= !StringUtils.isEmpty(email);

            if (foundEmail)  _idEmailMap.put(statusID,email);

            if ( foundEmail && (_currentEmail==null ||  statusID.equals(_lastEmailingID))) {
                setDialogEmailAddress(email);
            }

            if (!foundEmail && statusID.equals(updateEmailID) && _currentEmail!=null) {
                setEmailOnServer(_currentEmail, updateEmailID);
            }
        }
    }


    public interface OpsHandler {
        public void dialogOps(BackgroundUIOps ops);
    }


}

