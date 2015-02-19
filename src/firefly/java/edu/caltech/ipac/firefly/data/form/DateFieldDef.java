/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.form;


import com.google.gwt.i18n.client.DateTimeFormat;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.RangeFieldDef;

import java.util.Date;

/**
 * This class may be used to define a date field.
 * It may be constrained to take only a set of date formats via a setFormat()
 * or through a constructor.
 * In which case, certain methods are only accessible from GWT client code.
 * Using it outside of GWT will cause a ExceptionInInitializerError.
 * These methods include:
 * <ul>
 *      <li> #isValidForm()
 *      <li> #getMaxValue()
 *      <li> #getMinValue()
 *      <li> #compareToMin()
 *      <li> #compareToMax()
 *      <li> #getDefaultDateTimeFormat()
 *      <li> #getDate()
 * </ul>
 *
 */

public class DateFieldDef extends RangeFieldDef {

	private Date maxValue;
	private Date minValue;
    private String maxString;
    private String minString;
    private String [] formats;

    /**
     *
     * @param format    a string of formats separtated by ";".
     * @param min  min date as a string
     * @param max  max date as a string
     */
    public DateFieldDef(String format, String min, String max) {
        this(format == null ? null : format.split(";"), min, max);
    }

    public DateFieldDef(String[] format, String min, String max) {
        formats = format;
        minString = min;
        maxString = max;
        maxValue = null;
        minValue = null;
    }

    public boolean isValidForm(Object val) {
        boolean retval= true;
        try {
            getDate(val);
        } catch (Exception e) {
            retval= false;
        }
        return retval;
    }



    public int compareToMin(Object val) {
        return getDate(val).compareTo(getMinValue());
    }

    public int compareToMax(Object val) {
        return getDate(val).compareTo(getMaxValue());
    }

    public Date getMaxValue() {
        if (maxValue == null && maxString != null) {
            maxValue = getDate(maxString);
            maxString = null;
        }
        return maxValue;
    }

    public void setMaxValue(Date maxValue) {
        String boundType = getMaxBoundType().equals(UNDEFINED) ? INCLUSIVE : getMaxBoundType();
        setMaxValue(maxValue,  boundType);
    }
    public void setMaxValue(Date maxValue, String boundType) {
        this.maxValue = maxValue;
        this.maxString = null;
        setMaxBoundType(boundType);
    }

    public Date getMinValue() {
        if (minValue == null && minString != null) {
            minValue = getDate(minString);
            minString = null;
        }
        return minValue;
    }

    public void setMinValue(Date minValue) {
        String boundType = getMinBoundType().equals(UNDEFINED) ? INCLUSIVE : getMinBoundType();
        setMinValue(minValue,  boundType);
    }

    public void setMinValue(Date minValue, String boundType) {
        this.minValue = minValue;
        this.minString = null;
        setMinBoundType(boundType);
    }


    public void setFormat(String [] formats) {
        this.formats = formats;
    }

    public DateTimeFormat getDefaultDateTimeFormat() {
        if (formats != null && formats.length > 0) {
            return DateTimeFormat.getFormat(formats[0]);
        } else {
            return DateTimeFormat.getMediumDateTimeFormat();
        }
    }

    public Date getDate(Object val) {
        if (StringUtils.isEmpty(val)) return null;

        Date parsed = null;
        if (val instanceof Date) {
            return (Date) val;
        } else {
            DateTimeFormat dtFormat;
            if (formats != null) {
                for (String f : formats) {
                    dtFormat = DateTimeFormat.getFormat(f);
                    try {
                        parsed = dtFormat.parseStrict(val.toString().trim());
                        return parsed;
                    } catch (Exception e) {
                    }
                }
            }
            if (parsed == null) {
                throw new IllegalArgumentException("Unable to parse date from "+val.toString());
            } else {
                return parsed;
            }
        }
    }

    public Date getDateFromLong(String val) {
        Date dateVal;
        if (StringUtils.isEmpty(val)) {
            dateVal = null;
        } else {
            try {
                long internalValue = Long.parseLong(val);
                dateVal = new Date(internalValue);
            } catch (Exception e) {
                dateVal = null;
            }
        }
        return dateVal;
    }

    public String getDefaultMask() { return null; }


    public static void main(String... args) {
        DateFieldDef dfd = new DateFieldDef("dd/mm/yyyy;m/d/yy", "01/01/2001", "01/01/2010");
        System.out.println("creating the DateFieldDef should be okay. ==>" + dfd.toString());

        try {
            System.out.println("calling getDate() should fail.");
            dfd.getDate("01/01/2005");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}