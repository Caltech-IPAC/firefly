package edu.caltech.ipac.fuse.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.SpacialType;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.firefly.ui.input.DegreeInputField;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * User: roby
 * Date: Nov 4, 2009
 * Time: 10:01:46 AM
 */


/**
 * @author Trey Roby
*/
abstract class SpatialOps {

    private SpacialBehaviorPanel.HasRangePanel rangePanel;
    private static final WebClassProperties prop = new WebClassProperties(SpatialOps.class);

    public abstract List<Param> getParams();
    public abstract void setParams(List<Param> paramList);
    public abstract boolean validate() throws ValidationException;
    public abstract SpacialType getSpacialType();

    public boolean getRequireUpload() { return false; }
    public boolean getRequireTarget() { return true; }

    protected SpatialOps(SpacialBehaviorPanel.HasRangePanel rangePanel) {
        this.rangePanel = rangePanel;
    }
    protected SpatialOps() { this(null); }

    public void doUpload(AsyncCallback<String> cb) {
        cb.onSuccess("ok");
    }

    public void updateMax(int maxArcSec) {
        if (rangePanel!=null) rangePanel.updateMax(maxArcSec);
    }

    protected List<Param> makeList(Param... initParams) {
        List<Param> l= new ArrayList<Param>(10);
        l.addAll(Arrays.asList(initParams));
        return l;
    }

    protected static Param findParam(List<Param> list, String key)  {
        Param retval= null;
        for(Param p : list) {
            if (p.isKey(key)) {
                retval= p;
                break;
            }
        }
        return retval;
    }

    protected static void updateRadiusField(DegreeInputField df , List<Param> list, String degreeName) {
        Param p= findParam(list, CatalogRequest.RAD_UNITS);
        DegreeFieldDef.Units units= DegreeFieldDef.Units.ARCSEC;
        if (p!=null) {
            try {
                units= Enum.valueOf(DegreeFieldDef.Units.class, p.getValue());
            } catch (Exception e) {
                // do nothing
            }
        }
        df.setUnits(units);

        p= findParam(list, degreeName);
        if (p!=null) {
            df.setValue(p.getValue());
        }
    }


    public static class Cone extends SpatialOps {


        private DegreeInputField degreeField;

        public Cone(DegreeInputField degreeField, SpacialBehaviorPanel.HasRangePanel rangePanel) {
            super(rangePanel);
            this.degreeField = degreeField;
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.Cone; }

        @Override
        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) degreeField.getFieldDef();
            double degreeVal= df.getDoubleValue(degreeField.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.CONE.toString()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.ARCSEC.toString()),
                    new Param(CatalogRequest.RADIUS, valInAS + "")
            );
        }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(degreeField,paramList, CatalogRequest.RADIUS);
        }

        @Override
        public boolean validate() throws ValidationException {
            return degreeField.validate();
        }

    }



    public static class Elliptical extends SpatialOps {
        private final DegreeInputField smAxis;
        private final InputField pa;
        private final InputField ratio;

        public Elliptical(DegreeInputField smAxis,
                          InputField pa,
                          InputField ratio,
                          SpacialBehaviorPanel.HasRangePanel rangePanel) {
            super(rangePanel);
            this.smAxis = smAxis;
            this.pa = pa;
            this.ratio = ratio;
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.Elliptical; }

        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) smAxis.getFieldDef();
            double dVal= df.getDoubleValue( smAxis.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(dVal, DegreeFieldDef.Units.DEGREE);
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.ELIPTICAL.toString()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.ARCSEC.toString()),
                    new Param(CatalogRequest.RADIUS, valInAS+""),
                    new Param(CatalogRequest.PA, pa.getNumberValue().doubleValue()+""),
                    new Param(CatalogRequest.RATIO, ratio.getNumberValue().doubleValue()+"")
            );
        }

        @Override
        public void setParams(List<Param> paramList) {
            Param p;
            updateRadiusField(smAxis,paramList, CatalogRequest.RADIUS);
            p= findParam(paramList, CatalogRequest.PA);
            if (p!=null) pa.setValue(p.getValue());
            p= findParam(paramList, CatalogRequest.RATIO);
            if (p!=null) ratio.setValue(p.getValue());
        }

        @Override
        public boolean validate() throws ValidationException {
            return smAxis.validate() && pa.validate() && ratio.validate();
        }
    }


    public static class Box extends SpatialOps {
        private final DegreeInputField sideField;

        public Box(DegreeInputField sideField, SpacialBehaviorPanel.HasRangePanel rangePanel) {
            super(rangePanel);
            this.sideField = sideField;
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.Box; }

        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) sideField.getFieldDef();
            double dVal= df.getDoubleValue(sideField.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(dVal, DegreeFieldDef.Units.DEGREE);

            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.BOX.toString()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.ARCSEC.toString()),
                    new Param(CatalogRequest.SIZE, valInAS+"")
            );
        }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(sideField,paramList,CatalogRequest.SIZE);
        }

        public boolean validate() throws ValidationException {
            return sideField.validate();
        }
    }



    public static class Polygon extends SpatialOps {

        private final InputField polygonValue;

        public Polygon(InputField polygonValues) {
            this.polygonValue = polygonValues;
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.Polygon; }

        public List<Param> getParams() {
            String fv= polygonValue.getValue();
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.POLYGON.toString()),
                    new Param(CatalogRequest.POLYGON, fv)
                    );
        }

        @Override
        public void setParams(List<Param> paramList) {
            Param p= findParam(paramList, CatalogRequest.POLYGON);
            if (p!=null) polygonValue.setValue(p.getValue());
        }
        public boolean getRequireTarget() { return false; }

        public boolean validate() throws ValidationException {
            return polygonValue.validate();
        }
    }



    public static class TableUpload extends SpatialOps {

        private final DegreeInputField radiusField;
        private FileUploadField uploadField;

        public TableUpload(DegreeInputField radiusField,
                           FileUploadField uploadField,
                           SpacialBehaviorPanel.HasRangePanel rangePanel) {
            super(rangePanel);
            this.radiusField = radiusField;
            this.uploadField = uploadField;
        }


        @Override
        public SpacialType getSpacialType() { return SpacialType.MultiTableUpload; }

        public List<Param> getParams() {

            DegreeFieldDef df = (DegreeFieldDef) radiusField.getFieldDef();
            double degreeVal= df.getDoubleValue(radiusField.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);

            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.TABLE.toString()),
                    new Param(CatalogRequest.FILE_NAME, uploadField.getValue()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.ARCSEC.toString()),
                    new Param(CatalogRequest.RADIUS, valInAS + "")
            );
        }

        @Override
        public void doUpload(AsyncCallback<String> cb) {
            uploadField.submit(cb);
        }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(radiusField,paramList, CatalogRequest.RADIUS);
            Param p= findParam(paramList, CatalogRequest.FILE_NAME);
            if (p!=null) uploadField.setValue(p.getValue());
        }

        @Override
        public boolean getRequireUpload() { return true; }

        public boolean getRequireTarget() { return false; }

        public boolean validate() throws ValidationException {
            if (StringUtils.isEmpty(uploadField.validate())) {
                throw new ValidationException(prop.getError("fileUpload"));
            }
            return true;
        }
   }


    public static class PrevSearch extends SpatialOps { //todo

        private final DegreeInputField radiusField;

        public PrevSearch(DegreeInputField radiusField,
                           SpacialBehaviorPanel.HasRangePanel rangePanel) {
            super(rangePanel);
            this.radiusField = radiusField;
        }

        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) radiusField.getFieldDef();
            double degreeVal= df.getDoubleValue(radiusField.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.TABLE.toString()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.ARCSEC.toString()),
                    new Param(CatalogRequest.RADIUS, valInAS + "")
            );
        }


        @Override
        public SpacialType getSpacialType() { return SpacialType.MultiPrevSearch; }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(radiusField,paramList, CatalogRequest.RADIUS);
        }
        public boolean getRequireTarget() { return false; }

        public boolean validate() throws ValidationException {
            return true;
        }
    }


    public static class MultiPoint extends SpatialOps { //todo

        private final DegreeInputField radiusField;

        public MultiPoint(DegreeInputField radiusField,
                           SpacialBehaviorPanel.HasRangePanel rangePanel) {
            super(rangePanel);
            this.radiusField = radiusField;
        }

        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) radiusField.getFieldDef();
            double degreeVal= df.getDoubleValue(radiusField.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.TABLE.toString()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.ARCSEC.toString()),
                    new Param(CatalogRequest.RADIUS, valInAS + "")
            );
        }


        @Override
        public SpacialType getSpacialType() { return SpacialType.MultiPoints; }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(radiusField,paramList, CatalogRequest.RADIUS);
        }
        public boolean getRequireTarget() { return false; }

        public boolean validate() throws ValidationException {
            return true;
        }
    }




    public static class AllSky extends SpatialOps {

        public List<Param> getParams() {
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.ALL_SKY.toString())
            );
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.AllSky; }

        @Override
        public void setParams(List<Param> paramList) {
            //todo
        }

        public boolean validate() throws ValidationException { return true; }
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
