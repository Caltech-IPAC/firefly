/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.ui.HasWidgets;
import edu.caltech.ipac.firefly.data.Param;

import java.util.List;

/**
 * @author tatianag
 *         $Id: InputFieldGroup.java,v 1.3 2010/04/22 22:09:10 roby Exp $
 */
public interface InputFieldGroup extends HasWidgets {

    /**
     * return a list of KeyValue pair [parameters] for this group
     */
    public List<Param> getFieldValues();

    /**
     * Defines how to set values of the input fields inside the group from the list of KeyValue [parameters]
     * @param list
     */
    public void setFieldValues(List<Param> list);

    /**
     * Extra validation for group (other than input field validation)
     * @return true if validation was successful
     */
    public boolean validate();
}
