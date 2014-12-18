package edu.caltech.ipac.firefly.util.event;

/**
 * @see WebEvent
 * @see WebEventManager
 * @author Trey Roby
 */
public class VetoWebEventException extends Exception {

    private final WebEvent _ev;

    private VetoWebEventException() { this(null,"");  }


    /**
     * Create a new VetoClientEventException.
     * @param ev the ClientEvent that cause this exception
     * @param mess the error message.
     */
    public VetoWebEventException(WebEvent ev, String mess) {
        super(mess);
        _ev= ev;
    }

    /**
     * Get the WebEvent.
     * @return WebEvent the event
     */
    public WebEvent getClientEvent()    { return _ev; }
}
