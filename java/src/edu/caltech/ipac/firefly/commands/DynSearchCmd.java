package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutSelector;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.dyn.DynData;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.dyn.xstream.AndTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ConditionTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ConstraintsTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.DownloadTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.EventWorkerTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.FieldGroupTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.FormTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.HtmlLoaderTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LabelTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutAreaTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutContentTypeTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutTypeTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.OrTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.PreviewTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.QueryTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ResultTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchFormParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SplitPanelTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.TableTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ViewTag;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.DynDownloadSelectionDialog;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormUtil;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ResizablePanel;
import edu.caltech.ipac.firefly.ui.TargetPanel;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.TablePrimaryDisplay;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.gwtclone.SplitLayoutPanelFirefly;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.ui.GwtUtil.createShadowTitlePanel;

public class DynSearchCmd extends CommonRequestCmd {

    private String projectId;
    private SearchTypeTag searchTypeTag;
    private Form searchForm;


    public DynSearchCmd(String projectId, String cmdId, SearchTypeTag searchTypeTag) {
        super(cmdId, searchTypeTag.getTitle());
        this.projectId = projectId;
        this.searchTypeTag = searchTypeTag;
    }

    protected Form createForm() {
        searchForm = GwtUtil.createSearchForm(searchTypeTag.getForm(), null);

        // add helpId
        String helpId = searchTypeTag.getHelpId();
        if (helpId != null) {
            searchForm.setHelpId(helpId);
        }

        return searchForm;
    }

    private HasHorizontalAlignment.HorizontalAlignmentConstant getAlignment(FieldGroupTag fg) {
        String align = fg.getAlign();
        if (align == null || align.equalsIgnoreCase("left"))
            return HasHorizontalAlignment.ALIGN_LEFT;
        else if (align.equalsIgnoreCase("center"))
            return HasHorizontalAlignment.ALIGN_CENTER;
        else if (align.equalsIgnoreCase("right"))
            return HasHorizontalAlignment.ALIGN_RIGHT;
        else
            return HasHorizontalAlignment.ALIGN_LEFT;

    }

    @Override
    protected void createAndProcessRequest() {
        List<InputFieldGroup> groups = new ArrayList<InputFieldGroup>();
        FormUtil.getAllChildGroups(searchForm, groups);
        boolean noResolutionNeeded = true;

        for (InputFieldGroup ifG : groups) {
            // TODO: new TargetPanel resolves as user enters value - this code is no longer needed
            // resolve any TargetPanels found
            if (ifG instanceof TargetPanel) {
                TargetPanel tp = (TargetPanel) ifG;
                if (tp.isResolveNeeded()) {
                    tp.resolvePosition(new AsyncCallback<WorldPt>() {
                        public void onFailure(Throwable caught) {
                            PopupUtil.showSevereError(caught);
                        }

                        public void onSuccess(WorldPt result) {
                            // process again
                            createAndProcessRequest();
                        }
                    });

                    noResolutionNeeded = false;
                }
            }
        }

        if (noResolutionNeeded) {
            DynSearchCmd.super.createAndProcessRequest();
        }
    }

    @Override
    protected void onFormSubmit(Request req) {
        req.setParam(DynUtils.HYDRA_PROJECT_ID, projectId);
        req.setParam("searchName", searchTypeTag.getName());
        validate();
    }

    private void prepareEventWorker(EventHub hub, EventWorkerTag ev) {
        WidgetFactory factory = Application.getInstance().getWidgetFactory();

        String type = ev.getType();
        List<ParamTag> pList = ev.getParams();

        String queryIds = StringUtils.toString(ev.getQueryIds(), ",");
        Map<String, String> params = DynUtils.convertParams(pList);
        params.put(EventWorker.QUERY_SOURCE, queryIds);

        String id = ev.getId();
        params.put(EventWorker.ID, id);

        EventWorker w = factory.createEventWorker(type, params);
        w.setDelayTime(ev.getDelayTime());
        w.bind(hub);
    }

    protected void processRequest(final Request inputReq, final AsyncCallback<String> callback) {
        WidgetFactory factory = Application.getInstance().getWidgetFactory();
        EventHub hub = new EventHub();
        PrimaryTableUILoader loader = getTableUiLoader();

        if (Boolean.parseBoolean(searchTypeTag.getLayoutSelector())) {
            LayoutSelector loSel = Application.getInstance().getLayoutManager().getLayoutSelector();
            loSel.setHub(hub);
            loSel.setVisible(true);
        }
        ResultTag r = searchTypeTag.getResult();
        if (r != null) {
            List<EventWorkerTag> evList = r.getEventWorkers();
            for (EventWorkerTag ev : evList) {
                prepareEventWorker(hub, ev);
            }

            // process layout
            LayoutTag l = r.getLayout();
            if (l != null) {
                LayoutTypeTag lt = l.getLayoutType();
                if (lt != null && lt instanceof SplitPanelTag) {
                    SplitPanelTag sp = (SplitPanelTag) lt;
                    DockLayoutPanel slp = processSplitPanel(sp, inputReq, factory, hub, loader);

                    try {
                        loader.loadAll();
                        this.setResults(slp);
                    } catch (Exception e) {
                        PopupUtil.showSevereError(e);
                    }
                }
            }
        }
    }

    private String getGroupValueFromForm(Form f, String key) {
        String val = null;
        List<InputFieldGroup> groups = new ArrayList<InputFieldGroup>();

        FormUtil.getAllChildGroups(f, groups);
        boolean found = false;
        for (InputFieldGroup ifG : groups) {
            List<Param> pL = ifG.getFieldValues();
            for (Param _p : pL) {
                if (_p.getName().equals(key)) {
                    val = _p.getValue();
                    found = true;
                    break;
                }
            }

            if (found) {
                break;
            }
        }

        return val;
    }

    private void evaluateSearchFormParam(SearchFormParamTag t, List<ParamTag> pList) {
        String keyName = t.getKeyName();
        String keyValue = t.getKeyValue();
        String createParams = t.getCreateParams();

        InputField inF = searchForm.getField(keyName);
        if (inF == null) {
            // see if it a fieldgroup
            // TODO

        } else if (inF.isVisible()) {
            String fieldDefValue = inF.getValue();
            if (keyValue.equals(fieldDefValue) || (keyValue.equals("*") && fieldDefValue.length() > 0)) {
                String[] createParamArr = createParams.split(",");
                for (String createParam : createParamArr) {
                    InputField inF2 = searchForm.getField(createParam);
                    if (inF2 == null) {
                        // see if it is within a fieldgroup
                        String val = getGroupValueFromForm(searchForm, createParam);
                        if (val != null) {
                            ParamTag pt = new ParamTag(createParam, val);
                            pList.add(pt);
                        }

                    } else {
                        String val = inF2.getValue();
                        if (val != null) {
                            ParamTag pt = new ParamTag(createParam, val);
                            pList.add(pt);
                        }
                    }
                }
            }
        }
    }

    protected DockLayoutPanel processSplitPanel(SplitPanelTag sp, Request inputReq, WidgetFactory factory, EventHub hub,
                                                PrimaryTableUILoader loader) {

        DynData hData = (DynData) Application.getInstance().getAppData(DynUtils.HYDRA_APP_DATA);

        SplitLayoutPanelFirefly slp = new SplitLayoutPanelFirefly();
        hub.bind(slp);
//        SplitLayoutPanel slp = new SplitLayoutPanel();
        slp.setSize("100%", "100%");
        slp.setMinCenterSize(30, 120);
        GwtUtil.setStyle(slp, "overflowX", "auto");

        LinkedHashSet<LayoutAreaTag> laTags = sp.getLayoutAreas();
        boolean centerAdded = false;
        for (LayoutAreaTag laTag : laTags) {
            if (centerAdded) {
                DynUtils.PopupMessage("XML Error", "XML configuration file is invalid!  Center layouts must be added last within a SplitPanel!");
                break;
            }

            // if groupId set, store widget and parent splitlayoutpanel
            DynData.SplitLayoutPanelData panelData = null;
            String groupId = laTag.getGroupId();
            if (groupId != null) {
                panelData = new DynData.SplitLayoutPanelData(slp);
            }

            LayoutAreaTag.LayoutDirection dir = laTag.getType();
            boolean doTag = Boolean.parseBoolean(String.valueOf(laTag.getTagIt()));

            List<SplitPanelTag> spList = laTag.getSplitPanels();
            for (SplitPanelTag spItem : spList) {
                DockLayoutPanel slp2 = processSplitPanel(spItem, inputReq, factory, hub, loader);

                switch (dir) {
                    case NORTH:
                        slp.addNorth(slp2, laTag.getIntialHeight());
                        break;

                    case SOUTH:
                        slp.addSouth(slp2, laTag.getIntialHeight());
                        break;

                    case EAST:
                        slp.addEast(slp2, laTag.getIntialWidth());
                        break;

                    case WEST:
                        slp.addWest(slp2, laTag.getIntialWidth());
                        break;

                    case CENTER:
                        slp.add(slp2);
                        centerAdded = true;
                }
            }

            List<FormTag> fList = laTag.getForms();
            if (fList != null && fList.size() > 0) {
                for (FormTag f : fList) {
                    final String title = f.getTitle();
                    final String helpId = StringUtils.isEmpty(f.getHelpId()) ? null : f.getHelpId();
                    final Form form = GwtUtil.createForm(f, hub, null);
                    form.getFieldCount();  // adds listeners

                    Toolbar.CmdButton button = new Toolbar.CmdButton(title, title, title,
                            new Command() {
                                public void execute() {
                                    PopupUtil.showDialog(Application.getInstance().getToolBar(), form, title, "Done", helpId);
                                }
                            });
                    Application.getInstance().getToolBar().addButton(button);
                    WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_START, new WebEventListener() {
                        public void eventNotify(WebEvent ev) {
                            Application.getInstance().getToolBar().removeButton(title);
                            WebEventManager.getAppEvManager().removeListener(this);
                        }
                    });
                }
            }

            String layoutType = laTag.getLayout();
            TabPane tp = null;
            CellPanel container = null;

            if (layoutType != null && (layoutType.equalsIgnoreCase("tab") || layoutType.equalsIgnoreCase("fixedTab"))) {
                tp = new TabPane();
                tp.setSize("100%", "100%");

                String tpName = laTag.getLayoutName();
                if (!StringUtils.isEmpty(tpName)) {
                    tp.setTabPaneName(tpName);
                }

                if (layoutType.equalsIgnoreCase("tab")) {
                    new NewTableEventHandler(hub, tp);
                }

            } else if (layoutType != null) {
                container = layoutType.equalsIgnoreCase("horizontal") ? new HorizontalPanel() :
                        new VerticalPanel();
                container.setSize("100%", "100%");
                container.setSpacing(5);
                GwtUtil.setStyle(container, "borderSpacing", "10px 5px");
            }

            List<LayoutContentTypeTag> lctList = laTag.getLayoutContentTypes();
            for (LayoutContentTypeTag lct : lctList) {
                if (lct instanceof TableTag) {
                    TableTag t = (TableTag) lct;
                    String queryId = t.getQueryId();

                    // check constraintsTag
                    boolean constraintCheck = true;
                    List<QueryTag> queryTagList = searchTypeTag.getQueries();
                    QueryTag queryTag = null;

                    for (QueryTag q : queryTagList) {
                        if (queryId.equalsIgnoreCase(q.getId())) {
                            queryTag = q;

                            constraintCheck = checkConstraints(q.getConstraints(), inputReq);

                            // tables only have 1 QuerySource
                            break;
                        }
                    }

                    if (queryTag != null && constraintCheck) {
                        String searchProcessorId = queryTag.getSearchProcessorId();
                        if (searchProcessorId != null) {
                            Map<String, String> tableParams = new HashMap<String, String>();
                            tableParams.put(TablePanelCreator.TITLE, t.getTitle());
                            tableParams.put(TablePanelCreator.SHORT_DESC, t.getShortDescription());
                            tableParams.put(TablePanelCreator.QUERY_SOURCE, t.getQueryId());

                            List<ParamTag> pList = t.getParams();
                            for (ParamTag p : pList) {
                                tableParams.put(p.getKey(), p.getValue());
                            }

                            TableServerRequest tsReq = new TableServerRequest(searchProcessorId, inputReq);

                            tsReq.setParam(DynUtils.QUERY_ID, t.getQueryId());

                            // get query params
                            List<ParamTag> paramTagList = queryTag.getParams();
                            for (ParamTag p : paramTagList) {
                                tsReq.setParam(p.getKey(), p.getValue());
                            }

                            tableParams.put("QUERY_ID", searchProcessorId);

                            String tableType = t.getType();
                            final PrimaryTableUI primary = factory.createPrimaryUI(tableType, tsReq, tableParams);

                            // check for downloadTag options
                            DownloadTag dl = queryTag.getDownload();
                            if (dl != null) {
                                DynDownloadSelectionDialog ddsd = new DynDownloadSelectionDialog(dl.getTitle());
                                String maxRows = dl.getMaxRows();
                                if (!StringUtils.isEmpty(maxRows)) {
                                    DownloadSelectionIF.MinMaxValidator validator = new DownloadSelectionIF.MinMaxValidator(
                                            ddsd, 1, Integer.parseInt(maxRows));
                                    ddsd.setValidator(validator);
                                }

                                FormTag formTag = dl.getFormTag();
                                if (formTag != null) {
                                    List<FieldGroupTag> dlFg = formTag.getFieldGroups();
                                    if (dlFg != null) {
                                        Form dlform = GwtUtil.createForm(false, formTag, null, getForm());
                                        ddsd.addFieldDefPanel(dlform);
                                    }
                                }

                                String dlId = dl.getId();

                                List<ParamTag> dlParams = dl.getParams();
                                List<SearchFormParamTag> sfParams = dl.getSearchFormParams();
                                for (SearchFormParamTag sfpt : sfParams) {
                                    evaluateSearchFormParam(sfpt, dlParams);
                                }

                                String downloadTitle = dl.getTitlePrefix();
                                if (!StringUtils.isEmpty(inputReq.getShortDesc())) {
                                    downloadTitle += " " + inputReq.getShortDesc();
                                }
                                downloadTitle += " Search";
                                primary.addDownloadButton(ddsd, dlId, dl.getFilePrefix(), downloadTitle,
                                        DynUtils.convertToParamList(dlParams));
                            }

                            // process view, if exists
                            List<ViewTag> tviews = t.getViews();
                            if (tviews != null) {
                                HashMap<String, String> formFields = new HashMap<String, String>();
                                for (Param p : inputReq.getParams()) {
                                    formFields.put(p.getName(), p.getValue());
                                }

                                for (ViewTag v : tviews) {
                                    String vqueryId = !StringUtils.isEmpty(v.getQueryId()) ? v.getQueryId() : queryId;
                                    Map<String, String> params = new HashMap<String, String>();
                                    params.putAll(formFields);
                                    params.put(DynUtils.QUERY_ID, vqueryId);
                                    for (ParamTag pt : v.getParams()) {
                                        params.put(pt.getKey(), pt.getValue());
                                    }
                                    TablePanel.View view = factory.createTablePanelView(v.getType(), params);
                                    if (primary instanceof TablePrimaryDisplay) {
                                        view.bind(hub);
                                        ((TablePrimaryDisplay) primary).getTable().addView(view);
                                    }
                                }
                            }

                            if (primary != null)
                                primary.bind(hub);

                            loader.addTable(primary);

                            if (tp != null) {
                                tp.addTab(primary.getDisplay(), t.getTitle(), t.getShortDescription(), false);

                            } else if (container != null) {
//                                Widget w = createShadowTitlePanel(primary.getDisplay(), primary.getShortDesc(), laTag.getHelpId(), doTag);
                                SimplePanel wrapper = new SimplePanel();
                                wrapper.add(primary.getDisplay());
                                wrapper.setStyleName("shadow");
                                wrapper.addStyleName("expand-fully");
//                                w.setSize(getInitSizeStr(laTag.getIntialWidth()), getInitSizeStr(laTag.getIntialHeight()));
                                container.add(wrapper);
                                if (layoutType.equalsIgnoreCase("horizontal")) {
                                    container.setCellHeight(wrapper, "100%");
                                    container.setCellWidth(wrapper, 100 / lctList.size() + "%");
                                } else {
                                    container.setCellWidth(wrapper, "100%");
                                    container.setCellHeight(wrapper, 100 / lctList.size() + "%");
                                }
                            } else {
                                Widget w = createShadowTitlePanel(primary.getDisplay(), primary.getShortDesc(), laTag.getHelpId(), doTag);
                                switch (dir) {
                                    case NORTH:
                                        slp.addNorth(w, laTag.getIntialHeight());
                                        break;

                                    case SOUTH:
                                        slp.addSouth(w, laTag.getIntialHeight());
                                        break;

                                    case EAST:
                                        slp.addEast(w, laTag.getIntialWidth());
                                        break;

                                    case WEST:
                                        slp.addWest(w, laTag.getIntialWidth());
                                        break;

                                    case CENTER:
                                        slp.add(w);
                                        centerAdded = true;
                                }
                            }
                        }
                    }

                } else if (lct instanceof PreviewTag) {
                    PreviewTag pv = (PreviewTag) lct;
                    List<String> queryIds = pv.getQueryIds();

                    // check constraintsTag of all possible queries
                    boolean constraintCheck = false;

                    if (queryIds != null) {
                        List<QueryTag> queryTagList = searchTypeTag.getQueries();
                        for (String queryId : queryIds) {
                            List<String> qIdList = StringUtils.asList(queryId, ",");

                            for (QueryTag q : queryTagList) {
                                if (qIdList.contains(q.getId())) {
                                    constraintCheck = constraintCheck || checkConstraints(q.getConstraints(), inputReq);
                                }
                            }
                        }
                    }
                    ConstraintsTag c = pv.getConstraints();
                    if (c != null) {
                        constraintCheck = constraintCheck && checkConstraints(c, inputReq);
                    }

                    if (queryIds == null || constraintCheck) {
                        Map<String, String> previewParams = new HashMap<String, String>();
                        List<ParamTag> pList = pv.getParams();
                        for (ParamTag p : pList) {
                            String key = p.getKey();
                            String value = p.getValue();

                            previewParams.put(key, value);
                        }

                        if (queryIds != null) {
                            previewParams.put("QUERY_ID", StringUtils.toString(queryIds, ","));
                        }

                        List<String> eventWorkerIds = pv.getEventWorkerIds();
                        if (eventWorkerIds != null) {
                            previewParams.put("EVENT_WORKER_ID", StringUtils.toString(eventWorkerIds, ","));
                        }

                        String previewType = pv.getType();
                        TablePreview preview = factory.createObserverUI(previewType, previewParams);

                        String previewId = pv.getId();
                        if (previewId != null) {
                            preview.setID(previewId);
                        }

                        if (preview != null)
                            preview.bind(hub);


                        if (tp != null) {
                            tp.addTab(preview.getDisplay(), pv.getTitle(), pv.getShortDescription(), false);

                        } else if (container != null) {
//                            Widget pw = createPreviewPanel(preview, pv.getFrameType(), laTag.getHelpId(), doTag);
//                            pw.setSize(getInitSizeStr(laTag.getIntialWidth()), getInitSizeStr(laTag.getIntialHeight()));
                            ResizablePanel wrapper = new ResizablePanel();
                            wrapper.add(preview.getDisplay());
                            wrapper.setStyleName("shadow");
                            wrapper.addStyleName("expand-fully");
                            container.add(wrapper);
                            if (layoutType.equalsIgnoreCase("horizontal")) {
                                container.setCellHeight(wrapper, "100%");
                            } else {
                                container.setCellWidth(wrapper, "100%");
                            }
                        } else {
                            Widget pw = createPreviewPanel(preview, pv.getFrameType(), laTag.getHelpId(), doTag);
                            Double size = null;
                            switch (dir) {
                                case NORTH:
                                    size = new Double(laTag.getIntialHeight());
                                    slp.addNorth(pw, size);

                                    break;

                                case SOUTH:
                                    size = new Double(laTag.getIntialHeight());
                                    slp.addSouth(pw, size);
                                    break;

                                case EAST:
                                    size = new Double(laTag.getIntialWidth());
                                    slp.addEast(pw, size);
                                    break;

                                case WEST:
                                    size = new Double(laTag.getIntialWidth());
                                    slp.addWest(pw, size);
                                    break;

                                case CENTER:
                                    slp.add(pw);
                                    centerAdded = true;
                            }

                            // if groupId set, store widget and parent splitlayoutpanel
                            if (panelData != null) {
                                panelData.addWidget(previewId, pw);
                                panelData.addSize(previewId, size);
                            }
                        }
                    }
                }
            }

            // HtmlLoaderTag call the server, then display the result message as html
            List<HtmlLoaderTag> htmlLoaders = laTag.getHtmlLoaders();
            if (htmlLoaders != null) {
                for (HtmlLoaderTag htmlLoader : htmlLoaders) {
                    HTML html = new HTML();
                    LabelTag l = htmlLoader.getLabel();
                    if (l != null) {
                        html.setHTML(l.getHtmlString());
                    }
                    doHtmlLoad(inputReq, htmlLoader, html);
                    container.add(html);
                }
            }

            if (tp != null && tp.getWidgetCount() > 0) {
                Widget w = null;
                String taTitle = laTag.getTitle();
                if (taTitle != null && taTitle.length() > 0) {
                    w = createShadowTitlePanel(tp, taTitle, laTag.getHelpId(), doTag);
                } else {
                    w = tp;
                    if (!StringUtils.isEmpty(laTag.getHelpId())) {
                        tp.setHelpId(laTag.getHelpId());
                    }
                }

                switch (dir) {
                    case NORTH:
                        slp.addNorth(w, laTag.getIntialHeight());
                        break;

                    case SOUTH:
                        slp.addSouth(w, laTag.getIntialHeight());
                        break;

                    case EAST:
                        slp.addEast(w, laTag.getIntialWidth());
                        break;

                    case WEST:
                        slp.addWest(w, laTag.getIntialWidth());
                        break;

                    case CENTER:
                        slp.add(w);
                        centerAdded = true;
                }

                hub.bind(tp);

            } else if (container != null) {
                Widget w = null;
                String taTitle = laTag.getTitle();
                if (taTitle != null && taTitle.length() > 0) {
                    w = createShadowTitlePanel(container, taTitle, laTag.getHelpId(), doTag);
                } else
                    w = container;

                switch (dir) {
                    case NORTH:
                        slp.addNorth(w, laTag.getIntialHeight());
                        break;

                    case SOUTH:
                        slp.addSouth(w, laTag.getIntialHeight());
                        break;

                    case EAST:
                        slp.addEast(w, laTag.getIntialWidth());
                        break;

                    case WEST:
                        slp.addWest(w, laTag.getIntialWidth());
                        break;

                    case CENTER:
                        slp.add(w);
                        centerAdded = true;
                }

            }
            // store panel data, if necessary
            if (panelData != null) {
                hData.addSplitLayoutPanelItem(groupId, panelData);
            }


        }

        return slp;
    }

    private QueryTag getQueryTagIfValid(String queryId, Request inputReq) {
        // check constraintsTag
        boolean constraintCheck = true;
        List<QueryTag> queryTagList = searchTypeTag.getQueries();
        QueryTag queryTag = null;

        for (QueryTag q : queryTagList) {
            if (queryId.equalsIgnoreCase(q.getId())) {
                queryTag = q;

                constraintCheck = checkConstraints(q.getConstraints(), inputReq);

                // tables only have 1 QuerySource
                break;
            }
        }

        if (queryTag != null && constraintCheck) {
            return queryTag;
        } else {
            return null;
        }

    }

    private void doHtmlLoad(Request inputReq, HtmlLoaderTag htmlLoader, final HTML html) {
        String queryId = htmlLoader.getQueryId();
        QueryTag queryTag = getQueryTagIfValid(queryId, inputReq);
        if (queryTag != null) {
            String searchProcessorId = queryTag.getSearchProcessorId();
            if (searchProcessorId != null) {
                TableServerRequest tsReq = new TableServerRequest(searchProcessorId, inputReq);
                tsReq.setParam(DynUtils.QUERY_ID, htmlLoader.getQueryId());

                // get query params
                List<ParamTag> paramTagList = queryTag.getParams();
                for (ParamTag p : paramTagList) {
                    tsReq.setParam(p.getKey(), p.getValue());
                }
                SearchServices.App.getInstance().getRawDataSet(tsReq, new BaseCallback<RawDataSet>() {
                    public void onFailure(Throwable caught) {
                        Application.getInstance().getToolBar().getDropdown().open();
                        PopupUtil.showSevereError(caught);
                        unmask();
                    }

                    public void doSuccess(RawDataSet result) {
                        if (result != null) {
                            DataSet data = DataSetParser.parse(result);
                            String msg = data.getMeta().getAttribute("Message");
                            if (msg != null) {
                                html.setHTML(html.getHTML() + msg);
                            }
                        }
                    }
                });
            }

        }
    }

    private String getInitSizeStr(double v) {
        if (v == DynUtils.DEFAULT_LAYOUT_AREA_WIDTH) {
            return "100%";
        } else {
            return v + "px";
        }
    }

    public boolean checkConstraints(ConstraintsTag con, Request req) {
        boolean constraintCheck = true;

        if (con != null) {
            ConditionTag cond = con.getCondition();
            if (cond != null) {
                constraintCheck = checkCondition(cond, req);

            } else {
                AndTag a = con.getAnd();
                if (a != null) {
                    constraintCheck = checkAndConstraint(a, req);

                } else {
                    OrTag o = con.getOr();
                    if (o != null) {
                        constraintCheck = checkOrConstraint(o, req);
                    }
                }
            }
        }

        return constraintCheck;
    }

    private boolean checkCondition(ConditionTag cond, Request req) {
        boolean retVal = true;


        String fieldDefVisible = cond.getFieldDefVisible();
        if (!StringUtils.isEmpty(fieldDefVisible)) {
            String v = req.getParam(fieldDefVisible);
            retVal = !StringUtils.isEmpty(v);
        } else {
            String fieldDefHidden = cond.getFieldDefHidden();
            if (!StringUtils.isEmpty(fieldDefHidden)) {
                String v = req.getParam(fieldDefHidden);
                retVal = StringUtils.isEmpty(v);
            } else {
                String fieldDefId = cond.getFieldDefId();
                String value = cond.getValue();

                if (req.containsParam(fieldDefId)) {
                    String fdValue = req.getParam(fieldDefId) == null ? "" : req.getParam(fieldDefId);
                    if (!fdValue.equals(value)) {
                        // assume value with ',' are multi-value with comma separators.
                        if (fdValue.contains(",")) {
                            List l = StringUtils.asList(fdValue, ",");
                            retVal = l.contains(value);
                        } else {
                            retVal = false;
                        }
                    }
                } else {
                    retVal = false;
                }

            }
        }

        return retVal;
    }

    private boolean checkAndConstraint(AndTag a, Request req) {
        boolean retVal = true;

        List<ConditionTag> condList = a.getConditions();
        for (ConditionTag cond : condList) {
            retVal = retVal && checkCondition(cond, req);
        }

        // shortcut out
        if (!retVal)
            return retVal;

        OrTag orTag = a.getOr();
        if (orTag != null) {
            retVal = retVal && checkOrConstraint(orTag, req);
        }

        return retVal;
    }

    private boolean checkOrConstraint(OrTag o, Request req) {
        boolean retVal = false;

        List<ConditionTag> condList = o.getConditions();
        for (ConditionTag cond : condList) {
            retVal = retVal || checkCondition(cond, req);
        }

        // shortcut out
        if (retVal)
            return retVal;

        AndTag andTag = o.getAnd();
        if (andTag != null) {
            retVal = retVal || checkAndConstraint(andTag, req);
        }

        return retVal;
    }

    private Widget createPreviewPanel(TablePreview preview, String frameType, String helpId, boolean doTag) {
        if (frameType.equalsIgnoreCase("titleFrame")) {
            return createShadowTitlePanel(preview.getDisplay(), preview.getShortDesc(), helpId, doTag);

        } else if (frameType.equalsIgnoreCase("noFrame")) {
            return preview.getDisplay();

        } else {
            // default: frameType = "frameOnly"
            return createShadowTitlePanel(preview.getDisplay(), "", helpId, doTag);
        }

    }

    public String getDownloadTitlePrefix(Request inputReq) {
        String tname = inputReq.getParam(TargetPanel.TARGET_NAME_KEY);
        if (!StringUtils.isEmpty(tname)) {
            return tname + ": ";
        } else {
            return "";
        }
    }

    public String getDownloadFilePrefix(Request inputReq) {
        String tname = inputReq.getParam(TargetPanel.TARGET_NAME_KEY);
        if (!StringUtils.isEmpty(tname)) {
            return tname.replaceAll("\\s+", "") + "-";
        } else {
            return "tgt-";
        }
    }

    private static void setFormMinSize(FormTag ftag, Element el) {
        String msize = ftag.getMinSize();
        if (!StringUtils.isEmpty(msize)) {
            try {
                String[] wh = msize.split("x", 2);
                int minWidth = Integer.parseInt(wh[0]);
                int minHeight = Integer.parseInt(wh[1]);
                if (minWidth > 0) {
                    DOM.setStyleAttribute(el, "minWidth", minWidth + "px");
                }
                if (minHeight > 0) {
                    DOM.setStyleAttribute(el, "minHeight", minHeight + "px");
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private Map<String, String> getQueryParams(SearchTypeTag searchTag, String queryId) {
        Map<String, String> params = new HashMap<String, String>();

        List<QueryTag> queryTagList = searchTag.getQueries();
        QueryTag queryTag = null;

        for (QueryTag q : queryTagList) {
            if (queryId.equalsIgnoreCase(q.getId())) {
                queryTag = q;
                // tables only have 1 QuerySource
                break;
            }
        }

        if (queryTag != null) {
            String searchProcessorId = queryTag.getSearchProcessorId();
            if (!StringUtils.isEmpty(searchProcessorId)) {
                params.put(Request.ID_KEY, searchProcessorId);
            }

            // get query params
            List<ParamTag> paramTagList = queryTag.getParams();
            for (ParamTag p : paramTagList) {
                params.put(p.getKey(), p.getValue());
            }

        }
        return params;
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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