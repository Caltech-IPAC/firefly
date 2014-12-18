package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;


@XStreamAlias("ConstrainedParam")
public class ConstrainedParamTag implements Serializable {

    // xml attribute 'key'
    @XStreamAsAttribute
    protected String key;

    // xml attribute 'value'
    @XStreamAsAttribute
    protected String value;

    // xml element 'Constraints*'
    @XStreamAlias("Constraints")
    protected ConstraintsTag constraintsTag;


    public String getKey() {
        return key;
    }
    public void setKey(String value) {
        this.key = value;
    }


    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }


    public ConstraintsTag getConstraints() {
        return this.constraintsTag;
    }
    
}

