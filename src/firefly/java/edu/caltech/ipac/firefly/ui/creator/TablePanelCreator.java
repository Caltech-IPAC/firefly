/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
    public static final String HELP_ID = "HelpID";

    public static final String SHOW_FILTER = "show-filter";
    public static final String SHOW_POPOUT = "show-popout";
    public static final String SHOW_TITLE = "show-title";
    public static final String SHOW_TOOLBAR = "show-toolbar";
    public static final String SHOW_OPTIONS = "show-options";
    public static final String SHOW_PAGING = "show-paging";
    public static final String SHOW_SAVE = "show-save";
    public static final String SHOW_UNITS = "show-units";
    public static final String SHOW_TABLE_VIEW = "show-table-view";


    public TablePrimaryDisplay create(TableServerRequest req, final Map<String, String> params) {
        String title = params.get(TITLE);
        String desc = params.get(SHORT_DESC);
        String tname = params.get(QUERY_SOURCE);
        String helpId = params.get(HELP_ID);
        tname = tname == null ? req.getRequestId() : tname;

        final Ref<TablePanel> tableRef = new Ref<TablePanel>();

        BaseTableConfig<TableServerRequest> config =
                                new BaseTableConfig<TableServerRequest>(req, title, desc);
        final TablePanel table = makeTable(req.getRequestId(), config.getLoader());
        if (req.getPageSize() > 0) {
            table.getDataModel().setPageSize(req.getPageSize());
        }
        if (!StringUtils.isEmpty(req.getSortInfo())) {
            table.getDataModel().setSortInfo(req.getSortInfo());
        }
        if (!StringUtils.isEmpty(req.getFilters())) {
            table.getDataModel().setFilters(req.getFilters());
        }
        if (!StringUtils.isEmpty(helpId)) {
            table.setHelpId(helpId);
        }

        table.setShortDesc(desc);
        table.setName(tname);
        tableRef.setSource(table);
        table.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                if (params.containsKey(SHOW_FILTER)) {
                    boolean flag = Boolean.parseBoolean(params.get(SHOW_FILTER));
                    table.showFiltersButton(flag);
                    table.getTable().showFilters(flag);
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
                    table.showPagingBar(Boolean.parseBoolean(params.get(SHOW_PAGING)));
                }
                if (params.containsKey(SHOW_SAVE)) {
                    table.showSaveButton(Boolean.parseBoolean(params.get(SHOW_SAVE)));
                }
                if (params.containsKey(SHOW_UNITS)) {
                    table.getTable().setShowUnits(Boolean.parseBoolean(params.get(SHOW_UNITS)));
                }
                if (params.containsKey(SHOW_TABLE_VIEW)) {
                    table.showTableView(Boolean.parseBoolean(params.get(SHOW_TABLE_VIEW)));
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

