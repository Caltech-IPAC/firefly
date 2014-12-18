package edu.caltech.ipac.firefly.ui.searchui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.core.SearchAdmin;

import java.util.ArrayList;
import java.util.List;

public class FuseSearchPanel extends Composite {
    private static final String SEARCH_PROCESSOR_ID= "SearchProcessorID";
    private static final String SEARCH_UI_KEY= "SearchUIKey";
    private EventHandler handler;
    private DockLayoutPanel mainPanel= new DockLayoutPanel(Style.Unit.PX);

    private List<SearchUI> searchUIList;
    private List<Label> sideLinkList= new ArrayList<Label>(4);
    private DeckLayoutPanel allSearchUIPanel= new DeckLayoutPanel();
    private int activeSearchUI= 0;
    private ActiveSearchMonitorUI searchMon= new ActiveSearchMonitorUI(this);

    public FuseSearchPanel(List<SearchUI>  searchUIList) {
        mainPanel.setSize("97%", "97%");
        this.searchUIList= searchUIList;
        GwtUtil.setStyles(mainPanel, "minWidth", "600px", "minHeight", "500px");
        GwtUtil.setStyles(mainPanel, "minHeight", "600px", "maxHeight", "800px");
        initWidget(mainPanel);
        initUI();

    }


    public void start() {
        searchMon.clear();
    }


    private void initUI() {



        Widget searchMon= layoutSearchMon();
        DockLayoutPanel bottomWrapper= new DockLayoutPanel(Style.Unit.PX);
        bottomWrapper.addStyleName("bottomWrapper");
        mainPanel.addSouth(bottomWrapper, 100);


        if (searchUIList.size()>1) {
            Widget sideBar= createSidebar();
            mainPanel.addWest(sideBar, 150);
        }

        for(SearchUI sUI : searchUIList) {
            allSearchUIPanel.add(sUI.makeUI());
        }




        Widget aSec= createActivateSection();
        SimplePanel aSecWrapper= new SimplePanel(aSec);

        GwtUtil.setStyles(aSec, "position", "absolute",
                          "right", "10px",
                          "bottom", "40px",
                          "width", "auto");

        bottomWrapper.addEast(aSecWrapper, 350);
        bottomWrapper.add(searchMon);

        GwtUtil.setStyles(searchMon, "paddingLeft", "15px");

        setActiveSearchUIPanel(0);
        allSearchUIPanel.addStyleName("allSearchUIPanel");
        mainPanel.add(allSearchUIPanel);
    }


    private Widget layoutSearchMon() {
        GwtUtil.setStyles(searchMon.getWidget(), "border", "1px solid rgba(0,0,0,.1)", "lineHeight", "75px");
        return GwtUtil.wrap(searchMon.getWidget(), 10,80,10,10);
    }


    private static void setToInline(Widget w) { GwtUtil.setStyle(w,"display", "inline-block"); }


    private Widget createSidebar() {
        FlowPanel sideBarPanel= new FlowPanel();
        int i= 0;
        boolean first= true;
        for(SearchUI sUI : searchUIList) {
            final int panelIdx= i;
            Label link= GwtUtil.makeLinkButton(sUI.getPanelTitle(), sUI.getDesc(), new ClickHandler() {
                public void onClick(ClickEvent event) {  setActiveSearchUIPanel(panelIdx); }
            });
            sideLinkList.add(link);
            sideBarPanel.add(link);

            GwtUtil.setStyles(link,"paddingTop", first?"40px":"15px",
                                   "textAlign", "left");
            first= false;
            i++;
        }
        GwtUtil.setStyles(sideBarPanel,"borderRight", "1px solid rgba(0,0,0,.40)");
        Widget wrapper= GwtUtil.wrap(sideBarPanel,45,1,50,8);
        wrapper.addStyleName("SideBarWrapper");
        return wrapper;
    }


    private void setActiveSearchUIPanel(String key) {
        for(int i= 0; (i<searchUIList.size());i++) {
            if (searchUIList.get(i).getKey().equals(key)) {
                setActiveSearchUIPanel(i);
                break;
            }
        }
    }


    private void setActiveSearchUIPanel(int idx) {
        activeSearchUI= idx;
        allSearchUIPanel.showWidget(idx);
        for(int i= 0; (i<sideLinkList.size()); i++) {
            if (i==idx) sideLinkList.get(i).addStyleName("active-search-panel-link");
            else        sideLinkList.get(i).removeStyleName("active-search-panel-link");
        }
    }

    private SearchUI getActiveSearchUI() { return searchUIList.get(activeSearchUI); }


    private Widget createActivateSection() {
        FlowPanel activatePanel= new FlowPanel();
        setToInline(activatePanel);
        GwtUtil.setStyles(activatePanel, "float", "right", "paddingRight", "15px");
        activatePanel.addStyleName("right-floating");



        ButtonBase addToSearchList= makeButton("Add Search & Stay Here");
        addToSearchList.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) { search(true); }
        });

        ButtonBase search= makeButton("Search & View Results");
        search.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) { search(false); }
        });

        activatePanel.add(addToSearchList);
        activatePanel.add(search);
        GwtUtil.setStyle(search, "marginLeft", "10px");
        setToInline(search);
        setToInline(addToSearchList);

        return activatePanel;
    }


    private void search(final boolean isAndContinue) {
        FormHub.Validated v= validate();
        if (v.isValid()) {
            makeServerRequest(new RequestAsync() {
                public void onSuccess(ServerRequest r) {
                    if (doSearchNow(r,isAndContinue)) {
                        if (isAndContinue) handler.onSearchAndContinue();
                        else               handler.onSearch();
                    }
                }
            });
        }
        else {
            if  (v.getMessage() != null) {
                PopupUtil.showError("Validation Error", v.getMessage());
            } else {
                GwtUtil.showValidationError();
            }
        }
    }

    private boolean doSearchNow(ServerRequest req, boolean isAndContinue) {
        boolean success= false;
        req.setParam(Request.BOOKMARKABLE, true+"");
        req.setParam(Request.SEARCH_RESULT, true+"");
        req.setParam(Request.DRILLDOWN,true+"");
        req.setParam(Request.DO_SEARCH,true+"");
        req.setParam(SEARCH_PROCESSOR_ID, req.getRequestId());
        req.setParam(SEARCH_UI_KEY, getActiveSearchUI().getKey());
        if (req.getParam(Request.SHORT_DESC)==null)req.setParam(Request.SHORT_DESC, "temporary desc");

        TableServerRequest tbReq= new TableServerRequest(req.getRequestId(),req);
        if (searchMon.isADuplicate(tbReq)) {
            if (isAndContinue) PopupUtil.showWarning("Already in list", "This search is already in the search list", null);
            else success= true;
        }
        else {
            SearchAdmin.getInstance().submitSearch(tbReq, getSearchTitle());
            success= true;
        }
        return success;
    }


    public String getSearchTitle() {
        return getActiveSearchUI().getSearchTitle();
    }

    private void makeServerRequest(RequestAsync async) {
        getActiveSearchUI().makeServerRequest(async);
    }





    public FormHub.Validated validate() {
        return new FormHub.Validated(getActiveSearchUI().validate());
    }

    //todo - should remove?
    public void clear() {

    }

    public void populateFields(final ServerRequest clientRequest) {
        if (clientRequest==null) return;
        ServerRequest r= new ServerRequest(clientRequest.getParam(SEARCH_PROCESSOR_ID), clientRequest);
        if (clientRequest.containsParam(SEARCH_UI_KEY)) {
            setActiveSearchUIPanel(clientRequest.getParam(SEARCH_UI_KEY));
            getActiveSearchUI().setServerRequest(r);
        }
    }

    public void populateClientRequest(final ServerRequest clientRequest, final AsyncCallback<String> cb) {
        makeServerRequest(new RequestAsync() {
            @Override
            public void onSuccess(ServerRequest r) {
//                r.removeParam(ServerRequest.ID_KEY);
                clientRequest.setParams(r.getParams());
                clientRequest.setParam(SEARCH_PROCESSOR_ID, r.getRequestId());
                clientRequest.setParam(SEARCH_UI_KEY, getActiveSearchUI().getKey());
                cb.onSuccess("ok");
            }
        });
    }

    public void setHandler(EventHandler handler) {
        this.handler = handler;
    }


    public interface EventHandler {
        void onSearch();
        void onSearchAndContinue();
        void onClose();
    }


    protected ButtonBase makeButton(String desc) {
        PushButton button= new PushButton(desc);
        button.addStyleName("fuse-push-font-size");
        return button;
    }

    private static abstract class RequestAsync implements AsyncCallback<ServerRequest>  {
        public void onFailure(Throwable caught) {
            PopupUtil.showError("Search failed", "failed to create search request", null);
        }

        public abstract void onSuccess(ServerRequest result);
    }
}
