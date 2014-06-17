package edu.caltech.ipac.fuse.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.fuse.core.SearchAdmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Date: 9/12/13
 *
 * @author loi
 * @version $Id: $
 */
public class FuseSearchPanel extends Composite {
    private static final String SEARCH_PROCESSOR_ID= "SearchProcessorID";
    private static final String SEARCH_UI_KEY= "SearchUIKey";
    private SearchAdmin searchAdmin;
    private EventHandler handler;
    private DockLayoutPanel mainPanel= new DockLayoutPanel(Style.Unit.PX);

    private List<SearchUI> searchUIList= Arrays.asList( new AnyDataSetSearchUI(),
                                                        new DummyInventoryUI(),
                                                        new PopularQuickSearchUI()
                                                    );

    private List<Label> sideLinkList= new ArrayList<Label>(4);
    private DeckLayoutPanel allSearchUIPanel= new DeckLayoutPanel();
    private int activeSearchUI= 0;

    public FuseSearchPanel() {
        this.searchAdmin = new SearchAdmin();
//        HTML msg = new HTML("<font size=+3> Search panel is under construction</font>");
//        msg.setSize("600px", "400px");
//        mainPanel.setSize("600px", "400px");
        mainPanel.setSize("97%", "97%");
        GwtUtil.setStyles(mainPanel, "minWidth", "600px", "minHeight", "500px");
        GwtUtil.setStyles(mainPanel, "minHeight", "600px", "maxHeight", "800px");
        initWidget(mainPanel);
        initUI();

    }




    private void initUI() {



        Widget searchMon= makeSearchMon();
        DockLayoutPanel bottomWrapper= new DockLayoutPanel(Style.Unit.PX);
        bottomWrapper.addStyleName("bottomWrapper");
        mainPanel.addSouth(bottomWrapper, 100);


        if (searchUIList.size()>1) {
            Widget sideBar= createSidebar();
            mainPanel.addWest(sideBar, 100);
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

        bottomWrapper.addEast(aSecWrapper, 200);
        bottomWrapper.add(searchMon);
//        searchMon.setSize("200px", "130px");

        GwtUtil.setStyles(searchMon, "paddingLeft", "15px");
//        searchMon.addStyleName("left-floating");





        setActiveSearchUIPanel(0);
        allSearchUIPanel.addStyleName("allSearchUIPanel");
        mainPanel.add(allSearchUIPanel);
    }


    private Widget makeSearchMon() {
        HTML searchMon= new HTML("Search List Monitor Goes Here");
        GwtUtil.setStyles(searchMon, "border", "1px solid rgba(0,0,0,.1)", "lineHeight", "75px");
        return GwtUtil.wrap(searchMon, 10,80,10,10);
    }


    private static void setToInline(Widget w) { GwtUtil.setStyle(w,"display", "inline-block"); }


    private Widget createSidebar() {
        FlowPanel sideBarPanel= new FlowPanel();
        int i= 0;
        boolean first= true;
        for(SearchUI sUI : searchUIList) {
            final int panelIdx= i;
            Label link= GwtUtil.makeLinkButton(sUI.getTitle(), sUI.getDesc(), new ClickHandler() {
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

        ButtonBase search= makeButton("Search");
        search.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) { search(); }
        });


        ButtonBase addToSearchList= makeButton("Add to Search List");
        addToSearchList.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) { searchAndContinue(); }
        });

        activatePanel.add(search);
        activatePanel.add(addToSearchList);
        GwtUtil.setStyle(addToSearchList, "marginLeft", "5px");
        setToInline(search);
        setToInline(addToSearchList);

        return activatePanel;
    }


    private void search() {
        if (validate().isValid()) {
            makeServerRequest(new RequestAsync() {
                public void onSuccess(ServerRequest r) {
                    if (!isADuplicate(r)) {
                        sendRequest(r);
                        handler.onSearch();
                        //todo close panel and goto result viewer
                    }
                }
            });
        }
    }

    private void searchAndContinue() {
        makeServerRequest(new RequestAsync() {
            public void onSuccess(ServerRequest r) {
                if (isADuplicate(r)) {
                    PopupUtil.showWarning("Already in list", "This search is already in the search list", null);
                } else {
                    sendRequest(r);
                    handler.onSearchAndContinue();
                }
            }
        });
    }

    private void makeServerRequest(RequestAsync async) {
        getActiveSearchUI().makeServerRequest(async);
    }

    private void sendRequest(ServerRequest r) {
        //todo send the request to the background monitor
    }




    private static abstract class RequestAsync implements AsyncCallback<ServerRequest>  {
        public void onFailure(Throwable caught) {
            PopupUtil.showError("Search failed", "failed to create search request", null);
        }

        public abstract void onSuccess(ServerRequest result);
    }




    private boolean isADuplicate(ServerRequest r) {
        return false; // todo check the background monitor to see if this request is already running

    }



    //todo - should be made somwhere else?
    public SearchAdmin getSearchAdmin() {
        return searchAdmin;
    }

    public FormHub.Validated validate() {
        return new FormHub.Validated(getActiveSearchUI().validate());
    }

    //todo - should remove?
    public void clear() {

    }

    public void populateFields(final Request clientRequest) {
        ServerRequest r= new ServerRequest(clientRequest.getParam(SEARCH_PROCESSOR_ID), clientRequest);
        if (clientRequest.containsParam(SEARCH_UI_KEY)) {
            setActiveSearchUIPanel(clientRequest.getParam(SEARCH_UI_KEY));
            getActiveSearchUI().setServerRequest(r);
        }
    }

    public void populateClientRequest(final Request clientRequest, final AsyncCallback<String> cb) {
        makeServerRequest(new RequestAsync() {
            @Override
            public void onSuccess(ServerRequest r) {
                r.removeParam(ServerRequest.ID_KEY);
                clientRequest.setParams(r.getParams());
                clientRequest.setParam(SEARCH_PROCESSOR_ID,r.getRequestId());
                cb.onSuccess("ok");
            }
        });
    }

    //todo - why is this here?
    public void setHandler(EventHandler handler) {
        this.handler = handler;
    }


    public interface EventHandler {
        void onSearch();
        void onSearchAndContinue();
        void onClose();
    }


    protected ButtonBase makeButton(String desc) {
        return new PushButton(desc);
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
