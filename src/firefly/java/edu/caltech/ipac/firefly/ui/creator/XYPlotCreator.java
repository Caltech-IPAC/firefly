/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.*;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.graph.CustomMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tatianag
 *         1/14/2014
 */
public class XYPlotCreator implements ObsResultCreator {

    public static final String QUERY_ID= "QUERY_ID";

    public TablePreview create(Map<String, String> params) {
        return new XYPlotPreview(params);
    }

    public static class XYPlotPreview extends AbstractTablePreview {

        List<String> _tblSource;
        XYPlotWidget _xyPlotWidget;
        private DataSetTableModel _currentModel = null;

        private EventHub _hub;
        private WebEventListener _wel;

        public XYPlotPreview(Map<String,String> params) {
            _tblSource = DataViewCreator.getListParam(params, QUERY_ID);

            HashMap<String,String> xyPlotParams = new HashMap<String,String>();
            for (String p : params.keySet()) {
                if (CustomMetaSource.isValidParam(p)) {
                    xyPlotParams.put(p, params.get(p));
                }
            }

            XYPlotMeta meta = new XYPlotMeta("none", 0, 0, new CustomMetaSource(xyPlotParams));

            String maxPointsStr = params.get("maxPoints");
            if (maxPointsStr != null) {
                try {
                    int maxPoints = Integer.parseInt(maxPointsStr);
                    if (maxPoints >= 100) meta.setMaxPoints(maxPoints);
                } catch (Exception ignored) {}
            }

            _xyPlotWidget = new XYPlotWidget(meta);
            _xyPlotWidget.setTitleAreaAlwaysHidden(true);
            setDisplay(_xyPlotWidget);
        }


        @Override
        protected void updateDisplay(TablePanel table) {
            if (table != null && isSourceTbl(table)) {
                if ( !GwtUtil.isOnDisplay(getDisplay()) && !GwtUtil.isOnDisplay(table) )  return;
                if (table.getTable()!=null) {
                    DataSetTableModel tableModel = table.getDataModel();
                    if (!tableModel.equals(_currentModel)) {
                        _currentModel = tableModel;
                        _xyPlotWidget.makeNewChart(_currentModel, "XY Plot");
                    }
                }
            }
        }

        @Override
        public void bind(EventHub hub) {
            super.bind(hub);
            _hub = hub;
            _wel =  new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    Name evName= ev.getName();
                    if (evName.equals(EventHub.ON_DATA_LOAD) || evName.equals(EventHub.ON_TABLE_SHOW)) {
                        TablePanel table = (TablePanel) ev.getSource();
                        updateDisplay(table);
                     }
                    else if (evName.equals(EventHub.ON_TABLE_REMOVED)) {
                        TablePanel table = (TablePanel) ev.getData();
                        if (table != null && isSourceTbl(table)) {
                            _xyPlotWidget.removeCurrentChart();
                        }
                    }
                }
            };
            hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW,_wel);
            hub.getEventManager().addListener(EventHub.ON_DATA_LOAD, _wel);
            hub.getEventManager().addListener(EventHub.ON_TABLE_REMOVED, _wel);
            AllPlots.getInstance().registerPopout(_xyPlotWidget);
        }

        private boolean isSourceTbl(TablePanel table) {
            return _tblSource == null || (table != null && _tblSource.contains(table.getName()));
        }

        public void cleanup() {
            _hub.getEventManager().removeListener(EventHub.ON_TABLE_SHOW, _wel);
            _hub.getEventManager().removeListener(EventHub.ON_DATA_LOAD, _wel);
            _hub.getEventManager().removeListener(EventHub.ON_TABLE_REMOVED, _wel);
            AllPlots.getInstance().deregisterPopout(_xyPlotWidget);
            unbind();
        }

    }
}
