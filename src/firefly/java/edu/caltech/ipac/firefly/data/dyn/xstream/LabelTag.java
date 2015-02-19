/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.caltech.ipac.util.dd.UIComponent;

import java.io.Serializable;


@XStreamAlias("Label")
public class LabelTag implements UIComponent, Serializable {

    protected String htmlString;

    public LabelTag() {
        htmlString = "";
    }
    public LabelTag(String str) {
        htmlString = str;
    }

    public String getHtmlString() {
        return htmlString;
    }

    public void setHtmlString(String value) {
        htmlString = value;
    }

}

