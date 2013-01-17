package edu.caltech.ipac.heritage.commands;


import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.EphPair;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.rpc.TargetServices;
import edu.caltech.ipac.firefly.ui.*;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.SearchByNaifID;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;
import edu.caltech.ipac.heritage.ui.MoreOptionsPanel;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;


/**
 * Search by NAIF ID
 * $Id: SearchByNaifIDCmd.java,v 1.47 2012/09/11 19:10:13 tatianag Exp $
 */
public class SearchByNaifIDCmd  extends HeritageRequestCmd {

    public static final String COMMAND_NAME = "SearchByNaifID";
    public static final String NAIFID_KEY = "SearchByNaifID.field.naifID";
    public static final String TARGET_NAME_KEY = "SearchByNaifID.field.tgtName";
    public static final String START_DATE_KEY = MoreOptionsPanel.START_DATE_KEY;
    public static final String END_DATE_KEY = MoreOptionsPanel.END_DATE_KEY;

    private InputField targetField;
    private InputField naifIDField;
    Widget byNaifID;
    String lastResolved = null;
    String lastNaifIDResolved = null;

    public SearchByNaifIDCmd() {
        super(COMMAND_NAME);       
    }

    @Override
    protected FormHub.Validated validate() {
        if (GwtUtil.validateBlank(naifIDField) &&
                GwtUtil.validateIntList(naifIDField)) {
            return super.validate();
        } else {
            return new FormHub.Validated(false);
        }
    }


    protected Form createForm() {
        FormBuilder.Config config = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                            100, 5, HorizontalPanel.ALIGN_LEFT);
        DatePanel datePanel = new DatePanel((10 * 365 + 3) * 24 * 60 * 60, START_DATE_KEY, END_DATE_KEY, config);
        datePanel.setIntervalViolationError("Moving object searches can only cover 10-year period.");
        //DOM.setStyleAttribute(datePanel.getElement(),"border", "1px solid #ccc");
        InstrumentPanel instPanel = new InstrumentPanel();
        VerticalPanel vpMO = new VerticalPanel();
        vpMO.add(instPanel);
        vpMO.add(datePanel);        

        targetField = FormBuilder.createField(TARGET_NAME_KEY);
        naifIDField = FormBuilder.createField(NAIFID_KEY);
        targetField.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                if (!StringUtils.isEmpty(targetField.getValue())) {
                    resolveTargetName(targetField.getValue(), null);
                } else {
                    lastResolved = null;
                }
            }
        });
        naifIDField.addValueChangeHandler(new ValueChangeHandler<String>(){
            public void onValueChange(ValueChangeEvent<String> ev) {
                if (!ev.getValue().equals(lastNaifIDResolved)) {
                    // empty target name if user enters naif id
                    //targetField.setValue("");            XW ???
                }
            }
        });



        final CollapsiblePanel moreOptions = new CollapsiblePanel("More options", vpMO, false);

        instPanel.getEventManager().addListener(InstrumentPanel.ON_HIDE, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    moreOptions.collapse();
                }
            });

        byNaifID = FormBuilder.createPanel(125, targetField, naifIDField);

        HTML desc = GwtUtil.makeFaddedHelp("Enter moving target name (e.g. Gaspra); alternatively, enter "+
            "NAIF ID as <br>one or more comma delimited integers e.g. 2000021,2003226."
         );
        Label spacer1 = new Label("");
        spacer1.setHeight("5px");
        Label spacer2 = new Label("");
        spacer2.setHeight("5px");
        VerticalPanel vp = new VerticalPanel();
        vp.add(byNaifID);
        vp.add(spacer1);
        vp.add(desc);
        vp.add(spacer2);
        MoreOptionsPanel options = new MoreOptionsPanel(new DataType[]{}); // no level3 options
        vp.add(options);
        //Label spacer3 = new Label("");
        //spacer3.setHeight("5px");
        //vp.add(spacer3);

        vp.add(moreOptions);
        //vp.setWidth("350px");

        Form form = new Form();
        form.setHelpId("searching.byNaifId");
        form.add(vp);
        return form;
    }

    private void resolveTargetName(final String targetName, final AsyncCallback<ArrayList<String>> callback) {

        System.out.println("    Do moving target resolution ("+targetName+")");
        if (targetName == null || targetName.equals(lastNaifIDResolved)) {
            // doing nothing 
        }
        else {
        lastResolved = targetName;
        ServerTask<ArrayList<EphPair>> task = new ServerTask<ArrayList<EphPair>>(byNaifID,"Resolving...", true) {
            public void doTask(AsyncCallback<ArrayList<EphPair>> acb) {
                naifIDField.reset();
                TargetServices.App.getInstance().resolveNameToNaifIDs(targetName, acb);
            }

            public void onSuccess(ArrayList<EphPair> ephPairList) {

                ArrayList<String> idList= null;
                if(ephPairList!=null) {
                    idList= new ArrayList<String>(ephPairList.size());
                    for(EphPair ep : ephPairList) idList.add(ep.getNaifID());
                }

                if (idList == null || idList.size() == 0) {
                    targetField.forceInvalid("Unable to resolve this target");
                } else {
                    try {
                        String naifIDVal = "";
                        for (int i=0; i<idList.size(); i++) {
                            if (i!=0) naifIDVal +=",";
                            naifIDVal += idList.get(i);
                        }
                        naifIDField.setValue(naifIDVal);
                        lastNaifIDResolved = naifIDVal;
                        //lastResolved = targetName;

                        if (callback != null) {
                            callback.onSuccess(idList);
                        }
                    } catch (Exception e) {
                        targetField.forceInvalid(e.getMessage());
                    }
                }
            }

            public void onFailure(Throwable caught) {
                String msg = caught.getCause() == null ? caught.getMessage() : caught.getCause().getMessage();
                targetField.forceInvalid("Resolve Failed. "+msg);
                //PopupUtil.showInfo(targetField, "Resolve Failed", msg); 
                if (callback != null) {
                    callback.onFailure(caught);
                }
            }
        };
        task.start();
        }

    }

    @Override
    protected String getInitFocusField() {
        return TARGET_NAME_KEY;
    }

    @Override
    protected void createAndProcessRequest() {
        //make sure request is processed AFTER target name is resolved
        boolean nameResolved = true;
        String targetName = targetField.getValue();
        String naifID =  naifIDField.getValue();

        if (targetName != null && targetName.trim().length()>0) {
            if (lastResolved == null || !targetName.equals(lastResolved) || naifID.trim().length() == 0) {
                nameResolved = false;
                final SearchByNaifIDCmd cmd = this;
                resolveTargetName(targetName, new AsyncCallback<ArrayList<String>>() {
                    public void onFailure(Throwable caught) {}
                    public void onSuccess(ArrayList<String> result) {
                        cmd.doCreateAndProcessRequest();
                    }
                });
            }
        }
        if (validate().isValid() && nameResolved) {
                super.createAndProcessRequest();
            }       
    }

    public void doCreateAndProcessRequest() {
        super.createAndProcessRequest();
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        createTablePreviewDisplay();

        if (MoreOptionsPanel.isAorRequested(req)) addTable(new SearchByNaifID(SearchByNaifID.Type.AOR, req));
        if (MoreOptionsPanel.isPbcdRequested(req)) addTable(new SearchByNaifID(SearchByNaifID.Type.PBCD, req));
        if (MoreOptionsPanel.isBcdRequested(req)) addTable(new SearchByNaifID(SearchByNaifID.Type.BCD, req));
        loadAll();

        setResults(getResultsPanel());

    }


}
