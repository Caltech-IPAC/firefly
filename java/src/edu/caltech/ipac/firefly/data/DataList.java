package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.PropertyChangeSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Date: Jun 16, 2008
 *
 * @author loi
 * @version $Id: DataList.java,v 1.3 2008/09/19 01:27:38 loi Exp $
 */
public class DataList<E>  {
    public static String ADDED = "added";
    public static String REMOVED = "removed";
    public static String ADD_ALL = "addAll";
    public static String CLEAR = "clear";
    public static String REMOVE_RANGE = "removeRange";

    private LinkedList<E> list = new LinkedList<E>();
    transient private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Move the given elecment to the given index if that elecment exists in this list.
     * Otherwise, add the object to the list at the given index.
     * @param index
     * @param elecment
     * @return true if a move operation was performed.
     */
    public boolean moveTo(int index, E elecment) {
        int idx = list.indexOf(elecment);
        if (idx >= 0) {
            remove(idx);
        }
        this.add(index, elecment);
        return idx >= 0;
    }

    public void add(int index, E element) {
        list.add(index, element);
        pcs.firePropertyChange(ADDED, index, element);

    }

    public void addFirst(E o) {
        list.addFirst(o);
        pcs.firePropertyChange(ADDED, 0, o);
    }

    public void addLast(E o) {
        list.addLast(o);
        pcs.firePropertyChange(ADDED, list.size()-1, o);
    }

    public boolean remove(Object o) {
        int idx = list.indexOf(o);
        boolean success = list.remove(o);
        if (success) {
            pcs.firePropertyChange(REMOVED, idx, o);
        }
        return success;
    }

    public E remove(int index) {
        E o = list.remove(index);
        if (o != null) {
            pcs.firePropertyChange(REMOVED, index, o);
        }
        return o;
    }

    public List<E> getList() {
        return Collections.unmodifiableList(list);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        boolean success = list.addAll(index, c);
        if (success) {
            pcs.firePropertyChange(ADD_ALL, index, c);
        }
        return success;
    }

    public void clear() {
        list.clear();
        pcs.firePropertyChange(CLEAR, null, null);
    }

    public void removeRange(int fromIndex, int toIndex) {
        List<E> clone = new ArrayList<E>(list);
        int toIdx = Math.min(list.size(), toIndex);
        for (int i = fromIndex; i < toIdx; i++) {
            list.remove(clone.get(i));
        }
        pcs.firePropertyChange(REMOVE_RANGE, fromIndex, toIdx);

    }

    public int size() {
        return list.size();
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
