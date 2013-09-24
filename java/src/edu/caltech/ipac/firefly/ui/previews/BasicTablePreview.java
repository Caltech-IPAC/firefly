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
