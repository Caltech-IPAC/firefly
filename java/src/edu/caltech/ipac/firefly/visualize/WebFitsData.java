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
