/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.fftools;

import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.TableLoadHandler;
import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Usage: firefly.showTable(parameters, div)

 * parameters is an object attributes.  div is the div to load the table into.
 * below is a list of all possible parameters.
 *
 * parameters:
 * source      : required; location of the ipac table.  url or file path.
 * type        : basic, selectable, or chart; defaults to basic if not given
 * filters
 * sortInfo
 * pageSize
 * startIdx
 * tableOptions
 *
 * tableOptions:  option=true|false [,option=true|false]*
 * show-filter
 * show-popout
 * show-title
 * show-toolbar
 * show-options
 * show-paging
 * show-save
 * @version $Id: TableJSInterface.java,v 1.8 2012/12/11 21:10:01 tatianag Exp $
 */
public class TableJSInterface {
    public static final String TBL_TYPE = "type";
    public static final String TBL_SOURCE = "source";
    public static final String TBL_ALT_SOURCE = "alt_source";
    public static final String TBL_SORT_INFO = TableServerRequest.SORT_INFO;
    public static final String TBL_FILTER_BY = TableServerRequest.FILTERS;
    public static final String TBL_PAGE_SIZE = TableServerRequest.PAGE_SIZE;
    public static final String TBL_START_IDX = TableServerRequest.START_IDX;
    public static final String FIXED_LENGTH = TableServerRequest.FIXED_LENGTH;

    public static final String TBL_OPTIONS = "tableOptions";    // refer to TablePanelCreator for list of options

    public static final String TYPE_SELECTABLE = "selectable";
    public static final String TYPE_BASIC  = "basic";
    public static final String SEARCH_PROC_ID = "IpacTableFromSource";

//============================================================================================
//------- Methods take the JSPlotRequest, called from javascript, converts then calls others -
//============================================================================================

    public static void showTable(JscriptRequest jspr, String tableID, String containerID) {
        showTable(jspr, tableID, containerID, false);
    }

    public static void showTable(JscriptRequest jspr, String tableID, String containerID, boolean doCache) {

        if (containerID==null) containerID= tableID;
        else if (containerID.equals("none")) containerID = null;
        EventHub hub= FFToolEnv.getHub();
        TableServerRequest req = convertToRequest(jspr);
        Map<String, String> params = extractParams(jspr, TBL_OPTIONS);
        if (!doCache) {
            req.setParam("rtime", String.valueOf(System.currentTimeMillis()));
        }
        if (!params.containsKey(TablePanelCreator.QUERY_SOURCE)) {
            params.put(TablePanelCreator.QUERY_SOURCE,tableID);
        }

        if (!params.containsKey(TablePanelCreator.HELP_ID)) {
            params.put(TablePanelCreator.HELP_ID,"tables");
        }

        if (req == null) return;

        String type = jspr.getParam(TBL_TYPE);
        String tblType = (!StringUtils.isEmpty(type) && type.equals(TYPE_SELECTABLE)) ? WidgetFactory.TABLE : WidgetFactory.BASIC_TABLE;

        if (req.containsParam(ServerParams.SOURCE)) {
            req.setParam(ServerParams.SOURCE, FFToolEnv.modifyURLToFull(req.getParam(ServerParams.SOURCE)));
        }

        final PrimaryTableUI table = Application.getInstance().getWidgetFactory().createPrimaryUI(tblType, req, params);




        PrimaryTableUILoader loader = new PrimaryTableUILoader(new TableLoadHandler() {
            public Widget getMaskWidget() {
                return table.getDisplay();
            }
            public void onLoad() {}
            public void onError(PrimaryTableUI table, Throwable t) {}
            public void onLoaded(PrimaryTableUI table) {}
            public void onComplete(int totalRows) {}
        });

        loader.addTable(table);
        loader.loadAll();


        Widget panel= FFToolEnv.getPanelByID(containerID);
        if (panel instanceof RootPanel && ((RootPanel)panel).getWidgetCount()>0) {
            RootPanel rp= (RootPanel)panel;
            Widget w;
            for (int i = 0; i < rp.getWidgetCount(); i++) {
                w= rp.getWidget(i);
                if (w instanceof TablePanel) {
                    w.removeFromParent();
                    i = 0;
                }
            }
            rp.add(table.getDisplay());
        }
        else {
            FFToolEnv.addToPanel(containerID, table.getDisplay(), table.getTitle());
        }

        if (table.getDisplay() instanceof TablePanel) {
            hub.bind((TablePanel)table.getDisplay());
        }

    }


    private static Map<String, String> extractParams(JscriptRequest jspr, String paramName) {
        HashMap<String, String> params = new HashMap<String, String>();
        String optStr = jspr.getParam(paramName);
        if (!StringUtils.isEmpty(optStr)) {
            String[] options = StringUtils.split(optStr, ",");
            for (String s : options) {
                String[] parts = StringUtils.split(s, "=", 2);
                params.put(parts[0], parts[1]);
            }
        }
        return params;
    }

    public static void showExpandedTable(JscriptRequest jspr) {
        TableServerRequest req = convertToRequest(jspr);
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    public static TableServerRequest convertToRequest(JscriptRequest jspr) {

        TableServerRequest dataReq= new TableServerRequest(SEARCH_PROC_ID);

        Map<String, String> params = jspr.asMap();

        for (String key : params.keySet()) {
            if (StringUtils.isEmpty(key)) continue;

            String val = params.get(key);

            if (key.equals(TBL_SOURCE)) {
                if (val == null || val.startsWith("$") || val.startsWith("/")) {
                    dataReq.setParam(key, val);
                } else {
                    String url = FFToolEnv.modifyURLToFull(val);
                    dataReq.setParam(key, url);
                }
            } else if (key.equals(TBL_FILTER_BY) && !StringUtils.isEmpty(val)) {
                dataReq.setFilters(StringUtils.asList(val, ","));
            } else if (key.equals(TBL_SORT_INFO) && !StringUtils.isEmpty(val)) {
                dataReq.setSortInfo(SortInfo.parse(val));
            } else if (key.equals(TBL_START_IDX) && !StringUtils.isEmpty(val)) {
                dataReq.setStartIndex(Integer.parseInt(val));
            } else if (key.equals(TBL_PAGE_SIZE) && !StringUtils.isEmpty(val)) {
                dataReq.setPageSize(Integer.parseInt(val));
            } else if (!StringUtils.isEmpty(val)) {
                dataReq.setParam(key, val);
            }

        }
        return dataReq;
    }

}

