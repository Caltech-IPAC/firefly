package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.SearchInfo;
import edu.caltech.ipac.firefly.data.Status;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.StatefulWidget;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Base implementation of RequestHandler.  This implementation depends on the Request object.
 * The parameter portion of the url will be parsed into a Request object using the Request.parse(String)
 * method.
 *
 *  <code>
 *      gwt_app_url#string_serialized_request
 *
 *      string_serialized_request is the string representation of the Request object.  Request.parse(String) will instantiate it.
 * </code>
 */public class DefaultRequestHandler implements RequestHandler {
    private LinkedHashMap<String, StateWidgetEntry> statefulComponents = new LinkedHashMap<String, StateWidgetEntry>();
    protected Request currentRequest;
    protected Request currentSearchRequest;
    private boolean doRecordHistory = true;

    public void setDoRecordHistory(boolean doRecordHistory) {
        this.doRecordHistory = doRecordHistory;
    }

    public void onValueChange(ValueChangeEvent<String> str) {

        String token = str.getValue();
//        if (prevHistToken != null && prevHistToken.equals(token) &&
//                    Application.getInstance().getToolBar() != null && Application.getInstance().getToolBar().isOpen()) {
//            Application.getInstance().getToolBar().close();
//            if (currentSearchRequest != null) {
//                History.newItem(currentSearchRequest.toString(), false);
//            }
//        } else {
        processToken(token);
    }
    
    void processToken(String token) {
        if (token != null) {
            //!!!!!!
            //!! Note the second parameter is true, this disables decoding a plus sign.
            //!! We do not want to decode a plus sign as a space,  The problem is that if you run it through decode
            //!! twice it will gives different results. Therefore support a plus as a space give inconsistent results.
            //!! Combine this with the fact that Firefox does a automatic decode when you get a history token then
            //!! we cannot be sure what sort of string we have. The solution for this is not to support a plus sign
            //!! as a space.
            String decodeStr= URL.decodeComponent(token,false);
            //!! Do not change the above line with out talking to Trey or Loi or both.
            //!!!!!!!
            Request req = parse(decodeStr);
            if (req == null) {
                Application.getInstance().goHome();
            } else {
                if (currentSearchRequest != null && currentSearchRequest.equals(req)) {
                    Application.getInstance().getToolBar().close();
                    currentRequest = req;
                    onRequestSuccess(req, false);
                } else if (currentRequest == null || !currentRequest.equals(req)) {
                    processRequest(req, false);
                }
            }
        }
    }

    public void processRequest(Request req) {
        processRequest(req, true);
    }

    public Request parse(String str) {

        if (str != null && str.length() > 0) {
            Request req = Request.parse(str);
            if (req.getCmdName() != null) {
                return req;
            }
        }

        return null;
    }

    public SearchDescResolver getSearchDescResolver() {
        Application app = Application.getInstance();
        return app.getWidgetFactory().createSearchDescResolver(app.getAppDesc());
    }


//====================================================================
//  Page state / tagging related
//====================================================================
    class StateWidgetEntry {
        Context context;
        StatefulWidget swidget;

        StateWidgetEntry(Context context, StatefulWidget swidget) {
            this.context = context;
            this.swidget = swidget;
        }
    }

    public void registerComponent(String name, Context context, StatefulWidget b) {
        statefulComponents.put(name, new StateWidgetEntry(context, b));
    }

    public void registerComponent(String name, StatefulWidget b) {
        registerComponent(name, Context.PAGE, b);
    }

    public String getStateInfo(Context context) {
        boolean isSearchContext = context == Context.INCL_SEARCH;
        Request req = isSearchContext && currentSearchRequest != null ? currentSearchRequest : new Request("");

        for(StateWidgetEntry b : statefulComponents.values()) {
            if (b.context == Context.PAGE || (isSearchContext && (b.context == Context.INCL_SEARCH))) {
                if (b.swidget.isActive()) {
                    try {
                        b.swidget.recordCurrentState(req);
                    } catch(Exception e) {
                        GWT.log("error retrieving state info", e);
                    }
                }
            }
        }
        return req.toString();
    }

    public Request getCurrentRequest() {
        return currentRequest;
    }

    public Request getCurrentSearchRequest() {
        return currentSearchRequest;
    }

    public void moveToRequestState(Request req) {
        boolean isSearchContext = currentSearchRequest != null;
        for(StateWidgetEntry b : statefulComponents.values()) {
            if (isSearchContext || b.context == Context.PAGE) {
                if (b.swidget.isActive()) {
                    AsyncCallback acb = new AsyncCallback() {
                                public void onFailure(Throwable caught) {}
                                public void onSuccess(Object result) {}
                            };
                    b.swidget.moveToRequestState(req, acb);
                }
            }
        }
    }

//====================================================================
//
//====================================================================

    protected void processRequest(final Request req, final boolean createHistory) {
        if (req == null) return;

        currentRequest = req;
        if (req.isSearchResult()) {
            currentSearchRequest = req;
            WebEventManager.getAppEvManager().fireEvent(new WebEvent<Request>(this, Name.SEARCH_RESULT_START, req));
        }

        final GeneralCommand cmd = Application.getInstance().getCommand(req.getCmdName());
        final Request cReq = (Request) req.cloneRequest();
        if (cmd == null) {
            handleCmdNotFound(cReq, createHistory);
        } else {
            if (cmd instanceof RequestCmd) {
                processRequestCmd((RequestCmd) cmd, cReq, createHistory);
            } else {
                cmd.execute();
            }
        }
    }

    protected void processRequestCmd(final RequestCmd cmd, final Request req, final boolean createHistory) {

        BaseCallback<String> callback = new BaseCallback<String>() {
            public void doFinally() {
                onRequestSuccess(req, createHistory);
                if (req.isSearchResult()) {
                    moveToRequestState(req);
                }
            }
            public void doSuccess(String result) {
                req.setStatus(new Status(0, result));
                layout(cmd);   // and again here... if not.. coming from external page won't display result.
            }
        };
        cmd.execute(req, callback);
//        layout(cmd);  // called here..
        WebEventManager.getAppEvManager().fireEvent(new WebEvent<String>(this, Name.REQUEST_COMMAND_LAYOUT, cmd.getName()));
    }

    protected void handleCmdNotFound(Request req, final boolean createHistory) {
        // instead of throwing exception, just go home
        Application.getInstance().goHome();
        //throw new NullPointerException("Unable to resolve command name:" + req.getCmdName());
    }

    protected void onRequestSuccess(Request req, boolean createHistory) {
        if (req.isBookmarkable()) {
            Window.setTitle(getWindowTitle(req.getShortDesc()));
            Window.setStatus("");
            if(createHistory) {
                History.newItem(req.toString(), false);
            }
        }
        Application app = Application.getInstance();
        if (req.isSearchResult()) {
            String desc = getSearchDescResolver().getTitle(req) + ": " + getSearchDescResolver().getDesc(req);
            if (createHistory && doRecordHistory) {
                UserServices.App.getInstance().addSearchHistory(req.toString(), desc, false,
                        new BaseCallback<SearchInfo>(){
                            public void doSuccess(SearchInfo result) {
                            }
                        });
            }
            WebEventManager.getAppEvManager().fireEvent(new WebEvent<Request>(this, Name.SEARCH_RESULT_END, req));
        }

        if (req.isDrilldownRoot()) {
            if (app.getDrillDownItems().size() > 0) {
                app.getDrillDownItems().clear();
            }
            app.getDrillDownItems().addLast(req);
        } else if (req.isDrilldown()) {
            List<Request> l = app.getDrillDownItems().getList();
            if (l.contains(req)) {
                int idx = l.indexOf(req);
                app.getDrillDownItems().removeRange(idx+1, l.size());
            } else {
                app.getDrillDownItems().addLast(req);
            }
        } else {
            // for now, leave as-is
            //app.getDrillDownItems().clear();
        }
    }

    private String getWindowTitle(String reqDesc) {

        String windowTitle = "";

        String appName = Application.getInstance().getAppDesc();
        if (!StringUtils.isEmpty(appName)) {
            windowTitle = appName;
        }

        if (!StringUtils.isEmpty(reqDesc)) {
            if (!StringUtils.isEmpty(windowTitle)) {
                windowTitle += " - ";
            }
            windowTitle += reqDesc;
        }
        return windowTitle;
    }

    protected void layout(RequestCmd cmd) {
        LayoutManager lm = Application.getInstance().getLayoutManager();
        for (Region r : lm.getRegions()) {
            if ( cmd.isRegistered(r.getId()) ) {
                r.setDisplay(cmd.getView(r.getId()));
            }
        }
    }
}