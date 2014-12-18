package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.TablePanel;



/**
 * User: balandra
 * Date: Oct 9, 2009
 * Time: 11:54:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class HistoryCmd extends RequestCmd {
    public static final String COMMAND_NAME = "History";

    public HistoryCmd(){
        super(COMMAND_NAME);
    }

    protected Form createForm() {
        return null;
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {
        processRequest(req, callback);
    }

    protected void processRequest(Request req, AsyncCallback<String> callback) {
        TablePanel table = new HistoryTagsCmd.TagHistoryConfig(false).createAndLoadTable();
        table.setHeight("400px");
        SimplePanel p = new SimplePanel();
        p.add(table);
                GwtUtil.setStyle(p, "margin", "5px");
        registerView(LayoutManager.DROPDOWN_REGION, p);
        callback.onSuccess("");
    }

}

