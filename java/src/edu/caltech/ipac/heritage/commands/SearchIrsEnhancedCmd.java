package edu.caltech.ipac.heritage.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.catalog.CatColumnInfo;
import edu.caltech.ipac.firefly.ui.catalog.CatddPanel;
import edu.caltech.ipac.firefly.ui.table.TableOptions;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.SearchIrsEnhanced;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author tatianag
 *         $Id: SearchIrsEnhancedCmd.java,v 1.6 2012/10/08 22:51:25 tatianag Exp $
 */
public class SearchIrsEnhancedCmd extends HeritageRequestCmd {
    public static final String COMMAND_NAME = "SearchIrsEnhanced";
    public static final String SELECTED_COLS_KEY = CatddPanel.SELECTED_COLS_KEY;
    public static final String CONSTRAINTS_KEY = CatddPanel.CONSTRAINTS_KEY;

    private String SelectedColumns = "reqkey,tn,object,ra,dec,cra,cdec,date_obs,bandpass,obsrvr,progid";
    private String SelectedConstraints = "";


    public SearchIrsEnhancedCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected Form createForm() {
        CatalogRequest req = new CatalogRequest(CatalogRequest.RequestType.GATOR_DD);
        //req.setQueryCatName(_currentCatalog.getQueryCatName());

        WebAppProperties props = Application.getInstance().getProperties();
        req.setQueryCatName(props.getProperty("irsEnhanced.gator.catname"));
        req.setDDShort(false); // long DD is requested

        boolean defSelect = SelectedColumns.isEmpty();

        HTML desc;
        final CatddPanel panel;
        VerticalPanel vp = new VerticalPanel();
        Form form = new Form();
        form.setHelpId("searching.byIrsEnh");
        try {
            panel = new CatddPanel(new CatColumnInfo() {
                public void setSelectedColumns(String values) {
                    SelectedColumns = values;
                }

                public void setSelectedConstraints(String values) {
                    SelectedConstraints = values;
                }
            }, req.getParams(), SelectedColumns, "ra,dec", SelectedConstraints, defSelect);
            panel.setPixelSize(800, 200);

            vp.add(panel);

            form.addSubmitButton(GwtUtil.makeFormButton("<b>Search</b>",
                new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        panel.setColumns();
                        panel.setConstraints();
                        createAndProcessRequest();
                    }
            }));

        } catch (Exception e) {
            desc = new HTML("Search is not available: <b>Unable to get IRS Enhanced DD<b>");
            vp.add(desc);


        }

        form.add(vp);
        return form;


     }

    @Override
    protected void onFormSubmit(Request req) {
        super.onFormSubmit(req);
    }


    @Override
    protected void processRequest(Request req, AsyncCallback<String> callback) {
        // we always want long form, all columns
        // but we want only selected columns to be visible
        String cols = req.getParam(SELECTED_COLS_KEY);
        if (!StringUtils.isEmpty(cols)) {
            TableOptions.setPrefVisibleColumns(DataType.IRS_ENHANCED.getTitle(), cols.split(","));
        }

        createTablePreviewDisplay();
        addTable(new SearchIrsEnhanced(req));
        loadAll();
        setResults(getResultsPanel());

    }
}
