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
                if (checkUserInfo) {
                    sdt.setRpcRequestBuilder(new CustomBuilder());
                }
            }
        }
        return service;
    }

    protected abstract T createService();


    class CustomBuilder extends RpcRequestBuilder {
        @Override
        protected void doFinish(RequestBuilder rb) {
            super.doFinish(rb);
            String userToken = Cookies.getCookie(LoginManagerImpl.COOKIE_USER_KEY);
            userToken = userToken == null ? "" : userToken.replaceAll("\"", "");
            String user = userToken.contains("/") ? userToken.split("/", 2)[1] : null;
            LoginManager loginManager= Application.getInstance().getLoginManager();
            if (user != null && loginManager!=null && loginManager.getLoginInfo()!=null) {
                    if (!loginManager.getLoginInfo().getLoginName().equals(user)) {
                        loginManager.refreshUserInfo();
                    }
            }

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
