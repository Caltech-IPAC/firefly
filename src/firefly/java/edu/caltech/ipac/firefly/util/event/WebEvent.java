/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util.event;

import edu.caltech.ipac.firefly.util.WebAssert;

import java.util.EventObject;

/**
 * The event that is passed when ...
 * @author Trey Roby
 */
public class WebEvent <DataType> extends EventObject {


    private final Name _name;
    private final transient DataType _data;

    /**
     * Create a client event
     * @param source source of the event, may not be null.
     * @param name the name of the event, may not be null
     * @throws IllegalArgumentException  if either source or name is null
     */
    public WebEvent(Object source, Name name) {
        this(source,name,null);
    }


    /**
     * Create a client event
     * @param source source of the event, may not be null.
     * @param name the name of the event, may not be null
     * @param data data associated with this event (if any)
     * @throws IllegalArgumentException  if either source or name is null
     */
    public WebEvent(Object source, Name name, DataType data) {
        super(source);
        WebAssert.argTst(source!=null && name!=null, "You must pass a non-null value " +
                                                  "for both source and name");
        _name= name;
        _data= data;
    }

    public Name getName() { return _name; }
    public DataType getData() { return _data; }


    public String toString() {
        return "WebEvent- "+ _name +", Source: " + getSource() + ", Data: " + _data;
    }

}
