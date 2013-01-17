package edu.caltech.ipac.firefly.ui.imageGrid;

import com.google.gwt.gen2.table.client.CachedTableModel;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.gen2.table.client.TableModelHelper;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.table.Loader;

import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 28, 2010
 * Time: 3:55:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataSetTableModel extends CachedTableModel<TableData.Row> {
    private static final int BUFFER_LIMIT = Application.getInstance().getProperties().getIntProperty("DataSetTableModel.buffer.limit", 250);

    private Model model;

    public DataSetTableModel(Loader<TableDataView> loader) {
        this(new DataSetTableModel.Model(loader));
    }

    public DataSetTableModel(Model model) {
        super(model);

        model.setCachedModel(this);
        this.model = model;
        int buffer = Math.min(BUFFER_LIMIT, model.getLoader().getPageSize()*2);

        setPreCachedRowCount(buffer);
        setPostCachedRowCount(buffer);
    }

    public Model getModel() {
        return model;
    }


//====================================================================
//
//====================================================================

    public static class Model extends MutableTableModel<TableData.Row> {
        private Loader<TableDataView> loader;
        private DataSetTableModel cachedModel;

        Model(Loader<TableDataView> loader) {
            this.loader = loader;
        }

        public Loader<TableDataView> getLoader() {
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