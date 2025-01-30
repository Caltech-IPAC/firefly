/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.Alert;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.messaging.JsonHelper;
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
import static edu.caltech.ipac.util.StringUtils.getDouble;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

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


//    private static final String STRING_START="::STRING::";
//    private static final int SS_OFFSET= STRING_START.length();
    private static final String HELP_BASE_URL = "help.base.url";
    private static final String OP_ROOT = "OP_";
    private static final String EMPTY_APP_PROP = "{}";
    private static final int OP_ROOT_LENGTH = OP_ROOT.length();
    private static String getAppOptions() {
        String appOptStr= EMPTY_APP_PROP;
        try {
            appOptStr = AppProperties.getProperty(JsonProperty.FIREFLY_OPTIONS, EMPTY_APP_PROP);
            if (isEmpty(appOptStr)) appOptStr = EMPTY_APP_PROP; // this check to see if the property returns ''
            var extraProps = AppProperties.getWithStartingMatch(OP_ROOT);
            var jsonData = JsonHelper.parse(appOptStr);
            try{
                extraProps.forEach((k, v) -> setToJson(jsonData, k, v));
                applyIfNotEmpty(AppProperties.getProperty(HELP_BASE_URL), v -> jsonData.setValue(v,HELP_BASE_URL));
            }
            catch (Exception e){
                Logger.getLogger().error(String.format("Could not add props to %s", appOptStr));
                extraProps.forEach((k, v) -> Logger.getLogger().error("Could not add- "+ k+ ": "+v));
            }
            return jsonData.toJson();
        } catch (Exception e) {
            Logger.getLogger().error(String.format("Failed parsing %s", appOptStr));
            return EMPTY_APP_PROP;
        }


    }



    private static void setToJson(JsonHelper jhelp, String key, String valStr) {
        if (key.length()<=OP_ROOT_LENGTH) return;
        Object val;
//        if (valStr.startsWith(STRING_START)) {
//            val= valStr.substring(SS_OFFSET).replaceAll("_", " ");
//        }
//        else {
            double num= getDouble(valStr);
            val= Double.isNaN(num) ? valStr : num;
//        }
        jhelp.setValueFromPath(val,key.substring(OP_ROOT_LENGTH),"_");
    }
}

