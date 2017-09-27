/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imagesources;


import edu.caltech.ipac.firefly.util.ImageSetConverter;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class IrsaMasterDataSource implements ImageMasterDataSourceType {

    @Override
    public List<ImageMasterDataEntry> getImageMasterData() {
        try {
            //URL url= ImageMasterData.class.getResource("/edu/caltech/ipac/firefly/resources/irsa-image-data.json");
            //String dataStr= URLDownload.getStringFromURL(url,null);
           //JSONParser parser= new JSONParser();
            //JSONArray data= (JSONArray)parser.parse(dataStr);
            ImageSetConverter ic = new ImageSetConverter("IRSA");
            JSONArray data = ic.getImageSetToJSONArray();
            List<ImageMasterDataEntry>  retList= new ArrayList<>();
            for(Object obj : data) {
                Map<String,Object> map= new HashMap<>();
                map.putAll((JSONObject)obj);
                retList.add( ImageMasterDataEntry.makeFromMap(map));
            }
            return retList;
        } catch (FailedRequestException  | IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
