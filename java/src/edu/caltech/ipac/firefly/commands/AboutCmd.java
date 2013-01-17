package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.ui.PopupUtil;


public class AboutCmd extends GeneralCommand {
    public static final String COMMAND_NAME= "about";

    public AboutCmd() {
        super(COMMAND_NAME);
    }


    protected void doExecute() {
        Application.getInstance().findVersion(new AsyncCallback<Version>() {
            public void onFailure(Throwable caught) { }

            public void onSuccess(Version v) {
                String out= v.getAppName() + " " + v.toString();
                Widget w= Application.getInstance().getLayoutManager().getRegion(LayoutManager.MENU_REGION).getDisplay();
                PopupUtil.showInfo(w, v.getAppName(), out, 5);
            }
        });

    }
}