/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

