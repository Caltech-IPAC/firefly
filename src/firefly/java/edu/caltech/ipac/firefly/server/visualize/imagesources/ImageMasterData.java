/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


package edu.caltech.ipac.firefly.server.visualize.imagesources;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ImageMasterData {


    private static Map<String, ImageMasterDataSourceType> sources= new HashMap<>();


    static {
        sources.put(ServerParams.IRSA, new IrsaMasterDataSource());
        sources.put(ServerParams.EXTERNAL, new ExternalMasterDataSource());
        sources.put(ServerParams.LSST, new LsstMasterDataSource());
    }

    private static final Logger.LoggerImpl _log= Logger.getLogger();

    /**
     *
     * @param imageSources - this list of sources of image data, A source string should be a memter of the sources map
     * @param sortOrder the results should be sorted by project in the as the sortOrder any project not listed in
     *                  the sortOrder array can just be put on bottom
     * @return an array of JSONObject each is a image source
     */
    public static JSONArray getJson(String imageSources[], String sortOrder[]) {

        // todo implement the sortOrder parameter


        String workingSources[]= imageSources;
        if (imageSources==null || imageSources.length==0 ||
                 (imageSources.length==1 && imageSources[0].equalsIgnoreCase(ServerParams.ALL))) {
            workingSources= sources.keySet().toArray(new String [10]);
        }


        JSONArray jsonData= new JSONArray();
        for(String source : workingSources) {
            if (sources.get(source)!=null) {
                List<ImageMasterDataEntry> sourceData= sources.get(source).getImageMasterData();
                if (sourceData!=null) {
                    for( ImageMasterDataEntry sd : sourceData) jsonData.add(makeJsonObj(sd.getDataMap()));
                }

            }
        }
        return jsonData;
    }

    public static JSONObject makeJsonObj(Map<String,Object> map) {
        JSONObject jo= new JSONObject();
        jo.putAll(map);
        Map params= (Map)jo.get(ImageMasterDataEntry.PLOT_REQUEST_PARAMS);
        JSONObject prJo= new JSONObject();
        prJo.putAll(params);
        jo.put(ImageMasterDataEntry.PLOT_REQUEST_PARAMS, prJo);
        return jo;
    }
}


