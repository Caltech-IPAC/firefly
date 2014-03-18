package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.uman.core.PasswordResetDialog;
import edu.caltech.ipac.uman.data.UmanConst;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static edu.caltech.ipac.uman.data.UmanConst.ACTION;
import static edu.caltech.ipac.uman.data.UmanConst.ADMIN_ROLE;
import static edu.caltech.ipac.uman.data.UmanConst.LOGIN_NAME;
import static edu.caltech.ipac.uman.data.UmanConst.REMOVE_USER;
import static edu.caltech.ipac.uman.data.UmanConst.SHOW_USERS;
import static edu.caltech.ipac.uman.data.UmanConst.UMAN_PROCESSOR;

/**
 * @author loi
 * $Id: AccessCmd.java,v 1.1 2012/10/03 22:18:11 loi Exp $
 */
public class UsersCmd extends AdminUmanCmd {

    private PasswordResetDialog resetDialog;

    public UsersCmd() {
        super(SHOW_USERS, ADMIN_ROLE);
    }

    @Override
    protected TablePanel makeTable(Request req) {
        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, SHOW_USERS);

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, "Users");
        tableParams.put(TablePanelCreator.SHORT_DESC, "List of Users.");

        TablePanel table = null;
        if (hasAccess(UmanConst.SYS_ADMIN_ROLE)) {
            UmanAction reset = new UmanAction(this, "resetPassword", "Reset Password", "Reset the highlighted user's password.") {
                @Override
                protected void doExecute() {
                    int idx = getTable().getDataset().getHighlighted();
                    if (idx >= 0) {
                        TableData.Row row = getTable().getTable().getRowValue(idx);
                        if (row != null) {
                            if (resetDialog == null) {
                                resetDialog = new PasswordResetDialog(Application.getInstance().getLayoutManager().getDisplay(), UsersCmd.this);
                            }
                            resetDialog.updateForm(row);
                            resetDialog.show();
                        }
                    }
                }
            };

            UmanAction remove = new UmanAction(this, "removeUser", "Remove User", "Remove the highlighted user from the system.") {
                @Override
                protected void doExecute() {
                    int idx = getTable().getDataset().getHighlighted();
                    if (idx >= 0) {
                        final TableData.Row row = getTable().getTable().getRowValue(idx);
                        if (row != null && !StringUtils.isEmpty(row.getValue(UmanConst.DB_EMAIL))) {
                            final String email = String.valueOf(row.getValue(UmanConst.DB_EMAIL));
                            PopupUtil.showConfirmMsg("Remove highlighted user", "You are about to remove " + email + " from the system.<br>" +
                                    "This process cannot be undone.  <br>Are you sure you want to continue?",
                                    new ClickHandler() {
                                        public void onClick(ClickEvent event) {
                                            final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR);
                                            sreq.setParam(ACTION, REMOVE_USER);
                                            sreq.setParam(LOGIN_NAME, email);
                                            submitRequst(sreq);
                                        }
                                    });
                        }
                    }
                }
            };
            table = createTable(WidgetFactory.BASIC_TABLE, sreq, tableParams, reset, remove);
        } else {
            table = createTable(WidgetFactory.BASIC_TABLE, sreq, tableParams);
        }
        return table;
    }


//====================================================================
//
//====================================================================

}
