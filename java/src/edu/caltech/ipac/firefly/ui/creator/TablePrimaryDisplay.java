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
            table.addToolButton(cmd, false);

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
