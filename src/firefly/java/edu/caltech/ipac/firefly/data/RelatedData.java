/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

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
    private int maskValues[];
    private String desc;
    private Map<String,String> searchParams= new HashMap<>();


    private RelatedData() {}

    public static RelatedData makeMaskRelatedData(String fileName, int maskValues[], int extensionNumber) {
        RelatedData d= new RelatedData();
        d.dataType= IMAGE_MASK;
        d.maskValues= maskValues;

        d.desc= null;
        d.searchParams.put(WebPlotRequest.FILE, fileName);
        d.searchParams.put(WebPlotRequest.PLOT_AS_MASK, "true");
        d.searchParams.put(WebPlotRequest.TYPE, RequestType.FILE+"");
        d.searchParams.put(WebPlotRequest.MULTI_IMAGE_IDX, extensionNumber+"");

        return d;
    }


    public static RelatedData makeMaskRelatedData(Map<String,String> searchParams, int maskValues[]) {
        RelatedData d= new RelatedData();
        d.dataType= IMAGE_MASK;
        d.maskValues= maskValues;

        d.desc= null;
        d.searchParams= searchParams;
        return d;
    }

    public static RelatedData makeImageOverlayRelatedData(String fileName, String desc, int extensionNumber) {
        RelatedData d= new RelatedData();
        d.dataType= IMAGE_OVERLAY;
        d.desc= desc;

        d.maskValues= null;

        d.searchParams.put(WebPlotRequest.FILE, fileName);
        d.searchParams.put(WebPlotRequest.TYPE, RequestType.FILE+"");
        d.searchParams.put(WebPlotRequest.MULTI_IMAGE_IDX, extensionNumber+"");
        return d;
    }

    public static RelatedData makeImageOverlayRelatedData(Map<String,String> searchParams, String desc) {
        RelatedData d= new RelatedData();
        d.dataType= IMAGE_OVERLAY;
        d.desc= desc;
        d.maskValues= null;

        d.searchParams= searchParams;
        return d;
    }

    public static RelatedData makeTabularRelatedData(Map<String,String> searchParams, String desc) {
        RelatedData d= new RelatedData();
        d.dataType= TABLE;
        d.searchParams= searchParams;
        d.desc= desc;
        d.maskValues= null;
        return d;
    }


    public String getDataType() { return dataType;}
    public int[] getMaskValues() { return maskValues;}
    public String getDesc() { return desc;}
    public Map<String,String> getSearchParams() { return searchParams;}
}

