/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.util.StringUtils;
import java.io.Serializable;
import java.util.Map;


/**
 * @author Trey Roby
 */
public class DirectFitsAccessData implements Serializable {

    private static final String PLANE_NUMBER= "planeNumber";
    private static final String BITPIX= "bitpix";
    private static final String NAXIS= "naxis";
    private static final String NAXIS1= "naxis1";
    private static final String NAXIS2= "naxis2";
    private static final String NAXIS3= "naxis3";
    private static final String CDELT2= "cdelt2";
    private static final String BSCALE= "bscale";
    private static final String BZERO= "bzero";
    private static final String BLANK_VALUE= "blank_value";
    private static final String DATA_OFFSET= "dataOffset";
    private final Map<String,String> headers;

    public DirectFitsAccessData(Map<String, String> headers) { this.headers = headers;}


    public int planeNumber() { return getIntHeader(PLANE_NUMBER,0); }
    public int bitpix() { return getIntHeader(BITPIX,0); }
    public int naxis1() { return getIntHeader(NAXIS1,0); }
    public int naxis2() { return getIntHeader(NAXIS2,0); }
    public double cDelt2() { return getDoubleHeader(CDELT2,0.0); }
    public double bScale() { return getDoubleHeader(BSCALE,0.0); }
    public double bZero() { return getDoubleHeader(BZERO,0.0); }
    public String blankValue() { return getStringHeader(BLANK_VALUE,""); }
    public long dataOffset() { return getLongHeader(DATA_OFFSET,0L); }

    public int getIntHeader(String key, int defValue) {
        if (headers.containsKey(key)) {
            try {
                return Integer.parseInt(headers.get(key));
            } catch (NumberFormatException ignore) { }
        }
        return defValue;
    }

    public long getLongHeader(String key, long defValue) {
        if (headers.containsKey(key)) {
            try {
                return Long.parseLong(headers.get(key));
            } catch (NumberFormatException ignore) { }
        }
        return defValue;
    }

    public double getDoubleHeader(String key) { return getDoubleHeader(key, 0.0); }

    public double getDoubleHeader(String key, double defValue) {
        if (headers.containsKey(key)) {
            try {
                return StringUtils.parseDouble(headers.get(key));
            } catch (NumberFormatException ignore) { }
        }
        return defValue;
    }

    public String getStringHeader(String key, String defValue) { return headers.getOrDefault(key, defValue); }
    public boolean containsKey(String key) { return headers.containsKey(key);}
}
