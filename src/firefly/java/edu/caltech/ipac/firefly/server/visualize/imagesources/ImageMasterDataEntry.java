/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imagesources;
/**
 * User: roby
 * Date: 9/20/17
 * Time: 12:07 PM
 */


import org.json.simple.JSONObject;

/**
 * @author Trey Roby
 */
public class ImageMasterDataEntry {

    private JSONObject map= new JSONObject();

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
    public void setPlotRequestParams(JSONObject params) { map.put("plotRequestParams", params);}


    public JSONObject getJsonObject() { return map;}

    /**
     * This method should only used for experimenting.
     * @param j
     */
    public static ImageMasterDataEntry makefromJson(JSONObject j) {
        ImageMasterDataEntry retval= new ImageMasterDataEntry();
        retval.map= j;
        return retval;
    }



}
