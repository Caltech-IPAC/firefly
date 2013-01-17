package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.util.ComparisonUtil;

/**
 * Field definition that allows float input
 * @version $Id: DoubleFieldDef.java,v 1.3 2011/02/24 23:43:41 roby Exp $
 */
public class DoubleFieldDef extends DecimalFieldDef {

    public static String DEFAULT_MASK = "[+-]?[0-9]*[.]?[0-9]+";

    private double minValue;
    private double maxValue;

    public DoubleFieldDef() {}

    public DoubleFieldDef(String name) {
        super(name);
    }

    public int compareToMin(Object val) {
        return ComparisonUtil.doCompare(getDouble(val).doubleValue(),minValue,getPrecision());
    }

    public int compareToMax(Object val) {
        return ComparisonUtil.doCompare(getDouble(val).doubleValue(),maxValue,getPrecision());
    }

    public boolean isValidForm(Object val) {
        boolean retval;
        try {
            Double.parseDouble(val.toString());
            retval= true;
        } catch (NumberFormatException e) {
            retval= false;
        }
        return retval;
    }

    public Number getMinValue() {
        return minValue;
    }

    public void setMinValue(Number minValue) {
        String boundType = getMinBoundType().equals(UNDEFINED) ? INCLUSIVE : getMinBoundType();
        setMinValue(minValue.doubleValue(),  boundType);
    }

    public void setMinValue(double minValue, String boundType) {
        this.minValue = minValue;
        setMinBoundType(boundType);
    }

    public Number getMaxValue() {
        return maxValue;
    }


    public void setMaxValue(Number maxValue) {
        String boundType = getMaxBoundType().equals(UNDEFINED) ? INCLUSIVE : getMaxBoundType();
        setMaxValue(maxValue.doubleValue(),  boundType);
    }

    public void setMaxValue(double maxValue, String boundType) {
        this.maxValue = maxValue;
        setMaxBoundType(boundType);
    }

    protected Double getDouble(Object val) {
        if (val instanceof Double) {
            return (Double) val;
        } else {
            return new Double(val.toString());
        }
    }
}