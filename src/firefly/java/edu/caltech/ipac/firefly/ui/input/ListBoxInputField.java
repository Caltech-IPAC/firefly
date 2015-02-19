/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.ListBox;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.util.dd.FieldDef;

import java.util.List;

/**
 * @author Trey Roby
 * $Id: ListBoxInputField.java,v 1.6 2011/04/27 20:55:34 roby Exp $
 */
public class ListBoxInputField extends InputField {

    private ListBox _listBox= new ListBox();
    private final FieldDef _fieldDef;
    private final List<EnumFieldDef.Item> _items;
    private int _selectedIdx;
    private FieldLabel _label= null;

    public ListBoxInputField(EnumFieldDef fieldDef) {
        _fieldDef= fieldDef;
        initWidget(_listBox);

        //list box setup
        _items= ((EnumFieldDef)_fieldDef).getEnumValues();
        for(EnumFieldDef.Item item : _items) {
            _listBox.addItem(item.getTitle());
        }
        _listBox.setTitle(_fieldDef.getShortDesc());
        reset();

        _listBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                _selectedIdx= _listBox.getSelectedIndex();
                ValueChangeEvent.fire(ListBoxInputField.this,_selectedIdx+"");
                updatePref();
            }
        });

       // label setup
    }

    public FieldDef getFieldDef(){ return _fieldDef; }

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

    public FocusWidget getFocusWidget() {return _listBox; }

    public String getValue() {return _items.get(_selectedIdx).getName();  }

    public void setValue(String v) {
        int cnt= 0;
        for(EnumFieldDef.Item item : _items) {
            if (v.equals(item.getName())) {
                int oldSelectedIdx = _selectedIdx;
                _selectedIdx= cnt;
                _listBox.setSelectedIndex(_selectedIdx);
                if (oldSelectedIdx != _selectedIdx) {
                    ValueChangeEvent.fire(ListBoxInputField.this,_selectedIdx+"");    
                }
                updatePref();
            }
            cnt++;
        }
    }

    public void reset() { setValue(_fieldDef.getDefaultValueAsString()); }

    public boolean validate() {
        return true;
    }


    private void updatePref() {
        if (_fieldDef.isUsingPreference()) {
            String value= getValue();
            String key= _fieldDef.getPreferenceKey();
            if (!ComparisonUtil.equals(value, Preferences.get(key))) {
                Preferences.set(key,value);
            }
        }
    }


    public void forceInvalid(String reason) {}

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return addHandler(h, ValueChangeEvent.getType());
    }

}
