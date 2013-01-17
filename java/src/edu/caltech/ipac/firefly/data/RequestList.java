package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.PropertyChangeSupport;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A list of requests ordering like a stack.
 * The first item added appeared last on the list.
 */
public class RequestList {
    public static String ADDED = "added";
    public static String REMOVED = "removed";

    private LinkedList<Request> requests = new LinkedList<Request>();
    transient private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Add this request to the top of the list.
     * @param req
     */
    public void add(Request req) {
        if (requests.contains(req)) {
            remove(req);
        }
        requests.addFirst(req);
        pcs.firePropertyChange(ADDED, 0, req);
    }

    public void remove(Request req) {
        int index = indexOf(req);
        if (index >= 0) {
            requests.remove(req);
            pcs.firePropertyChange(REMOVED, index, req);
        }
    }

    public Request get(int index) {
        return requests.get(index);
    }

    public int indexOf(Request req) {
        return requests.indexOf(req);
    }
    
    public int size() {
        return requests.size();
    }

    public Request peek() {
        return requests.peek();
    }

    public List<Request> getRequests() {
        return Collections.unmodifiableList(requests);
    }


//====================================================================
//  PropertyChange aware
//====================================================================

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

    public void addPropertyChangeListener(String propName, PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(propName, pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

}