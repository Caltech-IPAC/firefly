/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
* @author Trey Roby
*/
public class RelatedData implements Serializable {
    public static final String IMAGE_OVERLAY= "IMAGE_OVERLAY";
    public static final String IMAGE_MASK= "IMAGE_MASK";
    public static final String TABLE= "TABLE";

    private String dataType= ""; // image or table
    private String desc;
    /** related dataKey should be a unique string for a fits file */
    private String dataKey;
    private Map<String,String> searchParams= new HashMap<>();
    private Map<String,String> availableMask= new HashMap<>();


    private RelatedData() {}

    /**
     * Factory method to create mask related data
     * @param fileName - fits file name on the server
     * @param availableMask - a map with the key to be the bits number (as a string) and the value to be a description
     * @param extensionNumber - extension number of the fits file
     * @param dataKey - should be a unique string for a fits file
     * @return RelatedData
     */
    public static RelatedData makeMaskRelatedData(String fileName, Map<String,String> availableMask, int extensionNumber, String dataKey) {
        Map<String,String> searchParams= new HashMap<>();
        searchParams.put(WebPlotRequest.FILE, fileName);
        searchParams.put(WebPlotRequest.PLOT_AS_MASK, "true");
        searchParams.put(WebPlotRequest.TYPE, RequestType.FILE+"");
        searchParams.put(WebPlotRequest.MULTI_IMAGE_IDX, extensionNumber+"");
        return makeMaskRelatedData(searchParams, availableMask, dataKey);
    }

    /**
     * Factory method to create mask related data
     * @param searchParams- parameters used to make a WebPlotRequest search
     * @param availableMask - a map with the key to be the bits number (as a string) and the value to be a description
     * @param dataKey - should be a unique string for a fits file
     * @return RelatedData
     */
    public static RelatedData makeMaskRelatedData(Map<String,String> searchParams, Map<String,String> availableMask, String dataKey) {
        RelatedData d= new RelatedData();
        d.dataType= IMAGE_MASK;
        d.availableMask= availableMask;

        d.desc= "Mask";
        d.dataKey = dataKey;
        d.searchParams= searchParams;
        return d;
    }

    /**
     * Factory method to create image overlay related data
     * @param fileName - name of the fits file
     * @param dataKey - should be a unique string for a fits file
     * @param desc - description of the data
     * @param extensionNumber - extenions number in the fits file
     * @return RelatedData
     */
    public static RelatedData makeImageOverlayRelatedData(String fileName, String dataKey, String desc, int extensionNumber) {
        Map<String,String> searchParams= new HashMap<>();
        searchParams.put(WebPlotRequest.FILE, fileName);
        searchParams.put(WebPlotRequest.TYPE, RequestType.FILE+"");
        searchParams.put(WebPlotRequest.MULTI_IMAGE_IDX, extensionNumber+"");
        return makeImageOverlayRelatedData(searchParams,dataKey,desc);
    }

    /**
     * Factory method to create image overlay related data
     * @param searchParams - parameters used to make a WebPlotRequest search
     * @param dataKey - should be a unique string for a fits file
     * @param desc - description of the data
     * @return RelatedData
     */
    public static RelatedData makeImageOverlayRelatedData(Map<String,String> searchParams, String dataKey, String desc) {
        RelatedData d= new RelatedData();
        d.dataType= IMAGE_OVERLAY;
        d.desc= desc;
        d.dataKey = dataKey;
        d.searchParams= searchParams;
        return d;
    }

    /**
     * Factory method to create tabular related data
     * @param searchParams - parameters us to make a table file type server request
     * @param dataKey - should be a unique string for a fits file
     * @param desc - description of the data
     * @return RelatedData
     */
    public static RelatedData makeTabularRelatedData(Map<String,String> searchParams, String dataKey, String desc) {
        RelatedData d= new RelatedData();
        d.dataType= TABLE;
        d.searchParams= searchParams;
        d.dataKey = dataKey;
        d.desc= desc;
        return d;
    }


    public String getDataType() { return dataType;}
    public String getDataKey() { return dataKey;}
    public Map<String,String> getAvailableMask() { return availableMask;}
    public String getDesc() { return desc;}
    public Map<String,String> getSearchParams() { return searchParams;}
}

