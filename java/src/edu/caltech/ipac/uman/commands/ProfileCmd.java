package edu.caltech.ipac.uman.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 * @author loi
 * $Id: ProfileCmd.java,v 1.11 2012/10/03 22:18:11 loi Exp $
 */
public class ProfileCmd extends UmanCmd {

    private HTML email;
    private List<String> cmds = Arrays.asList(PROFILE, NEW_PASS, NEW_EMAIL);

    public ProfileCmd() {
        super(PROFILE);
    }

    protected void updateUserInfo(UserInfo userInfo) {
        email.setHTML("<b>" + userInfo.getEmail() + "</b>");
        getForm().setValue(FIRST_NAME, userInfo.getFirstName());
        getForm().setValue(LAST_NAME, userInfo.getLastName());
        getForm().setValue(ADDRESS, userInfo.getAddress());
        getForm().setValue(CITY, userInfo.getCity());
        getForm().setValue(COUNTRY, userInfo.getCountry());
        getForm().setValue(POSTCODE, userInfo.getPostcode());
        getForm().setValue(PHONE, userInfo.getPhone());
        getForm().setValue(INSTITUTE, userInfo.getInstitute());
    }
    
    protected Form createForm() {

        email = new HTML();
        Widget emailp = FormBuilder.createWidget(125, 0, new Label("Email:"), email);
        GwtUtil.setStyle(email, "paddingLeft", "5px");
        GwtUtil.setStyle(emailp, "paddingLeft", "5px");

        Widget namePanel = FormBuilder.createPanel(new FormBuilder.Config(125, 0),
                emailp,
                FormBuilder.createField(FIRST_NAME),
                FormBuilder.createField(LAST_NAME));

        Widget infoPanel = FormBuilder.createPanel(new FormBuilder.Config(125, 0), ADDRESS, CITY, COUNTRY, POSTCODE, PHONE, INSTITUTE);

        VerticalPanel vp = new VerticalPanel();
        vp.add(namePanel);
        vp.add(GwtUtil.getFiller(0, 10));
        vp.add(infoPanel);

        Form form = new Form();
        form.add(vp);
        form.setHelpId(null);
//        form.setHelpId("uman.profile");

        return form;
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        UserInfo userInfo = getCurrentUser();
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, PROFILE);
        sreq.setParam(LOGIN_NAME, userInfo.getLoginName());
        sreq.setParam(EMAIL, userInfo.getEmail());
        submitRequst(sreq);
    }

    @Override
    protected void onSubmitSuccess(DataSet data) {
        updateCurrentUser(new AsyncCallback<UserInfo>() {
            public void onFailure(Throwable caught) {
                setStatus("Error:" + caught.getMessage(), true);
            }
            public void onSuccess(UserInfo result) {
                updateUserInfo(result);
            }
        });
    }

    @Override
    protected List<String> getCommands() {
        return cmds;
    }

//====================================================================
//
//====================================================================

}
