/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;

import java.util.HashMap;

public abstract class RequestCmd extends GeneralCommand {
    
    private HashMap<String, Widget> views = new HashMap<String, Widget>();

    public RequestCmd(String command) {
        super(command);
    }

    public RequestCmd(String command, String title) {
        this(command, title, title, true);
    }

    public RequestCmd(String command, String title, String desc, boolean enabled) {
        super(command, title, desc, enabled);
    }

    public Widget getView(String regionId) {
        return views.get(regionId);
    }

    public void execute(final Request req) {
        execute(req, new AsyncCallback<String>(){
                            public void onFailure(Throwable throwable) {}
                            public void onSuccess(String s) {}
                        });
    }

    public void execute(final Request req, final AsyncCallback<String> callback) {

        if (!isEnabled()) return;

        clearView();
        if (!isInit()) {
            GWT.runAsync(new GwtUtil.DefAsync() {
                public void onSuccess() {
                    setInit(init());
                    beginExecute(req,callback);
                }
            });
        }
        else {
            beginExecute(req,callback);
        }
    }


    private void beginExecute(final Request req, final AsyncCallback<String> callback) {
        if (isInit()) {
            GWT.runAsync(new GwtUtil.DefAsync() {
                public void onSuccess() {
                    doExecute(req, callback);
                }
            });
        } else {
            // due to async init, this init() could return false.
            // in that case, add a listener to execute this request once it is initialized.
            PropertyChangeListener list= new PropertyChangeListener(){
                public void propertyChange(PropertyChangeEvent pce) {
                    GWT.runAsync(new GwtUtil.DefAsync() {
                        public void onSuccess() {
                            if (isInit()) {
                                doExecute(req, callback);
                            }
                        }
                    });
                    RequestCmd.this.removePropertyChangeListener(this);
                }
            };
            this.addPropertyChangeListener(PROP_INIT, list);
        }

    }

    public boolean isRegistered(String regionId) {
        return views.containsKey(regionId);
    }

    protected void doExecute() {
        doExecute(null, new AsyncCallback<String>(){
                        public void onFailure(Throwable throwable) {}
                        public void onSuccess(String s) {}
                    });
    }

    protected void registerView(final String regionId, final Widget view) {
        views.put(regionId, view);
    }

    protected void clearView() {
        views.clear();
    }

    public boolean isTagSupported(Request req) {
        return true;
    }

//====================================================================
//  Abstract methods.  Must implement
//====================================================================
    protected abstract void doExecute(Request req, AsyncCallback<String> callback);

}