package edu.caltech.ipac.firefly.ui.input;

import edu.caltech.ipac.firefly.data.form.DateFieldDef;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.util.StringUtils;

import java.util.Date;

/**
 * DateFieldWidget is the input field for date, its value is a string,
 * representing long number of milliseconds since 1970,
 * however its representation is a regular date string in one of predefined formats
 * @author tatianag
 * $Id: DateInputField.java,v 1.4 2010/09/28 17:59:25 roby Exp $
 */
public class DateInputField extends TextBoxInputField {

    DateFieldDef _dateFieldDef;

    public DateInputField(DateFieldDef fd) {
        super(fd);
        _dateFieldDef = fd;
    }

    /**
     * Internally the value is the number of milliseconds since 1970
     * @return the number of milliseconds since 1970
     */
    @Override
    public String getValue() {
        String val = super.getValue();
        if (StringUtils.isEmpty(val)) {
            return null;
        } else {
            try {
                long internalValue= _dateFieldDef.getDate(val).getTime();
                return Long.toString(internalValue);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Set date; the date value is the number of milliseconds since 1970
     * @param val internal representation of the date (long number)
     */
    @Override
    public void setValue(String val) {
        Date dateVal;
        if (StringUtils.isEmpty(val)) {
            dateVal = null;
        } else {
            dateVal = _dateFieldDef.getDateFromLong(val);           
        }

        if (dateVal == null) {
            super.setValue(val);
        } else {
            super.setValue(_dateFieldDef.getDefaultDateTimeFormat().format(dateVal));
        }
    }

    @Override
    public boolean validate() {
        boolean retval;
        try {
            retval = _dateFieldDef.validate(super.getValue());
        } catch (ValidationException e) {
            retval= false;
        }
        return retval;
    }

}
