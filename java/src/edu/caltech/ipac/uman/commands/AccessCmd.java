package edu.caltech.ipac.uman.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.uman.core.AddAccessDialog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 * @author loi
 * $Id: AccessCmd.java,v 1.1 2012/10/03 22:18:11 loi Exp $
 */
public class AccessCmd extends UmanCmd {

    private List<String> cmds = Arrays.asList(SHOW_ROLES, SHOW_ACCESS, REGISTER);
    private AddAccessDialog addDialog;
    private TablePanel table;

    public AccessCmd() {
        super(SHOW_ACCESS);
    }

    @Override
    protected void checkAccess(Request req, AsyncCallback<String> callback) {
        // supply the role who have access to this page.
        doCheckAccess(ADMIN_ROLE, req, callback);
    }

    protected Form createForm() {

        Widget fields = FormBuilder.createPanel(new FormBuilder.Config(125, 0),
                FormBuilder.createField(MISSION_NAME)
        );


        VerticalPanel vp = new VerticalPanel();
        vp.add(fields);

        Form form = new Form();
        form.add(vp);
        form.setHelpId(null);
//        form.setHelpId("uman.profile");

        return form;
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, SHOW_ACCESS);

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, "User Access");
        tableParams.put(TablePanelCreator.SHORT_DESC, "List of user/role assignments matching search criteria.");

        GeneralCommand addRole = new GeneralCommand("addAccess", "Assign User Access", "Assign a new role to a user.", true) {
            @Override
            protected void doExecute() {
                showAddAccessDialog();
            }
        };

        GeneralCommand removeRole = new GeneralCommand("removeRole", "Remove Role", "Remove a role from the system permanently.", true) {
            @Override
            protected void doExecute() {
                removeSelectedRoles();
            }
        };

        table = setupTable(sreq, tableParams, addRole, removeRole);
    }

    private void removeSelectedRoles() {
    }

    private void showAddAccessDialog() {
        if (addDialog == null) {
            addDialog = new AddAccessDialog(table){
                @Override
                public void onCompleted() {
                    table.gotoPage(1);
                }
            };
        }
        addDialog.show();
    }

    @Override
    protected List<String> getCommands() {
        return cmds;
    }

//====================================================================
//
//====================================================================

}
