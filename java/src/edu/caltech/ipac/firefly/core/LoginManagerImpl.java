package edu.caltech.ipac.firefly.core;


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.layout.BaseRegion;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Status;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;

import java.util.*;


/**
 * @author tatianag
 * @version $Id: LoginManagerImpl.java,v 1.39 2012/04/05 23:08:24 tatianag Exp $
 */
public class LoginManagerImpl implements LoginManager {

    public static String LOGIN_INFO_REGION = "loginInfo";
    public static String COOKIE_USER_KEY = "usrkey";
//    public static int SYNC_INTERVAL_MILLISEC = 2*60*1000;  // 2 mins

    private List<SignInListener> listeners;
//    Map<String, String> userPrefs;
//    Timer syncTimer;
    private String sessionId;
    private BaseDialog warningDialog;
    private LoginToolbar toolbar;

    public LoginManagerImpl() {
        listeners = new ArrayList<SignInListener>();
//        userPrefs = new HashMap<String, String>();
    }

    public void addSignInListener(SignInListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeSignInListener(SignInListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    /**************************************************************/
    /*****            Login Manager Interface                ******/
    /**************************************************************/

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessId) {
        sessionId = sessId;
    }

    public Region makeLoginRegion() {
        toolbar = makeToolbar();
        BaseRegion r = new BaseRegion(LOGIN_INFO_REGION){
            @Override
            public Widget getDisplay() {
                GwtUtil.setStyle(toolbar, "rightPadding", "5px");
                return toolbar;
            }
        };
//        r.getDisplay().setWidth("100%");
        r.setAlign(BaseRegion.ALIGN_RIGHT);
        return r;
    }

    public UserInfo getLoginInfo() {
        return toolbar == null ? UserInfo.newGuestUser() : toolbar.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return toolbar != null && (!toolbar.getCurrentUser().isGuestUser());
    }

    public LoginToolbar getToolbar() {
        return toolbar;
    }

    public void refreshUserInfo() {
        if(toolbar != null) {
            toolbar.refreshUserInfo();
        }
    }

    public String getPreference(String prefname) {
        Map<String, String> prefs = getPreferences();
        return  (prefs != null && prefs.containsKey(prefname)) ? prefs.get(prefname) : null;
    }

    public void setPreference(String prefname, String prefvalue) {
        Map<String,String> prefs = new HashMap<String,String>(1);
        prefs.put(prefname, prefvalue);
        setPreferences(prefs, new AsyncCallback<Status>(){
                        public void onFailure(Throwable caught) {}
                        public void onSuccess(Status result) {}
                    });
    }

    public void setPreferences(Map<String, String> prefmap, AsyncCallback<Status> callback) {
        boolean needUpdate = false;
        for (String prefname : prefmap.keySet()) {
            needUpdate = needUpdate | setUserPref(prefname, prefmap.get(prefname));
        }
        if (needUpdate) {
            updatePreferences(prefmap, callback);
        }
    }

    private Map<String,String> getPreferences() {
        UserInfo uinfo = toolbar.getCurrentUser();
        if (uinfo != null) {
            return uinfo.getPreferences();
        }
        return null;
    }

    public Set<String> getPrefNames() {
        Map<String, String> prefs = getPreferences();
        return prefs == null ? null : prefs.keySet();
    }

//====================================================================
//
//====================================================================

    private void onUserChanged(UserInfo info) {
        String msg;
        if (info.isGuestUser()) {
            msg = "You've been logged out.  You are now a Guest user.";
        } else {
            msg = "You've been logged in as " + info.getLoginName() + " from another window.";
        }
        if (warningDialog != null) {
            warningDialog.setVisible(false);
        }
        warningDialog = PopupUtil.showInfo(msg);
    }

    private void updatePreferences(final Map<String, String> prefmap, final AsyncCallback<Status> callback) {

        ServerTask task = new ServerTask<Status>() {
            public void onSuccess(Status result) {
                if (result.getStatus() != 0) {
                    if (callback != null) {
                        callback.onFailure(new Exception(result.getMessage()));
                    } else {
                        PopupUtil.showInfo("Failed to update preferences <br>" + result.getMessage());
                    }
                } else {
                    callback.onSuccess(result);
                    // no need to fire events on preference update
                    //WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.PREFERENCE_UPDATE, prefmap));
                }
            }

            public void doTask(AsyncCallback<Status> passAlong) {
                UserServices.App.getInstance().updatePreferences(prefmap, passAlong);
            }
        };
        task.start();
    }

    private boolean setUserPref(String prefname, String prefvalue) {

        Map<String, String> prefs = getPreferences();
        if (prefs != null) {
            String oldvalue = prefs.get(prefname);
            if (prefvalue == null || !prefvalue.equals(oldvalue)) {
                prefs.put(prefname, prefvalue);
                return true;
            }
        }

        return false;
    }

    protected LoginToolbar makeToolbar() { return new LoginToolbar(); }

}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */