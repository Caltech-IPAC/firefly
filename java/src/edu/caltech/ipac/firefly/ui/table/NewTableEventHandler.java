package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.NewTableResults;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
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

    private TablePreviewEventHub hub;
    private TabPane tab;

    public NewTableEventHandler(TablePreviewEventHub hub, TabPane tab) {
        this.hub = hub;
        this.tab = tab;

        WebEventManager.getAppEvManager().addListener(Name.NEW_TABLE_RETRIEVED, this);

        tab.getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    TabPane.Tab<TablePanel> tab = (TabPane.Tab<TablePanel>) ev.getData();
                    if (tab.isRemovable()) {
                        NewTableEventHandler.this.hub.unbind(tab.getContent());
                    }
                }
            });

        tab.addCloseHandler(new CloseHandler<TabPane>(){
                    public void onClose(CloseEvent<TabPane> tabPaneCloseEvent) {
                        WebEventManager.getAppEvManager().removeListener(Name.NEW_TABLE_RETRIEVED, NewTableEventHandler.this);
                    }
                });
    }

    public void eventNotify(WebEvent ev) {
        NewTableResults tableInfo = (NewTableResults) ev.getData();
        if (tab == null || !GwtUtil.isOnDisplay(tab)) {
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
                }
            });
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
