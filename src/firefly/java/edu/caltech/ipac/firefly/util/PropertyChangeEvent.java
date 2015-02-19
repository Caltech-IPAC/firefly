/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;

public class PropertyChangeEvent {

    private Object source;
	private String propertyName;
    private Object oldValue;
	private Object newValue;


    public PropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue) {
        this.source = source;
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

}