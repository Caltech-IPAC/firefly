/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.packagedata.BackgroundInfoCacher;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.server.security.SsoAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;

public class AppServerCommands {
    private static final String SPA_NAME = "spaName";
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    public static class InitApp extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            String spaName = params.getRequired(SPA_NAME);

            // check for alerts
            AlertsMonitor.checkAlerts(true);
            // check for background jobs
            BackgroundEnv.getUserBackgroundInfo().stream()
                    .forEach(BackgroundInfoCacher::fireBackgroundJobAdd);
            // update login status
            UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
            LOG.debug("User info for this client: " + userInfo);
            return "true";
        }
    }

    public static class Logout extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            // should handle logout request.
            String authToken = SsoAdapter.getAdapter().getAuthTokenId();
            if (authToken != null) {
                SsoAdapter.getAdapter().logout(authToken);
            }
            // update login status
            UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
            LOG.debug("User logged out: " + userInfo);
            return "true";
        }
    }
}

