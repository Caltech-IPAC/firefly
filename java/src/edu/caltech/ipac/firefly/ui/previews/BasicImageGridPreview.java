package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.imageGrid.BasicImageGrid;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.ComparisonUtil;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Aug 9, 2010
 * Time: 4:19:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class BasicImageGridPreview extends AbstractTablePreview {
    private DockLayoutPanel container = new DockLayoutPanel(Style.Unit.PX);
    private BasicImageGrid grid;
    private boolean _isCatalogSelected = false;
    private String _currentTabName = null;
    private String _lastTabName = null;
    public BasicImageGridPreview(AbstractPreviewData previewData) {
        super(previewData.getTabTitle(), "");
        grid = new BasicImageGrid(previewData);
        //setDisplay(container);
        container.setSize("100%", "100%");
        container.add(grid);
        grid.setSize("100%", "100%");
        initWidget(container);
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);

        grid.bind(hub);                                                               
//        WebEventListener wel1 =  new WebEventListener(){
//                public void eventNotify(WebEvent ev) {
//                    if (grid.isFinderChartMode()) {
//                        if (ev.getSource() instanceof TablePanel) {
//                            //todo: finder chart
//                            TablePanel table = (TablePanel) ev.getSource();
//                            updateFinderChart(table);
//                        }
//                    } else {
//                        if (ev.getSource() instanceof TabPane)
//                            _currentTabName = ((TabPane)ev.getSource()).getSelectedTab().getName();
//                        updateDisplay(ev.getName().equals(EventHub.ON_ROWHIGHLIGHT_CHANGE));
//                    }
//                }
//            };

        WebEventListener<DataSet> wel2 =  new WebEventListener<DataSet>(){
                public void eventNotify(WebEvent<DataSet> ev) {
                    Object data = ev.getData();
                    if (data instanceof DataSet) {
                        loadTable((DataSet)data);
                        _lastTabName = _currentTabName;
                    }
                }
            };

//        WebEventListener wel3 =  new WebEventListener<DataSet>(){
//            public void eventNotify(WebEvent<DataSet> ev) {
//                    TablePanel table = (TablePanel) ev.getSource();
//                    _isCatalogSelected = isCatalog(table);
//                }
//        };
        hub.getEventManager().addListener(EventHub.ON_EVENT_WORKER_COMPLETE, wel2);
        hub.getEventManager().addListener(EventHub.ON_EVENT_WORKER_START, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        grid.clearTable();
                    }
                });
//        hub.getEventManager().addListener(EventHub.ON_TAB_SELECTED, wel1);
//        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel1);
//        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel3);

        //todo: ON_TABLE_SHOW: no grid.clearTable() called
    }

    protected void updateDisplay(TablePanel panel) {
    }

    protected void loadTable(DataSet data) {
        if (updateRequired()) {
            grid.updateDisplay();
            grid.loadTable(data);
        }
    }

    private void updateDisplay(boolean rowHighLightChanged) {
        if (updateRequired(rowHighLightChanged)) {
            grid.clearTable();
            grid.updateDisplay();
            _lastTabName = null;
        }
    }

    private boolean updateRequired(){
        return updateRequired(false);
    }

    private boolean updateRequired(boolean rowHighLightChanged) {
        if (_isCatalogSelected)
            return false;
        else if (ComparisonUtil.equals(_lastTabName, _currentTabName) && _currentTabName != null) {
            if (rowHighLightChanged)
                return true;
            else
                return false;
        } else
            return true;
    }

    private boolean isCatalog(TablePanel table) {

        boolean result= false;
        if (table!=null && table.getDataset()!=null) {
            TableMeta meta= table.getDataset().getMeta();
            boolean isCatalogOverlayType =  meta.contains(MetaConst.CATALOG_OVERLAY_TYPE);
            boolean isDataPrimary = meta.contains(MetaConst.DATA_PRIMARY);
            result = isCatalogOverlayType && !isDataPrimary;
        }

        return result;
    }
}

