/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsNoExport;
import com.google.gwt.core.client.js.JsType;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
@JsExport
@JsType
public class PlotState implements DataEntry, HandSerialize {

    private final static String SPLIT_TOKEN= "--PlotState--";
    public enum RotateType {NORTH, ANGLE, UNROTATE}
    public enum Operation {ROTATE, CROP, FLIP_Y}
    public static final String NO_CONTEXT = "NoContext";
    public static final int MAX_BANDS= 3;

    public enum MultiImageAction { GUESS,      // Default, guess between load first, and use all, depending on three color params
                                   USE_FIRST,   // only valid option if loading a three color with multiple Request
                                   USE_IDX,   // use a specific image from the fits read Array
                                   MAKE_THREE_COLOR, // make a three color out of the first three images, not yet implemented
                                   USE_ALL} // only valid in non three color, make a array of WebPlots


    private transient Band[] usedBands;


    // plot state information



    // must be three, one for RED, GREEN, BLUE -
    // NO_BAND uses 0 like RED
    // NO_BAND and 3 color are mutually exclusive so there is no conflict
    private BandState bandStateAry[]= new BandState[MAX_BANDS];


    private MultiImageAction multiImage = MultiImageAction.GUESS;
    private String ctxStr;
    private boolean newPlot = true;
    private float zoomLevel = 1F;
    private boolean threeColor = false;
    private int colorTableId = 0;
    private RotateType rotationType = RotateType.UNROTATE;
    private CoordinateSys rotaNorthType = CoordinateSys.EQ_J2000;
    private boolean flippedY = false;
    private double rotationAngle = Double.NaN;
    private List<Operation> ops = new ArrayList<Operation>(1);

//======================================================================
//----------------------- Private Constructors -------------------------
//======================================================================

    public PlotState() {
    }

//======================================================================
//----------------------- Public Static Factory Methods ----------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public BandState[] getBandStateAry() { return bandStateAry; }


    public MultiImageAction getMultiImageAction() { return multiImage; }
    public void setMultiImageAction(MultiImageAction multiImage) { this.multiImage = multiImage; }

    public void setBandStateAry(BandState bsAry[]) {
        for(int i=0; (i<MAX_BANDS); i++) {
            if (i<bsAry.length) {
                bandStateAry[i]= bsAry[i];
            }
        }
    }

    public Band firstBand() {
        Band bandAry[]= getBands();
        return (bandAry!=null && bandAry.length>0) ? getBands()[0] : null;
    }

    public Band[] getBands() {
        if (usedBands ==null || usedBands.length==0) {
            List<Band> bands= new ArrayList<Band>(3);
            if (threeColor) {
                if (get(Band.RED).hasRequest())   bands.add(Band.RED);
                if (get(Band.GREEN).hasRequest()) bands.add(Band.GREEN);
                if (get(Band.BLUE).hasRequest())  bands.add(Band.BLUE);
            }
            else {
                bands.add(Band.NO_BAND);
            }
            usedBands = bands.toArray(new Band[bands.size()]);
        }
        return usedBands;
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

    public boolean isNewPlot() { return newPlot; }
    public void setNewPlot(boolean newPlot ) { this.newPlot = newPlot; }

    public int getColorTableId() { return colorTableId; }
    public void setColorTableId(int id ) { colorTableId = id; }

    public void setThreeColor(boolean threeColor) { this.threeColor = threeColor; }
    public boolean isThreeColor() { return threeColor; }

    public int getThumbnailSize() {
        return get(firstBand()).getWebPlotRequest().getThumbnailSize();
    }

    public void setZoomLevel(float z) {
        zoomLevel = z;}
    public float getZoomLevel() {return zoomLevel;}

    public void setRotateType(RotateType rotationType) { this.rotationType = rotationType; }
    public RotateType getRotateType() {return rotationType;}
    public boolean isRotated() {return rotationType!=PlotState.RotateType.UNROTATE;}

    public void setFlippedY(boolean flippedY) { this.flippedY = flippedY; }
    public boolean isFlippedY() { return flippedY; }

    public void setRotationAngle(double angle) { rotationAngle = angle; }
    public double getRotationAngle() { return rotationAngle; }

    public void setRotateNorthType(CoordinateSys csys) {
        rotaNorthType = csys;
    }

    public CoordinateSys getRotateNorthType() {
        return rotaNorthType;
    }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param plotRequests copy this request
     * @param band the band to set the request for
     */
    @JsNoExport
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
        usedBands = null;
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
    @JsNoExport
    public WebPlotRequest getWebPlotRequest() { return get(firstBand()).getWebPlotRequest(); }


    public void setBandVisible(Band band, boolean visible) { get(band).setBandVisible(visible); }
    public boolean isBandVisible(Band band) { return  get(band).isBandVisible(); }


    public boolean isMultiImageFile(Band band) {
        if (band==null) band= firstBand();
        return get(band).isMultiImageFile();
    }

    @JsNoExport
    public boolean isMultiImageFile() { return get(firstBand()).isMultiImageFile(); }
    public void setMultiImageFile(boolean multiImageFile, Band band) { get(band).setMultiImageFile(multiImageFile); }

    public int getCubeCnt(Band band) {
        if (band==null) band= firstBand();
        return get(band).getCubeCnt();
    }
    @JsNoExport
    public int getCubeCnt() { return get(firstBand()).getCubeCnt(); }
    public void setCubeCnt(int cubeCnt, Band band) { get(band).setCubeCnt(cubeCnt); }

    public int getCubePlaneNumber(Band band) {
        if (band==null) band= firstBand();
        return get(band).getCubePlaneNumber();
    }

    @JsNoExport
    public int getCubePlaneNumber() { return get(firstBand()).getCubePlaneNumber(); }
    public void setCubePlaneNumber(int cubeIdx, Band band) { get(band).setCubePlaneNumber(cubeIdx); }


    public void setRangeValues(RangeValues rv, Band band) { get(band).setRangeValues(rv); }
    public RangeValues getRangeValues(Band band) {
        if (band==null) band= firstBand();
        return get(band).getRangeValues();
    }

    @JsNoExport
    public RangeValues getRangeValues() { return get(firstBand()).getRangeValues(); }

    public void setFitsHeader(ClientFitsHeader header, Band band) { get(band).setFitsHeader(header); }


    public FileAndHeaderInfo getFileAndHeaderInfo(Band band) {
        return get(band).getFileAndHeaderInfo();
    }


    public ClientFitsHeader getHeader(Band band) { return get(band).getHeader(); }


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
    public void removeOperation(Operation op) {if (ops.contains(op)) ops.remove(op); }
    public boolean hasOperation(Operation op) {return ops.contains(op); }
    public void clearOperations() {
        ops.clear(); }
    public List<Operation> getOperations() { return ops;}

    public boolean isFilesOriginal() {
        boolean matches= true;
        for(Band band : getBands()) {
            matches= get(band).isFileOriginal();
            if (!matches) break;
        }
        return matches;
    }

    public String toPrettyString() {
        String s= "PlotState: ";
        WebPlotRequest pr;
        for(Band band : getBands() ) {
            pr= get(band).getWebPlotRequest();
            if (pr!=null) s+= pr.prettyString() + ", ";
        }
        s+= "ctxStr: " + ctxStr +
            ", zoom: " + zoomLevel +
            ", color id: " + colorTableId +
            ", 3 color: " + threeColor;
        return s;

    }

    public String serialize() { return toString(); }

    public String toString() {

        StringBuilder sb= new StringBuilder(350);
        sb.append(multiImage).append(SPLIT_TOKEN);
        sb.append(ctxStr).append(SPLIT_TOKEN);
        sb.append(newPlot).append(SPLIT_TOKEN);
        sb.append(zoomLevel).append(SPLIT_TOKEN);
        sb.append(threeColor).append(SPLIT_TOKEN);
        sb.append(colorTableId).append(SPLIT_TOKEN);
        sb.append(rotationType).append(SPLIT_TOKEN);
        sb.append(rotationAngle).append(SPLIT_TOKEN);
        sb.append(flippedY).append(SPLIT_TOKEN);
        sb.append(rotaNorthType.toString()).append(SPLIT_TOKEN);

        for(int i= 0; (i< bandStateAry.length); i++) {
                sb.append(bandStateAry[i]==null ? null : bandStateAry[i].serialize());
                if (i< bandStateAry.length-1) sb.append(SPLIT_TOKEN);
        }
        return sb.toString();
    }

    public Map<String,String> originKeyValues() {
        Map<String,String> keyVals = new LinkedHashMap<String,String>(5);
        BandState firstBandState = get(firstBand());
        keyVals.put("workingFile", firstBandState.getWorkingFitsFileStr());
        if (firstBandState.isMultiImageFile()) {
            keyVals.put("multiImageFile", firstBandState.isMultiImageFile()+"");
            keyVals.put("imageIdx", firstBandState.getImageIdx()+"");
        }
        if (!firstBandState.isFileOriginal()) {
            keyVals.put("originalFile",firstBandState.getOriginalFitsFileStr());
            if (firstBandState.isMultiImageFile()) {
                keyVals.put("originalImageIdx", firstBandState.getOriginalImageIdx()+"");
            }

        }
        return keyVals;
    }

    public static PlotState parse(String s) {
        PlotState retval;
        try {
            String sAry[]= StringUtils.parseHelper(s,13,SPLIT_TOKEN);
            int i= 0;
            MultiImageAction multiImage= Enum.valueOf(MultiImageAction.class, sAry[i++]);
            String ctxStr= getString(sAry[i++]);
            boolean newPlot= Boolean.parseBoolean(sAry[i++]);
            float zoomLevel= StringUtils.getFloat(sAry[i++], 1F);
            boolean threeColor = Boolean.parseBoolean(sAry[i++]);
            int colorTableId= StringUtils.getInt(sAry[i++], 0);
            RotateType rotationType= Enum.valueOf(RotateType.class, sAry[i++]);
            double rotationAngle= StringUtils.getDouble(sAry[i++]);
            boolean flippedY= StringUtils.getBoolean(sAry[i++],false);
            CoordinateSys rotaNorthType= CoordinateSys.parse(getString(sAry[i++]));

            BandState bandStateAry[]= new BandState[MAX_BANDS];
            for(int j= 0; (j<MAX_BANDS);j++) {
                bandStateAry[j]= BandState.parse(getString(sAry[i++]));
            }

            retval= new PlotState();
            retval.multiImage = multiImage;
            retval.ctxStr = ctxStr;
            retval.newPlot = newPlot;
            retval.zoomLevel = zoomLevel;
            retval.threeColor = threeColor;
            retval.colorTableId = colorTableId;
            retval.rotationType = rotationType;
            retval.rotaNorthType = rotaNorthType;
            retval.rotationAngle = rotationAngle;
            retval.flippedY = flippedY;
            retval.bandStateAry = bandStateAry;

        } catch (Exception e) {
            retval= null;
        }
        return retval;
    }

    private static String getString(String s) { return s.equals("null") ? null : s; }

    public boolean equals(Object o) {
        boolean retval= false;
        if (o==this) {
            retval= true;
        }
        else if (o!=null && o instanceof PlotState) {
            PlotState ps= (PlotState)o;
            if ( ComparisonUtil.equals(bandStateAry, ps.bandStateAry) &&
                 ComparisonUtil.equals(ctxStr, ps.ctxStr) &&
                 zoomLevel ==ps.zoomLevel &&
                 threeColor ==ps.threeColor &&
                 colorTableId ==ps.colorTableId) {
                retval= true;
            } // end if
        }
        return retval;
    }

    public void clearBand(Band band) {
        int idx= band.getIdx();
        if (bandStateAry[idx]!=null) {
            bandStateAry[idx]= null;
            setWebPlotRequest(null, band);
        }
    }

// =====================================================================
// -------------------- private Methods --------------------------------
// =====================================================================

    private BandState get(Band band) {
        int idx= band.getIdx();
        if (bandStateAry[idx]==null) bandStateAry[idx]= new BandState();
        return bandStateAry[idx];
    }


    private void initColorStretch(WebPlotRequest request, Band band) {
        if (request!=null) {
            colorTableId = request.getInitialColorTable();
            if (request.containsParam(WebPlotRequest.INIT_RANGE_VALUES)) {
                String rvStr= request.getParam(WebPlotRequest.INIT_RANGE_VALUES);
                RangeValues rv= RangeValues.parse(rvStr);
                if (rv!=null) get(band).setRangeValues(rv);
            }
        }

    }



}

