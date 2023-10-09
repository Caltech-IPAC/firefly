/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class PlotState {

    public enum Operation {CROP}
    public static final int MAX_BANDS= 3;


    // must be three, one for RED, GREEN, BLUE -
    // NO_BAND uses 0 like RED
    // NO_BAND and 3 color are mutually exclusive so there is no conflict
    private final BandState[] bandStateAry= new BandState[MAX_BANDS];


    private String ctxStr;
    private boolean threeColor = false;
    private List<Operation> ops = new ArrayList<>(1);

//======================================================================
//----------------------- Private Constructors -------------------------
//======================================================================

    public PlotState() { }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public BandState[] getBandStateAry() { return bandStateAry; }

    public void setBandStateAry(BandState[] bsAry) {
        for(int i=0; (i<MAX_BANDS); i++) {
            if (i<bsAry.length) {
                bandStateAry[i]= bsAry[i];
            }
        }
    }

    public Band firstBand() {
        Band[] bandAry= getBands();
        return (bandAry!=null && bandAry.length>0) ? getBands()[0] : null;
    }

    Band[] NO_BAND_ARY= new Band[] {Band.NO_BAND};

    public Band[] getBands() {
        if (threeColor) {
            List<Band> bands= new ArrayList<>(3);
            if (get(Band.RED).hasRequest())   bands.add(Band.RED);
            if (get(Band.GREEN).hasRequest()) bands.add(Band.GREEN);
            if (get(Band.BLUE).hasRequest())  bands.add(Band.BLUE);
            return bands.toArray(new Band[0]);
        }
        else {
            return NO_BAND_ARY;
        }
    }

    public boolean isBandUsed(Band band) {
        Band[] bandAry= getBands();
        boolean retval= false;
        for(Band b : bandAry) {
            if (b==band) {
                retval= true;
                break;
            }
        }
        return retval;
    }

    public  String getContextString() { return ctxStr; }
    public  void setContextString(String ctxStr) { this.ctxStr = ctxStr; }

    public void setThreeColor(boolean threeColor) { this.threeColor = threeColor; }
    public boolean isThreeColor() { return threeColor; }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param plotRequests copy this request
     * @param band the band to set the request for
     */
    public void setWebPlotRequest(WebPlotRequest plotRequests, Band band) {
        setWebPlotRequest(plotRequests,band,true);
    }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param plotRequests copy this request
     * @param band the band to set the request for
     */
    public void setWebPlotRequest(WebPlotRequest plotRequests, Band band, boolean initStretch) {
        get(band).setWebPlotRequest(plotRequests);
        if (initStretch) initColorStretch(plotRequests,band);
    }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param band the band to get the request for
     * @return the WebPlotRequest
     */
    public WebPlotRequest getWebPlotRequest(Band band) {
        if (band==null) band= firstBand();
        return get(band).getWebPlotRequest();
    }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @return the WebPlotRequest
     */
    public WebPlotRequest getPrimaryRequest() { return get(firstBand()).getWebPlotRequest(); }



    /**
     * this method will make a copy of the primary WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @return the WebPlotRequest
     */
    public WebPlotRequest getWebPlotRequest() { return get(firstBand()).getWebPlotRequest(); }

    public boolean isMultiImageFile(Band band) {
        if (band==null) band= firstBand();
        return get(band).isMultiImageFile();
    }

    public boolean isMultiImageFile() { return get(firstBand()).isMultiImageFile(); }
    public void setMultiImageFile(boolean multiImageFile, Band band) { get(band).setMultiImageFile(multiImageFile); }

    public int getCubeCnt(Band band) {
        if (band==null) band= firstBand();
        return get(band).getCubeCnt();
    }
    public int getCubeCnt() { return get(firstBand()).getCubeCnt(); }
    public void setCubeCnt(int cubeCnt, Band band) { get(band).setCubeCnt(cubeCnt); }

    public int getCubePlaneNumber(Band band) {
        if (band==null) band= firstBand();
        return get(band).getCubePlaneNumber();
    }

    public int getCubePlaneNumber() { return get(firstBand()).getCubePlaneNumber(); }
    public void setCubePlaneNumber(int cubeIdx, Band band) { get(band).setCubePlaneNumber(cubeIdx); }


    public void setRangeValues(RangeValues rv, Band band) { get(band).setRangeValues(rv); }
    public RangeValues getRangeValues(Band band) {
        if (band==null) band= firstBand();
        return get(band).getRangeValues();
    }

    public RangeValues getRangeValues() { return get(firstBand()).getRangeValues(); }

    public BandState.FileAndHeaderInfo getFileAndHeaderInfo(Band band) {
        return get(band).getFileAndHeaderInfo();
    }

    public String getWorkingFitsFileStr(Band band) { return band!=null ? get(band).getWorkingFitsFileStr() : null; }
    public void setWorkingFitsFileStr(String fileStr, Band band) { get(band).setWorkingFitsFileStr(fileStr); }

    public String getOriginalFitsFileStr(Band band) { return band!=null ? get(band).getOriginalFitsFileStr() : null; }
    public void setOriginalFitsFileStr(String fileStr, Band band) { get(band).setOriginalFitsFileStr(fileStr); }

    public String getUploadFileName(Band band) { return band!=null ? get(band).getUploadedFileName() : null; }
    public void setUploadFileName(String fileStr, Band band) { get(band).setUploadedFileName(fileStr); }

    public void setImageIdx(int idx, Band band) { get(band).setImageIdx(idx);}
    public int getImageIdx(Band band) { return get(band).getImageIdx(); }

    public void setOriginalImageIdx(int idx, Band band) { get(band).setOriginalImageIdx(idx); }
    public int getOriginalImageIdx(Band band) { return get(band).getOriginalImageIdx(); }

    public void addOperation(Operation op) {if (!ops.contains(op)) ops.add(op); }
    public boolean hasOperation(Operation op) {return ops.contains(op); }
    public List<Operation> getOperations() { return ops;}

    public String toPrettyString() {
        return "ctxStr: " + ctxStr + ", 3 color: " + threeColor;
    }

    public String toString() { return toPrettyString(); }

    public PlotState makeCopy() {
        PlotState p= new PlotState();
        p.ctxStr= this.ctxStr;
        p.threeColor= this.threeColor;
        p.ops = this.ops;
        for(int i= 0; (i< bandStateAry.length); i++) {
            if (this.bandStateAry[i]!=null) {
                p.bandStateAry[i]= this.bandStateAry[i].makeCopy();
            }
        }
        return p;
    }

    public boolean equals(Object o) {
        if (o==this) return true;
        if (!(o instanceof PlotState ps)) return false;
        return ComparisonUtil.equals(bandStateAry, ps.bandStateAry) &&
                ComparisonUtil.equals(ctxStr, ps.ctxStr) &&
                threeColor == ps.threeColor;
    }

    public BandState get(Band band) {
        int idx= band.getIdx();
        if (bandStateAry[idx]==null) bandStateAry[idx]= new BandState();
        return bandStateAry[idx];
    }

// =====================================================================
// -------------------- private Methods --------------------------------
// =====================================================================

    private void initColorStretch(WebPlotRequest request, Band band) {
        if (request!=null) {
            if (request.containsParam(WebPlotRequest.INIT_RANGE_VALUES)) {
                String rvStr= request.getParam(WebPlotRequest.INIT_RANGE_VALUES);
                RangeValues rv= RangeValues.parse(rvStr);
                if (rv!=null) get(band).setRangeValues(rv);
            }
        }

    }
}

