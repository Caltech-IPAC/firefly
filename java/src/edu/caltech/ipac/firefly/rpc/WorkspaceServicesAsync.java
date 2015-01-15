/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.WspaceMeta;

/**
 * @author loi
 * @version $Id: UserServicesAsync.java,v 1.11 2011/04/30 00:01:45 loi Exp $
 */
public interface WorkspaceServicesAsync {

    void getMeta(String relPath, AsyncCallback<WspaceMeta> async);
    void getSearchMeta(AsyncCallback<WspaceMeta> async);
    void getStageMeta(AsyncCallback<WspaceMeta> async);

    void setMeta(WspaceMeta meta, AsyncCallback async);


}
