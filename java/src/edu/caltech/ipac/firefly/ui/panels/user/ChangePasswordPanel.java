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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
