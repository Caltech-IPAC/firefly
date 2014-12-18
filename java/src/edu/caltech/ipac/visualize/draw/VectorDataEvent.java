package edu.caltech.ipac.visualize.draw;

import java.util.EventObject;

/**
 * The event that is passed when vector data is changed
 * @author Trey Roby
 */
public class VectorDataEvent extends EventObject {

    /**
     * Create a new VectorDataEvent
     * @param v source of the event.
     */
    public VectorDataEvent(VectorObject v) {
        super(v);
    }

    public VectorObject getVectorObject() {return (VectorObject)getSource();}
}



