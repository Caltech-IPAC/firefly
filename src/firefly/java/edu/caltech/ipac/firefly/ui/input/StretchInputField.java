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
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import edu.caltech.ipac.firefly.data.form.DoubleFieldDef;
import edu.caltech.ipac.firefly.ui.FieldDefCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.WebPropFieldDefSource;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.RangeFieldDef;
import edu.caltech.ipac.visualize.plot.RangeValues;

/**
 * @author Trey
 *         $Id: StretchInputField.java,v 1.11 2011/06/29 17:03:49 roby Exp $
 */
public class StretchInputField extends InputField {
    //public static enum Type {MIN, MAX}
    //LZ 4/24/15 modified to add dr and gamma
    public static enum Type {MIN, MAX, DR, GAMMA}
    public static enum Units {PERCENT, DATA, MINMAX, SIGMA}
    private static WebClassProperties _prop= new WebClassProperties(StretchInputField.class);

     private static final int PERCENT_IDX= 0;
     private static final int ABSOLUTE_IDX= 1;
     private static final int MINMAX_IDX= 2;
     private static final int SIGMA_IDX= 3;

    private final static String _minUnitStrs[]= new String[4];
    private final static String  _maxUnitStrs[]= new String[4];
    private final ListBox _listBox= new ListBox();
    private final Type _type;
    private int _whichView= -1;
    private WebFitsData _wFitsData;
    private final TextBoxInputField _inputField;
    static {
        _minUnitStrs[PERCENT_IDX]= _prop.getName("minStretch.percent");
        _minUnitStrs[ABSOLUTE_IDX]= _prop.getName("minStretch.absolute");
        _minUnitStrs[MINMAX_IDX]= _prop.getName("minStretch.MinMax");
        _minUnitStrs[SIGMA_IDX]= _prop.getName("minStretch.Sigma");

        _maxUnitStrs[PERCENT_IDX]= _prop.getName("maxStretch.percent");
        _maxUnitStrs[ABSOLUTE_IDX]= _prop.getName("maxStretch.absolute");
        _maxUnitStrs[MINMAX_IDX]= _prop.getName("maxStretch.MinMax");
        _maxUnitStrs[SIGMA_IDX]= _prop.getName("maxStretch.Sigma");
     }

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public StretchInputField(Type type, WebFitsData wFitsData) {

        _inputField = new TextBoxInputField(makeFieldDef(type),true);

        _type= type;
        _inputField.getTextBox().setMaxLength(10);
        if (type==Type.MIN) {
            _listBox.addItem(_minUnitStrs[PERCENT_IDX]);
            _listBox.addItem(_minUnitStrs[ABSOLUTE_IDX]);
            _listBox.addItem(_minUnitStrs[MINMAX_IDX]);
            _listBox.addItem(_minUnitStrs[SIGMA_IDX]);
        }
        else if (type==Type.MAX) {
            _listBox.addItem(_maxUnitStrs[PERCENT_IDX]);
            _listBox.addItem(_maxUnitStrs[ABSOLUTE_IDX]);
            _listBox.addItem(_maxUnitStrs[MINMAX_IDX]);
            _listBox.addItem(_maxUnitStrs[SIGMA_IDX]);
        }

        else if (type!=Type.DR && type!=Type.GAMMA) {
            assert false;
        }


        _listBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                updateFieldDef();
            }
        });


        _inputField.getTextBox().setWidth("115px");


        _listBox.setWidth("85px");
        GwtUtil.setStyle(_listBox, "fontSize", "12px");

        HorizontalPanel hp= new HorizontalPanel();
        hp.add(_inputField.getTextBox());

        //LZ modified in June 2015
        if(type ==Type.MIN || type ==Type.MAX  ) {
            hp.add(_listBox);
            setUnits(PERCENT_IDX);

        }


        initWidget(hp);
        setWebFitsData(wFitsData);
    }


    public void setWebFitsData(WebFitsData wFitsData) {
        _wFitsData= wFitsData;
        if (_type==Type.DR|| _type==Type.GAMMA) return;
       if (_listBox.getSelectedIndex() != PERCENT_IDX) {
           updateFieldDef();
       }
    }

    public static DoubleFieldDef makeFieldDef(Type type) {
        DoubleFieldDef fd;
        if (type==Type.MIN) {
            fd= FieldDefCreator.makeDoubleFieldDef(new WebPropFieldDefSource(_prop.makeBase("minStretch")));
        }
        else if (type==Type.MAX) {
            fd= FieldDefCreator.makeDoubleFieldDef(new WebPropFieldDefSource(_prop.makeBase("maxStretch")));

        }
        //LZ 5/23/15 add for arcsine and power gamma
        else if (type==Type.DR) {
            fd= FieldDefCreator.makeDoubleFieldDef(new WebPropFieldDefSource(_prop.makeBase("drStretch")));


        }
        //LZ 5/23/15 add for power law gamma
        else if (type==Type.GAMMA) {
            fd= FieldDefCreator.makeDoubleFieldDef(new WebPropFieldDefSource(_prop.makeBase("powerLawGammaStretch")));


        }
        else {
            fd= null;
            assert false;
        }
        return fd;
    }

    public int getDataType() {
        int idx= _listBox.getSelectedIndex();
        int retval= RangeValues.PERCENTAGE;

        if (idx==PERCENT_IDX) {
            retval= RangeValues.PERCENTAGE;
        }
        else if (idx==ABSOLUTE_IDX) {
            retval= RangeValues.ABSOLUTE;
        }
        else if (idx==MINMAX_IDX) {
            retval= RangeValues.MAXMIN;
        }
        else if (idx==SIGMA_IDX) {
            retval= RangeValues.SIGMA;
        }
        else {
            assert false; // don't know any other types
        }

        return retval;
    }

    public FieldDef getFieldDef() { return _inputField.getFieldDef(); }

    public FieldLabel getFieldLabel() { return _inputField.getFieldLabel();  }

    public FocusWidget getFocusWidget() { return _inputField.getFocusWidget(); }

    public void reset() { _inputField.reset(); }

    public boolean validate() { return _inputField.validate(); }

    public void forceInvalid(String errorText) {
        _inputField.forceInvalid(errorText);
    }

    public String getValue() { return _inputField.getValue(); }
    public void setValue(String v) { _inputField.setValue(v); }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return _inputField.addValueChangeHandler(h);
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    private void updateFieldDef() {


        if (_type==Type.DR) {

            DoubleFieldDef fd = (DoubleFieldDef) getFieldDef();
            String def = "10.0";
            fd.setMinValue(1D, RangeFieldDef.INCLUSIVE);
            fd.setMaxValue(100000D, RangeFieldDef.INCLUSIVE);
            fd.setDefaultValue(def);
            setValue(def);


        }
        else if (_type==Type.GAMMA){

                DoubleFieldDef fd = (DoubleFieldDef) getFieldDef();
                String def = "2.0";
                 fd.setMinValue(2D, RangeFieldDef.INCLUSIVE);
                fd.setMaxValue(10D, RangeFieldDef.INCLUSIVE);
                fd.setDefaultValue(def);

                setValue(def);


        }
        else {
            int which = _listBox.getSelectedIndex();
            if (which != _whichView) {
                DoubleFieldDef fd = (DoubleFieldDef) getFieldDef();
                String def;
                switch (which) {
                    case PERCENT_IDX:
                        def = _type == Type.MIN ? "1.0" : "99.0";
                        fd.setMinValue(0D, RangeFieldDef.INCLUSIVE);
                        fd.setMaxValue(100D, RangeFieldDef.INCLUSIVE);
                        fd.setDefaultValue(def);
                        setValue(def);
                        break;
                    case SIGMA_IDX:
                        def = _type == Type.MIN ? "-2.0" : "10.0";

                        fd.setMinValue(-100D, RangeFieldDef.INCLUSIVE);
                        fd.setMaxValue(100D, RangeFieldDef.INCLUSIVE);
                        fd.setDefaultValue(def);
                        setValue(def);
                        break;
                    case ABSOLUTE_IDX:
                        def = _type == Type.MIN ? _wFitsData.getDataMin() + "" : _wFitsData.getDataMax() + "";
                        fd.setMinBoundType(null);
                        fd.setMaxBoundType(null);
                        fd.setDefaultValue(def);
                        setValue(def);
                        break;
                    case MINMAX_IDX:
                        def = _type == Type.MIN ? _wFitsData.getDataMin() + "" : _wFitsData.getDataMax() + "";
                        fd.setMinValue(_wFitsData.getDataMin(), RangeFieldDef.INCLUSIVE);
                        fd.setMaxValue(_wFitsData.getDataMax(), RangeFieldDef.INCLUSIVE);
                        fd.setDefaultValue(def);
                        setValue(def);
                        break;
                    default:
                        break;
                }
                _whichView = which;
            }
        }
    }


     private double getDoubleValue() {
       return Double.parseDouble(_inputField.getTextBox().getText());
     }

     private void setUnits(int idx) {
         _listBox.setSelectedIndex(idx);
         updateFieldDef();
     }

    public void setUnits(Units units) {
        switch (units) {
            case PERCENT:
                setUnits(PERCENT_IDX);
                break;
            case DATA:
                setUnits(ABSOLUTE_IDX);
                break;
            case MINMAX:
                setUnits(MINMAX_IDX);
                break;
            case SIGMA:
                setUnits(SIGMA_IDX);
                break;
            default:
                assert false;
                break;
        }
    }




//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================



// =====================================================================
// -------------------- Inner Classes --------------------------------
// =====================================================================


}
