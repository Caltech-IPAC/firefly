package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.util.dd.FieldDef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author tatianag
 * $Id: CheckBoxGroupInputField.java,v 1.5 2012/03/12 18:04:41 roby Exp $
 */
public class CheckBoxGroupInputField extends InputField implements HasWidgets {

    public static String MULTI_FIELD_SEPARATOR = ",";
    public static final String NONE = "_none_";
    public static final String ALL = "_all_";

    private CellPanel _fieldWidgetPanel;
    private Image _warningIcon = null;
    private final FocusPanel _warningArea= new FocusPanel();
    private PopupPane _errorPop= null;
    private Label _errorText= new Label();
    private boolean _errorOn= false;
    private FieldLabel _label= null;


    private final CellPanel _panel;
    private final FieldDef _fieldDef;
    private final List<EnumFieldDef.Item> _items;
    private final List<CheckBox> _cbs;
    private int _idxOfAll = -1;


    public CheckBoxGroupInputField(EnumFieldDef fieldDef) {
        _fieldDef = fieldDef;

        if (fieldDef.getOrientation().equals(EnumFieldDef.Orientation.Vertical)) {
            _panel = new VerticalPanel();
            _fieldWidgetPanel = new VerticalPanel();
        } else {
            _panel = new HorizontalPanel();
            _fieldWidgetPanel = new HorizontalPanel();
        }
        initWidget(_fieldWidgetPanel);
        _panel.setSpacing(5);
        _panel.setTitle(_fieldDef.getShortDesc());


        //list box setup
        _items= ((EnumFieldDef)_fieldDef).getEnumValues();
        _cbs = new ArrayList<CheckBox>(_items.size());
        CheckBox cb;
        int idx = 0;
        for(EnumFieldDef.Item item : _items) {
            cb = new CheckBox(" "+item.getTitle(), true);
            _cbs.add(cb);
            _panel.add(cb);
            if (item.getName().equals(ALL)) {
                _idxOfAll = idx;
            }
            idx++;
        }

        // add warning area
        _warningArea.setWidth("16px");
        _warningArea.setHeight("16px");
        DOM.setStyleAttribute(_warningArea.getElement(), "padding", "3px");
        ErrorHandler eh= new ErrorHandler();
        _warningArea.addFocusHandler(eh);
        _warningArea.addBlurHandler(eh);
        _warningArea.addMouseDownHandler(eh);
        _warningArea.addMouseOverHandler(eh);
        _warningArea.addMouseOutHandler(eh);
        _fieldWidgetPanel.add(_panel);
        _fieldWidgetPanel.add(_warningArea);
        _fieldWidgetPanel.setCellHorizontalAlignment(_warningArea, HasHorizontalAlignment.ALIGN_CENTER);
        _fieldWidgetPanel.setCellVerticalAlignment(_warningArea, HasVerticalAlignment.ALIGN_MIDDLE);

        // add click listeners
        for(idx=0; idx < _cbs.size(); idx++) {
            final CheckBox current = _cbs.get(idx);
            final int currentIdx = idx;
            current.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent ev) {
                    if ((currentIdx == _idxOfAll) && current.getValue()) {
                        //uncheck all other
                        for (int i=0; i<_cbs.size(); i++) {
                            if (i != _idxOfAll) {
                                _cbs.get(i).setValue(true);
                            }
                        }
                    }else if ((currentIdx == _idxOfAll) && !current.getValue()) {
                        //all is unchecked so uncheck all that were checked
                        for (int i=0; i<_cbs.size(); i++) {
                            if (i != _idxOfAll) {
                                _cbs.get(i).setValue(false);
                            }
                        }
                    }  else if (_idxOfAll >= 0) {
                        // uncheck _all_ unless all other checkboxes are checked
                        CheckBox all = null;
                        int nChecked = 0;
                        for (int i=0; i<_cbs.size(); i++) {
                            if (i == _idxOfAll) {
                                all = _cbs.get(i);
                            } else {
                                if (_cbs.get(i).getValue()) {
                                    nChecked++;
                                }
                            }
                        }

                        assert(all != null);
                        // all checkboxes are checked
                        if (nChecked == _cbs.size()-1) {
                            all.setValue(true);
                            for (int i=0; i<_cbs.size(); i++) {
                                if (i != _idxOfAll) {
                                    _cbs.get(i).setValue(true);
                                }
                            }
                        } else {
                            all.setValue(false);
                        }
                    }
                    ValueChangeEvent.fire(CheckBoxGroupInputField.this,getValue());
                }
            });

        }
        reset();

        addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                validate();
            }
        });

      // label setup
    }

    public FieldDef getFieldDef() {
        return _fieldDef;
    }

    public FieldLabel getFieldLabel() {
        if (_label==null) {
            if (_fieldDef.isTextImmutable())  {
                _label= new FieldLabel.Immutable() {
                    public String getHtml() {
                        return "<span title=\""+_fieldDef.getShortDesc() + "\"" +
                                "class=\"check-box-group-label\">"+
                                _fieldDef.getLabel() +"</span>";
                    }
                };



            }
            else {
                _label= new HTMLFieldLabel( _fieldDef.getLabel(),
                                            _fieldDef.getShortDesc());
            }

        }
        return _label;
    }

    public FocusWidget getFocusWidget() {
        return _cbs.get(0);
    }

    public void reset() {
        setValue(_fieldDef.getDefaultValueAsString());
        setErrorOn(false);
    }

    public boolean validate() {
        if (!_fieldDef.isNullAllow() && getValue().equals(NONE)) {
            _errorText.setText(_fieldDef.getErrMsg());
            setErrorOn(true);
            return false;
        } else {
            setErrorOn(false);
            return true;
        }
    }

    public void forceInvalid(String errorText) {
        _errorText.setText(errorText);
        setErrorOn(true);
    }

    public String getValue() {
        String value = "";
        boolean first = true;
        for (int idx = 0; idx < _items.size(); idx++) {
            if (_cbs.get(idx).getValue()) {
                if (!first) {
                    value += MULTI_FIELD_SEPARATOR;
                } else {
                    first = false;
                }
                value += _items.get(idx).getName();
            }
        }
        if (StringUtils.isEmpty(value)) value = NONE;
        else if (value.contains(ALL)) value = ALL;
        return value;
    }

    public void setValue(String value) {

        if (!getValue().equals(value)) {
            String [] checked;
            if (value.equals(NONE)) {
                // uncheck all
                for (CheckBox cb : _cbs) {
                    cb.setValue(false);
                }
            } else if (value.contains(ALL)) {
                // check all
                for (CheckBox cb : _cbs) {
                    cb.setValue(true);
                }
            } else {
                checked = value.split(MULTI_FIELD_SEPARATOR);
                String name;
                boolean shouldBeSelected;
                int idx = 0;
                for(EnumFieldDef.Item item : _items) {
                    name = item.getName();
                    shouldBeSelected = false;
                    for (String n : checked) {
                        if (n.equals(name)) {
                            shouldBeSelected = true;
                        }
                    }
                    _cbs.get(idx).setValue(shouldBeSelected);
                    idx++;
                }
            }
            ValueChangeEvent.fire(CheckBoxGroupInputField.this,value);
        }
    }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return addHandler(h, ValueChangeEvent.getType());
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


    private void setErrorOn(boolean on) {
        _errorOn= on;
        if (!_errorOn && _errorPop!=null)  _errorPop.hide();
        updateStyle();
    }


    private void updateStyle() {
        if (_errorOn) {
            if (_warningIcon==null) {
                _warningIcon= new Image(GwtUtil.EXCLAMATION);
                _warningIcon.setPixelSize(16,16);
            }
            _warningArea.setWidget(_warningIcon);
            _panel.addStyleName("firefly-inputfield-error");
        }
        else {
            if (_warningArea.getWidget()!=null) _warningArea.clear();
            _panel.removeStyleName("firefly-inputfield-error");
        }
    }


    /**
     * @param show boolean that tells whether to show warning area
     */
    private void showError(boolean show) {
        if (show && _errorOn) {
            if (_errorPop==null) createErrorPopNEWER();

            _errorPop.alignTo(_warningArea, PopupPane.Align.BOTTOM_CENTER);
            _errorPop.show();
        }
        else {
            if (_errorPop!=null)  _errorPop.hide();
        }
    }

    private void createErrorPopNEWER() {
        Image exclaim= new Image(GwtUtil.EXCLAMATION);
        HorizontalPanel hp= new HorizontalPanel();
        hp.add(exclaim);
        hp.add(_errorText);
        hp.setStylePrimaryName("firefly-inputfield-error-msg");
        _errorText.setWidth("150px");
        DOM.setStyleAttribute(_errorText.getElement(), "padding", "5px");
        _errorPop= new PopupPane(null,hp, PopupType.STANDARD,true, false,false, PopupPane.HeaderType.NONE);
        _errorPop.useHighestZIndexLevel();
    }

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


}
