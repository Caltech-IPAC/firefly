package edu.caltech.ipac.firefly.server.ws;

import java.io.IOException;

/**
 * Created by ejoliet on 6/19/17.
 */
public class WsException extends IOException {
    public WsException(String message) {
        super(message);
    }
    public WsException(String message, Throwable cause) {super(message, cause);}
}
