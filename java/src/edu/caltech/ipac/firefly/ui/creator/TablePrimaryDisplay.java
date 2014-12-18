package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.DownloadCmd;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;
/**
 * User: roby
 * Date: Apr 19, 2010
 * Time: 4:23:38 PM
 */


/**
 * @author Trey Roby
 */
public class TablePrimaryDisplay implements PrimaryTableUI {
    int prefHeight;
    int prefWidth;

    String title;

    private TablePanel table;

    public TablePrimaryDisplay(TablePanel table) {
        this.table = table;
    }

    public void bind(EventHub hub) {
        hub.bind(table);
    }

    public void setPrefHeight(int prefHeight) {
        this.prefHeight = prefHeight;
    }

    public void setPrefWidth(int prefWidth) {
        this.prefWidth = prefWidth;
    }

    public int getPrefHeight() {
        return prefHeight;
    }

    public int getPrefWidth() {
        return prefWidth;
    }

    public Widget getDisplay() {
        return table;
    }

    public String getShortDesc() {
        return table.getShortDesc();
    }

    public String getName() {
        return table.getName();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        if (StringUtils.isEmpty(title))
            return table.getName();
        else
            return title;
    }

    public void onShow() {
        // TODO: decide if we need to implement
    }

    public void onHide() {
        // TODO: decide if we need to implement
    }

    public void load(AsyncCallback<Integer> callback) {
        table.init(callback);
    }

    public DataSetTableModel getDataModel() {
        return table == null ? null : table.getDataModel();
    }

    public void addDownloadButton(final DownloadSelectionIF downloadDialog, String downloadProcessorId,
                                  String baseFileName, String title, List<Param> dlParamTags) {

        final DownloadRequest dlreq = new DownloadRequest(table.getDataModel().getRequest(), title, baseFileName);
        //dlreq.setBaseFileName(baseFileName);
        //dlreq.setTitle(title);
        dlreq.setRequestId(downloadProcessorId);

        if (dlParamTags != null) {
            for (Param p : dlParamTags) {
                dlreq.setParam(p.getName(), p.getValue());
            }
        }

        downloadDialog.setDownloadRequest(dlreq);

        if (table.isInit()) {
            createDownloadButton(table, downloadDialog, dlreq);

        } else {
            table.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    createDownloadButton(table, downloadDialog, dlreq);
                    table.getEventManager().removeListener(TablePanel.ON_INIT, this);
                }
            });
        }
    }

    public TablePanel getTable() {
        return table;
    }

    public static void createDownloadButton(TablePanel table, DownloadSelectionIF downloadDialog, final DownloadRequest dlreq) {
        final TableDataView ds = table.getDataset();
        if (ds != null && downloadDialog != null) {
            downloadDialog.setDataView(ds);
            DownloadCmd cmd = new DownloadCmd(ds, downloadDialog, "DynDownloadSelectionDialog");
            Widget btn = table.addToolButton(cmd, false);
            if (btn != null) {
                btn.getElement().setId(dlreq.getRequestId());
            }

            ds.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent pce) {
                    String event = pce.getPropertyName();
                    if (event.equalsIgnoreCase(TableDataView.ROW_SELECTED) ||
                            event.equalsIgnoreCase(TableDataView.ROW_DESELECTED) ||
                            event.equalsIgnoreCase(TableDataView.ROW_SELECT_ALL) ||
                            event.equalsIgnoreCase(TableDataView.ROW_DESELECT_ALL)) {

                        dlreq.setSelectionInfo(ds.getSelectionInfo());
                    }
                }
            });
        }
    }
}

