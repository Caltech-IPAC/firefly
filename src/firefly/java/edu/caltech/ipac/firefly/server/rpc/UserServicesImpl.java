/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.Alert;
import edu.caltech.ipac.firefly.data.SearchInfo;
import edu.caltech.ipac.firefly.data.Status;
import edu.caltech.ipac.firefly.data.TagInfo;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.data.fuse.MissionInfo;
import edu.caltech.ipac.firefly.data.fuse.config.MissionTag;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.fuse.MissionConfigManager;
import edu.caltech.ipac.firefly.server.persistence.GuestHistoryCache;
import edu.caltech.ipac.firefly.server.persistence.HistoryAndTagsDao;
import edu.caltech.ipac.firefly.server.persistence.PreferencesDao;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import javax.servlet.http.HttpSession;


/**
 * Implementation of user services. All methods expect encrypted password
 *
 * @author tatianag
 * @version $Id: UserServicesImpl.java,v 1.30 2011/11/10 16:38:19 tatianag Exp $
 */
public class UserServicesImpl extends BaseRemoteService implements UserServices {

    private static final String ALERTS_DIR = AppProperties.getProperty("alerts.dir", "/hydra/alerts/");
    private static final String URL_F = "servlet/Download?" + AnyFileDownload.FILE_PARAM +
            "=%s&" + AnyFileDownload.RETURN_PARAM + "=";

    public UserInfo getUserInfo(boolean includePreferences) throws RPCException {
        RequestOwner requestOwner = ServerContext.getRequestOwner();
        StopWatch.getInstance().start("User_getUserInfo");
        UserInfo userInfo = requestOwner.getUserInfo();
        userInfo = userInfo.clone();
        if (userInfo.isGuestUser()) {
            // clear search history...  this method should only return user info and preferences.
            if (!includePreferences) {
                userInfo.getPreferences().clear();
            }
        } else {
            if (includePreferences) {
                userInfo.setPreferences((new PreferencesDao()).getPreferences(userInfo.getLoginName()));
            }
        }
        StopWatch.getInstance().printLog("User_getUserInfo");
        return userInfo;
    }

    public Status updatePreference(String prefname, String prefvalue) throws RPCException {

        StopWatch.getInstance().start("updatePreference");
        UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
        Status status;

        if (userInfo.isGuestUser()) {
            userInfo.getPreferences().put(prefname, prefvalue);
            userInfoUpdated(userInfo);
            status = new Status(0, "Ok");
        } else {
            status = (new PreferencesDao()).updatePreference(userInfo.getLoginName(), prefname, prefvalue);
        }
        StopWatch.getInstance().printLog("updatePreference");
        return status;
    }

    public Status updatePreferences(Map<String, String> prefmap) throws RPCException {
        StopWatch.getInstance().start("User_updatePreferences");
        UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
        Status status;

        if (userInfo.isGuestUser()) {
            userInfo.getPreferences().putAll(prefmap);
            userInfoUpdated(userInfo);
            status = new Status(0, "Ok");
        } else {
            status = (new PreferencesDao()).updatePreferences(userInfo.getLoginName(), prefmap);
        }

        StopWatch.getInstance().printLog("User_updatePreferences");
        return status;
    }

    /**
     * Call this method when a UserInfo has been updated. This method is reposible for refreshing the UserInfo object in
     * cache.
     *
     * @param userInfo updated info
     * @throws RPCException if something goes wrong
     */
    private void userInfoUpdated(UserInfo userInfo) throws RPCException {
        RequestOwner reqOwner = ServerContext.getRequestOwner();
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        cache.put(new StringKey(reqOwner.getUserKey()), userInfo);
    }

    /**
     * resolve the 'createdBy' value depending on the status of the current user.
     *
     * @return String which is either loginName or guest user key
     */
    public static String getCreatedBy() {
        RequestOwner requestOwner = ServerContext.getRequestOwner();
        UserInfo userInfo = requestOwner.getUserInfo();
        return userInfo.isGuestUser() ? requestOwner.getUserKey() :
                userInfo.getLoginName();
    }

    //
    // TAG and Search History related methods
    //

    public TagInfo addTag(String queryString, String desc) throws RPCException {
        try {
            StopWatch.getInstance().start("User_addTag");
            String createdBy = getCreatedBy();
            TagInfo tagInfo = new HistoryAndTagsDao().addTag(createdBy, queryString, desc);
            StopWatch.getInstance().printLog("User_addTag");
            return tagInfo;
        } catch (Exception e) {
            throw new RPCException(e, "UserServices", "addTag", "Unable to add tag for " + desc, e.getMessage());
        }
    }

    public TagInfo getTag(String tagName) throws RPCException {
        try {
            StopWatch.getInstance().start("User_getTag");
            TagInfo tagInfo = new HistoryAndTagsDao().getTag(tagName);
            StopWatch.getInstance().printLog("User_getTag");
            return tagInfo;
        } catch (Exception e) {
            throw new RPCException(e, "UserServices", "getTag", "Unable to get tag " + tagName, e.getMessage());
        }
    }

    public void removeTag(String tagName) throws RPCException {
        try {
            StopWatch.getInstance().start("User_removeTag");
            String createdBy = getCreatedBy();
            new HistoryAndTagsDao().removeTag(createdBy, tagName);
            StopWatch.getInstance().printLog("User_removeTag");
        } catch (Exception e) {
            throw new RPCException(e, "UserServices", "removeTag", "Unable to remove tag " + tagName, e.getMessage());
        }
    }

    public List<TagInfo> getTags() throws RPCException {
        String createdBy = getCreatedBy();
        try {
            StopWatch.getInstance().start("User_getTags");
            List<TagInfo> tagInfo = new HistoryAndTagsDao().getTags(createdBy);
            StopWatch.getInstance().printLog("User_getTags");
            return tagInfo;
        } catch (Exception e) {
            throw new RPCException(e, "UserServices", "getTags", "Unable to get tags for user " + createdBy, e.getMessage());
        }
    }

    public SearchInfo addSearchHistory(String queryString, String desc, boolean isFavorite) throws RPCException {
        try {
            StopWatch.getInstance().start("User_addSearchHistory");
            UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
            SearchInfo searchInfo;
            String userKey = getCreatedBy();
            if (userInfo.isGuestUser()) {
                searchInfo = GuestHistoryCache.addSearchHistory(
                        userKey, queryString, desc, isFavorite);
            } else {
                searchInfo = new HistoryAndTagsDao().addSearchHistory(userKey, queryString, desc, isFavorite);
            }
            StopWatch.getInstance().printLog("User_addSearchHistory");
            return searchInfo;
        } catch (Exception e) {
            throw new RPCException(e, "UserServices", "addSearchHistory", "Unable to add to search history query " + desc, e.getMessage());
        }
    }

    public SearchInfo getSearch(int searchId) throws RPCException {
        try {
            StopWatch.getInstance().start("User_getSearch");
            SearchInfo searchInfo;
            UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
            if (userInfo.isGuestUser()) {
                searchInfo = GuestHistoryCache.getSearch(searchId);
            } else {
                searchInfo = new HistoryAndTagsDao().getSearch(searchId);
            }
            StopWatch.getInstance().printLog("User_getSearch");
            return searchInfo;
        } catch (Exception e) {
            throw new RPCException(e, "UserServices", "getSearch", "Unable to get " + searchId + " from the search history", e.getMessage());
        }
    }

    public void removeSearch(int[] searchIds) throws RPCException {
        try {
            StopWatch.getInstance().start("User_removeSearch");
            UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
            String userKey = getCreatedBy();
            if (userInfo.isGuestUser()) {
                GuestHistoryCache.removeSearch(userKey, searchIds);
            } else {
                new HistoryAndTagsDao().removeSearch(userKey, searchIds);
            }
            StopWatch.getInstance().printLog("User_removeSearch");
        } catch (Exception e) {
            throw new RPCException(e, "UserServices", "removeSearch", "Unable to remove search history records", e.getMessage());
        }
    }

    public void updateSearchHistory(int searchId, boolean isFavorite, String desc) throws RPCException {
        try {
            StopWatch.getInstance().start("User_setSearchFavorite");
            UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
            String userKey = getCreatedBy();
            if (userInfo.isGuestUser()) {
                GuestHistoryCache.updateSearch(userKey, searchId, isFavorite, desc);
            } else {
                new HistoryAndTagsDao().updateSearch(searchId, isFavorite, desc);
            }
            StopWatch.getInstance().printLog("User_setSearchFavorite");
        } catch (Exception e) {
            throw new RPCException(e, "UserServices", "setSearchFavorite", "Unable to set favorite id=" + searchId + ", isFavorite=" + isFavorite, e.getMessage());
        }
    }

    public List<SearchInfo> getSearchHistory() throws RPCException {
        try {
            StopWatch.getInstance().start("User_getSearchHistory");
            UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
            List<SearchInfo> searchInfo;
            String userKey = getCreatedBy();
            if (userInfo.isGuestUser()) {
                searchInfo = GuestHistoryCache.getSearchHistory(userKey);
            } else {
                searchInfo = new HistoryAndTagsDao().getSearchHistory(userKey);
            }
            StopWatch.getInstance().printLog("User_getSearchHistory");
            return searchInfo;
        } catch (Exception e) {
            throw new RPCException(e, "UserServices", "addSearchHistory", "Unable to get search history", e.getMessage());
        }
    }

    public List<Alert> getAlerts() throws RPCException {
        StringKey key = new StringKey("cached.system.alerts");
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        Map<String, Alert> alerts = (Map<String, Alert>) cache.get(key);
        alerts = alerts == null ? new HashMap<String, Alert>() : alerts;
        if (checkAlerts(alerts)) {
            cache.put(key, alerts);
        }

        StringKey ukey = new StringKey(ServerContext.getRequestOwner().getUserKey(), "alerts");
        Map<String, Boolean> userAlerts = (Map<String, Boolean>) cache.get(ukey);
        if (userAlerts == null) {
            userAlerts = new HashMap<String, Boolean>();
        }

        // now update the isNew flag for this user.
        boolean doUpdate = false;
        for (Alert a : alerts.values()) {
            String akey = a.getUrl() + "|" + a.getLastModDate();
            if (userAlerts.containsKey(akey)) {
                a.setNew(false);
            } else {
                a.setNew(true);
                userAlerts.put(akey, true);
                doUpdate = true;
            }
        }
        if (doUpdate) {
            cache.put(ukey, userAlerts, 7 * 24 * 60 * 60);       // last for one week
        }

        return new ArrayList<Alert>(alerts.values());
    }

    public MissionTag getMissionConfig(String dsName) {
        return MissionConfigManager.getInstance().getMissionConfig().getMission(dsName);
    }

    public List<MissionInfo> getAllMissionInfo() {
        return MissionConfigManager.getInstance().getMissionInfos();
    }

    public WspaceMeta getMeta(String relPath, WspaceMeta.Includes includes) {
        return ServerContext.getRequestOwner().getWsManager().getMeta(relPath, includes);
    }

    public void setMeta(WspaceMeta meta) {
        ServerContext.getRequestOwner().getWsManager().setMeta(meta);
    }

    private String getKey(File f) {
        return f == null ? "" : f.getName() + "|" + f.lastModified();
    }

    private boolean exists(File[] files, String key) {
        for (File f : files) {
            if (getKey(f).equals(key)) {
                return true;
            }
        }
        return false;
    }

    /*
        alerts key is File.getName() + "|" + File.lastModified()
     */
    private boolean checkAlerts(Map<String, Alert> alerts) {

        boolean needUpdate = false;

        File alertDir = new File(ALERTS_DIR);
        File[] files = alertDir.listFiles(new FileFilter(){
                        public boolean accept(File file) {
                            return file.isFile() && !file.isHidden();
                        }
                    });
        if (files == null || files.length == 0) {
            if (alerts != null && alerts.size() > 0) {
                alerts.clear();
                return true;
            } else {
                return false;
            }
        }

        // remove cached alerts
        for (String key : new ArrayList<String>(alerts.keySet())) {
            if (!exists(files, key)) {
                alerts.remove(key);
            }
            needUpdate = true;
        }

        for (File f : files) {
            String key = getKey(f);
            Alert alert = alerts.get(key);
            if (alert == null) {
                String title = null;
                try {

                    title = FileUtil.readFile(f);
                } catch (IOException e) {}

                String fStr = ServerContext.replaceWithPrefix(f);
                alert = new Alert(String.format(URL_F, fStr), title, true);
                alert.setLastModDate(f.lastModified());
                alerts.put(getKey(f), alert);
                needUpdate = true;
            }
        }
        return needUpdate;
    }

}
