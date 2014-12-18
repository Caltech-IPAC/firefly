package edu.caltech.ipac.firefly.ui.previews;

import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotRelatedPanel;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: roby
 * Date: Apr 13, 2010
 * Time: 11:17:40 AM
 */
public interface PreviewData {

    enum Type { SPECTRUM, FITS }

    public Info createRequestForRow(TableData.Row<String> row,
                                    Map<String, String> metaAttributes,
                                    List<String> columns);
    public boolean getHasPreviewData(String id,
                                     List<String> colNames,
                                     Map<String, String> metaAttributes);
    public String getTabTitle();
    public String getTip();
    public void prePlot(MiniPlotWidget mpw, Map<String, String> metaAttributes);
    public void postPlot(MiniPlotWidget mpw, WebPlot plot);
    public boolean isThreeColor();
    public String getGroup();
    public int getMinWidth();
    public int getMinHeight();
    public PlotRelatedPanel[] getExtraPanels();
    public List<String> getEventWorkerList();
    public boolean getPlotFailShowPrevious();
    public boolean getSaveImageCorners();

    public static class Info {
        private final Type _type;
        private final Map<Band,WebPlotRequest> _reqMap;
        private final boolean _threeColor;

        public Info(Type type, WebPlotRequest request) {
            _type= type;
            _reqMap= new HashMap<Band,WebPlotRequest>(2);
            _reqMap.put(Band.NO_BAND,request);
            _threeColor= false;
        }

        public Info(Type type, Map<Band,WebPlotRequest> reqMap, boolean threeColor) {
            _type= type;
            _reqMap= reqMap;
            _threeColor= threeColor;
        }

        public Type getType() { return _type; }

        public Map<Band,WebPlotRequest> getRequestMap() { return _reqMap; }

        public boolean isThreeColor() {return _threeColor;}
    }
}
