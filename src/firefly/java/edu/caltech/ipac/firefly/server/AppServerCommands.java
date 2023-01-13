/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.Alert;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.security.SsoAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;

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
            LOG.info("Init "+spaName);
            LOG.debug("User info for this client: " + userInfo);
            return "true";
        }
    }

    public static class JsonProperty extends ServCommand {
        static final String INVENTORY_PROP = "inventory.serverURLAry";
        static final String FIREFLY_OPTIONS = "FIREFLY_OPTIONS";
        static final Map<String, String> map = new HashMap<>();
        static final List<String> validatedList = new ArrayList<>();

        static {
            map.put(FIREFLY_OPTIONS, getAppOptions());
            map.put(INVENTORY_PROP, AppProperties.getProperty(INVENTORY_PROP, "[]"));
            validateAll();
        }

        private static void validateAll() {
            List<String> msgList = new ArrayList<>();
            msgList.add("-- JSON properties --");
            for (Map.Entry<String, String> entry : map.entrySet()) {
                var json = entry.getValue();
                var prop = entry.getKey();
                try {
                    new JSONParser().parse(json);
                    validatedList.add(prop);
                    msgList.add(prop + " parsed: " + json);
                } catch (ParseException e) {
                    msgList.add(prop + ": Could not parse Json: " + json);
                }
            }
            LOG.info(msgList.toArray(new String[0]));
        }

        public String doCommand(SrvParam sp) throws Exception {
            var propName = sp.getRequired(ServerParams.PROP);
            if (validatedList.contains(propName)) return map.get(propName);
            if (!map.containsKey(propName)) LOG.error("no json property exposed for " + propName);
            return "{}";
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


    private static final String HELP_BASE_URL = "help.base.url";
    private static String getAppOptions() {
        Map appOpts;
        String def = "{}";
        String appOptStr = AppProperties.getProperty(JsonProperty.FIREFLY_OPTIONS, def);
        try{
            appOpts = (Map) new JSONParser().parse(appOptStr);
            // additional props as FIREFLY_OPTIONS
            applyIfNotEmpty(AppProperties.getProperty(HELP_BASE_URL), v -> appOpts.put(HELP_BASE_URL, v));

            return new JSONObject(appOpts).toJSONString();
        }catch(ParseException pe){
            Logger.getLogger().error(String.format("Failed parsing %s", appOptStr));
            return def;
        }
    }


}
