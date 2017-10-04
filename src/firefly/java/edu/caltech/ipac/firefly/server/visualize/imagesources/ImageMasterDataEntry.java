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


    public void setProject(String p) { map.put(IrsaMasterTableParameters.PROJECT, p);}
    public void setSubProject(String p) { map.put(IrsaMasterTableParameters.SUB_PROJECT, p);}
    public void setTitle(String t) { map.put(IrsaMasterTableParameters.TITLE, t);}
    public void setWavelength(String wl) { map.put(IrsaMasterTableParameters.WAVELENGTH, wl);}
    public void setWavelengthDesc(String desc) { map.put(IrsaMasterTableParameters.WAVELENGTH_DESC, desc);}
    public void setHelpUrl(String url) { map.put(IrsaMasterTableParameters.HELP_URL, url);}
    public void setTooltip(String tip) { map.put(IrsaMasterTableParameters.TOOL_TIP, tip);}
    public void setProjectTypeKey(String key) { map.put(IrsaMasterTableParameters.PROJECT_TYPE_KEY, key);}
    public void setProjectTypeDesc(String desc) { map.put(IrsaMasterTableParameters.PROJECT_TYPE_DESC, desc);}
    public void setMinRangeDeg(String minRangeDeg) { map.put(IrsaMasterTableParameters.MIN_RANGE_DEG, minRangeDeg);}
    public void setMaxRangeDeg(String maxRangeDeg) { map.put(IrsaMasterTableParameters.MAX_RANGE_DEG, maxRangeDeg);}
    public void setPlotRequestParams(Map<String,String> params) { map.put(PLOT_REQUEST_PARAMS, params);}
    public void setMissionId(String missionId) { map.put(IrsaMasterTableParameters.MISSION_ID, missionId);}
    public void setInstrumentId(String instrumentId) { map.put(IrsaMasterTableParameters.INSTRUMENT_ID, instrumentId);}
    public void setAcronym(String acry) { map.put(IrsaMasterTableParameters.ACRONYM , acry);}
    public void setDataType(String type) { map.put(IrsaMasterTableParameters.DATA_TYPE, type);}
    public void setAtLasTable(String atlas) { map.put(IrsaMasterTableParameters.ATLAS_TABLE, atlas);}
    public void setImageId(String id) { map.put(IrsaMasterTableParameters.IMAGE_ID, id);}



    public void set(String key, String val){map.put(key, val);}

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
