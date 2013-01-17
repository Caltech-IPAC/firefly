package edu.caltech.ipac.heritage.ui;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.*;
import edu.caltech.ipac.firefly.ui.input.CheckBoxGroupInputField;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Date: Apr 20, 2009
 *
 * @author loi
 * @version $Id: InstrumentPanel.java,v 1.43 2012/08/21 21:20:44 tatianag Exp $
 */
public class InstrumentPanel extends Component implements InputFieldGroup {

    private static final String IRAC_MAP = "map,w3,w4,w5,w8";
    private static final String IRAC_POST = "post,w3,w4";

    private static final String IRS_MAP = "map,hi10,hi19,low5,low7,low14,low20,blue,red";
    private static final String IRS_STARE = "stare,hi10,hi19,low5,low7,low14,low20,blue,red";
    private static final String IRS_PU = "image,blue,red";

    private static final String MIPS_PHOTO = "photo,w24,w70,w160";
    private static final String MIPS_SCAN = "scan,w24,w70,w160";
    private static final String MIPS_SED = "sed,w24,w70,w160";
    private static final String MIPS_POWER = "power,w24,w70,w160";

    public static final String IRAC = "InstrumentPanel.field.irac";
    public static final String IRAC_READOUT = "InstrumentPanel.field.irac.readout";
    public static final String IRS = "InstrumentPanel.field.irs";
    public static final String MIPS = "InstrumentPanel.field.mips";
    public static final String MIPS_PHOT_SCALE = "InstrumentPanel.field.mips.phot.scale";
    public static final String MIPS_SCAN_RATE = "InstrumentPanel.field.mips.scan.rate";
    public static final String RADIO_PANEL = "InstrumentPanel.field.panel";
    public static final String WAVE_MIN = "InstrumentPanel.field.min";
    public static final String WAVE_MAX = "InstrumentPanel.field.max";
    public static final String FRAMETIME_MIN = "InstrumentPanel.field.frametime.min";
    public static final String FRAMETIME_MAX = "InstrumentPanel.field.frametime.max";


    public static final String NONE = CheckBoxGroupInputField.NONE;
    public static final String ALL = CheckBoxGroupInputField.ALL;

    final private HorizontalPanel instPanel;
    private MinMaxPanel wlRangePanel;
    private MinMaxPanel ftRangePanel;
    private VerticalPanel optionPanel;

    private SimpleInputField iracField;
    private SimpleInputField iracReadoutModeField;
    private SimpleInputField irsField;
    private SimpleInputField mipsField;
    private SimpleInputField mipsPhotScaleField;
    private SimpleInputField mipsScanRateField;
    private SimpleInputField radioField;
    private String errMsg = "";

    public ArrayList<InputField> fields = null;

    public InstrumentPanel() {

        optionPanel = new VerticalPanel();

        radioField = SimpleInputField.createByProp(RADIO_PANEL);
        radioField.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                showPanel();
            }
        });


        iracField = SimpleInputField.createByProp(IRAC, new SimpleInputField.Config("200px", HorizontalPanel.ALIGN_CENTER));
        iracField.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                checkBoxes(IRAC);
                validateIRAC();
            }
        });

        iracReadoutModeField = SimpleInputField.createByProp(IRAC_READOUT, new SimpleInputField.Config("200px", HorizontalPanel.ALIGN_CENTER));
        iracReadoutModeField.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                checkBoxes(IRAC_READOUT);
                validateIRAC();
            }
        });

        irsField = SimpleInputField.createByProp(IRS, new SimpleInputField.Config("200px", HorizontalPanel.ALIGN_CENTER));
        irsField.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                checkBoxes(IRS);
                validateIRS();
            }
        });


        mipsField = SimpleInputField.createByProp(MIPS, new SimpleInputField.Config("200px", HorizontalPanel.ALIGN_CENTER));
        mipsField.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                checkBoxes(MIPS);
                validateMIPS();
             }
        });

        mipsPhotScaleField = SimpleInputField.createByProp(MIPS_PHOT_SCALE, new SimpleInputField.Config("200px", HorizontalPanel.ALIGN_CENTER));
        mipsPhotScaleField.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                validateMIPS();
             }
        });

        mipsScanRateField = SimpleInputField.createByProp(MIPS_SCAN_RATE, new SimpleInputField.Config("200px", HorizontalPanel.ALIGN_CENTER));
        mipsScanRateField.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                validateMIPS();
             }
        });

        FormBuilder.Config c = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                            100, 5, HorizontalPanel.ALIGN_LEFT);

        wlRangePanel = new MinMaxPanel(WAVE_MIN, WAVE_MAX, c);
        wlRangePanel.setVisible(false);

        ftRangePanel = new MinMaxPanel(FRAMETIME_MIN, FRAMETIME_MAX, c);

        VerticalPanel iracPanel = new VerticalPanel();
        iracPanel.add(iracField);
        iracPanel.add(iracReadoutModeField);

        VerticalPanel mipsPanel = new VerticalPanel();
        mipsPanel.add(mipsField);
        mipsPanel.add(mipsPhotScaleField);
        mipsPanel.add(mipsScanRateField);

        instPanel = new HorizontalPanel();
        instPanel.add(iracPanel);
        instPanel.add(irsField);
        instPanel.add(mipsPanel);

        optionPanel.add(radioField);
        optionPanel.add(instPanel);
        optionPanel.add(wlRangePanel);
        optionPanel.add(ftRangePanel);

        initWidget(optionPanel);
    }


    private void showPanel(){
        if(radioField.getValue().equals("instrument")){
            wlRangePanel.setVisible(false);
            instPanel.setVisible(true);
        } else {
            instPanel.setVisible(false);
            wlRangePanel.setVisible(true);
        }
    }


    private void checkBoxes(String field){
        String selected;
        if(field.equals(IRAC)){
            selected = iracField.getValue();
        }else if(field.equals(IRAC_READOUT)){
            selected = iracReadoutModeField.getValue();
        }else if(field.equals(IRS)){
            selected = irsField.getValue();
        }else if(field.equals(MIPS)){
            selected = mipsField.getValue();
        }else {
            selected = null;
        }

        if(selected != null && selected.length() > 0){
            if(field.equals(IRAC)){
                if (selected.equals(NONE)) {
                    iracReadoutModeField.setValue(NONE);
                } else {
                    if (iracReadoutModeField.getValue().equals(NONE)) {
                        iracReadoutModeField.setValue(ALL);
                    }
                    if (!selected.contains(ALL) && !selected.contains("map") && !selected.contains("post")) {
                        iracField.setValue(NONE);
                        iracReadoutModeField.setValue(NONE);
                    }
                    else if(selected.equals("map") && !selected.contains("w")){
                        iracField.setValue(IRAC_MAP);
                    } else if ((!selected.contains("map") && selected.contains("post"))){
                        if(!selected.equals("post,w3") && !selected.contains("post,w4")){
                            iracField.setValue(IRAC_POST);
                        }
                    }
                }
            } else if (field.equals(IRAC_READOUT)) {
                if(selected.contains("full") || selected.contains("stellar") || selected.contains("hdr") || selected.contains("sub")){
                    if(iracField.getValue().equals(NONE)){
                        iracReadoutModeField.setValue(NONE);
                    }
                }
            } else if(field.equals(IRS)){
                if (!selected.contains(ALL) && !selected.contains("stare") && !selected.contains("map") && !selected.contains("image")) {
                    irsField.setValue(NONE);
                } else if((selected.contains("stare") && !selected.contains("image")) &&
                        (!selected.contains("low") && !selected.contains("hi") && !selected.contains("red") && !selected.contains("blue"))){
                    irsField.setValue(IRS_STARE);
                } else if ((selected.contains("map") && !selected.contains("image"))&&
                        (!selected.contains("low") && !selected.contains("hi") && !selected.contains("red") && !selected.contains("blue"))){
                    irsField.setValue(IRS_MAP);
                } else if ((!selected.contains("stare") && !selected.contains("map")) && selected.contains("image")){
                    if(!selected.equals("image,red") && !selected.equals("image,blue")){
                        irsField.setValue(IRS_PU);
                    }
                } else {
                    //DO NOTHING
                }
            } else if(field.equals(MIPS)){

                if (!selected.contains(ALL) && !selected.contains("photo") && !selected.contains("scan") && !selected.contains("sed") && !selected.contains("power")) {
                    mipsField.setValue(NONE);    
                } else if ((selected.contains("photo") && !selected.contains("scan") && !selected.contains("sed") && !selected.contains("power")) && !selected.contains(",w")){
                    mipsField.setValue(MIPS_PHOTO);
                }else if((!selected.contains("photo") && selected.contains("scan") && !selected.contains("sed") && !selected.contains("power")) && !selected.contains(",w")){
                    mipsField.setValue(MIPS_SCAN);
                }else if((!selected.contains("photo") && !selected.contains("scan") && selected.contains("sed") && !selected.contains("power")) && !selected.contains(",w")){
                    mipsField.setValue(MIPS_SED);
                }else if ((!selected.contains("photo") && !selected.contains("scan") && !selected.contains("sed") && selected.contains("power")) && !selected.contains(",w")){
                    mipsField.setValue(MIPS_POWER);
                } else {
                }

                selected = mipsField.getValue();

                if (selected.contains(ALL) || (selected.contains("photo") && selected.contains("w70"))) {
                    if (!mipsPhotScaleField.isVisible()) {
                        mipsPhotScaleField.setVisible(true);
                        mipsPhotScaleField.setValue(ALL);
                    }
                } else {
                    if (mipsPhotScaleField.isVisible()) {
                        mipsPhotScaleField.setVisible(false);
                        mipsPhotScaleField.setValue(NONE);
                    }
                }
                if (selected.contains(ALL) || selected.contains("scan")) {
                    if (!mipsScanRateField.isVisible()) {
                        mipsScanRateField.setVisible(true);
                        mipsScanRateField.setValue(ALL);
                    }
                } else {
                    if (mipsScanRateField.isVisible()) {
                        mipsScanRateField.setVisible(false);
                        mipsScanRateField.setValue(NONE);
                    }
                }
            }
        }
    }



    private boolean validateIRAC(){
        boolean passed = iracField.validate() && iracReadoutModeField.validate();
        String selected = iracField.getValue();
        if((selected.contains("map") || selected.contains("post")) && !selected.contains(",w")){
            //IRAC checked - no wave checked
            errMsg = "Please select the IRAC wavelength(s) of your choice.";
            iracField.forceInvalid(errMsg);
            passed = false;
        } else if ((!selected.contains("map") && !selected.contains("post")) && selected.contains("w")) {
            //IRAC wave checked - no aot checked
            errMsg =  "Please select the IRAC observing mode.";
            iracField.forceInvalid(errMsg);
            passed = false;
        }
        if (!selected.equals(NONE) && iracReadoutModeField.getValue().equals(NONE)) {
            iracReadoutModeField.forceInvalid("Please, select at least one option");
            passed = false;
        }
        if (!selected.contains("map") && selected.contains("post") && iracReadoutModeField.getValue().equals("stellar")) {
            iracReadoutModeField.forceInvalid("Stellar mode is not supported for IRAC PC");
            passed = false;
        }
        return passed;

    }

    private boolean validateIRS() {
        boolean passed = irsField.validate();
        String selected = irsField.getValue();
        if((selected.contains("stare") || selected.contains("map") || selected.contains("image")) &&
                (!selected.contains("low") && !selected.contains("hi") && !selected.contains("red") && !selected.contains("blue"))){
            //IRS checked - no wave checked
            errMsg = "Please select the IRS wavelength(s) of your choice.";
            irsField.forceInvalid(errMsg);
            passed = false;
        } else if ((!selected.contains("stare") && !selected.contains("map") && !selected.contains("image")) &&
                (selected.contains("low") || selected.contains("hi") || selected.contains("red") || selected.contains("blue"))) {
            //IRS wave checked - no aot checked
            errMsg = "Please select the IRS observing mode.";
            irsField.forceInvalid(errMsg);
            passed = false;
        }
        return passed;
    }

    private boolean validateMIPS() {
        boolean passed = mipsField.validate() && mipsPhotScaleField.validate() && mipsPhotScaleField.validate();
        String selected = mipsField.getValue();
        if((selected.contains("photo") || selected.contains("scan") || selected.contains("sed") || selected.contains("power")) &&
            (!selected.contains(",w"))){
            //MIPS checked - no wave checked
            errMsg = "Please select the MIPS wavelength(s) of your choice.";
            mipsField.forceInvalid(errMsg);
            passed = false;
        } else if ((!selected.contains("photo") && !selected.contains("scan") && !selected.contains("sed") && !selected.contains("power")) &&
            (selected.contains("w24") || selected.contains("w70") || selected.contains("w160"))) {
            //MIPS wave checked - no aot checked
            errMsg = "Please select the MIPS observing mode.";
            mipsField.forceInvalid(errMsg);
            passed = false;
        }
        if ((selected.equals(ALL) || selected.contains("photo") && selected.contains("70")) && mipsPhotScaleField.getValue().equals(NONE)) {
            mipsPhotScaleField.forceInvalid("Please select scale");
            passed = false;
        }
        if ((selected.equals(ALL) || selected.contains("scan")) && mipsScanRateField.getValue().equals(NONE)) {
            mipsScanRateField.forceInvalid("Please select scan rate");
            passed = false;
        }
        return passed;
    }


    public String getError(){
        return errMsg;
    }

    public String getRadioValue(){
        return radioField.getValue();
    }

    private boolean hasDefaultValue(InputField f) {
        return f.getValue().equals(f.getFieldDef().getDefaultValueAsString());
    }

//
//  HasWidgets implementation delegate to VerticalPanel
//

    public void add(Widget w) {
        optionPanel.add(w);
    }

    public void clear() {
        optionPanel.clear();
    }

    public Iterator<Widget> iterator() {
        return optionPanel.iterator();
    }

    public boolean remove(Widget w) {
        return optionPanel.remove(w);
    }

//
//  InputFieldGroup implementation
//

    public List<Param> getFieldValues() {
        ArrayList<Param> rval = new ArrayList<Param>();

        String selection = radioField.getValue();
        if (selection.equals("instrument")) {
            String iracValue = iracField.getValue();
            String mipsValue = mipsField.getValue();
            String irsValue = irsField.getValue();
            boolean nofilter = (iracValue.equals(ALL) && mipsValue.equals(ALL) && irsValue.equals(ALL)) ||
                  (iracValue.equals(NONE) && mipsValue.equals(NONE) && irsValue.equals(NONE));
            if (!nofilter) {
                doPopulateRequest(rval, iracField.getField(), mipsField.getField(), irsField.getField(), radioField.getField());
            }

            // irac readout
            if (!iracValue.equals(NONE)) {
                String readoutValue = iracReadoutModeField.getValue();
                nofilter = (readoutValue.contains("full") && readoutValue.contains("sub")) || readoutValue.equals(ALL) ;
                if (!nofilter) {
                    doPopulateRequest(rval, iracReadoutModeField.getField());
                }
            }

            // mips 70um scale
            if (mipsValue.equals(ALL) || (mipsValue.contains("photo") && mipsValue.contains("70"))) {
                String scaleValue = mipsPhotScaleField.getValue();
                if (!scaleValue.equals("fine,default") && !scaleValue.equals(ALL)) {
                    doPopulateRequest(rval, mipsPhotScaleField.getField());
                }
            }

            // mips scan rate
            if ( mipsValue.equals(ALL) || mipsValue.contains("scan")) {
                String scanRateValue = mipsScanRateField.getValue();
                if (!scanRateValue.equals("fast,medium,slow") && !scanRateValue.equals(ALL)) {
                    doPopulateRequest(rval, mipsScanRateField.getField());
                }
            }

        } else if (selection.equals("wavelength")) {
            InputField minField = wlRangePanel.getMinField();
            InputField maxField = wlRangePanel.getMaxField();
            boolean nofilter = hasDefaultValue(minField) && hasDefaultValue(maxField);
            if (!nofilter) {
                doPopulateRequest(rval, minField, maxField, radioField.getField());
            }
        }

        InputField minFrametimeField = ftRangePanel.getMinField();
        InputField maxFrametimeField = ftRangePanel.getMaxField();
        boolean nofilter = hasDefaultValue(minFrametimeField) && hasDefaultValue(maxFrametimeField);
        if (!nofilter) {
            doPopulateRequest(rval, minFrametimeField, maxFrametimeField);    
        }
        return rval;
    }

    private void doPopulateRequest(List<Param> values, InputField ... flds) {
        for (InputField f : flds) {
            String v = f.getValue();
            if (!StringUtils.isEmpty(v)) {
                values.add(new Param(f.getFieldDef().getName(), v));
            }
        }
    }

    public void setFieldValues(List<Param> list) {
        if (fields == null) {
            fields = new ArrayList<InputField>(10);
            FormUtil.getAllChildFields(optionPanel, fields);
        }

        for(InputField f : fields) {
            Param param = GwtUtil.findParam(list, f.getFieldDef().getName());
            if (param != null) {
                f.setValue(param.getValue());
            } else {
                f.reset();
            }
        }
        // make sure selections set as default are valid
        checkBoxes(IRAC);
        checkBoxes(IRS);
        checkBoxes(MIPS);
    }

    public boolean validate(){
        if (GwtUtil.isOnDisplay(wlRangePanel)) {
            return ftRangePanel.validate() && wlRangePanel.validate();
        } else {
            return ftRangePanel.validate() && validateIRAC() && validateIRS() && validateMIPS();
        }
    }

    public static boolean isIrsEnhancedRequested(Request req) {
        String sel = req.getParam(IRS);
        return sel == null || sel.contains(ALL) ||
                (sel.contains("stare") &&
                 (sel.contains("low5") || sel.contains("low7") || sel.contains("low14") || sel.contains("low20")));
    }

    public static boolean isSupermosaicRequested(Request req) {
        String selm = req.getParam(MIPS);
        String seli = req.getParam(IRAC);

        return selm==null || selm.contains(ALL) ||
                (selm.contains("photo") && selm.contains("w24")) ||
                (seli.contains("map") || seli.contains("post"));
    }


    /*
        Explicitly means subarray is the only readout mode selected
     */
    public static boolean isIracSubarrayExplicitlyRequested(Request req) {
        String irac = req.getParam(IRAC);
        if (!StringUtils.isEmpty(irac) && !irac.equals(NONE)) {
            String iracReadout = req.getParam(IRAC_READOUT);
            if (iracReadout.equals("sub")) { return true; }
        }
        return false;
    }

    public static boolean isMipsRequested(Request req) {
        String mips = req.getParam(MIPS);
        return (!StringUtils.isEmpty(mips) && !mips.equals(NONE));
    }

    public static boolean isIrsRequested(Request req) {
        String irs = req.getParam(IRS);
        return (!StringUtils.isEmpty(irs) && !irs.equals(NONE));
    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/