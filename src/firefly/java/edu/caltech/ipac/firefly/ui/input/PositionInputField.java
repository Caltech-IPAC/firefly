/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import edu.caltech.ipac.firefly.data.form.PositionFieldDef;
import edu.caltech.ipac.firefly.util.PositionParser;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Nov 29, 2010
 * Time: 2:43:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class PositionInputField extends TextBoxInputField  {
    private final PositionFieldDef _positionFieldDef;
//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PositionInputField(PositionFieldDef fd) {
        super(fd);
        _positionFieldDef = fd;

        getFieldLabel();
    }


    @Override
    public void setValue(String v) {
        WorldPt wp= WorldPt.parse(v);
        if (wp!=null) {

            String val= PositionFieldDef.formatPosForTextField(wp);

            try {
                _positionFieldDef.validate(val);
            } catch (ValidationException e) {
                // ignore
            }

            super.setValue(val);
        }
        else {
            _positionFieldDef.setObjectName(v);
            super.setValue(v);
        }
    }

    public WorldPt getPosition() {
        return _positionFieldDef.getPosition();
    }

}

