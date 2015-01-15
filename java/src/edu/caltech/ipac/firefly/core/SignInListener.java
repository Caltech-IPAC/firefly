/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;

/**
 * @author tatianag
 * @version $Id: SignInListener.java,v 1.1 2009/02/04 22:56:15 tatianag Exp $
 */
public interface SignInListener {

    public void signedIn(UserInfo userInfo);
    public void signedOut(UserInfo userInfo);
}
