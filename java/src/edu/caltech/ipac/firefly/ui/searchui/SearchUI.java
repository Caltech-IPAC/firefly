package edu.caltech.ipac.firefly.ui.searchui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.ServerRequest;

/**
 * User: roby
 * Date: 1/28/14
 * Time: 1:13 PM
 */
public interface SearchUI {
    public String getKey();
    public String getPanelTitle();
    public String getDesc();
    public Widget makeUI();
    public boolean validate();
    public String getSearchTitle();
    public void makeServerRequest(AsyncCallback<ServerRequest> cb);
    public boolean setServerRequest(ServerRequest request);
}
