package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.gen2.table.event.client.RowSelectionEvent;
import com.google.gwt.gen2.table.event.client.RowSelectionHandler;
import com.google.gwt.gen2.table.event.client.TableEvent;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.creator.eventworker.AbstractDatasetQueryWorker;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.BasicTable;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Set;


/**
 * Date: Aug 5, 2010
*
* @author loi
* @version $Id
*/
public class TableFilteringPreview extends AbstractTablePreview {
    private SimplePanel container = new SimplePanel();
    private String id;
    private int raIdx,decIdx,objNameIdx, idIdx;
    private TablePanel mainTable = null;

    public TableFilteringPreview(String name, String id) {
        super(name,"Apply filter to the primary table");
        this.id = id;
        setDisplay(container);
        container.setSize("100%", "100%");
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);

        WebEventListener wel =  new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    if (ev.getData() instanceof DataSet && match(ev)) {
                        TableFilteringPreview.this.mainTable = TableFilteringPreview.this.getEventHub().getActiveTable();
                        loadTable((DataSet) ev.getData());
                    }
                }
            };
        hub.getEventManager().addListener(EventHub.ON_EVENT_WORKER_COMPLETE, wel);
        hub.getEventManager().addListener(EventHub.ON_EVENT_WORKER_START, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        if (match(ev)) {
                            container.clear();
                        }
                    }
                });
    }

    protected void updateDisplay(TablePanel table) {
        /*Widget w = container.getWidget();
        if (w != null) {
            container.remove(w);
        }*/
    }

    protected void loadTable(final DataSet data) {
        Widget w = container.getWidget();
        if (w != null) {
            container.remove(w);
        }

        raIdx=data.getModel().getColumnNames().indexOf("ra");
        decIdx=data.getModel().getColumnNames().indexOf("dec");
        idIdx=data.getModel().getColumnNames().indexOf(CatalogRequest.UPDLOAD_ROW_ID);
        objNameIdx=data.getModel().getColumnNames().indexOf("objname");
        data.getColumn(idIdx).setHidden(true);
        //data.getColumn(idIdx).setVisible(false);
        //data.getColumn(idIdx).setWidth(0);
        data.getColumn(objNameIdx).setWidth(60);
        data.getColumn(raIdx).setWidth(60);
        data.getColumn(decIdx).setWidth(60);
        
        final BasicTable table = new BasicTable(data);
        table.setSize("100%", "100%");
        table.getDataTable().setSelectionEnabled(true);

        table.getDataTable().addRowSelectionHandler(new RowSelectionHandler(){
            public void onRowSelection(RowSelectionEvent event) {
                String filterStr;
                String colName, v;
                Set<TableEvent.Row> srows = event.getSelectedRows();
                ArrayList<String> fList = new ArrayList<String>();

                for(TableEvent.Row r : srows) {
                    int idx = r.getRowIndex();
                    TableData.Row row = /*data.getModel().getRow(idx);*/ table.getRows().get(idx);
                    //since RA Dec are unique keys in the table, we can simply use ra, dec columns for filtering.
                    /*v = getColumnValue(row,objNameIdx);
                    if (v!=null && v.length()>0) {
                        colName = getColumnName(data, objNameIdx);
                        filterStr = colName + " = " + v;
                        fList.add(filterStr);
                    } else*/ {
                        /*colName = getColumnName(data, raIdx);
                        v = getColumnValue(row,raIdx);
                        filterStr = colName + " = " + v;
                        fList.add(filterStr);
                        colName = getColumnName(data, decIdx);
                        v = getColumnValue(row,decIdx);
                        filterStr = colName + " = " + v;
                        fList.add(filterStr);*/
                        colName = getColumnName(data, idIdx);
                        v = getColumnValue(row,idIdx);
                        filterStr = colName + " = " + v;
                        fList.add(filterStr);
                    }

                    TableFilteringPreview.this.mainTable.getDataModel().setFilters(fList);
                    TableFilteringPreview.this.mainTable.getDataModel().fireDataStaleEvent();

                    setActiveTarget(row);
                }
            }

        });

        table.getDataTable().selectRow(0, true);
        container.setWidget(table);        
    }

    private void setActiveTarget(TableData.Row row) {
        //todo: set active target
        Object ra = row.getValue("ra");
        Object dec = row.getValue("dec");
        if (ra!=null && dec!=null) {
            ActiveTarget.getInstance().setActive(
                    new WorldPt(Double.parseDouble(ra.toString()), Double.parseDouble(dec.toString())));
        }
    }

    private boolean match(WebEvent ev) {
        Object source = ev.getSource();
        if (source instanceof AbstractDatasetQueryWorker) {
            AbstractDatasetQueryWorker w = (AbstractDatasetQueryWorker)source;
            if (w.getID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String getColumnName(DataSet data, int idx) {
        return data.getColumn(idx).getName();
    }

    private static String getColumnValue(TableData.Row row, int idx) {
        return String.valueOf(row.getValue(idx));
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
