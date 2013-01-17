package edu.caltech.ipac.heritage.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.SearchByCampaignID;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;
import edu.caltech.ipac.heritage.ui.MoreOptionsPanel;

/**
`* @author tatianag
 * $Id: SearchByCampaignCmd.java,v 1.41 2012/09/11 19:10:12 tatianag Exp $
 */
public class SearchByCampaignCmd  extends HeritageRequestCmd {
    public static final String COMMAND_NAME = "SearchByCampaign";
    public static final String CAMPAIGN_KEY = "SearchByCampaign.field.campaign";

    public SearchByCampaignCmd() {
        super(COMMAND_NAME);
    }

    protected Form createForm() {

        InputField campaignField = FormBuilder.createField(CAMPAIGN_KEY);
        Widget fieldPanel = FormBuilder.createPanel(125, campaignField);

        HTML desc = GwtUtil.makeFaddedHelp("Enter campaign name or numeric ID e.g. IRSX008300 or 1094.");
        VerticalPanel vp = new VerticalPanel();
        vp.add(fieldPanel);
        vp.add(desc);
        Label spacer = new Label("");
        spacer.setHeight("5px");
        vp.add(spacer);
        MoreOptionsPanel options = new MoreOptionsPanel(new DataType[]{DataType.IRS_ENHANCED});
        vp.add(options);

        InstrumentPanel instPanel = new InstrumentPanel();
        final CollapsiblePanel moreOptions = new CollapsiblePanel("More options", instPanel, false);
//        moreOptions.setAnimationEnabled(false);

        instPanel.getEventManager().addListener(InstrumentPanel.ON_HIDE, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    moreOptions.collapse();
                }
            });
        vp.add(moreOptions);


        Form form = new Form();
        form.add(vp);
        form.setHelpId("searching.byCampId");
        return form;
    }

    @Override
    protected FormHub.Validated validate() {
        if (GwtUtil.validateBlank(getForm().getField(CAMPAIGN_KEY))) {
            return super.validate();
        } else {
            return new FormHub.Validated(false);
        }
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        createTablePreviewDisplay();

        if (MoreOptionsPanel.isAorRequested(req)) addTable(new SearchByCampaignID(SearchByCampaignID.Type.AOR, req));
        if (MoreOptionsPanel.isPbcdRequested(req)) addTable(new SearchByCampaignID(SearchByCampaignID.Type.PBCD, req));
        if (MoreOptionsPanel.isBcdRequested(req)) addTable(new SearchByCampaignID(SearchByCampaignID.Type.BCD, req));
        if (MoreOptionsPanel.isIrsEnhancedRequested(req))  {
            addTable(new SearchByCampaignID(SearchByCampaignID.Type.IRS_ENHANCED, req));
        }

        loadAll();

        setResults(getResultsPanel());
    }
}
