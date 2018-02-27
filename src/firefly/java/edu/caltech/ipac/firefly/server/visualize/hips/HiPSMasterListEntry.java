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
        URL("Url", "Url", String.class, "HiPS access url"),
        ID("ID", "ID",  String.class, "HiPS id"),
        SOURCE("Source", "Source", String.class, "HiPS source");

        String key;
        String title;
        Class metaClass;
        String description;

        PARAMS(String key, String title, Class c, String des)
        {
            this.key=key;
            this.title = title;
            this.metaClass = c;
            this.description = des;
        }

        String getKey(){
            return this.key;
        }
        String getTitle(){
            return this.title;
        }
        Class getMetaClass() { return this.metaClass; }
        String getDescription() { return this.description; }
    }
    private static List<DataType> cols = new ArrayList<>();
    private static PARAMS[] orderCols = new PARAMS[]{PARAMS.TYPE, PARAMS.TITLE, PARAMS.ORDER,
                                            PARAMS.FRACTION, PARAMS.FRAME, PARAMS.URL, PARAMS.SOURCE};
    static {
        for (PARAMS param : orderCols) {
            DataType dt = new DataType(param.getKey(), param.getTitle(), param.getMetaClass());
            dt.setShortDesc(param.getDescription());
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
