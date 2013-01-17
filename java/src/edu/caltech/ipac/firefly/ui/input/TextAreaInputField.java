package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Command;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;

/**
 * $Id: TextAreaInputField.java,v 1.6 2011/12/21 23:42:28 xiuqin Exp $
 */
public class TextAreaInputField extends InputField {


    private TextArea _textArea = null;
    private FieldDef _fieldDef;
    private String _lastInput= "";
    private FieldLabel _label= null;


    /**
     * This is the normal construct used for creating or subclassing TextBoxFieldWidget
     * @param fieldDef the fieldDef determines the type of field to create
     */
    public TextAreaInputField(FieldDef fieldDef) { this(fieldDef,false);  }

    /**
     * Use this constructor only when you are subclassing TextAreaFieldWidget and need
     * the special case provided by the willEncapsulate parameter.
     * @param fieldDef the FieldDef that is the Model for this TextAreaFieldWidget
     * @param willEncapsulate this parameter should be true only if you are subclassing
     *        text box and plan to wrap it in another widget.  If true, you must call
     *        initWidget() in the subclass and TextAreaFieldWidget will not call it.
     *        This parameter is rarely used
     */
    protected TextAreaInputField(FieldDef fieldDef, boolean willEncapsulate) {
        _fieldDef = fieldDef;

        _textArea = new TextArea();
        _textArea.setSize("250px", "135px");
        addHandlers();

        if (!willEncapsulate)initWidget(_textArea);
        _textArea.setTitle(_fieldDef.getShortDesc());
        if (_fieldDef.getDefaultValueAsString() != null) {
           _textArea.setText(_fieldDef.getDefaultValueAsString());
        }

    }

    public TextArea getTextArea() { return _textArea; }


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


    public FocusWidget getFocusWidget() { return _textArea; }

    public String getValue() { return _textArea.getText(); }

    public void setValue(String v) {
        _textArea.setText(v);
    }

    public boolean validate() {
        boolean retval;
        try {
            retval = _fieldDef.validate(getValue());
        } catch (ValidationException e) {
            retval= false;
        }
        return retval;
    }

    public void reset() {
        setValue(_fieldDef.getDefaultValueAsString());
        checkForChange();
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
                checkForChange();
            }
        };
        _textArea.addKeyPressHandler(new KeyHandler());
        _textArea.addBlurHandler(blurHandler);
    }



    private void checkForChange() {
        String sval = getValue();
        boolean isDefault = StringUtils.areEqual(sval, getFieldDef().getDefaultValueAsString());
        // do not validate default
        if (isDefault || validate()) {
            if (!ComparisonUtil.equals(_lastInput, sval)) {
                ValueChangeEvent.fire(this,sval);
                _lastInput = sval;
            }
        }
    }

// =====================================================================
// -------------------- Inner Classes --------------------------------
// =====================================================================


    private class KeyHandler implements KeyPressHandler {

        public void onKeyPress(KeyPressEvent ev) {
            final char keyCode= ev.getCharCode();
            if (keyCode== KeyCodes.KEY_ENTER) {
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                            checkForChange();
                        }
                });
            }
        }
    }
}
