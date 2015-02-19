/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table.builder;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.TableLoadHandler;
import edu.caltech.ipac.firefly.core.background.Backgroundable;
import edu.caltech.ipac.firefly.core.background.CanCancel;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.NewTableResults;
import edu.caltech.ipac.firefly.ui.BundledServerTask;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.List;


/**
 * Date: Jun 9, 2009
 *
 * @author loi
 * @version $Id: PrimaryTableUILoader.java,v 1.4 2010/09/17 00:27:47 loi Exp $
 */
public class PrimaryTableUILoader {

    private TableLoadHandler handler;
    private List<PrimaryTableUI> tables = new ArrayList<PrimaryTableUI>();
    private int totalRows;
    private BundledServerTask tasks;
    private boolean autoMask = false;
    private boolean isCancelled = false;

    public PrimaryTableUILoader(TableLoadHandler handler) {
        this.handler = handler;
    }

    public void setAutoMask(boolean autoMask) {
        this.autoMask = autoMask;
    }

    public void addTable(PrimaryTableUI uiComp) {
        tables.add(uiComp);
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void loadAll() {

        if (tables.size() == 0) {
            handler.onComplete(0);
            return;
        }

        tasks = new BundledServerTask(autoMask ? handler.getMaskWidget() : null, true) {
            public void finish() {
                handler.onComplete(totalRows);
            }
        };

        for(PrimaryTableUI ui : tables) {
            TableLoader t = new TableLoader(ui);
            tasks.addServerTask(t);
            t.setAutoMask(autoMask);
        }

        tasks.startAll();
        handler.onLoad();
    }

    public void cancelAll() {
        isCancelled = true;
        if (tasks != null) {
            for(ServerTask t : tasks.getTasks()) {
                t.cancel();
            }
        }
        if (tables != null) {
            for(PrimaryTableUI t : tables) {
                if (t.getDataModel().getLoader() instanceof CanCancel) {
                    ((CanCancel)t.getDataModel().getLoader()).cancelTask();
                }
            }
        }
    }

    public void sendToBackground() {
        isCancelled = true;
        if (tables != null) {
            for(PrimaryTableUI t : tables) {
                if (t.getDataModel().getLoader() instanceof Backgroundable) {
                    ((Backgroundable)t.getDataModel().getLoader()).backgrounded();
                }
            }
        }
    }

    public List<PrimaryTableUI> getTables() {
        return tables;
    }

    class TableLoader extends ServerTask<Integer> {
        private PrimaryTableUI uiComp;

        TableLoader(PrimaryTableUI uiComp) {
            super(uiComp.getDisplay(), "Loading", true);
            this.uiComp = uiComp;
        }

        @Override
        protected void onFailure(Throwable caught) {
            handler.onError(uiComp, caught);
        }

        public void onSuccess(Integer result) {
            totalRows += result;
            handler.onLoaded(uiComp);
            DataSetTableModel ds = uiComp.getDataModel();
            if (ds != null && ds.getCurrentData() != null) {
                String use = ds.getCurrentData().getMeta().getAttribute(CatalogRequest.USE);
                if (CatalogRequest.Use.CATALOG_OVERLAY.getDesc().equals(String.valueOf(use))) {
                    NewTableResults tr = new NewTableResults(ds.getRequest(), WidgetFactory.TABLE, uiComp.getTitle());
                    WebEventManager.getAppEvManager().fireEvent(new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED, tr));
                }
            }
        }

        public void doTask(AsyncCallback<Integer> passAlong) {
            uiComp.load(passAlong);
        }
    }

}

