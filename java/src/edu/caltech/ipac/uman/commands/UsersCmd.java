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
import edu.caltech.ipac.uman.core.PasswordResetDialog;
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
public class UsersCmd extends AdminUmanCmd {

    private TablePanel table;
    private PasswordResetDialog resetDialog;

    public UsersCmd() {
        super(SHOW_USERS, SYS_ADMIN_ROLE);
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, SHOW_USERS);

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, "Users");
        tableParams.put(TablePanelCreator.SHORT_DESC, "List of Ssers.");

        GeneralCommand reset = new GeneralCommand("resetPassword", "Reset Password", "Reset the selected user's password.", true) {
            @Override
            protected void doExecute() {
                int idx = table.getDataset().getHighlighted();
                if (idx >= 0) {
                    TableData.Row row = table.getTable().getRowValue(idx);
                    if (row != null) {
                        if (resetDialog == null) {
                            resetDialog = new PasswordResetDialog(Application.getInstance().getLayoutManager().getDisplay());
                        }
                        resetDialog.updateForm(row);
                        resetDialog.show();
                    }
                }
            }
        };

        table = setupTable(WidgetFactory.BASIC_TABLE, sreq, tableParams, reset);
    }

//====================================================================
//
//====================================================================

}
