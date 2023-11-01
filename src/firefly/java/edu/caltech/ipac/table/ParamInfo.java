/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import java.io.Serializable;

public class ParamInfo extends DataType implements Serializable, Cloneable {

    private Object value;

    public ParamInfo() {
        this(null, null, null);
    }

    public ParamInfo(String keyName, Class type, Object value) {
        super(keyName, type);
        this.value = value;
    }

    /**
     * @return the String representation of the parameter in VOTable format
     */
    public String getStringValue() {
        return value == null ? null : format(value, false, false);
    }

    /**
     * @return the String representation of the parameter
     */
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
