/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.gen2.table.override.client.FlexTable;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Date: Mar 26, 2010
*
* @author loi
* @version $Id: RowDetailPreview.java,v 1.7 2012/11/14 00:55:03 tlau Exp $
*/
public class RowDetailPreview extends AbstractTablePreview {

    private static final int NO_DATA = 0;
    private static final int HAS_DATA = 1;

    private BasicTable view;
    private DeckPanel display = new DeckPanel();
    private BaseTableData data;
    private DataSet dataset;
    private String stateId=null;

    public RowDetailPreview(String name) {

        super(name,"Get additional information for the highlighted row");

        data = new BaseTableData(new String[]{"Name", "Value"});
        dataset = new DataSet(data);
        dataset.getColumn(0).setWidth(150);
        dataset.getColumn(1).setWidth(800);
        view = new BasicTable(dataset);
        view.getHeaderTable().insertRow(0);
        view.getHeaderTable().setHTML(0, 0, "<b>Additional Information</b>");
        FlexTable.FlexCellFormatter formatter = view.getHeaderTable().getFlexCellFormatter();
        formatter.setColSpan(0, 0, 2);

        display.insert(new HTML("<b>No additional information available</b>"), NO_DATA);
        display.insert(view, HAS_DATA);

        setDisplay(display);
        display.setSize("99%", "99%");
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);

        WebEventListener wel =  new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    final TablePanel table = (TablePanel) ev.getSource();
                    if (table != null ) {
                        updateDisplay(table);
                    }
                }
            };
        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);
    }

    public void onHide() {
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    protected void clear() {

    }

    protected void showTable(boolean show) {
        if (show) {
            display.showWidget(HAS_DATA);
        } else {
            display.showWidget(NO_DATA);
        }
    }

    protected void updateDisplay(final TablePanel table) {

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                if (table == null || table.getTable() == null|| !GwtUtil.isVisible(display.getElement())) {
                    showTable(false);
                    return;
                }

                clear();
                if (stateId == null || table.getStateId().equals(stateId)) {
                    TableData.Row selRow = table.getTable().getHighlightedRow();
                    if (selRow != null) {
                        doTableLoad(table, selRow);
                    } else {
                        loadTable(null);
                    }
                }
            }
        });
    }

    protected void doTableLoad(TablePanel table, TableData.Row selRow) {
        Map<String, String> data = new LinkedHashMap<String, String>();

        TableDataView dataset = table.getDataset();
        if (selRow != null) {
            for (TableDataView.Column tc :dataset.getColumns()) {
                if (!tc.isHidden()) {
                    String v = String.valueOf(selRow.getValue(tc.getName()));
                    data.put(tc.getTitle(), v);
                }
            }
        }
        loadTable(data);
    }

    protected void loadTable(Map<String, String> data) {

        if (data == null || data.size() == 0) {
            showTable(false);
            return;
        }
        this.data.clear();
        for (Map.Entry<String,String> entry : data.entrySet()) {
            this.data.addRow(new String[] {entry.getKey(), entry.getValue()});
        }
        view.loadTable(dataset);

        showTable(true);
    }

    protected BasicTable getView() {
        return view;
    }

}
