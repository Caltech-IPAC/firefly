package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.firefly.util.WebAssert;

/**
 * Field definition that allows float input
 * @version $Id: DegreeFieldDef.java,v 1.6 2012/09/27 00:29:12 tatianag Exp $
 */
public class DegreeFieldDef extends DoubleFieldDef {


    private final static double    ARCMIN_TO_DEG = .01666666667;
    private final static double    ARCSEC_TO_DEG = .00027777778;
    private final static double    DEG_TO_ARCMIN = 60.0;
    private final static double    DEG_TO_ARCSEC = 3600.0;
    private final static double    ARCMIN_TO_ARCSEC = 60.0;
    private final static double    ARCSEC_TO_ARCMIN = .01666666667;

    public enum Units {  DEGREE, ARCMIN, ARCSEC }

    private Units currentUnits;   // currently displayed units
    private Units originalUnits;  // original display units
    private Units internalUnits = Units.DEGREE;  // units used by getValue(), setValue()

    public DegreeFieldDef() {}

    public DegreeFieldDef(String name) {
        super(name);
    }

    public void setUnits(Units units) {
        Units oldUnits= this.currentUnits;
        if (oldUnits==null) {
            this.currentUnits= units;
            originalUnits= units;
        }
        else {
            if (oldUnits!=units) {
                this.currentUnits= units;
                setMinValue(convert(oldUnits,units,getMinValue().doubleValue()));
                setMaxValue(convert(oldUnits,units,getMaxValue().doubleValue()));
            }
        }
    }

    public Units getUnits() { return currentUnits; }

    public Units getOriginalUnits() { return originalUnits; }

    public Units getInternalUnits() {return internalUnits; }

    public void setInternalUnits(Units u) { if (u != null) internalUnits = u; }

    public static double getDegreeValue(double v, Units fromUnits) {
        return DegreeFieldDef.convert(fromUnits,Units.DEGREE, v);
    }
    public static double getArcminValue(double v, Units fromUnits) {
        return DegreeFieldDef.convert(fromUnits,Units.ARCMIN, v);
    }
    public static double getArcsecValue(double v, Units fromUnits) {
        return DegreeFieldDef.convert(fromUnits, Units.ARCSEC, v);
    }


    /**
     * convert the passed number to degrees form the units that are currently set in the class
     * @param v the value to convert
     * @return the converted value to degrees
     */
    public double getDegreeValue(double v) {
        return getDegreeValue(v,getUnits());
    }

    /**
     * convert the passed number to arc mins form the units that are currently set in the class
     * @param v the value to convert
     * @return the converted value to arc minutes
     */
    public double getArcminValue(double v) {
        return getArcminValue(v,getUnits());
    }

    /**
     * convert the passed number to arc seconds form the units that are currently set in the class
     * @param v the value to convert
     * @return the converted value to arc seconds
     */
    public double getArcsecValue(double v) {
        return getArcsecValue(v,getUnits());
    }





    public static double convert(Units fromUnits, Units toUnits, double v) {
        double retval;
        if (fromUnits==toUnits) {
            retval= v;
        }
        else if (Double.isNaN(v)) {
            retval= Double.NaN;
        }
        else if (fromUnits==Units.DEGREE && toUnits==Units.ARCMIN) {
            retval= v*DEG_TO_ARCMIN;
        }
        else if (fromUnits==Units.DEGREE && toUnits==Units.ARCSEC) {
            retval= (int)(v*DEG_TO_ARCSEC+0.5); // round to whole arcsec
        }

        else if (fromUnits==Units.ARCSEC && toUnits==Units.DEGREE) {
            retval= v*ARCSEC_TO_DEG;
        }

        else if (fromUnits==Units.ARCSEC && toUnits==Units.ARCMIN) {
            retval= v*ARCSEC_TO_ARCMIN;
        }

        else if (fromUnits==Units.ARCMIN && toUnits==Units.DEGREE) {
            retval= v*ARCMIN_TO_DEG;
        }

        else if (fromUnits==Units.ARCMIN && toUnits==Units.ARCSEC) {
            retval= (int)(v*ARCMIN_TO_ARCSEC+0.5); // round to whole arcsec
        }
        else { // this should never happpen
            retval= v;
            WebAssert.tst(false);
        }

        return retval;
    }

    public void setPrecisionByUnits(Units units) {
        setPrecision(computePrecision(units));
    }

    public int computePrecision(Units units) {
        int retval= 2;
        if (units==Units.DEGREE) {
            retval= 3;
        }
        else if (units==Units.ARCMIN) {
            retval= 2;
        }
        else if (units==Units.ARCSEC) {
            retval= 1;
        }
        else { // this should never happpen
            WebAssert.argTst(false, "invalid argument currentUnits=" +units);
        }
        return retval;
    }

    public static String getUnitDesc(Units unit) {
        String retval;
        switch (unit) {
            case DEGREE :
                retval= " Deg";
                break;
            case ARCSEC :
                retval= "\"";
                break;
            case ARCMIN :
                retval= "'";
                break;
            default :
                retval= "";
                WebAssert.argTst(false, "invalid argument currentUnits=" +unit);
                break;
        }
        return retval;
    }

    public String getErrMsg() {
        String desc= getUnitDesc(getUnits());
        String base= super.getErrMsg();
        String s= base+ ": Min: " + format(getMinValue()) + desc+ ",  Max: " + format(getMaxValue()) + desc;
        return s;
    }


}
