/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectListTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.QueryTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.rpc.DynService;
import edu.caltech.ipac.firefly.server.dyn.DynConfigManager;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;


public class DynServiceImpl extends BaseRemoteService
        implements DynService {

    public ProjectListTag getAllProjects() {
        ProjectListTag objCache = DynConfigManager.getInstance().getCachedProjects();
        return objCache;
    }

    public ProjectTag getProjectConfig(String projectId) {
        ProjectTag obj = DynConfigManager.getInstance().getCachedProject(projectId);

        if (obj == null)
            return null;

        else {
            // prepare client version
            return prepareForClient((ProjectTag) DynServerUtils.copy(obj));
        }
    }

    public ProjectTag prepareForClient(ProjectTag p) {
        for (SearchTypeTag st : p.getSearchTypes()) {
            for (QueryTag q : st.getQueries()) {
                q.removeServerOnlyParams();
                q.clearMetadata();
            }
        }

        return p;
    }


}

