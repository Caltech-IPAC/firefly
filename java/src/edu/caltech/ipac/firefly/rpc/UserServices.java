package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.Alert;
import edu.caltech.ipac.firefly.data.SearchInfo;
import edu.caltech.ipac.firefly.data.Status;
import edu.caltech.ipac.firefly.data.TagInfo;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;

import java.util.List;
import java.util.Map;


/**
 * @author tatianag
 * @version $Id: UserServices.java,v 1.15 2012/05/16 01:39:05 loi Exp $
 */
public interface UserServices extends RemoteService {

    // preferences
    public Status updatePreference(String prefname, String prefvalue) throws RPCException;
    public Status updatePreferences(Map<String, String> prefmap) throws RPCException;
    public UserInfo getUserInfo(boolean includePreferences) throws RPCException;

    // TAG and Search History related methods
    public TagInfo addTag(String queryString, String desc) throws RPCException;
    public TagInfo getTag(String tagName) throws RPCException;
    public void removeTag(String tagName) throws RPCException;
    public List<TagInfo> getTags() throws RPCException;   // return all tags created by current user

    SearchInfo addSearchHistory(String queryString, String desc, boolean isFavorite) throws RPCException;
    SearchInfo getSearch(int searchId) throws RPCException;
    void removeSearch(int[] searchIds) throws RPCException;
    void updateSearchHistory(int searchId, boolean isFavorite, String desc) throws RPCException;
    List<SearchInfo> getSearchHistory() throws RPCException;   // all search history for current user.

    // Alerts
    List<Alert> getAlerts() throws RPCException;   // all search history for current user.

    /**
     * Utility/Convenience class.
     * Use UserServices.App.getInstance() to access static instance of UserServicesAsync
     */
    public static class App extends ServiceLocator<UserServicesAsync> {
        private static final App locator = new App(false);

        private App(boolean checkUser) {
            super("rpc/UserServices", checkUser);
        }

        protected UserServicesAsync createService() {
            return (UserServicesAsync) GWT.create(UserServices.class);
        }

        public static UserServicesAsync getInstance() {
            return locator.getService();
        }
        public static UserServicesAsync getInstance(boolean checkUser) {
            return new App(true).getService();
        }
    }
}
