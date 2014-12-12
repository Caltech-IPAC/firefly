package edu.caltech.ipac.util.dd;
/**
 * User: roby
 * Date: 2/8/13
 * Time: 2:00 PM
 */


/**
 * @author Trey Roby
 */
public class RegionValue {

    public enum Unit {CONTEXT, DEGREE, ARCSEC, ARCMIN, RADIANS, SCREEN_PIXEL, IMAGE_PIXEL, UNKNOWN}


    private double value;
    private Unit unit;

    public RegionValue(double value, Unit unit)  {
        this.value= value;
        this.unit = unit;
    }


    public double getValue() { return value; }
    public Unit getType() { return unit; }


    public boolean isWorldCoords() {
        return (unit!=Unit.SCREEN_PIXEL &&
                unit!=Unit.IMAGE_PIXEL &&
                unit!=Unit.UNKNOWN) &&
                unit!=Unit.CONTEXT; }

    public double toDegree() {
        if (!isWorldCoords()) {
            throw new IllegalArgumentException("Can't convert to degrees");
        }

        double retval= value;
        switch(unit)
        {
            case ARCSEC:
                retval = value / 3600;
                break;
            case ARCMIN:
                retval = value / 60;
                break;
            case DEGREE:
                retval= value;
                break;
            case RADIANS:
                retval = value * 57.295779;
                unit= RegionValue.Unit.RADIANS;
                break;
        }
        return retval;
    }

    public String toString() {
        String uStr;
        switch (unit) {
            case CONTEXT:      uStr= "";   break;
            case DEGREE:       uStr= "d";  break;
            case ARCSEC:       uStr= "\""; break;
            case ARCMIN:       uStr= "'";  break;
            case RADIANS:      uStr= "r";  break;
            case SCREEN_PIXEL: uStr= "p";  break;
            case IMAGE_PIXEL:  uStr= "i";  break;
            case UNKNOWN:      uStr= "";   break;
            default:           uStr= "";   break;
        }
        return value+uStr;
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
