package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.util.StringUtils;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 * @author loi $Id: RegistrationCmd.java,v 1.8 2012/10/03 22:18:11 loi Exp $
 */
public class AddAccountCmd extends AdminUmanCmd {

    private InputField fname;
    private InputField lname;
    private InputField email;
    private InputField password;
    private InputField cpassword;
    private InputField genPass;
    private InputField sentTo;

    public AddAccountCmd() {
        super(ADD_ACCOUNT, ADMIN_ROLE);
    }

    protected Form createForm() {
        fname = FormBuilder.createField(FIRST_NAME);
        lname = FormBuilder.createField(LAST_NAME);
        email = FormBuilder.createField(EMAIL);
        password = FormBuilder.createField(PASSWORD);
        cpassword = FormBuilder.createField(CPASSWORD);
        genPass = FormBuilder.createField(GEN_PASS);
        sentTo = FormBuilder.createField(SENDTO_EMAIL);
        HTML emailDesc = GwtUtil.makeFaddedHelp("This email address is also your login name.");
        GwtUtil.setStyle(emailDesc, "textAlign", "left");

        Widget emailp = FormBuilder.createPanel(new FormBuilder.Config(125, 0), email, emailDesc);
        GwtUtil.setStyles(emailp, "paddingLeft", "10px");

        Widget namePanel = FormBuilder.createPanel(125, fname, lname, password, cpassword, genPass, sentTo);
        Widget infoPanel = FormBuilder.createPanel(125, ADDRESS, CITY, COUNTRY, POSTCODE, PHONE, INSTITUTE);

        genPass.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> event) {
                ensurePasswords();
            }
        });
        genPass.setValue("false");

        password.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> event) {
                String pw = password.getValue();
                if (!StringUtils.isEmpty(pw)) {
                    genPass.setValue(String.valueOf(false));
                }
            }
        });
        VerticalPanel vp = new VerticalPanel();
        vp.add(emailp);
        vp.add(namePanel);
        vp.add(GwtUtil.getFiller(0, 10));
        vp.add(infoPanel);

        HorizontalPanel hp = new HorizontalPanel();
        hp.add(vp);

        final Form form = new Form();
        form.add(hp);
        form.setHelpId(null);

        form.addSubmitButton(GwtUtil.makeFormButton("<b>Submit</b>",
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

    @Override
    protected void layout(Request req) {
        showResults(getForm());
    }

    @Override
    protected TableServerRequest makeServerRequest(Request req) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, REGISTER);
        sreq.setParam(LOGIN_NAME, email.getValue());
        return sreq;
    }

    private void ensurePasswords() {
        boolean gpass = Boolean.parseBoolean(genPass.getValue());
        password.getFieldDef().setNullAllow(gpass);
        cpassword.getFieldDef().setNullAllow(gpass);
        if (gpass) {
            password.setValue("");
            cpassword.setValue("");
        }
    }

}
