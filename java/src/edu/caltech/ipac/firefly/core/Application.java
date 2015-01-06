package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.UIObject;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitor;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitorEvent;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitorPolling;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.DataList;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.rpc.ResourceServices;
import edu.caltech.ipac.firefly.task.DataSetInfoFactory;
import edu.caltech.ipac.firefly.task.IrsaAllDataSetsFactory;
import edu.caltech.ipac.firefly.ui.BundledServerTask;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.background.BackgroundManager;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * A singleton class acting as a facade to hide the detail implementation of this application.  This application
 * framework should be generic enough to be reuse by other projects.  Beside controlling and maintaining the state, it
 * also handle history, bookmark, command triggering, and menu generation.  It is the responsibility of the actual
 * implementing application to layout its components.
 */
public class Application {

    public static final String IGNORE_QUERY_STR = "gwt.codesvr";
    public static final String PRIOR_STATE = "app_prior_state";
    private static final int DEF_Z_INDEX= 0;

    public enum EventMode { SSE, POLL}

    private static NetworkMode networkMode= NetworkMode.RPC;
    private static EventMode eventMode= EventMode.POLL;
//    private static NetworkMode networkMode= NetworkMode.JSONP; // for debugging

    private static Application app;
    private static Creator creator;
    private static boolean enjectCSS= true;

    private int defZIndex=DEF_Z_INDEX; // this is only used for x-site stuff to work with others websites
    private Map<String, GeneralCommand> commandTable;               // map of commands keyed by command_name.
    private Toolbar toolBar;
    private RequestHandler requestHandler;
    private DataList<Request> drillDownItems= null;
    private LayoutManager layoutManager= null;
    private LoginManager loginManager;
    private HelpManager helpManager;
    private Frame nullFrame;
    private List<ServerTask> userStartupTasks;
    private WebAppProperties appProp = null;
    private Request homeRequest;
    private Version version= null;
    private final BackgroundMonitor backgroundMonitor;

    private HashMap<String, Object> appData = new HashMap<String, Object>();    // map of arbitrary data used by this application.
    private BackgroundManager backgroundMan = null;
    private WidgetFactory widgetFactory = null;
    private boolean doSaveState = true;
    private ApplicationReady appReady;
    private EventHub eventHub;
    private static DataSetInfoFactory dataSetInfoFactory= null;

    /**
     * singleton; use getInstance().
     */
    private Application() {
        if (enjectCSS) {
            CssData cssData = CssData.Creator.getInstance();
            FireflyCss css = cssData.getFireflyCss();
            css.ensureInjected();
        }
        configureUncaughtExceptionHandling();
        if (creator == null) {
            throw new ResourceNotFoundException("Provider is not set.");
        }

        if (eventMode==EventMode.SSE && creator.isApplication()) {
            SSEClient.start();
            backgroundMonitor = new BackgroundMonitorEvent();
        }
        else {
            backgroundMonitor = new BackgroundMonitorPolling();
        }
    }

    private void configureUncaughtExceptionHandling() {
        if (GWT.isProdMode()) {
            GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
                public void onUncaughtException(Throwable e) {
                    if (networkMode==NetworkMode.RPC) {
                        Throwable t= GwtUtil.unwrapUmbrellaException(e);
                        GwtUtil.logToServer(Level.SEVERE, "Uncaught Exception: ", t);
                    }
                    GwtUtil.getClientLogger().log(Level.SEVERE,"Uncaught Exception: ",e);
                }
            });
        }
    }

    public static void disableCSSEject() { enjectCSS= false;}

    public EventHub getEventHub() {
        if (eventHub == null) {
            eventHub = new EventHub();
        }
        return eventHub;
    }


    public static DataSetInfoFactory getDataSetFactory() {
        if (dataSetInfoFactory == null) {
            dataSetInfoFactory= IrsaAllDataSetsFactory.getInstance();
        }
        return dataSetInfoFactory;
    }

    public static void setDataSetFactory(DataSetInfoFactory  datasetFactory) {
        dataSetInfoFactory= datasetFactory;
    }



    public void setDoSaveState(boolean doSaveState) {
        this.doSaveState = doSaveState;
    }

    public void runOnStartup(ServerTask task) {
        if (userStartupTasks == null) {
            userStartupTasks = new ArrayList<ServerTask>();
        }
        userStartupTasks.add(task);
    }

    public Frame getNullFrame() {
        return nullFrame;
    }

    public Creator getCreator() {
        return creator;
    }

    public static void setCreator(Creator creator) {
        if (Application.creator == null) {
            Application.creator = creator;
        }
    }

    public static Application getInstance() {
        if (app == null) {
            app = new Application();
        }
        return app;
    }

    public void start(Request welcomeCmd, ApplicationReady appReady) {
        registerExternalJS();
        setHomeRequest(welcomeCmd);
        this.appReady = appReady;


        loginManager = creator.makeLoginManager();
        drillDownItems = creator.isApplication() ? new DataList<Request>() : null;
        // load system tasks

        BundledServerTask tasks = new BundledServerTask() {
            public void finish() {
                // all startup steps are completed... init and show.
                try {
                    initAndShow();
                } catch (Throwable ex) {
                    GWT.log("Unexpected Exception during loading", ex);
                    try {
                        goHome();
                    } catch (Throwable e) {
                        GWT.log("Unexpected Exception while attempting to goHome() after an error.", e);
                        RootPanel.get().add(new Label("Unrecoverable exception while loading this page.  " + e.getMessage()));
                        if (!GWT.isProdMode()) throw new IllegalArgumentException(e);
                        return;
                    }
                }
                if (SupportedBrowsers.isSupported()) {
                    WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.APP_ONLOAD));
                    initAlerts();
                }
            }
        };

        if (userStartupTasks!=null) tasks.addServerTask(new LoadUserTasks());
        ServerTask creatorTask[]= creator.getCreatorInitTask();
        if (creatorTask!=null) {
            for(ServerTask t : creatorTask) tasks.addServerTask(t);
        }

        // start the tasks one at a time..
        tasks.start();

        findVersion(new AsyncCallback<Version>() {  // do this for tracking on the server side
            public void onFailure(Throwable caught) { }
            public void onSuccess(Version result) { }
        });
    }

    private void initAlerts() {
        AlertManager am = creator.makeAlertManager();
//        if (am!=null) {
//            GwtUtil.setStyles(am, "zIndex", "1");
//            RootPanel.get().add(am, 0, 0);
//        }
    }


    public Version findVersion(AsyncCallback<Version> async) {
        if (version!=null) {
            async.onSuccess(version);
        }
        else {
            new VersionInfo(async).start();
        }

        return version;
    }

    public void setDefZIndex(int defZIndex) { this.defZIndex = defZIndex; }
    public int getDefZIndex() { return defZIndex; }

    private void initAndShow() {

        // initialize JossoUtil... supply context information
        JossoUtil.init(
                Application.getInstance().getProperties().getProperty("sso.server.url"),
                GWT.getModuleBaseURL(),
                Application.getInstance().getProperties().getProperty("sso.user.profile.url")
        );

        commandTable = creator.makeCommandTable();
        toolBar = creator.getToolBar();
        layoutManager = creator.makeLayoutManager();
        if (creator.isApplication()) {
            requestHandler = getRequestHandler();
            History.addValueChangeHandler(requestHandler);
        }


        nullFrame = new Frame();
        nullFrame.setSize("0px", "0px");
        nullFrame.setVisible(false);

        RootPanel root = RootPanel.get();
        root.clear();
        root.add(nullFrame);
        if (BrowserUtil.isTouchInput()) root.addStyleName("disable-select");

        if (getLayoutManager()!=null) getLayoutManager().layout(creator.getLoadingDiv());

        checkMobilAppInstall();

        if (SupportedBrowsers.isSupported()) {
            if (appReady != null) {
                appReady.ready();
            }

            if (creator.isApplication()) {
                // save the current state when you leave.
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        Window.addCloseHandler(new CloseHandler<Window>() {
                            public void onClose(CloseEvent<Window> windowCloseEvent) {
                                gotoUrl(null, false);
                            }
                        });
                    }
                });

                // coming back from prior session
                String ssoBackTo = Cookies.getCookie(PRIOR_STATE);
                final Request prevState = Request.parse(ssoBackTo);
                if (prevState != null && prevState.isSearchResult()) {
                    Cookies.removeCookie(PRIOR_STATE);
                    History.newItem(ssoBackTo, true);
                } else {
                    // url contains request params
                    String qs = Window.Location.getQueryString().replace("?", "");
                    if (!StringUtils.isEmpty(qs) && !qs.contains(IGNORE_QUERY_STR)) {
                        String qsDecoded = URL.decodeQueryString(qs);
                        String base = Window.Location.getHref();
                        base = base.substring(0, base.indexOf("?"));
                        String newUrl = base + "#" + URL.encodePathSegment(qsDecoded);
                        Window.Location.replace(newUrl);
                    } else {
                        String startToken = History.getToken();
                        if (StringUtils.isEmpty(startToken)) {
                            goHome();
                        } else {
                            requestHandler.processToken(startToken);
                        }
                    }
                }
                if (backgroundMonitor!=null) backgroundMonitor.syncWithCache(null);
            }
        } else {
            hideDefaultLoadingDiv();
            SupportedBrowsers.showUnsupportedMessage();
        }


    }

    private void checkMobilAppInstall() {
        if (!creator.isApplication() || !enjectCSS) return;
        // not used so I am disabling
//        Timer timer = new Timer() {
//            @Override
//            public void run() {
//                if (BrowserUtil.isPlatform(Platform.IPAD) ) {
//                    if (!isIosAppStandalong()) {
//                        Image addPic = new Image(IconCreator.Creator.getInstance().getIpadAddButtonPicture());
//                        HorizontalPanel hp = new HorizontalPanel();
//                        hp.setSpacing(7);
//                        hp.add(addPic);
//                        HTML msg = new HTML("You can install this web app on you device by <br>" +
//                                                    "clicking on the arrow above and choosing<br><br>" +
//                                                    "\"Add to Home Screen\"");
//                        hp.add(msg);
//                        GwtUtil.setStyle(msg, "paddingLeft", "5px");
//                        PopupUtil.showInfoPointer(5, 10, "Install", hp, 10);
//                    }
//                }
//            }
//        };
//        timer.schedule(5000);
    }


    public void gotoUrl(String url, boolean saveSearchPage) {
        if (doSaveState) {
            doSaveState = false;
            RequestHandler.Context context = saveSearchPage ? RequestHandler.Context.INCL_SEARCH : RequestHandler.Context.PAGE;
            String cState = null;
            try {
                cState = Application.getInstance().getRequestHandler().getStateInfo(context);
            } catch (Exception e) {
                GWT.log("error retrieving state info", e);
            }
            if (cState != null) {
                Cookies.setCookie(PRIOR_STATE, cState);
        }
        }
        if (url != null) {
            Window.Location.assign(url);
        }
    }

    public String getAppDesc() {
        return creator.getAppDesc();
    }

    public String getAppName() {
        return creator.getAppName();
    }

    public void goHome() {
        if (!creator.isApplication()) return;
        drillDownItems.clear();
        if (homeRequest!=null) processRequest(homeRequest);
    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    public LoginManager getLoginManager() {
        return loginManager;
    }

    public void setNetworkMode(NetworkMode mode) { networkMode= mode; }
    public static void setEventMode(EventMode mode) { eventMode = mode; }

    public NetworkMode getNetworkMode()  { return networkMode; }

    public HelpManager getHelpManager() {
        if (helpManager == null) {
            helpManager = new HelpManager();
        }
        return helpManager;
    }

    public Toolbar getToolBar() {
        return toolBar;
    }

    public RequestHandler getRequestHandler() {
        if (requestHandler == null) {
            requestHandler = creator.makeCommandHandler();
        }
        return requestHandler;
    }

    public WidgetFactory getWidgetFactory() {
        if (widgetFactory == null) {
            widgetFactory = new WidgetFactory();
        }
        return widgetFactory;
    }

    public DataList<Request> getDrillDownItems() {
        return drillDownItems;
    }

    public void setHomeRequest(Request homeRequest) {
        this.homeRequest = homeRequest;
        if (homeRequest!=null) this.homeRequest.setBookmarkable(false);
    }

    public Request getHomeRequest() {
        return homeRequest;
    }

    /**
     * This method will log the request according to the framework's requirement. This includes history tracking,
     * logging, etc.
     *
     * @param req the Request
     * @throws ResourceNotFoundException
     */
    public void processRequest(Request req) throws ResourceNotFoundException {
        getRequestHandler().processRequest(req);
    }

    public static void processRequest(String reqString) {
        Application app = getInstance();
        Request req = app.getRequestHandler().parse(reqString);
        if (req != null) {
            app.processRequest(req);
        }
    }

    public GeneralCommand getCommand(String cmd) {
        return commandTable!=null? commandTable.get(cmd) : null;
    }

    public Map<String, GeneralCommand> getCommandTable() {
        return commandTable!=null ? commandTable : Collections.<String, GeneralCommand>emptyMap();
    }


    public Object getAppData(String key) {
        return appData.get(key);
    }

    public void setAppData(String key, Object value) {
        appData.put(key, value);
    }

    public boolean isPropertyDBLoaded() {
        return appProp != null;
    }

    public WebAppProperties getProperties() {
        return appProp;
    }

    public void setProperties(WebAppProperties properties) { appProp= properties;  }

    public BackgroundMonitor getBackgroundMonitor() {
        return backgroundMonitor;
    }


    public BackgroundManager getBackgroundManager() {
        if (backgroundMan == null) backgroundMan = new BackgroundManager();
        return backgroundMan;
    }

    public boolean hasSearchResult() {
        Region results = Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION);
        Region popout = Application.getInstance().getLayoutManager().getRegion(LayoutManager.POPOUT_REGION);
        return (results != null && GwtUtil.isOnDisplay(results.getContent())) ||
               (popout != null && GwtUtil.isOnDisplay(popout.getContent()));
    }

    public void setStatus(String s) {
        if (getLayoutManager() != null) {
            Region status = getLayoutManager().getRegion(LayoutManager.STATUS);
            if (status != null && status != getNullFrame()) {
                if(StringUtils.isEmpty(s)) {
                    status.hide();
                } else {
                    status.show();
                    status.setDisplay(new Label(s));
                }
            }
        }
    }

    public void resize() {
        if (getLayoutManager() != null) {
            try {
                getLayoutManager().resize();
                WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.WINDOW_RESIZE,
                                                                         new ResizeEvent(Window.getClientWidth(), Window.getClientHeight()) {
                                                                         }));
            } catch (Exception e) {
                GWT.log("unexpected exception", e);
            }
        }
    }

    public interface ApplicationReady {
        public void ready();
    }

    public void hideDefaultLoadingDiv() {
        final Element loading = DOM.getElementById("loading");
        if (loading!=null) UIObject.setVisible(loading, false);
    }
//====================================================================
//  system tasks to perform during start up
//====================================================================

    /**
     * retrieves application version infomation
     */
    private class VersionInfo extends ServerTask<Version> {
        AsyncCallback<Version> vAsync;

        public VersionInfo(AsyncCallback<Version> vAsync) {  this.vAsync= vAsync;  }

        public void onSuccess(Version version) {
            Application.this.version= version;
            vAsync.onSuccess(version);
        }

        public void doTask(AsyncCallback<Version> passAlong) {
            ResourceServices.App.getInstance().getVersion(BrowserUtil.getUserAgent(), passAlong);
        }
    }

    /**
     * load user tasks if it exists
     */
    private class LoadUserTasks extends ServerTask {
        public void onSuccess(Object result) {
        }

        public void doTask(final AsyncCallback passAlong) {
            if (userStartupTasks != null) {
                BundledServerTask bst = new BundledServerTask() {
                    public void finish() {
                        passAlong.onSuccess(null);
                    }
                };
                for (ServerTask st : userStartupTasks) {
                    bst.addServerTask(st);
                }
                bst.startAll();
            } else {
                passAlong.onSuccess(null);
            }

        }
    }

//====================================================================
//
//====================================================================
    public static native void registerExternalJS() /*-{
        $wnd.ffProcessRequest = @edu.caltech.ipac.firefly.core.Application::processRequest(Ljava/lang/String;);
    }-*/;

    public static native boolean isIosAppStandalong() /*-{
        if ("standalone" in $wnd.navigator) {
            return $wnd.navigator.standalone;
        }
        else {
            return false;
        }
    }-*/;

}

