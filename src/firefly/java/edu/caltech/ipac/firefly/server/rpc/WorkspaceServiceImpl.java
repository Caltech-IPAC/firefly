/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.rpc.WorkspaceServices;


/**
 * @author loi
 * @version $Id: WorkspaceServiceImpl.java,v 1.15 2012/05/16 01:39:05 loi Exp $
 */
public class WorkspaceServiceImpl extends BaseRemoteService implements WorkspaceServices {

    public WspaceMeta getMeta(String relPath) {
        return null;
    }

    public WspaceMeta getSearchMeta() {
        return null;
    }

    public WspaceMeta getStageMeta() {
        return null;
    }

    public void setMeta(WspaceMeta meta) {
    }
}

