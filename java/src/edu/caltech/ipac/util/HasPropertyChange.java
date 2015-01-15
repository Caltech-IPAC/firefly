/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;
/**
 * User: roby
 * Date: 5/23/11
 * Time: 4:17 PM
 */


import java.beans.PropertyChangeListener;

/**
 * @author Trey Roby
 */
public interface HasPropertyChange {

    public void addPropertyChangeListener(PropertyChangeListener listener);
    public void removePropertyChangeListener(PropertyChangeListener listener);
    public void addPropertyChangeListener( String propertyName, PropertyChangeListener listener);
    public void removePropertyChangeListener( String propertyName, PropertyChangeListener listener);


}

