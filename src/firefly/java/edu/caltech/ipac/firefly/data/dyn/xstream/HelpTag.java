/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import edu.caltech.ipac.util.dd.UIComponent;

import java.io.Serializable;


@XStreamAlias("Help")
public class HelpTag implements UIComponent, Serializable {

    // xml attribute 'helpId'
    @XStreamAsAttribute
    protected String helpId;

    // xml element 'Title'
    @XStreamAlias("Title")
    protected String title;


    public String getHelpId() {
        return helpId;
    }

    public void setHelpId(String value) {
        helpId = value;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

}

