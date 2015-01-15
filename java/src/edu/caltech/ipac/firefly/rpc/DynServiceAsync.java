/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectListTag;


public interface DynServiceAsync {

    public void getAllProjects(AsyncCallback<ProjectListTag> async);

    public void getProjectConfig(String projectId, AsyncCallback<ProjectTag> async);

}

