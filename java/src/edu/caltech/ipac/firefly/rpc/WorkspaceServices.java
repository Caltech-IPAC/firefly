package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import edu.caltech.ipac.firefly.data.WspaceMeta;


/**
 * @author loi
 * @version $Id: UserServices.java,v 1.15 2012/05/16 01:39:05 loi Exp $
 */
public interface WorkspaceServices extends RemoteService {

    WspaceMeta getMeta(String relPath);
    WspaceMeta getSearchMeta();
    WspaceMeta getStageMeta();

    void setMeta(WspaceMeta meta);

    /**
     * Utility/Convenience class. Use WorkspaceServices.App.getInstance() to access static instance of UserServicesAsync
     */
    public static class App extends ServiceLocator<WorkspaceServicesAsync> {
        private static final App locator = new App();

        private App() {
            super("rpc/WorkspaceServices", false);
        }

        protected WorkspaceServicesAsync createService() {
            return (WorkspaceServicesAsync) GWT.create(WorkspaceServices.class);
        }

        public static WorkspaceServicesAsync getInstance() {
            return locator.getService();
        }
    }
}
