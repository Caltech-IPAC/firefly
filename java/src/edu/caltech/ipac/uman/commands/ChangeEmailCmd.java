package edu.caltech.ipac.uman.commands;

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
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 * @author loi
 * $Id: ChangeEmailCmd.java,v 1.10 2012/10/03 22:18:11 loi Exp $
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

        Form form = new Form();
        form.add(namePanel);
        form.setHelpId(null);
//        form.setHelpId("uman.new_email");

        return form;
    }

    @Override
    protected FormHub.Validated  validate() {
        FormHub.Validated v =  super.validate();
        String msg = isEmailValidate(toEmail.getValue(), cToEmail.getValue());
        if (!StringUtils.isEmpty(msg)) {
            v.invalidate("New Email does not match Confirm New Password.");
        }
        return v;
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        UserInfo userInfo = getCurrentUser();
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, NEW_EMAIL);
        sreq.setParam(LOGIN_NAME, userInfo.getLoginName());
        submitRequst(sreq);
    }

    @Override
    protected void onSubmitSuccess(DataSet data) {
        String backTo = (String) Application.getInstance().getAppData(BACK_TO_URL);
        backTo = backTo == null ? Window.Location.getHref() : backTo;
        setStatus("Click <a href = '" + JossoUtil.makeLoginUrl(backTo) + "'>here</a> to login using your new email address.", false, false);
    }

    @Override
    protected List<String> getCommands() {
        return cmds;
    }

//====================================================================
//
//====================================================================

}
