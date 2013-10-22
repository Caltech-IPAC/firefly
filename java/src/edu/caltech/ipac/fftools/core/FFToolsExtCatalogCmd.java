package edu.caltech.ipac.fftools.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;

public class FFToolsExtCatalogCmd extends RequestCmd {

    public  static final String COMMAND = "FFToolsExtCatalogCmd";
    private final StandaloneUI aloneUI;

    public FFToolsExtCatalogCmd(StandaloneUI aloneUI) {
        super(COMMAND, "Catlog Viewer", "Catalog Viewer", true);
        this.aloneUI= aloneUI;
    }


    protected void doExecute(final Request req, AsyncCallback<String> callback) {
        aloneUI.eventSearchingCatalog();
        CatalogPanel.setDefaultSearchMethod(CatalogRequest.Method.CONE);
        ((FFToolsStandaloneCreator) Application.getInstance().getCreator()).activateToolbarCatalog();
    }


}

