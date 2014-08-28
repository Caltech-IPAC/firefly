package edu.caltech.ipac.fuse.ui;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.rpc.ResourceServices;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.VOResourceEndpoint;

import java.util.List;

/**
 * @author tatianag
 *         $Id: $
 */
public class LoadCatalogFromVOSearchUI implements SearchUI {

    private static final WebClassProperties _prop= new WebClassProperties(LoadCatalogFromVOSearchUI.class);

    private static String KEYWORDS_HELP =
            "Enter the keywords to search VO resources OR "+
            "if you know your Cone Search service URL, enter it below.";

    private SpacialBehaviorPanel.Cone cone;
    private SimpleTargetPanel targetPanel;
    private SimpleInputField accessUrl, keywordsFld;
    FlowPanel keywordQueryResults;

    private ServerTask _activeTask;
    String currentKeywords = "";
    String currentShortName = "";


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
        return StringUtils.isEmpty(currentShortName) ? "Cone Search" : currentShortName; //targetPanel.getTargetName();
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

        keywordsFld = SimpleInputField.createByProp(_prop.makeBase("keywords"));
        KeyDownHandler keywordsHandler = new KeyDownHandler() {
            public void onKeyDown(KeyDownEvent ev) {
                int c = ev.getNativeKeyCode();
                if (c == KeyCodes.KEY_TAB || c == KeyCodes.KEY_ENTER) {
                    queryRegistryAsync();
                }
            }
        };

        keywordsFld.getField().getFocusWidget().addKeyDownHandler(keywordsHandler);

        Button keywordsResetBtn = GwtUtil.makeButton("Reset", "Clear keywords search results",
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        keywordsFld.getField().reset();
                        keywordQueryResults.clear();
                        keywordQueryResults.add(new HTML(KEYWORDS_HELP));
                        accessUrl.getField().reset();
                        currentKeywords = "";
                        currentShortName = "";
                    }
                });
        GwtUtil.setStyle(keywordsResetBtn, "fontSize", "9pt");

        Widget keywordsFldContainer = GwtUtil.leftRightAlign(new Widget[]{keywordsFld}, new Widget[]{keywordsResetBtn});

        keywordQueryResults = new FlowPanel();
        GwtUtil.setStyles(keywordQueryResults,
                "display", "inline-block",
                "minWidth", "400px",
                "minHeight", "50px",
                "maxHeight", "130px",
                "overflow", "auto",
                "verticalAlign", "top");
        keywordQueryResults.add(new HTML(KEYWORDS_HELP));

        accessUrl = SimpleInputField.createByProp(_prop.makeBase("accessUrl"));

        targetPanel = new SimpleTargetPanel();

        cone =  new SpacialBehaviorPanel.Cone();
        Widget coneSearchPanel = cone.makePanel();
        GwtUtil.setStyles(coneSearchPanel,
                "display", "inline-block",
                "verticalAlign", "top",
                "padding", "8px 0 20px 40px");
        coneOps = new SpatialOps.Cone(cone.getField(), cone);

        FlowPanel container= new FlowPanel();
        container.add(targetPanel);
        container.add(coneSearchPanel);

        FlexTable grid = new FlexTable();
        grid.setCellSpacing(5);
        DOM.setStyleAttribute(grid.getElement(), "padding", "5px");
        grid.setWidget(0, 0, container);
        grid.setWidget(1, 0, new Label());
        grid.setWidget(2, 0, keywordsFldContainer);
        grid.setWidget(3, 0, keywordQueryResults);
        grid.setWidget(4, 0, accessUrl);

        return grid;
    }

    public void queryRegistryAsync() {

        final String keywords = keywordsFld.getValue();
        if (StringUtils.isEmpty(keywords) || keywords.equals(currentKeywords)) { return; }

        if (_activeTask != null) _activeTask.cancel();
        _activeTask = new ServerTask<List<VOResourceEndpoint>>() {

            @Override
            public void onSuccess(List<VOResourceEndpoint> result) {
                if (result.size() > 0) {
                    // update endpoints
                    FlexTable grid = new FlexTable();
                    grid.setCellSpacing(5);
                    int row = 0;
                    for (VOResourceEndpoint ep : result) {
                        final String shortName = ep.getShortName();
                        grid.setWidget(row, 0, new Label(ep.getTitle()+
                                (StringUtils.isEmpty(shortName) ? "" : " ["+shortName+"]")));
                        grid.setWidget(row, 1, new Label(ep.getId()));
                        final String url = ep.getUrl();
                        grid.setWidget(row, 2, GwtUtil.makeLinkButton("Use", "Use URL for this service",
                                new ClickHandler() {
                                    @Override
                                    public void onClick(ClickEvent event) {
                                        accessUrl.setValue(url);
                                        currentShortName = shortName;
                                    }
                                }));
                        row++;
                    }
                    keywordQueryResults.clear();
                    keywordQueryResults.add(grid);
                } else {
                    keywordQueryResults.clear();
                    keywordQueryResults.add(new Label("No matching resource titles found."));
                }
            }

            @Override
            public void doTask(AsyncCallback<List<VOResourceEndpoint>> passAlong) {
                keywordQueryResults.clear();
                keywordQueryResults.add(new Label("Searching VO resources [keywords="+keywords+"]..."));

                ResourceServices.App.getInstance().getVOResources("ConeSearch", keywords, passAlong);
            }

        };
        _activeTask.start();
    }
}
