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

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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