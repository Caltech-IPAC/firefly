package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.background.ActivationFactory;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.Backgroundable;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.DefaultWorkingWidget;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.panels.SearchPanel;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: Jul 27, 2009
 *
 * @author loi
 * @version $Id: CommonRequestCmd.java,v 1.44 2012/10/03 22:18:11 loi Exp $
 */
public abstract class CommonRequestCmd extends RequestCmd implements TableLoadHandler {

    private Form form;
    private AsyncCallback<String> callback;
    private SimplePanel resultWrapper;
    private Masker bgMasker;
    private Masker regMasker;
    private PrimaryTableUILoader tableUiLoader;
    private boolean isBackgrounded = false;
    private boolean isBackgroundable = false;
    private MonitorItem bgMonitorItem;
    private DefaultWorkingWidget.ButtonInfo bgButtonInfo;
    private Request currentRequest;
    private SearchPanel searchPanel = SearchPanel.getInstance();

    public CommonRequestCmd(String command) {
        super(command);
    }

    public CommonRequestCmd(String command, String title) {
        this(command, title, title, true);
    }

    public CommonRequestCmd(String command, String title, String desc, boolean enabled) {
        super(command, title, desc, enabled);
    }

    public void setResults(Widget w) {
        if (resultWrapper == null) {
            resultWrapper = new SimplePanel();
            resultWrapper.setSize("100%", "100%");
        }
        registerView(LayoutManager.RESULT_REGION, resultWrapper);
        DOM.setElementAttribute(resultWrapper.getElement(), "align", "left");
        resultWrapper.setWidget(w);
    }

    protected PrimaryTableUILoader getTableUiLoader() {
        if (tableUiLoader == null) {
            tableUiLoader = new PrimaryTableUILoader(this);
        }
        return tableUiLoader;
    }

    public boolean init() {

        SimplePanel main = new SimplePanel();
        form = createForm();
        if (form != null) {
            if (form.getFieldCount() > 0) {
                addActionButtons();
            }
            main.setWidget(form);
            searchPanel.setFormArea(main);
        }
        return true;
    }

    public Form getForm() {
        if (form == null) {
            form = createForm();
        }
        return form;
    }

    protected void addActionButtons() {
        form.addSubmitButton(GwtUtil.makeFormButton("<b>Search</b>",
                new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        createAndProcessRequest();
                    }
                }));

        form.addButton(GwtUtil.makeFormButton("Clear",
                new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        form.reset();
                        focus();
                    }
                }));
    }

    public Request makeRequest() {
        return makeRequest(getName(), this.getLabel());
    }

    public static Request makeRequest(String name, String desc) {
        Request req = new Request(name, true, false);
        req.setIsSearchResult(true);
        req.setIsDrilldownRoot(true);
        req.setDoSearch(true);
        if (desc != null) {
            req.setShortDesc(desc);
        }
        return req;
    }

    protected FormHub.Validated validate() {
        return form == null ? new FormHub.Validated() : form.validated();
    }

    protected void createAndProcessRequest() {
        FormHub.Validated validated = validate();
        if (validated.isValid()) {
            final Request req = makeRequest();
            if (form != null) {
                form.populateRequest(req, new AsyncCallback<String>(){
                    public void onFailure(Throwable caught) {
                    }
                    public void onSuccess(String result) {
                        onFormSubmit(req);
                        Application.getInstance().processRequest(req);
                    }
                });
            }
        } else {
            if (StringUtils.isEmpty(validated.getMessage())) {
                GwtUtil.showValidationError();
            } else {
                PopupUtil.showError("Validation Error", validated.getMessage());
            }
        }

    }

    protected void onFormSubmit(Request req) {}

    protected void clearState() {
        tableUiLoader = null;
        isBackgrounded = false;
        isBackgroundable = false;
        bgMonitorItem = null;
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {
        clearState();
        currentRequest = req;
        if (req == null) return;

        // fill the form's field based on the request parameters.
        if (form != null) {
            form.populateFields(req);
        }

        this.callback = callback;
        clearView();
        if (req.isDoSearch()) {
            // process the search request
            doProcessRequest(req, callback);
        } else {
            focus();
            searchPanel.setFormArea(form);
            registerView(LayoutManager.DROPDOWN_REGION, searchPanel);
            Application.getInstance().getLayoutManager().getRegion(LayoutManager.DROPDOWN_REGION).expand();
            callback.onSuccess("");
        }
    }

    protected String getInitFocusField() {
        if (form != null && form.getFieldCount() > 0) {
            return form.getFieldIds().get(0);
        }
        return null;
    }

    protected void doProcessRequest(Request req, AsyncCallback<String> callback) {
        clearState();
        isBackgroundable = req.isBackgroundable();

        SearchDescResolver resolver = Application.getInstance().getRequestHandler().getSearchDescResolver();
        Label title = new Label(resolver.getTitle(req));
        GwtUtil.setStyle(title, "fontSize", "12px");
        registerView(LayoutManager.SEARCH_TITLE_REGION, title);
        registerView(LayoutManager.SEARCH_DESC_REGION, new Label(resolver.getDesc(req)));

        try {
            processRequest(req, callback);
        } catch(Exception e) {
          GWT.log("unexpected error while processing request", e);  
        }
    }

    public void unmask() {
        if (isBackgroundable) {
            if (bgMasker != null) {
                bgMasker.hide();
            }
        } else {
            if (regMasker != null) {
                regMasker.hide();
            }
        }
    }

    private Masker createMaskPane(boolean isBackgroundable) {
        DefaultWorkingWidget.ButtonInfo cancel = new DefaultWorkingWidget.CancelButton(new ClickHandler(){
            public void onClick(ClickEvent event) {
                getTableUiLoader().cancelAll();
                onComplete(0);
            }
        });

        DefaultWorkingWidget mask;
        if (isBackgroundable) {
            bgButtonInfo = new DefaultWorkingWidget.ButtonInfo(new ClickHandler(){
                public void onClick(ClickEvent event) {
                    sendToBackground();
                }
            }, "Background", "Click here to send this search to the background");
            mask = new DefaultWorkingWidget(bgButtonInfo, cancel);
        } else {
            mask = new DefaultWorkingWidget(cancel);
        }
        return new Masker(new MaskPane(form, mask), mask);
    }

    public boolean canBackground() {
        for (PrimaryTableUI t : getTableUiLoader().getTables()) {
            if (t.getDataModel().getLoader() instanceof Backgroundable &&
                    ((Backgroundable)t.getDataModel().getLoader()).canBackground()) {
                    return true;
            }
        }
        return false;
    }

    public void mask(String msg) {
        Masker masker;

        if (isBackgroundable && canBackground()) {
            bgMasker = bgMasker == null ? createMaskPane(true) : bgMasker;
            masker = bgMasker;
        } else {
            regMasker = regMasker == null ? createMaskPane(false) : regMasker;
            masker = regMasker;
        }

        masker.show();
        masker.setMsg(msg);
    }

    private void sendToBackground() {
        if (bgMonitorItem != null) {
            isBackgrounded = true;
            getTableUiLoader().sendToBackground();
            ScreenPt pt = bgButtonInfo == null ? new ScreenPt(0, 0) : GwtUtil.getCenterPos(bgButtonInfo.getButton());
            onComplete(0);
            Application.getInstance().getBackgroundManager().animateToManager(pt.getIX(), pt.getIY(), 500);
            bgMonitorItem.setWatchable(true);
        }
    }

    protected String getNotFoundMsg() {
        return "Your search returned no results";
    }

    protected String getSuccessMsg(int totalRows) {
        return "Search results:  " + totalRows + " rows returned.";
    }

    protected void focus() {
        if (getInitFocusField() != null) {
            form.setFocus(getInitFocusField());
        }
    }

//====================================================================
//
//====================================================================

    protected abstract Form createForm();
    protected abstract void processRequest(Request req, AsyncCallback<String> callback);

//====================================================================
//  implements TableLoadHandler
//====================================================================

    public Widget getMaskWidget() {
        return form;
    }

    private void setBgButtonEnable(boolean flg) {
        if (bgButtonInfo != null && bgButtonInfo.getButton() != null ) {
            bgButtonInfo.getButton().setEnabled(flg);
        }
    }
    public void onLoad() {
        PrimaryTableUI t = tableUiLoader.getTables().get(0);
        mask("Loading " + t.getTitle() + "...");
        if (isBackgroundable) {
            setBgButtonEnable(false);
            DeferredCommand.addCommand(new IncrementalCommand() {
                public boolean execute() {
                    String title = currentRequest == null || StringUtils.isEmpty(currentRequest.getShortDesc()) ?
                            "Backgrounded search" : currentRequest.getShortDesc();
                    bgMonitorItem = new MonitorItem(title, ActivationFactory.Type.QUERY, true);
                    bgMonitorItem.setWatchable(false);
                    List<BackgroundStatus> bgStatusList = new ArrayList<BackgroundStatus>();
                    for (PrimaryTableUI t : tableUiLoader.getTables()) {
                        setBgButtonEnable(false);
                        if (t.getDataModel().getLoader() instanceof Backgroundable) {
                            BackgroundStatus status= ((Backgroundable) t.getDataModel().getLoader()).getBgStatus();
                            if (status != null) {
                                bgStatusList.add(status);
                            }
                        }
                    }
                    if (bgStatusList.size() == tableUiLoader.getTables().size()) {
                        bgMonitorItem.initStatusList(bgStatusList);
                        Application.getInstance().getBackgroundMonitor().addItem(bgMonitorItem);
                        setBgButtonEnable(true);
                        return false;
                    }
                    return true;
                }
            });
        }
    }

    public void onError(PrimaryTableUI table, Throwable t) {
        tableUiLoader.cancelAll();
        PopupUtil.showSevereError(t);
        Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION).hide();
        unmask();
    }

    public void onLoaded(PrimaryTableUI table) {
        if (!isBackgrounded) {
            for (PrimaryTableUI t : getTableUiLoader().getTables()) {
                if (t.getDataModel().getCurrentData() == null) {
                    mask("Loading " + table.getTitle() + "...");
                    break;
                }
            }

            if (table != null && table.getDataModel().getTotalRows() > 0) {
                Application.getInstance().getLayoutManager().getRegion(LayoutManager.DROPDOWN_REGION).collapse();
            }
        }
    }

    public void onComplete(int totalRows) {
        unmask();
        if (totalRows < 1) {
            HTML msg = isBackgrounded ? new HTML("<b>Search sent to background.</b>"):
                       getTableUiLoader().isCancelled()? new HTML("<b>Search cancelled.</b>") :
                                        new HTML("<b>" + getNotFoundMsg() + "</b>");
            // resultsWrapper can be null
            if (resultWrapper == null) {
                resultWrapper = new SimplePanel();
                resultWrapper.setSize("100%", "100%");
                registerView(LayoutManager.RESULT_REGION, resultWrapper);
            }
            DOM.setElementAttribute(resultWrapper.getElement(), "align", "center");
            Region results = Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION);
            results.setDisplay(resultWrapper);
            resultWrapper.setWidget(msg);
//            Application.getInstance().getLayoutManager().getRegion(LayoutManager.FORM_REGION).expand();
        }
        if (!isBackgrounded) {
            if (callback != null) {
                callback.onSuccess(getSuccessMsg(totalRows));
            }
        }
//        clearState();
    }

//====================================================================
//
//====================================================================
    
    private static class Masker {
        MaskPane maskPane;
        DefaultWorkingWidget mask;

        public Masker(MaskPane maskPane, DefaultWorkingWidget mask) {
            this.maskPane = maskPane;
            this.mask = mask;
        }

        public void show() {
            maskPane.show();
        }

        public void hide() {
            maskPane.hide();
        }

        public void setMsg(String msg) {
            mask.setText(msg);
        }

        public MaskPane getMaskPane() {
            return maskPane;
        }

        public DefaultWorkingWidget getMask() {
            return mask;
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