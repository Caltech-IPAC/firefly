package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.Iterator;


/**
 * @author tatianag
 * $Id: ValidationInputField.java,v 1.19 2012/03/12 18:04:41 roby Exp $
 */
public class ValidationInputField extends BaseInputFieldDecorator implements HasWidgets {

//    public static final String EXCLAMATION= "images/gxt/exclamation16x16.gif";
    private final FieldDef _fieldDef;
    private HorizontalPanel _fieldWidgetPanel = new HorizontalPanel();
    private Image _warningIcon= null;
    private final FocusPanel _warningArea= new FocusPanel();
    private PopupPane _errorPop= null;
    private Label _errorText= new Label();
    private boolean _errorOn= false;
    private String _lastInput= "";
    private boolean _hasFocus= false;


    public ValidationInputField(InputField inputField) {
        super(inputField);
        _fieldDef= inputField.getFieldDef();
        build();
        initWidget(_fieldWidgetPanel);
    }


    private void build() {

        _warningArea.setWidth("16px");
        _warningArea.setHeight("16px");
        DOM.setStyleAttribute(_warningArea.getElement(), "paddingLeft", "3px");
        ErrorHandler eh= new ErrorHandler();
        _warningArea.addFocusHandler(eh);
        _warningArea.addBlurHandler(eh);
        _warningArea.addMouseDownHandler(eh);
        _warningArea.addMouseOverHandler(eh);
        _warningArea.addMouseOutHandler(eh);




        _fieldWidgetPanel.add(getIF());
        _fieldWidgetPanel.add(_warningArea);

        _warningArea.setTabIndex(-1);


        FieldHandler fh= new FieldHandler();
        InputField inF= getIF();
        inF.getFocusWidget().addKeyDownHandler(fh);
        inF.getFocusWidget().addFocusHandler(fh);
        inF.getFocusWidget().addBlurHandler(fh);


        updateStyle();
    }


    @Override
    public void forceInvalid(String errorText) {
        _errorText.setText(errorText);
        _warningArea.setWidget(getWarningIcon());
        setErrorOn(true);
    }

    private Image getWarningIcon() {
        if (_warningIcon==null) {
            _warningIcon= new Image(GwtUtil.EXCLAMATION);
            _warningIcon.setPixelSize(16,16);
        }
        return _warningIcon;
    }

    @Override
    public void setValue(String v) {
        super.setValue(v);
        setErrorOn(false);
    }

    @Override
    public void reset() {
        super.reset();
        setErrorOn(false);
    }

    @Override
    public boolean validate() { return ingestInput(true); }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return super.addValueChangeHandler(h);
    }



    // implementation of HasWidgets interface

    public void add(Widget w) {
        _fieldWidgetPanel.add(w);
    }

    public void clear() {
        _fieldWidgetPanel.clear();
    }

    public Iterator<Widget> iterator() {
        return _fieldWidgetPanel.iterator();
    }

    public boolean remove(Widget w) {
        return _fieldWidgetPanel.remove(w);
    }


    private void validateKeyInput() {
        String s= super.getValue();

        if (!ComparisonUtil.equals(s,_lastInput)) {
            _lastInput= s;
            String mask= _fieldDef.getMask();
            boolean empty = StringUtils.isEmpty(s);
            if (!empty) {
                if (mask!=null) {
                    respondToValidation(s.matches(mask));
                } // end if mask != null
                else {
                    respondToValidation(getIF().validateSoft());
                } // end else
            } else {
                setErrorOn(false);
            }
        }
   }


    protected boolean ingestInput(boolean validateDefault) {
        String sval= super.getValue();
        boolean v= true;
        if (validateDefault || !StringUtils.areEqual(sval, getFieldDef().getDefaultValueAsString())) {
            try {
                v= getIF().validate();
                if (!v) throw new ValidationException(_fieldDef.getErrMsg());
                showError(false);
                setErrorOn(false);
            } catch (ValidationException e) {
                forceInvalid(e.getMessage());
                v = false;
            }
        }
        _lastInput= sval;
        return v;
    }

    private void respondToValidation(boolean fieldIsValid) {
        if (fieldIsValid)  setErrorOn(false);
        else               forceInvalid(_fieldDef.getErrMsg());
    }

    private void setErrorOn(boolean on) {
        _errorOn= on;
        if (!_errorOn && _errorPop!=null)  _errorPop.hide();
        updateStyle();
    }


    private void updateStyle() {
        if (_errorOn) {
            _warningArea.setWidget(getWarningIcon());
            getFocusWidget().setStylePrimaryName("firefly-inputfield-error");
        }
        else {
            if (_warningArea.getWidget()!=null) _warningArea.clear();
            if (_hasFocus) {
                getFocusWidget().setStylePrimaryName("firefly-inputfield-focus");

            }
            else {
                getFocusWidget().setStylePrimaryName("firefly-inputfield-valid");
            }
        }
    }



    /**
     * @param show boolean that tells whether to show warning area
     */
    private void showError(boolean show) {
        if (show && _errorOn) {
            if (_errorPop==null) createErrorPop();

            _errorPop.alignTo(_warningArea, PopupPane.Align.AUTO_POINTER);
            _errorPop.show();
        }
        else {
            if (_errorPop!=null)  _errorPop.hide();
        }
    }


    private void createErrorPop() {


        Image exclaim= new Image(GwtUtil.EXCLAMATION);
        HorizontalPanel hp= new HorizontalPanel();
        hp.add(exclaim);
        hp.add(_errorText);
        hp.setStylePrimaryName("firefly-inputfield-error-msg");
        _errorText.setWidth("150px");
        DOM.setStyleAttribute(_errorText.getElement(), "paddingLeft", "5px");
        _errorPop= new PopupPane(null,hp, PopupType.STANDARD,true, false,false, PopupPane.HeaderType.NONE);
        _errorPop.useHighestZIndexLevel();
    }

// =====================================================================
// -------------------- Inner Classes --------------------------------
// =====================================================================



    private class ErrorHandler implements FocusHandler,
                                          BlurHandler,
                                          MouseDownHandler,
                                          MouseOverHandler,
                                          MouseOutHandler {
        public void onMouseDown(MouseDownEvent ev) { showError(true); }
        public void onMouseOver(MouseOverEvent ev) { showError(true); }
        public void onMouseOut(MouseOutEvent ev) { showError(false); }
        public void onFocus(FocusEvent event) { showError(true); }
        public void onBlur(BlurEvent event) {  showError(false); }
    }

    private class FieldHandler implements FocusHandler,
                                          BlurHandler,
                                          KeyDownHandler {
        public void onFocus(FocusEvent ev) {
            _hasFocus= true;
            updateStyle();
        }
        public void onBlur(BlurEvent ev) {
            _hasFocus= false;
            ingestInput(false);
            updateStyle();
        }

        public void onKeyDown(KeyDownEvent ev) {
            final int keyCode= ev.getNativeKeyCode();
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    if (keyCode== KeyCodes.KEY_ENTER) {
                        ingestInput(true);
                    }
                    else {
                        validateKeyInput();
                    }
                }
            });
        }
    }



}
