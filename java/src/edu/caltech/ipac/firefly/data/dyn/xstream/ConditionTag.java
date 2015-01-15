/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;


@XStreamAlias("Condition")
public class ConditionTag implements Serializable {

    // xml attribute 'fieldDefId'
    @XStreamAsAttribute
    protected String fieldDefId;

    // xml attribute 'fieldDefVisible'
    @XStreamAsAttribute
    protected String fieldDefVisible;

    // xml attribute 'fieldDefHidden'
    @XStreamAsAttribute
    protected String fieldDefHidden;

    // xml attribute 'value'
    @XStreamAsAttribute
    protected String value;


    public String getFieldDefId() {
        return fieldDefId;
    }
    public void setFieldDefId(String fieldDefId) {
        this.fieldDefId = fieldDefId;
    }


    public String getFieldDefVisible() {
        return fieldDefVisible;
    }
    public void setFieldDefVisible(String fieldDefVisible) {
        this.fieldDefVisible = fieldDefVisible;
    }


    public String getFieldDefHidden() {
        return fieldDefHidden;
    }
    public void setFieldDefHidden(String fieldDefHidden) {
        this.fieldDefHidden = fieldDefHidden;
    }

    
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

}

