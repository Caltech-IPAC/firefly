package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.MutableTableModel;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.firefly.data.table.TableData;
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

    private SelectionTable table;

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
    protected BasicPagingTable newTable(MutableTableModel<TableData.Row> model, TableDataView dataset) {
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
    protected void onFiltered() {
        table.deselectAll();
    }

    @Override
    protected void onSorted() {
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
