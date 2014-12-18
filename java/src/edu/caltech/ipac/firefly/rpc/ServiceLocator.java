package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.LoginManagerImpl;
import edu.caltech.ipac.firefly.util.WebUtil;

/**
 * Date: Mar 11, 2009
 *
 * @author loi
 * @version $Id: ServiceLocator.java,v 1.7 2012/03/12 18:04:40 roby Exp $
 */
public abstract class ServiceLocator <T> {

    private T service;
    private String location;
    boolean checkUserInfo = true;

    protected ServiceLocator(String location) {
        this(location, true);
    }

    protected ServiceLocator(String location, boolean checkUserInfo) {
        this.location = location;
        this.checkUserInfo = checkUserInfo;
    }

    public synchronized T getService() {
        if (service == null) {
            service = createService();
            if (service instanceof ServiceDefTarget) {
                ServiceDefTarget sdt = (ServiceDefTarget) service;
                sdt.setServiceEntryPoint(WebUtil.encodeUrl(GWT.getModuleBaseURL() + location));
                sdt.setRpcRequestBuilder(new CustomBuilder());
            }
        }
        return service;
    }

    protected abstract T createService();


    class CustomBuilder extends RpcRequestBuilder {

        @Override
        protected RequestBuilder doCreate(String serviceEntryPoint) {
            RequestBuilder rb = super.doCreate(serviceEntryPoint);
            rb.setIncludeCredentials(true);
            return rb;
        }

        @Override
        protected void doFinish(RequestBuilder rb) {
            super.doFinish(rb);

            if (checkUserInfo) {
                String userToken = Cookies.getCookie(LoginManagerImpl.COOKIE_USER_KEY);
                userToken = userToken == null ? "" : userToken.replaceAll("\"", "");
                String user = userToken.contains("/") ? userToken.split("/", 2)[1] : null;
                LoginManager loginManager= Application.getInstance().getLoginManager();
                if (user != null && loginManager!=null && loginManager.getLoginInfo()!=null) {
                    if (!loginManager.getLoginInfo().getLoginName().equals(user)) {
                        loginManager.refreshUserInfo(true);
                    }
                }
            }
        }

    }
}
