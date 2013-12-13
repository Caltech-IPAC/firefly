package edu.caltech.ipac.firefly.ui.imageGrid;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.ui.VisibleListener;
import edu.caltech.ipac.firefly.ui.creator.PreviewImageGridCreator;
import edu.caltech.ipac.firefly.ui.creator.drawing.DatasetDrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.creator.drawing.DrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.previews.AbstractPreviewData;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetGroup;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.CatalogDisplay;
import edu.caltech.ipac.firefly.visualize.draw.DataConnectionDisplay;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Aug 9, 2010
 * Time: 4:29:19 PM
 * $Id: BasicImageGrid.java,v 1.133 2012/12/22 01:20:04 tlau Exp $
 */
public class BasicImageGrid extends ScrollPanel implements VisibleListener {

    public static final Name ON_SELECTED_PLOT_CHANGE = new Name("BasicImageGrid.ON_SELECTED_PLOT_CHANGE",
                                                                "When the new plot is selected (highlighted).");

    public static final Name ON_CHECKED_PLOT_CHANGE = new Name("BasicImageGrid.ON_CHECKED_PLOT_CHANGE",
                                                               "When a plot is checked or unchecked.");

    public static final Name ON_ALL_CHECKED_PLOT_CHANGE = new Name("BasicImageGrid.ON_ALL_CHECKED_PLOT_CHANGE",
                                                                   "When all plots are checked or unchecked. Data is the boolean, true for all checked, false for all unchecked");

    public static final Name ON_ALL_PLOTS_DONE = new Name("BasicImageGrid.ON_ALL_PLOTS_DONE",
                                                                   "When all plots are done.");
    private static int INIT_SIZE = 3;

    //IPAC TABLE HEADERS FOR IMAGE GRID LAYOUT OPTIONS
    public static String DATA_TYPE_COLUMN = "DATA_TYPE_COLUMN";
    public static String DESCRIPTION_COLUMN = "DESCRIPTION_COLUMN";
    public static String GROUPING_COLUMN = "GROUPING_COLUMN";
    public static String EVENTWORKER_COLUMN = "EVENTWORKER_COLUMN";
    public static String ALL_EVENTWORKER_COLUMN = "ALL_EVENTWORKER_COLUMN";
    public static String FULL_SIZE_URL_COLUMN = "FULL_SIZE_URL_COLUMN";
    public static String GROUPING = "GROUPING";
    public static String GRID_BACKGROUND = "GRID_BACKGROUND";
    public static String INFO = "INFO";
    public static String JPEG_SELECTION_HILITE = "JPEG_SELECTION_HILITE";
    public static String JPEG_SELECTION_DOUBLE_CLICK = "JPEG_SELECTION_DOUBLE_CLICK";
    public static String SOURCE_ID_COLUMN = "SOURCE_ID_COLUMN";
    public static String THUMBNAIL_URL_COLUMN = "THUMBNAIL_URL_COLUMN";
    public static String COLUMNS = "COLUMNS";

    //GROUPING PARAMETERS
    private static String show_in_a_row = "show_in_a_row";
    private static String show_label = "show_label";
    private static String groupBy = "groupBy";
    private static String keywords = "keywords";

    private static String TYPE_FITS = "fits";
    private static String TYPE_REQ = "req";
    private static String TYPE_JPEG = "jpg";
    private static final String PARAM_SEP = "&";
    private static final String GRID_BACKGROUND_DEFAULT_COLOR = "#f4f4f4";
    private static final int MPW_HEADER_HEIGHT = 24;
    private static final String EMPTY_GRID = "There are no data to display";
    //private static final String ENABLE_PLOT_WIDGET_GROUP_LOCK_RELATED = "ENABLE_PLOT_WIDGET_GROUP_LOCK_RELATED";
    private FlowPanel mainPanel = new FlowPanel();
    private Map<String, ImageGridWidgetGroup> imageGridWidgetGroups = new LinkedHashMap<String, ImageGridWidgetGroup>();
    private Map<String, Integer> rowWidths = new HashMap<String, Integer>();
    private int columns = 1; //disable if columns<=1
    private int mouseOverAt = 0;
    private int currentRow = 0;
    private int nextMpw = 0;
    private int currentViewHeight = 0;
    private int currentViewWidth = 0;
    private int currentScrollX = 0;
    private int currentScrollY = 0;
    private boolean updateDisplay = false;
    private String dataTypeColumnKeyword = null;
    private String thumbnailUrlColumnKeyword = null;
    private String thumbnailTitleColumnKeyword = null;
    private String eventWorkerKeyword = null;
    private String allEventWorkerKeyword = null;
    private String groupingColumnKeyword = null;
    private String fullSizeUrlColumnKeyword = null;
    private String sourceIdColumnKeyword = null;
    private boolean groupingByColumns = false;
    private boolean groupingInRow = false;
    private Boolean lockrelated = null;
    private Boolean useScrollBars = null;
    private Map<String, String> titleDescMap = null;
    private List<String> plotEWList = null;
    private String info = null;
    private String groupingShowLabel = "OFF"; //ON, OFF, TOP
    private LinkedHashMap<String, String> groupingParams = new LinkedHashMap<String, String>(0);
    private ImageGridPanelHandler imageGridPanelHandler = new ImageGridPanelHandler();
    private List<TableData.Row> rowValues = null;
    private ArrayList<MiniPlotWidget> mpwList = new ArrayList<MiniPlotWidget>(INIT_SIZE);
    private EventHub hub = null;
    private final PreviewImageGridCreator.ImageGridPreviewData _previewData;
    private HashMap<VerticalPanel, MiniPlotWidget> panelMap = new HashMap<VerticalPanel, MiniPlotWidget>();
    private boolean checkingEnabled = false;
    private boolean suspendEvents = false;
    private boolean onlyShowingFilteredResults = false;
    private int plottingCnt = 0;
    private int successPlots = 0;
    private int failurePlots = 0;

    private WebEventManager eventManager = new WebEventManager();
    private PlotWidgetEventListener plotWidgetEventListener;
    private PostPlotWidgetEventListener postPlotWidgetEventListener;
    private MiniPlotWidget.PlotError plotError = null;
    private int lastThumbnailHeight = 0;
    private String _plotGroup = null;
    //private MiniPlotWidget _currentMpw = null;
    private TablePanel tablePanel = null;

    public BasicImageGrid(AbstractPreviewData previewData) {
        super.setSize("100%", "100%");
        _previewData = (PreviewImageGridCreator.ImageGridPreviewData)previewData;
        mainPanel.setSize("100%", "100%");
        super.add(mainPanel);
        GwtUtil.setStyle(mainPanel, "backgroundColor", GRID_BACKGROUND_DEFAULT_COLOR);
        GwtUtil.setStyle(this, "backgroundColor", GRID_BACKGROUND_DEFAULT_COLOR);

        setPlotWidgetGroupValue(_previewData.getPlotWidgetGroup());

        for (int i = 0; i < INIT_SIZE; i++) {
            createAndAddMiniPlotWidget();
        }

        getPlotWidgetGroup().setImageGrid(this);
        getPlotWidgetGroup().setGridPopoutColumns(_previewData.getGridPopoutCols());
        getPlotWidgetGroup().setGridPopoutZoomType(_previewData.getGridPopoutZoomtype());

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                resize();
            }
        });
        plotError = new MiniPlotWidget.PlotError() {
            public void onError(WebPlot wp, String briefDesc, String desc, String details, Exception e) {

            }
        };
    }

    /**
     * This method is called immediately after a widget becomes attached to the
     * browser's document.
     */
    @Override
    protected void onLoad() {
        super.onLoad();
        addPlotWidgetEventListeners();
    }

    /**
     * This method is called immediately before a widget will be detached from the
     * browser's document.
     */
    @Override
    protected void onUnload() {
        removePlotWidgetEventListeners();
        super.onUnload();
    }

    public WebEventManager getEventManager() {
        return eventManager;
    }

    private void handlePlotEvent(WebEvent ev) {
        if (suspendEvents) return;
        if (ev.getName().equals(Name.FITS_VIEWER_CHANGE)) {
            MiniPlotWidget mpw = (MiniPlotWidget) ev.getData();
            for (int i = 0; i < nextMpw; i++) {
                if (mpwList.get(i).equals(mpw)) {
                    eventManager.fireEvent(new WebEvent(this, ON_SELECTED_PLOT_CHANGE));
                }
            }
        } else if (ev.getName().equals(Name.CHECKED_PLOT_CHANGE)) {
            eventManager.fireEvent(new WebEvent(this, ON_CHECKED_PLOT_CHANGE, false));
        } else if (ev.getName().equals(Name.ALL_CHECKED_PLOT_CHANGE)) {
            eventManager.fireEvent(new WebEvent(this, ON_ALL_CHECKED_PLOT_CHANGE, ev.getData()));
        }
    }

    private void handlePostPlotEvent(WebEvent ev) {
        if (suspendEvents) return;
        if (ev.getName().equals(Name.ALL_PLOT_TASKS_COMPLETE)) {
            resize();
        }
    }

    private void addPlotWidgetEventListeners() {
        plotWidgetEventListener = new PlotWidgetEventListener();
        postPlotWidgetEventListener = new PostPlotWidgetEventListener();
        AllPlots.getInstance().addListener(Name.FITS_VIEWER_CHANGE, plotWidgetEventListener);
        AllPlots.getInstance().addListener(Name.CHECKED_PLOT_CHANGE, plotWidgetEventListener);
        AllPlots.getInstance().addListener(Name.ALL_CHECKED_PLOT_CHANGE, plotWidgetEventListener);
        AllPlots.getInstance().addListener(Name.ALL_PLOT_TASKS_COMPLETE, postPlotWidgetEventListener);
    }

    private void removePlotWidgetEventListeners() {
        AllPlots.getInstance().removeListener(Name.FITS_VIEWER_CHANGE, plotWidgetEventListener);
        AllPlots.getInstance().removeListener(Name.CHECKED_PLOT_CHANGE, plotWidgetEventListener);
        AllPlots.getInstance().removeListener(Name.ALL_CHECKED_PLOT_CHANGE, plotWidgetEventListener);
    }

    /**
     * Select the plot widget in the cell with the given index
     *
     * @param idx cell index
     */
    public void setSelectedPlotIdx(int idx) {
        if (nextMpw > 0) {
            if (idx >= 0 && idx < nextMpw) {
                suspendEvents = true;
                AllPlots.getInstance().setSelectedWidget(mpwList.get(idx));
                suspendEvents = false;
            } else {
                throw new ArrayIndexOutOfBoundsException(idx + " must be from 0 to " + (nextMpw - 1));
            }
        }
    }

    /**
     * Get index of the cell with selected plot widget
     *
     * @return cell index or -1 if no plot widget is selected
     */
    public int getSelectedPlotIdx() {
        MiniPlotWidget selectedMpw = AllPlots.getInstance().getMiniPlotWidget();
        if (selectedMpw != null) {
            for (int i = 0; i < nextMpw; i++) {
                if (selectedMpw.equals(mpwList.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Check or uncheck all plot widgets
     *
     * @param check if true check, otherwise uncheck
     */
    public void setAllChecked(boolean check) {
        if (checkingEnabled && nextMpw > 0) {
            suspendEvents = true;
            mpwList.get(0).getGroup().setAllChecked(check);
            suspendEvents = false;
        }
    }

    /**
     * Returns true if all plot widgets are checked
     */
    public boolean isAllChecked() {
        boolean allChecked = false;
        if (checkingEnabled && nextMpw > 0) {
            allChecked = mpwList.get(0).getGroup().isAllChecked();
        }
        return allChecked;
    }


    /**
     * Check the plot widgets with the given indexes
     *
     * @param checked array list with indexes of the checked plots
     */
    public void setCheckedPlotIdx(List<Integer> checked) {
        if (checkingEnabled && nextMpw > 0) {
            suspendEvents = true;
            for (int i = 0; i < nextMpw; i++) {
                boolean doCheck = checked.contains(i);
                if (mpwList.get(i).isChecked() != doCheck) {
                    mpwList.get(i).setChecked(doCheck);
                }
            }
            suspendEvents = false;
        }
    }

    /**
     * Get indexes of the cells with checked plot widget
     *
     * @return array list with indexes of the checked plots
     */
    public ArrayList<Integer> getCheckedPlotIdx() {
        ArrayList<Integer> checked = new ArrayList<Integer>();
        if (checkingEnabled) {
            for (int i = 0; i < nextMpw; i++) {
                if (mpwList.get(i).isChecked()) {
                    checked.add(i);
                }
            }
        }
        return checked;
    }

    public void bind(EventHub hub) {
        this.hub = hub;
//        setupCatalog();
    }

    public void setEnableChecking(boolean checkingEnabled) {
        for (MiniPlotWidget mpw : mpwList) {
            if (mpw.getEnableChecking() != checkingEnabled) {
                mpw.setEnableChecking(checkingEnabled);
            }
        }
        this.checkingEnabled = checkingEnabled;

        PlotWidgetGroup group=getPlotWidgetGroup();
        group.setEnableChecking(checkingEnabled);
    }

    public void setEnablePdfDownload(boolean b) {
        PlotWidgetGroup group=getPlotWidgetGroup();
        group.setEnablePdfDownload(b);
    }

    public void setOnlyShowingFilteredResults(boolean b) {
        onlyShowingFilteredResults = b;
    }

    public void setShowDrawingLayers(boolean b) {
        getPlotWidgetGroup().setShowDrawingLayers(b);
    }

    public String getPlotWidgetGroupValue() { return _plotGroup;}
    public void setPlotWidgetGroupValue(String s) {
        if (s!=null) {
            _plotGroup = s;
        }
    }



    private PlotWidgetGroup getPlotWidgetGroup() {return mpwList.get(0).getGroup();}

    private boolean hasValidImageGridPreviewData() {
        return (_previewData != null && _previewData instanceof PreviewImageGridCreator.ImageGridPreviewData);
    }

    private void empty() {
        showMessage(EMPTY_GRID);
    }

    private boolean getLockRelated() {
        if (lockrelated == null) {
            if (hasValidImageGridPreviewData()) {
                lockrelated = _previewData.getLockRelated();
            } else {
                lockrelated = false;
            }
        }
        return lockrelated;
    }

    private boolean getUseScrollBars() {
        if (useScrollBars == null) {
            if (hasValidImageGridPreviewData()) {
                useScrollBars = _previewData.getUseScrollBars();
            } else {
                useScrollBars = false;
            }
        }
        return useScrollBars;
    }

    private List<String> getPlotEventWorkerIDList() {
        if (plotEWList == null) {
            if (hasValidImageGridPreviewData()) {
                plotEWList = _previewData.getPlotEventWorkerList();
            } else {
                plotEWList = null;
            }
        }
        return plotEWList;
    }

    private void setupCatalog() {
        if (hub != null) {
            Vis.init(new Vis.InitComplete() {
                public void done() {
                    setupDrawing();
                }
            });
        }
    }

    private void setupDrawing() {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                if (hub != null) {
                    List<WebPlotView> pvList = new ArrayList<WebPlotView>(mpwList.size());
                    for (MiniPlotWidget mpw : mpwList) {
                        pvList.add(mpw.getOps().getPlotView());
                    }
                    CatalogDisplay catDis = hub.getCatalogDisplay();
                    catDis.addPlotViewList(pvList);

                    DataConnectionDisplay dc = hub.getDataConnectionDisplay();
                    if (getPlotEventWorkerIDList() != null) {
                        for (WebPlotView pv : pvList) {
                            dc.addPlotView(pv, getPlotEventWorkerIDList());
                        }
                    }
                }
            }
        });
    }

    private void setupEventWorker(final String ew, final String all, final WebPlotView pv, final WebPlot plot) {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                if (hub != null) {
                    ArrayList<String> eventWorkerIDList = new ArrayList<String>();
                    List allEWorkers = Arrays.asList(all.split(","));
                    for (DrawingLayerProvider p : hub.getDrawingProviders()) {
                        if (allEWorkers.contains(p.getID())) {
                            eventWorkerIDList.add(p.getID());
                        }
                    }

                    DataConnectionDisplay dc = hub.getDataConnectionDisplay();
                    if (getPlotEventWorkerIDList() != null) {
                        dc.addPlotView(pv, eventWorkerIDList);
                    }

                    for (DrawingLayerProvider p : hub.getDrawingProviders()) {
                        if (eventWorkerIDList.contains(p.getID())) {
                            if (p instanceof DatasetDrawingLayerProvider) {
                                Map<String, String> params = ((DatasetDrawingLayerProvider) p).getParams();
                                params.put("subsize", plot.getAttribute("REQUESTED_SIZE").toString());
                                params.put("UserTargetWorldPt",
                                           ((ActiveTarget.PosEntry) plot.getAttribute("FIXED_TARGET")).getPt().toString());
                                p.activate(null, params);
                            }
                        }
                    }
                }
            }
        });
    }

    public void updateDisplay() {
        updateDisplay = true; // set value for a state-machine: inform object to get ready to update display.
    }

    public void clearTable() {
        saveScrollPosition();
        mainPanel.clear();
        if (imageGridWidgetGroups.size()>0) {
            for (ImageGridWidgetGroup g: imageGridWidgetGroups.values()) {
                g.clear();
            }
        }
        imageGridWidgetGroups.clear();
        rebuildGroups();
        // disable popouts
        MiniPlotWidget mpw;
        for (int i=0; i<mpwList.size(); i++) {
            mpw = mpwList.get(i);
            if (mpw != null) {
                AllPlots.getInstance().setStatus(mpw, AllPlots.PopoutStatus.Disabled);
            }
        }
        AllPlots.getInstance().clearSelectedWidget();
        nextMpw = 0;
    }

    public ComplexPanel getMainPanel() {return mainPanel;}

    /*
    public void addRows(ArrayList<WebPlotRequest> reqList) {
        renderRows(reqList, false);
    }

    public void addRows(DataSet data) {
        if (data == null) return;
        loadTableMeta(data);
        renderRows(data, false);
    }
    */

    public void loadTable(ArrayList<WebPlotRequest> reqList) {
        if (reqList == null) return;
        renderRows(reqList, true);
    }

    public void loadTable(DataSet data) {
        if (onlyShowingFilteredResults) {
            //todo: find better way to detect filtered results.
            if (hub != null && tablePanel==null) {
                tablePanel = hub.getActiveTable();
                //return;
            }
        }

        if (!updateDisplay) return;

        if (data == null) {
            if (mainPanel.getWidgetCount() == 1 && mainPanel.getWidget(0) instanceof HTML &&
                    ((HTML) mainPanel.getWidget(0)).getText().equals(EMPTY_GRID)) {
                return;
            }
            empty();
            return;
        }
        if (hub != null) {
            tablePanel = hub.getActiveTable();
            hub.getCatalogDisplay().beginBulkUpdate();
        }
        loadTableMeta(data);
        renderRows(data, true);
        if (hub != null) {
            hub.getCatalogDisplay().endBulkUpdate();
        }

        /* TG I don't think this code is used - delete
        MiniPlotWidget mpw=null;

        List<MiniPlotWidget> mpwLst = AllPlots.getInstance().getActiveList();
        if (mpwLst != null && mpwLst.size()>0) {
            mpw= AllPlots.getInstance().getActiveList().get(0);
        } else {
            mpwLst = AllPlots.getInstance().getAll();
            if (mpwLst != null && mpwLst.size()>0) {
                mpw= AllPlots.getInstance().getAll().get(0);
            }
        }
        if (mpw!=null) {
            _currentMpw = mpw;
        }
        */
        updateDisplay = false; // set value for a state-machine: inform object display updated.
    }

    public DownloadRequest getDownloadRequest() {
        DownloadRequest retval = tablePanel.getDownloadRequest();

        if (retval==null) {
            for (TablePanel panel: hub.getTables()) {
                retval = panel.getDownloadRequest();
                if (retval != null) {
                    break;
                }
            }
        }
        return retval;
    }

    protected void loadTableMeta(DataSet data) {
        if (data == null) { return; }

        TableMeta meta = data.getMeta();
        thumbnailUrlColumnKeyword = meta.getAttribute(THUMBNAIL_URL_COLUMN);
        thumbnailTitleColumnKeyword = meta.getAttribute(DESCRIPTION_COLUMN);
        fullSizeUrlColumnKeyword = meta.getAttribute(FULL_SIZE_URL_COLUMN);
        sourceIdColumnKeyword = meta.getAttribute(SOURCE_ID_COLUMN);
        dataTypeColumnKeyword = meta.getAttribute(DATA_TYPE_COLUMN);
        groupingColumnKeyword = meta.getAttribute(GROUPING_COLUMN);
        eventWorkerKeyword = meta.getAttribute(EVENTWORKER_COLUMN);
        allEventWorkerKeyword = meta.getAttribute(ALL_EVENTWORKER_COLUMN);

        info = meta.getAttribute(INFO);

        boolean jpegSelectionHiLite;
        boolean jpegSelectionDClick;
        String attribute = meta.getAttribute(JPEG_SELECTION_HILITE);
        if (attribute != null) {
            jpegSelectionHiLite = parseBoolean(attribute);
        } else {
            jpegSelectionHiLite = false;
        }

        attribute = meta.getAttribute(JPEG_SELECTION_DOUBLE_CLICK);
        if (attribute != null) {
            jpegSelectionDClick = parseBoolean(attribute);
        } else {
            jpegSelectionDClick = false;
        }

        attribute = meta.getAttribute(COLUMNS);
        if (attribute != null) {
            columns = Integer.valueOf(attribute);
            groupingByColumns = true;
        } else {
            columns = 1;
            groupingByColumns = false;
        }

        attribute = meta.getAttribute(GRID_BACKGROUND);
        if (attribute != null && attribute.length() > 0) {
            GwtUtil.setStyle(mainPanel, "backgroundColor", attribute);
            GwtUtil.setStyle(this, "backgroundColor", attribute);
            if (imageGridPanelHandler != null) {
                imageGridPanelHandler.setBackground(attribute);
            }
        }
        if (imageGridPanelHandler != null) {
            imageGridPanelHandler.enableSelectionHiLite(jpegSelectionHiLite);
            imageGridPanelHandler.enableSelectionDoubleClick(jpegSelectionDClick);
        }
        Map metas = meta.getAttributes();
        boolean hasTitleDesc = false;
        String key;
        for (Object o: metas.keySet()) {
            key = o.toString();
            if (key.startsWith("TitleDesc.")) {
                if (titleDescMap==null) {
                    titleDescMap = new HashMap<String, String>();
                }
                titleDescMap.put(key, metas.get(key).toString());
            }
        }
        parseGroupingMeta(meta.getAttribute(GROUPING));
    }

    public void resize() {
        if (requiresResize()) {

            for (ImageGridWidgetGroup g: imageGridWidgetGroups.values()) {
                g.resize();
            }
        }
    }

    public boolean isGroupingInRow() {return groupingInRow;}

    public boolean isGroupingByColumns() {return groupingByColumns;}

    public String getGroupingShowLabel() {return groupingShowLabel;}

    private boolean requiresResize() {
        return (columns > 1);
    }

    private void parseGroupingMeta(String str) {
        if (str == null) return;
        String[] params = str.split(PARAM_SEP);

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                Param p = Param.parse(params[i]);
                groupingParams.put(p.getName(), p.getValue());
            }
        }

        rebuildGroups();
    }

    private void rebuildGroups() {
        if (groupingParams.containsKey(keywords)) {
            String groups[] = groupingParams.get(keywords).split(",");
            if (groups != null) {
                for (String group : groups) {
                    //getWidgetList(group);
                }
            }
        }
        if (groupingParams.containsKey(show_in_a_row)) {
            groupingInRow = parseBoolean(groupingParams.get(show_in_a_row));
        }
        if (groupingParams.containsKey(show_label)) {
            groupingShowLabel = groupingParams.get(show_label).trim().toUpperCase();
        }
    }

    protected void renderRows(ArrayList<WebPlotRequest> reqList, boolean clearTable) {
        if (clearTable) clearTable();
        plottingCnt=0;
        successPlots= 0;
        failurePlots= 0;
        clearMaxRowWidth();

        for (WebPlotRequest req : reqList) {
            addWebPlotRequestWidget(getMiniPlotWidget(), req);
        }
        addStuff();
    }

    protected void renderRows(DataSet data, boolean clearTable) {
        String type;
        if (clearTable) clearTable();
        rowValues = new ArrayList<TableData.Row>(data.getModel().getRows());

        if (rowValues.size() == 0) {
            String text = (info != null && info.length() > 0) ? info : "Image data not found!";
            showMessage(text);
        } else {
            plottingCnt=0;
            successPlots= 0;
            failurePlots= 0;
            clearMaxRowWidth();

            MiniPlotWidget mpw;
            for (TableData.Row row : rowValues) {
                type = getThumbnailDataType(row);

                if (type.equals(TYPE_FITS)) {
                    addMiniPlotWidget(getMiniPlotWidget(), row);
                } else if (type.equals(TYPE_JPEG)) {
                    addWidget(makeCellWidget(row), row);
                } else if (type.equals(TYPE_REQ)) {
                    addWebPlotRequestWidget(getMiniPlotWidget(), row);
                }
            }
            addStuff();
        }
    }

    protected void addStuff() {
        for (ImageGridWidgetGroup group: imageGridWidgetGroups.values()) {
            group.populate();
        }
    }

    protected ThumbnailWidget makeCellWidget(TableData.Row row) {
        String background = GRID_BACKGROUND;
        if (imageGridPanelHandler != null) background = imageGridPanelHandler.getBackground();
        if (requiresResize()) {
            return new ThumbnailWidget(getThumbnailDesc(row), getThumbnailUrl(row), background, requiresResize(), columns);
        } else {
            return new ThumbnailWidget(getThumbnailDesc(row), getThumbnailUrl(row), background);
        }
    }

    protected void handleDoubleClick(TableData.Row row) {
        VerticalPanel vp = new VerticalPanel();
        final Image previewImage = new Image(getFullSizeUrl(row));
        final HTML caption = new HTML(getPopUpCaption(row));
        String title = getThumbnailDesc(row).replace("<em>", "").replace("</em>", "");

        caption.setWidth("320px");

        previewImage.addLoadHandler(new LoadHandler() {
            public void onLoad(LoadEvent ev) {
                caption.setWidth(previewImage.getWidth() + "px");
            }
        });
        GwtUtil.setStyle(vp, "margin", "8px");

        vp.setCellHorizontalAlignment(previewImage, HasHorizontalAlignment.ALIGN_CENTER);
        vp.setCellVerticalAlignment(previewImage, HasVerticalAlignment.ALIGN_MIDDLE);
        vp.add(previewImage);
        vp.add(caption);

        PopupPane popupPane = new PopupPane(title, vp, PopupType.STANDARD, false, false);

        popupPane.show();
    }

    protected int getResizedWidth() {
        int px = 64; // min width should be no less than min plot width
        Widget parent = mainPanel;

        if (parent != null) {
            px = parent.getOffsetWidth() / columns;
            px = px > 74 ? px - 10 : 64;
        }
        return px;
    }

    private void showMessage(String text) {
        HTML msg = new HTML(text);

        GwtUtil.setStyle(msg, "padding", "8px");
        GwtUtil.setStyle(msg, "fontSize", "120%");
        GwtUtil.setStyle(msg, "textAlign", "center");
        GwtUtil.setStyle(msg, "fontWeight", "bold");

        mainPanel.add(msg);

    }

    private void saveScrollPosition() {
        Element body = getElement();
        int scrollX = body.getScrollLeft();
        int scrollY = body.getScrollTop();
        int viewWidth = DOM.getElementPropertyInt(body, "clientWidth");
        int viewHeight = DOM.getElementPropertyInt(body, "clientHeight");

        if (currentViewHeight != viewHeight) {
            currentViewHeight = viewHeight;
        }

        if (currentViewWidth != viewWidth) {
            currentViewWidth = viewWidth;
        }

        if (currentScrollX != scrollX) {
            currentScrollX = scrollX;
        }

        if (currentScrollY != scrollY) {
            currentScrollY = scrollY;
        }

        if (lockrelated != null && mpwList.size() > 0) {
            lockrelated = mpwList.get(0).getGroup().getLockRelated();
        }
    }

    private void restoreScrollPositions() {
        Element body = getElement();
        int scrollX = body.getScrollLeft();
        int scrollY = body.getScrollTop();
        int viewWidth = DOM.getElementPropertyInt(body, "clientWidth");
        int viewHeight = DOM.getElementPropertyInt(body, "clientHeight");
        if (currentViewHeight == viewHeight && currentViewWidth == viewWidth) {
            if (currentScrollX != scrollX) {
                setHorizontalScrollPosition(currentScrollX);
            }

            if (currentScrollY != scrollY) {
                setScrollPosition(currentScrollY);
            }
        }
    }

    private MiniPlotWidget getMiniPlotWidget() {
        return getMiniPlotWidget(nextMpw++);
    }

    private MiniPlotWidget getMiniPlotWidget(int index) {

        if (index >= mpwList.size()) {
            for (int i = mpwList.size(); i <= index; i++) {
                createAndAddMiniPlotWidget();
            }
//            setupCatalog();
        }

        MiniPlotWidget mpw = mpwList.get(index);
        AllPlots.getInstance().setStatus(mpw, AllPlots.PopoutStatus.Enabled);
        return mpw;
    }

    private void createAndAddMiniPlotWidget() {
        String groupName = getPlotWidgetGroupValue();
        if (groupName==null) groupName = "GridGroup:" + this.hashCode();
        MiniPlotWidget mpw = new MiniPlotWidget(groupName);
        mpw.setErrorDisplayHandler(plotError);
        mpwList.add(mpw);
        mpw.setEnableChecking(checkingEnabled);
        mpw.setBoxSelection(true);

    }

    private String getWidgetGroupKey(String title) {
        String key = "";
        if (groupingParams.containsKey(keywords)) {

            for (String k: imageGridWidgetGroups.keySet()) {
                if (title.contains(k)) {
                    key = k;
                    break;
                }
            }
        } else if (groupingParams.containsKey(groupBy)) {
            String param = groupingParams.get(groupBy);

            if (param.equals("firstWord")) {
                key = title.substring(0, title.indexOf(" "));
            } else if (param.equals("alphabeticPrefix ")) {
                key = title.substring(0, title.indexOf(" "));
            } else if (param.equals("numericPrefix")) {
                key = title.substring(0, title.indexOf(" "));
            } else if (param.equals("wholeWord")) {
                key = title;
            }
        } else if (groupingColumnKeyword != null) {
            key = title;
        }
        return key;
    }

    private void addWidget(Widget widget, String title) {
        String key = getWidgetGroupKey(title);
        if (!imageGridWidgetGroups.containsKey(key)) {
            String desc = null;
            String titleDesc = "TitleDesc." + title;
            if (titleDescMap!=null && titleDescMap.size()>0) {
                desc= titleDescMap.get(titleDesc);
            }
            if (desc==null && hasValidImageGridPreviewData()) {
                //empty string returned if _previewData does not contain specific key
                desc = _previewData.getTitleDesc(titleDesc);
            }
            imageGridWidgetGroups.put(key, new ImageGridWidgetGroup(key, desc));
        }
        imageGridWidgetGroups.get(key).addWidget(widget);
        /*List<Widget> list = getWidgetList(getWidgetGroupKey(title));
        list.add(widget);
        GwtUtil.setStyle(widget, "float", "left");
        GwtUtil.setStyle(widget, "cssFloat", "left");
        GwtUtil.setStyle(widget, "styleFloat", "left");*/
    }

    private void addWidget(ThumbnailWidget widget, TableData.Row row) {
        widget.addClickHandler(imageGridPanelHandler);
        widget.addDoubleClickHandler(imageGridPanelHandler);
        //widget.addErrorHandler(imageGridPanelHandler);
        widget.addMouseOutHandler(imageGridPanelHandler);
        widget.addMouseOverHandler(imageGridPanelHandler);
        addWidget(widget, getGroupingTag(row));
        addToMaxRowWidth(getGroupingTag(row), 128, 10);
        widget.addStyleName("shadow");
                    GwtUtil.setStyles(widget, "margin", "2px",
                        "background","#E5E5E5 url(images/bg_boxgradient.png) repeat-x");
    }

    private void addMiniPlotWidget(MiniPlotWidget mpw, TableData.Row row) {
        float zoomLevel = 1.0F;

        String title = getThumbnailDesc(row);
        String url = getThumbnailUrl(row);

        if (row.getValue("initial_zoom_level") != null)
            zoomLevel = (Float) row.getValue("initial_zoom_level");

        WebPlotRequest req = WebPlotRequest.makeURLPlotRequest(url);

        req.setInitialZoomLevel(zoomLevel);
        req.setInitialRangeValues(new RangeValues());

        addWebPlotRequestWidget(mpw, req, title, getGroupingTag(row),
                getColumnValue(row, eventWorkerKeyword), getColumnValue(row, allEventWorkerKeyword));
    }

    private void addWebPlotRequestWidget(MiniPlotWidget mpw, WebPlotRequest req) {
        addWebPlotRequestWidget(mpw, req, null, null, null, null);
    }

    private void addWebPlotRequestWidget(MiniPlotWidget mpw, TableData.Row row) {
        WebPlotRequest req = WebPlotRequest.parse(getWebPlotRequest(row));

        if (hasValidImageGridPreviewData()) {
            Integer zoomToWidth = _previewData.getZoomToWidth();
            if (zoomToWidth != null && zoomToWidth > 0) {
                req.setZoomToWidth(zoomToWidth);
            }
        }
        String title = getThumbnailDesc(row);
        addWebPlotRequestWidget(mpw, req, title, getGroupingTag(row),
                getColumnValue(row, eventWorkerKeyword), getColumnValue(row, allEventWorkerKeyword));
    }

    private void addWebPlotRequestWidget(MiniPlotWidget mpw, WebPlotRequest req, String title, String groupTag,
                                         String eventWorkId, String allEventWorkers) {
        int minWidth = 64;
        int minHeight = 64;

        VerticalPanel panel = new GroupedVerticalPanel(groupTag);

        addWidget(panel, groupTag);

        if (title != null) req.setTitle(title);

        mpw.setMinSize(minWidth, minHeight);

        if (!req.containsParam(WebPlotRequest.SHOW_SCROLL_BARS)) req.setShowScrollBars(getUseScrollBars());
        mpw.getOps().plot(req, false, new WebPlotCallback(mpw, panel, title, req, eventWorkId, allEventWorkers));
        plottingCnt++;
//        batch.add(req,mpw,new WebPlotCallback(mpw, panel, title));
//        if (batch.size()>=4) batch.fire();
        panelMap.put(panel, mpw);
    }

    private void fireBatch() {

    }

    private String getGroupingTag(TableData.Row row) {
        String retval = null;

        if (groupingColumnKeyword != null)
            retval = String.valueOf(row.getValue(groupingColumnKeyword));
        else {
            retval = getWidgetGroupKey(getThumbnailDesc(row));
        }

        return retval;
    }

    private String getColumnValue(TableData.Row row, String columnKeyword) {
        String retval = null;

        if (columnKeyword != null) retval = String.valueOf(row.getValue(columnKeyword));
        return retval;
    }


    private String getThumbnailUrl(TableData.Row row) {
        String s = String.valueOf(row.getValue(thumbnailUrlColumnKeyword));
        if (!s.startsWith("http")) {
            s = GWT.getModuleBaseURL() + s;
        }
        return s;
    }

    private String getThumbnailDataType(TableData.Row row) {
        return String.valueOf(row.getValue(dataTypeColumnKeyword));
    }

    private String getThumbnailDesc(TableData.Row row) {
        return String.valueOf(row.getValue(thumbnailTitleColumnKeyword));
    }

    private String getFullSizeUrl(TableData.Row row) {
        return String.valueOf(row.getValue(fullSizeUrlColumnKeyword));
    }

    private String getSourceId(TableData.Row row) {
        String retval = String.valueOf(row.getValue(sourceIdColumnKeyword));
        if (retval.equals("null")) retval = "";
        return retval;
    }

    private String getWebPlotRequest(TableData.Row row) {
        return String.valueOf(row.getValue(thumbnailUrlColumnKeyword));
    }

    private void plotSuccess(WebPlot plot) {
    }

    private TableData.Row getRowValue(int row) {
        if (rowValues.size() <= row) {
            return null;
        }
        return rowValues.get(row);
    }

    private TableData.Row getRow(Widget widget) {
        int idx = findIndex(widget);
        return getRowValue(idx);
    }

    private int getMouseOverAt() {
        return this.mouseOverAt;
    }

    private void setMouseOverAt(Widget widget) {
        this.mouseOverAt = findIndex(widget);
    }

    private int getCurrentRow() {
        return this.currentRow;
    }

    private void setCurrentRow(Widget widget) {
        setCurrentRow(findIndex(widget));
    }

    private void setCurrentRow(int cur) {
        if (cur != this.currentRow) {
            this.currentRow = cur;
            //if (dataSet.getSelected().size()>0) {
            //    dataSet.deselectAll();
            //}
            //dataSet.select(this.currentRow );
            //todo: fireTablePreviewEventHubEvent(EventHub.ON_ROWHIGHLIGHT_CHANGE);
        }
    }

    // --------------- Define PopUp URL and title ---------------
    private String getPopUpCaption(TableData.Row row) {
        String caption = getSourceId(row);

        if (caption.length() > 0) caption += ": ";
        caption += getThumbnailDesc(row);
        return caption;
    }

    // --------------- find widget index ---------------
    private static int findIndex(Widget cell) {
        int retval = ((ComplexPanel) cell.getParent()).getWidgetIndex(cell);
        Widget w;
        for (int i = 0; i < ((ComplexPanel) cell.getParent()).getWidgetIndex(cell); i++) {
            w = ((ComplexPanel) cell.getParent()).getWidget(i);
            if (w instanceof HTML) retval--;
        }
        return retval;
    }

    private boolean parseBoolean(String s) {
        String value = s.toLowerCase().trim();
        return value.equals("t") || value.equals("true");
    }

    public void onShow() {
        for (MiniPlotWidget mpw : mpwList) {
            mpw.setActive(true);
        }
        if (mpwList.size() > 0) {
            mpwList.get(0).getGroup().setFloatingToolbarShowing(true);
        }
    }

    public void onHide() {
        for (MiniPlotWidget mpw : mpwList) {
            mpw.setActive(false);
        }
        if (mpwList.size() > 0) {
            mpwList.get(0).getGroup().setFloatingToolbarShowing(false);
        }
    }

    // MaxRowWidth methods
    private void addToMaxRowWidth(String groupTag, int width, int offset) {
        if (!rowWidths.containsKey(groupTag)) {
            rowWidths.put(groupTag, width+offset);
        } else
            rowWidths.put(groupTag, rowWidths.get(groupTag)+width+offset);
    }

    private void addToMaxRowWidth(String groupTag, int width) {
        addToMaxRowWidth(groupTag, width, 10);
    }

    private void clearMaxRowWidth() {
        if (rowWidths!=null) {
            rowWidths.clear();
        }
    }

    private int getMaxRowWidth() {
        int retval = 0;
        for (int i: rowWidths.values()) {
            if (i> retval) retval = i;
        }        
        return retval;
    }

    private void applyMaxRowWidth() {
        int px = getMaxRowWidth();
        if (px > 0) {
            Widget w = getMainPanel();
            GwtUtil.setStyle(w, "maxWidth", Integer.toString(px) + "px");
            GwtUtil.setStyle(w, "width", Integer.toString(px) + "px");
        }
    }
    
    private void addLineBreak() {
        Widget itemW = new HTML();
        GwtUtil.setStyle(itemW, "clear", "both");
        getMainPanel().add(itemW);
    }

    private static void setFloatLeft(Widget w) {
        GwtUtil.setStyle(w, "float", "left");
        GwtUtil.setStyle(w, "cssFloat", "left");
        GwtUtil.setStyle(w, "styleFloat", "left");
    }
    
//---- ImageGridPanelHandler class ----
    private class ImageGridPanelHandler implements ClickHandler, DoubleClickHandler,
                                                   ErrorHandler, MouseOverHandler, MouseOutHandler {
        private boolean doHilite = true;
        private boolean doDoubleClick = true;
        private String hiliteBackground = "#bde";
        private String hiliteBorder = "#abc";
        private String normalBackground = GRID_BACKGROUND_DEFAULT_COLOR;

        public void enableSelectionHiLite(boolean enable) {
            doHilite = enable;
        }

        public void enableSelectionDoubleClick(boolean enable) {
            doDoubleClick = enable;
        }

        public String getBackground() {

            return normalBackground;
        }

        public void setBackground(String rgb) {
            normalBackground = rgb;
        }

        public void setHiLiteBackground(String rgb) {
            hiliteBackground = rgb;
        }

        public void setHiLiteBorder(String rgb) {
            hiliteBorder = rgb;
        }

        private Timer mouseOverTimer = new Timer() {
            public void run() {
                BasicImageGrid.this.setCurrentRow(BasicImageGrid.this.getMouseOverAt());
            }
        };

        public void onDoubleClick(DoubleClickEvent event) {
            TableData.Row row = BasicImageGrid.this.getRow((Widget) event.getSource());
            handleDoubleClick(row);
        }

        public void onClick(ClickEvent event) {
            if (doDoubleClick) {
                Object source = event.getSource();
                BasicImageGrid.this.setCurrentRow((Widget) source);
            }
        }

        public void onMouseOver(MouseOverEvent event) {
            Widget widget = (Widget) event.getSource();
            if (doHilite) {
                GwtUtil.setStyle(widget, "backgroundColor", hiliteBackground);
                GwtUtil.setStyle(widget, "border", "1px solid " + hiliteBorder);
            }
            BasicImageGrid.this.setMouseOverAt(widget);
            mouseOverTimer.schedule(1500);
        }

        public void onMouseOut(MouseOutEvent event) {
            Widget widget = (Widget) event.getSource();
            if (doHilite) {
                GwtUtil.setStyle(widget, "backgroundColor", normalBackground);
                GwtUtil.setStyle(widget, "border", "1px solid " + normalBackground);
            }
            mouseOverTimer.cancel();
        }

        public void onError(ErrorEvent ev) {
            Object src = ev.getSource();

            if (src instanceof Image) {
                Image image = (Image) src;
                image.setUrl("images/blank_image_icon.png");
            }
        }
    }

//---- ImageGridWidgetGroup class ----
    private class ImageGridWidgetGroup {
        List<Widget> widgets = null;
        String groupTitle = null, groupDesc = null;

        public ImageGridWidgetGroup(String title, String desc) {
            this.groupTitle = title;
            this.groupDesc = desc;
        }

        public List<Widget> getWidgetsAsList() {
            if (widgets==null) widgets = new ArrayList<Widget>();
            return widgets;
        }

        public void addWidget(Widget w) {
            if (widgets==null) widgets = new ArrayList<Widget>();
            widgets.add(w);
        }

        public void clear() {
            widgets.clear();
        }

        public int populate() {
            int px, counter=0;
            Widget label, desc;

            if (!getGroupingShowLabel().equals("OFF")) { //add group title
                label = new HTML(groupTitle);
                GwtUtil.setStyle(label, "padding", "4px");
                GwtUtil.setStyle(label, "fontSize", "120%");
                GwtUtil.setStyle(label, "textShadow", "#888 0px 2px 2px");
                GwtUtil.setStyle(label, "textAlign", "left");
                GwtUtil.setStyle(label, "fontWeight", "bold");
                setFloatLeft(label);
                getMainPanel().add(label);
                if (groupDesc != null) {
                    if (!getGroupingShowLabel().equals("TOP")) addLineBreak();
                    desc = new HTML(groupDesc);
                    GwtUtil.setStyle(desc, "padding", "4px");
                    GwtUtil.setStyle(desc, "fontSize", "100%");
                    GwtUtil.setStyle(desc, "textAlign", "left");
                    setFloatLeft(desc);
                    getMainPanel().add(desc);
                }
            }

            if (getGroupingShowLabel().equals("TOP")) addLineBreak();

            px = getResizedWidth() - 8;
            for (Widget w: widgets) {
                if (requiresResize()) {
                    GwtUtil.setStyle(w, "maxWidth", Integer.toString(px) + "px");
                    GwtUtil.setStyle(w, "width", Integer.toString(px) + "px");
                }
                setFloatLeft(w);
                getMainPanel().add(w);
                counter++;
                if (columns>1 && counter%columns==0) {
                    addLineBreak();
                }
            }
            addLineBreak();

            return counter;
        }

        public void resize() {
            int px = getResizedWidth();
            for (Widget w : widgets) {
                if (!(w instanceof HTML)) {
                    VerticalPanel vp = ((VerticalPanel) w);
                    GwtUtil.setStyle(vp, "maxWidth", Integer.toString(px) + "px");
                    GwtUtil.setStyle(vp, "width", Integer.toString(px) + "px");
                    GwtUtil.setStyle(vp, "clear", "none");

                    for (Iterator<Widget> i = vp.iterator(); i.hasNext(); ) {
                        Widget c = i.next();
                        try {
                            if (panelMap.containsKey(vp)) {
                                int height = panelMap.get(vp).getCurrentPlot().getScreenHeight() + MPW_HEADER_HEIGHT;
                                int width = px - 8;
                                vp.clear();
                                vp.add(panelMap.get(vp).makeFixedSizeContainer(width, height, false));
                                vp.addStyleName("shadow");
                                GwtUtil.setStyles(vp, "margin", "2px",
                                        "background","#E5E5E5 url(images/bg_boxgradient.png) repeat-x");
                            } else {
                                GwtUtil.setStyle(c, "maxWidth", Integer.toString(px) + "px");
                                GwtUtil.setStyle(c, "width", Integer.toString(px) + "px");
                            }
                        } catch (NullPointerException e) {
                            //todo: handle null pointer exception
                        }
                    }
                }
            }
        }
    }

//---- WebPlotCallback class ----
    private class WebPlotCallback implements AsyncCallback<WebPlot> {
        MiniPlotWidget _mpw = null;
        String _title = null;
        VerticalPanel _panel = null;
        String _eventWorkerId = null;
        String _allEventWorkers = null;
        WebPlotRequest _req = null;

        public WebPlotCallback(MiniPlotWidget mpw, VerticalPanel panel, String title, WebPlotRequest req,
                               String eventWorkerId, String allEventWorkers) {
            _mpw = mpw;
            _panel = panel;
            _title = title;
            _req = req;
            _eventWorkerId = eventWorkerId;
            _allEventWorkers = allEventWorkers;
        }

        public void onFailure(Throwable caught) {
            plotDone();
            updatePlotWidgetGroupStatus(false);
            int width = 128;
            int height = 128;
            if (_previewData.getHideFailurePlot()) return;

            if (requiresResize()) {
                width = getResizedWidth() - 4;
                height = lastThumbnailHeight;
            } else if (_req.getZoomToWidth()>0) {
                width = _req.getZoomToWidth();
                height = _req.getZoomToWidth();
            }
            _panel.add(_mpw.makeFailureMessage("No Image returned.", width, height + MPW_HEADER_HEIGHT, false));
            _panel.addStyleName("shadow");
            GwtUtil.setStyles(_panel, "margin", "2px",
                    "background","#E5E5E5 url(images/bg_boxgradient.png) repeat-x");

            updateMaxRowWidth(width);
        }

        public void onSuccess(WebPlot plot) {
            if (plot != null && _panel != null && _mpw != null) {
                int width = plot.getScreenWidth();
                int height = plot.getScreenHeight();

                if (requiresResize()) {
                    width = getResizedWidth() - 4;
                } else if (_req.getZoomToWidth()>0) {
                    width = _req.getZoomToWidth();
                    height = _req.getZoomToWidth();
                }
                if (_previewData != null) _previewData.postPlot(_mpw, plot);
                _panel.add(_mpw.makeFixedSizeContainer(width, height + MPW_HEADER_HEIGHT, false));
                _panel.addStyleName("shadow");
                GwtUtil.setStyles(_panel, "margin", "2px",
                        "background","#E5E5E5 url(images/bg_boxgradient.png) repeat-x");
//                if (mpwList.lastIndexOf(_mpw)==(mpwList.size()-1)) {
//                }
                if ((_eventWorkerId != null || _allEventWorkers !=null)  && hub != null) {
                    setupEventWorker(_eventWorkerId, _allEventWorkers, _mpw.getPlotView(), plot);
                }
//                setupDrawing(_mpw.getPlotView());
                _mpw.getGroup().enableFloatVisBar(BasicImageGrid.this);
                lastThumbnailHeight = height;
                plotDone();
                updateMaxRowWidth(width);
                updatePlotWidgetGroupStatus(true);
            }
        }

        private void updatePlotWidgetGroupStatus(boolean success) {
            if (!_previewData.getUpdatePlotWidgetGroupStatus()) return;

            StringBuffer buffer = new StringBuffer();

            if (success)
                successPlots++;
            else
                failurePlots++;
            buffer.append("Success: ");
            buffer.append(successPlots);
            buffer.append(", Failure: ");
            buffer.append(failurePlots);

            _mpw.getGroup().updateFloatingStatus(new String(buffer), BasicImageGrid.this);
        }

        private void updateMaxRowWidth(int width) {
            if (!(_panel instanceof GroupedVerticalPanel)) return;

            addToMaxRowWidth(((GroupedVerticalPanel)_panel).getGroupTag(), width);

            if (plottingCnt == 0 && isGroupingInRow()) {
                applyMaxRowWidth();
                clearMaxRowWidth();
            }
        }

        private void plotDone() {
            plottingCnt--;
            if (plottingCnt == 0) {
                restoreScrollPositions();
                _mpw.getGroup().setLockRelated(getLockRelated());
                setupCatalog();
                eventManager.fireEvent(new WebEvent(this, ON_ALL_PLOTS_DONE));
                AllPlots.getInstance().setSelectedWidget(AllPlots.getInstance().getActiveList().get(0));
                resize();
            }
        }
    }

//---- PlotWidgetEventListener class ----
    private class PlotWidgetEventListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            handlePlotEvent(ev);
        }
    }

//---- PostPlotWidgetEventListener class ----
    private class PostPlotWidgetEventListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            handlePostPlotEvent(ev);
        }
    }

//---- GroupedVerticalPanel class ----
    private static class GroupedVerticalPanel extends VerticalPanel {
        String groupTag= null;
        public GroupedVerticalPanel(String groupTag) {
            super();
            this.groupTag = groupTag;
        }

        public String getGroupTag() {return groupTag;}
    }

    private class EventWorkerResolver {
        static final String KEYWORD = "col_keyword";
        static final String MAPPING = "col_mapping";

        String methodTag = null;
        HashMap<String, String> map = null;

        public EventWorkerResolver(String meta) {
            parseMetaData(meta);
        }

        private void parseMetaData(String meta) {
            String key, value;
            for (String tag : meta.split("&")) {
                key = getKey(tag, "=");
                value = getValue(tag, "=");
                if (key.equals("method"))
                    methodTag = value;
                else if (key.equals("map")) {
                    map = new HashMap<String, String>();
                    for (String duo : value.split(";")) {
                        map.put(getKey(duo, ";"), getValue(duo, ";"));
                    }
                }
            }

        }

        private String getKey(String tag, String delimiter) {
            return tag.split(delimiter)[0].trim();
        }

        private String getValue(String tag, String delimiter) {
            return tag.split(delimiter)[1].trim();
        }

        public String[] ResolveEventWorkerId(TableData.Row row) {
            if (methodTag.equals(KEYWORD)) {
                return null;
            } else if (methodTag.equals(MAPPING)) {
                return null;
            }
            return null;
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