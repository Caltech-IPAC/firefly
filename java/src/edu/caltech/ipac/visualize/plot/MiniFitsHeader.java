package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
/**
 * User: roby
 * Date: Mar 23, 2009
 * Time: 3:34:15 PM
 */


/**
 * @author Trey Roby
 */
public class MiniFitsHeader implements Serializable {

    private final static String SPLIT_TOKEN= "--MiniFitHead--";

    private static final String PLANE_NUMBER= "planeNumber";
    private static final String BITPIX= "bitpix";
    private static final String NAXIS= "naxis";
    private static final String NAXIS1= "naxis1";
    private static final String NAXIS2= "naxis2";
    private static final String NAXIS3= "naxis3";
    private static final String CDELT2= "cdelt2";
    private static final String BSCALE= "bscale";
    private static final String BZERO= "bzero";
    private static final String BLANK_VALUE= "blankValue";
    private static final String DATA_OFFSET= "dataOffset";



//    private int _planeNumber;
//    private int _bitpix;
//    private int _naxis;
//    private int _naxis1;
//    private int _naxis2;
//    private int _naxis3;
//    private double _cdelt2;
//    private double _blankValue;
//    private double _bscale;
//    private double _bzero;
//    private long _dataOffset;
    private Map<String,String> _headers= new HashMap<String,String>(20);



//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public MiniFitsHeader() {}

    private MiniFitsHeader(Map<String, String> headers) { _headers= headers;}

    public MiniFitsHeader(int planeNumber,
                          int bitpix,
                          int naxis,
                          int naxis1,
                          int naxis2,
                          int naxis3,
                          double cdelt2,
                          double bscale,
                          double bzero,
                          double blankValue,
                          long dataOffset) {
        _headers.put(PLANE_NUMBER,planeNumber+"");
        _headers.put(BITPIX,      bitpix+"");
        _headers.put(NAXIS,       naxis+"");
        _headers.put(NAXIS1,      naxis1+"");
        _headers.put(NAXIS2,      naxis2+"");
        _headers.put(NAXIS3,      naxis3+"");
        _headers.put(CDELT2,      cdelt2+"");
        _headers.put(BSCALE,      bscale+"");
        _headers.put(BZERO,       bzero+"");
        _headers.put(BLANK_VALUE, blankValue+"");
        _headers.put(DATA_OFFSET, dataOffset+"");
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public int getPlaneNumber() { return getIntHeader(PLANE_NUMBER); }
    public int getBixpix() { return getIntHeader(BITPIX); }
    public int getNaxis() { return getIntHeader(NAXIS); }
    public int getNaxis1() { return getIntHeader(NAXIS1); }
    public int getNaxis2() { return getIntHeader(NAXIS2); }
    public int getNaxis3() { return getIntHeader(NAXIS3); }
    public double getCDelt2() { return getDoubleHeader(CDELT2); }
    public double getBScale() { return getDoubleHeader(BSCALE); }
    public double getBZero() { return getDoubleHeader(BZERO); }
    public double getBlankValue() { return getDoubleHeader(BLANK_VALUE); }
    public long getDataOffset() { return getLongHeader(DATA_OFFSET); }

    public int getIntHeader(String key) { return getIntHeader(key,0); }

    public int getIntHeader(String key, int defValue) {
        int retval= defValue;
        if (_headers.containsKey(key)) {
            try {
                retval= Integer.parseInt(_headers.get(key));
            } catch (NumberFormatException e) {
                retval= defValue;
            }
        }
        return retval;
    }

    public long getLongHeader(String key) { return getLongHeader(key, 0L); }

    public long getLongHeader(String key, long defValue) {
        long retval= defValue;
        if (_headers.containsKey(key)) {
            try {
                retval= Long.parseLong(_headers.get(key));
            } catch (NumberFormatException e) {
                retval= defValue;
            }
        }
        return retval;
    }

    public double getDoubleHeader(String key) { return getDoubleHeader(key, 0.0); }

    public double getDoubleHeader(String key, double defValue) {
        double retval= defValue;
        if (_headers.containsKey(key)) {
            try {
                retval= StringUtils.parseDouble(_headers.get(key));
            } catch (NumberFormatException e) {
                retval= defValue;
            }
        }
        return retval;
    }


    public String getStringHeader(String key) { return getStringHeader(key,""); }

    public String getStringHeader(String key, String defValue) {
        String retval= defValue;
        if (_headers.containsKey(key)) {
            retval= _headers.get(key);
        }
        return retval;
    }

    public void setHeader(String key, int value) { setHeader(key,value+"");}
    public void setHeader(String key, double value) { setHeader(key,value+"");}
    public void setHeader(String key, String value) { _headers.put(key,value);  }

    public boolean containsKey(String key) { return _headers.containsKey(key);}

    public String toString() {

        StringBuilder sb = new StringBuilder(500);
        for(Map.Entry<String,String> entry : _headers.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            sb.append(SPLIT_TOKEN);
        }
        return sb.toString();
    }


    public static MiniFitsHeader parse(String s) {

        if (s==null) return null;
        Map<String,String> map= new HashMap<String,String>(50);
//        MapPropertyLoader.load(map,projStr);
        String sAry[]= s.split(SPLIT_TOKEN,60);
        MiniFitsHeader miniFitsHeader= null;
        if (sAry.length<60) {
            String pairAry[];
            for(String pair : sAry) {
                pairAry= pair.split("=",2);
                if (pairAry.length==2) {
                    map.put(pairAry[0],pairAry[1]);
                }
            }
            miniFitsHeader= new MiniFitsHeader(map);
        }

        return miniFitsHeader;
    }



}

