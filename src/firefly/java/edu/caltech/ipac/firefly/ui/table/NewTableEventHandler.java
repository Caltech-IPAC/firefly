/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Command;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.NewTabInfo;
import edu.caltech.ipac.firefly.data.NewTableResults;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.TablePrimaryDisplay;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.builder.TableConfig;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: Apr 30, 2010
 *
 * @author loi
 * @version $Id: NewTableEventHandler.java,v 1.5 2010/11/24 01:55:57 loi Exp $
 */
public class NewTableEventHandler implements WebEventListener {

    private EventHub hub;
    private TabPane tab;
    private boolean hiddenCleanup;

    public NewTableEventHandler(EventHub hub, TabPane tab) {
       this(hub,tab,true);
    }

    public NewTableEventHandler(EventHub hub, TabPane tab, boolean hiddenCleanup) {
        this.hub = hub;
        this.tab = tab;
        this.hiddenCleanup= hiddenCleanup;

        WebEventManager.getAppEvManager().addListener(Name.NEW_TABLE_RETRIEVED, this);

        tab.getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    TabPane.Tab<TablePanel> tab = (TabPane.Tab<TablePanel>) ev.getData();
                    if (tab.isRemovable()) {
                        if (tab.getContent() instanceof TablePanel) {
                            NewTableEventHandler.this.hub.unbind(tab.getContent());
                        }
                    }
                }
            });

        if (hiddenCleanup) {
            tab.addCloseHandler(new CloseHandler<TabPane>(){
                public void onClose(CloseEvent<TabPane> tabPaneCloseEvent) {
                    WebEventManager.getAppEvManager().removeListener(Name.NEW_TABLE_RETRIEVED, NewTableEventHandler.this);
                }
            });
        }
    }

    public void eventNotify(WebEvent ev) {

        if ( ev.getData() instanceof NewTableResults) {
            NewTableResults tableInfo = (NewTableResults) ev.getData();
            if (tab == null || (!GwtUtil.isOnDisplay(tab) && hiddenCleanup) ) {
                WebEventManager.getAppEvManager().removeListener(Name.NEW_TABLE_RETRIEVED,this);
            }

            if (tableInfo != null && tableInfo.getConfig() != null) {
                final TableConfig tconfig = tableInfo.getConfig();
                WidgetFactory factory = Application.getInstance().getWidgetFactory();
                Map<String, String> params = new HashMap<String, String>(2);
                params.put(TablePanelCreator.TITLE, tconfig.getTitle());
                params.put(TablePanelCreator.SHORT_DESC, tconfig.getShortDesc());
                final PrimaryTableUI table = factory.createPrimaryUI(tableInfo.getTableType(),
                        tconfig.getSearchRequest(), params);
                final TabPane.Tab tabItem = tab.addTab(table.getDisplay(), tconfig.getTitle(), tconfig.getShortDesc(), true);
                table.bind(hub);
                table.load(new BaseCallback<Integer>(){
                    public void doSuccess(Integer result) {
                        DownloadRequest dlreq = tconfig.getDownloadRequest();
                        if (dlreq!=null) {
                            table.addDownloadButton(tconfig.getDownloadSelectionIF(), dlreq.getRequestId(),
                                    dlreq.getFilePrefix(), dlreq.getTitlePrefix(), null);
                        }
                        tab.selectTab(tabItem);
                        if (table instanceof TablePrimaryDisplay) {
                            TablePanel tp= ((TablePrimaryDisplay)table).getTable();
                            tp.getTable().setShowUnits(true);
                            tp.getTable().showFilters(true);
                        }
                    }
                });
            }
        } else if (ev.getData() instanceof NewTabInfo) {
            NewTabInfo tabInfo = (NewTabInfo) ev.getData();
            final TabPane.Tab tabItem = tab.addTab(tabInfo.getDisplay(), tabInfo.getName(), tabInfo.getTooltips(), tabInfo.isRemovable());
            tabInfo.setOnLoadedAction(new Command(){
                public void execute() {
                    tab.selectTab(tabItem);
                }
            });
        }

    }
}
