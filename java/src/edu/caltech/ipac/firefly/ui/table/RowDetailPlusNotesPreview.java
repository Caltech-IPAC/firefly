package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;


/**
 * Date: Dec 6, 2010
 *
 * @author loi
 * @version $Id: RowDetailPlusNotesPreview.java,v 1.7 2012/12/06 23:49:14 tlau Exp $
 */
public class RowDetailPlusNotesPreview extends AbstractTablePreview {

    private RowDetailPreview details;
    private HTML notes = new HTML();
    private ScrollPanel notesView;
    private SplitLayoutPanel mainPanel = new SplitLayoutPanel();
    private String searchProcId;
    private int notesInitialHeight = 75;

    public RowDetailPlusNotesPreview(String name, String searchProcId) {
        this.searchProcId = searchProcId;
        notesView = new ScrollPanel(notes);
        details = new RowDetailPreview(name);
        mainPanel.addNorth(notesView, notesInitialHeight);
        mainPanel.add(details);
        initWidget(mainPanel);
    }

    public RowDetailPlusNotesPreview(String name, String searchProcId, String stateId) {
        this.searchProcId = searchProcId;
        notesView = new ScrollPanel(notes);
        details = new RowDetailPreview(name);
        details.setStateId(stateId);
        mainPanel.addNorth(notesView, notesInitialHeight);
        mainPanel.add(details);
        initWidget(mainPanel);
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);
        details.bind(hub);
        WebEventListener wel =  new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    TablePanel table = (TablePanel) ev.getSource();
                    if (table != null && GwtUtil.isVisible(table.getElement())) {
                        updateDisplay(table);
                    } else {
                        details.showTable(false);
                    }
                }
            };
        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);
    }

    protected void updateDisplay(TablePanel table) {
        if (table == null || table.getTable() == null|| !GwtUtil.isVisible(mainPanel.getElement())) {
            return;
        }

        if (searchProcId == null) {
            GwtUtil.SplitPanel.hideWidget(mainPanel, notesView);
        } else {
            TableData.Row selRow = table.getTable().getHighlightedRow();
            if (selRow != null) {
                doNotesLoad(selRow);
            } else {
                setNotes("");
            }
        }

    }

    private void doNotesLoad(final TableData.Row selRow) {
        ServerTask<RawDataSet> getNotesTask = new ServerTask<RawDataSet>() {
            public void doTask(AsyncCallback<RawDataSet> passAlong) {   
                TableServerRequest request = new TableServerRequest(searchProcId);
                request.setPageSize(1000);
                for(Object o : selRow.getValues().keySet()) {
                    String key = String.valueOf(o);
                    request.setParam(key, String.valueOf(selRow.getValue(key)));
                }
                SearchServices.App.getInstance().getRawDataSet(request, passAlong);
            }

            public void onSuccess(RawDataSet result) {
                DataSet dataset = DataSetParser.parse(result);
                if (dataset.getTotalRows() > 0) {
                    StringBuffer html = new StringBuffer();
                    for(int i = 0; i < dataset.getTotalRows(); i++) {
                        for (TableDataView.Column c : dataset.getColumns()) {
                            if (c.isVisible()) {
                                html.append("<b>").append(c.getTitle()).append("</b><br>");
                                html.append(dataset.getModel().getRow(i).getValue(c.getName()));
                            }
                        }
                    }
                    setNotes(html.toString());
                } else {
                    setNotes("");
                }
            }

            public void onFailure(Throwable caught) {
                setNotes("");
                PopupUtil.showSevereError(caught);
            }

        };
        getNotesTask.start();

    }

    private void setNotes(String html) {
        notes.setHTML(html);
        if (StringUtils.isEmpty(html)) {
            GwtUtil.SplitPanel.hideWidget(mainPanel, notesView);
        } else {
            GwtUtil.SplitPanel.showWidget(mainPanel, notesView);
        }
    }
}

