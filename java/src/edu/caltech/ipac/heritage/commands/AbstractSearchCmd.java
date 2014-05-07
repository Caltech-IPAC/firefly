package edu.caltech.ipac.heritage.commands;

import com.google.gwt.gen2.table.event.client.RowHighlightEvent;
import com.google.gwt.gen2.table.event.client.RowHighlightHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.TitlePanel;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePrimaryDisplay;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.table.SingleColDefinition;
import edu.caltech.ipac.firefly.ui.table.SingleColumnTablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.heritage.searches.AbstractSearch;

/**
`* @author tatianag
 * $Id: AbstractSearchCmd.java,v 1.31 2012/08/09 01:09:26 loi Exp $
 */
public class AbstractSearchCmd extends CommonRequestCmd {
    public static final String COMMAND_NAME = "AbstractSearch";
    public static final String SEARCH_FIELD_PROP = "AbstractSearch.field.search";

    private InputField queryField;
    private SingleColumnTablePanel tablePanel;
    private TitlePanel titlePanel;

    public AbstractSearchCmd() {
        super(COMMAND_NAME);
    }

    protected Form createForm() {

        queryField = FormBuilder.createField(SEARCH_FIELD_PROP);
        Widget fieldPanel = FormBuilder.createPanel(50, queryField);

        HTML desc = GwtUtil.makeFaddedHelp("Enter the string on which you'd like to search.");
        Label spacer = new Label("");
        spacer.setHeight("5px");
        VerticalPanel vp = new VerticalPanel();
        vp.add(fieldPanel);
        vp.add(desc);
        vp.add(spacer);

        Form form = new Form();
        form.add(vp);
        form.setHelpId("searching.byAbstract");
        return form;
    }

    @Override
    protected void onFormSubmit(Request req) {
        mask("Loading...");
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        AbstractSearch config = new AbstractSearch(req);
        tablePanel = new SingleColumnTablePanel(config.getTitle(), config.getLoader());
        tablePanel.showToolBar(false);
        tablePanel.showPagingBar(true);
        tablePanel.getEventManager().addListener(TablePanel.ON_SHOW, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        DeferredCommand.addCommand(new Command(){
                            public void execute() {
                                if (tablePanel.getTable() != null) {
                                    tablePanel.getTable().getDataTable().deselectAllRows();
                                    tablePanel.getTable().getDataTable().setSelectionEnabled(false);
                                }
                            }
                        });
                    }
                });


        final TablePrimaryDisplay tableDisplay = new TablePrimaryDisplay(tablePanel);
        getTableUiLoader().addTable(tableDisplay);
        getTableUiLoader().loadAll();

//        ShadowedPanel sp = new ShadowedPanel(tablePanel);
        setResults(tablePanel);
    }

    @Override
    public void onLoaded(PrimaryTableUI table) {
        super.onLoaded(table);
        postInit(tablePanel);
    }

    void postInit(final TablePanel table) {
        table.showColumnHeader(false);
        table.showOptionsButton(false);
        table.getTable().setTableDefinition(new SingleColDefinition(
                new CustomColDef("Results", table.getDataset())));
        table.showTitle(true);
        table.getTable().reloadPage();
    }

//====================================================================
//
//====================================================================

    class RowHLHandler implements RowHighlightHandler {
        TablePanel table;
        public RowHLHandler(TablePanel table) {
            this.table = table;
        }

        public void onRowHighlight(RowHighlightEvent event) {
            TableData.Row row = table.getTable().getRowValues().get(event.getValue().getRowIndex());
        }
    }

    class CustomColDef extends SingleColDefinition.SingleColDef {

        public CustomColDef(String name, TableDataView tableDef) {
            super(name, tableDef);
        }

        @Override
        public String getCellValue(TableData.Row row) {
            StringBuffer rval = new StringBuffer();
            String title = String.valueOf(row.getValue("progtitle"));
            String name = String.valueOf(row.getValue("progname"));
            String cat = String.valueOf(row.getValue("sciencecat"));
            String pAbstract = String.valueOf(row.getValue("abstract"));

            String progid = String.valueOf(row.getValue("progid"));
            Request req = SearchByProgramCmd.createRequest(progid);
            req.setIsDrilldown(true);
            req.setIsDrilldownRoot(false);

            rval.append("<a href='javascript:ffProcessRequest(\"" + req.toString() + "\");'>").append("<font style='font-size: 11pt;'>" + title + "</font>").append("</a>");
            rval.append("<div style='margin-left: 10px'>");
            rval.append(makeEntry("Program Name", name));
            rval.append("&nbsp;&nbsp;&nbsp;").append(makeEntry("Program ID", row.getValue("progid")));
            rval.append("&nbsp;&nbsp;&nbsp;").append(makeEntry("PI", row.getValue("pi")));
            rval.append("&nbsp;&nbsp;&nbsp;").append(makeEntry("Category", cat));
//            rval.append("<br>").append(makeEntry("Abstract", ""));
            rval.append("<br>").append(pAbstract).append("</p></div>");
            return rval.toString();
        }

    }

}
