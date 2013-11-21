package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.LinkButtonFactory;

import java.util.logging.Level;


/**
 * Date: Apr 5, 2010
 *
 * @author loi
 * @version $Id: LoginToolbar.java,v 1.23 2012/09/13 23:04:09 loi Exp $
 */
public class LoginToolbar extends Composite {

    private Label user;
    //    private Label preferences;
    private Label signing;
    private UserInfo currentUser;
    private Label profile;
    private HorizontalPanel upperHP = new HorizontalPanel();
    private HorizontalPanel lowerHP = new HorizontalPanel();
    private Widget helpLink;


    public LoginToolbar() {
       this(GwtUtil.getLinkButtonFactory());
    }

    public LoginToolbar(LinkButtonFactory linkButtonFactory) {
        // user authenticated on the server
//        final String cookieUserKey = getCookieUserKey();

        user = new Label("Guest");
        user.setStyleName("user-name");
        user.addStyleName("title-font-family");

        profile = linkButtonFactory.makeLinkButton("Profile",
                "View/Edit user Profile",
                new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        gotoUrl(JossoUtil.makeUserProfileUrl(makeBackToUrl()));
                    }
                });

        signing = linkButtonFactory.makeLinkButton("Login",
                "Login to access more features",
                new ClickHandler() {

                    public void onClick(ClickEvent ev) {
                        if (signing.getText().equals("Login")) {
                            login();
                        } else {
                            logout();
                        }
                    }
                });

        helpLink = HelpManager.makeHelpIcon("user");

        profile.setVisible(false);
        refreshUserInfo();

        HorizontalPanel vp = new HorizontalPanel();

        upperHP.addStyleName("alternate-text");
        upperHP.addStyleName("noborder");
        lowerHP.addStyleName("noborder");
        vp.addStyleName("noborder");
//        upperHP.setSpacing(5);
//        lowerHP.setSpacing(5);

//        hp.setStyleName("user-info");
//        upperHP.add(GwtUtil.getFiller(5, 1));
        upperHP.add(user);
        upperHP.add(helpLink);
//        hp.add(user);

        lowerHP.add(profile);
        lowerHP.add(signing);

        GwtUtil.setStyle(user, "padding", "8px 0 0 7px");
        GwtUtil.setStyle(helpLink, "padding", "5px 0 0 7px");
        GwtUtil.setStyle(signing, "padding", "7px 0 3px 7px");
        GwtUtil.setStyle(profile, "padding", "7px 0 3px 7px");

        vp.add(upperHP);
        vp.add(lowerHP);

        initWidget(vp);
    }

    public void refreshUserInfo() {
        getUserInfo(new UserInfoCallback() {
            public void onSuccess(UserInfo userInfo) {
                redraw(userInfo);
                Preferences.bulkSet(userInfo.getPreferences(), true); // session scope
            }
        });
    }

    private String makeBackToUrl() {
        String qStr = Window.Location.getQueryString();
        qStr = qStr == null ? "" : qStr;

        // needed to retain GWT url data
        String urlHash = Window.Location.getHash();
        if (urlHash != null && urlHash.length() > 0) {
            qStr += urlHash + "&ts=" + System.currentTimeMillis() % 1000;
        }

        return GWT.getModuleBaseURL() + qStr;
    }

    public void login() {
        gotoUrl(JossoUtil.makeLoginUrl(makeBackToUrl()));
    }

    public void logout() {
        gotoUrl(JossoUtil.makeLogOutUrl(makeBackToUrl()));
    }

    private void gotoUrl(String url) {
        Application.getInstance().gotoUrl(url, true);
    }

    void getUserInfo(final UserInfoCallback callback) {
        UserInfoCallback wrapper = new UserInfoCallback() {
            public void onSuccess(UserInfo result) {
                currentUser = result;
                callback.onSuccess(currentUser);
            }
        };

        UserServices.App.getInstance().getUserInfo(true, wrapper);

    }

    public void redraw(UserInfo userInfo) {
        if (userInfo.isGuestUser()) {
            user.setText("Guest");
            signing.setText("Login");
            signing.setTitle("Login to access more features");
            profile.setVisible(false);
            if (lowerHP.getWidgetIndex(signing) == -1) lowerHP.remove(signing);
            upperHP.insert(signing, 1);
            if (lowerHP.getWidgetIndex(helpLink) == -1) lowerHP.remove(helpLink);
            upperHP.add(helpLink);
        } else {
            user.setText(userInfo.getLoginName());
            signing.setText("Logout");
            signing.setTitle("Logout");
            profile.setVisible(true);
            if (upperHP.getWidgetIndex(signing) == -1) upperHP.remove(signing);
            lowerHP.insert(signing, 0);
            if (upperHP.getWidgetIndex(helpLink) == -1) upperHP.remove(helpLink);
            lowerHP.add(helpLink);
        }
    }


    public UserInfo getCurrentUser() {
        return currentUser == null ? UserInfo.newGuestUser() : currentUser ;
    }


    abstract static class UserInfoCallback implements AsyncCallback<UserInfo> {
        public void onFailure(Throwable caught) {
//            PopupUtil.showError("System Error", "Unable to retrieve user's information");
            GwtUtil.getClientLogger().log(Level.SEVERE,
                                          "System Error: Unable to retrieve user's information",
                                          caught.getCause() );
        }
    }
}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
