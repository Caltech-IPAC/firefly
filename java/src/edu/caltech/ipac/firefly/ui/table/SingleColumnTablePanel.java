package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.AbstractScrollTable;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * Date: Feb 10, 2009
 *
 * @author loi
 * @version $Id: SingleColumnTablePanel.java,v 1.3 2010/12/03 02:11:11 loi Exp $
 */
public class SingleColumnTablePanel extends TablePanel {

    public SingleColumnTablePanel(Loader<TableDataView> loader) {
        this("untitled", loader);
    }

    public SingleColumnTablePanel(String name, Loader<TableDataView> loader) {
        super(name, loader);
    }

    @Override
    protected BasicPagingTable newTable(MutableTableModel<TableData.Row> model, TableDataView dataset) {
        return new BasicPagingTable(getName(), model, new SingleColDefinition(getName(), dataset));
    }

    @Override
    public void onInit() {
        super.onInit();
        getTable().setResizePolicy(AbstractScrollTable.ResizePolicy.FILL_WIDTH);
        getTable().addStyleName("singleColumn");

        if (BrowserUtil.isBrowser(Browser.IE)) {
            WebEventManager.getAppEvManager().addListener(Name.WINDOW_RESIZE, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        getTable().getDataTable().setColumnWidth(0, getTable().getDataTable().getOffsetWidth()-30);
                    }
                });
            DeferredCommand.addCommand(new Command(){
                public void execute() {
                    getTable().getDataTable().setColumnWidth(0, getTable().getDataTable().getOffsetWidth()-30);
                }
            });
        }
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
