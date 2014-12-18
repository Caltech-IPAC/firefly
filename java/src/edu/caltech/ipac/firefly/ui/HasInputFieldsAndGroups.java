package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 10/13/11
 * Time: 3:54 PM
 */


import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;

import java.util.List;

/**
 * @author Trey Roby
 */
public interface HasInputFieldsAndGroups extends HasValueChangeHandlers<Integer> {
    public List<InputField> getFields();
    public List<InputFieldGroup> getGroups();

    /**
     * Listeners are called when the list the getFields() or getGroups() returns has changed.
     * @param handler the ValueChangeHandler
     * @return the HandlerRegistration
     */
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Integer> handler);
}

