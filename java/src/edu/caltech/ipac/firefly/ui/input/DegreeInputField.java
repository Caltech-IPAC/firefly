package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.ValidationException;

/**
 * @author tatianag
 *         $Id: DegreeInputField.java,v 1.10 2011/02/25 00:46:55 roby Exp $
 */
public class DegreeInputField extends InputField {

    private static WebClassProperties _prop= new WebClassProperties(DegreeInputField.class);

     private static final int DEGREE_IDX= 0;
     private static final int ARCMIN_IDX= 1;
     private static final int ARCSEC_IDX= 2;

     private final DegreeFieldDef _degreeDef;
     private final static String _unitStrs[]= new String[3];
     private final TextBoxInputField _inputField;
     private final TextBox _textBox;
     private final ListBox _listBox= new ListBox();

     static {
         _unitStrs[DEGREE_IDX]= _prop.getName("degree");
         _unitStrs[ARCSEC_IDX]= _prop.getName("arcsec");
         _unitStrs[ARCMIN_IDX]= _prop.getName("arcmin");
     }

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public DegreeInputField(DegreeFieldDef fd) {
        _degreeDef= fd;
        HorizontalPanel hp= new HorizontalPanel();
        initWidget(hp);
//        _inputField = new TextBoxInputField(fd,true);
        _inputField = new TextBoxInputField(fd,true);
        _textBox= _inputField.getTextBox();
        buildListBox();

        getFieldLabel();
        setUnits(_degreeDef.getUnits());
        _textBox.setMaxLength(10);



//        _inputField.getTextBox().setWidth("115px");
        _listBox.setWidth("100px");
        GwtUtil.setStyle(_listBox, "fontSize", "12px");

        hp.add(_textBox);
        hp.add(_listBox);


        GwtUtil.setStyle(_listBox, "margin", "1px 0 0 4px");


    }

    public FieldDef getFieldDef() { return _inputField.getFieldDef(); }

    public FieldLabel getFieldLabel() { return _inputField.getFieldLabel();  }

    public FocusWidget getFocusWidget() { return _inputField.getFocusWidget(); }

    public void forceInvalid(String errorText) {
        _inputField.forceInvalid(errorText);
    }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return _inputField.addValueChangeHandler(h);
    }


    /**
     * The units of the value returned are internal units in the field definition
     * @return the value in internal units (default - degrees)
     */
    @Override
    public String getValue() {
        DegreeFieldDef.Units displayUnits  = _degreeDef.getUnits();
        DegreeFieldDef.Units internalUnits = _degreeDef.getInternalUnits();
        String retval;
        try {
            double internalValue= DegreeFieldDef.convert(displayUnits, internalUnits,getDoubleValue());
            retval= ""+internalValue;
        } catch (NumberFormatException e) {
            retval= _inputField.getValue();
        }
        return retval;
    }

    /**
     * The units of the value are internal units in field definition
     * @param val the value in internal units (default - degrees)
     */
    @Override
    public void setValue(String val) {
        DegreeFieldDef.Units displayUnits  = _degreeDef.getUnits();
        DegreeFieldDef.Units internalUnits = _degreeDef.getInternalUnits();
        double internalValue = Double.parseDouble(val);
        double displayValue = DegreeFieldDef.convert(internalUnits, displayUnits, internalValue);
        _inputField.setValue(_degreeDef.format(displayValue));
    }

    @Override
    public boolean validate() {
        boolean retval;
        try {
            retval = _degreeDef.validate(_inputField.getValue());
        } catch (ValidationException e) {
            retval= false;
        }
        return retval;
    }

    @Override
    public void reset() {
        if (_degreeDef!=null) {
            setUnits(_degreeDef.getOriginalUnits());
        }
        _inputField.reset();
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================



     private double getDoubleValue() {
       return Double.parseDouble(_inputField.getValue());
     }

     public void setUnits(DegreeFieldDef.Units units) {
         DegreeFieldDef.Units oldUnits= _degreeDef.getUnits();
         _degreeDef.setUnits(units);
         updateUnitsLabel();
          updatePrecision();

         try {
             double v= DegreeFieldDef.convert(oldUnits, units, getDoubleValue());
             _inputField.setValue(_degreeDef.format(v));
             if (getDecorator()!=null) getDecorator().validate();
             _textBox.setFocus(true);
         } catch (NumberFormatException e) { /*no conversion*/ }
     }

     public DegreeFieldDef.Units getUnits() { return _degreeDef.getUnits(); }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================



     private void updatePrecision() {
         switch (_degreeDef.getUnits()) {
             case DEGREE :
                 _degreeDef.setPrecision(3);
                 break;
             case ARCMIN :
                 _degreeDef.setPrecision(2);
                 break;
             case ARCSEC :
                 _degreeDef.setPrecision(0);
                 break;
             default :
                 WebAssert.tst(false, "unknown units");

         }

     }

    private void buildListBox() {
        _listBox.addItem(_unitStrs[DEGREE_IDX]);
        _listBox.addItem(_unitStrs[ARCMIN_IDX]);
        _listBox.addItem(_unitStrs[ARCSEC_IDX]);

        _listBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                changeUnits(_listBox.getSelectedIndex());
            }
        });

    }

    public void updateUnitsLabel() {
        DegreeFieldDef.Units units= getUnits();
        switch (units) {
            case DEGREE:
                _listBox.setSelectedIndex(DEGREE_IDX);
                break;
            case ARCMIN:
                _listBox.setSelectedIndex(ARCMIN_IDX);
                break;
            case ARCSEC:
                _listBox.setSelectedIndex(ARCSEC_IDX);
                break;
            default :
                WebAssert.tst(false,"unknown units");
                break;
        }
    }

    private void changeUnits(int unitsIdx) {
        switch (unitsIdx) {
            case DEGREE_IDX :
                setUnits(DegreeFieldDef.Units.DEGREE);
                break;
            case ARCMIN_IDX :
                setUnits(DegreeFieldDef.Units.ARCMIN);
                break;
            case ARCSEC_IDX :
                setUnits(DegreeFieldDef.Units.ARCSEC);
                break;
            default :
                WebAssert.tst(false,"unknown unit index");
                break;
        }
        ValueChangeEvent.fire(DegreeInputField.this,unitsIdx+"");
    }




}
