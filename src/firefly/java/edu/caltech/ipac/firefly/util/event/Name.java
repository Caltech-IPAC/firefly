/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util.event;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsNoExport;
import com.google.gwt.core.client.js.JsType;
import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;

/**
 * User: roby
 * Date: Dec 20, 2007
 * Time: 2:59:22 PM
 */


/**
 * @author Trey Roby
 */
@JsExport
@JsType
public class Name implements Serializable {

    public static final Name WINDOW_RESIZE = new Name("WindowResize", "This event is triggered when the browser resizes.");
    public static final Name APP_ONLOAD = new Name("AppOnLoad", "This event is triggered after the application is loaded.");


    public static final Name NEW_TABLE_RETRIEVED =
                                  new Name("NewTableRetrieved",
                                    "This event happens after a search returns a table, "+
                                    "data should be a Loader");

    public static final Name REQUEST_COMMAND_LAYOUT =
                                 new Name("RequestCommandLayout",
                                          "This event happens when request command layout has been completed");


    public static final Name SEARCH_RESULT_START =
                                 new Name("SearchResultStart",
                                          "This event happens when search results are removed from the display");

    public static final Name SEARCH_RESULT_END =
                                  new Name("SearchResultEnd",
                                           "This event happens after a search request came back with data, " +
                                           "and results displayed.");

    public static final Name ON_PACKAGE_SUBMIT =
                                    new Name("OnPackageSubmit",
                                            "This event happens before a package request is submitted.");

    public static final Name AREA_SELECTION =
                                  new Name("AreaSelected",
                                           "An area on a plot have been selected or cleared, " +
                                           "data should be boolean true for use initiated");

    public static final Name LINE_SELECTION =
            new Name("LineSelected",
                     "An line on a plot have been selected or cleared, " +
                             "data should be boolean true for use initiated");

    public static final Name POINT_SELECTION =
            new Name("PointSelected",
                     "An point on a plot have been selected or cleared, " +
                             "data should be boolean true for use initiated");



    public static final Name DATA_SELECTION_CHANGE =
            new Name("DataSelectionChange",
                     "The data selection on the plot changed, " +
                             "no data passed");

    public static final Name PLOT_ADDED =
                                  new Name("PlotAdded",
                                           "A plot added to a container Object, "+
                                           "data should be a the plot that was added " +
                                           "(posibly WebPlot).");

    public static final Name PLOT_REMOVED =
                                  new Name("PlotRemoved",
                                           "A plot removed from a container Object, "+
                                           "data should be a the plot that was removed " +
                                           "(posibly WebPlot).");

    public static final Name PLOT_REQUEST_COMPLETED =
            new Name("PlotRequestCompleted",
                     "a PlotRequest was successfully completed, "+
                             "the data should be the WebPlot object on sucess, null on fail");



    public static final Name PLOT_TASK_WORKING =
            new Name("PlotTaskWorking",
                     "Some set of task are operating on a WebPlot in this WebPlotView,"+
                             "data should be a the PlotView");

    public static final Name PLOT_TASK_COMPLETE =
            new Name("PlotTaskCompleted",
                     "All task have completed on a WebPlot in this WebPlotView,"+
                             "data should be a the PlotView");

    public static final Name ALL_PLOT_TASKS_COMPLETE =
            new Name("AllPlotTasksCompleted",
                     "All tasks of selected plots have completed.");

    public static final Name FITS_VIEWER_CHANGE =
            new Name("PlotWidgetChange",
                     "The Selected MiniPlotWidget changed, "+
                             "data should be a the new MiniPlotWidget");

    public static final Name LAYER_ITEM_UI_CHANGE =
            new Name("LayerItemUIChange",
                     "New Layer item UI element was loaded "+
                             "no data");

    public static final Name LAYER_ITEM_ADDED =
            new Name("LayerItemAdded",
                     "A WebLayerItem was added to the WebPlotViw, "+
                             "data should be a the the WebLayerItem object");

    public static final Name LAYER_ITEM_REMOVED =
            new Name("LayerItemRemoved",
                     "A WebLayerItem was removed from the WebPlotViw, "+
                             "data should be a the the WebLayerItem object");

    public static final Name LAYER_ITEM_ACTIVE =
            new Name("LayerItemActive",
                     "A WebLayerItem active status has changed, "+
                             "data should be a the the WebLayerItem object");

    public static final Name LAYER_ITEM_VISIBLE =
            new Name("LayerItemActive",
                     "A WebLayerItem visibility status has changed, "+
                             "data should be a true for visible, false for invisible, source should be WebLayerItem");


    // No events on preference sets
    //public static final Name PREFERENCE_UPDATE =
    //        new Name("PreferenceUpdate",
    //                "This event happens when preference has been set to a new value, "+
    //                        "data should be a string with the name of the updated preference "+
    //                        "or null for bulk updates.");



    public static final Name PRIMARY_PLOT_CHANGE =
            new Name("PrimaryPlotChange",
                    "the primarily displayed plot was changed, "+
                            "data should be a the old plot and the new plot" +
                            "(possibly WebPlot).");


    public static final Name VIEW_PORT_CHANGE =
            new Name("ViewPortChange",
                     "the view port of the plot was changed, "+
                             "data should be a the WebPlot");


    public static final Name REPLOT =
                                  new Name("Replot",
                                           "A plot was replotted, "+
                                           "data should be a class containing the plot and reason why" +
                                           "it was reploted (posibly ReplotDetails)");

    public static final Name ZOOM_LEVEL_BUTTON_PUSHED =
            new Name("ZoomButtonPushed",
                     "User activated a zoom command, "+
                             "data should be the command name");


    public static final Name PLOTVIEW_LOCKED =
            new Name("PlotviewLocked",
                     "A plotview was lock from plotting other images " +
                             "data should be a boolean true for locked, false for unlocked" );

    public static final Name FITS_VIEWER_ADDED =
            new Name("FitsViewerAdded",
                     "A new fits viewer widget was added");

    public static final Name FITS_VIEWER_REMOVED =
            new Name("FitsViewerRemoved",
                     "A new fits viewer widget was removed");

    public static final Name ALL_FITS_VIEWERS_TEARDOWN =
            new Name("AllFitsViewersCleared",
                     "All the fits viewer widgets where cleared");


    public static final Name GRID_ANNOTATION =
            new Name("GridAnnotation",
                    "a grid was added or removed, "+
                    "data should be a boolean, true is grid is on, false is grid is off");

    //================================

    public static final Name MONITOR_ITEM_CREATE =
            new Name("MonitorItemCreate",
                     "A new MonitorItem was created, "+
                             "data should be a class containing the MonitorItem");

    public static final Name MONITOR_ITEM_UPDATE =
            new Name("MonitorItemUpdate",
                     "A MonitorItem was updated, "+
                             "data should be a class containing the MonitorItem");

    public static final Name MONITOR_ITEM_REMOVED =
            new Name("MonitorItemRemoved",
                     "A MonitorItem was deleted, "+
                             "data should be a class containing the MonitorItem");



    public static final Name BG_MANAGER_STATE_CHANGED =
            new Name("BackgroundManagerStateChanged",
                     "The background manager's state has changed, "+
                             "data should be the new state");

    public static final Name BG_MANAGER_PRE_ANIMATE =
            new Name("BackgroundManagerStateChanged",
                     "The background manager's si about do to an animation, it should be visible"+
                             ", no data");


    //================================

    public static final Name SELECT_DIALOG_BEGIN_PLOT =
            new Name("SelectDialogBeginPlot", "a plot has been initiated by the plot dialog");

    public static final Name SELECT_DIALOG_CANCEL=
            new Name("SelectDialogCancel", "the use canceled trying the ImageSelectDialog");


    public static final Name REGION_CHANGE =
                                  new Name("RegionChange",
                                           "A Region was changed, "+
                                           "data should be which region was changed");
    public static final Name REGION_ADDED = new Name("RegionAdded",
                                           "A Region was added, "+
                                           "source is the region, data is the content added.");

    public static final Name REGION_REMOVED = new Name("RegionRemoved",
                                            "A Region was removed, "+
                                            "source is the region, data is the content removed.");

    public static final Name REGION_HIDE = new Name("RegionHide",
                                            "A Region was set hidden, "+
                                            "source is the region.");

    public static final Name REGION_SHOW = new Name("RegionShow", 
                                            "A Region was set visible, "+
                                            "source is the region.");

    public static final Name DROPDOWN_OPEN = new Name("DropdownOpen",
                                            "Fires after the drop down toolbar opens.");

    public static final Name DROPDOWN_CLOSE = new Name("DropdownClose",
                                            "Fires after the dropdown toolbar close.");

    public static final Name CHECKED_PLOT_CHANGE = new Name("CheckedPlotChange",
                                                       "Fires a plot is checked or unchecked. ,data will be a boolean");

    public static final Name ALL_CHECKED_PLOT_CHANGE = new Name("ALLCheckedPlotChange",
                                                            "Fires a All plots are checked or unchecked. ,data will be a boolean");
    public static final Name VIS_MENU_BAR_POP_SHOWING= new Name("VisMenuBarPopupShowing",
                                                                "the popup is showing or hidden, data will be a boolean");

    public static final Name SESSION_MISMATCH =
                                  new Name("SessionMismatch",
                                           "Session ID given by the server does not match with "+
                                           "the session ID assigned during loading");

    public static final Name EVENT_HUB_CREATED= new Name("EventHubCreated",
                                                         "new EventHub has been created, data will be the hub");


    public static final Name BYPASS_EVENT= new Name("BypassEvent", "Event to bypass checking");

    public static final Name DOWNLOAD_REQUEST_READY= new Name("DownloadRequestReady", "A DownloadRequest is ready to use.");

    public static final Name CATALOG_SEARCH_IN_PROCESS =
            new Name("CatalogSearchInProcess",
                     "A catalog search has started and will most likely return results");

    public static final Name WCS_SYNC_CHANGE=  new Name("WCSSyncChange",
                     "World coordinate system lock status change, data will be a boolean");

    public static final Name CROP=  new Name("Crop",
                                         "Plot was replaced with a cropped version, data will be the WebPlotView");

    public static final Name EVT_CONN_EST =  new Name("EVT_CONN_EST",
                                             "Event connection established.  Along with this event, you can expect connID and channel in the event's data. ie. {connID: val, channel: val}");

//====================================================================
//  A list of PUSH events available: {WEB_PLOT_REQUEST, REGION_FILE_NAME, TABLE_FILE_NAME, FITS_COMMAND_EXT }
    public static final Name PUSH_WEB_PLOT_REQUEST  =  new Name("PUSH_WEB_PLOT_REQUEST", "Load this webplot.  Expect WebPlotRequest as data");
    public static final Name PUSH_REGION_FILE       =  new Name("PUSH_REGION_FILE", "Overlay this region file.  Expect TableInfo as data");
    public static final Name PUSH_REMOVE_REGION_FILE=  new Name("PUSH_REMOVE_REGION_FILE", "remove a region.  Expect TableInfo as data");
    public static final Name PUSH_REGION_DATA       =  new Name("PUSH_REGION_DATA", "Overlay this region date.  Expect region data");
    public static final Name REMOVE_REGION_DATA     =  new Name("REMOVE_REGION_DATA", "Overlay this region date.  Expect region data");
    public static final Name PUSH_TABLE_FILE        =  new Name("PUSH_TABLE_FILE", "Load this table file.  Expect TableInfo as data");
    public static final Name PUSH_XYPLOT_FILE       =  new Name("PUSH_XYPLOT_FILE", "Load this table into XYPlotViewer. Expect XYPlot Viewer params");
    public static final Name PUSH_FITS_COMMAND_EXT  =  new Name("PUSH_FITS_COMMAND_EXT", "Add an extension command.  Expect ExtInfo as data");
    public static final Name PUSH_PAN               =  new Name("PUSH_PAN", "pan around");
    public static final Name PUSH_ZOOM              =  new Name("PUSH_ZOOM", "change zoom factor");
    public static final Name PUSH_RANGE_VALUES      =  new Name("PUSH_RANGE_VALUES", "change range values");
    public static final Name PUSH_ADD_MASK          =  new Name("PUSH_ADD_MASK", "add a masking layer to plot");
    public static final Name PUSH_REMOVE_MASK       =  new Name("PUSH_REMOVE_MASK", "remove a masking layer");
    public static final Name REPORT_USER_ACTION     =  new Name("REPORT_USER_ACTION", "report a user response");
    public static final Name REPORT_ALIVE           =  new Name("REPORT_ALIVE", "report if url is alive");
    public static final Name RESPONDED_ALIVE        =  new Name("RESPONDED_ALIVE", "responded if url is alive");

    public static final Name ACTION                 =  new Name("FLUX_ACTION", "an action message.");
//====================================================================


    private final String _name;
    private final String _desc;

    public Name(String name, String desc) {
        _name= name;
        _desc= desc;
    }

    @JsNoExport
    public Name() {
        this("unknown", "bad name.");
    }

    public String toString() { return "EventName: "+ _name + " - " + _desc;}

    @Override
    public int hashCode() {
        return _name == null ? 0 : _name.hashCode();
    }

    public String getName() {
        return _name;
    }

    public String getDesc() { return _desc; }
    
    public boolean equals(Object other) {
        boolean retval= false;
        if (other==this) {
            retval= true;
        }
        else if (other!=null && other instanceof Name) {
            Name en= (Name)other;
            retval= ComparisonUtil.equals(_name, en._name);
        }
        return retval;
    }

    /**
     * Should make Name into a string.
     * @param name
     * @return
     */
    public static Name parse(String name) {
        return new Name(name, "unknown");
    }
}

