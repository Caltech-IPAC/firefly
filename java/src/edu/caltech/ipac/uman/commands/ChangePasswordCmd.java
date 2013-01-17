package edu.caltech.ipac.uman.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
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
 * $Id: ChangePasswordCmd.java,v 1.9 2012/09/05 01:37:59 loi Exp $
 */
public class ChangePasswordCmd extends UmanCmd {

    private HTML email;
    private InputField password;
    private InputField cpassword;
    private List<String> cmds = Arrays.asList(PROFILE, NEW_PASS, NEW_EMAIL);

    public ChangePasswordCmd() {
        super(NEW_PASS);
    }

    @Override
    protected void checkAccess(Request req, AsyncCallback<String> callback) {
        // supply the role who have access to this page.
        doCheckAccess(null, req, callback);
    }

    protected void updateUserInfo(UserInfo userInfo) {
        email.setHTML("<b>" + userInfo.getEmail() + "</b>");
    }

    protected Form createForm() {

        email = new HTML();
        Widget emailp = FormBuilder.createWidget(125, 0, new Label("Email:"), email);
        GwtUtil.setStyle(emailp, "paddingLeft", "5px");
        GwtUtil.setStyle(email, "paddingLeft", "5px");

        password = FormBuilder.createField(NPASSWORD);
        cpassword = FormBuilder.createField(CPASSWORD);
        Widget namePanel = FormBuilder.createPanel(new FormBuilder.Config(125, 0), emailp, password, cpassword);

        Form form = new Form();
        form.add(namePanel);
        form.setHelpId(null);
//        form.setHelpId("uman.change_password");

        return form;
    }

    @Override
    protected FormHub.Validated  validate() {
        FormHub.Validated v =  super.validate();
        String msg = isPasswordValidate(password.getValue(), cpassword.getValue());
        if (!StringUtils.isEmpty(msg)) {
            v.invalidate("Password does not match Confirm Password.");
        }
        return v;
    }


    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        UserInfo userInfo = getCurrentUser();
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, NEW_PASS);
        sreq.setParam(LOGIN_NAME, userInfo.getLoginName());
        submitRequst(sreq);
    }

    @Override
    protected List<String> getCommands() {
        return cmds;
    }

//====================================================================
//
//====================================================================

}
