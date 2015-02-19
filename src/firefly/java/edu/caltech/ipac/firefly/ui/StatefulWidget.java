/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.Request;

/**
 * Date: Sep 14, 2009
 *
 * @author loi
 * @version $Id: StatefulWidget.java,v 1.2 2009/09/28 23:26:36 loi Exp $
 */
public interface StatefulWidget {

    String getStateId();
    void setStateId(String iod);
    void recordCurrentState(Request request);
    void moveToRequestState(Request request, AsyncCallback callback);
    boolean isActive();
}
