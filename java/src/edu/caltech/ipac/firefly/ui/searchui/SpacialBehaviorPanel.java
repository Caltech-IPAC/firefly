package edu.caltech.ipac.firefly.ui.searchui;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.DegreeInputField;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldCreator;
import edu.caltech.ipac.firefly.ui.input.InputFieldPanel;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.ValidationInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;
/**
 * User: roby
 * Date: Nov 4, 2009
 * Time: 10:01:46 AM
 */


/**
 * @author Trey Roby
*/
abstract class SpacialBehaviorPanel {

    private enum InVal { CENTER, COVERS, ENCLOSED, OVERLAPS}

    public interface HasRangePanel {
        public void updateMax(int maxArcSec);
    }

    private static final WebClassProperties prop = new WebClassProperties(SpacialBehaviorPanel.class);
    private static String RANGES_STR= prop.getName("ranges");
    public abstract Widget makePanel();
    public int getHeight() { return 75; }
    public int getWidth() { return 300; }




    protected static FlowPanel makeRangePanel(final SimpleInputField field, final Label rangesLabel, int paddingTop ) {
        FlowPanel fp= new FlowPanel();
        rangesLabel.addStyleName("on-dialog-help");
        fp.add(field);
        fp.add(rangesLabel);
        GwtUtil.setStyles(rangesLabel, "marginLeft", "auto", "marginRight", "auto");
        GwtUtil.setPadding(field,paddingTop,0,0,0);
//        fp.add(GwtUtil.centerAlign(rangesLabel));
//        DOM.setStyleAttribute(field.getElement(), "paddingTop", "7px");
        field.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                computeRangesLabel(field.getField(), rangesLabel);
            }
        });
        computeRangesLabel(field.getField(),rangesLabel);
        return fp;
    }


    protected static void computeRangesLabel(InputField field, Label rangesLabel) {
        DegreeFieldDef df = (DegreeFieldDef)field.getFieldDef();
        DegreeFieldDef.Units currentUnits= df.getUnits();
        String unitDesc= DegreeFieldDef.getUnitDesc(currentUnits);
        double min= df.getMinValue().doubleValue();
        double max= df.getMaxValue().doubleValue();

        if (max>min) {
            rangesLabel.setText(RANGES_STR +" "+ df.format(min) +unitDesc +
                                        " and " + df.format(max) + unitDesc);
        }

    }

    protected static void updateMaxRangeField(InputField field, int maxArcSec) {

        DegreeFieldDef df = (DegreeFieldDef)field.getFieldDef();
        DegreeFieldDef.Units currentUnits= df.getUnits();

        double max= DegreeFieldDef.convert(DegreeFieldDef.Units.ARCSEC,
                                           currentUnits, maxArcSec);
        df.setMaxValue(max);
    }


//====================================================================
//-------------------- Search Method Inner Classes -------------------
//====================================================================


    public static class Cone extends SpacialBehaviorPanel implements HasRangePanel {
        private final SimpleInputField field = SimpleInputField.createByProp(prop.makeBase("radius"));
        private final Label rangesLabel= new Label();

        public Widget makePanel() {
            return makeRangePanel(field, rangesLabel, 20);
        }

        public DegreeInputField getField(){ return (DegreeInputField)((ValidationInputField)field.getField()).getIF(); }

        public void updateMax(int maxArcSec) {
            updateMaxRangeField(field.getField(), maxArcSec);
            computeRangesLabel(field.getField(),rangesLabel);
        }

    }

    //todo
    public static class IbeSingle extends SpacialBehaviorPanel implements HasRangePanel {

//        private final InputField intersect= InputFieldCreator.createFieldWidget(prop.makeBase("ibe.intersect"));
//        private final InputField size=      InputFieldCreator.createFieldWidget(prop.makeBase("ibe.size"));
//        private final InputField subSize=   InputFieldCreator.createFieldWidget(prop.makeBase("ibe.subsize"));
//        private final InputField mCenter=   InputFieldCreator.createFieldWidget(prop.makeBase("ibe.mcenter"));


        private SimpleInputField intersect;
        private SimpleInputField size;
        private SimpleInputField subSize;
        private SimpleInputField mCenter;




        public Widget makePanel() {
            SimpleInputField.Config labConfig= new SimpleInputField.Config("300px");
            intersect= SimpleInputField.createByProp(prop.makeBase("ibe.intersect"),labConfig);
            size=      SimpleInputField.createByProp(prop.makeBase("ibe.size"),labConfig);
            subSize=   SimpleInputField.createByProp(prop.makeBase("ibe.subsize"),labConfig);
            mCenter=   SimpleInputField.createByProp(prop.makeBase("ibe.mcenter"),labConfig);
            GwtUtil.setStyle(intersect, "margin", "0 auto 0 0");
            GwtUtil.setStyle(size, "margin", "0 auto 0 0");
            GwtUtil.setStyle(subSize, "margin", "0 auto 0 0");
            GwtUtil.setStyle(mCenter, "margin", "0 auto 0 0");
            FlowPanel panel= new FlowPanel();
            panel.add(intersect);
            panel.add(size);
            panel.add(subSize);
            panel.add(mCenter);

            intersect.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(ValueChangeEvent<String> ev) {
                    updateDisplay();
                }
            });
            updateDisplay();

            return panel;
        }

        private void updateDisplay() {
            InVal v = StringUtils.getEnum(intersect.getValue(), InVal.CENTER);
            boolean subSizeHidden= true;
            boolean sizeHidden= true;
            boolean mCenterHidden= true;
            switch (v) {
                case CENTER:
                    subSizeHidden= false;
                    mCenterHidden= false;
                    break;
                case COVERS:
                    subSizeHidden= false;
                    mCenterHidden= false;
                    sizeHidden= false;
                    break;
                case ENCLOSED:
                case OVERLAPS:
                    sizeHidden= false;
                    break;
            }
            GwtUtil.setHidden(size, sizeHidden);
            GwtUtil.setHidden(subSize, subSizeHidden);
            GwtUtil.setHidden(mCenter, mCenterHidden);
        }


        public InputField getIntersect() { return intersect.getField(); }
        public InputField getSize() { return size.getField(); }
        public InputField getSubSize() { return subSize.getField(); }
        public InputField getMCenter() { return mCenter.getField(); }


        public void updateMax(int maxArcSec) {
        }

    }

    public static class IbeTableUpload extends SpacialBehaviorPanel {

        private FileUploadField _uploadField;
        private IbeSingle single= new IbeSingle();

        public Widget makePanel() {
            SimpleInputField field = SimpleInputField.createByProp(prop.makeBase("upload"));
            FlowPanel fp= new FlowPanel();
            _uploadField= (FileUploadField)field.getField();
            GwtUtil.setPadding(_uploadField,20,0,0,0);
            Widget singlePanel= single.makePanel();
            GwtUtil.setPadding(singlePanel,20,0,0,0);

            fp.add(_uploadField);
            fp.add(singlePanel);

            return fp;
        }
        public InputField getIntersect() { return single.getIntersect(); }
        public InputField getSize() { return single.getSize(); }
        public InputField getSubSize() { return single.getSubSize(); }
        public InputField getMCenter() { return single.getMCenter(); }

        public FileUploadField getUploadField(){ return _uploadField; }

        public void upload(AsyncCallback<String> postCommand) {
            _uploadField.submit(postCommand);
        }

    }


    public static class Elliptical extends SpacialBehaviorPanel implements HasRangePanel {
        private final SimpleInputField smAxis = SimpleInputField.createByProp(prop.makeBase("smaxis"), new SimpleInputField.Config("180px"));
        private InputField _pa;
        private InputField _ratio;


        private final Label _rangesLabel= new Label();

        public Widget makePanel() {


            InputFieldPanel ifPanel= new InputFieldPanel(121);
            _pa= InputFieldCreator.createFieldWidget(prop.makeBase("pa"));
            _ratio= InputFieldCreator.createFieldWidget(prop.makeBase("axialratio"));

            _pa = new ValidationInputField(_pa);
            _ratio = new ValidationInputField(_ratio);

            GwtUtil.setStyle(_rangesLabel, "padding", "0 0 4px 40px");

            ifPanel.addUserField(_pa, HorizontalPanel.ALIGN_LEFT);
            ifPanel.addUserField(_ratio, HorizontalPanel.ALIGN_LEFT);


            VerticalPanel panel= new VerticalPanel();
            FlowPanel smAxisPanel= makeRangePanel(smAxis, _rangesLabel, 3);
            panel.add(smAxisPanel);
            panel.add(ifPanel);
            return panel;
        }

        public DegreeInputField getAxisField(){
            return (DegreeInputField)((ValidationInputField)smAxis.getField()).getIF();
        }

        public InputField getPaField(){
            return _pa;
        }

        public InputField getRatioField(){
            return _ratio; 
        }

        public void updateMax(int maxArcSec) {
            updateMaxRangeField(smAxis.getField(), maxArcSec);
            computeRangesLabel(smAxis.getField(),_rangesLabel);
        }

        public int getHeight() { return 140; }
    }


    public static class Box extends SpacialBehaviorPanel implements HasRangePanel {
        private final SimpleInputField field = SimpleInputField.createByProp(prop.makeBase("side"));
        private final Label rangesLabel= new Label();

        public Widget makePanel() {
            return makeRangePanel(field,rangesLabel, 20);
        }

        public DegreeInputField getField(){ return (DegreeInputField)((ValidationInputField)field.getField()).getIF(); }

        public void updateMax(int maxArcSec) {
            updateMaxRangeField(field.getField(), maxArcSec * 2);
            computeRangesLabel(field.getField(),rangesLabel);
        }

    }


    public static class Polygon extends SpacialBehaviorPanel {

        private final SimpleInputField _field = SimpleInputField.createByProp(prop.makeBase("poly"));

        public Widget makePanel() {
            VerticalPanel vp= new VerticalPanel();
            vp.add(_field);
            _field.getField().getFocusWidget().setSize("400px", "60px");
            vp.add(new HTML("- Each vertex is defined by a J2000 RA and Dec position pair <br>" +
                             "- A max of 15 and min of 3 vertices is allowed <br>" +
                             "- Vertices must be separated by a comma (,) <br>" +
                             "- Example: 20.7 21.5, 20.5 20.5, 21.5 20.5, 21.5 21.5"));
            DOM.setStyleAttribute(vp.getElement(), "padding", "10px 0px 0px 10px");

            return vp;
        }

        public InputField getPolygonField(){ return _field.getField(); }

        public boolean validate() throws ValidationException {
            return _field.validate();
        }
        public int getHeight() { return 240; }
        public int getWidth() { return 350; }
    }


    public static class TableUpload extends SpacialBehaviorPanel {

        private FileUploadField _uploadField;

        public Widget makePanel() {
            SimpleInputField field = SimpleInputField.createByProp(prop.makeBase("upload"));
            FlowPanel fp= new FlowPanel();
            _uploadField= (FileUploadField)field.getField();
            GwtUtil.setPadding(_uploadField,20,0,0,0);

            fp.add(_uploadField);
            return fp;
        }

        public FileUploadField getUploadField(){ return _uploadField; }

        public void upload(AsyncCallback<String> postCommand) {
            _uploadField.submit(postCommand);
        }

    }


    public static class PrevSearch extends SpacialBehaviorPanel {
        public Widget makePanel() {
            Label l= new Label("search using previous search from workspace goes where");
            GwtUtil.setPadding(l,20,0,0,0);
            return l;
        }

        public boolean validate() throws ValidationException {
            return true;
        }
    }

    public static class UserEnteredCoords extends SpacialBehaviorPanel {
        public Widget makePanel() {
            Label l= new Label("put user enter coords ui here");
            GwtUtil.setPadding(l,20,0,0,0);
            return l;
        }

        public boolean validate() throws ValidationException {
            return true;
        }
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
