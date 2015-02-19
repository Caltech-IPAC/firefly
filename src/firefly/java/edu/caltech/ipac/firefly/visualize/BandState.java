/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.MiniFitsHeader;
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
public class BandState implements Serializable, HandSerialize {

    private final static String SPLIT_TOKEN= "--BandState--";

    private String _workingFitsFileStr = null;
    private String _originalFitsFileStr= null;
    private String _uploadFileNameStr= null;
    private int    _imageIdx= 0;
    private int    _originalImageIdx= 0;

    private String _plotRequestSerialize = null; // Serialized WebPlotRequest
    private String _rangeValuesSerialize = null; // Serialized RangeValues
    private String _fitsHeaderSerialize = null; // Serialized MiniFitsHeader
    private boolean _bandVisible= true;
    private boolean multiImageFile = false;
    private int     cubeCnt = 0;
    private int     cubePlaneNumber = 0;

    private transient WebPlotRequest _plotRequestTmp= null;
    private transient RangeValues    _rangeValues   = null;


//======================================================================
//----------------------- Private Constructors -------------------------
//======================================================================

    public BandState() {}


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setImageIdx(int idx) { _imageIdx= idx; }
    public int getImageIdx() { return _imageIdx; }

    public boolean isMultiImageFile() { return multiImageFile; }

    public void setMultiImageFile(boolean multiImageFile) {
        this.multiImageFile = multiImageFile;
    }

    public int getCubePlaneNumber() { return cubePlaneNumber; }
    public void setCubePlaneNumber(int cubePlaneNumber) { this.cubePlaneNumber = cubePlaneNumber; }

    public int getCubeCnt() { return cubeCnt; }
    public void setCubeCnt(int cubeCnt) { this.cubeCnt = cubeCnt; }

    public void setOriginalImageIdx(int idx) { _originalImageIdx= idx; }
    public int getOriginalImageIdx() { return _originalImageIdx; }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param plotRequests copy this request
     */
    public void setWebPlotRequest(WebPlotRequest plotRequests) {
        _plotRequestTmp = null;
        _plotRequestSerialize = (plotRequests==null) ? null : plotRequests.toString();
    }


    /**
     * get a copy of the WebPlotRequest for this BandState.  Any changes to the object will not be reflected in
     * BandState you must set it back in
     * @return
     */
    public WebPlotRequest getWebPlotRequest() {
        if (_plotRequestTmp==null) _plotRequestTmp= WebPlotRequest.parse(_plotRequestSerialize);
        return _plotRequestTmp;
    }

    public boolean hasRequest() { return _plotRequestSerialize!=null; }

    public void setBandVisible(boolean visible) { _bandVisible= visible; }
    public boolean isBandVisible() { return _bandVisible; }


    public void setRangeValues(RangeValues rangeValues) {
        _rangeValues= null;
        _rangeValuesSerialize= (rangeValues==null) ? null : rangeValues.serialize();
    }
    public RangeValues getRangeValues() {
        if (_rangeValues==null) _rangeValues= RangeValues.parse(_rangeValuesSerialize);
        return _rangeValues;
    }

    /**
     * this method will make a copy of MiniFitsHeader. Any changes to the MiniFitsHeader object
     * after the set will not be
     * reflected here.
     * @param header
     */
    public void setFitsHeader(MiniFitsHeader header) {
        _fitsHeaderSerialize = header==null ? null : header.toString();
    }

    public FileAndHeaderInfo getFileAndHeaderInfo() {
        return new FileAndHeaderInfo(_workingFitsFileStr, _fitsHeaderSerialize);
    }

    public MiniFitsHeader getHeader() { return MiniFitsHeader.parse(_fitsHeaderSerialize); }





    public String getWorkingFitsFileStr() { return _workingFitsFileStr; }
    public void setWorkingFitsFileStr(String fileStr) { _workingFitsFileStr = fileStr; }


    public String getOriginalFitsFileStr() { return _originalFitsFileStr; }
    public void setOriginalFitsFileStr(String fileStr) {_originalFitsFileStr= fileStr; }

    public boolean isFileOriginal() {
       return ComparisonUtil.equals(_originalFitsFileStr, _workingFitsFileStr);
    }

    public void setUploadedFileName(String uploadFile) { _uploadFileNameStr= uploadFile; }
    public String getUploadedFileName() { return _uploadFileNameStr; }


    public String toString() {
        return StringUtils.combine(SPLIT_TOKEN,
                                   _workingFitsFileStr,
                                   _originalFitsFileStr,
                                   _uploadFileNameStr,
                                   _imageIdx+"",
                                   _originalImageIdx+"",
                                   _plotRequestSerialize,
                                   _rangeValuesSerialize,
                                   _fitsHeaderSerialize,
                                   _bandVisible+"",
                                   multiImageFile+"",
                                   cubeCnt+"",
                                   cubePlaneNumber+"");
    }

    public String serialize() { return toString(); }

    public static BandState parse(String s) {
        BandState retval= null;
        try {
            String sAry[]= StringUtils.parseHelper(s,12,SPLIT_TOKEN);
            int i= 0;
            String workingFileStr=  StringUtils.checkNull(sAry[i++]);
            String originalFileStr= StringUtils.checkNull(sAry[i++]);
            String uploadFileStr=   StringUtils.checkNull(sAry[i++]);
            int    imageIdx=        Integer.parseInt(sAry[i++]);
            int    originalImageIdx=Integer.parseInt(sAry[i++]);
            WebPlotRequest req=     WebPlotRequest.parse(sAry[i++]);
            RangeValues rv=         RangeValues.parse(sAry[i++]);
            MiniFitsHeader header=  MiniFitsHeader.parse(sAry[i++]);
            boolean bandVisible=    Boolean.parseBoolean(sAry[i++]);
            boolean multiImageFile= Boolean.parseBoolean(sAry[i++]);
            int cubeCnt=            Integer.parseInt(sAry[i++]);
            int cubePlaneNumber=    Integer.parseInt(sAry[i++]);
            if (req!=null && header!=null ) {
                retval= new BandState();
                retval.setWorkingFitsFileStr(workingFileStr);
                retval.setOriginalFitsFileStr(originalFileStr);
                retval.setUploadedFileName(uploadFileStr);
                retval.setImageIdx(imageIdx);
                retval.setOriginalImageIdx(originalImageIdx);
                retval.setWebPlotRequest(req);
                retval.setRangeValues(rv);
                retval.setFitsHeader(header);
                retval.setBandVisible(bandVisible);
                retval.setMultiImageFile(multiImageFile);
                retval.setCubeCnt(cubeCnt);
                retval.setCubePlaneNumber(cubePlaneNumber);
            }
        } catch (IllegalArgumentException e) {
            retval= null;
        }
        return retval;
    }


    public boolean equals(Object o) {
        boolean retval= false;
        if (o==this) {
            retval= true;
        }
        else if (o!=null && o instanceof BandState) {
            BandState bs= (BandState)o;
            if ( ComparisonUtil.equals(_workingFitsFileStr, bs._workingFitsFileStr) &&
                 ComparisonUtil.equals(_originalFitsFileStr, bs._originalFitsFileStr) &&
                 ComparisonUtil.equals(_uploadFileNameStr, bs._uploadFileNameStr) &&
                 ComparisonUtil.equals(_plotRequestSerialize, bs._plotRequestSerialize ) &&
                 ComparisonUtil.equals(_rangeValuesSerialize, bs._rangeValuesSerialize) &&
                 ComparisonUtil.equals(_fitsHeaderSerialize, bs._fitsHeaderSerialize) &&
                 _imageIdx==bs._imageIdx &&
                 _originalImageIdx==bs._originalImageIdx &&
                 _bandVisible==bs._bandVisible) {
                retval= true;
            } // end if
        }
        return retval;
    }

}
