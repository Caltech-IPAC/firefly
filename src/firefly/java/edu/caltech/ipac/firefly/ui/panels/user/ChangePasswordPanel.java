/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.panels.user;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.PasswordUtil;


/**
 * @author tatianag
 * @version $Id: ChangePasswordPanel.java,v 1.8 2012/08/09 01:09:26 loi Exp $
 */
public class ChangePasswordPanel extends Composite {
    public static final String USERNAME_KEY="ManageAccount.field.username";
    public static final String PASSWORD_KEY="ManageAccount.field.passOld";
    public static final String PASSWORDNEW1_KEY="ManageAccount.field.passNew1";
    public static final String PASSWORDNEW2_KEY="ManageAccount.field.passNew2";

    private Form inputForm;
    HTML status = new HTML("<br>");
    VerticalPanel vp = new VerticalPanel();

    public ChangePasswordPanel(final ChangePasswordListener listener) {
        initWidget(vp);
        Widget cp1 = FormBuilder.createPanel(300, PASSWORD_KEY, PASSWORDNEW1_KEY, PASSWORDNEW2_KEY);
        inputForm = new Form();
        inputForm.setHelpId("user.changepass");
        inputForm.add(cp1);

        final ChangePasswordPanel panel = this;
        inputForm.addSubmitButton(GwtUtil.makeFormButton("Change Password",
                new ClickHandler(){
                    public void onClick(ClickEvent ev) {
                       setStatus("");
                       if (inputForm.validate()) {
                           String oldPass = inputForm.getValue(PASSWORD_KEY);
                           String newPass = inputForm.getValue(PASSWORDNEW1_KEY);
                           String newPass2 = inputForm.getValue(PASSWORDNEW2_KEY);
                           if (newPass.equals(newPass2)) {
                               listener.onPasswordChanged(PasswordUtil.getMD5Hash(oldPass), PasswordUtil.getMD5Hash(newPass), panel);
                           } else {
                               setStatus("Confirmed password does not match new password");
                           }
                        } else {
                            setStatus("One or more input field(s) is not valid.");
                        }
                    }
                }));

        inputForm.addButton(GwtUtil.makeFormButton("Reset",
                new ClickHandler(){
                    public void onClick(ClickEvent ev) {
                        reset();
                    }
                }));

        status = new HTML("<br>");
        status.setStyleName("user-status-text");
        vp.setSpacing(20);
        vp.add(inputForm);
        vp.add(status);


    }

    public void setStatus(String msg) {
        status.setText(msg);
        status.setStyleName("user-status-text");
    }

    public void setStatus(String msg, String styleName) {
        status.setText(msg);
        status.setStyleName(styleName);
    }

    public void reset() {
        inputForm.reset();
        setStatus("");
        focus();
    }

    public void focus() {
        inputForm.setFocus(PASSWORD_KEY);
    }

    public interface ChangePasswordListener {
        public void onPasswordChanged(String oldPass, String newPass, ChangePasswordPanel panel);
    }
}
