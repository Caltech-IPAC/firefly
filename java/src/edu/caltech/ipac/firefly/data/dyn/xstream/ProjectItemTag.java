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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
