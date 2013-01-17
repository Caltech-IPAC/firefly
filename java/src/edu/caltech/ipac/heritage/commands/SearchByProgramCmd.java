package edu.caltech.ipac.heritage.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Preferences;
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
import edu.caltech.ipac.heritage.searches.SearchByProgramID;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;
import edu.caltech.ipac.heritage.ui.MoreOptionsPanel;
import edu.caltech.ipac.util.StringUtils;

public class SearchByProgramCmd extends HeritageRequestCmd {
    public static final String COMMAND_NAME = "SearchByProgram";
    public static final String PROGRAM_KEY = "SearchByProgram.field.program";

    public SearchByProgramCmd() {
        super(COMMAND_NAME);
    }

    protected Form createForm() {

        InputField programField = FormBuilder.createField(PROGRAM_KEY);
        Widget fieldPanel = FormBuilder.createPanel(125, programField);

        HTML desc = GwtUtil.makeFaddedHelp("Enter program name or numeric ID e.g. COLLISION or 30080.");
        Label spacer = new Label("");
        spacer.setHeight("5px");
        VerticalPanel vp = new VerticalPanel();
        vp.add(fieldPanel);
        vp.add(desc);
        vp.add(spacer);
        MoreOptionsPanel options = new MoreOptionsPanel(new DataType[]{DataType.IRS_ENHANCED});
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
        form.setHelpId("searching.byProgId");
        form.add(vp);
        return form;
    }

    @Override
    protected FormHub.Validated validate() {
        if (GwtUtil.validateBlank(getForm().getField(PROGRAM_KEY))) {
            return super.validate();
        } else {
            return new FormHub.Validated(false);
        }
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        createTablePreviewDisplay();

        if (MoreOptionsPanel.isAorRequested(req)) addTable(new SearchByProgramID(SearchByProgramID.Type.AOR, req));
        if (MoreOptionsPanel.isPbcdRequested(req)) addTable(new SearchByProgramID(SearchByProgramID.Type.PBCD, req));
        if (MoreOptionsPanel.isBcdRequested(req)) addTable(new SearchByProgramID(SearchByProgramID.Type.BCD, req));
        if (MoreOptionsPanel.isIrsEnhancedRequested(req))  {
            addTable(new SearchByProgramID(SearchByProgramID.Type.IRS_ENHANCED, req));
        }

        loadAll();

        setResults(getResultsPanel());
    }

    /**
     * returns a request
     * @param program  program
     * @return request
     */
    public static Request createRequest(String program) {
        SearchByProgramCmd cmd = new SearchByProgramCmd();
        Request req = cmd.makeRequest();
        req.setParam(PROGRAM_KEY, program);
        cmd.onFormSubmit(req);

        String prodType = Preferences.get(MoreOptionsPanel.PRODTYPE_PREF);
        if (StringUtils.isEmpty(prodType)) {
            prodType = MoreOptionsPanel.AOR + "," + MoreOptionsPanel.PBCD;
        }
        req.setParam(MoreOptionsPanel.PRODTYPE_KEY, prodType);

        return req;
    }
}
