package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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

import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 * @author loi $Id: RegistrationCmd.java,v 1.8 2012/10/03 22:18:11 loi Exp $
 */
public class RegistrationCmd extends UmanCmd {

    private InputField fname;
    private InputField lname;
    private InputField email;
    private InputField password;
    private InputField cpassword;
    private List<String> cmds = Arrays.asList(REGISTER);

    public RegistrationCmd() {
        super(REGISTER);
    }

    @Override
    protected Form createForm() {
        fname = FormBuilder.createField(FIRST_NAME);
        lname = FormBuilder.createField(LAST_NAME);
        email = FormBuilder.createField(EMAIL);
        password = FormBuilder.createField(PASSWORD);
        cpassword = FormBuilder.createField(CPASSWORD);
        HTML emailDesc = GwtUtil.makeFaddedHelp("This email address is also your login name.");
        GwtUtil.setStyle(emailDesc, "textAlign", "left");

        Widget emailp = FormBuilder.createPanel(new FormBuilder.Config(75, 0), email, emailDesc);
        GwtUtil.setStyles(emailp, "paddingLeft", "10px");

        Widget namePanel = FormBuilder.createPanel(80, fname, lname, password, cpassword);
        Widget infoPanel = FormBuilder.createPanel(80, ADDRESS, CITY, COUNTRY, POSTCODE, PHONE, INSTITUTE);

        VerticalPanel vp = new VerticalPanel();
        vp.add(emailp);
        vp.add(namePanel);
        vp.add(GwtUtil.getFiller(0, 10));
        vp.add(infoPanel);

        HTML notes = new HTML("The account created here is an IRSA account, allowing you to access data " +
                "from multiple projects at IRSA with the same account. Proprietary data can be accessed " +
                "via this account, but are controlled on a project-by-project basis, all served by the " +
                "same login system.  History and preferences are also distinct between projects.");
        notes.setStyleName("highlight");

        HorizontalPanel hp = new HorizontalPanel();
        hp.add(vp);
        hp.add(notes);
        hp.setCellWidth(vp, "350px");
        hp.setCellWidth(notes, "250px");


        final Form form = new Form();
        form.add(hp);
        form.setHelpId(null);
        hp.setWidth("100%");

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

    protected void layout(Request req) {
        showResults(getForm());
    }

    protected TableServerRequest makeServerRequest(final Request req) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, REGISTER);
        sreq.setParam(LOGIN_NAME, email.getValue());
        return sreq;
    }

    @Override
    public boolean hasAccess() {
        return true;
    }

    @Override
    protected List<String> getCommands() {
        return cmds;
    }

}
