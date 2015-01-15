/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
/**
 * User: roby
 * Date: Sep 11, 2009
 * Time: 10:27:38 AM
 */


/**
 * @author Trey Roby
 */
public class WebFitsData implements Serializable {

    private final static String SPLIT_TOKEN= "--WebFitsData--";

    private double _dataMin;
    private double _dataMax;
    private long   _fitsFileSize;
    private String _fluxUnits;

    public WebFitsData() {}

    public WebFitsData(double dataMin,
                       double dataMax,
                       long   fitsFileSize,
                       String fluxUnits ) {
        _dataMin= dataMin;
        _dataMax= dataMax;
        _fitsFileSize= fitsFileSize;
        _fluxUnits= fluxUnits;

    }

    public double getDataMin()      { return _dataMin; }
    public double getDataMax()      { return _dataMax; }

    /**
     * Return the size of the fits file the this image was created from
     * @return size of the file
     */
    public long   getFitsFileSize() { return _fitsFileSize; }
    
    /**
     * get units that this flux data is in.
     * @return String the units.
     */
    public String getFluxUnits() { return _fluxUnits; }

    @Override
    public String toString() {
        return _dataMin + SPLIT_TOKEN +
               _dataMax + SPLIT_TOKEN +
               _fitsFileSize + SPLIT_TOKEN +
               _fluxUnits;
    }

    public static WebFitsData parse(String s) {
        if (s==null) return null;
        String sAry[]= s.split(SPLIT_TOKEN,5);
        WebFitsData retval= null;
        if (sAry.length==4) {
            try {
                int i= 0;
                double dataMin= StringUtils.getDouble(sAry[i++]);
                double dataMax= StringUtils.getDouble(sAry[i++]);
                long fitsFileSize= Long.parseLong(sAry[i++]);
                String fluxUnits= getString(sAry[i]);;
                retval= new WebFitsData(dataMin,dataMax,fitsFileSize,fluxUnits);
            } catch (NumberFormatException e) {
                retval= null;
            }
        }
        return retval;

    }

    private static String getString(String s) { return s.equals("null") ? null : s; }
}

