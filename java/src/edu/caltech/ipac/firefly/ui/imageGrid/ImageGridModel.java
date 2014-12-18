package edu.caltech.ipac.firefly.ui.imageGrid;

import com.google.gwt.gen2.table.client.CachedTableModel;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.gen2.table.client.TableModelHelper;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
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
public class ImageGridModel extends CachedTableModel<TableData.Row> {
    private static final int BUFFER_LIMIT = Application.getInstance().getProperties().getIntProperty("DataSetTableModel.buffer.limit", 250);

    private Model model;

    public ImageGridModel(Loader<TableDataView> loader) {
        this(new ImageGridModel.Model(loader));
    }

    public ImageGridModel(Model model) {
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
        private ImageGridModel cachedModel;

        Model(Loader<TableDataView> loader) {
            this.loader = loader;
        }

        public Loader<TableDataView> getLoader() {
            return loader;
        }

        void setCachedModel(ImageGridModel cachedModel) {
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
