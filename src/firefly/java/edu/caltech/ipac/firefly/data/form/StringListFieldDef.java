/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.util.StringUtils;

/**
 * @version $Id: StringListFieldDef.java,v 1.1 2011/03/23 20:30:32 loi Exp $
 */
public class StringListFieldDef extends IntFieldDef {

    public StringListFieldDef() {}

    public StringListFieldDef(String name) {
        super(name);
    }

    public boolean isValidForm(Object val) {
        return true;
    }

    public int compareToMin(Object val) {
        return size(val).compareTo(getMinValue());
    }

    public int compareToMax(Object val) {
        return size(val).compareTo(getMaxValue());
    }

    public String getDefaultMask() { return null; }

    protected String[] getValues(Object val) {
        if (StringUtils.isEmpty(val)) return new String[0];
        String v = String.valueOf(val);
        return v.split(",");
    }

    protected Integer size(Object val) {
        return getValues(val).length;
    }
}