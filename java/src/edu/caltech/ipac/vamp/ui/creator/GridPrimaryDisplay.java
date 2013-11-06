package edu.caltech.ipac.vamp.ui.creator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.DownloadCmd;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.imageGrid.ImageGridPanel;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: May 28, 2010
 * Time: 3:55:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class GridPrimaryDisplay implements PrimaryTableUI {
    int prefHeight;
    int prefWidth;
    private ImageGridPanel grid;

    public GridPrimaryDisplay(ImageGridPanel grid) {
        this.grid = grid;
    }

    public void bind(EventHub hub) {
        grid.setTablePreviewEventHub(hub);
        //hub.bind(grid);
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
        return grid;
    }

    public String getShortDesc() {
        return grid.getShortDesc();
    }

    public String getName() {
        return grid.getName();
    }

    public String getTitle() {
        return grid.getName();
    }

    public void onShow() {
        // TODO: decide if we need to implement
    }

    public void onHide() {
        // TODO: decide if we need to implement
    }

    public void load(AsyncCallback<Integer> callback) {
        grid.init(callback);
    }

    public DataSetTableModel getDataModel() {
        return grid == null ? null : grid.getDataModel();
    }

    public void addDownloadButton(final DownloadSelectionIF downloadDialog, String downloadProcessorId,
                                  String filePrefix, String titlePrefix, List<Param> dlParamTags) {

        DownloadRequest dlreq = new DownloadRequest(grid.getDataModel().getRequest(), titlePrefix, filePrefix);
        dlreq.setRequestId(downloadProcessorId);

        downloadDialog.setDownloadRequest(dlreq);
        if (grid.isInit()) {
            if (grid.getDataModel().getCurrentData() != null) {
                DownloadCmd cmd = new DownloadCmd(grid.getDataModel().getCurrentData(), downloadDialog);
                //grid.addToolButton(cmd, false);
            }
        } else {
            grid.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    if (grid.getDataModel().getCurrentData() != null) {
                        DownloadCmd cmd = new DownloadCmd(grid.getDataModel().getCurrentData(), downloadDialog);
                        //grid.addToolButton(cmd, false);
                    }
                    grid.getEventManager().removeListener(TablePanel.ON_INIT, this);
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
