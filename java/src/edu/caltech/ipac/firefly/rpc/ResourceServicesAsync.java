package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ResourcePath;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.data.table.RawDataSet;

/**
 * Date: Nov 9, 2007
 *
 * @author loi
 * @version $Id: ResourceServicesAsync.java,v 1.13 2010/04/05 19:32:37 roby Exp $
 */
public interface ResourceServicesAsync {

    void getIpacTable(ResourcePath path, Request req, AsyncCallback<RawDataSet> async);

    void getIpacTable(String filePath, Request req, AsyncCallback<RawDataSet> async);

    void getSessionId(AsyncCallback<String> async);

    public void getVersion(String userAgentStr, AsyncCallback<Version> async);
}
