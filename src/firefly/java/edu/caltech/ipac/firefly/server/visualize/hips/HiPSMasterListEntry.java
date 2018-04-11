/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.util.DataType;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 * 10/5/17
 * Modified by LZ
 */
public class HiPSMasterListEntry {
    private Map<String, String> mapInfo = new HashMap<>();

    public enum PARAMS{
        TITLE("Title", "Title", String.class, "Data set title"),
        ORDER("Order", "Order", Integer.class, "HiPS order"),
        TYPE("Type", "Type", String.class, "Type of data, 'image', 'cube', 'catalog'"),
        FRACTION("Coverage", "Coverage", Float.class, "Fraction of the sky covers by the MOC associated to the HiPS"),
        FRAME("Frame", "Frame", String.class, "Coordinate frame reference"),
        URL("Url", "Url", String.class, "HiPS url"),
        ID("ID", "ID",  String.class, "HiPS id"),
        IVOID("CreatorID", "CreatorID", String.class, "Unique id of HiPS"), // note- don't change this unless you change the client also
        WAVELENGTH("Wavelength", "Wavelength", String.class, "General wavelength, Radio, Millimeter, Infared, Optical, " +
                "UV, EUV, X-ray, Gamma-ray"),
        RELEASEDATE("Release_date", "Release_date", String.class, "Last HiPS update date, YYYY-mm-ddTHH:MMZ"),
        PIXELSCALE("Pixel_scale", "Pixel_scale", Float.class, "pixel angular resolution at the highest order", "degrees"),
        SOURCE("Source", "Source", String.class, "HiPS source"),
        PROPERTIES("Properties", "Properties", String.class, "HiPS properties url"),
        STATUS("Status", "Status", String.class, "HiPS Status");

        String key;
        String title;
        Class metaClass;
        String description;
        String units;

        PARAMS(String key, String title, Class c, String des) {
            this(key, title, c, des, null);
        }
        PARAMS(String key, String title, Class c, String des, String units)
        {
            this.key=key;
            this.title = title;
            this.metaClass = c;
            this.description = des;
            this.units = units;
        }

        String getKey(){
            return this.key;
        }
        String getTitle(){
            return this.title;
        }
        Class getMetaClass() { return this.metaClass; }
        String getDescription() { return this.description; }
        String getUnits() {return this.units; }
    }
    private static List<DataType> cols = new ArrayList<>();

    // columns included in the HiPS list table, some column may be hidden, URL, SOURCE
    private static PARAMS[] orderCols = new PARAMS[]{PARAMS.TYPE, PARAMS.TITLE, PARAMS.WAVELENGTH,
                                            PARAMS.RELEASEDATE, PARAMS.FRAME, PARAMS.ORDER, PARAMS.PIXELSCALE,
                                            PARAMS.FRACTION,  PARAMS.PROPERTIES, PARAMS.URL,
                                            PARAMS.SOURCE, PARAMS.IVOID};
    static {
        for (PARAMS param : orderCols) {
            DataType dt = new DataType(param.getKey(), param.getTitle(), param.getMetaClass()) ;
            dt.setShortDesc(param.getDescription());
            dt.setUnits(param.getUnits());
            cols.add(dt);
        }
    }

    public static List<DataType> getHiPSEntryColumns() {
        return cols;
    }


    public static String getParamString(Map<String, String>map, PARAMS key) {
        if (key != null) {
            Object v = map.get(key.getKey());
            return  v == null ? null : v.toString();
        }
        return null;
    }

    public static void setParamsMap(Map<String, String> map, PARAMS key, String val){map.put(key.getKey(), val);}

    public void set(String key, String val)  {
        mapInfo.put(key, val);
    }
    public Map<String, String> getMapInfo() {
        return mapInfo;
    }
}
