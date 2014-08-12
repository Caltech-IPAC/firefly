package edu.caltech.ipac.fuse.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;

import java.util.List;

/**
 * @author tatianag
 *         $Id: $
 */
public class LoadCatalogFromVOSearchUI implements SearchUI {

    private static final WebClassProperties _prop= new WebClassProperties(LoadCatalogFromVOSearchUI.class);

    private SpacialBehaviorPanel.Cone cone;
    private SimpleTargetPanel targetPanel;
    private SimpleInputField accessUrl;

    private SpatialOps.Cone coneOps;

    public String getKey() {
        return "LoadCatalogVO";
    }

    public String getPanelTitle() {
        return "Catalog from VO";
    }

    public String getDesc() {
        return "Load catalog from VO cone search service";
    }

    public String getSearchTitle() {
        return "Cone Search"; //targetPanel.getTargetName();
    }

    public Widget makeUI() {
        Widget loadCatalogWidget = createLoadCatalogsContent();
        SimplePanel panel = new SimplePanel(loadCatalogWidget);
        GwtUtil.setStyle(panel, "lineHeight", "100px");
//        panel.setSize("400px", "300px");
        return GwtUtil.wrap(panel, 50, 50, 50,20);
    }

    public boolean validate() {
        return targetPanel.validate() &&
                cone.getField().validate()
                && accessUrl.validate();
    }

    public void makeServerRequest(final AsyncCallback<ServerRequest> cb) {

        final TableServerRequest req = new TableServerRequest("ConeSearchByURL");
        req.setParam("accessUrl", accessUrl.getValue());
        req.setParams(targetPanel.getFieldValues());
        req.setParams(coneOps.getParams());

        cb.onSuccess(req);

    }

    public boolean setServerRequest(ServerRequest request) {
        accessUrl.setValue(request.getParam("accessurl"));
        List<Param> params = request.getParams();
        targetPanel.setFieldValues(params);
        coneOps.setParams(params);
        return true;
    }


    private Widget createLoadCatalogsContent() {

        accessUrl = SimpleInputField.createByProp(_prop.makeBase("accessUrl"));

        targetPanel = new SimpleTargetPanel();

        cone =  new SpacialBehaviorPanel.Cone();
        Widget coneSearchPanel = cone.makePanel();
        GwtUtil.setStyles(coneSearchPanel,
                          "display", "inline-block",
                          "verticalAlign", "top",
                          "padding", "8px 0 0 20px");
        coneOps = new SpatialOps.Cone(cone.getField(), cone);

        FlowPanel container= new FlowPanel();
        container.add(targetPanel);
        container.add(coneSearchPanel);


        FlexTable grid = new FlexTable();
        grid.setCellSpacing(5);
        DOM.setStyleAttribute(grid.getElement(), "padding", "5px");
        grid.setWidget(0, 0, accessUrl);
        grid.setWidget(1, 0, container);

        return grid;
    }
}
