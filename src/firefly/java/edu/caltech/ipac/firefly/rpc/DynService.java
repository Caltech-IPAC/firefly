/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectListTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;


public interface DynService extends RemoteService {

    public ProjectListTag getAllProjects();

    public ProjectTag getProjectConfig(String projectId);

    /**
     * Utility/Convenience class.
     * Use PlotService.App.getInstance() to access static instance of PlotServiceAsync
     */
    public static class App extends ServiceLocator<DynServiceAsync> {
        private static final App locator = new App();

        private App() {
            super("rpc/DynConfigService");
        }

        protected DynServiceAsync createService() {
            return (DynServiceAsync) GWT.create(DynService.class);
        }

        public static DynServiceAsync getInstance() {
            return locator.getService();
        }
    }

}

