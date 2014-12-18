package edu.caltech.ipac.visualize.draw;

import java.util.EventObject;

/**
 * The event that is passed when FixedObject data is changed
 * @author Trey Roby
 */
public class FixedObjectGroupDataEvent extends EventObject {

    private FixedObject _changeObject;
    private int         _indexOfChange;

    /**
     * Create a new FixedObjectGroupDataEvent 
     */
    public FixedObjectGroupDataEvent(FixedObjectGroup fixedGroup,
                                     FixedObject      changeObject,
                                     int              indexOfChange) {
        super(fixedGroup);
        _changeObject= changeObject;
        _indexOfChange= indexOfChange;
    }

    public FixedObjectGroup getGroup() {return (FixedObjectGroup)getSource();}
    public FixedObject      getChangedFixedObject() { return _changeObject;}
    public int              getIndexOfChange()      { return _indexOfChange;}
}



