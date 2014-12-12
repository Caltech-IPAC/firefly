package edu.caltech.ipac.util.dd;

import java.lang.Exception;

public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationException() {
    }
}