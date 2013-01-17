package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.uman.core.AddRoleDialog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 * @author loi
 * $Id: RolesCmd.java,v 1.1 2012/10/03 22:18:11 loi Exp $
 */
public class RolesCmd extends UmanCmd {

    private List<String> cmds = Arrays.asList(SHOW_ROLES, SHOW_ACCESS, REGISTER);
    private AddRoleDialog addDialog;
    private TablePanel table;

    public RolesCmd() {
        super(SHOW_ROLES);
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
        sreq.setParam(ACTION, SHOW_ROLES);

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, "Roles");
        tableParams.put(TablePanelCreator.SHORT_DESC, "List of roles matching search criteria.");
        
        GeneralCommand addRole = new GeneralCommand("addRole", "Add Role", "Add a new role into the system.", true) {
            @Override
            protected void doExecute() {
                showAddRoleDialog();
            }
        };

        GeneralCommand removeRole = new GeneralCommand("removeRole", "Remove Role", "Remove a role from the system permanently.", true) {
            @Override
            protected void doExecute() {
                PopupUtil.showConfirmMsg("Remove selected role(s)", "Removing a role will remove all access information associated to that role.  This process cannot be undo.  <br>Are you sure you want to continue?",
                        new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                removeSelectedRoles();
                            }
                        });
            }
        };

        table = setupTable(sreq, tableParams, addRole, removeRole);
    }

    private void removeSelectedRoles() {
        Set<TableData.Row> sels = table.getTable().getSelectedRowValues();
        for(TableData.Row row : sels) {
            final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR);
            sreq.setParam(ACTION, REMOVE_ROLE);
            sreq.setParam(MISSION_NAME, String.valueOf(row.getValue(DB_MISSION)));
            sreq.setParam(MISSION_ID, String.valueOf(row.getValue(DB_MISSION_ID)));
            sreq.setParam(GROUP_NAME, String.valueOf(row.getValue(DB_GROUP)));
            sreq.setParam(GROUP_ID, String.valueOf(row.getValue(DB_GROUP_ID)));
            submitRequst(sreq);
        }
        table.getDataset().deselectAll();
        table.gotoPage(1);
    }

    private void showAddRoleDialog() {
        if (addDialog == null) {
            addDialog = new AddRoleDialog(table){
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
