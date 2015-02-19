/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.panels.user;


import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.EventListener;

/**
 * @author tatianag
 * @version $Id: UserInfoPanel.java,v 1.9 2012/09/13 22:15:11 loi Exp $
 */
@Deprecated
public class UserInfoPanel extends Composite {
    public static final String ERROR_SIGN_IN = "Please, login before using this panel.";

    public static final String FIRSTNAME_KEY="ManageAccount.field.firstname";
    public static final String LASTNAME_KEY="ManageAccount.field.lastname";
    public static final String EMAIL_KEY="ManageAccount.field.email";
    public static final String USERNAME_KEY="ManageAccount.field.username";
    public static final String PASSWORD_KEY="ManageAccount.field.pass";

    Form inputForm;
    HTML status = new HTML("<br>");
    VerticalPanel vp = new VerticalPanel();
    UserInfo userInfo = null;


    public UserInfoPanel(final UserInfoListener listener) {

        Widget fp = FormBuilder.createPanel(300, FIRSTNAME_KEY, LASTNAME_KEY, EMAIL_KEY, USERNAME_KEY, PASSWORD_KEY);

        inputForm = new Form();
        inputForm.setHelpId("user.manageacct");
        inputForm.add(fp);

        // disable username field
        inputForm.getField(USERNAME_KEY).getFocusWidget().setEnabled(false);

        final UserInfoPanel panel = this;
        inputForm.getButtonBar().addLeft(GwtUtil.makeFormButton("Delete Account",
                new ClickHandler(){
                    public void onClick(ClickEvent ev) {
                        if (userInfo == null) {
                            setStatus(ERROR_SIGN_IN);
                            return;
                        }
                        setStatus("");
                        if (inputForm.validate()) {
                            listener.onDeleteAccount(panel);
                        } else {
                            setStatus("One or more input field(s) is not valid.");
                        }
                    }
                }));
        inputForm.getButtonBar().addLeftSpacer();

        inputForm.addSubmitButton(GwtUtil.makeFormButton("Update",
                new ClickHandler(){
                    public void onClick(ClickEvent ev) {
                        if (userInfo == null) {
                            setStatus(ERROR_SIGN_IN);
                            return;
                        }
                        setStatus("");
                        if (inputForm.validate()) {
                            if (unchanged(userInfo, getUpdatedInfo())) {
                                setStatus("Nothing to update.");
                            } else {
                                listener.onUpdateAccount(panel);
                            }
                        } else {
                            setStatus("One or more input field(s) is not valid.");
                        }
                    }
                }));

        inputForm.addButton(GwtUtil.makeFormButton("Reset",
                new ClickHandler(){
                    public void onClick(ClickEvent ev) {
                        populateForm(userInfo);
                    }
                }));

        status.setStyleName("user-status-text");

        vp.setSpacing(20);
        vp.add(inputForm);
        vp.add(status);
        initWidget(vp);
    }

    public UserInfo getUpdatedInfo() {
/** no longer need this class.. comment out code so it'll compile
        return new UserInfo( (userInfo == null) ? 0 : userInfo.getLoginid(),
                inputForm.getValue(USERNAME_KEY),
                PasswordUtil.getMD5Hash(inputForm.getValue(PASSWORD_KEY)),
                inputForm.getValue(FIRSTNAME_KEY),
                inputForm.getValue(LASTNAME_KEY),
                inputForm.getValue(EMAIL_KEY));
 */
        return null;
    }

    public void setStatus(String msg) {
        status.setText(msg);
        status.setStyleName("user-status-text");
    }

    public void setStatus(String msg, String styleName) {
        status.setText(msg);
        status.setStyleName(styleName);
    }

    public static interface UserInfoListener extends EventListener {

        public void onUpdateAccount(UserInfoPanel infoPanel);

        public void onDeleteAccount(UserInfoPanel infoPanel);
    }

    public void populateForm(UserInfo info) {
        userInfo = info;
        if (userInfo == null) {
            setStatus(ERROR_SIGN_IN);
            inputForm.setValue(FIRSTNAME_KEY, null);
            inputForm.setValue(LASTNAME_KEY, null);
            inputForm.setValue(EMAIL_KEY, null);
            inputForm.setValue(USERNAME_KEY, null);
            inputForm.setValue(PASSWORD_KEY, null);
            return;
        }
        setStatus("");
        inputForm.setValue(FIRSTNAME_KEY, info.getFirstName());
        inputForm.setValue(LASTNAME_KEY, info.getLastName());
        inputForm.setValue(EMAIL_KEY, info.getEmail());
        inputForm.setValue(USERNAME_KEY, info.getLoginName());
        inputForm.setValue(PASSWORD_KEY, info.getPassword());
    }

    private boolean unchanged(UserInfo oldInfo, UserInfo newInfo) {
        return (oldInfo.getFirstName().equals(newInfo.getFirstName()) &&
            oldInfo.getLastName().equals(newInfo.getLastName()) &&
            oldInfo.getEmail().equals(newInfo.getEmail()));
    }

}
