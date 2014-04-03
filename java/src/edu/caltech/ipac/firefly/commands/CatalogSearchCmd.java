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
