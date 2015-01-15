/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Status;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;

import java.util.Map;
import java.util.Set;

/**
 * @author tatianag
 * @version $Id: LoginManager.java,v 1.11 2010/09/08 22:50:59 schimms Exp $
 */
public interface LoginManager {

    Region makeLoginRegion();
    UserInfo getLoginInfo();
    boolean isLoggedIn();
    LoginToolbar getToolbar();
    void refreshUserInfo(boolean inclPreferences);
    String getPreference(String prefname);
    void setPreference(String prefname, String prefvalue);
    void setPreferences(Map<String,String> prefmap, AsyncCallback<Status> callback);
    Set<String> getPrefNames();
    public void addSignInListener(SignInListener listener);
    public void removeSignInListener(SignInListener listener);


}
