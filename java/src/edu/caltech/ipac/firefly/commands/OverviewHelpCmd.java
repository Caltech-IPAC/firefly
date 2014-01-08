package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.util.StringUtils;


public class OverviewHelpCmd extends RequestCmd {
    public static final String COMMAND_NAME= "overviewHelp";
    public static final String HELP_ID= "helpId";

    public OverviewHelpCmd() {
        super(COMMAND_NAME);
    }

    @Override
    public void execute(Request req, AsyncCallback<String> callback) {
        doExecute(req, callback);
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {

        HelpManager helpMan = Application.getInstance().getHelpManager();
        if (req != null) {
            String helpId = req.getParam(HELP_ID);
            if (!StringUtils.isEmpty(helpId)) {
                helpMan.showHelpAt(helpId);
            } else {
                helpMan.showHelp();
            }
        }
        else {
            helpMan.showHelp();
        }
        callback.onSuccess("ok");
    }

    public static Request makeHelpRequest(String helpId) {
        Request req = new Request(COMMAND_NAME);
        req.setShortDesc("Online Help");
        if (!StringUtils.isEmpty(helpId)) {
            req.setParam(HELP_ID, helpId);
        }
        return req;
    }
}