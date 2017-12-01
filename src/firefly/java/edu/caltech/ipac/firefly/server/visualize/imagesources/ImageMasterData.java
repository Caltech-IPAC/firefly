/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


package edu.caltech.ipac.firefly.server.visualize.imagesources;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterDataEntry.PARAMS.MISSION_ID;

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
     * @param imageSources - this list of sources of image data, A source string should be a memter of the sources map
     * @param sortOrder    the results should be sorted by project in the as the sortOrder any project not listed in
     *                     the sortOrder array can just be put on bottom
     * @return an array of JSONObject each is a image source
     */
    public static JSONArray getJson(String imageSources[], String sortOrder[]) {

        String workingSources[] = imageSources;
        if (imageSources == null || imageSources.length == 0 ||
                (imageSources.length == 1 && imageSources[0].equalsIgnoreCase(ServerParams.ALL))) {
            workingSources = sources.keySet().toArray(new String[10]);
        }

        List<ImageMasterDataEntry> allSourceData = new ArrayList<>();
        for (String source : workingSources) {
            ImageMasterDataSourceType imds = sources.get(source);
            if (imds != null) {
                List<ImageMasterDataEntry> imd = imds.getImageMasterData();
                if (imd != null) allSourceData.addAll(imds.getImageMasterData());
            }
        }

        // do sort if requested
        if (sortOrder != null && sortOrder.length > 0) {
            List<String> orders = Arrays.asList(sortOrder);
            Collections.sort(allSourceData, (e1, e2) -> {
                int e1Order = orders.indexOf(e1.getParamString(MISSION_ID));
                e1Order = e1Order < 0 ? Integer.MAX_VALUE : e1Order;
                int e2Order = orders.indexOf(e2.getParamString(MISSION_ID));
                e2Order = e2Order < 0 ? Integer.MAX_VALUE : e2Order;
                return Integer.compare(e1Order, e2Order);
            });
        }

        JSONArray jsonData = new JSONArray();
        for (ImageMasterDataEntry sd : allSourceData) {
            jsonData.add(makeJsonObj(sd.getDataMap()));
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


