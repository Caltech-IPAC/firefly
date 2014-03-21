package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
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
    private Label signing;
    private UserInfo currentUser;
    private HorizontalPanel displayP = new HorizontalPanel();
//    private Widget helpLink;


    public LoginToolbar(boolean defineFontStyle) {
       this(GwtUtil.getLinkButtonFactory(), defineFontStyle);
    }

    public LoginToolbar(LinkButtonFactory linkButtonFactory, boolean defineFontStyle) {
        // user authenticated on the server
//        final String cookieUserKey = getCookieUserKey();

        user = new Label("");
        user.setWidth("125px");
        user.setStyleName("user-name");
        GwtUtil.setStyles(user, "padding", "7px 3px", "textAlign", "right", "textOverflow", "ellipsis", "overflow", "hidden");


        linkButtonFactory.makeIntoLinkButton(user);
        user.setTitle("View/Edit user Profile");
        user.addClickHandler(new ClickHandler() {
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
        signing.setWidth("50px");
        GwtUtil.setStyles(signing, "padding", "7px 3px", "textAlign", "center");

        user.setVisible(false);
        refreshUserInfo(false);

        if (defineFontStyle) displayP.addStyleName("alternate-text");
        displayP.addStyleName("noborder");
        displayP.add(user);
        displayP.add(signing);
        displayP.setCellVerticalAlignment(user, VerticalPanel.ALIGN_MIDDLE);
        displayP.setCellVerticalAlignment(signing, VerticalPanel.ALIGN_MIDDLE);


        initWidget(displayP);
    }

    public void refreshUserInfo(boolean inclPreferences) {
        refreshUserInfo(inclPreferences, null);
    }

    public void refreshUserInfo(boolean inclPreferences, final AsyncCallback<UserInfo> callback) {
        getUserInfo(inclPreferences, new UserInfoCallback() {
            public void onSuccess(UserInfo userInfo) {
                redraw(userInfo);
                Preferences.bulkSet(userInfo.getPreferences(), true); // session scope
                if (callback != null) {
                    callback.onSuccess(userInfo);
                }
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

    private void getUserInfo(boolean inclPreference, final UserInfoCallback callback) {
        UserInfoCallback wrapper = new UserInfoCallback() {
            public void onSuccess(UserInfo result) {
                currentUser = result;
                callback.onSuccess(currentUser);
            }
        };

        UserServices.App.getInstance().getUserInfo(inclPreference, wrapper);

    }

    public void redraw(UserInfo userInfo) {
        if (userInfo.isGuestUser()) {
            user.setVisible(false);
            signing.setText("Login");
            signing.setTitle("Login to access more features");
        } else {
            user.setVisible(true);
            user.setText(userInfo.getLoginName());
            signing.setText("Logout");
            signing.setTitle("Logout");
        }
    }


    public UserInfo getCurrentUser() {
        return currentUser == null ? UserInfo.newGuestUser() : currentUser ;
    }


    abstract public static class UserInfoCallback implements AsyncCallback<UserInfo> {
        public void onFailure(Throwable caught) {
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
