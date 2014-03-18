package edu.caltech.ipac.uman.commands;

import com.google.gwt.core.client.Scheduler;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.uman.data.UmanConst.ADD_ACCOUNT;
import static edu.caltech.ipac.uman.data.UmanConst.SHOW_ACCESS;
import static edu.caltech.ipac.uman.data.UmanConst.SHOW_MISSION_XREF;
import static edu.caltech.ipac.uman.data.UmanConst.SHOW_ROLES;
import static edu.caltech.ipac.uman.data.UmanConst.SHOW_USERS;
import static edu.caltech.ipac.uman.data.UmanConst.SYS_ADMIN_ROLE;

/**
 * @author loi $Id: UmanCmd.java,v 1.13 2012/11/19 22:05:43 loi Exp $
 */
abstract public class AdminUmanCmd extends UmanCmd {
    private List<String> cmds = new ArrayList<String>(Arrays.asList(ADD_ACCOUNT, SHOW_USERS, SHOW_ROLES, SHOW_ACCESS));
    private TablePanel table;
    private boolean doEvent = true;


    public AdminUmanCmd(String command, String accessRole) {
        super(command, accessRole);
    }

    @Override
    protected Form createForm() {
        return null;
    }

    protected TablePanel makeTable(Request req) {
        return null;
    }

    protected TablePanel getTable() {
        return table;
    }

    @Override
    protected void layout(Request req) {
        if (table == null) {
            table = makeTable(req);
            if (table != null) {
                GwtUtil.setStyle(table, "backgroundColor", "white");
            }
        } else {
            doEvent = false;
            table.getEventManager().addListener(TablePanel.ON_PAGE_LOAD, new WebEventListener() {
                                @Override
                                public void eventNotify(WebEvent ev) {
                                    table.getEventManager().removeListener(TablePanel.ON_PAGE_LOAD, this);
                                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                        @Override
                                        public void execute() {
                                            doEvent = true;
                                        }
                                    });
                                }
                            });
            table.getDataset().deselectAll();
            table.reloadTable(0);
        }
        showResults(table);
    }

    @Override
    protected void onSubmitSuccess(DataSet data) {
        layout(null);
        super.onSubmitSuccess(data);
    }

    @Override
    public boolean init() {
        if (hasAccess(SYS_ADMIN_ROLE)) {
            cmds.add(SHOW_MISSION_XREF);
        }
        return super.init();
    }

    @Override
    protected List<String> getCommands() {
        return cmds;
    }

    protected TablePanel createTable(TableServerRequest sreq, Map<String, String> tableParams, final UmanAction... buttons) {
        return createTable(WidgetFactory.BASIC_TABLE, sreq, tableParams, buttons);
    }

    protected TablePanel createTable(String type, TableServerRequest sreq, Map<String, String> tableParams, final UmanAction... buttons) {

        WidgetFactory factory = Application.getInstance().getWidgetFactory();

        final PrimaryTableUI primary = factory.createPrimaryUI(type, sreq, tableParams);
        EventHub hub = new EventHub();
        primary.bind(hub);
        hub.getEventManager().addListener(new WebEventListener() {
            @Override
            public void eventNotify(WebEvent ev) {
                if (doEvent) {
                    setStatus("", false);
                }
            }
        });

        PrimaryTableUILoader loader = getTableUiLoader();
        loader.addTable(primary);
        loader.loadAll();

        final TablePanel table = (TablePanel) primary.getDisplay();
        table.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                table.showPopOutButton(false);
//                table.showSaveButton(false);
                if (buttons != null) {
                    for (GeneralCommand cmd : buttons) {
                        table.addToolButton(cmd, false);
                    }
                }
                table.getEventManager().removeListener(TablePanel.ON_INIT, this);
            }
        });

//        SimplePanel wrapper = new SimplePanel(table);
//        wrapper.setHeight("600px");
//        GwtUtil.setStyle(wrapper, "backgroundColor", "white");
//        showResults(wrapper);
//
//        Application.getInstance().resize();
        return table;
    }

}
