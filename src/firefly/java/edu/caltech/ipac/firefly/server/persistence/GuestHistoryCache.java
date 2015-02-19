/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.SearchInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author loi
 * $Id: GuestHistoryCache.java,v 1.3 2011/10/21 18:54:37 tlau Exp $
 */
public class GuestHistoryCache {
    private static final int LIFE_TO_LIVE = 60*60*24*14;    // 14 days.


    public static SearchInfo addSearchHistory(String userKey, final String queryString, final String desc, final boolean isFavorite) {

        List<SearchInfo> searchHistory = getSearchHistory(userKey);
        int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        SearchInfo si = new SearchInfo(id, "Guest", queryString, desc, isFavorite, new Date());
        searchHistory.add(si);
        updateSearchHistory(userKey, searchHistory);
        return si;
    }

    public static SearchInfo getSearch(int searchId) {
        List<SearchInfo> sh = getSearchHistory();
        for (SearchInfo si : sh) {
            if (si.getQueryID() == searchId) {
                return si;
            }
        }
        return null;
    }

    public static void removeSearch(final String userKey, final int... searchIds) {
        boolean needUpdate = false;
        List<SearchInfo> sh = getSearchHistory(userKey);
        ArrayList<SearchInfo> newSh = new ArrayList<SearchInfo>(sh);
        for(int id : searchIds) {
            for (int i=0; i< newSh.size(); i++) {
                if (newSh.get(i).getQueryID() == id) {
                    newSh.remove(i);
                    needUpdate = true;
                    break;
                }
            }
        }
        if (needUpdate) {
            updateSearchHistory(userKey, newSh);
        }
    }

    public static void updateSearch(final String userKey,int searchId, boolean isFavorite, String desc) {
        //SearchInfo sh = getSearch(searchId);
        List<SearchInfo> shh = getSearchHistory(userKey);
        boolean needUpdate = false;

        for (int i=0; i< shh.size(); i++) {
            if (shh.get(i).getQueryID() == searchId) {
                shh.get(i).setFavorite(isFavorite);
                shh.get(i).setDescription(desc);
                needUpdate = true;
                break;
            }
        }

        if (needUpdate) {
            updateSearchHistory(userKey, shh);
        }
/*        if (sh != null) {
            sh.setFavorite(isFavorite);
            sh.setDescription(desc);
            updateSearchHistory(getSearchHistory());
        }*/
    }

    public static List<SearchInfo> getSearchHistory(String userKey) {
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        List<SearchInfo> searchHistory = (List<SearchInfo>) cache.get(new StringKey("GuestHistoryCache", userKey));
        return searchHistory == null ? new ArrayList<SearchInfo>() : searchHistory;
    }

    public static List<SearchInfo> getSearchHistory() {
        String userKey = ServerContext.getRequestOwner().getUserInfo().getLoginName();
        return getSearchHistory(userKey);
    }

    static void updateSearchHistory(String userKey, List<SearchInfo> searchHistory) {
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        cache.put(new StringKey("GuestHistoryCache", userKey), searchHistory, LIFE_TO_LIVE);
    }

    static void updateSearchHistory(List<SearchInfo> searchHistory) {
        String userKey = ServerContext.getRequestOwner().getUserInfo().getLoginName();
        updateSearchHistory(userKey, searchHistory);
    }

}