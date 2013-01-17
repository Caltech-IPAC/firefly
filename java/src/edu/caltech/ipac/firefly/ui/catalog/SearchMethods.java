package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldCreator;
import edu.caltech.ipac.firefly.ui.input.InputFieldPanel;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.ValidationInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.util.StringUtils;
/**
 * User: roby
 * Date: Nov 4, 2009
 * Time: 10:01:46 AM
 */


/**
 * @author Trey Roby
*/
abstract class SearchMethods {

    private static final WebClassProperties _prop= new WebClassProperties(SearchMethods.class);
    private static String RANGES_STR= _prop.getName("ranges");



    public abstract Widget makePanel();
    public abstract void setIntoRequest(CatalogRequest req);
    public abstract boolean validate() throws ValidationException;
    public void updateMax(int max) {}
    public int getHeight() { return 75; }
    public int getWidth() { return 300; }
    public boolean usesTarget() { return true; }
    public boolean isImplemented() { return true; }
    public boolean getRequireUpload() { return false; }
    public void upload(AsyncCallback<String> cb) { }



    protected static void computeRangesLabel(InputField field, Label rangesLabel) {
        DegreeFieldDef df = (DegreeFieldDef)field.getFieldDef();
        DegreeFieldDef.Units currentUnits= df.getUnits();
        String unitDesc= DegreeFieldDef.getUnitDesc(currentUnits);

        double min= df.getMinValue().doubleValue();
        double max= df.getMaxValue().doubleValue();

//        WebAssert.tst(minDeg!=maxDeg, "problem in computing ranges: minDeg="+
//                minDeg+" maxDeg= " + maxDeg);
        if (max>min) {
//            double min= DegreeFieldDef.convert(DegreeFieldDef.Units.DEGREE,
//                                               currentUnits, minDeg);
//            double max= DegreeFieldDef.convert(DegreeFieldDef.Units.DEGREE,
//                                               currentUnits, maxDeg);

            rangesLabel.setText(RANGES_STR +" "+ df.format(min) +unitDesc +
                    " and " + df.format(max) + unitDesc);
        }

    }


    protected static VerticalPanel makeRangePanel(final SimpleInputField field, final Label rangesLabel) {
        VerticalPanel vp= new VerticalPanel();
        rangesLabel.addStyleName("on-dialog-help");
        vp.add(field);
        vp.add(GwtUtil.centerAlign(rangesLabel));
        DOM.setStyleAttribute(field.getElement(), "paddingTop", "7px");
        field.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                computeRangesLabel(field.getField(), rangesLabel);
            }
        });
        computeRangesLabel(field.getField(),rangesLabel);
        return vp;
    }

    private static void addPadding(Widget w) {
        DOM.setStyleAttribute(w.getElement(), "padding", "30px 0px 0px 20px");
    }

    protected static void updateMax(InputField field, double maxArcSec) {

        DegreeFieldDef df = (DegreeFieldDef)field.getFieldDef();
        DegreeFieldDef.Units currentUnits= df.getUnits();

        double max= DegreeFieldDef.convert(DegreeFieldDef.Units.ARCSEC,
                                           currentUnits, maxArcSec);
        df.setMaxValue(max);
    }


//====================================================================
//-------------------- Search Method Inner Classes -------------------
//====================================================================


    public static class Cone extends SearchMethods {
        private static final SimpleInputField _field = SimpleInputField.createByProp(_prop.makeBase("radius"));
        private final Label _rangesLabel= new Label();

        public Widget makePanel() {
            return makeRangePanel(_field, _rangesLabel);
        }

        public static SimpleInputField getField(){
            return _field;
        }

        public void setIntoRequest(CatalogRequest req) {
            req.setMethod(CatalogRequest.Method.CONE);

            DegreeFieldDef df = (DegreeFieldDef) _field.getFieldDef();
            double degreeVal= df.getDoubleValue( _field.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);
            req.setRadius(valInAS);
            req.setRadUnits(CatalogRequest.RadUnits.ARCSEC);
        }

        public void updateMax(int max) {
            updateMax(_field.getField(),(double)max);
            computeRangesLabel(_field.getField(),_rangesLabel);
        }

        public boolean validate() throws ValidationException {
            return _field.validate();
        }
    }



    public static class Elliptical extends SearchMethods {
        private static final SimpleInputField _smAxis = SimpleInputField.createByProp(_prop.makeBase("smaxis"),new SimpleInputField.Config("180px"));
        private static InputField _pa;
        private static InputField _ratio;


        private final Label _rangesLabel= new Label();

        public Widget makePanel() {


            InputFieldPanel ifPanel= new InputFieldPanel(180);
            _pa= InputFieldCreator.createFieldWidget(_prop.makeBase("pa"));
            _ratio= InputFieldCreator.createFieldWidget(_prop.makeBase("axialratio"));

            _pa = new ValidationInputField(_pa);
            _ratio = new ValidationInputField(_ratio);
            

            ifPanel.addUserField(_pa, HorizontalPanel.ALIGN_LEFT);
            ifPanel.addUserField(_ratio, HorizontalPanel.ALIGN_LEFT);


            VerticalPanel panel= new VerticalPanel();
            VerticalPanel smAxisPanel= makeRangePanel(_smAxis, _rangesLabel);
            panel.add(smAxisPanel);
            panel.add(ifPanel);
//            panel.add(_pa);
//            panel.add(_ratio);

//            DOM.setStyleAttribute(_pa.getElement(), "paddingTop", "10px");
            return panel;
        }

        public static SimpleInputField getAxisField(){
            return _smAxis;
        }

        public static InputField getPaField(){
            return _pa;
        }

        public static InputField getRatioField(){
            return _ratio; 
        }

        public void setIntoRequest(CatalogRequest req) {
            req.setMethod(CatalogRequest.Method.ELIPTICAL);

            DegreeFieldDef df = (DegreeFieldDef) _smAxis.getFieldDef();
            double dVal= df.getDoubleValue( _smAxis.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(dVal, DegreeFieldDef.Units.DEGREE);
            req.setRadius(valInAS);
            req.setRadUnits(CatalogRequest.RadUnits.ARCSEC);
            req.setPA(_pa.getNumberValue().doubleValue());
            req.setRatio(_ratio.getNumberValue().doubleValue());

        }


        public void updateMax(int max) {
            updateMax(_smAxis.getField(),(double)max);
            computeRangesLabel(_smAxis.getField(),_rangesLabel);
        }

        public boolean validate() throws ValidationException {
            return _smAxis.validate() && _pa.validate() && _ratio.validate();
        }
        public int getHeight() { return 140; }
    }


    public static class Box extends SearchMethods {
        private final SimpleInputField _field = SimpleInputField.createByProp(_prop.makeBase("side"));
        private final Label _rangesLabel= new Label();

        public Widget makePanel() {
            return makeRangePanel(_field,_rangesLabel);
        }

        public void setIntoRequest(CatalogRequest req) {
            req.setMethod(CatalogRequest.Method.BOX);

            DegreeFieldDef df = (DegreeFieldDef) _field.getFieldDef();
            double dVal= df.getDoubleValue( _field.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(dVal, DegreeFieldDef.Units.DEGREE);
            req.setSide(valInAS);
            req.setRadUnits(CatalogRequest.RadUnits.ARCSEC);
        }

        public void updateMax(int max) {
            updateMax(_field.getField(),(double)max*2);
            computeRangesLabel(_field.getField(),_rangesLabel);
        }

        public boolean validate() throws ValidationException {
            return _field.validate();
        }
    }



    public static class Polygon extends SearchMethods {

        private static final SimpleInputField _field = SimpleInputField.createByProp(_prop.makeBase("poly"));

        public Widget makePanel() {
            VerticalPanel vp= new VerticalPanel();
            vp.add(_field);
            vp.add(new HTML("- Each vertex is defined by a J2000 RA and Dec position pair <br>" +
                    "- A max of 15 and min of 3 vertices is allowed <br>" +
                    "- Vertices must be separated by a comma (,) <br>" +
                    "- Example: 20.7 21.5, 20.5 20.5, 21.5 20.5, 21.5 21.5"));
            //addPadding(vp);
            DOM.setStyleAttribute(vp.getElement(), "padding", "10px 0px 0px 10px");

            return vp;
        }

        public static SimpleInputField getField(){
            return _field;
        }

        public void setIntoRequest(CatalogRequest req) {
            req.setMethod(CatalogRequest.Method.POLYGON);
            req.setPolygon(_field.getValue());
        }

        public boolean validate() throws ValidationException {
            return _field.validate();
        }
        public boolean usesTarget() { return false; }
        public int getHeight() { return 240; }
        public int getWidth() { return 350; }
    }



    public static class Table extends SearchMethods {

        private final SimpleInputField _field = SimpleInputField.createByProp(_prop.makeBase("radius"));
        private final Label _rangesLabel= new Label();
        private FileUploadField _uploadField;
        private String _cacheKey;

        public Widget makePanel() {
            SimpleInputField field = SimpleInputField.createByProp(_prop.makeBase("upload"));
            VerticalPanel vp= new VerticalPanel();
            _uploadField= (FileUploadField)field.getField();
            VerticalPanel panel= makeRangePanel(_field, _rangesLabel);

            vp.add(field);
            vp.add(panel);

            return vp;
        }

        public void setIntoRequest(CatalogRequest req) {
            req.setMethod(CatalogRequest.Method.TABLE);
            req.setFileName(_uploadField.getValue());

            DegreeFieldDef df = (DegreeFieldDef) _field.getFieldDef();
            double degreeVal= df.getDoubleValue( _field.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);
            req.setRadius(valInAS);
            req.setRadUnits(CatalogRequest.RadUnits.ARCSEC);
            
        }

        public boolean validate() throws ValidationException {
            if (StringUtils.isEmpty(_uploadField.validate())) {
                throw new ValidationException(_prop.getError("fileUpload"));
            }
            return true;
        }

        public boolean isImplemented() { return true; }
        public boolean usesTarget() { return false; }

        @Override
        public boolean getRequireUpload() { return true; }

        @Override
        public void upload(AsyncCallback<String> postCommand) {
            _uploadField.submit(postCommand);
        }
        public int getHeight() { return 130; }
        public int getWidth() { return 300; }

   }





    public static class AllSky extends SearchMethods {

        public Widget makePanel() {
            Label label= new Label(_prop.getName("allskyDesc"));
            addPadding(label);
            return label;
        }

        public void setIntoRequest(CatalogRequest req) {
            req.setMethod(CatalogRequest.Method.ALL_SKY);
        }

        public boolean  validate() throws ValidationException { return true; }
        public boolean usesTarget() { return false; }
    }


}






/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
