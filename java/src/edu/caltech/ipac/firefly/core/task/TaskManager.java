/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.task;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.data.table.BaseTableColumn;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.table.AbstractLoader;
import edu.caltech.ipac.firefly.ui.table.BasicTable;

/**
 * Date: 7/14/14
 *
 * @author loi
 * @version $Id: $
 */
public class TaskManager {

    public static final String SEARCHES = "";
    public static final String CATALOGS = WspaceMeta.CATALOGS;
    public static final String IMAGESET = WspaceMeta.IMAGESET;
    public static final String DOWNLOADS = WspaceMeta.DOWNLOADS;

    private PopupPane popup;
    private BasicTable searches;

    public TaskManager() {
        popup = new PopupPane("Task Manager");
    }

    public void show() {
        new WsLoader(SEARCHES).load(0, 0, new BaseCallback<DataSet>() {
            public void doSuccess(DataSet result) {
                searches = new BasicTable(result);
                searches.setSize("100%", "100%");
                popup.setWidget(searches);
                popup.setDefaultSize(500, 400);
                searches.fillWidth();
            }
        });
        popup.show();
    }

    public void hide() {
        popup.hide();
    }

    class WsLoader extends AbstractLoader<DataSet> {
        String type;

        WsLoader(String type) {
            this.type = type;
        }

        public void load(int offset, int pageSize, final AsyncCallback<DataSet> callback) {
            final String searchPath;
            if (type.equals(DOWNLOADS)) {
                searchPath = "/" + WspaceMeta.STAGING_DIR + "/" + type + "/";
            } else {
                String t = type.equals("") ? "" : type + "/";
                searchPath = "/" + WspaceMeta.SEARCH_DIR + "/" + t;
            }

            UserServices.App.getInstance().getMeta(searchPath, WspaceMeta.Includes.ALL_PROPS, new AsyncCallback<WspaceMeta>() {
                public void onFailure(Throwable throwable) {

                }

                public void onSuccess(WspaceMeta wspaceMeta) {
                    DataSet ds = convertToDataset(wspaceMeta);
                    ds.setMeta(new TableMeta(searchPath));
                    setCurrentData(ds);
                    callback.onSuccess(ds);
                }
            });
        }

        private DataSet convertToDataset(WspaceMeta wspaceMeta) {
            DataSet ds = new DataSet(new DataSet.Column[]{new BaseTableColumn("type"), new BaseTableColumn("desc"), new BaseTableColumn("size"), new BaseTableColumn("relPath"), new BaseTableColumn("lastModified")});

            if (wspaceMeta == null) return ds;
            for (WspaceMeta meta : wspaceMeta.getAllNodes()) {
                if (meta.getSize() > 0) {
                    String[] values = {meta.getProperty(WspaceMeta.TYPE), meta.getProperty(WspaceMeta.DESC), String.valueOf(meta.getSize()), meta.getRelPath(), meta.getLastModified()};
                    ds.getModel().addRow(new BaseTableData.RowData(ds.getModel().getColumnNames(), values));
                }
            }
            return ds;
        }

        public String getSourceUrl() {
            return null;
        }

        public TableServerRequest getRequest() {
            return null;
        }
    }
}
