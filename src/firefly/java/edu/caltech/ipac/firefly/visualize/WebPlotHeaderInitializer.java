package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 2019-04-09
 * Time: 11:10
 */


import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.Header;

import java.util.Map;

/**
 * @author Trey Roby
 */
public class WebPlotHeaderInitializer {

    private final String workingFitsFileStr;
    private final String originalFitsFileStr;
    private final String uploadFileNameStr;
    private final String dataDesc;
    private final String rangeValuesSerialized;
    private final boolean threeColor;
    private final WebPlotRequest request;
    private final Header[] zeroHeaderAry;
    private final Map<String,String> attributes;
    private final boolean multiImageFile;
    private final PlotState.MultiImageAction multiImage;
    private final int colorTableId;

    public WebPlotHeaderInitializer(String originalFitsFileStr, String workingFitsFileStr,
                                    String uploadFileNameStr, RangeValues rv,
                                    String dataDesc, int colorTableId, boolean multiImageFile,
                                    PlotState.MultiImageAction multiImage,
                                    boolean threeColor, WebPlotRequest request, Header[] zeroHeaderAry,
                                    Map<String,String> attributes) {
        this.originalFitsFileStr= originalFitsFileStr;
        this.workingFitsFileStr= workingFitsFileStr;
        this.uploadFileNameStr = uploadFileNameStr;
        this.threeColor = threeColor;
        this.request = request;
        this.dataDesc= dataDesc;
        this.rangeValuesSerialized= rv.serialize();
        this.zeroHeaderAry= zeroHeaderAry;
        this.attributes= attributes;
        this.multiImage= multiImage;
        this.multiImageFile= multiImageFile;
        this.colorTableId= colorTableId;
    }

    public String getWorkingFitsFileStr() { return workingFitsFileStr; }

    public String getOriginalFitsFileStr() { return originalFitsFileStr; }

    public boolean isThreeColor() { return threeColor; }

    public String getUploadFileNameStr() { return uploadFileNameStr; }

    public String getRangeValuesSerialized() { return rangeValuesSerialized; }

    public String getDataDesc() { return dataDesc; }

    public WebPlotRequest getRequest() { return request; }

    public Header[] getZeroHeaderAry() { return zeroHeaderAry; }

    public Map<String,String> getAttributes() { return attributes;}

    public boolean isMultiImageFile() { return multiImageFile; }

    public PlotState.MultiImageAction getMultiImage() { return multiImage; }

    public int getColorTableId() { return colorTableId; }
}

