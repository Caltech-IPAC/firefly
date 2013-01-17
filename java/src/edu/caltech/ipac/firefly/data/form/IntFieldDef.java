package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.util.dd.RangeFieldDef;

/**
 * @version $Id: IntFieldDef.java,v 1.5 2011/02/24 23:43:41 roby Exp $
 */
public class IntFieldDef extends RangeFieldDef {

    public static String DEFAULT_MASK = "[+-]?[0-9]+";

    private int minValue;
    private int maxValue;


    public IntFieldDef() {}

    public IntFieldDef(String name) {
        super(name);
    }

    public boolean isValidForm(Object val) {
        boolean retval;
        try {
            Integer.parseInt(val.toString());
            retval= true;
        } catch (NumberFormatException e) {
            retval= false;
        }
        return retval;
    }



    public int compareToMin(Object val) {
        return getInteger(val).compareTo(minValue);
    }

    public int compareToMax(Object val) {
        return getInteger(val).compareTo(maxValue);
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        String boundType = getMinBoundType().equals(UNDEFINED) ? INCLUSIVE : getMinBoundType();
        setMinValue(minValue,  boundType);
    }

    public void setMinValue(int minValue, String boundType) {
        this.minValue = minValue;
        setMinBoundType(boundType);
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        String boundType = getMaxBoundType().equals(UNDEFINED) ? INCLUSIVE : getMaxBoundType();
        setMaxValue(maxValue,  boundType);
    }

    public void setMaxValue(int maxValue, String boundType) {
        this.maxValue = maxValue;
        setMaxBoundType(boundType);
    }


    public String getDefaultMask() { return DEFAULT_MASK; }


    private Integer getInteger(Object val) {
        if (val instanceof Integer) {
            return (Integer) val;
        } else {
            return new Integer(val.toString());
        }
    }
}