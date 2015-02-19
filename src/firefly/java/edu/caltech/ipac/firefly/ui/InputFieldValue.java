/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.input.InputField;
/**
 * User: roby
 * Date: Aug 13, 2010
 * Time: 12:50:41 PM
 */


/**
 * @author Trey Roby
 */
public class InputFieldValue {

    public final InputField _field;
    public final Param _param;

    public InputFieldValue(InputField field, Param param) {
       _field= field;
       _param= param;
    }


}

