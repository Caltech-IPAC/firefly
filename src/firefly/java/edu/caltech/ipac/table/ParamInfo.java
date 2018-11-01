/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import java.io.Serializable;

public class ParamInfo extends DataType implements Serializable, Cloneable {

    private String value;

    public ParamInfo(String name, Class clz) {
        this(name, clz, null);
    }

    public ParamInfo(String keyName, Class type, String value) {
        super(keyName, type);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
