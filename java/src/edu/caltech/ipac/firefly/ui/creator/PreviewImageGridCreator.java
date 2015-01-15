/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.previews.AbstractPreviewData;
import edu.caltech.ipac.firefly.ui.previews.BasicImageGridPreview;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Aug 9, 2010
 * Time: 2:58:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class PreviewImageGridCreator implements ObsResultCreator {
    final static String ZOOM_TO_WIDTH="ZoomToWidth";
    final static String HIDE_FAILURE_PLOT="HideFailurePlot";
    final static String UPDATE_PLOT_WIDGET_GROUP_STATUS ="UpdatePlotWidgetGroupStatus";
    final static String ENABLE_CHECKING="EnableChecking";
    final static String ENABLE_PDF_DOWNLOAD="EnablePdfDownload";
    final static String ENABLE_SHOW_DRAWING_LAYERS = "EnableShowDrawingLayers";
    final static String SELECT_ALL_PLOTS="SelectAllPlots";
    final static String PLOT_WIDGET_GROUP="PlotWidgetGroup";
    final static String ONLY_SHOWING_FILTERED_RESULTS="OnlyShowingFilteredResults";
    final static String GRID_POPOUT_COLS="GridPopoutCols";
    final static String GRID_POPOUT_ZOOMTYPE="GridPopoutZoomtype";
    
    public TablePreview create(Map<String, String> params) {
        ImageGridPreviewData previewData=makePreviewData(params);
        return new BasicImageGridPreview(previewData);
    }

    protected static void addCommonParams(ImageGridPreviewData prevData,
                                   Map<String, String> params) {
        prevData.setMouseReadout(parseReadoutString(params.get(CommonParams.READOUT)));
        prevData.setLockRelated(parseBoolean(params.get(CommonParams.LOCK_RELATED)));
        prevData.setUseScrollBars(parseBoolean(params.get(CommonParams.USE_SCROLL_BARS)));
        prevData.setPlotEventWorkerList(StringUtils.asList(params.get(CommonParams.PLOT_EVENT_WORKERS), ","));
        prevData.setZoomToWidth(params.get(ZOOM_TO_WIDTH));
        prevData.setHideFailurePlot(params.get(HIDE_FAILURE_PLOT));
        prevData.setUpdatePlotWidgetGroupStatus(params.get(UPDATE_PLOT_WIDGET_GROUP_STATUS));
        prevData.setEnableChecking(params.get(ENABLE_CHECKING));
        prevData.setEnablePdfDownload(params.get(ENABLE_PDF_DOWNLOAD));
        prevData.setOnlyShowingFilteredResults(params.get(ONLY_SHOWING_FILTERED_RESULTS));
        prevData.setShowDrawingLayers(params.get(ENABLE_SHOW_DRAWING_LAYERS));
        prevData.setSelectAllPlots(params.get(SELECT_ALL_PLOTS));
        prevData.setPlotWidgetGroup(params.get(PLOT_WIDGET_GROUP));
        prevData.setGridPopoutZoomtype(params.get(GRID_POPOUT_ZOOMTYPE));
        prevData.setGridPopoutCols(params.get(GRID_POPOUT_COLS));
        //if group title description param(s) available, add them into ImageGridPreviewData object.
        for (String key: params.keySet()) {
            if (key.startsWith("TitleDesc.")) {
                prevData.addTitleDesc(key, params.get(key));
            }
        }
    }

    private static boolean parseBoolean(String value) {
        boolean retval = false;
        if (value != null) {
            retval = value.trim().toLowerCase().equals("true");
        }
        return retval;
    }

    private static Map<Integer,String> parseReadoutString(String readout) {

        Map<Integer,String> retval= new HashMap<Integer,String>(11);
        if (readout==null ) return retval;
        List<String> l= StringUtils.asList(readout,",");
        for (String s : l) {
            Param param= Param.parse(s);
            if (param!=null) {
                try {
                    int v= Integer.parseInt(param.getName());
                    retval.put(v,param.getValue());
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return retval;
    }

    public static ImageGridPreviewData makePreviewData(Map<String, String> params) {
        ImageGridPreviewData igpd = new ImageGridPreviewData();
        addCommonParams(igpd,params);
        return igpd;
    }

    public static class ImageGridPreviewData extends AbstractPreviewData {
        private Map<String, String> _titleDescMap = new HashMap<String, String>();
        private List<String> _plotEventWorkerList= null;
        private ArrayList<String> _surveys = null;
        private Integer _zoomToWidth = null;
        private boolean _hideFailurePlot = false;
        private boolean _updatePlotWidgetGroupStatus = false;
        private boolean _enableChecking = true;
        private boolean _enablePdfDownload = false;
        private boolean _onlyShowingFilteredResults = false;
        private boolean _enableShowDrawingLayers = false;
        private boolean _selectAllPlots = false;
        private String _plotGroup = null;
        private String _gridPopoutZoomtype = "ONLY_WIDTH"; //ONLY_WIDTH, WIDTH_HEIGHT, ONLY_HEIGHT, SMART
        private int _gridPopoutCols = 4;

        public void setSurvey(String surveys) {
            if (_surveys==null) {
                _surveys = new ArrayList<String>();
            }
            if (surveys.contains(",")) {
                for (String survey: surveys.split(","))
                    _surveys.add(survey.trim());
            }
        }

        public void addTitleDesc(String key, String value) {
            _titleDescMap.put(key, value);
        }

        public void clearTitleDesc() {
            _titleDescMap.clear();
        }

        public String getTitleDesc(String key) {
            String retval = "";
            if (_titleDescMap.containsKey(key)) {
                retval = _titleDescMap.get(key);
            }
            return retval;
        }

        public String getTabTitle() {
            return "Image Grid";
        }

        public String getTip() {
            return "";
        }

        public boolean getHasPreviewData(String id, List<String> colNames, Map<String, String> ma) {
            boolean retval= false;
            if (ma!=null && getSourceList().contains(id)) {
                retval= TableMeta.getCenterCoordColumns(ma)!=null;
            }
            return retval;
        }

        public void postPlot(MiniPlotWidget mpw, WebPlot plot) {
            if (getMouseReadout()!=null) {
                plot.getPlotView().setAttribute(WebPlot.READOUT_ROW_PARAMS, getMouseReadout());
            }
        }
        public Info createRequestForRow(TableData.Row<String> row, Map<String, String> metaAttributes, List<String> columns) {
            return null;
        }

        public void setPlotEventWorkerList(List<String> plotEventWorkerList) {
            _plotEventWorkerList=plotEventWorkerList;
        }

        public List<String> getPlotEventWorkerList() { return _plotEventWorkerList;}

        public Integer getZoomToWidth() {return _zoomToWidth;}
        public void setZoomToWidth(String s) {
            if (s!=null && s.length()>0) {
                int zoomToWidth= Integer.parseInt(s);
                _zoomToWidth = zoomToWidth;
            }
        }


        public boolean getUpdatePlotWidgetGroupStatus() { return _updatePlotWidgetGroupStatus;}
        public void setUpdatePlotWidgetGroupStatus(String s) {
            if (s!=null) {
                _updatePlotWidgetGroupStatus = Boolean.parseBoolean(s);
            }
        }

        public boolean getHideFailurePlot() { return _hideFailurePlot;}
        public void setHideFailurePlot(String s) {
            if (s!=null) {
                _hideFailurePlot = Boolean.parseBoolean(s);
            }
        }

        public boolean getEnableChecking() { return _enableChecking;}
        public void setEnableChecking(String s) {
            if (s!=null) {
                _enableChecking = Boolean.parseBoolean(s);
            }
        }

        public boolean getEnablePdfDownload() { return _enablePdfDownload;}
        public void setEnablePdfDownload(String s) {
            if (s!=null) {
                _enablePdfDownload = Boolean.parseBoolean(s);
            }
        }
        public boolean getOnlyShowingFilteredResults() {return _onlyShowingFilteredResults;}
        public void setOnlyShowingFilteredResults(String s) {
            if (s!=null) {
                _onlyShowingFilteredResults = Boolean.parseBoolean(s);
            }
        }

        public boolean getShowDrawingLayers() {return _enableShowDrawingLayers;}
        public void setShowDrawingLayers(String s) {
            if (s!=null) {
                _enableShowDrawingLayers = Boolean.parseBoolean(s);
            }
        }

        public boolean getSelectAllPlots() { return _selectAllPlots;}
        public void setSelectAllPlots(String s) {
            if (s!=null) {
                _selectAllPlots = Boolean.parseBoolean(s);
            }
        }

        public String getPlotWidgetGroup() { return _plotGroup;}
        public void setPlotWidgetGroup(String s) {
            if (s!=null) {
                _plotGroup = s;
            }
        }

        public String getGridPopoutZoomtype() { return _gridPopoutZoomtype;}
        public void setGridPopoutZoomtype(String s) {
            if (s!=null) {
                _gridPopoutZoomtype = s;
            }
        }

        public int getGridPopoutCols() { return _gridPopoutCols;}
        public void setGridPopoutCols(String s) {
            if (s!=null) {
                _gridPopoutCols = Integer.parseInt(s);
            }
        }
    }

}
