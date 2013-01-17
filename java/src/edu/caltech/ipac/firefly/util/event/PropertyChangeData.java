package edu.caltech.ipac.firefly.util.event;

public class PropertyChangeData {

	private final String propertyName;
    private final Object oldValue;
	private final Object newValue;


    public PropertyChangeData(String propertyName, Object oldValue, Object newValue) {
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getOldValue() {
        return oldValue;
    }


    public Object getNewValue() {
        return newValue;
    }

}