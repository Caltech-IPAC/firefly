package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.imageGrid.ImageGridPanel;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.ui.table.EventHub;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: May 28, 2010
 * Time: 3:55:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageGridPrimaryDisplay implements PrimaryTableUI {
    int prefHeight;
    int prefWidth;
    private ImageGridPanel grid;

    public ImageGridPrimaryDisplay(ImageGridPanel grid) {
        this.grid= grid;
    }

    public void bind(EventHub hub) {
        grid.setTablePreviewEventHub(hub);
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

    }
}

