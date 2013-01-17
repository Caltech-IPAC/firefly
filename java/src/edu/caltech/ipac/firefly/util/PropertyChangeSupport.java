package edu.caltech.ipac.firefly.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class PropertyChangeSupport {

    private Object source;


    /** list of PropertyChangeListener register to all properties.
     *  lazy-loading to save memory.  use getter to access variable
     */
    private ArrayList<PropertyChangeListener> listeners;

    /** a map of a list of PropertyChangeListener register to a property (key of map)
     *  lazy-loading to save memory.  use getter to access variable
     */
    private HashMap<String, List<PropertyChangeListener>> namedListeners;


    public PropertyChangeSupport(Object source) {
        this.source = source;
    }

    public List<PropertyChangeListener> getListeners() {
        if (listeners == null) {
            listeners = new ArrayList<PropertyChangeListener>(2);
        }
        

        return listeners;
    }

    public List<PropertyChangeListener> getListeners(String propName) {
        List<PropertyChangeListener> l = getNamedListeners().get(propName);
        if (l == null || l.size() == 0) {
            l = new ArrayList<PropertyChangeListener>(2);
            getNamedListeners().put(propName, l);
        }
        return l;
    }

    Map<String, List<PropertyChangeListener>> getNamedListeners() {
        if (namedListeners == null) {
            namedListeners = new HashMap<String, List<PropertyChangeListener>>(2);
        }
        return namedListeners;
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        getListeners().add(pcl);
	}

	public void addPropertyChangeListener(String propName, PropertyChangeListener pcl) {
        getListeners(propName).add(pcl);
	}

	public void firePropertyChange(PropertyChangeEvent pce) {

        if (listeners != null && getListeners().size() > 0) {
            List<PropertyChangeListener> l = new ArrayList<PropertyChangeListener>(getListeners());
            for(int i = 0; i < l.size(); i++) {
                PropertyChangeListener pcl = l.get(i);
                pcl.propertyChange(pce);
            }
        }
	}

    public void firePropertyChange(String propName, PropertyChangeEvent pce) {
        if (namedListeners != null && getNamedListeners().containsKey(propName)) {
            List<PropertyChangeListener> l = new ArrayList<PropertyChangeListener>(getListeners(propName));
            for(int i = 0; i < l.size(); i++) {
                PropertyChangeListener pcl = l.get(i);
                pcl.propertyChange(pce);
            }
        }
        firePropertyChange(pce);
    }

    public void firePropertyChange(String propName, Object oldVal, Object newVal) {
        firePropertyChange(propName, new PropertyChangeEvent(source, propName, oldVal, newVal));
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
        getListeners().remove(pcl);
	}

    public void removePropertyChangeListener(String propName) {
        getNamedListeners().remove(propName);
    }
}