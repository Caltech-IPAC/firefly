package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.JossoUtil;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.uman.data.UmanConst.ACTION;
import static edu.caltech.ipac.uman.data.UmanConst.BACK_TO_URL;
import static edu.caltech.ipac.uman.data.UmanConst.CONFIRM_TO_EMAIL;
import static edu.caltech.ipac.uman.data.UmanConst.LOGIN_NAME;
import static edu.caltech.ipac.uman.data.UmanConst.NEW_EMAIL;
import static edu.caltech.ipac.uman.data.UmanConst.NEW_PASS;
import static edu.caltech.ipac.uman.data.UmanConst.PROFILE;
import static edu.caltech.ipac.uman.data.UmanConst.TO_EMAIL;
import static edu.caltech.ipac.uman.data.UmanConst.UMAN_PROCESSOR;

/**
 * @author loi $Id: ChangeEmailCmd.java,v 1.10 2012/10/03 22:18:11 loi Exp $
 */
public class ChangeEmailCmd extends UmanCmd {

    private HTML fromEmail;
    private InputField cToEmail;
    private InputField toEmail;
    private List<String> cmds = Arrays.asList(PROFILE, NEW_PASS, NEW_EMAIL);

    public ChangeEmailCmd() {
        super(NEW_EMAIL);
    }

    protected void updateUserInfo(UserInfo userInfo) {
        fromEmail.setHTML("<b>" + userInfo.getEmail() + "</b>");
    }

    protected Form createForm() {

        fromEmail = new HTML();
        Widget emailp = FormBuilder.createWidget(125, 0, new Label("Old Email:"), fromEmail);
        GwtUtil.setStyle(emailp, "paddingLeft", "5px");
        GwtUtil.setStyle(fromEmail, "paddingLeft", "5px");

        toEmail = FormBuilder.createField(TO_EMAIL);
        cToEmail = FormBuilder.createField(CONFIRM_TO_EMAIL);

        Widget namePanel = FormBuilder.createPanel(new FormBuilder.Config(125, 0),
                emailp,
                toEmail,
                cToEmail);

        final Form form = new Form();
        form.add(namePanel);
        form.setHelpId(null);
//        form.setHelpId("uman.new_email");
        form.addSubmitButton(GwtUtil.makeFormButton("<b>Update</b>",
                new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        createAndProcessRequest();
                    }
                }));

        form.addButton(GwtUtil.makeFormButton("Clear",
                new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        form.reset();
                        focus();
                    }
                }));


        return form;
    }

    protected void layout(Request req) {
        Form form = getForm();
        UserInfo userInfo = getCurrentUser();
        fromEmail.setHTML("<b>" + userInfo.getEmail() + "</b>");
        showResults(form);
    }

    @Override
    protected TableServerRequest makeServerRequest(Request req) {
        UserInfo userInfo = getCurrentUser();
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, NEW_EMAIL);
        sreq.setParam(LOGIN_NAME, userInfo.getLoginName());
        return sreq;
    }

    @Override
    protected void onSubmitSuccess(DataSet data) {
        String msg = data.getMeta().getAttribute("Message");
        msg = StringUtils.isEmpty(msg) ? "" : msg + "; ";
        String backTo = (String) Application.getInstance().getAppData(BACK_TO_URL);
        backTo = backTo == null ? Window.Location.getHref() : backTo;
        setStatus(msg + "Click <a href = '" + JossoUtil.makeLoginUrl(backTo) + "'>here</a> to login using your new email address.", false);

        updateCurrentUser();
        accessDenied();
    }

    @Override
    protected List<String> getCommands() {
        return cmds;
    }

//====================================================================
//
//====================================================================

}
