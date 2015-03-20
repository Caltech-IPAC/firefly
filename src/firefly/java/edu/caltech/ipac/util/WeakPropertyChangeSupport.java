/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
* Similar functionality as PropertyChangeSupport but holds only
* weak references to listener classes
*
*/

public class WeakPropertyChangeSupport {

    private transient ArrayList<WeakReference<PropertyChangeListener >> listeners =
                   new ArrayList<WeakReference<PropertyChangeListener >>();

    private transient ArrayList<String> interestNames = new ArrayList<String>();


    /** Add weak listener to listen to change of any property. The caller must
    * hold the listener object in some instance variable to prevent it
    * from being garbage collected.
    */
    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        addLImpl(null, l);
    }

    /** Add weak listener to listen to change of the specified property.
    * The caller must hold the listener object in some instance variable
    * to prevent it from being garbage collected.
    */
    public synchronized void addPropertyChangeListener(String propertyName,
            PropertyChangeListener l) {
        addLImpl(propertyName, l);
    }

    /** Remove listener for changes in properties */
    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        int cnt = listeners.size();
        for (int i = 0; i < cnt; i++) {
            Object o = ((WeakReference)listeners.get(i)).get();
            if (o == null || o == l) { // remove null references and the required one
                listeners.remove(i);
                interestNames.remove(i);
                i--;
                cnt--;
            }
        }
    }

    public void firePropertyChange(Object source, String propertyName,
                                   Object oldValue, Object newValue) {
        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }
        PropertyChangeListener la[];
        String isa[];
        int cnt;
        synchronized (this) {
            cnt = listeners.size();
            la = new PropertyChangeListener[cnt];
            for (int i = 0; i < cnt; i++) {
                PropertyChangeListener l =  listeners.get(i).get();
                if (l == null) { // remove null references
                    listeners.remove(i);
                    interestNames.remove(i);
                    i--;
                    cnt--;
                } else {
                    la[i] = l;
                }
            }
            isa = interestNames.toArray(new String[cnt]);
        }

        // now create and fire the event
        PropertyChangeEvent evt = new PropertyChangeEvent(source, propertyName,
                                  oldValue, newValue);
        for (int i = 0; i < cnt; i++) {
            if (isa[i] == null || propertyName == null || isa[i].equals(propertyName)) {
                la[i].propertyChange(evt);
            }
        }
    }

    private void addLImpl(String sn, PropertyChangeListener l) {
        listeners.add(new WeakReference<PropertyChangeListener >(l));
        interestNames.add(sn);
    }

}

