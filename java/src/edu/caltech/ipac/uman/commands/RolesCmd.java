package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.gwtclone.SplitLayoutPanelFirefly;
import edu.caltech.ipac.firefly.ui.table.BasicTable;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.uman.core.AddAccessDialog;
import edu.caltech.ipac.uman.core.AddRoleDialog;
import edu.caltech.ipac.util.CollectionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 * @author loi
 * $Id: RolesCmd.java,v 1.1 2012/10/03 22:18:11 loi Exp $
 */
public class RolesCmd extends AdminUmanCmd {

    private AddAccessDialog addAccessDialog;
    private AddRoleDialog addDialog;
    private TablePanel table;
    private SplitLayoutPanelFirefly splitPanel;
    private SimplePanel details = new SimplePanel();

    public RolesCmd() {
        super(SHOW_ROLES, ADMIN_ROLE);
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, "Roles");
        tableParams.put(TablePanelCreator.SHORT_DESC, "List of roles matching search criteria.");
        
        GeneralCommand addRole = new GeneralCommand("addRole", "Add a New Role", "Add a new role into the system.", true) {
            @Override
            protected void doExecute() {
                showAddRoleDialog();
            }
        };

        GeneralCommand removeRole = new GeneralCommand("removeRole", "Remove Selected Roles", "Remove the selected roles from the system permanently.", true) {
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

        GeneralCommand addAccess = new GeneralCommand("addAccess", "Grant User Access", "Grant a user access to the selected role.", true) {
            @Override
            protected void doExecute() {
                showAddAccessDialog();
            }
        };

        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, SHOW_ROLES);
        table = setupTable(sreq, tableParams, addRole, removeRole, addAccess);

        splitPanel = new SplitLayoutPanelFirefly();
        splitPanel.addEast(details, 250);
        splitPanel.add(table);
        splitPanel.setSize("100%", "100%");
        details.setHeight("100%");

        SimplePanel wrapper = new SimplePanel(splitPanel);
        wrapper.setHeight("600px");
        GwtUtil.setStyle(wrapper, "backgroundColor", "white");
        setResults(wrapper);

        Application.getInstance().resize();

        WebEventListener wel = new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        showDetails();
                    }
                };
        table.getEventManager().addListener(TablePanel.ON_ROWHIGHLIGHT_CHANGE, wel);
    }

    private void showDetails() {
        TableData.Row row = table.getTable().getHighlightedRow();
        if (row == null) return;

        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR);
        sreq.setParam(ACTION, USERS_BY_ROLE);
        sreq.setParam(MISSION_ID, String.valueOf(row.getValue(DB_MISSION_ID)));
        sreq.setParam(MISSION_NAME, String.valueOf(row.getValue(DB_MISSION)));
        sreq.setParam(GROUP_ID, String.valueOf(row.getValue(DB_GROUP_ID)));
        sreq.setParam(GROUP_NAME, String.valueOf(row.getValue(DB_GROUP)));
        sreq.setParam(PRIVILEGE, String.valueOf(row.getValue(DB_PRIVILEGE)));
        sreq.setPageSize(Integer.MAX_VALUE);


        ServerTask<RawDataSet> st = new ServerTask<RawDataSet>() {

            @Override
            protected void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(RawDataSet result) {
                VerticalPanel vp = new VerticalPanel();
                vp.add(new HTML("<b>Users with access to the selected Role:</>"));
                vp.setSize("100%", "100%");
                vp.setCellHeight(vp.getWidget(0), "18px");
                details.setWidget(vp);
                vp.setCellVerticalAlignment(vp.getWidget(0), VerticalPanel.ALIGN_MIDDLE);
                GwtUtil.setStyles(vp.getWidget(0), "height", "18px", "width", "100%", "padding", "5px", "backgroundColor", "lightgray");

                if (result.getTotalRows() > 0) {
                    DataSet data = DataSetParser.parse(result);
                    BasicTable detailsTable = new BasicTable(data);
                    detailsTable.setHeight("100%");
                    vp.add(detailsTable);
                    vp.setCellHeight(detailsTable, "100%");
                    detailsTable.fillWidth();
                } else {
                    vp.add(new HTML("<br>&nbsp;&nbsp;&nbsp;No records found."));
                }
            }

            @Override
            public void doTask(AsyncCallback<RawDataSet> passAlong) {
                SearchServices.App.getInstance().getRawDataSet(sreq,passAlong);
            }
        };
        st.start();
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
            addDialog = new AddRoleDialog(Application.getInstance().getLayoutManager().getDisplay()){
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

    private void showAddAccessDialog() {
        if (addAccessDialog == null) {
            addAccessDialog = new AddAccessDialog(Application.getInstance().getLayoutManager().getDisplay()){
                @Override
                public void onCompleted() {
                    table.getEventManager().fireEvent(new WebEvent(table, TablePanel.ON_ROWHIGHLIGHT_CHANGE));
                }
            };
        }
        TableData.Row row = table.getTable().getHighlightedRow();
        if (row != null) {
            addAccessDialog.updateForm(row);
        }
        addAccessDialog.show();
    }


//====================================================================
//
//====================================================================

}
