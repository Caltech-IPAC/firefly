package edu.caltech.ipac.firefly.ui.previews;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotRelatedPanel;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Aug 25, 2010
 * Time: 12:31:34 PM
 */


/**
 * @author Trey Roby
 */
public abstract class AbstractPreviewData implements PreviewData {

    public static final String DEFAULT= "Default";
    public static final String META_ADJUST = "MetaAdjust";
    public static final float USE_META_VALUE= -1.0F;


    public static final String STRETCH_LINEAR  = "Linear";
    public static final String STRETCH_LOG     = "Log";
    public static final String STRETCH_LOGLOG  = "LogLog";
    public static final String STRETCH_EQUAL   = "HistEqual";
    public static final String STRETCH_SQUARED = "Squared";
    public static final String STRETCH_SQRT    = "Sqrt";
    public static final String TYPE            = "Type";
    public static final String MIN_PERCENT     = "min%";
    public static final String MAX_PERCENT     = "max%";
    public static final String MIN_SIGMA       = "minSigma";
    public static final String MAX_SIGMA       = "maxSigma";
    public static final String ZSCALE          = "zscale";



    private List<String> _sourceList;
    private List<String> _eventWorkerList = new ArrayList<String>(3);
    private PlotRelatedPanel _prpUI= null;
    private int _minWidth= 0;
    private int _minHeight= 1;
    private String _group= null;
    private Map<String,Float> _zoomLevels= new HashMap<String,Float>(5);
    private Map<String,String> _prefColorKeys = new HashMap<String,String>(5);
    private Map<String,String> _prefZoomKeys = new HashMap<String,String>(5);
    private Map<String,String> _extraPlotRequestParams = null;
    private String _title;
    private boolean _threeColor= false;
    private List<String> _limitTableParam;
    private List<String> _headerParams;
    private boolean _centerOnQueryTarget= false;
    private int _colorTableID= 0;
    private RangeValues _rv= null;
    private Map<Integer,String> _readoutParams= null;
    private boolean _failShowPrevious= false;
    private boolean _lockRelated = false;
    private boolean _useSB = false;
    private boolean _imageSel = false;
    private boolean _enableDecimation = false;
    private boolean _rotateNorthUp = false;
    private String _toolbarPlot = null;
    private boolean _rememberPrefs= false;
    private String _zoomMetaTag= null;
    private String _prefColorMetaTag = null;
    private CommonParams.DataSource _dataSource= null;
    private String _dataColumn= null;

    public AbstractPreviewData() {
    }



    public void setLockRelated(boolean value) { _lockRelated = value; }

    public boolean getLockRelated() { return _lockRelated; }

    public void setUseScrollBars(boolean value) { _useSB = value; }

    public boolean getUseScrollBars() { return _useSB; }

    public void setRotateNorthUp(boolean up) { _rotateNorthUp = up; }
    public boolean isRotateNorthUp() { return _rotateNorthUp; }

    public void setImageSelection(boolean sel) { _imageSel = sel; }

    public boolean getImageSelection() { return _imageSel; }

    public void setRememberPrefs(boolean remember) { _rememberPrefs = remember; }

    public void setExtraPlotRequestParams(Map<String,String> params) {
        _extraPlotRequestParams= params;
    }

    public Map<String,String> getExtraPlotRequestParams() { return _extraPlotRequestParams; }

    public boolean getRememberPrefs() { return _rememberPrefs; }

    public void setEnableDecimation(boolean enableDecimation) {
       _enableDecimation= enableDecimation;
    }

    public CommonParams.DataSource getDataSource() { return _dataSource; }
    public void setDataSource(CommonParams.DataSource dataSource) { _dataSource= dataSource; }

    public String getDataColumn() { return _dataColumn; }
    public void setDataColumn(String dataColumn) { _dataColumn= dataColumn; }



    public void setSourceList(List<String> sourceList) {
        _sourceList= (sourceList==null) ? Collections.<String>emptyList() : sourceList;
    }
    public List<String> getSourceList() { return _sourceList; }



    public void setLimitTableParams(List<String> limitTableParam) {
        _limitTableParam= (limitTableParam==null) ? Collections.<String>emptyList() : limitTableParam;
    }


    public void setHeaderParams(List<String> headerParams) {
        _headerParams= (headerParams==null) ? Collections.<String>emptyList() : headerParams;
    }

    public List<String> getLimitTableParams() { return _limitTableParam; }
    public List<String> getHeaderParams() { return _headerParams; }

    public void setColorTableID(int id) {
        if (id==Integer.MAX_VALUE) id= 0;
        _colorTableID= id;
    }

    public int getColorTableID() { return _colorTableID; }

    public void setThreeColor(boolean threeColor) { _threeColor= threeColor; }
    public boolean isThreeColor() { return _threeColor; }

    public void setToolBarPlot(String toolbarPlot ) { _toolbarPlot= toolbarPlot; }

    public void setCenterOnQueryTarget(boolean c) {
        _centerOnQueryTarget= c;
    }

    public void setStretchStr(String sStr) {
        if (sStr==null) {
            _rv= null;
            return;
        }
        List<String> l= StringUtils.asList(sStr,",");
        int rvType= RangeValues.STRETCH_LINEAR;
        double min= 0D;
        double max= 100D;
        int minMaxType= RangeValues.PERCENTAGE;
        for (String s : l) {
            Param param= Param.parse(s);
            if (param!=null) {
                if (param.getName().equals(TYPE)) {

                    String tStr= param.getValue();
                    if (tStr.equals(STRETCH_LINEAR)) {
                        rvType= RangeValues.STRETCH_LINEAR;
                    }
                    else if (tStr.equals(STRETCH_LOG)) {
                        rvType= RangeValues.STRETCH_LOG;
                    }
                    else if (tStr.equals(STRETCH_LOGLOG)) {
                        rvType= RangeValues.STRETCH_LOGLOG;
                    }
                    else if (tStr.equals(STRETCH_EQUAL)) {
                        rvType= RangeValues.STRETCH_EQUAL;
                    }
                    else if (tStr.equals(STRETCH_SQUARED)) {
                        rvType= RangeValues.STRETCH_SQUARED;
                    }
                    else if (tStr.equals(STRETCH_SQRT)) {
                        rvType= RangeValues.STRETCH_SQRT;
                    }
                    else {
                        rvType= RangeValues.STRETCH_LINEAR;
                    }

                }
                else if (param.getName().equals(MIN_PERCENT)) {
                    try {
                        min= Double.parseDouble(param.getValue());
                    } catch (NumberFormatException e) {
                        min= 0D;
                    }
                    minMaxType= RangeValues.PERCENTAGE;

                }
                else if (param.getName().equals(MAX_PERCENT)) {
                    try {
                        max= Double.parseDouble(param.getValue());
                    } catch (NumberFormatException e) {
                        max= 100D;
                    }
                    minMaxType= RangeValues.PERCENTAGE;
                }
                else if (param.getName().equals(MIN_SIGMA)) {
                    try {
                        min= Double.parseDouble(param.getValue());
                    } catch (NumberFormatException e) {
                        min= -2D;
                    }
                    minMaxType= RangeValues.SIGMA;

                }
                else if (param.getName().equals(MAX_SIGMA)) {
                    try {
                        max= Double.parseDouble(param.getValue());
                    } catch (NumberFormatException e) {
                        max= 10D;
                    }
                    minMaxType= RangeValues.SIGMA;
                }
                else if (param.getName().equals(ZSCALE)) {
                    minMaxType= RangeValues.ZSCALE;
                }
            }
        }

        _rv= new RangeValues(minMaxType,min,minMaxType,max,rvType);
    }


    public boolean getCenterOnQueryTarget() { return _centerOnQueryTarget; }

    public void setMouseReadout(Map<Integer,String> readoutParams) {
        if (readoutParams==null || readoutParams.size()==0) {
            _readoutParams= null;
        }
        else {
            _readoutParams= readoutParams;
        }
    }

    public Map<Integer,String> getMouseReadout() {
       return _readoutParams;
    }

    public void setTitle(String title) {
        _title= title==null ? "" : title; 
    }
    public String getTitle() { return _title; }

    public void setGroup(String group) { _group= group; }
    public String getGroup() { return _group; }

    public void setMinSize(int w, int h) {
        _minWidth= w;
        _minHeight= h;
    }

    public boolean getSaveImageCorners() { return true; }

    public int getMinWidth() { return _minWidth;}
    public int getMinHeight() { return _minHeight;}

    public void setZoomMetaTag(String metaTag) { _zoomMetaTag = metaTag; }
    public void setPrefColorMetaTag(String metaTag) { _prefColorMetaTag = metaTag; }

    public void setZoomLevel(String key,float zl) { _zoomLevels.put(key,zl);}
    public float getZoomLevel(String key) {
        float retval= Float.NaN;
        if (_zoomLevels.containsKey(key)) {
            retval= _zoomLevels.get(key);
        }
        else if (_zoomLevels.containsKey(DEFAULT)) {
            retval= _zoomLevels.get(DEFAULT);
        }
        return retval;
    }


    protected Map<String,String> getMetaAttributes(TablePanel table) {
        TableMeta meta= table.getDataset().getMeta();
        return meta.getAttributes();
    }


    protected float computeZoomLevel(Map<String,String> metaAttributes) {
        float zl= getZoomLevel(DEFAULT);
        if (_zoomMetaTag!=null  && metaAttributes.containsKey(_zoomMetaTag)) {
            zl= getZoomLevel(metaAttributes.get(_zoomMetaTag));
        }
        return zl;
    }

    protected String computeColorPrefKey(Map<String,String> metaAttributes) {
        String prefKey= getColorPreferenceKey(DEFAULT);
        if (_prefColorMetaTag !=null  && metaAttributes.containsKey(_prefColorMetaTag)) {
            prefKey= getColorPreferenceKey(metaAttributes.get(_prefColorMetaTag));
        }
        return prefKey;
    }

    protected String computeZoomPrefKey(Map<String,String> metaAttributes) {
        String prefKey= getTitle() + "-Zoom-" + DEFAULT;
        if (_zoomMetaTag!=null  && metaAttributes.containsKey(_zoomMetaTag)) {
            prefKey= getTitle() + "-Zoom-" +  metaAttributes.get(_zoomMetaTag);
        }
        return prefKey;
    }

//    protected float computeZoomLevel(TablePanel table) {
//        float metaZoomLevel= Float.NaN;
//        float metaAdjust= 1.0F;
//        boolean enableMetaAdjust= true;
//        TableMeta meta= table.getDataset().getMeta();
//        if (meta.contains(CommonParams.ZOOM)) {
//            try {
//                metaZoomLevel = Float.parseFloat(meta.getAttribute(CommonParams.ZOOM));
//            } catch (NumberFormatException e) { /* do nothing */ }
//        }
//
//
//        if (meta.contains(CommonParams.ZOOM_META_ADJUST)) {
//            try {
//                enableMetaAdjust = Boolean.parseBoolean(meta.getAttribute(CommonParams.ZOOM_META_ADJUST)) ;
//            } catch (NumberFormatException e) { /* do nothing */ }
//        }
//
//        if (enableMetaAdjust && _zoomLevels.containsKey(META_ADJUST)) {
//            try {
//                metaAdjust= _zoomLevels.get(META_ADJUST);
//            } catch (NumberFormatException e) { /* do nothing */ }
//        }
//
//        float zl= getZoomLevel(table.getName());
//        if (zl == USE_META_VALUE) {
//            zl= metaZoomLevel * metaAdjust;
//        }
//        else  if (_zoomLevels.get(DEFAULT)==USE_META_VALUE && !Float.isNaN(metaZoomLevel)) {
//            zl= metaZoomLevel * metaAdjust;
//        }
//        return zl;
//    }


    public List<String> getEventWorkerList() {
        return _eventWorkerList;
    }


    public void setEventWorkerList(List<String> l) {
        _eventWorkerList = l;
    }


    public PlotRelatedPanel[] getExtraPanels() {

        if (_prpUI !=null) {
            return new PlotRelatedPanel[] {_prpUI};
        }
        else {
            return null;
        }
    }
    public void setExtraPanel(PlotRelatedPanel prp) {
        _prpUI= prp;
    }

    public RangeValues getRangeValues() { return _rv; }


    public void setPlotFailShowPrevious(boolean failShowPrevious) { _failShowPrevious= failShowPrevious;}
    public boolean getPlotFailShowPrevious() { return _failShowPrevious;   }


    public void setColorPreferenceKey(String key, String prefName) { _prefColorKeys.put(key,prefName); }

    public String getColorPreferenceKey(String key) {
        String retval= null;
        if (_prefColorKeys.containsKey(key)) {
            retval= _prefColorKeys.get(key);
        }
        else if (_zoomLevels.containsKey(DEFAULT)) {
            retval= _prefColorKeys.get(DEFAULT);
        }
        return retval;
    }


    public void setZoomPreferenceKey(String key, String prefName) { _prefZoomKeys.put(key,prefName); }

    public String getZoomPreferenceKey(String key) {
        String retval= null;
        if (_prefZoomKeys.containsKey(key)) {
            retval= _prefZoomKeys.get(key);
        }
        else if (_zoomLevels.containsKey(DEFAULT)) {
            retval= _prefZoomKeys.get(DEFAULT);
        }
        return retval;
    }



    public void prePlot(MiniPlotWidget mpw, Map<String, String> metaAttributes) {
        if (_rememberPrefs) {
            mpw.setPreferenceColorKey(computeColorPrefKey(metaAttributes));
            mpw.setPreferenceZoomKey(computeZoomPrefKey(metaAttributes));
        }
        else {
            mpw.setPreferenceColorKey(null);
            mpw.setPreferenceZoomKey(null);
        }
    }

    public void postPlot(MiniPlotWidget mpw, WebPlot plot) {
        WebPlotView pv= plot.getPlotView();

        if (getMouseReadout()!=null) {
            pv.setAttribute(WebPlot.READOUT_ROW_PARAMS, getMouseReadout());
        }
        mpw.setImageSelection(_imageSel);
//        mpw.setShowScrollBars(getUseScrollBars());
        if (getLockRelated()) mpw.getGroup().setLockRelated(true);

        if (getCenterOnQueryTarget()) {
            ActiveTarget at= ActiveTarget.getInstance();
            if (at.getPos()!=null) {
                ImageWorkSpacePt ipt= plot.getImageWorkSpaceCoords(at.getPos());
                if (plot.pointInPlot(ipt)) {
                    pv.centerOnPoint(ipt);
                }
            }
        }
    }
}

