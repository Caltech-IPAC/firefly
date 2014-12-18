package edu.caltech.ipac.fuse.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.fuse.core.FuseLayoutManager;

/**
 * Date: 7/3/14
 *
 * @author loi
 * @version $Id: $
 */
public class TestLayoutCmd  extends RequestCmd {
    private FuseLayoutManager.VType _VType;

    public TestLayoutCmd(FuseLayoutManager.VType VType) {
        super("test");
        this._VType = VType;
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {
        FuseLayoutManager lm = (FuseLayoutManager) Application.getInstance().getLayoutManager();
        lm.switchView(_VType);
    }
}
