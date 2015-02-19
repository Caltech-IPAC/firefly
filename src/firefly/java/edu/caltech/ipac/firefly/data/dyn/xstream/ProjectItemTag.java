/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

import edu.caltech.ipac.firefly.data.dyn.DynUtils;

@XStreamAlias("ProjectItem")
public class ProjectItemTag implements Serializable {

    // xml attribute 'id'
    @XStreamAsAttribute
    protected String id;

    // xml attribute 'active'
    @XStreamAsAttribute
    protected String active;

    // xml attribute 'isCommand'
    @XStreamAsAttribute
    protected String isCommand;

    // xml element 'Display'
    @XStreamAlias("Display")
    protected String display;

    // xml element 'Tooltip'
    @XStreamAlias("Tooltip")
    protected String tooltip;

    // xml element 'ConfigFile'
    @XStreamAlias("ConfigFile")
    protected String configFile;


    public String getId() {
        return id;
    }
    public void setId(String value) {
        this.id = value;
    }


    public String getActive() {
        if (active == null) {
            return DynUtils.DEFAULT_PROJECT_ITEM_ACTIVE_FLAG;
        } else {
            return active;
        }
    }
    public void setActive(String value) {
        this.active = value;
    }


    public String getIsCommand() {
        if (isCommand == null) {
            return DynUtils.DEFAULT_PROJECT_ITEM_IS_COMMAND_FLAG;
        } else {
            return isCommand;
        }
    }
    public void setIsCommand(String value) {
        this.isCommand = value;
    }


    public String getDisplay() {
        return display;
    }
    public void setDisplay(String value) {
        this.display = value;
    }


    public String getTooltip() {
        return tooltip;
    }
    public void setTooltip(String value) {
        this.tooltip = value;
    }


    public String getConfigFile() {
        return configFile;
    }
    public void setConfigFile(String value) {
        this.configFile = value;
    }

}

