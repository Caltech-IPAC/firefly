package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.panels.SearchPanel;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.uman.data.UmanConst.BACK_TO_URL;
import static edu.caltech.ipac.uman.data.UmanConst.TITLE_AREA;

/**
 * @author loi
 * $Id: UmanCmd.java,v 1.13 2012/11/19 22:05:43 loi Exp $
 */
abstract public class UmanCmd extends CommonRequestCmd {
    List<String> cmds = new ArrayList<String>(0);
    VerticalPanel msgPane = new VerticalPanel();
    VerticalPanel statusPane = new VerticalPanel();
    UserInfo currentUser;
    
    
    public UmanCmd(String command) {
        super(command);
    }

    public void setStatus(String msg, boolean isError) {
        setStatus(msg, isError, true);
    }

    public void setStatus(String msg, boolean isError, boolean doClear) {
        if (doClear) {
            msgPane.clear();
        }
        if (!StringUtils.isEmpty(msg)) {
            Widget w = new HTML(msg);
            if (isError) {
                w.setStyleName("alert-text");
                GwtUtil.setStyles(w, "fontStyle", "italic", "fontWeight", "bold");
            } else {
                w.setStyleName("title-label");
            }
            msgPane.add(w);
        }
        Region statusBar = Application.getInstance().getLayoutManager().getRegion(LayoutManager.FOOTER_REGION);
        statusBar.setDisplay(statusPane);
    }

    @Override
    public boolean init() {

        final String backTo = (String) Application.getInstance().getAppData(BACK_TO_URL);
        Widget goback = GwtUtil.makeLinkButton("Back to previous page", "Click here to go back to your previous page", new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (StringUtils.isEmpty(backTo)) {
                    History.back();
                } else {
                    Window.Location.assign(backTo);
                }
            }
        });

        statusPane.add(msgPane);
        statusPane.add(goback);
        GwtUtil.setStyles(msgPane, "textAlign", "left", "padding", "30px 0px 30px 0px");
        return super.init();
    }

    @Override
    protected void doExecute(Request req, AsyncCallback<String> callback) {

        final String backTo = req.getParam(BACK_TO_URL);
        if (!StringUtils.isEmpty(backTo)) {
            Application.getInstance().setAppData(BACK_TO_URL, backTo);
            // workaround for bad url parsing
            req.setDoSearch(false);
        }

        Application.getInstance().getLayoutManager().getRegion(TITLE_AREA).setDisplay(new Label(getLabel()));

        SearchPanel.getInstance().setApplicationContext("", getCommands());
        SearchPanel.getInstance().setFormArea(getForm());
        setStatus("", false);
        setResults(null);

        checkAccess(req, callback);
    }

    protected void checkAccess(Request req, AsyncCallback<String> callback) {
        hasAccess(null, req, callback);
    }
    
    protected void hasAccess(UserInfo userInfo, Request req, AsyncCallback<String> callback) {

        if (getForm() != null ) {
            getForm().getSubmitButton().setText("Submit");
        }
        super.doExecute(req, callback);
        updateUserInfo(userInfo);
    }

    protected void updateUserInfo(UserInfo userInfo) {};

    protected void submitRequst(final TableServerRequest req) {
        ServerTask<RawDataSet> st = new ServerTask<RawDataSet>() {

            @Override
            protected void onFailure(Throwable caught) {
                String msg = caught instanceof RPCException ? ((RPCException) caught).getEndUserMsg() : caught.getMessage();
                setStatus(msg, true);
            }

            @Override
            public void onSuccess(RawDataSet result) {
                DataSet data = DataSetParser.parse(result);
                String msg = data.getMeta().getAttribute("Message");
                if (msg != null) {
                    setStatus(msg, false);
                }
                UmanCmd.this.onSubmitSuccess(data);
            }

            @Override
            public void doTask(AsyncCallback<RawDataSet> passAlong) {
                SearchServices.App.getInstance().getRawDataSet(req,passAlong);
            }
        };
        st.start();

    }

    protected List<String> getCommands() {
        return cmds;
    }

    protected void onSubmitSuccess(DataSet data) {
    }


    protected String isPasswordValidate(String pass, String cpass) {
        if (!StringUtils.isEmpty(pass) && !StringUtils.isEmpty(cpass)) {
            if (!pass.equals(cpass)) {
                return "Password does not match Confirm Password.";
            }
        }
        return "";
    }

    protected String isEmailValidate(String toEmail, String ctoEmail) {
        if (!StringUtils.isEmpty(toEmail) && !StringUtils.isEmpty(ctoEmail)) {
            if (!toEmail.equals(ctoEmail)) {
                return "New Email does not match Confirm New Email.";
            }
        }
        return "";
    }

    protected void doCheckAccess(final String role, final Request req, final AsyncCallback<String> callback) {
        
        updateCurrentUser(new AsyncCallback<UserInfo>() {
            public void onFailure(Throwable caught) {/** do nothing **/}

            public void onSuccess(UserInfo user) {
                if (user == null) {
                    accessDenied(null, req, callback);
                } else {
                    if (!user.isGuestUser()) {
                        if (StringUtils.isEmpty(role) ||
                                user.getRoles().hasAccess(role)) {
                            hasAccess(user, req, callback);
                            return;
                        }
                    }
                    accessDenied(user, req, callback);
                }
            }
        });
    }
    
    protected void updateCurrentUser(final AsyncCallback<UserInfo> callback) {
        UserServices.App.getInstance(true).getUserInfo(false, new AsyncCallback<UserInfo>() {
            public void onFailure(Throwable caught) {
                currentUser = null;
                callback.onSuccess(null);
            }

            public void onSuccess(UserInfo result) {
                currentUser = result;
                callback.onSuccess(result);
            }
        });
    }

    protected void accessDenied(UserInfo userInfo, Request req, AsyncCallback<String> callback) {
        SearchPanel.getInstance().setApplicationContext("", new ArrayList<String>());
        if (userInfo.isGuestUser()) {
            setStatus("You are not logged in.  Click <a href=/account/signon/login.do?josso_back_to=" + Window.Location.getPath() +
                        ">here</a> to login.", true);
        } else {
            setStatus("You are not authorized to view this page.", true);
        }
    }

    public UserInfo getCurrentUser() {
        return currentUser;
    }

    protected TablePanel setupTable(TableServerRequest sreq, Map<String, String> tableParams, final GeneralCommand... buttons) {

        WidgetFactory factory = Application.getInstance().getWidgetFactory();

        final PrimaryTableUI primary = factory.createPrimaryUI(WidgetFactory.TABLE, sreq, tableParams);
        TablePreviewEventHub hub = new TablePreviewEventHub();
        primary.bind(hub);

        PrimaryTableUILoader loader = getTableUiLoader();
        loader.addTable(primary);
        loader.loadAll();

        final TablePanel table = (TablePanel) primary.getDisplay();
        table.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                table.showPopOutButton(false);
//                table.showSaveButton(false);
                if (buttons != null) {
                    for (GeneralCommand cmd : buttons) {
                        table.addToolButton(cmd, false);
                    }
                }
                table.getEventManager().removeListener(TablePanel.ON_INIT, this);
            }
        });

        SimplePanel wrapper = new SimplePanel(table);
        wrapper.setHeight("600px");
        GwtUtil.setStyle(wrapper, "backgroundColor", "white");
        setResults(wrapper);

        Application.getInstance().resize();
        return table;
    }


}
