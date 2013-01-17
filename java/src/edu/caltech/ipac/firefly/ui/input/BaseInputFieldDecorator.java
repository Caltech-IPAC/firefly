package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FocusWidget;
import edu.caltech.ipac.util.dd.FieldDef;


/**
 * @author tatianag
 * $Id: BaseInputFieldDecorator.java,v 1.6 2011/02/25 00:46:55 roby Exp $
 */
public class BaseInputFieldDecorator extends InputField {

    private final InputField _inputField;

    public BaseInputFieldDecorator(InputField inputField) {
        _inputField= inputField;
        _inputField.setDecorator(this);
    }


    public InputField getIF() { return _inputField; }

    public void forceInvalid(String errorText) { _inputField.forceInvalid(errorText);}


    @Override
    public FieldDef getFieldDef() { return _inputField.getFieldDef(); }

    @Override
    public FieldLabel getFieldLabel() { return _inputField.getFieldLabel(); }

    @Override
    public FocusWidget getFocusWidget() { return _inputField.getFocusWidget(); }

    @Override
    public String getValue() { return _inputField.getValue(); }


    @Override
    public void setValue(String v) { _inputField.setValue(v); }

    @Override
    public void reset() { _inputField.reset(); }

    @Override
    public boolean validate() { return _inputField.validate(); }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return _inputField.addValueChangeHandler(h);
    }

    @Override
    public void setFireValueChangeOnKeystroke(boolean fireOnKeystroke) {
        super.setFireValueChangeOnKeystroke(fireOnKeystroke);
        _inputField.setFireValueChangeOnKeystroke(fireOnKeystroke);
    }

}