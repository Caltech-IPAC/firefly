/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

@XStreamAlias("SearchFormParam")
public class SearchFormParamTag implements Serializable {

    // xml attribute 'keyName'
    @XStreamAsAttribute
    protected String keyName;

    // xml attribute 'keyValue'
    @XStreamAsAttribute
    protected String keyValue;

    // xml attribute 'createParams'
    @XStreamAsAttribute
    protected String createParams;


    public String getKeyName() {
        return keyName;
    }
    public void setKeyName(String value) {
        this.keyName = value;
    }


    public String getKeyValue() {
        return keyValue;
    }
    public void setKeyValue(String value) {
        this.keyValue = value;
    }


    public String getCreateParams() {
        return createParams;
    }
    public void setCreateParams(String value) {
        this.createParams = value;
    }

}

