package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.InputField;

import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.uman.data.UmanConst.ACTION;
import static edu.caltech.ipac.uman.data.UmanConst.CPASSWORD;
import static edu.caltech.ipac.uman.data.UmanConst.LOGIN_NAME;
import static edu.caltech.ipac.uman.data.UmanConst.NEW_EMAIL;
import static edu.caltech.ipac.uman.data.UmanConst.NEW_PASS;
import static edu.caltech.ipac.uman.data.UmanConst.NPASSWORD;
import static edu.caltech.ipac.uman.data.UmanConst.PROFILE;
import static edu.caltech.ipac.uman.data.UmanConst.UMAN_PROCESSOR;

/**
 * @author loi $Id: ChangePasswordCmd.java,v 1.9 2012/09/05 01:37:59 loi Exp $
 */
public class ChangePasswordCmd extends UmanCmd {

    private HTML email;
    private InputField password;
    private InputField cpassword;
    private List<String> cmds = Arrays.asList(PROFILE, NEW_PASS, NEW_EMAIL);

    public ChangePasswordCmd() {
        super(NEW_PASS);
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

        final Form form = new Form();
        form.add(namePanel);
        form.setHelpId(null);
//        form.setHelpId("uman.change_password");
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
        email.setHTML("<b>" + userInfo.getEmail() + "</b>");
        showResults(form);
    }

    @Override
    protected TableServerRequest makeServerRequest(Request req) {
        UserInfo userInfo = getCurrentUser();
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, NEW_PASS);
        sreq.setParam(LOGIN_NAME, userInfo.getLoginName());
        return sreq;
    }

    @Override
    protected List<String> getCommands() {
        return cmds;
    }

//====================================================================
//
//====================================================================

}
