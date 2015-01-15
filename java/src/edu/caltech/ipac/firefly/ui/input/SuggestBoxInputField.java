/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import edu.caltech.ipac.util.dd.FieldDef;

/**
 * @author tatianag
 *         $Id: SuggestBoxInputField.java,v 1.7 2012/01/24 20:31:14 roby Exp $
 */
public class SuggestBoxInputField extends InputField {

    private final SuggestBox _suggestBox;
    private final TextBoxInputField _inputField;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public SuggestBoxInputField(FieldDef fieldDef, SuggestOracle oracle) {
        _inputField = new TextBoxInputField(fieldDef,true);
        _suggestBox = new SuggestBox(oracle, _inputField.getTextBox());
        initWidget(_suggestBox );
    }

    @Override
    public FocusWidget getFocusWidget() {
        return _inputField.getTextBox();
    }

    public FieldDef getFieldDef() { return _inputField.getFieldDef(); }

    public FieldLabel getFieldLabel() { return _inputField.getFieldLabel(); }

    public void reset() { _inputField.reset(); }

    public boolean validate() { return _inputField.validate(); }

    @Override
    public boolean validateSoft() { return validate(); }

    public void forceInvalid(String errorText) { _inputField.forceInvalid(errorText); }

    public String getValue() {return  _inputField.getValue(); }

    public void setValue(String v) { _inputField.setValue(v); }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return _inputField.addValueChangeHandler(h);
    }

    public SuggestBox getSuggestBox() { return _suggestBox; }
}
