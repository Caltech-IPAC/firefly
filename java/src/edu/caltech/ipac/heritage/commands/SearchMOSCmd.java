package edu.caltech.ipac.heritage.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.MOSRequest;
import edu.caltech.ipac.firefly.data.NewTableResults;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.MOSPanel;
import edu.caltech.ipac.firefly.ui.NaifTargetPanel;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * @author tatianag
 */
public class SearchMOSCmd extends HeritageRequestCmd {

    public static final String COMMAND_NAME = "SearchMOS";
    MOSPanel mosPanel;

    public SearchMOSCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected Form createForm() {
        mosPanel = new MOSPanel();
        Form form = new Form();
        form.setHelpId("searching.byPosition");
        form.add(mosPanel);
        form.setFocus(NaifTargetPanel.NAIF_NAME_KEY);
        return form;

    }

    @Override
    protected void processRequest(Request req, AsyncCallback<String> callback) {
        createTablePreviewDisplay(false);
        req.setRequestId("MOSQuery");
        req.setParam(MOSRequest.CATALOG, "spitzer_bcd");
        BaseTableConfig config = new BaseTableConfig(req, "Pre-covery", "Pre-covery search results");
        addTable(config);
        //NewTableResults tr = new NewTableResults(req, WidgetFactory.TABLE, "Pre-covery search");
        //WebEventManager.getAppEvManager().fireEvent(new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED, tr));
        loadAll();
        setResults(getResultsPanel());
    }
}
