package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.ui.input.FieldLabel;
import edu.caltech.ipac.firefly.ui.input.HTMLFieldLabel;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.util.dd.FieldDef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author tatianag
 *         $Id: RadioGroupInputField.java,v 1.13 2012/05/25 04:36:40 tatianag Exp $
 */
public class RadioGroupInputField extends InputField implements HasWidgets {

    public static int singleSeqIdx = 0;

    private final CellPanel _panel;
    private final FieldDef _fieldDef;
    private final List<EnumFieldDef.Item> _items;
    private final List<RadioButton> _rbs;
    private FieldLabel _label= null;

    public RadioGroupInputField(EnumFieldDef fieldDef) {

        _fieldDef = fieldDef;

        if (fieldDef.getOrientation().equals(EnumFieldDef.Orientation.Vertical)) {
            _panel = new VerticalPanel();
        } else {
            _panel = new HorizontalPanel();
        }
        initWidget(_panel);
        _panel.setSpacing(5);
        _panel.setTitle(_fieldDef.getShortDesc());


        //list box setup
        _items= ((EnumFieldDef)_fieldDef).getEnumValues();
        _rbs = new ArrayList<RadioButton>(_items.size());
        RadioButton rb;
        if (_items.size() == 1) {
            rb = new RadioButton(_fieldDef.getName()+singleSeqIdx, " "+_items.get(0).getTitle());
            if (_fieldDef.getDefaultValueAsString().toLowerCase().equals("false")) {
                rb.setValue(false);
            } else {
                rb.setValue(true);
            }
            rb.addClickHandler(new ClickHandler(){
                public void onClick(ClickEvent event) {
                    ValueChangeEvent.fire(RadioGroupInputField.this, getValue());
                    updatePref(getValue());
                }
            });
            _rbs.add(rb);
            _panel.add(rb);
            singleSeqIdx++;
        } else {
            String enumLock= _fieldDef.getName() + singleSeqIdx;
            if (StringUtils.isEmpty(enumLock)) enumLock= "radio-group-"+ singleSeqIdx;
            for(EnumFieldDef.Item item : _items) {
                rb = new RadioButton(enumLock, " "+item.getTitle());
                GwtUtil.setStyle(rb, "whiteSpace", "nowrap");
                rb.addClickHandler(new ClickHandler(){
                    public void onClick(ClickEvent event) {
                        ValueChangeEvent.fire(RadioGroupInputField.this, getValue());
                        updatePref(getValue());
                    }
                });
                _rbs.add(rb);
                _panel.add(rb);
            }
            singleSeqIdx++;
        }
        reset();
    }

    public FieldDef getFieldDef() {
        return _fieldDef;
    }

    public void setPaddingBetween(int btwn) {
        String padding= _panel instanceof VerticalPanel ? "paddingTop" : "paddingLeft";
        String space= btwn+"px";
        if (_rbs.size()>1) {
            for(int i= 1; (i<_rbs.size()); i++) {
                GwtUtil.setStyle(_rbs.get(i), padding, space);
            }
        }
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
        return _rbs.get(0);
    }

    public void reset() { setValue(_fieldDef.getDefaultValueAsString()); }

    public boolean validate() { return true; }

    public void forceInvalid(String errorText) {}

    // value is the value of the selected radio box
    public String getValue() {
        String value = "";
        for (int idx = 0; idx < _items.size(); idx++) {
            if (_rbs.get(idx).getValue()) {
                value = _items.get(idx).getName();
                break;
            }
        }
        return value;
    }

    public RadioButton getRadioButton(String value) {
        RadioButton rb= null;
        for (int idx = 0; idx < _items.size(); idx++) {
            if (value.equals(_items.get(idx).getName())) {
                return _rbs.get(idx);

            }
        }
        return rb;
    }

    public void setValue(String value) {

        if (_items.size() == 1) {
            String oldValue = getValue();
            if (!oldValue.equals(value)) {
                _rbs.get(0).setValue(Boolean.parseBoolean(value));
                ValueChangeEvent.fire(RadioGroupInputField.this, getValue());
                updatePref(getValue());
            }
            return;
        }

        // multiple buttons
        boolean shouldBeSelected;
        int idx = 0;
        String oldValue = getValue();
        for(EnumFieldDef.Item item : _items) {
            shouldBeSelected = item.getName().equals(value);
            _rbs.get(idx).setValue(shouldBeSelected);
            idx++;
        }
        String newValue = getValue();
        // if no radio button is selected,
        // which can happen if the value is set from a preference
        // set value to default
        if (StringUtils.isEmpty(newValue)) {
            updatePref("");
            String defaultVal = _fieldDef.getDefaultValueAsString();
            if (!StringUtils.isEmpty(defaultVal)) {
                idx = 0;
                for(EnumFieldDef.Item item : _items) {
                    shouldBeSelected = item.getName().equals(defaultVal);
                    _rbs.get(idx).setValue(shouldBeSelected);
                    idx++;
                }
                newValue = getValue();
            }
        }
        if (!oldValue.equals(newValue)) {
            ValueChangeEvent.fire(RadioGroupInputField.this, getValue());
            updatePref(getValue());
        }
    }


    private void updatePref(String value) {
        if (_fieldDef.isUsingPreference()) {
            String key= _fieldDef.getPreferenceKey();
            if (!ComparisonUtil.equals(value, Preferences.get(key))) {
                Preferences.set(key, value);
            }
        }
    }



    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return addHandler(h, ValueChangeEvent.getType());
    }


    // implementation of HasWidgets interface

    public void add(Widget w) {
        _panel.add(w);
    }

    public void clear() {
        _panel.clear();
    }

    public Iterator<Widget> iterator() {
        return _panel.iterator();
    }

    public boolean remove(Widget w) {
        return _panel.remove(w);
    }

}
