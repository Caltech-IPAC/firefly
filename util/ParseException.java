package edu.caltech.ipac.util;

/**
* Object used to signal errors in the parsing procedure
* parameters indicate the type of error that was encountered and
* the line number.
*/

public class ParseException extends Exception {
    private int _lineNumber = 1;
    
    public ParseException (String    message,
                           int       initLineNumber,
                           Exception cause){
        super(message,cause);
        _lineNumber = initLineNumber;
    }

    public ParseException (String    message,
                           Exception cause){
        this(message,0, cause);
    }

    public ParseException (String message){
        this(message, 0, null);
    }

    public void setErrorLineNumber (int errorLineNumber){
        _lineNumber = errorLineNumber;
    }
    public int getErrorLineNumber (){
        return _lineNumber;
    }
}
