/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XStreamAlias("ProjectList")
public class ProjectListTag implements Serializable {

    // xml element 'ProjectItem*'
    @XStreamImplicit
    protected List<ProjectItemTag> projectItemTags;


    public List<ProjectItemTag> getProjectItems() {
        if (projectItemTags == null) {
            projectItemTags = new ArrayList<ProjectItemTag>();
        }
        return this.projectItemTags;
    }

    public String getXmlForProject(String projectId) {
        for (ProjectItemTag pi : projectItemTags) {
            if (projectId.equalsIgnoreCase(pi.getId())) {
                return pi.getConfigFile();
            }
        }

        return null;
    }

    public void removeProject(String projectId) {
        for (ProjectItemTag pi : projectItemTags) {
            if (projectId.equalsIgnoreCase(pi.getId())) {
                projectItemTags.remove(pi);
                return;
            }
        }
    }

}

