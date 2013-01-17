package edu.caltech.ipac.heritage.commands;


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.DatePanel;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.SearchByDate;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;
import edu.caltech.ipac.heritage.ui.MoreOptionsPanel;

/**
 * @author tatianag
 * $Id: SearchByDateCmd.java,v 1.39 2012/09/11 19:10:12 tatianag Exp $
 */
public class SearchByDateCmd  extends HeritageRequestCmd {
    public static final String COMMAND_NAME = "SearchByDate";
    public static final String START_DATE_KEY = DatePanel.START_DATE_KEY;
    public static final String END_DATE_KEY = DatePanel.END_DATE_KEY;

    private DatePanel datePanel;

    public SearchByDateCmd() {
        super(COMMAND_NAME);
    }

    protected Form createForm() {

        datePanel = new DatePanel(14*24*60*60);
        datePanel.setIntervalViolationError("Observation Date searches can only cover one 14 day period.");
        VerticalPanel vp = new VerticalPanel();
        vp.add(datePanel);
        Label spacer = new Label("");
        spacer.setHeight("5px");
        vp.add(spacer);
        MoreOptionsPanel options = new MoreOptionsPanel(new DataType[]{}); // no level3 options
        vp.add(options);
        //Label spacer1 = new Label("");
        //spacer1.setHeight("5px");
        //vp.add(spacer1);
        
        InstrumentPanel instPanel = new InstrumentPanel();
        final CollapsiblePanel moreOptions = new CollapsiblePanel("More options", instPanel, false);

        instPanel.getEventManager().addListener(InstrumentPanel.ON_HIDE, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    moreOptions.collapse();
                }
            });
        vp.add(moreOptions);


        Form form = new Form();
        form.add(vp);
        form.setHelpId("searching.byObsDate");
        return form;
    }

    @Override
    protected FormHub.Validated validate() {
        boolean hasStartDate = GwtUtil.validateBlank(getForm().getField(START_DATE_KEY));
        boolean hasEndDate = GwtUtil.validateBlank(getForm().getField(END_DATE_KEY));
        return  new FormHub.Validated(hasStartDate &&
                                        hasEndDate &&
                                        datePanel.validate());
    }

    @Override
    protected String getInitFocusField() {
        return START_DATE_KEY;
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        createTablePreviewDisplay();

        if (MoreOptionsPanel.isAorRequested(req)) addTable(new SearchByDate(SearchByDate.Type.AOR, req));
        if (MoreOptionsPanel.isPbcdRequested(req)) addTable(new SearchByDate(SearchByDate.Type.PBCD, req));
        if (MoreOptionsPanel.isBcdRequested(req)) addTable(new SearchByDate(SearchByDate.Type.BCD, req));
        loadAll();

        setResults(getResultsPanel());

    }

}
