package edu.caltech.ipac.util;

import edu.caltech.ipac.util.dd.MappedFieldDefSource;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
/**
 * User: roby
 * Date: Feb 10, 2010
 * Time: 1:31:38 PM
 */


/**
 * @author Trey Roby
 */
public class MutableFieldDefSource extends MappedFieldDefSource implements HasPropertyChange {

    private final PropertyChangeSupport pcSupport= new PropertyChangeSupport(this);

    private MutableFieldDefSource() {}
    public MutableFieldDefSource(String fieldDefType) { super(fieldDefType); }
    public MutableFieldDefSource(MappedFieldDefSource fds) { super(fds); }
    public MutableFieldDefSource(String name, String fieldDefType) { super(name, fieldDefType); }

    public void set(String key, String value) {
        String old= get(key);
        super.set(key,value);
        if (pcSupport!=null) pcSupport.firePropertyChange(key,old,value);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcSupport.removePropertyChangeListener(listener);

    }
    public void addPropertyChangeListener( String propertyName, PropertyChangeListener listener) {
        pcSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener( String propertyName, PropertyChangeListener listener) {
        pcSupport.removePropertyChangeListener(propertyName, listener);
    }
}

