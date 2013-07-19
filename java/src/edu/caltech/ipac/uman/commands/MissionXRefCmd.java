package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.uman.core.AddAccessDialog;
import edu.caltech.ipac.uman.core.AddMissionDialog;
import edu.caltech.ipac.uman.data.UserRoleEntry;
import edu.caltech.ipac.util.CollectionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 * @author loi
 * $Id: AccessCmd.java,v 1.1 2012/10/03 22:18:11 loi Exp $
 */
public class MissionXRefCmd extends AdminUmanCmd {

    private AddMissionDialog addDialog;
    private TablePanel table;

    public MissionXRefCmd() {
        super(SHOW_MISSION_XREF, SYS_ADMIN_ROLE);
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, SHOW_MISSION_XREF);

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, "MISSIONS");
        tableParams.put(TablePanelCreator.SHORT_DESC, "A list of missions and its' IDs.");

        GeneralCommand addMission = new GeneralCommand("addMission", "Add a new mission", "Add a new mission into the system.", true) {
            @Override
            protected void doExecute() {
                showAddDialog();
            }
        };

        table = setupTable(WidgetFactory.BASIC_TABLE, sreq, tableParams, addMission);
    }

    @Override
    protected void onSubmitSuccess(DataSet data) {
        table.getDataset().deselectAll();
        table.reloadTable(0);
    }

    private void showAddDialog() {
        if (addDialog == null) {
            addDialog = new AddMissionDialog(Application.getInstance().getLayoutManager().getDisplay()){
                @Override
                public void onCompleted() {
                    table.reloadTable(0);
                }
            };
        }
        addDialog.show();
    }

//====================================================================
//
//====================================================================

}
