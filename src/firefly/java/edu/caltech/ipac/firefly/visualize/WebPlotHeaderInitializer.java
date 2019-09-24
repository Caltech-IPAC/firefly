package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 2019-04-09
 * Time: 11:10
 */


import nom.tam.fits.Header;

import java.util.Map;

/**
 * @author Trey Roby
 */
public class WebPlotHeaderInitializer {

    private String workingFitsFileStr;
    private String originalFitsFileStr;
    private String uploadFileNameStr;
    private String dataDesc;
    private boolean threeColor;
    private WebPlotRequest request;
    private Header[] zeroHeaderAry;
    private Map<String,String> attributes;

    public WebPlotHeaderInitializer(String originalFitsFileStr, String workingFitsFileStr,
                                    String uploadFileNameStr, String dataDesc,
                                    boolean threeColor, WebPlotRequest request, Header[] zeroHeaderAry,
                                    Map<String,String> attributes) {
        this.originalFitsFileStr= originalFitsFileStr;
        this.workingFitsFileStr= workingFitsFileStr;
        this.uploadFileNameStr = uploadFileNameStr;
        this.threeColor = threeColor;
        this.request = request;
        this.dataDesc= dataDesc;
        this.zeroHeaderAry= zeroHeaderAry;
        this.attributes= attributes;
    }

    public String getWorkingFitsFileStr() { return workingFitsFileStr; }

    public String getOriginalFitsFileStr() { return originalFitsFileStr; }

    public boolean isThreeColor() { return threeColor; }

    public String getUploadFileNameStr() { return uploadFileNameStr; }

    public String getDataDesc() { return dataDesc; }

    public WebPlotRequest getRequest() { return request; }

    public Header[] getZeroHeaderAry() { return zeroHeaderAry; }

    public Map<String,String> getAttributes() { return attributes;}
}

