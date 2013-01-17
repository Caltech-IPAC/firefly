package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.TargetPanel;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.HashMap;
import java.util.Map;


/**
 * Search by Position
 * @version $Id: DemoSearch2MassPosCmd.java,v 1.13 2012/08/09 01:09:27 loi Exp $
 */
public class DemoSearch2MassPosCmd extends CommonRequestCmd {

//    public static final String RA_KEY = TargetPanel.RA_KEY;
//    public static final String DEC_KEY = TargetPanel.DEC_KEY;
    public static final String COORDSYS_KEY = TargetPanel.COORDSYS_KEY;
    public static final String RADIUS_KEY = "DemoSearch2MassPosCmd.field.radius";
//    public static final String MATCH_BY_AOR_KEY = "SearchByPosition.field.matchByAOR";

    public static final String COMMAND_NAME= "DemoSearch2MassPosCmd";
    public static final String TARGETLIST_CACHED_ID = "TargetListCachedID";

    private TargetPanel targetPanel;


    public DemoSearch2MassPosCmd() {
        super(COMMAND_NAME);
    }

    protected Form createForm() {

        targetPanel = new TargetPanel();
        SimpleInputField radiusp= SimpleInputField.createByProp(RADIUS_KEY);




        final CollapsiblePanel moreOptions = new CollapsiblePanel("More options",
                new Label("more options here"), false);



        VerticalPanel vp = new VerticalPanel();
        vp.setSpacing(5);
        vp.add(targetPanel);
        vp.add(radiusp);
//        vp.add(options);
        vp.add(moreOptions);

        Form form = new Form();
        form.setHelpId("searching.byPosition");
        form.add(vp);
        form.setFocus(TargetPanel.TARGET_NAME_KEY);
        return form;

    }

    @Override
    protected void createAndProcessRequest() {
            targetPanel.resolvePosition(new AsyncCallback<WorldPt>() {
                public void onFailure(Throwable caught) {
                    PopupUtil.showSevereError(caught);
                }
                public void onSuccess(WorldPt result) {
                    DemoSearch2MassPosCmd.super.createAndProcessRequest();
                }
            });
    }

 //==================================================================
//  private supporting methods
//====================================================================


    protected void processRequest(final Request inputReq, final AsyncCallback<String> callback) {
        processRequest_NORMAL(inputReq,callback);
    }

    protected void processRequest_NORMAL(final Request inputReq, final AsyncCallback<String> callback) {

        String queryId = "DemoSearch2MassPos";

        Map<String, String> previewParams = new HashMap<String, String>(3);
        previewParams.put("PREVIEW_COLUMN", "download");
        previewParams.put("RESOURCE_TYPE", "URL");

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, queryId);
        tableParams.put(TablePanelCreator.SHORT_DESC, "Results");


        TableServerRequest req= new TableServerRequest(queryId, inputReq);

        WidgetFactory factory = Application.getInstance().getWidgetFactory();

        final PrimaryTableUI primary = factory.createPrimaryUI(WidgetFactory.TABLE, req, tableParams);
        final TablePreview dataViewer = factory.createObserverUI(WidgetFactory.DATA_VIEW, previewParams);
        final TablePreview coverage = factory.createObserverUI(WidgetFactory.COVERAGE_VIEW, previewParams);

        TablePreviewEventHub hub = new TablePreviewEventHub();
        primary.bind(hub);
        dataViewer.bind(hub);
        coverage.bind(hub);

        final int prefWidth = dataViewer.getPrefWidth() == 0 ? 400 : dataViewer.getPrefWidth();

        final SplitLayoutPanel sp = new SplitLayoutPanel();
        sp.setSize("100%", "100%");

        final TabPane tab = new TabPane();
        tab.addTab(dataViewer.getDisplay(), dataViewer.getName());
        tab.addTab(coverage.getDisplay(), coverage.getName());

        sp.addEast(GwtUtil.createShadowTitlePanel(tab, "Details"), prefWidth);
        sp.add(GwtUtil.createShadowTitlePanel(primary.getDisplay(), primary.getShortDesc()));

        PrimaryTableUILoader loader = new PrimaryTableUILoader(this);
        loader.addTable(primary);
        loader.loadAll();

        this.setResults(sp);
    }

}