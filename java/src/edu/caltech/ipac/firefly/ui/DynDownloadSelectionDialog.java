/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.DynRequestHandler;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.ui.background.BackgroundManager;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.HiddenField;
import edu.caltech.ipac.firefly.ui.table.BaseDownloadDialog;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;
import java.util.Map;


public class DynDownloadSelectionDialog extends BaseDownloadDialog {

    private DockPanel _layout = new DockPanel();
    private VerticalPanel _panel = new VerticalPanel();
    private VerticalPanel _optionsPanel = new VerticalPanel();
    private VerticalPanel _contents = new VerticalPanel();
    private HorizontalPanel _buttons = new HorizontalPanel();
    private SimpleInputField _emailField;
    private CheckBox _useEmail;

    private static int downloadCounter = 1;

    private static WebClassProperties _prop= new WebClassProperties(DynDownloadSelectionDialog.class);

    private final static String DOWNLOAD_TXT = _prop.getName("download");

    public DynDownloadSelectionDialog(String dlTitle){
        super(dlTitle, null);
    }

    public void addFieldDefPanel(Widget fieldDefPanel){
        _optionsPanel.add(fieldDefPanel);
    }

    @Override
    protected void deferredBuild() {
        buildDialog();
    }

    @Override
    protected void onVisible() {
        super.setAutoLocate(false);
        setDefaultEmail();
    }
        
    private void buildDialog() {
        createContents();
        _layout.add(_buttons, DockPanel.SOUTH);
        _layout.add(_contents, DockPanel.CENTER);
        _layout.add(_panel, DockPanel.NORTH);
        setWidget(_layout);
    }

    private void createContents()  {
        _useEmail= GwtUtil.makeCheckBox(_prop.makeBase("useEmail"));
        _useEmail.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) { showEmail(_useEmail.getValue()); }
        });

        _emailField= SimpleInputField.createByProp(_prop.makeBase("email"));
        setDefaultEmail();

        // DEBUG
        //_optionsPanel.insert(htmlObj, 0);
        Label holder= new Label();
        holder.setWidth("450px");
        _panel.add(GwtUtil.centerAlign(_optionsPanel));
        _panel.setSpacing(5);
        _panel.add(holder);
        //_layout.add(_panel, DockPanel.NORTH);

        _contents.add(_useEmail);
        _contents.add(_emailField);
        _emailField.setVisible(_useEmail.getValue());
        _contents.add(GwtUtil.centerAlign(_useEmail));
        _contents.add(GwtUtil.centerAlign(_emailField));
        _contents.setSpacing(5);
        holder= new Label();
        holder.setWidth("450px");
        _contents.add(holder);

        Button okb= this.getButton(ButtonID.OK);
        okb.setText(DOWNLOAD_TXT);
    }

    private void showEmail(boolean show) {
        if (show) {
            setDefaultEmail();
         }
        _emailField.setVisible(show);
    }

    private void setDefaultEmail() {
        String prefEmailStr = Preferences.get(BackgroundManager.EMAIL_PREF);
        if (!StringUtils.isEmpty(prefEmailStr)) {
            _emailField.setValue(prefEmailStr);
        } else {
            LoginManager loginManager = Application.getInstance().getLoginManager();
            if (loginManager.isLoggedIn()) {
                UserInfo userInfo = loginManager.getLoginInfo();
                String emailStr = userInfo.getEmail();
                if (!StringUtils.isEmpty(emailStr))  _emailField.setValue(emailStr);
            } else {
                _emailField.reset();
            }
        }
    }

    protected void inputComplete() {
        startPackaging();
    }

    private void startPackaging() {
        TitleRet title= buildTitle();

        String emailStr= null;
        if (_useEmail.getValue() && !StringUtils.isEmpty(_emailField.getValue()))  {
            emailStr= _emailField.getValue();
            Preferences.set(BackgroundManager.EMAIL_PREF, emailStr);
        }

        DownloadRequest dataRequest = getDownloadRequest();
        LayoutManager lman= Application.getInstance().getLayoutManager();
        dataRequest.setBaseFileName(title.getFileName());
        dataRequest.setTitle(title.getTitle());
        dataRequest.setEmail(emailStr);
        dataRequest.setDataSource(DynRequestHandler.getCurrentProject());

        // set options into request
        List<InputField> ifs = Form.searchForFields(_optionsPanel);
        for (InputField i : ifs) {
            if (GwtUtil.isOnDisplay(i) || i instanceof HiddenField) {
                dataRequest.setParam(i.getFieldDef().getName(), i.getValue());
            }
        }

        Widget maskW=  lman.getRegion(LayoutManager.RESULT_REGION).getDisplay();
        Widget w= getDialogWidget();
        int cX= w.getAbsoluteLeft()+ w.getOffsetWidth()/2;
        int cY= w.getAbsoluteTop()+ w.getOffsetHeight()/2;
        PackageTask.preparePackage(maskW, cX,cY,dataRequest);
    }

    private TitleRet buildTitle() {
        return new TitleRet(getDownloadRequest().getTitlePrefix() + " " + downloadCounter++, getDownloadRequest().getFilePrefix());
    }

//====================================================================
//  implements DownloadSelectionIF
//====================================================================



    private static class TitleRet {
        private final String _title;
        private final String _baseFileName;
        public TitleRet(String title, String baseFileName)  {
            _title= title;
            _baseFileName= baseFileName;
        }
        public String getTitle() { return _title; }
        public String getFileName() { return _baseFileName; }
    }
}
