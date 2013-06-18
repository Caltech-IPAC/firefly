package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.uman.core.AddRoleDialog;
import edu.caltech.ipac.util.CollectionUtil;

import java.util.ArrayList;
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
public class RolesCmd extends AdminUmanCmd {

    private AddRoleDialog addDialog;
    private TablePanel table;

    public RolesCmd() {
        super(SHOW_ROLES);
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, SHOW_ROLES);

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, "Roles");
        tableParams.put(TablePanelCreator.SHORT_DESC, "List of roles matching search criteria.");
        
        GeneralCommand addRole = new GeneralCommand("addRole", "Add a new role", "Add a new role into the system.", true) {
            @Override
            protected void doExecute() {
                showAddRoleDialog();
            }
        };

        GeneralCommand removeRole = new GeneralCommand("removeRole", "Remove selected roles", "Remove the selected roles from the system permanently.", true) {
            @Override
            protected void doExecute() {
                List<Integer> sels = table.getDataset().getSelected();
                if (sels != null && sels.size() > 0) {
                    PopupUtil.showConfirmMsg("Remove selected role(s)", "You are about to remove " + sels.size() + " role(s) from the system.<br>" +
                            "Removing a role will remove all access information associated to that role.<br>  This process cannot be undone.  <br>Are you sure you want to continue?",
                            new ClickHandler() {
                                public void onClick(ClickEvent event) {
                                    removeSelectedRoles();
                                }
                            });
                } else {
                    PopupUtil.showWarning("Invalid selection", "You must first select one or more roles you want to remove.", null);
                }
            }
        };

        table = setupTable(sreq, tableParams, addRole, removeRole);
    }

    private void removeSelectedRoles() {
        List<Integer> sels = table.getDataset().getSelected();
        if (sels == null || sels.size() <=0) return;

        AsyncCallback callback = new AsyncCallback<TableDataView>() {
            public void onFailure(Throwable caught) {
            }

            public void onSuccess(TableDataView result) {
                List<String> selectedRoles = makeRoleList(result);
                final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR);
                sreq.setParam(ACTION, REMOVE_ROLE);
                sreq.setParam(ROLE_LIST, CollectionUtil.toString(selectedRoles));
                submitRequst(sreq);
            }
        };

        List<String> filters = new ArrayList(table.getDataModel().getFilters());
        filters.add(TableDataView.ROWID + " in (" + CollectionUtil.toString(sels) + ")");
        table.getDataModel().getAdHocData(callback, null, filters.toArray(new String[filters.size()]));
    }

    private List<String> makeRoleList(TableDataView result) {
        ArrayList<String> roles = new ArrayList<String>();

        for(TableData.Row row : result.getModel().getRows()) {
            RoleList.RoleEntry re = new RoleList.RoleEntry(
                            getString(row, DB_MISSION),
                            getInt(row, DB_MISSION_ID),
                            getString(row, DB_GROUP),
                            getInt(row, DB_GROUP_ID),
                            getString(row, DB_PRIVILEGE)
                        );
            roles.add(re.toString());
        }
        return roles;
    }

    @Override
    protected void onSubmitSuccess(DataSet data) {
        table.getDataset().deselectAll();
        table.reloadTable(0);
    }

    private void showAddRoleDialog() {
        if (addDialog == null) {
            addDialog = new AddRoleDialog(table){
                @Override
                public void onCompleted() {
                    table.reloadTable(0);
                }
            };
        }
        TableData.Row row = table.getTable().getHighlightedRow();
        if (row != null) {
            addDialog.updateForm(row);
        }
        addDialog.show();
    }

//====================================================================
//
//====================================================================

}
