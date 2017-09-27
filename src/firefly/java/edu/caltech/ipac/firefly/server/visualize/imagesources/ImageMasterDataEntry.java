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
 */
public class ImageMasterDataEntry {

    public static final String PLOT_REQUEST_PARAMS= "plotRequestParams";
    private Map<String,Object> map= new HashMap<>();


    public void setProject(String p) { map.put("project", p);}
    public void setSubProject(String p) { map.put("subProject", p);}
    public void setTitle(String t) { map.put("title", t);}
    public void setWavelength(String wl) { map.put("wavelength", wl);}
    public void setWavelengthDesc(String desc) { map.put("wavelengthDesc", desc);}
    public void setHelpUrl(String url) { map.put("helpUrl", url);}
    public void setTooltip(String tip) { map.put("tooltip", tip);}
    public void setImageId(String id) { map.put("imageId", id);}
    public void setProjectTypeKey(String key) { map.put("projectTypeKey", key);}
    public void setProjectTypeDesc(String desc) { map.put("projectTypeDesc", desc);}
    public void setMinRangeDeg(String minRangeDeg) { map.put("minRangeDEg", minRangeDeg);}
    public void setMaxRangeDeg(String maxRangeDeg) { map.put("maxRangeDEg", maxRangeDeg);}
    public void setPlotRequestParams(Map<String,String> params) { map.put(PLOT_REQUEST_PARAMS, params);}

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
