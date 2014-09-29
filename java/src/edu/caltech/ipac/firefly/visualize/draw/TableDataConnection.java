package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.decimate.DecimateKey;

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

    private static String WEIGHT = "weight";
    private static String ROWIDX= "rowidx";
    private static final int MAX_UNDECIMATED= 30000;
    private final TablePanel table;
    private final boolean _supportsHighlight;
    private final boolean _supportsAreaSelect;
    private final boolean _supportsFilter;
    private final boolean _supportsMouse;
    private final boolean _onlyVisibleIfTabActive;
    private final String _helpLine;
    private AsyncTableDataLoader dataLoader= null;
    private TableDataView tableDataView= null;
    private List<DrawObj> _lastDataReturn= null;
    private List<String> _subGroupList= null;

    public TableDataConnection(TablePanel table,String helpLine) {this(table,helpLine, true, false, false, true, false); }

    public TableDataConnection(TablePanel table,
                               String helpLine,
                               boolean supportsHighlight,
                               boolean supportsAreaSelect,
                               boolean supportsFilter,
                               boolean supportsMouse,
                               boolean onlyVisibleIfTabActive) {
        this.table = table;
        _helpLine= helpLine;
        _supportsHighlight = supportsHighlight;
        _supportsAreaSelect= supportsAreaSelect;
        _supportsFilter= supportsFilter;
        _supportsMouse = supportsMouse;
        _onlyVisibleIfTabActive = onlyVisibleIfTabActive;
    }

    public void setSubgroupList(List<String> subGroupList) {
       _subGroupList= subGroupList;
    }

    public boolean getOKForSubgroups() { return true; }

    public List<String> getDefaultSubgroupList() { return _subGroupList; }

    public TablePanel getTable() { return table; }
    public TableDataView getTableDatView() { return tableDataView; }

    public boolean isPriorityLayer() { return false; }

    public String getTitle(WebPlot plot) {
            return !StringUtils.isEmpty(table.getShortDesc()) ? table.getShortDesc() : table.getName();
    }
    public int size() { return tableDataView!=null ? tableDataView.getSize() : 0; }
    public boolean isActive() { return true; }

    public void setHighlightedIdx(int idx) {
        int idxToSet= idx;
        if (isDecimating()) {
            try {
                TableData.Row<String> r=getTableDatView().getModel().getRow(idx);
                idxToSet= Integer.parseInt(r.getValue(ROWIDX));
            } catch (NumberFormatException e) {
                idxToSet= -1;
            }
        }
        if (idxToSet>-1) getTable().highlightRow(true, idxToSet);
    }


    public void setSelectedIdx(Integer... idxAry) {
        getTable().getDataModel().getCurrentData().deselectAll();
        if (idxAry.length>0 && !isDecimating()) {
            getTable().getDataModel().getCurrentData().select(idxAry);
            tableDataView.select(idxAry);
        }
    }

    public List<Integer> getSelectedIdx() {
        return isDecimating() ?
               new ArrayList<Integer>(1) :
               getTable().getDataModel().getCurrentData().getSelected();
    }

    public void showDetails(int x, int y, int index) {}
    public void hideDetails() {}
    public WebEventManager getEventManager() { return table.getEventManager(); }

    public boolean getSupportsHighlight() { return _supportsHighlight; }
    public SelectSupport getSupportsAreaSelect() {
        if (_supportsAreaSelect) {
            return isDecimating() ? SelectSupport.TOO_BIG : SelectSupport.YES;
        }
        else {
            return SelectSupport.NO;
        }
    }

    public int getSelectedCount() {
        return tableDataView!=null ? tableDataView.getSelectionInfo().getSelectedCount() : 0;
    }

    public boolean getSupportsFilter() { return _supportsFilter; }

    public boolean getSupportsMouse() { return _supportsMouse; }
    public boolean getOnlyShowIfDataIsVisible() { return _onlyVisibleIfTabActive; }

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

        StringBuilder sb= new StringBuilder(20+ (idxAry.length*5));

        if (isDecimating()) {
            DecimateKey decimateKey= tableDataView.getMeta().getDecimateKey();
            List<String> currentFilters= new ArrayList<String>(model.getFilters());

            if (decimateKey!=null) {
                sb.append(decimateKey.toString()).append(" IN (");
                TableData<TableData.Row> dataViewModel= tableDataView.getModel();
                for(int i= 0; (i<idxAry.length); i++) {
                    String s= (String)dataViewModel.getRow(idxAry[i]).getValue(DecimateKey.DECIMATE_KEY);
                    sb.append(s);
                    if (i<idxAry.length-1) sb.append(",");
                }
            }
            sb.append(")");
            currentFilters.add(sb.toString());
            model.setFilters(currentFilters);
            model.fireDataStaleEvent();
        }
        else {
            model.getCurrentData().deselectAll();
            sb.append(TableDataView.ROWID + " IN (");
            TableData<TableData.Row> dataViewModel= tableDataView.getModel();
            for(int i= 0; (i<idxAry.length); i++) {
                sb.append(dataViewModel.getRow(idxAry[i]).getRowIdx());
                if (i<idxAry.length-1) sb.append(",");
            }
            sb.append(")");
            model.setFilters(Arrays.asList(sb.toString()));
            model.fireDataStaleEvent();
        }



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

    public List<DrawObj> getHighlightData(WebPlot p) {
        List<DrawObj> retval= null;
        if (tableDataView!=null) {
            retval= getHighlightDataImpl();
        }
        return retval;
    }




    protected int getWeight(int row) {
        int weight;
        if (table.getDataModel().getRowCount()> MAX_UNDECIMATED) {
            try {
                TableData.Row<String> r=getTableDatView().getModel().getRow(row);
                weight= Integer.parseInt(r.getValue(WEIGHT));
            } catch (NumberFormatException e) {
                weight= 1;
            }
        }
        else {
            weight= 1;
        }
        return weight;
    }

    protected TableData.Row<String> getTableHighlightedRow() {
        int rowIdx= getTable().getDataModel().getCurrentData().getHighlighted();
        DataSet currentData= getTable().getDataModel().getCurrentData();
        TableData.Row highlightedRow = null;

        int currDataIdx= rowIdx-currentData.getStartingIdx();
        if (currDataIdx>=0 && currDataIdx<currentData.getSize()) {
            highlightedRow = currentData.getModel().getRow(currDataIdx);
        }

        return highlightedRow;
    }

    /**
     * List of columns, the size should length of the list should always be two
     * @return a list that contains the two columns to use
     */
    protected abstract List<String> getDataColumns();
    public abstract List<DrawObj> getDataImpl();
    public abstract List<DrawObj> getHighlightDataImpl();


    private boolean isDecimating() {
        return table!=null && table.getDataModel()!=null && table.getDataModel().getRowCount()> MAX_UNDECIMATED;
    }


    private class AsyncTableDataLoader implements AsyncDataLoader {
        List<LoadCallback> loadCalls= new ArrayList<LoadCallback>(10);
        private boolean inProcess= false;
        public void requestLoad(LoadCallback cb) {
            if (tableDataView!=null) {
                cb.loaded();
            }
            else if (table.getDataModel().isMaxRowsExceeded()) {
                tableDataView= null;
                cb.loaded();
            }
            else {
                List<String> cols= getDataColumns();
                if (cols.size()<2) {
                    tableDataView= null;
                    cb.loaded();
                    return;
                }
                loadCalls.add(cb);
                if (!inProcess) {
                    inProcess= true;
                    AsyncCallback<TableDataView> completedCB= new AsyncCallback<TableDataView>() {
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
                    };
                    if (isDecimating()) {
                        DecimateInfo di= new DecimateInfo(cols.get(0), cols.get(1), MAX_UNDECIMATED, 1);
                        getTable().getDataModel().getDecimatedAdHocData(completedCB,di);
                    }
                    else {
                        getTable().getDataModel().getAdHocData(completedCB, getDataColumns());
                    }
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
