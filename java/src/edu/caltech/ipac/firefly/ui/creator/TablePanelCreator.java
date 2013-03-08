package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.table.Loader;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.Map;
/**
 * User: roby
 * Date: Apr 19, 2010
 * Time: 4:26:06 PM
 */


/**
 * @author Trey Roby
 */
public class TablePanelCreator implements PrimaryTableCreator {
    public static final String TITLE = "Title";
    public static final String SHORT_DESC = "ShortDesc";
    public static final String QUERY_SOURCE = "QuerySource";

    public static final String SHOW_FILTER = "show-filter";
    public static final String SHOW_POPOUT = "show-popout";
    public static final String SHOW_TITLE = "show-title";
    public static final String SHOW_TOOLBAR = "show-toolbar";
    public static final String SHOW_OPTIONS = "show-options";
    public static final String SHOW_PAGING = "show-paging";
    public static final String SHOW_SAVE = "show-save";
    public static final String SHOW_UNITS = "show-units";


    public TablePrimaryDisplay create(TableServerRequest req, final Map<String, String> params) {
        String title = params.get(TITLE);
        String desc = params.get(SHORT_DESC);
        String tname = params.get(QUERY_SOURCE);
        tname = tname == null ? req.getRequestId() : tname;

        final Ref<TablePanel> tableRef = new Ref<TablePanel>();

        BaseTableConfig<TableServerRequest> config =
                                new BaseTableConfig<TableServerRequest>(req, title, desc);
        final TablePanel table = makeTable(req.getRequestId(), config.getLoader());
        if (req.getPageSize() > 0) {
            table.getLoader().setPageSize(req.getPageSize());
        }
        if (!StringUtils.isEmpty(req.getSortInfo())) {
            table.getLoader().setSortInfo(req.getSortInfo());
        }
        if (!StringUtils.isEmpty(req.getFilters())) {
            table.getLoader().setFilters(req.getFilters());
        }

        table.setShortDesc(desc);
        table.setName(tname);
        tableRef.setSource(table);
        table.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                if (params.containsKey(SHOW_FILTER)) {
                    table.showFilters(Boolean.parseBoolean(params.get(SHOW_FILTER)));
                }
                if (params.containsKey(SHOW_POPOUT)) {
                    table.showPopOutButton(Boolean.parseBoolean(params.get(SHOW_POPOUT)));
                }
                if (params.containsKey(SHOW_TITLE)) {
                    table.showTitle(Boolean.parseBoolean(params.get(SHOW_TITLE)));
                }
                if (params.containsKey(SHOW_TOOLBAR)) {
                    table.showToolBar(Boolean.parseBoolean(params.get(SHOW_TOOLBAR)));
                }
                if (params.containsKey(SHOW_OPTIONS)) {
                    table.showOptionsButton(Boolean.parseBoolean(params.get(SHOW_OPTIONS)));
                }
                if (params.containsKey(SHOW_PAGING)) {
                    table.showPaggingBar(Boolean.parseBoolean(params.get(SHOW_PAGING)));
                }
                if (params.containsKey(SHOW_SAVE)) {
                    table.showSaveButton(Boolean.parseBoolean(params.get(SHOW_SAVE)));
                }
                if (params.containsKey(SHOW_UNITS)) {
                    table.getTable().setShowUnits(Boolean.parseBoolean(params.get(SHOW_UNITS)));
                }
                table.getEventManager().removeListener(TablePanel.ON_INIT, this);
            }
        });



        TablePrimaryDisplay tpd = new TablePrimaryDisplay(table);

        if (!StringUtils.isEmpty(title))
            tpd.setTitle(title);

        return tpd; 
    }

    protected TablePanel makeTable(String name, Loader<TableDataView> loader) {
        return new TablePanel(name, loader);
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
