/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.table.DataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 * 10/5/17
 * Modified by LZ
 */
public class HiPSMasterListEntry {
    private Map<String, String> mapInfo = new HashMap<>();

    public enum PARAMS{
        TITLE("Title", "Title", String.class, "Dataset title (obs_title)"),
        ORDER("Order", "HiPS Order", Integer.class, "HEALPix order of the highest-resolution layer of the HiPS dataset (hips_order)", "HEALPix"),
        TYPE("Type", "Type", String.class, "Image or cube (dataproduct_type)"),
        FRACTION("Coverage", "Coverage", Float.class, "Sky coverage fraction measured in the MOC system (moc_sky_fraction)", "percent"),
        FRAME("Frame", "Frame", String.class, "Coordinate frame in which the HEALPix tiling of the dataset was constructed;"+
                " also default coordinate system used in display (hips_frame)"),
        URL("Url", "Url", String.class, "HiPS url"),
        ID("ID", "ID",  String.class, "HiPS id"),
        IVOID("CreatorID", "Dataset IVOA ID", String.class, "Unique ID of the dataset preserved across mirrors (creator_did)"), // note- don't change this unless you change the client also
        WAVELENGTH("Wavelength", "Waveband", String.class, "Wavelength regime, e.g.: "+
                "Radio, Millimeter, Infrared, Optical, UV, EUV, X-ray, Gamma-ray (obs_regime)"),
        RELEASEDATE("Release_date", "Release Date", String.class, "Last HiPS update date (hips_release_date)", "date"),
        PIXELSCALE("Pixel_scale", "Pixel Size", Float.class, "Median angular resolution of the highest-resolution "+
                "layer of the HiPS dataset (s_pixel_scale, converted to arcsec)", "degrees"),
        SOURCE("Source", "Source", String.class, "HiPS source"),
        PROPERTIES("Properties", "Properties", String.class, "URL for the HiPS properties file (derived from hips_service_url)"),
        STATUS("Status", "Status", String.class, "HiPS Status"),
        HAS_MOC("hasMOC", "Has MOC", Boolean.class, "There is a MOC available","boolean");

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
    private static List<DataType> hipsColTypes = new ArrayList<>();
    private static List<DataType> mocColTypes = new ArrayList<>();

    // columns included in the HiPS list table, some column may be hidden, URL, SOURCE
    private static PARAMS[] orderCols = new PARAMS[]{PARAMS.TYPE,  PARAMS.PROPERTIES, PARAMS.TITLE, PARAMS.WAVELENGTH,
                                            PARAMS.FRACTION, PARAMS.PIXELSCALE, PARAMS.ORDER, PARAMS.FRAME,
                                            PARAMS.URL, PARAMS.IVOID, PARAMS.HAS_MOC};

    private static PARAMS[] mocCols = new PARAMS[]{PARAMS.FRACTION, PARAMS.WAVELENGTH, PARAMS.TITLE,
            PARAMS.ORDER, PARAMS.URL, PARAMS.RELEASEDATE, PARAMS.IVOID, PARAMS.SOURCE, PARAMS.PROPERTIES};


    static {
        for (PARAMS param : orderCols) {
            DataType dt = new DataType(param.getKey(), param.getTitle(), param.getMetaClass()) ;
            dt.setDesc(param.getDescription());
            dt.setUnits(param.getUnits());
            hipsColTypes.add(dt);
        }
        for (PARAMS param : mocCols) {
            DataType dt = new DataType(param.getKey(), param.getTitle(), param.getMetaClass()) ;
            dt.setDesc(param.getDescription());
            dt.setUnits(param.getUnits());
            mocColTypes.add(dt);
        }
    }

    public static List<DataType> getHiPSEntryColumns() { return hipsColTypes; }
    public static List<DataType> getMOCEntryColumns() { return mocColTypes; }


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
