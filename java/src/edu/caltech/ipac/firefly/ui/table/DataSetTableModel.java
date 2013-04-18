package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.CachedTableModel;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.gen2.table.client.TableModelHelper;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.util.StringUtils;

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

    public TableDataView getCurrentData() {
        return modelAdapter.getLoader().getCurrentData();
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

    public void getAdHocData(AsyncCallback<TableDataView> callback, List<String> cols, String... filters) {
        getAdHocData(callback, cols, 0, Integer.MAX_VALUE, filters);
    }

    public Loader<TableDataView> getLoader() {
        return modelAdapter.getLoader();
    }

    /**
     * Getting the data backed by this model for ad hoc use.  It does not cache this data.  You should
     * only use this method if you intent to only get a limited set of columns from the data set.
     * @param callback
     * @param cols  a list of columns to retrieve
     * @param fromIdx from index.  index starts from 0.
     * @param toIdx
     * @param filters
     */
    public void getAdHocData(AsyncCallback<TableDataView> callback, List<String> cols, int fromIdx, int toIdx, String... filters) {
        Loader<TableDataView>  loader = modelAdapter.getLoader();
        TableServerRequest req = (TableServerRequest) loader.getRequest().cloneRequest();
        req.setStartIndex(fromIdx);
        req.setPageSize(toIdx - fromIdx);
        req.setParam(TableServerRequest.INCL_COLUMNS, StringUtils.toString(cols, ","));
        if (filters != null && filters.length > 0) {
            req.setFilters(Arrays.asList(filters));
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
                for (Iterator<TableData.Row> itr = response.getRowValues(); itr.hasNext(); ) {
                    callback.onSuccess(modelAdapter.getLoader().getCurrentData());
                }
            }
        });
    }

    public void setTable(BasicPagingTable table) {
        modelAdapter.setTable(table);
    }

//====================================================================
//
//====================================================================

    public static abstract class DataModelCallback implements AsyncCallback<TableDataView> {

        public void onFailure(Throwable caught) {
        }

        public void onStatusUpdated(TableDataView result) {

        }

        public void onCompleted(TableDataView result) {

        }
    }

    private class CheckFileStatusTimer extends Timer {
        DataModelCallback callback;

        private CheckFileStatusTimer(DataModelCallback callback) {
            this.callback = callback;
        }

        public void run() {
            SearchServices.App.getInstance().getFileStatus(getCurrentData().getMeta().getSource(),
                    new AsyncCallback<FileStatus>(){
                        public void onFailure(Throwable caught) {
                            CheckFileStatusTimer.this.cancel();
                            getCurrentData().getMeta().setIsLoaded(true);
                            callback.onCompleted(getCurrentData());
                        }
                        public void onSuccess(FileStatus result) {
                            boolean isLoaded = !result.getState().equals(FileStatus.State.INPROGRESS);
                            getCurrentData().setTotalRows(result.getRowCount());
                            getCurrentData().getMeta().setIsLoaded(isLoaded);
                            setRowCount(result.getRowCount());
                            if (isLoaded) {
                                CheckFileStatusTimer.this.cancel();
                                callback.onCompleted(getCurrentData());
                            } else {
                                callback.onStatusUpdated(getCurrentData());
                            }
                        }
                    });
        }
    }


    static class ModelAdapter extends MutableTableModel<TableData.Row> {
        private Loader<TableDataView>  loader;
        private DataSetTableModel cachedModel;
        private BasicPagingTable table;

        ModelAdapter(Loader<TableDataView>  loader) {
            this.loader = loader;
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
                loader.setSortInfo(getSortInfo(request));
            }

            loader.load(request.getStartRow(), request.getNumRows(), new AsyncCallback<TableDataView>() {
                public void onFailure(Throwable throwable) {
                    rowCallback.onFailure(throwable);
                }

                public void onSuccess(TableDataView data) {
                    cachedModel.setRowCount(data.getTotalRows());
                    rowCallback.onRowsReady(request, new DataSetResponse(data.getModel().getRows()));
                }
            });
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
                if (col != null) {
                    if (col.getColumn().getSortByCols() != null) {
                        return new SortInfo(dir, col.getColumn().getSortByCols());
                    } else {
                        return new SortInfo(dir, col.getName());
                    }
                }
            }
            return null;
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
