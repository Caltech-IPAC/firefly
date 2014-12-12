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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
