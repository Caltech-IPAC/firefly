package edu.caltech.ipac.heritage.commands;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.Request;

public class DownloadManagerCmd extends RequestCmd {

    public static final String COMMAND = "DownloadManager";


    public DownloadManagerCmd(String title, String desc, boolean enabled) {
        super(COMMAND, title, desc, enabled);
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {
        Window.alert("This dialog will monitor all of the downloads and its progress");
    }
}