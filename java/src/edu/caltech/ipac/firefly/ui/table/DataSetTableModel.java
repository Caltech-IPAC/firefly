package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.CachedTableModel;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.gen2.table.client.TableModelHelper;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Date: Feb 27, 2009
 *
 * @author loi
 * @version $Id: DataSetTableModel.java,v 1.8 2012/06/16 00:21:53 loi Exp $
 */
public class DataSetTableModel extends CachedTableModel<TableData.Row> {
    private static final int BUFFER_LIMIT = Application.getInstance().getProperties().getIntProperty("DataSetTableModel.buffer.limit", 250);

    private ModelAdapter modelAdapter;

    public DataSetTableModel(Loader<TableDataView>  loader) {
        this(new ModelAdapter(loader));
    }

    DataSetTableModel(ModelAdapter model) {
        super(model);

        model.setCachedModel(this);
        this.modelAdapter = model;
        int buffer = Math.min(BUFFER_LIMIT, model.getLoader().getPageSize()*2);

        setPreCachedRowCount(buffer);
        setPostCachedRowCount(buffer);
    }

    public TableServerRequest getRequest(){
        return modelAdapter.getLoader().getRequest();
    }

    public int getTotalRows() {
        return getRowCount();
    }

    public DataSet getCurrentData() {
        return (DataSet) modelAdapter.getLoader().getCurrentData();
    }

    public List<String> getFilters() {
        return modelAdapter.getLoader().getFilters();
    }

    public void setFilters(List<String> filters) {
        modelAdapter.getLoader().setFilters(filters);
    }

    public int getPageSize() {
        return modelAdapter.getLoader().getPageSize();
    }

    public void setPageSize(int pageSize) {
        modelAdapter.getLoader().setPageSize(pageSize);
    }

    public void setSortInfo(SortInfo sortInfo) {
        modelAdapter.getLoader().setSortInfo(sortInfo);
    }

    public SortInfo getSortInfo() {
        return modelAdapter.getLoader().getSortInfo();
    }

    public List<ModelEventHandler> getHandlers() {
        return modelAdapter.getHandlers();
    }

    public void addHandler(ModelEventHandler handler) {
        modelAdapter.addHandler(handler);
    }

    public boolean removeHandler(ModelEventHandler handler) {
        return modelAdapter.removeHandler(handler);
    }

    public Loader<TableDataView> getLoader() {
        return modelAdapter.getLoader();
    }

    public void getAdHocData(AsyncCallback<TableDataView> callback, List<String> cols, String... filters) {
        getAdHocData(callback, cols, -1, -1, null, filters);
    }

    public void getAdHocData(AsyncCallback<TableDataView> callback, List<String> cols, int fromIdx, int toIdx, String... filters) {
        getAdHocData(callback, cols, fromIdx, toIdx, null, filters);
    }

        /**
         * Getting the data backed by this model for ad hoc use.  It does not cache this data.  You should
         * only use this method if you intent to only get a limited set of columns from the data set.
         * @param callback
         * @param cols  a list of columns to retrieve
         * @param fromIdx from index.  index starts from 0.  negative will be treated as 0.
         * @param toIdx  to index.  negative will be treated as Integer.MAX_VALUE
         * @param sortInfo  sort info.  use model's if not given
         * @param filters filters.  use model's if not given
         */
    public void getAdHocData(AsyncCallback<TableDataView> callback, List<String> cols, int fromIdx, int toIdx, SortInfo sortInfo, String... filters) {
        fromIdx = fromIdx < 0 ? 0 : fromIdx;
        toIdx = toIdx < 0 ? Integer.MAX_VALUE : toIdx;
        Loader<TableDataView>  loader = modelAdapter.getLoader();
        TableServerRequest req = (TableServerRequest) loader.getRequest().cloneRequest();
        req.setSortInfo(loader.getSortInfo());
        req.setFilters(loader.getFilters());
        req.setStartIndex(fromIdx);
        req.setPageSize(toIdx - fromIdx);
        if (cols != null && cols.size() > 0) {
            req.setParam(TableServerRequest.INCL_COLUMNS, StringUtils.toString(cols, ","));
        }
        if (filters != null && filters.length > 0) {
            req.setFilters(Arrays.asList(filters));
        }
        if (sortInfo != null) {
            req.setSortInfo(sortInfo);
        }
        loader.getData(req, callback);
    }

    /**
     * Return a page of data.  This model will handle the caching.  It may or may not call the server
     * to load the data.
     * @param callback
     * @param pageNo  page number.  number starts from 0;
     */
    public void getData(final AsyncCallback<TableDataView> callback, int pageNo) {
        TableModelHelper.Request req = new TableModelHelper.Request(pageNo * getPageSize(), getPageSize());
        requestRows(req, new Callback<TableData.Row>() {
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            public void onRowsReady(TableModelHelper.Request request, TableModelHelper.Response<TableData.Row> response) {
                callback.onSuccess(modelAdapter.getLoader().getCurrentData());
            }
        });
    }

    public void setTable(BasicPagingTable table) {
        modelAdapter.setTable(table);
    }

    /**
     * call this method when the data previously retrieved from this model is no longer valid.
     * you need to get fresh data from the model
     */
    public void fireDataStaleEvent() {
        for(ModelEventHandler h : modelAdapter.getHandlers()) {
            h.onDataStale(this);
        }
    }

//====================================================================
//
//====================================================================

    public interface ModelEventHandler {
        public void onFailure(Throwable caught);
        public void onLoad(TableDataView result);
        public void onStatusUpdated(TableDataView result);
        public void onDataStale(DataSetTableModel model);
    }


    static class ModelAdapter extends MutableTableModel<TableData.Row> {
        private Loader<TableDataView>  loader;
        private DataSetTableModel cachedModel;
        private BasicPagingTable table;
        private List<ModelEventHandler> handlers = new ArrayList<ModelEventHandler>();
        private CheckFileStatusTimer checkStatusTimer = null;
        private boolean gotEnums;

        ModelAdapter(Loader<TableDataView>  loader) {
            this.loader = loader;
        }

        public List<ModelEventHandler> getHandlers() {
            return handlers;
        }

        public void addHandler(ModelEventHandler handler) {
            handlers.add(handler);
        }

        public boolean removeHandler(ModelEventHandler handler) {
            return handlers.remove(handler);
        }

        public BasicPagingTable getTable() {
            return table;
        }

        void setTable(BasicPagingTable table) {
            this.table = table;
        }

        public Loader<TableDataView>  getLoader() {
            return loader;
        }

        void setCachedModel(DataSetTableModel cachedModel) {
            this.cachedModel = cachedModel;
        }

        protected boolean onRowInserted(int i) {
            return false;
        }

        protected boolean onRowRemoved(int i) {
            return false;
        }

        protected boolean onSetRowValue(int i, TableData.Row row) {
            return false;
        }

        public void requestRows(final TableModelHelper.Request request, final Callback<TableData.Row> rowCallback) {
            SortInfo sortInfo = getSortInfo(request);
            if (sortInfo != null) {
                loader.setSortInfo(sortInfo);
            }

            loader.load(request.getStartRow(), request.getNumRows(), new AsyncCallback<TableDataView>() {
                public void onFailure(Throwable throwable) {
                    rowCallback.onFailure(throwable);
                    for (ModelEventHandler h : handlers) {
                        h.onFailure(throwable);
                    }
                }

                public void onSuccess(TableDataView data) {
                    cachedModel.setRowCount(data.getTotalRows());
                    rowCallback.onRowsReady(request, new DataSetResponse(data.getModel().getRows()));

                    if (data.getMeta().isLoaded()) {
                        if (checkStatusTimer != null) {
                            checkStatusTimer.cancel();
                        }
                        onLoadCompleted();
                    } else {
                        if (checkStatusTimer == null) {
                            checkStatusTimer = new CheckFileStatusTimer();
                        }
                        checkStatusTimer.cancel();
                        checkStatusTimer.scheduleRepeating(1500);
                    }

                    for (ModelEventHandler h : handlers) {
                        h.onLoad(cachedModel.getCurrentData());
                    }
                }
            });
        }

        private void onLoadCompleted() {
            try {
                if (gotEnums) return;

                gotEnums = true;
                String source = cachedModel.getCurrentData().getMeta().getSource();
                if (!StringUtils.isEmpty(source)) {
                    SearchServices.App.getInstance().getEnumValues(source,
                            new AsyncCallback<RawDataSet>() {
                                public void onFailure(Throwable throwable) {
                                    //do nothing
                                }
                                public void onSuccess(RawDataSet rawDataSet) {
                                    TableDataView ds = cachedModel.getCurrentData();
                                    DataSet enums = DataSetParser.parse(rawDataSet);
                                    for(TableDataView.Column c : enums.getColumns()) {
                                        if (c.getEnums() != null && c.getEnums().length > 0) {
                                            TableDataView.Column fc = ds.findColumn(c.getName());
                                            if (fc != null) {
                                                fc.setEnums(c.getEnums());
                                            }
                                        }
                                    }
                                    if (table != null) {
                                        table.updateHeaderTable(true);
                                    }
                                }
                            });
                }
            } catch (RPCException e) {
                e.printStackTrace();
                //do nothing.
            }
        }

        private SortInfo getSortInfo(TableModelHelper.Request req) {
            if (table == null) {
                return null;
            }
            TableModelHelper.ColumnSortList sortList = req.getColumnSortList();
            TableModelHelper.ColumnSortInfo si = sortList.getPrimaryColumnSortInfo();

            if (si != null) {
                SortInfo.Direction dir = sortList.isPrimaryAscending() ? SortInfo.Direction.ASC : SortInfo.Direction.DESC;
                ColDef col = table.getColumnDefinition(si.getColumn());
                if (col != null && col.getColumn() != null) {
                    if (col.getColumn().getSortByCols() != null) {
                        return new SortInfo(dir, col.getColumn().getSortByCols());
                    } else {
                        return new SortInfo(dir, col.getName());
                    }
                }
            }
            return null;
        }
//====================================================================
//
//====================================================================
        private class CheckFileStatusTimer extends Timer {

            public void run() {
                final TableDataView dataset = cachedModel.getCurrentData();
                if (dataset == null) return;

                SearchServices.App.getInstance().getFileStatus(dataset.getMeta().getSource(),
                        new AsyncCallback<FileStatus>(){
                            public void onFailure(Throwable caught) {
                                CheckFileStatusTimer.this.cancel();
                                dataset.getMeta().setIsLoaded(true);

                                for (ModelEventHandler h : handlers) {
                                    h.onStatusUpdated(dataset);
                                }
                            }
                            public void onSuccess(FileStatus result) {
                                boolean isLoaded = !result.getState().equals(FileStatus.State.INPROGRESS);
                                dataset.setTotalRows(result.getRowCount());
                                dataset.getMeta().setIsLoaded(isLoaded);
                                cachedModel.setRowCount(result.getRowCount());

                                for (ModelEventHandler h : handlers) {
                                    h.onStatusUpdated(dataset);
                                }
                                if (isLoaded) {
                                    CheckFileStatusTimer.this.cancel();
                                    onLoadCompleted();
                                }
                            }
                        });
            }
        }
    }


    static class DataSetResponse extends TableModelHelper.Response<TableData.Row> {
        private List<TableData.Row> rows;

        DataSetResponse(List<TableData.Row> rows) {
            this.rows = rows;
        }

        public Iterator<TableData.Row> getRowValues() {
            return rows.iterator();
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
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
