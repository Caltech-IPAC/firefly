package edu.caltech.ipac.firefly.ui.imageGrid;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.gen2.table.client.TableModel;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.Component;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.StatefulWidget;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageChangeEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageChangeHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageCountChangeEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageCountChangeHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageLoadEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageLoadHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PagingFailureEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PagingFailureHandler;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.Loader;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: May 28, 2010
 * Time: 4:04:35 PM
 * To change this template use File | Settings | File Templates.
 */
//todo: change all DOM.setStyleAttribute() statements to CSS
public abstract class ImageGridPanel extends Component implements StatefulWidget {
    private static final String HIGHLIGHTED_ROW_IDX = "GridPanel_HLIdx";
    private static int maxRowLimit = Application.getInstance().getProperties().getIntProperty(
                                     "ImageGridPanel.max.row.Limit", 100000);
    private static final String TOO_LARGE_MSG = "Sorting is disabled on table with more than " +
                                            NumberFormat.getFormat("#,##0").format(maxRowLimit) + " rows.";
    public static final Name ON_PAGE_LOAD        = new Name("ImageGridPanel.onPageLoad",
                                                        "After a page is loaded.");
    public static final Name ON_PAGE_CHANGE      = new Name("ImageGridPanel.onPageChange",
                                                        "Page change; before a new page is loaded.");
    public static final Name ON_PAGE_ERROR       = new Name("ImageGridPanel.onPageError",
                                                        "Page load error.");
    public static final Name ON_PAGECOUNT_CHANGE = new Name("ImageGridPanel.onPageCountChange",
                                                        "The number of pages changed.");
    public static final Name ON_STATUS_UPDATE    = new Name("ImageGridPanel.onStatusUpdate",
                                                        "Called when table's status is updated");
    private String stateId = "GRID";
    private String name;
    private String shortDesc;
    private DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.PX);
    private DataSetTableModel dataModel;
    private TablePreviewEventHub hub;

//    private TableDataView dataSet;

    private PagingToolbar pagingBar = null;
    private BasicPagingImageGrid basicPagingImageGrid = null;
//    private HTML centerStatus;
    private int mouseOverAt = 0;
    private int currentRow = 0;
    private boolean tableTooLarge = false;
    private boolean tableNotLoaded = true;
    private ImageGridPanelHandler imageGridPanelHandler= new ImageGridPanelHandler();

    public ImageGridPanel(Loader<TableDataView> loader) {
        this("untitled", loader);    
    }

    public ImageGridPanel(String name, Loader<TableDataView> loader) {
        setInit(false);
        this.name = name;                           
        dataModel = new DataSetTableModel(loader);
        initWidget(mainPanel);
        mainPanel.setSize("100%", "100%");
        DOM.setStyleAttribute(mainPanel.getElement(), "borderSpacing", "0px");
        sinkEvents(Event.ONMOUSEOVER);
    }

    abstract public Widget makeCellWidget(TableData.Row row);
    abstract public void handleDoubleClick(TableData.Row row);

    public BasicPagingImageGrid getImageGrid() {
        return basicPagingImageGrid;
    }

    public void addHandlers() {
        basicPagingImageGrid.addClickHandler(imageGridPanelHandler);
        basicPagingImageGrid.addDoubleClickHandler(imageGridPanelHandler);
        basicPagingImageGrid.addMouseOverHandler(imageGridPanelHandler);
        basicPagingImageGrid.addMouseOutHandler(imageGridPanelHandler);
        basicPagingImageGrid.addErrorHandler(imageGridPanelHandler);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public void setMouseOverAt(Widget widget) {
        this.mouseOverAt = findIndex(widget);
    }

    public int getMouseOverAt() {
        return this.mouseOverAt;
    }

    public void setCurrentRow(Widget widget) {
        setCurrentRow(findIndex(widget));
    }

    public void setCurrentRow(int cur) {
        if (cur != this.currentRow) {
            this.currentRow = cur;
            if (dataModel.getCurrentData().getSelected().size()>0) {
                dataModel.getCurrentData().deselectAll();
            }
            dataModel.getCurrentData().select(this.currentRow );
            fireTablePreviewEventHubEvent(TablePreviewEventHub.ON_ROWHIGHLIGHT_CHANGE);
        }
    }

    public TableData.Row getRow(Widget widget) {
        int idx = findIndex(widget);
        return basicPagingImageGrid.getRowValue(idx);
    }

    public int getCurrentRow() {
        return this.currentRow;
    }

    public DataSetTableModel getDataModel() {
        return dataModel;
    }

    public void setTablePreviewEventHub(TablePreviewEventHub hub) {
        this.hub = hub;
    }

    public void fireTablePreviewEventHubEvent(Name eventName) {
        hub.getEventManager().fireEvent(new WebEvent(this, eventName));
    }

    public void init() {
        init(null);
    }

    public void init(final AsyncCallback<Integer> callback) {


        AsyncCallback<TableDataView> cb = new AsyncCallback<TableDataView>() {
            public void onFailure(Throwable caught) {
                // not sure what to do with this.
                // need to set init to true so other code can continue..
                // but, has no way of passing the error.
                try {
                    if (callback != null) {
                        callback.onFailure(caught);
                    }
                } finally {
                    setInit(true);
                }
            }

            public void onSuccess(TableDataView result) {
                try {
                    layout();
                    addListeners();
                    if (GwtUtil.isOnDisplay(ImageGridPanel.this)) {
                        onShow();
                    }
                    DeferredCommand.addCommand(new Command() {
                        public void execute() {
                            basicPagingImageGrid.gotoFirstPage();
                        }
                    });
                    ImageGridPanel.this.setInit(true);
                } finally {
                    if (callback != null) {
                        callback.onSuccess(dataModel.getTotalRows());
                    }
                }
            }
        };
        // load up the first page of data.. upon success, creates and initializes the tablepanel.
        dataModel.getData(cb, 0);
    }

    public boolean isTableLoaded() {
        return !tableNotLoaded;
    }

    //====================================================================
    //  Implementing StatefulWidget
    //====================================================================
    public String getStateId() {
        return stateId;
    }

    public void setStateId(String id) {
        stateId = id;
    }

    public void recordCurrentState(Request req) {
        int ps = basicPagingImageGrid.getPageSize();
        int startIdx = basicPagingImageGrid.getCurrentPage() * basicPagingImageGrid.getPageSize();

        if (ps > 0) {
            req.setParam(getStateId() + "_" + Request.PAGE_SIZE, String.valueOf(ps));
        }
        if (startIdx > 0) {
            req.setParam(getStateId() + "_" + Request.START_IDX, String.valueOf(startIdx));
        }
    }

    public void moveToRequestState(final Request req, final AsyncCallback callback) {
        int rps = Math.max(0, req.getIntParam(getStateId() + "_" + Request.PAGE_SIZE));
        int lps = Math.max(0, dataModel.getPageSize());
        int rsIdx = Math.max(0, req.getIntParam(getStateId() + "_" + Request.START_IDX) );
        int lsIdx = Math.max(0, basicPagingImageGrid.getCurrentPage() * basicPagingImageGrid.getPageSize());
        List<String> filters = Request.parseFilters(req.getParam(getStateId() + "_" + Request.FILTERS));
        final SortInfo sortInfo = SortInfo.parse(req.getParam(getStateId() + "_" + Request.SORT_INFO));

        boolean doRefresh = (rps != 0 && rps != lps) || (rsIdx != lsIdx);
        doRefresh = doRefresh || !Request.toFilterStr(filters).equals(Request.toFilterStr(dataModel.getFilters()));
        doRefresh = doRefresh || !String.valueOf(sortInfo).equals(String.valueOf(dataModel.getSortInfo()));

        if (doRefresh) {
            rps = rps == 0 ? dataModel.getPageSize() : rps;
            int page = rsIdx/rps;
            int idx = req.getIntParam(getStateId() + "_"  + HIGHLIGHTED_ROW_IDX);
            dataModel.setFilters(filters);
            dataModel.setSortInfo(sortInfo);

            gotoPage(page, rps, idx);
            getEventManager().addListener(ON_PAGE_LOAD, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    ImageGridPanel.this.getEventManager().removeListener(ON_PAGE_LOAD,this);
                    DeferredCommand.addCommand(new Command(){
                        public void execute() {
                            onMoveToReqStateCompleted(req);
                            callback.onSuccess(null);
                        }
                    });
                }
            });
        } else {
            onMoveToReqStateCompleted(req);
            callback.onSuccess(null);
        }
    }

    protected void onMoveToReqStateCompleted(Request req) {}

    public boolean isActive() {
        return GwtUtil.isOnDisplay(this);
    }

    public void gotoPage(int page) {
        gotoPage(page, dataModel.getPageSize(), 0);
    }

    public void gotoPage(int page, int pageSize, final int hlRowIdx) {
        dataModel.clearCache();

        if (dataModel.getPageSize() != pageSize) {
            dataModel.setPageSize(pageSize);
            basicPagingImageGrid.setPageSize(pageSize);
        }

        page = Math.min(page, dataModel.getTotalRows()/pageSize);
        basicPagingImageGrid.getTableModel().setRowCount(TableModel.UNKNOWN_ROW_COUNT);
        basicPagingImageGrid.gotoPage(page, true);
        WebEventListener doHL = new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                ImageGridPanel.this.getEventManager().removeListener(ON_PAGE_LOAD,this);
            }
        };
        getEventManager().addListener(ON_PAGE_LOAD, doHL);
    }

    protected void layout() {
        basicPagingImageGrid = makeImageGrid();

        addHandlers();

        pagingBar = new PagingToolbar(this);

        mainPanel.addSouth(pagingBar, 32);
        mainPanel.add(basicPagingImageGrid.getDisplay());
    }

    protected BasicPagingImageGrid newImageGrid(MutableTableModel<TableData.Row> model, TableDataView dataset) {
        return new BasicPagingImageGrid(this, model, dataset);
    }

    protected BasicPagingImageGrid makeImageGrid() {
        final BasicPagingImageGrid grid = newImageGrid(dataModel, dataModel.getCurrentData());
        //cachedModel.getModel().setImageGrid(grid);
        grid.setPageSize(dataModel.getPageSize());
        return grid;
    }

    public void updateTableStatus() {
        tableTooLarge = basicPagingImageGrid.getTableModel().getRowCount() > maxRowLimit;
        tableNotLoaded = !dataModel.getCurrentData().getMeta().isLoaded();

        if (tableTooLarge) {
            /*if (!centerStatus.getHTML().startsWith("<img")) {
                centerStatus.setHTML("<img src='images/gxt/exclamation16x16.gif' valign='bottom' title='" + TOO_LARGE_MSG +
                            "  Use filtering to narrow down your search results.'> &nbsp;" +
                            "Large Table: Sorting disabled");
            }*/
        } else if (tableNotLoaded) {
            //centerStatus.setHTML("<blink>Table Loading...</blink>");
        } else {
            //centerStatus.setHTML("");
        }
        getEventManager().fireEvent(new WebEvent<Boolean>(this, ON_STATUS_UPDATE, isTableLoaded()));
    }

    protected void addListeners() {
        // listen to table's events
        ImageGridPanel.this.basicPagingImageGrid.addPageChangeHandler(new PageChangeHandler(){
            public void onPageChange(PageChangeEvent event) {
                mask("Loading...", 200);
                getEventManager().fireEvent(new WebEvent(ImageGridPanel.this, ON_PAGE_CHANGE));
            }
        });
        basicPagingImageGrid.addPageCountChangeHandler(new PageCountChangeHandler(){
            public void onPageCountChange(PageCountChangeEvent event) {
                updateTableStatus();
                getEventManager().fireEvent(new WebEvent(ImageGridPanel.this, ON_PAGECOUNT_CHANGE));
            }
        });
        basicPagingImageGrid.addPageLoadHandler(new PageLoadHandler(){
            public void onPageLoad(PageLoadEvent event) {
                unmask();
                getEventManager().fireEvent(new WebEvent(ImageGridPanel.this, ON_PAGE_LOAD));
            }
        });
        basicPagingImageGrid.addPagingFailureHandler(new PagingFailureHandler(){
            public void onPagingFailure(PagingFailureEvent event) {
                unmask();
                getEventManager().fireEvent(new WebEvent(ImageGridPanel.this, ON_PAGE_ERROR));
            }
        });
    }

    private static int findIndex(Widget cell) {
        return ((ComplexPanel)cell.getParent()).getWidgetIndex(cell);
    }

    // ---------------------------- private class ----------------------------
    public class ImageGridPanelHandler implements ClickHandler, DoubleClickHandler,
                                                    ErrorHandler, MouseOverHandler,
                                                    MouseOutHandler {
        private Timer mouseOverTimer = new Timer() {
			public void run() {
                ImageGridPanel.this.setCurrentRow(ImageGridPanel.this.getMouseOverAt());
			}
		};

        public void onDoubleClick(DoubleClickEvent event)  {
            TableData.Row row = ImageGridPanel.this.getRow((Widget)event.getSource());
            handleDoubleClick(row);
        }

        public void onClick(ClickEvent event) {
            Object source = event.getSource();
            ImageGridPanel.this.setCurrentRow((Widget)source);
        }

        public void onMouseOver(MouseOverEvent event) {
            Widget widget = (Widget)event.getSource();
            DOM.setStyleAttribute(widget.getElement(), "backgroundColor", "#bde");
            DOM.setStyleAttribute(widget.getElement(), "border", "1px solid #abc");
            ImageGridPanel.this.setMouseOverAt(widget);
            mouseOverTimer.schedule(1500);
        }

        public void onMouseOut(MouseOutEvent event) {
            Widget widget = (Widget)event.getSource();
            DOM.setStyleAttribute(widget.getElement(), "backgroundColor", "#ddd");
            DOM.setStyleAttribute(widget.getElement(), "border", "1px solid #ddd");
            mouseOverTimer.cancel();
        }

        public void onError(ErrorEvent ev) {
            Object src = ev.getSource();

            if (src instanceof Image) {
                Image image = (Image)src;
                image.setUrl("images/blank_image_icon.png");
            }
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
