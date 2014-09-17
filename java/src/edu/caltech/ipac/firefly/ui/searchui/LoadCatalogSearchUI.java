package edu.caltech.ipac.firefly.ui.searchui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;

/**
 * @author tatianag
 */
public class LoadCatalogSearchUI implements SearchUI {

    private static final WebClassProperties _prop= new WebClassProperties(LoadCatalogSearchUI.class);

    FileUploadField _uploadField;

    public String getKey() {
        return "LoadCatalog";
    }

    public String getPanelTitle() {
        return "Upload Catalog File";
    }

    public String getDesc() {
        return "Load catalog from user's file";
    }

    public String getSearchTitle() {
        String basename;
        String fullPath = _uploadField.getUploadFilename();
        int idx = fullPath.lastIndexOf('/');
        if (idx<0) idx = fullPath.lastIndexOf('\\');
        if (idx > 1) {
            basename = fullPath.substring(idx+1);
        } else {
            basename = fullPath;
        }

        return basename;
    }

    public Widget makeUI() {
        Widget loadCatalogWidget = createLoadCatalogsContent();
        SimplePanel panel = new SimplePanel(loadCatalogWidget);
        GwtUtil.setStyle(panel, "lineHeight", "100px");
//        panel.setSize("400px", "300px");
        return GwtUtil.wrap(panel, 50, 50, 50,20);
    }

    public boolean validate() {
        return _uploadField.validate();
    }

    public void makeServerRequest(final AsyncCallback<ServerRequest> cb) {
        if (_uploadField.validate()) {
            /*

             */
            _uploadField.submit(new AsyncCallback<String>() {

                public void onFailure(Throwable caught) {
                    cb.onFailure(caught);
                }

                public void onSuccess(final String filepath) {
                    // filepath is returned
                    final TableServerRequest req = new TableServerRequest(CommonParams.USER_CATALOG_FROM_FILE);
                    req.setParam("filePath", filepath);
                    req.setStartIndex(0);
                    req.setPageSize(1000);
                    cb.onSuccess(req);
                }
            });
        }

    }

    public boolean setServerRequest(ServerRequest request) {
        return true;
    }


    private Widget createLoadCatalogsContent() {
        SimpleInputField field = SimpleInputField.createByProp(_prop.makeBase("upload"));
        _uploadField= (FileUploadField)field.getField();

        HTML text = GwtUtil.makeFaddedHelp("Custom catalog in IPAC table format&nbsp;&nbsp;");

        Label pad = new Label(" ");
        pad.setHeight("400px");

        FlexTable grid = new FlexTable();
        grid.setCellSpacing(5);
        DOM.setStyleAttribute(grid.getElement(), "padding", "5px");
        grid.setWidget(0, 0, field);
        grid.setWidget(1, 0, text);

        return grid;
    }
}
