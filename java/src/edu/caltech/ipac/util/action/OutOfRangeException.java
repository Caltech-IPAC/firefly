/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.action;

/**
 * This exception is thrown when a value is out of range.
 * @see FloatAction
 * @see IntAction
 * @see edu.caltech.ipac.gui.ValidationTextField
 * @see edu.caltech.ipac.gui.FloatTextField
 * @see edu.caltech.ipac.gui.IntTextField
 * @author Trey Roby
 */
public class OutOfRangeException extends Exception {

    private boolean _warningOnly;
    private String  _defaultValue= null;

    /**
     * Create a new OutOfRange Exception.
     * @param mess the error message.
     */
    public OutOfRangeException(String mess, String defaultValue) {
        this(mess,false,defaultValue);
    }

    /**
     * Create a new OutOfRange Exception.
     * @param mess the error message.
     */
    public OutOfRangeException(String mess) {
        this(mess,false,null);
    }

    /**
     * Create a new OutOfRange Exception.
     * @param mess the error message.
     * @param warningOnly this exception is only a warning this data is on
     *                really out of range but in some zone of concern.
     * @param defaultValue what the default value should be
     */
    public OutOfRangeException(String  mess, 
                               boolean warningOnly,
                               String  defaultValue) {
        super(mess);
        _warningOnly = warningOnly;
        _defaultValue= defaultValue;
    }

    // Xiuqin added this for IrsPeakupValidator 8/16/2004
    public OutOfRangeException(String  mess, 
                               boolean warningOnly) {
        super(mess);
        _warningOnly = warningOnly;
    }

    public boolean isWarningOnly() { return _warningOnly; }

    public String getDefaultValue() { return _defaultValue; }
}

