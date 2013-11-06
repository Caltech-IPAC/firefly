package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.MiniFitsHeader;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Aug 7, 2008
 * Time: 4:17:43 PM
 */


/**
 * @author Trey Roby
 */
public class PlotState implements DataEntry, HandSerialize {

    private final static String SPLIT_TOKEN= "--PlotState--";
    public enum RotateType {NORTH, ANGLE, UNROTATE}
    public enum Operation {ROTATE, CROP, FLIP_Y}
    public static final String NO_CONTEXT = "NoContext";
    private static final int MAX_BANDS= 3;

    public enum MultiImageAction { GUESS,      // Default, guess between load first, and use all, depending on three color params
                                   USE_FIRST,   // only valid option if loading a three color with multiple Request
                                   MAKE_THREE_COLOR, // make a three color out of the first three images, not yet implemented
                                   //ASK_USER, // ask use what to do, like spot, not yet implemented
                                   USE_ALL} // only valid in non three color, make a array of WebPlots


    private transient Band[] _usedBands;


    // plot state information



    // must be three, one for RED, GREEN, BLUE -
    // NO_BAND uses 0 like RED
    // NO_BAND and 3 color are mutually exclusive so there is no conflict
    private BandState _bandStateAry[]= new BandState[MAX_BANDS];


    private MultiImageAction _multiImage= MultiImageAction.GUESS;
    private String _ctxStr;
    private boolean _newPlot= false;
    private float _zoomLevel= 1F;
    private boolean _threeColor= false;
    private int _colorTableId= 0;
    private RotateType _rotationType= RotateType.UNROTATE;
    private CoordinateSys _rotaNorthType= CoordinateSys.EQ_J2000;
    private boolean _flippedY= false;
    private double _rotationAngle= Double.NaN;
    private List<Operation> _ops= new ArrayList<Operation>(1);

//======================================================================
//----------------------- Private Constructors -------------------------
//======================================================================

    private PlotState() {
        _newPlot= true;
    }


    public PlotState(WebPlotRequest request) {
        this();
        setWebPlotRequest(request, Band.NO_BAND);
        _multiImage= MultiImageAction.USE_ALL;
    }


//    public PlotState(WebPlotRequest request, MultiImageAction action) {
//        this();
//        _threeColor= (_multiImage==MultiImageAction.MAKE_THREE_COLOR);
//        setWebPlotRequest(request, Band.NO_BAND);
//        _multiImage= action;
//    }



    public PlotState(boolean threeColor) {
        this();
        _threeColor= threeColor;
        _multiImage= threeColor ? MultiImageAction.USE_FIRST : MultiImageAction.USE_ALL;
    }




    public PlotState(WebPlotRequest redRequest,
                     WebPlotRequest greenRequest, 
                     WebPlotRequest blueRequest) {
        this();
        _threeColor= true;
        _multiImage= MultiImageAction.USE_FIRST;
        setWebPlotRequest(redRequest, Band.RED);
        setWebPlotRequest(greenRequest, Band.GREEN);
        setWebPlotRequest(blueRequest, Band.BLUE);
    }



//======================================================================
//----------------------- Public Static Factory Methods ----------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public MultiImageAction getMultiImageAction() { return _multiImage; }
    public void setMultiImageAction(MultiImageAction multiImage) { _multiImage= multiImage; }

    public Band firstBand() {
        Band bandAry[]= getBands();
        return (bandAry!=null && bandAry.length>0) ? getBands()[0] : null;
    }

    public Band[] getBands() {
        if (_usedBands ==null || _usedBands.length==0) {
            List<Band> bands= new ArrayList<Band>(3);
            if (_threeColor) {
                if (get(Band.RED).hasRequest())   bands.add(Band.RED);
                if (get(Band.GREEN).hasRequest()) bands.add(Band.GREEN);
                if (get(Band.BLUE).hasRequest())  bands.add(Band.BLUE);
            }
            else {
                bands.add(Band.NO_BAND);
            }
            _usedBands = bands.toArray(new Band[bands.size()]);
        }
        return _usedBands;
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

    public  String getContextString() { return _ctxStr; }
    public  void setContextString(String ctxStr) { _ctxStr= ctxStr; }

    public boolean isNewPlot() { return _newPlot; }
    public void setNewPlot(boolean newPlot ) { _newPlot= newPlot; }

    public int getColorTableId() { return _colorTableId; }
    public void setColorTableId(int id ) { _colorTableId= id; }

    public void setThreeColor(boolean threeColor) { _threeColor= threeColor; }
    public boolean isThreeColor() { return _threeColor; }

    public int getThumbnailSize() {
        return get(firstBand()).getWebPlotRequest().getThumbnailSize();
    }

    public void setZoomLevel(float z) {_zoomLevel= z;}
    public float getZoomLevel() {return _zoomLevel;}

    public void setRotateType(RotateType rotationType) { _rotationType= rotationType; }
    public RotateType getRotateType() {return _rotationType;}

    public void setFlippedY(boolean flippedY) { _flippedY= flippedY; }
    public boolean isFlippedY() { return _flippedY; }

    public void setRotationAngle(double angle) { _rotationAngle= angle; }
    public double getRotationAngle() { return _rotationAngle; }

    public void setRotateNorthType(CoordinateSys csys) {
        _rotaNorthType= csys;
    }

    public CoordinateSys getRotateNorthType() {
        return _rotaNorthType;
    }

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
        _usedBands = null;
        if (initStretch) initColorStretch(plotRequests,band);
    }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param band the band to get the request for
     * @return the WebPlotRequest
     */
    public WebPlotRequest getWebPlotRequest(Band band) { return get(band).getWebPlotRequest(); }

    /**
     * this method will make a copy of the primary WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @return the WebPlotRequest
     */
    public WebPlotRequest getPrimaryWebPlotRequest() { return get(firstBand()).getWebPlotRequest(); }


    public void setBandVisible(Band band, boolean visible) { get(band).setBandVisible(visible); }
    public boolean isBandVisible(Band band) { return  get(band).isBandVisible(); }


    public boolean isMultiImageFile(Band band) { return get(band).isMultiImageFile(); }
    public void setMultiImageFile(boolean multiImageFile, Band band) { get(band).setMultiImageFile(multiImageFile); }

    public int getCubeCnt(Band band) { return get(band).getCubeCnt(); }
    public void setCubeCnt(int cubeCnt, Band band) { get(band).setCubeCnt(cubeCnt); }

    public int getCubePlaneNumber(Band band) { return get(band).getCubePlaneNumber(); }
    public void setCubePlaneNumber(int cubeIdx, Band band) { get(band).setCubePlaneNumber(cubeIdx); }


    public void setRangeValues(RangeValues rv, Band band) { get(band).setRangeValues(rv); }
    public RangeValues getRangeValues(Band band) { return get(band).getRangeValues(); }
    public RangeValues getPrimaryRangeValues() { return get(firstBand()).getRangeValues(); }

    public void setFitsHeader(MiniFitsHeader header, Band band) { get(band).setFitsHeader(header); }


    public FileAndHeaderInfo getFileAndHeaderInfo(Band band) {
        return get(band).getFileAndHeaderInfo();
    }


    public MiniFitsHeader getHeader(Band band) { return get(band).getHeader(); }


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

    public void addOperation(Operation op) {if (!_ops.contains(op)) _ops.add(op); }
    public void removeOperation(Operation op) {if (_ops.contains(op)) _ops.remove(op); }
    public boolean hasOperation(Operation op) {return _ops.contains(op); }
    public void clearOperations() {_ops.clear(); }
    public List<Operation> getOperations() { return _ops;}

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
        s+= "ctxStr: " + _ctxStr +
            ", zoom: " + _zoomLevel +
            ", color id: " + _colorTableId +
            ", 3 color: " + _threeColor;
        return s;

    }

    public String serialize() { return toString(); }

    public String toString() {

        StringBuilder sb= new StringBuilder(350);
        sb.append(_multiImage).append(SPLIT_TOKEN);
        sb.append(_ctxStr).append(SPLIT_TOKEN);
        sb.append(_newPlot).append(SPLIT_TOKEN);
        sb.append(_zoomLevel).append(SPLIT_TOKEN);
        sb.append(_threeColor).append(SPLIT_TOKEN);
        sb.append(_colorTableId).append(SPLIT_TOKEN);
        sb.append(_rotationType).append(SPLIT_TOKEN);
        sb.append(_rotationAngle).append(SPLIT_TOKEN);
        sb.append(_flippedY).append(SPLIT_TOKEN);
        sb.append(_rotaNorthType.toString()).append(SPLIT_TOKEN);

        for(int i= 0; (i<_bandStateAry.length); i++) {
                sb.append(_bandStateAry[i]==null ? null : _bandStateAry[i].serialize());
                if (i<_bandStateAry.length-1) sb.append(SPLIT_TOKEN);
        }
        return sb.toString();
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
            retval._multiImage= multiImage;
            retval._ctxStr= ctxStr;
            retval._newPlot= newPlot;
            retval._zoomLevel= zoomLevel;
            retval._threeColor= threeColor;
            retval._colorTableId= colorTableId;
            retval._rotationType= rotationType;
            retval._rotaNorthType= rotaNorthType;
            retval._rotationAngle= rotationAngle;
            retval._flippedY = flippedY;
            retval._bandStateAry= bandStateAry;

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
            if ( ComparisonUtil.equals(_bandStateAry, ps._bandStateAry) &&
                 ComparisonUtil.equals(_ctxStr, ps._ctxStr) &&
                 _zoomLevel==ps._zoomLevel &&
                 _threeColor==ps._threeColor &&
                 _colorTableId==ps._colorTableId) {
                retval= true;
            } // end if
        }
        return retval;
    }

    public void clearBand(Band band) {
        int idx= band.getIdx();
        if (_bandStateAry[idx]!=null) {
            _bandStateAry[idx]= null;
            setWebPlotRequest(null, band);
        }
    }

// =====================================================================
// -------------------- private Methods --------------------------------
// =====================================================================

    private BandState get(Band band) {
        int idx= band.getIdx();
        if (_bandStateAry[idx]==null) _bandStateAry[idx]= new BandState();
        return _bandStateAry[idx];
    }


    private void initColorStretch(WebPlotRequest request, Band band) {
        if (request!=null) {
            _colorTableId= request.getInitialColorTable();
            if (request.containsParam(WebPlotRequest.INIT_RANGE_VALUES)) {
                String rvStr= request.getParam(WebPlotRequest.INIT_RANGE_VALUES);
                RangeValues rv= RangeValues.parse(rvStr);
                get(band).setRangeValues(rv);
            }
        }

    }



}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
