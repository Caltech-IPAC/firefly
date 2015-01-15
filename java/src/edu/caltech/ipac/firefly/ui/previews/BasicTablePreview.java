/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.BasicTable;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;


/**
 * Date: Aug 5, 2010
*
* @author loi
* @version $Id
*/
public class BasicTablePreview extends AbstractTablePreview {
    private SimplePanel container = new SimplePanel();

    public BasicTablePreview(String name) {
        super(name,"Get additional information for the highlighted row");
        setDisplay(container);
        container.setSize("100%", "100%");
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);

        WebEventListener wel =  new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    if (ev.getData() instanceof DataSet) {
                        loadTable((DataSet) ev.getData());
                    }
                }
            };
        hub.getEventManager().addListener(EventHub.ON_EVENT_WORKER_COMPLETE, wel);
        hub.getEventManager().addListener(EventHub.ON_EVENT_WORKER_START, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        container.clear();
                    }
                });
    }

    protected void updateDisplay(TablePanel table) {
        /*Widget w = container.getWidget();
        if (w != null) {
            container.remove(w);
        }*/
    }

    protected void loadTable(DataSet data) {
        Widget w = container.getWidget();
        if (w != null) {
            container.remove(w);
        }

        BasicTable table = new BasicTable(data);
        table.setResizePolicy(ScrollTable.ResizePolicy.FILL_WIDTH);
        table.setSize("100%", "100%");
        container.setWidget(table);
    }

}

