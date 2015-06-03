/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.previews;

import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; /**
 * User: roby
 * Date: Apr 20, 2010
 * Time: 9:27:43 AM
 */


/**
 * @author Trey Roby
 */
public abstract class AbstractCoverageData implements CoverageData {

    public static final String DEFAULT_VALUE= "AllTablesDefaultValueInternal";
    private String _group= null;
    private TableMeta.LonLatColumns[] _fallbackCornerCols = null;
    private TableMeta.LonLatColumns _fallbackCenterCol= null;
    private boolean _multi= false;
    private Map<String,String> _colorMap= new HashMap<String, String>(5);
    private Map<String,DrawSymbol> _shapeMap= new HashMap<String, DrawSymbol>(5);
    private Map<String,String> _highlightColorMap = new HashMap<String, String>(5);
    private Map<String,Integer> _symbolSize= new HashMap<String, Integer>(5);
    private int _minWidth= 0;
    private int _minHeight= 1;
    private boolean _useBlankPlot= false;
    private boolean treatCatalogsAsOverlays= true;
    private WorldPt queryCenter;
    private WebPlotRequest.GridOnStatus gridOn= WebPlotRequest.GridOnStatus.FALSE;
    private FitType fitType= FitType.WIDTH;
    private boolean minimalReadout= false;
    private String title= "Coverage";
    private String tip= "Shows the coverage of the table";
    private boolean useTitleForPlot= false;

    public AbstractCoverageData() {
        _colorMap.put(DEFAULT_VALUE,AutoColor.PT_1);
        _shapeMap.put(DEFAULT_VALUE, DrawSymbol.X);
        _highlightColorMap.put(DEFAULT_VALUE,AutoColor.HIGHLIGHTED_PT);
        _symbolSize.put(DEFAULT_VALUE, PointDataObj.DEFAULT_SIZE);
    }

    public void setMinSize(int w, int h) {
        _minWidth= w;
        _minHeight= h;
    }

    public int getMinWidth() { return _minWidth;}
    public int getMinHeight() { return _minHeight;}

    public boolean isTreatCatalogsAsOverlays() { return treatCatalogsAsOverlays; }

    public WorldPt getQueryCenter() { return queryCenter; }
    public void setQueryCenter(WorldPt queryCenter) { this.queryCenter = queryCenter; }

    public void setTreatCatalogsAsOverlays(boolean treatCatalogsAsOverlays) {
        this.treatCatalogsAsOverlays = treatCatalogsAsOverlays;
    }


    public String getTitle() { return title; }
    public void setTitle(String  title) { this.title= title; }


    public String getTip() { return tip; }
    public void setTip(String  tip) { this.tip= tip; }

    public boolean isUseTitleForPlot() { return useTitleForPlot; }
    public void setUseTitleForPlot(boolean useTitleForPlot) { this.useTitleForPlot = useTitleForPlot; }

    public WebPlotRequest.GridOnStatus getGridOn() { return gridOn; }
    public void setGridOn(WebPlotRequest.GridOnStatus gridOn) { this.gridOn = gridOn; }

    public boolean isMinimalReadout() { return minimalReadout; }
    public void setMinimalReadout(boolean minimalReadout) { this.minimalReadout = minimalReadout; }

    public FitType getFitType() { return fitType; }

    public void setFitType(FitType fitType) { this.fitType = fitType; }

    public void enableDefaultColumns() {
            initFallbackCol("ra", "dec",
                            "ra1","dec1",
                            "ra2","dec2",
                            "ra3","dec3",
                            "ra4","dec4");

    }

    public void initFallbackCol(String cRA,
                                String cDec) {
        initFallbackCol(cRA,cDec,null,null,null,null,null,null,null,null);
    }


    public void initFallbackCol(String cRA,
                                String cDec,
                                String ra1,
                                String dec1,
                                String ra2,
                                String dec2,
                                String ra3,
                                String dec3,
                                String ra4,
                                String dec4 ) {
        if ( ra1!=null && dec1!=null && ra2!=null && dec2!=null &&
             ra3!=null && dec3!=null && ra4!=null && dec4!=null ) {
            _fallbackCornerCols = new TableMeta.LonLatColumns[] {
                    new TableMeta.LonLatColumns(ra1, dec1, CoordinateSys.EQ_J2000),
                    new TableMeta.LonLatColumns(ra2, dec2, CoordinateSys.EQ_J2000),
                    new TableMeta.LonLatColumns(ra3, dec3, CoordinateSys.EQ_J2000),
                    new TableMeta.LonLatColumns(ra4, dec4, CoordinateSys.EQ_J2000)
            };
        }

        if ( cRA!=null && cDec!=null) {
            _fallbackCenterCol= new TableMeta.LonLatColumns(cRA, cDec, CoordinateSys.EQ_J2000);
        }
    }

    public boolean canDoCorners(TableCtx table) {
        Map<String,String> meta= table.getMeta();
        TableMeta.LonLatColumns col[]= TableMeta.getCorners(meta, MetaConst.ALL_CORNERS);
        boolean doesCorners= (col!=null && col.length>1);
        if (!doesCorners) { // if not meta corners defined then decide if we can use the fallback
            TableMeta.LonLatColumns centerCol= TableMeta.getCenterCoordColumns(meta);
            /*
             * if meta does define a center and I have a fallback corners then return true
             * this is because not defining a center probably means there is no meta specified
             * a center define means that the corners where intentionally not defined
             */
            if (!containCenter(centerCol) && _fallbackCornerCols !=null) {
                List<String> cList= table.getColumns();
                doesCorners= true;
                for(TableMeta.LonLatColumns llC : _fallbackCornerCols) {
                    if ( !cList.contains(llC.getLatCol()) || !cList.contains(llC.getLonCol())) {
                        doesCorners= false;
                        break;
                    }
                }
            }
            else if (meta.containsKey(CommonParams.HAS_COVERAGE_DATA)) {
                doesCorners= true;
            }
        }
        return doesCorners;
    }

    private boolean containCenter(TableMeta.LonLatColumns centerCol) {
        boolean retval= false;
        if (centerCol!=null) {
            String lat= centerCol.getLatCol();
            String lon= centerCol.getLonCol();
            retval= !StringUtils.isEmpty(lon) && !StringUtils.isEmpty(lat) &&
                     !lon.equals("null") && !lat.equals("null");
        }
        return retval;
    }

    public List<WorldPt[]> modifyBox(WorldPt[] pts, TableCtx table, TableData.Row<String> row) {
        List<WorldPt[]> list= new ArrayList<WorldPt[]>(1);
        list.add(pts);
        return list;
    }


    public TableMeta.LonLatColumns[] getCornersColumns(TableCtx table) {
        TableMeta.LonLatColumns[] retval= null;
        if (canDoCorners(table)) {
            retval= TableMeta.getCorners(table.getMeta(), MetaConst.ALL_CORNERS);
            if (retval==null && _fallbackCornerCols !=null) {
                retval= _fallbackCornerCols;
            }
        }
        return retval;
    }

    public TableMeta.LonLatColumns getCenterColumns(TableCtx table) {
        TableMeta.LonLatColumns retval= TableMeta.getCenterCoordColumns(table.getMeta());
        if (retval==null && _fallbackCenterCol!=null) {
            retval= _fallbackCenterCol;
        }
        if (retval!=null && retval.getLonCol().equals("null") && retval.getLatCol().equals("null")) {
            retval= _fallbackCenterCol;
        }
        return retval;
    }

    public List<String> getExtraColumns() { return new ArrayList<String>(0); }

    public List<String> getEventWorkerList() { return null; }

    public ZoomType getSmartZoomHint() { return ZoomType.SMART; }


    public void setGroup(String group) { _group= group; }
    public String getGroup() { return _group; }


    public void setShape(String id, DrawSymbol ds) {
        if (id!=null && ds!=null) _shapeMap.put(id,ds);
    }

    public DrawSymbol getShape(String id) {
        DrawSymbol retval= _shapeMap.get(DEFAULT_VALUE);
        if (_shapeMap.containsKey(id)) retval= _shapeMap.get(id);
        return retval;
    }

    public void setColor(String id, String c) {
        if (id!=null && c!=null) _colorMap.put(id,c);
    }

    public String getColor(String id) {
        String retval= _colorMap.get(DEFAULT_VALUE);
        if (_colorMap.containsKey(id)) retval= _colorMap.get(id);
        return retval;
    }

    public void setHighlightedColor(String id, String c) {
        if (id!=null && c!=null) _highlightColorMap.put(id,c);
    }

    public String getHighlightedColor(String id) {
        String retval= _highlightColorMap.get(DEFAULT_VALUE);
        if (_highlightColorMap.containsKey(id)) retval= _highlightColorMap.get(id);
        return retval;
    }

    public void setSymbolSize(String id, int size) {
        if (id!=null && (size>1&& size<30)) _symbolSize.put(id,size);
    }

    public int getSymbolSize(String id) {
        int retval= _symbolSize.get(DEFAULT_VALUE);
        if (_symbolSize.containsKey(id)) retval= _symbolSize.get(id);
        return retval;
    }

    public boolean isMultiCoverage() { return _multi; }
    public void setMultiCoverage(boolean multi) { _multi= multi; }


    public boolean getUseBlankPlot() { return _useBlankPlot;  }
    public void setUseBlankPlot(boolean useBlankPlot) { _useBlankPlot= useBlankPlot;  }



    protected TableMeta.LonLatColumns getFallbackCenterCol() {  return _fallbackCenterCol; }
}

