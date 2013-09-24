package edu.caltech.ipac.fuse.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.fuse.commands.FuseRequestCmd;
import edu.caltech.ipac.fuse.core.SearchAdmin;

/**
 * Date: 9/12/13
 *
 * @author loi
 * @version $Id: $
 */
public class FuseSearchPanel extends Composite {
    private SearchAdmin searchAdmin;
    private EventHandler handler;

    public FuseSearchPanel() {
        this.searchAdmin = new SearchAdmin();
        HTML msg = new HTML("<font size=+3> Search panel is under construction</font>");
        msg.setSize("600px", "400px");
        initWidget(msg);
    }

    public SearchAdmin getSearchAdmin() {
        return searchAdmin;
    }

    public FormHub.Validated validate() {
        return null;
    }

    public void populateRequest(Request req, AsyncCallback<String> callback) {

    }

    public void clear() {

    }

    public void populateFields(Request req) {

    }

    public void setHandler(EventHandler handler) {
        this.handler = handler;
    }


    public interface EventHandler {
        void onSearch();
        void onSearchAndContinue();
        void onClose();
    }
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
