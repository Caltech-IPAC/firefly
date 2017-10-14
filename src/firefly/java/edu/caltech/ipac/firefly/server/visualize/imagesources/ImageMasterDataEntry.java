/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imagesources;

import java.util.HashMap;
import java.util.Map;
/**
 * @author Trey Roby
 * 10/5/17
 * Modified by LZ
 */
public class ImageMasterDataEntry {

    public static final String PLOT_REQUEST_PARAMS= "plotRequestParams";
    private Map<String,Object> map= new HashMap<>();

    public enum PARAMS{
        MIN_RANGE_DEG("minRangeDeg"),
        MAX_RANGE_DEG("maxRangeDeg"),
        PROJECT("project"),
        SUB_PROJECT ("subProject"),
        TITLE("title"),
        WAVELENGTH ("wavelength"),
        WAVELENGTH_DESC("wavelengthDesc"),
        WAVEBAND_ID("wavebandId"),
        HELP_URL("helpUrl"),
        TOOL_TIP ( "tooltip"),
        IMAGE_ID ("imageId"),
        PROJECT_TYPE_KEY("projectTypeKey"),
        PROJECT_TYPE_DESC("projectTypeDesc"),
        MISSION_ID("missionId"),
        ACRONYM("acronym"),
        DATA_TYPE("dataType");

        private String key;
        PARAMS(String key){
            this.key=key;
        }

        String getKey(){
            return this.key;
        }

    }

    public void setPlotRequestParams(Map<String,String> params) { map.put(PLOT_REQUEST_PARAMS, params);}


    public void set(PARAMS key, String val){map.put(key.getKey(), val);}


    public Map<String,Object> getDataMap() {
        return map;
    }

    /**
     * This method should only used for experimenting.
     * @param m the map to ingest
     */
    public static ImageMasterDataEntry makeFromMap(Map<String,Object> m) {
        ImageMasterDataEntry retval= new ImageMasterDataEntry();
        Map params= (Map)m.get(PLOT_REQUEST_PARAMS);
        retval.map= new HashMap<>(m);
        retval.map.put(PLOT_REQUEST_PARAMS, new HashMap<String,String>(params));
        return retval;
    }



}
