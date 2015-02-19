/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: balandra
 * Date: Jul 28, 2010
 */
@Deprecated
public class CatalogSearchCmd extends CommonRequestCmd {

    public static final String COMMAND_NAME= "CatalogSearch";

    private CatalogPanel panel;

    public CatalogSearchCmd(){
        super(COMMAND_NAME);        
    }

    protected Form createForm() {
        panel = new CatalogPanel(panel, DynUtils.getProjectIdFromUrl());
        VerticalPanel vp = new VerticalPanel();
        vp.add(panel);

        Form form = new Form();
        form.add(vp);

        return form;
    }

    @Override
    protected FormHub.Validated validate() {
        return super.validate();
    }

    @Override
    protected void onFormSubmit(Request req) {
        req.setCmdName(COMMAND_NAME);
        req.setIsBackgroundable(true);
        super.onFormSubmit(req);
    }

    protected void processRequest(Request req, AsyncCallback<String> callback) {

        String query = CatalogRequest.RequestType.GATOR_QUERY.getSearchProcessor();

        Map<String, String> previewParams = new HashMap<String, String>(3);
        previewParams.put("QUERY_ID", query);

        TableServerRequest newReq = new TableServerRequest(query, req);

        Map<String, String> tableParams = new HashMap<String, String>(3);
        tableParams.put(TablePanelCreator.TITLE, panel.getCatName());
        tableParams.put(TablePanelCreator.SHORT_DESC, newReq.getParam(CatalogRequest.CATALOG));

        WidgetFactory factory = Application.getInstance().getWidgetFactory();

        final PrimaryTableUI primary = factory.createPrimaryUI(WidgetFactory.TABLE, newReq, tableParams);
        final TablePreview coverage = factory.createObserverUI(WidgetFactory.COVERAGE_VIEW, previewParams);
        EventHub hub = new EventHub();
        primary.bind(hub);
        coverage.bind(hub);

        final int prefWidth = coverage.getPrefWidth() == 0 ? 400 : coverage.getPrefWidth();

        final SplitLayoutPanel sp = new SplitLayoutPanel();
        sp.setSize("100%", "100%");


        sp.addEast(GwtUtil.createShadowTitlePanel(coverage.getDisplay(), "Details"), prefWidth);
        sp.add(GwtUtil.createShadowTitlePanel(primary.getDisplay(), primary.getShortDesc()));

        PrimaryTableUILoader loader = getTableUiLoader();
        loader.addTable(primary);
        loader.loadAll();

        this.setResults(sp);
    }
}
