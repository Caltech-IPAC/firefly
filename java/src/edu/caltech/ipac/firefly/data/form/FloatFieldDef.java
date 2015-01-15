/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.util.ComparisonUtil;

/**
 * Field definition that allows float input
 * @version $Id: FloatFieldDef.java,v 1.5 2011/02/24 23:43:41 roby Exp $
 */
public class FloatFieldDef extends DecimalFieldDef {

    public static String DEFAULT_MASK = "[+-]?[0-9]*[.]?[0-9]+";

    private float minValue;
    private float maxValue;

    public FloatFieldDef() {}

    public FloatFieldDef(String name) {
        super(name);
    }

    public boolean isValidForm(Object val) {
        boolean retval;
        try {
            Float.parseFloat(val.toString());
            retval= true;
        } catch (NumberFormatException e) {
            retval= false;
        }
        return retval;
    }

    public int compareToMin(Object val) {
        return ComparisonUtil.doCompare(getFloat(val).floatValue(), minValue, getPrecision());
    }

    public int compareToMax(Object val) {
        return ComparisonUtil.doCompare(getFloat(val).floatValue(),maxValue,getPrecision());
    }

    public Number getMinValue() {
        return minValue;
    }

    public void setMinValue(Number minValue) {
        String boundType = getMinBoundType().equals(UNDEFINED) ? INCLUSIVE : getMinBoundType();
        setMinValue(minValue.floatValue(),  boundType);
    }

    public void setMinValue(float minValue, String boundType) {
        this.minValue = minValue;
        setMinBoundType(boundType);
    }

    public Number getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Number maxValue) {
        String boundType = getMaxBoundType().equals(UNDEFINED) ? INCLUSIVE : getMaxBoundType();
        setMaxValue(maxValue.floatValue(),  boundType);
    }

    public void setMaxValue(float maxValue, String boundType) {
        this.maxValue = maxValue;
        setMaxBoundType(boundType);
    }


    protected Float getFloat(Object val) {
        if (val instanceof Float) {
            return (Float) val;
        } else {
            return new Float(val.toString());
        }
    }
}