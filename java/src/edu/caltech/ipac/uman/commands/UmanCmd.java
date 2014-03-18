package edu.caltech.ipac.uman.commands;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.core.JossoUtil;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static edu.caltech.ipac.uman.data.UmanConst.BACK_TO_URL;
import static edu.caltech.ipac.uman.data.UmanConst.UMAN_PROCESSOR;

/**
 * @author loi $Id: UmanCmd.java,v 1.13 2012/11/19 22:05:43 loi Exp $
 */
abstract public class UmanCmd extends CommonRequestCmd {
    private List<String> cmds = new ArrayList<String>(0);

    private String accessRole;

    private FlowPanel container = new FlowPanel();
    private DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.PX);
    private SimplePanel statusBox = new SimplePanel();
    private Label titleBox = new Label();
    private SimplePanel resultsBox = new SimplePanel();
    private FlowPanel options = new FlowPanel();

    protected UmanCmd(String command) {
        this(command, null);
    }

    public UmanCmd(String command, String accessRole) {
        super(command);
        this.accessRole = accessRole;
    }

    private HTML makeSimpleLabel(String label, String desc, ClickHandler handler) {
        HTML html = new HTML("<ul><li>" + label);
        html.setStyleName("options");
        if (desc != null) {
            html.setTitle(desc);
        }
        if (handler != null) {
            html.addClickHandler(handler);
        } else {
            html.addStyleName("options-selected");
        }
        return html;
    }

    private Label makeOption(final CommonRequestCmd cmd, boolean isClickable) {
        ClickHandler handler = null;
        if (isClickable) {
            handler = new  ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    cmd.execute(null, new AsyncCallback<String>() {
                        public void onFailure(Throwable throwable) {
                        }

                        public void onSuccess(String s) {
                        }
                    });
                }
            };
        }
        HTML label = makeSimpleLabel(cmd.getLabel(), cmd.getDesc(), handler);
        return label;
    }

    @Override
    public boolean init() {
        updateCurrentUser();
        final String backTo = (String) Application.getInstance().getAppData(BACK_TO_URL);
        Label goback = makeSimpleLabel("Back to previous page", "Click here to go back to your previous page",
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        if (StringUtils.isEmpty(backTo)) {
                            History.back();
                        } else {
                            Window.Location.assign(backTo);
                        }
                    }
                });

        for (String cmd : getCommands()) {
            final CommonRequestCmd c = (CommonRequestCmd) Application.getInstance().getCommand(cmd);
            Label link = makeOption(c, !c.getName().equals(getName()));
            options.add(link);
        }

        GwtUtil.setStyle(goback, "marginTop", "30px");
//        options.add(GwtUtil.getFiller(0, 30));
        options.add(goback);
        options.setStyleName("optionsBox");
        options.setHeight("100%");

        titleBox.setText(getShortDesc());
        titleBox.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        titleBox.setStyleName("title");
        titleBox.setHeight("20px");

        mainPanel.addWest(options, 180);
        mainPanel.add(resultsBox);
        mainPanel.setSize("100%", "600px");

        GwtUtil.setStyles(statusBox, "minHeight", "34px", "marginBottom", "3px");

        container.add(titleBox);
        container.add(statusBox);
        container.add(mainPanel);
        container.setSize("100%", "100%");

        GwtUtil.setStyle(options, "margin", "20px");
        GwtUtil.setStyles(resultsBox, "margin", "5px 10px", "height", "590px");
        DOM.setStyleAttribute((Element) options.getElement().getParentElement(), "boxShadow", "inset 0 0 3px #000000");
        DOM.setStyleAttribute((Element) options.getElement().getParentElement(), "backgroundColor", "#ccc");
        DOM.setStyleAttribute((Element) resultsBox.getElement().getParentElement(), "boxShadow", "inset 0 0 3px #000000");
        DOM.setStyleAttribute((Element) resultsBox.getElement().getParentElement(), "marginLeft", "3px");

        Form form = getForm();
        if (form != null && form.getFieldCount() > 0){
            form.getHub().getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, new WebEventListener() {
                @Override
                public void eventNotify(WebEvent ev) {
                    setStatus("", false);
                }
            });
        }
        return true;
    }

    @Override
    protected FormHub.Validated validate() {
        return super.validate();
    }

    public boolean hasAccess() {
        return hasAccess(accessRole);
    }

    protected boolean hasAccess(String role) {
        if (role == null && !getCurrentUser().isGuestUser()) return true;
        return getCurrentUser().getRoles() != null && getCurrentUser().getRoles().hasAccess(role);
    }

    public void setStatus(String msg, boolean isError) {
        setStatus(isError, msg.split(";"));
    }

    public void setStatus(boolean isError, String... msgs) {
        statusBox.clear();

        if (msgs != null && msgs.length > 0) {
            String msg = "";
            for (String s: msgs) {
                if (!StringUtils.isEmpty(s)) {
                    msg = msg + "<li>" + s;
                }
            }
            if (!StringUtils.isEmpty(msg)) {
                Widget w = new HTML("<ul>" + msg + "</ul>");
                w.setStyleName("status-msg");
                if (isError) {
                    w.addStyleName("error-msg");
                }
                statusBox.add(w);
            }
        }
    }

    @Override
    protected void processRequest(Request req, AsyncCallback<String> callback) {
        // override doExecute..  this method is not needed.
    }

    @Override
    protected void doExecute(Request req, AsyncCallback<String> callback) {
        if (!hasAccess()) {
            accessDenied();
            callback.onSuccess("");
            return;
        }
        setStatus("", false);
        layout(req);
        callback.onSuccess("");
    }

    public void showResults(Widget w) {
        resultsBox.setWidget(w);
        Region r = Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION);
        if (r != null) {
            r.setMinHeight(600);
            r.setDisplay(container);
            r.show();
        }
    }

    abstract protected void layout(Request req);

    public void submitRequst(final TableServerRequest req) {
        ServerTask<RawDataSet> st = new ServerTask<RawDataSet>() {

            @Override
            protected void onFailure(Throwable caught) {
                String msg = caught.getMessage();
                if (caught instanceof RPCException) {
                    String eum = ((RPCException) caught).getEndUserMsg();
                    if (!StringUtils.isEmpty(eum)) {
                        msg = eum;
                    }
                }
                setStatus(msg, true);
            }

            @Override
            public void onSuccess(RawDataSet result) {
                DataSet data = DataSetParser.parse(result);
                UmanCmd.this.onSubmitSuccess(data);
            }

            @Override
            public void doTask(AsyncCallback<RawDataSet> passAlong) {
                SearchServices.App.getInstance().getRawDataSet(req, passAlong);
            }
        };
        st.start();

    }

    protected List<String> getCommands() {
        return cmds;
    }

    protected void onSubmitSuccess(DataSet data) {
        String msg = data.getMeta().getAttribute("Message");
        if (msg != null) {
            setStatus(msg, false);
        }
    }

    protected void createAndProcessRequest() {
        FormHub.Validated validated = getForm().validated();
        if (validated.isValid()) {
            final Request req = new Request();
            if (getForm() != null) {
                getForm().populateRequest(req, new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) {
                    }

                    public void onSuccess(String result) {
                        TableServerRequest sreq = makeServerRequest(req);
                        submitRequst(sreq);
                    }
                });
            }
        } else {
            if (StringUtils.isEmpty(validated.getMessage())) {
                GwtUtil.showValidationError();
            } else {
                setStatus(validated.getMessage(), true);
            }
        }


    }

    protected TableServerRequest makeServerRequest(Request req) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        return sreq;
    }

    protected void updateCurrentUser() {
        Application.getInstance().getLoginManager().refreshUserInfo();
//
//
//        Application.getInstance().getLoginManager().getLoginInfo();
//        UserServices.App.getInstance(true).getUserInfo(false, new AsyncCallback<UserInfo>() {
//            public void onFailure(Throwable caught) {
//                currentUser = null;
//                callback.onSuccess(null);
//            }
//
//            public void onSuccess(UserInfo result) {
//                currentUser = result;
//                callback.onSuccess(result);
//            }
//        });
    }

    protected void accessDenied() {
        HTML msg = null;
        String loginUrl = JossoUtil.makeLoginUrl(Window.Location.getHref());
        if (getCurrentUser().isGuestUser()) {
            msg = new HTML("You are not logged in.  Click <a href=" + loginUrl + ">here</a> to login.");
        } else {
            msg = new HTML("You are not authorized to view this page.");
        }
        showResults(msg);
    }

    public UserInfo getCurrentUser() {
        return Application.getInstance().getLoginManager().getLoginInfo();
    }

    protected int getInt(TableData.Row row, String cname) {
        try {
            return Integer.parseInt((String) row.getValue(cname));
        } catch (Exception e) {
            return -1;
        }
    }

    protected String getString(TableData.Row row, String cname) {
        Object v = row.getValue(cname);
        return v == null ? "" : v.toString().trim();
    }


}
