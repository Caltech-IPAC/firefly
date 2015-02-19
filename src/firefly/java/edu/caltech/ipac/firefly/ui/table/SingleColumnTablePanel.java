/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.AbstractScrollTable;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
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
    protected BasicPagingTable newTable(DataSetTableModel model, TableDataView dataset) {
        return new BasicPagingTable(getName(), model, new BasicPagingTable.DataTable(), new SingleColDefinition(getName(), dataset));
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

