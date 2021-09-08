/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.io.Serializable;
/**
 * User: roby
 * Date: Aug 7, 2008
 * Time: 4:17:43 PM
 */



/**
 * @author Trey Roby
 */
public class BandState implements Serializable {

    private final static String SPLIT_TOKEN= "--BandState--";

    private String workingFitsFileStr = null;
    private String originalFitsFileStr = null;
    private String uploadFileNameStr = null;
    private int    imageIdx = 0;
    private int    originalImageIdx = 0;

    private String plotRequestSerialize = null; // Serialized WebPlotRequest
    private String rangeValuesSerialize = null; // Serialized RangeValues
    private ClientFitsHeader directFileAcessData;
    private boolean bandVisible = true;
    private boolean multiImageFile = false;
    private boolean tileCompress = false;
    private int     cubeCnt = 0;
    private int     cubePlaneNumber = 0;

    private transient WebPlotRequest plotRequestTmp = null;
    private transient RangeValues rangeValues = null;


//======================================================================
//----------------------- Private Constructors -------------------------
//======================================================================

    public BandState() {}


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setImageIdx(int idx) { imageIdx = idx; }
    public int getImageIdx() { return imageIdx; }

    public boolean isMultiImageFile() { return multiImageFile; }

    public void setMultiImageFile(boolean multiImageFile) {
        this.multiImageFile = multiImageFile;
    }

    public boolean isTileCompress() {return tileCompress; }
    public void setTileCompress(boolean tCompress) {
        this.tileCompress = tCompress;
    }

    public int getCubePlaneNumber() { return cubePlaneNumber; }
    public void setCubePlaneNumber(int cubePlaneNumber) { this.cubePlaneNumber = cubePlaneNumber; }

    public int getCubeCnt() { return cubeCnt; }
    public void setCubeCnt(int cubeCnt) { this.cubeCnt = cubeCnt; }

    public void setOriginalImageIdx(int idx) { originalImageIdx = idx; }
    public int getOriginalImageIdx() { return originalImageIdx; }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param plotRequests copy this request
     */
    public void setWebPlotRequest(WebPlotRequest plotRequests) {
        plotRequestTmp = null;
        plotRequestSerialize = (plotRequests==null) ? null : plotRequests.toString();
    }


    /**
     * get a copy of the WebPlotRequest for this BandState.  Any changes to the object will not be reflected in
     * BandState you must set it back in
     * @return
     */
    public WebPlotRequest getWebPlotRequest() {
        if (plotRequestTmp ==null) plotRequestTmp = WebPlotRequest.parse(plotRequestSerialize);
        return plotRequestTmp;
    }


    public String getWebPlotRequestSerialized() {
        return plotRequestSerialize;
    }

    public boolean hasRequest() { return plotRequestSerialize !=null; }

    public void setBandVisible(boolean visible) { bandVisible = visible; }
    public boolean isBandVisible() { return bandVisible; }


    public void setRangeValues(RangeValues rangeValues) {
        this.rangeValues = null;
        rangeValuesSerialize = (rangeValues==null) ? null : rangeValues.serialize();
    }
    public RangeValues getRangeValues() {
        if (rangeValues ==null) rangeValues = RangeValues.parse(rangeValuesSerialize);
        return rangeValues;
    }

    public String getRangeValuesSerialized() {
        return rangeValuesSerialize;
    }

    /**
     * this method will make a copy of ClientFitsHeader. Any changes to the ClientFitsHeader object
     * after the set will not be
     * reflected here.
     * @param header
     */
    public void setDirectFileAccessData(ClientFitsHeader header) {
        directFileAcessData = header;
    }

    public FileAndHeaderInfo getFileAndHeaderInfo() {
        return new FileAndHeaderInfo(workingFitsFileStr, directFileAcessData);
    }

    public String getWorkingFitsFileStr() { return workingFitsFileStr; }
    public void setWorkingFitsFileStr(String fileStr) { workingFitsFileStr = fileStr; }


    public String getOriginalFitsFileStr() { return originalFitsFileStr; }
    public void setOriginalFitsFileStr(String fileStr) {
        originalFitsFileStr = fileStr; }

    public boolean isFileOriginal() {
       return ComparisonUtil.equals(originalFitsFileStr, workingFitsFileStr);
    }

    public void setUploadedFileName(String uploadFile) { uploadFileNameStr = uploadFile; }
    public String getUploadedFileName() { return uploadFileNameStr; }
    
    public BandState makeCopy() {
        BandState b= new BandState();
        b.workingFitsFileStr = this.workingFitsFileStr;
        b.originalFitsFileStr = this.originalFitsFileStr;
        b.uploadFileNameStr = this.uploadFileNameStr;
        b.imageIdx = this.imageIdx;
        b.originalImageIdx = this.originalImageIdx;

        b.plotRequestSerialize = this.plotRequestSerialize;
        b.rangeValuesSerialize = this.rangeValuesSerialize;
        b.directFileAcessData= this.directFileAcessData;
        b.bandVisible = this.bandVisible;
        b.multiImageFile = this.multiImageFile;
        b.tileCompress = this.tileCompress;
        b.cubeCnt = this.cubeCnt;
        b.cubePlaneNumber = this.cubePlaneNumber;
        return b;
    }


    public String toString() {
        return StringUtils.combine(SPLIT_TOKEN,
                workingFitsFileStr,
                originalFitsFileStr,
                uploadFileNameStr,
                imageIdx +"",
                originalImageIdx +"",
                plotRequestSerialize,
                rangeValuesSerialize,
                directFileAcessData +"",
                bandVisible +"",
                multiImageFile+"",
                tileCompress+"",
                cubeCnt+"",
                cubePlaneNumber+"");
    }

    public boolean equals(Object o) {
        if (o==this) return true;
        if (!(o instanceof BandState)) return false;
        BandState bs= (BandState)o;
        return ( ComparisonUtil.equals(workingFitsFileStr, bs.workingFitsFileStr) &&
                ComparisonUtil.equals(originalFitsFileStr, bs.originalFitsFileStr) &&
                ComparisonUtil.equals(uploadFileNameStr, bs.uploadFileNameStr) &&
                ComparisonUtil.equals(plotRequestSerialize, bs.plotRequestSerialize) &&
                ComparisonUtil.equals(rangeValuesSerialize, bs.rangeValuesSerialize) &&
                ComparisonUtil.equals(directFileAcessData, bs.directFileAcessData) &&
                imageIdx ==bs.imageIdx &&
                originalImageIdx ==bs.originalImageIdx &&
                bandVisible ==bs.bandVisible);
    }

}
