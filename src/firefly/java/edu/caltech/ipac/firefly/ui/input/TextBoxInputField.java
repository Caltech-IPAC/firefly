/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.PasswordFieldDef;
import edu.caltech.ipac.util.dd.ValidationException;

/**
 * @author tatianag
 *         $Id: TextBoxInputField.java,v 1.17 2012/02/06 20:18:36 tatianag Exp $
 */
public class TextBoxInputField extends InputField {


    private TextBox _textBox = null;
    private FieldDef _fieldDef;
    private String _lastInput= "";
    private FieldLabel _label= null;


    /**
     * This is the normal construct used for creating or subclassing TextBoxFieldWidget
     * @param fieldDef the fieldDef determines the type of field to create
     */
    public TextBoxInputField(FieldDef fieldDef) { this(fieldDef,false);  }

    /**
     * Use this constructor only when you are subclassing TextBoxFieldWidget and need
     * the special case provided by the willEncapsulate parameter.
     * @param fieldDef the FieldDef that is the Model for this TextBoxFieldWidget
     * @param willEncapsulate this parameter should be true only if you are subclassing
     *        text box and plan to wrap it in another widget.  If true, you must call
     *        initWidget() in the subclass and TextBoxFieldWidget will not call it.
     *        This parameter is rarely used
     */
    protected TextBoxInputField(FieldDef fieldDef, boolean willEncapsulate) {
        _fieldDef = fieldDef;

        if (fieldDef instanceof PasswordFieldDef) {
            _textBox = new PasswordTextBox();
        } else {
            _textBox = new TextBox();
        }
        addHandlers();
        if (!willEncapsulate)initWidget(_textBox);
        _textBox.setTitle(_fieldDef.getShortDesc());
        if (_fieldDef.getPreferWidth()>0) _textBox.setVisibleLength(_fieldDef.getPreferWidth());
//      _textBox.addStyleName("firefly-inputfield-standard");
        if (_fieldDef.getDefaultValueAsString() != null) {
           _textBox.setText(_fieldDef.getDefaultValueAsString());
        }

    }

    public TextBox getTextBox() { return _textBox; }


//======================================================================
//----------------- Implementation of InputField -----------
//======================================================================


    public FieldDef getFieldDef() { return _fieldDef; }

    public FieldLabel getFieldLabel() {
        if (_label==null) {
            if (_fieldDef.isTextImmutable())  {
                _label= new HTMLImmutableLabel(_fieldDef.getLabel(),
                                               _fieldDef.getShortDesc());
            }
            else {
                _label= new HTMLFieldLabel( _fieldDef.getLabel(),
                                            _fieldDef.getShortDesc());
            }
        }
        return _label;
    }


    public FocusWidget getFocusWidget() { return _textBox; }

    public String getValue() {
        String val = _textBox.getText();
        return (val != null) ? val.trim() : val;
    }

    public void setValue(String v) {
        _textBox.setText(v);
        checkForChange(true);
    }

    public boolean validate() {
        boolean valid;
        String value= getValue();
        try {
            valid = _fieldDef.validate(value);
        } catch (ValidationException e) {
            valid= false;
        }

        if (valid && _fieldDef.isUsingPreference()) {
            String key= _fieldDef.getPreferenceKey();
            if (!ComparisonUtil.equals(value,Preferences.get(key))) {
                Preferences.set(key,value);
            }
        }
        return valid;
    }

    public boolean validateSoft() {
        boolean retval;
        try {
            retval = _fieldDef.validateSoft(getValue());
        } catch (ValidationException e) {
            retval= false;
        }
        return retval;
    }

    public void reset() {
        _textBox.setText(_fieldDef.getDefaultValueAsString());
        checkForChange(true);
    }

    public void forceInvalid(String reason) {}

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return addHandler(h, ValueChangeEvent.getType());
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================



    private void addHandlers() {
        BlurHandler blurHandler =  new BlurHandler() {
            public void onBlur(BlurEvent ev) {
                checkForChange(true);
            }
        };
        _textBox.addKeyPressHandler(new KeyHandler());
        _textBox.addBlurHandler(blurHandler);
    }


    private void checkForChange(boolean hardValidate) {
        String sval = getValue();
        boolean isDefault = StringUtils.areEqual(sval, getFieldDef().getDefaultValueAsString());
        // do not validate default
        boolean valid= hardValidate ? validate() : validateSoft();
        if (isDefault || valid) {
            if (!ComparisonUtil.equals(_lastInput, sval)) {
                _lastInput = sval;
                ValueChangeEvent.fire(this,sval);
            }
        }
    }

// =====================================================================
// -------------------- Inner Classes --------------------------------
// =====================================================================


    private class KeyHandler implements KeyPressHandler {

        public void onKeyPress(KeyPressEvent ev) {
            final char keyCode= ev.getCharCode();
            if (keyCode== KeyCodes.KEY_ENTER || getFireValueChangeOnKeystroke()) {
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                            checkForChange(true);
                        }
                });
            }
        }
    }
}
