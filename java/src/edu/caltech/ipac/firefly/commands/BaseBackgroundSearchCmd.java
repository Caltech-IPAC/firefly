package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.core.SearchAdmin;
import edu.caltech.ipac.firefly.ui.searchui.FuseSearchPanel;
import edu.caltech.ipac.firefly.ui.searchui.SearchUI;

import java.util.List;

/**
 * Date: Sep 12, 2013
 *
 * @author loi
 * @version $Id: CommonRequestCmd.java,v 1.44 2012/10/03 22:18:11 loi Exp $
 */
public abstract class BaseBackgroundSearchCmd extends RequestCmd implements FuseSearchPanel.EventHandler {

    private FuseSearchPanel searchPanel;

    public BaseBackgroundSearchCmd(String commandName) {
        super(commandName);
    }

    public boolean init() {
        if (searchPanel==null) {
            searchPanel = new FuseSearchPanel(getSearchUIList());
            searchPanel.setHandler(this);
        }
        return true;
    }

    protected abstract List<SearchUI> getSearchUIList();


    protected FormHub.Validated validate() {
        return searchPanel == null ? new FormHub.Validated() : searchPanel.validate();
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {

        // fill the form's field based on the request parameters.
        if (searchPanel != null) {
            searchPanel.clear();
            searchPanel.populateFields(req);
        }
        else {
            init();
        }

        if (req!=null && req.isDoSearch()) {
            // process the search request
            doProcessRequest(req);
        } else {
            searchPanel.start();
            Application.getInstance().getLayoutManager().getRegion(LayoutManager.DROPDOWN_REGION).setDisplay(searchPanel);
        }
        callback.onSuccess("");
    }

    protected void doProcessRequest(Request req) {
        SearchAdmin.getInstance().submitSearch(req);
    }

//====================================================================
//  implements SearchPanel's EventHandler
//====================================================================

    public void onSearch() {
        Application.getInstance().getToolBar().getDropdown().close();
    }

    public void onSearchAndContinue() { }

    public void onClose() {
        Application.getInstance().getLayoutManager().getRegion(LayoutManager.DROPDOWN_REGION).collapse();
    }

//====================================================================
//  Private Methods
//====================================================================


}
