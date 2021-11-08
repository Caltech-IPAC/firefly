/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.Alert;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.security.SsoAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.json.simple.JSONObject;

public class AppServerCommands {
    private static final String SPA_NAME = "spaName";
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    public static class InitApp extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            String spaName = params.getRequired(SPA_NAME);

            // check for alerts
            AlertsMonitor.checkAlerts(true);
            // update login status
            UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
            LOG.briefInfo("Init "+spaName);
            LOG.debug("User info for this client: " + userInfo);
            return "true";
        }
    }

    public static class Logout extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            // should handle logout request.
            SsoAdapter ssoAdapter = ServerContext.getRequestOwner().getSsoAdapter();
            if (ssoAdapter != null) {
                ssoAdapter.logout();
            }
            // update login status
            UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
            LOG.debug("User logged out: " + userInfo);
            return "true";
        }
    }



    public static class GetAlerts extends ServCommand {
        public String doCommand(SrvParam params) throws Exception {
            Alert alert= AlertsMonitor.checkAlerts(false);
            JSONObject data = new JSONObject();
            data.put("isNew", alert.isNew());
            data.put("url", alert.getUrl());
            data.put("msg", alert.getMsg());
            data.put("lastModData", alert.getLastModDate());
            JSONObject map = new JSONObject();
            map.put( "success", true);
            map.put("data", data);
            return map.toString();
        }
    }

    public static class GetUserInfo extends ServCommand {
        public String doCommand(SrvParam params) throws Exception {
            RequestOwner requestOwner = ServerContext.getRequestOwner();

            String backToUrl = params.getRequired(ServerParams.BACK_TO_URL);

            SsoAdapter ssoAdapter = ServerContext.getRequestOwner().getSsoAdapter();
            UserInfo info = requestOwner.getUserInfo();

            JSONObject data = new JSONObject();
            data.put(UserInfo.GUEST, info.isGuestUser());
            if (!info.isGuestUser()) {
                data.put(UserInfo.LASTNAME, info.getLastName());
                data.put(UserInfo.FIRSTNAME, info.getFirstName());
                data.put("loginName", info.getLoginName());
            }
            data.put("login_url", ssoAdapter.getLoginUrl(backToUrl));
            data.put("logout_url", ssoAdapter.getLogoutUrl(backToUrl));
            data.put("profile_url", ssoAdapter.getProfileUrl(backToUrl));
            JSONObject map = new JSONObject();
            map.put( "success", true);
            map.put("data", data);

            return map.toString();
        }
    }
}
