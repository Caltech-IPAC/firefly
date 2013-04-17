package edu.caltech.ipac.firefly.ui.table.builder;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.TableLoadHandler;
import edu.caltech.ipac.firefly.core.background.Backgroundable;
import edu.caltech.ipac.firefly.core.background.CanCancel;
import edu.caltech.ipac.firefly.ui.BundledServerTask;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;

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
        }

        public void doTask(AsyncCallback<Integer> passAlong) {
            uiComp.load(passAlong);
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
