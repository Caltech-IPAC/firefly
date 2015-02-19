/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.searchui;
/**
 * User: roby
 * Date: 2/14/14
 * Time: 11:39 AM
 */


import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;

/**
 * @author Trey Roby
 */
public interface DataTypeSelectUI extends InputFieldGroup {
    public Widget makeUI();
    public abstract String makeRequestID();
    public String getDataDesc();
}


