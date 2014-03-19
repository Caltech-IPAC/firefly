package edu.caltech.ipac.uman.commands;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.uman.core.AddMissionDialog;

import java.util.HashMap;
import java.util.Map;

import static edu.caltech.ipac.uman.data.UmanConst.ACTION;
import static edu.caltech.ipac.uman.data.UmanConst.SHOW_MISSION_XREF;
import static edu.caltech.ipac.uman.data.UmanConst.SYS_ADMIN_ROLE;
import static edu.caltech.ipac.uman.data.UmanConst.UMAN_PROCESSOR;

/**
 * @author loi
 * $Id: AccessCmd.java,v 1.1 2012/10/03 22:18:11 loi Exp $
 */
public class MissionXRefCmd extends AdminUmanCmd {

    private AddMissionDialog addDialog;

    public MissionXRefCmd() {
        super(SHOW_MISSION_XREF, SYS_ADMIN_ROLE);
    }

    @Override
    protected TablePanel makeTable(Request req) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, SHOW_MISSION_XREF);

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, "MISSIONS");
        tableParams.put(TablePanelCreator.SHORT_DESC, "A list of missions and its' IDs.");

        UmanAction addMission = new UmanAction(this, "addMission", "Add a new mission", "Add a new mission into the system.") {
            @Override
            protected void doExecute() {
                showAddDialog();
            }
        };

        return createTable(WidgetFactory.BASIC_TABLE, sreq, tableParams, addMission);
    }

    private void showAddDialog() {
        if (addDialog == null) {
            addDialog = new AddMissionDialog(Application.getInstance().getLayoutManager().getDisplay(), this);
        }
        addDialog.show();
    }

//====================================================================
//
//====================================================================

}
