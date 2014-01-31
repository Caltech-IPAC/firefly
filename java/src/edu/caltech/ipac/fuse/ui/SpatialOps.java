package edu.caltech.ipac.fuse.ui;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;

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

    public abstract List<Param> getParams();
    public abstract void setParams(List<Param> paramList);
    public boolean getRequireUpload() { return false; }

    protected List<Param> makeList(Param... initParams) {
        List<Param> l= new ArrayList<Param>(10);
        l.addAll(Arrays.asList(initParams));
        return l;
    }



    public static class Cone extends SpatialOps {


        InputField degreeField;

        public Cone(InputField degreeField) {
            this.degreeField = degreeField;
        }

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
            //todo
        }
    }



    public static class Elliptical extends SpatialOps {
        private final InputField smAxis;
        private final InputField pa;
        private final InputField ratio;

        public Elliptical(InputField smAxis, InputField pa, InputField ratio) {
            this.smAxis = smAxis;
            this.pa = pa;
            this.ratio = ratio;
        }

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
            //todo
        }

    }


    public static class Box extends SpatialOps {
        private final InputField sideField;

        public Box(InputField sideField) {
            this.sideField = sideField;
        }

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
            //todo
        }
    }



    public static class Polygon extends SpatialOps {

        private final InputField polygonValue;

        public Polygon(InputField polygonValues) {
            this.polygonValue = polygonValues;
        }

        public List<Param> getParams() {
            String fv= polygonValue.getValue();
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.POLYGON.toString()),
                    new Param(CatalogRequest.POLYGON, fv)
                    );
        }

        @Override
        public void setParams(List<Param> paramList) {
            //todo
        }
    }



    public static class TableUpload extends SpatialOps {

        private final SimpleInputField radiusField;
        private FileUploadField uploadField;

        public TableUpload(SimpleInputField radiusField, FileUploadField uploadField) {
            this.radiusField = radiusField;
            this.uploadField = uploadField;
        }

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
        public void setParams(List<Param> paramList) {
            //todo
        }

        @Override
        public boolean getRequireUpload() { return true; }

   }


    public static class PrevSearch extends SpatialOps { //todo


        public PrevSearch() { }

        public List<Param> getParams() {
            return makeList();
        }

        @Override
        public void setParams(List<Param> paramList) {
            //todo
        }
    }

    public static class MultiPoint extends SpatialOps { //todo


        public MultiPoint() {
        }

        public List<Param> getParams() {
            return makeList();
        }

        @Override
        public void setParams(List<Param> paramList) {
            //todo
        }
    }


    public static class AllSky extends SpatialOps {

        public List<Param> getParams() {
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.ALL_SKY.toString())
            );
        }

        @Override
        public void setParams(List<Param> paramList) {
            //todo
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
