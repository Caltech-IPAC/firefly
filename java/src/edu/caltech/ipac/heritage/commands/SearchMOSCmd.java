package edu.caltech.ipac.heritage.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.MOSPanel;
import edu.caltech.ipac.firefly.ui.NaifTargetPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.ImageGridViewCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tatianag
 */
public class SearchMOSCmd extends HeritageRequestCmd {

    public static final String COMMAND_NAME = "MOSQuery";
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
        createTablePreviewDisplay(true);
        req.setRequestId("MOSQuery");
        req.setParam(MOSRequest.CATALOG, "spitzer_bcd");
        BaseTableConfig config = new BaseTableConfig(req, "Pre-covery", "Pre-covery search results");
        TablePanel tablePanel = addTable(config);

        //add image grid view
        List<Param> paramList = req.getParams();
        Map<String,String> params = new HashMap<String,String>();
        for (Param p : paramList) {
            if (!p.getName().equals(ServerRequest.ID_KEY)) {
                params.put(p.getName(), p.getValue());
            }
        }
        params.put(ServerRequest.ID_KEY, "shaGridMOSQuery");
        params.put(CommonParams.SEARCH_PROCESSOR_ID, "shaGridMOSQuery");
        params.put("Index", "-1");
        params.put(CommonParams.PAGE_SIZE, "15");
        TablePanel.View imageGridView = new ImageGridViewCreator().create(params);
        tablePanel.addView(imageGridView);

        loadAll();
        setResults(getResultsPanel());
        //NewTableResults tr = new NewTableResults(req, WidgetFactory.TABLE, "Pre-covery search");
        //WebEventManager.getAppEvManager().fireEvent(new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED, tr));

    }
}
