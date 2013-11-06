package edu.caltech.ipac.fftools.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.NewTableResults;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

public class FFToolsExtCatalogCmd extends RequestCmd {

    public  static final String COMMAND = "FFToolsExtCatalogCmd";
    private final StandaloneUI aloneUI;
    public static final String SEARCH_PROC_ID = "IpacTableFromSource";

    public FFToolsExtCatalogCmd(StandaloneUI aloneUI) {
        super(COMMAND, "External Catalog load", "External Catalog load", true);
        this.aloneUI= aloneUI;
    }


    protected void doExecute(final Request inReq, AsyncCallback<String> callback) {

        final TableServerRequest req = new TableServerRequest(SEARCH_PROC_ID);
        req.setStartIndex(0);
        req.setPageSize(100);

        req.copyFrom(inReq);
        req.setRequestId(SEARCH_PROC_ID);


        SearchServices.App.getInstance().getRawDataSet(req, new AsyncCallback<RawDataSet>() {

            public void onFailure(Throwable caught) {
                if (caught != null) PopupUtil.showSevereError(caught);
            }

            public void onSuccess(RawDataSet result) {
               String title= "Loaded Table";

                if (req.containsParam(ServerParams.TITLE)) {
                    title= req.getParam(ServerParams.TITLE);
                }
                else if (req.containsParam(ServerParams.SOURCE)) {
                    req.setParam(ServerParams.SOURCE, FFToolEnv.modifyURLToFull(req.getParam(ServerParams.SOURCE)));
                    String url = req.getParam(ServerParams.SOURCE);
                    int idx = url.lastIndexOf('/');
                    if (idx<0) idx = url.lastIndexOf('\\');
                    if (idx > 1) {
                        title = url.substring(idx+1);
                    } else {
                        title = url;
                    }
                }
                newRawDataSet(title, result, req);


            }
        });


    }

    private void newRawDataSet(String title, RawDataSet rawDataSet, TableServerRequest req) {
        DataSet ds= DataSetParser.parse(rawDataSet);
        if (ds.getTotalRows()>0) {
            ((BaseTableData)ds.getModel()).setAttribute(WebPlotRequest.OVERLAY_POSITION,
                                                        req.getParam(WebPlotRequest.OVERLAY_POSITION));

            NewTableResults data= new NewTableResults(req, WidgetFactory.TABLE, title);
            WebEvent<NewTableResults> ev= new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED,
                                                                        data);
            WebEventManager.getAppEvManager().fireEvent(ev);
        }
        else {
            PopupUtil.showError("No Rows returned", "The search did not find any data");
        }
    }

}

