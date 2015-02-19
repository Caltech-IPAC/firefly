/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.event.WebEvent;

/**
 * Date: Feb 10, 2009
 *
 * @author loi
 * @version $Id: SelectableTablePanel.java,v 1.4 2010/12/03 02:11:11 loi Exp $
 */
public class SelectableTablePanel extends TablePanel {
    public static final String SEL_INFO_KEY = "SelInfo";

    protected SelectionTable table;

    public SelectableTablePanel(Loader<TableDataView> loader) {
        this("untitled", loader);
    }

    public SelectableTablePanel(String name, Loader<TableDataView> loader) {
        super(name, loader);
    }

    public SelectionTable getSelectionTable(){
        return table;
    }

//====================================================================
//  Implementing StatefulWidget
//====================================================================
    @Override
    public void recordCurrentState(Request req) {
        super.recordCurrentState(req);
        if (table.getSelectInfo().getSelectedCount() > 0) {
            req.setParam(getStateId() + "_" + SEL_INFO_KEY, String.valueOf(table.getSelectInfo()));
        }
    }

    @Override
    protected void onMoveToReqStateCompleted(Request req) {

        if (req.getParam(getStateId() + "_" + SEL_INFO_KEY) != null) {
            SelectionInfo selInfo = SelectionInfo.parse(req.getParam(getStateId() + "_" + SEL_INFO_KEY));
            table.setSelectionInfo(selInfo);
        }
    }

    //====================================================================

    @Override
    protected BasicPagingTable newTable(DataSetTableModel model, TableDataView dataset) {
        table = new SelectionTable(getName(), model, dataset);
        return table;
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        table.addSelectionTableListener(new SelectionTable.SelectListener(){
                public void onRowSelectChange(SelectionTable table, SelectionInfo selectInfo) {
                    getEventManager().fireEvent(new WebEvent(SelectableTablePanel.this, ON_ROWSELECT_CHANGE));
                }
        });

    }

    @Override
    public void doFilters() {
        super.doFilters();
        table.deselectAll();
    }

    @Override
    protected void onSorted() {
        super.onSorted();
        table.deselectAll();
    }

    @Override
    protected void bindDataViewToTable(TableDataView dataset) {
        super.bindDataViewToTable(dataset);
        new TableDataViewToTableAdapter(dataset);
    }

    private class TableDataViewToTableAdapter {

        private TableDataView dataset;
        boolean sinkEvent = true;

        public TableDataViewToTableAdapter(TableDataView dataset) {
            this.dataset = dataset;
            bind();
        }

        protected void bind() {

            table.addSelectionTableListener(new SelectionTable.SelectListener(){
                        public void onRowSelectChange(SelectionTable table, SelectionInfo selectInfo) {
                            if (sinkEvent) {
                                sinkEvent = false;
                                dataset.setSelectionInfo(selectInfo);
                                sinkEvent = true;
                            }
                        }
                    });

            dataset.addPropertyChangeListener(new PropertyChangeListener(){
                        public void propertyChange(PropertyChangeEvent pce) {

                            if (sinkEvent) {
                                sinkEvent = false;
                                if (pce.getPropertyName().equals(DataSet.ROW_SELECT_ALL)) {
                                    table.selectAll();
                                } else if (pce.getPropertyName().equals(DataSet.ROW_DESELECT_ALL)) {
                                    table.deselectAll();
                                } else if (pce.getPropertyName().equals(DataSet.ROW_SELECTED)) {
                                    table.select((Integer[]) pce.getNewValue());
                                } else if (pce.getPropertyName().equals(DataSet.ROW_DESELECTED)) {
                                    table.deselect((Integer[]) pce.getNewValue());
                                }
                                sinkEvent = true;
                            }
                        }
                    });
        }

    }


}
