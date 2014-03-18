package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.uman.data.UserRoleEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 * @author loi
 * $Id: AccessCmd.java,v 1.1 2012/10/03 22:18:11 loi Exp $
 */
public class AccessCmd extends AdminUmanCmd {

    public AccessCmd() {
        super(SHOW_ACCESS, ADMIN_ROLE);
    }

    @Override
    protected TablePanel makeTable(Request req) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, SHOW_ACCESS);

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, "User Access");
        tableParams.put(TablePanelCreator.SHORT_DESC, "List of user/role assignments matching search criteria.");

        UmanAction removeAccess = new UmanAction(this, "removeAccess", "Remove user access", "Remove the selected user access entrie from the system.") {
            @Override
            protected void doExecute() {
                removeSelectedAccess();

//                List<Integer> sels = getTable().getDataset().getSelected();
//                if (sels != null && sels.size() > 0) {
//                    PopupUtil.showConfirmMsg("Remove Selected User Access", "You are about to remove " + sels.size() + " access entries from the system.<br>" +
//                            "This process cannot be undone.  <br>Are you sure you want to continue?",
//                            new ClickHandler() {
//                                public void onClick(ClickEvent event) {
//                                    removeSelectedAccess();
//                                }
//                            });
//                } else {
//                    setStatus("You must first select one or more access entries you want to remove.", true);
//                }
            }
        };

        return createTable(sreq, tableParams, removeAccess);
    }

    private void removeSelectedAccess() {
        TableData.Row row = getTable().getTable().getHighlightedRow();
        RoleList.RoleEntry re = new RoleList.RoleEntry(
                                getString(row, DB_MISSION),
                                getInt(row, DB_MISSION_ID),
                                getString(row, DB_GROUP),
                                getInt(row, DB_GROUP_ID),
                                getString(row, DB_PRIVILEGE)
                        );
        final UserRoleEntry ure = new UserRoleEntry(getString(row, DB_LOGIN_NAME), re);

        PopupUtil.showConfirmMsg("Remove Selected User Access", "You are about to remove " + ure.toString() + " from the system.<br>" +
                "This process cannot be undone.  <br>Are you sure you want to continue?",
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR);
                        sreq.setParam(ACTION, REMOVE_ACCESS);
                        sreq.setParam(ACCESS_LIST, ure.toString());
                        submitRequst(sreq);
                    }
                });

//        List<Integer> sels = getTable().getDataset().getSelected();
//        if (sels == null || sels.size() <=0) return;
//
//        AsyncCallback callback = new AsyncCallback<TableDataView>() {
//            public void onFailure(Throwable caught) {
//            }
//
//            public void onSuccess(TableDataView result) {
//                List<String> selectedRoles = makeUserRoleList(result);
//                final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR);
//                sreq.setParam(ACTION, REMOVE_ACCESS);
//                sreq.setParam(ACCESS_LIST, CollectionUtil.toString(selectedRoles));
//                submitRequst(sreq);
//            }
//        };
//        List<String> filters = new ArrayList(getTable().getDataModel().getFilters());
//        filters.add(TableDataView.ROWID + " in (" + CollectionUtil.toString(sels) + ")");
//        getTable().getDataModel().getAdHocData(callback, null, filters.toArray(new String[filters.size()]));
    }

    private List<String> makeUserRoleList(TableDataView result) {
        ArrayList<String> roles = new ArrayList<String>();

        for(TableData.Row row : result.getModel().getRows()) {
            RoleList.RoleEntry re = new RoleList.RoleEntry(
                    getString(row, DB_MISSION),
                    getInt(row, DB_MISSION_ID),
                    getString(row, DB_GROUP),
                    getInt(row, DB_GROUP_ID),
                    getString(row, DB_PRIVILEGE)
            );
            UserRoleEntry ure = new UserRoleEntry(getString(row, DB_LOGIN_NAME), re);
            roles.add(ure.toString());
        }
        return roles;
    }

//====================================================================
//
//====================================================================

}
