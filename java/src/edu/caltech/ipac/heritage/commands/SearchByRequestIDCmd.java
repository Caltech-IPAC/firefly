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
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.heritage.commands.HeritageRequestCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.SearchByRequestID;
import edu.caltech.ipac.heritage.ui.MoreOptionsPanel;
import edu.caltech.ipac.util.StringUtils;

public class SearchByRequestIDCmd extends HeritageRequestCmd {
    public static final String COMMAND_NAME = "SearchByRequestID";
    public static final String REQUESTID_KEY = "SearchByRequestID.field.requestID";
    public static final String INCLUDE_SAME_CONSTRAINTS_KEY = "SearchByRequestID.field.includeSameConstraints";


    private InputField requestIDField;

    public SearchByRequestIDCmd() {
        super(COMMAND_NAME);
    }

    protected Form createForm() {
        // if more one reqkey, it will be separate by the delimiter ","
        requestIDField = FormBuilder.createField(REQUESTID_KEY);
        Widget byRequestID = FormBuilder.createPanel(80, requestIDField);

        HTML desc = GwtUtil.makeFaddedHelp("Enter one or more values delimited by a ',' e.g. 21641216,17758208.");
        Label spacer = new Label("");
        spacer.setHeight("5px");
        VerticalPanel vp = new VerticalPanel();
        vp.add(byRequestID);
        vp.add(desc);
        vp.add(spacer);
        vp.add(SimpleInputField.createByProp(INCLUDE_SAME_CONSTRAINTS_KEY));
        Label spacer2 = new Label("");
        spacer2.setHeight("5px");
        vp.add(spacer2);
        MoreOptionsPanel options = new MoreOptionsPanel(new DataType[]{DataType.IRS_ENHANCED});
        vp.add(options);
        Label spacer3 = new Label("");
        spacer3.setHeight("5px");
        vp.add(spacer3);


        Form form = new Form();
        form.setHelpId("searching.byReqId");
        form.add(vp);
        return form;
    }

    @Override
    protected FormHub.Validated validate() {
        if (GwtUtil.validateIntList(requestIDField)) {
            return super.validate();
        } else {
            return new FormHub.Validated(false);
        }
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        createTablePreviewDisplay();

        if (MoreOptionsPanel.isAorRequested(req)) addTable(new SearchByRequestID(SearchByRequestID.Type.AOR, req));
        if (MoreOptionsPanel.isPbcdRequested(req)) addTable(new SearchByRequestID(SearchByRequestID.Type.PBCD, req));
        if (MoreOptionsPanel.isBcdRequested(req)) addTable(new SearchByRequestID(SearchByRequestID.Type.BCD, req));
        if (MoreOptionsPanel.isIrsEnhancedRequested(req))  {
            addTable(new SearchByRequestID(SearchByRequestID.Type.IRS_ENHANCED, req));
        }

        loadAll();

        setResults(getResultsPanel());
    }

    /**
     * returns a request
     * @param reqkey AORKEY
     * @return Request
     */
    public static Request createRequest(String reqkey) {
        SearchByRequestIDCmd cmd = new SearchByRequestIDCmd();
        Request req = cmd.makeRequest();
        req.setParam(REQUESTID_KEY, reqkey);
        cmd.onFormSubmit(req);

        String prodType = Preferences.get(MoreOptionsPanel.PRODTYPE_PREF);
        if (StringUtils.isEmpty(prodType)) {
            prodType = MoreOptionsPanel.AOR + "," + MoreOptionsPanel.PBCD;
        }
        req.setParam(MoreOptionsPanel.PRODTYPE_KEY, prodType);

        return req;
    }


}
