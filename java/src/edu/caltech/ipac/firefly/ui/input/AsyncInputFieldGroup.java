/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.Param;

import java.util.List;
/**
 * User: roby
 * Date: Aug 16, 2010
 * Time: 3:01:26 PM
 */


/**
 * @author Trey Roby
 */
public interface AsyncInputFieldGroup extends InputFieldGroup {

    public void getFieldValuesAsync(AsyncCallback<List<Param>> cb);
    public boolean isAsyncCallRequired();

}

