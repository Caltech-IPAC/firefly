package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * User: roby
 * Date: Jul 16, 2010
 * Time: 12:52:28 PM
 */


/**
* @author Trey Roby
*/
public abstract class TableDataConnection implements DataConnection {

    private final TablePanel table;
    private final boolean _supportsHighlight;
    private final boolean _supportsAreaSelect;
    private final boolean _supportsFilter;
    private final boolean _supportsMouse;
    private final boolean _onlyIfTabActive;
    private final String _helpLine;
    private AsyncTableDataLoader dataLoader= null;
    private TableDataView tableDataView= null;
    private List<DrawObj> _lastDataReturn= null;

    public TableDataConnection(TablePanel table,String helpLine) {this(table,helpLine, true, false, false, true, false); }

    public TableDataConnection(TablePanel table,
                               String helpLine,
                               boolean supportsHighlight,
                               boolean supportsAreaSelect,
                               boolean supportsFilter,
                               boolean supportsMouse,
                               boolean onlyIfTabActive) {
        this.table = table;
        _helpLine= helpLine;
        _supportsHighlight = supportsHighlight;
        _supportsAreaSelect= supportsAreaSelect;
        _supportsFilter= supportsFilter;
        _supportsMouse = supportsMouse;
        _onlyIfTabActive = onlyIfTabActive;
    }

    public TablePanel getTable() { return table; }
    public TableDataView getTableDatView() { return tableDataView; }

    public boolean isPriorityLayer() { return false; }

    public String getTitle(WebPlot plot) {
            return !StringUtils.isEmpty(table.getShortDesc()) ? table.getShortDesc() : table.getName();
    }
    public int size() { return tableDataView!=null ? tableDataView.getSize() : 0; }
    public boolean isActive() { return true; }
    public void setHighlightedIdx(int idx) {
        getTable().highlightRow(true, idx);
    }

    public int getHighlightedIdx() {
        return table.getTable().getHighlightedRowIdx();
    }

    public void setSelectedIdx(Integer... idx) {
        getTable().getDataModel().getCurrentData().deselectAll();// TODO: would like to do this with the select, so next line just replaces the selection
        if (idx.length>0) {
            getTable().getDataModel().getCurrentData().select(idx);
            tableDataView.select(idx);
        }
    }

    public List<Integer> getSelectedIdx() {
        return getTable().getDataModel().getCurrentData().getSelected();
    }

    public void showDetails(int x, int y, int index) {}
    public void hideDetails() {}
    public WebEventManager getEventManager() { return table.getEventManager(); }

    public boolean getSupportsHighlight() { return _supportsHighlight; }
    public boolean getSupportsAreaSelect() { return _supportsAreaSelect; }
    public boolean getSupportsFilter() { return _supportsFilter; }

    public boolean getSupportsMouse() { return _supportsMouse; }
    public boolean getOnlyIfDataVisible() { return _onlyIfTabActive; }

    public boolean getHasPerPlotData() { return false; }
    public boolean isPointData() { return false; }
    public boolean isVeryLargeData() { return true; }

    public DrawConnector getDrawConnector() { return null; }
    public String getInitDefaultColor() { return null; }
    public String getHelpLine() { return _helpLine; }
    public boolean isDataVisible() { return GwtUtil.isOnDisplay(table); }

    public AsyncDataLoader getAsyncDataLoader() {
        if (dataLoader==null)  dataLoader= new AsyncTableDataLoader();
        return dataLoader;
    }

    public void filter(Integer... idxAry) {

        if (getTable()==null) return;
        DataSetTableModel model= getTable().getDataModel();
        if (model==null) return;

        StringBuilder sb;
            sb= new StringBuilder(20+ (idxAry.length*5));
            sb.append("ROWID IN (");
            TableData<TableData.Row> dataViewModel= tableDataView.getModel();
            for(int i= 0; (i<idxAry.length); i++) {
                sb.append(dataViewModel.getRow(idxAry[i]).getRowIdx());
                if (i<idxAry.length-1) sb.append(",");
            }
        sb.append(")");
        if (false) {
            model.setFilters(Arrays.asList(sb.toString()));
        }
        else {
            List<String> filterList= new ArrayList<String>(10);
            if (model.getFilters()!=null) filterList.addAll(model.getFilters());
            filterList.add(sb.toString());
            model.setFilters(filterList);
        }
        model.fireDataStaleEvent();

    }

    public List<DrawObj> getData(boolean rebuild, WebPlot p) {
        List<DrawObj> retval= null;
        if (tableDataView!=null) {
            if (_lastDataReturn==null || _lastDataReturn.size()==0 || rebuild) {
                _lastDataReturn= getDataImpl();
            }
            retval= _lastDataReturn;
        }
        return retval;
    }



    protected abstract List<String> getDataColumns();
    public abstract List<DrawObj> getDataImpl();

    private class AsyncTableDataLoader implements AsyncDataLoader {
        List<LoadCallback> loadCalls= new ArrayList<LoadCallback>(10);
        private boolean inProcess= false;
        public void requestLoad(LoadCallback cb) {
            if (tableDataView!=null) {
                cb.loaded();
            }
            else {
                loadCalls.add(cb);
                if (!inProcess) {
                    inProcess= true;
                    getTable().getDataModel().getAdHocData(new AsyncCallback<TableDataView>() {
                        public void onFailure(Throwable caught) {
                            inProcess= false;
                        }

                        public void onSuccess(TableDataView result) {
                            tableDataView= result;
                            inProcess= false;
                            for(LoadCallback cb : loadCalls) {
                                cb.loaded();
                            }
                            loadCalls.clear();
                        }
                    }, getDataColumns());
                }

            }
        }

        public void disableLoad() { /* ignore */ }
        public boolean isDataAvailable() { return tableDataView!=null; }
        public void markStale() { tableDataView= null; }
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
