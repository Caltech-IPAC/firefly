/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;


@XStreamAlias("Param")
public class ParamTag implements Serializable {

    public ParamTag() {
    }

    public ParamTag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    // xml attribute 'key'
    @XStreamAsAttribute
    protected String key;

    // xml attribute 'value'
    @XStreamAsAttribute
    protected String value;

    @XStreamAsAttribute
    private boolean serverOnly;



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

    public boolean getServerOnly() {
        return serverOnly;
    }

    public void setServerOnly(boolean serverOnly) {
        this.serverOnly = serverOnly;
    }
}

